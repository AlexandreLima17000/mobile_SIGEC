package com.example.mobilesigec.checkout;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mobilesigec.ConexaoMySQL;
import com.example.mobilesigec.R;
import com.example.mobilesigec.adapter.InsumoAdapter;
import com.example.mobilesigec.model.InsumoModelo;
import com.example.mobilesigec.model.ReceitaModelo;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
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
        SharedPreferences prefs = requireActivity().getSharedPreferences("SessaoApp", Context.MODE_PRIVATE);
        int idUsuarioLogado = prefs.getInt("ID_USUARIO", -1);

        if (idUsuarioLogado == -1) return;

        progressLoading.setVisibility(View.VISIBLE);
        spinnerReceitas.setEnabled(false);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<ReceitaModelo> listaReceitas = new ArrayList<>();
            try {
                Connection con = ConexaoMySQL.conectar();
                if (con != null) {
                    String sql = "SELECT f.id_ficha, f.nome_ficha, t.nome_turma, l.nome_laboratorio " +
                            "FROM ficha f " +
                            "INNER JOIN turma t ON f.id_turma = t.id_turma " +
                            "INNER JOIN laboratorio l ON t.id_laboratorio = l.id_laboratorio " +
                            "INNER JOIN usuario_turma ut ON t.id_turma = ut.id_turma " +
                            "WHERE ut.id_usuario = ? AND t.situacao = 'A'";

                    PreparedStatement stmt = con.prepareStatement(sql);
                    stmt.setInt(1, idUsuarioLogado);
                    ResultSet rs = stmt.executeQuery();

                    while (rs.next()) {
                        listaReceitas.add(new ReceitaModelo(
                                rs.getInt("id_ficha"),
                                rs.getString("nome_ficha"),
                                rs.getString("nome_turma"),
                                rs.getString("nome_laboratorio")
                        ));
                    }
                    rs.close(); stmt.close(); con.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    progressLoading.setVisibility(View.GONE);
                    spinnerReceitas.setEnabled(true);

                    if (!listaReceitas.isEmpty() && getContext() != null) {
                        ArrayAdapter<ReceitaModelo> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, listaReceitas);
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
                });
            }
        });
    }

    private void carregarInsumosDaReceita(int idFicha) {
        progressLoading.setVisibility(View.VISIBLE);
        listaInsumosAtual.clear();

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                Connection con = ConexaoMySQL.conectar();
                if (con != null) {
                    String sql = "SELECT i.id_insumo, p.nome_produto, i.quantidade, p.unidade " +
                            "FROM insumo i " +
                            "INNER JOIN produto p ON i.id_produto = p.id_produto " +
                            "WHERE i.id_ficha = ? AND i.cancelado = 'N'";

                    PreparedStatement stmt = con.prepareStatement(sql);
                    stmt.setInt(1, idFicha);
                    ResultSet rs = stmt.executeQuery();

                    while (rs.next()) {
                        listaInsumosAtual.add(new InsumoModelo(
                                rs.getInt("id_insumo"),
                                rs.getString("nome_produto"),
                                String.valueOf(rs.getInt("quantidade")),
                                rs.getString("unidade")
                        ));
                    }
                    rs.close(); stmt.close(); con.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    progressLoading.setVisibility(View.GONE);

                    adapter = new InsumoAdapter(listaInsumosAtual, () -> atualizarProgresso());
                    rvInsumos.setAdapter(adapter);
                    atualizarProgresso();
                });
            }
        });
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

        // 1. Filtrar apenas os itens que foram realmente marcados pelo instrutor
        List<InsumoModelo> insumosUtilizados = new ArrayList<>();
        for (InsumoModelo insumo : listaInsumosAtual) {
            if (insumo.isMarcado()) {
                insumosUtilizados.add(insumo);
            }
        }

        // 2. Valida se ele marcou pelo menos alguma coisa
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
                    // Desliga o commit automático para garantir que a baixa de estoque
                    // e o agendamento sejam salvos juntos (Transação Segura)
                    con.setAutoCommit(false);

                    // Passo A: Atualizar o agendamento da aula para concluído
                    String sqlAgendamento = "UPDATE agendamento SET concluido = 'S' WHERE id_ficha = ?";
                    PreparedStatement stmtAgendamento = con.prepareStatement(sqlAgendamento);
                    stmtAgendamento.setInt(1, receitaSelecionada.getIdReceita());
                    stmtAgendamento.executeUpdate();
                    stmtAgendamento.close();

                    // Passo B: Registrar a SAÍDA no estoque apenas para os insumos marcados na tela
                    // O sub-select '(SELECT id_produto...)' acha a qual produto aquele insumo pertence.
                    String sqlEstoque = "INSERT INTO movimentacao_estoque (tipo_movimentacao, quantidade, id_produto, id_insumo, observacao) " +
                            "VALUES ('SAIDA', ?, (SELECT id_produto FROM insumo WHERE id_insumo = ?), ?, 'Baixa confirmada pelo instrutor')";

                    PreparedStatement stmtEstoque = con.prepareStatement(sqlEstoque);

                    // Roda o INSERT para cada item que estava com a caixa marcada (isMarcado = true)
                    for (InsumoModelo insumo : insumosUtilizados) {
                        stmtEstoque.setDouble(1, Double.parseDouble(insumo.getQuantidade())); // Quantidade gasta
                        stmtEstoque.setInt(2, insumo.getIdInsumo()); // Usado no sub-select para achar o produto
                        stmtEstoque.setInt(3, insumo.getIdInsumo()); // Preenche a coluna id_insumo
                        stmtEstoque.executeUpdate();
                    }
                    stmtEstoque.close();

                    // Passo C: Salva tudo de forma definitiva no banco
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
                        Toast.makeText(getContext(), "Separação e baixa de estoque concluídas com sucesso!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "Erro ao processar a baixa no estoque.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}