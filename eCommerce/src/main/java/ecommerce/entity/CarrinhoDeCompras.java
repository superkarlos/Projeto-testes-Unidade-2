package ecommerce.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class CarrinhoDeCompras
{

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne 
	@JoinColumn(name = "cliente_id")
	private Cliente cliente;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true) 
	@JoinColumn(name = "carrinho_id") 
	private List<ItemCompra> itens = new ArrayList<>();

	private LocalDate data;

	public CarrinhoDeCompras()
	{
	}

	public CarrinhoDeCompras(Long id, Cliente cliente, List<ItemCompra> itens, LocalDate data)
	{
		this.id = id;
		this.cliente = cliente;
		this.itens = itens;
		this.data = data;
	}

	// Getters e Setters
	public Long getId()
	{
		return id;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	public Cliente getCliente()
	{
		return cliente;
	}

	public void setCliente(Cliente cliente)
	{
		this.cliente = cliente;
	}

	public List<ItemCompra> getItens()
	{
		return itens;
	}

	public void setItens(List<ItemCompra> itens)
	{
		this.itens = itens;
	}

	public LocalDate getData()
	{
		return data;
	}

	public void setData(LocalDate data)
	{
		this.data = data;
	}
}
