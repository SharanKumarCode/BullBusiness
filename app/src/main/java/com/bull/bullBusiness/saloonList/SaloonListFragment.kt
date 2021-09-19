package com.bull.bullBusiness.saloonList

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.transition.TransitionInflater
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bull.bullBusiness.R
import com.bull.bullBusiness.SingletonInstances
import com.bull.bullBusiness.adapters.SaloonListRecyclerViewAdapter
import com.bull.bullBusiness.databinding.FragmentSaloonListBinding
import com.bull.bullBusiness.genericClasses.SingletonUserData
import com.bull.bullBusiness.genericClasses.dataClasses.SaloonDataClass
import com.bullSaloon.bull.viewModel.MainActivityViewModel
import com.google.firebase.firestore.GeoPoint
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt


class SaloonListFragment : Fragment() {

    private var _binding: FragmentSaloonListBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataViewModel: MainActivityViewModel
    private lateinit var recyclerState: Parcelable
    private val saloonLists = mutableListOf<SaloonDataClass>()

    private val db = SingletonInstances.getFireStoreInstance()
    private val auth = SingletonInstances.getAuthInstance()

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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val saloonNavHostFragment = this.parentFragmentManager.findFragmentById(R.id.fragmentSaloonContainerView)
        val navController = saloonNavHostFragment?.findNavController()

        binding.addSaloonButton.setOnClickListener {
            navController?.navigate(R.id.action_saloonListFragment_to_addSaloonFragment)
        }

        binding.saloonListRecycler.layoutManager = LinearLayoutManager(activity)

        //restore scroll state
        val scrollState = SingletonUserData.getScrollState("SaloonListRecycler")

        if (scrollState != null){
            binding.saloonListRecycler.layoutManager?.onRestoreInstanceState(scrollState)
        }

        generateDataFirestore()

    }

    override fun onPause() {
        super.onPause()

        recyclerState = binding.saloonListRecycler.layoutManager?.onSaveInstanceState()!!
        SingletonUserData.updateScrollState("SaloonListRecycler",recyclerState)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun generateDataFirestore() {

        dataViewModel = ViewModelProvider(requireActivity()).get(MainActivityViewModel::class.java)

        val query = db.collection("Saloons")
            .whereEqualTo("saloon_owner_id",auth.currentUser?.uid.toString())
            .orderBy("saloon_name")
            .get()

        query.addOnSuccessListener {
            for (document in it.documents) {

                val saloonID: String? = document.getString("saloon_id")
                val saloonName: String? = document.getString("saloon_name")
                val areaName: String? = document.getString("area")
                val openStatus: Boolean? = document.getBoolean("open_status")
                val contact: String? = document.getString("contact")
                val saloonAddress: String? = document.getString("address")

                val locationData: GeoPoint = document.getGeoPoint("location_data")!!
                val pricingList = document.get("pricing_list") as HashMap<String, Number>
                val haircutPrice: Number? = pricingList["Haircut"]
                val shavingPrice: Number? = pricingList["Shaving"]

                val averageRating =
                    document.getDouble("average_rating.current_average_rating")?.roundToInt()
                val reviewCount = document.getLong("average_rating.number_of_reviews")?.toInt()

                val saloonNameUnderScore = saloonName?.replace("\\s".toRegex(), "_")
                val imageUrl = "Saloon_Images/$saloonID/${saloonNameUnderScore}_displayPicture.jpg"

                val distance = 0F

                val timeFormatter = DateTimeFormatter.ofPattern("hh : mm a")
                val currentDay = LocalDate.now().dayOfWeek.name.lowercase().replaceFirstChar { firstChar -> firstChar.uppercase() }
                val currentTime = LocalTime.now().format(timeFormatter)
                val saloonTimingsData = document.get("saloon_timings") as HashMap<String, HashMap<String, Any>>
                val timeData = saloonTimingsData[currentDay]
                val openTime = LocalTime.parse(timeData?.get("open_time").toString(), timeFormatter)
                val closeTime = LocalTime.parse(timeData?.get("close_time").toString(), timeFormatter)
                val currentTimeTemporal = LocalTime.parse(currentTime, timeFormatter)
                val diffOpenTime = Duration.between(openTime, currentTimeTemporal)
                val diffCloseTime = Duration.between(currentTimeTemporal, closeTime)

                val saloonData = SaloonDataClass(
                    saloonID,
                    saloonName,
                    areaName,
                    averageRating,
                    imageUrl,
                    openStatus,
                    contact,
                    saloonAddress,
                    haircutPrice,
                    shavingPrice,
                    reviewCount,
                    locationData,
                    distance,
                    saloonTimingsData
                )

                saloonLists.add(saloonData)
                dataViewModel.assignSaloonData(saloonLists)
                if(view != null){
                    dataViewModel.getSaloonDataList().observe(viewLifecycleOwner, { result ->
                        binding.saloonListRecycler.adapter = SaloonListRecyclerViewAdapter(result, dataViewModel, this)
                        val recyclerAdapter = binding.saloonListRecycler.adapter
                        recyclerAdapter?.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
                    })
                }

            }
        }
        .addOnFailureListener {e->
            Log.i(TAG, "error in fetching saloon list : ${e.message}")
        }

    }

    companion object {
        private const val TAG = "TAGSaloonListFragment"
    }
}