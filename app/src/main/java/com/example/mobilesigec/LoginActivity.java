package com.example.mobilesigec;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginActivity extends AppCompatActivity {

    EditText emailLogin, senhaLogin;
    MaterialButton btnEntrar;

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
        TextView tvEsqueciSenha = findViewById(R.id.tv_esqueci_senha);

        tvEsqueciSenha.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RecuperarSenhaEmailActivity.class)));

        btnEntrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String email = emailLogin.getText().toString().trim();
                String senha = senhaLogin.getText().toString().trim();

                if (email.isEmpty() || senha.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Preencha e-mail e senha", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Ativa o estado de Carregamento (Cor Laranja e Texto correspondente)
                btnEntrar.setEnabled(false);
                btnEntrar.setText("Carregando...");
                btnEntrar.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(LoginActivity.this, R.color.senac_orange)));

                // Executa a operação do banco de dados em segundo plano (Thread) para não travar o carregamento visual da UI
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            con = ConexaoMySQL.conectar();
                            sql = "SELECT id_usuario FROM usuario WHERE email = ? AND senha = ?";
                            
                            if (con != null) {
                                stmt = con.prepareStatement(sql);
                                stmt.setString(1, email);
                                stmt.setString(2, senha);
                                rs = stmt.executeQuery();

                                if (rs.next()) {
                                    // Login efetuado com sucesso -> Direciona para a MainActivity
                                    runOnUiThread(() -> {
                                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                        finish();
                                    });
                                } else {
                                    // Credenciais incorretas -> Restaura o botão e mostra erro
                                    runOnUiThread(() -> {
                                        Toast.makeText(LoginActivity.this, "E-mail ou senha inválidos", Toast.LENGTH_SHORT).show();
                                        restaurarBotaoLogin();
                                    });
                                }

                                rs.close();
                                stmt.close();
                                con.close();
                            } else {
                                // Erro ao conectar ao servidor de banco de dados
                                runOnUiThread(() -> {
                                    Toast.makeText(LoginActivity.this, "Erro de conexão com o banco de dados", Toast.LENGTH_SHORT).show();
                                    restaurarBotaoLogin();
                                });
                            }

                        } catch (SQLException e) {
                            runOnUiThread(() -> {
                                Toast.makeText(LoginActivity.this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                restaurarBotaoLogin();
                            });
                        }
                    }
                }).start();
            }
        });
    }

    // Método auxiliar para restaurar o estado original do botão em caso de falhas
    private void restaurarBotaoLogin() {
        btnEntrar.setEnabled(true);
        btnEntrar.setText(R.string.login_button_enter);
        btnEntrar.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(LoginActivity.this, R.color.senac_blue)));
    }
}

