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

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Optional;

import android.test.AndroidTestCase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static com.sudhir.json.matchers.JsonMatchers.*;

public class IOMessageTest extends AndroidTestCase {
	
	private void assertFailParsing(String message) {
		try {
			IOMessage.parse(message);
			fail("Should throw error while parsing: " + message);
		} catch (WrongSocketIOResponse e) {}
	}
	
	public void testWrongMessages() {
		assertFailParsing("");
		assertFailParsing("1");
		assertFailParsing("-1::");
		assertFailParsing("1234::");
		assertFailParsing("asdf::");
	}
	
	public void testGoodMessages() throws WrongSocketIOResponse {
		IOMessage.parse("1:");
		IOMessage.parse("1::");
		IOMessage.parse("2::");
		IOMessage.parse("3:::");
		IOMessage.parse("4::::::::::::");
	}
	
	
	public void testEventParsing() throws WrongSocketIOResponse, JSONException {
		String msg = "5::/namespace:{\"some1\":\"somevalue1\",\"some2\":[\"2012-10-22T09:55:10Z\"]}";
		IOMessage message = IOMessage.parse(msg);
		assertThat(message.mMessageType, is(equalTo(5)));
		assertThat(message.mMessageId, is(equalTo("")));
		assertThat(message.mMessageEndpoint, is(equalTo("/namespace")));
		assertThat(message.mMessageData, isPresent());
		JSONObject json = new JSONObject(message.mMessageData.get());
		assertThat(json, hasKey("some1"));
		assertThat(json, hasKey("some2"));
	}
	
	public void testOpenEmptyParsing() throws WrongSocketIOResponse {
		String msg = "1::";
		IOMessage message = IOMessage.parse(msg);
		assertThat(message.mMessageType, is(equalTo(1)));
		assertThat(message.mMessageId, is(equalTo("")));
		assertThat(message.mMessageEndpoint, is(equalTo("")));
	}
	
	public void testOpenEmptyEndpointParsing() throws WrongSocketIOResponse {
		String msg = "1:asdf:";
		IOMessage message = IOMessage.parse(msg);
		assertThat(message.mMessageType, is(equalTo(1)));
		assertThat(message.mMessageId, is(equalTo("asdf")));
		assertThat(message.mMessageEndpoint, is(equalTo("")));
	}
	
	public void testOpenParsing() throws WrongSocketIOResponse {
		String msg = "1:asdf:/namespace";
		IOMessage message = IOMessage.parse(msg);
		assertThat(message.mMessageType, is(equalTo(1)));
		assertThat(message.mMessageId, is(equalTo("asdf")));
		assertThat(message.mMessageEndpoint, is(equalTo("/namespace")));
	}

	private static class  IsPresent extends TypeSafeMatcher<Optional<?>> {

		@Override
		public void describeTo(Description description) {
			description.appendText("not present");
		}

		@Override
		protected boolean matchesSafely(Optional<?> optional) {
			 return optional.isPresent();
		}
	}
	

	private Matcher<? super Optional<?>> isPresent() {
		return new IsPresent();
	}
}
