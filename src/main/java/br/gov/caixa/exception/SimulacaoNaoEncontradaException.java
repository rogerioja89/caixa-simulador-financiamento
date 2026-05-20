package br.gov.caixa.exception;

public class SimulacaoNaoEncontradaException extends RuntimeException {

    public final Long id;

    public SimulacaoNaoEncontradaException(Long id) {
        super("Simulação com id " + id + " não encontrada");
        this.id = id;
    }
}