package com.bull.bullBusiness.saloonList

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.transition.TransitionInflater
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.bull.bullBusiness.R
import com.bull.bullBusiness.SingletonInstances
import com.bull.bullBusiness.databinding.FragmentAddSaloonBinding
import com.bull.bullBusiness.genericClasses.GlideApp
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.material.snackbar.BaseTransientBottomBar.ANIMATION_MODE_SLIDE
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.GeoPoint
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AddSaloonFragment : Fragment() {

    private var _binding: FragmentAddSaloonBinding? = null
    private val binding get() = _binding!!

    private val db = SingletonInstances.getFireStoreInstance()
    private val storageRef = SingletonInstances.getStorageReference()
    private val auth = SingletonInstances.getAuthInstance()

    private lateinit var photoFileTemp: File
    private lateinit var outputDirectory: File
    private lateinit var dialog: Dialog
    private lateinit var animate: AnimatedVectorDrawable
    private lateinit var animCallback: Animatable2Compat.AnimationCallback

    private var longitude = 0.0
    private var latitude = 0.0
    private var stateNameText = ""
    private var address = ""

    private lateinit var snackBar: Snackbar

    private object InputErrorFlags  {
        var saloonName = false
        var contactNumber = false
        var doorNumber = false
        var streetName = false
        var addressLine = true
        var cityName = false
        var stateName = false
        var zipCode = false
        var image = false
        var location = false
        var haircutPrice = false
        var shavingPrice = false
        var haircutShavingPrice = false
    }

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
        _binding = FragmentAddSaloonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner){
            onBackButtonPressed()
        }

        outputDirectory = getOutputDirectory()

//        setting values for state list
        val items = activity?.resources?.getStringArray(R.array.state_region_list)!!
        val adapter = ArrayAdapter(requireContext(), R.layout.state_list_item, items)
        binding.stateNameTextField.setAdapter(adapter)

