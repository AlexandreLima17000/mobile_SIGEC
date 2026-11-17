package com.example.mobilesigec;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Locale;

public class GerenciadorBloqueioRecuperacao {

    private static final String TAG = "GerenciadorBloqueio";
    private static final String PREFS_NAME = "SegurancaRecuperacaoPrefs";

    // 1 minuto e 30 segundos (90 segundos) de espera para reenviar o código
    public static final long TEMPO_ESPERA_REENVIO_MS = 90 * 1000L;

    // Máximo de 30 reenvios permitidos antes de bloquear
    public static final int MAX_REENVIOS = 30;

    // 10 minutos de bloqueio para login (e-mail e IP)
    public static final long TEMPO_BLOQUEIO_LOGIN_MS = 10 * 60 * 1000L;

    // Janela de expiração de uma sessão de recuperação (60 minutos após o último envio)
    private static final long JANELA_SESSAO_RECUPERACAO_MS = 60 * 60 * 1000L;

    private static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Obtém o endereço IPv4 local do dispositivo.
     */
    public static String obterIpDispositivo() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        String hostAddress = addr.getHostAddress();
                        if (hostAddress != null && !hostAddress.isEmpty()) {
                            return hostAddress;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Não foi possível obter IP da interface de rede", e);
        }
        return "127.0.0.1";
    }

    /**
     * Obtém o IP do cliente visto pelo servidor MySQL, caso haja conexão ativa.
     */
    private static String obterIpBanco(Connection con) {
        if (con == null) return null;
        try (Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT SUBSTRING_INDEX(USER(), '@', -1)")) {
            if (rs.next()) {
                String ip = rs.getString(1);
                if (ip != null && !ip.trim().isEmpty()) {
                    return ip.trim();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static void garantirTabelaBloqueio(Connection con) {
        if (con == null) return;
        String sql = "CREATE TABLE IF NOT EXISTS bloqueio_login (" +
                "id_bloqueio INT AUTO_INCREMENT PRIMARY KEY, " +
                "email VARCHAR(150), " +
                "ip VARCHAR(64), " +
                "bloqueado_ate BIGINT NOT NULL" +
                ")";
        try (Statement stmt = con.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) {
            Log.w(TAG, "Aviso ao garantir tabela bloqueio_login: " + e.getMessage());
        }
    }

    /**
     * Verifica se o e-mail ou o IP atual estão bloqueados para login.
     * Retorna o tempo restante de bloqueio em milissegundos (0 se não estiver bloqueado).
     */
    public static long obterTempoRestanteBloqueioMs(Context context, Connection con, String email) {
        long agora = System.currentTimeMillis();
        long maiorBloqueioAte = 0L;

        String emailNorm = normalizarEmail(email);
        String ipLocal = obterIpDispositivo();

        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            boolean alterou = false;

            if (!emailNorm.isEmpty()) {
                String keyEmail = "bloqueio_email_" + emailNorm;
                long ateEmail = prefs.getLong(keyEmail, 0L);
                if (ateEmail > agora) {
                    maiorBloqueioAte = Math.max(maiorBloqueioAte, ateEmail);
                } else if (ateEmail > 0L) {
                    editor.remove(keyEmail);
                    alterou = true;
                }
            }

            if (ipLocal != null && !ipLocal.isEmpty()) {
                String keyIp = "bloqueio_ip_" + ipLocal;
                long ateIp = prefs.getLong(keyIp, 0L);
                if (ateIp > agora) {
                    maiorBloqueioAte = Math.max(maiorBloqueioAte, ateIp);
                } else if (ateIp > 0L) {
                    editor.remove(keyIp);
                    alterou = true;
                }
            }

            if (alterou) {
                editor.apply();
            }
        }

        if (con != null) {
            try {
                garantirTabelaBloqueio(con);
                String ipBanco = obterIpBanco(con);
                String sql = "SELECT MAX(bloqueado_ate) FROM bloqueio_login " +
                        "WHERE (LOWER(email) = ? OR ip = ? OR ip = ?) AND bloqueado_ate > ?";
                try (PreparedStatement stmt = con.prepareStatement(sql)) {
                    stmt.setString(1, emailNorm);
                    stmt.setString(2, ipLocal);
                    stmt.setString(3, ipBanco != null ? ipBanco : ipLocal);
                    stmt.setLong(4, agora);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            long ateBanco = rs.getLong(1);
                            if (ateBanco > agora) {
                                maiorBloqueioAte = Math.max(maiorBloqueioAte, ateBanco);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Aviso ao consultar bloqueio no banco: " + e.getMessage());
            }
        }

        return maiorBloqueioAte > agora ? (maiorBloqueioAte - agora) : 0L;
    }

    /**
     * Bloqueia o e-mail e o IP do dispositivo por 15 minutos.
     */
    public static void registrarBloqueio(Context context, Connection con, String email) {
        long agora = System.currentTimeMillis();
        long bloqueadoAte = agora + TEMPO_BLOQUEIO_LOGIN_MS;
        String emailNorm = normalizarEmail(email);
        String ipLocal = obterIpDispositivo();

        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            if (!emailNorm.isEmpty()) {
                editor.putLong("bloqueio_email_" + emailNorm, bloqueadoAte);
                editor.remove("reenvios_" + emailNorm);
                editor.remove("ultimo_envio_" + emailNorm);
                editor.remove("codigo_atual_" + emailNorm);
            }
            if (ipLocal != null && !ipLocal.isEmpty()) {
                editor.putLong("bloqueio_ip_" + ipLocal, bloqueadoAte);
            }
            editor.apply();
        }

        if (con != null) {
            try {
                garantirTabelaBloqueio(con);
                String ipBanco = obterIpBanco(con);
                String sql = "INSERT INTO bloqueio_login (email, ip, bloqueado_ate) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = con.prepareStatement(sql)) {
                    stmt.setString(1, emailNorm);
                    stmt.setString(2, ipLocal);
                    stmt.setLong(3, bloqueadoAte);
                    stmt.executeUpdate();
                }
                if (ipBanco != null && !ipBanco.equals(ipLocal)) {
                    try (PreparedStatement stmt = con.prepareStatement(sql)) {
                        stmt.setString(1, emailNorm);
                        stmt.setString(2, ipBanco);
                        stmt.setLong(3, bloqueadoAte);
                        stmt.executeUpdate();
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Aviso ao gravar bloqueio no banco: " + e.getMessage());
            }
        }
    }

    /**
     * Verifica se existe uma sessão ativa de recuperação de senha para o e-mail nos últimos 15 minutos.
     */
    public static boolean possuiSessaoAtiva(Context context, String email) {
        if (context == null) return false;
        String emailNorm = normalizarEmail(email);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long ultimoEnvio = prefs.getLong("ultimo_envio_" + emailNorm, 0L);
        if (ultimoEnvio <= 0L) return false;
        return (System.currentTimeMillis() - ultimoEnvio) < JANELA_SESSAO_RECUPERACAO_MS;
    }

    /**
     * Registra o primeiro envio de código de uma sessão de recuperação.
     */
    public static void registrarEnvioInicial(Context context, String email, String codigo) {
        if (context == null) return;
        String emailNorm = normalizarEmail(email);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putLong("ultimo_envio_" + emailNorm, System.currentTimeMillis())
                .putInt("reenvios_" + emailNorm, 0)
                .putString("codigo_atual_" + emailNorm, codigo)
                .apply();
    }

    /**
     * Registra um reenvio de código, atualiza o timestamp de último envio e incrementa o contador.
     * Retorna o novo total de reenvios realizados.
     */
    public static int registrarReenvio(Context context, String email, String novoCodigo) {
        if (context == null) return 1;
        String emailNorm = normalizarEmail(email);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int atual = obterQuantidadeReenvios(context, email);
        int novoTotal = atual + 1;
        prefs.edit()
                .putLong("ultimo_envio_" + emailNorm, System.currentTimeMillis())
                .putInt("reenvios_" + emailNorm, novoTotal)
                .putString("codigo_atual_" + emailNorm, novoCodigo)
                .apply();
        return novoTotal;
    }

    /**
     * Retorna a quantidade de reenvios já realizados para o e-mail na sessão atual.
     */
    public static int obterQuantidadeReenvios(Context context, String email) {
        if (context == null) return 0;
        String emailNorm = normalizarEmail(email);
        if (!possuiSessaoAtiva(context, emailNorm)) {
            return 0;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt("reenvios_" + emailNorm, 0);
    }

    /**
     * Obtém o código atual salvo para o e-mail na sessão ativa.
     */
    public static String obterCodigoAtual(Context context, String email) {
        if (context == null) return null;
        String emailNorm = normalizarEmail(email);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString("codigo_atual_" + emailNorm, null);
    }

    /**
     * Retorna o tempo restante (em ms) para poder reenviar o código (1:30 após o último envio).
     */
    public static long obterTempoRestanteReenvioMs(Context context, String email) {
        if (context == null) return 0L;
        String emailNorm = normalizarEmail(email);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long ultimoEnvio = prefs.getLong("ultimo_envio_" + emailNorm, 0L);
        if (ultimoEnvio <= 0L) return 0L;

        long decorrido = System.currentTimeMillis() - ultimoEnvio;
        if (decorrido >= TEMPO_ESPERA_REENVIO_MS) {
            return 0L;
        }
        return TEMPO_ESPERA_REENVIO_MS - decorrido;
    }

    /**
     * Limpa o contador de reenvios e estado de recuperação após redefinição de senha concluída.
     */
    public static void limparEstadoRecuperacao(Context context, String email) {
        if (context == null) return;
        String emailNorm = normalizarEmail(email);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove("ultimo_envio_" + emailNorm)
                .remove("reenvios_" + emailNorm)
                .remove("codigo_atual_" + emailNorm)
                .apply();
    }

    /**
     * Formata um tempo em milissegundos no padrão MM:SS (ex: 01:30, 14:59).
     */
    public static String formatarTempoMinutosSegundos(long tempoMs) {
        long totalSegundos = (long) Math.ceil(tempoMs / 1000.0);
        if (totalSegundos < 0) totalSegundos = 0;
        long minutos = totalSegundos / 60;
        long segundos = totalSegundos % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutos, segundos);
    }
}
