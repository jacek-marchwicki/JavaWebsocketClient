/*
 * Copyright (C) 2015 Jacek Marchwicki <jacek.marchwicki@gmail.com>
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

package com.appunite.websocket.rx;

import java.io.IOException;

import javax.annotation.Nonnull;

import okhttp3.Response;

/**
 * Exception indicating that server requested close connection
 */
public class ServerHttpError extends IOException {

    @Nonnull
    private final Response response;

    public ServerHttpError(@Nonnull Response response) {
        super("Http server error=" + response.code() + ", message= " + response.message());
        this.response = response;
    }

    /**
     * Response from server
     * @return response why connection couldn't be established
     */
    @Nonnull
    public Response response() {
        return response;
    }

}
