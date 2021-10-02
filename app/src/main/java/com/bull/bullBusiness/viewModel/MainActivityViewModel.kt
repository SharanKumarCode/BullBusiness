package com.bullSaloon.bull.viewModel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bull.bullBusiness.genericClasses.dataClasses.SaloonDataClass
import com.bull.bullBusiness.genericClasses.dataClasses.SaloonPhotosData
import com.google.firebase.firestore.GeoPoint

class MainActivityViewModel: ViewModel() {

    private var saloonDataList = MutableLiveData<MutableList<SaloonDataClass>>()
    private var saloonData = MutableLiveData<SaloonDataClass>()
    private var saloonRefreshDataState = MutableLiveData<SaloonRefreshData>()
    private var saloonPhotoData = MutableLiveData<SaloonPhotosData>()
    private var cameraFragmentMode = MutableLiveData("none")


    fun assignSaloonData(shop: MutableList<SaloonDataClass>){
        Log.i("TAGLocation","assignShopData")
        saloonDataList.value = shop
    }

    fun getSaloonDataList(): MutableLiveData<MutableList<SaloonDataClass>>{
        return saloonDataList
    }

    fun putSaloonData(id: String?){
        for (d in saloonDataList.value!!){
            if (d.saloonID == id){
                saloonData.value = d
            }
        }
    }

    fun getSaloonData():MutableLiveData<SaloonDataClass>{
        return saloonData
    }

    fun setSaloonRefreshState(data: SaloonRefreshData){
        saloonRefreshDataState.value = data
    }

    fun getSaloonRefreshState(): MutableLiveData<SaloonRefreshData>{
        return saloonRefreshDataState
    }

    fun assignSaloonPhotoData(data: SaloonPhotosData){
        saloonPhotoData.value = data
    }

    fun getSaloonPhotoData(): MutableLiveData<SaloonPhotosData>{
        return saloonPhotoData
    }

    fun assignCameraClickMode(data: String){
        cameraFragmentMode.value = data
    }

    fun getCameraClickMode(): MutableLiveData<String>{
        return cameraFragmentMode
    }

    data class SaloonRefreshData(
        val saloonPhotosState: Boolean = false,
        val saloonReview: Boolean = false
    )

    companion object {
        private const val TAG = "TAGMainActivityViewModel"
    }



}