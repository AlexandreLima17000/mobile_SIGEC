package com.example.mobilesigec;

import android.os.StrictMode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexaoMySQL {
    private static final String URL = BuildConfig.DB_URL;
    private static final String USUARIO = BuildConfig.DB_USER;
    private static final String SENHA = BuildConfig.DB_PASSWORD;

    public static Connection conectar() {
        try {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                StrictMode.ThreadPolicy policy = new
                        StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            return DriverManager.getConnection(URL, USUARIO, SENHA);
        } catch (SQLException e) {
            System.out.println("Erro ao conectar: " + e.getMessage());
            return null;

        }
    }

    public static void fecharConexao(Connection conexao) {
        try {
            if (conexao != null) {
                conexao.close();
            }
        } catch (SQLException e) {
            System.out.println("Erro ao fechar conexão: " + e.getMessage());
        }
    }

    public static String horaBanco(Connection conexao) {
        if (conexao == null) return null;

        String hora = null;
        try {
            Statement stmt = conexao.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT NOW()");

            if (rs.next()) {
                hora = rs.getString(1); // Retorna a data/hora completa (ex: 2026-09-29 11:55:00)
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.out.println("Erro ao buscar hora: " + e.getMessage());
        }
        return hora;
    }
}
