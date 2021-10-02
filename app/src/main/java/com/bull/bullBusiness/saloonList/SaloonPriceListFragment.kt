package com.bull.bullBusiness.saloonList

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageButton
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bull.bullBusiness.R
import com.bull.bullBusiness.adapters.SaloonPricingRecyclerViewAdapter
import com.bull.bullBusiness.databinding.FragmentSaloonPriceListBinding
import com.bullSaloon.bull.viewModel.MainActivityViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore

class SaloonPriceListFragment : Fragment() {

    private var _binding: FragmentSaloonPriceListBinding? = null
    private val binding get() = _binding!!

    private lateinit var saloonID: String
    private lateinit var dataViewModel: MainActivityViewModel

    private lateinit var db: FirebaseFirestore


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSaloonPriceListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dataViewModel = ViewModelProvider(requireActivity()).get(MainActivityViewModel::class.java)

        binding.saloonPricingRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.saloonPricingRecyclerView.layoutAnimation

        val priceList = mutableListOf<HashMap<String, Number>>()

//        Getting data from ViewModel - MainActivityViewModel

        dataViewModel.getSaloonData().observe(viewLifecycleOwner, { data ->

            data.pricingList?.forEach { (key, value) ->
                priceList.add(hashMapOf(key to value))
            }

            binding.saloonPricingRecyclerView.adapter = SaloonPricingRecyclerViewAdapter(priceList, this)
        })

        binding.addPriceButton.setOnClickListener {
            launchPriceDialog("new")
        }

    }

    override fun onResume() {
        super.onResume()

        binding.root.requestLayout()
    }

    fun launchPriceDialog(type: String, styleName: String = "", price: Int = 0){

        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_edit_pricing)

        val styleNameTextLayout = dialog.findViewById<TextInputLayout>(R.id.styleNameDialogPriceTextInputLayout)
        val styleNameTextField = dialog.findViewById<TextInputEditText>(R.id.styleNameDialogPriceTextField)
        val priceTextLayout = dialog.findViewById<TextInputLayout>(R.id.priceDialogPriceTextInputLayout)
        val priceTextField = dialog.findViewById<TextInputEditText>(R.id.priceDialogPriceTextField)

        val okButton = dialog.findViewById<MaterialButton>(R.id.okDialogPriceButton)
        val deleteButton = dialog.findViewById<MaterialButton>(R.id.deleteDialogPriceButton)
        val closeButton = dialog.findViewById<AppCompatImageButton>(R.id.closeDialogPriceButton)


        if (type == "new"){
            val params = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2.0F )
            params.setMargins(40,0,40,0)
            deleteButton.visibility = View.GONE
            okButton.layoutParams = params
        } else  if (type == "edit") {

            styleNameTextField.setText(styleName)
            priceTextField.setText(price.toString())
            styleNameTextField.inputType = InputType.TYPE_NULL
            styleNameTextLayout.isCounterEnabled = false
            styleNameTextField.setBackgroundColor(requireContext().resources.getColor(R.color.black_mild))

        }

        styleNameTextField.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(p0: Editable?) {
                when {
                    styleNameTextField.text?.length!! < 3 -> {
                        styleNameTextLayout.error = "minimum 3 characters required"
                    }
                    styleNameTextField.text?.length!! > 20 -> {
                        styleNameTextLayout.error = "restrict style name to 20 characters"
                    }
                    else -> {
                        styleNameTextLayout.error = null
                    }
                }
            }
        })

        priceTextField.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(p0: Editable?) {
                when {
                    priceTextField.text?.length!! < 1 -> {
                        priceTextLayout.error = "minimum 1 digit required"
                    }
                    priceTextField.text?.length!! > 5 -> {
                        priceTextLayout.error = "restrict price to 5 digits"
                    }
                    else -> {
                        priceTextLayout.error = null
                    }
                }
            }
        })

        okButton.setOnClickListener {

            val styleNameLength = styleNameTextField.text?.length!!
            val priceLength = priceTextField.text?.length!!

            if ((styleNameLength in 4..19) || (priceLength in 2..4)){

                uploadPriceToFireStore(styleNameTextField.text.toString(), priceTextField.text.toString().toInt())

                activity?.window?.setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

                dialog.hide()
            }
        }

        deleteButton.setOnClickListener {

            dialog.hide()
        }

        closeButton.setOnClickListener {

            dialog.hide()
        }

        dialog.show()

    }

    private fun uploadPriceToFireStore(styleName: String, price: Int){

        db.collection("Saloons")
            .document(saloonID)
            .update("pricing_list.${styleName}", price)
            .addOnSuccessListener {
                Log.i(TAG, "price is added")
            }
            .addOnFailureListener { e->
                Log.i(TAG, "Error in adding price : ${e.message}")
            }
    }

    companion object {
        private const val TAG = "TAGSaloonPriceListFragment"
    }
}