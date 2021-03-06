// Authors     : Luis Fernando
//               Kevin Legarreta
//               David J. Ortiz Rivera
//               Enrique Rodriguez
//
// File        : VideoActivity.kt
// Description : Takes video or grabs from gallery
//               and uploads it to server
// Copyright © 2018 Los Duendes Malvados. All rights reserved.

package com.example.spider.grafia

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_video.*
import java.lang.Exception
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.os.AsyncTask
import android.provider.DocumentsContract
import android.support.v7.app.AlertDialog
import java.io.*
import java.net.URL


class VideoActivity : AppCompatActivity() {

    // Global variables
    private var mCurrentVideoPath = ""
    private var mCurrentVideoUri : Uri = Uri.EMPTY
    private var mCurrentPickedVideo = ""
    private var mCurrentPickedVideoName = ""
    private var saved = false
    private var userId = -1
    private var projectId = -1
    private var restart = false
    private var projectPath = ""

    // Method to show an alert dialog with yes, no and cancel button
    private fun showInternetNotification(mContext: Context){
        // Late initialize an alert dialog object
        lateinit var dialog: AlertDialog


        // Initialize a new instance of alert dialog builder object
        val builder = AlertDialog.Builder(mContext)

        // Set a title for alert dialog
        builder.setTitle("Lost Internet Connection.")

        // Set a message for alert dialog
        builder.setMessage("Do you want to log out or retry?")


        // On click listener for dialog buttons
        val dialogClickListener = DialogInterface.OnClickListener{ _, which ->
            when(which){
                DialogInterface.BUTTON_POSITIVE -> {

                    val intent = Intent(mContext, LoginActivity::class.java)
                    intent.putExtra("Failed",true)
                    mContext.startActivity(intent)
                }
                DialogInterface.BUTTON_NEGATIVE -> {
                    finish()
                    startActivity(intent)
                }
            }
        }

        // Set the alert dialog positive/yes button
        builder.setPositiveButton("Log Out",dialogClickListener)

        // Set the alert dialog negative/no button
        builder.setNegativeButton("Retry",dialogClickListener)

        // Initialize the AlertDialog using builder object
        dialog = builder.create()

        // Finally, display the alert dialog
        dialog.show()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        videoView.visibility = View.INVISIBLE

        // get user data
        userId = intent.getIntExtra("userId", -1)
        projectId = intent.getIntExtra("pId",-1)
        projectPath = intent.getStringExtra("projectPath")
        val name = intent.getStringExtra("projectName")


        // Segue
        backToProject2.setOnClickListener {
            finish()
            if ((mCurrentVideoPath != "") and !saved) {
                val myFile = File(mCurrentVideoPath)
                myFile.delete()
                mCurrentVideoPath = ""
            }

            val intent = Intent(this@VideoActivity, ProjectActivity::class.java)
            // To pass any data to next activity
            intent.putExtra("userId", userId)
            intent.putExtra("pId", projectId)
            intent.putExtra("projectName",name)
            // start your next activity
            startActivity(intent)
        }

        // open video with intent
        openVideo.setOnClickListener {

            if ((mCurrentVideoPath != "") and !saved) {
                val myFile = File(mCurrentVideoPath)
                myFile.delete()
                mCurrentVideoPath = ""
                saved = false
            }

            mCurrentPickedVideo = ""
            mCurrentPickedVideoName = ""

            dispatchTakeVideoIntent()
        }

        // open video gallery
        openVideoGallery.setOnClickListener {

            dispatchPicVideoIntent()
        }

        // save video to gallery
        saveVideo.setOnClickListener {
            if (mCurrentVideoPath != "" && !saved) {
                galleryAddVideo()
            } else {
                Toast.makeText(this, "Nothing to Save.", Toast.LENGTH_SHORT).show()
            }
        }

        // play video
        play.setOnClickListener {
            if ((mCurrentVideoUri != Uri.EMPTY) and restart) {
                videoView.setVideoURI((mCurrentVideoUri) as Uri)
                restart = false
            }
            videoView.requestFocus()
            videoView.start()
        }
        // pause video
        pause.setOnClickListener {
            videoView.pause()
        }

        // stop video
        stop.setOnClickListener {
            videoView.stopPlayback()
            restart = true
        }

        // upload video
        uploadVideo.setOnClickListener {
            if(isNetworkAvailable()) {
                if (!mCurrentPickedVideo.isNullOrBlank() || !mCurrentVideoPath.isNullOrBlank()) {
                    UploadFileAsync(projectPath).execute("")
                } else{
                    Toast.makeText(this@VideoActivity, "Nothing to upload.", Toast.LENGTH_SHORT).show()
                }
            } else
                showInternetNotification(this@VideoActivity)
        }
    }

