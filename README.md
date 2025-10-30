# Projeto: JoséCarlos-LucasEmanuel

Funcionalidade: Finalização de Compra

Este projeto implementa e testa a funcionalidade de cálculo do custo total da 
compra (CompraService.calcularCustoTotal) de forma isolada, utilizando Java 17, JUnit 5 e AssertJ.

Autores
- José Carlos da Silva
- Lucas Emanuel Ribeiro Costa

## Descrição da Funcionalidade

A funcionalidade de finalização de compra calcula o preço total considerando:

- Subtotal dos produtos.
- Descontos por quantidade de itens do mesmo tipo.
- Desconto por valor total da compra (>500 ou >1000).
- Cálculo de frete baseado em faixas de peso e região.
- Taxa adicional para produtos frágeis.
- Descontos de fidelidade (Ouro, Prata, Bronze).
- Arredondamento final para duas casas decimais (Half-up).

## Partições de domínio e valores limites do domínio
<img width="1296" height="699" alt="image" src="https://github.com/user-attachments/assets/a2435862-bf0a-492e-ac4c-872551ab2381" />

## Tabelas de decisão
<img width="1495" height="233" alt="tabela-decisao-descontos" src="https://github.com/user-attachments/assets/8f232cda-c542-4794-aa30-c6d8133fa540" />

## Projeto dos Casos de Teste (Mapeamento)

Este projeto utiliza um conjunto de 87 testes automatizados, com uso extensivo de Testes Parametrizados (`@ParameterizedTest` com `@CsvFileSource`) para cobrir os critérios de caixa preta.

Devido ao alto volume de testes e ao uso de arquivos CSV externos, a documentação detalhada dos casos de teste (conforme solicitado ) encontra-se diretamente nos arquivos CSV de teste, localizados em `src/test/resources/ecommerce/service/`.

A estratégia de mapeamento é a seguinte:

1.  **Partições e Limites:** Os arquivos `particoes_*.csv` e `limites_*.csv` implementam diretamente as partições e limites definidos na imagem "Partições de domínio e valores limites" (Seção 2). Cada linha em um CSV representa um caso de teste que cobre um ponto de dados específico daquele critério.
2.  **Tabela de Decisão:** Os testes para a Tabela de Decisão (`Tabela de Decisão: Descontos Combinados` ) são implementados em uma classe de teste dedicada, `CompraServiceRegrasTest.java`.
3.  **Robustez:** Os testes de robustez (P1-P10) estão na classe `CompraServiceParticoesTest.java` e usam `@Test` em sua maioria, garantindo que a exceção correta (`IllegalArgumentException`) é lançada.


## Instruções de Execução Pré-requisitos

- Java 17+
- Maven 3.8+
- JUnit 5
- AssertJ
- Jacoco (para cobertura)

### Executar os testes
<pre> mvn clean test </pre>

### Gerar relatório de cobertura
<pre>mvn jacoco:report</pre>

### O relatório ficará disponível em:
target/site/jacoco/index.html

