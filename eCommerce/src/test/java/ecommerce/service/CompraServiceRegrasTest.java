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

public class CompraServiceRegrasTest {

    private CompraService compraService;

    @BeforeEach
    void setup() {
        compraService = new CompraService(null, null, null, null);
    }

    @Test
    @DisplayName("Regra: desconto por tipo + desconto por valor acumulados")
    void descontoTipoEValor_acumuladosCorretamente() {
       
        Produto p = TestUtils.produto("ProdA", "200.00", "1.00", "10", "10", "10", false, TipoProduto.ELETRONICO);
        ItemCompra i1 = TestUtils.item(p, 3);
        ItemCompra i2 = TestUtils.item(p, 2);
        CarrinhoDeCompras c = TestUtils.carrinho(i1, i2);

        BigDecimal total = compraService.calcularCustoTotal(c, Regiao.SUDESTE, TipoCliente.BRONZE);
        assertThat(total).as("acumula desconto por tipo e por valor").isEqualByComparingTo("810.00");
    }

    @Test
    @DisplayName("Entrada inválida: carrinho nulo lança IllegalArgumentException")
    void carrinhoNulo_deveLancarExcecao() {
        assertThatThrownBy(() -> compraService.calcularCustoTotal(null, Regiao.SUDESTE, TipoCliente.BRONZE))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Carrinho não pode ser nulo");
    }
}
