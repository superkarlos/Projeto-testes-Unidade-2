package ecommerce.service;

import ecommerce.entity.*;
import ecommerce.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

public class CompraServiceEstruturalTest {

    private CompraService compraService;

    @BeforeEach
    void setup() {
        compraService = new CompraService(null, null, null, null);
    }

    /**
     * Teste de Caixa Branca: Cobertura de Ramo
     * Cobre o ramo (carrinho.getItens() == null) no método validarEntradas.
     * O teste de robustez (P4b) cobriu apenas (carrinho.getItens().isEmpty()).
     */
    @Test
    @DisplayName("Caixa Branca: Lança exceção se a LISTA de itens do carrinho for nula")
    void quandoListaDeItensNula_entaoLancaExcecao() {

        CarrinhoDeCompras carrinhoMalformado = new CarrinhoDeCompras();
        carrinhoMalformado.setItens(null);

        Regiao regiao = Regiao.SUDESTE;
        TipoCliente cliente = TipoCliente.BRONZE;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            compraService.calcularCustoTotal(carrinhoMalformado, regiao, cliente);
        }, "Deveria lançar exceção para lista de itens nula");

        assertThat(exception.getMessage())
                .as("Mensagem de erro deve bater com a implementação")
                .isEqualTo("Carrinho não pode estar vazio");
    }

    /**
     * Teste de Caixa Branca: Cobertura de Ramo
     * Cobre o ramo (item.getQuantidade() == null) no método validarItens.
     * O teste de robustez (P1) cobriu apenas (item.getQuantidade() <= 0).
     */
    @Test
    @DisplayName("Caixa Branca: Lança exceção se Quantidade do item for nula")
    void quandoQuantidadeItemNula_entaoLancaExcecao() {

        Produto p = TestUtils.produtoPadrao();
        String nomeProduto = p.getNome();

        ItemCompra itemInvalido = new ItemCompra();
        itemInvalido.setProduto(p);
        itemInvalido.setQuantidade(null);

        CarrinhoDeCompras carrinho = TestUtils.carrinho(itemInvalido);
        Regiao regiao = Regiao.SUDESTE;
        TipoCliente cliente = TipoCliente.BRONZE;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            compraService.calcularCustoTotal(carrinho, regiao, cliente);
        }, "Deveria lançar exceção para quantidade nula");

        assertThat(exception.getMessage())
                .isEqualTo("Quantidade inválida no produto: " + nomeProduto);
    }

    /**
     * Teste de Caixa Branca: Cobertura de Ramo
     * Cobre o ramo (item.getProduto().getPreco() == null) no método validarItens.
     * O teste de robustez (P1) cobriu apenas (itemgetProduto().getPreco().compareTo(BigDecimal.ZERO) < 0).
     */
    @Test
    @DisplayName("Caixa Branca: Lança exceção se o preço do item for nulo")
    void quandoPrecoItemNula_entaoLancaExcecao() {

        Produto p = TestUtils.produtoPadrao();
        p.setPreco(null);
        String nomeProduto = p.getNome();

        ItemCompra item = TestUtils.item(p, 1);
        CarrinhoDeCompras carrinho = TestUtils.carrinho(item);
        Regiao regiao = Regiao.SUDESTE;
        TipoCliente cliente = TipoCliente.BRONZE;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            compraService.calcularCustoTotal(carrinho, regiao, cliente);
        }, "Deveria lançar exceção para preço nulo");

        assertThat(exception.getMessage())
                .isEqualTo("Preço inválido no produto: " + nomeProduto);
    }

    /**
     * Teste de Caixa Branca: Cobertura de Ramo
     * Cobre o ramo (p.getPesoFisico() == null) no método validarItens.
     * O teste de robustez (P6) cobriu apenas (peso <= 0).
     */
    @Test
    @DisplayName("Caixa Branca: Lança exceção se Peso do produto for nulo")
    void quandoPesoProdutoNulo_entaoLancaExcecao() {

        Produto p = TestUtils.produtoPadrao();
        p.setPesoFisico(null);
        String nomeProduto = p.getNome();

        ItemCompra item = TestUtils.item(p, 1);
        CarrinhoDeCompras carrinho = TestUtils.carrinho(item);
        Regiao regiao = Regiao.SUDESTE;
        TipoCliente cliente = TipoCliente.BRONZE;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            compraService.calcularCustoTotal(carrinho, regiao, cliente);
        }, "Deveria lançar exceção para peso nulo");

        assertThat(exception.getMessage())
                .isEqualTo("Peso físico inválido (deve ser > 0) no produto: " + nomeProduto);
    }

    /**
     * Teste de Caixa Branca: Cobertura de Ramo
     * Cobre o ramo (p.getComprimento() == null) no método validarItens.
     * O teste de robustez (P6) cobriu apenas (peso <= 0).
     */
    @Test
    @DisplayName("Caixa Branca: Lança exceção se Comprimento do produto for nulo")
    void quandoComprimentoNulo_entaoLancaExcecao() {

        Produto p = TestUtils.produtoPadrao();
        p.setComprimento(null);
        String nomeProduto = p.getNome();

        ItemCompra item = TestUtils.item(p, 1);
        CarrinhoDeCompras carrinho = TestUtils.carrinho(item);
        Regiao regiao = Regiao.SUDESTE;
        TipoCliente cliente = TipoCliente.BRONZE;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            compraService.calcularCustoTotal(carrinho, regiao, cliente);
        }, "Deveria lançar exceção para comprimento nulo");

        assertThat(exception.getMessage())
                .isEqualTo("Dimensões inválidas (devem ser > 0) no produto: " + nomeProduto);
    }

    /**
     * Teste de Caixa Branca: Cobertura de Ramo
     * Cobre o ramo (p.getAltura() == null) no método validarItens.
     * O teste de robustez (P6) cobriu apenas (peso <= 0).
     */
    @Test
    @DisplayName("Caixa Branca: Lança exceção se a Altura do produto for nula")
    void quandoAlturaNula_entaoLancaExcecao() {

        Produto p = TestUtils.produtoPadrao();
        p.setAltura(null);
        String nomeProduto = p.getNome();

        ItemCompra item = TestUtils.item(p, 1);
        CarrinhoDeCompras carrinho = TestUtils.carrinho(item);
        Regiao regiao = Regiao.SUDESTE;
        TipoCliente cliente = TipoCliente.BRONZE;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            compraService.calcularCustoTotal(carrinho, regiao, cliente);
        }, "Deveria lançar exceção para altura nula");

        assertThat(exception.getMessage())
                .isEqualTo("Dimensões inválidas (devem ser > 0) no produto: " + nomeProduto);
    }

    /**
     * Teste de Caixa Branca: Cobertura de Ramo
     * Cobre o ramo (p.getLargura() == null) no método validarItens.
     * O teste de robustez (P6) cobriu apenas (peso <= 0).
     */
    @Test
    @DisplayName("Caixa Branca: Lança exceção se a Largura do produto for nula")
    void quandoLarguraNula_entaoLancaExcecao() {

        Produto p = TestUtils.produtoPadrao();
        p.setLargura(null);
        String nomeProduto = p.getNome();

        ItemCompra item = TestUtils.item(p, 1);
        CarrinhoDeCompras carrinho = TestUtils.carrinho(item);
        Regiao regiao = Regiao.SUDESTE;
        TipoCliente cliente = TipoCliente.BRONZE;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            compraService.calcularCustoTotal(carrinho, regiao, cliente);
        }, "Deveria lançar exceção para largura nula");

        assertThat(exception.getMessage())
                .isEqualTo("Dimensões inválidas (devem ser > 0) no produto: " + nomeProduto);
    }

    /**
     * Teste de Caixa Branca: Cobertura de Ramo
     * Cobre o ramo (produto.getTipo() != null) no método calcularDescontoPorTipo.
     * O teste de robustez (P6=9) cobriu apenas (produto != null).
     */
    @Test
    @DisplayName("Caixa Branca: Lança exceção se a Largura do produto for nula")
    void quandoTipoProdutoNulo_entaoLancaExcecao() {

        Produto p = TestUtils.produtoPadrao();
        p.setLargura(null);
        String nomeProduto = p.getNome();

        ItemCompra item = TestUtils.item(p, 1);
        CarrinhoDeCompras carrinho = TestUtils.carrinho(item);
        Regiao regiao = Regiao.SUDESTE;
        TipoCliente cliente = TipoCliente.BRONZE;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            compraService.calcularCustoTotal(carrinho, regiao, cliente);
        }, "Deveria lançar exceção para tipo de produto nulo");

        assertThat(exception.getMessage())
                .isEqualTo("Dimensões inválidas (devem ser > 0) no produto: " + nomeProduto);
    }

}
