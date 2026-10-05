package br.com.tesouromonitor.service;

public class CotacaoNaoEncontradaException extends RuntimeException {

    public CotacaoNaoEncontradaException(Long ativoId) {
        super("Ainda não há cotação disponível para o ativo " + ativoId + ".");
    }
}
