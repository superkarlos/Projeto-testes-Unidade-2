package ecommerce.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.TipoProduto;

public class TestUtils {

    public static Produto produto(String nome, String preco, String pesoKg,
                                  String c, String l, String a, Boolean fragil, TipoProduto tipo) {
        Produto p = new Produto();
        p.setNome(nome);
        p.setPreco(new BigDecimal(preco));
        p.setPesoFisico(new BigDecimal(pesoKg));
        p.setComprimento(new BigDecimal(c));
        p.setLargura(new BigDecimal(l));
        p.setAltura(new BigDecimal(a));
        p.setFragil(fragil);
        p.setTipo(tipo);
        return p;
    }

    public static ItemCompra item(Produto p, long qtd) {
        ItemCompra i = new ItemCompra();
        i.setProduto(p);
        i.setQuantidade(qtd);
        return i;
    }

    public static CarrinhoDeCompras carrinho(ItemCompra... itens) {
        CarrinhoDeCompras c = new CarrinhoDeCompras();
        c.setData(LocalDate.now());
        c.setItens(Arrays.asList(itens));
        return c;
    }
}
