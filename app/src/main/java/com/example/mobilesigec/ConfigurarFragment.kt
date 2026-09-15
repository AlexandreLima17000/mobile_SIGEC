package com.example.mobilesigec

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar

class ConfigurarFragment : Fragment(R.layout.fragment_configurar) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

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
                Toast.makeText(context, "Notificações clicadas", Toast.LENGTH_SHORT).show()
                true
            } else if (itemId == R.id.action_perfil) {
                parentFragmentManager.popBackStack() // Volta para a tela de perfil
                true
            } else if (itemId == R.id.action_configuracao) {
                // Já está em configurações
                true
            } else if (itemId == R.id.action_sair) {
                activity?.let {
                    ActivityCompat.finishAffinity(it)
                }
                true
            } else {
                false
            }
        }

        val tvVoltarPerfil = view.findViewById<TextView>(R.id.tvVoltarPerfil)
        tvVoltarPerfil?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}