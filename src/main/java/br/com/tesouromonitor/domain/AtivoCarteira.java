package br.com.tesouromonitor.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "ativos_carteira")
public class AtivoCarteira {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nomeTitulo;
    private LocalDate dataCompra;
    private BigDecimal taxaContratada;
    private BigDecimal precoCompra;
    private BigDecimal quantidade;

    public AtivoCarteira() {
    }

    public AtivoCarteira(Long id, String nomeTitulo, LocalDate dataCompra, BigDecimal taxaContratada,
                         BigDecimal precoCompra, BigDecimal quantidade) {
        this.id = id;
        this.nomeTitulo = nomeTitulo;
        this.dataCompra = dataCompra;
        this.taxaContratada = taxaContratada;
        this.precoCompra = precoCompra;
        this.quantidade = quantidade;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNomeTitulo() { return nomeTitulo; }
    public void setNomeTitulo(String nomeTitulo) { this.nomeTitulo = nomeTitulo; }
    public LocalDate getDataCompra() { return dataCompra; }
    public void setDataCompra(LocalDate dataCompra) { this.dataCompra = dataCompra; }
    public BigDecimal getTaxaContratada() { return taxaContratada; }
    public void setTaxaContratada(BigDecimal taxaContratada) { this.taxaContratada = taxaContratada; }
    public BigDecimal getPrecoCompra() { return precoCompra; }
    public void setPrecoCompra(BigDecimal precoCompra) { this.precoCompra = precoCompra; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
}
