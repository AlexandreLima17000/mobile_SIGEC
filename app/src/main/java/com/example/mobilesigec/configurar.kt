package com.example.mobilesigec

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar

class configurar : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_configurar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // Estilizar o item Sair para vermelho
        val menu = toolbar.menu
        val sairItem = menu.findItem(R.id.action_sair)
        if (sairItem != null) {
            val s = SpannableString(sairItem.title)
            s.setSpan(ForegroundColorSpan(Color.RED), 0, s.length, 0)
            sairItem.title = s
        }

        toolbar.setOnMenuItemClickListener { item ->
            val itemId = item.itemId
            if (itemId == R.id.action_notificacoes) {
                Toast.makeText(this, "Notificações clicadas", Toast.LENGTH_SHORT).show()
                true
            } else if (itemId == R.id.action_perfil) {
                finish() // Volta para a tela de perfil
                true
            } else if (itemId == R.id.action_configuracao) {
                // Já está em configurações
                true
            } else if (itemId == R.id.action_sair) {
                finishAffinity() // Fecha o app
                true
            } else {
                false
            }
        }
    }
}