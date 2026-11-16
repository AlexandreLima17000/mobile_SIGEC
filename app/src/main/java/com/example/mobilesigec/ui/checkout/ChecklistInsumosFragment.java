package com.example.mobilesigec.ui.checkout;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilesigec.CacheManager;
import com.example.mobilesigec.R;
import com.example.mobilesigec.adapter.InsumoAdapter;
import com.example.mobilesigec.database.ConexaoMySQL;
import com.example.mobilesigec.model.InsumoModelo;
import com.example.mobilesigec.model.ReceitaModelo;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChecklistInsumosFragment extends Fragment {

    private Spinner spinnerReceitas;
    private TextView tvReceitaSelecionada;
    private TextView tvLaboratorioSelecionado;
    private TextView tvTurmaSelecionada;

    private RecyclerView rvInsumos;
    private InsumoAdapter adapter;
    private List<InsumoModelo> listaInsumosAtual = new ArrayList<>();
    private LinearProgressIndicator progressBarInsumos;
    private TextView tvProgressoTexto;
    private TextView tvProgressoPorcentagem;
    private ProgressBar progressLoading;

    private MaterialButton btnLimparInsumos;
    private MaterialButton btnConfirmarInsumos;

    public ChecklistInsumosFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_checklist_insumos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spinnerReceitas = view.findViewById(R.id.spinner_receitas_insumos);
        tvReceitaSelecionada = view.findViewById(R.id.tv_receita_selecionada);
        tvLaboratorioSelecionado = view.findViewById(R.id.tv_laboratorio_selecionado);
        tvTurmaSelecionada = view.findViewById(R.id.tv_turma_selecionada);

        rvInsumos = view.findViewById(R.id.rv_insumos);
        rvInsumos.setLayoutManager(new LinearLayoutManager(getContext()));

        progressBarInsumos = view.findViewById(R.id.progress_bar_insumos);
        tvProgressoTexto = view.findViewById(R.id.tv_progresso_texto_insumos);
        tvProgressoPorcentagem = view.findViewById(R.id.tv_progresso_porcentagem_insumos);
        progressLoading = view.findViewById(R.id.progress_loading);

        btnLimparInsumos = view.findViewById(R.id.btn_limpar_insumos);
        btnConfirmarInsumos = view.findViewById(R.id.btn_confirmar_insumos);

        btnLimparInsumos.setOnClickListener(v -> limparChecklist());
        btnConfirmarInsumos.setOnClickListener(v -> confirmarSeparacao());

        carregarReceitasNoSpinner();
    }

    private void carregarReceitasNoSpinner() {
        if (getContext() == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences("SessaoApp", Context.MODE_PRIVATE);
        int idUsuarioLogado = prefs.getInt("id_usuario", -1);
        if (idUsuarioLogado == -1) {
            idUsuarioLogado = prefs.getInt("ID_USUARIO", -1);
        }
        int idTurmaSelecionada = prefs.getInt("id_turma_selecionada", -1);

        if (idTurmaSelecionada == -1) return;

        String dataHoje = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String chaveCacheReceitasDia = CacheManager.CHAVE_CHECKLIST_INSUMOS + "_receitas_dia_"
                + idUsuarioLogado + "_" + idTurmaSelecionada + "_" + dataHoje;

        // 1. Tenta carregar as receitas do dia direto do cache local
        String jsonReceitasCache = CacheManager.obterJson(requireContext(), chaveCacheReceitasDia, CacheManager.TEMPO_EXPIRACAO_PADRAO);
        if (jsonReceitasCache != null) {
            List<ReceitaModelo> receitasCache = desserializarReceitasSpinner(jsonReceitasCache);
            if (!receitasCache.isEmpty()) {
                configurarSpinnerReceitas(receitasCache);
                return;
            }
        }

        // 2. Se não houver cache válido, busca no MySQL
        progressLoading.setVisibility(View.VISIBLE);
        spinnerReceitas.setEnabled(false);

        final int finalIdUsuario = idUsuarioLogado;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<ReceitaModelo> listaReceitas = new ArrayList<>();
            boolean conexaoOk = false;

            try (Connection con = ConexaoMySQL.conectar()) {
                if (con != null) {
                    conexaoOk = true;
                    String sql;
                    if (finalIdUsuario != -1) {
                        sql = "SELECT DISTINCT f.id_ficha, f.nome_ficha, t.nome_turma, l.nome_laboratorio " +
                                "FROM agendamento a " +
                                "INNER JOIN ficha f ON f.id_ficha = a.id_ficha " +
                                "INNER JOIN turma t ON t.id_turma = f.id_turma " +
                                "INNER JOIN laboratorio l ON l.id_laboratorio = t.id_laboratorio " +
                                "INNER JOIN usuario_turma ut ON ut.id_turma = t.id_turma " +
                                "WHERE ut.id_usuario = ? AND f.id_turma = ? AND DATE(a.data) = CURDATE() AND (a.situacao = 'A' OR a.situacao IS NULL OR a.situacao = '')";
                    } else {
                        sql = "SELECT DISTINCT f.id_ficha, f.nome_ficha, t.nome_turma, l.nome_laboratorio " +
                                "FROM agendamento a " +
                                "INNER JOIN ficha f ON f.id_ficha = a.id_ficha " +
                                "INNER JOIN turma t ON t.id_turma = f.id_turma " +
                                "INNER JOIN laboratorio l ON l.id_laboratorio = t.id_laboratorio " +
                                "WHERE f.id_turma = ? AND DATE(a.data) = CURDATE() AND (a.situacao = 'A' OR a.situacao IS NULL OR a.situacao = '')";
                    }

                    try (PreparedStatement stmt = con.prepareStatement(sql)) {
                        if (finalIdUsuario != -1) {
                            stmt.setInt(1, finalIdUsuario);
                            stmt.setInt(2, idTurmaSelecionada);
                        } else {
                            stmt.setInt(1, idTurmaSelecionada);
                        }
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                listaReceitas.add(new ReceitaModelo(
                                        rs.getInt("id_ficha"),
                                        rs.getString("nome_ficha"),
                                        rs.getString("nome_turma"),
                                        rs.getString("nome_laboratorio")
                                ));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (conexaoOk && !listaReceitas.isEmpty() && getContext() != null) {
                CacheManager.salvarJson(getContext(), chaveCacheReceitasDia, serializarReceitasSpinner(listaReceitas));
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    progressLoading.setVisibility(View.GONE);
                    spinnerReceitas.setEnabled(true);

                    if (!listaReceitas.isEmpty() && getContext() != null) {
                        configurarSpinnerReceitas(listaReceitas);
                    }
                });
            }
        });
    }

    private void configurarSpinnerReceitas(List<ReceitaModelo> listaReceitas) {
        if (!isAdded() || getContext() == null) return;

        ArrayAdapter<ReceitaModelo> spinnerAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                listaReceitas
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerReceitas.setAdapter(spinnerAdapter);

        spinnerReceitas.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ReceitaModelo receitaSelecionada = (ReceitaModelo) parent.getItemAtPosition(position);
                tvReceitaSelecionada.setText("Receita: " + receitaSelecionada.getNomeReceita());
                tvLaboratorioSelecionado.setText(receitaSelecionada.getNomeLaboratorio());
                tvTurmaSelecionada.setText(receitaSelecionada.getNomeTurma() + " • " + receitaSelecionada.getNomeLaboratorio());

                carregarInsumosDaReceita(receitaSelecionada.getIdReceita());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void carregarInsumosDaReceita(int idFicha) {
        if (getContext() == null) return;

        listaInsumosAtual.clear();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        final String chaveCacheItens = CacheManager.CHAVE_CHECKLIST_INSUMOS + "_itens_" + idFicha;

        // 1. Tenta carregar os insumos desta ficha direto do cache local
        String jsonItensCache = CacheManager.obterJson(requireContext(), chaveCacheItens, CacheManager.TEMPO_EXPIRACAO_PADRAO);
        if (jsonItensCache != null) {
            List<InsumoModelo> insumosCache = desserializarInsumos(jsonItensCache);
            listaInsumosAtual.addAll(insumosCache);
            exibirListaInsumos();
            return;
        }

        // 2. Se não houver cache válido, busca no MySQL
        progressLoading.setVisibility(View.VISIBLE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            JSONArray jsonArrayParaCache = new JSONArray();
            boolean conexaoOk = false;

            try (Connection con = ConexaoMySQL.conectar()) {
                if (con != null) {
                    conexaoOk = true;
                    String sql = "SELECT i.id_insumo, p.nome_produto, i.quantidade, p.unidade " +
                            "FROM insumo i " +
                            "INNER JOIN produto p ON i.id_produto = p.id_produto " +
                            "WHERE i.id_ficha = ? AND (i.cancelado = 'N' OR i.cancelado IS NULL OR i.cancelado = '' OR i.cancelado = '0')";

                    try (PreparedStatement stmt = con.prepareStatement(sql)) {
                        stmt.setInt(1, idFicha);
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                int idInsumo = rs.getInt("id_insumo");
                                String nomeProduto = rs.getString("nome_produto");
                                String qtd = String.valueOf(rs.getInt("quantidade"));
                                String unidade = rs.getString("unidade");

                                listaInsumosAtual.add(new InsumoModelo(idInsumo, nomeProduto, qtd, unidade));

                                JSONObject obj = new JSONObject();
                                obj.put("id_insumo", idInsumo);
                                obj.put("nome_produto", nomeProduto != null ? nomeProduto : "");
                                obj.put("quantidade", qtd);
                                obj.put("unidade", unidade != null ? unidade : "");
                                jsonArrayParaCache.put(obj);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (conexaoOk && getContext() != null) {
                CacheManager.salvarJson(getContext(), chaveCacheItens, jsonArrayParaCache.toString());
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    progressLoading.setVisibility(View.GONE);
                    exibirListaInsumos();
                });
            }
        });
    }

    private void exibirListaInsumos() {
        if (!isAdded()) return;
        adapter = new InsumoAdapter(listaInsumosAtual, this::atualizarProgresso);
        rvInsumos.setAdapter(adapter);
        atualizarProgresso();
    }

    private void atualizarProgresso() {
        if (listaInsumosAtual == null || listaInsumosAtual.isEmpty()) {
            progressBarInsumos.setProgress(0);
            tvProgressoTexto.setText("0 de 0 itens marcados");
            tvProgressoPorcentagem.setText("0%");
            return;
        }

        int totalItens = listaInsumosAtual.size();
        int itensMarcados = 0;
        for (InsumoModelo insumo : listaInsumosAtual) {
            if (insumo.isMarcado()) itensMarcados++;
        }
        int porcentagem = (int) (((float) itensMarcados / totalItens) * 100);

        progressBarInsumos.setProgress(porcentagem);
        tvProgressoTexto.setText(itensMarcados + " de " + totalItens + " itens marcados");
        tvProgressoPorcentagem.setText(porcentagem + "%");
    }

    private void limparChecklist() {
        if (listaInsumosAtual != null && adapter != null) {
            for (InsumoModelo insumo : listaInsumosAtual) {
                insumo.setMarcado(false);
            }
            adapter.notifyDataSetChanged();
            atualizarProgresso();
        }
    }

    private void confirmarSeparacao() {
        ReceitaModelo receitaSelecionada = (ReceitaModelo) spinnerReceitas.getSelectedItem();
        if (receitaSelecionada == null || listaInsumosAtual.isEmpty()) {
            Toast.makeText(getContext(), "Nenhuma receita disponível.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<InsumoModelo> insumosUtilizados = new ArrayList<>();
        for (InsumoModelo insumo : listaInsumosAtual) {
            if (insumo.isMarcado()) {
                insumosUtilizados.add(insumo);
            }
        }

        if (insumosUtilizados.isEmpty()) {
            Toast.makeText(getContext(), "Marque pelo menos um insumo utilizado antes de confirmar!", Toast.LENGTH_LONG).show();
            return;
        }

        progressLoading.setVisibility(View.VISIBLE);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            boolean sucesso = false;
            try {
                Connection con = ConexaoMySQL.conectar();
                if (con != null) {
                    con.setAutoCommit(false);

                    String sqlEstoque = "INSERT INTO movimentacao_estoque (tipo_movimentacao, quantidade, id_produto, id_insumo, observacao) " +
                            "VALUES ('SAIDA', ?, (SELECT id_produto FROM insumo WHERE id_insumo = ?), ?, 'Baixa confirmada pelo instrutor')";
                    PreparedStatement stmtEstoque = con.prepareStatement(sqlEstoque);

                    String sqlBaixaProduto = "UPDATE produto SET quantidade = quantidade - ? WHERE id_produto = (SELECT id_produto FROM insumo WHERE id_insumo = ?)";
                    PreparedStatement stmtBaixaProduto = con.prepareStatement(sqlBaixaProduto);

                    for (InsumoModelo insumo : insumosUtilizados) {
                        double qtdUtilizada = Double.parseDouble(insumo.getQuantidade());
                        int idInsumo = insumo.getIdInsumo();

                        stmtEstoque.setDouble(1, qtdUtilizada);
                        stmtEstoque.setInt(2, idInsumo);
                        stmtEstoque.setInt(3, idInsumo);
                        stmtEstoque.executeUpdate();

                        stmtBaixaProduto.setDouble(1, qtdUtilizada);
                        stmtBaixaProduto.setInt(2, idInsumo);
                        stmtBaixaProduto.executeUpdate();
                    }

                    stmtEstoque.close();
                    stmtBaixaProduto.close();

                    con.commit();
                    sucesso = true;
                    con.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            final boolean finalSucesso = sucesso;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    progressLoading.setVisibility(View.GONE);
                    if (finalSucesso) {
                        if (getContext() != null) {
                            CacheManager.invalidarChave(
                                    getContext(),
                                    CacheManager.CHAVE_CHECKLIST_INSUMOS + "_itens_" + receitaSelecionada.getIdReceita()
                            );
                        }
                        Toast.makeText(getContext(), "Separação e baixa de estoque concluídas com sucesso!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "Erro ao processar a baixa no estoque.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    // --- MÉTODOS DE CONVERSÃO JSON PARA O CACHE ---

    private String serializarReceitasSpinner(List<ReceitaModelo> lista) {
        JSONArray array = new JSONArray();
        for (ReceitaModelo r : lista) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id_ficha", r.getIdReceita());
                obj.put("nome_ficha", r.getNomeReceita());
                obj.put("nome_turma", r.getNomeTurma());
                obj.put("nome_laboratorio", r.getNomeLaboratorio());
                array.put(obj);
            } catch (Exception ignored) {}
        }
        return array.toString();
    }

    private List<ReceitaModelo> desserializarReceitasSpinner(String json) {
        List<ReceitaModelo> lista = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                lista.add(new ReceitaModelo(
                        obj.getInt("id_ficha"),
                        obj.getString("nome_ficha"),
                        obj.getString("nome_turma"),
                        obj.getString("nome_laboratorio")
                ));
            }
        } catch (Exception ignored) {}
        return lista;
    }

    private List<InsumoModelo> desserializarInsumos(String json) {
        List<InsumoModelo> lista = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                lista.add(new InsumoModelo(
                        obj.getInt("id_insumo"),
                        obj.getString("nome_produto"),
                        obj.getString("quantidade"),
                        obj.getString("unidade")
                ));
            }
        } catch (Exception ignored) {}
        return lista;
    }
}