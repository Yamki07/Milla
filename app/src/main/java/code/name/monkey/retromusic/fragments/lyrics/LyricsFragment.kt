/*
 * Copyright (c) 2026 RetroMusic / Milla Automix Engine
 *
 * Licensed under the GNU General Public License v3
 */
package code.name.monkey.retromusic.fragments.lyrics

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Fade
import code.name.monkey.appthemehelper.common.ATHToolbarActivity
import code.name.monkey.appthemehelper.util.ToolbarContentTintHelper
import code.name.monkey.appthemehelper.util.VersionUtils
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.activities.tageditor.TagWriter
import code.name.monkey.retromusic.adapter.lyrics.LyricsAdapter
import code.name.monkey.retromusic.automix.AudioPlayerHandler
import code.name.monkey.retromusic.databinding.FragmentLyricsBinding
import code.name.monkey.retromusic.extensions.accentColor
import code.name.monkey.retromusic.extensions.materialDialog
import code.name.monkey.retromusic.extensions.openUrl
import code.name.monkey.retromusic.extensions.uri
import code.name.monkey.retromusic.fragments.base.AbsMainActivityFragment
import code.name.monkey.retromusic.glide.BlurTransformation
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.glide.RetroGlideExtension.simpleSongCoverOptions
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.helper.MusicProgressViewUpdateHelper
import code.name.monkey.retromusic.model.AudioTagInfo
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.*
import com.afollestad.materialdialogs.input.input
import com.bumptech.glide.Glide
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.collections.set
import kotlinx.coroutines.Dispatchers

