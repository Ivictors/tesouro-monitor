package br.com.tesouromonitor.service;

public class IntervaloDataInvalidoException extends RuntimeException {

    public IntervaloDataInvalidoException() {
        super("A data inicial deve ser anterior ou igual à data final.");
    }
}
