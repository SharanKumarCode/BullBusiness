package com.bull.bullBusiness.genericClasses.dataClasses

import android.graphics.Bitmap
import com.google.firebase.firestore.GeoPoint

data class MyNicesData(
    val targetUserID: String,
    val targetPhotoID: String,
    val targetUserName: String,
    val targetImageRef: String,
    val targetUserProfilePicRef: String,
    val timeStamp: String
)

data class SaloonPhotosData(
    val saloonID: String,
    val photoID: String,
    val imageRef: String,
    val timestamp: String,
    val displayPic: Boolean
)

data class SaloonDataClass(val saloonID: String?,
                           val saloonName:String?,
                           val areaName:String?,
                           val rating: Int?,
                           val imageSource:String?,
                           val openStatus:Boolean?,
                           val contact: String?,
                           val saloonAddress: String?,
                           val pricingList: HashMap<String, Number>?,
                           val haircutPrice: Number?,
                           val shavingPrice: Number?,
                           val reviewCount: Number?,
                           val locationData: GeoPoint?,
                           var distance: Float?,
                           val saloonTimingsData: HashMap<String, HashMap<String, Any>>)

data class UserDataClass(
    val user_id: String,
    val user_name: String,
    val mobileNumber: String,
    val profilePicBitmap: Bitmap? = null)

data class AppointmentDataClass(
    val appointmentID: String,
    val userID: String,
    val user_name: String,
    val saloonID: String,
    val saloonName: String,
    val areaName: String,
    val service: String,
    val date: String,
    val time: String
)
