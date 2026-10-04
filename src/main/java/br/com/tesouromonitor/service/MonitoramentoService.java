package br.com.tesouromonitor.service;

import br.com.tesouromonitor.domain.AtivoCarteira;
import br.com.tesouromonitor.domain.CotacaoDiaria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MonitoramentoService {

    private static final Logger log = LoggerFactory.getLogger(MonitoramentoService.class);

    public void avaliarAgio(AtivoCarteira ativo, CotacaoDiaria cotacaoAtual) {
        // TODO: [Issue #8] Implementar cálculo de variação percentual.
        log.debug("Avaliação de ágio pendente para ativo {} na cotação {}",
                ativo.getNomeTitulo(), cotacaoAtual.getDataCotacao());
    }
}
