package com.phoneauthdemo.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.phoneauthdemo.R
import com.phoneauthdemo.util.KEY_COUNTRY_CODE
import kotlinx.android.synthetic.main.activity_country_code_list.*
import kotlinx.android.synthetic.main.item_rv_country_code_list.view.*

class CountryCodeListActivity : AppCompatActivity() {

    val mCountryCodeList: Array<out String> by lazy {
        resources.getStringArray(R.array.DialingCountryCode)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_country_code_list)

        title = "Select Country Code"

        // setup countryCodeList
        setupCountryCodeList()
    }


    private fun setupCountryCodeList() {
        rvCountryCodeList.layoutManager = LinearLayoutManager(this)

        rvCountryCodeList.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))

        rvCountryCodeList.adapter = CountryCodeListAdapter()
    }


    inner class CountryCodeListAdapter : RecyclerView.Adapter<CountryCodeListAdapter.VHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_rv_country_code_list, parent, false)
            return VHolder(view)
        }

        override fun getItemCount(): Int {
            return mCountryCodeList.size
        }

        override fun onBindViewHolder(holder: VHolder, position: Int) {
            holder.textCountryCode?.text = mCountryCodeList[position]

            holder.textCountryCode?.setOnClickListener {
                val returnIntent = Intent()
                returnIntent.putExtra(KEY_COUNTRY_CODE, mCountryCodeList[position])
                setResult(Activity.RESULT_OK, returnIntent)
                finish()
            }
        }

        inner class VHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textCountryCode: TextView? = view.text_country_code
        }
    }
}