/*
 * Copyright 2015 Jacek Marchwicki <jacek.marchwicki@gmail.com>
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

package com.appunite.detector

import com.google.common.base.Function
import com.google.common.collect.FluentIterable

import java.util.ArrayList

import com.google.common.base.Preconditions.checkNotNull

class ChangesDetector<T, H>(detector: Detector<T, H>) {

    var mItems: List<H> = ArrayList()
    private val mDetector: Detector<T, H> = detector

    interface ChangesAdapter {

        fun notifyItemRangeInserted(start: Int, count: Int)

        fun notifyItemRangeChanged(start: Int, count: Int)

        fun notifyItemRangeRemoved(start: Int, count: Int)
    }

    interface Detector<T, H> : Function<T, H> {

        override fun apply(item: T?): H?

        fun matches(item: H, newOne: H): Boolean

        fun same(item: H, newOne: H): Boolean
    }

    private fun indexOf(list: List<H>,
                        start: Int,
                        item: H): Int {
        var i = start
        val listSize = list.size
        while (i < listSize) {
            val t = list[i]
            if (mDetector.matches(t, item)) {
                return i
            }
            i++
        }
        return -1
    }

    fun newData(adapter: ChangesAdapter,
                values: List<T>,
                force: Boolean) {
        checkNotNull(adapter)
        checkNotNull(values)

        val list = FluentIterable.from(values)
                .transform(mDetector)
                .toList()

        var firstListPosition = 0
        var secondListPosition = 0

        var counter = 0
        var toRemove = 0

        while (firstListPosition < mItems.size) {
            val first = mItems[firstListPosition]
            val indexOf = indexOf(list, secondListPosition, first)
            if (indexOf >= 0) {
                val itemsInserted = indexOf - secondListPosition
                counter = notify(adapter, counter, toRemove, itemsInserted)
                toRemove = 0
                secondListPosition = indexOf + 1

                val second = list[indexOf]
                if (force || !mDetector.same(first, second)) {
                    adapter.notifyItemRangeChanged(counter, 1)
                }
                counter += 1
            } else {
                toRemove += 1
            }
            ++firstListPosition
        }

        val itemsInserted = values.size - secondListPosition
        notify(adapter, counter, toRemove, itemsInserted)
        mItems = list
    }

    private fun notify(adapter: ChangesAdapter,
                       counter: Int,
                       toRemove: Int,
                       itemsInserted: Int): Int {
        var mCounter = counter
        var mToRemove = toRemove
        var mItemsInserted = itemsInserted
        val itemsChanged = Math.min(itemsInserted, mToRemove)
        mToRemove -= itemsChanged
        mItemsInserted -= itemsChanged
        if (itemsChanged > 0) {
            adapter.notifyItemRangeChanged(mCounter, itemsChanged)
            mCounter += itemsChanged
        }
        if (toRemove > 0) {
            adapter.notifyItemRangeRemoved(mCounter, mToRemove)
        }
        if (itemsInserted > 0) {
            adapter.notifyItemRangeInserted(mCounter, mItemsInserted)
            mCounter += mItemsInserted
        }
        return counter
    }

}