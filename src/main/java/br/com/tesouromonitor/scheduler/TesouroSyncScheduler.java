package br.com.tesouromonitor.scheduler;

import br.com.tesouromonitor.dto.TesouroResponseDTO;
import br.com.tesouromonitor.domain.AtivoCarteira;
import br.com.tesouromonitor.repository.AtivoCarteiraRepository;
import br.com.tesouromonitor.service.CotacaoDiariaService;
import br.com.tesouromonitor.service.MonitoramentoService;
import br.com.tesouromonitor.service.TesouroDiretoClient;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TesouroSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(TesouroSyncScheduler.class);

    private final TesouroDiretoClient tesouroDiretoClient;
    private final MonitoramentoService monitoramentoService;
    private final CotacaoDiariaService cotacaoDiariaService;
    private final AtivoCarteiraRepository ativoCarteiraRepository;

    public TesouroSyncScheduler(TesouroDiretoClient tesouroDiretoClient,
                                MonitoramentoService monitoramentoService,
                                CotacaoDiariaService cotacaoDiariaService,
                                AtivoCarteiraRepository ativoCarteiraRepository) {
        this.tesouroDiretoClient = tesouroDiretoClient;
        this.monitoramentoService = monitoramentoService;
        this.cotacaoDiariaService = cotacaoDiariaService;
        this.ativoCarteiraRepository = ativoCarteiraRepository;
    }

    @Scheduled(cron = "0 15 10 * * MON-FRI", zone = "${tesouro.scheduler.zone:America/Sao_Paulo}")
    public void sincronizarCotacoes() {
        try {
            TesouroResponseDTO response = tesouroDiretoClient.buscarTitulos().block();
            if (response == null || response.titulos() == null || response.titulos().isEmpty()) {
                log.warn("Fonte do Tesouro Direto não retornou títulos para sincronização.");
                return;
            }

            int quotesSaved = cotacaoDiariaService.salvarCotacoes(response.titulos());
            List<AtivoCarteira> holdings = ativoCarteiraRepository.findAll();
            int positionsMatched = 0;
            for (AtivoCarteira holding : holdings) {
                var quote = cotacaoDiariaService.cotacaoMaisRecente(holding.getNomeTitulo());
                if (quote.isPresent()) {
                    positionsMatched++;
                    try {
                        monitoramentoService.avaliarAgio(holding, quote.orElseThrow());
                    } catch (RuntimeException exception) {
                        log.warn("Falha ao avaliar ativo {} na data {}.",
                                holding.getNomeTitulo(), quote.orElseThrow().getDataCotacao(), exception);
                    }
                }
            }
            log.info("Sincronização concluída: {} cotações persistidas, {} posições correspondentes.",
                    quotesSaved, positionsMatched);
        } catch (RuntimeException exception) {
            log.error("Falha ao sincronizar cotações do Tesouro Direto.", exception);
        }
    }
}
