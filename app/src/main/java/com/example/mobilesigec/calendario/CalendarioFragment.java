package com.example.mobilesigec.calendario;

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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CalendarioFragment extends Fragment {

    private static final String TAG = "CalendarioFragment";

    private Calendar currentCalendar;
    private int selectedDay = 2;

    private GridLayout calendarGrid;
    private TextView textMonthYear;
    private TextView textCurrentDate;
    private LinearLayout layoutFichasLista;
    private TextView textNoFichas;
    private ImageView btnPrevMonth;
    private ImageView btnNextMonth;

    private final Map<Integer, List<AulaAgenda>> eventosDoMes = new HashMap<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private View lastSelectedDayView = null;

    public CalendarioFragment() {
        // Required empty public constructor
    }

    public static CalendarioFragment newInstance() {
        return new CalendarioFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentCalendar = Calendar.getInstance();
        currentCalendar.set(Calendar.YEAR, 2026);
        currentCalendar.set(Calendar.MONTH, Calendar.SEPTEMBER);
        currentCalendar.set(Calendar.DAY_OF_MONTH, 2);
        selectedDay = 2;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendario, container, false);

        calendarGrid = view.findViewById(R.id.calendar_grid);
        textMonthYear = view.findViewById(R.id.text_month_year);
        textCurrentDate = view.findViewById(R.id.text_current_date);
        layoutFichasLista = view.findViewById(R.id.layout_fichas_lista);
        textNoFichas = view.findViewById(R.id.text_no_fichas);
        btnPrevMonth = view.findViewById(R.id.btn_prev_month);
        btnNextMonth = view.findViewById(R.id.btn_next_month);

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

        carregarCalendarioEEventos(inflater);

        return view;
    }

    private void carregarCalendarioEEventos(LayoutInflater inflater) {
        atualizarTituloMesAno();
        atualizarDataSelecionada();
        buscarEventosAivenEAtualizarGrid(inflater);
    }

    private void atualizarTituloMesAno() {
        if (textMonthYear != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy.", Locale.forLanguageTag("pt-BR"));
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

    private void carregarMockDefaultSetembro() {
        // Mock default exatamente conforme o protótipo para Setembro de 2026
        if (currentCalendar.get(Calendar.YEAR) == 2026 && currentCalendar.get(Calendar.MONTH) == Calendar.SEPTEMBER) {
            adicionarEventoMock(1, "Preparo de Molhos Básicos", "Gastronomia A", "Aula Prática", "#F57C00");
            adicionarEventoMock(2, "Cortes de Carnes e Aves", "Gastronomia B", "Aula Prática", "#F57C00");
            adicionarEventoMock(8, "Confeitaria Clássica", "Panificação I", "Aula Prática", "#F57C00");
            adicionarEventoMock(8, "Técnicas de Empratamento", "Panificação I", "Aula Teórica", "#F57C00");
            adicionarEventoMock(10, "Panificação Artesanal", "Panificação II", "Aula Prática", "#F57C00");
            adicionarEventoMock(14, "Cozinha Internacional", "Gastronomia C", "Aula Prática", "#F57C00");
        }
    }

    private void adicionarEventoMock(int dia, String titulo, String turma, String desc, String cor) {
        List<AulaAgenda> lista = eventosDoMes.get(dia);
        if (lista == null) {
            lista = new ArrayList<>();
            eventosDoMes.put(dia, lista);
        }
        Calendar cal = (Calendar) currentCalendar.clone();
        cal.set(Calendar.DAY_OF_MONTH, dia);
        lista.add(new AulaAgenda(dia, 1, cal.getTime(), titulo, desc, turma, "Agendado", cor));
    }

    private void buscarEventosAivenEAtualizarGrid(LayoutInflater inflater) {
        eventosDoMes.clear();
        carregarMockDefaultSetembro();

        int mes = currentCalendar.get(Calendar.MONTH) + 1; // 1-based
        int ano = currentCalendar.get(Calendar.YEAR);

        executorService.execute(() -> {
            Connection con = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                con = ConexaoMySQL.conectar();
                if (con != null) {
                    String sql = "SELECT id_agenda, id_usuario, data_aula, titulo, descricao, turma, status, cor_indicador " +
                            "FROM agenda_aula WHERE MONTH(data_aula) = ? AND YEAR(data_aula) = ?";
                    stmt = con.prepareStatement(sql);
                    stmt.setInt(1, mes);
                    stmt.setInt(2, ano);
                    rs = stmt.executeQuery();

                    while (rs.next()) {
                        int idAgenda = rs.getInt("id_agenda");
                        int idUsuario = rs.getInt("id_usuario");
                        Date dataAula = rs.getDate("data_aula");
                        String titulo = rs.getString("titulo");
                        String descricao = rs.getString("descricao");
                        String turma = rs.getString("turma");
                        String status = rs.getString("status");
                        String corIndicador = rs.getString("cor_indicador");

                        AulaAgenda aula = new AulaAgenda(idAgenda, idUsuario, dataAula, titulo, descricao, turma, status, corIndicador);

                        Calendar cal = Calendar.getInstance();
                        cal.setTime(dataAula);
                        int dia = cal.get(Calendar.DAY_OF_MONTH);

                        List<AulaAgenda> listaDia = eventosDoMes.get(dia);
                        if (listaDia == null) {
                            listaDia = new ArrayList<>();
                            eventosDoMes.put(dia, listaDia);
                        }
                        listaDia.add(aula);
                    }
                }
            } catch (SQLException e) {
                Log.e(TAG, "Erro ao buscar eventos do banco de dados Aiven: " + e.getMessage());
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (stmt != null) stmt.close();
                    if (con != null) ConexaoMySQL.fecharConexao(con);
                } catch (SQLException e) {
                    Log.e(TAG, "Erro ao fechar conexao/stmt/rs: " + e.getMessage());
                }
            }

            mainHandler.post(() -> {
                if (isAdded()) {
                    populateCalendar(calendarGrid, inflater);
                    atualizarDetalhesDiaSelecionado();
                }
            });
        });
    }

    private void populateCalendar(GridLayout grid, LayoutInflater inflater) {
        if (grid == null) return;
        grid.removeAllViews();
        lastSelectedDayView = null;

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

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = (int) (52 * density);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            dayView.setLayoutParams(params);

            TextView dayText = dayView.findViewById(R.id.day_text);
            View dayContainer = dayView.findViewById(R.id.day_container);
            ImageView dot1 = dayView.findViewById(R.id.dot1);
            ImageView dot2 = dayView.findViewById(R.id.dot2);
            ImageView dotWeekend = dayView.findViewById(R.id.dot_weekend);

            dayText.setText(String.valueOf(day));

            int dayOfWeek = (day + startOffset - 1) % 7;
            boolean isNonWorkingDay = (dayOfWeek == 0 || dayOfWeek == 6 || (day == 7 && currentCalendar.get(Calendar.MONTH) == Calendar.SEPTEMBER));

            if (isNonWorkingDay) {
                dayContainer.setBackgroundResource(R.drawable.bg_calendar_day_weekend);
                dayText.setTextColor(Color.WHITE);
                dotWeekend.setVisibility(View.VISIBLE);
                dotWeekend.setImageTintList(ColorStateList.valueOf(Color.BLACK));
            } else {
                dayContainer.setBackgroundResource(R.drawable.bg_calendar_day_weekday);
                dayText.setTextColor(Color.parseColor("#002F6C"));
            }

            if (day == selectedDay) {
                dayContainer.setBackgroundResource(R.drawable.bg_calendar_day_selected);
                lastSelectedDayView = dayContainer;
            }

            List<AulaAgenda> aulas = eventosDoMes.get(day);
            if (aulas != null && !aulas.isEmpty()) {
                dot1.setVisibility(View.VISIBLE);
                String hexColor = (aulas.get(0).getCorIndicador() != null) ? aulas.get(0).getCorIndicador() : "#F57C00";
                try {
                    dot1.setImageTintList(ColorStateList.valueOf(Color.parseColor(hexColor)));
                } catch (Exception e) {
                    dot1.setImageTintList(ColorStateList.valueOf(Color.parseColor("#F57C00")));
                }

                if (aulas.size() > 1) {
                    dot2.setVisibility(View.VISIBLE);
                    String hexColor2 = (aulas.get(1).getCorIndicador() != null) ? aulas.get(1).getCorIndicador() : "#F57C00";
                    try {
                        dot2.setImageTintList(ColorStateList.valueOf(Color.parseColor(hexColor2)));
                    } catch (Exception e) {
                        dot2.setImageTintList(ColorStateList.valueOf(Color.parseColor("#F57C00")));
                    }
                }
            }

            dayContainer.setOnClickListener(v -> {
                if (isNonWorkingDay) {
                    Toast.makeText(getContext(), "Dia não letivo / Final de semana.", Toast.LENGTH_SHORT).show();
                    return;
                }

                selectedDay = currentDay;
                if (lastSelectedDayView != null && lastSelectedDayView != dayContainer) {
                    Object tagObj = lastSelectedDayView.getTag();
                    if (tagObj instanceof Integer) {
                        int tagDay = (Integer) tagObj;
                        int prevDayOfWeek = (tagDay + startOffset - 1) % 7;
                        if (prevDayOfWeek == 0 || prevDayOfWeek == 6 || (tagDay == 7 && currentCalendar.get(Calendar.MONTH) == Calendar.SEPTEMBER)) {
                            lastSelectedDayView.setBackgroundResource(R.drawable.bg_calendar_day_weekend);
                        } else {
                            lastSelectedDayView.setBackgroundResource(R.drawable.bg_calendar_day_weekday);
                        }
                    } else {
                        lastSelectedDayView.setBackgroundResource(R.drawable.bg_calendar_day_weekday);
                    }
                }
                dayContainer.setBackgroundResource(R.drawable.bg_calendar_day_selected);
                lastSelectedDayView = dayContainer;

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

        if (isWeekend) {
            TextView tvBlocked = new TextView(getContext());
            tvBlocked.setText("Final de semana bloqueado.");
            tvBlocked.setTextColor(Color.parseColor("#C0392B"));
            tvBlocked.setTextSize(13);
            tvBlocked.setTypeface(null, android.graphics.Typeface.BOLD);
            layoutFichasLista.addView(tvBlocked);
            return;
        }

        List<AulaAgenda> aulas = eventosDoMes.get(selectedDay);

        if (aulas != null && !aulas.isEmpty()) {
            for (AulaAgenda aula : aulas) {
                LinearLayout card = new LinearLayout(getContext());
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(12, 8, 12, 8);
                card.setBackgroundColor(Color.parseColor("#F5F5F5"));

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 4, 0, 8);
                card.setLayoutParams(params);

                TextView tvTitulo = new TextView(getContext());
                tvTitulo.setText(aula.getTitulo());
                tvTitulo.setTextColor(Color.parseColor("#002F6C"));
                tvTitulo.setTextSize(14);
                tvTitulo.setTypeface(null, android.graphics.Typeface.BOLD);
                card.addView(tvTitulo);

                if (aula.getTurma() != null && !aula.getTurma().isEmpty()) {
                    TextView tvTurma = new TextView(getContext());
                    tvTurma.setText("Turma: " + aula.getTurma());
                    tvTurma.setTextColor(Color.parseColor("#F57C00"));
                    tvTurma.setTextSize(12);
                    card.addView(tvTurma);
                }

                if (aula.getDescricao() != null && !aula.getDescricao().isEmpty()) {
                    TextView tvDesc = new TextView(getContext());
                    tvDesc.setText(aula.getDescricao());
                    tvDesc.setTextColor(Color.parseColor("#555555"));
                    tvDesc.setTextSize(12);
                    card.addView(tvDesc);
                }

                layoutFichasLista.addView(card);
            }
        } else {
            if (textNoFichas == null) {
                textNoFichas = new TextView(getContext());
                textNoFichas.setText("Nenhuma aula ou ficha programada para este dia.");
                textNoFichas.setTextColor(Color.parseColor("#757575"));
                textNoFichas.setTextSize(13);
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
