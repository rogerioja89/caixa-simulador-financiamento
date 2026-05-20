# Histórico de Desenvolvimento — Simulador de Financiamentos/Investimentos

## Contexto do Projeto

API REST para simulação de juros compostos (financiamento/investimento) desenvolvida como desafio técnico.  
**Stack:** Java 25 · Quarkus 3.35.3 · H2 in-memory · Hibernate ORM Panache · Jacoco

---

## Sessão 1 — Implementação inicial

### Análise do desafio (`desafio.pdf`)

- **Requisitos funcionais:**
  - `POST /simulacoes` — recebe `valorInicial`, `taxaJurosMensal`, `prazoMeses`, persiste e retorna memória de cálculo mês a mês
  - `GET /simulacoes/{id}` — consulta simulação existente com toda a memória de cálculo
- **Requisitos não funcionais:**
  - Cobertura de testes ≥ 80% (Jacoco) — **critério eliminatório**
  - `BigDecimal` obrigatório para precisão financeira (proibido `double`/`float`)
  - OpenAPI/Swagger exposto automaticamente
  - H2 in-memory, sem Docker, sem scripts SQL manuais
  - Camadas: Resource → Service → Repository

### Estrutura de pacotes criada pelo usuário

```
br.gov.caixa
├── dto/
├── entity/
├── exception/
├── mapper/
├── repository/
├── resource/
└── service/
```

### Arquivos implementados

| Arquivo | Camada | Responsabilidade |
|---|---|---|
| `entity/Simulacao.java` | Entidade JPA | Cabeçalho da simulação (Panache public fields) |
| `entity/SimulacaoItem.java` | Entidade JPA | Linha da memória de cálculo |
| `dto/SimulacaoRequest.java` | DTO | Payload de entrada com `@NotNull`/`@Positive`/`@Min(1)` |
| `dto/SimulacaoResponse.java` | DTO | Resposta completa com lista de itens |
| `dto/ItemMemoriaCalculoDTO.java` | DTO | Item individual da memória de cálculo |
| `repository/SimulacaoRepository.java` | Repositório | `PanacheRepository<Simulacao>` |
| `service/SimulacaoService.java` | Serviço | Juros compostos iterativo com `BigDecimal HALF_UP` |
| `mapper/SimulacaoMapper.java` | Mapper | Conversão entity → DTO |
| `exception/SimulacaoNaoEncontradaException.java` | Exceção | RuntimeException para 404 |
| `exception/GlobalExceptionMapper.java` | Mapper de exceções | `@ServerExceptionMapper` para 404 e 400 |
| `resource/SimulacaoResource.java` | Resource | Endpoints REST com anotações OpenAPI |

### Dependência adicionada ao `pom.xml`

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-openapi</artifactId>
</dependency>
```

### `application.properties` configurado

```properties
quarkus.datasource.db-kind=h2
quarkus.datasource.jdbc.url=jdbc:h2:mem:simulador;DB_CLOSE_DELAY=-1
quarkus.hibernate-orm.schema-management.strategy=drop-and-create
quarkus.swagger-ui.always-include=true
```

### Lógica de cálculo (juros compostos iterativos)

```
taxa = taxaJurosMensal / 100
Para cada mês de 1 até prazoMeses:
    juro       = saldoInicial × taxa  (arredondado HALF_UP, escala 4)
    saldoFinal = saldoInicial + juro
    saldo      = saldoFinal (próximo mês)

valorTotalFinal  = último saldoFinal
valorTotalJuros  = valorTotalFinal - valorInicial
```

### Testes criados

| Classe | Tipo | Testes |
|---|---|---|
| `SimulacaoServiceTest` | Integração com H2 | 8 testes: cálculo, memória, persistência, exceção |
| `SimulacaoResourceTest` | HTTP via Rest-Assured | 8 testes: 201, 200, 404, 400 (validações) |

**Resultado:** 17/17 testes · Cobertura Jacoco 100%

---

## Sessão 2 — Discussão sobre encapsulamento

### Pergunta do usuário

> "não foi utilizados gets sets construtores padrões e construtores com argumentos para o encapsulamento, não seria bom fazer isso?"

### Decisão

- **Entidades Panache:** campos `public` são o padrão proposital do framework. Adicionar getters/setters seria redundante e vai contra o design do Quarkus Panache.
- **DTOs:** campos `public` funcionam perfeitamente com Jackson e são idiomáticos para POJOs simples.
- Foram feitos testes com encapsulamento clássico (getters/setters/construtores) e **revertidos** a pedido do usuário. Projeto voltou ao estado com campos públicos.

---

## Sessão 3 — Melhorias aplicadas

### 1. Remoção de classes legadas do `target`
Executado `./mvnw clean test` — eliminou `GreetingResourceTest.class` e `GreetingResourceIT.class` que eram resíduos do skeleton original do Quarkus e poluíam o relatório Jacoco.  
Resultado: 16 testes (era 17 com o legado).

### 2. Isolamento de dados entre testes
Adicionado `@TestTransaction` na classe `SimulacaoServiceTest`.  
Cada método de teste agora roda em uma transação que é revertida automaticamente ao final, evitando dependência entre testes.

### 3. `buscarPorId` como operação read-only
```java
// antes
@Transactional
public SimulacaoResponse buscarPorId(Long id) { ... }

