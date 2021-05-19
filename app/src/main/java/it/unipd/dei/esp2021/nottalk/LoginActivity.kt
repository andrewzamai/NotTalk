package it.unipd.dei.esp2021.nottalk

import android.app.Activity
import android.content.Intent
import android.graphics.ColorSpace
import android.os.Bundle
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import it.unipd.dei.esp2021.nottalk.remote.ServerAdapter
import java.util.concurrent.Executors

class LoginActivity : AppCompatActivity() {
    //private var option: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)
        val userText = findViewById<EditText>(R.id.username_text)
        val passText = findViewById<EditText>(R.id.password_text)
        val loginButton = findViewById<Button>(R.id.login_button)
        val createButton = findViewById<Button>(R.id.create_button)
        val backgroundExecutor = Executors.newSingleThreadScheduledExecutor()
        val code = intent.extras?.getInt("requestCode")
        /*
        val sharedPref = getSharedPreferences("notTalkPref", MODE_PRIVATE)
        if(code!=ItemDetailHostActivity.REQUEST_LOGIN &&
                sharedPref.getString("thisUsername","")!=""){
            val intent = Intent(this,ItemDetailHostActivity::class.java)
            finish()
            startActivity(intent)
        }
                //val option = intent.extras?.getString("option")
         */

        loginButton.setOnClickListener { view ->
            val sys = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            sys.hideSoftInputFromWindow(view.applicationWindowToken, 0)
            val username = userText.text.toString()
            val password = passText.text.toString()
            if(username.isEmpty() || password.isEmpty()){
                Toast.makeText(this, "Field must not be empty", Toast.LENGTH_LONG).show()
                if(username.isEmpty()) userText.setBackgroundColor(0x33FF0000)
                else userText.setBackgroundColor(0x0)
                if(password.isEmpty()) passText.setBackgroundColor(0x33FF0000)
                else passText.setBackgroundColor(0x0)
                return@setOnClickListener
            }
            val repo = ServerAdapter()
            var result: String
            //if (code == ItemDetailHostActivity.REQUEST_LOGIN)
            backgroundExecutor.execute {
                result = repo.login(username, password)
                mainExecutor.execute {
                    if (result == "username does not exist") {
                        Toast.makeText(this, result, Toast.LENGTH_LONG).show()
                        userText.setBackgroundColor(0x33FF0000)
                        passText.setBackgroundColor(0x0)
                    } else if (result == "password incorrect") {
                        Toast.makeText(this, result, Toast.LENGTH_LONG).show()
                        passText.setBackgroundColor(0x33FF0000)
                        userText.setBackgroundColor(0x0)
                    } else {
                        Toast.makeText(this, "Welcome $username", Toast.LENGTH_LONG).show()
                        val data = Intent()
                        data.putExtra("username", username)
                        data.putExtra("uuid", result)
                        if(code == ItemDetailHostActivity.REQUEST_LOGIN) {
                            setResult(Activity.RESULT_OK, data)
                            finish()
                        } else {
                            data.setClass(this,ItemDetailHostActivity::class.java)
                            finish()
                            startActivity(data)
                        }

                    }
                }
            }
        }

        createButton.setOnClickListener { view ->
            val sys = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            sys.hideSoftInputFromWindow(view.applicationWindowToken, 0)
            val username = userText.text.toString()
            val password = passText.text.toString()
            if(username.isEmpty() || password.isEmpty()){
                Toast.makeText(this, "Field must not be empty", Toast.LENGTH_LONG).show()
                if(username.isEmpty()) userText.setBackgroundColor(0x33FF0000)
                else userText.setBackgroundColor(0x0)
                if(password.isEmpty()) passText.setBackgroundColor(0x33FF0000)
                else passText.setBackgroundColor(0x0)
                return@setOnClickListener
            }
            val repo = ServerAdapter()
            var result: String
            backgroundExecutor.execute {
                result = repo.createUser(username, password)
                mainExecutor.execute {
                    if (result == "username not available") {
                        Toast.makeText(this, result, Toast.LENGTH_LONG).show()
                        userText.setBackgroundColor(0x33FF0000)
                        passText.setBackgroundColor(0x0)
                    } else {
                        Toast.makeText(this, "Welcome $username", Toast.LENGTH_LONG).show()
                        val data = Intent()
                        data.putExtra("username", username)
                        data.putExtra("uuid", result)
                        if(code == ItemDetailHostActivity.REQUEST_LOGIN) {
                            setResult(Activity.RESULT_OK, data)
                            finish()
                        } else {
                            data.setClass(this,ItemDetailHostActivity::class.java)
                            finish()
                            startActivity(data)
                        }
                    }
                }
            }
        }
    }
/*
    override fun onStart() {
        super.onStart()
        val code = intent.extras?.getInt("requestCode")
        val sharedPref = getSharedPreferences("notTalkPref", MODE_PRIVATE)

        if(code!=ItemDetailHostActivity.REQUEST_LOGIN &&
            sharedPref.getString("thisUsername","")!=""){
            val intent = Intent(this,ItemDetailHostActivity::class.java)
            finish()
            startActivity(intent)
        }
    }
  */

    /*
    override fun onBackPressed() {
        if(option!="MANDATORY") super.onBackPressed()
        else {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }
    */
}