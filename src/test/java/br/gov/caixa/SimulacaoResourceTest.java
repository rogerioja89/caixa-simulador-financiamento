package br.gov.caixa;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.JsonConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.path.json.config.JsonPathConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class SimulacaoResourceTest {

    @BeforeAll
    static void configureRestAssured() {
        RestAssured.config = RestAssuredConfig.config().jsonConfig(
                JsonConfig.jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL));
    }

    @Test
    void deveRetornar201AoCriarSimulacao() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "valorInicial": 1000.00,
                    "taxaJurosMensal": 1.5,
                    "prazoMeses": 12
                }
                """)
            .when().post("/simulacoes")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("memoriaCalculo.size()", is(12))
            .body("memoriaCalculo[0].mes", is(1))
            .body("memoriaCalculo[0].saldoInicial", comparesEqualTo(new BigDecimal("1000.0000")))
            .body("memoriaCalculo[0].juro", comparesEqualTo(new BigDecimal("15.0000")))
            .body("memoriaCalculo[0].saldoFinal", comparesEqualTo(new BigDecimal("1015.0000")));
    }

    @Test
    void deveRetornarTodosOsCamposNaResposta() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "valorInicial": 1000.00,
                    "taxaJurosMensal": 1.5,
                    "prazoMeses": 6
                }
                """)
            .when().post("/simulacoes")
            .then()
            .statusCode(201)
            .body("valorInicial", comparesEqualTo(new BigDecimal("1000.00")))
            .body("taxaJurosMensal", comparesEqualTo(new BigDecimal("1.5")))
            .body("prazoMeses", is(6))
            .body("memoriaCalculo[0].mes", is(1))
            .body("memoriaCalculo[0].saldoInicial", comparesEqualTo(new BigDecimal("1000.0000")))
            .body("memoriaCalculo[0].juro", comparesEqualTo(new BigDecimal("15.0000")))
            .body("memoriaCalculo[0].saldoFinal", comparesEqualTo(new BigDecimal("1015.0000")));
    }

    @Test
    void deveRetornarSimulacaoPorId() {
        Long id = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "valorInicial": 5000.00,
                    "taxaJurosMensal": 2.0,
                    "prazoMeses": 6
                }
                """)
            .when().post("/simulacoes")
            .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");

        given()
            .when().get("/simulacoes/" + id)
            .then()
            .statusCode(200)
            .body("id", notNullValue())
            .body("prazoMeses", is(6))
            .body("memoriaCalculo.size()", is(6))
            .body("valorInicial", comparesEqualTo(new BigDecimal("5000.00")))
            .body("memoriaCalculo[0].juro", comparesEqualTo(new BigDecimal("100.0000")));
    }

    @Test
    void deveRetornar404ParaIdInexistente() {
        given()
            .when().get("/simulacoes/99999")
            .then()
            .statusCode(404)
            .body("erro", notNullValue());
    }

    @Test
    void deveRetornar400ParaValorInicialNegativo() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "valorInicial": -100.00,
                    "taxaJurosMensal": 1.5,
                    "prazoMeses": 12
                }
                """)
            .when().post("/simulacoes")
            .then()
            .statusCode(400)
            .body("erros", notNullValue())
            .body("erros.size()", greaterThanOrEqualTo(1));
    }

    @Test
    void deveRetornar400ParaPrazoZero() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "valorInicial": 1000.00,
                    "taxaJurosMensal": 1.5,
                    "prazoMeses": 0
                }
                """)
            .when().post("/simulacoes")
            .then()
            .statusCode(400)
            .body("erros", notNullValue());
    }

    @Test
    void deveRetornar400ParaCamposAusentes() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
            .when().post("/simulacoes")
            .then()
            .statusCode(400)
            .body("erros", notNullValue())
            .body("erros.size()", is(3));
    }

    @Test
    void deveRetornar400ParaTaxaNegativa() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "valorInicial": 1000.00,
                    "taxaJurosMensal": -1.5,
                    "prazoMeses": 12
                }
                """)
            .when().post("/simulacoes")
            .then()
            .statusCode(400)
            .body("erros", notNullValue());
    }
}
