package com.example.mobilesigec.ui.settings;

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

import com.example.mobilesigec.CacheManager;
import com.example.mobilesigec.R;
import com.example.mobilesigec.database.ConexaoMySQL;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";
    private static final String CHAVE_CACHE_PERFIL = "cache_perfil_instrutor";

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
        int idUsuarioLogado = prefs.getInt("ID_USUARIO", prefs.getInt("id_usuario", -1));
        String prefNome = prefs.getString("NOME_USUARIO", prefs.getString("nome_usuario", "Instrutor Gastronomia"));
        String prefEmail = prefs.getString("EMAIL_USUARIO", prefs.getString("email", ""));

        if (tvNomeInstrutor != null) {
            tvNomeInstrutor.setText(prefNome);
        }

        if (tvEmailInstitucional != null && !prefEmail.isEmpty()) {
            tvEmailInstitucional.setText("E-mail: " + prefEmail);
        }

        String identificadorCache = (idUsuarioLogado != -1) ? String.valueOf(idUsuarioLogado) : prefEmail;
        final String chaveCache = CHAVE_CACHE_PERFIL + "_" + identificadorCache;

        // 1. Tenta carregar os dados do perfil e turmas direto do cache local
        String jsonCache = CacheManager.obterJson(activity, chaveCache, CacheManager.TEMPO_EXPIRACAO_PADRAO);
        if (jsonCache != null) {
            try {
                JSONObject root = new JSONObject(jsonCache);
                String cacheNome = root.optString("nome", prefNome);
                String cacheEmail = root.optString("email", prefEmail);
                List<TurmaPerfilInfo> turmasCache = desserializarTurmasPerfil(root.optJSONArray("turmas"));

                atualizarUIPerfil(cacheNome, cacheEmail, turmasCache);
                return;
            } catch (Exception e) {
                Log.e(TAG, "Erro ao ler cache do perfil", e);
            }
        }

        // 2. Se não houver cache válido, consulta o MySQL
        if (progressLoadingPerfil != null) {
            progressLoadingPerfil.setVisibility(View.VISIBLE);
        }

        new Thread(() -> {
            String dbNome = prefNome;
            String dbEmail = prefEmail;
            List<TurmaPerfilInfo> listaTurmas = new ArrayList<>();
            boolean conexaoOk = false;

            try (Connection con = ConexaoMySQL.conectar()) {
                if (con != null) {
                    conexaoOk = true;
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

            if (conexaoOk && getContext() != null) {
                try {
                    JSONObject root = new JSONObject();
                    root.put("nome", dbNome);
                    root.put("email", dbEmail);
                    root.put("turmas", serializarTurmasPerfil(listaTurmas));

                    String jsonParaSalvar = root.toString();
                    CacheManager.salvarJson(getContext(), chaveCache, jsonParaSalvar);

                    int targetId = prefs.getInt("ID_USUARIO", idUsuarioLogado);
                    if (targetId != -1) {
                        CacheManager.salvarJson(getContext(), CHAVE_CACHE_PERFIL + "_" + targetId, jsonParaSalvar);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao salvar perfil no cache", e);
                }
            }

            final String finalNome = dbNome;
            final String finalEmail = dbEmail;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (progressLoadingPerfil != null) {
                        progressLoadingPerfil.setVisibility(View.GONE);
                    }
                    atualizarUIPerfil(finalNome, finalEmail, listaTurmas);
                });
            }
        }).start();
    }

    private void atualizarUIPerfil(String nome, String email, List<TurmaPerfilInfo> turmas) {
        if (!isAdded()) return;

        if (tvNomeInstrutor != null) {
            tvNomeInstrutor.setText(nome);
        }

        if (tvEmailInstitucional != null) {
            tvEmailInstitucional.setText("E-mail: " + email);
        }

        if (tvUnidade != null) {
            tvUnidade.setText("Unidade: Senac Aclimação");
        }

        if (tvTurmasAtivas != null) {
            tvTurmasAtivas.setText(String.valueOf(turmas.size()));
        }

        renderizarCardsTurmas(turmas);
    }

    private JSONArray serializarTurmasPerfil(List<TurmaPerfilInfo> lista) {
        JSONArray array = new JSONArray();
        for (TurmaPerfilInfo t : lista) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("idTurma", t.idTurma);
                obj.put("nomeTurma", t.nomeTurma);
                obj.put("nomeLaboratorio", t.nomeLaboratorio);
                obj.put("totalAlunos", t.totalAlunos);
                array.put(obj);
            } catch (Exception ignored) {}
        }
        return array;
    }

    private List<TurmaPerfilInfo> desserializarTurmasPerfil(JSONArray array) {
        List<TurmaPerfilInfo> lista = new ArrayList<>();
        if (array == null) return lista;

        for (int i = 0; i < array.length(); i++) {
            try {
                JSONObject obj = array.getJSONObject(i);
                lista.add(new TurmaPerfilInfo(
                        obj.getInt("idTurma"),
                        obj.getString("nomeTurma"),
                        obj.getString("nomeLaboratorio"),
                        obj.optInt("totalAlunos", 0)
                ));
            } catch (Exception ignored) {}
        }
        return lista;
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
            int p16 = (int) (16 * density);
            mainLayout.setPadding(p16, p16, p16, p16);

            LinearLayout headerLayout = new LinearLayout(getContext());
            headerLayout.setOrientation(LinearLayout.HORIZONTAL);
            android.graphics.drawable.GradientDrawable headerBg = new android.graphics.drawable.GradientDrawable();
            headerBg.setColor(Color.parseColor("#0B1A30"));
            headerBg.setCornerRadius(10 * density);
            headerLayout.setBackground(headerBg);
            int p12 = (int) (12 * density);
            int p10 = (int) (10 * density);
            headerLayout.setPadding(p12, p10, p12, p10);
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