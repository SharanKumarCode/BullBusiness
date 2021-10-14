package com.bull.bullBusiness.saloonList

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bull.bullBusiness.R
import com.bull.bullBusiness.SingletonInstances
import com.bull.bullBusiness.adapters.AppointmentRecyclerViewAdapter
import com.bull.bullBusiness.databinding.FragmentSaloonAppointmentBinding
import com.bull.bullBusiness.genericClasses.dataClasses.AppointmentDataClass
import com.bullSaloon.bull.viewModel.MainActivityViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.type.DateTime
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.collections.HashMap


class SaloonAppointmentFragment : Fragment() {

    private var _binding: FragmentSaloonAppointmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var saloonID: String
    private lateinit var dataViewModel: MainActivityViewModel

    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSaloonAppointmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = SingletonInstances.getFireStoreInstance()
        dataViewModel = ViewModelProvider(requireActivity()).get(MainActivityViewModel::class.java)

        dataViewModel.getSaloonData().observe(viewLifecycleOwner, {
            saloonID = it.saloonID!!
            getAppointmentListFireStore()
        })

        binding.recyclerSaloonAppointments.layoutManager = LinearLayoutManager(requireContext())

    }

    override fun onResume() {
        super.onResume()

        binding.root.requestLayout()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getAppointmentListFireStore(){

        val appointmentLists = mutableListOf<AppointmentDataClass>()

        db.collection("Saloons")
            .document(saloonID)
            .collection("appointments")
            .get()
            .addOnSuccessListener {
                if (!it.isEmpty){
                    for (document in it.documents){
                        val appointmentID = document.getString("appointment_id")!!
                        val userID = document.getString("user_id")!!
                        val userName = document.getString("user_name")!!
                        val saloonID = document.getString("saloon_id")!!
                        val saloonName = document.getString("saloon_name")!!
                        val areaName = document.getString("area")!!
                        val service = document.getString("service")!!
                        val dateMap = document.get("date") as HashMap<String, Int>
                        val timeMap = document.get("time") as HashMap<String, String>

                        val appointmentDate = LocalDate.of(dateMap["Year"]!!, dateMap["Month"]!!, dateMap["Day"]!!)
                        val diff = ChronoUnit.DAYS.between(LocalDate.now(), appointmentDate)

                        val date = if (diff == 0L){
                            "Today"
                        } else {
                            "${dateMap["Day"]} , ${Month.of(dateMap["Month"].toString().toInt()+1).toString().substring(0,3).lowercase().replaceFirstChar { c -> c.uppercase() }} ${dateMap["Year"]}"
                        }

                        val time = "${timeMap["Hour"]} : ${timeMap["Minute"]} ${timeMap["AmPm"]}"

                        val appointmentData = AppointmentDataClass(
                            appointmentID,
                            userID,
                            userName,
                            saloonID,
                            saloonName,
                            areaName,
                            service,
                            date,
                            time
                        )

                        appointmentLists.add(appointmentData)
                    }
                }

                binding.appointmentsNumber.text = requireContext().resources.getString(R.string.placeHolderNumberOfAppointments, appointmentLists.size)

                if (appointmentLists.size == 1){
                    binding.appointmentsLabel.text = requireContext().resources.getString(R.string.textLabelAppointment)
                } else {
                    binding.appointmentsLabel.text = requireContext().resources.getString(R.string.textLabelAppointments)
                }

                binding.recyclerSaloonAppointments.adapter = AppointmentRecyclerViewAdapter(appointmentLists)
            }
            .addOnFailureListener { e->
                Log.i(TAG, "error in getting appointment list : ${e.message}")
            }
    }


    companion object {
        private const val TAG = "SaloonAppointmentFragment"
    }
}