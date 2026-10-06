package br.com.tesouromonitor.service;

import br.com.tesouromonitor.domain.AtivoCarteira;
import br.com.tesouromonitor.dto.SincronizacaoStatusDTO;
import br.com.tesouromonitor.dto.TesouroResponseDTO;
import br.com.tesouromonitor.repository.AtivoCarteiraRepository;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CotacaoSincronizacaoService {

    private static final Logger log = LoggerFactory.getLogger(CotacaoSincronizacaoService.class);

    private final TesouroDiretoClient tesouroDiretoClient;
    private final MonitoramentoService monitoramentoService;
    private final CotacaoDiariaService cotacaoDiariaService;
    private final AtivoCarteiraRepository ativoCarteiraRepository;
    private final AtomicBoolean emAndamento = new AtomicBoolean();
    private volatile SincronizacaoStatusDTO status = new SincronizacaoStatusDTO(
            false, null, null, null, null, null, "Nenhuma sincronização realizada nesta execução.");

    public CotacaoSincronizacaoService(TesouroDiretoClient tesouroDiretoClient,
                                       MonitoramentoService monitoramentoService,
                                       CotacaoDiariaService cotacaoDiariaService,
                                       AtivoCarteiraRepository ativoCarteiraRepository) {
        this.tesouroDiretoClient = tesouroDiretoClient;
        this.monitoramentoService = monitoramentoService;
        this.cotacaoDiariaService = cotacaoDiariaService;
        this.ativoCarteiraRepository = ativoCarteiraRepository;
    }

    public SincronizacaoStatusDTO statusAtual() {
        return status;
    }

    public SincronizacaoStatusDTO sincronizar() {
        if (!emAndamento.compareAndSet(false, true)) {
            throw new SincronizacaoEmAndamentoException();
        }

        Instant inicio = Instant.now();
        status = new SincronizacaoStatusDTO(true, inicio, null, null, null, null,
                "Sincronizando cotações do Tesouro Direto.");
        try {
            TesouroResponseDTO resposta = tesouroDiretoClient.buscarTitulos().block();
            if (resposta == null || resposta.titulos() == null || resposta.titulos().isEmpty()) {
                return concluirFalha(inicio, "A fonte do Tesouro Direto não retornou cotações.", null);
            }

            int cotacoesPersistidas = cotacaoDiariaService.salvarCotacoes(resposta.titulos());
            int posicoesCorrespondentes = avaliarPosicoes();
            status = new SincronizacaoStatusDTO(false, inicio, Instant.now(), true,
                    cotacoesPersistidas, posicoesCorrespondentes,
                    "Sincronização concluída com sucesso.");
            log.info("Sincronização concluída: {} cotações persistidas, {} posições correspondentes.",
                    cotacoesPersistidas, posicoesCorrespondentes);
            return status;
        } catch (RuntimeException exception) {
            return concluirFalha(inicio,
                    "Não foi possível sincronizar as cotações. Verifique a conexão e a configuração da fonte.",
                    exception);
        } finally {
            emAndamento.set(false);
        }
    }

    private int avaliarPosicoes() {
        int correspondentes = 0;
        for (AtivoCarteira ativo : ativoCarteiraRepository.findAll()) {
            var cotacao = cotacaoDiariaService.cotacaoMaisRecente(ativo.getNomeTitulo());
            if (cotacao.isEmpty()) {
                continue;
            }
            correspondentes++;
            try {
                monitoramentoService.avaliarAgio(ativo, cotacao.orElseThrow());
            } catch (RuntimeException exception) {
                log.warn("Falha ao avaliar ativo {} na data {}.", ativo.getNomeTitulo(),
                        cotacao.orElseThrow().getDataCotacao(), exception);
            }
        }
        return correspondentes;
    }

    private SincronizacaoStatusDTO concluirFalha(Instant inicio, String mensagem, RuntimeException exception) {
        if (exception == null) {
            log.warn("{}", mensagem);
        } else {
            log.error("Falha ao sincronizar cotações do Tesouro Direto. Tipo de falha: {}.",
                    exception.getClass().getSimpleName());
        }
        status = new SincronizacaoStatusDTO(false, inicio, Instant.now(), false, 0, 0, mensagem);
        return status;
    }
}
