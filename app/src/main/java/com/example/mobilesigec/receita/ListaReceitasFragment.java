package com.example.mobilesigec.receita;

import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
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
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

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

    private final List<Receita> listaReceitas = new ArrayList<>();

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

        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle("Receitas");
            }
        }

        gridReceitas = view.findViewById(R.id.gridReceitas);
        edtPesquisaReceita = view.findViewById(R.id.edtPesquisaReceita);
        txtQuantidadeReceitas = view.findViewById(R.id.txtQuantidadeReceitas);

        carregarReceitas();

        edtPesquisaReceita.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarReceitas(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    // ==================================================
    // CARREGA AS RECEITAS DO BANCO DE DADOS (DB_SIGEC)
    // ==================================================
    private void carregarReceitas() {
        new Thread(() -> {
            List<Receita> resultado = new ArrayList<>();
            Connection conexao = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                conexao = ConexaoMySQL.conectar();

                // SQL corrigido: fichas são globais, não possuem id_turma na tabela ficha
                String sql = "SELECT f.id_ficha, f.nome_ficha, f.preparo " +
                        "FROM ficha f " +
                        "WHERE f.situacao = 'A' " +
                        "ORDER BY f.nome_ficha ASC";

                stmt = conexao.prepareStatement(sql);
                rs = stmt.executeQuery();

                while (rs.next()) {
                    int idFicha = rs.getInt("id_ficha");
                    String nome = rs.getString("nome_ficha");
                    String preparo = rs.getString("preparo");
                    String turma = "RECEITA GERAL";

                    resultado.add(new Receita(idFicha, nome, preparo, turma));
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        listaReceitas.clear();
                        listaReceitas.addAll(resultado);
                        mostrarReceitas(listaReceitas);
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Erro ao carregar receitas: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (stmt != null) stmt.close();
                    if (conexao != null) conexao.close();
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private void mostrarReceitas(List<Receita> receitas) {
        gridReceitas.removeAllViews();

        String textoQuantidade = "RESULTADO DOS FILTROS: <font color='#F59E0B'>"
                + receitas.size() + " receitas encontradas</font>";
        txtQuantidadeReceitas.setText(Html.fromHtml(textoQuantidade, Html.FROM_HTML_MODE_LEGACY));

        for (Receita receita : receitas) {
            criarCardReceita(receita);
        }
    }

    private void criarCardReceita(Receita receita) {
        View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.item_receita, gridReceitas, false);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;

        int alturaFixa = (int) (125 * getResources().getDisplayMetrics().density);
        params.height = alturaFixa;

        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);

        int margemLateral = (int) (6 * getResources().getDisplayMetrics().density);
        int margemBaixo = (int) (12 * getResources().getDisplayMetrics().density);
        params.setMargins(margemLateral, 0, margemLateral, margemBaixo);

        cardView.setLayoutParams(params);

        TextView tvNome = cardView.findViewById(R.id.tv_nome_receita);
        TextView tvDetalhes = cardView.findViewById(R.id.tv_detalhes_receita);
        TextView tvStatus = cardView.findViewById(R.id.tv_status_text);

        tvNome.setText(receita.getNome());
        tvDetalhes.setText(receita.getCategoria());
        tvStatus.setText("DISPONÍVEL");

        cardView.setOnClickListener(v -> abrirDetalhes(receita.getId()));

        gridReceitas.addView(cardView);
    }

    private void abrirDetalhes(int idFicha) {
        Bundle bundle = new Bundle();
        bundle.putInt("id_ficha", idFicha);
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.nav_detalhes_receita, bundle);
    }

    private void filtrarReceitas(String texto) {
        List<Receita> filtradas = new ArrayList<>();
        String pesquisa = texto.toLowerCase().trim();

        for (Receita receita : listaReceitas) {
            if (receita.getNome().toLowerCase().contains(pesquisa)) {
                filtradas.add(receita);
            }
        }
        mostrarReceitas(filtradas);
    }

    public static class Receita {
        private final int id;
        private final String nome;
        private final String preparo;
        private final String categoria;

        public Receita(int id, String nome, String preparo, String categoria) {
            this.id = id;
            this.nome = nome;
            this.preparo = preparo;
            this.categoria = categoria;
        }

        public int getId() { return id; }
        public String getNome() { return nome; }
        public String getPreparo() { return preparo; }
        public String getCategoria() { return categoria; }
    }
}