//        initialise snackBar
        snackBar = Snackbar
                    .make(binding.uploadSaloonButton, "Please fill all the Address Fields" , Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(requireContext().getColor(R.color.teal_700))
                    .setTextColor(requireContext().getColor(R.color.black))
                    .setAnimationMode(ANIMATION_MODE_SLIDE)

                snackBar.setAction("Dismiss"){
                    snackBar.dismiss()
                }

        binding.enterSaloonNameTextField.addTextChangedListener {
            val textLength = it.toString().length
            when {
                textLength < 5 -> {
                    InputErrorFlags.saloonName = false
                    binding.enterSaloonNameTextInputLayout.error = "minimum 4 characters is required"
                }
                textLength > 40 -> {
                    binding.enterSaloonNameTextInputLayout.error = "maximum 40 characters only allowed"
                    InputErrorFlags.saloonName = false
                }
                else -> {
                    binding.enterSaloonNameTextInputLayout.error = null
                    binding.enterSaloonNameTextInputLayout.boxStrokeColor = ContextCompat.getColor(binding.root.context, R.color.openStatusColor)
                    InputErrorFlags.saloonName = true
                }
            }
        }

        binding.mobileNumberTextField.addTextChangedListener {
            val textLength = it.toString().length
            when {
                textLength < 10 -> {
                    InputErrorFlags.contactNumber = false
                }
                textLength > 10 -> {
                    binding.mobileNumberTextInputLayout.error = "maximum 10 numbers only allowed"
                    InputErrorFlags.contactNumber = false
                }
                else -> {
                    binding.mobileNumberTextInputLayout.error = null
                    binding.mobileNumberTextInputLayout.boxStrokeColor = ContextCompat.getColor(binding.root.context, R.color.openStatusColor)
                    InputErrorFlags.contactNumber = true
                }
            }
        }

        binding.doorNumberTextField.addTextChangedListener {
            val textLength = it.toString().length
            when {
                textLength < 1 -> {
                    InputErrorFlags.doorNumber = false
                    binding.doorNumberTextInputLayout.error = "minimum 1 character is required"
                }
                textLength > 20 -> {
                    binding.doorNumberTextInputLayout.error = "maximum 20 characters only allowed"
                    InputErrorFlags.doorNumber = false
                }
                else -> {
                    binding.doorNumberTextInputLayout.error = null
                    binding.doorNumberTextInputLayout.boxStrokeColor = ContextCompat.getColor(binding.root.context, R.color.openStatusColor)
                    InputErrorFlags.doorNumber = true
                }
            }
        }

        binding.streetNameTextField.addTextChangedListener {
            val textLength = it.toString().length
            when {
                textLength < 4 -> {
                    InputErrorFlags.streetName = false
                    binding.streetNameTextInputLayout.error = "minimum 4 character is required"
                }
                textLength > 20 -> {
                    binding.streetNameTextInputLayout.error = "maximum 20 characters only allowed"
                    InputErrorFlags.streetName = false
                }
                else -> {
                    binding.streetNameTextInputLayout.error = null
                    binding.streetNameTextInputLayout.boxStrokeColor = ContextCompat.getColor(binding.root.context, R.color.openStatusColor)
                    InputErrorFlags.streetName = true
                }
            }
        }

        binding.addressLineTextField.addTextChangedListener {
            val textLength = it.toString().length
            when {
                textLength > 30 -> {
                    binding.addressLineInputLayout.error = "maximum 30 characters only allowed"
                    InputErrorFlags.addressLine = false
                }
                textLength == 0 -> {
                    binding.addressLineInputLayout.error = null
                    InputErrorFlags.addressLine = true
                }
                else -> {
                    binding.addressLineInputLayout.error = null
                    binding.addressLineInputLayout.boxStrokeColor = ContextCompat.getColor(binding.root.context, R.color.openStatusColor)
                    InputErrorFlags.addressLine = true
                }
            }
        }

        binding.pinCodeTextField.addTextChangedListener {
            val textLength = it.toString().length
            when {
                textLength < 6 -> {
                    InputErrorFlags.zipCode = false
                }
                textLength > 30 -> {
                    binding.pinCodeTextInputLayout.error = "maximum 6 characters only allowed"
                    InputErrorFlags.zipCode = false
                }
                else -> {
                    binding.pinCodeTextInputLayout.error = null
                    binding.pinCodeTextInputLayout.boxStrokeColor = ContextCompat.getColor(binding.root.context, R.color.openStatusColor)
                    InputErrorFlags.zipCode = true
                }
            }
        }

        binding.cityNameTextField.addTextChangedListener {
            val textLength = it.toString().length
            when {
                textLength < 3 -> {
                    InputErrorFlags.cityName = false
                    binding.cityNameTextInputLayout.error = "minimum 3 characters is required"
                }
                textLength > 20 -> {
                    binding.cityNameTextInputLayout.error = "maximum 20 characters only allowed"
                    InputErrorFlags.cityName = false
                }
                else -> {
                    binding.cityNameTextInputLayout.error = null
                    binding.cityNameTextInputLayout.boxStrokeColor = ContextCompat.getColor(binding.root.context, R.color.openStatusColor)
                    InputErrorFlags.cityName = true
                }
            }
        }

        binding.stateNameTextField.onItemClickListener = object : AdapterView.OnItemClickListener{
            override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                InputErrorFlags.stateName = true
                stateNameText = p0?.getItemAtPosition(p2).toString()
                Log.i(TAG, "clicked item ${p0?.getItemAtPosition(p2).toString()}")
            }
        }

        binding.saloonDisplayGalleryButton.setOnClickListener {
            getImageFromGallery()
        }

        binding.saloonLocationButton.setOnClickListener {

            if (validateAddressFields()){

                ViewCompat.setBackgroundTintList(binding.saloonLocationButton, ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.googleMapIconActive)))

                address = binding.enterSaloonNameTextField.text.toString() + " ," +
                        binding.doorNumberTextField.text.toString() + " " +
                        binding.streetNameTextField.text.toString() + " " +
                        binding.addressLineTextField.text.toString() + " " +
                        binding.cityNameTextField.text.toString() + " " +
                        binding.pinCodeTextField.text.toString() + " " +
                        stateNameText

                val location = Geocoder(requireContext()).getFromLocationName(address, 1)
                longitude = location[0].longitude
                latitude = location[0].latitude

                if (longitude != 0.0 && latitude != 0.0){
                    InputErrorFlags.location = true
                }

                Log.i(TAG, "Address is : $address")
                Log.i(TAG, "location is : $location")
                Log.i(TAG, "latitude is : $latitude , longitude is : $longitude")

                binding.saloonLocationStatusText.visibility = View.VISIBLE

            } else {

                snackBar.show()
            }

        }

        binding.haircutPriceTextField.addTextChangedListener {
            val textLength = it.toString().length
            when {
                textLength < 1 -> {
                    InputErrorFlags.haircutPrice = false
                    binding.haircutPriceTextInputLayout.error = "minimum 1 characters is required"
                }
                textLength > 6 -> {
                    binding.haircutPriceTextInputLayout.error = "maximum 6 characters only allowed"
                    InputErrorFlags.haircutPrice = false
                }
                else -> {
                    binding.haircutPriceTextInputLayout.error = null
                    binding.haircutPriceTextInputLayout.boxStrokeColor = ContextCompat.getColor(binding.root.context, R.color.openStatusColor)
                    InputErrorFlags.haircutPrice = true
                }
            }
        }

        binding.shavingPriceTextField.addTextChangedListener {
            val textLength = it.toString().length
            when {
                textLength < 1 -> {
                    InputErrorFlags.shavingPrice = false
                    binding.shavingPriceTextInputLayout.error = "minimum 1 characters is required"
                }
                textLength > 6 -> {
                    binding.shavingPriceTextInputLayout.error = "maximum 6 characters only allowed"
                    InputErrorFlags.shavingPrice = false
                }
                else -> {
                    binding.shavingPriceTextInputLayout.error = null
                    binding.shavingPriceTextInputLayout.boxStrokeColor = ContextCompat.getColor(binding.root.context, R.color.openStatusColor)
                    InputErrorFlags.shavingPrice = true
                }
            }
        }

        binding.haircutShavingTextField.addTextChangedListener {
            val textLength = it.toString().length
            when {
                textLength < 1 -> {
                    InputErrorFlags.haircutShavingPrice = false
                    binding.haircutShavingPriceTextInputLayout.error = "minimum 1 characters is required"
                }
                textLength > 6 -> {
                    binding.haircutShavingPriceTextInputLayout.error = "maximum 6 characters only allowed"
                    InputErrorFlags.haircutShavingPrice = false
                }
                else -> {
                    binding.haircutShavingPriceTextInputLayout.error = null
                    binding.haircutShavingPriceTextInputLayout.boxStrokeColor = ContextCompat.getColor(binding.root.context, R.color.openStatusColor)
                    InputErrorFlags.haircutShavingPrice = true
                }
            }
        }

        binding.uploadSaloonButton.setOnClickListener {
            if(validateAddressFields() && otherFieldsValidate()){

                dialog = Dialog(requireContext())
                dialog.setContentView(R.layout.dialog_saloon_data_uploading)
                val loadingIcon = dialog.findViewById<ImageView>(R.id.saloonDataUploadingImage)

                animate = loadingIcon.drawable as AnimatedVectorDrawable

                animCallback = object : Animatable2Compat.AnimationCallback() {
                    override fun onAnimationEnd(drawable: Drawable?) {
                        super.onAnimationEnd(drawable)

                        animate.start()
                    }
                }

                AnimatedVectorDrawableCompat.registerAnimationCallback(animate , animCallback)

                animate.start()

                requireActivity().window.setFlags(
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                )

                dialog.show()

                uploadSaloonDataToFireStore()

            } else {

                snackBar.setText("Please fill in all the Data Fields").show()
            }

        }

    }

    private fun onBackButtonPressed(){

        launchSaloonListFragment()
    }

    private fun getImageFromGallery(){
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT

        resultLauncher.launch(Intent.createChooser(intent, "Select an image"))
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if (it.resultCode == Activity.RESULT_OK){
            val imageUri = it.data?.data

            uploadImageFromGallery(imageUri!!)

            Log.i(TAG,"Obtained image: $imageUri")
        }
    }

    private fun uploadImageFromGallery(imageUri: Uri){

        binding.uploadedSaloonDisplayPic.visibility = View.VISIBLE

        imageUri.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(activity?.contentResolver!!, imageUri)
                val bitmap = ImageDecoder.decodeBitmap(source)

                val bg = bitmap.copy(Bitmap.Config.ARGB_8888, false)

                photoFileTemp = File(
                    outputDirectory,
                    SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
                )

                val fileOut = FileOutputStream(photoFileTemp)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fileOut)
                fileOut.flush()
                fileOut.close()

                GlideApp.with(requireContext())
                    .load(bg)
                    .centerCrop()
                    .thumbnail(0.2F)
                    .placeholder(R.drawable.ic_baseline_camera_40)
                    .fallback(R.drawable.ic_baseline_map_40)
                    .into(binding.uploadedSaloonDisplayPic)

                InputErrorFlags.image = true
            }
        }
    }

    private fun validateAddressFields(): Boolean{
        var validateFlag = true

        if (!InputErrorFlags.addressLine){
            validateFlag = false
            Log.i(TAG, "address Line is false")
        }
        if (!InputErrorFlags.cityName) {
            validateFlag = false
            Log.i(TAG, "cityName is false")
        }
        if (!InputErrorFlags.contactNumber){
            validateFlag = false
            Log.i(TAG, "contactNumber is false")
        }
        if (!InputErrorFlags.doorNumber){
            validateFlag = false
            Log.i(TAG, "doorNumber is false")
        }
        if (!InputErrorFlags.saloonName){
            validateFlag = false
            Log.i(TAG, "saloonName is false")
        }
        if (!InputErrorFlags.streetName){
            validateFlag = false
            Log.i(TAG, "streetName is false")
        }
        if (!InputErrorFlags.stateName){
            validateFlag = false
            Log.i(TAG, "stateName is false")
        }
        if (!InputErrorFlags.zipCode){
            validateFlag = false
            Log.i(TAG, "zipCode is false")
        }
        if (!InputErrorFlags.image){
            validateFlag = false
            Log.i(TAG, "image is false")
        }

        if (validateFlag){
            Log.i(TAG, "all fields are validated")
        }

        return validateFlag

    }

    private fun otherFieldsValidate(): Boolean{
        var otherValidateFlag = true

        if (!InputErrorFlags.haircutPrice){
            otherValidateFlag = false
            Log.i(TAG, "haircutPrice is false")
        }
        if (!InputErrorFlags.shavingPrice){
            otherValidateFlag = false
            Log.i(TAG, "shavingPrice is false")
        }
        if (!InputErrorFlags.haircutShavingPrice){
            otherValidateFlag = false
            Log.i(TAG, "haircutShavingPrice is false")
        }
        if (!InputErrorFlags.location){
            otherValidateFlag = false
            Log.i(TAG, "location is false")
        }

        return otherValidateFlag
    }

    private fun uploadSaloonDataToFireStore(){

        val saloonID = UUID.randomUUID().toString()
        val saloonMapData = hashMapOf<String, Any>()

        saloonMapData["saloon_name"] = binding.enterSaloonNameTextField.text.toString()
        saloonMapData["saloon_id"] = saloonID
        saloonMapData["saloon_owner_id"] = auth.currentUser?.uid.toString()
        saloonMapData["address"] = address
        saloonMapData["contact"] = "+91${binding.mobileNumberTextField.text.toString()}"
        saloonMapData["open_status"] = true
        saloonMapData["area"] = binding.streetNameTextField.text.toString()
        saloonMapData["location_data"] = GeoPoint(latitude , longitude)
        saloonMapData["geoHash"] = GeoFireUtils.getGeoHashForLocation(GeoLocation(latitude, longitude))
        saloonMapData["average_rating"] = hashMapOf("current_average_rating" to 5 ,
                                                    "number_of_ratings" to 0,
                                                    "number_of_reviews" to 0)
        saloonMapData["pricing_list"] = hashMapOf("Haircut" to binding.haircutPriceTextField.text.toString().toInt() ,
                                                    "Shaving" to binding.shavingPriceTextField.text.toString().toInt() ,
                                                    "Haircut + Shaving" to binding.haircutShavingTextField.text.toString().toInt())

        db.collection("Saloons")
            .document(saloonID)
            .set(saloonMapData)
            .addOnSuccessListener {
                Log.i(TAG,"Updated Saloon data to FireStore")
                uploadImageToStorage(saloonID)
            }
            .addOnFailureListener {e->
                disableDialog()
                snackBar.setText("Saloon Data Upload to Server failed. \n\n Please try again later.").show()
                Log.i(TAG,"Saloon Data update to FireStore failed : ${e.message}")
            }
    }

    private fun uploadImageToStorage(saloonID: String){
        val imagePath ="Saloon_Images/$saloonID/${binding.enterSaloonNameTextField.text.toString()}_displayPicture.jpg"

        val imageRef = storageRef.child(imagePath)
        val uploadTask =imageRef.putFile(Uri.fromFile(photoFileTemp))

        uploadTask.addOnSuccessListener {
            disableDialog()
            Toast.makeText(requireActivity(),"Saloon Data Uploaded Successfully", Toast.LENGTH_SHORT).show()
            Log.i(TAG,"Updated Saloon Image to Firebase Storage")
            launchSaloonListFragment()
        }

        uploadTask.addOnFailureListener {e->
            disableDialog()
            snackBar.setText("Saloon Data Upload to Server failed. \n\n Please try again later.").show()
            Log.i(TAG,"Saloon Image update to Firebase Storage failed : ${e.message}")
        }
    }

    private fun launchSaloonListFragment(){
        val navController = this.parentFragmentManager.findFragmentById(R.id.fragmentSaloonContainerView)?.findNavController()
        navController?.navigate(R.id.action_addSaloonFragment_to_saloonListFragment)
    }

    private fun getOutputDirectory(): File {
        val mediaDir = requireContext().getExternalFilesDir(null).let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }
        return if (mediaDir.exists())
            mediaDir else requireContext().filesDir
    }

    private fun disableDialog(){
        dialog.dismiss()
        AnimatedVectorDrawableCompat.unregisterAnimationCallback(animate, animCallback)
        animate.stop()
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    companion object {
        private const val TAG = "TAGAddSaloonFragment"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}