// depois
@Transactional(Transactional.TxType.SUPPORTS)
public SimulacaoResponse buscarPorId(Long id) { ... }
```
`SUPPORTS`: se há transação ativa, participa; se não há, executa sem transação.

### 4. Formato de erro 400 consistente com 404
`GlobalExceptionMapper` passou a tratar `ConstraintViolationException`:
```json
{ "erros": ["valorInicial: deve ser positivo", "..."] }
```
Antes o 400 usava o formato padrão do Quarkus; agora usa o mesmo padrão do 404 (`{"erro": "..."}` / `{"erros": [...]}`).

### 5. Asserções com valores exatos nos testes de resource
- Configurado `NumberReturnType.BIG_DECIMAL` no REST-Assured via `@BeforeAll`
- Substituídos `notNullValue()` por `comparesEqualTo(new BigDecimal("valor"))` para verificar os cálculos matematicamente

**Resultado final:** 16/16 testes · Cobertura ≥ 80% · BUILD SUCCESS

---

## Discussão técnica — Financiamento vs Investimento

O enunciado menciona "financiamentos e investimentos" e usa o termo "saldo devedor", porém as regras de negócio descritas implementam um **simulador de investimento**:

| Característica | Financiamento real | O que o desafio pede |
|---|---|---|
| Saldo ao longo do tempo | Decresce até zero | **Cresce** (juros compostos) |
| Parcela mensal | Sim (fórmula Price/SAC) | **Não** |
| Amortização | Sim | **Não** |
| Fórmula | PMT = PV × i / (1-(1+i)^-n) | **saldoFinal = saldoInicial × (1 + taxa)** |

**Conclusão:** a implementação está correta em relação às regras de negócio descritas. A inconsistência é do enunciado.

---

## Comandos úteis

```bash
# Rodar testes e gerar relatório de cobertura
./mvnw clean test

# Relatório Jacoco
target/jacoco-report/index.html

# Iniciar em modo dev (hot reload)
./mvnw quarkus:dev

# URLs em modo dev
# Swagger UI:  http://localhost:8080/q/swagger-ui
# OpenAPI spec: http://localhost:8080/q/openapi
```

---

## Sessão 4 — Refinamentos, testes unitários e alinhamento ao desafio.pdf

---

### 1. Análise completa de código não utilizado

Foi realizada uma análise detalhada de todo o projeto (código Java, `pom.xml`, `application.properties`) para identificar o que não estava sendo utilizado.

#### Achados e ações

| # | Onde | Achado | Ação |
|---|---|---|---|
| 1 | `pom.xml` | `quarkus-hibernate-orm` redundante — já incluso transitivamente pelo Panache | **Removido** |
| 2 | `pom.xml` | `maven-failsafe-plugin` configurado sem nenhum `*IT.java` no projeto | **Removido** |
| 3 | `pom.xml` | Profile `native` e propriedade `<skipITs>` inutilizáveis sem GraalVM | **Removidos** |
| 4 | `application.properties` | Faltava `DB_CLOSE_ON_EXIT=FALSE` na URL do H2 (padrão Quarkus inclui, URL customizada não) | **Adicionado** |
| 5 | `SimulacaoNaoEncontradaException.java` | Campo `public final Long id` gravado no construtor mas nunca lido externamente | **Mantido** a pedido do usuário |
| 6 | `application.properties` | `schema-management.strategy=drop-and-create` redundante em dev/test (já é o default) | **Mantido** (boa prática de documentação explícita) |

#### `application.properties` após correção

```properties
quarkus.datasource.db-kind=h2
quarkus.datasource.jdbc.url=jdbc:h2:mem:simulador;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE

quarkus.hibernate-orm.schema-management.strategy=drop-and-create

quarkus.swagger-ui.always-include=true
```

---

### 2. Resposta da API com 2 casas decimais

#### Problema
Os valores monetários na resposta JSON estavam sendo retornados com 4 casas decimais (ex: `1015.0000`), que era a escala interna dos cálculos.

#### Decisão de design
Manter 4 casas decimais **internamente** no serviço (para não acumular erros de arredondamento nos cálculos iterativos) e arredondar para 2 casas decimais **apenas na camada de apresentação** (mapper).

#### Alteração em `SimulacaoMapper.java`

```java
private static final int DISPLAY_SCALE = 2;
private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

