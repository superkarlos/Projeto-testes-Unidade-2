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
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

public class CompraServiceParticoesTest {

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
    @CsvSource({
            // os parâmetros são respectivamente peso, C,  L,  A, totalEsperado, cenario

            // P1: Físico > Cúbico
            // Peso Cúbico = (10*10*60)/6000 = 1.0kg
            // Peso Tributável = max(6.0, 1.0) = 6.0kg (Faixa B: 6*2 + 12 = 24.00)
            // Total = 50.00 (subtotal) + 24.00 (frete) = 74.00
            "'6.0', '10', '10', '60', '74.00', 'P1: Peso Físico (6kg) > Cúbico (1kg)'",

            // P2: Cúbico > Físico
            // Peso Cúbico = (50*30*44)/6000 = 11.0kg
            // Peso Tributável = max(2.0, 11.0) = 11.0kg (Faixa C: 11*4 + 12 = 56.00)
            // Total = 50.00 (subtotal) + 56.00 (frete) = 106.00
            "'2.0', '50', '30', '44', '106.00', 'P2: Peso Cúbico (11kg) > Físico (2kg)'",

            // P2.1: Físico == Cúbico (Limite)
            // Peso Cúbico = (20*20*60)/6000 = 4.0kg
            // Peso Tributável = max(4.0, 4.0) = 4.0kg (Faixa A: Isento)
            // Total = 50.00 (subtotal) + 0.00 (frete) = 50.00
            "'4.0', '20', '20', '60', '50.00', 'P2.1: Peso Cúbico (4kg) == Físico (4kg)'"
    })
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

    @Test
    @DisplayName("Partição: peso <= 5 => frete isento")
    void quandoPesoMenorOuIgual5_entaoFreteIsento() {
        Produto p = TestUtils.produto("Livro", "100.00", "1.00", "10", "10", "10", false, TipoProduto.LIVRO);
        ItemCompra i = TestUtils.item(p, 1);
        CarrinhoDeCompras carrinho = TestUtils.carrinho(i);

        BigDecimal total = compraService.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

        assertThat(total).as("subtotal 100 + frete 0").isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Partição: item frágil adiciona taxa por unidade")
    void quandoItemFragil_entaoTaxaFragilAplicada() {
        Produto p = TestUtils.produto("Vaso", "50.00", "6.00", "30", "20", "10", true, TipoProduto.MOVEL);
        ItemCompra i = TestUtils.item(p, 1);
        CarrinhoDeCompras carrinho = TestUtils.carrinho(i);

        BigDecimal total = compraService.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

        // calcular esperado manualmente:
        // subtotal = 50
        // pesoTrib >= pesoFisico(6) -> faixa 5<peso<=10 => frete = 6*2 + 12 = 24
        // taxa fragil = 5*1 = 5 -> frete = 29
        // multiplicador SUDESTE = 1.0 -> freteFinal = 29
        // total = 50 + 29 = 79.00
        assertThat(total).as("sub+frete(frágil)").isEqualByComparingTo("79.00");
    }

    @Test
    @DisplayName("Partição: cliente Ouro paga subtotal apenas (frete zerado)")
    void quandoClienteOuro_entaoFreteZerado() {
        Produto p = TestUtils.produto("Sofa", "500.00", "20.00", "100", "200", "50", false, TipoProduto.MOVEL);
        ItemCompra i = TestUtils.item(p, 1);
        CarrinhoDeCompras carrinho = TestUtils.carrinho(i);

        BigDecimal total = compraService.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.OURO);
        assertThat(total).as("cliente ouro paga apenas subtotal").isEqualByComparingTo("500.00");
    }
}