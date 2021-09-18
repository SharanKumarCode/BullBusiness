package com.bull.bullBusiness.saloonList

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.TransitionInflater
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.os.HandlerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.bull.bullBusiness.R
import com.bull.bullBusiness.SingletonInstances
import com.bull.bullBusiness.adapters.SaloonItemViewPagerAdapter
import com.bull.bullBusiness.databinding.FragmentSaloonItemBinding
import com.bull.bullBusiness.genericClasses.GlideApp
import com.bullSaloon.bull.viewModel.MainActivityViewModel
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.roundToInt

class SaloonItemFragment : Fragment() {

    private var _binding: FragmentSaloonItemBinding? = null
    private val binding get() = _binding!!

    private lateinit var saloonID: String
    private lateinit var dataViewModel: MainActivityViewModel

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var storageRef = SingletonInstances.getStorageReference()
    private var rating = 1

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
        _binding = FragmentSaloonItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dataViewModel = ViewModelProvider(requireActivity()).get(MainActivityViewModel::class.java)
        db = SingletonInstances.getFireStoreInstance()
        auth = SingletonInstances.getAuthInstance()

        var contact = ""
        var shopAddress = ""

        val saloonNavHostFragment = this.parentFragment?.childFragmentManager?.findFragmentById(R.id.fragmentSaloonContainerView)
        val navController = saloonNavHostFragment?.findNavController()
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                navController?.navigate(R.id.action_saloonItemFragment_to_saloonListFragment)
            }
        })

        //        Getting data from ViewModel - MainActivityViewModel

        dataViewModel.getSaloonData().observe(viewLifecycleOwner, { data ->

//            Set Image, shop name, shop address and contact number
            binding.saloonItemNameTextView.text = data.saloonName
            binding.saloonItemAddressTextView.text = data.saloonAddress
            contact = data.contact.toString()
            shopAddress = "${data.saloonName} ${data.saloonAddress}"

//            Set Open Status
            setSaloonOpenStatus(data.openStatus!!)

            saloonID = data.saloonID.toString()

//            set saloon display picture
            setSaloonDisplayPic(data.imageSource!!)

//            set saloon rating and review count
            setRatingAndReview()
        })

        val viewPagerAdapter = SaloonItemViewPagerAdapter(this)
        binding.ViewPagerSaloonItem.adapter = viewPagerAdapter

        TabLayoutMediator(binding.tabInputSaloonItem, binding.ViewPagerSaloonItem){tab, position->
            when(position){
                0->{
                    tab.text = "Pricing"
                    tab.setIcon(R.drawable.ic_rupee_icon)
                }
                1->{
                    tab.text = "Photos"
                    tab.setIcon(R.drawable.ic_baseline_photo_library_24)
                }
                2->{
                    tab.text = "Reviews"
                    tab.setIcon(R.drawable.ic_baseline_rate_review_24)
                }
            }
        }.attach()

        binding.saloonRefresher.setOnRefreshListener {

            if (binding.ViewPagerSaloonItem.currentItem == 1){
                Log.i(TAG, "Refreshed : ${binding.ViewPagerSaloonItem.currentItem}")
            }else if (binding.ViewPagerSaloonItem.currentItem == 2){
                dataViewModel.setSaloonRefreshState(MainActivityViewModel.SaloonRefreshData(saloonPhotosState = false,saloonReview = true))
                setRatingAndReview()
                viewPagerAdapter.notifyItemChanged(binding.ViewPagerSaloonItem.currentItem)
            }

            HandlerCompat.postDelayed(
                Handler(Looper.getMainLooper()),
                {
                    binding.saloonRefresher.isRefreshing = false
                    dataViewModel.setSaloonRefreshState(MainActivityViewModel.SaloonRefreshData(saloonPhotosState = false,saloonReview = false))
                }
                ,null,2000)
        }

        binding.ViewPagerSaloonItem.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                viewPagerAdapter.notifyItemChanged(position)
            }
        })

        binding.switchSaloonStatusRadioGroup.setOnCheckedChangeListener { p0, _ ->
            if (p0?.checkedRadioButtonId == R.id.saloonOpenButton) {
                setSaloonOpenStatus(true)
                uploadSaloonOpenStatusToFireStore(true)
            } else if (p0?.checkedRadioButtonId == R.id.saloonCloseButton) {
                setSaloonOpenStatus(false)
                uploadSaloonOpenStatusToFireStore(false)
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setSaloonDisplayPic(profilePicRef: String){

        val imageRef = storageRef.child(profilePicRef)

        GlideApp.with(binding.root.context)
            .asBitmap()
            .load(imageRef)
            .centerCrop()
            .placeholder(R.drawable.ic_bull)
            .into(binding.saloonItemDisplayImage)
    }

    private fun setRatingAndReview(){

        db.collection("Saloons")
            .document(saloonID)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()){

                    val averageRating = document.getDouble("average_rating.current_average_rating")?.roundToInt()
                    val reviewCount = document.getLong("average_rating.number_of_reviews")?.toInt()

                    when(averageRating){
                        1 -> setRatingPic(R.drawable.ic_rating_one_stars)
                        2 -> setRatingPic(R.drawable.ic_rating_two_stars)
                        3 -> setRatingPic(R.drawable.ic_rating_three_stars)
                        4 -> setRatingPic(R.drawable.ic_rating_four_stars)
                        5 -> setRatingPic(R.drawable.ic_rating_five_stars)
                        else -> setRatingPic(R.drawable.ic_rating_one_stars)
                    }

                    binding.saloonItemReviewCountText.text = reviewCount.toString()
                }
            }
    }

    private fun setRatingPic(drawableResource: Int){

        GlideApp.with(binding.root.context)
            .asBitmap()
            .load(drawableResource)
            .placeholder(R.drawable.ic_rating_one_stars)
            .into(binding.ratingSaloonItemImageView)

    }

    private fun setSaloonOpenStatus(openStatus: Boolean){
        if (openStatus){
            binding.saloonOpenButton.isChecked
            binding.switchSaloonStatusRadioGroup.background = AppCompatResources.getDrawable(
                requireContext(),
                R.drawable.bg_toggle_out_line_open
            )
        } else {
            binding.saloonCloseButton.isChecked
            binding.switchSaloonStatusRadioGroup.background = AppCompatResources.getDrawable(
                requireContext(),
                R.drawable.bg_toggle_out_line_closed
            )
        }
    }

    private fun uploadSaloonOpenStatusToFireStore(openStatus: Boolean){
        db.collection("Saloons")
            .document(saloonID)
            .update("open_status", openStatus)
            .addOnSuccessListener {
                Log.i(TAG, "Saloon open status is updated")
                if (openStatus){
                    setSaloonOpenStatus(true)
                } else {
                    setSaloonOpenStatus(false)
                }
            }
            .addOnFailureListener { e->
                Log.i(TAG, "Error in Uploading Saloon Status : ${e.message}")
            }
    }

    companion object {
        private const val TAG = "TAGSaloonItemFragment"
    }
}