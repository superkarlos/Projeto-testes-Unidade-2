package ecommerce.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
import jakarta.transaction.Transactional;

@Service
public class CompraService {

	private final CarrinhoDeComprasService carrinhoService;
	private final ClienteService clienteService;
	private final IEstoqueExternal estoqueExternal;
	private final IPagamentoExternal pagamentoExternal;

	@Autowired
	public CompraService(CarrinhoDeComprasService carrinhoService, ClienteService clienteService,
			IEstoqueExternal estoqueExternal, IPagamentoExternal pagamentoExternal) {
		this.carrinhoService = carrinhoService;
		this.clienteService = clienteService;

		this.estoqueExternal = estoqueExternal;
		this.pagamentoExternal = pagamentoExternal;
	}

	@Transactional
	public CompraDTO finalizarCompra(Long carrinhoId, Long clienteId) {

		Cliente cliente = clienteService.buscarPorId(clienteId);
		CarrinhoDeCompras carrinho = carrinhoService.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente);

		List<Long> produtosIds = carrinho.getItens().stream().map(i -> i.getProduto().getId())
				.collect(Collectors.toList());
		List<Long> produtosQtds = carrinho.getItens().stream().map(i -> i.getQuantidade()).collect(Collectors.toList());

		DisponibilidadeDTO disponibilidade = estoqueExternal.verificarDisponibilidade(produtosIds, produtosQtds);

		if (!disponibilidade.disponivel()) {
			throw new IllegalStateException("Itens fora de estoque.");
		}

		BigDecimal custoTotal = calcularCustoTotal(carrinho, cliente.getRegiao(), cliente.getTipo());

		PagamentoDTO pagamento = pagamentoExternal.autorizarPagamento(cliente.getId(), custoTotal.doubleValue());

		if (!pagamento.autorizado()) {
			throw new IllegalStateException("Pagamento não autorizado.");
		}

		EstoqueBaixaDTO baixaDTO = estoqueExternal.darBaixa(produtosIds, produtosQtds);

		if (!baixaDTO.sucesso()) {
			pagamentoExternal.cancelarPagamento(cliente.getId(), pagamento.transacaoId());
			throw new IllegalStateException("Erro ao dar baixa no estoque.");
		}

		CompraDTO compraDTO = new CompraDTO(true, pagamento.transacaoId(), "Compra finalizada com sucesso.");

