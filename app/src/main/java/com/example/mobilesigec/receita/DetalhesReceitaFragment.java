package com.example.mobilesigec.receita;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.mobilesigec.ConexaoMySQL;
import com.example.mobilesigec.R;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class DetalhesReceitaFragment extends Fragment {

    private TextView txtCategoriaReceita, txtNomeReceita;
    private LinearLayout containerIngredientes, containerUtensilios, containerPassos;
    private int idFicha;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detalhes_receita, container, false);

        // Título da Toolbar
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle("Receitas");
            }
        }

        // Mapeamento dos Componentes do XML
        txtCategoriaReceita = view.findViewById(R.id.txtCategoriaReceita);
        txtNomeReceita = view.findViewById(R.id.txtNomeReceita);
        containerIngredientes = view.findViewById(R.id.containerIngredientes);
        containerUtensilios = view.findViewById(R.id.containerUtensilios);
        containerPassos = view.findViewById(R.id.containerPassos);

        Bundle bundle = getArguments();
        if (bundle != null) {
            idFicha = bundle.getInt("id_ficha", -1);
            if (idFicha != -1) {
                carregarDetalhesBanco();
            } else {
                Toast.makeText(requireContext(), "Receita não encontrada.", Toast.LENGTH_SHORT).show();
            }
        }

        return view;
    }

    private void carregarDetalhesBanco() {
        new Thread(() -> {
            Connection conexao = null;
            try {
                conexao = ConexaoMySQL.conectar();

                // 1. BUSCAR DADOS DA FICHA
                String sqlFicha = "SELECT f.nome_ficha, f.preparo " +
                        "FROM ficha f " +
                        "WHERE f.id_ficha = ?";
                PreparedStatement stmtFicha = conexao.prepareStatement(sqlFicha);
                stmtFicha.setInt(1, idFicha);
                ResultSet rsFicha = stmtFicha.executeQuery();

                String nome = "", preparo = "", categoria = "GERAL";
                if (rsFicha.next()) {
                    nome = rsFicha.getString("nome_ficha");
                    preparo = rsFicha.getString("preparo");
                }
                rsFicha.close();
                stmtFicha.close();

                // 2. BUSCAR INGREDIENTES (INSUMOS)
                List<String> listaIngredientes = new ArrayList<>();
                String sqlIngredientes = "SELECT p.nome_produto, i.quantidade, p.unidade " +
                        "FROM insumo i JOIN produto p ON i.id_produto = p.id_produto " +
                        "WHERE i.id_ficha = ? AND i.cancelado = 'N'";
                PreparedStatement stmtIngr = conexao.prepareStatement(sqlIngredientes);
                stmtIngr.setInt(1, idFicha);
                ResultSet rsIngr = stmtIngr.executeQuery();
                while (rsIngr.next()) {
                    double qtd = rsIngr.getDouble("quantidade");
                    String unidade = rsIngr.getString("unidade");
                    String produto = rsIngr.getString("nome_produto");

                    // Remove zeros desnecessários (ex: 500.000 -> 500)
                    String qtdFormatada = (qtd % 1 == 0) ? String.format("%.0f", qtd) : String.format("%.2f", qtd);
                    listaIngredientes.add(qtdFormatada + unidade + " de " + produto);
                }
                rsIngr.close();
                stmtIngr.close();

                // 3. BUSCAR UTENSÍLIOS (CHECKLIST)
                List<String> listaUtensilios = new ArrayList<>();
                String sqlUtensilios = "SELECT u.nome_utensilio FROM checklist_utensilho c " +
                        "JOIN utensilio u ON c.id_utensilio = u.id_utensilio " +
                        "WHERE c.id_ficha = ? GROUP BY u.nome_utensilio";
                PreparedStatement stmtUten = conexao.prepareStatement(sqlUtensilios);
                stmtUten.setInt(1, idFicha);
                ResultSet rsUten = stmtUten.executeQuery();
                while (rsUten.next()) {
                    listaUtensilios.add(rsUten.getString("nome_utensilio"));
                }
                rsUten.close();
                stmtUten.close();

                // Variáveis finais para acessar na UI Thread
                final String fNome = nome;
                final String fPreparo = preparo;
                final String fCategoria = categoria;

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        txtNomeReceita.setText(fNome);
                        txtCategoriaReceita.setText(fCategoria.toUpperCase());

                        // Desenha as caixas na tela dinamicamente
                        desenharCaixasAzuis(containerIngredientes, listaIngredientes, "Nenhum ingrediente registrado.");
                        desenharCaixasAzuis(containerUtensilios, listaUtensilios, "Nenhum utensílio registrado.");
                        desenharPassos(containerPassos, fPreparo);
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            } finally {
                try { if (conexao != null) conexao.close(); } catch (Exception ignored) {}
            }
        }).start();
    }

    // ==================================================
    // MÉTODOS PARA DESENHAR O DESIGN DO CANVA VIA CÓDIGO
    // ==================================================

    // Cria as caixinhas brancas com borda azul (Ingredientes e Utensílios)
    private void desenharCaixasAzuis(LinearLayout container, List<String> itens, String msgVazio) {
        container.removeAllViews();
        if (itens.isEmpty()) {
            itens.add(msgVazio);
        }

        for (String item : itens) {
            TextView txtItem = new TextView(getContext());
            txtItem.setText(item);
            txtItem.setTextColor(Color.parseColor("#054584")); // Azul Escuro do Senac
            txtItem.setTextSize(14f);
            txtItem.setTypeface(null, Typeface.BOLD);
            txtItem.setGravity(Gravity.CENTER);
            txtItem.setPadding(16, 24, 16, 24);

            // Borda azul, fundo branco e cantos arredondados
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setColor(Color.WHITE);
            shape.setCornerRadius(12f);
            shape.setStroke(3, Color.parseColor("#054584"));
            txtItem.setBackground(shape);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 16);
            txtItem.setLayoutParams(params);

            container.addView(txtItem);
        }
    }

    // Cria as caixas azuis escuras numeradas do Modo de Preparo
    private void desenharPassos(LinearLayout container, String preparoCompleto) {
        container.removeAllViews();

        if (preparoCompleto == null || preparoCompleto.trim().isEmpty()) {
            return;
        }

        // O banco tem um texto corrido. Dividimos pelos pontos finais para simular "Passos"
        String[] passos = preparoCompleto.split("\\.");
        int numeroPasso = 1;

        for (String passo : passos) {
            if (passo.trim().length() > 3) { // Ignora espaços vazios

                LinearLayout row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(32, 24, 32, 24);

                // Fundo Azul Escuro do cartão
                GradientDrawable shape = new GradientDrawable();
                shape.setShape(GradientDrawable.RECTANGLE);
                shape.setColor(Color.parseColor("#0B2647"));
                shape.setCornerRadius(16f);
                row.setBackground(shape);

                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.setMargins(0, 0, 0, 16);
                row.setLayoutParams(rowParams);

                // Número Grande na Esquerda
                TextView txtNumero = new TextView(getContext());
                txtNumero.setText(String.valueOf(numeroPasso));
                txtNumero.setTextColor(Color.WHITE);
                txtNumero.setTextSize(36f);
                txtNumero.setTypeface(null, Typeface.BOLD);
                txtNumero.setPadding(0, 0, 32, 0);

                // Texto da instrução na Direita
                TextView txtInstrucao = new TextView(getContext());
                txtInstrucao.setText(passo.trim() + ".");
                txtInstrucao.setTextColor(Color.parseColor("#E2E8F0")); // Cinza bem clarinho
                txtInstrucao.setTextSize(14f);
                txtInstrucao.setLineSpacing(4f, 1f);

                // Configura o peso para o texto ocupar o resto do espaço
                LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                txtInstrucao.setLayoutParams(textParams);

                row.addView(txtNumero);
                row.addView(txtInstrucao);
                container.addView(row);

                numeroPasso++;
            }
        }
    }
}