package com.example.mobilesigec.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.mobilesigec.ConexaoMySQL;
import com.example.mobilesigec.R;
import com.example.mobilesigec.model.TurmaModelo;

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

public class HomePageFragment extends Fragment {

    private static final String TAG = "HomePageFragment";

    private static final String NOME_PREFERENCIAS = "SessaoApp";
    private static final String CHAVE_TURMA_SELECIONADA =
            "id_turma_selecionada";

    private TextView textSaudacao;
    private TextView textDataHome;
    private Spinner spinnerTurmasHome;

    private ExecutorService executorService;

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private int idTurmaReceitasCarregada = Integer.MIN_VALUE;

    public HomePageFragment() {
        // Construtor obrigatório.
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_home_page,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        executorService = Executors.newSingleThreadExecutor();
        idTurmaReceitasCarregada = Integer.MIN_VALUE;

        textSaudacao = view.findViewById(R.id.textSaudacao);
        textDataHome = view.findViewById(R.id.textDataHome);

        CardView cardInsumos =
                view.findViewById(R.id.card_checklist_insumos);

        CardView cardUtensilios =
                view.findViewById(R.id.card_checklist_utensilios);

        CardView cardReceitasHoje =
                view.findViewById(R.id.card_receitas_hoje);

        // Nome do usuário.
        Context context = getContext();

        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(
                    NOME_PREFERENCIAS,
                    Context.MODE_PRIVATE
            );

            String nome = prefs.getString(
                    "nome_usuario",
                    "Instrutor"
            );

            if (textSaudacao != null) {
                textSaudacao.setText(
                        "Bom dia, " + formatarNomeCurto(nome) + "!"
                );
            }
        }

        // Data atual.
        SimpleDateFormat sdf = new SimpleDateFormat(
                "EEEE, dd 'DE' MMMM",
                Locale.forLanguageTag("pt-BR")
        );

        String dataFormatada =
                sdf.format(new Date()).toUpperCase(Locale.ROOT);

        if (textDataHome != null) {
            textDataHome.setText(dataFormatada);
        }

        // Carrega as turmas disponíveis.
        carregarTurmasNoSpinner(view);

        // Navegação para o checklist de insumos.
        if (cardInsumos != null) {
            cardInsumos.setOnClickListener(v ->
                    Navigation.findNavController(v)
                            .navigate(R.id.action_home_to_insumos)
            );
        }

        // Navegação para o checklist de utensílios.
        if (cardUtensilios != null) {
            cardUtensilios.setOnClickListener(v ->
                    Navigation.findNavController(v)
                            .navigate(R.id.action_home_to_utensilios)
            );
        }

