package com.example.mobilesigec;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class RecuperarSenhaEmailActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private MaterialButton btnEnviar;
    private ProgressBar progressEnviar;
    private static String ultimoErroEmail = "";

    public static String getUltimoErroEmail() {
        return ultimoErroEmail;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recuperar_senha_email);

        // Garante que a StatusBar fique Azul Escura com ícones brancos (igual ao Login)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.senac_blue));
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(decor.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

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

            if (!email.toLowerCase().endsWith("df.senac.br")) {
                Toast.makeText(RecuperarSenhaEmailActivity.this, "O e-mail deve pertencer ao domínio df.senac.br", Toast.LENGTH_SHORT).show();
                etEmail.setError("Domínio inválido. Use um e-mail @df.senac.br");
                return;
            }

            // Verifica se o e-mail ou IP já estão bloqueados localmente
            long bloqueioLocalMs = GerenciadorBloqueioRecuperacao.obterTempoRestanteBloqueioMs(
                    RecuperarSenhaEmailActivity.this, null, email);
            if (bloqueioLocalMs > 0) {
                String tempoFmt = GerenciadorBloqueioRecuperacao.formatarTempoMinutosSegundos(bloqueioLocalMs);
                Toast.makeText(RecuperarSenhaEmailActivity.this,
                        "Login (e-mail) e IP bloqueados por excesso de reenvios. Tente novamente em " + tempoFmt + ".",
                        Toast.LENGTH_LONG).show();
                return;
            }

            // Se já enviou um código há menos de 1:30, redireciona para a tela de confirmação mantendo a contagem
            long esperaReenvioMs = GerenciadorBloqueioRecuperacao.obterTempoRestanteReenvioMs(
                    RecuperarSenhaEmailActivity.this, email);
            String codigoSalvo = GerenciadorBloqueioRecuperacao.obterCodigoAtual(
                    RecuperarSenhaEmailActivity.this, email);
            if (esperaReenvioMs > 0 && codigoSalvo != null) {
                String tempoFmt = GerenciadorBloqueioRecuperacao.formatarTempoMinutosSegundos(esperaReenvioMs);
                Toast.makeText(RecuperarSenhaEmailActivity.this,
                        "Um código já foi enviado. Aguarde " + tempoFmt + " para solicitar o reenvio.",
                        Toast.LENGTH_LONG).show();
                Intent intent = new Intent(RecuperarSenhaEmailActivity.this, ConfirmacaoCodigoActivity.class);
                intent.putExtra("email", email);
                intent.putExtra("codigo", codigoSalvo);
                startActivity(intent);
                finish();
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
                        // Verifica bloqueio também no banco de dados
                        long bloqueioBancoMs = GerenciadorBloqueioRecuperacao.obterTempoRestanteBloqueioMs(
                                RecuperarSenhaEmailActivity.this, con, email);
                        if (bloqueioBancoMs > 0) {
                            String tempoFmt = GerenciadorBloqueioRecuperacao.formatarTempoMinutosSegundos(bloqueioBancoMs);
                            runOnUiThread(() -> {
                                Toast.makeText(RecuperarSenhaEmailActivity.this,
                                        "Login (e-mail) e IP bloqueados por excesso de reenvios. Tente novamente em " + tempoFmt + ".",
                                        Toast.LENGTH_LONG).show();
                                restaurarBotao();
                            });
                            return;
                        }

                        stmt.setString(1, email);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                boolean sessaoAtiva = GerenciadorBloqueioRecuperacao.possuiSessaoAtiva(
                                        RecuperarSenhaEmailActivity.this, email);
                                int reenviosAtuais = GerenciadorBloqueioRecuperacao.obterQuantidadeReenvios(
                                        RecuperarSenhaEmailActivity.this, email);

                                if (sessaoAtiva && reenviosAtuais >= GerenciadorBloqueioRecuperacao.MAX_REENVIOS) {
                                    GerenciadorBloqueioRecuperacao.registrarBloqueio(
                                            RecuperarSenhaEmailActivity.this, con, email);
                                    runOnUiThread(() -> {
                                        Toast.makeText(RecuperarSenhaEmailActivity.this,
                                                "Limite de 3 reenvios excedido! Seu login (e-mail) e IP foram bloqueados por 15 minutos.",
                                                Toast.LENGTH_LONG).show();
                                        finish();
                                    });
                                    return;
                                }

                                // E-mail existe -> Gera código e envia e-mail
                                String codigo = String.format(Locale.getDefault(), "%06d", new Random().nextInt(1000000));

                                boolean enviado = enviarEmailRecuperacao(email, codigo);

                                if (enviado) {
                                    if (sessaoAtiva) {
                                        GerenciadorBloqueioRecuperacao.registrarReenvio(
                                                RecuperarSenhaEmailActivity.this, email, codigo);
                                    } else {
                                        GerenciadorBloqueioRecuperacao.registrarEnvioInicial(
                                                RecuperarSenhaEmailActivity.this, email, codigo);
                                    }

                                    runOnUiThread(() -> {
                                        Toast.makeText(RecuperarSenhaEmailActivity.this, "Código enviado com sucesso para " + email, Toast.LENGTH_LONG).show();
                                        Intent intent = new Intent(RecuperarSenhaEmailActivity.this, ConfirmacaoCodigoActivity.class);
                                        intent.putExtra("email", email);
                                        intent.putExtra("codigo", codigo);
                                        startActivity(intent);
                                        finish();
                                    });
                                } else {
                                    runOnUiThread(() -> {
                                        String msg = "Erro ao enviar e-mail. " + (ultimoErroEmail.isEmpty() ? "Tente novamente." : ultimoErroEmail);
                                        Toast.makeText(RecuperarSenhaEmailActivity.this, msg, Toast.LENGTH_LONG).show();
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

    public static boolean enviarEmailRecuperacao(String destinatario, String codigo) {
        final String remetente = BuildConfig.MAIL_NAME;
        final String senhaApp = BuildConfig.MAIL_PASS;
        final String host = BuildConfig.MAIL_HOST != null ? BuildConfig.MAIL_HOST : "smtp.gmail.com";
        final String port = BuildConfig.MAIL_PORT != null ? BuildConfig.MAIL_PORT : "587";

        ultimoErroEmail = "";

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", host);

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
        } catch (Exception e) {
            Log.e("RecuperarSenha", "Erro ao enviar e-mail de recuperação", e);
            ultimoErroEmail = e.getMessage() != null ? e.getMessage() : e.toString();
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
