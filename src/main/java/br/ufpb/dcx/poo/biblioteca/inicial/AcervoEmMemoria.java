package br.ufpb.dcx.poo.biblioteca.inicial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.ufpb.dcx.poo.biblioteca.contrato.AcervoService;
import br.ufpb.dcx.poo.biblioteca.contrato.ExemplarView;
import br.ufpb.dcx.poo.biblioteca.contrato.ItemView;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusExemplar;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.OperacaoNaoPermitidaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoDuplicadoException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoNaoEncontradoException;
import br.ufpb.dcx.poo.biblioteca.persistencia.DadosPersistencia;

public class AcervoEmMemoria implements AcervoService {

    private final Map<String, Item> itensPorCodigo = new LinkedHashMap<>();
    private final Map<String, Exemplar> exemplaresPorTombo = new HashMap<>();
    private EmprestimosEmMemoria emprestimos;

    public void setEmprestimos(EmprestimosEmMemoria emprestimos) {
        this.emprestimos = emprestimos;
    }

    @Override
    public void cadastrarItem(String codigo, String titulo, String autoria,
                              String categoria, int ano)
            throws RecursoDuplicadoException {

        exigirTextoPreenchido(codigo, "codigo");
        exigirTextoPreenchido(titulo, "titulo");
        exigirTextoPreenchido(autoria, "autoria");
        exigirTextoPreenchido(categoria, "categoria");

        String codigoFormatado = codigo.trim();

        if (itensPorCodigo.containsKey(codigoFormatado)) {
            throw new RecursoDuplicadoException(
                    "Já existe item com o código " + codigoFormatado);
        }

        Item novoItem = new Item(codigoFormatado, titulo, autoria, categoria, ano);
        itensPorCodigo.put(codigoFormatado, novoItem);
    }

    @Override
    public ItemView buscarItem(String codigo)
            throws RecursoNaoEncontradoException {

        if (codigo == null || codigo.isBlank()) {
            throw new DadosInvalidosException(
                    "Código do item não pode ser nulo ou vazio.");
        }

        Item item = itensPorCodigo.get(codigo.trim());

        if (item == null) {
            throw new RecursoNaoEncontradoException(
                    "Item não encontrado: " + codigo);
        }

        return paraView(item);
    }

    @Override
    public List<ItemView> listarItens() {

        List<ItemView> resultado = new ArrayList<>();

        for (Item item : itensPorCodigo.values()) {
            resultado.add(paraView(item));
        }

        resultado.sort(
                Comparator.comparing(
                        ItemView::titulo,
                        String.CASE_INSENSITIVE_ORDER));

        return Collections.unmodifiableList(resultado);
    }

    @Override
    public List<ItemView> buscarPorTitulo(String trecho) {

        if (trecho == null) {
            throw new DadosInvalidosException(
                    "O trecho de busca não pode ser nulo.");
        }

        String termo = trecho.trim().toLowerCase();

        List<ItemView> resultado = new ArrayList<>();

        for (Item item : itensPorCodigo.values()) {
            if (item.getTitulo().toLowerCase().contains(termo)) {
                resultado.add(paraView(item));
            }
        }

        resultado.sort(
                Comparator.comparing(
                        ItemView::titulo,
                        String.CASE_INSENSITIVE_ORDER));

        return Collections.unmodifiableList(resultado);
    }

    @Override
    public List<ItemView> buscarPorCategoria(String categoria) {

        exigirTextoPreenchido(categoria, "categoria");

        List<ItemView> resultado = new ArrayList<>();

        for (Item item : itensPorCodigo.values()) {
            if (item.getCategoria().equalsIgnoreCase(categoria.trim())) {
                resultado.add(paraView(item));
            }
        }

        resultado.sort(
                Comparator.comparing(
                        ItemView::titulo,
                        String.CASE_INSENSITIVE_ORDER));

        return Collections.unmodifiableList(resultado);
    }

    @Override
    public void adicionarExemplar(String codigoDoItem, String tombo)
            throws RecursoNaoEncontradoException, RecursoDuplicadoException {

        exigirTextoPreenchido(codigoDoItem, "codigoDoItem");
        exigirTextoPreenchido(tombo, "tombo");

        String codigoFormatado = codigoDoItem.trim();
        String tomboFormatado = tombo.trim();

        Item item = itensPorCodigo.get(codigoFormatado);

        if (item == null) {
            throw new RecursoNaoEncontradoException(
                    "Item não encontrado: " + codigoDoItem);
        }

        if (exemplaresPorTombo.containsKey(tomboFormatado)) {
            throw new RecursoDuplicadoException(
                    "Já existe exemplar cadastrado com o tombo " + tomboFormatado);
        }

        Exemplar novoExemplar = new Exemplar(tomboFormatado, item);

        exemplaresPorTombo.put(tomboFormatado, novoExemplar);
        item.adicionarExemplar(novoExemplar);
    }

