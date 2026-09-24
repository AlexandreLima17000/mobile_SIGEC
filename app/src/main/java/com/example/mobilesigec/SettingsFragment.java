package com.example.mobilesigec;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public class SettingsFragment extends Fragment {

    public SettingsFragment() {
        // Construtor público necessário
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Infla o layout fragment_configuracoes
        return inflater.inflate(R.layout.fragment_configuracoes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvNomeInstrutor = view.findViewById(R.id.tvNomeInstrutor);
        TextView tvEmailInstitucional = view.findViewById(R.id.tvEmailInstitucional);

        // Resgatar dados salvos do usuário na sessão
        FragmentActivity activity = getActivity();
        if (activity != null) {
            SharedPreferences prefs = activity.getSharedPreferences("SessaoApp", Context.MODE_PRIVATE);
            String nomeUsuario = prefs.getString("NOME_USUARIO", null);
            String emailUsuario = prefs.getString("EMAIL_USUARIO", null);

            if (nomeUsuario != null && !nomeUsuario.trim().isEmpty() && tvNomeInstrutor != null) {
                tvNomeInstrutor.setText(nomeUsuario);
            }

            if (emailUsuario != null && !emailUsuario.trim().isEmpty() && tvEmailInstitucional != null) {
                tvEmailInstitucional.setText(emailUsuario);
            }
        }

        // Botão Trocar Foto
        Button btnTrocarFoto = view.findViewById(R.id.btnTrocarFoto);
        if (btnTrocarFoto != null) {
            btnTrocarFoto.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Selecione uma nova foto de perfil", Toast.LENGTH_SHORT).show()
            );
        }
    }
}