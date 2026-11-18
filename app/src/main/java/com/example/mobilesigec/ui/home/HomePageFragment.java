package com.example.mobilesigec.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import com.example.mobilesigec.R;
import com.example.mobilesigec.database.CalendarioCache;
import com.example.mobilesigec.model.TurmaModelo;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class HomePageFragment extends Fragment {

    private TextView textSaudacao, textDataHome;
    private Spinner spinnerTurmasHome;

    private ExecutorService executorService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicInteger idRequisicaoHome = new AtomicInteger(0);
    private int ultimaTurmaRenderizada = Integer.MIN_VALUE;

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
        spinnerTurmasHome = view.findViewById(R.id.spinnerTurmasHome);
        CardView cardInsumos = view.findViewById(R.id.card_checklist_insumos);
        CardView cardUtensilios = view.findViewById(R.id.card_checklist_utensilios);
        View cardReceitasHoje = view.findViewById(R.id.card_receitas_hoje);

        Context context = getContext();
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences("SessaoApp", Context.MODE_PRIVATE);
            String nomeCompleto = prefs.getString("nome_usuario", "Instrutor");
            String nomeFormatado = formatarNomeCurto(nomeCompleto);

            if (textSaudacao != null) {
                textSaudacao.setText("Bom dia, " + nomeFormatado + "!");
            }

            // Renderização imediata antes mesmo do primeiro frame se o cache já estiver pronto
            int idTurmaSalva = prefs.getInt("id_turma_selecionada", -1);
            List<TurmaModelo> turmasCache = CalendarioCache.obterTurmasCache();
            if (!turmasCache.isEmpty()) {
                TurmaModelo turmaAlvo = turmasCache.get(0);
                for (TurmaModelo t : turmasCache) {
                    if (t.getIdTurma() == idTurmaSalva) {
                        turmaAlvo = t;
                        break;
                    }
                }
                Map<Integer, List<CalendarioCache.ItemReceitaCache>> semanaCache =
                        CalendarioCache.obterReceitasSemanaSeDisponivel(turmaAlvo.getIdTurma());
                if (semanaCache != null) {
                    ultimaTurmaRenderizada = turmaAlvo.getIdTurma();
                    renderizarCardsSemana(view, semanaCache, turmaAlvo.getNomeLaboratorio());
                }
            }
        }

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd 'DE' MMMM", Locale.forLanguageTag("pt-BR"));
        String dataFormatada = sdf.format(new Date()).toUpperCase();
        if (textDataHome != null) {
            textDataHome.setText(dataFormatada);
        }

        carregarTurmasNoSpinner();

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

    private void dispararPreloadCalendario(int idTurma) {
        Calendar hoje = Calendar.getInstance();
        int mesAtual = hoje.get(Calendar.MONTH) + 1;
        int anoAtual = hoje.get(Calendar.YEAR);
        CalendarioCache.preCarregarMesAsync(idTurma, mesAtual, anoAtual);
    }

    private void carregarTurmasNoSpinner() {
        if (spinnerTurmasHome == null || getContext() == null) return;

        List<TurmaModelo> turmasEmCache = CalendarioCache.obterTurmasCache();
        if (!turmasEmCache.isEmpty()) {
            configurarSpinnerHome(turmasEmCache);
            return;
        }

        SharedPreferences prefs = getContext().getSharedPreferences("SessaoApp", Context.MODE_PRIVATE);
        int idUsuarioLogado = prefs.getInt("id_usuario", -1);

        garantirExecutorAtivo();

        executorService.execute(() -> {
            List<TurmaModelo> listaTurmas = CalendarioCache.buscarTurmasNoBanco(idUsuarioLogado);
            if (!listaTurmas.isEmpty()) {
                CalendarioCache.salvarTurmasCache(listaTurmas);
            }
            mainHandler.post(() -> configurarSpinnerHome(listaTurmas));
        });
    }

    private void configurarSpinnerHome(List<TurmaModelo> listaTurmas) {
        if (!isAdded() || getContext() == null || spinnerTurmasHome == null) return;

        SharedPreferences prefs = getContext().getSharedPreferences("SessaoApp", Context.MODE_PRIVATE);

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

        if (listaTurmas.isEmpty()) {
            prefs.edit().remove("id_turma_selecionada").apply();
            return;
        }

        int idTurmaSalva = prefs.getInt("id_turma_selecionada", -1);
        int posicaoInicial = 0;
        for (int i = 0; i < listaTurmas.size(); i++) {
            if (listaTurmas.get(i).getIdTurma() == idTurmaSalva) {
                posicaoInicial = i;
                break;
            }
        }

        spinnerTurmasHome.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View selectedView, int position, long id) {
                if (position < 0 || position >= listaTurmas.size()) return;
                TurmaModelo turmaSelecionada = (TurmaModelo) parent.getItemAtPosition(position);
                if (turmaSelecionada != null) {
                    int idTurma = turmaSelecionada.getIdTurma();
                    prefs.edit().putInt("id_turma_selecionada", idTurma).apply();

                    dispararPreloadCalendario(idTurma);
                    if (idTurma != ultimaTurmaRenderizada) {
                        carregarReceitasDaTurma(idTurma, turmaSelecionada.getNomeLaboratorio());
                    }
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        spinnerTurmasHome.setSelection(posicaoInicial, false);
        TurmaModelo turmaInicial = listaTurmas.get(posicaoInicial);
        int idInicial = turmaInicial.getIdTurma();
        if (idTurmaSalva != idInicial) {
            prefs.edit().putInt("id_turma_selecionada", idInicial).apply();
        }

        dispararPreloadCalendario(idInicial);
        if (idInicial != ultimaTurmaRenderizada) {
            carregarReceitasDaTurma(idInicial, turmaInicial.getNomeLaboratorio());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getContext() == null) return;

        if (spinnerTurmasHome == null || spinnerTurmasHome.getAdapter() == null || spinnerTurmasHome.getAdapter().getCount() == 0) {
            carregarTurmasNoSpinner();
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
                    carregarReceitasDaTurma(t.getIdTurma(), t.getNomeLaboratorio());
                }
                break;
            }
        }
    }

    private void carregarReceitasDaTurma(int idTurma, String nomeLaboratorio) {
        if (!isAdded() || getContext() == null) return;

        Map<Integer, List<CalendarioCache.ItemReceitaCache>> cacheSemana =
                CalendarioCache.obterReceitasSemanaSeDisponivel(idTurma);

        if (cacheSemana != null) {
            ultimaTurmaRenderizada = idTurma;
            renderizarCardsSemana(getView(), cacheSemana, nomeLaboratorio);
            return;
        }

        final int reqId = idRequisicaoHome.incrementAndGet();
        garantirExecutorAtivo();

        executorService.execute(() -> {
            Map<Integer, List<CalendarioCache.ItemReceitaCache>> mapDias =
                    CalendarioCache.buscarReceitasSemanaNoBanco(idTurma);

            CalendarioCache.salvarReceitasSemanaCache(idTurma, mapDias);

            mainHandler.post(() -> {
                if (!isAdded() || getContext() == null || reqId != idRequisicaoHome.get()) return;
                ultimaTurmaRenderizada = idTurma;
                renderizarCardsSemana(getView(), mapDias, nomeLaboratorio);
            });
        });
    }

    private void renderizarCardsSemana(View rootView, Map<Integer, List<CalendarioCache.ItemReceitaCache>> mapDias, String nomeLaboratorio) {
        if (rootView == null || getContext() == null) return;

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

            List<CalendarioCache.ItemReceitaCache> receitasDoDia = mapDias.get(diaNum);

            if (receitasDoDia == null || receitasDoDia.isEmpty()) {
                if (tvSemReceitas != null) {
                    tvSemReceitas.setVisibility(View.VISIBLE);
                }
            } else {
                if (tvSemReceitas != null) {
                    tvSemReceitas.setVisibility(View.GONE);
                }
                for (CalendarioCache.ItemReceitaCache itemData : receitasDoDia) {
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }
}