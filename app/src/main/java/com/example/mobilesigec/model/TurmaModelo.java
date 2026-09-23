package com.example.mobilesigec.model;

public class TurmaModelo {

    private int idTurma;
    private String nomeTurma;
    private String nomeLaboratorio;

    // Construtor para facilitar a criação do objeto
    public TurmaModelo(int idTurma, String nomeTurma, String nomeLaboratorio) {
        this.idTurma = idTurma;
        this.nomeTurma = nomeTurma;
        this.nomeLaboratorio = nomeLaboratorio;
    }

    // Getters para podermos acessar o ID quando o usuário selecionar a turma no Spinner
    public int getIdTurma() {
        return idTurma;
    }

    public String getNomeTurma() {
        return nomeTurma;
    }

    public String getNomeLaboratorio() {
        return nomeLaboratorio;
    }

    /*
     * O SEGREDO DO SPINNER:
     * O Spinner chama automaticamente o método toString() de qualquer objeto
     * que você colocar dentro dele para saber o que escrever na tela.
     */
    @Override
    public String toString() {
        return nomeTurma + " - " ;
    }
}