package com.example.alldebrid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alldebrid.data.AllDebridRepository
import com.example.alldebrid.data.ApiResult
import com.example.alldebrid.data.Magnet
import com.example.alldebrid.data.MagnetLink
import com.example.alldebrid.data.PreferencesManager
import com.example.alldebrid.data.ThemeMode
import com.example.alldebrid.data.UserInfo
import com.example.alldebrid.data.HostInfo
import com.example.alldebrid.data.HostsResponse
import com.example.alldebrid.data.LinkUnlockResponse
import com.example.alldebrid.data.SavedLink
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

enum class LoginState { CHECKING, LOGGED_OUT, LOGGING_IN, LOGGED_IN, ERROR }

enum class FileSource { MAGNET, SAVED, HISTORY }

data class DownloadableFile(
    val magnetId: Long?,
    val magnetName: String?,
    val fileName: String,
    val size: Long?,
    val link: String,
    val source: FileSource,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)
    private var repository: AllDebridRepository? = null

    private val _loginState = MutableStateFlow(LoginState.CHECKING)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _user = MutableStateFlow<UserInfo?>(null)
    val user: StateFlow<UserInfo?> = _user.asStateFlow()

    private val _magnets = MutableStateFlow<List<Magnet>>(emptyList())
    val magnets: StateFlow<List<Magnet>> = _magnets.asStateFlow()

    private val _savedLinks = MutableStateFlow<List<SavedLink>>(emptyList())
    val savedLinks: StateFlow<List<SavedLink>> = _savedLinks.asStateFlow()

    private val _historyLinks = MutableStateFlow<List<SavedLink>>(emptyList())
    val historyLinks: StateFlow<List<SavedLink>> = _historyLinks.asStateFlow()

    private val _hiddenLinks = MutableStateFlow<Set<String>>(emptySet())

    private val _uploadInProgress = MutableStateFlow(value = false)
    val uploadInProgress: StateFlow<Boolean> = _uploadInProgress.asStateFlow()

    private val _uploadMessage = MutableStateFlow<String?>(null)
    val uploadMessage: StateFlow<String?> = _uploadMessage.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _unlockedHistory = MutableStateFlow<List<LinkUnlockResponse>>(emptyList())
    val unlockedHistory: StateFlow<List<LinkUnlockResponse>> = _unlockedHistory.asStateFlow()

    private val _hosterStats = MutableStateFlow<List<HostInfo>>(emptyList())
    val hosterStats: StateFlow<List<HostInfo>> = _hosterStats.asStateFlow()

    private val _hostsLoading = MutableStateFlow(false)
    val hostsLoading: StateFlow<Boolean> = _hostsLoading.asStateFlow()

    private val deletingIds = mutableSetOf<Long>()
    private val deletingLinks = mutableSetOf<String>()

    private val _readyMagnets = _magnets.map { list ->
        list.filter { it.status?.equals("Ready", ignoreCase = true) == true || it.statusCode == 4 }
    }.distinctUntilChanged()

    val readyFiles: StateFlow<List<DownloadableFile>> = combine(_readyMagnets, _savedLinks, _historyLinks, _hiddenLinks) { magnets, savedLinks, historyLinks, hiddenLinks ->
        val allFiles = mutableListOf<DownloadableFile>()
        
        // 1. Files from active magnets
        for (magnet in magnets) {
            allFiles.addAll(
                flattenLinks(
                    links = magnet.links,
                    magnetId = magnet.id,
                    magnetName = magnet.filename ?: "Unknown"
                )
            )
        }
        
        // 2. Saved Links (Library)
        for (saved in savedLinks) {
            val link = saved.link
            if (!link.isNullOrBlank() && link !in hiddenLinks) {
                allFiles.add(
                    DownloadableFile(
                        magnetId = null,
                        magnetName = null,
                        fileName = saved.filename ?: "Saved File",
                        size = saved.size,
                        link = link,
                        source = FileSource.SAVED
                    )
                )
            }
        }

        // 3. History Links (Whole Library)
        for (hist in historyLinks) {
            val link = hist.link
            if (!link.isNullOrBlank() && link !in hiddenLinks && allFiles.none { it.link == link }) {
                allFiles.add(
                    DownloadableFile(
                        magnetId = null,
                        magnetName = null,
                        fileName = hist.filename ?: "History File",
                        size = hist.size,
                        link = link,
                        source = FileSource.HISTORY
                    )
                )
            }
        }
        allFiles
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var pollingStarted = false
    private var lastLibraryRefresh = 0L

    init {
        viewModelScope.launch {
            _themeMode.value = prefs.themeModeFlow.first()
            val savedKey = prefs.apiKeyFlow.first()
            if (!savedKey.isNullOrBlank()) {
                loginWithKey(savedKey, persist = false)
            } else {
                _loginState.value = LoginState.LOGGED_OUT
            }
        }
        viewModelScope.launch {
            prefs.themeModeFlow.collect { _themeMode.value = it }
        }
        viewModelScope.launch {
            prefs.hiddenLinksFlow.collect { _hiddenLinks.value = it }
        }
    }

    fun refreshLibrary(force: Boolean = false) {
        val now = System.currentTimeMillis()
        val shouldRefreshFull = force || (now - lastLibraryRefresh > 30_000)
        if (shouldRefreshFull) lastLibraryRefresh = now
        refreshMagnets(full = shouldRefreshFull)
    }

    fun refreshHosts() {
        val repo = repository ?: return
        viewModelScope.launch {
            _hostsLoading.value = true
            when (val result = repo.getHosts()) {
                is ApiResult.Success -> {
                    val allHosts = mutableListOf<HostInfo>()
                    // Convert Maps to List
                    result.data.hosts?.values?.let { allHosts.addAll(it) }
                    result.data.streams?.values?.let { allHosts.addAll(it) }
                    result.data.redirectors?.values?.let { allHosts.addAll(it) }
                    _hosterStats.value = allHosts
                        .filter { it.status == true }
                        .sortedBy { it.name?.lowercase() }
                }
                is ApiResult.Failure -> {
                    _uploadMessage.value = result.message
                }
            }
            _hostsLoading.value = false
        }
    }

    fun unlockDirectLink(url: String, onUnlocking: (Boolean) -> Unit, onSuccess: (LinkUnlockResponse) -> Unit) {
        val repo = repository ?: return
        if (url.isBlank()) {
            _uploadMessage.value = "Paste a link first"
            return
        }
        viewModelScope.launch {
            onUnlocking(true)
            _uploadMessage.value = null
            when (val result = repo.unlockLink(url.trim())) {
                is ApiResult.Success -> {
                    val currentHistory = _unlockedHistory.value.toMutableList()
                    currentHistory.add(0, result.data)
                    _unlockedHistory.value = currentHistory
                    onSuccess(result.data)
                }
                is ApiResult.Failure -> {
                    _uploadMessage.value = result.message
                }
            }
            onUnlocking(false)
        }
    }

    fun loginWithKey(apiKey: String, persist: Boolean = true) {
        viewModelScope.launch {
            _loginState.value = LoginState.LOGGING_IN
            _loginError.value = null
            val repo = AllDebridRepository(apiKey)
            when (val result = repo.verifyUser()) {
                is ApiResult.Success -> {
                    repository = repo
                    _user.value = result.data
                    _loginState.value = LoginState.LOGGED_IN
                    if (persist) prefs.saveApiKey(apiKey)
                    startPollingIfNeeded()
                    refreshLibrary(force = true)
                }
                is ApiResult.Failure -> {
                    _loginError.value = result.message
                    _loginState.value = LoginState.ERROR
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            prefs.clearApiKey()
            repository = null
            _user.value = null
            _magnets.value = emptyList()
            pollingStarted = false
            _loginState.value = LoginState.LOGGED_OUT
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefs.saveThemeMode(mode) }
    }

    fun submitMagnet(magnetLink: String) {
        val repo = repository ?: return
        if (magnetLink.isBlank()) {
            _uploadMessage.value = "Paste a magnet link first"
            return
        }
        viewModelScope.launch {
            _uploadInProgress.value = true
            _uploadMessage.value = null
            when (val result = repo.uploadMagnet(magnetLink.trim())) {
                is ApiResult.Success -> {
                    _uploadMessage.value = "Added: ${result.data.name ?: "magnet"}"
                    refreshMagnets()
                }
                is ApiResult.Failure -> {
                    _uploadMessage.value = "Failed: ${result.message}"
                }
            }
            _uploadInProgress.value = false
        }
    }

    fun deleteMagnet(id: Long) {
        val repo = repository ?: return
        viewModelScope.launch {
            deletingIds.add(id)
            _magnets.value = _magnets.value.filter { it.id != id }
            repo.deleteMagnet(id)
        }
    }

    fun deleteSavedLink(link: String) {
        val repo = repository ?: return
        viewModelScope.launch {
            deletingLinks.add(link)
            prefs.addHiddenLink(link) // Persist deletion
            _savedLinks.value = _savedLinks.value.filter { it.link != link }
            val result = repo.deleteSavedLink(link)
            if (result is ApiResult.Failure) {
                deletingLinks.remove(link)
                refreshLibrary(force = true)
            }
        }
    }

    fun deleteHistoryLink(link: String) {
        viewModelScope.launch {
            deletingLinks.add(link)
            prefs.addHiddenLink(link) // Persist deletion locally
            _historyLinks.value = _historyLinks.value.filter { it.link != link }
        }
    }

    fun clearHistory() {
        val repo = repository ?: return
        viewModelScope.launch {
            _historyLinks.value = emptyList()
            prefs.clearHiddenLinks()
            repo.purgeHistory()
        }
    }

    fun refreshMagnets(full: Boolean = false) {
        val repo = repository ?: return
        viewModelScope.launch {
            val magnetsDeferred = async { repo.getAllMagnets() }
            val linksDeferred = if (full) async { repo.getUserLinks() } else null
            val historyDeferred = if (full) async { repo.getUserHistory() } else null

            val magnetsResult = magnetsDeferred.await()
            val linksResult = linksDeferred?.await()
            val historyResult = historyDeferred?.await()

            if (linksResult is ApiResult.Success) {
                _savedLinks.value = linksResult.data.filter { it.link !in deletingLinks }
            }
            if (historyResult is ApiResult.Success) {
                _historyLinks.value = historyResult.data
            }

            if (magnetsResult is ApiResult.Success) {
                val currentMagnets = _magnets.value
                val fetchedMagnets = magnetsResult.data.filter { it.id !in deletingIds }

                val enrichedMagnets = fetchedMagnets.map { fetched ->
                    async {
                        try {
                            val id = fetched.id
                            val isReady = fetched.status?.equals("Ready", ignoreCase = true) == true || fetched.statusCode == 4
                            val hasNoLinks = fetched.links.isNullOrEmpty()

                            if (id != null && isReady && hasNoLinks) {
                                val existing = currentMagnets.find { it.id == id }
                                if (existing != null && !existing.links.isNullOrEmpty()) {
                                    existing
                                } else {
                                    when (val detailResult = repo.getMagnetDetails(id)) {
                                        is ApiResult.Success -> {
                                            // MERGE links into metadata, don't replace metadata
                                            fetched.copy(links = detailResult.data.links)
                                        }
                                        is ApiResult.Failure -> fetched
                                    }
                                }
                            } else {
                                fetched
                            }
                        } catch (e: Exception) {
                            fetched
                        }
                    }
                }.awaitAll()

                _magnets.value = enrichedMagnets.sortedByDescending { it.uploadDate ?: 0L }
            }
        }
    }

    private fun startPollingIfNeeded() {
        if (pollingStarted) return
        pollingStarted = true
        viewModelScope.launch {
            while (_loginState.value == LoginState.LOGGED_IN) {
                delay(5.seconds)
                // Polling only refreshes magnets by default now (internal logic handles frequency)
                refreshLibrary(force = false)
            }
        }
    }

    suspend fun unlockAndGetLink(link: String): String? {
        val repo = repository ?: return null
        return when (val result = repo.unlockLink(link)) {
            is ApiResult.Success -> result.data.link
            is ApiResult.Failure -> {
                if (link.contains("alldebrid.com/")) {
                    link
                } else {
                    _uploadMessage.value = "Could not unlock: ${result.message}"
                    null
                }
            }
        }
    }

    private fun flattenLinks(
        links: List<MagnetLink>?,
        magnetId: Long?,
        magnetName: String
    ): List<DownloadableFile> {
        if (links == null) return emptyList()
        val result = mutableListOf<DownloadableFile>()
        for (link in links) {
            if (!link.link.isNullOrBlank()) {
                result.add(
                    DownloadableFile(
                        magnetId = magnetId,
                        magnetName = magnetName,
                        fileName = link.filename ?: magnetName,
                        size = link.size,
                        link = link.link,
                        source = FileSource.MAGNET
                    )
                )
            }
            link.elements?.let {
                result.addAll(flattenLinks(it, magnetId, magnetName))
            }
        }
        return result
    }
}
