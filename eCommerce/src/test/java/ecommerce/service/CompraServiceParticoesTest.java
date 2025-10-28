package ecommerce.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;
import ecommerce.entity.TipoProduto;
import ecommerce.util.TestUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class  CompraServiceParticoesTest {

    private CompraService compraService;

    @BeforeEach
    void setup() {
        compraService = new CompraService(null, null, null, null);
    }

    /**
     * Testa as partições do Peso Tributável, que é max(físico, cúbico).
     * Usamos um subtotal fixo de R$ 50,00 (sem desconto por valor).
     * Usamos Regiao.SUDESTE (x1.0) e TipoCliente.BRONZE (sem desconto no frete)
     * para isolar o cálculo do frete baseado apenas no peso.
     */
    @ParameterizedTest(name = "[{index}] {5}]")
    @CsvFileSource(
            resources = "/ecommerce/service/particoes_peso_tributavel.csv",
            numLinesToSkip = 1
    )
    @DisplayName("Partições: Cálculo do Peso Tributável (Físico vs Cúbico)")
    void quandoPesoFisicoOuCubicoMaior_entaoAplicaFreteSobreMaior(String peso, String c, String l, String a, String totalEsperado, String cenario) {
        Produto produto = TestUtils.produto(
                "Produto de teste",
                "50.00",
                peso,
                c, l, a,
                false,
                TipoProduto.ELETRONICO
        );

        ItemCompra i = TestUtils.item(produto, 1);
        CarrinhoDeCompras carrinho = TestUtils.carrinho(i);

        BigDecimal total = compraService.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

        assertThat(total).as(cenario).isEqualByComparingTo(totalEsperado);
    }

    /**
     * Testa as partições de desconto por quantidade de itens do MESMO TIPO.
     * Para isolar esta regra:
     * - O subtotal é mantido abaixo de R$ 500,00 para não ativar o desconto por valor.
     * - O peso é mantido em 1kg (isento) para não adicionar frete.
     * - Cliente BRONZE e Região SUDESTE são usados para não alterar o frete.
     */
    @ParameterizedTest(name = "[{index}] {3}") // Usa a 4ª coluna (cenario)
    @CsvFileSource(
            resources = "/ecommerce/service/particoes_desconto_itens.csv",
            numLinesToSkip = 1 // Pula a linha de cabeçalho
    )
    @DisplayName("Partições: Desconto por Múltiplos Itens")
    void quandoQuantidadeItensMesmoTipoVaria_entaoAplicaDescontoCorreto(
            int quantidade, String precoUnitario, String totalEsperado, String cenario) {

        Produto p = TestUtils.produto(
                "Produto de teste",
                precoUnitario,
                "0.5",
                "1", "1", "1",
                false,
                TipoProduto.ELETRONICO
        );

        ItemCompra i = TestUtils.item(p, quantidade);
        CarrinhoDeCompras carrinho = TestUtils.carrinho(i);

        BigDecimal total = compraService.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

        assertThat(total).as(cenario).isEqualByComparingTo(totalEsperado);
    }

    /**
     * Testa as partições de desconto por valor total do carrinho .
     * Para isolar esta regra:
     * - Usamos apenas 1 item para não ativar o desconto por múltiplos itens.
     * - O peso é mantido em 0.5kg (isento) para não adicionar frete.
     * - Cliente BRONZE e Região SUDESTE são usados para não alterar o frete.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(
            resources = "/ecommerce/service/particoes_desconto_valor.csv",
            numLinesToSkip = 1
    )
    @DisplayName("Partições: Desconto por Valor de Carrinho")
    void quandoSubtotalVaria_entaoAplicaDescontoPorValorCorreto(String subtotal, String totalEsperado, String cenario) {
        Produto produto = TestUtils.produto(
                "Tesde de produto caro",
                subtotal,
                "1",
                "1","1","1",
                false,
                TipoProduto.ELETRONICO
        );

        ItemCompra i = TestUtils.item(produto, 1);
        CarrinhoDeCompras carrinho = TestUtils.carrinho(i);

        BigDecimal total = compraService.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

        assertThat(total).as(cenario).isEqualByComparingTo(totalEsperado);
    }

    /**
     * Testa o cálculo de frete baseado nas faixas de peso total.
     * Para isolar esta regra:
     * - Usamos 1 item não-frágil.
     * - Usamos Cliente BRONZE (sem desconto de frete).
     * - Usamos Região SUDESTE (multiplicador 1.0x).
     * - Subtotal abaixo de R$ 500 (sem desconto de valor).
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(
            resources = "/ecommerce/service/particoes_frete_peso.csv",
            numLinesToSkip = 1
    )
    @DisplayName("Partições: Cálculo do Frete Base por Faixa de Peso")
    void quandoPesoVaria_entaoAplicaFretePorFaixaCorreta(String peso, String subtotal, String totalEsperado, String cenario) {

        Produto p = TestUtils.produto(
                "Produto Teste Peso",
                subtotal,
                peso,
                "1", "1", "1",
                false,
                TipoProduto.ELETRONICO
        );

        ItemCompra i = TestUtils.item(p, 1);
        CarrinhoDeCompras carrinho = TestUtils.carrinho(i);

        BigDecimal total = compraService.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

        assertThat(total).as(cenario).isEqualByComparingTo(totalEsperado);
    }

    /**
     * Testa as partições da Taxa Mínima de Frete (R$ 12,00).
     * P1: Faixa isenta (sem taxa)
     * P2: Faixa não isenta (com taxa)
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(
            resources = "/ecommerce/service/particoes_frete_taxa_minima.csv",
            numLinesToSkip = 1
    )
    @DisplayName("Partições: Taxa Mínima de Frete (R$ 12)")
    void quandoFreteIsentoOuNao_entaoAplicaTaxaMinimaCorretamente(String peso, String subtotal, String totalEsperado, String cenario) {

        Produto p = TestUtils.produto(
                "Produto Teste Taxa",
                subtotal,
                peso,
                "1", "1", "1",
                false,
                TipoProduto.ELETRONICO
        );

        ItemCompra i = TestUtils.item(p, 1);
        CarrinhoDeCompras carrinho = TestUtils.carrinho(i);

        BigDecimal total = compraService.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

        assertThat(total).as(cenario).isEqualByComparingTo(totalEsperado);
    }

    /**
     * Testa as partições da Taxa de Manuseio (Item Frágil).
     * P1: Item não frágil (sem taxa).
     * P2: Item frágil (com taxa de R$ 5 * quantidade).
     * * Para isolar:
     * - Usamos um peso (2 * 3.0 = 6.0kg) para sair da faixa isenta e ter um frete base.
     * - Cliente BRONZE e Região SUDESTE.
     */
    @ParameterizedTest(name = "[{index}] {5}") // Usa a 6ª coluna (cenario)
    @CsvFileSource(
            resources = "/ecommerce/service/particoes_frete_taxa_manuseio.csv",
            numLinesToSkip = 1
    )
    @DisplayName("Partições: Taxa de Manuseio (Item Frágil)")
    void quandoItemFragil_entaoAplicaTaxaManuseio(boolean isFragil, int quantidade, String pesoProduto, String subtotalProduto, String totalEsperado, String cenario) {

        Produto p = TestUtils.produto(
                "Produto Teste Frágil",
                subtotalProduto,
                pesoProduto,
                "1", "1", "1",
                isFragil,
                TipoProduto.ELETRONICO
        );

        ItemCompra i = TestUtils.item(p, quantidade);
        CarrinhoDeCompras carrinho = TestUtils.carrinho(i);

        BigDecimal total = compraService.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

        assertThat(total).as(cenario).isEqualByComparingTo(totalEsperado);
    }

    /**
     * Testa as partições do Multiplicador de Frete por Região.
     * P1-P5: Cada uma das 5 regiões do enum.
     * - Para isolar, usamos um frete base fixo de R$ 24,00.
     * - (Peso 6kg = R$ 12 frete + R$ 12 taxa)
     * - Cliente BRONZE e subtotal < R$ 500.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(
            resources = "/ecommerce/service/particoes_frete_regiao.csv",
            numLinesToSkip = 1
    )
    @DisplayName("Partições: Multiplicador de Frete por Região")
    void quandoRegiaoVaria_entaoAplicaMultiplicadorFrete(String regiao, String totalEsperado, String cenario) {

        Regiao regiaoEnum = Regiao.valueOf(regiao);

        Produto p = TestUtils.produto(
                "Produto Teste Região",
                "50.00",
                "6.0",
                "1", "1", "1",
                false,
                TipoProduto.ELETRONICO
        );

        ItemCompra i = TestUtils.item(p, 1);
        CarrinhoDeCompras carrinho = TestUtils.carrinho(i);

        BigDecimal total = compraService.calcularCustoTotal(carrinho, regiaoEnum, TipoCliente.BRONZE);

        assertThat(total).as(cenario).isEqualByComparingTo(totalEsperado);
    }

    /**
     * Testa as partições do Benefício de Nível do Cliente sobre o frete.
     * P1-P3: Ouro (100%), Prata (50%), Bronze (0%) .
     * - Para isolar, usamos um frete base fixo de R$ 24,00 (Peso 6kg, Região SUDESTE).
     * - Subtotal abaixo de R$ 500.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(
            resources = "/ecommerce/service/particoes_beneficio_cliente.csv",
            numLinesToSkip = 1
    )
    @DisplayName("Partições: Benefício de Nível do Cliente (Desconto Frete)")
    void quandoNivelClienteVaria_entaoAplicaDescontoFrete(String tipoCliente, String totalEsperado, String cenario) {

        TipoCliente tipoClienteEnum = TipoCliente.valueOf(tipoCliente);

        Produto p = TestUtils.produto(
                "Produto Teste Cliente",
                "50.00",
                "6.0",
                "1", "1", "1",
                false,
                TipoProduto.ELETRONICO
        );

        ItemCompra i = TestUtils.item(p, 1);
        CarrinhoDeCompras carrinho = TestUtils.carrinho(i);

        BigDecimal total = compraService.calcularCustoTotal(carrinho, Regiao.SUDESTE, tipoClienteEnum);

        assertThat(total).as(cenario).isEqualByComparingTo(totalEsperado);
    }

    // --- TESTES DE ROBUSTEZ ---

    /**
     * Teste de Robustez P1: Quantidade <= 0
     * Verifica se o sistema lança uma exceção quando a quantidade
     * de um item no carrinho é zero ou negativa.
     */
    @ParameterizedTest(name = "Robustez P1: Quantidade = {0} (inválida)")
    @ValueSource(ints = {0, -1})
    @DisplayName("Robustez P1: Lança exceção se Quantidade for zero ou negativa")
    void quandoItemComQuantidadeInvalida_entaoLancaExcecao(int quantidadeInvalida) {
        Produto p = TestUtils.produtoPadrao();
        ItemCompra itemInvalido = TestUtils.item(p, quantidadeInvalida);
        CarrinhoDeCompras carrinho = TestUtils.carrinho(itemInvalido);

        Regiao regiao = Regiao.SUDESTE;
        TipoCliente cliente = TipoCliente.BRONZE;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            compraService.calcularCustoTotal(carrinho, regiao, cliente);
        }, "Deveria lançar exceção para quantidade <= 0");

        String nomeProduto = p.getNome();
        assertThat(exception.getMessage()).as("Mensagem de erro deve ser clara")
                .contains("Quantidade inválida no produto: " + nomeProduto);
    }
}