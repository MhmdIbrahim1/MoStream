package com.lagradost.cloudstream3.ui.result

import android.content.Context
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isGone
import androidx.core.view.isVisible
import android.graphics.Bitmap
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.utils.AppUtils.html
import com.lagradost.cloudstream3.utils.UIHelper.setImage

sealed class UiText {
    companion object {
        const val TAG = "UiText"
    }

    data class DynamicString(val value: String) : UiText() {
        override fun toString(): String = value
        override fun equals(other: Any?): Boolean {
            if (other !is DynamicString) return false
            return this.value == other.value
        }

        override fun hashCode(): Int = value.hashCode()
    }

    class StringResource(
        @StringRes val resId: Int,
        val args: List<Any>
    ) : UiText() {
        override fun toString(): String =
            "resId = $resId\nargs = ${args.toList().map { "(${it::class} = $it)" }}"
        override fun equals(other: Any?): Boolean {
            if (other !is StringResource) return false
            return this.resId == other.resId && this.args == other.args
        }

        override fun hashCode(): Int {
            var result = resId
            result = 31 * result + args.hashCode()
            return result
        }
    }

    fun asStringNull(context: Context?): String? {
        try {
            return asString(context ?: return null)
        } catch (e: Exception) {
            Log.e(TAG, "Got invalid data from $this")
            logError(e)
            return null
        }
    }

    fun asString(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> {
                val str = context.getString(resId)
                if (args.isEmpty()) {
                    str
                } else {
                    str.format(*args.map {
                        when (it) {
                            is UiText -> it.asString(context)
                            else -> it
                        }
                    }.toTypedArray())
                }
            }
        }
    }
}

sealed class UiImage {
    data class Image(
        val url: String,
        val headers: Map<String, String>? = null,
        @DrawableRes val errorDrawable: Int? = null
    ) : UiImage()

    data class Drawable(@DrawableRes val resId: Int) : UiImage()
    data class Bitmap(val bitmap: android.graphics.Bitmap) : UiImage()

}

fun ImageView?.setImage(value: UiImage?, fadeIn: Boolean = true) {
    when (value) {
        is UiImage.Image -> setImageImage(value, fadeIn)
        is UiImage.Drawable -> setImageDrawable(value)
        is UiImage.Bitmap -> setImageBitmap(value)
        null -> {
            this?.isVisible = false
        }
    }
}



fun ImageView?.setImageBitmap(value: UiImage.Bitmap) {
    if (this == null) return
    this.isVisible = true
    this.setImageBitmap(value.bitmap)
}
fun ImageView?.setImageImage(value: UiImage.Image, fadeIn: Boolean = true) {
    if (this == null) return
    this.isVisible = setImage(value.url, value.headers, value.errorDrawable, fadeIn)
}

fun ImageView?.setImageDrawable(value: UiImage.Drawable) {
    if (this == null) return
    this.isVisible = true
    this.setImage(UiImage.Drawable(value.resId))
}

fun img(bitmap: Bitmap): UiImage {
    return UiImage.Bitmap(bitmap)
}

@JvmName("imgNull")
fun img(
    url: String?,
    headers: Map<String, String>? = null,
    @DrawableRes errorDrawable: Int? = null
): UiImage? {
    if (url.isNullOrBlank()) return null
    return UiImage.Image(url, headers, errorDrawable)
}

fun img(
    url: String,
    headers: Map<String, String>? = null,
    @DrawableRes errorDrawable: Int? = null
): UiImage {
    return UiImage.Image(url, headers, errorDrawable)
}

fun img(@DrawableRes drawable: Int): UiImage {
    return UiImage.Drawable(drawable)
}

fun txt(value: String): UiText {
    return UiText.DynamicString(value)
}

@JvmName("txtNull")
fun txt(value: String?): UiText? {
    return UiText.DynamicString(value ?: return null)
}

fun txt(@StringRes resId: Int, vararg args: Any): UiText {
    return UiText.StringResource(resId, args.toList())
}

@JvmName("txtNull")
fun txt(@StringRes resId: Int?, vararg args: Any?): UiText? {
    if (resId == null || args.any { it == null }) {
        return null
    }
    return UiText.StringResource(resId, args.filterNotNull().toList())
}

fun TextView?.setText(text: UiText?) {
    if (this == null) return
    if (text == null) {
        this.isVisible = false
    } else {
        val str = text.asStringNull(context)?.let {
            if (this.maxLines == 1) {
                it.replace("\n", " ")
            } else {
                it
            }
        }

        this.isGone = str.isNullOrBlank()
        this.text = str
    }
}

fun TextView?.setTextHtml(text: UiText?) {
    if (this == null) return
    if (text == null) {
        this.isVisible = false
    } else {
        val str = text.asStringNull(context)
        this.isGone = str.isNullOrBlank()
        this.text = str.html()
    }
}
