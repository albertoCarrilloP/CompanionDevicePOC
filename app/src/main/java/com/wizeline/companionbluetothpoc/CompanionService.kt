package com.wizeline.companionbluetothpoc

import android.companion.AssociationInfo
import android.companion.CompanionDeviceService
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.S)
class CompanionService : CompanionDeviceService() {

    override fun onDeviceAppeared(associationInfo: AssociationInfo) {
        super.onDeviceAppeared(associationInfo)
        Log.d("TAG", "onDeviceAppeared: ")

        // start a new activity from background
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}