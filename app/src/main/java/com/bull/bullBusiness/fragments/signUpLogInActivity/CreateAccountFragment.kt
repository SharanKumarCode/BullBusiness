package com.bull.bullBusiness.fragments.signUpLogInActivity

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.transition.TransitionInflater
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bull.bullBusiness.MainActivity
import com.bull.bullBusiness.R
import com.bull.bullBusiness.databinding.FragmentCreateAccountBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class CreateAccountFragment : Fragment() {

    private var _binding: FragmentCreateAccountBinding? = null
    private val binding get() = _binding!!
    private var timer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val inflaterTrans = TransitionInflater.from(requireContext())
        enterTransition = inflaterTrans.inflateTransition(R.transition.slide_left_to_right)
        exitTransition = inflaterTrans.inflateTransition(R.transition.slide_right_to_left)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.UserNameTextField.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(p0: Editable?) {
                when {
                    binding.UserNameTextField.text?.length!! < 5 -> {
                        binding.UserNameTextInputLayout.error = "minimum 5 characters required"
                    }
                    binding.UserNameTextField.text?.length!! > 20 -> {
                        binding.UserNameTextInputLayout.error = "restrict name to 20 characters"
                    }
                    else -> {
                        binding.UserNameTextInputLayout.error = null
                    }
                }
            }
        })

        binding.UserNameOkButton.setOnClickListener {
            val textLength = binding.UserNameTextField.text.toString().length
            if (binding.UserNameTextInputLayout.error == null && textLength > 5){
                createAccount()
            } else {
                Toast.makeText(requireContext(),"Enter a Valid User Name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        timer?.cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun createAccount() {
        val db = Firebase.firestore
        val userID = Firebase.auth.currentUser?.uid

        db.collection("Users")
            .document(userID!!)
            .get()
            .addOnSuccessListener {
                if (it.exists()){
                    db.collection("Users")
                        .document(userID)
                        .update("user_name",binding.UserNameTextField.text.toString())
                        .addOnSuccessListener {
                            Log.i(TAG, "User name successfully added")
                            launchMainActivity()
                        }
                        .addOnFailureListener {e->
                            Log.i(TAG, "error in adding userName : ${e.message}")
                        }
                }
            }
            .addOnFailureListener {e->
                Log.i(TAG, "error in getting fireStore data : ${e.message}")
            }

    }

    private fun launchMainActivity(){
        val intent = Intent(requireContext(), MainActivity::class.java)
        startActivity(intent)
    }

    companion object {
        private const val TAG = "CreateAccountFragment"
    }

}