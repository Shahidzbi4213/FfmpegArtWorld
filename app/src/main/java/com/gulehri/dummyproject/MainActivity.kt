package com.gulehri.dummyproject

import android.Manifest
import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.Surface
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS
import com.arthenica.mobileffmpeg.ExecuteCallback
import com.arthenica.mobileffmpeg.FFmpeg
import com.gulehri.dummyproject.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var videoUri: Uri
    private lateinit var audioUri: Uri
    private lateinit var mediaPlayer: MediaPlayer

    private val videoLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) {
            it?.let {
                videoUri = it
                setVideoPlayer()
            }
        }

    private val audioLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
        it?.let {
            audioUri = it
            setAudioPlayer()
        }
    }

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO),
            11
        )

        binding.btnVideo.setOnClickListener {
            videoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        }

        binding.btnAudio.setOnClickListener {
            audioLauncher.launch("audio/*")
        }

        binding.btnStyle.setOnClickListener {

            val videoFilePath = Util.getRealPathFromURI(baseContext, videoUri)
            val audioFilePath = Util.getRealPathFromURI(baseContext, audioUri)
            val output = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "dummy${System.currentTimeMillis()}.mp4"
            )

            CoroutineScope(Dispatchers.IO).launch {


                FFmpeg.executeAsync(
                    "-i $videoFilePath -i $audioFilePath -map 0:v -map 1:a -c:v copy -shortest $output"
                ) { executionId, returnCode ->

                    if (returnCode == RETURN_CODE_SUCCESS) {
                        showToast()
                    }

                }
            }
        }
    }

    private fun showToast() {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(baseContext, "Done", Toast.LENGTH_SHORT).show()

        }
    }

    private fun setVideoPlayer() {
        binding.videoView.setVideoURI(videoUri)
        binding.videoView.setOnCompletionListener {
            it.stop()
            mediaPlayer.stop()
        }
    }

    private fun setAudioPlayer() {
        mediaPlayer = MediaPlayer.create(baseContext, audioUri)
        mediaPlayer.start()
        binding.videoView.start()
    }


}