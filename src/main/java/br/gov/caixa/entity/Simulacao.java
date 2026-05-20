package br.gov.caixa.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "simulacao")
public class Simulacao extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    public Long id;

    @Column(nullable = false, precision = 19, scale = 4)
    public BigDecimal valorInicial;

    @Column(nullable = false, precision = 10, scale = 4)
    public BigDecimal taxaJurosMensal;

    @Column(nullable = false)
    public Integer prazoMeses;

    @Column(nullable = false, precision = 19, scale = 4)
    public BigDecimal valorTotalFinal;

    @Column(nullable = false, precision = 19, scale = 4)
    public BigDecimal valorTotalJuros;

    @OneToMany(mappedBy = "simulacao", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @OrderBy("mes ASC")
    public List<SimulacaoItem> memoriaCalculo = new ArrayList<>();
}
