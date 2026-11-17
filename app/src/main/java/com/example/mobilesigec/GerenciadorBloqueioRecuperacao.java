package com.example.mobilesigec;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

public class GerenciadorBloqueioRecuperacao {

    private static final String PREFS_NAME = "SegurancaRecuperacaoPrefs";

    // 1 minuto e 30 segundos (90 segundos) de espera para reenviar o código
    public static final long TEMPO_ESPERA_REENVIO_MS = 90 * 1000L;

    // Janela de expiração de uma sessão de recuperação (60 minutos após o último envio)
    private static final long JANELA_SESSAO_RECUPERACAO_MS = 60 * 60 * 1000L;

    private static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Verifica se existe uma sessão ativa de recuperação de senha para o e-mail.
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
     * Registra o envio inicial de código de uma sessão de recuperação.
     */
    public static void registrarEnvioInicial(Context context, String email, String codigo) {
        registrarEnvio(context, email, codigo);
    }

    /**
     * Registra um reenvio de código e atualiza o timestamp de último envio (reiniciando o timer de 1:30).
     */
    public static void registrarReenvio(Context context, String email, String novoCodigo) {
        registrarEnvio(context, email, novoCodigo);
    }

    private static void registrarEnvio(Context context, String email, String codigo) {
        if (context == null) return;
        String emailNorm = normalizarEmail(email);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putLong("ultimo_envio_" + emailNorm, System.currentTimeMillis())
                .putString("codigo_atual_" + emailNorm, codigo)
                .apply();
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
     * Limpa o estado de recuperação após redefinição de senha concluída.
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
     * Formata um tempo em milissegundos no padrão MM:SS (ex: 01:30).
     */
    public static String formatarTempoMinutosSegundos(long tempoMs) {
        long totalSegundos = (long) Math.ceil(tempoMs / 1000.0);
        if (totalSegundos < 0) totalSegundos = 0;
        long minutos = totalSegundos / 60;
        long segundos = totalSegundos % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutos, segundos);
    }
}
