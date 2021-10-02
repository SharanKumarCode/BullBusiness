package com.bull.bullBusiness.saloonList

import android.os.Bundle
import android.transition.TransitionInflater
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bull.bullBusiness.MainActivity
import com.bull.bullBusiness.R
import com.bull.bullBusiness.SingletonInstances
import com.bull.bullBusiness.adapters.SaloonPhotosRecyclerViewAdapter
import com.bull.bullBusiness.databinding.FragmentSaloonPhotosBinding
import com.bull.bullBusiness.genericClasses.dataClasses.SaloonPhotosData
import com.bullSaloon.bull.viewModel.MainActivityViewModel


class SaloonPhotosFragment : Fragment() {

    private var _binding: FragmentSaloonPhotosBinding? = null
    private val binding get() = _binding!!

    private val db = SingletonInstances.getFireStoreInstance()

    private lateinit var dataViewModel: MainActivityViewModel
    private var saloonID = ""

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
        _binding = FragmentSaloonPhotosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dataViewModel = ViewModelProvider(requireActivity()).get(MainActivityViewModel::class.java)

        binding.saloonPhotosRecyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

        dataViewModel.getSaloonData().observe(viewLifecycleOwner , { data ->

            saloonID = data.saloonID!!

            binding.addPhotoButton.setOnClickListener {
                Log.i(TAG, "Button Clicked")
                launchCameraFragment()
            }

            getPhotosFirestoreData()
        })
    }

    override fun onResume() {
        super.onResume()
        binding.root.requestLayout()
    }

    private fun getPhotosFirestoreData(){

        db.collection("Saloons")
            .document(saloonID)
            .collection("photos")
            .addSnapshotListener { snapshot, error ->
                if (error == null){
                    val myPhotosList: MutableList<SaloonPhotosData>  = mutableListOf()

                    if (!snapshot?.isEmpty!!){

                        for (document in snapshot.documents){

                            val data = SaloonPhotosData(
                                saloonID,
                                document.get("photoID").toString(),
                                document.get("image_ref").toString(),
                                document.get("timestamp").toString(),
                                document.get("display_pic").toString().toBoolean()
                            )
                            myPhotosList.add(data)
                        }
                    }

                    myPhotosList.sortBy {
                        it.timestamp
                    }
                    myPhotosList.reverse()
                    binding.saloonPhotosRecyclerView.adapter = SaloonPhotosRecyclerViewAdapter(myPhotosList, dataViewModel, this)
                }
                else {
                    Log.i(TAG, "error occurred: $error")
                }
            }
    }

    private fun launchCameraFragment(){
        (activity as MainActivity).setActionBarBottomBarVisibility("hide")
        dataViewModel.assignCameraClickMode("clicked")
    }

    companion object {
        private const val TAG = "TAGSaloonPhotosFragment"
    }
}