		return compraDTO;
	}

	public BigDecimal calcularCustoTotal(CarrinhoDeCompras carrinho, Regiao regiao, TipoCliente tipoCliente) {

		if (carrinho == null) {
			throw new IllegalArgumentException("Carrinho não pode ser nulo");
		}
		if (regiao == null) {
			throw new IllegalArgumentException("Região não pode ser nula");
		}
		if (tipoCliente == null) {
			throw new IllegalArgumentException("Tipo de cliente não pode ser nulo");
		}

		List<ItemCompra> itens = carrinho.getItens();
		BigDecimal subtotal = BigDecimal.ZERO;

		if (itens == null || itens.isEmpty()) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
	
		for (ItemCompra item : itens) {

			Produto produto = item.getProduto();

			if (produto  == null || produto .getPreco() == null){
				continue;
			}
				
			BigDecimal quantidade = BigDecimal.valueOf(item.getQuantidade() == null ? 0L : item.getQuantidade());
			BigDecimal valorItem = produto .getPreco().multiply(quantidade);

			subtotal = subtotal.add(valorItem);
		}

		Map<TipoProduto, Long> qtdPorTipo = new HashMap<>();

		for (ItemCompra item : itens) {
			Produto produto = item.getProduto();

			if (produto  == null || produto .getTipo() == null){
				continue;
			}
				
			TipoProduto tipo = produto.getTipo();

			long quantidade = item.getQuantidade() == null ? 0L : item.getQuantidade();
			qtdPorTipo.put(tipo, qtdPorTipo.getOrDefault(tipo, 0L) + quantidade);
		}

		BigDecimal descontoPorTipoTotal = BigDecimal.ZERO;

		for (Map.Entry<TipoProduto, Long> entry : qtdPorTipo.entrySet()) {

			long quantidade = entry.getValue();
			BigDecimal percentual = BigDecimal.ZERO;
			
			if (quantidade >= 3 && quantidade  <= 4)
				percentual = new BigDecimal("0.05"); // 5%
			else if (quantidade  >= 5 && quantidade  <= 7)
				percentual = new BigDecimal("0.10"); // 10%
			else if (quantidade  >= 8)
				percentual = new BigDecimal("0.15"); // 15%

			if (percentual.compareTo(BigDecimal.ZERO) > 0) {
		
				BigDecimal subtotalTipo = BigDecimal.ZERO;

				for (ItemCompra item : itens) {
					Produto produto = item.getProduto();

					if (produto != null && produto.getTipo() == entry.getKey()) {
						BigDecimal qtdItem = BigDecimal
								.valueOf(item.getQuantidade() == null ? 0L : item.getQuantidade());
						subtotalTipo = subtotalTipo.add(produto.getPreco().multiply(qtdItem));
					}
				}

				descontoPorTipoTotal = descontoPorTipoTotal.add(subtotalTipo.multiply(percentual));
			}
		}

		BigDecimal subtotalAposDescontoTipo = subtotal.subtract(descontoPorTipoTotal);
		BigDecimal descontoPorValor = BigDecimal.ZERO;

		if (subtotalAposDescontoTipo.compareTo(new BigDecimal("1000.00")) > 0) {
			descontoPorValor = subtotalAposDescontoTipo.multiply(new BigDecimal("0.20")); // 20%
		} 
		else if (subtotalAposDescontoTipo.compareTo(new BigDecimal("500.00")) > 0) {
			descontoPorValor = subtotalAposDescontoTipo.multiply(new BigDecimal("0.10")); // 10%
		}

		BigDecimal subtotalFinal = subtotalAposDescontoTipo.subtract(descontoPorValor);
		BigDecimal pesoTotal = BigDecimal.ZERO;

		for (ItemCompra item : itens) {
			Produto p = item.getProduto();
			if (p == null){

			}
				
			BigDecimal pesoFisico = p.getPesoFisico() == null ? BigDecimal.ZERO : p.getPesoFisico(); // kg
			BigDecimal c = p.getComprimento() == null ? BigDecimal.ZERO : p.getComprimento();
			BigDecimal l = p.getLargura() == null ? BigDecimal.ZERO : p.getLargura();
			BigDecimal a = p.getAltura() == null ? BigDecimal.ZERO : p.getAltura();

			// peso cubico = (C * L * A) / 6000
			BigDecimal pesoCubico = c.multiply(l).multiply(a).divide(new BigDecimal("6000"), 10, RoundingMode.HALF_UP);

			BigDecimal pesoTributavel = pesoFisico.max(pesoCubico);
			BigDecimal qtd = BigDecimal.valueOf(item.getQuantidade() == null ? 0L : item.getQuantidade());
			pesoTotal = pesoTotal.add(pesoTributavel.multiply(qtd));
		}

	
		BigDecimal frete = BigDecimal.ZERO;
		if (pesoTotal.compareTo(new BigDecimal("0.00")) == 0 || pesoTotal.compareTo(new BigDecimal("5.00")) <= 0) {
			frete = BigDecimal.ZERO;
		}
		
		else if (pesoTotal.compareTo(new BigDecimal("5.00")) > 0 && pesoTotal.compareTo(new BigDecimal("10.00")) <= 0) {
			frete = pesoTotal.multiply(new BigDecimal("2.00")).add(new BigDecimal("12.00"));
		}
	
		else if (pesoTotal.compareTo(new BigDecimal("10.00")) > 0
				&& pesoTotal.compareTo(new BigDecimal("50.00")) <= 0) {
			frete = pesoTotal.multiply(new BigDecimal("4.00")).add(new BigDecimal("12.00"));
		}
		
		else {
			frete = pesoTotal.multiply(new BigDecimal("7.00")).add(new BigDecimal("12.00"));
		}


		BigDecimal taxaFragil = BigDecimal.ZERO;
		for (ItemCompra item : itens) {
			Produto produto = item.getProduto();
			if (produto != null && Boolean.TRUE.equals(produto.isFragil())) {
				BigDecimal qtd = BigDecimal.valueOf(item.getQuantidade() == null ? 0L : item.getQuantidade());
				taxaFragil = taxaFragil.add(new BigDecimal("5.00").multiply(qtd));
			}
		}

		frete = frete.add(taxaFragil);
		BigDecimal multiplicador = getMultiplicadorPorRegiao(regiao);
		frete = frete.multiply(multiplicador);

		
		BigDecimal freteFinal = frete;
		if (tipoCliente == TipoCliente.OURO) {
			freteFinal = BigDecimal.ZERO;
		} else if (tipoCliente == TipoCliente.PRATA) {
			freteFinal = frete.divide(new BigDecimal("2.00"), 10, RoundingMode.HALF_UP);
		}
		
		BigDecimal total = subtotalFinal.add(freteFinal).setScale(2, RoundingMode.HALF_UP);
		return total;
	}

	private BigDecimal getMultiplicadorPorRegiao(Regiao regiao) {
		switch (regiao) {
			case SUDESTE:
				return new BigDecimal("1.00");
			case SUL:
				return new BigDecimal("1.05");
			case NORDESTE:
				return new BigDecimal("1.10");
			case CENTRO_OESTE:
				return new BigDecimal("1.20");
			case NORTE:
				return new BigDecimal("1.30");
			default:
				return new BigDecimal("1.00");
		}
	}
	
}
