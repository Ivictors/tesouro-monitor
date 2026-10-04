package br.com.tesouromonitor.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cotacoes_diarias")
public class CotacaoDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nomeAtivo;
    private LocalDate dataCotacao;
    private BigDecimal taxaAtual;
    private BigDecimal precoCompraAtual;
    private BigDecimal precoResgateAtual;

    public CotacaoDiaria() {
    }

    public CotacaoDiaria(Long id, String nomeAtivo, LocalDate dataCotacao, BigDecimal taxaAtual,
                         BigDecimal precoCompraAtual, BigDecimal precoResgateAtual) {
        this.id = id;
        this.nomeAtivo = nomeAtivo;
        this.dataCotacao = dataCotacao;
        this.taxaAtual = taxaAtual;
        this.precoCompraAtual = precoCompraAtual;
        this.precoResgateAtual = precoResgateAtual;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNomeAtivo() { return nomeAtivo; }
    public void setNomeAtivo(String nomeAtivo) { this.nomeAtivo = nomeAtivo; }
    public LocalDate getDataCotacao() { return dataCotacao; }
    public void setDataCotacao(LocalDate dataCotacao) { this.dataCotacao = dataCotacao; }
    public BigDecimal getTaxaAtual() { return taxaAtual; }
    public void setTaxaAtual(BigDecimal taxaAtual) { this.taxaAtual = taxaAtual; }
    public BigDecimal getPrecoCompraAtual() { return precoCompraAtual; }
    public void setPrecoCompraAtual(BigDecimal precoCompraAtual) { this.precoCompraAtual = precoCompraAtual; }
    public BigDecimal getPrecoResgateAtual() { return precoResgateAtual; }
    public void setPrecoResgateAtual(BigDecimal precoResgateAtual) { this.precoResgateAtual = precoResgateAtual; }
}
