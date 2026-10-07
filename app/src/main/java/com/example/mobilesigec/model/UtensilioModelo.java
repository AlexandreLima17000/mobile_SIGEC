package com.example.mobilesigec.model;

public class UtensilioModelo {

    private int idUtensilio;
    private String nomeUtensilio;
    private String quantidade;
    private boolean isMarcado;

    public UtensilioModelo(int idUtensilio, String nomeUtensilio, String quantidade) {
        this.idUtensilio = idUtensilio;
        this.nomeUtensilio = nomeUtensilio;
        this.quantidade = quantidade;
        this.isMarcado = false;
    }

    public int getIdUtensilio() { return idUtensilio; }
    public String getNomeUtensilio() { return nomeUtensilio; }
    public String getQuantidade() { return quantidade; }
    public boolean isMarcado() { return isMarcado; }
    public void setMarcado(boolean marcado) { this.isMarcado = marcado; }
}