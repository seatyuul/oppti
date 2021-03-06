package com.serhatyurdakul.todo.app.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.serhatyurdakul.todo.R
import com.serhatyurdakul.todo.app.ui.main.adapter.MyFragmentPagerAdapter
import kotlinx.android.synthetic.main.fragment_main.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MainFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MainFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var pagerAdapter = MyFragmentPagerAdapter(fragmentManager!!)
        view_pager.adapter=pagerAdapter
        tabLayout.setupWithViewPager(view_pager)
        setTabs()
        view_pager.setCurrentItem(0,false)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener
        {
            override fun onTabSelected(tab: TabLayout.Tab) {
               if (tab.position==0)
               {
                   collapsing_toolbar_appBarLayout.setExpanded(true,true)
                   (activity as MainActivity).runOnUiThread {
                       (activity as MainActivity).appBarLayout?.setExpanded(true,true)
                   }
                   actions_layout.visibility=View.GONE
               }else
               {
                   (activity as MainActivity).runOnUiThread {
                       (activity as MainActivity).appBarLayout?.setExpanded(false,true)
                   }
                   actions_layout.visibility=View.VISIBLE
               }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {

            }

        }
        )

        action_clear_completed_task.setOnClickListener {
            pagerAdapter.taskFragment?.clearCompletedTasks()

        }
        action_complete_all_task.setOnClickListener {
            pagerAdapter.taskFragment?.completeAllTasks()
        }
        action_show_categories.setOnClickListener {
            pagerAdapter.taskFragment?.adapter?.showCategories()

        }
    }

    fun setTabs()
    {
            activity?.runOnUiThread {
            var v1= LayoutInflater.from(activity).inflate(R.layout.tab_item_layout,null)
            var v2= LayoutInflater.from(activity).inflate(R.layout.tab_item_layout,null)
            v1.findViewById<TextView>(R.id.tabTitle).text="TAKVİM"
            v2.findViewById<TextView>(R.id.tabTitle).text="GÖREVLER"
            v1.findViewById<TextView>(R.id.tabBadge).visibility= View.GONE
            v2.findViewById<TextView>(R.id.tabBadge).visibility= View.GONE
            tabLayout?.getTabAt(0)?.customView = v1
            tabLayout?.getTabAt(1)?.customView = v2
            }




    }
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MainFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MainFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}