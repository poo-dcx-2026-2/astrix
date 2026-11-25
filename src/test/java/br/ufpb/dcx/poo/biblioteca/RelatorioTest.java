
package br.ufpb.dcx.poo.biblioteca;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.ufpb.dcx.poo.biblioteca.contrato.Biblioteca;

class RelatorioTest {

    private Biblioteca biblioteca;

    private static final LocalDate HOJE =
            LocalDate.of(2026, 11, 24);

    @BeforeEach
    void setup() throws Exception {
        biblioteca = Fabrica.novaBiblioteca();

        biblioteca.acervo().cadastrarItem(
                "L1", "Java Efetivo", "Bloch", "Livro", 2019);

        biblioteca.acervo().cadastrarItem(
                "L2", "Codigo Limpo", "Martin", "livro", 2009);

        biblioteca.acervo().cadastrarItem(
                "R1", "Revista de Tecnologia", "Autor", "Revista", 2020);

        biblioteca.acervo().adicionarExemplar("L1", "T-001");
        biblioteca.acervo().adicionarExemplar("L2", "T-002");
        biblioteca.acervo().adicionarExemplar("R1", "T-003");

        biblioteca.usuarios().cadastrarUsuario("U1", "Alice");
        biblioteca.usuarios().cadastrarUsuario("U2", "Bob");
    }

    // ==========================================
    // 1. EMPRESTIMOS POR CATEGORIA
    // ==========================================

    @Test
    @DisplayName("Sem emprestimos, retorna mapa vazio")
    void emprestimosPorCategoriaVazio() {
        assertEquals(
                Map.of(),
                biblioteca.relatorios().emprestimosPorCategoria()
        );
    }

    @Test
    @DisplayName("Contabiliza emprestimos por categoria")
    void contabilizarEmprestimosPorCategoria() throws Exception {
        biblioteca.emprestimos().emprestar("T-001", "U1", HOJE);
        biblioteca.emprestimos().emprestar("T-003", "U2", HOJE);

        assertEquals(
                Map.of("livro", 1L, "revista", 1L),
                biblioteca.relatorios().emprestimosPorCategoria()
        );
    }

    @Test
    @DisplayName("Agrupa categorias ignorando maiusculas")
    void agruparCategoriasIgnorandoMaiusculas() throws Exception {
        biblioteca.emprestimos().emprestar("T-001", "U1", HOJE);
        biblioteca.emprestimos().emprestar("T-002", "U2", HOJE);

        assertEquals(
                Map.of("livro", 2L),
                biblioteca.relatorios().emprestimosPorCategoria()
        );
    }

    @Test
    @DisplayName("Contabiliza emprestimos ativos e devolvidos")
    void contabilizarEmprestimosDevolvidos() throws Exception {
        biblioteca.emprestimos().emprestar("T-001", "U1", HOJE);

        biblioteca.emprestimos().devolver(
                "T-001", HOJE.plusDays(1));

        biblioteca.emprestimos().emprestar(
                "T-001", "U2", HOJE.plusDays(2));

        assertEquals(
                Map.of("livro", 2L),
                biblioteca.relatorios().emprestimosPorCategoria()
        );
    }

    // ==========================================
    // 2. TAXA DE OCUPACAO DO ACERVO
    // ==========================================

    @Test
    @DisplayName("Acervo vazio possui taxa de ocupacao zero")
    void taxaDeOcupacaoAcervoVazio() {
        Biblioteca vazia = Fabrica.novaBiblioteca();

        assertEquals(
                0.0,
                vazia.relatorios().taxaDeOcupacaoDoAcervo(),
                0.0001
        );
    }

    @Test
    @DisplayName("Todos os exemplares disponiveis: taxa zero")
    void taxaSemEmprestimos() {
        assertEquals(
                0.0,
                biblioteca.relatorios().taxaDeOcupacaoDoAcervo(),
                0.0001
        );
    }

    @Test
    @DisplayName("Calcula proporcao de exemplares emprestados")
    void calcularTaxaDeOcupacao() throws Exception {
        biblioteca.emprestimos().emprestar("T-001", "U1", HOJE);

        assertEquals(
                1.0 / 3.0,
                biblioteca.relatorios().taxaDeOcupacaoDoAcervo(),
                0.0001
        );
    }

    @Test
    @DisplayName("Exemplares baixados nao entram no calculo")
    void taxaDesconsideraExemplaresBaixados() throws Exception {
        biblioteca.emprestimos().emprestar("T-001", "U1", HOJE);
        biblioteca.acervo().baixarExemplar("T-003");

        assertEquals(
                0.5,
                biblioteca.relatorios().taxaDeOcupacaoDoAcervo(),
                0.0001
        );
    }
    @Test
    @DisplayName("Todos os exemplares baixados: taxa zero")
    void taxaComTodosExemplaresBaixados() throws Exception {

        biblioteca.acervo().baixarExemplar("T-001");
        biblioteca.acervo().baixarExemplar("T-002");
        biblioteca.acervo().baixarExemplar("T-003");

        assertEquals(
                0.0,
                biblioteca.relatorios().taxaDeOcupacaoDoAcervo(),
                0.0001
        );
    }

    @Test
    @DisplayName("Preserva historico de emprestimos de exemplares baixados")
    void contabilizarHistoricoDeExemplarBaixado() throws Exception {

        biblioteca.emprestimos().emprestar("T-001", "U1", HOJE);

        biblioteca.emprestimos().devolver(
                "T-001", HOJE.plusDays(1));

        biblioteca.acervo().baixarExemplar("T-001");

        assertEquals(
                Map.of("livro", 1L),
                biblioteca.relatorios().emprestimosPorCategoria()
        );
    }
}

