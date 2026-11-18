package dev.lifeos.android

import android.app.Application
import dev.lifeos.data.Repository
import dev.lifeos.data.createDatabase

class LifeOsApp : Application() {
    val repository: Repository by lazy { Repository(createDatabase(this)) }
}
