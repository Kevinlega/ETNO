// Authors     : Luis Fernando
//               Kevin Legarreta
//               David J. Ortiz Rivera
//               Enrique Rodriguez
//
// File        : ProjectActivity.kt
// Description : Shows all the files on the server
//               and segue to download and take data
// Copyright © 2018 Los Duendes Malvados. All rights reserved.

package com.example.spider.grafia

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.ConnectivityManager
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_project.*
import java.lang.Exception

import com.google.gson.GsonBuilder
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URL

class ProjectActivity : AppCompatActivity() {
    // Global variables
    private var projectPath = ""
    private var userId = -1
    private var projectId = -1
    private var title = ""


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
        setContentView(R.layout.activity_project)
        title = intent.getStringExtra("projectName")
        supportActionBar!!.title = title

        // Get user data
        userId = intent.getIntExtra("userId",-1)
        projectId = intent.getIntExtra("pId",-1)

        var connectToAPI = Connect(this,1)
        // Check if admin
        if(isNetworkAvailable()) {
            try{
                val url = "http://54.81.239.120/selectAPI.php?queryType=8&pid=$projectId"
                connectToAPI.execute(url)
            }
            catch (error: Exception){}
        } else {
            showInternetNotification(this@ProjectActivity)
        }

        if(isNetworkAvailable()) {
            // Get project path
            connectToAPI = Connect(this, 0)
            try {
                val url = "http://54.81.239.120/selectAPI.php?queryType=10&pid=$projectId"
                connectToAPI.execute(url)
            } catch (error: Exception) {
            }
        } else {
            showInternetNotification(this@ProjectActivity)
        }

        // Segues
        BackToDashboard.setOnClickListener {
            val intent = Intent(this@ProjectActivity, DashboardActivity::class.java)
            // To pass any data to next activity
            intent.putExtra("userId", userId)
            // start your next activity
            startActivity(intent)
        }

        AddUsers.setOnClickListener {
            val intent = Intent(this@ProjectActivity, AddParticipantsActivity::class.java)
            // To pass any data to next activity
            intent.putExtra("userId", userId)
            intent.putExtra("pId", projectId)
            intent.putExtra("projectName",title)
            // start your next activity
            startActivity(intent)
        }

        Camera.setOnClickListener {
            val intent = Intent(this@ProjectActivity, CameraActivity::class.java)
            // To pass any data to next activity
            intent.putExtra("userId", userId)
            intent.putExtra("pId", projectId)
            intent.putExtra("projectPath",projectPath)
            intent.putExtra("projectName",title)
            // start your next activity
            startActivity(intent)
        }

        Voice.setOnClickListener {
            val intent = Intent(this@ProjectActivity, VoiceActivity::class.java)
            // To pass any data to next activity
            intent.putExtra("userId", userId)
            intent.putExtra("pId", projectId)
            intent.putExtra("projectPath",projectPath)
            intent.putExtra("projectName",title)
            // start your next activity
            startActivity(intent)
        }

        Video.setOnClickListener {
            val intent = Intent(this@ProjectActivity, VideoActivity::class.java)
            // To pass any data to next activity
            intent.putExtra("userId", userId)
            intent.putExtra("pId", projectId)
            intent.putExtra("projectPath",projectPath)
            intent.putExtra("projectName",title)
            // start your next activity
            startActivity(intent)
        }

        Notes.setOnClickListener {
            val intent = Intent(this@ProjectActivity, NotesActivity::class.java)
            // To pass any data to next activity
            intent.putExtra("userId", userId)
            intent.putExtra("pId", projectId)
            intent.putExtra("projectPath",projectPath)
            intent.putExtra("projectName",title)
            // start your next activity
            startActivity(intent)
        }
    }

    // fetch files
    private fun fetchJson() {
        if (isNetworkAvailable()) {

            val url = "http://54.81.239.120/listdir.php?path=$projectPath"

            var request = Request.Builder().url(url).build()
            val client = OkHttpClient()
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val body = response.body()?.string()
                    val gson = GsonBuilder().create()
                    val myfiles: myFiles?
                    myfiles = gson.fromJson(body, myFiles::class.java)
                    val listview = findViewById<ListView>(R.id.project_list_files)

                    // list adapter
                    runOnUiThread {


                        listview.adapter = MyCustomAdapter(this@ProjectActivity, myfiles)


                        listview.setOnItemClickListener { parent, view, position, id ->

                            val file = listview.getItemAtPosition(position) as Array<String>

                            var intent = Intent(this@ProjectActivity, ProjectActivity::class.java)

                            when (file[1]) {
                                "voice" -> intent = Intent(this@ProjectActivity, DownloadAudioActivity::class.java)
                                "images" -> intent = Intent(this@ProjectActivity, DownloadImageActivity::class.java)
                                "videos" -> intent = Intent(this@ProjectActivity, DownloadVideoActivity::class.java)
                                "docs" -> intent = Intent(this@ProjectActivity, DownloadNotesActivity::class.java)
                            }

                            var path = projectPath.substringAfter("/var/www/html/")

                            val location = "http://54.81.239.120/" + path + "/" + file[1] + "/" + file[0]

                            // To pass any data to next activity
                            intent.putExtra("userId", userId)
                            intent.putExtra("pId", projectId)
                            intent.putExtra("projectPath", location)
                            intent.putExtra("projectName", title)
                            // start your next activity
                            startActivity(intent)
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    println("Error")
                }
            })
        } else {
            showInternetNotification(this@ProjectActivity)
        }
    }
    // List Adapter
    private inner class MyCustomAdapter(context: Context, myfiles : myFiles) : BaseAdapter() {
        private val mContext: Context
        private val myfiles : myFiles

        init {
            this.mContext = context
            this.myfiles = myfiles
        }
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

            val layoutinflator = LayoutInflater.from(mContext)
            val row_main = layoutinflator.inflate(R.layout.file_row, parent, false)

            if (!myfiles.empty) {

                val name_text_view = row_main.findViewById<TextView>(R.id.filename)
                name_text_view.text = myfiles.files[position].filename
                val filetype_texview = row_main.findViewById<TextView>(R.id.filetype)
                filetype_texview.text = myfiles.files[position].type
            }
            return row_main
        }

        override fun getItem(position: Int): Array<String> {
            if (!myfiles.empty) {
                val filename = myfiles.files.get(position).filename

                val type = myfiles.files.get(position).type

                return arrayOf(filename, type)
            } else {
                return emptyArray()
            }
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            if (myfiles.empty){
                return 0
            } else{
                return myfiles.files.size
            }
        }
    }

    // Connect to API
    private inner class Connect(val mContext: Context,val flag:Int) : AsyncTask<String, Void, String>() {

        override fun doInBackground(vararg p0: String?): String {
            return downloadJSON(p0[0])
        }

        private fun downloadJSON(url: String?): String {
            return URL(url).readText()
        }

        override fun onPostExecute(result: String) {
            try {
                val jSONObject = JSONObject(result)
                if (flag == 0) {


                    val empty = jSONObject.getBoolean("empty")

                    if (!empty) {
                        projectPath = jSONObject.getString("path")
                        fetchJson()

                    }
                } else if (flag == 1) {
                    if (jSONObject.getInt("admin") != userId) {
                        AddUsers.visibility = View.INVISIBLE
                        }
                    }
                } catch (error: Exception) {}
            super.onPostExecute(result)
        }
    }
}
    class myFiles(val empty : Boolean,val files : List<mFile>) {

    }

    class mFile(val filename : String, val type : String) {

    }
