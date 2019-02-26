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

package com.example

import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import java.util.logging.Level
import java.util.logging.Logger

object LoggingObservables {

    fun logging(logger: Logger, tag: String): DisposableObserver<Any> = object : DisposableObserver<Any>() {
        override fun onNext(o: Any) {
            logger.log(Level.INFO, "$tag - onNext: {0}", o.toString())
        }

        override fun onComplete() {
            logger.log(Level.INFO, "$tag - onCompleted")
        }

        override fun onError(e: Throwable) {
            logger.log(Level.SEVERE, "$tag - onError", e)
        }

    }



}
