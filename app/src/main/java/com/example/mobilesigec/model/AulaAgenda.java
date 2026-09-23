package com.example.mobilesigec.model;

import java.util.Date;

public class AulaAgenda {
    private int idAgenda;
    private int idUsuario;
    private Date dataAula;
    private String titulo;
    private String descricao;
    private String turma;
    private String status;
    private String corIndicador;

    public AulaAgenda() {
    }

    public AulaAgenda(int idAgenda, int idUsuario, Date dataAula, String titulo, String descricao, String turma, String status, String corIndicador) {
        this.idAgenda = idAgenda;
        this.idUsuario = idUsuario;
        this.dataAula = dataAula;
        this.titulo = titulo;
        this.descricao = descricao;
        this.turma = turma;
        this.status = status;
        this.corIndicador = corIndicador;
    }

    public int getIdAgenda() {
        return idAgenda;
    }

    public void setIdAgenda(int idAgenda) {
        this.idAgenda = idAgenda;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Date getDataAula() {
        return dataAula;
    }

    public void setDataAula(Date dataAula) {
        this.dataAula = dataAula;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getTurma() {
        return turma;
    }

    public void setTurma(String turma) {
        this.turma = turma;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCorIndicador() {
        return corIndicador;
    }

    public void setCorIndicador(String corIndicador) {
        this.corIndicador = corIndicador;
    }
}
