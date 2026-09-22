package com.example.mobilesigec.receita;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class DetalhesReceitaFragment extends Fragment {

    private TextView txtNomeReceita;
    private TextView txtPreparoReceita;

    private int idFicha;


    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_detalhes_receita,
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
                        .setTitle("Receita");
            }
        }


        // ==========================================
        // COMPONENTES
        // ==========================================

        txtNomeReceita =
                view.findViewById(
                        R.id.txtNomeReceita
                );


        txtPreparoReceita =
                view.findViewById(
                        R.id.txtPreparoReceita
                );


        // ==========================================
        // RECEBER ID DA LISTA
        // ==========================================

        Bundle bundle =
                getArguments();


        if (bundle != null) {

            idFicha =
                    bundle.getInt(
                            "id_ficha",
                            -1
                    );


            if (idFicha != -1) {

                carregarDetalhesReceita();

            } else {

                Toast.makeText(
                        requireContext(),
                        "Receita não encontrada.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }


        return view;
    }


    // ==================================================
    // BUSCAR DETALHES
    // ==================================================

    private void carregarDetalhesReceita() {

        new Thread(() -> {

            Connection conexao = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;


            try {

                // SUA CONEXÃO EXISTENTE
                conexao = ConexaoMySQL.conectar();


                String sql =
                        "SELECT id_ficha, nome_ficha, preparo " +
                                "FROM ficha " +
                                "WHERE id_ficha = ?";


                stmt =
                        conexao.prepareStatement(
                                sql
                        );


                stmt.setInt(
                        1,
                        idFicha
                );


                rs =
                        stmt.executeQuery();


                if (rs.next()) {

                    String nome =
                            rs.getString(
                                    "nome_ficha"
                            );


                    String preparo =
                            rs.getString(
                                    "preparo"
                            );


                    if (getActivity() != null) {

                        getActivity().runOnUiThread(() -> {

                            txtNomeReceita.setText(
                                    nome
                            );


                            txtPreparoReceita.setText(
                                    preparo
                            );

                        });
                    }

                } else {

                    if (getActivity() != null) {

                        getActivity().runOnUiThread(() -> {

                            Toast.makeText(
                                    requireContext(),
                                    "Receita não encontrada no banco.",
                                    Toast.LENGTH_SHORT
                            ).show();

                        });
                    }
                }


            } catch (Exception e) {

                e.printStackTrace();


                if (getActivity() != null) {

                    getActivity().runOnUiThread(() -> {

                        Toast.makeText(
                                requireContext(),
                                "Erro ao carregar receita: "
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
}