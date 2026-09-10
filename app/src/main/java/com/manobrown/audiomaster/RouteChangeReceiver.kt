package com.manobrown.audiomaster

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.util.Log

class RouteChangeReceiver(
    private val ctx: Context,
    private val onChange: () -> Unit
) : BroadcastReceiver() {

    fun start() {
        val f = IntentFilter().apply {
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(Intent.ACTION_HEADSET_PLUG)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                addAction("android.media.action.HDMI_AUDIO_PLUG")
            }
        }
        ctx.registerReceiver(this, f)
    }

    fun stop() {
        try { ctx.unregisterReceiver(this) } catch (_: Throwable) {}
    }

    override fun onReceive(c: Context?, i: Intent?) {
        Log.d("RouteChange", "rota mudou: ${i?.action}")
        onChange()
    }
}
