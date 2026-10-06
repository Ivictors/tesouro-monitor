package br.com.tesouromonitor.scheduler;

import static org.mockito.Mockito.verify;

import br.com.tesouromonitor.service.CotacaoSincronizacaoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TesouroSyncSchedulerTest {

    @Mock
    private CotacaoSincronizacaoService sincronizacaoService;

    @InjectMocks
    private TesouroSyncScheduler scheduler;

    @Test
    void delegaSincronizacaoParaCasoDeUsoCompartilhado() {
        scheduler.sincronizarCotacoes();

        verify(sincronizacaoService).sincronizar();
    }
}
