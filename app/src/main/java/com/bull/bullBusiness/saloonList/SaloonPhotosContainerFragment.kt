package com.bull.bullBusiness.saloonList

import android.os.Bundle
import android.transition.TransitionInflater
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bull.bullBusiness.R
import com.bull.bullBusiness.databinding.FragmentSaloonPhotosBinding
import com.bull.bullBusiness.databinding.FragmentSaloonPhotosContainerBinding

class SaloonPhotosContainerFragment : Fragment() {

    private var _binding: FragmentSaloonPhotosContainerBinding? = null
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
        _binding = FragmentSaloonPhotosContainerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()

        binding.root.requestLayout()

    }
}