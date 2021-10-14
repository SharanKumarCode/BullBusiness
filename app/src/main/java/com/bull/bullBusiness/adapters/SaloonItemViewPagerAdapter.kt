package com.bull.bullBusiness.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bull.bullBusiness.saloonList.*

class SaloonItemViewPagerAdapter(fragment: Fragment): FragmentStateAdapter(fragment) {

//    private val fragmentList = mutableListOf<Fragment>()
    override fun getItemCount(): Int {
        return 4
    }

    override fun createFragment(position: Int): Fragment {
        return when(position){
            0->{
                SaloonPriceListFragment()
            }
            1->{
                SaloonPhotosContainerFragment()
            }
            2->{
                SaloonReviewFragment()
            }
            3->{
                SaloonAppointmentFragment()
            }
            else -> Fragment()
        }
    }
}