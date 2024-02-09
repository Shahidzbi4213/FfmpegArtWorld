package com.gulehri.dummyproject

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.ThumbnailUtils
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
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
    private var isPauseDueToBg = false
    private var lastAudioPositionBeforePause = 0
    private var lastVideoPositionBeforePause = 0

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
                ) { _, returnCode ->

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

        var bitmap:Bitmap? = null
        binding.videoView.apply {
            setVideoURI(videoUri)
        }


        Util.getRealPathFromURI(baseContext, videoUri)?.let {
             bitmap = ThumbnailUtils.createVideoThumbnail(it,ThumbnailUtils.OPTIONS_RECYCLE_INPUT)
            binding.imageView.setImageBitmap(bitmap)
        }

        binding.videoView.setOnCompletionListener { videoPlayer ->
            videoPlayer.stop()
            mediaPlayer.stop()
            binding.imageView.setImageBitmap(bitmap)

        }
    }

    private fun setAudioPlayer() {
        mediaPlayer = MediaPlayer.create(baseContext, audioUri)
        mediaPlayer.start()
        binding.imageView.visibility = View.GONE
        binding.videoView.start()
    }


    override fun onResume() {
        super.onResume()

        if (isPauseDueToBg && ::mediaPlayer.isInitialized) {
            isPauseDueToBg = false
            binding.videoView.apply {
                seekTo(lastVideoPositionBeforePause)
                start()
            }
            mediaPlayer.apply {
                seekTo(lastAudioPositionBeforePause)
                start()
            }

            lastAudioPositionBeforePause = 0
            lastVideoPositionBeforePause = 0
        }
    }


    override fun onPause() {
        super.onPause()

        if (::mediaPlayer.isInitialized) {
            lastAudioPositionBeforePause = mediaPlayer.currentPosition
            lastVideoPositionBeforePause = binding.videoView.currentPosition

            binding.videoView.pause()
            mediaPlayer.pause()
            isPauseDueToBg = true
        }
    }

}