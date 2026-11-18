package com.example.mobilesigec.database;

import android.util.Log;

import com.example.mobilesigec.model.AulaAgenda;
import com.example.mobilesigec.model.TurmaModelo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CalendarioCache {

    private static final String TAG = "CalendarioCache";
    // CachedThreadPool permite pré-carregar Home e Calendário ao mesmo tempo sem fila
    private static final ExecutorService executorPreload = Executors.newCachedThreadPool();

    // --- CACHE DO CALENDÁRIO (MENSAL) ---
    private static int idTurmaCache = -1;
    private static int mesCache = -1;
    private static int anoCache = -1;
    private static Map<Integer, List<AulaAgenda>> eventosCache = new HashMap<>();
    private static List<TurmaModelo> turmasCache = new ArrayList<>();

    // --- CACHE DAS RECEITAS DA SEMANA (HOME PAGE) ---
    public static class ItemReceitaCache {
        public final int idFicha;
        public final String nomeFicha;
        public final String concluido;

        public ItemReceitaCache(int idFicha, String nomeFicha, String concluido) {
            this.idFicha = idFicha;
            this.nomeFicha = nomeFicha;
            this.concluido = concluido;
        }
    }

    private static int idTurmaHomeCache = -1;
    private static Map<Integer, List<ItemReceitaCache>> receitasSemanaCache = new HashMap<>();

    // =========================================================================
    // PRÉ-CARREGAMENTO INICIAL (CHAMAR NA SPLASH / LOGIN)
    // =========================================================================

    public static void preCarregarTudoInicialAsync(int idUsuarioLogado, int idTurmaSalva) {
        executorPreload.execute(() -> {
            List<TurmaModelo> turmas = obterTurmasCache();
            if (turmas.isEmpty()) {
                turmas = buscarTurmasNoBanco(idUsuarioLogado);
                salvarTurmasCache(turmas);
            }

            if (turmas.isEmpty()) return;

            int idTurmaAlvo = turmas.get(0).getIdTurma();
            for (TurmaModelo t : turmas) {
                if (t.getIdTurma() == idTurmaSalva) {
                    idTurmaAlvo = t.getIdTurma();
                    break;
                }
            }

            Calendar hoje = Calendar.getInstance();
            preCarregarReceitasSemanaAsync(idTurmaAlvo);
            preCarregarMesAsync(idTurmaAlvo, hoje.get(Calendar.MONTH) + 1, hoje.get(Calendar.YEAR));
        });
    }

    // =========================================================================
    // MÉTODOS DE CACHE DAS TURMAS (SPINNER)
    // =========================================================================

    public static synchronized void salvarTurmasCache(List<TurmaModelo> turmas) {
        if (turmas != null) {
            turmasCache = new ArrayList<>(turmas);
        }
    }

    public static synchronized List<TurmaModelo> obterTurmasCache() {
        return new ArrayList<>(turmasCache);
    }

    public static List<TurmaModelo> buscarTurmasNoBanco(int idUsuarioLogado) {
        List<TurmaModelo> listaTurmas = new ArrayList<>();
        try (Connection con = ConexaoMySQL.conectar()) {
            if (con != null) {
                String sql = (idUsuarioLogado != -1)
                        ? "SELECT t.id_turma, t.nome_turma, l.nome_laboratorio FROM turma t " +
                          "INNER JOIN usuario_turma ut ON t.id_turma = ut.id_turma " +
                          "INNER JOIN laboratorio l ON t.id_laboratorio = l.id_laboratorio " +
                          "WHERE ut.id_usuario = ? AND t.situacao = 'A' ORDER BY t.nome_turma"
                        : "SELECT t.id_turma, t.nome_turma, l.nome_laboratorio FROM turma t " +
                          "INNER JOIN laboratorio l ON t.id_laboratorio = l.id_laboratorio " +
                          "WHERE t.situacao = 'A' ORDER BY t.nome_turma";

                try (PreparedStatement stmt = con.prepareStatement(sql)) {
                    if (idUsuarioLogado != -1) {
                        stmt.setInt(1, idUsuarioLogado);
                    }
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            listaTurmas.add(new TurmaModelo(
                                    rs.getInt("id_turma"),
                                    rs.getString("nome_turma"),
                                    rs.getString("nome_laboratorio")
                            ));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao buscar turmas no banco", e);
        }
        return listaTurmas;
    }

    // =========================================================================
    // MÉTODOS DE CACHE DO CALENDÁRIO (TELA CALENDÁRIO)
    // =========================================================================

    public static void preCarregarMesAsync(int idTurma, int mes, int ano) {
        if (idTurma == -1) return;

        synchronized (CalendarioCache.class) {
            if (idTurma == idTurmaCache && mes == mesCache && ano == anoCache && !eventosCache.isEmpty()) {
                return;
            }
        }

        executorPreload.execute(() -> {
            Map<Integer, List<AulaAgenda>> resultado = buscarEventosNoBanco(idTurma, mes, ano);
            salvarNoCache(idTurma, mes, ano, resultado);
            Log.d(TAG, "Pré-carregamento do calendário concluído para turma ID: " + idTurma);
        });
    }

    public static synchronized Map<Integer, List<AulaAgenda>> obterEventosSeDisponivel(int idTurma, int mes, int ano) {
        if (idTurma == idTurmaCache && mes == mesCache && ano == anoCache && !eventosCache.isEmpty()) {
            Map<Integer, List<AulaAgenda>> copia = new HashMap<>();
            for (Map.Entry<Integer, List<AulaAgenda>> entry : eventosCache.entrySet()) {
                copia.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            return copia;
        }
        return null;
    }

    public static synchronized void salvarNoCache(int idTurma, int mes, int ano, Map<Integer, List<AulaAgenda>> eventos) {
        idTurmaCache = idTurma;
        mesCache = mes;
        anoCache = ano;
        eventosCache = new HashMap<>();
        if (eventos != null) {
            for (Map.Entry<Integer, List<AulaAgenda>> entry : eventos.entrySet()) {
                eventosCache.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }
    }

    public static Map<Integer, List<AulaAgenda>> buscarEventosNoBanco(int idTurma, int mes, int ano) {
        Map<Integer, List<AulaAgenda>> novosEventos = new HashMap<>();

        try (Connection con = ConexaoMySQL.conectar()) {
            if (con != null) {
                String sql =
                        "SELECT " +
                                "a.id_agendamento, " +
                                "a.data AS data_agendada, " +
                                "a.situacao AS situacao_agendamento, " +
                                "a.concluido, " +
                                "f.id_ficha, " +
                                "f.nome_ficha, " +
                                "f.preparo, " +
                                "t.nome_turma " +
                                "FROM agendamento a " +
                                "INNER JOIN ficha f ON f.id_ficha = a.id_ficha " +
                                "LEFT JOIN turma t ON t.id_turma = f.id_turma " +
                                "WHERE MONTH(a.data) = ? " +
                                "AND YEAR(a.data) = ? " +
                                "AND a.situacao = 'A' " +
                                "AND f.situacao = 'A' " +
                                "AND (f.id_turma = ? OR ? = -1) " +
                                "ORDER BY a.data, f.nome_ficha";

                try (PreparedStatement stmt = con.prepareStatement(sql)) {
                    stmt.setInt(1, mes);
                    stmt.setInt(2, ano);
                    stmt.setInt(3, idTurma);
                    stmt.setInt(4, idTurma);

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            int idFicha = rs.getInt("id_ficha");
                            java.sql.Date dataAgendada = rs.getDate("data_agendada");
                            String nomeFicha = rs.getString("nome_ficha");
                            String preparo = rs.getString("preparo");
                            String nomeTurma = rs.getString("nome_turma");
                            String situacao = rs.getString("situacao_agendamento");

                            AulaAgenda aula = new AulaAgenda(
                                    idFicha,
                                    0,
                                    dataAgendada,
                                    nomeFicha,
                                    preparo,
                                    nomeTurma,
                                    situacao,
                                    "#F7941D"
                            );

                            Calendar cal = Calendar.getInstance();
                            cal.setTime(dataAgendada);
                            int dia = cal.get(Calendar.DAY_OF_MONTH);

                            List<AulaAgenda> listaDia = novosEventos.get(dia);
                            if (listaDia == null) {
                                listaDia = new ArrayList<>();
                                novosEventos.put(dia, listaDia);
                            }

                            boolean jaExiste = false;
                            for (AulaAgenda existente : listaDia) {
                                if (existente.getIdAgenda() == idFicha ||
                                        (existente.getTitulo() != null && existente.getTitulo().equalsIgnoreCase(nomeFicha))) {
                                    jaExiste = true;
                                    break;
                                }
                            }

                            if (!jaExiste) {
                                listaDia.add(aula);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao buscar eventos do calendário", e);
        }

        return novosEventos;
    }

    // =========================================================================
    // MÉTODOS DE CACHE DAS RECEITAS DA SEMANA (TELA HOME PAGE)
    // =========================================================================

    public static void preCarregarReceitasSemanaAsync(int idTurma) {
        if (idTurma == -1) return;

        synchronized (CalendarioCache.class) {
            if (idTurma == idTurmaHomeCache && !receitasSemanaCache.isEmpty()) {
                return;
            }
        }

        executorPreload.execute(() -> {
            Map<Integer, List<ItemReceitaCache>> resultado = buscarReceitasSemanaNoBanco(idTurma);
            salvarReceitasSemanaCache(idTurma, resultado);
            Log.d(TAG, "Pré-carregamento da Home concluído para turma ID: " + idTurma);
        });
    }

    public static synchronized Map<Integer, List<ItemReceitaCache>> obterReceitasSemanaSeDisponivel(int idTurma) {
        if (idTurma != -1 && idTurma == idTurmaHomeCache && !receitasSemanaCache.isEmpty()) {
            Map<Integer, List<ItemReceitaCache>> copia = new HashMap<>();
            for (Map.Entry<Integer, List<ItemReceitaCache>> entry : receitasSemanaCache.entrySet()) {
                copia.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            return copia;
        }
        return null;
    }

    public static synchronized void salvarReceitasSemanaCache(int idTurma, Map<Integer, List<ItemReceitaCache>> mapDias) {
        idTurmaHomeCache = idTurma;
        receitasSemanaCache = new HashMap<>();
        if (mapDias != null) {
            for (Map.Entry<Integer, List<ItemReceitaCache>> entry : mapDias.entrySet()) {
                receitasSemanaCache.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }
    }

    public static Map<Integer, List<ItemReceitaCache>> buscarReceitasSemanaNoBanco(int idTurma) {
        Map<Integer, List<ItemReceitaCache>> mapDias = new HashMap<>();
        for (int dayIndex = Calendar.MONDAY; dayIndex <= Calendar.FRIDAY; dayIndex++) {
            mapDias.put(dayIndex, new ArrayList<>());
        }

        try (Connection con = ConexaoMySQL.conectar()) {
            if (con != null) {
                String sql = "SELECT f.nome_ficha, COALESCE(a.concluido, 'N') AS concluido, a.* " +
                        "FROM ficha f " +
                        "LEFT JOIN agendamento a ON f.id_ficha = a.id_ficha " +
                        "WHERE f.id_turma = ?";

                try (PreparedStatement stmt = con.prepareStatement(sql)) {
                    stmt.setInt(1, idTurma);
                    try (ResultSet rs = stmt.executeQuery()) {
                        ResultSetMetaData metaData = rs.getMetaData();
                        int columnCount = metaData.getColumnCount();

                        Calendar calNow = Calendar.getInstance();
                        calNow.set(Calendar.HOUR_OF_DAY, 0);
                        calNow.set(Calendar.MINUTE, 0);
                        calNow.set(Calendar.SECOND, 0);
                        calNow.set(Calendar.MILLISECOND, 0);

                        int dow = calNow.get(Calendar.DAY_OF_WEEK);
                        int daysToMonday = (dow == Calendar.SUNDAY) ? -6 : (Calendar.MONDAY - dow);

                        Calendar startOfWeek = (Calendar) calNow.clone();
                        startOfWeek.add(Calendar.DAY_OF_MONTH, daysToMonday);

                        Calendar endOfWeek = (Calendar) startOfWeek.clone();
                        endOfWeek.add(Calendar.DAY_OF_MONTH, 6);
                        endOfWeek.set(Calendar.HOUR_OF_DAY, 23);
                        endOfWeek.set(Calendar.MINUTE, 59);
                        endOfWeek.set(Calendar.SECOND, 59);
                        endOfWeek.set(Calendar.MILLISECOND, 999);

                        long startMillis = startOfWeek.getTimeInMillis();
                        long endMillis = endOfWeek.getTimeInMillis();

                        while (rs.next()) {
                            int idFicha = rs.getInt("id_ficha");
                            String nomeFicha = rs.getString("nome_ficha");
                            String concluido = rs.getString("concluido");

                            int dayOfWeek = -1;
                            boolean temData = false;
                            boolean pertenceASemanaAtual = false;

                            for (int i = 1; i <= columnCount; i++) {
                                String colName = metaData.getColumnLabel(i).toLowerCase();
                                int colType = metaData.getColumnType(i);

                                if (!colName.startsWith("id_") && (colType == java.sql.Types.DATE || colType == java.sql.Types.TIMESTAMP || colName.contains("data") || colName.contains("date"))) {
                                    try {
                                        java.util.Date dateVal = rs.getTimestamp(i);
                                        if (dateVal == null) {
                                            dateVal = rs.getDate(i);
                                        }
                                        if (dateVal != null) {
                                            temData = true;
                                            long t = dateVal.getTime();
                                            if (t >= startMillis && t <= endMillis) {
                                                pertenceASemanaAtual = true;
                                                Calendar itemCal = Calendar.getInstance();
                                                itemCal.setTime(dateVal);
                                                dayOfWeek = itemCal.get(Calendar.DAY_OF_WEEK);
                                            }
                                            break;
                                        }
                                    } catch (Exception ignored) {}
                                } else if (colName.equals("dia_semana") || colName.equals("dia")) {
                                    try {
                                        int val = rs.getInt(i);
                                        if (val >= 1 && val <= 7) {
                                            dayOfWeek = val;
                                            temData = true;
                                            pertenceASemanaAtual = true;
                                            break;
                                        }
                                    } catch (Exception ignored) {}
                                }
                            }

                            if (!temData || !pertenceASemanaAtual) {
                                continue;
                            }

                            List<ItemReceitaCache> targetList = mapDias.get(dayOfWeek);
                            if (targetList != null) {
                                targetList.add(new ItemReceitaCache(idFicha, nomeFicha, concluido));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao buscar receitas da semana no cache", e);
        }

        return mapDias;
    }

    // =========================================================================
    // LIMPEZA GERAL (USAR NO LOGOUT)
    // =========================================================================

    public static synchronized void limparCache() {
        idTurmaCache = -1;
        mesCache = -1;
        anoCache = -1;
        eventosCache.clear();
        turmasCache.clear();

        idTurmaHomeCache = -1;
        receitasSemanaCache.clear();
    }
}