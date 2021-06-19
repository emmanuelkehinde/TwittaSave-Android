/*
 * Copyright (C) 2017 Emmanuel Kehinde
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.emmanuelkehinde.twittasave.activities

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import com.emmanuelkehinde.shared.di.CommonAndroidDIModule
import com.emmanuelkehinde.shared.twitter.model.MediaData
import com.emmanuelkehinde.shared.twitter.model.MediaType
import com.emmanuelkehinde.twittasave.BuildConfig
import com.emmanuelkehinde.twittasave.R
import com.emmanuelkehinde.twittasave.data.TwitterCredentialsDataProvider
import com.emmanuelkehinde.twittasave.extensions.selectedTheme
import com.emmanuelkehinde.twittasave.extensions.showLongToast
import com.emmanuelkehinde.twittasave.extensions.showToast
import com.emmanuelkehinde.twittasave.service.AutoListenService
import com.emmanuelkehinde.twittasave.utils.Constant
import com.emmanuelkehinde.twittasave.utils.EXTRA_AUTO_LISTEN_SERVICE_ON
import com.esafirm.rxdownloader.RxDownloader
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import rx.Observer

class MainActivity : AppCompatActivity() {

    private val autoListenSwitch: SwitchMaterial by lazy { findViewById(R.id.swt_autolisten) }
    private val downloadButton: MaterialButton by lazy { findViewById(R.id.btn_download) }
    private val tweetUrlEditText: TextInputEditText by lazy { findViewById(R.id.txt_tweet_url) }
    private val fileNameEditText: TextInputEditText by lazy { findViewById(R.id.txt_filename) }
    private val toolbar: MaterialToolbar by lazy { findViewById(R.id.toolbar) }
    private val sharedPreferences: SharedPreferences by lazy {
        getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
    }

    private val progressDialog: ProgressDialog by lazy {
        ProgressDialog(this).apply {
            setCancelable(false)
            isIndeterminate = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        autoListenSwitch.isChecked = intent.getBooleanExtra(EXTRA_AUTO_LISTEN_SERVICE_ON, false)

        if (!isStoragePermissionGranted()) { // Permission not granted, so prompt the user
            ActivityCompat.requestPermissions(this, Constant.PERMISSIONS_STORAGE, Constant.REQUEST_EXTERNAL_STORAGE)
        }

        if (intent.action == Intent.ACTION_SEND && intent.type != null && intent.type == "text/plain") {
            handleSharedText(intent)
        }

        downloadButton.setOnClickListener {
            onDownloadButtonClicked()
        }

        autoListenSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startAutoService()
            } else {
                stopAutoService()
            }
        }
    }

    private fun onDownloadButtonClicked() {
        val tweetUrl = tweetUrlEditText.text.toString() // https://twitter.com/ManUtd/status/1371250019425251328
        if (tweetUrl.isEmpty() || !tweetUrl.contains("twitter.com/")) {
            showLongToast("Enter a valid tweet link")
            return
        }

        progressDialog.setMessage("Fetching tweet...")
        showProgressDialog()

        val commonDIModule = CommonAndroidDIModule(TwitterCredentialsDataProvider)
        val twitterClient = commonDIModule.twitterClient
        twitterClient.getMediaData(
            tweetUrl = tweetUrl,
            onSuccess = { mediaData ->
                hideProgressDialog()
                val fileName = getFileName(mediaData)
                downloadVideo(mediaData.downloadLink, fileName)
            },
            onError = { throwable ->
                hideProgressDialog()
                showLongToast(throwable.message)
            }
        )
    }

    private fun getFileName(mediaData: MediaData): String {
        var fileName = fileNameEditText.text.toString()
        if (fileName.isEmpty()) fileName = mediaData.tweetId
        return if (mediaData.mediaType == MediaType.VIDEO) {
            "$fileName.mp4"
        } else {
            "$fileName.gif"
        }
    }

    private fun downloadVideo(url: String, fileName: String) {
        if (fileName.endsWith(".mp4")) {
            progressDialog.setMessage("Fetching video...")
        } else {
            progressDialog.setMessage("Fetching gif...")
        }

        // Check if External Storage permission is allowed
        if (!isStoragePermissionGranted()) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(this, Constant.PERMISSIONS_STORAGE, Constant.REQUEST_EXTERNAL_STORAGE)
            hideProgressDialog()
            showToast("Kindly grant the request and try again")
            return
        }

        RxDownloader.getInstance(this)
            .download(url, fileName, "video/*") // url, filename, and mimeType
            .subscribe(
                object : Observer<String> {
                    override fun onCompleted() {
                        showToast("Download Complete")
                    }

                    override fun onError(e: Throwable) {
                        showLongToast(e.localizedMessage)
                    }

                    override fun onNext(s: String) {
                    }
                }
            )

        hideProgressDialog()
        showLongToast("Download Started: Check Notification")
    }

    // Method handling pasting the tweet url into the field when Sharing the tweet url from the twitter app
    private fun handleSharedText(intent: Intent) {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return

        try {
            if (sharedText.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size > 1) {
                tweetUrlEditText.setText(sharedText.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[4])
            } else {
                tweetUrlEditText.setText(sharedText.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0])
            }
        } catch (e: Exception) {
            Log.d(javaClass.simpleName, "handleSharedText: $e")
        }
    }

    private fun stopAutoService() {
        stopService(Intent(this, AutoListenService::class.java))
    }

    private fun startAutoService() {
        startService(Intent(this, AutoListenService::class.java))
    }

    private fun isStoragePermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permission = ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return permission == PackageManager.PERMISSION_GRANTED
        }

        return true
    }

    private fun showProgressDialog() {
        progressDialog.show()
    }

    private fun hideProgressDialog() {
        progressDialog.hide()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.about) {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        if (item.itemId == R.id.web) {
            val urlIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://twittasave.net")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(urlIntent)
        }

        if (item.itemId == R.id.theme) {
            showThemeOptionsDialog()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun showThemeOptionsDialog() {
        val mapOfModeToThemes = hashMapOf(
            AppCompatDelegate.MODE_NIGHT_YES to getString(R.string.dark),
            AppCompatDelegate.MODE_NIGHT_NO to getString(R.string.light),
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM to getString(R.string.system_default),
        )

        val themesArray = mapOfModeToThemes.values.toTypedArray()
        val currentMode = mapOfModeToThemes[AppCompatDelegate.getDefaultNightMode()]
        val currentModePosition = mapOfModeToThemes.values.indexOf(currentMode)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.select_theme))
            .setSingleChoiceItems(themesArray, currentModePosition) { _: DialogInterface, position: Int ->
                val theme = mapOfModeToThemes.keys.elementAt(position)
                AppCompatDelegate.setDefaultNightMode(theme)
                sharedPreferences.selectedTheme = theme
            }
            .create()
            .show()
    }
}
