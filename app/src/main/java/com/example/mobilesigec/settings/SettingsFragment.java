package com.example.mobilesigec.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.mobilesigec.ConexaoMySQL;
import com.example.mobilesigec.R;
import com.google.android.material.card.MaterialCardView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";

    private TextView tvNomeInstrutor;
    private TextView tvEmailInstitucional;
    private TextView tvUnidade;
    private TextView tvTurmasAtivas;
    private LinearLayout containerTurmas;
    private ProgressBar progressLoadingPerfil;

    public SettingsFragment() {
        // Construtor público necessário
    }

    private static class TurmaPerfilInfo {
        int idTurma;
        String nomeTurma;
        String nomeLaboratorio;
        int totalAlunos;

        TurmaPerfilInfo(int idTurma, String nomeTurma, String nomeLaboratorio, int totalAlunos) {
            this.idTurma = idTurma;
            this.nomeTurma = nomeTurma;
            this.nomeLaboratorio = nomeLaboratorio;
            this.totalAlunos = totalAlunos;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_configuracoes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvNomeInstrutor = view.findViewById(R.id.tvNomeInstrutor);
        tvEmailInstitucional = view.findViewById(R.id.tvEmailInstitucional);
        tvUnidade = view.findViewById(R.id.tvUnidade);
        tvTurmasAtivas = view.findViewById(R.id.tvTurmasAtivas);
        containerTurmas = view.findViewById(R.id.container_turmas);
        progressLoadingPerfil = view.findViewById(R.id.progress_loading_perfil);

        carregarDadosPerfilEPerfilBanco();
    }

    private void carregarDadosPerfilEPerfilBanco() {
        FragmentActivity activity = getActivity();
        if (activity == null) return;

        SharedPreferences prefs = activity.getSharedPreferences("SessaoApp", Context.MODE_PRIVATE);
        int idUsuarioLogado = prefs.getInt("ID_USUARIO", -1);
        String prefNome = prefs.getString("NOME_USUARIO", "Instrutor Gastronomia");
        String prefEmail = prefs.getString("EMAIL_USUARIO", "");

        // Preenche com os dados salvos da sessão imediatamente
        if (tvNomeInstrutor != null) {
            tvNomeInstrutor.setText(prefNome);
        }

        if (tvEmailInstitucional != null && !prefEmail.isEmpty()) {
            tvEmailInstitucional.setText("E-mail: " + prefEmail);
        }

        if (progressLoadingPerfil != null) {
            progressLoadingPerfil.setVisibility(View.VISIBLE);
        }

        new Thread(() -> {
            String dbNome = prefNome;
            String dbEmail = prefEmail;
            List<TurmaPerfilInfo> listaTurmas = new ArrayList<>();

            try (Connection con = ConexaoMySQL.conectar()) {
                if (con != null) {
                    // 1. Busca dados atualizados do usuário se ID for válido
                    if (idUsuarioLogado != -1) {
                        String sqlUser = "SELECT * FROM usuario WHERE id_usuario = ?";
                        try (PreparedStatement stmtUser = con.prepareStatement(sqlUser)) {
                            stmtUser.setInt(1, idUsuarioLogado);
                            try (ResultSet rsUser = stmtUser.executeQuery()) {
                                if (rsUser.next()) {
                                    try {
                                        String e = rsUser.getString("email");
                                        if (e != null && !e.trim().isEmpty()) dbEmail = e;
                                    } catch (Exception ignored) {}

                                    try {
                                        java.sql.ResultSetMetaData metaData = rsUser.getMetaData();
                                        for (int i = 1; i <= metaData.getColumnCount(); i++) {
                                            String colName = metaData.getColumnName(i).toLowerCase();
                                            if ((colName.contains("nome") || colName.contains("usuario") || i == 2) && !colName.equals("id_usuario") && !colName.contains("email") && !colName.contains("senha")) {
                                                String val = rsUser.getString(i);
                                                if (val != null && !val.trim().isEmpty() && !val.contains("@")) {
                                                    dbNome = val.trim();
                                                    break;
                                                }
                                            }
                                        }
                                    } catch (Exception ignored) {}
                                }
                            }
                        }
                    } else if (!prefEmail.isEmpty()) {
                        String sqlUser = "SELECT * FROM usuario WHERE email = ?";
                        try (PreparedStatement stmtUser = con.prepareStatement(sqlUser)) {
                            stmtUser.setString(1, prefEmail);
                            try (ResultSet rsUser = stmtUser.executeQuery()) {
                                if (rsUser.next()) {
                                    int id = rsUser.getInt("id_usuario");
                                    try {
                                        String e = rsUser.getString("email");
                                        if (e != null && !e.trim().isEmpty()) dbEmail = e;
                                    } catch (Exception ignored) {}

                                    try {
                                        java.sql.ResultSetMetaData metaData = rsUser.getMetaData();
                                        for (int i = 1; i <= metaData.getColumnCount(); i++) {
                                            String colName = metaData.getColumnName(i).toLowerCase();
                                            if ((colName.contains("nome") || colName.contains("usuario") || i == 2) && !colName.equals("id_usuario") && !colName.contains("email") && !colName.contains("senha")) {
                                                String val = rsUser.getString(i);
                                                if (val != null && !val.trim().isEmpty() && !val.contains("@")) {
                                                    dbNome = val.trim();
                                                    break;
                                                }
                                            }
                                        }
                                    } catch (Exception ignored) {}

                                    prefs.edit().putInt("ID_USUARIO", id).apply();
                                }
                            }
                        }
                    }

                    int targetId = prefs.getInt("ID_USUARIO", idUsuarioLogado);

                    // 2. Busca turmas e laboratórios vinculados ao instrutor
                    if (targetId != -1) {
                        String sqlTurmas = "SELECT t.id_turma, t.nome_turma, l.nome_laboratorio " +
                                "FROM turma t " +
                                "INNER JOIN usuario_turma ut ON t.id_turma = ut.id_turma " +
                                "INNER JOIN laboratorio l ON t.id_laboratorio = l.id_laboratorio " +
                                "WHERE ut.id_usuario = ? AND t.situacao = 'A'";

                        try (PreparedStatement stmtT = con.prepareStatement(sqlTurmas)) {
                            stmtT.setInt(1, targetId);
                            try (ResultSet rsT = stmtT.executeQuery()) {
                                while (rsT.next()) {
                                    int idT = rsT.getInt("id_turma");
                                    String nomeT = rsT.getString("nome_turma");
                                    String nomeL = rsT.getString("nome_laboratorio");
                                    listaTurmas.add(new TurmaPerfilInfo(idT, nomeT, nomeL, 0));
                                }
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                Log.e(TAG, "Erro ao carregar perfil do banco: " + e.getMessage(), e);
            }

            final String finalNome = dbNome;
            final String finalEmail = dbEmail;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (progressLoadingPerfil != null) {
                        progressLoadingPerfil.setVisibility(View.GONE);
                    }

                    if (tvNomeInstrutor != null) {
                        tvNomeInstrutor.setText(finalNome);
                    }

                    if (tvEmailInstitucional != null) {
                        tvEmailInstitucional.setText("E-mail: " + finalEmail);
                    }

                    if (tvUnidade != null) {
                        tvUnidade.setText("Unidade: Senac Aclimação");
                    }

                    if (tvTurmasAtivas != null) {
                        tvTurmasAtivas.setText(String.valueOf(listaTurmas.size()));
                    }

                    renderizarCardsTurmas(listaTurmas);
                });
            }
        }).start();
    }

    private void renderizarCardsTurmas(List<TurmaPerfilInfo> turmas) {
        if (containerTurmas == null || getContext() == null) return;

        containerTurmas.removeAllViews();

        if (turmas.isEmpty()) {
            TextView tvEmpty = new TextView(getContext());
            tvEmpty.setText("Nenhuma turma ativa vinculada ao instrutor.");
            tvEmpty.setTextColor(Color.parseColor("#64748B"));
            tvEmpty.setTextSize(14);
            tvEmpty.setPadding(0, 16, 0, 16);
            containerTurmas.addView(tvEmpty);
            return;
        }

        float density = getResources().getDisplayMetrics().density;

        for (TurmaPerfilInfo turma : turmas) {
            MaterialCardView card = new MaterialCardView(requireContext());
            card.setRadius(16 * density);
            card.setCardElevation(2 * density);
            card.setCardBackgroundColor(Color.WHITE);
            card.setStrokeWidth(0);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, (int) (16 * density));
            card.setLayoutParams(cardParams);

            LinearLayout mainLayout = new LinearLayout(getContext());
            mainLayout.setOrientation(LinearLayout.VERTICAL);
            mainLayout.setPadding((int) (16 * density), (int) (16 * density), (int) (16 * density), (int) (16 * density));

            // Header azul do card da turma
            LinearLayout headerLayout = new LinearLayout(getContext());
            headerLayout.setOrientation(LinearLayout.HORIZONTAL);
            headerLayout.setBackgroundColor(Color.parseColor("#0B1A30"));
            headerLayout.setPadding((int) (10 * density), (int) (10 * density), (int) (10 * density), (int) (10 * density));
            headerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView tvHeaderTitle = new TextView(getContext());
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tvHeaderTitle.setLayoutParams(titleParams);
            tvHeaderTitle.setText("LOTAÇÃO & LABORATÓRIO");
            tvHeaderTitle.setTextColor(Color.WHITE);
            tvHeaderTitle.setTextSize(13);
            tvHeaderTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            headerLayout.addView(tvHeaderTitle);

            TextView tvHeaderAno = new TextView(getContext());
            tvHeaderAno.setText("SIGEC 2026");
            tvHeaderAno.setTextColor(Color.parseColor("#D97706"));
            tvHeaderAno.setTextSize(12);
            tvHeaderAno.setTypeface(null, android.graphics.Typeface.BOLD);
            headerLayout.addView(tvHeaderAno);

            mainLayout.addView(headerLayout);

            // Nome da Turma e Laboratorio
            TextView tvTurmaNome = new TextView(getContext());
            LinearLayout.LayoutParams nomeParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            nomeParams.setMargins(0, (int) (12 * density), 0, 0);
            tvTurmaNome.setLayoutParams(nomeParams);
            tvTurmaNome.setText(turma.nomeTurma + " • " + turma.nomeLaboratorio);
            tvTurmaNome.setTextColor(Color.parseColor("#0B1A30"));
            tvTurmaNome.setTextSize(15);
            tvTurmaNome.setTypeface(null, android.graphics.Typeface.BOLD);
            mainLayout.addView(tvTurmaNome);

            // Laboratório Centralizado
            LinearLayout statsLayout = new LinearLayout(getContext());
            statsLayout.setOrientation(LinearLayout.VERTICAL);
            statsLayout.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams statsParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            statsParams.setMargins(0, (int) (14 * density), 0, 0);
            statsLayout.setLayoutParams(statsParams);

            TextView tvLabLabel = new TextView(getContext());
            tvLabLabel.setText("Laboratório");
            tvLabLabel.setGravity(android.view.Gravity.CENTER);
            tvLabLabel.setTextColor(Color.parseColor("#0B1A30"));
            tvLabLabel.setTextSize(15);
            tvLabLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            statsLayout.addView(tvLabLabel);

            TextView tvLabVal = new TextView(getContext());
            tvLabVal.setText(turma.nomeLaboratorio);
            tvLabVal.setGravity(android.view.Gravity.CENTER);
            tvLabVal.setTextColor(Color.parseColor("#D97706"));
            tvLabVal.setTextSize(16);
            tvLabVal.setTypeface(null, android.graphics.Typeface.BOLD);
            statsLayout.addView(tvLabVal);

            mainLayout.addView(statsLayout);
            card.addView(mainLayout);
            containerTurmas.addView(card);
        }
    }
}
