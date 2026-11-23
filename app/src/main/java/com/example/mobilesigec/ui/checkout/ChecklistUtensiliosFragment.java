package com.example.mobilesigec.ui.checkout;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.mobilesigec.CacheManager;
import com.example.mobilesigec.R;
import com.example.mobilesigec.adapter.UtensilioAdapter;
import com.example.mobilesigec.database.ConexaoMySQL;
import com.example.mobilesigec.model.ReceitaModelo;
import com.example.mobilesigec.model.UtensilioModelo;
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

public class ChecklistUtensiliosFragment extends Fragment {

    private SwipeRefreshLayout swipeRefreshUtensilios;
    private Spinner spinnerReceitas;
    private TextView tvReceitaSelecionada;
    private TextView tvLaboratorioSelecionado;
    private TextView tvTurmaSelecionada;

    private RecyclerView rvUtensilios;
    private UtensilioAdapter adapter;
    private final List<UtensilioModelo> listaUtensiliosAtual = new ArrayList<>();
    private LinearProgressIndicator progressBarUtensilios;
    private TextView tvProgressoTexto;
    private TextView tvProgressoPorcentagem;
    private ProgressBar progressLoading;

    private MaterialButton btnLimparUtensilios;
    private MaterialButton btnConfirmarUtensilios;
    private MaterialButton btnDevolverUtensilios;

    private int idFichaAtual = -1;
    private boolean carregandoDados = false;

    public ChecklistUtensiliosFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_checklist_utensilios, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeRefreshUtensilios = view.findViewById(R.id.swipe_refresh_utensilios);
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

        btnDevolverUtensilios = view.findViewById(R.id.btn_devolver_utensilios);
        btnDevolverUtensilios.setOnClickListener(v -> abrirDialogDevolucao());

        btnLimparUtensilios.setOnClickListener(v -> limparChecklist());
        btnConfirmarUtensilios.setOnClickListener(v -> confirmarUtensilios());

        if (swipeRefreshUtensilios != null) {
            swipeRefreshUtensilios.setColorSchemeColors(
                    Color.parseColor("#002747"),
                    Color.parseColor("#F7941D")
            );
            swipeRefreshUtensilios.setOnRefreshListener(() -> carregarReceitasNoSpinner(true));
        }

