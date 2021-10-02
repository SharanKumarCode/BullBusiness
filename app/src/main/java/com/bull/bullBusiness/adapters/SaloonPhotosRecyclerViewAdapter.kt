package com.bull.bullBusiness.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bull.bullBusiness.R
import com.bull.bullBusiness.SingletonInstances
import com.bull.bullBusiness.databinding.ViewHolderSaloonPhotosBinding
import com.bull.bullBusiness.genericClasses.GlideApp
import com.bull.bullBusiness.genericClasses.dataClasses.SaloonPhotosData
import com.bull.bullBusiness.saloonList.SaloonPhotosFragment
import com.bullSaloon.bull.viewModel.MainActivityViewModel

class SaloonPhotosRecyclerViewAdapter(lists: MutableList<SaloonPhotosData>, dataViewModel: MainActivityViewModel, childFragmentManager: SaloonPhotosFragment): RecyclerView.Adapter<SaloonPhotosRecyclerViewAdapter.SaloonPhotosViewHolder>() {

    private val photosList = lists
    private val storageRef = SingletonInstances.getStorageReference()
    private val dataModel = dataViewModel
    private val childFragManager = childFragmentManager

    inner class SaloonPhotosViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){

        val binding: ViewHolderSaloonPhotosBinding = ViewHolderSaloonPhotosBinding.bind(itemView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SaloonPhotosViewHolder {

        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_holder_saloon_photos, parent, false)
        return SaloonPhotosViewHolder(view)
    }

    override fun onBindViewHolder(holder: SaloonPhotosViewHolder, position: Int) {
        val holderBinding = holder.binding

        setImageFromFirebase(holderBinding.root.context, holderBinding, photosList[position].imageRef)

        holderBinding.saloonPhotosItemImageView.setOnClickListener {

            dataModel.assignSaloonPhotoData(photosList[position])
            startYourProfilePhotoItemFragment(childFragManager)
        }

    }

    override fun getItemCount(): Int {
        return photosList.size
    }

    private fun setImageFromFirebase(context: Context, binding: ViewHolderSaloonPhotosBinding, imageUrl: String){

        val imageRef = storageRef.storage.getReferenceFromUrl(imageUrl)

        Log.i(TAG, "set image from firebase : $imageUrl")

        GlideApp.with(context)
            .load(imageRef)
            .centerCrop()
            .placeholder(R.drawable.ic_bull)
            .into(binding.saloonPhotosItemImageView)
    }

    private fun startYourProfilePhotoItemFragment(childFragManager: SaloonPhotosFragment){

        val yourProfilePhotoItemFragmentHost = childFragManager.parentFragmentManager.findFragmentById(R.id.fragmentSaloonPhotosContainerView)
        val navController = yourProfilePhotoItemFragmentHost?.findNavController()

        navController?.navigate(R.id.action_saloonPhotosFragment_to_saloonPhotosItemFragment)
    }

    companion object {
        private const val TAG = "TAGSaloonPhotosRecyclerViewAdapter"
    }
}