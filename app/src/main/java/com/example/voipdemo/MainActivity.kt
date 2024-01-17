package com.example.voipdemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import com.example.voipdemo.ui.theme.VoipDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VoipDemoTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    VoipDemo(mainActivity = this)
                }
            }
        }
        val PERMISSION_AUDIO = arrayOf(Manifest.permission.RECORD_AUDIO)
        val permission = ActivityCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.RECORD_AUDIO
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this@MainActivity, PERMISSION_AUDIO, 1)
        }
    }

    inner class VoipThread: Thread() {
        val buffer = ByteArray(640)
        var quit_request = false
        var setSpeakphoneOnReq = false
        var setSpeakphoneOn = false

        @SuppressLint("MissingPermission")
        override fun run() {

            Log.i("VoipDemo", "in thread start")

            val audioManager = this@MainActivity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

            if (setSpeakphoneOn != setSpeakphoneOnReq) {
                Log.i("VoipDemo", "setSpeakphoneOn ${setSpeakphoneOnReq}")
                audioManager.isSpeakerphoneOn = setSpeakphoneOnReq
                setSpeakphoneOn = setSpeakphoneOnReq
            }

            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                640
            )

            val audioTrack = AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
                AudioFormat.Builder()
                    .setSampleRate(16000)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build(),
                640,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE);

            audioRecord.startRecording()
            audioTrack.play()

            while(!quit_request) {
                if (setSpeakphoneOn != setSpeakphoneOnReq) {
                    Log.i("VoipDemo", "setSpeakphoneOn ${setSpeakphoneOnReq}")
                    audioManager.isSpeakerphoneOn = setSpeakphoneOnReq
                    setSpeakphoneOn = setSpeakphoneOnReq
                }
                audioRecord.read(buffer, 0, 640)
                audioTrack.write(buffer, 0, 640)
            }

            audioRecord.stop()
            audioTrack.stop()

            audioManager.isSpeakerphoneOn = false
            audioManager.mode = AudioManager.MODE_NORMAL
            Log.i("VoipDemo", "in thread stop")
        }
    }

    var voipThread : VoipThread? = null

    fun startVoipThread(on: Boolean) {
        Log.i("VoipDemo", "thread start")
        voipThread = VoipThread()
        voipThread!!.setSpeakphoneOnReq = on
        voipThread!!.quit_request = false
        voipThread!!.start()
    }

    fun stopVoipThread() {
        voipThread!!.quit_request = true
        voipThread!!.join()
        voipThread = null
        Log.i("VoipDemo", "thread stop")
    }

    fun setSpeakphoneOn(on: Boolean) {
        voipThread?.setSpeakphoneOnReq = on
    }
}

@Composable
fun VoipDemo(modifier: Modifier = Modifier, mainActivity: MainActivity? = null) {
    var started by remember {
        mutableStateOf(false)
    }
    var isSpeakphoneOn by remember {
        mutableStateOf(false)
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    started = true
                    mainActivity?.startVoipThread(isSpeakphoneOn)
                },
                enabled = !started
            ) {
                Text(
                    text = "开始",
                    style = MaterialTheme.typography.displaySmall
                )
            }
            Button(
                onClick = {
                    mainActivity?.stopVoipThread()
                    started = false
                },
                enabled = started
            ) {
                Text(
                    text = "结束",
                    style = MaterialTheme.typography.displaySmall
                )
            }
        }
        Row (
            modifier = Modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "免提",
                style = MaterialTheme.typography.headlineSmall
            )
            Switch(
                checked = isSpeakphoneOn,
                onCheckedChange = {
                    isSpeakphoneOn = !isSpeakphoneOn
                    mainActivity?.setSpeakphoneOn(isSpeakphoneOn
                    )
                }
            )
        }

        Text(
            text = "VOIP测试工具 V1.1",
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    VoipDemoTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            VoipDemo()
        }
    }
}