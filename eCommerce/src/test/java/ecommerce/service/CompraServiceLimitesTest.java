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

    private CompraService compraService;

    @BeforeEach
    void setup() {
        compraService = new CompraService(null, null, null, null);
    }

    @Test
    @DisplayName("Limite: peso exatamente 5 kg -> frete isento")
    void pesoExato5_freteIsento() {

        Produto p = TestUtils.produto("ItemP", "10.00", "5.00", "10", "10", "10", false, TipoProduto.ELETRONICO);
        ItemCompra i = TestUtils.item(p, 1);
        CarrinhoDeCompras c = TestUtils.carrinho(i);

        BigDecimal total = compraService.calcularCustoTotal(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).as("peso 5 => frete 0").isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("Limite: subtotal exatamente 500 -> sem desconto por valor")
    void subtotal500_semDesconto() {
        Produto p = TestUtils.produto("ProdutoX", "500.00", "1.00", "10", "10", "10", false, TipoProduto.ELETRONICO);
        ItemCompra i = TestUtils.item(p, 1);
        CarrinhoDeCompras c = TestUtils.carrinho(i);

        BigDecimal total = compraService.calcularCustoTotal(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).as("subtotal 500 sem desconto por valor").isEqualByComparingTo("500.00");
    }
    
    //Falta olhar esse

    // @Test
    // @DisplayName("Limite: subtotal exatamente 1000 -> sem desconto 20%")
    // void subtotal1000_semDesconto20() {
    //     Produto p = TestUtils.produto("ProdutoY", "1000.00", "1.00", "10", "10", "10", false, TipoProduto.ELETRONICO);
    //     ItemCompra i = TestUtils.item(p, 1);
    //     CarrinhoDeCompras c = TestUtils.carrinho(i);

    //     BigDecimal total = compraService.calcularCustoTotal(c, Regiao.SUDESTE, TipoCliente.BRONZE);
    //     assertThat(total).as("subtotal 1000 sem desconto 20%").isEqualByComparingTo("1000.00");
    // }
}
