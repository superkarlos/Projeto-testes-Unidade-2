package ecommerce.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;
import ecommerce.entity.TipoProduto;
import ecommerce.util.TestUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

public class CompraServiceLimitesTest {

    private CompraService compraService;

    @BeforeEach
    void setup() {
        compraService = new CompraService(null, null, null, null);
    }

    /**
     * Testa os limites do Peso Tributável, que é quando físico = cúbico.
     * Usamos um subtotal fixo de R$ 50,00 (sem desconto por valor).
     * Usamos Regiao.SUDESTE (x1.0) e TipoCliente.BRONZE (sem desconto no frete)
     * para isolar o cálculo do frete baseado apenas no peso.
     */
    @ParameterizedTest(name = "[{index}] {5}]")
    @CsvFileSource(
            resources = "/ecommerce/service/limites_peso_tributavel.csv",
            numLinesToSkip = 1
    )
    @DisplayName("Limites: Cálculo do Peso Tributável (Físico vs Cúbico)")
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
     * Testa os limites de desconto por quantidade de itens do mesmo tipo.
     * Para isolar esta regra:
     * - O subtotal é mantido abaixo de R$ 500,00 para não ativar o desconto por valor.
     * - O peso é mantido em 0.5kg (isento) para não adicionar frete.
     * - Cliente BRONZE e Região SUDESTE são usados para não alterar o frete.
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @CsvFileSource(
            resources = "/ecommerce/service/limites_desconto_itens.csv",
            numLinesToSkip = 1
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
     * Testa os limites de desconto por valor total do carrinho .
     * Para isolar esta regra:
     * - Usamos apenas 1 item para não ativar o desconto por múltiplos itens.
     * - O peso é mantido em 1kg (isento) para não adicionar frete.
     * - Cliente BRONZE e Região SUDESTE são usados para não alterar o frete.
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @CsvFileSource(
            resources = "/ecommerce/service/limites_desconto_valor.csv",
            numLinesToSkip = 1
    )
    @DisplayName("Limites: Desconto por Valor de Carrinho")
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
            resources = "/ecommerce/service/limites_frete_peso.csv",
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
}
