package com.example.mobilesigec;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class HomePageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_painel_instrutor);

        // 1. Encontra o "Espaço Fantasma" que criamos no XML
        View systemBarSpacer = findViewById(R.id.systemBarSpacer);

        // 2. Faz o espaço fantasma crescer exatamente do tamanho da barra do celular
        ViewCompat.setOnApplyWindowInsetsListener(systemBarSpacer, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Pega os parâmetros da view e altera a altura.
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.height = insets.bottom;
            view.setLayoutParams(params);

            return windowInsets;
        });
    }
}