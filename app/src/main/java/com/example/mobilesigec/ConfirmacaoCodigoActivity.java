package com.example.mobilesigec;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.sql.Connection;
import java.util.Locale;
import java.util.Random;

public class ConfirmacaoCodigoActivity extends AppCompatActivity {

    private EditText etCode1, etCode2, etCode3, etCode4, etCode5, etCode6;
    private MaterialButton btnReenviarCodigo;
    private String emailUsuario;
    private String codigoEsperado;
    private CountDownTimer countDownTimer;
    private boolean reenviandoEmAndamento = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmacao_codigo);

        // Garante que a StatusBar fique Azul Escura com ícones brancos (igual ao Login)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.senac_blue));
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(decor.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        // Receber dados da Intent
        Intent intentOriginal = getIntent();
        if (intentOriginal != null) {
            emailUsuario = intentOriginal.getStringExtra("email");
            codigoEsperado = intentOriginal.getStringExtra("codigo");
        }

        if (emailUsuario != null && !emailUsuario.isEmpty()) {
            if (!GerenciadorBloqueioRecuperacao.possuiSessaoAtiva(this, emailUsuario)) {
                GerenciadorBloqueioRecuperacao.registrarEnvioInicial(this, emailUsuario, codigoEsperado);
            } else {
                String codigoSalvo = GerenciadorBloqueioRecuperacao.obterCodigoAtual(this, emailUsuario);
                if (codigoSalvo != null && !codigoSalvo.isEmpty()) {
                    codigoEsperado = codigoSalvo;
                }
            }
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
        ProgressBar progressConfirmar = findViewById(R.id.progress_confirmar);
        btnReenviarCodigo = findViewById(R.id.btn_reenviar_codigo);
        TextView tvVoltarLogin = findViewById(R.id.tv_voltar_login);

        // Configurar transição automática de foco entre os campos de código
        setupCodeInputs();

        // Inicia o cronômetro de 1:30 para liberar o reenvio
        iniciarOuAtualizarTimerReenvio();

        // Botão de Reenviar Código
        if (btnReenviarCodigo != null) {
            btnReenviarCodigo.setOnClickListener(v -> processarReenvioCodigo());
        }

        // Botão de Confirmação
        btnConfirmarCodigo.setOnClickListener(v -> {
            long bloqueioMs = GerenciadorBloqueioRecuperacao.obterTempoRestanteBloqueioMs(
                    ConfirmacaoCodigoActivity.this, null, emailUsuario);
            if (bloqueioMs > 0) {
                String tempoFmt = GerenciadorBloqueioRecuperacao.formatarTempoMinutosSegundos(bloqueioMs);
                Toast.makeText(ConfirmacaoCodigoActivity.this,
                        "Login (e-mail) e IP bloqueados por 15 minutos. Aguarde " + tempoFmt + ".",
                        Toast.LENGTH_LONG).show();
                voltarParaLogin();
                return;
            }

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

        // Botão "Voltar ao Login"
        tvVoltarLogin.setOnClickListener(v -> voltarParaLogin());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!reenviandoEmAndamento) {
            iniciarOuAtualizarTimerReenvio();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelarTimerReenvio();
    }

    private void processarReenvioCodigo() {
        if (reenviandoEmAndamento) {
            return;
        }

        if (emailUsuario == null || emailUsuario.isEmpty()) {
            Toast.makeText(this, "E-mail do usuário não identificado.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Verifica se o usuário ou IP já estão bloqueados
        long bloqueioMs = GerenciadorBloqueioRecuperacao.obterTempoRestanteBloqueioMs(this, null, emailUsuario);
        if (bloqueioMs > 0) {
            String tempoFmt = GerenciadorBloqueioRecuperacao.formatarTempoMinutosSegundos(bloqueioMs);
            Toast.makeText(this,
                    "Login (e-mail) e IP bloqueados. Tente novamente em " + tempoFmt + ".",
                    Toast.LENGTH_LONG).show();
            voltarParaLogin();
            return;
        }

        // 2. Verifica se já atingiu o limite de 3 reenvios antes de pedir mais um
        int reenviosAtuais = GerenciadorBloqueioRecuperacao.obterQuantidadeReenvios(this, emailUsuario);
        if (reenviosAtuais >= GerenciadorBloqueioRecuperacao.MAX_REENVIOS) {
            bloquearUsuarioEVoltarAoLogin();
            return;
        }

        // 3. Verifica se já se passaram 1:30 (90 segundos) desde o último envio
        long tempoRestanteMs = GerenciadorBloqueioRecuperacao.obterTempoRestanteReenvioMs(this, emailUsuario);
        if (tempoRestanteMs > 0) {
            String tempoFmt = GerenciadorBloqueioRecuperacao.formatarTempoMinutosSegundos(tempoRestanteMs);
            Toast.makeText(this,
                    "Você só pode reenviar o código após 1:30 do último envio. Aguarde " + tempoFmt + ".",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // 4. Realiza o reenvio do novo código por e-mail
        reenviandoEmAndamento = true;
        cancelarTimerReenvio();
        if (btnReenviarCodigo != null) {
            btnReenviarCodigo.setText("Reenviando código...");
            btnReenviarCodigo.setTextColor(ContextCompat.getColor(this, R.color.senac_orange));
        }

        new Thread(() -> {
            String novoCodigo = String.format(Locale.getDefault(), "%06d", new Random().nextInt(1000000));
            boolean enviado = RecuperarSenhaEmailActivity.enviarEmailRecuperacao(emailUsuario, novoCodigo);

            runOnUiThread(() -> {
                reenviandoEmAndamento = false;
                if (enviado) {
                    int totalReenvios = GerenciadorBloqueioRecuperacao.registrarReenvio(
                            ConfirmacaoCodigoActivity.this, emailUsuario, novoCodigo);
                    codigoEsperado = novoCodigo;
                    limparCamposCodigo();

                    Toast.makeText(ConfirmacaoCodigoActivity.this,
                            "Novo código enviado para " + emailUsuario + " (Reenvio " + totalReenvios + "/" + GerenciadorBloqueioRecuperacao.MAX_REENVIOS + ")",
                            Toast.LENGTH_LONG).show();

                    iniciarOuAtualizarTimerReenvio();
                } else {
                    String erro = RecuperarSenhaEmailActivity.getUltimoErroEmail();
                    Toast.makeText(ConfirmacaoCodigoActivity.this,
                            "Erro ao reenviar código. " + (erro.isEmpty() ? "Tente novamente." : erro),
                            Toast.LENGTH_LONG).show();
                    atualizarBotaoReenvioDisponivel();
                }
            });
        }).start();
    }

    private void bloquearUsuarioEVoltarAoLogin() {
        reenviandoEmAndamento = true;
        cancelarTimerReenvio();
        if (btnReenviarCodigo != null) {
            btnReenviarCodigo.setEnabled(false);
        }

        // Registra o bloqueio localmente imediatamente e também sincroniza no banco
        GerenciadorBloqueioRecuperacao.registrarBloqueio(ConfirmacaoCodigoActivity.this, null, emailUsuario);

        new Thread(() -> {
            try (Connection con = ConexaoMySQL.conectar()) {
                if (con != null) {
                    GerenciadorBloqueioRecuperacao.registrarBloqueio(ConfirmacaoCodigoActivity.this, con, emailUsuario);
                }
            } catch (Exception ignored) {
            }

            runOnUiThread(() -> {
                Toast.makeText(ConfirmacaoCodigoActivity.this,
                        "Limite de 3 reenvios excedido! Seu login (e-mail) e IP foram bloqueados por 15 minutos.",
                        Toast.LENGTH_LONG).show();
                voltarParaLogin();
            });
        }).start();
    }

    private void iniciarOuAtualizarTimerReenvio() {
        cancelarTimerReenvio();
        if (btnReenviarCodigo == null) return;

        long tempoRestanteMs = GerenciadorBloqueioRecuperacao.obterTempoRestanteReenvioMs(this, emailUsuario);
        if (tempoRestanteMs > 0) {
            btnReenviarCodigo.setTextColor(ContextCompat.getColor(this, R.color.gray_text));
            String tempoInicial = GerenciadorBloqueioRecuperacao.formatarTempoMinutosSegundos(tempoRestanteMs);
            btnReenviarCodigo.setText(getString(R.string.resend_code_button) + " (" + tempoInicial + ")");

            countDownTimer = new CountDownTimer(tempoRestanteMs, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    if (btnReenviarCodigo != null) {
                        String tempo = GerenciadorBloqueioRecuperacao.formatarTempoMinutosSegundos(millisUntilFinished);
                        btnReenviarCodigo.setText(getString(R.string.resend_code_button) + " (" + tempo + ")");
                        btnReenviarCodigo.setTextColor(ContextCompat.getColor(ConfirmacaoCodigoActivity.this, R.color.gray_text));
                    }
                }

                @Override
                public void onFinish() {
                    atualizarBotaoReenvioDisponivel();
                }
            }.start();
        } else {
            atualizarBotaoReenvioDisponivel();
        }
    }

    private void atualizarBotaoReenvioDisponivel() {
        if (btnReenviarCodigo == null) return;
        int reenviosUsados = GerenciadorBloqueioRecuperacao.obterQuantidadeReenvios(this, emailUsuario);
        btnReenviarCodigo.setEnabled(true);
        btnReenviarCodigo.setTextColor(ContextCompat.getColor(this, R.color.senac_blue));
        btnReenviarCodigo.setText(getString(R.string.resend_code_button) + " (" + reenviosUsados + "/" + GerenciadorBloqueioRecuperacao.MAX_REENVIOS + ")");
    }

    private void cancelarTimerReenvio() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private void limparCamposCodigo() {
        EditText[] edits = new EditText[]{etCode1, etCode2, etCode3, etCode4, etCode5, etCode6};
        for (EditText edit : edits) {
            if (edit != null) {
                edit.setText("");
            }
        }
        if (etCode1 != null) {
            etCode1.requestFocus();
        }
    }

    private void voltarParaLogin() {
        Intent intent = new Intent(ConfirmacaoCodigoActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
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
