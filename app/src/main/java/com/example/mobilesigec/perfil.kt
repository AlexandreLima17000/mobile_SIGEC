package com.example.mobilesigec

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar

class perfil : AppCompatActivity() {
    private var btnTrocarFoto: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_perfil)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)

        // Estilizar o item Sair para vermelho
        val menu = toolbar.menu
        val sairItem = menu.findItem(R.id.action_sair)
        if (sairItem != null) {
            val s = SpannableString(sairItem.title)
            s.setSpan(ForegroundColorSpan(Color.RED), 0, s.length, 0)
            sairItem.title = s
        }

        toolbar.setOnMenuItemClickListener(Toolbar.OnMenuItemClickListener { item ->
            val itemId = item.itemId
            if (itemId == R.id.action_notificacoes) {
                Toast.makeText(this, "Notificações clicadas", Toast.LENGTH_SHORT).show()
                return@OnMenuItemClickListener true
            } else if (itemId == R.id.action_perfil) {
                // Já está no perfil, nada a fazer ou recarregar
                return@OnMenuItemClickListener true
            } else if (itemId == R.id.action_configuracao) {
                val intent = Intent(this@perfil, configurar::class.java)
                startActivity(intent)
                return@OnMenuItemClickListener true
            } else if (itemId == R.id.action_sair) {
                finishAffinity() // Fecha o app
                return@OnMenuItemClickListener true
            }
            false
        })

        btnTrocarFoto = findViewById(R.id.btnTrocarFoto)
        btnTrocarFoto?.setOnClickListener {
            Toast.makeText(this, "Trocar Foto clicado", Toast.LENGTH_SHORT).show()
        }
    }
}