private BigDecimal round(BigDecimal value) {
    return value.setScale(DISPLAY_SCALE, ROUNDING);
}
```

O método `round()` é aplicado em todos os campos monetários ao construir o `SimulacaoResponse` e o `ItemMemoriaCalculoDTO`. O campo `taxaJurosMensal` **não** é arredondado — é devolvido exatamente como foi informado pelo usuário.

#### Testes atualizados
Todas as asserções numéricas em `SimulacaoServiceTest` e `SimulacaoResourceTest` foram atualizadas de `"1000.0000"` para `"1000.00"` etc.

---

### 3. Discussão técnica — `precision = 19` nas entidades

Explicação sobre o `@Column(precision = 19, scale = 4)` nas entidades JPA:

- `precision` = total de dígitos significativos
- `scale` = dígitos após a vírgula
- `precision = 19`, `scale = 4` → suporta até **999.999.999.999.999,9999**
- 19 é o número máximo de dígitos de um `long` em Java (`Long.MAX_VALUE`), convenção de mercado para `NUMERIC(19,4)`
- `taxaJurosMensal` usa `precision = 10` porque taxa percentual nunca terá 15 dígitos inteiros

---

### 4. Criação de testes unitários

#### Problema identificado
O `desafio.pdf` exige explicitamente (seção 4): *"Deve possuir testes de **unidade e integração**."*  
Os 16 testes existentes eram todos de integração (`@QuarkusTest` sobe o container Quarkus completo com H2).

#### Solução
Adicionada dependência ao `pom.xml`:
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-junit-mockito</artifactId>
    <scope>test</scope>
</dependency>
```

Criada classe `SimulacaoServiceUnitTest.java` — sem `@QuarkusTest`, usando `@ExtendWith(MockitoExtension.class)`:

| Teste | O que verifica |
|---|---|
| `deveCalcularJurosCorretamentePara1Mes` | Lógica de juros compostos em isolamento |
| `devePropagarSaldoFinalComoSaldoInicialDoMesSeguinte` | Encadeamento correto entre meses |
| `deveGerarMemoriaComQuantidadeCorretaDeMeses` | Tamanho e numeração da memória de cálculo |
| `deveCalcularValorTotalJurosComoSomaDosJurosMensais` | Coerência entre juros mensais e total |
| `deveChamarPersistUmaVezAoSimular` | Interação com o repositório (mock) |
| `deveLancarExcecaoQuandoIdNaoEncontrado` | Exceção para ID inexistente via mock |

O `SimulacaoRepository` é mockado com `@Mock`; o `SimulacaoMapper` é espionado com `@Spy` (instância real); o `SimulacaoService` recebe as injeções via `@InjectMocks`.

**Resultado:** 22/22 testes · 6 unitários + 16 integração · BUILD SUCCESS

---

### 5. Análise de alinhamento ao `desafio.pdf`

Foi realizada análise completa cruzando o projeto com todos os requisitos do PDF.

#### Resultado

| Pilar | Status |
|---|---|
| Requisitos funcionais (`POST`, `GET`, memória de cálculo, persistência) | ✅ |
| Cobertura ≥ 80% com Jacoco | ✅ (100%) |
| Testes de unidade **e** integração | ✅ (após sessão 4) |
| OpenAPI / Swagger automático | ✅ |
| `BigDecimal` sem `double`/`float` | ✅ |
| H2 in-memory sem Docker, sem scripts SQL | ✅ |
| HTTP 201, 200, 400, 404 corretos | ✅ |
| Camadas Resource → Service → Repository | ✅ |
| README com instruções e comando exato | ✅ |

---

### 6. Melhorias no README.md

| Correção | Detalhe |
|---|---|
| Exemplo de resposta | Valores atualizados de 4 para 2 casas decimais |
| Comandos de teste | Adicionada variante Windows (`.\mvnw test`) além do Linux (`./mvnw test`) |
| Comando modo dev | Adicionada variante Windows (`.\mvnw quarkus:dev`) |
| Versão do Quarkus | Corrigido de `3.x` para `3.35.3` |
| Pré-requisitos | Mencionado wrapper para Linux/macOS e Windows |

---

### 7. Tentativa de enforce do threshold 80% — revertida a pedido do usuário

Foi testada a adição de threshold mínimo de cobertura via `jacoco-maven-plugin` no `pom.xml` (goal `check` na fase `verify`). O usuário optou por **não** incluir essa configuração. O relatório Jacoco continua sendo gerado normalmente em `target/jacoco-report/index.html`.

---

**Resultado final da sessão:** 22/22 testes · Cobertura 100% · BUILD SUCCESS · projeto totalmente alinhado ao `desafio.pdf`
