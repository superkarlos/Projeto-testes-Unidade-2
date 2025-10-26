# Projeto: JoséCarlos-LucasEmanoel

Funcionalidade: Finalização de Compra

Este projeto implementa e testa a funcionalidade de cálculo do custo total da 
compra (CompraService.calcularCustoTotal) de forma isolada, utilizando Java 17, JUnit 5 e AssertJ.

Autores
- José Carlos da Silva
- Lucas Emanoel

## Descrição da Funcionalidade

A funcionalidade de finalização de compra calcula o preço total considerando:

- Subtotal dos produtos.
- Descontos por quantidade de itens do mesmo tipo.
- Desconto por valor total da compra (>500 ou >1000).
- Cálculo de frete baseado em faixas de peso e região.
- Taxa adicional para produtos frágeis.
- Descontos de fidelidade (Ouro, Prata, Bronze).
- Arredondamento final para duas casas decimais (Half-up).

## Regras de Negócio

<img width="897" height="219" alt="Captura de tela de 2025-10-26 16-25-25" src="https://github.com/user-attachments/assets/4d916db5-0f2f-4fdc-872e-dad8fb766326" />


## Organização dos Testes

<img width="868" height="195" alt="Captura de tela de 2025-10-26 16-23-40" src="https://github.com/user-attachments/assets/2f20fe69-85a5-4925-9bad-711dad738f92" />


## Tabela de Casos de Teste
<img width="839" height="324" alt="Captura de tela de 2025-10-26 16-20-49" src="https://github.com/user-attachments/assets/6738bf44-2bb8-4014-ad5a-cf1caeefbc16" />

## Cobertura MC/DC (Decisão Composta Mais Complexa)

<img width="822" height="159" alt="Captura de tela de 2025-10-26 16-28-30" src="https://github.com/user-attachments/assets/7701b511-2227-4d2e-827e-e0b42eb05dc6" />


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
