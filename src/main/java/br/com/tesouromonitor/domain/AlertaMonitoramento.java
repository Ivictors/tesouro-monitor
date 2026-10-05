package br.com.tesouromonitor.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Column;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "alertas_monitoramento", uniqueConstraints = {
        @UniqueConstraint(name = "uk_alerta_ativo_cotacao", columnNames = {"ativo_id", "cotacao_id"})
})
public class AlertaMonitoramento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long ativoId;
    private Long cotacaoId;
    private String nomeAtivo;
    private LocalDate dataCotacao;
    private BigDecimal percentualObservado;
    @Column(nullable = false, length = 64)
    private String regraAplicada;
    private BigDecimal limitePercentual;
    private LocalDateTime criadoEm;
    private boolean lido;
    private LocalDateTime lidoEm;

    public AlertaMonitoramento() {
    }

    public AlertaMonitoramento(Long id, Long ativoId, Long cotacaoId, String nomeAtivo,
                               LocalDate dataCotacao, BigDecimal percentualObservado,
                               String regraAplicada, BigDecimal limitePercentual, LocalDateTime criadoEm,
                               boolean lido, LocalDateTime lidoEm) {
        this.id = id;
        this.ativoId = ativoId;
        this.cotacaoId = cotacaoId;
        this.nomeAtivo = nomeAtivo;
        this.dataCotacao = dataCotacao;
        this.percentualObservado = percentualObservado;
        this.regraAplicada = regraAplicada;
        this.limitePercentual = limitePercentual;
        this.criadoEm = criadoEm;
        this.lido = lido;
        this.lidoEm = lidoEm;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAtivoId() { return ativoId; }
    public void setAtivoId(Long ativoId) { this.ativoId = ativoId; }
    public Long getCotacaoId() { return cotacaoId; }
    public void setCotacaoId(Long cotacaoId) { this.cotacaoId = cotacaoId; }
    public String getNomeAtivo() { return nomeAtivo; }
    public void setNomeAtivo(String nomeAtivo) { this.nomeAtivo = nomeAtivo; }
    public LocalDate getDataCotacao() { return dataCotacao; }
    public void setDataCotacao(LocalDate dataCotacao) { this.dataCotacao = dataCotacao; }
    public BigDecimal getPercentualObservado() { return percentualObservado; }
    public void setPercentualObservado(BigDecimal percentualObservado) { this.percentualObservado = percentualObservado; }
    public String getRegraAplicada() { return regraAplicada; }
    public void setRegraAplicada(String regraAplicada) { this.regraAplicada = regraAplicada; }
    public BigDecimal getLimitePercentual() { return limitePercentual; }
    public void setLimitePercentual(BigDecimal limitePercentual) { this.limitePercentual = limitePercentual; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public boolean isLido() { return lido; }
    public void setLido(boolean lido) { this.lido = lido; }
    public LocalDateTime getLidoEm() { return lidoEm; }
    public void setLidoEm(LocalDateTime lidoEm) { this.lidoEm = lidoEm; }
}
