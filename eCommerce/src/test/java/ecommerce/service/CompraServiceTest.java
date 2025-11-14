package ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import ecommerce.dto.CompraDTO;
import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.dto.PagamentoDTO;
import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;
import ecommerce.entity.TipoProduto;
import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CompraServiceTest {

    @Mock
    private CarrinhoDeComprasService carrinhoService;

    @Mock
    private ClienteService clienteService;

    @Mock
    private IEstoqueExternal estoqueExternal;

    @Mock
    private IPagamentoExternal pagamentoExternal;

    @InjectMocks
    private CompraService compraService;

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
    void deveFinalizarCompraComSucesso() {

        Long carrinhoId = 1L;
        Long clienteId = 2L;

        Cliente cliente = new Cliente();

        cliente.setId(clienteId);
        cliente.setRegiao(Regiao.SUDESTE);
        cliente.setTipo(TipoCliente.BRONZE);

        Produto produto = criarProduto("Calular", PRECO_BASE, TipoProduto.ELETRONICO, true);

        ItemCompra item = new ItemCompra();
        item.setProduto(produto);
        item.setQuantidade(2L);

        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(List.of(item));
        
        when(clienteService.buscarPorId(clienteId))
            .thenReturn(cliente);

        when(carrinhoService.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente))
            .thenReturn(carrinho);

        when(estoqueExternal.verificarDisponibilidade(anyList(), anyList()))
                .thenReturn(new DisponibilidadeDTO(true, List.of()));

        when(pagamentoExternal.autorizarPagamento(eq(clienteId), anyDouble()))
            .thenReturn(new PagamentoDTO(true, 123L));

        when(estoqueExternal.darBaixa(anyList(), anyList()))
            .thenReturn(new EstoqueBaixaDTO(true));

        CompraDTO resultado = compraService.finalizarCompra(carrinhoId, clienteId);

        assertTrue(resultado.sucesso());
        assertEquals(123L, resultado.transacaoPagamentoId());
        verify(estoqueExternal).verificarDisponibilidade(anyList(), anyList());
        verify(pagamentoExternal).autorizarPagamento(eq(clienteId), anyDouble());
        verify(estoqueExternal).darBaixa(anyList(), anyList());
    }

    @Test
    @DisplayName("Deve lançar exceção quando o carrinho for nulo")
    void calcularCustoTotal_quandoCarrinhoNulo_entaoLancaExcecao() {

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.calcularCustoTotal(null, Regiao.SUDESTE, TipoCliente.BRONZE));

        assertThat(exception).isNotNull().as("Esperado que exista execao");
        assertThat(exception.getMessage()).isEqualTo("Carrinho não pode ser nulo")
                .as("Esperado a mensagem igual da execao");
        assertEquals("java.lang.IllegalArgumentException", exception.getClass().getName());
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

        assertThat(total).as("Soma simples de 100*1 + 100*1").isEqualByComparingTo("200.00");
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

        assertThat(total).as("Deve aplicar 5% de desconto para 3 itens do mesmo tipo")
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