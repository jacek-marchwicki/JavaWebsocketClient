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
import android.view.ViewGroup

import java.util.Collections

import androidx.recyclerview.widget.RecyclerView

/**
 * Universal adapter for [RecyclerView] that will automatically detect changes
 */
open class UniversalAdapter
/**
 * Usage:
 * <pre>`
 * public static class Item implements BaseAdapterItem {
 *
 * &#64;Nonnull
 * private final String id;
 * &#64;Nullable
 * private final String lastMessage;
 *
 * public ChatsItem(@Nonnull String id,
 * &#64;Nullable String lastMessage) {
 * this.id = id;
 * this.lastMessage = lastMessage;
 * }
 *
 * &#64;Override
 * public boolean matches(@Nonnull BaseAdapterItem item) {
 * return item instanceof Item && Objects.equal(id, ((Item)item).id);
 * }
 *
 * &#64;Override
 * public boolean same(@Nonnull BaseAdapterItem item) {
 * return equals(item);
 * }
 *
 * &#64;Nonnull
 * public String id() {
 * return id;
 * }
 *
 * &#64;Nullable
 * public String lastMessage() {
 * return lastMessage;
 * }
 *
 * &#64;Override
 * public boolean equals(Object o) {
 * if (this == o) return true;
 * if (!(o instanceof Item)) return false;
 * final Item chatsItem = (ChatsItem) o;
 * return Objects.equal(id, chatsItem.id) &&
 * Objects.equal(lastMessage, chatsItem.lastMessage);
 * }
 *
 * &#64;Override
 * public int hashCode() {
 * return Objects.hashCode(id, lastMessage);
 * }
 *
 * &#64;Override
 * public long adapterId() {
 * return id.hashCode();
 * }
 * }
 *
 * private static class MyViewHolderManager implements ViewHolderManager {
 * &#64;Override
 * public boolean matches(BaseAdapterItem baseAdapterItem) {
 * return baseAdapterItem instanceof Item;
 * }
 *
 * &#64;Override
 * public BaseViewHolder createViewHolder(ViewGroup parent, LayoutInflater from) {
 * return new Holder(from.inflate(R.layout.activity_chats_item, parent, false));
 * }
 *
 * public static class Holder extends BaseViewHolder<Item> {
 *
 * private final TextView textView;
 *
 * public Holder(View itemView) {
 * super(itemView);
 * textView = (TextView) itemView;
 * }
 *
 * &#64;Override
 * public void bind(@Nonnull Item item) {
 * textView.setText(item.lastMessage());
 * }
 *
 * }
 * }
 *
 * final UniversalAdapter adapter = new UniversalAdapter(
 * ImmutableList.<ViewHolderManager>of(new MyViewHolderManager()));
 *
 * recyclerView.setAdapter(adapter);
`</pre> *
 *
 * @param managers for inflating views
 */
(private val managers: List<ViewHolderManager>) : RecyclerView.Adapter<ViewHolderManager.BaseViewHolder<BaseAdapterItem>>(), ChangesDetector.ChangesAdapter {
    private val changesDetector = ChangesDetector(SimpleDetector<BaseAdapterItem>())
    private var items = emptyList<BaseAdapterItem>()

    fun call(baseAdapterItems: List<BaseAdapterItem>) {
        items = baseAdapterItems
        changesDetector.newData(this, items, false)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderManager.BaseViewHolder<BaseAdapterItem> {
        val manager = managers[viewType]
        return manager.createViewHolder(parent, LayoutInflater.from(parent.context)) as ViewHolderManager.BaseViewHolder<BaseAdapterItem>
    }

    override fun getItemViewType(position: Int): Int {
        val baseAdapterItem = items[position]
        for (i in managers.indices) {
            val manager = managers[i]
            if (manager.matches(baseAdapterItem)) {
                return i
            }
        }
        throw RuntimeException("Unsupported item type: $baseAdapterItem")
    }

    override fun onBindViewHolder(holder: ViewHolderManager.BaseViewHolder<BaseAdapterItem>, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemId(position: Int): Long {
        return items[position].adapterId()
    }

    /**
     * Return item at position
     *
     *
     * Tip: Should not be used in reactive code because it's not a function
     * Tip: Need to be called from UIThread - because it can change
     *
     * @param position of item on the list
     * @return item at position
     * @throws IndexOutOfBoundsException if the position is out of range
     * (<tt>position &lt; 0 || index &gt;= getItemCount()</tt>)
     */
    fun getItemAtPosition(position: Int): BaseAdapterItem {
        return items[position]
    }

    override fun onFailedToRecycleView(holder: ViewHolderManager.BaseViewHolder<BaseAdapterItem>): Boolean {
        return holder.onFailedToRecycleView()
    }

    override fun onViewAttachedToWindow(holder: ViewHolderManager.BaseViewHolder<BaseAdapterItem>) {
        super.onViewAttachedToWindow(holder)
        holder.onViewAttachedToWindow()
    }

    override fun onViewDetachedFromWindow(holder: ViewHolderManager.BaseViewHolder<BaseAdapterItem>) {
        super.onViewDetachedFromWindow(holder)
        holder.onViewDetachedFromWindow()
    }

    override fun onViewRecycled(holder: ViewHolderManager.BaseViewHolder<BaseAdapterItem>) {
        holder.onViewRecycled()
        super.onViewRecycled(holder)
    }
}