        carregarReceitasNoSpinner(false);
    }

    private void encerrarAnimacaoRefresh() {
        if (swipeRefreshUtensilios != null && swipeRefreshUtensilios.isRefreshing()) {
            swipeRefreshUtensilios.setRefreshing(false);
        }
    }
    private void mostrarNenhumaReceitaDisponivel() {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"Nenhuma receita disponível"}
        );

        spinnerAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerReceitas.setAdapter(spinnerAdapter);
        spinnerReceitas.setEnabled(false);

        tvReceitaSelecionada.setText("Nenhuma receita disponível");
        tvLaboratorioSelecionado.setText("");
        tvTurmaSelecionada.setText("");
    }


    private void carregarReceitasNoSpinner(boolean forcarAtualizacao) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("SessaoApp", Context.MODE_PRIVATE);
        int idUsuarioLogado = prefs.getInt("id_usuario", -1);
        if (idUsuarioLogado == -1) {
            idUsuarioLogado = prefs.getInt("ID_USUARIO", -1);
        }
        int idTurmaSelecionada = prefs.getInt("id_turma_selecionada", -1);

        if (idTurmaSelecionada == -1) {
            encerrarAnimacaoRefresh();
            return;
        }

        String dataHoje = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String chaveCacheReceitasDia = CacheManager.CHAVE_CHECKLIST_INSUMOS + "_receitas_dia_"
                + idUsuarioLogado + "_" + idTurmaSelecionada + "_" + dataHoje;
        if (idTurmaSelecionada == -1) {
            mostrarNenhumaReceitaDisponivel();
            return;
        }

        if (forcarAtualizacao) {
            CacheManager.invalidarChave(requireContext(), chaveCacheReceitasDia);
            if (idFichaAtual != -1) {
                CacheManager.invalidarChave(requireContext(), CacheManager.CHAVE_CHECKLIST_UTENSILIOS + "_itens_" + idFichaAtual);
            }
        } else {
            String jsonReceitasCache = CacheManager.obterJson(requireContext(), chaveCacheReceitasDia, CacheManager.TEMPO_EXPIRACAO_PADRAO);
            if (jsonReceitasCache != null) {
                List<ReceitaModelo> receitasCache = desserializarReceitasSpinner(jsonReceitasCache);
                if (!receitasCache.isEmpty()) {
                    configurarSpinnerReceitas(receitasCache, false);
                    return;
                }
            }
        }

        if (!forcarAtualizacao) {
            progressLoading.setVisibility(View.VISIBLE);
        }
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
                    if (getContext() == null) return;

                    if (listaReceitas.isEmpty()) {
                        mostrarNenhumaReceitaDisponivel();
                        return;
                    }

                    if (!listaReceitas.isEmpty() && getContext() != null) {
                        configurarSpinnerReceitas(listaReceitas, forcarAtualizacao);
                    } else {
                        encerrarAnimacaoRefresh();
                    }
                });
            }
        });
    }

    private void configurarSpinnerReceitas(List<ReceitaModelo> listaReceitas, boolean forcarAtualizacaoItens) {
        if (!isAdded() || getContext() == null) {
            encerrarAnimacaoRefresh();
            return;
        }

        int posicaoMantida = 0;
        if (idFichaAtual != -1) {
            for (int i = 0; i < listaReceitas.size(); i++) {
                if (listaReceitas.get(i).getIdReceita() == idFichaAtual) {
                    posicaoMantida = i;
                    break;
                }
            }
        }

        ArrayAdapter<ReceitaModelo> spinnerAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                listaReceitas
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerReceitas.setAdapter(spinnerAdapter);

        final int posInicial = posicaoMantida;
        spinnerReceitas.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ReceitaModelo receitaSelecionada = (ReceitaModelo) parent.getItemAtPosition(position);
                tvReceitaSelecionada.setText("Receita: " + receitaSelecionada.getNomeReceita());
                tvLaboratorioSelecionado.setText(receitaSelecionada.getNomeLaboratorio());
                tvTurmaSelecionada.setText(receitaSelecionada.getNomeTurma() + " • " + receitaSelecionada.getNomeLaboratorio());

                carregarUtensiliosDaReceita(receitaSelecionada.getIdReceita(), forcarAtualizacaoItens);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerReceitas.setSelection(posInicial, false);
        ReceitaModelo inicial = listaReceitas.get(posInicial);
        tvReceitaSelecionada.setText("Receita: " + inicial.getNomeReceita());
        tvLaboratorioSelecionado.setText(inicial.getNomeLaboratorio());
        tvTurmaSelecionada.setText(inicial.getNomeTurma() + " • " + inicial.getNomeLaboratorio());
        carregarUtensiliosDaReceita(inicial.getIdReceita(), forcarAtualizacaoItens);
    }

    private void carregarUtensiliosDaReceita(int idFicha, boolean forcarAtualizacao) {
        if (getContext() == null) {
            encerrarAnimacaoRefresh();
            return;
        }

        this.idFichaAtual = idFicha;
        this.carregandoDados = true;

        listaUtensiliosAtual.clear();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        final String chaveCacheItens = CacheManager.CHAVE_CHECKLIST_UTENSILIOS + "_itens_" + idFicha;

        if (!forcarAtualizacao) {
            String jsonItensCache = CacheManager.obterJson(requireContext(), chaveCacheItens, CacheManager.TEMPO_EXPIRACAO_PADRAO);
            if (jsonItensCache != null) {
                List<UtensilioModelo> utensiliosCache = desserializarUtensilios(jsonItensCache);
                listaUtensiliosAtual.addAll(utensiliosCache);
                this.carregandoDados = false;
                exibirListaUtensilios();
                encerrarAnimacaoRefresh();
                return;
            }
        }

        if (!forcarAtualizacao) {
            progressLoading.setVisibility(View.VISIBLE);
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<UtensilioModelo> carregados = new ArrayList<>();
            boolean conexaoOk = false;

            try (Connection con = ConexaoMySQL.conectar()) {
                if (con != null) {
                    conexaoOk = true;
                    String sql = "SELECT u.id_utensilio, u.nome_utensilio, COALESCE(cu.quantidade, u.quantidade, 1) AS quantidade " +
                            "FROM checklist_utensilio cu " +
                            "INNER JOIN utensilio u ON cu.id_utensilio = u.id_utensilio " +
                            "WHERE cu.id_ficha = ? AND (cu.situacao = 'A' OR cu.situacao IS NULL OR cu.situacao = '')";

                    try (PreparedStatement stmt = con.prepareStatement(sql)) {
                        stmt.setInt(1, idFicha);
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                int idUtensilio = rs.getInt("id_utensilio");
                                String nome = rs.getString("nome_utensilio");
                                String qtd = String.valueOf(rs.getInt("quantidade"));

                                carregados.add(new UtensilioModelo(idUtensilio, nome, qtd));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            final boolean finalConexaoOk = conexaoOk;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    progressLoading.setVisibility(View.GONE);
                    listaUtensiliosAtual.clear();
                    listaUtensiliosAtual.addAll(carregados);
                    this.carregandoDados = false;

                    if (finalConexaoOk && getContext() != null) {
                        salvarEstadoChecklistNoCache();
                    }

                    exibirListaUtensilios();
                    encerrarAnimacaoRefresh();
                });
            }
        });
    }

    private void exibirListaUtensilios() {
        if (!isAdded()) return;
        adapter = new UtensilioAdapter(listaUtensiliosAtual, this::atualizarProgresso);
        rvUtensilios.setAdapter(adapter);
        atualizarProgresso();
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

        // Auto-save imediato no cache
        salvarEstadoChecklistNoCache();
    }

    private void salvarEstadoChecklistNoCache() {
        if (carregandoDados || idFichaAtual == -1 || getContext() == null) return;
        String chaveCacheItens = CacheManager.CHAVE_CHECKLIST_UTENSILIOS + "_itens_" + idFichaAtual;
        CacheManager.salvarJson(getContext(), chaveCacheItens, serializarUtensilios(listaUtensiliosAtual));
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
        Object itemSelecionado = spinnerReceitas.getSelectedItem();

        if (!(itemSelecionado instanceof ReceitaModelo)) {
            Toast.makeText(getContext(), "Nenhuma receita disponível.", Toast.LENGTH_SHORT).show();
            return;
        }

        ReceitaModelo receitaSelecionada = (ReceitaModelo) itemSelecionado;
        if (receitaSelecionada == null || listaUtensiliosAtual.isEmpty()) {
            Toast.makeText(getContext(), "Nenhuma receita disponível.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<UtensilioModelo> utensiliosUtilizados = new ArrayList<>();
        for (UtensilioModelo utensilio : listaUtensiliosAtual) {
            if (utensilio.isMarcado()) {
                utensiliosUtilizados.add(utensilio);
            }
        }

        if (utensiliosUtilizados.isEmpty()) {
            Toast.makeText(getContext(), "Marque pelo menos um utensílio utilizado antes de confirmar!", Toast.LENGTH_LONG).show();
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

                    String sqlUtensilio = "UPDATE checklist_utensilio SET data_hora_saida = NOW(), observacao = 'Retirado para aula' " +
                            "WHERE id_ficha = ? AND id_utensilio = ?";

                    PreparedStatement stmtUtensilio = con.prepareStatement(sqlUtensilio);

                    for (UtensilioModelo utensilio : utensiliosUtilizados) {
                        stmtUtensilio.setInt(1, receitaSelecionada.getIdReceita());
                        stmtUtensilio.setInt(2, utensilio.getIdUtensilio());
                        stmtUtensilio.executeUpdate();
                    }
                    stmtUtensilio.close();

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
                                    CacheManager.CHAVE_CHECKLIST_UTENSILIOS + "_itens_" + receitaSelecionada.getIdReceita()
                            );
                        }
                        Toast.makeText(getContext(), "Retirada de utensílios registrada com sucesso!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "Erro ao processar a retirada dos utensílios.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    private void abrirDialogDevolucao() {
        Object itemSelecionado = spinnerReceitas.getSelectedItem();

        if (!(itemSelecionado instanceof ReceitaModelo)) {
            Toast.makeText(getContext(), "Nenhuma receita disponível.", Toast.LENGTH_SHORT).show();
            return;
        }

        ReceitaModelo receitaSelecionada = (ReceitaModelo) itemSelecionado;
        if (receitaSelecionada == null || listaUtensiliosAtual.isEmpty()) {
            Toast.makeText(getContext(), "Nenhuma receita disponível.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<UtensilioModelo> utensiliosDevolvidos = new ArrayList<>();
        for (UtensilioModelo utensilio : listaUtensiliosAtual) {
            if (utensilio.isMarcado()) {
                utensiliosDevolvidos.add(utensilio);
            }
        }

        if (utensiliosDevolvidos.isEmpty()) {
            Toast.makeText(getContext(), "Marque os itens que deseja devolver.", Toast.LENGTH_LONG).show();
            return;
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_devolver_utensilio, null);
        builder.setView(view);
        android.app.AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Spinner spinnerEstado = view.findViewById(R.id.spinner_estado_utensilio);
        String[] estados = {"PRONTO", "DANIFICADO", "EM_MANUTENCAO"};
        ArrayAdapter<String> adapterEstado = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, estados);
        adapterEstado.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEstado.setAdapter(adapterEstado);

        android.widget.EditText etObservacao = view.findViewById(R.id.et_observacao_devolucao);

        view.findViewById(R.id.btn_cancelar_devolucao).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btn_confirmar_devolucao).setOnClickListener(v -> {
            String estadoAtual = spinnerEstado.getSelectedItem().toString();
            String observacao = etObservacao.getText().toString().trim();
            processarDevolucao(utensiliosDevolvidos, estadoAtual, observacao, receitaSelecionada.getIdReceita(), dialog);
        });

        dialog.show();
    }

    private void processarDevolucao(List<UtensilioModelo> utensilios, String estadoAtual, String observacao, int idFicha, android.app.AlertDialog dialog) {
        progressLoading.setVisibility(View.VISIBLE);
        dialog.dismiss();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            boolean sucesso = false;
            try {
                Connection con = ConexaoMySQL.conectar();
                if (con != null) {
                    con.setAutoCommit(false);

                    String sqlDevolucao = "UPDATE checklist_utensilio SET data_hora_entrada = NOW(), estado_atual = ?, observacao = ? " +
                            "WHERE id_ficha = ? AND id_utensilio = ?";
                    PreparedStatement stmtDevolucao = con.prepareStatement(sqlDevolucao);

                    for (UtensilioModelo utensilio : utensilios) {
                        stmtDevolucao.setString(1, estadoAtual);
                        stmtDevolucao.setString(2, observacao);
                        stmtDevolucao.setInt(3, idFicha);
                        stmtDevolucao.setInt(4, utensilio.getIdUtensilio());
                        stmtDevolucao.executeUpdate();
                    }
                    stmtDevolucao.close();

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
                                    CacheManager.CHAVE_CHECKLIST_UTENSILIOS + "_itens_" + idFicha
                            );
                        }
                        Toast.makeText(getContext(), "Devolução registrada com sucesso!", Toast.LENGTH_LONG).show();
                        limparChecklist();
                    } else {
                        Toast.makeText(getContext(), "Erro ao processar devolução no banco.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

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

    private String serializarUtensilios(List<UtensilioModelo> lista) {
        JSONArray array = new JSONArray();
        for (UtensilioModelo item : lista) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id_utensilio", item.getIdUtensilio());
                obj.put("nome_utensilio", item.getNomeUtensilio() != null ? item.getNomeUtensilio() : "");
                obj.put("quantidade", item.getQuantidade() != null ? item.getQuantidade() : "1");
                obj.put("marcado", item.isMarcado());
                array.put(obj);
            } catch (Exception ignored) {}
        }
        return array.toString();
    }

    private List<UtensilioModelo> desserializarUtensilios(String json) {
        List<UtensilioModelo> lista = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                UtensilioModelo utensilio = new UtensilioModelo(
                        obj.getInt("id_utensilio"),
                        obj.getString("nome_utensilio"),
                        obj.getString("quantidade")
                );
                utensilio.setMarcado(obj.optBoolean("marcado", false));
                lista.add(utensilio);
            }
        } catch (Exception ignored) {}
        return lista;
    }
}