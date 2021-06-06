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

/**
 * Secondary activity in charge of perform user-related operations
 * Login and Create User
 */

class LoginActivity : AppCompatActivity() {
    private var code: Int? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)
        val userText = findViewById<EditText>(R.id.username_text)
        val passText = findViewById<EditText>(R.id.password_text)
        val loginButton = findViewById<Button>(R.id.login_button)
        val registerButton = findViewById<Button>(R.id.register_button)
        val backgroundExecutor = Executors.newSingleThreadScheduledExecutor()
        code = intent.extras?.getInt("requestCode") //get intent request code

        //When Login button is pressed
        loginButton.setOnClickListener { view ->
            //Close the keyboard
            val sys = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            sys.hideSoftInputFromWindow(view.applicationWindowToken, 0)
            val username = userText.text.toString()
            val password = passText.text.toString()
            //Form validation and error feedback
            if(username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Field must not be empty", Toast.LENGTH_LONG).show()
                if (username.isEmpty()) userText.setBackgroundColor(0x33FF0000)
                else userText.setBackgroundColor(0x0)
                if (password.isEmpty()) passText.setBackgroundColor(0x33FF0000)
                else passText.setBackgroundColor(0x0)
                return@setOnClickListener
            }
            else if(username.length>50){
                Toast.makeText(this, "Max username length: 50", Toast.LENGTH_LONG).show()
                userText.setBackgroundColor(0x33FF0000)
                passText.setBackgroundColor(0x0)
                return@setOnClickListener
            }
            else if(password.length>60){
                Toast.makeText(this, "Max password length: 60", Toast.LENGTH_LONG).show()
                passText.setBackgroundColor(0x33FF0000)
                userText.setBackgroundColor(0x0)
                return@setOnClickListener
            }
            val repo = ServerAdapter()
            var result: String
            backgroundExecutor.execute {
                //Perform http request to server
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
                        //If result is ok update intent and finish activity
                        Toast.makeText(this, "Welcome $username", Toast.LENGTH_LONG).show()
                        val data = Intent()
                        data.putExtra("username", username)
                        data.putExtra("uuid", result)
                        setResult(Activity.RESULT_OK, data)
                        finish()
                    }
                }
            }
        }

        //When Register button is pressed
        registerButton.setOnClickListener { view ->
            //Close the keyboard
            val sys = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            sys.hideSoftInputFromWindow(view.applicationWindowToken, 0)
            val username = userText.text.toString()
            val password = passText.text.toString()
            //Form validation and error feedback
            if(username.isEmpty() || password.isEmpty()){
                Toast.makeText(this, "Field must not be empty", Toast.LENGTH_LONG).show()
                if(username.isEmpty()) userText.setBackgroundColor(0x33FF0000)
                else userText.setBackgroundColor(0x0)
                if(password.isEmpty()) passText.setBackgroundColor(0x33FF0000)
                else passText.setBackgroundColor(0x0)
                return@setOnClickListener
            }
            else if(username.length>50){
                Toast.makeText(this, "Max username length: 50", Toast.LENGTH_LONG).show()
                userText.setBackgroundColor(0x33FF0000)
                passText.setBackgroundColor(0x0)
                return@setOnClickListener
            }
            else if(password.length>60){
                Toast.makeText(this, "Max password length: 60", Toast.LENGTH_LONG).show()
                passText.setBackgroundColor(0x33FF0000)
                userText.setBackgroundColor(0x0)
                return@setOnClickListener
            }
            val repo = ServerAdapter()
            var result: String
            backgroundExecutor.execute {
                //Perform http request to server
                result = repo.createUser(username, password)
                mainExecutor.execute {
                    if (result == "username not available") {
                        Toast.makeText(this, result, Toast.LENGTH_LONG).show()
                        userText.setBackgroundColor(0x33FF0000)
                        passText.setBackgroundColor(0x0)
                    } else {
                        //If result is ok update intent and finish activity
                        Toast.makeText(this, "Welcome $username", Toast.LENGTH_LONG).show()
                        val data = Intent()
                        data.putExtra("username", username)
                        data.putExtra("uuid", result)
                        setResult(Activity.RESULT_OK, data)
                        finish()
                    }
                }
            }
        }
    }
    override fun onBackPressed() {
        /**
         * In case of intent with request code "REQUEST_MUST_LOGIN"
         * sets a special result that cause the application to close
         * on back pressed
         */
        if (code == ItemDetailHostActivity.REQUEST_MUST_LOGIN){
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
        super.onBackPressed()
    }
}