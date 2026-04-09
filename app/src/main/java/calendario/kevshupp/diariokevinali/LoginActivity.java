package calendario.kevshupp.diariokevinali;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etUser, etPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Verificar si ya hay sesión
        SharedPreferences prefs = getSharedPreferences("DiarioPrefs", MODE_PRIVATE);
        if (prefs.contains("userId")) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        etUser = findViewById(R.id.etUser);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String user = etUser.getText().toString().trim().toLowerCase();
            String pass = etPassword.getText().toString().trim();

            if (pass.equals("Miaumiau123")) {
                if (user.equals("ali") || user.equals("kevin")) {
                    loginSuccess(user);
                } else {
                    Toast.makeText(this, "Usuario incorrecto", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginSuccess(String username) {
        SharedPreferences.Editor editor = getSharedPreferences("DiarioPrefs", MODE_PRIVATE).edit();
        String userId = username.equals("ali") ? "user_ali_02" : "user_kevin_01";
        String displayName = username.equals("ali") ? "Ali" : "Kevin";
        
        editor.putString("userId", userId);
        editor.putString("userName", displayName);
        editor.apply();

        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
