package com.phoneauthdemo.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.phoneauthdemo.R
import com.phoneauthdemo.util.KEY_COUNTRY_CODE
import com.phoneauthdemo.util.RQ_COUNTRY_CODE
import com.phoneauthdemo.util.getCountryDialCode
import com.phoneauthdemo.util.isValidPhone
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()


    private var isResendEnable = false

    private var handlerForShowHideExpireTime: Handler? = null
    private var runnableForExpireTime: Runnable? = null

    private lateinit var mVerificationId: String

    private var mResendToken: PhoneAuthProvider.ForceResendingToken? = null

    private val mCountryCode: String by lazy {
        getCountryDialCode()
    }

    private val mCallbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks by lazy {
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                Log.d(TAG, "onVerificationCompleted:$credential")

                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                progress.visibility = View.GONE
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                Log.w(TAG, "onVerificationFailed", e)

                if (e is FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    // ...
                } else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    // ...
                }

                // Show a message and update the UI
                // ...
            }

            override fun onCodeSent(verificationId: String?,
                                    token: PhoneAuthProvider.ForceResendingToken?) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                Log.d(TAG, "onCodeSent:" + verificationId!!)

                progress.visibility = View.GONE

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId
                mResendToken = token

                showVerifyOtpScreen()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // set language to english
        mAuth.setLanguageCode("en")

        // init views
        initViews()
    }


    private fun initViews() {

        textCountryCode.text = "+$mCountryCode"

        textCountryCode.setOnClickListener {
            startActivityForResult(Intent(this, CountryCodeListActivity::class.java), RQ_COUNTRY_CODE)
        }

        // handle continue click
        btnContinue.setOnClickListener {
            handlePhoneAuth()
        }
    }

    private fun handlePhoneAuth() {
        // get the phone number and validate it
        val phoneNumber = etPhone.text.toString()

        if (!TextUtils.isEmpty(phoneNumber.trim())) {
            if (isValidPhone(phoneNumber)) {
                initPhoneVerification(phoneNumber)
            } else {
                Toast.makeText(this, getString(R.string.msg_phone_invalid), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, getString(R.string.msg_phone_empty), Toast.LENGTH_SHORT).show()
        }
    }

    private fun initPhoneVerification(phoneNumber: String) {
        progress.visibility = View.VISIBLE

        val mPhone = "+$mCountryCode$phoneNumber"

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                mPhone,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks)         // OnVerificationStateChangedCallbacks
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, { task ->
                    if (task.isSuccessful) {
                        progress.visibility = View.GONE
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithCredential:success")

                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()

                        val user = task.result.user
                        // ...
                    } else {
                        // Sign in failed, display a message and update the UI
                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                        if (task.exception is FirebaseAuthInvalidCredentialsException) {
                            // The verification code entered was invalid
                        }
                    }
                })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RQ_COUNTRY_CODE && resultCode == Activity.RESULT_OK) {
            val countryCode = data?.getStringExtra(KEY_COUNTRY_CODE)

            val arrDial = countryCode?.split(",".toRegex())?.dropLastWhile({ it.isEmpty() })?.toTypedArray()

            if (!TextUtils.isEmpty(countryCode)) textCountryCode.text = "+${arrDial?.get(0)}"
        }
    }

    /**
     * Verify Otp related functions
     */


    private val countDownTimer = object : CountDownTimer(60000, 1000) {
        override fun onFinish() {
            isResendEnable = true
            btnVerify.isEnabled = true
            btnVerify.text = getString(R.string.resend)

            textExpireTime.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_dark))
            textExpireTime.text = "00:00"

            expireTimeHideShow()
        }

        override fun onTick(millisUntilFinished: Long) {
            val timer = millisUntilFinished / 1000

            // set time text
            setTimerText(timer)
        }
    }

    private fun expireTimeHideShow() {
        textExpireTime.visibility = View.VISIBLE
        handlerForShowHideExpireTime = Handler()
        runnableForExpireTime = Runnable {
            textExpireTime.visibility = View.GONE
            expireTimeHideShow()
        }
        handlerForShowHideExpireTime?.postDelayed(runnableForExpireTime, 1000)
    }

    private fun setTimerText(timer: Long) {
        val timeMin = timer / 60
        val timeSec = timer % 60
        textExpireTime?.text = "${String.format("%02d", timeMin.toInt())}:${String.format("%02d", timeSec.toInt())}"
    }

    private fun showVerifyOtpScreen() {
        // hide register screen and show verify screen
        llRegisterPhone.visibility = View.GONE
        llVerify.visibility = View.VISIBLE
        isResendEnable = false

        textExpireTime.setTextColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))

        handlerForShowHideExpireTime?.removeCallbacks(runnableForExpireTime)

        // start the count down timer for 2 min
        countDownTimer.start()

        // disable verify button
        btnVerify.text = getString(R.string.verify)
        btnVerify.isEnabled = false

        etOtp.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let {
                    btnVerify.isEnabled = it.isNotEmpty()
                }
            }
        })

        btnVerify.setOnClickListener {
            if (isResendEnable) {
                handlePhoneAuth()
            } else {
                val credential = PhoneAuthProvider.getCredential(mVerificationId, etOtp.text.toString())
                progress.visibility = View.VISIBLE
                // call sign in
                signInWithPhoneAuthCredential(credential)
            }
        }
    }
}
