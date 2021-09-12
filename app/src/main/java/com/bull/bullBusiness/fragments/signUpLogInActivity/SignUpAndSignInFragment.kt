package com.bull.bullBusiness.fragments.signUpLogInActivity

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bull.bullBusiness.R
import com.bull.bullBusiness.databinding.FragmentSignUpAndSignInBinding


class SignUpAndSignInFragment : Fragment() {

    private var _binding: FragmentSignUpAndSignInBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpAndSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val createAccountFragment = CreateAccountFragment()

        binding.CreateAccountButton.setOnClickListener {
            activity?.supportFragmentManager?.beginTransaction()
                ?.setReorderingAllowed(true)
                ?.replace(R.id.SignInFragmentContainer, createAccountFragment)
                ?.commit()
        }

        binding.SignInButton.setOnClickListener {
            activity?.supportFragmentManager?.beginTransaction()
                ?.setReorderingAllowed(true)
                ?.replace(R.id.SignInFragmentContainer, createAccountFragment)
                ?.commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "TAGSignUpAndSignInFragment"
    }

}