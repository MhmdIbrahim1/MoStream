package com.lagradost.cloudstream3.ui.settings

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.core.view.isVisible
import com.lagradost.cloudstream3.ui.settings.Globals.EMULATOR
import com.lagradost.cloudstream3.ui.settings.Globals.TV
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.utils.UIHelper
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.lagradost.cloudstream3.LoginRegisterActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.MainSettingsBinding
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.ui.home.HomeFragment
import com.lagradost.cloudstream3.ui.loginregister.RegisterViewModel.Companion.USER_COLLECTION
import com.lagradost.cloudstream3.ui.loginregister.UserSign
import com.lagradost.cloudstream3.utils.UIHelper.fixPaddingStatusbar
import com.lagradost.cloudstream3.utils.UIHelper.navigate
import com.lagradost.cloudstream3.utils.UIHelper.setImage
import com.lagradost.cloudstream3.utils.UIHelper.toPx
import java.io.File

class SettingsFragment : Fragment(), UserFetchCallback {
    companion object {

        fun PreferenceFragmentCompat?.getPref(id: Int): Preference? {
            if (this == null) return null

            return try {
                findPreference(getString(id))
            } catch (e: Exception) {
                logError(e)
                null
            }
        }

        /**
         * On TV you cannot properly scroll to the bottom of settings, this fixes that.
         * */
        fun PreferenceFragmentCompat.setPaddingBottom() {
            if (isLayout(TV or EMULATOR)) {
                listView?.setPadding(0, 0, 0, 100.toPx)
            }
        }

        fun PreferenceFragmentCompat.setToolBarScrollFlags() {
            if (isLayout(TV or EMULATOR)) {
                val settingsAppbar = view?.findViewById<MaterialToolbar>(R.id.settings_toolbar)

                settingsAppbar?.updateLayoutParams<AppBarLayout.LayoutParams> {
                    scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL
                }
            }
        }

        fun Fragment?.setToolBarScrollFlags() {
            if(isLayout(TV or EMULATOR)) {
                val settingsAppbar =
                    this?.view?.findViewById<MaterialToolbar>(R.id.settings_toolbar)

                settingsAppbar?.updateLayoutParams<AppBarLayout.LayoutParams> {
                    scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL
                }
            }
        }

        fun Fragment?.setUpToolbar(title: String) {
            if (this == null) return
            val settingsToolbar =
                view?.findViewById<MaterialToolbar>(R.id.settings_toolbar) ?: return

            settingsToolbar.apply {
                setTitle(title)
                setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
                setNavigationOnClickListener {
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }
            }
            fixPaddingStatusbar(settingsToolbar)
        }

        fun Fragment?.setUpToolbar(@StringRes title: Int) {
            if (this == null) return
            val settingsToolbar =
                view?.findViewById<MaterialToolbar>(R.id.settings_toolbar) ?: return

            settingsToolbar.apply {
                setTitle(title)
                setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
                children.firstOrNull { it is ImageView }?.tag = getString(R.string.tv_no_focus_tag)
                setNavigationOnClickListener {
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }
            }
            fixPaddingStatusbar(settingsToolbar)
        }

        fun getFolderSize(dir: File): Long {
            var size: Long = 0
            dir.listFiles()?.let {
                for (file in it) {
                    size += if (file.isFile) {
                        // System.out.println(file.getName() + " " + file.length());
                        file.length()
                    } else getFolderSize(file)
                }
            }

            return size
        }




        private fun Context.isAutoTv(): Boolean {
            val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager?
            // AFT = Fire TV
            val model = Build.MODEL.lowercase()
            return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION || Build.MODEL.contains(
                "AFT"
            ) || model.contains("firestick") || model.contains("fire tv") || model.contains("chromecast")
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    var binding: MainSettingsBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val localBinding = MainSettingsBinding.inflate(inflater, container, false)
        binding = localBinding
        return localBinding.root
        //return inflater.inflate(R.layout.main_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        fun navigate(id: Int) {
            activity?.navigate(id, Bundle())
        }

        // used to debug leaks showToast(activity,"${VideoDownloadManager.downloadStatusEvent.size} : ${VideoDownloadManager.downloadProgressEvent.size}")


//        for (syncApi in accountManagers) {
//            val login = syncApi.loginInfo()
//            val pic = login?.profilePicture ?: continue
//            if (binding?.settingsProfilePic?.setImage(
//                    pic,
//                    errorImageDrawable = HomeFragment.errorProfilePic
//                ) == true
//            ) {
//                binding?.settingsProfileText?.text = login.name
//                binding?.settingsProfile?.isVisible = true
//                break
//            }
//        }
        // set user image and name from firestore
        getUser(this)


        binding?.apply {
            listOf(
                settingsGeneral to R.id.action_navigation_global_to_navigation_settings_general,
                settingsPlayer to R.id.action_navigation_global_to_navigation_settings_player,
                settingsCredits to R.id.action_navigation_global_to_navigation_settings_account,
                settingsUi to R.id.action_navigation_global_to_navigation_settings_ui,
                settingsProviders to R.id.action_navigation_global_to_navigation_settings_providers,
                settingsUpdates to R.id.action_navigation_global_to_navigation_settings_updates,
                settingsExtensions to R.id.action_navigation_global_to_navigation_settings_extensions,
            ).forEach { (view, navigationId) ->
                view.apply {
                    setOnClickListener {
                        navigate(navigationId)
                    }
                    if (isLayout(TV)) {
                        isFocusable = true
                        isFocusableInTouchMode = true
                    }
                }
            }

            binding?.apply {
                settingsLogout.setOnClickListener {
                    logout()
                }
            }

            // Default focus on TV
            if (isLayout(TV)) {
                settingsGeneral.requestFocus()
            }
        }
    }

