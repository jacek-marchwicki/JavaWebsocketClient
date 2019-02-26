package com.appunite.detector

import android.view.View
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.disposables.SerialDisposable

abstract class DefinedViewHolder<T : BaseAdapterItem>(view: View) : ViewHolderManager.BaseViewHolder<T>(view) {
    private val disposable = SerialDisposable()

    open fun bindDisposable(item: T): Disposable = Disposables.empty()
    open fun bindStatic(item: T) = Unit
    final override fun bind(item: T) {
        bindStatic(item)
        disposable.set(bindDisposable(item))
    }
    final override fun onViewRecycled() {
        disposable.set(Disposables.empty())
    }
}