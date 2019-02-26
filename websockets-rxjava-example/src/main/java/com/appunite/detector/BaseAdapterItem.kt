package com.appunite.detector

/*
 * Copyright 2016 Jacek Marchwicki <jacek.marchwicki@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

interface BaseAdapterItem : SimpleDetector.Detectable<BaseAdapterItem> {

    /**
     * Unique adapter id or [.NO_ID]
     *
     * @return adapter id or [.NO_ID]
     */
    fun adapterId(): Long

    companion object {
        /**
         * return this id if you don't have any id's
         */
        const val NO_ID: Long = -1
    }
}

interface DefinedAdapterItem<out T : Any> : BaseAdapterItem {
    fun itemId(): T
    override fun adapterId(): Long = itemId().hashCode().toLong()
    override fun matches(item: BaseAdapterItem): Boolean = when (item) {
        is DefinedAdapterItem<*> -> itemId() == item.itemId()
        else -> false
    }
    override fun same(item: BaseAdapterItem): Boolean = this == item
}
