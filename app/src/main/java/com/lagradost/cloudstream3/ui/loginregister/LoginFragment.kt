package com.lagradost.cloudstream3.ui.loginregister

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
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
                            Intent(
                                requireActivity(),
                                AccountSelectActivity::class.java
                            ).also { intent ->
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                            }
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
}