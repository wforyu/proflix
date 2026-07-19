package com.proflix.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proflix.database.datastore.PreferencesManager
import com.proflix.provider.domain.ProviderRepository
import com.proflix.provider.domain.ProviderType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DomainState(
    val anoboyDomain: String = "",
    val samehadakuDomain: String = "",
    val oploverzDomain: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val providerRepository: ProviderRepository
) : ViewModel() {

    val isDarkTheme = preferencesManager.isDarkTheme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val playbackSpeed = preferencesManager.playbackSpeed.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 1f
    )

    val subtitleEnabled = preferencesManager.subtitleEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val _selectedProvider = MutableStateFlow(ProviderType.ANOBOY)
    val selectedProvider: StateFlow<ProviderType> = _selectedProvider.asStateFlow()

    private val _domainState = MutableStateFlow(DomainState())
    val domainState: StateFlow<DomainState> = _domainState.asStateFlow()

    init {
        loadProviderSettings()
    }

    private fun loadProviderSettings() {
        viewModelScope.launch {
            val savedProvider = try {
                preferencesManager.selectedProvider.first()
            } catch (_: Exception) { "ANOBOY" }
            val type = try { ProviderType.valueOf(savedProvider) } catch (_: Exception) { ProviderType.ANOBOY }
            _selectedProvider.value = type
            providerRepository.switchProvider(type)

            val customAnoboy = try { preferencesManager.customDomainAnoboy.first() } catch (_: Exception) { "" }
            val customSamehadaku = try { preferencesManager.customDomainSamehadaku.first() } catch (_: Exception) { "" }
            val customOploverz = try { preferencesManager.customDomainOploverz.first() } catch (_: Exception) { "" }

            if (customAnoboy.isNotBlank()) providerRepository.setCustomDomain(ProviderType.ANOBOY, customAnoboy)
            if (customSamehadaku.isNotBlank()) providerRepository.setCustomDomain(ProviderType.SAMEHADAKU, customSamehadaku)
            if (customOploverz.isNotBlank()) providerRepository.setCustomDomain(ProviderType.OPLOVERZ, customOploverz)

            _domainState.value = DomainState(
                anoboyDomain = customAnoboy.ifBlank { providerRepository.getDefaultDomain(ProviderType.ANOBOY) },
                samehadakuDomain = customSamehadaku.ifBlank { providerRepository.getDefaultDomain(ProviderType.SAMEHADAKU) },
                oploverzDomain = customOploverz.ifBlank { providerRepository.getDefaultDomain(ProviderType.OPLOVERZ) }
            )
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDarkTheme(enabled)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            preferencesManager.setPlaybackSpeed(speed)
        }
    }

    fun setSubtitleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setSubtitleEnabled(enabled)
        }
    }

    fun selectProvider(type: ProviderType) {
        _selectedProvider.value = type
        providerRepository.switchProvider(type)
        viewModelScope.launch {
            preferencesManager.setSelectedProvider(type.name)
        }
    }

    fun updateDomain(type: ProviderType, domain: String) {
        providerRepository.setCustomDomain(type, domain)
        viewModelScope.launch {
            when (type) {
                ProviderType.ANOBOY -> {
                    preferencesManager.setCustomDomainAnoboy(domain)
                    _domainState.value = _domainState.value.copy(anoboyDomain = domain)
                }
                ProviderType.SAMEHADAKU -> {
                    preferencesManager.setCustomDomainSamehadaku(domain)
                    _domainState.value = _domainState.value.copy(samehadakuDomain = domain)
                }
                ProviderType.OPLOVERZ -> {
                    preferencesManager.setCustomDomainOploverz(domain)
                    _domainState.value = _domainState.value.copy(oploverzDomain = domain)
                }
            }
        }
    }
}
