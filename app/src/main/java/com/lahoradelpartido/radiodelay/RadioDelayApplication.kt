package com.lahoradelpartido.radiodelay

import android.app.Application
import com.lahoradelpartido.radiodelay.di.AppContainer

class RadioDelayApplication : Application() {
    val container: AppContainer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppContainer(applicationContext)
    }
}
