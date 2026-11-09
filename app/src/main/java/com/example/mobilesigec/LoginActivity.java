package com.example.mobilesigec;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginActivity extends AppCompatActivity {

    EditText emailLogin, senhaLogin;
    MaterialButton btnEntrar;
    ProgressBar progressEntrar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Removemos o EdgeToEdge.enable(this); que causava a transparência na barra
        setContentView(R.layout.activity_login);

        // Garante que a StatusBar fique Azul Escura com ícones brancos (igual à Home)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.senac_blue));
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            View decor = getWindow().getDecorView();
            // Desativa o modo "Light Status Bar" para que os ícones (bateria, relógio) fiquem brancos
            decor.setSystemUiVisibility(decor.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        emailLogin = findViewById(R.id.emailLogin);
        senhaLogin = findViewById(R.id.senhaLogin);
        btnEntrar = findViewById(R.id.btnEntrar);
        progressEntrar = findViewById(R.id.progress_entrar);
        TextView tvEsqueciSenha = findViewById(R.id.tv_esqueci_senha);

        if (tvEsqueciSenha != null) {
            tvEsqueciSenha.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, RecuperarSenhaEmailActivity.class);
                startActivity(intent);
            });
        }

        btnEntrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String email = emailLogin.getText().toString().trim();
                String senha = senhaLogin.getText().toString().trim();

                if (email.isEmpty() || senha.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Preencha e-mail e senha", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!email.toLowerCase().contains("senac.br") && !email.contains("@")) {
                    Toast.makeText(LoginActivity.this, "O e-mail deve ser válido", Toast.LENGTH_SHORT).show();
                    emailLogin.setError("Domínio inválido. Use um e-mail do Senac");
                    return;
                }

                // Ativa o estado de Carregamento (Cor Laranja e Texto correspondente)
                btnEntrar.setEnabled(false);
                btnEntrar.setText("Carregando...");
                btnEntrar.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(LoginActivity.this, R.color.senac_orange)));
                if (progressEntrar != null) {
                    progressEntrar.setVisibility(View.VISIBLE);
                }

                new Thread(() -> {
                    String querySql = "SELECT id_usuario, senha, nome_usuario FROM usuario WHERE email = ?";
                    boolean loginEfetuado = false;
                    boolean conexaoOk = false;

                    try (Connection conexao = ConexaoMySQL.conectar()) {
                        if (conexao != null) {
                            conexaoOk = true;
                            try (PreparedStatement stmtLocal = conexao.prepareStatement(querySql)) {
                                stmtLocal.setString(1, email);
                                try (ResultSet rsLocal = stmtLocal.executeQuery()) {
                                    if (rsLocal.next()) {
                                        String hashBanco = rsLocal.getString("senha");
                                        int idUsuario = rsLocal.getInt("id_usuario");
                                        String nomeUsuario = rsLocal.getString("nome_usuario");

                                        boolean senhaValida = false;
                                        if (hashBanco != null && hashBanco.trim().startsWith("$2")) {
                                            try {
                                                String hashTratado = hashBanco.trim();
                                                if (hashTratado.startsWith("$2y$")) {
                                                    hashTratado = "$2a$" + hashTratado.substring(4);
                                                }
                                                senhaValida = BCrypt.checkpw(senha, hashTratado);
                                            } catch (IllegalArgumentException ex) {
                                                android.util.Log.e("AuthError", "Hash malformado no banco: " + hashBanco, ex);
                                            }
                                        }

                                        if (!senhaValida && hashBanco != null) {
                                            senhaValida = senha.equals(hashBanco.trim());
                                        }

                                        if (senhaValida) {
                                            loginEfetuado = true;

                                            if (nomeUsuario == null || nomeUsuario.trim().isEmpty()) {
                                                nomeUsuario = email.contains("@") ? email.split("@")[0] : email;
                                            }

                                            getSharedPreferences("SessaoApp", MODE_PRIVATE)
                                                    .edit()
                                                    .clear()
                                                    .putBoolean("logado", true)
                                                    .putInt("id_usuario", idUsuario)
                                                    .putInt("ID_USUARIO", idUsuario)
                                                    .putString("nome_usuario", nomeUsuario)
                                                    .putString("NOME_USUARIO", nomeUsuario)
                                                    .putString("EMAIL_USUARIO", email)
                                                    .putString("email", email)
                                                    .apply();
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("LoginActivity", "Erro durante login", e);
                    }

                    final boolean finalLoginEfetuado = loginEfetuado;
                    final boolean finalConexaoOk = conexaoOk;

                    runOnUiThread(() -> {
                        if (finalLoginEfetuado) {
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else if (!finalConexaoOk) {
                            String erro = ConexaoMySQL.getUltimoErro();
                            String msg = "Erro de conexão com o banco de dados" + (erro.isEmpty() ? "" : ": " + erro);
                            Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
                            restaurarBotaoLogin();
                        } else {
                            Toast.makeText(LoginActivity.this, "E-mail ou senha inválidos", Toast.LENGTH_SHORT).show();
                            restaurarBotaoLogin();
                        }
                    });
                }).start();
            }
        });
    }

    // Método auxiliar para restaurar o estado original do botão em caso de falhas
    private void restaurarBotaoLogin() {
        btnEntrar.setEnabled(true);
        btnEntrar.setText(R.string.login_button_enter);
        btnEntrar.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(LoginActivity.this, R.color.senac_blue)));
        if (progressEntrar != null) {
            progressEntrar.setVisibility(View.GONE);
        }
    }
}