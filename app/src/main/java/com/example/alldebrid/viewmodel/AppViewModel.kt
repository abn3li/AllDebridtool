package com.example.alldebrid.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.viewModelScope
import com.example.alldebrid.BuildConfig
import com.example.alldebrid.data.AllDebridRepository
import com.example.alldebrid.data.ApiResult
import com.example.alldebrid.data.Magnet
import com.example.alldebrid.data.MagnetLink
import com.example.alldebrid.data.PreferencesManager
import com.example.alldebrid.data.ThemeMode
import com.example.alldebrid.data.UserInfo
import com.example.alldebrid.data.HostInfo
import com.example.alldebrid.data.LinkUnlockResponse
import com.example.alldebrid.data.SavedLink
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

    private val _historyLinks = MutableStateFlow<List<SavedLink>>(emptyList())

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
        val seenLinks = HashSet<String>()
        val seenByName = HashMap<String, MutableList<Int>>()

        fun isNameDuplicate(fileName: String, size: Long?): Boolean {
            val candidates = seenByName[fileName.lowercase()] ?: return false
            return candidates.any { idx ->
                val existing = allFiles[idx]
                size == null || existing.size == size || existing.size == 0L || size == 0L
            }
        }

        fun addFile(file: DownloadableFile) {
            allFiles.add(file)
            seenLinks.add(file.link)
            seenByName.getOrPut(file.fileName.lowercase()) { mutableListOf() }.add(allFiles.lastIndex)
        }

        // 1. Files from active magnets
        for (magnet in magnets) {
            for (file in flattenLinks(
                links = magnet.links,
                magnetId = magnet.id,
                magnetName = magnet.filename ?: "Unknown"
            )) {
                addFile(file)
            }
        }

        // 2. Saved Links (Library)
        for (saved in savedLinks) {
            val link = saved.link
            val fileName = saved.filename ?: "Saved File"
            val size = saved.size
            if (!link.isNullOrBlank() && link !in hiddenLinks) {
                val isDuplicate = link in seenLinks || isNameDuplicate(fileName, size)
                if (!isDuplicate) {
                    addFile(
                        DownloadableFile(
                            magnetId = null,
                            magnetName = null,
                            fileName = fileName,
                            size = size,
                            link = link,
                            source = FileSource.SAVED
                        )
                    )
                }
            }
        }

        // 3. History Links (Whole Library)
        for (hist in historyLinks) {
            val link = hist.link
            val fileName = hist.filename ?: "History File"
            val size = hist.size
            if (!link.isNullOrBlank() && link !in hiddenLinks) {
                val isDuplicate = link in seenLinks || isNameDuplicate(fileName, size)
                if (!isDuplicate) {
                    addFile(
                        DownloadableFile(
                            magnetId = null,
                            magnetName = null,
                            fileName = fileName,
                            size = size,
                            link = link,
                            source = FileSource.HISTORY
                        )
                    )
                }
            }
        }
        allFiles
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var pollingStarted = false
    private var lastLibraryRefresh = 0L
    private val _appInForeground = MutableStateFlow(true)

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            _appInForeground.value = true
        }
        override fun onStop(owner: LifecycleOwner) {
            _appInForeground.value = false
        }
    }

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
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
        if (shouldRefreshFull) {
            lastLibraryRefresh = now
            refreshUserInfo()
        }
        refreshMagnets(full = shouldRefreshFull)
    }

    fun refreshUserInfo() {
        val repo = repository ?: return
        viewModelScope.launch {
            when (val result = repo.verifyUser()) {
                is ApiResult.Success -> _user.value = result.data
                is ApiResult.Failure -> { /* maybe logged out or network error */ }
            }
        }
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
        val trimmed = url.trim()
        if (trimmed.contains("debrid.it", ignoreCase = true)) {
            val fakeResponse = LinkUnlockResponse(
                link = trimmed,
                filename = trimmed.substringAfterLast("/").ifBlank { "File" },
                filesize = 0L,
                host = "debrid.it"
            )
            val currentHistory = _unlockedHistory.value.toMutableList()
            currentHistory.add(0, fakeResponse)
            _unlockedHistory.value = currentHistory
            onSuccess(fakeResponse)
            return
        }
        viewModelScope.launch {
            onUnlocking(true)
            _uploadMessage.value = null
            when (val result = repo.unlockLink(trimmed)) {
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
            _savedLinks.value = emptyList()
            _historyLinks.value = emptyList()
            _unlockedHistory.value = emptyList()
            deletingIds.clear()
            deletingLinks.clear()
            pollingStarted = false
            _loginState.value = LoginState.LOGGED_OUT
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefs.saveThemeMode(mode) }
    }

    fun submitMagnet(magnetInput: String) {
        val repo = repository ?: return
        if (magnetInput.isBlank()) {
            _uploadMessage.value = "Paste a magnet link first"
            return
        }
        viewModelScope.launch {
            _uploadInProgress.value = true
            _uploadMessage.value = null

            val links = magnetInput.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }

            if (links.isEmpty()) {
                _uploadMessage.value = "Paste a magnet link first"
            } else {
                val addedIds = mutableListOf<Long>()
                for (link in links) {
                    val id = uploadSingleMagnet(repo, link)
                    if (id != null) addedIds.add(id)
                }
                _uploadMessage.value = if (addedIds.isNotEmpty()) "Successfully added ${addedIds.size} magnet(s)" else "Failed to add magnet(s)"
                refreshMagnetsInternal(repo, full = true)
                val allReady = addedIds.isNotEmpty() && addedIds.all { id ->
                    _magnets.value.find { it.id == id }?.let { it.statusCode == 4 || it.status?.equals("Ready", ignoreCase = true) == true } == true
                }
                if (!allReady) {
                    delay(3.seconds)
                    refreshMagnetsInternal(repo, full = true)
                }
            }
            _uploadInProgress.value = false
        }
    }

    private suspend fun uploadSingleMagnet(repo: AllDebridRepository, magnetLink: String): Long? {
        return when (val result = repo.uploadMagnet(magnetLink)) {
            is ApiResult.Success -> {
                val data = result.data
                val newMagnet = Magnet(
                    id = data.id,
                    filename = data.name ?: "New Magnet",
                    size = data.size ?: 0L,
                    hash = data.hash,
                    status = if (data.ready == true) "Ready" else "Downloading",
                    statusCode = if (data.ready == true) 4 else 1,
                    downloaded = 0L,
                    uploaded = 0L,
                    seeders = 0,
                    downloadSpeed = 0L,
                    processingPerc = if (data.ready == true) 100.0 else 0.0,
                    uploadDate = System.currentTimeMillis() / 1000L,
                    completionDate = null,
                    links = emptyList()
                )
                val currentList = _magnets.value.toMutableList()
                if (newMagnet.id != null && currentList.none { it.id == newMagnet.id }) {
                    currentList.add(0, newMagnet)
                    _magnets.value = currentList
                }
                newMagnet.id
            }
            is ApiResult.Failure -> {
                null
            }
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
            refreshMagnetsInternal(repo, full)
        }
    }

    private suspend fun refreshMagnetsInternal(repo: AllDebridRepository, full: Boolean) {
        coroutineScope {
            if (BuildConfig.DEBUG) Log.d("AppViewModel", "--- START POLL / REFRESH MAGNETS (full=$full) ---")
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
                val rawFetched = magnetsResult.data
                if (BuildConfig.DEBUG) {
                    Log.d("AppViewModel", "RAW /v4.1/magnet/status returned ${rawFetched.size} magnet(s):")
                    rawFetched.forEach { m ->
                        Log.d("AppViewModel", "  -> ID: ${m.id} | Name: '${m.filename}' | status (string): '${m.status}' | statusCode (int): ${m.statusCode} | seeders: ${m.seeders} | speed: ${m.downloadSpeed}")
                    }

                    // Force dump ID-to-status map for debugging
                    val idStatusMap = rawFetched.associate { it.id to "status='${it.status}', statusCode=${it.statusCode}" }
                    Log.d("AppViewModel", "DEBUG MAP (ID -> status): $idStatusMap")
                }

                val currentMagnets = _magnets.value
                val fetchedMagnets = rawFetched.filter { it.id !in deletingIds }

                val enrichedMagnets = fetchedMagnets.map { fetched ->
                    async {
                        try {
                            val id = fetched.id
                            // Authoritative check: statusCode == 4 or status == "Ready"
                            val isReady = fetched.statusCode == 4 || fetched.status?.equals("Ready", ignoreCase = true) == true

                            if (BuildConfig.DEBUG) Log.d("AppViewModel", "Evaluating Magnet ID $id: isReady=$isReady (statusCode=${fetched.statusCode}, status='${fetched.status}')")

                            if (id != null && isReady) {
                                val existing = currentMagnets.find { it.id == id }
                                // If we already unlocked links for this ready magnet, reuse them
                                if (existing != null && !existing.links.isNullOrEmpty() && existing.links.all { !it.link.isNullOrBlank() && it.link.startsWith("http") }) {
                                    if (BuildConfig.DEBUG) Log.d("AppViewModel", "Using already unlocked direct links for Ready Magnet ID $id")
                                    existing
                                } else {
                                    if (BuildConfig.DEBUG) Log.d("AppViewModel", "Magnet ID $id is READY. Fetching details & automatically unlocking links via /v4/link/unlock...")
                                    val detailResult = when {
                                        !fetched.links.isNullOrEmpty() -> ApiResult.Success(fetched)
                                        else -> repo.getMagnetDetails(id)
                                    }

                                    if (detailResult is ApiResult.Success) {
                                        val rawLinks = detailResult.data.links ?: emptyList()

                                        val unlockedLinks = rawLinks.map { ml ->
                                            async {
                                                val intermediateUrl = ml.link
                                                if (intermediateUrl.isNullOrBlank()) {
                                                    ml
                                                } else if (intermediateUrl.contains("debrid.it", ignoreCase = true)) {
                                                    if (BuildConfig.DEBUG) Log.d("AppViewModel", "Link already direct (debrid.it): $intermediateUrl")
                                                    ml
                                                } else {
                                                    if (BuildConfig.DEBUG) Log.d("AppViewModel", "-> REQUEST /v4/link/unlock?link=$intermediateUrl")
                                                    when (val unlockResult = repo.unlockLink(intermediateUrl)) {
                                                        is ApiResult.Success -> {
                                                            val directUrl = unlockResult.data.link
                                                            val resolvedName = unlockResult.data.filename ?: ml.filename ?: fetched.filename ?: "File"
                                                            val resolvedSize = unlockResult.data.filesize ?: ml.size
                                                            if (BuildConfig.DEBUG) Log.d("AppViewModel", "<- SUCCESS /v4/link/unlock: directUrl=$directUrl, filename=$resolvedName")
                                                            MagnetLink(
                                                                link = directUrl,
                                                                filename = resolvedName,
                                                                size = resolvedSize,
                                                                elements = ml.elements
                                                            )
                                                        }
                                                        is ApiResult.Failure -> {
                                                            if (BuildConfig.DEBUG) Log.e("AppViewModel", "<- FAILED /v4/link/unlock for $intermediateUrl: ${unlockResult.message}")
                                                            ml
                                                        }
                                                    }
                                                }
                                            }
                                        }.awaitAll()
                                        fetched.copy(links = unlockedLinks)
                                    } else {
                                        Log.e("AppViewModel", "Failed to fetch details for Ready Magnet ID $id")
                                        fetched
                                    }
                                }
                            } else {
                                fetched
                            }
                        } catch (e: Exception) {
                            Log.e("AppViewModel", "Exception enriching magnet ID ${fetched.id}: ${e.localizedMessage}", e)
                            fetched
                        }
                    }
                }.awaitAll()

                val sorted = enrichedMagnets.sortedByDescending { it.uploadDate ?: 0L }
                _magnets.value = sorted.toList() // Ensure new reference to trigger StateFlow emission
                if (BuildConfig.DEBUG) Log.d("AppViewModel", "--- END POLL / REFRESH MAGNETS ---")
            }
        }
    }

    private fun startPollingIfNeeded() {
        if (pollingStarted) return
        pollingStarted = true
        viewModelScope.launch {
            while (_loginState.value == LoginState.LOGGED_IN) {
                delay(5.seconds)
                // Skip polling while the app is backgrounded to save battery/network.
                if (_appInForeground.value) {
                    // Polling only refreshes magnets by default now (internal logic handles frequency)
                    refreshLibrary(force = false)
                }
            }
        }
    }

    suspend fun unlockAndGetLink(link: String): String? {
        val repo = repository ?: return null
        if (link.contains("debrid.it", ignoreCase = true)) {
            return link
        }
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
