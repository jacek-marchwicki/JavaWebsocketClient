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

package com.appunite.socketio.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.appunite.socketio.BuildConfig;

import android.text.TextUtils;
import android.util.Log;

/**
 * Utilities that are for sure necessary when dealing with apache HttpClient and
 * with JSON
 * 
 * @author Jacek Marchwicki <jacek.marchwicki@gmail.com>
 * 
 */
public class HTTPUtils {

	/**
	 * Error thrown when wrong response code was returned by HttpResponse
	 * 
	 * @author Jacek Marchwicki <jacek.marchwicki@gmail.com>
	 * 
	 */
	public static class WrongHttpResponseCode extends Exception {
		private int mStatusCode;

		public WrongHttpResponseCode(HttpResponse response) {
			super(String.format("Wrong response from server: %d", response
					.getStatusLine().getStatusCode()));
			mStatusCode = response.getStatusLine().getStatusCode();

		}

		public int getStatusCode() {
			return mStatusCode;
		}

		private static final long serialVersionUID = 1L;

	}

	/**
	 * Builder helpful when creating URL Get requests
	 * 
	 * @author Jacek Marchwicki <jacek.marchwicki@gmail.com>
	 * 
	 *         Example:
	 * 
	 *         <pre>
	 * String url = new GetBuilder(&quot;www.google.com&quot;)
	 * 	.addParam(&quot;q&quot;, &quot;appunite.com&quot;)
	 * 	.addParam(&quot;lng&quot;, &quot;en&quot;)
	 * 	.build();
	 * </pre>
	 * 
	 */
	public static class GetBuilder {
		private String mUrl;
		private List<NameValuePair> mParams;

		/**
		 * Create new builder with url
		 * 
		 * @param url
		 *            url
		 */
		public GetBuilder(String url) {
			mUrl = url;
			mParams = new LinkedList<NameValuePair>();
		}
		
		/**
		 * Append path segment to url
		 * @param pathSegment path segment to append to url
		 * @return builder
		 */
		public GetBuilder appendPathSegment(String pathSegment) {
			mUrl += mUrl + "/" + pathSegment;
			return this;
		}

		/**
		 * Add get params to url
		 * 
		 * @param paramName
		 *            name of parameter
		 * @param param
		 *            value of parameter
		 * @return builder
		 */
		public GetBuilder addParam(String paramName, String param) {
			mParams.add(new BasicNameValuePair(paramName, param));
			return this;
		}

		/**
		 * build utf-8 encoded url
		 * 
		 * @return utf-8 encoded url
		 */
		public String build() {
			return mUrl + "?" + URLEncodedUtils.format(mParams, "utf-8");
		}
	}

	private final static String TAG = HTTPUtils.class.getCanonicalName();

	private static String sLanguageString = null;

	private static Locale sCurrentLocale = null;

	private static void getLanguageString() {
		sCurrentLocale = Locale.getDefault();
		String language = sCurrentLocale.getLanguage();

		if (TextUtils.isEmpty(language)) {
			sLanguageString = null;
			return;
		}

		String country = sCurrentLocale.getCountry();
		if (TextUtils.isEmpty(country)) {
			sLanguageString = language;
			return;
		}

		String variant = sCurrentLocale.getVariant();
		if (TextUtils.isEmpty(variant)) {
			sLanguageString = String.format("%s_%s, %s", language, country,
					language);
			return;
		}

		sLanguageString = String.format("%s_%s_%s, %s_%s, %s", language,
				country, variant, language, country, language);
	}

	private static void setLangageForHttpRequest(HttpRequestBase request) {
		if (sCurrentLocale == null
				|| !Locale.getDefault().equals(sCurrentLocale)) {
			getLanguageString();

		}
		if (sLanguageString != null) {
			request.addHeader("Accept-Language", sLanguageString);
		}
	}

	/**
	 * Add standard headers to response.
	 * 
	 * initialize Accept-Language by current locale setup Accept-Encoding to
	 * gzip
	 * 
	 * @param request
	 *            request to witch should be added headers
	 */
	public static void setupDefaultHeaders(HttpRequestBase request) {
		setLangageForHttpRequest(request);
		request.addHeader("Accept-Encoding", "gzip");
	}

	private static String getStringFromInputStream(InputStream is)
			throws IOException {
		byte[] bytes = new byte[1000];

		StringBuilder sb = new StringBuilder();

		int numRead = 0;
		while ((numRead = is.read(bytes)) >= 0) {
			sb.append(new String(bytes, 0, numRead));
		}
		return sb.toString();
	}

	/**
	 * Return body as string from given response even if it is gzip compressed
	 * 
	 * @param response
	 * @return string encoded body
	 * @throws IOException
	 *             thrown when connection was broken
	 */
	public static String getStringFromResponse(HttpResponse response)
			throws IOException {

		HttpEntity entity = response.getEntity();

		Header contentEncoding = response.getFirstHeader("Content-Encoding");
		if (contentEncoding != null
				&& contentEncoding.getValue().equalsIgnoreCase("gzip")) {
			InputStream inStream = new GZIPInputStream(entity.getContent());
			return getStringFromInputStream(inStream);
		} else {
			return EntityUtils.toString(entity);
		}
	}

	/**
	 * Return body as json from given response even if it is gzip compressed
	 * 
	 * @param response
	 * @return json body
	 * @throws JSONException
	 *             thrown when response is not an json
	 * @throws IOException
	 *             thrown when connection was broken
	 */
	public static JSONObject getJsonFromResponse(HttpResponse response)
			throws JSONException, IOException {

		String responseString = getStringFromResponse(response);
		;

		try {
			JSONObject json = new JSONObject(responseString);
			return json;
		} catch (JSONException e) {
			if (BuildConfig.DEBUG) {
				Log.d(TAG, String.format("Wrong response from server: %s",
						response));
			}
			throw e;
		}
	}
}
