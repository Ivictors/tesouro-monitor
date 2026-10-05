package br.com.tesouromonitor.service;

public class TesouroDiretoIntegracaoException extends RuntimeException {

    public TesouroDiretoIntegracaoException(String message) {
        super(message);
    }

    public TesouroDiretoIntegracaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
