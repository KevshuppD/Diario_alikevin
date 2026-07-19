package calendario.kevshupp.diariokevinali

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var etUser: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar si ya hay sesión
        val prefs = getSharedPreferences("DiarioPrefs", MODE_PRIVATE)
        if (prefs.contains("userId")) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        etUser = findViewById(R.id.etUser)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val user = etUser.text.toString().trim().lowercase()
            val pass = etPassword.text.toString().trim()

            if (pass == "Miaumiau123") {
                if (user == "ali" || user == "kevin") {
                    loginSuccess(user)
                } else {
                    Toast.makeText(this, "Usuario incorrecto", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loginSuccess(username: String) {
        val editor = getSharedPreferences("DiarioPrefs", MODE_PRIVATE).edit()
        val userId = if (username == "ali") "user_ali_02" else "user_kevin_01"
        val displayName = if (username == "ali") "Ali" else "Kevin"

        editor.putString("userId", userId)
        editor.putString("userName", displayName)
        editor.putString("coupleId", "vínculo_único_123")
        editor.apply()

        val wIntent = Intent(this, LastMessageWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val wIds = AppWidgetManager.getInstance(application)
                .getAppWidgetIds(ComponentName(application, LastMessageWidget::class.java))
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, wIds)
        }
        sendBroadcast(wIntent)

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
