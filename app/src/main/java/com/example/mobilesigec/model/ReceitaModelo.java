package com.example.mobilesigec.model;

public class ReceitaModelo {

    private int idReceita;
    private String nomeReceita;
    private String nomeTurma;
    private String nomeLaboratorio;

    public ReceitaModelo(int idReceita, String nomeReceita, String nomeTurma, String nomeLaboratorio) {
        this.idReceita = idReceita;
        this.nomeReceita = nomeReceita;
        this.nomeTurma = nomeTurma;
        this.nomeLaboratorio = nomeLaboratorio;
    }

    public int getIdReceita() {
        return idReceita;
    }

    public String getNomeReceita() {
        return nomeReceita;
    }

    public String getNomeTurma() {
        return nomeTurma;
    }

    public String getNomeLaboratorio() {
        return nomeLaboratorio;
    }

    @Override
    public String toString() {
        return nomeReceita;
    }
}