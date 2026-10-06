package br.com.tesouromonitor.dto;

import java.time.Instant;

public record SincronizacaoStatusDTO(
        boolean emAndamento,
        Instant iniciadaEm,
        Instant concluidaEm,
        Boolean sucesso,
        Integer cotacoesPersistidas,
        Integer posicoesCorrespondentes,
        String mensagem) {
}
