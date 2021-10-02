package com.bull.bullBusiness.genericClasses.dataClasses

import android.os.Parcel
import android.os.Parcelable

class UploadImageServicePayload(
    private val saloonID: String,
    private val photoFileTempPath: String
) : Parcelable {

    constructor(parcel: Parcel) : this(
        saloonID = parcel.readString().toString(),
        photoFileTempPath = parcel.readString().toString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(saloonID)
        parcel.writeString(photoFileTempPath)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun getSaloonID(): String {
        return saloonID
    }

    fun getPhotoFileTempPath(): String {
        return photoFileTempPath
    }

    companion object CREATOR : Parcelable.Creator<UploadImageServicePayload> {
        override fun createFromParcel(parcel: Parcel): UploadImageServicePayload {
            return UploadImageServicePayload(parcel)
        }

        override fun newArray(size: Int): Array<UploadImageServicePayload?> {
            return arrayOfNulls(size)
        }
    }
}