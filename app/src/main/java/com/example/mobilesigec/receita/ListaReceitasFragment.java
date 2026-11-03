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

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ListaReceitasFragment extends Fragment {

    private GridLayout gridReceitas;
    private AutoCompleteTextView edtPesquisaReceita;
    private TextView txtQuantidadeReceitas;
    private TextView filtroTodas, filtroDisponiveis, filtroDatadas;

    private String statusFiltroAtual = "TODAS";
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

        filtroTodas = view.findViewById(R.id.filtroTodas);
        filtroDisponiveis = view.findViewById(R.id.filtroDisponiveis);
        filtroDatadas = view.findViewById(R.id.filtroDatadas);

        if (filtroTodas != null) {
            filtroTodas.setOnClickListener(v -> selecionarFiltroStatus("TODAS"));
        }
        if (filtroDisponiveis != null) {
            filtroDisponiveis.setOnClickListener(v -> selecionarFiltroStatus("DISPONÍVEL"));
        }
        if (filtroDatadas != null) {
            filtroDatadas.setOnClickListener(v -> selecionarFiltroStatus("DATADA"));
        }

        carregarReceitas();

        edtPesquisaReceita.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                aplicarFiltrosCombinados();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        edtPesquisaReceita.setOnItemClickListener((parent, view1, position, id) -> {
            String selecao = (String) parent.getItemAtPosition(position);
            if (selecao != null) {
                edtPesquisaReceita.setText(selecao);
                edtPesquisaReceita.setSelection(selecao.length());
                aplicarFiltrosCombinados();
            }
        });

        return view;
    }

    // ==================================================
    // CARREGA AS RECEITAS DO BANCO DE DADOS (DB_SIGEC)
    // ==================================================
    private void carregarReceitas() {
        if (getContext() == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences("SessaoApp", Context.MODE_PRIVATE);
        int idUsuarioLogado = prefs.getInt("id_usuario", -1);
        if (idUsuarioLogado == -1) {
            idUsuarioLogado = prefs.getInt("ID_USUARIO", -1);
        }

        final int idUsuario = idUsuarioLogado;

        new Thread(() -> {
            List<Receita> resultado = new ArrayList<>();
            Connection conexao = null;

            try {
                conexao = ConexaoMySQL.conectar();
                if (conexao != null) {
                    // 1. Buscar os IDs de fichas com agendamento ativo e data preenchida
                    Set<Integer> fichasDatadas = new HashSet<>();
                    String sqlAgendamentos = "SELECT DISTINCT id_ficha FROM agendamento WHERE situacao = 'A' AND data IS NOT NULL";
                    try (PreparedStatement stmtAg = conexao.prepareStatement(sqlAgendamentos);
                         ResultSet rsAg = stmtAg.executeQuery()) {
                        while (rsAg.next()) {
                            fichasDatadas.add(rsAg.getInt("id_ficha"));
                        }
                    } catch (Exception ignored) {}

                    // 2. Buscar as fichas técnicas ativas vinculadas às turmas do usuário logado
                    String sql;
                    if (idUsuario != -1) {
                        sql = "SELECT DISTINCT f.id_ficha, f.nome_ficha, f.preparo, t.nome_turma " +
                                "FROM ficha f " +
                                "INNER JOIN turma t ON t.id_turma = f.id_turma " +
                                "INNER JOIN usuario_turma ut ON ut.id_turma = t.id_turma " +
                                "WHERE ut.id_usuario = ? AND f.situacao = 'A' AND t.situacao = 'A' " +
                                "ORDER BY f.nome_ficha ASC";
                    } else {
                        sql = "SELECT DISTINCT f.id_ficha, f.nome_ficha, f.preparo, t.nome_turma " +
                                "FROM ficha f " +
                                "LEFT JOIN turma t ON t.id_turma = f.id_turma " +
                                "WHERE f.situacao = 'A' " +
                                "ORDER BY f.nome_ficha ASC";
                    }

                    try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
                        if (idUsuario != -1) {
                            stmt.setInt(1, idUsuario);
                        }
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                int idFicha = rs.getInt("id_ficha");
                                String nome = rs.getString("nome_ficha");
                                String preparo = rs.getString("preparo");
                                String turma = rs.getString("nome_turma");
                                if (turma == null || turma.trim().isEmpty()) {
                                    turma = "RECEITA GERAL";
                                }

                                String status = fichasDatadas.contains(idFicha) ? "DATADA" : "DISPONÍVEL";

                                resultado.add(new Receita(idFicha, nome, preparo, turma, status));
                            }
                        }
                    }
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        listaReceitas.clear();
                        listaReceitas.addAll(resultado);
                        atualizarSugestoesAutocomplete();
                        aplicarFiltrosCombinados();
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
                    if (conexao != null) conexao.close();
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private String removerAcentos(String text) {
        if (text == null) return "";
        String normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase();
    }

    private int calcularDistanciaLevenshtein(String s1, String s2) {
        if (s1 == null || s2 == null) return Integer.MAX_VALUE;
        String str1 = removerAcentos(s1);
        String str2 = removerAcentos(s2);

        int[] costs = new int[str2.length() + 1];
        for (int j = 0; j <= str2.length(); j++) costs[j] = j;

        for (int i = 1; i <= str1.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= str2.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        str1.charAt(i - 1) == str2.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[str2.length()];
    }

    private boolean ehSimilar(String textoBusca, String textoAlvo) {
        if (textoBusca == null || textoAlvo == null) return false;
        String busca = removerAcentos(textoBusca.trim());
        String alvo = removerAcentos(textoAlvo.trim());

        if (busca.isEmpty()) return true;
        if (alvo.contains(busca)) return true;

        int limiteErros = (busca.length() <= 2) ? 1 : (busca.length() <= 5 ? 2 : 3);

        String[] palavrasAlvo = alvo.split("\\s+");
        for (String palavra : palavrasAlvo) {
            if (calcularDistanciaLevenshtein(busca, palavra) <= limiteErros) {
                return true;
            }
        }

        return calcularDistanciaLevenshtein(busca, alvo) <= limiteErros;
    }

    private void atualizarSugestoesAutocomplete() {
        if (edtPesquisaReceita == null || !isAdded() || getContext() == null) return;

        List<String> sugestoes = new ArrayList<>();
        for (Receita r : listaReceitas) {
            if (r.getNome() != null && !r.getNome().trim().isEmpty()) {
                String nomeTrim = r.getNome().trim();
                boolean jaExiste = false;
                for (String s : sugestoes) {
                    if (s.equalsIgnoreCase(nomeTrim)) {
                        jaExiste = true;
                        break;
                    }
                }
                if (!jaExiste) {
                    sugestoes.add(nomeTrim);
                }
            }
        }

        SugestoesFuzzyAdapter adapter = new SugestoesFuzzyAdapter(
                requireContext(),
                sugestoes
        );
        edtPesquisaReceita.setAdapter(adapter);
    }

    private class SugestoesFuzzyAdapter extends ArrayAdapter<String> {
        private final List<String> todasSugestoes;

        public SugestoesFuzzyAdapter(@NonNull Context context, @NonNull List<String> sugestoes) {
            super(context, R.layout.item_autocomplete_suggestion, new ArrayList<>(sugestoes));
            this.todasSugestoes = new ArrayList<>(sugestoes);
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    if (constraint == null || constraint.toString().trim().isEmpty()) {
                        results.values = todasSugestoes;
                        results.count = todasSugestoes.size();
                    } else {
                        String termo = constraint.toString().trim();
                        List<String> filtradas = new ArrayList<>();

                        for (String sugestao : todasSugestoes) {
                            if (removerAcentos(sugestao).contains(removerAcentos(termo))) {
                                filtradas.add(sugestao);
                            }
                        }

                        for (String sugestao : todasSugestoes) {
                            if (!filtradas.contains(sugestao) && ehSimilar(termo, sugestao)) {
                                filtradas.add(sugestao);
                            }
                        }

                        results.values = filtradas;
                        results.count = filtradas.size();
                    }
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    clear();
                    if (results != null && results.values != null) {
                        @SuppressWarnings("unchecked")
                        List<String> lista = (List<String>) results.values;
                        addAll(lista);
                    }
                    notifyDataSetChanged();
                }
            };
        }
    }

    private void selecionarFiltroStatus(String status) {
        this.statusFiltroAtual = status;

        if (filtroTodas != null) {
            boolean selected = "TODAS".equalsIgnoreCase(status);
            filtroTodas.setBackgroundResource(selected ? R.drawable.bg_filter_button_selected : R.drawable.bg_filter_button);
            filtroTodas.setTextColor(selected ? Color.WHITE : Color.parseColor("#002747"));
        }

        if (filtroDisponiveis != null) {
            boolean selected = "DISPONÍVEL".equalsIgnoreCase(status);
            filtroDisponiveis.setBackgroundResource(selected ? R.drawable.bg_filter_button_selected : R.drawable.bg_filter_button);
            filtroDisponiveis.setTextColor(selected ? Color.WHITE : Color.parseColor("#002747"));
        }

        if (filtroDatadas != null) {
            boolean selected = "DATADA".equalsIgnoreCase(status);
            filtroDatadas.setBackgroundResource(selected ? R.drawable.bg_filter_button_selected : R.drawable.bg_filter_button);
            filtroDatadas.setTextColor(selected ? Color.WHITE : Color.parseColor("#002747"));
        }

        aplicarFiltrosCombinados();
    }

    private void aplicarFiltrosCombinados() {
        String textoBusca = (edtPesquisaReceita != null && edtPesquisaReceita.getText() != null)
                ? edtPesquisaReceita.getText().toString().trim() : "";

        List<Receita> filtradas = new ArrayList<>();

        for (Receita receita : listaReceitas) {
            boolean matchTexto = textoBusca.isEmpty() ||
                    ehSimilar(textoBusca, receita.getNome()) ||
                    ehSimilar(textoBusca, receita.getCategoria());

            boolean matchStatus = "TODAS".equalsIgnoreCase(statusFiltroAtual) ||
                    ("DISPONÍVEL".equalsIgnoreCase(statusFiltroAtual) && "DISPONÍVEL".equalsIgnoreCase(receita.getStatus())) ||
                    ("DATADA".equalsIgnoreCase(statusFiltroAtual) && "DATADA".equalsIgnoreCase(receita.getStatus()));

            if (matchTexto && matchStatus) {
                filtradas.add(receita);
            }
        }

        mostrarReceitas(filtradas);
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

        if (tvNome != null) tvNome.setText(receita.getNome());
        if (tvDetalhes != null) tvDetalhes.setText(receita.getCategoria());

        if (tvStatus != null) {
            if ("DATADA".equalsIgnoreCase(receita.getStatus())) {
                tvStatus.setText("DATADA");
                tvStatus.setTextColor(Color.parseColor("#F59E0B"));
                tvStatus.setBackgroundColor(Color.parseColor("#78350F"));
            } else {
                tvStatus.setText("DISPONÍVEL");
                tvStatus.setTextColor(Color.parseColor("#4ADE80"));
                tvStatus.setBackgroundColor(Color.parseColor("#14532D"));
            }
        }

        cardView.setOnClickListener(v -> abrirDetalhes(receita.getId()));

        gridReceitas.addView(cardView);
    }

    private void abrirDetalhes(int idFicha) {
        Bundle bundle = new Bundle();
        bundle.putInt("id_ficha", idFicha);
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.nav_detalhes_receita, bundle);
    }

    public static class Receita {
        private final int id;
        private final String nome;
        private final String preparo;
        private final String categoria;
        private final String status;

        public Receita(int id, String nome, String preparo, String categoria, String status) {
            this.id = id;
            this.nome = nome;
            this.preparo = preparo;
            this.categoria = categoria;
            this.status = status;
        }

        public int getId() { return id; }
        public String getNome() { return nome; }
        public String getPreparo() { return preparo; }
        public String getCategoria() { return categoria; }
        public String getStatus() { return status; }
    }
}