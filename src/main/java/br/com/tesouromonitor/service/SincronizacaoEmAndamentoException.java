package br.com.tesouromonitor.service;

public class SincronizacaoEmAndamentoException extends RuntimeException {

    public SincronizacaoEmAndamentoException() {
        super("Já existe uma sincronização de cotações em andamento.");
    }
}
