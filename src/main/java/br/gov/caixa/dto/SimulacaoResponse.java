package br.gov.caixa.dto;

import java.math.BigDecimal;
import java.util.List;

public class SimulacaoResponse {

    public Long id;
    public BigDecimal valorInicial;
    public BigDecimal taxaJurosMensal;
    public Integer prazoMeses;
    public BigDecimal valorTotalFinal;
    public BigDecimal valorTotalJuros;
    public List<ItemMemoriaCalculoDTO> memoriaCalculo;
}
