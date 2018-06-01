package com.phoneauthdemo.util

import android.content.Context
import android.telephony.TelephonyManager
import com.phoneauthdemo.R


/*
* validate the phone number
* */
fun isValidPhone(phone: String): Boolean {
    return android.util.Patterns.PHONE.matcher(phone).matches()
}

/*
* get country dial code
 */
fun Context.getCountryDialCode(): String {
    var countryDialCode = ""

    val telephonyMngr = this.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    val countryId = telephonyMngr.simCountryIso.toUpperCase()
    val arrCountryCode = this.resources.getStringArray(R.array.DialingCountryCode)
    for (i in arrCountryCode.indices) {
        val arrDial = arrCountryCode[i].split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        if (arrDial[1].trim({ it <= ' ' }) == countryId.trim()) {
            countryDialCode = arrDial[0]
            break
        }
    }
    return countryDialCode
}