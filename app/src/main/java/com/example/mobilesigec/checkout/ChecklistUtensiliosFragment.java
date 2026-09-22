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
import com.example.mobilesigec.adapter.UtensilioAdapter;
import com.example.mobilesigec.model.ReceitaModelo;
import com.example.mobilesigec.model.UtensilioModelo;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChecklistUtensiliosFragment extends Fragment {

    private Spinner spinnerReceitas;
    private TextView tvReceitaSelecionada;
    private TextView tvLaboratorioSelecionado;
    private TextView tvTurmaSelecionada;

    private RecyclerView rvUtensilios;
    private UtensilioAdapter adapter;
    private List<UtensilioModelo> listaUtensiliosAtual = new ArrayList<>();
    private LinearProgressIndicator progressBarUtensilios;
    private TextView tvProgressoTexto;
    private TextView tvProgressoPorcentagem;
    private ProgressBar progressLoading;

    private MaterialButton btnLimparUtensilios;
    private MaterialButton btnConfirmarUtensilios;

    public ChecklistUtensiliosFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_checklist_utensilios, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spinnerReceitas = view.findViewById(R.id.spinner_receitas_utensilios);
        tvReceitaSelecionada = view.findViewById(R.id.tv_receita_selecionada_utensilios);
        tvLaboratorioSelecionado = view.findViewById(R.id.tv_laboratorio_selecionado_utensilios);
        tvTurmaSelecionada = view.findViewById(R.id.tv_turma_selecionada_utensilios);

        rvUtensilios = view.findViewById(R.id.rv_utensilios);
        rvUtensilios.setLayoutManager(new LinearLayoutManager(getContext()));

        progressBarUtensilios = view.findViewById(R.id.progress_bar_utensilios);
        tvProgressoTexto = view.findViewById(R.id.tv_progresso_texto_utensilios);
        tvProgressoPorcentagem = view.findViewById(R.id.tv_progresso_porcentagem_utensilios);
        progressLoading = view.findViewById(R.id.progress_loading_utensilios);

        btnLimparUtensilios = view.findViewById(R.id.btn_limpar_utensilios);
        btnConfirmarUtensilios = view.findViewById(R.id.btn_confirmar_utensilios);

        btnLimparUtensilios.setOnClickListener(v -> limparChecklist());
        btnConfirmarUtensilios.setOnClickListener(v -> confirmarUtensilios());

        carregarReceitasNoSpinner();
    }

    private void carregarReceitasNoSpinner() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("SessaoApp", Context.MODE_PRIVATE);
        int idUsuarioLogado = prefs.getInt("ID_USUARIO", -1);

        if (idUsuarioLogado == -1) return;

        progressLoading.setVisibility(View.VISIBLE); // Mostra o "carregando"
        spinnerReceitas.setEnabled(false); // Desativa o spinner para evitar cliques acidentais

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

            // Volta para a thread principal (UI Thread) para atualizar a tela
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

                                carregarUtensiliosDaReceita(receitaSelecionada.getIdReceita());
                            }
                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {}
                        });
                    }
                });
            }
        });
    }

    private void carregarUtensiliosDaReceita(int idFicha) {
        progressLoading.setVisibility(View.VISIBLE); // Mostra o "carregando" para a lista
        listaUtensiliosAtual.clear();

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                Connection con = ConexaoMySQL.conectar();
                if (con != null) {
                    String sql = "SELECT u.id_utensilio, u.nome_utensilio, u.quantidade " +
                            "FROM checklist_utensilho cu " +
                            "INNER JOIN utensilio u ON cu.id_utensilio = u.id_utensilio " +
                            "WHERE cu.id_ficha = ? AND cu.situacao = 'A'";

                    PreparedStatement stmt = con.prepareStatement(sql);
                    stmt.setInt(1, idFicha);
                    ResultSet rs = stmt.executeQuery();

                    while (rs.next()) {
                        int idUtensilio = rs.getInt("id_utensilio");
                        String nome = rs.getString("nome_utensilio");
                        String qtd = String.valueOf(rs.getInt("quantidade"));

                        listaUtensiliosAtual.add(new UtensilioModelo(idUtensilio, nome, qtd));
                    }
                    rs.close(); stmt.close(); con.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Volta para a tela para injetar a lista
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    progressLoading.setVisibility(View.GONE); // Esconde o "carregando"

                    adapter = new UtensilioAdapter(listaUtensiliosAtual, () -> atualizarProgresso());
                    rvUtensilios.setAdapter(adapter);
                    atualizarProgresso();
                });
            }
        });
    }

    private void atualizarProgresso() {
        if (listaUtensiliosAtual == null || listaUtensiliosAtual.isEmpty()) {
            progressBarUtensilios.setProgress(0);
            tvProgressoTexto.setText("0 de 0 itens marcados");
            tvProgressoPorcentagem.setText("0%");
            return;
        }

        int totalItens = listaUtensiliosAtual.size();
        int itensMarcados = 0;

        for (UtensilioModelo utensilio : listaUtensiliosAtual) {
            if (utensilio.isMarcado()) itensMarcados++;
        }

        int porcentagem = (int) (((float) itensMarcados / totalItens) * 100);

        progressBarUtensilios.setProgress(porcentagem);
        tvProgressoTexto.setText(itensMarcados + " de " + totalItens + " itens marcados");
        tvProgressoPorcentagem.setText(porcentagem + "%");
    }

    private void limparChecklist() {
        if (listaUtensiliosAtual != null && adapter != null) {
            for (UtensilioModelo utensilio : listaUtensiliosAtual) {
                utensilio.setMarcado(false);
            }
            adapter.notifyDataSetChanged();
            atualizarProgresso();
        }
    }

    private void confirmarUtensilios() {
        if (listaUtensiliosAtual.isEmpty()) {
            Toast.makeText(getContext(), "Nenhum utensílio disponível.", Toast.LENGTH_SHORT).show();
            return;
        }

        for (UtensilioModelo utensilio : listaUtensiliosAtual) {
            if (!utensilio.isMarcado()) {
                Toast.makeText(getContext(), "Marque todos os utensílios antes de confirmar!", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Toast.makeText(getContext(), "Checklist de utensílios confirmado com sucesso!", Toast.LENGTH_SHORT).show();
    }
}