/*
 * Copyright (C) 2012 Jacek Marchwicki <jacek.marchwicki@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.appunite.socketio;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.net.Uri;
import android.text.TextUtils;

import com.appunite.socketio.helpers.HTTPUtils;
import com.appunite.socketio.helpers.HTTPUtils.WrongHttpResponseCode;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

/**
 * Base class for {@link SocketIO}. Its ask socketio server for websocket
 * connection.
 * 
 * @author Jacek Marchwicki <jacek.marchwicki@gmail.com>
 * 
 */
abstract class SocketIOBase extends SocketBase {

	private HttpClient mClient = null;
	protected final String mSocketIOUrl;
	private HttpGet mRequest;

	protected static class ConnectionResult {
		String sessionId;
		Uri socketUri;
		Optional<Integer> timeout = null;
		Optional<Integer> heartbeat = null;
	}
	
	private HttpClient getClient() {
		if (mClient != null)
			return mClient;
		mClient = new DefaultHttpClient();
		return mClient;
	}

	public SocketIOBase(String url, SocketListener listener) {
		super(listener);
		checkArgument(url != null, "Argument should not be null");
		this.mSocketIOUrl = url;
	}

	private Optional<Integer> getIntOrAbsetFromString(String str)
			throws WrongSocketIOResponse {
		if (TextUtils.isEmpty(str)) {
			return Optional.absent();
		} else {
			try {
				return Optional.of(Integer.parseInt(str));
			} catch (NumberFormatException e) {
				throw new WrongSocketIOResponse("Could not parse integer", e);
			}
		}
	}

	private ImmutableSet<String> getTypesFromString(String str)
			throws WrongSocketIOResponse {
		StringTokenizer st = new StringTokenizer(str, ",");
		HashSet<String> types = new HashSet<String>();
		while (st.hasMoreTokens()) {
			types.add(st.nextToken());
		}
		if (types.isEmpty())
			throw new WrongSocketIOResponse(
					"No transport types in socket response");
		return ImmutableSet.copyOf(types);
	}

	private ConnectionResult connect(String url) throws WrongHttpResponseCode,
			IOException, WrongSocketIOResponse,
			InterruptedException {
		Uri uri = Uri.parse(url);

		synchronized (mInterruptionLock) {
			if (mInterrupted) {
				throw new InterruptedException();
			}
			mRequest = new HttpGet(url);
		}
		HTTPUtils.setupDefaultHeaders(mRequest);

		HttpResponse response = getClient().execute(mRequest);
		synchronized (mInterruptionLock) {
			if (mRequest.isAborted() || mInterrupted) {
				mRequest = null;
				throw new InterruptedException();
			}
			mRequest = null;
		}
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			throw new WrongHttpResponseCode(response);
		}

		String responseStr = HTTPUtils.getStringFromResponse(response);
		StringTokenizer responseSplit = new StringTokenizer(responseStr, ":");

		ConnectionResult result = new ConnectionResult();
		try {
			result.sessionId = responseSplit.nextToken();
			if (TextUtils.isEmpty(result.sessionId)) {
				throw new WrongSocketIOResponse("Empty socket io session id");
			}

			result.timeout = getIntOrAbsetFromString(responseSplit.nextToken());
			result.heartbeat = getIntOrAbsetFromString(responseSplit.nextToken());

			ImmutableSet<String> types = getTypesFromString(responseSplit.nextToken());
			if (!types.contains("websocket")) {
				throw new WrongSocketIOResponse(
						"Websocket not found in server response");
			}
		} catch (NoSuchElementException e) {
			throw new WrongSocketIOResponse(
					"Not enough color separated values in response", e);
		}
		result.socketUri = new Uri.Builder().scheme("ws")
				.encodedAuthority(uri.getEncodedAuthority())
				.path(uri.getPath()).appendPath("websocket")
				.appendPath(result.sessionId).build();
		return result;
	}

	@Override
	protected void interrupt() {
		super.interrupt();
		if (mRequest != null) {
			mRequest.abort();
		}
	}
	
	@Override
	public void runSocket() {
		while (!mInterrupted) {
			try {
				ConnectionResult connect = connect(mSocketIOUrl);
				connectToTransport(connect);
			} catch (WrongHttpResponseCode e) {
				error(e);
			} catch (IOException e) {
				error(e);
			} catch (WrongSocketIOResponse e) {
				error(e);
			} catch (InterruptedException e) {
				// just let die
				continue;
			}
			return;
		}
	}

	abstract void connectToTransport(ConnectionResult connect) throws InterruptedException;

}