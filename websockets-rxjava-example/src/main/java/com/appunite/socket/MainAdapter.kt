package com.appunite.socket

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.google.common.collect.ImmutableList

import java.text.DateFormat
import java.util.Date


import androidx.recyclerview.widget.RecyclerView
import com.appunite.detector.*
import io.reactivex.functions.Consumer

import com.google.common.base.Preconditions.checkNotNull

open class RxUniversalAdapter(managers: List<ViewHolderManager>) : UniversalAdapter(managers), Consumer<List<BaseAdapterItem>>{

    override fun accept(t: List<BaseAdapterItem>) {
        call(t)
    }

}

class MainViewHolder internal constructor() : ViewHolderManager {

    override fun matches(baseAdapterItem: BaseAdapterItem): Boolean {
        return baseAdapterItem is MainPresenter.AdapterItem
    }

    override fun createViewHolder(parent: ViewGroup, inflater: LayoutInflater): DefinedViewHolder<*> {
        return ViewHolder(inflater.inflate(R.layout.main_adapter_item, parent, false))
    }

    private inner class ViewHolder constructor(itemView: View) : DefinedViewHolder<MainPresenter.AdapterItem>(itemView) {

    private val timeInstance = DateFormat.getTimeInstance(DateFormat.MEDIUM)

        private val text: TextView = checkNotNull(itemView.findViewById<View>(R.id.main_adapter_item_text) as TextView)
        private val date: TextView = checkNotNull(itemView.findViewById<View>(R.id.main_adapter_item_date) as TextView)
        private val details: TextView = checkNotNull(itemView.findViewById<View>(R.id.main_adapter_item_details) as TextView)

        override fun bindStatic(item: MainPresenter.AdapterItem) {
            text.text = item.text
            date.text = timeInstance.format(Date(item.publishTime))
            details.text = item.details
            details.visibility = if (item.details == null) View.GONE else View.VISIBLE
        }

    }

}
