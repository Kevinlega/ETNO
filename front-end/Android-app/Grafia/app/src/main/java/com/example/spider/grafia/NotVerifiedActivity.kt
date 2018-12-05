package com.example.spider.grafia

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_not_verified.*
import org.json.JSONObject
import java.lang.Exception
import java.net.URL

class NotVerifiedActivity : AppCompatActivity() {

    fun verify(email: String){
        val query = 1
        val connectToAPI = Connect(this@NotVerifiedActivity)
        try{
            val url = "http://54.81.239.120/updateAPI.php?queryType=$query&email=$email"
            println(url)
            connectToAPI.execute(url)
        }
        catch (error: Exception){}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_not_verified)

        supportActionBar!!.setTitle("Account Verification")

        val Email = findViewById<EditText>(R.id.emailVerify)


        Verify.setOnClickListener {

            val email = Email.text.toString()
            if(!email.isNullOrBlank()){
                verify(email)
            }
        }
    }

    companion object {
        class Connect(private val mContext: Context) :
            AsyncTask<String, Void, String>() {

            override fun doInBackground(vararg p0: String?): String {
                return downloadJSON(p0[0])
            }

            private fun downloadJSON(url: String?): String {
                return URL(url).readText()
            }

            override fun onPostExecute(result: String) {
                try {
                    val jSONObject = JSONObject(result)
                    println(jSONObject)


                    val update = jSONObject.getBoolean("updated")

                    if (update) {

                        val intent = Intent(mContext, LoginActivity::class.java)
                        mContext.startActivity(intent)

                    } else {
                        Toast.makeText(mContext, "Something went wrong, try again.", Toast.LENGTH_LONG).show()
                    }
                } catch (error: Exception) {}
                super.onPostExecute(result)
            }
        }
    }
}
