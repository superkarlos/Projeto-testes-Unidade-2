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

import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

public class CompraServiceRegrasTest {

    private CompraService service;

    @BeforeEach
    void setup() {
        service = new CompraService(null, null, null, null);
    }

    /**
     * Testa as 12 regras da Tabela de Decisão para descontos combinados.
     * Isola a lógica de desconto usando frete R$ 0,00.
     */
    @ParameterizedTest(name = "[{index}] {4}")
    @CsvFileSource(
            resources = "/ecommerce/service/tabela_decisao_descontos.csv",
            numLinesToSkip = 1
    )
    @DisplayName("Tabela de Decisão: Descontos Combinados (Tipo x Valor)")
    void quandoCombinacoesDeDescontoOcorrem_entaoAplicaRegrasCorretamente(
            int qtdItens, String precoUnitario, String subtotal,
            String totalEsperado, String cenario) {

        Produto p = TestUtils.produto(
                "Produto Teste",
                precoUnitario,
                "0.5",
                "1", "1", "1",
                false,
                TipoProduto.ELETRONICO
        );

        ItemCompra i = TestUtils.item(p, qtdItens);
        CarrinhoDeCompras carrinho = TestUtils.carrinho(i);

        BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

        assertThat(total).as(cenario).isEqualByComparingTo(totalEsperado);
    }

    @Test
    @DisplayName("R01 - Acumula desconto por tipo (10%) e por valor (10%) => 5 itens mesma categoria")
    void R01_descontoTipoEValor_acumulados() {
        Produto p = TestUtils.produto("ProdA", "200.00", "1.00", "10", "10", "10", false, TipoProduto.ELETRONICO);
        ItemCompra i1 = TestUtils.item(p, 3);
        ItemCompra i2 = TestUtils.item(p, 2);
        CarrinhoDeCompras c = TestUtils.carrinho(i1, i2);

        BigDecimal total = service.calcularCustoTotal(c, Regiao.SUDESTE, TipoCliente.BRONZE);

        // subtotal=1000; desconto tipo 10% => 900; depois desconto valor 10% => 810; sem frete
        assertThat(total).as("acumula desconto tipo e por valor").isEqualByComparingTo("810.00");
    }

    // -----------------------
    // MC/DC para decisão composta:
    // Decisão: aplicaDesconto20 = subtotalAposDescontoTipo > 1000
    // Condições consideradas:
    //  C1: existeDescontoPorTipo (true/false)
    //  C2: subtotalAposDescontoTipo > 1000 (true/false)
    // Queremos casos que mostrem cada condição influencia decisão.
    // -----------------------

    @Test
    @DisplayName("MC/DC T1: C1=false, C2=false -> não aplica 20%")
    void MC_DC_T1_C1F_C2F() {
        // cria sem desconto por tipo (itens diferentes tipos), subtotal 400 => C2 false
        Produto p1 = TestUtils.produto("A", "200.00", "1", "10", "10", "10", false, TipoProduto.LIVRO);
        Produto p2 = TestUtils.produto("B", "200.00", "1", "10", "10", "10", false, TipoProduto.MOVEL);
        CarrinhoDeCompras c = TestUtils.carrinho(TestUtils.item(p1, 1), TestUtils.item(p2, 1));

        BigDecimal total = service.calcularCustoTotal(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).as("C1=false, C2=false => sem 20%").isEqualByComparingTo("400.00");
    }

    

    @Test
    @DisplayName("MC/DC T3: C1=false, C2=true -> aplica 20%")
    void MC_DC_T3_C1F_C2T() {
        // C1 false (itens de tipos diferentes), subtotal >1000 -> aplica 20%
        Produto p = TestUtils.produto("Y", "1200.00", "1", "10", "10", "10", false, TipoProduto.MOVEL);
        CarrinhoDeCompras c = TestUtils.carrinho(TestUtils.item(p, 1));

        BigDecimal total = service.calcularCustoTotal(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        BigDecimal subtotal = new BigDecimal("1200.00");
        BigDecimal esperado = subtotal.subtract(subtotal.multiply(new BigDecimal("0.20")));
        assertThat(total).as("C1=false, C2=true => aplica 20%").isEqualByComparingTo(esperado.setScale(2));
    }

   

    @Test
    @DisplayName("Robustez: carrinho nulo lança IllegalArgumentException")
    void R_robustez_carrinhoNulo() {
        assertThatThrownBy(() -> service.calcularCustoTotal(null, Regiao.SUDESTE, TipoCliente.BRONZE))
            .as("Carrinho nulo deve lançar IllegalArgumentException")
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Carrinho não pode ser nulo");
    }
}
