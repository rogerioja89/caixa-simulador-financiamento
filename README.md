# Simulador de Financiamentos - CAIXA

API REST para simulação de financiamentos com juros compostos.

**Stack:** Java 25 · Quarkus 3.x · H2 in-memory · Hibernate ORM Panache · Jacoco

---

## Pré-requisitos

- Java 25+
- Maven 3.9+ (ou use o wrapper `./mvnw` incluído)

Sem Docker. Sem scripts SQL. A aplicação sobe 100% nativa.

---

## Executar os testes e validar cobertura

```bash
./mvnw test
```

O relatório de cobertura Jacoco é gerado em:

```
target/jacoco-report/index.html
```

---

## Rodar a aplicação em modo dev

```bash
./mvnw quarkus:dev
```

A API estará disponível em `http://localhost:8080`.

---

## Endpoints

### POST /simulacoes
Cria uma nova simulação de financiamento.

**Body:**
```json
{
  "valorInicial": 1000.00,
  "taxaJurosMensal": 1.5,
  "prazoMeses": 12
}
```

**Resposta 201:**
```json
{
  "id": 1,
  "valorInicial": 1000.00,
  "taxaJurosMensal": 1.5,
  "prazoMeses": 12,
  "valorTotalFinal": 1195.6182,
  "valorTotalJuros": 195.6182,
  "memoriaCalculo": [
    { "mes": 1, "saldoInicial": 1000.0000, "juro": 15.0000, "saldoFinal": 1015.0000 },
    ...
  ]
}
```

### GET /simulacoes/{id}
Consulta uma simulação existente.

- `200 OK` — simulação encontrada
- `404 Not Found` — ID não existe

---

## Documentação OpenAPI / Swagger

Disponível em modo dev em:
- Swagger UI: `http://localhost:8080/q/swagger-ui`
- Spec JSON: `http://localhost:8080/q/openapi`