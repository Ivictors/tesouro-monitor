package br.com.tesouromonitor.scheduler;

import br.com.tesouromonitor.service.CotacaoSincronizacaoService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TesouroSyncScheduler {

    private final CotacaoSincronizacaoService sincronizacaoService;

    public TesouroSyncScheduler(CotacaoSincronizacaoService sincronizacaoService) {
        this.sincronizacaoService = sincronizacaoService;
    }

    @Scheduled(cron = "0 15 10 * * MON-FRI", zone = "${tesouro.scheduler.zone:America/Sao_Paulo}")
    public void sincronizarCotacoes() {
        sincronizacaoService.sincronizar();
    }
}
