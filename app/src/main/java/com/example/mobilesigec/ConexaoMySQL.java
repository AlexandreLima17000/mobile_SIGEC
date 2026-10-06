package com.example.mobilesigec;

import android.os.StrictMode;
import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexaoMySQL {
    private static final String DEFAULT_URL = "jdbc:mysql://10.0.2.2:3306/sigec?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DEFAULT_USUARIO = "root";
    private static final String DEFAULT_SENHA = "";

    private static final String URL = (BuildConfig.DB_URL != null && !BuildConfig.DB_URL.isEmpty())
            ? BuildConfig.DB_URL : DEFAULT_URL;
    private static final String USUARIO = (BuildConfig.DB_USER != null && !BuildConfig.DB_USER.isEmpty())
            ? BuildConfig.DB_USER : DEFAULT_USUARIO;
    private static final String SENHA = (BuildConfig.DB_PASSWORD != null)
            ? BuildConfig.DB_PASSWORD : DEFAULT_SENHA;

    private static String ultimoErro = "";

    public static String getUltimoErro() {
        return ultimoErro;
    }

    public static Connection conectar() {
        ultimoErro = "";
        try {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                StrictMode.ThreadPolicy policy = new
                        StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
            } catch (ClassNotFoundException e) {
                ultimoErro = "Driver MySQL não encontrado: " + e.getMessage();
                Log.e("ConexaoMySQL", ultimoErro, e);
                return null;
            }
            Connection con = DriverManager.getConnection(URL, USUARIO, SENHA);
            Log.d("ConexaoMySQL", "Conectado com sucesso ao MySQL: " + URL);
            return con;
        } catch (SQLException e) {
            ultimoErro = e.getMessage() != null ? e.getMessage() : e.toString();
            Log.e("ConexaoMySQL", "Erro SQL ao conectar no banco (" + URL + "): " + ultimoErro, e);
            System.out.println("Erro ao conectar: " + ultimoErro);
            return null;
        } catch (Exception e) {
            ultimoErro = e.getMessage() != null ? e.getMessage() : e.toString();
            Log.e("ConexaoMySQL", "Erro inesperado ao conectar no banco: " + ultimoErro, e);
            return null;
        }
    }

    public static void fecharConexao(Connection conexao) {
        try {
            if (conexao != null && !conexao.isClosed()) {
                conexao.close();
            }
        } catch (SQLException e) {
            System.out.println("Erro ao fechar conexão: " + e.getMessage());
        }
    }
}
