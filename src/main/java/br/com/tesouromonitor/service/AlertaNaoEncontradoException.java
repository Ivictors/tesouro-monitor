package br.com.tesouromonitor.service;

public class AlertaNaoEncontradoException extends RuntimeException {

    public AlertaNaoEncontradoException(Long id) {
        super("Alerta " + id + " não encontrado.");
    }
}
