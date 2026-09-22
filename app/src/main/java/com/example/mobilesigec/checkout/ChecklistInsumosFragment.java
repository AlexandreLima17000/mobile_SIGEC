package com.example.mobilesigec.checkout;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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
import java.sql.Statement;
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
    private MaterialButton btnNovaReceita;
    private AppCompatButton btnSolicitarInsumo;

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
        btnNovaReceita = view.findViewById(R.id.btn_nova_receita);
        btnSolicitarInsumo = view.findViewById(R.id.btn_solicitar_insumo);

        btnLimparInsumos.setOnClickListener(v -> limparChecklist());
        btnConfirmarInsumos.setOnClickListener(v -> confirmarSeparacao());
        btnNovaReceita.setOnClickListener(v -> Toast.makeText(getContext(), "Redirecionar para criação", Toast.LENGTH_SHORT).show());

        // Chamada atualizada para o novo formulário
        btnSolicitarInsumo.setOnClickListener(v -> abrirDialogSolicitarInsumo());

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

        for (InsumoModelo insumo : listaInsumosAtual) {
            if (!insumo.isMarcado()) {
                Toast.makeText(getContext(), "Marque todos os insumos!", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        progressLoading.setVisibility(View.VISIBLE);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            boolean sucesso = false;
            try {
                Connection con = ConexaoMySQL.conectar();
                if (con != null) {
                    String sql = "UPDATE agendamento SET concluido = 'S' WHERE id_ficha = ?";
                    PreparedStatement stmt = con.prepareStatement(sql);
                    stmt.setInt(1, receitaSelecionada.getIdReceita());
                    if(stmt.executeUpdate() > 0) sucesso = true;
                    stmt.close(); con.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            final boolean finalSucesso = sucesso;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    progressLoading.setVisibility(View.GONE);
                    if (finalSucesso) {
                        Toast.makeText(getContext(), "Separação confirmada!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Erro ao confirmar no banco.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void abrirDialogSolicitarInsumo() {
        ReceitaModelo receitaSelecionada = (ReceitaModelo) spinnerReceitas.getSelectedItem();
        if (receitaSelecionada == null) {
            Toast.makeText(getContext(), "Selecione uma receita primeiro.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_solicitar_insumo, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        EditText etNome = view.findViewById(R.id.et_nome_insumo);
        EditText etQuantidade = view.findViewById(R.id.et_quantidade_insumo);
        EditText etUnidade = view.findViewById(R.id.et_unidade_insumo);
        EditText etObservacao = view.findViewById(R.id.et_observacao_insumo);

        view.findViewById(R.id.btn_fechar_dialog).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btn_cancelar_dialog).setOnClickListener(v -> dialog.dismiss());

        view.findViewById(R.id.btn_adicionar_dialog).setOnClickListener(v -> {
            String nome = etNome.getText().toString().trim();
            String qtdStr = etQuantidade.getText().toString().trim();
            String unidade = etUnidade.getText().toString().trim();
            String observacao = etObservacao.getText().toString().trim();

            if (nome.isEmpty() || qtdStr.isEmpty() || unidade.isEmpty()) {
                Toast.makeText(getContext(), "Preencha os campos obrigatórios.", Toast.LENGTH_SHORT).show();
                return;
            }

            int quantidade = Integer.parseInt(qtdStr);
            inserirInsumoExtra(nome, quantidade, unidade, observacao, receitaSelecionada.getIdReceita(), dialog);
        });

        dialog.show();
    }

    private void inserirInsumoExtra(String nome, int quantidade, String unidade, String observacao, int idFicha, AlertDialog dialog) {
        progressLoading.setVisibility(View.VISIBLE);
        dialog.dismiss();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            boolean sucesso = false;
            try {
                Connection con = ConexaoMySQL.conectar();
                if (con != null) {
                    con.setAutoCommit(false); // Inicia transação

                    //  Inserir o novo produto com o NOME LIMPO
                    String sqlProduto = "INSERT INTO produto (nome_produto, unidade, id_categoria, situacao) VALUES (?, ?, 1, 'A')";
                    PreparedStatement stmtProduto = con.prepareStatement(sqlProduto, Statement.RETURN_GENERATED_KEYS);
                    stmtProduto.setString(1, nome);
                    stmtProduto.setString(2, unidade);
                    stmtProduto.executeUpdate();

                    ResultSet rsKeys = stmtProduto.getGeneratedKeys();
                    int idProdutoGerado = -1;
                    if (rsKeys.next()) {
                        idProdutoGerado = rsKeys.getInt(1);
                    }
                    rsKeys.close();
                    stmtProduto.close();

                    //  Inserir o insumo com a flag extra = 'S'
                    if (idProdutoGerado != -1) {
                        String sqlInsumo = "INSERT INTO insumo (quantidade, cancelado, id_produto, id_ficha, extra) VALUES (?, 'N', ?, ?, 'S')";
                        PreparedStatement stmtInsumo = con.prepareStatement(sqlInsumo);
                        stmtInsumo.setInt(1, quantidade);
                        stmtInsumo.setInt(2, idProdutoGerado);
                        stmtInsumo.setInt(3, idFicha);
                        stmtInsumo.executeUpdate();
                        stmtInsumo.close();

                        con.commit(); // Confirma transação
                        sucesso = true;
                    } else {
                        con.rollback(); // Desfaz se falhou
                    }
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
                        Toast.makeText(getContext(), "Insumo extra adicionado!", Toast.LENGTH_SHORT).show();
                        carregarInsumosDaReceita(idFicha); // Atualiza a lista automaticamente
                    } else {
                        Toast.makeText(getContext(), "Erro ao adicionar insumo.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}