package com.bull.bullBusiness.saloonList

import android.os.Bundle
import android.transition.TransitionInflater
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.bull.bullBusiness.R
import com.bull.bullBusiness.databinding.FragmentSaloonContainerBinding
import com.bull.bullBusiness.databinding.FragmentSaloonListBinding


class SaloonListFragment : Fragment() {

    private var _binding: FragmentSaloonListBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val inflaterTrans = TransitionInflater.from(requireContext())
        enterTransition = inflaterTrans.inflateTransition(R.transition.slide_right_to_left)
        exitTransition = inflaterTrans.inflateTransition(R.transition.fade)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSaloonListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val saloonNavHostFragment = this.parentFragmentManager.findFragmentById(R.id.fragmentSaloonContainerView)
        val navController = saloonNavHostFragment?.findNavController()

        binding.addSaloonButton.setOnClickListener {
            navController?.navigate(R.id.action_saloonListFragment_to_addSaloonFragment)
        }

    }

    companion object {
        private const val TAG = "TAGSaloonListFragment"
    }
}