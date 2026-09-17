package com.example.mobilesigec.checkout;

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
        progressLoading = view.findViewById(R.id.progress_loading); // Vinculando

        btnLimparInsumos = view.findViewById(R.id.btn_limpar_insumos);
        btnConfirmarInsumos = view.findViewById(R.id.btn_confirmar_insumos);
        btnNovaReceita = view.findViewById(R.id.btn_nova_receita);
        btnSolicitarInsumo = view.findViewById(R.id.btn_solicitar_insumo);

        btnLimparInsumos.setOnClickListener(v -> limparChecklist());
        btnConfirmarInsumos.setOnClickListener(v -> confirmarSeparacao());
        btnNovaReceita.setOnClickListener(v -> Toast.makeText(getContext(), "Redirecionar para criação", Toast.LENGTH_SHORT).show());
        btnSolicitarInsumo.setOnClickListener(v -> Toast.makeText(getContext(), "Abrir formulário", Toast.LENGTH_SHORT).show());

        carregarReceitasNoSpinner();
    }

    private void carregarReceitasNoSpinner() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("SessaoApp", Context.MODE_PRIVATE);
        int idUsuarioLogado = prefs.getInt("ID_USUARIO", -1);

        if (idUsuarioLogado == -1) return;

        progressLoading.setVisibility(View.VISIBLE); // Mostra o "carregando"
        spinnerReceitas.setEnabled(false); // Desativa o spinner para evitar toques apressados

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
                            "WHERE ut.id_usuario = ? AND t.situação = 'A'";

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

            // VOLTA PARA A THREAD PRINCIPAL PARA ATUALIZAR A TELA
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    progressLoading.setVisibility(View.GONE); // Esconde o "carregando"
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
        progressLoading.setVisibility(View.VISIBLE); // Mostra o "carregando" para a lista também!
        listaInsumosAtual.clear();

        if (adapter != null) {
            adapter.notifyDataSetChanged(); // Limpa a tela rapidamente antes de buscar o novo
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

            // VOLTA PARA A TELA PARA INJETAR A LISTA
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    progressLoading.setVisibility(View.GONE); // Esconde o "carregando"

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

        // Executando em Background também para não travar
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
}