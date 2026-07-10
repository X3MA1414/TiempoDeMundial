package com.tiempodemundial.radiodelay

import android.app.Application
import com.tiempodemundial.radiodelay.di.AppContainer

class RadioDelayApplication : Application() {
    val container: AppContainer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppContainer(applicationContext)
    }
}
