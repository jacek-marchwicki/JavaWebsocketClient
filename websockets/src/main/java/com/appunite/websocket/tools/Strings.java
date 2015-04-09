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

package com.appunite.websocket.tools;

import javax.annotation.Nullable;

public class Strings {

    /**
     * Returns the given string if it is non-null; the empty string otherwise.
     *
     * @param string the string to test and possibly return
     * @return {@code string} itself if it is non-null; {@code ""} if it is null
     */
    public static String nullToEmpty(@Nullable String string) {
        return (string == null) ? "" : string;
    }

    /**
     * Returns the given string if it is nonempty; {@code null} otherwise.
     *
     * @param string the string to test and possibly return
     * @return {@code string} itself if it is nonempty; {@code null} if it is
     *     empty or null
     */
    public static @Nullable String emptyToNull(@Nullable String string) {
        return isNullOrEmpty(string) ? null : string;
    }

    /**
     * Returns {@code true} if the given string is null or is the empty string.
     *
     * <p>Consider normalizing your string references with {@link #nullToEmpty}.
     * If you do, you can use {@link String#isEmpty()} instead of this
     * method, and you won't need special null-safe forms of methods like {@link
     * String#toUpperCase} either. Or, if you'd like to normalize "in the other
     * direction," converting empty strings to {@code null}, you can use {@link
     * #emptyToNull}.
     *
     * @param string a string reference to check
     * @return {@code true} if the string is null or is the empty string
     */
    public static boolean isNullOrEmpty(@Nullable String string) {
        return string == null || string.length() == 0; // string.isEmpty() in Java 6
    }
}