    private fun logout() {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.reset_password_dialog, null)
        val logoutDialog =
            AlertDialog.Builder(
                requireContext(),
                R.style.AlertDialogCustom
            ) // Use the custom style here
                .setView(dialogView)
                .create()
        dialogView.findViewById<View>(R.id.inputLayoutEdEmail).visibility = View.GONE
        dialogView.findViewById<View>(R.id.tvResetPasswordText).visibility = View.INVISIBLE
        val btnCancel = dialogView.findViewById<Button>(R.id.cancel_btn)
        val btnConfirm = dialogView.findViewById<Button>(R.id.apply_btn)
        val tvResetPasswordTitle = dialogView.findViewById<TextView>(R.id.tvResetPassword)
        tvResetPasswordTitle.text = getString(R.string.logout)
        btnConfirm.text = getString(R.string.logout)
        // make margin top to 8dp for buttons
        btnCancel.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = 8.toPx
        }
        btnConfirm.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = 8.toPx
        }

        btnCancel.setOnClickListener {
            logoutDialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Intent(
                requireActivity(),
                LoginRegisterActivity::class.java
            ).also { intent ->
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            requireActivity().finish()
        }

        logoutDialog.show()
    }

    // fun to get the user information from firestore first name, last name, email
    private fun getUser(callback: UserFetchCallback) {
        callback.onLoading()
        val auth = FirebaseAuth.getInstance()
        val firestore = Firebase.firestore
        firestore.collection(USER_COLLECTION).document(auth.uid!!)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    callback.onError(error.message.toString())
                } else {
                    val user = value?.toObject(UserSign::class.java)
                    user?.let {
                        callback.onSuccess(user)
                    }
                }
            }
    }

    override fun onLoading() {
        //nothing
    }

    override fun onSuccess(user: UserSign) {
        binding?.settingsProfileText?.text = "${user.firstName} ${user.lastName}"
        binding?.settingsProfile?.isVisible = true

        // Initial random image
        val initialProfilePic = HomeFragment.errorProfilePics.random()
        binding?.settingsProfilePic?.setImage(
            initialProfilePic,
            errorImageDrawable = HomeFragment.errorProfilePic
        )

        // Roll the image forwards once of the user clicks on it
        binding?.settingsProfilePic?.setOnClickListener {
            // Show a different random image from errorProfilePics list
            val newProfilePic = HomeFragment.errorProfilePics.random()
            binding?.settingsProfilePic?.setImage(
                newProfilePic,
                errorImageDrawable = HomeFragment.errorProfilePic
            )
        }

    }

    override fun onError(errorMessage: String) {
        //nothing
    }

}
