package com.bull.bullBusiness.adapters


import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.Slide
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.bull.bullBusiness.R
import com.bull.bullBusiness.SingletonInstances
import com.bull.bullBusiness.databinding.ViewHolderSaloonAppointmentBinding
import com.bull.bullBusiness.genericClasses.GlideApp
import com.bull.bullBusiness.genericClasses.dataClasses.AppointmentDataClass

class AppointmentRecyclerViewAdapter(lists: MutableList<AppointmentDataClass>): RecyclerView.Adapter<AppointmentRecyclerViewAdapter.AppointmentViewHolder>() {

    private val appointmentList = lists
    private val storageRef = SingletonInstances.getStorageReference()
    private val db = SingletonInstances.getFireStoreInstance()

    private lateinit var userName: String
    private lateinit var userID: String
    private lateinit var saloonID: String
    private lateinit var appointmentID: String


    inner class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){

        val binding : ViewHolderSaloonAppointmentBinding = ViewHolderSaloonAppointmentBinding.bind(itemView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_holder_saloon_appointment,parent,false)
        return AppointmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        val holderBinding = holder.binding

        userName = appointmentList[position].user_name
        userID = appointmentList[position].userID
        saloonID = appointmentList[position].saloonID
        appointmentID = appointmentList[position].appointmentID

        holderBinding.appointmentUserName.text = appointmentList[position].user_name
        holderBinding.appointmentServiceNameText.text = appointmentList[position].service
        holderBinding.appointmentDateText.text = appointmentList[position].date
        holderBinding.appointmentTimeText.text = appointmentList[position].time

        var expansionFlag = false

        holderBinding.appointmentsContainer.setOnClickListener {
            if (expansionFlag){
                contractItem(holderBinding)
                expansionFlag = false
            } else {
                expandItem(holderBinding)
                expansionFlag = true
            }
        }

        setUserDisplayPic(holderBinding)
    }

    override fun getItemCount(): Int {
        return appointmentList.size
    }

    private fun expandItem(binding: ViewHolderSaloonAppointmentBinding){

//        TransitionManager.beginDelayedTransition(binding.appointmentsContainer, AutoTransition())
        TransitionManager.beginDelayedTransition(binding.appointmentsContainer, AutoTransition())
        val shortDuration = binding.root.context.resources.getInteger(R.integer.material_motion_duration_short_1)

        binding.appointmentLabelServiceName.apply {
            alpha = 0f
            visibility = View.VISIBLE

            animate()
                .alpha(1f)
                .setDuration(shortDuration.toLong())
                .setListener(null)
        }

        binding.appointmentServiceNameText.apply {
            alpha = 0f
            visibility = View.VISIBLE

            animate()
                .alpha(1f)
                .setDuration(shortDuration.toLong())
                .setListener(null)
        }

        binding.cancelAppointment.apply {
            alpha = 0f
            visibility = View.VISIBLE

            animate()
                .alpha(1f)
                .setDuration(shortDuration.toLong())
                .setListener(null)
        }
    }

    private fun contractItem(binding: ViewHolderSaloonAppointmentBinding){
        binding.appointmentLabelServiceName.visibility = View.GONE
        binding.appointmentServiceNameText.visibility = View.GONE
        binding.cancelAppointment.visibility = View.GONE

        TransitionManager.beginDelayedTransition(binding.appointmentsContainer, AutoTransition())
    }

    private fun setUserDisplayPic(binding: ViewHolderSaloonAppointmentBinding){

        val userNameUnderscore =
            userName.replace("\\s".toRegex(), "_")
        val imageUrl =
            "User_Images/${userID}/${userNameUnderscore}_profilePicture.jpg"

        val imageRef = storageRef.child(imageUrl)

        GlideApp.with(binding.root.context)
            .asBitmap()
            .load(imageRef)
            .centerCrop()
            .placeholder(R.drawable.ic_bull)
            .into(binding.appointmentUserDisplayPic)

    }

    companion object{
        private const val TAG = "TAGAppointmentRecyclerViewAdapter"
    }
}