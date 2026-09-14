package com.example.mobilesigec;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginActivity extends AppCompatActivity {

    EditText emailLogin, senhaLogin;
    Button btnEntrar;
    

    Connection con = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;
    String sql;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });



            emailLogin = findViewById(R.id.emailLogin);
            senhaLogin = findViewById(R.id.senhaLogin);
            btnEntrar = findViewById(R.id.btnEntrar);

            btnEntrar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    String email = emailLogin.getText().toString().trim();
                    String senha = senhaLogin.getText().toString().trim();

                    if (email.isEmpty() || senha.isEmpty()) {
                        Toast.makeText(LoginActivity.this, "Preencha e-mail e senha", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        con = ConexaoMySQL.conectar();
                        sql = "SELECT id_usuario FROM usuario WHERE email = ? AND senha = ?";
                        stmt = con.prepareStatement(sql);
                        stmt.setString(1, email);
                        stmt.setString(2, senha);
                        rs = stmt.executeQuery();


                        if (rs.next()) {
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "E-mail ou senha inválidos", Toast.LENGTH_SHORT).show();
                        }

                        rs.close();
                        stmt.close();
                        con.close();


                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

        }

        }

