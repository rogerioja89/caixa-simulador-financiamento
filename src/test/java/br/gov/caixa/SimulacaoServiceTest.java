package br.gov.caixa;

import br.gov.caixa.dto.SimulacaoRequest;
import br.gov.caixa.dto.SimulacaoResponse;
import br.gov.caixa.exception.SimulacaoNaoEncontradaException;
import br.gov.caixa.service.SimulacaoService;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
class SimulacaoServiceTest {

    @Inject
    SimulacaoService service;

    @Test
    void deveCalcularCorretamentePara1Mes() {
        SimulacaoRequest request = criarRequest("1000.00", "1.5", 1);
        SimulacaoResponse response = service.simular(request);

        assertEquals(1, response.memoriaCalculo.size());
        assertEquals(1, response.memoriaCalculo.get(0).mes);
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
        SimulacaoRequest request = criarRequest("1000.00", "0.01", 1);
        SimulacaoResponse response = service.simular(request);

        assertEquals(0, new BigDecimal("0.10").compareTo(response.memoriaCalculo.get(0).juro));
        assertTrue(response.memoriaCalculo.get(0).juro.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void deveCalcularCorretamenteComTaxaAlta() {
        SimulacaoRequest request = criarRequest("1000.00", "5.0", 2);
        SimulacaoResponse response = service.simular(request);

        assertEquals(0, new BigDecimal("50.00").compareTo(response.memoriaCalculo.get(0).juro));
        assertEquals(0, new BigDecimal("1050.00").compareTo(response.memoriaCalculo.get(0).saldoFinal));
        assertEquals(0, new BigDecimal("52.50").compareTo(response.memoriaCalculo.get(1).juro));
        assertEquals(0, new BigDecimal("1102.50").compareTo(response.memoriaCalculo.get(1).saldoFinal));
    }

    @Test
    void deveCalcularCorretamenteComValorInicialPequeno() {
        SimulacaoRequest request = criarRequest("0.01", "1.5", 1);
        SimulacaoResponse response = service.simular(request);

        assertEquals(0, new BigDecimal("0.01").compareTo(response.memoriaCalculo.get(0).saldoInicial));
        assertEquals(0, new BigDecimal("0.00").compareTo(response.memoriaCalculo.get(0).juro));
        assertTrue(response.valorTotalFinal.compareTo(request.valorInicial) >= 0);
    }

    @Test
    void deveCalcularCorretamenteComValorInicialAlto() {
        SimulacaoRequest request = criarRequest("999999999.99", "1.0", 1);
        SimulacaoResponse response = service.simular(request);

        assertEquals(0, new BigDecimal("999999999.99").compareTo(response.memoriaCalculo.get(0).saldoInicial));
        assertEquals(0, new BigDecimal("10000000.00").compareTo(response.memoriaCalculo.get(0).juro));
        assertEquals(0, new BigDecimal("1009999999.99").compareTo(response.memoriaCalculo.get(0).saldoFinal));
    }

    @Test
    void deveGerarMemoriaComQuantidadeCorretaDeMeses() {
        SimulacaoRequest request = criarRequest("1000.00", "1.5", 12);
        SimulacaoResponse response = service.simular(request);

        assertEquals(12, response.memoriaCalculo.size());
        for (int i = 0; i < 12; i++) {
            assertEquals(i + 1, response.memoriaCalculo.get(i).mes);
        }
    }

    @Test
    void devePropagarSaldoFinalComoSaldoInicialDoMesSeguinte() {
        SimulacaoRequest request = criarRequest("1000.00", "1.5", 2);
        SimulacaoResponse response = service.simular(request);

        assertEquals(0, response.memoriaCalculo.get(0).saldoFinal
                .compareTo(response.memoriaCalculo.get(1).saldoInicial));
    }

    @Test
    void deveAtribuirIdAposPersistencia() {
        SimulacaoRequest request = criarRequest("500.00", "2.0", 3);
        SimulacaoResponse response = service.simular(request);

        assertNotNull(response.id);
    }

    @Test
    void deveRetornarTodosOsCamposDeEntrada() {
        SimulacaoRequest request = criarRequest("3000.00", "1.0", 6);
        SimulacaoResponse response = service.simular(request);

        assertEquals(0, new BigDecimal("3000.00").compareTo(response.valorInicial));
        assertEquals(0, new BigDecimal("1.0").compareTo(response.taxaJurosMensal));
        assertEquals(6, response.prazoMeses);
    }

    @Test
    void deveLancarExcecaoQuandoIdNaoEncontrado() {
        assertThrows(SimulacaoNaoEncontradaException.class,
                () -> service.buscarPorId(99999L));
    }

    @Test
    void deveRetornarSimulacaoJaPersistida() {
        SimulacaoRequest request = criarRequest("2000.00", "1.0", 6);
        SimulacaoResponse criado = service.simular(request);

        SimulacaoResponse encontrado = service.buscarPorId(criado.id);

        assertEquals(criado.id, encontrado.id);
        assertEquals(0, criado.valorTotalFinal.compareTo(encontrado.valorTotalFinal));
        assertEquals(6, encontrado.memoriaCalculo.size());
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

    private SimulacaoRequest criarRequest(String valor, String taxa, int prazo) {
        SimulacaoRequest request = new SimulacaoRequest();
        request.valorInicial = new BigDecimal(valor);
        request.taxaJurosMensal = new BigDecimal(taxa);
        request.prazoMeses = prazo;
        return request;
    }
}