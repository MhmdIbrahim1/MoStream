package com.lagradost.cloudstream3.utils

import android.content.Context
import android.os.Build
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.fragment.app.DialogFragment
import com.google.android.material.imageview.ShapeableImageView
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.home.HomeFragment
import android.os.Bundle as Bundle1

class ImageSelectionDialogFragment(private val onImageSelected: (Int) -> Unit) : DialogFragment() {

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle1?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_select_image, container, false)
        val gridLayout = view.findViewById<GridLayout>(R.id.gridLayout)

        HomeFragment.errorProfilePics.forEach { imageResId ->
            val cardView = CardView(requireContext()).apply {
                layoutParams = ViewGroup.LayoutParams(200, 200)
                radius = 100f
                setCardBackgroundColor(resources.getColor(android.R.color.transparent, null))
                cardElevation = 8f
                preventCornerOverlap = true
                useCompatPadding = true

                val imageView = ShapeableImageView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setImageResource(imageResId)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setPadding(8, 8, 8, 8)
                    setOnClickListener {
                        animateSelection()
                        saveSelectedImage(imageResId)

                        // Delay dismissing the dialog to allow the animation to complete
                        postDelayed({
                            onImageSelected(imageResId)
                            dismiss()
                        }, 200) // Adjust the delay as needed
                    }
                }
                addView(imageView)
            }
            gridLayout.addView(cardView)
        }

        return view
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun ShapeableImageView.animateSelection() {
        val cardView = parent as? CardView
        cardView?.setCardBackgroundColor(resolveColorAttr(requireContext(), R.attr.colorPrimary))
        postDelayed({
            cardView?.setCardBackgroundColor(resources.getColor(android.R.color.transparent, null))
        }, 100)
    }

    private fun resolveColorAttr(context: Context, attr: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    private fun saveSelectedImage(resId: Int) {
        val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt("selected_profile_pic", resId).apply()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}
