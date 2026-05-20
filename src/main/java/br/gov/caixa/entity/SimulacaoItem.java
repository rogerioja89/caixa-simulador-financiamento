package br.gov.caixa.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "simulacao_item")
public class SimulacaoItem extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "simulacao_id", nullable = false)
    public Simulacao simulacao;

    @Column(nullable = false)
    public Integer mes;

    @Column(nullable = false, precision = 19, scale = 4)
    public BigDecimal saldoInicial;

    @Column(nullable = false, precision = 19, scale = 4)
    public BigDecimal juro;

    @Column(nullable = false, precision = 19, scale = 4)
    public BigDecimal saldoFinal;
}