    // takes intent to open camera to record
    private fun dispatchTakeVideoIntent() {
        Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? =  try {
                    createTempVideoFile()
                } catch (t: Exception){null}
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this@VideoActivity,
                        "com.example.spider.grafia", it )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)

                    startActivityForResult(takePictureIntent, 1)
                }
            }
        }
    }
    // takes intent to open gallery
    private fun dispatchPicVideoIntent(){

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 2)
        } else {

            val intent = Intent()
            intent.type = "video/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Video"), 2)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // take video
        if (requestCode == 1 && resultCode == RESULT_OK) {
            videoView.visibility = View.VISIBLE

            val videoURI = data?.data
            if (videoURI != null) {
                mCurrentVideoUri = videoURI as Uri
                videoView.setVideoURI(videoURI)
            }

        }
        // takes video from gallery
        else if (requestCode == 2 && resultCode == RESULT_OK){
            videoView.visibility = View.VISIBLE
            if(mCurrentVideoPath != ""){
                val myFile = File(mCurrentVideoPath)
                myFile.delete()
                mCurrentVideoPath = ""
            }
            val videoURI = data?.data
            mCurrentPickedVideo = getPath(this@VideoActivity,videoURI as Uri).toString()

            val timeStamp: String = java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(java.util.Date())
            mCurrentPickedVideoName = "VIDEO_${userId}_${timeStamp}_.mp4"


            if (videoURI != null) {
                mCurrentVideoUri = videoURI
                videoView.setVideoURI(videoURI)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {

                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(
                        this@VideoActivity,
                        "Permission needed to save video.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else{
                    save()
                }
            }

            2 -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(
                        this@VideoActivity,
                        "Permission needed to retrieve video.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val intent = Intent()
                    intent.type = "video/*"
                    intent.action = Intent.ACTION_GET_CONTENT
                    startActivityForResult(Intent.createChooser(intent, "Select Video"), 2)
                }
            }

        }
    }

    private fun createTempVideoFile(): File {
        // Create a temporary video file
        val timeStamp: String = java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(java.util.Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        return File.createTempFile(
            "VIDEO_${userId}_${timeStamp}_", /* prefix */
            ".mp4", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            mCurrentVideoPath = absolutePath
        }
    }

    // check permission and save video
    private fun galleryAddVideo() = if (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED
    ) {

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1
        )
    } else {
        save()
    }

    // detects the path of the video selected
    private fun getPath(context:Context, uri: Uri) : String? {

        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
            if (isDownloadsDocument(uri)) {
                return getDataColumn(context, uri, null, null).toString()
            }
            // MediaProvider
            else
                if (isMediaDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri);
                    val split = docId.split(":");
                    val type = split[0];
                    var contentUri = Uri.EMPTY
                    if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    }
                    val selection = "_id=?";
                    val selectionArgs =  arrayOf(split[1])
                    return getDataColumn(context, contentUri, selection, selectionArgs);
                }
        }
        // MediaStore (and general)
        else if ("content".equals(uri.getScheme(),true)) {
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equals(uri.getScheme(),true)) {
            return uri.getPath();
        }
        return null
    }

    // get path to file
    private fun getDataColumn(context: Context, uri: Uri, selection: String?, selectionArgs: Array<String>?): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)
        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } finally {
            if (cursor != null)
                cursor.close()
        }
        return null
    }


    // return Whether the Uri authority is DownloadsProvider.
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    // return Whether the Uri authority is MediaProvider.
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }


    // return Whether the Uri authority is Google Photos.
    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

    private fun save(){
        // Save video to gallery

        val retriever = MediaMetadataRetriever()
        //use one of overloaded setDataSource() functions to set your data source
        retriever.setDataSource(this, mCurrentVideoUri)
        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val timeInMillisec = time.toLong()
        retriever.release()

        // Save the name and description of a video in a ContentValues map.
        val values = ContentValues(6)
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        values.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
        values.put(MediaStore.Video.Media.DURATION, timeInMillisec)

        // Add a new record (identified by uri) without the video, but with the values just set.

        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)

        // Now get a handle to the file for that record, and save the data into it.
        try {
            val istream = FileInputStream(mCurrentVideoPath)
            val os = contentResolver.openOutputStream(uri!!)
            val buffer = ByteArray(4096) // tweaking this number may increase performance
            var len = istream.read(buffer)
            while (len != -1) {
                os!!.write(buffer, 0, len)
                len = istream.read(buffer)
            }
            os!!.flush()
            istream.close()
            os.close()
        } catch (e: Exception) {}


        sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))

        saved = true

        Toast.makeText(this, "Saved to Gallery.", Toast.LENGTH_SHORT).show()
    }

    // Delete Temps
    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            deleteTempFiles(getExternalFilesDir(Environment.DIRECTORY_MOVIES))
        }
    }

    private fun deleteTempFiles(file: File): Boolean {
        if (file.isDirectory) {
            val files = file.listFiles()
            if (files != null) {
                for (f in files) {
                    if (f.isDirectory) {
                        deleteTempFiles(f)
                    } else {
                        f.delete()
                    }
                }
            }
        }
        return file.delete()
    }

    // Upload Video to server
    private inner class UploadFileAsync(val projectPath: String) : AsyncTask<String, Void, String>() {

        override fun doInBackground(vararg params: String): String {

            var path = ""
            var name = ""

            if (mCurrentVideoPath == "" && mCurrentPickedVideo != "") {
                path = mCurrentPickedVideo
                name = mCurrentPickedVideoName

            } else if (mCurrentVideoPath != "" && mCurrentPickedVideo == "") {
                path = File(mCurrentVideoPath).absolutePath
                name = path.substringAfterLast("/")
            }


            var bool = false
            try {

                val multipart = Multipart(URL("http://54.81.239.120/fUploadAPI.php"))
                multipart.addFormField("fileType", "1")
                multipart.addFormField("path", (projectPath + "/videos/"))
                multipart.addFormField("uid", userId.toString())
                multipart.addFormField("pid", projectId.toString())
                multipart.addFilePart("file", path, name, "video/mp4")

                bool = multipart.upload()
            } catch (e: Exception){
                return "NO"
            }

            if (bool) {
                return "YES"
            } else {
                return "NO"
            }
        }

        override fun onPostExecute(result: String) {

            if (result == "YES") {
                Toast.makeText(this@VideoActivity, "Uploaded!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@VideoActivity, "Try Again. Note: If video is from public download folder or drive it will not upload", Toast.LENGTH_LONG).show()
            }

        }

        override fun onPreExecute() {}

        override fun onProgressUpdate(vararg values: Void) {}
    }
}