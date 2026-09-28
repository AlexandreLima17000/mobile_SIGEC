package com.example.mobilesigec;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

public class ConfirmacaoCodigoActivity extends AppCompatActivity {

    private EditText etCode1, etCode2, etCode3, etCode4, etCode5, etCode6;
    private String emailUsuario;
    private String codigoEsperado;

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

        // Botão "Voltar ao Login"
        tvVoltarLogin.setOnClickListener(v -> {
            Intent intent = new Intent(ConfirmacaoCodigoActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
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
