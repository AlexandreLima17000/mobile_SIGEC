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

public class HomePageFragment extends Fragment {

    TextView textSaudacao, textDataHome;
    Spinner spinnerTurmasHome;

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

        textSaudacao = view.findViewById(R.id.textSaudacao);
        textDataHome = view.findViewById(R.id.textDataHome);
        CardView cardInsumos = view.findViewById(R.id.card_checklist_insumos);
        CardView cardUtensilios = view.findViewById(R.id.card_checklist_utensilios);

        // Acessa o SharedPreferences para pegar o nome
        SharedPreferences prefs = requireActivity().getSharedPreferences("SessaoApp", Context.MODE_PRIVATE);
        String nome = prefs.getString("NOME_USUARIO", "Instrutor");

        // Atualiza o texto na tela
        textSaudacao.setText("Bom dia, " + nome + "!");

        // Injetar a data atual na Home Page
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd 'DE' MMMM", new Locale("pt", "BR"));
        String dataFormatada = sdf.format(new Date()).toUpperCase();
        textDataHome.setText(dataFormatada);

        carregarTurmasNoSpinner(view);

        cardInsumos.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_home_to_insumos));
        cardUtensilios.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_home_to_utensilios));
    }

    private void carregarTurmasNoSpinner(View view) {
        spinnerTurmasHome = view.findViewById(R.id.spinnerTurmasHome);
        List<TurmaModelo> listaTurmas = new ArrayList<>();

        // Resgata o ID do professor que logou
        SharedPreferences prefs = requireActivity().getSharedPreferences("SessaoApp", Context.MODE_PRIVATE);
        int idUsuarioLogado = prefs.getInt("ID_USUARIO", -1);

        if (idUsuarioLogado == -1) {
            Toast.makeText(getContext(), "Erro de sessão do usuário.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Conecta no banco e busca as turmas
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

                // Monta a lista
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
            Toast.makeText(getContext(), "Erro ao carregar turmas", Toast.LENGTH_SHORT).show();
        }

        // Injeta a lista no Spinner usando o ArrayAdapter
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
    } // <--- ESTA ERA A CHAVE FALTANDO QUE CAUSAVA O ERRO!

    private void carregarReceitasDaTurma(int idTurma, String nomeLaboratorio) {
        // Encontra o container vazio na tela
        android.widget.LinearLayout containerReceitas = requireView().findViewById(R.id.container_receitas);
        // Limpa o container (para apagar as receitas da turma anterior, se houver)
        containerReceitas.removeAllViews();

        android.view.LayoutInflater inflater = android.view.LayoutInflater.from(getContext());

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

                // Para cada receita encontrada no banco, criamos um item na tela
                while (rs.next()) {
                    String nomeFicha = rs.getString("nome_ficha");
                    String concluido = rs.getString("concluido"); // 'S' ou 'N'

                    // Pega o nosso "molde" XML
                    View itemReceita = inflater.inflate(R.layout.item_receita, containerReceitas, false);

                    // Encontra os campos dentro do molde
                    android.widget.TextView tvNome = itemReceita.findViewById(R.id.tv_nome_receita);
                    android.widget.TextView tvDetalhes = itemReceita.findViewById(R.id.tv_detalhes_receita);
                    android.widget.TextView tvStatusText = itemReceita.findViewById(R.id.tv_status_text);
                    android.widget.ImageView ivStatusIcon = itemReceita.findViewById(R.id.iv_status_icon);

                    // Preenche os dados
                    tvNome.setText(nomeFicha);
                    tvDetalhes.setText(nomeLaboratorio);

                    // Configura as cores e ícones baseados no status do banco (S ou N)
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

                    // Finalmente, injeta o item pronto dentro da tela!
                    containerReceitas.addView(itemReceita);
                }
                rs.close();
                stmt.close();
                con.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}