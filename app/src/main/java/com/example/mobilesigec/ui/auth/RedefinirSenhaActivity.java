package com.example.mobilesigec.ui.auth;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mobilesigec.GerenciadorBloqueioRecuperacao;
import com.example.mobilesigec.R;
import com.example.mobilesigec.database.ConexaoMySQL;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.mindrot.jbcrypt.BCrypt;

public class RedefinirSenhaActivity extends AppCompatActivity {

    private TextInputEditText etNovaSenha, etConfirmarSenha;
    private MaterialButton btnRedefinirSenha;
    private ProgressBar progressRedefinir;
    private String emailUsuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_redefinir_senha);

        // Garante que a StatusBar fique Azul Escura com ícones brancos (igual ao Login)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.senac_blue));
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(decor.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        if (getIntent() != null) {
            emailUsuario = getIntent().getStringExtra("email");
        }

        etNovaSenha = findViewById(R.id.et_nova_senha);
        etConfirmarSenha = findViewById(R.id.et_confirmar_senha);
        btnRedefinirSenha = findViewById(R.id.btn_redefinir_senha);
        progressRedefinir = findViewById(R.id.progress_redefinir);

        btnRedefinirSenha.setOnClickListener(v -> {
            String novaSenha = etNovaSenha.getText() != null ? etNovaSenha.getText().toString().trim() : "";
            String confirmarSenha = etConfirmarSenha.getText() != null ? etConfirmarSenha.getText().toString().trim() : "";

            if (novaSenha.isEmpty()) {
                Toast.makeText(RedefinirSenhaActivity.this, "Digite a nova senha", Toast.LENGTH_SHORT).show();
                return;
            }

            if (confirmarSenha.isEmpty()) {
                Toast.makeText(RedefinirSenhaActivity.this, "Confirme a nova senha", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!novaSenha.equals(confirmarSenha)) {
                Toast.makeText(RedefinirSenhaActivity.this, "As senhas não coincidem", Toast.LENGTH_SHORT).show();
                return;
            }

            if (emailUsuario == null || emailUsuario.isEmpty()) {
                Toast.makeText(RedefinirSenhaActivity.this, "Erro: E-mail não identificado", Toast.LENGTH_SHORT).show();
                return;
            }

            btnRedefinirSenha.setEnabled(false);
            btnRedefinirSenha.setText("Redefinindo...");
            progressRedefinir.setVisibility(View.VISIBLE);
            btnRedefinirSenha.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(RedefinirSenhaActivity.this, R.color.senac_orange)));

            new Thread(() -> {
                try (Connection con = ConexaoMySQL.conectar();
                     PreparedStatement stmt = con != null ? con.prepareStatement("UPDATE usuario SET senha = ? WHERE email = ?") : null) {

                    if (con != null && stmt != null) {
                        String senhaHash = BCrypt.hashpw(novaSenha, BCrypt.gensalt());
                        stmt.setString(1, senhaHash);
                        stmt.setString(2, emailUsuario);
                        int rowsUpdated = stmt.executeUpdate();

                        if (rowsUpdated > 0) {
                            GerenciadorBloqueioRecuperacao.limparEstadoRecuperacao(
                                    RedefinirSenhaActivity.this, emailUsuario);
                            runOnUiThread(() -> {
                                Toast.makeText(RedefinirSenhaActivity.this, "Senha redefinida com sucesso!", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(RedefinirSenhaActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            });
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(RedefinirSenhaActivity.this, "Não foi possível atualizar a senha. Usuário não encontrado.", Toast.LENGTH_LONG).show();
                                restaurarBotao();
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(RedefinirSenhaActivity.this, "Erro de conexão com o banco de dados", Toast.LENGTH_SHORT).show();
                            restaurarBotao();
                        });
                    }

                } catch (SQLException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(RedefinirSenhaActivity.this, "Erro ao atualizar senha: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        restaurarBotao();
                    });
                }
            }).start();
        });
    }

    private void restaurarBotao() {
        btnRedefinirSenha.setEnabled(true);
        btnRedefinirSenha.setText(R.string.reset_password_button);
        progressRedefinir.setVisibility(View.GONE);
        btnRedefinirSenha.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(RedefinirSenhaActivity.this, R.color.senac_blue)));
    }
}
