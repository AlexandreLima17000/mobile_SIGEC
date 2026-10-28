package com.example.mobilesigec.calendario;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mobilesigec.ConexaoMySQL;
import com.example.mobilesigec.R;
import com.example.mobilesigec.model.AulaAgenda;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Filter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.example.mobilesigec.model.TurmaModelo;

public class CalendarioFragment extends Fragment {

    private static final String TAG = "CalendarioFragment";

    private Calendar currentCalendar;
    private int selectedDay = 1;

    private GridLayout calendarGrid;
    private TextView textMonthYear;
    private TextView textCurrentDate;
    private TextView textInstrutorHeader;
    private LinearLayout layoutFichasLista;
    private TextView textNoFichas;
    private ImageView btnPrevMonth;
    private ImageView btnNextMonth;
    private Spinner spinnerTurmasCalendario;
    private AutoCompleteTextView etPesquisa;

    private String termoFiltro = "";
    private LayoutInflater currentInflater;

    private final Map<Integer, List<AulaAgenda>> eventosDoMes = new HashMap<>();
    private ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    private int ultimaTurmaCarregada = Integer.MIN_VALUE;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private View lastSelectedDayView = null;
    private TextView lastSelectedTextView = null;

    public CalendarioFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentCalendar = Calendar.getInstance();
        selectedDay = currentCalendar.get(Calendar.DAY_OF_MONTH);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendario, container, false);
        this.currentInflater = inflater;

        calendarGrid = view.findViewById(R.id.calendar_grid);
        textMonthYear = view.findViewById(R.id.text_month_year);
        textCurrentDate = view.findViewById(R.id.text_current_date);
        textInstrutorHeader = view.findViewById(R.id.text_instrutor_header);
        layoutFichasLista = view.findViewById(R.id.layout_fichas_lista);
        textNoFichas = view.findViewById(R.id.text_no_fichas);
        btnPrevMonth = view.findViewById(R.id.btn_prev_month);
        btnNextMonth = view.findViewById(R.id.btn_next_month);
        spinnerTurmasCalendario = view.findViewById(R.id.spinnerTurmasCalendario);
        etPesquisa = view.findViewById(R.id.et_pesquisa_calendario);

        if (etPesquisa != null) {
            etPesquisa.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    termoFiltro = (s != null) ? s.toString().trim().toLowerCase() : "";
                    aplicarFiltroEAtualizarUI();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            etPesquisa.setOnItemClickListener((parent, view1, position, id) -> {
                String selecao = (String) parent.getItemAtPosition(position);
                if (selecao != null) {
                    termoFiltro = selecao.trim().toLowerCase();
                    aplicarFiltroEAtualizarUI();
                }
            });
        }

        if (getActivity() != null) {
            SharedPreferences prefs = getActivity().getSharedPreferences("SessaoApp", Context.MODE_PRIVATE);
            String nomeUser = prefs.getString("NOME_USUARIO", "Instrutor Gastronomia");
            if (textInstrutorHeader != null) {
                textInstrutorHeader.setText(nomeUser);
            }
        }

        if (btnPrevMonth != null) {
            btnPrevMonth.setOnClickListener(v -> {
                currentCalendar.add(Calendar.MONTH, -1);
                selectedDay = 1;
                carregarCalendarioEEventos(inflater);
            });
        }

        if (btnNextMonth != null) {
            btnNextMonth.setOnClickListener(v -> {
                currentCalendar.add(Calendar.MONTH, 1);
                selectedDay = 1;
                carregarCalendarioEEventos(inflater);
            });
        }

        carregarTurmasNoSpinner();

        return view;
    }

    private String removerAcentos(String text) {
        if (text == null) return "";
        String normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase();
    }

    private int calcularDistanciaLevenshtein(String s1, String s2) {
        if (s1 == null || s2 == null) return Integer.MAX_VALUE;
        String str1 = removerAcentos(s1);
        String str2 = removerAcentos(s2);

        int[] costs = new int[str2.length() + 1];
        for (int j = 0; j <= str2.length(); j++) costs[j] = j;

        for (int i = 1; i <= str1.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= str2.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        str1.charAt(i - 1) == str2.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[str2.length()];
    }

    private boolean ehSimilar(String textoBusca, String textoAlvo) {
        if (textoBusca == null || textoAlvo == null) return false;
        String busca = removerAcentos(textoBusca.trim());
        String alvo = removerAcentos(textoAlvo.trim());

        if (busca.isEmpty()) return true;
        if (alvo.contains(busca)) return true;

        int limiteErros = (busca.length() <= 2) ? 1 : (busca.length() <= 5 ? 2 : 3);

        String[] palavrasAlvo = alvo.split("\\s+");
        for (String palavra : palavrasAlvo) {
            if (calcularDistanciaLevenshtein(busca, palavra) <= limiteErros) {
                return true;
            }
        }

        return calcularDistanciaLevenshtein(busca, alvo) <= limiteErros;
    }

    private List<AulaAgenda> filtrarAulas(List<AulaAgenda> aulas) {
        if (aulas == null || aulas.isEmpty()) return new ArrayList<>();
        if (termoFiltro.isEmpty()) return aulas;

        List<AulaAgenda> filtradas = new ArrayList<>();
        for (AulaAgenda aula : aulas) {
            boolean matchTitulo = (aula.getTitulo() != null && ehSimilar(termoFiltro, aula.getTitulo()));
            boolean matchDesc = (aula.getDescricao() != null && ehSimilar(termoFiltro, aula.getDescricao()));
            boolean matchTurma = (aula.getTurma() != null && ehSimilar(termoFiltro, aula.getTurma()));

            if (matchTitulo || matchDesc || matchTurma) {
                filtradas.add(aula);
            }
        }
        return filtradas;
    }

    private void aplicarFiltroEAtualizarUI() {
        if (currentInflater == null && getView() != null && getContext() != null) {
            currentInflater = LayoutInflater.from(requireContext());
        }
        if (currentInflater == null) return;

        if (!termoFiltro.isEmpty()) {
            boolean diaAtualTemMatch = !filtrarAulas(eventosDoMes.get(selectedDay)).isEmpty();
            if (!diaAtualTemMatch) {
                for (int day = 1; day <= 31; day++) {
                    List<AulaAgenda> aulasDoDia = eventosDoMes.get(day);
                    if (aulasDoDia != null && !filtrarAulas(aulasDoDia).isEmpty()) {
                        selectedDay = day;
                        break;
                    }
                }
            }
        }

        atualizarDataSelecionada();
        populateCalendar(calendarGrid, currentInflater);
        atualizarDetalhesDiaSelecionado();
    }

    private void carregarTurmasNoSpinner() {

        if (spinnerTurmasCalendario == null || getContext() == null) {
            return;
        }

        Context context = getContext();

        if (context == null) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(
                "SessaoApp",
                Context.MODE_PRIVATE
        );

        int idUsuarioLogado = prefs.getInt("id_usuario", -1);

        garantirExecutorAtivo();

        executorService.execute(() -> {

            List<TurmaModelo> listaTurmas = new ArrayList<>();

            String sql;

            try (Connection con = ConexaoMySQL.conectar()) {

                if (con == null) {
                    Log.e(TAG, "Não foi possível conectar ao MySQL.");
                    return;
                }

                if (idUsuarioLogado != -1) {

                    sql =
                            "SELECT t.id_turma, t.nome_turma, l.nome_laboratorio " +
                                    "FROM turma t " +
                                    "INNER JOIN usuario_turma ut " +
                                    "ON t.id_turma = ut.id_turma " +
                                    "INNER JOIN laboratorio l " +
                                    "ON t.id_laboratorio = l.id_laboratorio " +
                                    "WHERE ut.id_usuario = ? " +
                                    "AND t.situacao = 'A' " +
                                    "ORDER BY t.nome_turma";

                } else {

                    sql =
                            "SELECT t.id_turma, t.nome_turma, l.nome_laboratorio " +
                                    "FROM turma t " +
                                    "INNER JOIN laboratorio l " +
                                    "ON t.id_laboratorio = l.id_laboratorio " +
                                    "WHERE t.situacao = 'A' " +
                                    "ORDER BY t.nome_turma";
                }

                try (PreparedStatement stmt = con.prepareStatement(sql)) {

                    if (idUsuarioLogado != -1) {
                        stmt.setInt(1, idUsuarioLogado);
                    }

                    try (ResultSet rs = stmt.executeQuery()) {

                        while (rs.next()) {

                            listaTurmas.add(
                                    new TurmaModelo(
                                            rs.getInt("id_turma"),
                                            rs.getString("nome_turma"),
                                            rs.getString("nome_laboratorio")
                                    )
                            );
                        }
                    }
                }

            } catch (Exception e) {

                Log.e(TAG, "Erro ao carregar turmas do calendário", e);
                return;
            }

            mainHandler.post(() -> {

                if (!isAdded()
                        || getContext() == null
                        || getView() == null
                        || spinnerTurmasCalendario == null) {
                    return;
                }

                Context contextoAtual = getContext();

                if (contextoAtual == null) {
                    return;
                }

                SharedPreferences preferencias =
                        contextoAtual.getSharedPreferences(
                                "SessaoApp",
                                Context.MODE_PRIVATE
                        );

                ArrayAdapter<TurmaModelo> adapter =
                        new ArrayAdapter<>(
                                contextoAtual,
                                android.R.layout.simple_spinner_item,
                                listaTurmas
                        );

                adapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item
                );

                spinnerTurmasCalendario.setAdapter(adapter);

                if (listaTurmas.isEmpty()) {

                    preferencias.edit()
                            .remove("id_turma_selecionada")
                            .apply();

                    eventosDoMes.clear();

                    populateCalendar(
                            calendarGrid,
                            LayoutInflater.from(contextoAtual)
                    );

                    atualizarDetalhesDiaSelecionado();

                    Toast.makeText(
                            contextoAtual,
                            "Nenhuma turma ativa encontrada.",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                // Recupera a mesma turma salva pela HomePage.
                int idTurmaSalva = preferencias.getInt(
                        "id_turma_selecionada",
                        -1
                );

                int posicaoInicial = 0;

                for (int i = 0; i < listaTurmas.size(); i++) {

                    if (listaTurmas.get(i).getIdTurma() == idTurmaSalva) {
                        posicaoInicial = i;
                        break;
                    }
                }

                spinnerTurmasCalendario.setOnItemSelectedListener(
                        new AdapterView.OnItemSelectedListener() {

                            @Override
                            public void onItemSelected(
                                    AdapterView<?> parent,
                                    View selectedView,
                                    int position,
                                    long id
                            ) {

                                if (position < 0 || position >= listaTurmas.size()) {
                                    return;
                                }

                                TurmaModelo turmaSelecionada =
                                        (TurmaModelo) parent.getItemAtPosition(position);

                                if (turmaSelecionada == null) {
                                    return;
                                }

                                int novoIdTurma = turmaSelecionada.getIdTurma();

                                SharedPreferences prefsAtuais =
                                        requireContext().getSharedPreferences(
                                                "SessaoApp",
                                                Context.MODE_PRIVATE
                                        );

                                int idSalvo = prefsAtuais.getInt(
                                        "id_turma_selecionada",
                                        -1
                                );

                                // Compartilha a seleção com a HomePage.
                                prefsAtuais.edit()
                                        .putInt(
                                                "id_turma_selecionada",
                                                novoIdTurma
                                        )
                                        .apply();

                                // Só consulta novamente se a turma mudou.
                                if (novoIdTurma != ultimaTurmaCarregada) {

                                    carregarCalendarioEEventos(
                                            LayoutInflater.from(requireContext())
                                    );
                                }
                            }

                            @Override
                            public void onNothingSelected(
                                    AdapterView<?> parent
                            ) {
                                // Nenhuma ação necessária.
                            }
                        });

                spinnerTurmasCalendario.setSelection(
                        posicaoInicial,
                        false
                );

                // Garante uma turma válida mesmo na primeira utilização.
                TurmaModelo turmaInicial = listaTurmas.get(posicaoInicial);

                preferencias.edit()
                        .putInt(
                                "id_turma_selecionada",
                                turmaInicial.getIdTurma()
                        )
                        .apply();

                carregarCalendarioEEventos(
                        LayoutInflater.from(contextoAtual)
                );
            });
        });
    }

    private void garantirExecutorAtivo() {
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newSingleThreadExecutor();
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        if (getView() == null || getContext() == null) {
            return;
        }

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("SessaoApp", Context.MODE_PRIVATE);

        int idTurmaAtual = prefs.getInt("id_turma_selecionada", -1);

        // Recarrega o calendário se a turma foi alterada na HomePage.
        if (idTurmaAtual != ultimaTurmaCarregada) {
            carregarCalendarioEEventos(
                    LayoutInflater.from(requireContext())
            );
        }
    }
    private void carregarCalendarioEEventos(LayoutInflater inflater) {
        atualizarTituloMesAno();
        atualizarDataSelecionada();
        buscarEventosEAtualizarGrid(inflater);
    }

    private void atualizarTituloMesAno() {
        if (textMonthYear != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("pt-BR"));
            String monthYearStr = sdf.format(currentCalendar.getTime()).toUpperCase();
            textMonthYear.setText(monthYearStr);
        }
    }

    private void atualizarDataSelecionada() {
        if (textCurrentDate != null) {
            Calendar calSel = (Calendar) currentCalendar.clone();
            calSel.set(Calendar.DAY_OF_MONTH, selectedDay);
            SimpleDateFormat sdf = new SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"));
            textCurrentDate.setText(sdf.format(calSel.getTime()));
        }
    }

    private String getFeriadoNacional(Calendar cal) {
        int dia = cal.get(Calendar.DAY_OF_MONTH);
        int mes = cal.get(Calendar.MONTH); // 0-based: JANUARY = 0 ... DECEMBER = 11

        if (mes == Calendar.JANUARY && dia == 1) return "Confraternização Universal";
        if (mes == Calendar.APRIL && dia == 21) return "Tiradentes";
        if (mes == Calendar.MAY && dia == 1) return "Dia do Trabalho";
        if (mes == Calendar.SEPTEMBER && dia == 7) return "Independência do Brasil";
        if (mes == Calendar.OCTOBER && dia == 12) return "Nossa Senhora Aparecida";
        if (mes == Calendar.NOVEMBER && dia == 2) return "Finados";
        if (mes == Calendar.NOVEMBER && dia == 15) return "Proclamação da República";
        if (mes == Calendar.NOVEMBER && dia == 20) return "Dia da Consciência Negra";
        if (mes == Calendar.DECEMBER && dia == 25) return "Natal";

        return null;
    }


    private void buscarEventosEAtualizarGrid(LayoutInflater inflater) {
        eventosDoMes.clear();

        final int mes = currentCalendar.get(Calendar.MONTH) + 1;
        final int ano = currentCalendar.get(Calendar.YEAR);

        Context context = getContext();

        if (context == null) {
            return;
        }

        // Recupera a turma selecionada na HomePage.
        SharedPreferences prefs = context.getSharedPreferences(
                "SessaoApp",
                Context.MODE_PRIVATE
        );

        final int idTurmaSelecionada = prefs.getInt(
                "id_turma_selecionada",
                -1
        );

        ultimaTurmaCarregada = idTurmaSelecionada;

        // Evita executar tarefas em um executor que já foi encerrado.
        garantirExecutorAtivo();

        executorService.execute(() -> {
            try (Connection con = ConexaoMySQL.conectar()) {

                if (con != null) {

                    String sql =
                            "SELECT " +
                                    "a.id_agendamento, " +
                                    "a.data AS data_agendada, " +
                                    "a.situacao AS situacao_agendamento, " +
                                    "a.concluido, " +
                                    "f.id_ficha, " +
                                    "f.nome_ficha, " +
                                    "f.preparo, " +
                                    "t.nome_turma " +
                                    "FROM agendamento a " +
                                    "INNER JOIN ficha f " +
                                    "ON f.id_ficha = a.id_ficha " +
                                    "LEFT JOIN turma t " +
                                    "ON t.id_turma = f.id_turma " +
                                    "WHERE MONTH(a.data) = ? " +
                                    "AND YEAR(a.data) = ? " +
                                    "AND a.situacao = 'A' " +
                                    "AND f.situacao = 'A' " +
                                    "AND (f.id_turma = ? OR ? = -1) " +
                                    "ORDER BY a.data, f.nome_ficha";

                    try (PreparedStatement stmt = con.prepareStatement(sql)) {

                        stmt.setInt(1, mes);
                        stmt.setInt(2, ano);
                        stmt.setInt(3, idTurmaSelecionada);
                        stmt.setInt(4, idTurmaSelecionada);

                        try (ResultSet rs = stmt.executeQuery()) {

                            while (rs.next()) {

                                int idFicha =
                                        rs.getInt("id_ficha");

                                java.sql.Date dataAgendada =
                                        rs.getDate("data_agendada");

                                String nomeFicha =
                                        rs.getString("nome_ficha");

                                String preparo =
                                        rs.getString("preparo");

                                String nomeTurma =
                                        rs.getString("nome_turma");

                                String situacao =
                                        rs.getString("situacao_agendamento");

                                AulaAgenda aula = new AulaAgenda(
                                        idFicha,
                                        0,
                                        dataAgendada,
                                        nomeFicha,
                                        preparo,
                                        nomeTurma,
                                        situacao,
                                        "#F7941D"
                                );

                                Calendar cal = Calendar.getInstance();
                                cal.setTime(dataAgendada);

                                int dia = cal.get(Calendar.DAY_OF_MONTH);

                                List<AulaAgenda> listaDia =
                                        eventosDoMes.get(dia);

                                if (listaDia == null) {
                                    listaDia = new ArrayList<>();
                                    eventosDoMes.put(dia, listaDia);
                                }

                                listaDia.add(aula);
                            }
                        }
                    }
                }

            } catch (SQLException e) {
                Log.e(
                        TAG,
                        "Erro ao consultar fichas da turma selecionada",
                        e
                );
            }

            mainHandler.post(() -> {
                if (isAdded()
                        && calendarGrid != null
                        && getView() != null) {

                    populateCalendar(calendarGrid, inflater);
                    atualizarDataSelecionada();
                    atualizarDetalhesDiaSelecionado();
                    atualizarSugestoesPesquisa();
                }
            });
        });
    }

    private void atualizarSugestoesPesquisa() {
        if (etPesquisa == null || getContext() == null) return;

        Context context = getContext();
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences("SessaoApp", Context.MODE_PRIVATE);
        final int idTurmaSelecionada = prefs.getInt("id_turma_selecionada", -1);

        garantirExecutorAtivo();

        executorService.execute(() -> {
            List<String> sugestoes = new ArrayList<>();
            try (Connection con = ConexaoMySQL.conectar()) {
                if (con != null) {
                    String sql = "SELECT DISTINCT f.nome_ficha " +
                            "FROM ficha f " +
                            "WHERE f.situacao = 'A' " +
                            "AND (f.id_turma = ? OR ? = -1) " +
                            "ORDER BY f.nome_ficha";

                    try (PreparedStatement stmt = con.prepareStatement(sql)) {
                        stmt.setInt(1, idTurmaSelecionada);
                        stmt.setInt(2, idTurmaSelecionada);
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                String nome = rs.getString("nome_ficha");
                                if (nome != null && !nome.trim().isEmpty()) {
                                    String nomeTrim = nome.trim();
                                    if (!sugestoes.contains(nomeTrim)) {
                                        sugestoes.add(nomeTrim);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao carregar sugestões de pesquisa", e);
            }

            mainHandler.post(() -> {
                if (isAdded() && getContext() != null && etPesquisa != null) {
                    SugestoesFuzzyAdapter adapter = new SugestoesFuzzyAdapter(
                            requireContext(),
                            sugestoes
                    );
                    etPesquisa.setAdapter(adapter);
                }
            });
        });
    }

    private class SugestoesFuzzyAdapter extends ArrayAdapter<String> {
        private final List<String> todasSugestoes;

        public SugestoesFuzzyAdapter(@NonNull Context context, @NonNull List<String> sugestoes) {
            super(context, R.layout.item_autocomplete_suggestion, new ArrayList<>(sugestoes));
            this.todasSugestoes = new ArrayList<>(sugestoes);
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    if (constraint == null || constraint.toString().trim().isEmpty()) {
                        results.values = todasSugestoes;
                        results.count = todasSugestoes.size();
                    } else {
                        String termo = constraint.toString().trim().toLowerCase();
                        List<String> filtradas = new ArrayList<>();

                        for (String sugestao : todasSugestoes) {
                            if (sugestao.toLowerCase().contains(termo)) {
                                filtradas.add(sugestao);
                            }
                        }

                        for (String sugestao : todasSugestoes) {
                            if (!filtradas.contains(sugestao) && ehSimilar(termo, sugestao)) {
                                filtradas.add(sugestao);
                            }
                        }

                        results.values = filtradas;
                        results.count = filtradas.size();
                    }
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    clear();
                    if (results != null && results.values != null) {
                        @SuppressWarnings("unchecked")
                        List<String> lista = (List<String>) results.values;
                        addAll(lista);
                    }
                    notifyDataSetChanged();
                }
            };
        }
    }


    private void populateCalendar(GridLayout grid, LayoutInflater inflater) {
        grid.removeAllViews();
        lastSelectedDayView = null;
        lastSelectedTextView = null;

        Calendar tempCal = (Calendar) currentCalendar.clone();
        tempCal.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK); // Sunday = 1, Monday = 2...
        int startOffset = firstDayOfWeek - 1; // Sunday = 0
        int daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        float density = getResources().getDisplayMetrics().density;

        for (int i = 0; i < startOffset; i++) {
            View spacer = new View(getContext());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = (int) (52 * density);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            spacer.setLayoutParams(params);
            spacer.setBackgroundResource(R.drawable.bg_calendar_day_weekday);
            grid.addView(spacer);
        }

        for (int day = 1; day <= daysInMonth; day++) {
            final int currentDay = day;
            View dayView = inflater.inflate(R.layout.item_calendar_day, grid, false);
            TextView dayText = dayView.findViewById(R.id.day_text);
            View dayContainer = dayView.findViewById(R.id.day_container);
            ImageView dot1 = dayView.findViewById(R.id.dot1);
            ImageView dot2 = dayView.findViewById(R.id.dot2);

            dayText.setText(String.valueOf(day));

            int dayOfWeek = (day + startOffset - 1) % 7;
            boolean isWeekend = (dayOfWeek == 0 || dayOfWeek == 6);

            Calendar tempDayCal = (Calendar) currentCalendar.clone();
            tempDayCal.set(Calendar.DAY_OF_MONTH, day);
            String nomeFeriado = getFeriadoNacional(tempDayCal);
            boolean isBloqueado = isWeekend || (nomeFeriado != null);

            if (isBloqueado) {
                dayContainer.setBackgroundResource(R.drawable.bg_calendar_day_weekend);
                dayText.setTextColor(Color.parseColor("#64748B"));
            } else {
                dayContainer.setBackgroundResource(R.drawable.bg_calendar_day_weekday);
                dayText.setTextColor(Color.parseColor("#002747"));
            }

            if (day == selectedDay) {
                dayContainer.setBackgroundResource(R.drawable.bg_calendar_day_selected);
                dayText.setTextColor(Color.WHITE);
                lastSelectedDayView = dayContainer;
                lastSelectedTextView = dayText;
            }

            // Exibir indicadores dinâmicos de aulas (filtradas pela busca)
            List<AulaAgenda> aulas = filtrarAulas(eventosDoMes.get(day));
            if (aulas != null && !aulas.isEmpty()) {
                dot1.setVisibility(View.VISIBLE);
                String hexColor = (aulas.get(0).getCorIndicador() != null) ? aulas.get(0).getCorIndicador() : "#F7941D";
                try {
                    dot1.setImageTintList(ColorStateList.valueOf(Color.parseColor(hexColor)));
                } catch (Exception e) {
                    dot1.setImageTintList(ColorStateList.valueOf(Color.parseColor("#F7941D")));
                }

                if (aulas.size() > 1) {
                    dot2.setVisibility(View.VISIBLE);
                    String hexColor2 = (aulas.get(1).getCorIndicador() != null) ? aulas.get(1).getCorIndicador() : "#F7941D";
                    try {
                        dot2.setImageTintList(ColorStateList.valueOf(Color.parseColor(hexColor2)));
                    } catch (Exception e) {
                        dot2.setImageTintList(ColorStateList.valueOf(Color.parseColor("#F7941D")));
                    }
                }

                // Quando houver filtro ativo, destacar no calendário os dias da receita pesquisada
                if (!termoFiltro.isEmpty() && day != selectedDay) {
                    dayContainer.setBackgroundResource(R.drawable.bg_calendar_day_selected);
                    dayText.setTextColor(Color.WHITE);
                }
            }

            dayContainer.setOnClickListener(v -> {
                if (nomeFeriado != null) {
                    Toast.makeText(getContext(), "Feriado bloqueado: " + nomeFeriado, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (isWeekend) {
                    Toast.makeText(getContext(), "Final de semana bloqueado para agendamentos.", Toast.LENGTH_SHORT).show();
                    return;
                }

                selectedDay = currentDay;
                if (lastSelectedDayView != null && lastSelectedTextView != null && lastSelectedDayView != dayContainer) {
                    int prevDay = (int) lastSelectedDayView.getTag();
                    Calendar prevCal = (Calendar) currentCalendar.clone();
                    prevCal.set(Calendar.DAY_OF_MONTH, prevDay);

                    int prevDayOfWeek = (prevDay + startOffset - 1) % 7;
                    boolean prevIsWeekend = (prevDayOfWeek == 0 || prevDayOfWeek == 6);
                    boolean prevIsFeriado = (getFeriadoNacional(prevCal) != null);

                    if (prevIsWeekend || prevIsFeriado) {
                        lastSelectedDayView.setBackgroundResource(R.drawable.bg_calendar_day_weekend);
                        lastSelectedTextView.setTextColor(Color.parseColor("#64748B"));
                    } else {
                        lastSelectedDayView.setBackgroundResource(R.drawable.bg_calendar_day_weekday);
                        lastSelectedTextView.setTextColor(Color.parseColor("#002747"));
                    }
                }
                dayContainer.setBackgroundResource(R.drawable.bg_calendar_day_selected);
                dayText.setTextColor(Color.WHITE);
                lastSelectedDayView = dayContainer;
                lastSelectedTextView = dayText;

                atualizarDataSelecionada();
                atualizarDetalhesDiaSelecionado();
            });

            dayContainer.setTag(day);
            grid.addView(dayView);
        }

        int totalCells = startOffset + daysInMonth;
        int remaining = (7 - (totalCells % 7)) % 7;
        for (int i = 0; i < remaining; i++) {
            View spacer = new View(getContext());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = (int) (52 * density);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            spacer.setLayoutParams(params);
            spacer.setBackgroundResource(R.drawable.bg_calendar_day_weekday);
            grid.addView(spacer);
        }
    }

    private void atualizarDetalhesDiaSelecionado() {
        if (layoutFichasLista == null) return;

        layoutFichasLista.removeAllViews();

        Calendar temp = (Calendar) currentCalendar.clone();
        temp.set(Calendar.DAY_OF_MONTH, selectedDay);
        int dayOfWeek = temp.get(Calendar.DAY_OF_WEEK);
        boolean isWeekend = (dayOfWeek == Calendar.SUNDAY || dayOfWeek == Calendar.SATURDAY);

        String nomeFeriado = getFeriadoNacional(temp);

        if (nomeFeriado != null) {
            TextView tvBlocked = new TextView(getContext());
            tvBlocked.setText("Feriado Nacional: " + nomeFeriado + " (Bloqueado para agendamentos)");
            tvBlocked.setTextColor(Color.parseColor("#DC2626"));
            tvBlocked.setTextSize(14);
            tvBlocked.setTypeface(null, android.graphics.Typeface.BOLD);
            tvBlocked.setPadding(0, 12, 0, 12);
            layoutFichasLista.addView(tvBlocked);
            return;
        } else if (isWeekend) {
            TextView tvBlocked = new TextView(getContext());
            tvBlocked.setText("Final de semana bloqueado.");
            tvBlocked.setTextColor(Color.parseColor("#DC2626"));
            tvBlocked.setTextSize(14);
            tvBlocked.setTypeface(null, android.graphics.Typeface.BOLD);
            tvBlocked.setPadding(0, 12, 0, 12);
            layoutFichasLista.addView(tvBlocked);
            return;
        }

        List<AulaAgenda> aulas = filtrarAulas(eventosDoMes.get(selectedDay));

        if (aulas != null && !aulas.isEmpty()) {
            for (AulaAgenda aula : aulas) {
                com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(requireContext());
                card.setRadius(12 * getResources().getDisplayMetrics().density);
                card.setCardElevation(2 * getResources().getDisplayMetrics().density);
                card.setCardBackgroundColor(Color.parseColor("#F8FAFC"));
                card.setStrokeWidth(0);

                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                cardParams.setMargins(0, 4, 0, 10);
                card.setLayoutParams(cardParams);

                LinearLayout innerLayout = new LinearLayout(getContext());
                innerLayout.setOrientation(LinearLayout.HORIZONTAL);
                innerLayout.setPadding(0, 0, 12, 0);

                View accentBar = new View(getContext());
                LinearLayout.LayoutParams accentParams = new LinearLayout.LayoutParams(
                        (int) (6 * getResources().getDisplayMetrics().density), ViewGroup.LayoutParams.MATCH_PARENT);
                accentBar.setLayoutParams(accentParams);
                accentBar.setBackgroundColor(Color.parseColor("#F7941D"));
                innerLayout.addView(accentBar);

                LinearLayout contentLayout = new LinearLayout(getContext());
                contentLayout.setOrientation(LinearLayout.VERTICAL);
                contentLayout.setPadding(12, 10, 12, 10);
                LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                contentLayout.setLayoutParams(contentParams);

                TextView tvTitulo = new TextView(getContext());
                tvTitulo.setText(aula.getTitulo());
                tvTitulo.setTextColor(Color.parseColor("#002747"));
                tvTitulo.setTextSize(15);
                tvTitulo.setTypeface(null, android.graphics.Typeface.BOLD);
                contentLayout.addView(tvTitulo);

                if (aula.getTurma() != null && !aula.getTurma().isEmpty()) {
                    TextView tvTurma = new TextView(getContext());
                    tvTurma.setText("Turma: " + aula.getTurma());
                    tvTurma.setTextColor(Color.parseColor("#F7941D"));
                    tvTurma.setTextSize(13);
                    tvTurma.setTypeface(null, android.graphics.Typeface.BOLD);
                    contentLayout.addView(tvTurma);
                }

                if (aula.getDescricao() != null && !aula.getDescricao().isEmpty()) {
                    TextView tvDesc = new TextView(getContext());
                    tvDesc.setText(aula.getDescricao());
                    tvDesc.setTextColor(Color.parseColor("#475569"));
                    tvDesc.setTextSize(13);
                    tvDesc.setPadding(0, 4, 0, 0);
                    contentLayout.addView(tvDesc);
                }

                innerLayout.addView(contentLayout);
                card.addView(innerLayout);

                card.setOnClickListener(v -> {
                    Bundle bundle = new Bundle();
                    bundle.putInt("id_ficha", aula.getIdAgenda());
                    bundle.putString("nome_ficha", aula.getTitulo());
                    androidx.navigation.Navigation.findNavController(v).navigate(R.id.nav_detalhes_receita, bundle);
                });

                layoutFichasLista.addView(card);
            }
        } else {
            if (textNoFichas == null) {
                textNoFichas = new TextView(getContext());
                textNoFichas.setTextColor(Color.parseColor("#64748B"));
                textNoFichas.setTextSize(14);
            }
            if (!termoFiltro.isEmpty()) {
                textNoFichas.setText("Nenhuma receita ou aula encontrada para \"" + termoFiltro + "\".");
            } else {
                textNoFichas.setText("Nenhuma aula ou ficha programada para este dia.");
            }
            layoutFichasLista.addView(textNoFichas);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executorService.shutdown();
    }
}
