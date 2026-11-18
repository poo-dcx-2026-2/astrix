package br.ufpb.dcx.poo.biblioteca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.ufpb.dcx.poo.biblioteca.contrato.Biblioteca;
import br.ufpb.dcx.poo.biblioteca.contrato.UsuarioView;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.BibliotecaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoDuplicadoException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoNaoEncontradoException;

/** Testes unitários do gerenciamento de usuários. */
class UsuarioTest {

    private Biblioteca biblioteca;

    @BeforeEach
    void criarBibliotecaVazia() {
        biblioteca = Fabrica.novaBiblioteca();
    }

    @Test
    @DisplayName("usuário cadastrado começa ativo e sem empréstimos")
    void cadastrarEBuscar() throws BibliotecaException {
        biblioteca.usuarios().cadastrarUsuario("2026001", "Ana Souza");

        UsuarioView usuario = biblioteca.usuarios().buscarUsuario("2026001");

        assertEquals("Ana Souza", usuario.nome());
        assertTrue(usuario.ativo());
        assertEquals(0, usuario.emprestimosAtivos());
    }

    @Test
    @DisplayName("matrícula repetida é recusada")
    void matriculaDuplicada() throws BibliotecaException {
        biblioteca.usuarios().cadastrarUsuario("2026001", "Ana Souza");

        assertThrows(RecursoDuplicadoException.class,
                () -> biblioteca.usuarios().cadastrarUsuario("2026001", "Outro Nome"));
    }

    @Test
    @DisplayName("buscar usuário inexistente lança RecursoNaoEncontradoException")
    void buscarInexistente() {
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.usuarios().buscarUsuario("0000"));
    }

    @Test
    @DisplayName("nome em branco é entrada inválida")
    void nomeEmBranco() {
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.usuarios().cadastrarUsuario("2026001", "   "));
    }

    @Test
    @DisplayName("matrícula em branco é entrada inválida")
    void matriculaEmBranco() {
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.usuarios().cadastrarUsuario("   ", "Carlos"));
    }

    @Test
    @DisplayName("listar devolve os usuários ordenados por nome")
    void listarOrdenado() throws BibliotecaException {
        biblioteca.usuarios().cadastrarUsuario("3", "Carlos");
        biblioteca.usuarios().cadastrarUsuario("1", "Ana");
        biblioteca.usuarios().cadastrarUsuario("2", "Bruno");

        List<UsuarioView> usuarios = biblioteca.usuarios().listarUsuarios();

        assertEquals(3, usuarios.size());
        assertEquals("Ana", usuarios.get(0).nome());
        assertEquals("Bruno", usuarios.get(1).nome());
        assertEquals("Carlos", usuarios.get(2).nome());
    }

    @Test
    @DisplayName("encapsulamento: lista de usuários retornada é imutável")
    void encapsulamentoListaUsuarios() throws BibliotecaException {
        biblioteca.usuarios().cadastrarUsuario("1", "Ana");
        List<UsuarioView> lista = biblioteca.usuarios().listarUsuarios();
        assertThrows(UnsupportedOperationException.class, () -> lista.clear());
        assertEquals(1, biblioteca.usuarios().listarUsuarios().size());
    }

    // ==========================================
    // TESTES DA ENTREGA 2
    // ==========================================

    @Test
    @DisplayName("buscarUsuario com matrícula nula ou em branco lança DadosInvalidosException (Devolutiva E1)")
    void buscarUsuarioMatriculaEmBranco() {
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.usuarios().buscarUsuario(null));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.usuarios().buscarUsuario("   "));
    }

    @Test
    @DisplayName("desativarUsuario com sucesso: ativo passa a false")
    void desativarUsuarioSucesso() throws BibliotecaException {
        biblioteca.usuarios().cadastrarUsuario("U1", "Alice");
        biblioteca.usuarios().desativarUsuario("U1");

        UsuarioView u = biblioteca.usuarios().buscarUsuario("U1");
        org.junit.jupiter.api.Assertions.assertFalse(u.ativo());
    }

    @Test
    @DisplayName("desativarUsuario é idempotente: desativar quem já está inativo não muda nada")
    void desativarUsuarioIdempotente() throws BibliotecaException {
        biblioteca.usuarios().cadastrarUsuario("U1", "Alice");
        biblioteca.usuarios().desativarUsuario("U1");
        biblioteca.usuarios().desativarUsuario("U1"); // segunda vez

        UsuarioView u = biblioteca.usuarios().buscarUsuario("U1");
        org.junit.jupiter.api.Assertions.assertFalse(u.ativo());
    }

    @Test
    @DisplayName("desativarUsuario com matrícula inválida lança DadosInvalidosException")
    void desativarUsuarioMatriculaInvalida() {
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.usuarios().desativarUsuario(null));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.usuarios().desativarUsuario("   "));
    }

    @Test
    @DisplayName("desativarUsuario com matrícula inexistente lança RecursoNaoEncontradoException")
    void desativarUsuarioInexistente() {
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.usuarios().desativarUsuario("NAO-EXISTE"));
    }

    @Test
    @DisplayName("desativarUsuario com empréstimo ativo lança OperacaoNaoPermitidaException")
    void desativarUsuarioComEmprestimoAtivo() throws BibliotecaException {
        biblioteca.usuarios().cadastrarUsuario("U1", "Alice");
        biblioteca.acervo().cadastrarItem("L1", "Java Efetivo", "Bloch", "livro", 2019);
        biblioteca.acervo().adicionarExemplar("L1", "T-001");
        biblioteca.emprestimos().emprestar("T-001", "U1", java.time.LocalDate.of(2026, 11, 10));

        assertThrows(br.ufpb.dcx.poo.biblioteca.contrato.excecoes.OperacaoNaoPermitidaException.class,
                () -> biblioteca.usuarios().desativarUsuario("U1"));
    }

    @Test
    @DisplayName("reativarUsuario com sucesso: ativo passa a true")
    void reativarUsuarioSucesso() throws BibliotecaException {
        biblioteca.usuarios().cadastrarUsuario("U1", "Alice");
        biblioteca.usuarios().desativarUsuario("U1");
        biblioteca.usuarios().reativarUsuario("U1");

        UsuarioView u = biblioteca.usuarios().buscarUsuario("U1");
        assertTrue(u.ativo());
    }

    @Test
    @DisplayName("reativarUsuario é idempotente: reativar usuário já ativo não muda nada")
    void reativarUsuarioIdempotente() throws BibliotecaException {
        biblioteca.usuarios().cadastrarUsuario("U1", "Alice");
        biblioteca.usuarios().reativarUsuario("U1"); // já era ativo

        UsuarioView u = biblioteca.usuarios().buscarUsuario("U1");
        assertTrue(u.ativo());
    }

    @Test
    @DisplayName("reativarUsuario com matrícula inválida lança DadosInvalidosException")
    void reativarUsuarioMatriculaInvalida() {
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.usuarios().reativarUsuario(null));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.usuarios().reativarUsuario("   "));
    }

    @Test
    @DisplayName("reativarUsuario com matrícula inexistente lança RecursoNaoEncontradoException")
    void reativarUsuarioInexistente() {
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.usuarios().reativarUsuario("NAO-EXISTE"));
    }
}
