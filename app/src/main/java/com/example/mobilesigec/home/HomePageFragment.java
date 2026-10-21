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
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

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

    // Evita carregar a mesma turma mais de uma vez
    // quando o Spinner restaura a seleção.
    private int idTurmaReceitasCarregada = Integer.MIN_VALUE;

    public HomePageFragment() {
        // Construtor obrigatório do Fragment.
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

        // A View é recriada quando o Fragment volta a ser exibido.
        idTurmaReceitasCarregada = Integer.MIN_VALUE;

        textSaudacao = view.findViewById(R.id.textSaudacao);
        textDataHome = view.findViewById(R.id.textDataHome);

        CardView cardInsumos =
                view.findViewById(R.id.card_checklist_insumos);

        CardView cardUtensilios =
                view.findViewById(R.id.card_checklist_utensilios);

        CardView cardReceitasHoje =
                view.findViewById(R.id.card_receitas_hoje);

        // Nome do usuário logado.
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
                textSaudacao.setText("Bom dia, " + nome + "!");
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

        // Carrega as turmas disponíveis para o usuário.
        carregarTurmasNoSpinner(view);

        // Navegação para checklist de insumos.
        if (cardInsumos != null) {
            cardInsumos.setOnClickListener(v ->
                    Navigation.findNavController(v)
                            .navigate(R.id.action_home_to_insumos)
            );
        }

        // Navegação para checklist de utensílios.
        if (cardUtensilios != null) {
            cardUtensilios.setOnClickListener(v ->
                    Navigation.findNavController(v)
                            .navigate(R.id.action_home_to_utensilios)
            );
        }

        // Navegação para o calendário.
        if (cardReceitasHoje != null) {
            cardReceitasHoje.setOnClickListener(v -> {

                // Evita abrir o calendário sem uma turma selecionada.
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
                    android.widget.Toast.makeText(
                            contextoAtual,
                            "Selecione uma turma antes de abrir o calendário.",
                            android.widget.Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                Navigation.findNavController(v)
                        .navigate(R.id.nav_calendario);
            });
        }
    }

    /**
     * Carrega do MySQL as turmas disponíveis
     * e preenche o Spinner da HomePage.
     */
    private void carregarTurmasNoSpinner(View view) {

        spinnerTurmasHome =
                view.findViewById(R.id.spinnerTurmasHome);

        if (spinnerTurmasHome == null) {
            return;
        }

        Context context = getContext();

        if (context == null || executorService == null) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(
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

                /*
                 * Se existe um usuário logado, carrega somente
                 * as turmas relacionadas a ele.
                 *
                 * Se o ID do usuário não estiver salvo,
                 * mantém o comportamento anterior do projeto:
                 * carregar todas as turmas ativas.
                 */
                if (idUsuarioLogado != -1) {

                    sql =
                            "SELECT t.id_turma, " +
                                    "t.nome_turma, " +
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
                            "SELECT t.id_turma, " +
                                    "t.nome_turma, " +
                                    "l.nome_laboratorio " +
                                    "FROM turma t " +
                                    "INNER JOIN laboratorio l " +
                                    "ON t.id_laboratorio = l.id_laboratorio " +
                                    "WHERE t.situacao = 'A' " +
                                    "ORDER BY t.nome_turma";
                }

                try (PreparedStatement stmt =
                             con.prepareStatement(sql)) {

                    if (idUsuarioLogado != -1) {
                        stmt.setInt(1, idUsuarioLogado);
                    }

                    try (ResultSet rs = stmt.executeQuery()) {

                        while (rs.next()) {

                            int idTurma =
                                    rs.getInt("id_turma");

                            String nomeTurma =
                                    rs.getString("nome_turma");

                            String nomeLaboratorio =
                                    rs.getString("nome_laboratorio");

                            listaTurmas.add(
                                    new TurmaModelo(
                                            idTurma,
                                            nomeTurma,
                                            nomeLaboratorio
                                    )
                            );
                        }
                    }
                }

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Erro ao carregar turmas do banco de dados.",
                        e
                );

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

                // Caso não existam turmas disponíveis.
                if (listaTurmas.isEmpty()) {

                    preferencias.edit()
                            .remove(CHAVE_TURMA_SELECIONADA)
                            .apply();

                    spinnerTurmasHome.setAdapter(
                            new ArrayAdapter<>(
                                    contextoAtual,
                                    android.R.layout.simple_spinner_item,
                                    new ArrayList<String>()
                            )
                    );

                    LinearLayout containerReceitas =
                            getView().findViewById(
                                    R.id.container_receitas
                            );

                    if (containerReceitas != null) {

                        containerReceitas.removeAllViews();

                        TextView mensagem = new TextView(
                                contextoAtual
                        );

                        mensagem.setText(
                                "Nenhuma turma ativa encontrada."
                        );

                        mensagem.setTextColor(
                                android.graphics.Color.parseColor(
                                        "#64748B"
                                )
                        );

                        mensagem.setTextSize(14);

                        mensagem.setPadding(8, 16, 8, 16);

                        containerReceitas.addView(mensagem);
                    }

                    return;
                }

                // Cria o adaptador do Spinner.
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

                // Recupera a última turma selecionada.
                int idTurmaSalva = preferencias.getInt(
                        CHAVE_TURMA_SELECIONADA,
                        -1
                );

                int posicaoInicial = 0;

                // Procura a turma salva na lista carregada do banco.
                for (int i = 0; i < listaTurmas.size(); i++) {

                    if (listaTurmas.get(i).getIdTurma()
                            == idTurmaSalva) {

                        posicaoInicial = i;
                        break;
                    }
                }

                // Monitora as alterações na seleção do Spinner.
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
                                        (TurmaModelo)
                                                parent.getItemAtPosition(
                                                        position
                                                );

                                if (turmaSelecionada == null) {
                                    return;
                                }

                                salvarTurmaSelecionada(
                                        turmaSelecionada
                                );
                            }

                            @Override
                            public void onNothingSelected(
                                    AdapterView<?> parent
                            ) {
                                // Nenhuma ação necessária.
                            }
                        }
                );

                // Restaura a seleção anterior.
                spinnerTurmasHome.setSelection(
                        posicaoInicial,
                        false
                );

                /*
                 * Processa explicitamente a seleção inicial.
                 * Isso garante o carregamento das receitas
                 * mesmo se o Spinner não disparar o listener.
                 */
                salvarTurmaSelecionada(
                        listaTurmas.get(posicaoInicial)
                );
            });
        });
    }

    /**
     * Salva a turma selecionada e carrega as receitas
     * correspondentes no card da HomePage.
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

        /*
         * IMPORTANTE:
         * O ID fica salvo para que o CalendarioFragment
         * possa utilizá-lo como filtro na consulta SQL.
         */
        SharedPreferences prefs =
                contextoAtual.getSharedPreferences(
                        NOME_PREFERENCIAS,
                        Context.MODE_PRIVATE
                );

        prefs.edit()
                .putInt(
                        CHAVE_TURMA_SELECIONADA,
                        idTurma
                )
                .apply();

        // Evita uma consulta duplicada para a mesma seleção.
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
     * Carrega as fichas da turma selecionada no card
     * "Receitas de Hoje".
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

        // Limpa as receitas da seleção anterior.
        containerReceitas.removeAllViews();

        executorService.execute(() -> {

            List<ItemReceitaData> receitasList =
                    new ArrayList<>();

            try (Connection con = ConexaoMySQL.conectar()) {

                if (con == null) {
                    Log.e(
                            TAG,
                            "Não foi possível conectar ao MySQL."
                    );
                    return;
                }

                String sql =
                        "SELECT f.nome_ficha, " +
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

                            String nomeFicha =
                                    rs.getString("nome_ficha");

                            String concluido =
                                    rs.getString("concluido");

                            receitasList.add(
                                    new ItemReceitaData(
                                            nomeFicha,
                                            concluido
                                    )
                            );
                        }
                    }
                }

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Erro ao carregar receitas da turma.",
                        e
                );
            }

            mainHandler.post(() -> {

                if (!isAdded()
                        || getContext() == null
                        || getView() == null) {
                    return;
                }

                /*
                 * Confere se a seleção continua sendo a mesma.
                 * Evita exibir resultados antigos caso o professor
                 * mude rapidamente de turma.
                 */
                SharedPreferences prefs =
                        requireContext().getSharedPreferences(
                                NOME_PREFERENCIAS,
                                Context.MODE_PRIVATE
                        );

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
                        LayoutInflater.from(getContext());

                if (receitasList.isEmpty()) {

                    TextView mensagem = new TextView(
                            requireContext()
                    );

                    mensagem.setText(
                            "Nenhuma receita encontrada para esta turma."
                    );

                    mensagem.setTextColor(
                            android.graphics.Color.parseColor(
                                    "#FFFFFF"
                            )
                    );

                    mensagem.setTextSize(14);

                    mensagem.setPadding(8, 12, 8, 12);

                    currentContainer.addView(mensagem);

                    return;
                }

                // Monta os cards das receitas.
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

                        // Receita concluída.
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

                        // Receita pendente.
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

                    currentContainer.addView(itemReceita);
                }
            });
        });
    }

    /**
     * Modelo simples usado para exibir cada ficha na HomePage.
     */
    private static class ItemReceitaData {

        final String nomeFicha;
        final String concluido;

        ItemReceitaData(
                String nomeFicha,
                String concluido
        ) {
            this.nomeFicha = nomeFicha;
            this.concluido = concluido;
        }
    }

    // ==================================================
    // FUNÇÃO PARA FORMATAR O NOME (DOIS PRIMEIROS NOMES)
    // ==================================================
    private String formatarNomeCurto(String nomeCompleto) {
        if (nomeCompleto == null || nomeCompleto.trim().isEmpty()) {
            return "";
        }

        // Divide o nome completo em uma lista de palavras usando os espaços
        String[] partes = nomeCompleto.trim().split("\\s+");

        // Se tiver 2 ou mais nomes, junta apenas o primeiro e o segundo
        if (partes.length >= 2) {
            return partes[0] + " " + partes[1];
        } else {
            // Se a pessoa tiver cadastrado só um nome, retorna apenas ele
            return partes[0];
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