        // Navegação para o calendário.
        if (cardReceitasHoje != null) {
            cardReceitasHoje.setOnClickListener(v -> {

                Context contextoAtual = getContext();

                if (contextoAtual == null) {
                    return;
                }

                SharedPreferences prefs =
                        contextoAtual.getSharedPreferences(
                                NOME_PREFERENCIAS,
                                Context.MODE_PRIVATE
                        );

                int idTurma = prefs.getInt(
                        CHAVE_TURMA_SELECIONADA,
                        -1
                );

                if (idTurma == -1) {
                    Toast.makeText(
                            contextoAtual,
                            "Selecione uma turma antes de abrir o calendário.",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                Navigation.findNavController(v)
                        .navigate(R.id.nav_calendario);
            });
        }
    }

    /**
     * Carrega as turmas do banco e configura o Spinner.
     */
    private void carregarTurmasNoSpinner(View view) {

        spinnerTurmasHome =
                view.findViewById(R.id.spinnerTurmasHome);

        if (spinnerTurmasHome == null
                || getContext() == null
                || executorService == null) {
            return;
        }

        SharedPreferences prefs = requireContext()
                .getSharedPreferences(
                        NOME_PREFERENCIAS,
                        Context.MODE_PRIVATE
                );

        int idUsuarioLogado = prefs.getInt(
                "id_usuario",
                -1
        );

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
                            "SELECT t.id_turma, t.nome_turma, " +
                                    "l.nome_laboratorio " +
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
                            "SELECT t.id_turma, t.nome_turma, " +
                                    "l.nome_laboratorio " +
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
                Log.e(TAG, "Erro ao carregar turmas.", e);
                return;
            }

            mainHandler.post(() -> {

                if (!isAdded()
                        || getContext() == null
                        || getView() == null
                        || spinnerTurmasHome == null) {
                    return;
                }

                Context contextoAtual = getContext();

                if (contextoAtual == null) {
                    return;
                }

                SharedPreferences preferencias =
                        contextoAtual.getSharedPreferences(
                                NOME_PREFERENCIAS,
                                Context.MODE_PRIVATE
                        );

                if (listaTurmas.isEmpty()) {

                    preferencias.edit()
                            .remove(CHAVE_TURMA_SELECIONADA)
                            .apply();

                    spinnerTurmasHome.setAdapter(
                            new ArrayAdapter<String>(
                                    contextoAtual,
                                    android.R.layout.simple_spinner_item,
                                    new ArrayList<>()
                            )
                    );

                    idTurmaReceitasCarregada = Integer.MIN_VALUE;

                    LinearLayout container =
                            getView().findViewById(
                                    R.id.container_receitas
                            );

                    if (container != null) {
                        container.removeAllViews();

                        TextView mensagem =
                                new TextView(contextoAtual);

                        mensagem.setText(
                                "Nenhuma turma ativa encontrada."
                        );
                        mensagem.setTextColor(
                                android.graphics.Color.parseColor(
                                        "#FFFFFF"
                                )
                        );
                        mensagem.setTextSize(14);
                        mensagem.setPadding(8, 12, 8, 12);

                        container.addView(mensagem);
                    }

                    return;
                }

                ArrayAdapter<TurmaModelo> adapter =
                        new ArrayAdapter<>(
                                contextoAtual,
                                android.R.layout.simple_spinner_item,
                                listaTurmas
                        );

                adapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item
                );

                spinnerTurmasHome.setAdapter(adapter);

                // Recupera a turma anteriormente selecionada.
                int idTurmaSalva = preferencias.getInt(
                        CHAVE_TURMA_SELECIONADA,
                        -1
                );

                int posicaoInicial = 0;

                for (int i = 0; i < listaTurmas.size(); i++) {
                    if (listaTurmas.get(i).getIdTurma()
                            == idTurmaSalva) {
                        posicaoInicial = i;
                        break;
                    }
                }

                spinnerTurmasHome.setOnItemSelectedListener(
                        new AdapterView.OnItemSelectedListener() {

                            @Override
                            public void onItemSelected(
                                    AdapterView<?> parent,
                                    View selectedView,
                                    int position,
                                    long id
                            ) {
                                if (position < 0
                                        || position >= listaTurmas.size()) {
                                    return;
                                }

                                TurmaModelo turmaSelecionada =
                                        (TurmaModelo) parent.getItemAtPosition(
                                                position
                                        );

                                salvarTurmaSelecionada(turmaSelecionada);
                            }

                            @Override
                            public void onNothingSelected(
                                    AdapterView<?> parent
                            ) {
                            }
                        });

                spinnerTurmasHome.setSelection(
                        posicaoInicial,
                        false
                );

                // Garante o carregamento da turma inicial.
                salvarTurmaSelecionada(
                        listaTurmas.get(posicaoInicial)
                );
            });
        });
    }

    /**
     * Salva a turma escolhida para a HomePage e o calendário.
     */
    private void salvarTurmaSelecionada(
            TurmaModelo turmaSelecionada
    ) {
        if (turmaSelecionada == null || getContext() == null) {
            return;
        }

        Context contextoAtual = getContext();

        if (contextoAtual == null) {
            return;
        }

        int idTurma = turmaSelecionada.getIdTurma();

        SharedPreferences prefs =
                contextoAtual.getSharedPreferences(
                        NOME_PREFERENCIAS,
                        Context.MODE_PRIVATE
                );

        prefs.edit()
                .putInt(CHAVE_TURMA_SELECIONADA, idTurma)
                .apply();

        if (idTurmaReceitasCarregada == idTurma) {
            return;
        }

        idTurmaReceitasCarregada = idTurma;

        carregarReceitasDaTurma(
                idTurma,
                turmaSelecionada.getNomeLaboratorio()
        );
    }

    /**
     * Busca as fichas associadas à turma selecionada.
     */
    private void carregarReceitasDaTurma(
            int idTurma,
            String nomeLaboratorio
    ) {
        View view = getView();

        if (view == null || executorService == null) {
            return;
        }

        LinearLayout containerReceitas =
                view.findViewById(R.id.container_receitas);

        if (containerReceitas == null) {
            return;
        }

        containerReceitas.removeAllViews();

        executorService.execute(() -> {

            List<ItemReceitaData> receitasList =
                    new ArrayList<>();

            try (Connection con = ConexaoMySQL.conectar()) {

                if (con == null) {
                    Log.e(TAG, "Não foi possível conectar ao MySQL.");
                    return;
                }

                // Inclui id_ficha para abrir os detalhes da receita.
                String sql =
                        "SELECT f.id_ficha, f.nome_ficha, " +
                                "COALESCE(a.concluido, 'N') AS concluido " +
                                "FROM ficha f " +
                                "LEFT JOIN agendamento a " +
                                "ON f.id_ficha = a.id_ficha " +
                                "WHERE f.id_turma = ?";

                try (PreparedStatement stmt =
                             con.prepareStatement(sql)) {

                    stmt.setInt(1, idTurma);

                    try (ResultSet rs = stmt.executeQuery()) {

                        while (rs.next()) {

                            int idFicha = rs.getInt("id_ficha");

                            String nomeFicha =
                                    rs.getString("nome_ficha");

                            String concluido =
                                    rs.getString("concluido");

                            receitasList.add(
                                    new ItemReceitaData(
                                            idFicha,
                                            nomeFicha,
                                            concluido
                                    )
                            );
                        }
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Erro ao carregar receitas da turma.", e);
            }

            mainHandler.post(() -> {

                if (!isAdded()
                        || getContext() == null
                        || getView() == null) {
                    return;
                }

                SharedPreferences prefs =
                        requireContext().getSharedPreferences(
                                NOME_PREFERENCIAS,
                                Context.MODE_PRIVATE
                        );

                // Evita mostrar resultados de uma seleção antiga.
                int turmaAtual = prefs.getInt(
                        CHAVE_TURMA_SELECIONADA,
                        -1
                );

                if (turmaAtual != idTurma) {
                    return;
                }

                View currentView = getView();

                if (currentView == null) {
                    return;
                }

                LinearLayout currentContainer =
                        currentView.findViewById(
                                R.id.container_receitas
                        );

                if (currentContainer == null) {
                    return;
                }

                currentContainer.removeAllViews();

                LayoutInflater inflater =
                        LayoutInflater.from(requireContext());

                if (receitasList.isEmpty()) {

                    TextView mensagem =
                            new TextView(requireContext());

                    mensagem.setText(
                            "Nenhuma receita encontrada para esta turma."
                    );
                    mensagem.setTextColor(
                            android.graphics.Color.parseColor("#FFFFFF")
                    );
                    mensagem.setTextSize(14);
                    mensagem.setPadding(8, 12, 8, 12);

                    currentContainer.addView(mensagem);
                    return;
                }

                for (ItemReceitaData itemData : receitasList) {

                    View itemReceita = inflater.inflate(
                            R.layout.item_receita,
                            currentContainer,
                            false
                    );

                    TextView tvNome =
                            itemReceita.findViewById(
                                    R.id.tv_nome_receita
                            );

                    TextView tvDetalhes =
                            itemReceita.findViewById(
                                    R.id.tv_detalhes_receita
                            );

                    TextView tvStatusText =
                            itemReceita.findViewById(
                                    R.id.tv_status_text
                            );

                    ImageView ivStatusIcon =
                            itemReceita.findViewById(
                                    R.id.iv_status_icon
                            );

                    if (tvNome != null) {
                        tvNome.setText(itemData.nomeFicha);
                    }

                    if (tvDetalhes != null) {
                        tvDetalhes.setText(nomeLaboratorio);
                    }

                    if ("S".equals(itemData.concluido)) {

                        if (tvStatusText != null) {
                            tvStatusText.setText("CONCLUÍDO");
                            tvStatusText.setTextColor(
                                    android.graphics.Color.parseColor(
                                            "#15803D"
                                    )
                            );
                        }

                        if (ivStatusIcon != null) {
                            ivStatusIcon.setImageResource(
                                    R.drawable.ic_check_circle
                            );
                            ivStatusIcon.setColorFilter(
                                    android.graphics.Color.parseColor(
                                            "#15803D"
                                    )
                            );
                        }

                    } else {

                        if (tvStatusText != null) {
                            tvStatusText.setText("PENDENTE");
                            tvStatusText.setTextColor(
                                    android.graphics.Color.parseColor(
                                            "#42474F"
                                    )
                            );
                        }

                        if (ivStatusIcon != null) {
                            ivStatusIcon.setImageResource(
                                    R.drawable.ic_notifications
                            );
                            ivStatusIcon.setColorFilter(
                                    android.graphics.Color.parseColor(
                                            "#42474F"
                                    )
                            );
                        }
                    }

                    // Abre os detalhes da receita selecionada.
                    itemReceita.setOnClickListener(v -> {

                        Bundle bundle = new Bundle();

                        bundle.putInt(
                                "id_ficha",
                                itemData.idFicha
                        );

                        bundle.putString(
                                "nome_ficha",
                                itemData.nomeFicha
                        );

                        Navigation.findNavController(v)
                                .navigate(
                                        R.id.nav_detalhes_receita,
                                        bundle
                                );
                    });

                    currentContainer.addView(itemReceita);
                }
            });
        });
    }

    /**
     * Modelo dos dados utilizados nos cards de receita.
     */
    private static class ItemReceitaData {

        final int idFicha;
        final String nomeFicha;
        final String concluido;

        ItemReceitaData(
                int idFicha,
                String nomeFicha,
                String concluido
        ) {
            this.idFicha = idFicha;
            this.nomeFicha = nomeFicha;
            this.concluido = concluido;
        }
    }

    /**
     * Retorna os dois primeiros nomes do usuário.
     */
    private String formatarNomeCurto(String nomeCompleto) {

        if (nomeCompleto == null
                || nomeCompleto.trim().isEmpty()) {
            return "";
        }

        String[] partes = nomeCompleto.trim().split("\\s+");

        if (partes.length >= 2) {
            return partes[0] + " " + partes[1];
        }

        return partes[0];
    }

    /**
     * Sincroniza o Spinner ao retornar do calendário.
     */
    @Override
    public void onResume() {
        super.onResume();

        sincronizarSpinnerComTurmaSelecionada();
    }

    private void sincronizarSpinnerComTurmaSelecionada() {

        if (!isAdded()
                || getContext() == null
                || spinnerTurmasHome == null
                || spinnerTurmasHome.getAdapter() == null) {
            return;
        }

        SharedPreferences prefs = requireContext()
                .getSharedPreferences(
                        NOME_PREFERENCIAS,
                        Context.MODE_PRIVATE
                );

        int idTurmaSalva = prefs.getInt(
                CHAVE_TURMA_SELECIONADA,
                -1
        );

        for (int i = 0;
             i < spinnerTurmasHome.getAdapter().getCount();
             i++) {

            Object item =
                    spinnerTurmasHome.getAdapter().getItem(i);

            if (item instanceof TurmaModelo) {

                TurmaModelo turma = (TurmaModelo) item;

                if (turma.getIdTurma() == idTurmaSalva) {

                    if (spinnerTurmasHome.getSelectedItemPosition()
                            != i) {
                        spinnerTurmasHome.setSelection(i);
                    }

                    return;
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (executorService != null
                && !executorService.isShutdown()) {
            executorService.shutdown();
        }

        executorService = null;

        textSaudacao = null;
        textDataHome = null;
        spinnerTurmasHome = null;
    }
}