package com.bull.bullBusiness.saloonList

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.TransitionInflater
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.os.HandlerCompat
import androidx.core.os.bundleOf
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
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.collections.HashMap
import kotlin.math.roundToInt

class SaloonItemFragment : Fragment() {

    private var _binding: FragmentSaloonItemBinding? = null
    private val binding get() = _binding!!

    private lateinit var saloonID: String
    private lateinit var dataViewModel: MainActivityViewModel
    private lateinit var saloonTimingsData: HashMap<String, HashMap<String, Any>>

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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dataViewModel = ViewModelProvider(requireActivity()).get(MainActivityViewModel::class.java)
        db = SingletonInstances.getFireStoreInstance()
        auth = SingletonInstances.getAuthInstance()

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

            saloonID = data.saloonID.toString()

            //            Set Open Status
            setSaloonOpenStatus(data.openStatus!!)

//            set saloon display picture
            setSaloonDisplayPic(data.imageSource!!)

//            set saloon rating and review count
            setRatingAndReview()

//            set saloon timing data
            saloonTimingsData = data.saloonTimingsData
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
                viewPagerAdapter.notifyDataSetChanged()
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

        binding.saloonOpenTimingButtons.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                dialogSaloonTimings()
            }
        }

        dataViewModel.getCameraClickMode().observe(viewLifecycleOwner, {
            if (it == "clicked"){
                navController?.navigate(R.id.action_saloonItemFragment_to_cameraFragment, bundleOf("saloon_id" to saloonID))
                dataViewModel.assignCameraClickMode("none")
            }
        })

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun dialogSaloonTimings(){
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_saloon_timings)
        val okButton = dialog.findViewById<Button>(R.id.dialogSaloonTimingOkButton)
        val resetButton = dialog.findViewById<Button>(R.id.dialogSaloonTimingResetButton)
        val closeButton = dialog.findViewById<AppCompatImageButton>(R.id.dialogSaloonTimingCloseButton)

        val listToggleButtons = mutableListOf<SwitchCompat>()
        listToggleButtons.add(dialog.findViewById(R.id.dialogMondayToggle))
        listToggleButtons.add(dialog.findViewById(R.id.dialogTuesdayToggle))
        listToggleButtons.add(dialog.findViewById(R.id.dialogWednesdayToggle))
        listToggleButtons.add(dialog.findViewById(R.id.dialogThursdayToggle))
        listToggleButtons.add(dialog.findViewById(R.id.dialogFridayToggle))
        listToggleButtons.add(dialog.findViewById(R.id.dialogSaturdayToggle))
        listToggleButtons.add(dialog.findViewById(R.id.dialogSundayToggle))

        val openTimeButtons = mutableListOf<AppCompatButton>()
        openTimeButtons.add(dialog.findViewById(R.id.dialogMondayOpenTimeButton))
        openTimeButtons.add(dialog.findViewById(R.id.dialogTuesdayOpenTimeButton))
        openTimeButtons.add(dialog.findViewById(R.id.dialogWednesdayOpenTimeButton))
        openTimeButtons.add(dialog.findViewById(R.id.dialogThursdayOpenTimeButton))
        openTimeButtons.add(dialog.findViewById(R.id.dialogFridayOpenTimeButton))
        openTimeButtons.add(dialog.findViewById(R.id.dialogSaturdayOpenTimeButton))
        openTimeButtons.add(dialog.findViewById(R.id.dialogSundayOpenTimeButton))

        val closeTimeButtons = mutableListOf<AppCompatButton>()
        closeTimeButtons.add(dialog.findViewById(R.id.dialogMondayCloseTimeButton))
        closeTimeButtons.add(dialog.findViewById(R.id.dialogTuesdayCloseTimeButton))
        closeTimeButtons.add(dialog.findViewById(R.id.dialogWednesdayCloseTimeButton))
        closeTimeButtons.add(dialog.findViewById(R.id.dialogThursdayCloseTimeButton))
        closeTimeButtons.add(dialog.findViewById(R.id.dialogFridayCloseTimeButton))
        closeTimeButtons.add(dialog.findViewById(R.id.dialogSaturdayCloseTimeButton))
        closeTimeButtons.add(dialog.findViewById(R.id.dialogSundayCloseTimeButton))

        val allTimeButtons = openTimeButtons.plus(closeTimeButtons)

        resetSaloonTimeDialog(openTimeButtons, closeTimeButtons, listToggleButtons)

        listToggleButtons.forEach { button ->
            button.setOnCheckedChangeListener { p0, p1 -> setWorkingDayDialog(dialog, p0!!, p1) }
        }

        allTimeButtons.forEach { button ->
            button.setOnClickListener {
                val dialogTimePicker = TimePickerDialog(requireContext(), object : TimePickerDialog.OnTimeSetListener {
                    override fun onTimeSet(p0: TimePicker?, p1: Int, p2: Int) {

                        button.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.teal_700)

                        when (p1){
                            0 -> button.text = requireContext().resources.getString(R.string.placeHolderTime, "12", p2.toString().padStart(2, '0'), "AM")
                            12 -> button.text = requireContext().resources.getString(R.string.placeHolderTime, "12", p2.toString().padStart(2, '0'), "PM")
                            in 1..12 -> button.text = requireContext().resources.getString(R.string.placeHolderTime, p1.toString().padStart(2, '0'), p2.toString().padStart(2, '0'), "AM")
                            in 12..24 -> button.text = requireContext().resources.getString(R.string.placeHolderTime, (p1-12).toString().padStart(2, '0'), p2.toString().padStart(2, '0'), "PM")
                        }
                    }

                }, 8, 0, false)
                dialogTimePicker.show()
            }
        }

        okButton.setOnClickListener {

            var timeValidityFlag = true
            val timeFormatter = DateTimeFormatter.ofPattern("hh : mm a")

            for (i in 0..6){

                val openTime = LocalTime.parse(openTimeButtons[i].text.toString(), timeFormatter)
                val closeTime = LocalTime.parse(closeTimeButtons[i].text.toString(), timeFormatter)
                val diff = Duration.between(openTime, closeTime)

                if (diff.isNegative){
                    timeValidityFlag = false
                    closeTimeButtons[i].backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.teal_200_comp_red)
                    Toast.makeText(requireActivity(), "Closing time should be after Open Time", Toast.LENGTH_SHORT).show()
                }
            }

            if (timeValidityFlag){

                val saloonTimeData = hashMapOf<String, HashMap<String, Any>>()
                for (i in 0..6){
                    saloonTimeData[daysOfWeek[i]] = hashMapOf("open_time" to openTimeButtons[i].text.toString(),
                        "close_time" to closeTimeButtons[i].text.toString(),
                        "working" to listToggleButtons[i].isChecked)
                }

                uploadSaloonTimeToFireStore(saloonTimeData)

                dialog.dismiss()

            }

        }

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        resetButton.setOnClickListener {
            resetSaloonTimeDialog(openTimeButtons, closeTimeButtons, listToggleButtons)
        }

        dialog.show()
    }

    private fun setWorkingDayDialog(dialog: Dialog, button: CompoundButton, state: Boolean){
        when (button.id){
            R.id.dialogMondayToggle -> setWorkingDayHelper(dialog.findViewById(R.id.dialogMondayOpenTimeButton), dialog.findViewById(R.id.dialogMondayCloseTimeButton), state)
            R.id.dialogTuesdayToggle -> setWorkingDayHelper(dialog.findViewById(R.id.dialogTuesdayOpenTimeButton), dialog.findViewById(R.id.dialogTuesdayCloseTimeButton), state)
            R.id.dialogWednesdayToggle -> setWorkingDayHelper(dialog.findViewById(R.id.dialogWednesdayOpenTimeButton), dialog.findViewById(R.id.dialogWednesdayCloseTimeButton), state)
            R.id.dialogThursdayToggle -> setWorkingDayHelper(dialog.findViewById(R.id.dialogThursdayOpenTimeButton), dialog.findViewById(R.id.dialogThursdayCloseTimeButton), state)
            R.id.dialogFridayToggle -> setWorkingDayHelper(dialog.findViewById(R.id.dialogFridayOpenTimeButton), dialog.findViewById(R.id.dialogFridayCloseTimeButton), state)
            R.id.dialogSaturdayToggle -> setWorkingDayHelper(dialog.findViewById(R.id.dialogSaturdayOpenTimeButton), dialog.findViewById(R.id.dialogSaturdayCloseTimeButton), state)
            R.id.dialogSundayToggle -> setWorkingDayHelper(dialog.findViewById(R.id.dialogSundayOpenTimeButton), dialog.findViewById(R.id.dialogSundayCloseTimeButton), state)
        }
    }

    private fun setWorkingDayHelper(openTimeButton: Button , closeTimeButton: Button, state: Boolean){

        if (state){
            openTimeButton.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.teal_700)
            closeTimeButton.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.teal_700)
            openTimeButton.isClickable = true
            closeTimeButton.isClickable = true
        } else {
            openTimeButton.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.black_mild)
            closeTimeButton.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.black_mild)
            openTimeButton.isClickable = false
            closeTimeButton.isClickable = false
        }
    }

    private fun resetSaloonTimeDialog(openTimeButtons: MutableList<AppCompatButton>, closeTimeButtons: MutableList<AppCompatButton>, toggleButtons: MutableList<SwitchCompat>){

        for (i in 0..6){
            openTimeButtons[i].text = saloonTimingsData[daysOfWeek[i]]?.get("open_time").toString()
            closeTimeButtons[i].text = saloonTimingsData[daysOfWeek[i]]?.get("close_time").toString()
            toggleButtons[i].isChecked = saloonTimingsData[daysOfWeek[i]]?.get("working").toString().toBoolean()
            setWorkingDayHelper(openTimeButtons[i] , closeTimeButtons[i], saloonTimingsData[daysOfWeek[i]]?.get("working").toString().toBoolean())
        }

    }

    private fun uploadSaloonTimeToFireStore(saloonTimeData : HashMap<String, HashMap<String, Any>>){

        db.collection("Saloons")
            .document(saloonID)
            .update("saloon_timings", saloonTimeData)
            .addOnSuccessListener {
                Log.i(TAG, "Saloon timing is updated")
                Toast.makeText(requireActivity(), "Timings are updated", Toast.LENGTH_SHORT).show()
                saloonTimingsData = saloonTimeData
            }
            .addOnFailureListener { e->
                Log.i(TAG, "Error in uploading Saloon timing : ${e.message}")
                Toast.makeText(requireActivity(), "Error in uploading timing data.. \n\nPlease try again later", Toast.LENGTH_SHORT).show()
            }

    }

    private fun setSaloonDisplayPic(profilePicRef: String){

        val imageRef = storageRef.storage.getReferenceFromUrl(profilePicRef)

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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setSaloonOpenStatus(openStatus: Boolean){

        val timeFormatter = DateTimeFormatter.ofPattern("hh : mm a")
        val currentDay = LocalDate.now().dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
        val currentTime = LocalTime.now().format(timeFormatter)


        Log.i(TAG, "current day : $currentDay ; current time : $currentTime")

        db.collection("Saloons")
            .document(saloonID)
            .get()
            .addOnSuccessListener {
                if (it.exists()){

                    val saloonTimingsData = it.get("saloon_timings") as HashMap<String, HashMap<String, Any>>
                    val timeData = saloonTimingsData[currentDay]
                    val openTime = LocalTime.parse(timeData?.get("open_time").toString(), timeFormatter)
                    val closeTime = LocalTime.parse(timeData?.get("close_time").toString(), timeFormatter)
                    val currentTimeTemporal = LocalTime.parse(currentTime, timeFormatter)
                    val diffOpenTime = Duration.between(openTime, currentTimeTemporal)
                    val diffCloseTime = Duration.between(currentTimeTemporal, closeTime)

                    Log.i(TAG, "openTime : $openTime ; closeTime : $closeTime")
                    Log.i(TAG, "diffOpenTime : $diffOpenTime ; diffCloseTime : $diffCloseTime")
                    Log.i(TAG, "diffOpenTimeStatus : ${diffOpenTime.isNegative} ; diffCloseTime : ${diffCloseTime.isNegative}")


//                    if (timeData?.get("working") == false){
//
//                        binding.saloonCloseButton.isChecked
//                        binding.switchSaloonStatusRadioGroup.background = AppCompatResources.getDrawable(
//                            requireContext(),
//                            R.drawable.bg_toggle_out_line_closed
//                        )
//
//                    } else if (diffOpenTime.isNegative && !diffCloseTime.isNegative){
//
//                    }

                }
            }
            .addOnFailureListener { e->
                Log.i(TAG, "Error in fetching saloon timing : ${e.message}")
            }

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

    @RequiresApi(Build.VERSION_CODES.O)
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
        private val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    }
}