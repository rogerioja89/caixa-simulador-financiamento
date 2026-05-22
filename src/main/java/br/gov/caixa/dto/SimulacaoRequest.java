package br.gov.caixa.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class SimulacaoRequest {

    @NotNull(message = "valorInicial é obrigatório")
    @Positive(message = "valorInicial deve ser positivo")
    public BigDecimal valorInicial;

    @NotNull(message = "taxaJurosMensal é obrigatória")
    @Positive(message = "taxaJurosMensal deve ser positiva")
    public BigDecimal taxaJurosMensal;

    @NotNull(message = "prazoMeses é obrigatório")
    @Min(value = 1, message = "prazoMeses deve ser no mínimo 1")
    public Integer prazoMeses;
}