    @Override
    public List<ExemplarView> listarExemplares(String codigoDoItem)
            throws RecursoNaoEncontradoException {

        if (codigoDoItem == null || codigoDoItem.isBlank()) {
            throw new DadosInvalidosException(
                    "Código do item não pode ser nulo ou vazio.");
        }

        Item item = itensPorCodigo.get(codigoDoItem.trim());

        if (item == null) {
            throw new RecursoNaoEncontradoException(
                    "Item não encontrado: " + codigoDoItem);
        }

        List<ExemplarView> resultado = new ArrayList<>();

        for (Exemplar exemplar : item.getExemplares()) {
            resultado.add(
                    new ExemplarView(
                            exemplar.getTombo(),
                            item.getCodigo(),
                            exemplar.getStatus()));
        }

        // Ordenar por tombo (correção da Entrega 1)
        resultado.sort(Comparator.comparing(ExemplarView::tombo));

        return Collections.unmodifiableList(resultado);
    }

    @Override
    public void baixarExemplar(String tombo)
            throws RecursoNaoEncontradoException, OperacaoNaoPermitidaException {

        if (tombo == null || tombo.isBlank()) {
            throw new DadosInvalidosException(
                    "O tombo do exemplar não pode ser nulo ou vazio.");
        }

        Exemplar exemplar = exemplaresPorTombo.get(tombo.trim());

        if (exemplar == null) {
            throw new RecursoNaoEncontradoException(
                    "Exemplar não encontrado: " + tombo);
        }

        if (exemplar.getStatus() == StatusExemplar.EMPRESTADO) {
            throw new OperacaoNaoPermitidaException(
                    "Não é possível baixar um exemplar emprestado.");
        }

        if (exemplar.getStatus() == StatusExemplar.INDISPONIVEL) {
            throw new OperacaoNaoPermitidaException(
                    "Exemplar já está indisponível.");
        }

        // Se o exemplar estava RESERVADO, a reserva volta a esperar
        if (exemplar.getStatus() == StatusExemplar.RESERVADO) {
            if (emprestimos != null) {
                emprestimos.liberarExemplarSeparado(exemplar.getTombo());
            }
        }

        exemplar.setStatus(StatusExemplar.INDISPONIVEL);
    }

    private ItemView paraView(Item item) {
        return new ItemView(
                item.getCodigo(),
                item.getTitulo(),
                item.getAutoria(),
                item.getCategoria(),
                item.getAno(),
                item.getTotalDeExemplares(),
                item.getExemplaresDisponiveis());
    }

    private static void exigirTextoPreenchido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DadosInvalidosException(
                    "O campo " + campo + " é obrigatório.");
        }
    }

    Item obterItem(String codigo) throws RecursoNaoEncontradoException {
        if (codigo == null || codigo.isBlank()) {
            throw new DadosInvalidosException(
                    "Código do item não pode ser vazio.");
        }
        Item item = itensPorCodigo.get(codigo.trim());
        if (item == null) {
            throw new RecursoNaoEncontradoException(
                    "Item não encontrado: " + codigo);
        }
        return item;
    }

    Exemplar obterExemplar(String tombo)
            throws RecursoNaoEncontradoException {

        if (tombo == null || tombo.isBlank()) {
            throw new DadosInvalidosException(
                    "Tombo do exemplar não pode ser vazio.");
        }

        Exemplar exemplar = exemplaresPorTombo.get(tombo.trim());

        if (exemplar == null) {
            throw new RecursoNaoEncontradoException(
                    "Exemplar não encontrado: " + tombo);
        }

        return exemplar;
    }

    List<Item> itens() {
        return Collections.unmodifiableList(
                new ArrayList<>(itensPorCodigo.values()));
    }

    List<DadosPersistencia.ItemDTO> exportarItens() {
        List<DadosPersistencia.ItemDTO> lista = new ArrayList<>();
        for (Item item : itensPorCodigo.values()) {
            lista.add(new DadosPersistencia.ItemDTO(
                    item.getCodigo(), item.getTitulo(), item.getAutoria(),
                    item.getCategoria(), item.getAno()
            ));
        }
        return lista;
    }

    List<DadosPersistencia.ExemplarDTO> exportarExemplares() {
        List<DadosPersistencia.ExemplarDTO> lista = new ArrayList<>();
        for (Exemplar ex : exemplaresPorTombo.values()) {
            lista.add(new DadosPersistencia.ExemplarDTO(
                    ex.getTombo(), ex.getItem().getCodigo(), ex.getStatus()
            ));
        }
        return lista;
    }

    void importar(List<DadosPersistencia.ItemDTO> itens, List<DadosPersistencia.ExemplarDTO> exemplares) {
        itensPorCodigo.clear();
        exemplaresPorTombo.clear();

        for (DadosPersistencia.ItemDTO dto : itens) {
            Item item = new Item(dto.codigo(), dto.titulo(), dto.autoria(), dto.categoria(), dto.ano());
            itensPorCodigo.put(item.getCodigo(), item);
        }

        for (DadosPersistencia.ExemplarDTO dto : exemplares) {
            Item item = itensPorCodigo.get(dto.codigoDoItem());
            if (item != null) {
                Exemplar ex = new Exemplar(dto.tombo(), item);
                ex.setStatus(dto.status());
                exemplaresPorTombo.put(ex.getTombo(), ex);
                item.adicionarExemplar(ex);
            }
        }
    }
}
