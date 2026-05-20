package br.gov.caixa.service;

import br.gov.caixa.dto.SimulacaoRequest;
import br.gov.caixa.dto.SimulacaoResponse;
import br.gov.caixa.entity.Simulacao;
import br.gov.caixa.entity.SimulacaoItem;
import br.gov.caixa.exception.SimulacaoNaoEncontradaException;
import br.gov.caixa.mapper.SimulacaoMapper;
import br.gov.caixa.repository.SimulacaoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class SimulacaoService {

    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    @Inject
    SimulacaoRepository repository;

    @Inject
    SimulacaoMapper mapper;

    @Transactional
    public SimulacaoResponse simular(SimulacaoRequest request) {
        List<SimulacaoItem> itens = calcularMemoria(request);

        BigDecimal valorTotalFinal = itens.get(itens.size() - 1).saldoFinal;
        BigDecimal valorTotalJuros = valorTotalFinal.subtract(request.valorInicial);

        Simulacao simulacao = new Simulacao();
        simulacao.valorInicial = request.valorInicial;
        simulacao.taxaJurosMensal = request.taxaJurosMensal;
        simulacao.prazoMeses = request.prazoMeses;
        simulacao.valorTotalFinal = valorTotalFinal;
        simulacao.valorTotalJuros = valorTotalJuros;
        simulacao.memoriaCalculo = itens;

        for (SimulacaoItem item : itens) {
            item.simulacao = simulacao;
        }

        repository.persist(simulacao);

        return mapper.toResponse(simulacao);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public SimulacaoResponse buscarPorId(Long id) {
        Simulacao simulacao = repository.findByIdOptional(id)
                .orElseThrow(() -> new SimulacaoNaoEncontradaException(id));
        return mapper.toResponse(simulacao);
    }

    private List<SimulacaoItem> calcularMemoria(SimulacaoRequest request) {
        BigDecimal taxa = request.taxaJurosMensal.divide(new BigDecimal("100"), 8, ROUNDING);
        BigDecimal saldo = request.valorInicial.setScale(SCALE, ROUNDING);
        List<SimulacaoItem> itens = new ArrayList<>();

        for (int mes = 1; mes <= request.prazoMeses; mes++) {
            BigDecimal saldoInicial = saldo;
            BigDecimal juro = saldoInicial.multiply(taxa).setScale(SCALE, ROUNDING);
            BigDecimal saldoFinal = saldoInicial.add(juro);

            SimulacaoItem item = new SimulacaoItem();
            item.mes = mes;
            item.saldoInicial = saldoInicial;
            item.juro = juro;
            item.saldoFinal = saldoFinal;

            itens.add(item);
            saldo = saldoFinal;
        }

        return itens;
    }
}
