package com.bull.bullBusiness.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bull.bullBusiness.R
import com.bull.bullBusiness.databinding.ViewHolderSaloonPricingItemBinding
import com.bull.bullBusiness.saloonList.SaloonPriceListFragment

class SaloonPricingRecyclerViewAdapter(_lists: MutableList<HashMap<String, Number>>, _fragment: SaloonPriceListFragment): RecyclerView.Adapter<SaloonPricingRecyclerViewAdapter.SaloonPricingRecyclerViewHolder>() {

    private val lists = _lists
    private val fragment = _fragment

    inner class SaloonPricingRecyclerViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val binding : ViewHolderSaloonPricingItemBinding = ViewHolderSaloonPricingItemBinding.bind(itemView)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SaloonPricingRecyclerViewHolder {

        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_holder_saloon_pricing_item,parent,false)
        return SaloonPricingRecyclerViewHolder(view)
    }

    override fun onBindViewHolder(holder: SaloonPricingRecyclerViewHolder, position: Int) {

        lists[position].forEach { (key, value) ->
            holder.binding.saloonPricingLabelText.text = key
            holder.binding.saloonPricingPriceText.text = holder.binding.root.resources.getString(R.string.textHaircutPrice, value.toString())
        }

        holder.binding.saloonPricingLabelText.setOnClickListener {
            lists[position].forEach { (key, value) ->
                holder.binding.saloonPricingLabelText.text = key
                holder.binding.saloonPricingPriceText.text = holder.binding.root.resources.getString(R.string.textHaircutPrice, value.toString())

                fragment.launchPriceDialog("edit", key, value.toInt())
                }
        }

        holder.binding.saloonPricingPriceText.setOnClickListener {
            lists[position].forEach { (key, value) ->
                holder.binding.saloonPricingLabelText.text = key
                holder.binding.saloonPricingPriceText.text = holder.binding.root.resources.getString(R.string.textHaircutPrice, value.toString())

                fragment.launchPriceDialog("edit", key, value.toInt())
            }
        }

    }

    override fun getItemCount(): Int {
        return lists.size
    }
}