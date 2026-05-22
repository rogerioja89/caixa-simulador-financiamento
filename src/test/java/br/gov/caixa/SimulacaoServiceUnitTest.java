package br.gov.caixa;

import br.gov.caixa.dto.SimulacaoRequest;
import br.gov.caixa.dto.SimulacaoResponse;
import br.gov.caixa.exception.SimulacaoNaoEncontradaException;
import br.gov.caixa.mapper.SimulacaoMapper;
import br.gov.caixa.repository.SimulacaoRepository;
import br.gov.caixa.service.SimulacaoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import br.gov.caixa.entity.Simulacao;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimulacaoServiceUnitTest {

    @Mock
    SimulacaoRepository repository;

    @Spy
    SimulacaoMapper mapper;

    @InjectMocks
    SimulacaoService service;

    @Test
    void deveCalcularCorretamentePara1Mes() {
        SimulacaoRequest request = criarRequest("1000.00", "1.5", 1);

        SimulacaoResponse response = service.simular(request);

        assertEquals(1, response.memoriaCalculo.size());
        assertEquals(0, new BigDecimal("1000.00").compareTo(response.memoriaCalculo.get(0).saldoInicial));
        assertEquals(0, new BigDecimal("15.00").compareTo(response.memoriaCalculo.get(0).juro));
        assertEquals(0, new BigDecimal("1015.00").compareTo(response.memoriaCalculo.get(0).saldoFinal));
        assertEquals(0, new BigDecimal("1015.00").compareTo(response.valorTotalFinal));
        assertEquals(0, new BigDecimal("15.00").compareTo(response.valorTotalJuros));
    }

    @Test
    void deveCalcularCorretamenteParaPrazoLongo() {
        SimulacaoRequest request = criarRequest("1000.00", "1.5", 360);

        SimulacaoResponse response = service.simular(request);

        assertEquals(360, response.memoriaCalculo.size());
        assertEquals(1, response.memoriaCalculo.get(0).mes);
        assertEquals(360, response.memoriaCalculo.get(359).mes);
        for (int i = 0; i < 359; i++) {
            assertEquals(0, response.memoriaCalculo.get(i).saldoFinal
                    .compareTo(response.memoriaCalculo.get(i + 1).saldoInicial));
        }
    }

    @Test
    void deveCalcularCorretamenteComTaxaPequena() {
        // 1000.00 * 0.0001 = 0.10 — com double arredondaria para 0.00
        SimulacaoRequest request = criarRequest("1000.00", "0.01", 1);

        SimulacaoResponse response = service.simular(request);

        assertEquals(0, new BigDecimal("0.10").compareTo(response.memoriaCalculo.get(0).juro));
        assertTrue(response.memoriaCalculo.get(0).juro.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void deveCalcularCorretamenteComTaxaAlta() {
        // com taxa alta, o juro do mês 2 deve incidir sobre o saldoFinal do mês 1, não sobre o valorInicial
        SimulacaoRequest request = criarRequest("1000.00", "5.0", 2);

        SimulacaoResponse response = service.simular(request);

        assertEquals(0, new BigDecimal("50.00").compareTo(response.memoriaCalculo.get(0).juro));
        assertEquals(0, new BigDecimal("1050.00").compareTo(response.memoriaCalculo.get(0).saldoFinal));
        assertEquals(0, new BigDecimal("52.50").compareTo(response.memoriaCalculo.get(1).juro));
        assertEquals(0, new BigDecimal("1102.50").compareTo(response.memoriaCalculo.get(1).saldoFinal));
    }

    @Test
    void deveCalcularCorretamenteComValorInicialPequeno() {
        // 0.01 com double é impreciso em IEEE 754; com BigDecimal("0.01") é exato
        SimulacaoRequest request = criarRequest("0.01", "1.5", 1);

        SimulacaoResponse response = service.simular(request);

        // juro interno = 0.0002, exibido como 0.00 por arredondamento — comportamento esperado
        assertEquals(0, new BigDecimal("0.01").compareTo(response.memoriaCalculo.get(0).saldoInicial));
        assertEquals(0, new BigDecimal("0.00").compareTo(response.memoriaCalculo.get(0).juro));
        assertTrue(response.valorTotalFinal.compareTo(request.valorInicial) >= 0);
    }

    @Test
    void deveCalcularCorretamenteComValorInicialAlto() {
        // com double, 999999999.99 perderia os centavos por falta de precisão
        SimulacaoRequest request = criarRequest("999999999.99", "1.0", 1);

        SimulacaoResponse response = service.simular(request);

        assertEquals(0, new BigDecimal("999999999.99").compareTo(response.memoriaCalculo.get(0).saldoInicial));
        assertEquals(0, new BigDecimal("10000000.00").compareTo(response.memoriaCalculo.get(0).juro));
        assertEquals(0, new BigDecimal("1009999999.99").compareTo(response.memoriaCalculo.get(0).saldoFinal));
    }

    @Test
    void devePropagarSaldoFinalComoSaldoInicialDoMesSeguinte() {
        SimulacaoRequest request = criarRequest("1000.00", "1.5", 2);

        SimulacaoResponse response = service.simular(request);

        assertEquals(0, response.memoriaCalculo.get(0).saldoFinal
                .compareTo(response.memoriaCalculo.get(1).saldoInicial));
    }

    @Test
    void deveGerarMemoriaComQuantidadeCorretaDeMeses() {
        SimulacaoRequest request = criarRequest("1000.00", "1.0", 6);

        SimulacaoResponse response = service.simular(request);

        assertEquals(6, response.memoriaCalculo.size());
        for (int i = 0; i < 6; i++) {
            assertEquals(i + 1, response.memoriaCalculo.get(i).mes);
        }
    }

    @Test
    void deveCalcularValorTotalJurosComoSomaDosJurosMensais() {
        SimulacaoRequest request = criarRequest("1000.00", "2.0", 3);

        SimulacaoResponse response = service.simular(request);

        BigDecimal somaJuros = response.memoriaCalculo.stream()
                .map(i -> i.juro)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, somaJuros.compareTo(response.valorTotalJuros));
    }

    @Test
    void deveChamarPersistUmaVezAoSimular() {
        SimulacaoRequest request = criarRequest("500.00", "1.0", 3);

        service.simular(request);

        verify(repository, times(1)).persist(any(Simulacao.class));
    }

    @Test
    void deveLancarExcecaoQuandoIdNaoEncontrado() {
        when(repository.findByIdOptional(99L)).thenReturn(Optional.empty());

        assertThrows(SimulacaoNaoEncontradaException.class,
                () -> service.buscarPorId(99L));
    }

    private SimulacaoRequest criarRequest(String valor, String taxa, int prazo) {
        SimulacaoRequest request = new SimulacaoRequest();
        request.valorInicial = new BigDecimal(valor);
        request.taxaJurosMensal = new BigDecimal(taxa);
        request.prazoMeses = prazo;
        return request;
    }
}