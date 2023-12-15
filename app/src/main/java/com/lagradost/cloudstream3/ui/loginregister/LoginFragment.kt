package com.lagradost.cloudstream3.ui.loginregister

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.lagradost.cloudstream3.LoginRegisterActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.FragmentLoginBinding
import com.lagradost.cloudstream3.ui.account.AccountSelectActivity
import com.lagradost.cloudstream3.ui.loginregister.LoginViewModel.Companion.MAIN_ACTIVITY
import com.lagradost.cloudstream3.utils.NetworkResult
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class LoginFragment : Fragment() {
    private lateinit var binding: FragmentLoginBinding

    // Manually provide dependencies here
    private val firebaseAuth = FirebaseAuth.getInstance()

    // Create LoginViewModel manually with provided dependencies
    private val viewModel = LoginViewModel(firebaseAuth)

    private var resetPasswordDialog: AlertDialog? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            FragmentLoginBinding.bind(inflater.inflate(R.layout.fragment_login, container, false))
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        login()
        observeLogin()
        resetPassword()
        observeResetPassword()
        binding.signup.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        lifecycleScope.launchWhenStarted {
            viewModel.navigateState.collect{
                when(it){
                    MAIN_ACTIVITY -> {
                        Intent(requireActivity(), AccountSelectActivity::class.java).also { intent ->
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        }
                    }
                    else -> {Unit}
                }
            }
        }
    }


    private fun login() {
        binding.apply {
            loginBtn.setOnClickListener {
                val email = edEmail.text.toString().trim()
                val password = edPassword.text.toString()
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    viewModel.login(email, password)
                } else {
                    Snackbar.make(
                        it,
                        "Please enter email and password",
                        Snackbar.LENGTH_SHORT
                    ).show()

                }
            }
        }
    }

    private fun observeLogin() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.login.collectLatest {
                    when (it) {
                        is NetworkResult.Loading -> {
                            binding.progressBar2.visibility = View.VISIBLE
                            binding.loginBtn.visibility = View.GONE
                        }

                        is NetworkResult.Success -> {
                            binding.progressBar2.visibility = View.GONE
                            binding.loginBtn.visibility = View.VISIBLE
                            getUserStatusFromDatabase()
                        }

                        is NetworkResult.Error -> {
                            binding.progressBar2.visibility = View.GONE
                            binding.loginBtn.visibility = View.VISIBLE
                            Snackbar.make(
                                binding.root,
                                it.message.toString(),
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    private fun getUserStatusFromDatabase() {
        val auth = FirebaseAuth.getInstance()
        val firestore = Firebase.firestore

        // Check if the user is authenticated
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            // Fetch user status from Firestore
            firestore.collection(RegisterViewModel.USER_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val userStatus = documentSnapshot.getString("userStatus")
                        if (userStatus == "banned") {
                            val dialogBuilder = AlertDialog.Builder(requireContext())
                            val alert = dialogBuilder.create()

                            // Set the message and positive button properties on 'alert'
                            alert.setMessage("Your Subscriptions have expired. Renew your subscription to continue using the app")
                            alert.setCancelable(false)
                            alert.setButton(AlertDialog.BUTTON_POSITIVE, "Ok") { _, _ ->
                                // Perform any action if needed when the user clicks "Ok"
                                alert.dismiss()
                            }

                            alert.setTitle("Session Expired")
                            alert.show()
                        } else {
                            Intent(requireActivity(), AccountSelectActivity::class.java).also { intent ->
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                            }
                        }
                    }
                }
                .addOnFailureListener { e -> }
        } else {
            // User is not authenticated
            // Do nothing
        }
    }

    private fun resetPassword() {
        binding.apply {
            forgotPassword.setOnClickListener {
                val dialogView =
                    LayoutInflater.from(requireContext()).inflate(R.layout.reset_password_dialog, null)
                val resetPasswordDialog =
                    AlertDialog.Builder(requireContext(), R.style.AlertDialogCustom) // Use the custom style here
                        .setView(dialogView)
                        .create()
                val btnCancel = dialogView.findViewById<Button>(R.id.cancel_btn)
                val btnConfirm = dialogView.findViewById<Button>(R.id.apply_btn)

                btnCancel.setOnClickListener {
                    resetPasswordDialog.dismiss()
                }

                btnConfirm.setOnClickListener {
                    val email = dialogView.findViewById<EditText>(R.id.edrestEmail).text.toString()
                    if (email.isNotEmpty()) {
                        viewModel.resetPassword(email)
                        resetPasswordDialog.dismiss()
                    } else {
                        Snackbar.make(
                            it,
                            "Please enter email",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
                resetPasswordDialog.show()
            }
        }
    }

    private fun observeResetPassword() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.resetPassword.collectLatest {
                    when (it) {
                        is NetworkResult.Loading -> {
                            binding.progressBar2.visibility = View.VISIBLE
                        }

                        is NetworkResult.Success -> {
                            binding.progressBar2.visibility = View.GONE
                            dismissResetPasswordDialog()
                            Snackbar.make(
                                binding.root,
                                "Reset password email sent",
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }

                        is NetworkResult.Error -> {
                            binding.progressBar2.visibility = View.GONE
                            binding.loginBtn.visibility = View.VISIBLE
                            Snackbar.make(
                                binding.root,
                                it.message.toString(),
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    private fun dismissResetPasswordDialog() {
        // Dismiss the reset password dialog here
        if (resetPasswordDialog != null && resetPasswordDialog!!.isShowing) {
            resetPasswordDialog!!.dismiss()
        }
    }

}