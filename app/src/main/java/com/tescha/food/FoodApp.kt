package com.tescha.food

import android.app.Application
import com.tescha.food.di.AppContainer

class FoodApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
