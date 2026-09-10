package com.manobrown.audiomaster

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.audio.AudioProcessor
import com.google.android.exoplayer2.audio.BaseAudioProcessor
import com.google.android.exoplayer2.audio.DefaultRenderersFactory
import com.google.android.exoplayer2.audio.SilenceSkippingAudioProcessor
import com.google.android.exoplayer2.ui.PlayerView
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private val dsp = DspChain()

    private val dspProcessor = object : BaseAudioProcessor() {
        override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
            return inputAudioFormat
        }

        override fun queueInput(inputBuffer: ByteBuffer) {
            val pos = inputBuffer.position()
            val lim = inputBuffer.limit()
            val bytes = lim - pos
            if (bytes <= 0) return

            val frames = bytes / 8 // Float32 stereo = 8 bytes/frame
            val out = replaceOutputBuffer(bytes)
            val src = ByteBuffer.allocate(bytes).order(ByteOrder.nativeOrder())
            src.put(inputBuffer)
            src.flip()
            val pcm = FloatArray(frames * 2)
            src.asFloatBuffer().get(pcm)

            dsp.processar(pcm, frames, currentInputAudioFormat.sampleRate)

            out.asFloatBuffer().put(pcm)
            inputBuffer.position(lim)
            out.position(0); out.limit(bytes)
        }
    }

    private val escolherAudio = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) tocarArquivo(uri)
        else Toast.makeText(this, "Nenhum arquivo selecionado", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        playerView = findViewById(R.id.player_view)

        val audioProcessors = arrayOf<AudioProcessor>(dspProcessor, SilenceSkippingAudioProcessor())

        val renderersFactory = DefaultRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
            .setAudioProcessors(audioProcessors)

        player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(
                com.google.android.exoplayer2.audio.AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        playerView.player = player

        escolherAudio.launch("audio/*")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        // Conectar SeekBars (se existirem na UI futura)
        val seekBarIds = intArrayOf(
            R.id.seek_31, R.id.seek_62, R.id.seek_125, R.id.seek_250, R.id.seek_500,
            R.id.seek_1k, R.id.seek_2k, R.id.seek_4k, R.id.seek_8k, R.id.seek_16k
        )
        seekBarIds.forEachIndexed { indice, id ->
            try {
                val sb = findViewById<SeekBar>(id)
                sb.max = 24; sb.progress = 12
                sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                        if (!fromUser) return
                        dsp.setBanda(indice, (p - 12).toFloat())
                    }
                    override fun onStartTrackingTouch(s: SeekBar?) = Unit
                    override fun onStopTrackingTouch(s: SeekBar?) = Unit
                })
            } catch (t: Throwable) {
                // UI may not have these seekbars yet; ignore
            }
        }
    }

    private fun tocarArquivo(uri: Uri) {
        try {
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.play()
            player.playbackParameters = PlaybackParameters(1.0f)
            Log.i("AudioMaster", "Tocando: $uri")
        } catch (t: Throwable) {
            Log.e("AudioMaster", "Erro ao tocar", t)
            Toast.makeText(this, "Erro ao abrir arquivo: ${t.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        dsp.reset()
        player.release()
        super.onDestroy()
    }
}
