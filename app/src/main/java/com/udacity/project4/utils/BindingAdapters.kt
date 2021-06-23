package com.udacity.project4.utils

import android.view.View
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.slider.Slider
import com.udacity.project4.base.BaseRecyclerViewAdapter
import kotlin.math.truncate


object BindingAdapters {

    /**
     * Use binding adapter to set the recycler view data using livedata object
     */
    @Suppress("UNCHECKED_CAST")
    @BindingAdapter("android:liveData")
    @JvmStatic
    fun <T> setRecyclerViewData(recyclerView: RecyclerView, items: LiveData<List<T>>?) {
        items?.value?.let { itemList ->
            (recyclerView.adapter as? BaseRecyclerViewAdapter<T>)?.apply {
                clear()
                addData(itemList)
            }
        }
    }

    /**
     * Use this binding adapter to show and hide the views using boolean variables
     */
    @BindingAdapter("android:fadeVisible")
    @JvmStatic
    fun setFadeVisible(view: View, visible: Boolean? = true) {
        if (view.tag == null) {
            view.tag = true
            view.visibility = if (visible == true) View.VISIBLE else View.GONE
        } else {
            view.animate().cancel()
            if (visible == true) {
                if (view.visibility == View.GONE)
                    view.fadeIn()
            } else {
                if (view.visibility == View.VISIBLE)
                    view.fadeOut()
            }
        }
    }

    @BindingAdapter("android:rangeValue")
    @JvmStatic
    fun setDoubleValue(slider: Slider, value: Float) {
        slider.value = truncate(value.coerceAtLeast(slider.valueFrom).coerceAtMost(slider.valueTo) / slider.stepSize) * slider.stepSize
    }

    @InverseBindingAdapter(attribute = "android:rangeValue")
    @JvmStatic
    fun getDoubleValue(slider: Slider): Float = slider.value

    @BindingAdapter("android:rangeValueAttrChanged")
    @JvmStatic
    fun setSliderListeners(slider: Slider, attrChange: InverseBindingListener?) {
        attrChange?.let {
            slider.addOnChangeListener { _, _, _ -> attrChange.onChange() }
        }
    }
}