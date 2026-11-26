package br.com.tesouromonitor.service;

import br.com.tesouromonitor.domain.AtivoCarteira;
import br.com.tesouromonitor.dto.SincronizacaoStatusDTO;
import br.com.tesouromonitor.dto.TesouroResponseDTO;
import br.com.tesouromonitor.repository.AtivoCarteiraRepository;
import java.time.Instant;
import java.time.LocalDate;
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
            false, null, null, null, null, null, "Nenhuma sincroniza\u00e7\u00e3o realizada nesta execu\u00e7\u00e3o.",
            null, null);

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
        int cotacoesPersistidas = 0;
        status = new SincronizacaoStatusDTO(true, inicio, null, null, null, null,
                "Sincronizando cota\u00e7\u00f5es do Tesouro Direto.", null, null);
        try {
            TesouroResponseDTO resposta = tesouroDiretoClient.buscarTitulos().block();
            if (resposta == null || resposta.titulos() == null || resposta.titulos().isEmpty()) {
                return concluirFalha(inicio, "A fonte do Tesouro Direto n\u00e3o retornou cota\u00e7\u00f5es.", null,
                        cotacoesPersistidas);
            }

            cotacoesPersistidas = cotacaoDiariaService.salvarCotacoes(resposta.titulos());
            ResultadoAvaliacao resultadoAvaliacao = avaliarPosicoes();
            boolean monitoramentoCompleto = resultadoAvaliacao.avaliacoesFalhas() == 0;
            String mensagem = monitoramentoCompleto
                    ? "Sincroniza\u00e7\u00e3o conclu\u00edda com sucesso."
                    : "Cota\u00e7\u00f5es importadas, mas o monitoramento ficou incompleto: falha ao avaliar "
                            + resultadoAvaliacao.avaliacoesFalhas() + " posi\u00e7\u00e3o(\u00f5es).";
            status = new SincronizacaoStatusDTO(false, inicio, Instant.now(), true,
                    cotacoesPersistidas, resultadoAvaliacao.posicoesCorrespondentes(), mensagem,
                    resultadoAvaliacao.avaliacoesFalhas(), monitoramentoCompleto);
            log.info("Sincroniza\u00e7\u00e3o conclu\u00edda: {} cota\u00e7\u00f5es persistidas, {} posi\u00e7\u00f5es "
                            + "correspondentes, {} falhas de avalia\u00e7\u00e3o.", cotacoesPersistidas,
                    resultadoAvaliacao.posicoesCorrespondentes(), resultadoAvaliacao.avaliacoesFalhas());
            return status;
        } catch (RuntimeException exception) {
            return concluirFalha(inicio,
                    "N\u00e3o foi poss\u00edvel sincronizar as cota\u00e7\u00f5es. Verifique a conex\u00e3o e a configura\u00e7\u00e3o da fonte.",
                    exception, cotacoesPersistidas);
        } finally {
            emAndamento.set(false);
        }
    }

    private ResultadoAvaliacao avaliarPosicoes() {
        int correspondentes = 0;
        int falhas = 0;
        for (AtivoCarteira ativo : ativoCarteiraRepository.findAll()) {
            LocalDate dataCotacao = null;
            try {
                var cotacao = cotacaoDiariaService.cotacaoMaisRecente(ativo.getNomeTitulo());
                if (cotacao.isEmpty()) {
                    continue;
                }
                dataCotacao = cotacao.orElseThrow().getDataCotacao();
                correspondentes++;
                monitoramentoService.avaliarAgio(ativo, cotacao.orElseThrow());
            } catch (RuntimeException exception) {
                falhas++;
                log.warn("Falha ao avaliar ativo {} na data {}.", ativo.getNomeTitulo(), dataCotacao, exception);
            }
        }
        return new ResultadoAvaliacao(correspondentes, falhas);
    }

    private SincronizacaoStatusDTO concluirFalha(Instant inicio, String mensagem, RuntimeException exception,
                                                  int cotacoesPersistidas) {
        if (exception == null) {
            log.warn("{}", mensagem);
        } else {
            log.error("Falha ao sincronizar cota\u00e7\u00f5es do Tesouro Direto. Tipo de falha: {}.",
                    exception.getClass().getSimpleName());
        }
        status = new SincronizacaoStatusDTO(false, inicio, Instant.now(), false, cotacoesPersistidas, 0, mensagem,
                0, false);
        return status;
    }

    private record ResultadoAvaliacao(int posicoesCorrespondentes, int avaliacoesFalhas) {
    }
}
