package com.galerie.app.ui

import android.app.Application
import android.content.Context
import android.content.IntentSender
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.galerie.app.data.Album
import com.galerie.app.data.Media
import com.galerie.app.data.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class SortMode(val label: String) { TAKEN("Date de capture"), ADDED("Date d'ajout"), NAME("Nom"), SIZE("Taille") }
enum class Filter(val label: String) { ALL("Tout"), PHOTOS("Photos"), VIDEOS("Vidéos") }
enum class AlbumSort(val label: String) { RECENT("Plus récents"), NAME("Nom"), COUNT("Nombre d'éléments"), SIZE("Taille") }
enum class ThemeMode(val label: String) { SYSTEM("Système"), LIGHT("Clair"), DARK("Sombre"), AMOLED("Noir AMOLED") }

enum class SmartKind(val label: String, val short: String) {
    FAVORITES("Favoris", "Favoris"), VIDEOS("Vidéos", "Vidéos"), SCREENSHOTS("Captures d'écran", "Captures"),
    RECENT("Ajoutés récemment", "Récents"), GIF("GIF", "GIF"), HD("Haute définition", "HD"),
    PANORAMA("Panoramas", "Panos"), LARGE("Gros fichiers", "Lourds"), DUPLICATES("Doublons", "Doublons"),
}

data class Settings(
    val sort: SortMode = SortMode.TAKEN,
    val ascending: Boolean = false,
    val filter: Filter = Filter.ALL,
    val columns: Int = 4,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val autoplay: Boolean = true,
    val loopVideos: Boolean = true,
    val albumSort: AlbumSort = AlbumSort.RECENT,
    val hiddenAlbums: Set<Long> = emptySet(),
    val slideshowSeconds: Int = 3,
    val dynamicColor: Boolean = false,
    val panelLeft: Boolean = false,
)

/** Seuls les réglages qui changent le contenu d'une liste (pas les colonnes, le thème…). */
fun Settings.queryOnly() = Settings(sort = sort, ascending = ascending, filter = filter, hiddenAlbums = hiddenAlbums)

sealed interface Source {
    data object Timeline : Source
    data class AlbumSrc(val bucketIds: Set<Long>) : Source
    data class Smart(val kind: SmartKind) : Source
    data class Search(val query: String) : Source
    data object Trash : Source
}

sealed interface Screen {
    data object Home : Screen
    data class Collection(val source: Source, val title: String) : Screen
    data class Viewer(val source: Source, val startId: Long) : Screen
    data object SettingsScreen : Screen
    data object Cleanup : Screen
    data object Search : Screen
}

sealed interface UiEvent {
    data class Sender(val sender: IntentSender) : UiEvent
    data class Toast(val text: String) : UiEvent
}

private sealed interface PendingOp {
    data class Trash(val n: Int) : PendingOp
    data class Restore(val n: Int) : PendingOp
    data class Delete(val n: Int) : PendingOp
    data object Favorite : PendingOp
    data class Move(val items: List<Media>, val path: String) : PendingOp
    data class Rename(val item: Media, val name: String) : PendingOp
}

class GalleryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MediaRepository(app)
    private val prefs = app.getSharedPreferences("galerie", Context.MODE_PRIVATE)

    val all = MutableStateFlow<List<Media>>(emptyList())
    val trash = MutableStateFlow<List<Media>>(emptyList())
    val loaded = MutableStateFlow(false)
    val settings = MutableStateFlow(readSettings())
    val events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 16)

    // Navigation
    val stack = mutableStateListOf<Screen>(Screen.Home)
    var tab by mutableIntStateOf(0)
    var selection by mutableStateOf<Set<Long>>(emptySet())
    var lastViewedId by mutableStateOf<Long?>(null)

    val timeline: StateFlow<List<Media>> = combine(all, settings.map { it.queryOnly() }.distinctUntilChanged()) { a, s -> resolve(Source.Timeline, a, emptyList(), s) }
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val albums: StateFlow<List<Album>> = combine(all, settings.map { it.albumSort }.distinctUntilChanged()) { a, sort -> buildAlbums(a, sort) }
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val smartCounts: StateFlow<Map<SmartKind, Pair<Int, Long>>> = all.map { a ->
        SmartKind.entries.associateWith { k -> smart(k, a).let { l -> l.size to l.sumOf { it.size } } }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val repoLock = Mutex()
    private var reloadJob: Job? = null
    private var started = false
    private var pending: PendingOp? = null

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = scheduleReload()
    }

    fun start() {
        if (!started) {
            started = true
            runCatching { getApplication<Application>().contentResolver.registerContentObserver(repo.filesUri, true, observer) }
        }
        reload()
    }

    private fun scheduleReload() {
        reloadJob?.cancel()
        reloadJob = viewModelScope.launch { delay(350); reload() }
    }

    fun reload() {
        viewModelScope.launch(Dispatchers.IO) {
            repoLock.withLock {
                val a = repo.load(trashed = false)
                val t = repo.load(trashed = true)
                all.value = a
                trash.value = t
                loaded.value = true
            }
        }
    }

    override fun onCleared() {
        runCatching { getApplication<Application>().contentResolver.unregisterContentObserver(observer) }
    }

    // ---------- Navigation ----------
    fun push(s: Screen) { selection = emptySet(); stack.add(s) }
    fun pop(): Boolean {
        if (stack.size <= 1) return false
        selection = emptySet()
        stack.removeAt(stack.lastIndex)
        return true
    }

    // ---------- Requêtes ----------
    fun byIds(ids: Set<Long>): List<Media> =
        (all.value.asSequence() + trash.value.asSequence()).filter { it.id in ids }.distinctBy { it.id }.toList()

    fun resolve(source: Source, a: List<Media>, t: List<Media>, s: Settings): List<Media> = when (source) {
        Source.Timeline -> sort(
            a.filter {
                it.bucketId !in s.hiddenAlbums && when (s.filter) {
                    Filter.ALL -> true
                    Filter.PHOTOS -> !it.isVideo
                    Filter.VIDEOS -> it.isVideo
                }
            }, s,
        )
        is Source.AlbumSrc -> sort(a.filter { it.bucketId in source.bucketIds }, s)
        is Source.Smart -> when (source.kind) {
            SmartKind.LARGE, SmartKind.DUPLICATES, SmartKind.RECENT -> smart(source.kind, a)
            else -> sort(smart(source.kind, a), s)
        }
        is Source.Search -> sort(search(source.query, a), s)
        Source.Trash -> t.sortedByDescending { it.dateModified }
    }

    private fun sort(l: List<Media>, s: Settings): List<Media> {
        val c: Comparator<Media> = when (s.sort) {
            SortMode.TAKEN -> compareBy { it.date }
            SortMode.ADDED -> compareBy { it.dateAdded }
            SortMode.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            SortMode.SIZE -> compareBy { it.size }
        }
        return l.sortedWith(if (s.ascending) c else c.reversed())
    }

    fun smart(kind: SmartKind, a: List<Media>): List<Media> = when (kind) {
        SmartKind.FAVORITES -> a.filter { it.isFavorite }
        SmartKind.VIDEOS -> a.filter { it.isVideo }
        SmartKind.SCREENSHOTS -> a.filter { it.isScreenshot }
        SmartKind.GIF -> a.filter { it.isGif }
        SmartKind.HD -> a.filter { it.isHd }
        SmartKind.PANORAMA -> a.filter { it.isPanorama }
        SmartKind.RECENT -> {
            val limit = System.currentTimeMillis() - 30L * 86_400_000
            a.filter { it.dateAdded >= limit }.sortedByDescending { it.dateAdded }
        }
        SmartKind.LARGE -> a.filter { it.size >= 20L * 1024 * 1024 }.sortedByDescending { it.size }
        // Copies : même taille, mêmes dimensions, même type. On garde le plus ancien.
        SmartKind.DUPLICATES -> a.filter { it.size > 0 }
            .groupBy { Triple(it.size, it.pixels, it.mime) }
            .values.filter { it.size > 1 }
            .flatMap { g -> g.sortedBy { it.dateAdded }.drop(1) }
            .sortedByDescending { it.size }
    }

    private var indexSource: List<Media>? = null
    private var index: List<Pair<Media, String>> = emptyList()

    @Synchronized
    private fun searchIndex(a: List<Media>): List<Pair<Media, String>> {
        if (indexSource !== a) {
            index = a.map { m ->
                val words = buildString {
                    append(m.name).append(' ').append(m.bucketName).append(' ').append(m.relativePath).append(' ')
                    append(Format.searchDate(m.date)).append(' ')
                    append(if (m.isVideo) "video film " else "photo image ")
                    if (m.isGif) append("gif anime ")
                    if (m.isScreenshot) append("capture screenshot ecran ")
                    if (m.isFavorite) append("favori favoris coeur ")
                    if (m.isPanorama) append("panorama ")
                    if (m.isHd) append("hd haute definition 4k ")
                    append(m.mime)
                }
                m to Format.normalize(words)
            }
            indexSource = a
        }
        return index
    }

    fun search(q: String, a: List<Media>): List<Media> {
        val terms = Format.normalize(q).split(' ', ',').filter { it.isNotBlank() }
        if (terms.isEmpty()) return emptyList()
        return searchIndex(a).filter { (_, hay) -> terms.all { hay.contains(it) } }.map { it.first }
    }

    private fun buildAlbums(a: List<Media>, sort: AlbumSort): List<Album> {
        // Fusion par nom : un seul « Camera », un seul « Screenshots »…
        val list = a.groupBy { it.bucketName.trim().lowercase() }.map { (_, items) ->
            val cover = items.maxBy { it.date }
            val ids = items.mapTo(HashSet()) { it.bucketId }
            Album(ids.min(), ids, cover.bucketName, cover.relativePath, cover, items.size, items.sumOf { it.size }, cover.date)
        }
        val sorted = when (sort) {
            AlbumSort.RECENT -> list.sortedByDescending { it.latest }
            AlbumSort.NAME -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            AlbumSort.COUNT -> list.sortedByDescending { it.count }
            AlbumSort.SIZE -> list.sortedByDescending { it.size }
        }
        // Appareil photo et captures toujours en tête, comme une galerie système.
        val pinned = sorted.filter { it.relativePath.equals("DCIM/Camera/", true) } +
            sorted.filter { it.relativePath.contains("Screenshots", true) }
        return pinned.distinctBy { it.id } + sorted.filter { it !in pinned }
    }

    // ---------- Actions ----------
    private fun request(op: PendingOp, sender: () -> IntentSender) {
        runCatching { sender() }
            .onSuccess { pending = op; events.tryEmit(UiEvent.Sender(it)) }
            .onFailure { toast("Action impossible : ${it.message}") }
    }

    fun trashItems(items: List<Media>) { if (items.isNotEmpty()) request(PendingOp.Trash(items.size)) { repo.trashRequest(items, true) } }
    fun restore(items: List<Media>) { if (items.isNotEmpty()) request(PendingOp.Restore(items.size)) { repo.trashRequest(items, false) } }
    fun deleteForever(items: List<Media>) { if (items.isNotEmpty()) request(PendingOp.Delete(items.size)) { repo.deleteRequest(items) } }
    fun emptyTrash() = deleteForever(trash.value)
    fun setFavorite(items: List<Media>, fav: Boolean) { if (items.isNotEmpty()) request(PendingOp.Favorite) { repo.favoriteRequest(items, fav) } }
    fun move(items: List<Media>, path: String) { if (items.isNotEmpty()) request(PendingOp.Move(items, path)) { repo.writeRequest(items) } }
    fun rename(item: Media, name: String) { request(PendingOp.Rename(item, name)) { repo.writeRequest(listOf(item)) } }

    fun copy(items: List<Media>, path: String) {
        selection = emptySet()
        viewModelScope.launch {
            val n = withContext(Dispatchers.IO) { repo.copy(items, path) }
            toast(if (n == items.size) "${Format.plural(n, "élément copié", "éléments copiés")}" else "$n/${items.size} copiés")
        }
    }

    fun onIntentResult(ok: Boolean) {
        val op = pending
        pending = null
        if (!ok || op == null) return
        selection = emptySet()
        when (op) {
            is PendingOp.Trash -> toast("${Format.plural(op.n, "élément déplacé", "éléments déplacés")} dans la corbeille")
            is PendingOp.Restore -> toast("${Format.plural(op.n, "élément restauré", "éléments restaurés")}")
            is PendingOp.Delete -> toast("${Format.plural(op.n, "élément supprimé", "éléments supprimés")} définitivement")
            PendingOp.Favorite -> Unit
            is PendingOp.Move -> viewModelScope.launch {
                val n = withContext(Dispatchers.IO) { repo.move(op.items, op.path) }
                toast(if (n == op.items.size) "${Format.plural(n, "élément déplacé", "éléments déplacés")}"
                else "$n/${op.items.size} déplacés (dossier non autorisé pour ce type de fichier)")
            }
            is PendingOp.Rename -> viewModelScope.launch {
                val ok2 = withContext(Dispatchers.IO) { repo.rename(op.item, op.name) }
                toast(if (ok2) "Renommé" else "Impossible de renommer")
            }
        }
        scheduleReload()
    }

    suspend fun details(m: Media, withLocation: Boolean) = withContext(Dispatchers.IO) { repo.details(m, withLocation) }

    private fun toast(s: String) { events.tryEmit(UiEvent.Toast(s)) }

    // ---------- Réglages ----------
    fun update(f: (Settings) -> Settings) {
        val s = f(settings.value)
        settings.value = s
        prefs.edit()
            .putString("sort", s.sort.name).putBoolean("asc", s.ascending).putString("filter", s.filter.name)
            .putInt("cols", s.columns).putString("theme", s.theme.name).putBoolean("autoplay", s.autoplay)
            .putBoolean("loop", s.loopVideos).putString("albumSort", s.albumSort.name)
            .putStringSet("hidden", s.hiddenAlbums.map { it.toString() }.toSet())
            .putInt("slide", s.slideshowSeconds).putBoolean("dyn", s.dynamicColor).putBoolean("panelLeft", s.panelLeft)
            .apply()
    }

    private fun readSettings(): Settings {
        val d = Settings()
        fun <T : Enum<T>> e(key: String, def: T, values: Array<T>) = values.firstOrNull { it.name == prefs.getString(key, null) } ?: def
        return Settings(
            sort = e("sort", d.sort, SortMode.entries.toTypedArray()),
            ascending = prefs.getBoolean("asc", d.ascending),
            filter = e("filter", d.filter, Filter.entries.toTypedArray()),
            columns = prefs.getInt("cols", d.columns),
            theme = e("theme", d.theme, ThemeMode.entries.toTypedArray()),
            autoplay = prefs.getBoolean("autoplay", d.autoplay),
            loopVideos = prefs.getBoolean("loop", d.loopVideos),
            albumSort = e("albumSort", d.albumSort, AlbumSort.entries.toTypedArray()),
            hiddenAlbums = prefs.getStringSet("hidden", emptySet())!!.mapNotNull { it.toLongOrNull() }.toSet(),
            slideshowSeconds = prefs.getInt("slide", d.slideshowSeconds),
            dynamicColor = prefs.getBoolean("dyn", d.dynamicColor),
            panelLeft = prefs.getBoolean("panelLeft", d.panelLeft),
        )
    }

    fun toggleHidden(ids: Set<Long>) = update {
        it.copy(hiddenAlbums = if (it.hiddenAlbums.containsAll(ids)) it.hiddenAlbums - ids else it.hiddenAlbums + ids)
    }
}