class LyricsFragment : AbsMainActivityFragment(R.layout.fragment_lyrics),
    MusicProgressViewUpdateHelper.Callback {

    private var _binding: FragmentLyricsBinding? = null
    private val binding get() = _binding!!
    private lateinit var song: Song

    private lateinit var normalLyricsLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var editSyncedLyricsLauncher: ActivityResultLauncher<IntentSenderRequest>

    private lateinit var cacheFile: File
    private var syncedLyrics: String = ""
    private lateinit var syncedFileUri: Uri

    private var lyricsType: LyricsType = LyricsType.NORMAL_LYRICS

    // Motor nativo FASE 7: Letras Sincrónicas Dinámicas (Efecto Ola / Centrado Orgánico)
    private lateinit var lyricsAdapter: LyricsAdapter
    private lateinit var centerSmoothScroller: CenterSmoothScroller
    private var currentLyricsList: List<LyricLine> = emptyList()
    private var tickerJob: Job? = null

    private val googleSearchLrcUrl: String
        get() {
            var baseUrl = "http://www.google.com/search?"
            var query = song.title + "+" + song.artistName
            query = "q=" + query.replace(" ", "+") + " lyrics"
            baseUrl += query
            return baseUrl
        }

    private lateinit var updateHelper: MusicProgressViewUpdateHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        normalLyricsLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    FileUtils.copyFileToUri(requireContext(), cacheFile, song.uri)
                }
            }
        editSyncedLyricsLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    requireContext().contentResolver.openOutputStream(syncedFileUri)?.use { os ->
                        (os as FileOutputStream).channel.truncate(0)
                        os.write(syncedLyrics.toByteArray())
                        os.flush()
                    }
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        enterTransition = Fade()
        exitTransition = Fade()
        _binding = FragmentLyricsBinding.bind(view)
        updateHelper = MusicProgressViewUpdateHelper(this, 500, 1000)
        updateTitleSong()

        setupLyricsRecyclerView()
        updateBlurBackground()
        loadLyrics()

        setupWakelock()
        setupViews()
        setupToolbar()
    }

    private fun setupLyricsRecyclerView() {
        lyricsAdapter = LyricsAdapter().apply {
            setWaveColor(accentColor())
            onLyricLineClickListener = { line, _ ->
                MusicPlayerRemote.seekTo(line.timeMs.toInt())
            }
        }
        centerSmoothScroller = CenterSmoothScroller(requireContext())
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = lyricsAdapter
        }
    }

    private fun updateTitleSong() {
        val currentSong = MusicPlayerRemote.currentSong
        
        // Update header texts
        binding.headerTitle.text = currentSong.title
        binding.headerArtist.text = currentSong.artistName
        
        // Load header cover
        Glide.with(this)
            .load(RetroGlideExtension.getSongModel(currentSong))
            .apply(RetroGlideExtension.simpleSongCoverOptions(currentSong))
            .into(binding.headerCover)
    }

    private fun updateBlurBackground() {
        val currentSong = MusicPlayerRemote.currentSong
        val currentArtwork = RetroGlideExtension.getSongModel(currentSong)
        
        // Use strong blur for background
        Glide.with(this)
            .load(currentArtwork)
            .apply(RetroGlideExtension.simpleSongCoverOptions(currentSong))
            .transform(jp.wasabeef.glide.transformations.BlurTransformation(25, 4))
            .into(binding.blurBackground)
    }

    override fun onUpdateProgressViews(progress: Int, total: Int) {
        
        if (::lyricsAdapter.isInitialized && currentLyricsList.isNotEmpty()) {
            val progressMs = progress.toLong()
            lyricsAdapter.updateTime(progressMs)
            
            // Buscar la línea activa actual
            var activeIndex = -1
            for (i in currentLyricsList.indices.reversed()) {
                if (progressMs >= currentLyricsList[i].timeMs) {
                    activeIndex = i
                    break
                }
            }
            
            if (activeIndex != -1 && activeIndex != lyricsAdapter.currentLineIndex) {
                lyricsAdapter.setCurrentLineIndex(activeIndex)
                
                // Desplazamiento suave al centro
                if (::centerSmoothScroller.isInitialized) {
                    centerSmoothScroller.targetPosition = activeIndex
                    binding.recyclerView.layoutManager?.startSmoothScroll(centerSmoothScroller)
                } else {
                    binding.recyclerView.smoothScrollToPosition(activeIndex)
                }
            }
        }
    }

    private fun setupViews() {
        binding.editButton.accentColor()
        binding.editButton.setOnClickListener {
            when (lyricsType) {
                LyricsType.SYNCED_LYRICS -> {
                    editSyncedLyrics()
                }
                LyricsType.NORMAL_LYRICS -> {
                    editNormalLyrics()
                }
            }
        }
        
        // Sync Buttons
        binding.btnSyncMinus.setOnClickListener {
            lyricsAdapter.currentTimeOffsetMs -= 500L
            onUpdateProgressViews(code.name.monkey.retromusic.helper.MusicPlayerRemote.songProgressMillis, code.name.monkey.retromusic.helper.MusicPlayerRemote.songDurationMillis)
        }
        
        binding.btnSyncPlus.setOnClickListener {
            lyricsAdapter.currentTimeOffsetMs += 500L
            onUpdateProgressViews(code.name.monkey.retromusic.helper.MusicPlayerRemote.songProgressMillis, code.name.monkey.retromusic.helper.MusicPlayerRemote.songDurationMillis)
        }
        
        // Translate Button
        binding.btnTranslate.accentColor()
        binding.btnTranslate.setOnClickListener {
            if (currentLyricsList.isNotEmpty()) {
                lifecycleScope.launch {
                    binding.btnTranslate.isEnabled = false
                    try {
                        val translated = TranslationHelper.translateLyrics(currentLyricsList)
                        currentLyricsList = translated
                        lyricsAdapter.submitList(translated)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        binding.btnTranslate.isEnabled = true
                    }
                }
            }
        }
    }

    override fun onPlayingMetaChanged() {
        super.onPlayingMetaChanged()
        updateTitleSong()
        updateBlurBackground()
        loadLyrics()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        updateTitleSong()
        updateBlurBackground()
        loadLyrics()
    }

    private fun updateTitleSong() {
        song = MusicPlayerRemote.currentSong
        if (::lyricsAdapter.isInitialized) {
            lyricsAdapter.setWaveColor(accentColor())
        }
    }

    private fun setupToolbar() {
        mainActivity.setSupportActionBar(binding.toolbar)
        ToolbarContentTintHelper.colorBackButton(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupWakelock() {
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_lyrics, menu)
        ToolbarContentTintHelper.handleOnCreateOptionsMenu(
            requireContext(),
            binding.toolbar,
            menu,
            ATHToolbarActivity.getToolbarBackgroundColor(binding.toolbar)
        )
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_search) {
            openUrl(googleSearchLrcUrl)
        }
        return false
    }

    @SuppressLint("CheckResult")
    private fun editNormalLyrics(lyrics: String? = null) {
        val file = File(song.data)
        val content = lyrics ?: try {
            AudioFileIO.read(file).tagOrCreateDefault.getFirst(FieldKey.LYRICS)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }

        val song = song

        materialDialog().show {
            title(res = R.string.edit_normal_lyrics)
            input(
                hintRes = R.string.paste_lyrics_here,
                prefill = content,
                inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_CLASS_TEXT
            ) { _, input ->
                val fieldKeyValueMap = EnumMap<FieldKey, String>(FieldKey::class.java)
                fieldKeyValueMap[FieldKey.LYRICS] = input.toString()
                
                val isOnline = song.data.startsWith("http://", true) || song.data.startsWith("https://", true) || song.data.startsWith("deezer://", true) || song.data.startsWith("tidal://", true)
                
                if (isOnline) {
                    // Solo guardar offline en caché
                    LyricUtil.writeLrc(song, input.toString())
                    requireActivity().runOnUiThread { loadNormalLyrics() }
                } else {
                    GlobalScope.launch {
                        if (VersionUtils.hasR()) {
                            cacheFile = TagWriter.writeTagsToFilesR(
                                requireContext(), AudioTagInfo(
                                    listOf(song.data), fieldKeyValueMap, null
                                )
                            )[0]
                            val pendingIntent =
                                MediaStore.createWriteRequest(
                                    requireContext().contentResolver,
                                    listOf(song.uri)
                                )

                            normalLyricsLauncher.launch(
                                IntentSenderRequest.Builder(pendingIntent).build()
                            )
                        } else {
                            TagWriter.writeTagsToFiles(
                                requireContext(), AudioTagInfo(
                                    listOf(song.data), fieldKeyValueMap, null
                                )
                            )
                        }
                    }
                }
            }
            positiveButton(res = R.string.save) {
                loadNormalLyrics()
            }
            negativeButton(res = android.R.string.cancel)
        }
    }


    @SuppressLint("CheckResult")
    private fun editSyncedLyrics(lyrics: String? = null) {
        val content = lyrics ?: LyricUtil.getStringFromLrc(LyricUtil.getSyncedLyricsFile(song))

        val song = song
        materialDialog().show {
            title(res = R.string.edit_synced_lyrics)
            input(
                hintRes = R.string.paste_timeframe_lyrics_here,
                prefill = content,
                inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_CLASS_TEXT
            ) { _, input ->
                val fieldKeyValueMap = EnumMap<FieldKey, String>(FieldKey::class.java)
                fieldKeyValueMap[FieldKey.LYRICS] = input.toString()
                
                val isOnline = song.data.startsWith("http://", true) || song.data.startsWith("https://", true) || song.data.startsWith("deezer://", true) || song.data.startsWith("tidal://", true)

                if (isOnline) {
                    LyricUtil.writeLrc(song, input.toString())
                    requireActivity().runOnUiThread { loadLRCLyrics() }
                } else {
                    GlobalScope.launch {
                        if (VersionUtils.hasR()) {
                            cacheFile = TagWriter.writeTagsToFilesR(
                                requireContext(),
                                AudioTagInfo(listOf(song.data), fieldKeyValueMap, null)
                            )[0]
                            val pendingIntent = MediaStore.createWriteRequest(
                                requireContext().contentResolver,
                                listOf(song.uri)
                            )

                            normalLyricsLauncher.launch(
                                IntentSenderRequest.Builder(pendingIntent).build()
                            )
                        } else {
                            TagWriter.writeTagsToFiles(
                                requireContext(),
                                AudioTagInfo(listOf(song.data), fieldKeyValueMap, null)
                            )
                        }
                    }
                }
            }
            positiveButton(res = R.string.save) {
                loadLRCLyrics()
            }
            negativeButton(res = android.R.string.cancel)
        }
    }

    private fun loadNormalLyrics() {
        val file = File(song.data)
        val lyrics = try {
            AudioFileIO.read(file).tagOrCreateDefault.getFirst(FieldKey.LYRICS)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
        
        if (!lyrics.isNullOrEmpty()) {
            val lines = lyrics.split("\n").map { LyricLine(0, it.trim()) }
            currentLyricsList = LrcParser.generateEstimatedTimestamps(lines, song.duration)
            
            if (currentLyricsList.isNotEmpty()) {
                lyricsAdapter.submitList(currentLyricsList)
                binding.recyclerView.isVisible = true
                binding.normalLyrics.isVisible = false
                binding.noLyricsFound.isVisible = false
                return
            }
        }
        
        binding.normalLyrics.isVisible = false
        binding.noLyricsFound.isVisible = true
        binding.recyclerView.isVisible = false
    }

    /**
     * Carga de letras sincronizadas con parser LrcParser 100% offline.
     */
    private fun loadLRCLyrics(): Boolean {
        val lrcFile = LyricUtil.getSyncedLyricsFile(song)
        if (lrcFile != null && lrcFile.exists()) {
            currentLyricsList = LrcParser.parse(lrcFile)
        } else {
            val embeddedLyrics = LyricUtil.getEmbeddedSyncedLyrics(song.data)
            if (embeddedLyrics != null) {
                currentLyricsList = LrcParser.parse(embeddedLyrics)
            } else {
                currentLyricsList = emptyList()
                return false
            }
        }

        return if (currentLyricsList.isNotEmpty()) {
            lyricsAdapter.submitList(currentLyricsList)
            binding.recyclerView.isVisible = true
            binding.normalLyrics.isVisible = false
            binding.noLyricsFound.isVisible = false
            true
        } else {
            binding.recyclerView.isVisible = false
            false
        }
    }

    private fun loadLyrics() {
        lyricsType = if (!loadLRCLyrics()) {
            loadNormalLyrics()
            // Si tampoco encontró letras normales, buscamos en internet
            if (binding.noLyricsFound.isVisible) {
                fetchLyricsFromInternet()
            }
            LyricsType.NORMAL_LYRICS
        } else {
            binding.normalLyrics.isVisible = false
            binding.noLyricsFound.isVisible = false
            binding.recyclerView.isVisible = true
            LyricsType.SYNCED_LYRICS
        }
    }

    private fun fetchLyricsFromInternet() {
        binding.noLyricsFound.text = "Buscando letras en línea..."
        lifecycleScope.launch(Dispatchers.Main) {
            val result = LRCLibFetcher.fetchLyrics(song)
            if (result != null) {
                // Letra encontrada! Guardamos la letra para la próxima vez
                // Detectar si es sincronizada
                val isSynced = result.contains("[00:") || result.contains("[01:") || result.contains("[02:")
                
                if (isSynced) {
                    // Cargar en memoria inmediatamente para que el usuario no espere
                    currentLyricsList = LrcParser.parse(result)
                    if (currentLyricsList.isNotEmpty()) {
                        lyricsAdapter.submitList(currentLyricsList)
                        binding.recyclerView.isVisible = true
                        binding.normalLyrics.isVisible = false
                        binding.noLyricsFound.isVisible = false
                        lyricsType = LyricsType.SYNCED_LYRICS
                        startLyricsTicker()
                        // Intentar guardar en disco silenciosamente
                        LyricUtil.writeLrc(song, result)
                    } else {
                        binding.noLyricsFound.isVisible = false
                        binding.normalLyrics.isVisible = true
                        binding.normalLyrics.text = result
                        lyricsType = LyricsType.NORMAL_LYRICS
                    }
                } else {
                    binding.noLyricsFound.isVisible = false
                    
                    // Render static lyrics with estimated timestamps for wave effect!
                    val lines = result.split("\n").map { LyricLine(0, it.trim()) }
                    currentLyricsList = LrcParser.generateEstimatedTimestamps(lines, song.duration)
                    if (currentLyricsList.isNotEmpty()) {
                        lyricsAdapter.submitList(currentLyricsList)
                        binding.recyclerView.isVisible = true
                        binding.normalLyrics.isVisible = false
                        startLyricsTicker()
                    } else {
                        binding.normalLyrics.isVisible = true
                        binding.normalLyrics.text = result
                    }
                    
                    lyricsType = LyricsType.NORMAL_LYRICS
                    // Opcionalmente guardar como tag ID3 de letra plana
                }
            } else {
                binding.noLyricsFound.text = getString(R.string.no_lyrics_found)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateHelper.start()
        startLyricsTicker()
    }

    override fun onPause() {
        super.onPause()
        updateHelper.stop()
        stopLyricsTicker()
    }

    private fun startLyricsTicker() {
        stopLyricsTicker()
        tickerJob = lifecycleScope.launch {
            while (isActive) {
                if (lyricsType == LyricsType.SYNCED_LYRICS && currentLyricsList.isNotEmpty()) {
                    val currentPos = AudioPlayerHandler.playbackState.position
                    updateSyncLine(currentPos)
                }
                delay(16L) // ~60 FPS
            }
        }
    }

    private fun stopLyricsTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun updateSyncLine(currentPositionMs: Long) {
        if (currentLyricsList.isEmpty()) return

        var activeIndex = -1
        for (i in currentLyricsList.indices) {
            if (currentPositionMs >= currentLyricsList[i].timeMs) {
                activeIndex = i
            } else {
                break
            }
        }

        if (activeIndex != -1 && activeIndex != lyricsAdapter.currentLineIndex) {
            lyricsAdapter.setCurrentLineIndex(activeIndex)
            binding.recyclerView.layoutManager?.let { layoutManager ->
                centerSmoothScroller.targetPosition = activeIndex
                layoutManager.startSmoothScroll(centerSmoothScroller)
            }
        }

        if (activeIndex != -1) {
            val holder = binding.recyclerView.findViewHolderForAdapterPosition(activeIndex) as? LyricsAdapter.LyricViewHolder
            holder?.updateProgress(currentPositionMs)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopLyricsTicker()
        if (MusicPlayerRemote.playingQueue.isNotEmpty())
            mainActivity.expandPanel()
        _binding = null
    }

    enum class LyricsType {
        NORMAL_LYRICS,
        SYNCED_LYRICS
    }
}
