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


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.RecyclerView

/**
 * Manager that managing of creation [ViewHolderManager.BaseViewHolder]
 */
interface ViewHolderManager {

    /**
     * Return if this manager can handle that kind of type
     * @param baseAdapterItem adapter item
     * @return true if can handle this item
     */
    fun matches(baseAdapterItem: BaseAdapterItem): Boolean

    /**
     * Create [ViewHolderManager.BaseViewHolder] for this item
     * @param parent parent view
     * @param inflater layout inflater
     * @return new [ViewHolderManager.BaseViewHolder]
     */
    fun createViewHolder(parent: ViewGroup, inflater: LayoutInflater): BaseViewHolder<*>

    /**
     * ViewHolder for managing view
     * @param <T> type of adapter item
    </T> */
    abstract class BaseViewHolder<T : BaseAdapterItem>
    /**
     * Create view holder
     * @param itemView view
     */
    (itemView: View) : RecyclerView.ViewHolder(itemView) {

        /**
         * Called when a view created by this view holder has been recycled.
         *
         * @see RecyclerView.Adapter.onViewRecycled
         */
        open fun onViewRecycled() {

        }

        /**
         * Called by RecyclerView to display the data at the specified position. This method
         * should update the contents of that view holder to reflect the item.
         *
         * @param item adapter item to bind
         *
         * @see RecyclerView.Adapter.onBindViewHolder
         */
        abstract fun bind(item: T)

        /**
         * Called by the RecyclerView if a view holder cannot be recycled due to its transient
         * state. Upon receiving this callback, view holder can clear the animation(s) that effect
         * the View's transient state and return `true` so that the View can be recycled.
         * Keep in mind that the View in question is already removed from the RecyclerView.
         *
         * @see RecyclerView.Adapter.onFailedToRecycleView
         * @return True if the View should be recycled, false otherwise. Note that if this method
         * returns `true`, RecyclerView *will ignore* the transient state of
         * the View and recycle it regardless. If this method returns `false`,
         * RecyclerView will check the View's transient state again before giving a final decision.
         * Default implementation returns false.
         */
        fun onFailedToRecycleView(): Boolean {
            return false
        }

        /**
         * Called when a view created by this view holder has been attached to a window.
         *
         * @see RecyclerView.Adapter.onViewAttachedToWindow
         */
        fun onViewAttachedToWindow() {

        }

        /**
         * Called when a view created by this view holder has been detached from its window.
         *
         * @see RecyclerView.Adapter.onViewDetachedFromWindow
         */
        fun onViewDetachedFromWindow() {

        }
    }
}