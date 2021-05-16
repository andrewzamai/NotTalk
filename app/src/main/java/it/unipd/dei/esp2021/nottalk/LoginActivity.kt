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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)
        val userText = findViewById<EditText>(R.id.username_text)
        val passText = findViewById<EditText>(R.id.password_text)
        val loginButton = findViewById<Button>(R.id.login_button)
        val backgroundExecutor = Executors.newSingleThreadScheduledExecutor()
        val code = intent.extras?.getInt("requestCode")

        if(code==ItemDetailHostActivity.REQUEST_CREATE) loginButton.setText(R.string.button_create_text)

        loginButton.setOnClickListener { view ->
            val sys = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            sys.hideSoftInputFromWindow(view.applicationWindowToken, 0)
            val username = userText.text.toString()
            val password = passText.text.toString()
            val repo = ServerAdapter()
            var result: String
            if (code == ItemDetailHostActivity.REQUEST_LOGIN) {
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
                            setResult(Activity.RESULT_OK, data)
                            finish()
                        }
                    }
                }
            }
            else if (code == ItemDetailHostActivity.REQUEST_CREATE) {
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
                            setResult(Activity.RESULT_OK, data)
                            finish()
                        }
                    }
                }
            }
        }
    }
}