package com.bull.bullBusiness.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.*
import android.transition.TransitionInflater
import android.util.Log
import android.util.Size
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.bull.bullBusiness.MainActivity
import com.bull.bullBusiness.databinding.FragmentCameraBinding
import com.bull.bullBusiness.R
import com.bull.bullBusiness.SingletonInstances
import com.bull.bullBusiness.genericClasses.SingletonUserData
import com.bull.bullBusiness.genericClasses.dataClasses.UploadImageServicePayload
import com.bull.bullBusiness.services.UploadImageToFirebaseService
import com.bullSaloon.bull.viewModel.MainActivityViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.BaseTransientBottomBar.ANIMATION_MODE_SLIDE
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.StorageReference
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataViewModel: MainActivityViewModel

    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    private lateinit var outputDirectory: File
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraSelector: CameraSelector
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var camera: Camera
    private lateinit var photoFileTemp: File
    private var cameraLens: Int = LENS_FRONT

    private lateinit var storageRef: StorageReference
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var loadingIconAnim: AnimatedVectorDrawable
    private lateinit var snackBar: Snackbar

    private var saloonID = ""

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

        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility", "RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.actionBar?.hide()

        dataViewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)

        saloonID = arguments?.getString("saloon_id")!!

        storageRef = SingletonInstances.getStorageReference()
        db = SingletonInstances.getFireStoreInstance()
        auth = SingletonInstances.getAuthInstance()

        snackBar = Snackbar
            .make(binding.takePhotoButton, "Image is being uploaded in the background", Snackbar.LENGTH_SHORT)
            .setBackgroundTint(requireContext().getColor(R.color.teal_200))
            .setTextColor(requireContext().getColor(R.color.black))
            .setAnimationMode(ANIMATION_MODE_SLIDE)

        snackBar.setAction(R.string.buttonDismiss) {
            snackBar.dismiss()
        }

