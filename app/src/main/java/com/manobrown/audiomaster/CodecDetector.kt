package com.manobrown.audiomaster

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager

object CodecDetector {

    data class CodecReal(
        val codec: String,
        val sampleRate: Int,
        val bitsPerSample: Int,
        val channelMode: String
    )

    fun detectarRota(ctx: Context): String {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return "—"
        val dev = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull { d ->
            d.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            d.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
            d.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
            d.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            d.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ||
            d.type == AudioDeviceInfo.TYPE_HDMI
        } ?: return "—"
        return when (dev.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth A2DP"
            AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> "USB"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Fone com fio"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Alto-falante"
            AudioDeviceInfo.TYPE_HDMI -> "HDMI"
            else -> "—"
        }
    }

    fun detectarCodecBt(ctx: Context): CodecReal {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return naoDisponivel()
        if (!adapter.isEnabled) return naoDisponivel()
        // Em Android 13+ o codec real pode ser lido via BluetoothA2dp.getCodecStatus(device).
        // Isso precisa de um ServiceListener assíncrono; por enquanto retornamos "Não disponível".
        return naoDisponivel()
    }

    private fun naoDisponivel() = CodecReal("Não disponível", 0, 0, "?")
}
