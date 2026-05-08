package com.ramos.marketly.controller

import android.app.Application

class MarketlyApp : Application() {
    companion object {
        lateinit var instance: MarketlyApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}