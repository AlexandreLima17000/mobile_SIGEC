package com.example.mobilesigec;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

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
import java.util.Locale;
import java.util.Properties;
import java.util.Random;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class RecuperarSenhaEmailActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private MaterialButton btnEnviar;
    private ProgressBar progressEnviar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recuperar_senha_email);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etEmail = findViewById(R.id.et_email);
        btnEnviar = findViewById(R.id.btn_enviar);
        progressEnviar = findViewById(R.id.progress_enviar);
        TextView tvVoltarLogin = findViewById(R.id.tv_voltar_login);

        tvVoltarLogin.setOnClickListener(v -> finish());

        btnEnviar.setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

            if (email.isEmpty()) {
                Toast.makeText(RecuperarSenhaEmailActivity.this, "Digite seu e-mail", Toast.LENGTH_SHORT).show();
                return;
            }

            // Ativa estado de carregamento
            btnEnviar.setEnabled(false);
            btnEnviar.setText(R.string.forgot_password_checking);
            progressEnviar.setVisibility(View.VISIBLE);
            btnEnviar.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(RecuperarSenhaEmailActivity.this, R.color.senac_orange)));

            new Thread(() -> {
                try (Connection con = ConexaoMySQL.conectar();
                     PreparedStatement stmt = con != null ? con.prepareStatement("SELECT id_usuario FROM usuario WHERE email = ?") : null) {

                    if (con != null && stmt != null) {
                        stmt.setString(1, email);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                // E-mail existe -> Gera código e envia e-mail
                                String codigo = String.format(Locale.getDefault(), "%06d", new Random().nextInt(999999));
                                
                                boolean enviado = enviarEmailRecuperacao(email, codigo);

                                if (enviado) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(RecuperarSenhaEmailActivity.this, "Código enviado com sucesso para " + email, Toast.LENGTH_LONG).show();
                                        finish();
                                    });
                                } else {
                                    runOnUiThread(() -> {
                                        Toast.makeText(RecuperarSenhaEmailActivity.this, "Erro ao enviar e-mail. Tente novamente.", Toast.LENGTH_SHORT).show();
                                        restaurarBotao();
                                    });
                                }
                            } else {
                                runOnUiThread(() -> {
                                    Toast.makeText(RecuperarSenhaEmailActivity.this, "E-mail não encontrado no sistema", Toast.LENGTH_SHORT).show();
                                    restaurarBotao();
                                });
                            }
                        }
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(RecuperarSenhaEmailActivity.this, "Erro de conexão com o banco de dados", Toast.LENGTH_SHORT).show();
                            restaurarBotao();
                        });
                    }

                } catch (SQLException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(RecuperarSenhaEmailActivity.this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        restaurarBotao();
                    });
                }
            }).start();
        });
    }

    private boolean enviarEmailRecuperacao(String destinatario, String codigo) {
        String remetente = "sigec.teste@gmail.com";
        String senhaApp = "heplqzyxmmklopbh";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(remetente, senhaApp);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(remetente));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            message.setSubject("Código de Recuperação - SIGEC");
            message.setText("Olá,\n\nSeu código de recuperação de senha do Sistema de Gerenciamento de Estoque da Cozinha (SIGEC) é: " + codigo + "\n\nUtilize este código para redefinir sua senha.\n\nAtenciosamente,\nEquipe SIGEC");
            
            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void restaurarBotao() {
        btnEnviar.setEnabled(true);
        btnEnviar.setText(R.string.forgot_password_button_send);
        progressEnviar.setVisibility(View.GONE);
        btnEnviar.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(RecuperarSenhaEmailActivity.this, R.color.senac_blue)));
    }
}
