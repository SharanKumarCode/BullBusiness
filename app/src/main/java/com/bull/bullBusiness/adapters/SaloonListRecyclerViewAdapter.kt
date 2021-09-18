package com.bull.bullBusiness.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bull.bullBusiness.R
import com.bull.bullBusiness.SingletonInstances
import com.bull.bullBusiness.databinding.ViewHolderSaloonItemBinding
import com.bull.bullBusiness.genericClasses.GlideApp
import com.bull.bullBusiness.genericClasses.dataClasses.SaloonDataClass
import com.bullSaloon.bull.viewModel.MainActivityViewModel
import java.lang.Error
import kotlin.math.roundToLong

class SaloonListRecyclerViewAdapter(lists: MutableList<SaloonDataClass>, dataViewModel: MainActivityViewModel, _fragment: Fragment): RecyclerView.Adapter<SaloonListRecyclerViewAdapter.SaloonRecyclerViewHolder>() {

    private val saloonList = lists
    private val dataModel = dataViewModel
    private val fragment = _fragment
    private val storageRef = SingletonInstances.getStorageReference()

    inner class SaloonRecyclerViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

        val binding: ViewHolderSaloonItemBinding = ViewHolderSaloonItemBinding.bind(itemView)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SaloonRecyclerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_holder_saloon_item,parent,false)
        return SaloonRecyclerViewHolder(view)
    }

    override fun onBindViewHolder(holder: SaloonRecyclerViewHolder, position: Int) {
        val holderBinding = holder.binding

        val context: Context = holder.itemView.context
        setSaloonDisplayPic(holderBinding, saloonList[position].imageSource!!)

        holderBinding.saloonNameTextView.text = saloonList[position].saloonName
        holderBinding.areaNameTextView.text = saloonList[position].areaName

        if (saloonList[position].openStatus == true){
            holderBinding.openStatusTextView.text = holderBinding.root.context.resources.getString(R.string.placeHolderOpenStatus)
            holderBinding.openStatusTextView.setTextColor(context.getColor(R.color.openStatusColor))
        } else {
            holderBinding.openStatusTextView.text = holderBinding.root.context.resources.getString(R.string.placeHolderClosedStatus)
            holderBinding.openStatusTextView.setTextColor(context.getColor(R.color.closeStatusColor))
        }

        when(saloonList[position].rating){
            1 -> setRatingPic(holderBinding, R.drawable.ic_rating_one_stars)
            2 -> setRatingPic(holderBinding, R.drawable.ic_rating_two_stars)
            3 -> setRatingPic(holderBinding, R.drawable.ic_rating_three_stars)
            4 -> setRatingPic(holderBinding, R.drawable.ic_rating_four_stars)
            5 -> setRatingPic(holderBinding, R.drawable.ic_rating_five_stars)
            else -> setRatingPic(holderBinding, R.drawable.ic_rating_one_stars)
        }

        holderBinding.saloonHaircutPriceText.text = String.format(holderBinding.root.context.resources.getString(R.string.textHaircutPrice), saloonList[position].haircutPrice)
        holderBinding.saloonShavingPriceText.text = String.format(holderBinding.root.context.resources.getString(R.string.textHaircutPrice), saloonList[position].shavingPrice)

        holder.itemView.setOnClickListener {

            try {
                launchSaloonItemFragment(fragment, dataModel, saloonList[position])
            }catch (e: Error){
                Log.i(TAG, "error occurred: $e")
            }
        }
    }

    override fun getItemCount(): Int {
        return saloonList.size
    }

    private fun launchSaloonItemFragment(fragment: Fragment, dataModel: MainActivityViewModel, saloonList: SaloonDataClass){

        dataModel.putSaloonData(saloonList.saloonID)

        val saloonNavHostFragment =
            fragment.parentFragment?.childFragmentManager?.findFragmentById(R.id.fragmentSaloonContainerView)
        val navController = saloonNavHostFragment?.findNavController()
        navController?.navigate(R.id.action_saloonListFragment_to_saloonItemFragment)
    }

    private fun setSaloonDisplayPic(binding: ViewHolderSaloonItemBinding, profilePicRef: String){

        val imageRef = storageRef.storage.reference.child(profilePicRef)

        GlideApp.with(binding.root.context)
            .asBitmap()
            .load(imageRef)
            .centerCrop()
            .placeholder(R.drawable.ic_bull)
            .into(binding.saloonDisplayImageView)
    }

    private fun setRatingPic(binding: ViewHolderSaloonItemBinding, drawableResource: Int){

        GlideApp.with(binding.root.context)
            .asBitmap()
            .load(drawableResource)
            .placeholder(R.drawable.ic_rating_one_stars)
            .into(binding.saloonRatingImage)
    }

    companion object {
        private const val TAG = "TAGSaloonListRecyclerViewAdapter"
    }
}