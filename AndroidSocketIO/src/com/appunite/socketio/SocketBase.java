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

import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.appunite.socketio.helpers.HTTPUtils.WrongHttpResponseCode;
import com.appunite.websocket.WrongWebsocketResponse;
import static com.google.common.base.Preconditions.*;

/**
 * Base class for {@link SocketIOBase}. It creates abstraction layer for thread
 * spawning
 * 
 * @author Jacek Marchwicki <jacek.marchwicki@gmail.com>
 * 
 */
abstract class SocketBase implements Runnable {

	private static final String TAG = SocketBase.class.getCanonicalName();
	private static final String SOCKET_THREAD_NAME = "SocketIO thread";

	private static class SocketHandler extends Handler {

		private final SocketBase mSocketBase;

		public SocketHandler(SocketBase socketBase) {
			super();
			this.mSocketBase = socketBase;
		}

		@Override
		public void dispatchMessage(Message msg) {
			mSocketBase.handlerMessage(msg);
		}

	}

	private Object mStartStopLock = new Object();
	private Thread mThread = null;

	protected final SocketListener mListener;

	protected Object mInterruptionLock = new Object();
	protected boolean mInterrupted = true;

	private SocketHandler mHandler;

	public SocketBase(SocketListener listener) {
		super();
		checkArgument(listener != null, "Listener should not be null");
		mListener = listener;
	}

	/**
	 * Receive message on connect method thread
	 * 
	 * @param msg
	 */
	public abstract void handlerMessage(Message msg);

	/**
	 * Connect to server (thread safe)
	 */
	public void connect() {
		synchronized (mStartStopLock) {
			checkState(mThread == null, "You are already connected/connecting");
			assert (mInterrupted == true);
			mInterrupted = false;
			mHandler = new SocketHandler(this);
			mThread = new Thread(this);
			mThread.setName(SOCKET_THREAD_NAME);
			mThread.start();
		}
	}

	/**
	 * Return true if connecting or connection is alive
	 * 
	 * (Thread safe)
	 * 
	 * @return true if connection is alive
	 */
	public boolean isAlive() {
		synchronized (mStartStopLock) {
			return mThread != null;
		}
	}

	/**
	 * Disconnect asynchrously
	 * 
	 * (Thread safe)
	 */
	public void disconnectAsyncIfAlive() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				disconnectIfAlive();
			}
		}).start();
	}

	/**
	 * Disconnect from server if is connecting or connected. This method will
	 * block until disconnection successes. Could not be called from socket
	 * thread;
	 * 
	 * (Thread safe)
	 * 
	 * @return true if connection was alive
	 */
	public boolean disconnectIfAlive() {
		synchronized (mStartStopLock) {
			if (!isAlive())
				return false;
			if (Thread.currentThread() == mThread) {
				throw new RuntimeException(
						"disconnectIfAlive could not be called from socket thread");
			}
			assert (mInterrupted == false);
			synchronized (mInterruptionLock) {
				mInterrupted = true;
				interrupt();
			}
			for (;;) {
				try {
					mThread.join();
					// Loop until success
					break;
				} catch (InterruptedException e) {
				}
			}
			mHandler.removeCallbacksAndMessages(null);
			mHandler = null;
			mThread = null;
			return true;
		}
	}

	@Override
	public void run() {
		for (;;) {
			runSocket();
			synchronized (mInterruptionLock) {
				boolean reconnect = mListener.onDisconnected(mInterrupted);
				if (mInterrupted) {
					checkState(reconnect == false,
							"If disconnection is forced by user reconnect value should be always false");
				}
				if (!reconnect)
					return;
			}
		}
	}

	protected abstract void runSocket();

	protected void sendEmptyMessageDelayed(int what, int delayMillis) {
		mHandler.sendEmptyMessageDelayed(what, delayMillis);
	}

	protected void removeMessages(int what) {
		mHandler.removeMessages(what);
	}

	/**
	 * Try to interrupt current thread
	 */
	protected void interrupt() {
		mThread.interrupt();
	}

	protected void error(WrongSocketIOResponse e) {
		Log.v(TAG, "error:", e);
		mListener.onNetworkError(e);
	}

	protected void error(IOException e) {
		Log.v(TAG, "error:", e);
		mListener.onNetworkError(e);
	}

	protected void error(WrongHttpResponseCode e) {
		Log.v(TAG, "error:", e);
		mListener.onNetworkError(e);
	}

	protected void error(WrongWebsocketResponse e) {
		Log.v(TAG, "error:", e);
		mListener.onNetworkError(e);
	}

}