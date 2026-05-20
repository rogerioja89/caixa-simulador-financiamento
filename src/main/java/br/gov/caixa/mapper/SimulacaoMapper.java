package br.gov.caixa.mapper;

import br.gov.caixa.dto.ItemMemoriaCalculoDTO;
import br.gov.caixa.dto.SimulacaoResponse;
import br.gov.caixa.entity.Simulacao;
import br.gov.caixa.entity.SimulacaoItem;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.math.RoundingMode;

@ApplicationScoped
public class SimulacaoMapper {

    private static final int DISPLAY_SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public SimulacaoResponse toResponse(Simulacao simulacao) {
        SimulacaoResponse response = new SimulacaoResponse();
        response.id = simulacao.id;
        response.valorInicial = round(simulacao.valorInicial);
        response.taxaJurosMensal = simulacao.taxaJurosMensal;
        response.prazoMeses = simulacao.prazoMeses;
        response.valorTotalFinal = round(simulacao.valorTotalFinal);
        response.valorTotalJuros = round(simulacao.valorTotalJuros);
        response.memoriaCalculo = simulacao.memoriaCalculo.stream()
                .map(this::toItemDTO)
                .toList();
        return response;
    }

    private ItemMemoriaCalculoDTO toItemDTO(SimulacaoItem item) {
        ItemMemoriaCalculoDTO dto = new ItemMemoriaCalculoDTO();
        dto.mes = item.mes;
        dto.saldoInicial = round(item.saldoInicial);
        dto.juro = round(item.juro);
        dto.saldoFinal = round(item.saldoFinal);
        return dto;
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(DISPLAY_SCALE, ROUNDING);
    }
}