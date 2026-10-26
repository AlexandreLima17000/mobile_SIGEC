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

    private TextView textSaudacao, textDataHome;
    private Spinner spinnerTurmasHome;

    private ExecutorService executorService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public HomePageFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        executorService = Executors.newSingleThreadExecutor();

        textSaudacao = view.findViewById(R.id.textSaudacao);
        textDataHome = view.findViewById(R.id.textDataHome);
        CardView cardInsumos = view.findViewById(R.id.card_checklist_insumos);
        CardView cardUtensilios = view.findViewById(R.id.card_checklist_utensilios);
        CardView cardReceitasHoje = view.findViewById(R.id.card_receitas_hoje);

        Context context = getContext();
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences("SessaoApp", Context.MODE_PRIVATE);
            String nomeCompleto = prefs.getString("nome_usuario", "Instrutor");

            // Aplica a formatação para pegar apenas os dois primeiros nomes
            String nomeFormatado = formatarNomeCurto(nomeCompleto);

            if (textSaudacao != null) {
                textSaudacao.setText("Bom dia, " + nomeFormatado + "!");
            }
        }

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd 'DE' MMMM", Locale.forLanguageTag("pt-BR"));
        String dataFormatada = sdf.format(new Date()).toUpperCase();
        if (textDataHome != null) {
            textDataHome.setText(dataFormatada);
        }

        carregarTurmasNoSpinner(view);

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

    private void carregarTurmasNoSpinner(View view) {
        spinnerTurmasHome = view.findViewById(R.id.spinnerTurmasHome);
        if (spinnerTurmasHome == null) return;

        Context context = getContext();
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences("SessaoApp", Context.MODE_PRIVATE);
        int idUsuarioLogado = prefs.getInt("id_usuario", -1);

        executorService.execute(() -> {
            List<TurmaModelo> listaTurmas = new ArrayList<>();
            try {
                Connection con = ConexaoMySQL.conectar();
                if (con != null) {
                    String sql;
                    PreparedStatement stmt;
                    if (idUsuarioLogado != -1) {
                        sql = "SELECT t.id_turma, t.nome_turma, l.nome_laboratorio " +
                                "FROM turma t " +
                                "INNER JOIN usuario_turma ut ON t.id_turma = ut.id_turma " +
                                "INNER JOIN laboratorio l ON t.id_laboratorio = l.id_laboratorio " +
                                "WHERE ut.id_usuario = ? AND t.situacao = 'A'";
                        stmt = con.prepareStatement(sql);
                        stmt.setInt(1, idUsuarioLogado);
                    } else {
                        sql = "SELECT t.id_turma, t.nome_turma, l.nome_laboratorio " +
                                "FROM turma t " +
                                "INNER JOIN laboratorio l ON t.id_laboratorio = l.id_laboratorio " +
                                "WHERE t.situacao = 'A'";
                        stmt = con.prepareStatement(sql);
                    }

                    ResultSet rs = stmt.executeQuery();

                    while (rs.next()) {
                        int idTurma = rs.getInt("id_turma");
                        String nomeTurma = rs.getString("nome_turma");
                        String nomeLab = rs.getString("nome_laboratorio");
                        listaTurmas.add(new TurmaModelo(idTurma, nomeTurma, nomeLab));
                    }
                    rs.close();
                    stmt.close();
                    con.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao carregar turmas", e);
            }

            mainHandler.post(() -> {
                if (!isAdded() || getContext() == null) return;

                ArrayAdapter<TurmaModelo> adapter = new ArrayAdapter<>(
                        getContext(),
                        android.R.layout.simple_spinner_item,
                        listaTurmas
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerTurmasHome.setAdapter(adapter);

                spinnerTurmasHome.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(android.widget.AdapterView<?> parent, View selectedView, int position, long id) {
                        TurmaModelo turmaSelecionada = (TurmaModelo) parent.getItemAtPosition(position);
                        if (turmaSelecionada != null) {
                            carregarReceitasDaTurma(turmaSelecionada.getIdTurma(), turmaSelecionada.getNomeLaboratorio());
                        }
                    }

                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                });
            });
        });
    }

    private void carregarReceitasDaTurma(int idTurma, String nomeLaboratorio) {
        View view = getView();
        if (view == null) return;

        LinearLayout containerReceitas = view.findViewById(R.id.container_receitas);
        if (containerReceitas == null) return;

        containerReceitas.removeAllViews();

        executorService.execute(() -> {
            List<ItemReceitaData> receitasList = new ArrayList<>();
            try {
                Connection con = ConexaoMySQL.conectar();
                if (con != null) {
                    String sql = "SELECT f.nome_ficha, COALESCE(a.concluido, 'N') AS concluido " +
                            "FROM ficha f " +
                            "LEFT JOIN agendamento a ON f.id_ficha = a.id_ficha " +
                            "WHERE f.id_turma = ?";

                    PreparedStatement stmt = con.prepareStatement(sql);
                    stmt.setInt(1, idTurma);
                    ResultSet rs = stmt.executeQuery();

                    while (rs.next()) {
                        String nomeFicha = rs.getString("nome_ficha");
                        String concluido = rs.getString("concluido");
                        receitasList.add(new ItemReceitaData(nomeFicha, concluido));
                    }
                    rs.close();
                    stmt.close();
                    con.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao carregar receitas da turma", e);
            }

            mainHandler.post(() -> {
                if (!isAdded() || getContext() == null) return;
                View currentView = getView();
                if (currentView == null) return;

                LinearLayout currentContainer = currentView.findViewById(R.id.container_receitas);
                if (currentContainer == null) return;

                currentContainer.removeAllViews();
                LayoutInflater inflater = LayoutInflater.from(getContext());

                for (ItemReceitaData itemData : receitasList) {
                    View itemReceita = inflater.inflate(R.layout.item_receita, currentContainer, false);

                    TextView tvNome = itemReceita.findViewById(R.id.tv_nome_receita);
                    TextView tvDetalhes = itemReceita.findViewById(R.id.tv_detalhes_receita);
                    TextView tvStatusText = itemReceita.findViewById(R.id.tv_status_text);
                    ImageView ivStatusIcon = itemReceita.findViewById(R.id.iv_status_icon);

                    if (tvNome != null) tvNome.setText(itemData.nomeFicha);
                    if (tvDetalhes != null) tvDetalhes.setText(nomeLaboratorio);

                    if ("S".equals(itemData.concluido)) {
                        if (tvStatusText != null) {
                            tvStatusText.setText("CONCLUÍDO");
                            tvStatusText.setTextColor(android.graphics.Color.parseColor("#15803d"));
                        }
                        if (ivStatusIcon != null) {
                            ivStatusIcon.setImageResource(R.drawable.ic_check_circle);
                            ivStatusIcon.setColorFilter(android.graphics.Color.parseColor("#15803d"));
                        }
                    } else {
                        if (tvStatusText != null) {
                            tvStatusText.setText("PENDENTE");
                            tvStatusText.setTextColor(android.graphics.Color.parseColor("#42474f"));
                        }
                        if (ivStatusIcon != null) {
                            ivStatusIcon.setImageResource(R.drawable.ic_notifications);
                            ivStatusIcon.setColorFilter(android.graphics.Color.parseColor("#42474f"));
                        }
                    }

                    currentContainer.addView(itemReceita);
                }
            });
        });
    }

    private static class ItemReceitaData {
        final String nomeFicha;
        final String concluido;

        ItemReceitaData(String nomeFicha, String concluido) {
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
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}