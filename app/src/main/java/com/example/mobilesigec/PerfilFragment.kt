package com.example.mobilesigec

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar

class PerfilFragment : Fragment(R.layout.fragment_perfil) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)

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
                Toast.makeText(context, "Notificações clicadas", Toast.LENGTH_SHORT).show()
                return@OnMenuItemClickListener true
            } else if (itemId == R.id.action_perfil) {
                // Já está no perfil
                return@OnMenuItemClickListener true
            } else if (itemId == R.id.action_configuracao) {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, ConfigurarFragment())
                    .addToBackStack(null)
                    .commit()
                return@OnMenuItemClickListener true
            } else if (itemId == R.id.action_sair) {
                activity?.let {
                    ActivityCompat.finishAffinity(it)
                }
                return@OnMenuItemClickListener true
            }
            false
        })

        val btnTrocarFoto = view.findViewById<Button>(R.id.btnTrocarFoto)
        btnTrocarFoto?.setOnClickListener {
            Toast.makeText(context, "Trocar Foto clicado", Toast.LENGTH_SHORT).show()
        }
    }
}