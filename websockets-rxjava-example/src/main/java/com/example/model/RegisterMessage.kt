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

package com.example.model

import com.google.gson.annotations.SerializedName

class RegisterMessage(@field:SerializedName("auth_token")
                      val authToken: String) : Message(MessageType.REGISTER) {

    fun authToken(): String {
        return authToken
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is RegisterMessage) return false

        val that = o as RegisterMessage?

        return authToken == that!!.authToken

    }

    override fun hashCode(): Int {
        return authToken.hashCode()
    }

    override fun toString(): String {
        return "RegisterRequest{" +
                "message='" + authToken + '\''.toString() +
                "} " + super.toString()
    }
}
