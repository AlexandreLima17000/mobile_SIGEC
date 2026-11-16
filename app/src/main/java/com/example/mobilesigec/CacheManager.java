package com.example.mobilesigec;

import android.content.Context;
import android.content.SharedPreferences;

public class CacheManager {

    private static final String PREF_NAME = "CacheDadosSIGEC";

    // Chaves padronizadas para cada tela do aplicativo
    public static final String CHAVE_HOME_TURMAS = "cache_home_turmas";
    public static final String CHAVE_HOME_RECEITAS_SEMANA = "cache_home_receitas_semana";
    public static final String CHAVE_CALENDARIO = "cache_calendario";
    public static final String CHAVE_RECEITAS = "cache_lista_receitas";
    public static final String CHAVE_CHECKLIST_INSUMOS = "cache_checklist_insumos";
    public static final String CHAVE_CHECKLIST_UTENSILIOS = "cache_checklist_utensilios";

    // Tempo padrão de validade do cache: 30 minutos (em milissegundos)
    public static final long TEMPO_EXPIRACAO_PADRAO = 30 * 60 * 1000L;

    /**
     * Salva uma String JSON no cache do aparelho junto com o horário atual.
     */
    public static void salvarJson(Context context, String chave, String jsonDados) {
        if (context == null || chave == null || jsonDados == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(chave, jsonDados)
                .putLong(chave + "_timestamp", System.currentTimeMillis())
                .apply();
    }

    /**
     * Recupera o JSON salvo se ele ainda estiver dentro do prazo de validade.
     * Retorna null se não existir cache ou se já tiver expirado.
     */
    public static String obterJson(Context context, String chave, long tempoExpiracaoMs) {
        if (context == null || chave == null) return null;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String dados = prefs.getString(chave, null);
        long salvoEm = prefs.getLong(chave + "_timestamp", 0);

        if (dados == null || salvoEm == 0) {
            return null;
        }

        long agora = System.currentTimeMillis();
        if ((agora - salvoEm) > tempoExpiracaoMs) {
            // Cache expirou: precisa buscar dados novos no MySQL
            return null;
        }

        return dados;
    }

    /**
     * Remove apenas uma chave específica (útil quando o usuário altera algo,
     * como finalizar um checklist ou solicitar insumo extra).
     */
    public static void invalidarChave(Context context, String chave) {
        if (context == null || chave == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(chave)
                .remove(chave + "_timestamp")
                .apply();
    }

    /**
     * Apaga todo o cache do aparelho (obrigatório chamar ao fazer Logout).
     */
    public static void limparTodoCache(Context context) {
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }
}