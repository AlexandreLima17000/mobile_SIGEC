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
    private int selectedDay = 1;

    private GridLayout calendarGrid;
    private TextView textMonthYear;
    private TextView textCurrentDate;
    private TextView textInstrutorHeader;
    private LinearLayout layoutFichasLista;
    private TextView textNoFichas;
    private ImageView btnPrevMonth;
    private ImageView btnNextMonth;

    private final Map<Integer, List<AulaAgenda>> eventosDoMes = new HashMap<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
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

        calendarGrid = view.findViewById(R.id.calendar_grid);
        textMonthYear = view.findViewById(R.id.text_month_year);
        textCurrentDate = view.findViewById(R.id.text_current_date);
        textInstrutorHeader = view.findViewById(R.id.text_instrutor_header);
        layoutFichasLista = view.findViewById(R.id.layout_fichas_lista);
        textNoFichas = view.findViewById(R.id.text_no_fichas);
        btnPrevMonth = view.findViewById(R.id.btn_prev_month);
        btnNextMonth = view.findViewById(R.id.btn_next_month);

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

        carregarCalendarioEEventos(inflater);

        return view;
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
        int mes = currentCalendar.get(Calendar.MONTH) + 1; // 1-based
        int ano = currentCalendar.get(Calendar.YEAR);

        int idUsuarioLogado = -1;
        if (getActivity() != null) {
            SharedPreferences prefs = getActivity().getSharedPreferences("SessaoApp", Context.MODE_PRIVATE);
            idUsuarioLogado = prefs.getInt("ID_USUARIO", -1);
        }

        final int finalIdUsuario = idUsuarioLogado;

        executorService.execute(() -> {
            try (Connection con = ConexaoMySQL.conectar()) {
                if (con != null) {
                    String sql = "SELECT id_agenda, id_usuario, data_aula, titulo, descricao, turma, status, cor_indicador " +
                            "FROM agenda_aula WHERE MONTH(data_aula) = ? AND YEAR(data_aula) = ?" +
                            (finalIdUsuario != -1 ? " AND (id_usuario = ? OR id_usuario IS NULL)" : "");

                    try (PreparedStatement stmt = con.prepareStatement(sql)) {
                        stmt.setInt(1, mes);
                        stmt.setInt(2, ano);
                        if (finalIdUsuario != -1) {
                            stmt.setInt(3, finalIdUsuario);
                        }
                        try (ResultSet rs = stmt.executeQuery()) {
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
                    }
                }
            } catch (SQLException e) {
                Log.e(TAG, "Erro ao buscar eventos do banco de dados: " + e.getMessage());
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

            // Exibir indicadores dinâmicos de aulas
            List<AulaAgenda> aulas = eventosDoMes.get(day);
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

        List<AulaAgenda> aulas = eventosDoMes.get(selectedDay);

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
                layoutFichasLista.addView(card);
            }
        } else {
            if (textNoFichas == null) {
                textNoFichas = new TextView(getContext());
                textNoFichas.setText("Nenhuma aula ou ficha programada para este dia.");
                textNoFichas.setTextColor(Color.parseColor("#64748B"));
                textNoFichas.setTextSize(14);
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
