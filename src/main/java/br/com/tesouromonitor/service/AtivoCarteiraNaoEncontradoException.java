package br.com.tesouromonitor.service;

public class AtivoCarteiraNaoEncontradoException extends RuntimeException {

    public AtivoCarteiraNaoEncontradoException(Long id) {
        super("Ativo da carteira não encontrado: " + id);
    }
}
