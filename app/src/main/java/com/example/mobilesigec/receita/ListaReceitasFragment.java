package com.example.mobilesigec.receita;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.mobilesigec.ConexaoMySQL;
import com.example.mobilesigec.R;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ListaReceitasFragment extends Fragment {

    private GridLayout gridReceitas;
    private EditText edtPesquisaReceita;
    private TextView txtQuantidadeReceitas;

    private final List<Receita> listaReceitas =
            new ArrayList<>();


    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_lista_receitas,
                container,
                false
        );


        // ==========================================
        // TÍTULO
        // ==========================================

        if (getActivity() instanceof AppCompatActivity) {

            AppCompatActivity activity =
                    (AppCompatActivity) getActivity();

            if (activity.getSupportActionBar() != null) {

                activity.getSupportActionBar()
                        .setTitle("Receitas");
            }
        }


        // ==========================================
        // COMPONENTES
        // ==========================================

        gridReceitas =
                view.findViewById(R.id.gridReceitas);

        edtPesquisaReceita =
                view.findViewById(R.id.edtPesquisaReceita);

        txtQuantidadeReceitas =
                view.findViewById(
                        R.id.txtQuantidadeReceitas
                );


        // ==========================================
        // CARREGAR RECEITAS
        // ==========================================

        carregarReceitas();


        // ==========================================
        // PESQUISA
        // ==========================================

        edtPesquisaReceita.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {

                        filtrarReceitas(
                                s.toString()
                        );
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s) {
                    }
                }
        );


        return view;
    }


    // ==================================================
    // CONSULTAR BANCO
    // ==================================================

    private void carregarReceitas() {

        new Thread(() -> {

            List<Receita> resultado =
                    new ArrayList<>();

            Connection conexao = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {

                // USE A SUA CLASSE DE CONEXÃO
                conexao = ConexaoMySQL.conectar();


                String sql =
                        "SELECT id_ficha, nome_ficha, preparo " +
                                "FROM ficha " +
                                "ORDER BY nome_ficha ASC";


                stmt = conexao.prepareStatement(sql);

                rs = stmt.executeQuery();


                while (rs.next()) {

                    int idFicha =
                            rs.getInt("id_ficha");

                    String nome =
                            rs.getString("nome_ficha");

                    String preparo =
                            rs.getString("preparo");


                    resultado.add(
                            new Receita(
                                    idFicha,
                                    nome,
                                    preparo
                            )
                    );
                }


                if (getActivity() != null) {

                    getActivity().runOnUiThread(() -> {

                        listaReceitas.clear();

                        listaReceitas.addAll(
                                resultado
                        );

                        mostrarReceitas(
                                listaReceitas
                        );
                    });
                }


            } catch (Exception e) {

                e.printStackTrace();


                if (getActivity() != null) {

                    getActivity().runOnUiThread(() -> {

                        Toast.makeText(
                                requireContext(),
                                "Erro ao carregar receitas: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                    });
                }


            } finally {

                try {

                    if (rs != null)
                        rs.close();

                    if (stmt != null)
                        stmt.close();

                    if (conexao != null)
                        conexao.close();

                } catch (Exception ignored) {
                }
            }

        }).start();
    }


    // ==================================================
    // MOSTRAR RECEITAS
    // ==================================================

    private void mostrarReceitas(
            List<Receita> receitas) {

        gridReceitas.removeAllViews();


        txtQuantidadeReceitas.setText(
                "RESULTADO DOS FILTROS: "
                        + receitas.size()
                        + " receitas encontradas"
        );


        for (Receita receita : receitas) {

            criarCardReceita(receita);
        }
    }


    // ==================================================
    // CRIAR CARD
    // ==================================================

    private void criarCardReceita(
            Receita receita) {

        CardView card =
                new CardView(requireContext());


        int altura = (int) (
                125 *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );


        GridLayout.LayoutParams params =
                new GridLayout.LayoutParams();


        params.width = 0;

        params.height = altura;

        params.columnSpec =
                GridLayout.spec(
                        GridLayout.UNDEFINED,
                        1f
                );


        params.setMargins(
                6,
                0,
                6,
                12
        );


        card.setLayoutParams(params);

        card.setRadius(10);

        card.setCardElevation(6);

        card.setCardBackgroundColor(
                Color.parseColor("#1E3A8A")
        );


        // ==========================================
        // NOME DA RECEITA
        // ==========================================

        TextView nome =
                new TextView(requireContext());


        nome.setText(
                receita.getNome()
        );

        nome.setTextColor(
                Color.WHITE
        );

        nome.setTextSize(17);

        nome.setGravity(
                Gravity.CENTER
        );

        nome.setPadding(
                12,
                12,
                12,
                12
        );


        card.addView(nome);


        // ==========================================
        // CLIQUE
        // ==========================================

        card.setOnClickListener(v -> {

            abrirDetalhes(
                    receita.getId()
            );

        });


        gridReceitas.addView(card);
    }


    // ==================================================
    // ABRIR DETALHES
    // ==================================================

    private void abrirDetalhes(
            int idFicha) {

        Bundle bundle =
                new Bundle();


        bundle.putInt(
                "id_ficha",
                idFicha
        );


        DetalhesReceitaFragment detalhes =
                new DetalhesReceitaFragment();


        detalhes.setArguments(
                bundle
        );


        getParentFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.nav_host_fragment_content_main,
                        detalhes
                )
                .addToBackStack(null)
                .commit();
    }


    // ==================================================
    // PESQUISA
    // ==================================================

    private void filtrarReceitas(
            String texto) {

        List<Receita> filtradas =
                new ArrayList<>();


        String pesquisa =
                texto.toLowerCase().trim();


        for (Receita receita :
                listaReceitas) {

            if (receita
                    .getNome()
                    .toLowerCase()
                    .contains(pesquisa)) {

                filtradas.add(
                        receita
                );
            }
        }


        mostrarReceitas(
                filtradas
        );
    }


    // ==================================================
    // MODELO DA RECEITA
    // ==================================================

    private static class Receita {

        private final int id;

        private final String nome;

        private final String preparo;


        public Receita(
                int id,
                String nome,
                String preparo) {

            this.id = id;

            this.nome = nome;

            this.preparo = preparo;
        }


        public int getId() {

            return id;
        }


        public String getNome() {

            return nome;
        }


        public String getPreparo() {

            return preparo;
        }
    }
}