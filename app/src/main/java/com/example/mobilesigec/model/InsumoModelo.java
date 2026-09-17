package com.example.mobilesigec.model;

public class InsumoModelo {

    private int idInsumo;
    private String nomeInsumo;
    private String quantidade;
    private String unidadeMedida;
    private boolean isMarcado; // Controla se o checkbox está ticado na tela


    public InsumoModelo(int idInsumo, String nomeInsumo, String quantidade, String unidadeMedida) {
        this.idInsumo = idInsumo;
        this.nomeInsumo = nomeInsumo;
        this.quantidade = quantidade;
        this.unidadeMedida = unidadeMedida;
        this.isMarcado = false; // Todo insumo começa desmarcado (false) por padrão
    }

    public int getIdInsumo() {
        return idInsumo;
    }

    public String getNomeInsumo() {
        return nomeInsumo;
    }

    public String getQuantidade() {
        return quantidade;
    }

    public String getUnidadeMedida() {
        return unidadeMedida;
    }

    // Retorna true se estiver marcado, e false se estiver desmarcado
    public boolean isMarcado() {
        return isMarcado;
    }

    // Setter: Usado para alterar o status quando o usuário clicar no CheckBox
    public void setMarcado(boolean marcado) {
        this.isMarcado = marcado;
    }
}