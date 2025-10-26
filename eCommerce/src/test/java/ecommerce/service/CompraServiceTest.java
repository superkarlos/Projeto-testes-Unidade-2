package ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;
import ecommerce.entity.TipoProduto;

import org.junit.jupiter.api.*;


public class CompraServiceTest {

    private CompraService service;

 
    private static final BigDecimal PRECO_BASE = new BigDecimal("100.00");
    private static final BigDecimal DESCONTO_5 = new BigDecimal("0.05");
    private static final BigDecimal DESCONTO_10 = new BigDecimal("0.10");
    private static final BigDecimal DESCONTO_20 = new BigDecimal("0.20");

    @BeforeEach
    void setup() {
        service = new CompraService(null, null, null, null);
    }

  

    @Test
    @DisplayName("Deve lançar exceção quando o carrinho for nulo")
    void calcularCustoTotal_quandoCarrinhoNulo_entaoLancaExcecao() {
        assertThrows(IllegalArgumentException.class,
                () -> service.calcularCustoTotal(null, Regiao.SUDESTE, TipoCliente.BRONZE),
                "Carrinho não pode ser nulo");
    }

    @Test
    @DisplayName("Deve lançar exceção quando a região for nula")
    void calcularCustoTotal_quandoRegiaoNula_entaoLancaExcecao() {
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        assertThrows(IllegalArgumentException.class,
                () -> service.calcularCustoTotal(carrinho, null, TipoCliente.BRONZE),
                "Região não pode ser nula");
    }

    @Test
    @DisplayName("Deve retornar 0.00 quando o carrinho estiver vazio")
    void calcularCustoTotal_quandoCarrinhoVazio_entaoRetornaZero() {
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(List.of());

        BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

        assertThat(total)
            .as("Total esperado para carrinho vazio")
            .isEqualByComparingTo("0.00");
    }

  

    @Test
    @DisplayName("Deve calcular subtotal corretamente sem descontos")
    void calcularCustoTotal_quandoCarrinhoSemDescontos_entaoRetornaSomaSimples() {
        Produto produto1 = criarProduto("Produto A", PRECO_BASE, TipoProduto.ELETRONICO, false);
        Produto produto2 = criarProduto("Produto B", PRECO_BASE, TipoProduto.ELETRONICO, false);

        ItemCompra item1 = new ItemCompra();
        item1.setProduto(produto1);
        item1.setQuantidade(1L);

        ItemCompra item2 = new ItemCompra();
        item2.setProduto(produto2);
        item2.setQuantidade(1L);

        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(Arrays.asList(item1, item2));

        BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

        assertThat(total)
            .as("Soma simples de 100*1 + 100*1")
            .isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("Deve aplicar desconto de 5% quando houver 3 itens do mesmo tipo")
    void calcularCustoTotal_quando3ItensMesmoTipo_entaoAplicaDesconto5PorCento() {
        Produto produto = criarProduto("Produto X", PRECO_BASE, TipoProduto.ELETRONICO, false);

        ItemCompra item1 = new ItemCompra();
		item1.setProduto(produto);
        item1.setQuantidade(1L);
        ItemCompra item2 = new ItemCompra();
        item2.setProduto(produto);
        item2.setQuantidade(1L);
        ItemCompra item3 = new ItemCompra();
        item3.setProduto(produto);
        item3.setQuantidade(1L);

        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(Arrays.asList(item1, item2, item3));

        BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

        BigDecimal subtotal = PRECO_BASE.multiply(BigDecimal.valueOf(3));
        BigDecimal esperado = subtotal.subtract(subtotal.multiply(DESCONTO_5));

        assertThat(total)
            .as("Deve aplicar 5% de desconto para 3 itens do mesmo tipo")
            .isEqualByComparingTo(esperado.setScale(2, BigDecimal.ROUND_HALF_UP));
    }

    @Test
    @DisplayName("Deve aplicar desconto de 20% quando o subtotal for maior que 1000.00")
    void calcularCustoTotal_quandoSubtotalMaiorQue1000_entaoAplicaDesconto20PorCento() {
        Produto produto = criarProduto("Produto Y", new BigDecimal("600.00"), TipoProduto.MOVEL, false);

        ItemCompra item1 = new ItemCompra(); // total = 1200
        item1.setProduto(produto);
		item1.setQuantidade(2L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(List.of(item1));

        BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

        BigDecimal subtotal = new BigDecimal("1200.00");
        BigDecimal esperado = subtotal.subtract(subtotal.multiply(DESCONTO_20));

        assertThat(total)
            .as("Deve aplicar 20% de desconto no subtotal acima de 1000")
            .isEqualByComparingTo(esperado.setScale(2));
    }



    @Test
    @DisplayName("Deve aplicar frete zero para clientes Ouro")
    void calcularCustoTotal_quandoClienteOuro_entaoFreteZero() {
        Produto produto = criarProduto("Produto Frete", PRECO_BASE, TipoProduto.MOVEL, false);
        produto.setPesoFisico(new BigDecimal("20.00"));

        ItemCompra item = new ItemCompra();
	    item.setProduto(produto);
		item.setQuantidade(1L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(List.of(item));

        BigDecimal total = service.calcularCustoTotal(carrinho, Regiao.NORDESTE, TipoCliente.OURO);

        BigDecimal subtotal = PRECO_BASE;
        assertThat(total)
            .as("Cliente Ouro tem 100% de desconto no frete")
            .isEqualByComparingTo(subtotal.setScale(2));
    }

    @Test
    @DisplayName("Deve aplicar multiplicador de região corretamente (Nordeste = 1.10)")
    void calcularCustoTotal_quandoRegiaoNordeste_entaoMultiplicaFretePor110() {
        Produto produto = criarProduto("Produto Frete", PRECO_BASE, TipoProduto.MOVEL, false);
        produto.setPesoFisico(new BigDecimal("12.00"));

        ItemCompra item = new ItemCompra();
        item.setProduto(produto);
        item.setQuantidade(1L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(List.of(item));

        BigDecimal totalNordeste = service.calcularCustoTotal(carrinho, Regiao.NORDESTE, TipoCliente.BRONZE);
        BigDecimal totalSudeste = service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE);

        assertThat(totalNordeste)
            .as("Frete Nordeste deve ser maior que Sudeste devido ao multiplicador de 1.10")
            .isGreaterThan(totalSudeste);
    }

 

    private Produto criarProduto(String nome, BigDecimal preco, TipoProduto tipo, boolean fragil) {
        Produto p = new Produto();
        p.setNome(nome);
        p.setPreco(preco);
        p.setTipo(tipo);
        p.setFragil(fragil);
        p.setPesoFisico(new BigDecimal("1.00"));
        p.setAltura(new BigDecimal("10"));
        p.setLargura(new BigDecimal("10"));
        p.setComprimento(new BigDecimal("10"));
        return p;
    }
}