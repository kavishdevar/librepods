/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package me.kavishdevar.librepods.billing

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.kavishdevar.librepods.LibrePodsApplication
import me.kavishdevar.librepods.R
import kotlin.time.Duration.Companion.seconds

class FOSSBillingProvider(context: Context): BillingProvider {
    private val appDataRepository = (context.applicationContext as LibrePodsApplication).appDataRepository

    private val _isPremium = MutableStateFlow(appDataRepository.state.value.isPremium)
    override val isPremium: StateFlow<Boolean> = _isPremium

    private val _price = MutableStateFlow(context.getString(R.string.name_your_own_price))
    override val price: StateFlow<String> = _price

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var purchaseJob: Job? = null

    init {
        queryPurchases()
    }

    override fun purchase(activity: Activity) {
        activity.startActivity(
            Intent(Intent.ACTION_VIEW, "https://github.com/sponsors/kavishdevar".toUri())
        )

        purchaseJob?.cancel()

        purchaseJob = scope.launch {
            delay(5.seconds)
            _isPremium.value = true
            appDataRepository.updateState { it.copy(isPremium = true) }
        }
    }

    override fun queryPurchases() {
        _isPremium.value = appDataRepository.state.value.isPremium
    }

    override fun restorePurchases() {
        _isPremium.value = true
        appDataRepository.updateState { it.copy(isPremium = true) }
    }
}
