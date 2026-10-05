/*package com.example.mobilesigec.receita;

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

// SE O ALT+ENTER NÃO FUNCIONAR, DEIXE ESTE IMPORT AQUI:
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

    private void carregarReceitas() {
        new Thread(() -> {
            List<Receita> resultado = new ArrayList<>();
            Connection conexao = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                conexao = ConexaoMySQL.conectar();

                // ATENÇÃO: Adicionei "categoria_receita" no SQL.
                // VOCÊ PRECISA TER ESSA COLUNA NA SUA TABELA "ficha" NO MYSQL.
                // Se a coluna se chamar diferente (ex: "nacionalidade"), mude aqui.
                String sql = "SELECT id_ficha, nome_ficha, preparo, categoria_receita FROM ficha ORDER BY nome_ficha ASC";
                stmt = conexao.prepareStatement(sql);
                rs = stmt.executeQuery();

                while (rs.next()) {
                    int idFicha = rs.getInt("id_ficha");
                    String nome = rs.getString("nome_ficha");
                    String preparo = rs.getString("preparo");

                    // Puxa a categoria do banco de dados
                    String categoria = rs.getString("categoria_receita");

                    // TRAVA DE SEGURANÇA:
                    // Se a categoria vier nula ou vazia do banco (porque ainda não alimentaram),
                    // ele exibe um texto padrão provisório no lugar.
                    if (categoria == null || categoria.trim().isEmpty()) {
                        categoria = "A DEFINIR 🏳️";
                    }

                    resultado.add(new Receita(idFicha, nome, preparo, categoria));
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

        // A SOLUÇÃO DO TAMANHO: Em vez de WRAP_CONTENT, definimos uma altura fixa para TODOS os cards.
        // Assim, mesmo que o nome quebre a linha, o card não estica.
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

        // A SOLUÇÃO DA NACIONALIDADE: Agora ele puxa a variável que veio do banco!
        // Se a receita não tiver categoria no banco, ele mostra "SEM CATEGORIA".
        if(receita.getCategoria() != null) {
            tvDetalhes.setText(receita.getCategoria());
        } else {
            tvDetalhes.setText("SEM CATEGORIA");
        }

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

    // A SOLUÇÃO DO MODELO: Adicionei a variável 'categoria' aqui.
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
}*/

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.mobilesigec.R;

import java.util.ArrayList;
import java.util.List;

public class ListaReceitasFragment extends Fragment {

    private GridLayout gridReceitas;
    private EditText edtPesquisaReceita;
    private TextView txtQuantidadeReceitas;

    // Lista que vai guardar os dados estáticos
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

        // Chama a função que cria os dados estáticos
        carregarReceitasEstaticas();

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
    // DADOS ESTÁTICOS (MOCK) PARA TESTAR O VISUAL
    // ==================================================
    private void carregarReceitasEstaticas() {
        listaReceitas.clear();

        // Criando receitas na mão para testar o Grid e os tamanhos
        listaReceitas.add(new Receita(1, "Risoto de Funghi", "Preparo...", "ITALIANA \uD83C\uDDEE\uD83C\uDDF9"));
        listaReceitas.add(new Receita(2, "Feijoada Completa", "Preparo...", "BRASILEIRA \uD83C\uDDE7\uD83C\uDDF7"));
        listaReceitas.add(new Receita(3, "Tacos de Carnitas", "Preparo...", "MEXICANA \uD83C\uDDF2\uD83C\uDDFD"));
        listaReceitas.add(new Receita(4, "Bolo Ópera Tradicional", "Preparo...", "FRANCESA \uD83C\uDDEB\uD83C\uDDF7"));
        listaReceitas.add(new Receita(5, "Moqueca de Peixe", "Preparo...", "BRASILEIRA \uD83C\uDDE7\uD83C\uDDF7"));
        listaReceitas.add(new Receita(6, "Sushi de Salmão", "Preparo...", "JAPONESA \uD83C\uDDEF\uD83C\uDDF5"));

        mostrarReceitas(listaReceitas);
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

        // Mantém a altura fixa para todos os cards ficarem perfeitamente iguais
        int alturaFixa = (int) (125 * getResources().getDisplayMetrics().density);
        params.height = alturaFixa;

        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);

        // Aplica as margens (calculando os DP para Pixels da tela)
        int margemLateral = (int) (6 * getResources().getDisplayMetrics().density);
        int margemBaixo = (int) (12 * getResources().getDisplayMetrics().density);
        params.setMargins(margemLateral, 0, margemLateral, margemBaixo);

        cardView.setLayoutParams(params);

        TextView tvNome = cardView.findViewById(R.id.tv_nome_receita);
        TextView tvDetalhes = cardView.findViewById(R.id.tv_detalhes_receita);
        TextView tvStatus = cardView.findViewById(R.id.tv_status_text);

        // Alimenta os componentes com os dados da classe estática
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

    // Modelo com a categoria já adicionada
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