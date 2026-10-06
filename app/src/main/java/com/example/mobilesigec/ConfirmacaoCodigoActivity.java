package com.example.mobilesigec;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

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

public class ConfirmacaoCodigoActivity extends AppCompatActivity {

    private EditText etCode1, etCode2, etCode3, etCode4, etCode5, etCode6;
    private MaterialButton btnReenviarCodigo;
    private String emailUsuario;
    private String codigoEsperado;
    private String ultimoErroEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_confirmacao_codigo);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Receber dados da Intent
        Intent intentOriginal = getIntent();
        if (intentOriginal != null) {
            emailUsuario = intentOriginal.getStringExtra("email");
            codigoEsperado = intentOriginal.getStringExtra("codigo");
        }

        // Inicializar Views
        etCode1 = findViewById(R.id.et_code_1);
        etCode2 = findViewById(R.id.et_code_2);
        etCode3 = findViewById(R.id.et_code_3);
        etCode4 = findViewById(R.id.et_code_4);
        etCode5 = findViewById(R.id.et_code_5);
        etCode6 = findViewById(R.id.et_code_6);

        TextView tvSubLabel = findViewById(R.id.tv_sub_label);
        if (emailUsuario != null && !emailUsuario.isEmpty() && tvSubLabel != null) {
            tvSubLabel.setText("Digite o código de 6 dígitos enviado para\n" + emailUsuario);
        }

        MaterialButton btnConfirmarCodigo = findViewById(R.id.btn_confirmar_codigo);
        btnReenviarCodigo = findViewById(R.id.btn_reenviar_codigo);
        ProgressBar progressConfirmar = findViewById(R.id.progress_confirmar);
        TextView tvVoltarLogin = findViewById(R.id.tv_voltar_login);

        // Configurar transição automática de foco entre os campos de código
        setupCodeInputs();

        // Botão de Confirmação
        btnConfirmarCodigo.setOnClickListener(v -> {
            String code = getEnteredCode();
            if (code.length() < 6) {
                Toast.makeText(ConfirmacaoCodigoActivity.this, "Por favor, digite o código completo de 6 dígitos.", Toast.LENGTH_SHORT).show();
                return;
            }

            btnConfirmarCodigo.setEnabled(false);
            btnConfirmarCodigo.setText("Confirmando...");
            if (progressConfirmar != null) {
                progressConfirmar.setVisibility(View.VISIBLE);
            }
            btnConfirmarCodigo.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ConfirmacaoCodigoActivity.this, R.color.senac_orange)));

            if (codigoEsperado != null && !code.equals(codigoEsperado)) {
                Toast.makeText(ConfirmacaoCodigoActivity.this, "Código incorreto. Verifique o e-mail enviado.", Toast.LENGTH_LONG).show();
                btnConfirmarCodigo.setEnabled(true);
                btnConfirmarCodigo.setText(R.string.confirm_code_button);
                if (progressConfirmar != null) {
                    progressConfirmar.setVisibility(View.GONE);
                }
                btnConfirmarCodigo.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ConfirmacaoCodigoActivity.this, R.color.senac_blue)));
            } else {
                Toast.makeText(ConfirmacaoCodigoActivity.this, "Código confirmado com sucesso!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(ConfirmacaoCodigoActivity.this, RedefinirSenhaActivity.class);
                intent.putExtra("email", emailUsuario);
                startActivity(intent);
                finish();
            }
        });

        // Botão "Reenviar Código"
        if (btnReenviarCodigo != null) {
            btnReenviarCodigo.setOnClickListener(v -> {
                if (emailUsuario == null || emailUsuario.isEmpty()) {
                    Toast.makeText(ConfirmacaoCodigoActivity.this, "E-mail não identificado.", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnReenviarCodigo.setEnabled(false);
                btnReenviarCodigo.setText("Reenviando...");

                new Thread(() -> {
                    String novoCodigo = String.format(Locale.getDefault(), "%06d", new Random().nextInt(1000000));
                    boolean enviado = enviarEmailRecuperacao(emailUsuario, novoCodigo);

                    runOnUiThread(() -> {
                        btnReenviarCodigo.setEnabled(true);
                        btnReenviarCodigo.setText(R.string.resend_code_button);

                        if (enviado) {
                            codigoEsperado = novoCodigo;
                            limparCamposCodigo();
                            Toast.makeText(ConfirmacaoCodigoActivity.this, "Novo código enviado com sucesso para " + emailUsuario, Toast.LENGTH_LONG).show();
                        } else {
                            String msg = "Erro ao reenviar código. " + (ultimoErroEmail.isEmpty() ? "Tente novamente." : ultimoErroEmail);
                            Toast.makeText(ConfirmacaoCodigoActivity.this, msg, Toast.LENGTH_LONG).show();
                        }
                    });
                }).start();
            });
        }

        // Botão "Voltar ao Login"
        tvVoltarLogin.setOnClickListener(v -> {
            Intent intent = new Intent(ConfirmacaoCodigoActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void limparCamposCodigo() {
        etCode1.setText("");
        etCode2.setText("");
        etCode3.setText("");
        etCode4.setText("");
        etCode5.setText("");
        etCode6.setText("");
        etCode1.requestFocus();
    }

    private boolean enviarEmailRecuperacao(String destinatario, String codigo) {
        String remetente = "sigec.teste@gmail.com";
        String senhaApp = "pfqrwulpoclkxgzj";
        ultimoErroEmail = "";

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
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
            message.setSubject("Novo Código de Recuperação - SIGEC");
            message.setText("Olá,\n\nSeu NOVO código de recuperação de senha do Sistema de Gerenciamento de Estoque da Cozinha (SIGEC) é: " + codigo + "\n\nUtilize este código para redefinir sua senha.\n\nAtenciosamente,\nEquipe SIGEC");

            Transport.send(message);
            return true;
        } catch (Exception e) {
            Log.e("ConfirmacaoCodigo", "Erro ao reenviar e-mail de recuperação", e);
            ultimoErroEmail = e.getMessage() != null ? e.getMessage() : e.toString();
            return false;
        }
    }

    private void setupCodeInputs() {
        EditText[] edits = new EditText[]{etCode1, etCode2, etCode3, etCode4, etCode5, etCode6};

        for (int i = 0; i < edits.length; i++) {
            final int index = i;

            edits[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < edits.length - 1) {
                        edits[index + 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            edits[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (edits[index].getText().toString().isEmpty() && index > 0) {
                        edits[index - 1].requestFocus();
                        edits[index - 1].setText("");
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private String getEnteredCode() {
        return etCode1.getText().toString().trim() +
                etCode2.getText().toString().trim() +
                etCode3.getText().toString().trim() +
                etCode4.getText().toString().trim() +
                etCode5.getText().toString().trim() +
                etCode6.getText().toString().trim();
    }
}
