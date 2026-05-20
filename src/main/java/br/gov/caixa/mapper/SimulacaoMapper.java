package br.gov.caixa.mapper;

import br.gov.caixa.dto.ItemMemoriaCalculoDTO;
import br.gov.caixa.dto.SimulacaoResponse;
import br.gov.caixa.entity.Simulacao;
import br.gov.caixa.entity.SimulacaoItem;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SimulacaoMapper {

    public SimulacaoResponse toResponse(Simulacao simulacao) {
        SimulacaoResponse response = new SimulacaoResponse();
        response.id = simulacao.id;
        response.valorInicial = simulacao.valorInicial;
        response.taxaJurosMensal = simulacao.taxaJurosMensal;
        response.prazoMeses = simulacao.prazoMeses;
        response.valorTotalFinal = simulacao.valorTotalFinal;
        response.valorTotalJuros = simulacao.valorTotalJuros;
        response.memoriaCalculo = simulacao.memoriaCalculo.stream()
                .map(this::toItemDTO)
                .toList();
        return response;
    }

    private ItemMemoriaCalculoDTO toItemDTO(SimulacaoItem item) {
        ItemMemoriaCalculoDTO dto = new ItemMemoriaCalculoDTO();
        dto.mes = item.mes;
        dto.saldoInicial = item.saldoInicial;
        dto.juro = item.juro;
        dto.saldoFinal = item.saldoFinal;
        return dto;
    }
}
