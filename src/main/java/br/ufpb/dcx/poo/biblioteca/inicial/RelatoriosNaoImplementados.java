
package br.ufpb.dcx.poo.biblioteca.inicial;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.stream.Collectors;

import br.ufpb.dcx.poo.biblioteca.contrato.ItemView;
import br.ufpb.dcx.poo.biblioteca.contrato.RelatorioService;
import br.ufpb.dcx.poo.biblioteca.contrato.UsuarioView;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusExemplar;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.PersistenciaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoNaoEncontradoException;

public class RelatoriosNaoImplementados implements RelatorioService {

    private final AcervoEmMemoria acervo;
    private final UsuariosEmMemoria usuarios;
    private final EmprestimosEmMemoria emprestimos;

    public RelatoriosNaoImplementados(
            AcervoEmMemoria acervo,
            UsuariosEmMemoria usuarios,
            EmprestimosEmMemoria emprestimos) {

        this.acervo = acervo;
        this.usuarios = usuarios;
        this.emprestimos = emprestimos;
    }

    @Override
    public List<ItemView> itensMaisEmprestados(int n) {
        throw new UnsupportedOperationException(
                "Entrega 3: implementar itensMaisEmprestados");
    }

    @Override
    public Map<String, Long> emprestimosPorCategoria() {

        return emprestimos.listarTodosEmprestimos()
                .stream()
                .map(e -> {
                    try {
                        return acervo.obterExemplar(e.getTombo());
                    } catch (RecursoNaoEncontradoException ex) {
                        throw new IllegalStateException(
                                "Exemplar do emprestimo nao encontrado", ex);
                    }
                })
                .map(exemplar -> exemplar.getItem().getCategoria())
                .map(categoria -> categoria.toLowerCase(Locale.ROOT))
                .collect(Collectors.groupingBy(
                        categoria -> categoria,
                        Collectors.counting()
                ));
    }

    @Override
    public List<UsuarioView> usuariosComAtraso(LocalDate data) {
        throw new UnsupportedOperationException(
                "Entrega 3: implementar usuariosComAtraso");
    }

    @Override
    public double taxaDeOcupacaoDoAcervo() {

        var exemplaresValidos = acervo.itens()
                .stream()
                .flatMap(item -> item.getExemplares().stream())
                .filter(exemplar ->
                        exemplar.getStatus() != StatusExemplar.INDISPONIVEL)
                .toList();

        if (exemplaresValidos.isEmpty()) {
            return 0.0;
        }

        long emprestados = exemplaresValidos.stream()
                .filter(exemplar ->
                        exemplar.getStatus() == StatusExemplar.EMPRESTADO)
                .count();

        return (double) emprestados / exemplaresValidos.size();
    }

    @Override
    public int importarItensEmLote(Path arquivo)
            throws PersistenciaException {

        throw new UnsupportedOperationException(
                "Entrega 3: implementar importarItensEmLote");
    }
}
