package com.example.mobilesigec.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomePageFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomePageFragment extends Fragment {

    TextView textSaudacao, textDataHome;
    Spinner spinnerTurmasHome;
    private ExecutorService databaseExecutor;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public HomePageFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomePageFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomePageFragment newInstance(String param1, String param2) {
        HomePageFragment fragment = new HomePageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        databaseExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
       View  view = inflater.inflate(R.layout.fragment_home_page, container, false);
       return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView textSaudacao = view.findViewById(R.id.textSaudacao);
        TextView textDataHome = view.findViewById(R.id.textDataHome);
        CardView cardInsumos = view.findViewById(R.id.card_checklist_insumos);
        CardView cardUtensilios = view.findViewById(R.id.card_checklist_utensilios);

        //  Acessa o SharedPreferences para pegar o nome
        SharedPreferences prefs = requireActivity().getSharedPreferences("SessaoApp", Context.MODE_PRIVATE);
        // O segundo parâmetro é o valor padrão caso não encontre (ex: se for direto para a Home sem logar)
        String nome = prefs.getString("NOME_USUARIO", "Instrutor");

        // 3. Atualiza o texto na tela
        textSaudacao.setText("Bom dia, " + nome + "!");



        //-----------------------Injetar a data atua na Home Page----------------------------------


        //  Cria o formatador com o padrão desejado e o idioma (Português do Brasil)
        // EEEE = Dia da semana por extenso, dd = dia do mês, MMMM = Mês por extenso
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd 'DE' MMMM", new Locale("pt", "BR"));

        //  Pega a data de hoje, formata e transforma tudo em maiúsculo
        String dataFormatada = sdf.format(new Date()).toUpperCase();

        //  Injeta o texto na tela
        textDataHome.setText(dataFormatada);

        carregarTurmasNoSpinner(view);

        cardInsumos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(R.id.action_home_to_insumos);
            }
        });

        cardUtensilios.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(R.id.action_home_to_utensilios);
            }
        });
    }



    private void carregarTurmasNoSpinner(View view) {
        spinnerTurmasHome = view.findViewById(R.id.spinnerTurmasHome);
        
        //  Resgata o ID do professor que logou
        SharedPreferences prefs = requireActivity().getSharedPreferences("SessaoApp", Context.MODE_PRIVATE);
        final int idUsuarioLogado = prefs.getInt("ID_USUARIO", -1);

        if (idUsuarioLogado == -1) {
            Toast.makeText(getContext(), "Erro de sessão do usuário.", Toast.LENGTH_SHORT).show();
            return;
        }

        databaseExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final List<TurmaModelo> listaTurmas = new ArrayList<>();
                //  Conecta no banco e busca as turmas
                try {
                    Connection con = ConexaoMySQL.conectar();
                    if (con != null) {
                        // INNER JOIN para pegar Nome da Turma e Nome do Laboratório onde o professor dá aula
                        String sql = "SELECT t.id_turma, t.nome_turma, l.nome_laboratorio " +
                                "FROM turma t " +
                                "INNER JOIN usuario_turma ut ON t.id_turma = ut.id_turma " +
                                "INNER JOIN laboratorio l ON t.id_laboratorio = l.id_laboratorio " +
                                "WHERE ut.id_usuario = ? AND t.situacao = 'A'";

                        PreparedStatement stmt = con.prepareStatement(sql);
                        stmt.setInt(1, idUsuarioLogado);
                        ResultSet rs = stmt.executeQuery();

                        //  Monta a lista
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
                    e.printStackTrace();
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getContext(), "Erro ao carregar turmas", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                // Injeta a lista no Spinner na Main Thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (getContext() != null) {
                                ArrayAdapter<TurmaModelo> adapter = new ArrayAdapter<>(
                                        getContext(),
                                        android.R.layout.simple_spinner_item,
                                        listaTurmas
                                );
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                spinnerTurmasHome.setAdapter(adapter);
                                spinnerTurmasHome.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                                    @Override
                                    public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                                        TurmaModelo turmaSelecionada = (TurmaModelo) parent.getItemAtPosition(position);
                                        // Chama a função que busca as receitas passando o ID da turma
                                        carregarReceitasDaTurma(turmaSelecionada.getIdTurma(), turmaSelecionada.getNomeLaboratorio());
                                    }

                                    @Override
                                    public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                                });
                            }
                        }
                    });
                }
            }
        });
    }

    private void carregarReceitasDaTurma(final int idTurma, final String nomeLaboratorio) {
        final android.widget.LinearLayout containerReceitas = requireView().findViewById(R.id.container_receitas);
        containerReceitas.removeAllViews();
        final LayoutInflater inflater = LayoutInflater.from(getContext());

        databaseExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final List<View> viewsReceitas = new ArrayList<>();
                try {
                    Connection con = ConexaoMySQL.conectar();
                    if (con != null) {
                        // Busca a ficha técnica e o status no agendamento
                        String sql = "SELECT f.nome_ficha, a.concluido " +
                                "FROM ficha f " +
                                "INNER JOIN agendamento a ON f.id_ficha = a.id_ficha " +
                                "WHERE f.id_turma = ?";

                        PreparedStatement stmt = con.prepareStatement(sql);
                        stmt.setInt(1, idTurma);
                        ResultSet rs = stmt.executeQuery();

                        // Para cada receita encontrada no banco, criamos um item estruturado
                        while (rs.next()) {
                            final String nomeFicha = rs.getString("nome_ficha");
                            final String concluido = rs.getString("concluido"); // 'S' ou 'N'

                            if (getActivity() != null) {
                                // Criar a View temporária para preencher em background (inflater seguro)
                                View itemReceita = inflater.inflate(R.layout.item_receita, containerReceitas, false);

                                android.widget.TextView tvNome = itemReceita.findViewById(R.id.tv_nome_receita);
                                android.widget.TextView tvDetalhes = itemReceita.findViewById(R.id.tv_detalhes_receita);
                                android.widget.TextView tvStatusText = itemReceita.findViewById(R.id.tv_status_text);
                                android.widget.ImageView ivStatusIcon = itemReceita.findViewById(R.id.iv_status_icon);

                                tvNome.setText(nomeFicha);
                                tvDetalhes.setText(nomeLaboratorio);

                                if ("S".equals(concluido)) {
                                    tvStatusText.setText("CONCLUÍDO");
                                    tvStatusText.setTextColor(android.graphics.Color.parseColor("#15803d")); // Verde
                                    ivStatusIcon.setImageResource(R.drawable.ic_check_circle);
                                    ivStatusIcon.setColorFilter(android.graphics.Color.parseColor("#15803d"));
                                } else {
                                    tvStatusText.setText("PENDENTE");
                                    tvStatusText.setTextColor(android.graphics.Color.parseColor("#42474f")); // Cinza
                                    ivStatusIcon.setImageResource(R.drawable.ic_notifications);
                                    ivStatusIcon.setColorFilter(android.graphics.Color.parseColor("#42474f"));
                                }
                                viewsReceitas.add(itemReceita);
                            }
                        }

                        rs.close();
                        stmt.close();
                        con.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Injeta todas as views prontas no container pela UI Thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            for (View item : viewsReceitas) {
                                containerReceitas.addView(item);
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (databaseExecutor != null) {
            databaseExecutor.shutdown();
        }
    }
}