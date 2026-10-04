package br.com.tesouromonitor.scheduler;

import br.com.tesouromonitor.dto.TesouroResponseDTO;
import br.com.tesouromonitor.service.MonitoramentoService;
import br.com.tesouromonitor.service.TesouroDiretoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TesouroSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(TesouroSyncScheduler.class);

    private final TesouroDiretoClient tesouroDiretoClient;
    private final MonitoramentoService monitoramentoService;

    public TesouroSyncScheduler(TesouroDiretoClient tesouroDiretoClient,
                                MonitoramentoService monitoramentoService) {
        this.tesouroDiretoClient = tesouroDiretoClient;
        this.monitoramentoService = monitoramentoService;
    }

    @Scheduled(cron = "0 15 10 * * MON-FRI")
    public void sincronizarCotacoes() {
        // TODO: [Issue #9] Confirmar contrato JSON e mapear cotacoes para ativos da carteira.
        TesouroResponseDTO response = tesouroDiretoClient.buscarTitulos().block();
        int quantidade = response == null || response.titulos() == null ? 0 : response.titulos().size();
        log.info("Consulta ao Tesouro Direto concluída; {} títulos recebidos", quantidade);
        // A avaliação depende do mapeamento dos campos externos para CotacaoDiaria (Issue #9).
        log.debug("Serviço de monitoramento disponível: {}", monitoramentoService.getClass().getSimpleName());
    }
}
