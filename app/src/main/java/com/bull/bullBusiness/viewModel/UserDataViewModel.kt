package com.bull.bullBusiness.viewModel

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bull.bullBusiness.genericClasses.dataClasses.UserDataClass

class UserDataViewModel: ViewModel() {

    private var userBasicData = MutableLiveData<UserDataClass>()
    private var userDataProfilePic = MutableLiveData<Bitmap>()

    fun assignBasicUserData(data: UserDataClass){
        userBasicData.value = data
    }

    fun getUserBasicData(): MutableLiveData<UserDataClass>{
        return userBasicData
    }

    fun assignProfilePic(data: Bitmap){
        userDataProfilePic.value = data
    }

    fun getProfilePic(): MutableLiveData<Bitmap>{
        return userDataProfilePic
    }



}