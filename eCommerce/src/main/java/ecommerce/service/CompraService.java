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

	//  @Transactional
	// public CompraDTO finalizarCompra(Long carrinhoId, Long clienteId) {

	// 	Cliente cliente = clienteService.buscarPorId(clienteId);
	// 	CarrinhoDeCompras carrinho = carrinhoService.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente);

	// 	List<Long> produtosIds = carrinho.getItens().stream().map(i -> i.getProduto().getId())
	// 			.collect(Collectors.toList());
	// 	List<Long> produtosQtds = carrinho.getItens().stream().map(i -> i.getQuantidade()).collect(Collectors.toList());

	// 	DisponibilidadeDTO disponibilidade = estoqueExternal.verificarDisponibilidade(produtosIds, produtosQtds);

	// 	if (!disponibilidade.disponivel()) {
	// 		throw new IllegalStateException("Itens fora de estoque.");
	// 	}

	// 	BigDecimal custoTotal = calcularCustoTotal(carrinho, cliente.getRegiao(), cliente.getTipo());

	// 	PagamentoDTO pagamento = pagamentoExternal.autorizarPagamento(cliente.getId(), custoTotal.doubleValue());

	// 	if (!pagamento.autorizado()) {
	// 		throw new IllegalStateException("Pagamento não autorizado.");
	// 	}

	// 	EstoqueBaixaDTO baixaDTO = estoqueExternal.darBaixa(produtosIds, produtosQtds);

	// 	if (!baixaDTO.sucesso()) {
	// 		pagamentoExternal.cancelarPagamento(cliente.getId(), pagamento.transacaoId());
	// 		throw new IllegalStateException("Erro ao dar baixa no estoque.");
	// 	}

	// 	CompraDTO compraDTO = new CompraDTO(true, pagamento.transacaoId(), "Compra finalizada com sucesso.");

	// 	return compraDTO;
	// } 
    
	public BigDecimal calcularCustoTotal(CarrinhoDeCompras carrinho, Regiao regiao, TipoCliente tipoCliente) {

		validarEntradas(carrinho, regiao, tipoCliente);
		List<ItemCompra> itensCarrinho = carrinho.getItens();
		validarItens(itensCarrinho);
	
		BigDecimal subtotalGeral = calcularSubtotal(itensCarrinho);
		BigDecimal descontoPorTipo = calcularDescontoPorTipo(itensCarrinho);
		BigDecimal subtotalComDescontoPorTipo = subtotalGeral.subtract(descontoPorTipo);
		BigDecimal descontoPorValorTotal = calcularDescontoPorValor(subtotalComDescontoPorTipo);
		BigDecimal subtotalFinal = subtotalComDescontoPorTipo.subtract(descontoPorValorTotal);
		BigDecimal pesoTotal = calcularPesoTotal(itensCarrinho);
		BigDecimal valorFrete = calcularFrete(pesoTotal);
		BigDecimal taxaFragilidade = calcularTaxaDeProdutosFrageis(itensCarrinho);
		valorFrete = valorFrete.add(taxaFragilidade);
		BigDecimal freteComMultiplicador = aplicarMultiplicadorDeRegiao(valorFrete, regiao);
		BigDecimal freteFinal = aplicarDescontoPorTipoCliente(freteComMultiplicador, tipoCliente);
	
		return subtotalFinal.add(freteFinal).setScale(2, RoundingMode.HALF_UP);
	}
	

	private void validarEntradas(CarrinhoDeCompras carrinho, Regiao regiao, TipoCliente tipoCliente) {
		if (carrinho == null) {
			throw new IllegalArgumentException("Carrinho não pode ser nulo");
		}
        if (carrinho.getItens() == null || carrinho.getItens().isEmpty()) {
            throw new IllegalArgumentException("Carrinho não pode estar vazio");
        }
		if (regiao == null) {
			throw new IllegalArgumentException("Região não pode ser nula");
		}
		if (tipoCliente == null) {
			throw new IllegalArgumentException("Tipo de cliente não pode ser nulo");
		}
	}
	
	private void validarItens(List<ItemCompra> itensCarrinho) {
		for (ItemCompra item : itensCarrinho) {
			if (item == null || item.getProduto() == null) {
				throw new IllegalArgumentException("Item de compra ou produto não pode ser nulo");
			}

            Produto p = item.getProduto();
	
			if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
				throw new IllegalArgumentException("Quantidade inválida no produto: " + item.getProduto().getNome());
			}
	
			if (p.getPreco() == null || p.getPreco().compareTo(BigDecimal.ZERO) < 0) {
				throw new IllegalArgumentException("Preço inválido no produto: " + item.getProduto().getNome());
			}

            if (p.getTipo() == null) {
                throw new IllegalArgumentException("Tipo do produto não pode ser nulo: " + p.getNome());
            }

            if (p.getComprimento() == null || p.getLargura() == null || p.getAltura() == null ||
                    p.getComprimento().compareTo(BigDecimal.ZERO) <= 0 ||
                    p.getLargura().compareTo(BigDecimal.ZERO) <= 0 ||
                    p.getAltura().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Dimensões inválidas (devem ser > 0) no produto: " + p.getNome());
            }

            if (p.getPesoFisico() == null || p.getPesoFisico().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Peso físico inválido (deve ser > 0) no produto: " + p.getNome());
            }
		}
	}
	
	public BigDecimal calcularSubtotal(List<ItemCompra> itensCarrinho) {

		BigDecimal subtotal = BigDecimal.ZERO;
	
		for (ItemCompra item : itensCarrinho) {
			Produto produto = item.getProduto();
			BigDecimal precoProduto = produto.getPreco();
			BigDecimal quantidadeItem = BigDecimal.valueOf(item.getQuantidade());
			subtotal = subtotal.add(precoProduto.multiply(quantidadeItem));
		}
	
		return subtotal;
	}
	
	private BigDecimal calcularDescontoPorTipo(List<ItemCompra> itensCarrinho) {

		Map<TipoProduto, Long> quantidadePorTipo = new HashMap<>();

		for (ItemCompra item : itensCarrinho) {
			Produto produto = item.getProduto();
			long quantidade = item.getQuantidade();
            TipoProduto tipoProduto = produto.getTipo();

            quantidadePorTipo.put(tipoProduto, quantidadePorTipo.getOrDefault(tipoProduto, 0L)+ quantidade);
		}
	
		BigDecimal totalDesconto = BigDecimal.ZERO;
	
		for (Map.Entry<TipoProduto, Long> entrada : quantidadePorTipo.entrySet()) {
			BigDecimal percentualDesconto = calcularPercentualDescontoPorQuantidade(entrada.getValue());
			if (percentualDesconto.compareTo(BigDecimal.ZERO) > 0) {
				BigDecimal subtotalTipo = calcularSubtotalPorTipo(itensCarrinho, entrada.getKey());
				totalDesconto = totalDesconto.add(subtotalTipo.multiply(percentualDesconto));
			}
		}
	
		return totalDesconto;
	}
	
	private BigDecimal calcularPercentualDescontoPorQuantidade(Long quantidade) {
		if (quantidade >= 3 && quantidade <= 4) return new BigDecimal("0.05");
		if (quantidade >= 5 && quantidade <= 7) return new BigDecimal("0.10");
		if (quantidade >= 8) return new BigDecimal("0.15");
		return BigDecimal.ZERO;
	}
	
	private BigDecimal calcularSubtotalPorTipo(List<ItemCompra> itensCarrinho, TipoProduto tipoProduto) {
		BigDecimal subtotalTipo = BigDecimal.ZERO;
	
		for (ItemCompra item : itensCarrinho) {
			Produto produto = item.getProduto();
			if (produto.getTipo() == tipoProduto) {
				BigDecimal precoProduto = produto.getPreco();
				BigDecimal quantidadeItem = BigDecimal.valueOf(item.getQuantidade());
				subtotalTipo = subtotalTipo.add(precoProduto.multiply(quantidadeItem));
			}
		}
	
		return subtotalTipo;
	}
	
	private BigDecimal calcularDescontoPorValor(BigDecimal subtotal) {
		if (subtotal.compareTo(new BigDecimal("1000.00")) > 0) {
			return subtotal.multiply(new BigDecimal("0.20"));
		} else if (subtotal.compareTo(new BigDecimal("500.00")) > 0) {
			return subtotal.multiply(new BigDecimal("0.10"));
		}
		return BigDecimal.ZERO;
	}
	
	private BigDecimal calcularPesoTotal(List<ItemCompra> itensCarrinho) {
		BigDecimal pesoTotal = BigDecimal.ZERO;
	
		for (ItemCompra item : itensCarrinho) {
			Produto produto = item.getProduto();

			BigDecimal pesoFisico = produto.getPesoFisico();
			BigDecimal comprimento = produto.getComprimento();
			BigDecimal largura = produto.getLargura();
			BigDecimal altura = produto.getAltura();
            BigDecimal quantidade = BigDecimal.valueOf(item.getQuantidade());

			BigDecimal pesoCubico = comprimento.multiply(largura).multiply(altura).divide(new BigDecimal("6000"), 
			10, RoundingMode.HALF_UP);

			BigDecimal pesoTributavel = pesoFisico.max(pesoCubico);
	
			pesoTotal = pesoTotal.add(pesoTributavel.multiply(quantidade));
		}
	
		return pesoTotal;
	}
	
	private BigDecimal calcularFrete(BigDecimal pesoTotal) {
		if (pesoTotal.compareTo(BigDecimal.ZERO) == 0 || pesoTotal.compareTo(new BigDecimal("5.00")) <= 0)
			return BigDecimal.ZERO;
	
		if (pesoTotal.compareTo(new BigDecimal("5.00")) > 0 && pesoTotal.compareTo(new BigDecimal("10.00")) <= 0)
			return pesoTotal.multiply(new BigDecimal("2.00")).add(new BigDecimal("12.00"));
	
		if (pesoTotal.compareTo(new BigDecimal("10.00")) > 0 && pesoTotal.compareTo(new BigDecimal("50.00")) <= 0)
			return pesoTotal.multiply(new BigDecimal("4.00")).add(new BigDecimal("12.00"));
	
		return pesoTotal.multiply(new BigDecimal("7.00")).add(new BigDecimal("12.00"));
	}
	
	private BigDecimal calcularTaxaDeProdutosFrageis(List<ItemCompra> itensCarrinho) {
		BigDecimal taxaTotal = BigDecimal.ZERO;
	
		for (ItemCompra item : itensCarrinho) {
			Produto produto = item.getProduto();
			if (produto != null && Boolean.TRUE.equals(produto.isFragil())) {
				BigDecimal quantidade = BigDecimal.valueOf(item.getQuantidade() == null ? 0L : item.getQuantidade());
				taxaTotal = taxaTotal.add(new BigDecimal("5.00").multiply(quantidade));
			}
		}
	
		return taxaTotal;
	}
	
	private BigDecimal aplicarMultiplicadorDeRegiao(BigDecimal valorFrete, Regiao regiao) {
		BigDecimal multiplicador = getMultiplicadorPorRegiao(regiao);
		return valorFrete.multiply(multiplicador);
	}
	
	private BigDecimal aplicarDescontoPorTipoCliente(BigDecimal valorFrete, TipoCliente tipoCliente) {
		if (tipoCliente == TipoCliente.OURO) {
			return BigDecimal.ZERO;
		} else if (tipoCliente == TipoCliente.PRATA) {
			return valorFrete.divide(new BigDecimal("2.00"), 10, RoundingMode.HALF_UP);
		}
		return valorFrete;
	}
		
	
	private BigDecimal getMultiplicadorPorRegiao(Regiao regiao) {
   
			if (regiao == Regiao.SUL) {
				return new BigDecimal("1.05");
			} else if (regiao == Regiao.NORDESTE) {
				return new BigDecimal("1.10");
			} else if (regiao == Regiao.CENTRO_OESTE) {
				return new BigDecimal("1.20");
			} else if (regiao == Regiao.NORTE) {
				return new BigDecimal("1.30");
			} else {
				return new BigDecimal("1.00"); // por padeão ele retorna 1.00 no caso SUDESTE
			} 
    }

    
	
    
}
