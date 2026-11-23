package com.example.mobilesigec.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.mobilesigec.CacheManager;
import com.example.mobilesigec.R;
import com.example.mobilesigec.database.ConexaoMySQL;
import com.example.mobilesigec.model.TurmaModelo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class HomePageFragment extends Fragment {

    private static final String TAG = "HomePageFragment";

    private TextView textSaudacao, textDataHome;
    private Spinner spinnerTurmasHome;
    private SwipeRefreshLayout swipeRefreshHome;

    private ExecutorService executorService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicInteger idRequisicaoHome = new AtomicInteger(0);
    private int ultimaTurmaRenderizada = Integer.MIN_VALUE;

    private TextView textNenhumaTurma;


    public HomePageFragment() {
        // Required empty public constructor
    }

    private void garantirExecutorAtivo() {
        if (executorService == null || executorService.isShutdown() || executorService.isTerminated()) {
            executorService = Executors.newSingleThreadExecutor();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        garantirExecutorAtivo();

        textSaudacao = view.findViewById(R.id.textSaudacao);
        textDataHome = view.findViewById(R.id.textDataHome);
        textNenhumaTurma= view.findViewById(R.id.textNenhumaTurma);
        spinnerTurmasHome = view.findViewById(R.id.spinnerTurmasHome);
        swipeRefreshHome = view.findViewById(R.id.swipe_refresh_home);
        CardView cardInsumos = view.findViewById(R.id.card_checklist_insumos);
        CardView cardUtensilios = view.findViewById(R.id.card_checklist_utensilios);
        View cardReceitasHoje = view.findViewById(R.id.card_receitas_hoje);

        if (swipeRefreshHome != null) {
            swipeRefreshHome.setColorSchemeColors(
                    Color.parseColor("#002747"),
                    Color.parseColor("#F7941D")
            );
            swipeRefreshHome.setOnRefreshListener(() -> carregarTurmasNoSpinner(view, true));
        }

        Context context = getContext();
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences("SessaoApp", Context.MODE_PRIVATE);
            String nomeCompleto = prefs.getString("nome_usuario", "Instrutor");
            String nomeFormatado = formatarNomeCurto(nomeCompleto);

            if (textSaudacao != null) {
                textSaudacao.setText("Bom dia, " + nomeFormatado + "!");
            }

            // Renderização imediata antes mesmo do primeiro frame se o cache já estiver pronto
            int idUsuarioLogado = prefs.getInt("id_usuario", -1);
            int idTurmaSalva = prefs.getInt("id_turma_selecionada", -1);
            String jsonTurmasCache = CacheManager.obterJson(
                    context,
                    CacheManager.CHAVE_HOME_TURMAS + "_" + idUsuarioLogado,
                    CacheManager.TEMPO_EXPIRACAO_PADRAO
            );
            if (jsonTurmasCache != null) {
                List<TurmaModelo> turmasCache = desserializarTurmas(jsonTurmasCache);
                if (!turmasCache.isEmpty()) {
                    TurmaModelo turmaAlvo = turmasCache.get(0);
                    for (TurmaModelo t : turmasCache) {
                        if (t.getIdTurma() == idTurmaSalva) {
                            turmaAlvo = t;
                            break;
                        }
                    }
                    String jsonSemanaCache = CacheManager.obterJson(
                            context,
                            CacheManager.CHAVE_HOME_RECEITAS_SEMANA + "_" + turmaAlvo.getIdTurma(),
                            CacheManager.TEMPO_EXPIRACAO_PADRAO
                    );
                    if (jsonSemanaCache != null) {
                        Map<Integer, List<ItemReceitaData>> semanaCache = desserializarMapaDias(jsonSemanaCache);
                        ultimaTurmaRenderizada = turmaAlvo.getIdTurma();
                        renderizarCardsSemana(view, semanaCache, turmaAlvo.getNomeLaboratorio());
                    }
                }
            }
        }

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd 'DE' MMMM", Locale.forLanguageTag("pt-BR"));
        String dataFormatada = sdf.format(new Date()).toUpperCase();
        if (textDataHome != null) {
            textDataHome.setText(dataFormatada);
        }

        carregarTurmasNoSpinner(view, false);

        if (cardInsumos != null) {
            cardInsumos.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_home_to_insumos));
        }
        if (cardUtensilios != null) {
            cardUtensilios.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_home_to_utensilios));
        }
        if (cardReceitasHoje != null) {
            cardReceitasHoje.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.nav_calendario));
        }
    }

    private void carregarTurmasNoSpinner(View view, boolean forcarAtualizacao) {
        if (spinnerTurmasHome == null && view != null) {
            spinnerTurmasHome = view.findViewById(R.id.spinnerTurmasHome);
        }
        Context context = getContext();
        if (spinnerTurmasHome == null || context == null) {
            finalizarRefresh();
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences("SessaoApp", Context.MODE_PRIVATE);
        int idUsuarioLogado = prefs.getInt("id_usuario", -1);
        String chaveCacheTurmas = CacheManager.CHAVE_HOME_TURMAS + "_" + idUsuarioLogado;

        // 1. Tenta carregar as turmas direto do cache local (se não for atualização forçada)
        if (!forcarAtualizacao) {
            String jsonTurmasCache = CacheManager.obterJson(context, chaveCacheTurmas, CacheManager.TEMPO_EXPIRACAO_PADRAO);
            if (jsonTurmasCache != null) {
                List<TurmaModelo> turmasEmCache = desserializarTurmas(jsonTurmasCache);
                if (!turmasEmCache.isEmpty()) {
                    configurarAdapterTurmas(turmasEmCache, prefs, false);
                    return;
                }
            }
        }

        garantirExecutorAtivo();

        // 2. Busca no MySQL
        executorService.execute(() -> {
            List<TurmaModelo> listaTurmas = new ArrayList<>();
            boolean conexaoOk = false;

            try (Connection con = ConexaoMySQL.conectar()) {
                if (con != null) {
                    conexaoOk = true;
                    String sql;
                    if (idUsuarioLogado != -1) {
                        sql = "SELECT t.id_turma, t.nome_turma, l.nome_laboratorio " +
                                "FROM turma t " +
                                "INNER JOIN usuario_turma ut ON t.id_turma = ut.id_turma " +
                                "INNER JOIN laboratorio l ON t.id_laboratorio = l.id_laboratorio " +
                                "WHERE ut.id_usuario = ? AND t.situacao = 'A'";
                    } else {
                        sql = "SELECT t.id_turma, t.nome_turma, l.nome_laboratorio " +
                                "FROM turma t " +
                                "INNER JOIN laboratorio l ON t.id_laboratorio = l.id_laboratorio " +
                                "WHERE t.situacao = 'A'";
                    }

                    try (PreparedStatement stmt = con.prepareStatement(sql)) {
                        if (idUsuarioLogado != -1) {
                            stmt.setInt(1, idUsuarioLogado);
                        }
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                int idTurma = rs.getInt("id_turma");
                                String nomeTurma = rs.getString("nome_turma");
                                String nomeLab = rs.getString("nome_laboratorio");
                                listaTurmas.add(new TurmaModelo(idTurma, nomeTurma, nomeLab));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao carregar turmas", e);
            }

            // Salva no cache se a consulta ao banco funcionou
            if (conexaoOk && !listaTurmas.isEmpty() && getContext() != null) {
                CacheManager.salvarJson(getContext(), chaveCacheTurmas, serializarTurmas(listaTurmas));
            }

            mainHandler.post(() -> {
                if (!isAdded() || getContext() == null) {
                    finalizarRefresh();
                    return;
                }
                configurarAdapterTurmas(listaTurmas, prefs, forcarAtualizacao);
            });
        });
    }

    private void configurarAdapterTurmas(List<TurmaModelo> listaTurmas, SharedPreferences prefs, boolean forcarAtualizacao) {
        if (!isAdded() || getContext() == null || spinnerTurmasHome == null) {
            finalizarRefresh();
            return;
        }

        ArrayAdapter<TurmaModelo> adapter = new ArrayAdapter<TurmaModelo>(
                requireContext(),
                R.layout.spinner_item_preto,
                listaTurmas
        ) {
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View dropView = super.getDropDownView(position, convertView, parent);
                TextView texto = (TextView) dropView;
                texto.setTextColor(Color.BLACK);
                texto.setBackgroundColor(Color.WHITE);
                return dropView;
            }
        };

        adapter.setDropDownViewResource(R.layout.spinner_dropdown_preto);
        spinnerTurmasHome.setAdapter(adapter);

                if (listaTurmas.isEmpty()){
                    prefs.edit().remove("id_turma_selecionada").apply();
                    if (textNenhumaTurma!=null){
                        textNenhumaTurma.setText("Nenhuma turma selecionada para hoje.");
                        textNenhumaTurma.setVisibility(View.VISIBLE);
                    }
                    spinnerTurmasHome.setEnabled(false);
                    return;
                }
        if (listaTurmas.isEmpty()) {
            prefs.edit().remove("id_turma_selecionada").apply();
            View v = getView();
            if (v != null) {
                LinearLayout containerReceitas = v.findViewById(R.id.container_receitas);
                if (containerReceitas != null) containerReceitas.removeAllViews();
            }
            finalizarRefresh();
            return;
        }

                if (textNenhumaTurma!=null) {
                    textNenhumaTurma.setVisibility(View.GONE);
                }
                spinnerTurmasHome.setEnabled(true);

                int idTurmaSalva = prefs.getInt("id_turma_selecionada", -1);
                int posicaoInicial = 0;
                for (int i = 0; i < listaTurmas.size(); i++) {
                    if (listaTurmas.get(i).getIdTurma() == idTurmaSalva) {
                        posicaoInicial = i;
                        break;
                    }
                }

        final int posInicialFinal = posicaoInicial;
        final boolean[] primeiraChamada = {true};

        spinnerTurmasHome.setSelection(posicaoInicial, false);
        spinnerTurmasHome.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View selectedView, int position, long id) {
                if (primeiraChamada[0]) {
                    primeiraChamada[0] = false;
                    if (position == posInicialFinal) return;
                }
                if (position < 0 || position >= listaTurmas.size()) return;
                TurmaModelo turmaSelecionada = (TurmaModelo) parent.getItemAtPosition(position);
                if (turmaSelecionada != null) {
                    int idTurma = turmaSelecionada.getIdTurma();
                    prefs.edit().putInt("id_turma_selecionada", idTurma).apply();
                    if (idTurma != ultimaTurmaRenderizada) {
                        carregarReceitasDaTurma(idTurma, turmaSelecionada.getNomeLaboratorio(), false);
                    }
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        TurmaModelo turmaInicial = listaTurmas.get(posicaoInicial);
        int idInicial = turmaInicial.getIdTurma();
        if (idTurmaSalva != idInicial) {
            prefs.edit().putInt("id_turma_selecionada", idInicial).apply();
        }
        carregarReceitasDaTurma(idInicial, turmaInicial.getNomeLaboratorio(), forcarAtualizacao);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getContext() == null) return;

        if (spinnerTurmasHome == null || spinnerTurmasHome.getAdapter() == null || spinnerTurmasHome.getAdapter().getCount() == 0) {
            carregarTurmasNoSpinner(getView(), false);
            return;
        }

        SharedPreferences prefs = getContext().getSharedPreferences("SessaoApp", Context.MODE_PRIVATE);
        int idSalvo = prefs.getInt("id_turma_selecionada", -1);
        @SuppressWarnings("unchecked")
        ArrayAdapter<TurmaModelo> adapter = (ArrayAdapter<TurmaModelo>) spinnerTurmasHome.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            TurmaModelo t = adapter.getItem(i);
            if (t != null && t.getIdTurma() == idSalvo) {
                if (spinnerTurmasHome.getSelectedItemPosition() != i) {
                    spinnerTurmasHome.setSelection(i, false);
                }
                if (idSalvo != ultimaTurmaRenderizada) {
                    carregarReceitasDaTurma(t.getIdTurma(), t.getNomeLaboratorio(), false);
                }
                break;
            }
        }
    }

    private void carregarReceitasDaTurma(int idTurma, String nomeLaboratorio, boolean forcarAtualizacao) {
        View view = getView();
        Context context = getContext();
        if (view == null || context == null) {
            finalizarRefresh();
            return;
        }

        LinearLayout containerReceitas = view.findViewById(R.id.container_receitas);
        if (containerReceitas == null) {
            finalizarRefresh();
            return;
        }

        String chaveCacheReceitas = CacheManager.CHAVE_HOME_RECEITAS_SEMANA + "_" + idTurma;

        // 1. Tenta recuperar as receitas da semana desta turma no cache (se não for atualização forçada)
        if (!forcarAtualizacao) {
            String jsonReceitasCache = CacheManager.obterJson(context, chaveCacheReceitas, CacheManager.TEMPO_EXPIRACAO_PADRAO);
            if (jsonReceitasCache != null) {
                Map<Integer, List<ItemReceitaData>> mapCache = desserializarMapaDias(jsonReceitasCache);
                ultimaTurmaRenderizada = idTurma;
                renderizarCardsSemana(view, mapCache, nomeLaboratorio);
                finalizarRefresh();
                return;
            }
        }

        containerReceitas.removeAllViews();
        final int reqId = idRequisicaoHome.incrementAndGet();
        garantirExecutorAtivo();

        // 2. Busca no MySQL
        executorService.execute(() -> {
            Map<Integer, List<ItemReceitaData>> mapDias = new HashMap<>();
            for (int dayIndex = Calendar.MONDAY; dayIndex <= Calendar.FRIDAY; dayIndex++) {
                mapDias.put(dayIndex, new ArrayList<>());
            }

            boolean conexaoOk = false;

            try (Connection con = ConexaoMySQL.conectar()) {
                if (con != null) {
                    conexaoOk = true;
                    String sql = "SELECT f.nome_ficha, COALESCE(a.concluido, 'N') AS concluido, a.* " +
                            "FROM ficha f " +
                            "LEFT JOIN agendamento a ON f.id_ficha = a.id_ficha " +
                            "WHERE f.id_turma = ?";

                    try (PreparedStatement stmt = con.prepareStatement(sql)) {
                        stmt.setInt(1, idTurma);
                        try (ResultSet rs = stmt.executeQuery()) {
                            ResultSetMetaData metaData = rs.getMetaData();
                            int columnCount = metaData.getColumnCount();

                            Calendar calNow = Calendar.getInstance();
                            calNow.set(Calendar.HOUR_OF_DAY, 0);
                            calNow.set(Calendar.MINUTE, 0);
                            calNow.set(Calendar.SECOND, 0);
                            calNow.set(Calendar.MILLISECOND, 0);

                            int dow = calNow.get(Calendar.DAY_OF_WEEK);
                            int daysToMonday = (dow == Calendar.SUNDAY) ? -6 : (Calendar.MONDAY - dow);

                            Calendar startOfWeek = (Calendar) calNow.clone();
                            startOfWeek.add(Calendar.DAY_OF_MONTH, daysToMonday);

                            Calendar endOfWeek = (Calendar) startOfWeek.clone();
                            endOfWeek.add(Calendar.DAY_OF_MONTH, 6);
                            endOfWeek.set(Calendar.HOUR_OF_DAY, 23);
                            endOfWeek.set(Calendar.MINUTE, 59);
                            endOfWeek.set(Calendar.SECOND, 59);
                            endOfWeek.set(Calendar.MILLISECOND, 999);

                            long startMillis = startOfWeek.getTimeInMillis();
                            long endMillis = endOfWeek.getTimeInMillis();

                            while (rs.next()) {
                                int idFicha = rs.getInt("id_ficha");
                                String nomeFicha = rs.getString("nome_ficha");
                                String concluido = rs.getString("concluido");

                                int dayOfWeek = -1;
                                boolean temData = false;
                                boolean pertenceASemanaAtual = false;

                                for (int i = 1; i <= columnCount; i++) {
                                    String colName = metaData.getColumnLabel(i).toLowerCase();
                                    int colType = metaData.getColumnType(i);

                                    if (!colName.startsWith("id_") && (colType == java.sql.Types.DATE || colType == java.sql.Types.TIMESTAMP || colName.contains("data") || colName.contains("date"))) {
                                        try {
                                            java.util.Date dateVal = rs.getTimestamp(i);
                                            if (dateVal == null) {
                                                dateVal = rs.getDate(i);
                                            }
                                            if (dateVal != null) {
                                                temData = true;
                                                long t = dateVal.getTime();
                                                if (t >= startMillis && t <= endMillis) {
                                                    pertenceASemanaAtual = true;
                                                    Calendar itemCal = Calendar.getInstance();
                                                    itemCal.setTime(dateVal);
                                                    dayOfWeek = itemCal.get(Calendar.DAY_OF_WEEK);
                                                }
                                                break;
                                            }
                                        } catch (Exception ignored) {}
                                    } else if (colName.equals("dia_semana") || colName.equals("dia")) {
                                        try {
                                            int val = rs.getInt(i);
                                            if (val >= 1 && val <= 7) {
                                                dayOfWeek = val;
                                                temData = true;
                                                pertenceASemanaAtual = true;
                                                break;
                                            }
                                        } catch (Exception ignored) {}
                                    }
                                }

                                if (!temData || !pertenceASemanaAtual) {
                                    continue;
                                }

                                ItemReceitaData item = new ItemReceitaData(idFicha, nomeFicha, concluido);
                                List<ItemReceitaData> targetList = mapDias.get(dayOfWeek);
                                if (targetList != null) {
                                    targetList.add(item);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao carregar receitas da turma", e);
            }

            if (conexaoOk && getContext() != null) {
                CacheManager.salvarJson(getContext(), chaveCacheReceitas, serializarMapaDias(mapDias));
            }

            mainHandler.post(() -> {
                if (!isAdded() || getContext() == null || reqId != idRequisicaoHome.get()) {
                    finalizarRefresh();
                    return;
                }
                ultimaTurmaRenderizada = idTurma;
                renderizarCardsSemana(getView(), mapDias, nomeLaboratorio);
                finalizarRefresh();
            });
        });
    }

    private void finalizarRefresh() {
        if (swipeRefreshHome != null && swipeRefreshHome.isRefreshing()) {
            swipeRefreshHome.setRefreshing(false);
        }
    }

    private void renderizarCardsSemana(View rootView, Map<Integer, List<ItemReceitaData>> mapDias, String nomeLaboratorio) {
        if (!isAdded() || rootView == null || getContext() == null) return;

        LinearLayout currentContainer = rootView.findViewById(R.id.container_receitas);
        if (currentContainer == null) return;

        currentContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        Calendar now = Calendar.getInstance();
        int hojeDiaSemana = now.get(Calendar.DAY_OF_WEEK);

        int[] diasSemana = new int[]{
                Calendar.MONDAY,
                Calendar.TUESDAY,
                Calendar.WEDNESDAY,
                Calendar.THURSDAY,
                Calendar.FRIDAY
        };

        String[] nomesDias = new String[]{
                "Segunda-feira",
                "Terça-feira",
                "Quarta-feira",
                "Quinta-feira",
                "Sexta-feira"
        };

        for (int i = 0; i < diasSemana.length; i++) {
            int diaNum = diasSemana[i];
            String nomeDiaBase = nomesDias[i];
            boolean ehHoje = (diaNum == hojeDiaSemana);

            String tituloDia = ehHoje ? nomeDiaBase + " (Hoje)" : nomeDiaBase;

            View cardDia = inflater.inflate(R.layout.item_dia_semana, currentContainer, false);

            TextView tvNomeDia = cardDia.findViewById(R.id.tv_nome_dia);
            ImageView ivChevron = cardDia.findViewById(R.id.iv_chevron);
            LinearLayout containerReceitasDia = cardDia.findViewById(R.id.container_receitas_dia);
            TextView tvSemReceitas = cardDia.findViewById(R.id.tv_sem_receitas);
            View headerDiaSemana = cardDia.findViewById(R.id.header_dia_semana);

            if (tvNomeDia != null) {
                tvNomeDia.setText(tituloDia);
            }

            List<ItemReceitaData> receitasDoDia = mapDias.get(diaNum);

            if (receitasDoDia == null || receitasDoDia.isEmpty()) {
                if (tvSemReceitas != null) {
                    tvSemReceitas.setVisibility(View.VISIBLE);
                }
            } else {
                if (tvSemReceitas != null) {
                    tvSemReceitas.setVisibility(View.GONE);
                }
                for (ItemReceitaData itemData : receitasDoDia) {
                    View itemReceitaView = inflater.inflate(R.layout.item_receita_semana, containerReceitasDia, false);
                    TextView tvNome = itemReceitaView.findViewById(R.id.tv_nome_receita);
                    TextView tvDetalhes = itemReceitaView.findViewById(R.id.tv_detalhes_receita);

                    if (tvNome != null) {
                        tvNome.setText(itemData.nomeFicha);
                    }
                    if (tvDetalhes != null) {
                        tvDetalhes.setText(nomeLaboratorio);
                    }
                    itemReceitaView.setOnClickListener(v -> {
                        Bundle bundle = new Bundle();
                        bundle.putInt("id_ficha", itemData.idFicha);
                        Navigation.findNavController(v).navigate(R.id.nav_detalhes_receita, bundle);
                    });
                    containerReceitasDia.addView(itemReceitaView);
                }
            }

            if (ehHoje) {
                if (containerReceitasDia != null) containerReceitasDia.setVisibility(View.VISIBLE);
                if (ivChevron != null) ivChevron.setRotation(180f);
            } else {
                if (containerReceitasDia != null) containerReceitasDia.setVisibility(View.GONE);
                if (ivChevron != null) ivChevron.setRotation(0f);
            }

            if (headerDiaSemana != null && containerReceitasDia != null && ivChevron != null) {
                headerDiaSemana.setOnClickListener(v -> {
                    boolean estaVisivel = (containerReceitasDia.getVisibility() == View.VISIBLE);
                    if (estaVisivel) {
                        containerReceitasDia.setVisibility(View.GONE);
                        ivChevron.animate().rotation(0f).setDuration(200).start();
                    } else {
                        containerReceitasDia.setVisibility(View.VISIBLE);
                        ivChevron.animate().rotation(180f).setDuration(200).start();
                    }
                });
            }

            currentContainer.addView(cardDia);
        }
    }

    // --- MÉTODOS DE CONVERSÃO JSON PARA O CACHE ---

    private String serializarTurmas(List<TurmaModelo> lista) {
        JSONArray array = new JSONArray();
        for (TurmaModelo t : lista) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id_turma", t.getIdTurma());
                obj.put("nome_turma", t.getNomeTurma());
                obj.put("nome_laboratorio", t.getNomeLaboratorio());
                array.put(obj);
            } catch (Exception e) {
                Log.e(TAG, "Erro ao serializar turma", e);
            }
        }
        return array.toString();
    }

    private List<TurmaModelo> desserializarTurmas(String json) {
        List<TurmaModelo> lista = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                lista.add(new TurmaModelo(
                        obj.getInt("id_turma"),
                        obj.getString("nome_turma"),
                        obj.getString("nome_laboratorio")
                ));
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao desserializar turmas", e);
        }
        return lista;
    }

    private String serializarMapaDias(Map<Integer, List<ItemReceitaData>> mapDias) {
        JSONObject root = new JSONObject();
        try {
            for (Map.Entry<Integer, List<ItemReceitaData>> entry : mapDias.entrySet()) {
                JSONArray diaArray = new JSONArray();
                for (ItemReceitaData item : entry.getValue()) {
                    JSONObject obj = new JSONObject();
                    obj.put("id_ficha", item.idFicha);
                    obj.put("nome_ficha", item.nomeFicha);
                    obj.put("concluido", item.concluido);
                    diaArray.put(obj);
                }
                root.put(String.valueOf(entry.getKey()), diaArray);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao serializar receitas da semana", e);
        }
        return root.toString();
    }

    private Map<Integer, List<ItemReceitaData>> desserializarMapaDias(String json) {
        Map<Integer, List<ItemReceitaData>> mapDias = new HashMap<>();
        for (int dayIndex = Calendar.MONDAY; dayIndex <= Calendar.FRIDAY; dayIndex++) {
            mapDias.put(dayIndex, new ArrayList<>());
        }
        try {
            JSONObject root = new JSONObject(json);
            for (int dayIndex = Calendar.MONDAY; dayIndex <= Calendar.FRIDAY; dayIndex++) {
                String key = String.valueOf(dayIndex);
                if (root.has(key)) {
                    JSONArray diaArray = root.getJSONArray(key);
                    List<ItemReceitaData> lista = mapDias.get(dayIndex);
                    for (int i = 0; i < diaArray.length(); i++) {
                        JSONObject obj = diaArray.getJSONObject(i);
                        lista.add(new ItemReceitaData(
                                obj.getInt("id_ficha"),
                                obj.getString("nome_ficha"),
                                obj.optString("concluido", "N")
                        ));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao desserializar receitas da semana", e);
        }
        return mapDias;
    }

    private static class ItemReceitaData {
        final int idFicha;
        final String nomeFicha;
        final String concluido;

        ItemReceitaData(int idFicha, String nomeFicha, String concluido) {
            this.idFicha = idFicha;
            this.nomeFicha = nomeFicha;
            this.concluido = concluido;
        }
    }

    private String formatarNomeCurto(String nomeCompleto) {
        if (nomeCompleto == null || nomeCompleto.trim().isEmpty()) {
            return "";
        }
        String[] partes = nomeCompleto.trim().split("\\s+");
        if (partes.length >= 2) {
            return partes[0] + " " + partes[1];
        } else {
            return partes[0];
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        idRequisicaoHome.incrementAndGet();
        ultimaTurmaRenderizada = Integer.MIN_VALUE;
        swipeRefreshHome = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }
}