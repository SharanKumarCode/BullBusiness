package com.bull.bullBusiness.saloonList

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.transition.TransitionInflater
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bull.bullBusiness.R
import com.bull.bullBusiness.SingletonInstances
import com.bull.bullBusiness.databinding.FragmentSaloonPhotosItemBinding
import com.bull.bullBusiness.genericClasses.GlideApp
import com.bullSaloon.bull.viewModel.MainActivityViewModel
import com.bumptech.glide.request.target.Target
import com.google.firebase.firestore.FieldValue
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class SaloonPhotosItemFragment : Fragment() {

    private var _binding: FragmentSaloonPhotosItemBinding? = null
    private val binding get() = _binding!!

    private val storageRef = SingletonInstances.getStorageReference()
    private val db = SingletonInstances.getFireStoreInstance()

    private var saloonID = ""
    private var photoID = ""
    private var imageRef = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val inflaterTrans = TransitionInflater.from(requireContext())
        enterTransition = inflaterTrans.inflateTransition(R.transition.slide_right_to_left)
        exitTransition = inflaterTrans.inflateTransition(R.transition.slide_left_to_right)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSaloonPhotosItemBinding.inflate(inflater,container,false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dataViewModel = ViewModelProvider(requireActivity()).get(MainActivityViewModel::class.java)
        dataViewModel.getSaloonPhotoData().observe(viewLifecycleOwner, { data ->

//            set date
            val date = data.timestamp.substring(0,10)
            val dateFormatted = LocalDate.parse(date, DateTimeFormatter.ISO_DATE)
            val month = dateFormatted.month.toString()
                .lowercase()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            binding.saloonPhotoItemDate.text = resources.getString(R.string.textBullMagicImageDate,dateFormatted.dayOfMonth,month,dateFormatted.year)

            saloonID = data.saloonID
            photoID = data.photoID
            imageRef = data.imageRef

//            set image
            setImageFromFirebase(requireContext(), binding, data.imageRef)

//            delete image
            binding.deleteImageView.setOnClickListener {
                deleteImageFromFirebaseCloud(data.imageRef, data.photoID)
            }
        })

        binding.setSaloonDpButton.setOnClickListener {
            db.collection("Saloons")
                .document(saloonID)
                .update("display_pic_image_ref", imageRef)
                .addOnSuccessListener {
                    Log.i(TAG, "Photo set as display picture")
                    Toast.makeText(requireActivity(), "Image set as Display Picture.\n\n Refresh app to see changes", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e->
                    Log.i(TAG, "Error to set Photo as display picture : ${e.message}")
                    Toast.makeText(requireActivity(), "Something is wrong.\n\n Please try again later.", Toast.LENGTH_SHORT).show()
                }
        }

        binding.saloonPhotoBackButtonImageView.setOnClickListener {
            this.parentFragmentManager.findFragmentById(R.id.fragmentSaloonPhotosContainerView)
                ?.findNavController()
                ?.navigate(R.id.action_saloonPhotosItemFragment_to_saloonPhotosFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.root.requestLayout()
    }

    private fun setImageFromFirebase(context: Context, binding: FragmentSaloonPhotosItemBinding, imageUrl: String){

        val imageRef = storageRef.storage.getReferenceFromUrl(imageUrl)
        val width = Resources.getSystem().displayMetrics.widthPixels

        GlideApp.with(context)
            .load(imageRef)
            .override(width, Target.SIZE_ORIGINAL)
            .placeholder(R.drawable.ic_bull)
            .into(binding.saloonPhotoItemImageView)
    }

    private fun deleteImageFromFirebaseCloud(imageUrl: String, photoID: String){

        val imageRef = storageRef.storage.reference.child(imageUrl.replace("gs://bull-saloon.appspot.com",""))

        imageRef.delete()
            .addOnSuccessListener {
                Log.i(TAG,"pic is deleted")
                Toast.makeText(context,"Picture is deleted", Toast.LENGTH_SHORT).show()
                deleteImageFromFirestore(imageUrl, photoID)
            }
            .addOnFailureListener {
                Log.i(TAG,"Error : ${it.message}")
                Toast.makeText(context,"Error occurred. Please try again after sometime or check your internet connection",
                    Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteImageFromFirestore(imageUrl: String, photoID: String){

        db.collection("Saloons")
            .document(saloonID)
            .collection("photos")
            .document(photoID)
            .delete()
            .addOnSuccessListener {
                Log.i(TAG,"pic deleted from fireStore")
                this.parentFragmentManager.popBackStack()
            }
            .addOnFailureListener {
                Log.i(TAG,"error on getting data from fireStore: ${it.message}")
                Toast.makeText(requireActivity(), "Error in deleting image. \n\n Please try again after some time.", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        private const val TAG = "TAGSaloonPhotosItemFragment"
    }
}