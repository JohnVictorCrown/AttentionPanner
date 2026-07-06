package com.jv.attentionpanner

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class LifecycleComposeLayout(context: Context) : FrameLayout(context), LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private var composeView: ComposeView? = null

    init {
        this.setViewTreeLifecycleOwner(this)
        this.setViewTreeSavedStateRegistryOwner(this)
    }

    fun setContent(content: @Composable () -> Unit) {
        if (composeView == null) {
            composeView = ComposeView(context).apply { setContent(content) }
            addView(composeView)
        } else { composeView?.setContent(content) }
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_CREATE) savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
}