//        Request camera permissions
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        loadingIconAnim = binding.loadingIconCamera.drawable as AnimatedVectorDrawable
        outputDirectory = getOutputDirectory()

        startCamera(cameraLens)

        binding.takePhotoButton.setOnClickListener{
            setButtonVisibility(ViewVisibility.INITIAL_INVISIBLE)

//            starting loading icon
            startLoadingIcon()
            takeImage()
        }

        binding.changeCameraButton.setOnClickListener {
            if (cameraLens == LENS_FRONT){
                cameraLens = LENS_BACK
                startCamera(cameraLens)
            } else {
                cameraLens = LENS_FRONT
                startCamera(cameraLens)
            }
        }

        binding.UploadImageFirebaseButton.setOnClickListener{

            launchUploadImageService()
        }

        binding.CancelImageButton.setOnClickListener{
            deleteImageFile()
        }

        binding.viewFinder.setOnTouchListener { _, motionEvent ->
            when(motionEvent.action){
                MotionEvent.ACTION_DOWN -> return@setOnTouchListener true
                MotionEvent.ACTION_UP -> {
                    Log.i(TAG,"tap to focus is called")
                    tapToFocus()
                    return@setOnTouchListener true
                }
                else-> return@setOnTouchListener false
            }
        }

        binding.backCameraButton.setOnClickListener {
            deleteImageFile()
            (activity as MainActivity).setActionBarBottomBarVisibility("show")
            val navHostFragment = this.parentFragmentManager.findFragmentById(R.id.fragmentSaloonContainerView)

            navHostFragment?.findNavController()?.navigate(R.id.saloonItemFragment)
        }

        binding.uploadFromGalleryButton.setOnClickListener {
            setButtonVisibility(ViewVisibility.INITIAL_INVISIBLE)
            getImageFromGallery()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!allPermissionsGranted()) {
                Toast.makeText(requireContext(),
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (this::photoFileTemp.isInitialized){
            photoFileTemp.delete()
        }

        _binding = null
        cameraExecutor.shutdown()
    }

    private fun startCamera(cameraLens: Int){
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener( {
            val cameraProvider = cameraProviderFuture.get()
            bindUseCases(cameraProvider, cameraLens)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindUseCases(cameraProvider : ProcessCameraProvider, cameraLens: Int){
        cameraProvider.unbindAll()

        val preview: Preview = Preview.Builder()
            .build()

        cameraSelector = CameraSelector.Builder().requireLensFacing(cameraLens).build()

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280,720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        camera = cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector,imageCapture, imageAnalysis, preview)

        preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)

    }

    private fun takeImage(){

        photoFileTemp = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFileTemp).build()

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(requireContext()), object: ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {

                    Log.d(TAG, "Image is stored in file")

                    val imgBitmap = BitmapFactory.decodeFile(photoFileTemp.absolutePath)

                    val ei = ExifInterface(photoFileTemp).rotationDegrees

                    val matrix = Matrix()
                    matrix.postRotate(ei.toFloat())

                    //rotating image to correct orientation
                    if (cameraLens == LENS_FRONT){
                        matrix.preScale(1.0F, -1.0F)
                    }

                    val rotatedImgBitmap = Bitmap.createBitmap(imgBitmap, 0, 0, imgBitmap.width, imgBitmap.height, matrix, true)

                    stopLoadingIcon()

                    val bg = BitmapDrawable(resources,rotatedImgBitmap)

                    val aspectRatio = rotatedImgBitmap.width.toFloat() / rotatedImgBitmap.height.toFloat()
                    val width = Resources.getSystem().displayMetrics.widthPixels
                    val height = (width / aspectRatio).toInt()

                    Glide.with(binding.root.context)
                        .load(bg)
                        .apply(RequestOptions.overrideOf(width, height))
                        .placeholder(R.drawable.ic_bull)
                        .into(binding.capturedImageView)

                }

                override fun onError(exception: ImageCaptureException) {
                    setButtonVisibility(ViewVisibility.UPLOAD_COMPLETE_VISIBILITY)
                    Log.d(TAG, "error occurred: $exception")
                }
            }
        )
    }

    private fun tapToFocus(){
        val previewView = binding.viewFinder
        val x = (previewView.width/2).toFloat()
        val y = (previewView.height/2).toFloat()
        Log.i(TAG,"x: $x, y:$y")
        val meteringPoint = DisplayOrientedMeteringPointFactory(
            previewView.display,
            camera.cameraInfo,
            previewView.width.toFloat(),
            previewView.height.toFloat()).createPoint(x,y)

        val action = FocusMeteringAction.Builder(meteringPoint).build()
        camera.cameraControl.startFocusAndMetering(action)
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

            Log.i("CameraX","Obtained image: $imageUri")
        }
    }

    private fun uploadImageFromGallery(imageUri: Uri){

        binding.capturedImageViewLayout.visibility = View.VISIBLE

        imageUri.let { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(activity?.contentResolver!!, imageUri)
            val bitmap = ImageDecoder.decodeBitmap(source)

            val bg = BitmapDrawable(resources, bitmap)

            photoFileTemp = File(
                outputDirectory,
                SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
            )

            val fileOut = FileOutputStream(photoFileTemp)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fileOut)
            fileOut.flush()
            fileOut.close()

            binding.capturedImageView.setImageDrawable(bg)
            }
        }
    }

    private fun launchUploadImageService(){
        Log.i("TAGUploadImageToFirebaseService", "launchUploadImageService from Camera Fragment")

        try {
            val serviceIntent = Intent(requireContext(), UploadImageToFirebaseService::class.java)
            val serviceData = UploadImageServicePayload(
                saloonID,
                photoFileTemp.absolutePath)
            val servicePayload = Bundle()
            servicePayload.putParcelable("service_data", serviceData)
            serviceIntent.putExtra("service_payload", servicePayload)
            requireActivity().startService(serviceIntent)

            Log.i("TAGUploadImageToFirebaseService", "launchUploadImageService from Camera Fragment trying")

            setButtonVisibility(ViewVisibility.UPLOAD_COMPLETE_VISIBILITY)

        } catch (e: Exception){

            setButtonVisibility(ViewVisibility.UPLOAD_COMPLETE_VISIBILITY)
            Log.i("TAGUploadImageToFirebaseService", "error in service : ${e.message}")
            Toast.makeText(requireActivity(), "Error in uploading Image.. Please try again later", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteImageFile(){

        if (this::photoFileTemp.isInitialized){
            photoFileTemp.delete()
        }
        Log.i(TAG, "PhotoFile Deleted")

        setButtonVisibility(ViewVisibility.UPLOAD_COMPLETE_VISIBILITY)

    }

    private fun startLoadingIcon(){

        binding.loadingIconCamera.visibility = View.VISIBLE
        loadingIconAnim = binding.loadingIconCamera.drawable as AnimatedVectorDrawable

        AnimatedVectorDrawableCompat.registerAnimationCallback(loadingIconAnim, object: Animatable2Compat.AnimationCallback(){
            override fun onAnimationEnd(drawable: Drawable?) {
                super.onAnimationEnd(drawable)
                loadingIconAnim.start()
            }
        })

        loadingIconAnim.start()
    }

    private fun stopLoadingIcon(){
        loadingIconAnim.stop()
        binding.loadingIconCamera.visibility = View.GONE
        binding.capturedImageViewLayout.visibility = View.VISIBLE
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = requireContext().getExternalFilesDir(null).let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }
        return if (mediaDir.exists())
            mediaDir else requireContext().filesDir
    }

    private fun setButtonVisibility(flag: String){
        if (flag == ViewVisibility.INITIAL_INVISIBLE){
            binding.takePhotoButton.visibility = View.INVISIBLE
            binding.changeCameraButton.visibility = View.INVISIBLE
            binding.uploadFromGalleryButton.visibility = View.INVISIBLE
            binding.backCameraButton.visibility = View.INVISIBLE

        } else if (flag == ViewVisibility.UPLOAD_COMPLETE_VISIBILITY){
            binding.capturedImageViewLayout.visibility = View.GONE
            binding.takePhotoButton.visibility = View.VISIBLE
            binding.changeCameraButton.visibility = View.VISIBLE
            binding.uploadFromGalleryButton.visibility = View.VISIBLE
            binding.backCameraButton.visibility = View.VISIBLE

        }

    }

    companion object {
        private const val TAG = "CameraX"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val LENS_FRONT = CameraSelector.LENS_FACING_FRONT
        private const val LENS_BACK = CameraSelector.LENS_FACING_BACK
        const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private object ViewVisibility {
            const val INITIAL_INVISIBLE = "initial_invisible"
            const val UPLOAD_COMPLETE_VISIBILITY = "upload_complete_visibility"
        }
    }





}