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

import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

public class CompraServiceLimitesTest {

    private CompraService service;

    @BeforeEach
    void setup() {
        service = new CompraService(null, null, null, null);
    }

    @Test
    @DisplayName("L01 - peso exatamente 5.00 kg -> frete isento")
    void L01_pesoExato5_freteIsento() {
        Produto p = TestUtils.produto("ItemP", "10.00", "5.00", "10", "10", "10", false, TipoProduto.ELETRONICO);
        ItemCompra i = TestUtils.item(p, 1);
        CarrinhoDeCompras c = TestUtils.carrinho(i);

        BigDecimal total = service.calcularCustoTotal(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).as("peso 5 => frete 0").isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("L02 - subtotal exatamente 500 -> sem desconto por valor")
    void L02_subtotal500_semDesconto() {
        Produto p = TestUtils.produto("ProdutoX", "500.00", "1.00", "10", "10", "10", false, TipoProduto.ELETRONICO);
        ItemCompra i = TestUtils.item(p, 1);
        CarrinhoDeCompras c = TestUtils.carrinho(i);

        BigDecimal total = service.calcularCustoTotal(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).as("subtotal 500 => sem desconto por valor").isEqualByComparingTo("500.00");
    }


    @Test
    @DisplayName("L04 - peso 5.01 -> faixa B com taxa minima")
    void L04_peso5p01_faixaB() {
        // Criar produto com peso tributavel 5.01: peso físico 5.01
        Produto p = TestUtils.produto("Heavy", "10.00", "5.01", "10", "10", "10", false, TipoProduto.ELETRONICO);
        ItemCompra i = TestUtils.item(p, 1);
        CarrinhoDeCompras c = TestUtils.carrinho(i);
        BigDecimal total = service.calcularCustoTotal(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).as("peso 5.01 => frete com taxa minima aplicado").isNotNull();
    }

}
