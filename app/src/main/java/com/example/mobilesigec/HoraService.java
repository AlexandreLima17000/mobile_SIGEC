package com.example.mobilesigec;

import java.net.PortUnreachableException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class HoraService {
    public static int getHoraServidor() {
        Connection conn = ConexaoMySQL.conectar();
        if (conn == null) return -1;
        int hora = -1;
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT HOUR(NOW())");

            if (rs.next()) {
                hora = rs.getInt(1);
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.out.println("Erro ao buscar hora: " + e.getMessage());
        } finally {
            // Garante que a conexão sempre seja fechada
            ConexaoMySQL.fecharConexao(conn);
        }

        return hora;
    }

    public static String saudacaoHora() {
        try {
            int hora = getHoraServidor();
            if (hora < 0 || hora > 23) {
                return "Olá!";
            } else {
                if (hora >= 5 && hora < 12) {
                    return "Bom dia!";
                } else if (hora >= 12 && hora < 18) {
                    return "Boa Tarde!";
                } else {
                    return "Boa Noite!";
                }
            }
        } catch (Exception e) {
            System.out.println("Erro na saudação: " + e.getMessage());
            return "Olá!";
        }
    }
}
