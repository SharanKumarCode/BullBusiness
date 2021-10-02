package com.bull.bullBusiness.services

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bull.bullBusiness.R
import com.bull.bullBusiness.SingletonInstances
import com.bull.bullBusiness.camera.CameraFragment.Companion.FILENAME_FORMAT
import com.bull.bullBusiness.genericClasses.dataClasses.UploadImageServicePayload
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.StorageReference
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class UploadImageToFirebaseService : Service() {

    private lateinit var db: FirebaseFirestore
    private lateinit var storageRef: StorageReference
    private lateinit var photoFileTempPath: String
    private lateinit var photoFileTemp: File

    private lateinit var saloonID: String

    private var serviceLooper: Looper? = null
    private var serviceHandler: ServiceHandler? = null

    // Handler that receives messages from the thread
    private inner class ServiceHandler(looper: Looper) : Handler(looper) {

        override fun handleMessage(msg: Message) {
            try {
                uploadImage()
            } catch (e: InterruptedException) {
                // Restore interrupt status.

                Log.i(TAG, "service thread interrupted error: ${e.message}")
                Thread.currentThread().interrupt()
            }

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1)
        }
    }

    override fun onCreate() {
        super.onCreate()

        db = SingletonInstances.getFireStoreInstance()
        storageRef = SingletonInstances.getStorageReference()

        HandlerThread("UploadImageServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()

            serviceLooper = looper
            serviceHandler = ServiceHandler(looper)
        }

        Log.i(TAG,"service created")

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.i(TAG,"service started")

        val dataBundle = intent?.extras?.getBundle("service_payload")
        val data = dataBundle?.getParcelable<UploadImageServicePayload>("service_data")

        if (data != null) {
            saloonID = data.getSaloonID()
            photoFileTempPath = data.getPhotoFileTempPath()
        }

        serviceHandler?.obtainMessage()?.also { msg ->
            msg.arg1 = startId
            serviceHandler?.sendMessage(msg)
        }

        return START_NOT_STICKY
    }


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()

        deleteImageFile()

        Log.i(TAG,"service stopped")
    }

    private fun uploadImage(){

        photoFileTemp = File(photoFileTempPath)

        val dateFormat = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        Log.d(TAG, "Image is uploading - saloonID : $saloonID")

        val imagePathFirestore = "Saloon_Images/${saloonID}/$dateFormat.jpg"

        val imageRef = storageRef.child(imagePathFirestore)
        val uploadTask = imageRef.putFile(Uri.fromFile(photoFileTemp))

        uploadTask.addOnSuccessListener{

            Toast.makeText(this,"Image uploaded successfully..", Toast.LENGTH_SHORT).show()

            Log.d(TAG, "Image is uploaded")

            updateFirestoreUserData(imagePathFirestore, dateFormat)
        }

        uploadTask.addOnFailureListener{

            Log.d(TAG, "Image upload failed: ${it.message}")
            Toast.makeText(this,"Image upload failed..", Toast.LENGTH_SHORT).show()

            deleteImageFile()
        }

//        show progress in notification
        val notificationID = 123
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).apply {
            setContentTitle("Image upload")
            setContentText("Upload is in progress")
            setSmallIcon(R.drawable.ic_bull)
            priority = NotificationCompat.PRIORITY_DEFAULT
        }

        NotificationManagerCompat.from(this).apply{
            builder.setProgress(100, 0, false)
            notify(notificationID, builder.build())

            uploadTask.addOnProgressListener {
                val progress = ((it.bytesTransferred.toFloat() / it.totalByteCount.toFloat()) * 100).toInt()
                builder.setProgress(100, progress, false)
                builder.priority = NotificationCompat.PRIORITY_LOW
                notify(notificationID, builder.build())
                Log.i(TAG, "upload in progress :$progress %")
            }

            uploadTask.addOnCompleteListener{

                if (it.isSuccessful){
                    builder.setContentText("Upload Complete")
                        .setProgress(0,0, false)
                        .priority = NotificationCompat.PRIORITY_DEFAULT
                    notify(notificationID, builder.build())
                } else {
                    builder.setContentText("Upload Failed")
                        .setProgress(0,0, false)
                        .priority = NotificationCompat.PRIORITY_DEFAULT
                    notify(notificationID, builder.build())
                }
            }
        }
    }

    private fun updateFirestoreUserData(imagePath: String, dateFormat: String){

        val fireStoreUrl = "gs://bull-saloon.appspot.com/"

        try {

            val photoUUID = UUID.randomUUID().toString()
            val mapData = hashMapOf<String, Any>("timestamp" to dateFormat,
                "image_ref" to "$fireStoreUrl$imagePath",
                "photoID" to photoUUID,
                "display_pic" to false)

            db.collection("Saloons")
                .document(saloonID)
                .collection("photos")
                .document(photoUUID)
                .set(mapData)
                .addOnSuccessListener {

                    Log.i(TAG, "Data updated")
                }
                .addOnFailureListener {

                    Log.i(TAG, "Data update Failed : ${it.message}")
                }
                .addOnCompleteListener {
                    deleteImageFile()
                }

        }catch (e: Exception){
            deleteImageFile()
            Log.i(TAG, "error: $e")
        }
    }

    private fun deleteImageFile(){

        if (this::photoFileTemp.isInitialized){
            photoFileTemp.delete()
        }
        Log.i(TAG, "PhotoFile Deleted")

    }

    companion object {
        private const val TAG = "TAGUploadImageToFirebaseService"
        private const val NOTIFICATION_CHANNEL_ID = "100"
    }
}