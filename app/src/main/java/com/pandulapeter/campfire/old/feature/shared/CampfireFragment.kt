package com.pandulapeter.campfire.old.feature.shared

import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding
import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.annotation.StringRes
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pandulapeter.campfire.BR
import com.pandulapeter.campfire.R
import com.pandulapeter.campfire.old.networking.AnalyticsManager
import com.pandulapeter.campfire.util.color
import org.koin.android.ext.android.inject

/**
 * Base class for all Fragments in the app. Handles layout inflation, setting up the view model,
 * and displaying snackbars.
 *
 * Controlled by subclasses of [CampfireViewModel].
 */
abstract class CampfireFragment<B : ViewDataBinding, out VM : CampfireViewModel>(@LayoutRes private val layoutResourceId: Int) : Fragment() {
    protected val analyticsManager by inject<AnalyticsManager>()
    protected abstract val viewModel: VM
    protected lateinit var binding: B
    private var snackbar: Snackbar? = null
    private var hintSnackbar: Snackbar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, layoutResourceId, container, false)
        binding.setVariable(BR.viewModel, viewModel)
        return binding.root
    }

    override fun onPause() {
        super.onPause()
        dismissSnackbar()
    }

    open fun onBackPressed() = false

    protected fun View.showFirstTimeUserExperienceSnackbar(@StringRes message: Int, onGotItClicked: (View) -> Unit) {
        dismissSnackbar()
        hintSnackbar = makeSnackbar(context.getString(message), Snackbar.LENGTH_INDEFINITE).apply {
            setAction(R.string.got_it, onGotItClicked)
        }
        hintSnackbar?.show()
    }

    protected fun View.showSnackbar(
            @StringRes message: Int, @StringRes actionButton: Int? = null, action: (View) -> Unit = {},
            dismissListener: (() -> Unit)? = null
    ) = showSnackbar(context.getString(message), actionButton, action, dismissListener)

    protected fun View.showSnackbar(message: String, @StringRes actionButton: Int? = null, action: (View) -> Unit = {}, dismissListener: (() -> Unit)? = null) {
        dismissSnackbar()
        snackbar = makeSnackbar(message, Snackbar.LENGTH_LONG, dismissListener)
        actionButton?.let {
            snackbar?.setAction(it, action)
        }
        snackbar?.show()
    }

    protected fun dismissHintSnackbar() {
        hintSnackbar?.dismiss()
        hintSnackbar = null
    }

    private fun dismissSnackbar() {
        snackbar?.dismiss()
        snackbar = null
    }

    private fun View.makeSnackbar(message: String, duration: Int, dismissListener: (() -> Unit)? = null) = Snackbar.make(this, message, duration).apply {
        view.setBackgroundColor(context.color(R.color.primary))
        dismissListener?.let {
            addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) = it()
            })
        }
    }
}