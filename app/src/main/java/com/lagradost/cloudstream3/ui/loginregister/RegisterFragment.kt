package com.lagradost.cloudstream3.ui.loginregister

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.RegisterFragmentBinding
import com.lagradost.cloudstream3.utils.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class RegisterFragment : Fragment() {
    private lateinit var binding: RegisterFragmentBinding

    // Manually provide dependencies here
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = Firebase.firestore
    private val viewModel = RegisterViewModel(firebaseAuth, firestore)


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = RegisterFragmentBinding.bind(inflater.inflate(R.layout.register_fragment, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        register()
        observeRegister()
        observeValidation()

        binding.login.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun register(){
        binding.apply {
            signupBtn.setOnClickListener {
                val user = UserSign(
                    edFirstName.text.toString().trim(),
                    edLastName.text.toString().trim(),
                    edEmail.text.toString().trim(),
                )
                val password = edPassword.text.toString()
                viewModel.createAccountWithEmailAndPassword(user,password)
            }
        }

    }

    private fun observeRegister() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.register.collect {
                    when (it) {
                        is NetworkResult.Loading -> {
                            binding.progressBar2.visibility = View.VISIBLE
                            binding.signupBtn.visibility = View.GONE
                        }

                        is NetworkResult.Success -> {
                            Log.d("RegisterFragment", it.data.toString())
                            binding.progressBar2.visibility = View.GONE
                            binding.apply {
                                edFirstName.setText("")
                                edLastName.setText("")
                                edEmail.setText("")
                                edPassword.setText("")
                            }
                            requireActivity().onBackPressed()
                            // Navigate to login screen
                            Toast.makeText(
                                requireContext(),
                                "Register success",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        is NetworkResult.Error -> {
                            Log.e("test", it.message.toString())
                            binding.progressBar2.visibility = View.GONE
                            binding.signupBtn.visibility = View.VISIBLE
                            Toast.makeText(
                                requireContext(),
                                it.message.toString(),
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    private fun observeValidation() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.validation.collect { validation ->
                    if (validation.email is RegisterValidation.Failed) {
                        withContext(Dispatchers.Main) {
                            binding.edEmail.apply {
                                requestFocus()
                                error = validation.email.message
                            }
                        }
                    }
                    if (validation.password is RegisterValidation.Failed) {
                        withContext(Dispatchers.Main) {
                            binding.edPassword.apply {
                                requestFocus()
                                error = validation.password.message
                            }
                        }
                    }
                }
            }
        }
    }

}