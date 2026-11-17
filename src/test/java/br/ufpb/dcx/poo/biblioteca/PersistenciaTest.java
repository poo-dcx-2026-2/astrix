package br.ufpb.dcx.poo.biblioteca;

import static org.junit.jupiter.api.Assertions.*;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.ufpb.dcx.poo.biblioteca.contrato.Biblioteca;
import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoView;
import br.ufpb.dcx.poo.biblioteca.contrato.ItemView;
import br.ufpb.dcx.poo.biblioteca.contrato.ReservaView;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusEmprestimo;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusExemplar;
import br.ufpb.dcx.poo.biblioteca.contrato.UsuarioView;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.PersistenciaException;

class PersistenciaTest {

    @TempDir
    Path tempDir;

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 15);

    @BeforeEach
    void apontarPersistenciaParaPastaTemporaria() {
        System.setProperty("biblioteca.dados", tempDir.toString());
    }

    @AfterEach
    void restaurarPropriedade() {
        System.clearProperty("biblioteca.dados");
    }

    @Test
    @DisplayName("arquivo ou diretório ausente equivale a acervo vazio: carregar() não falha")
    void carregarComArquivoAusente() throws Exception {
        Biblioteca b = Fabrica.novaBiblioteca();
        // Não há arquivo gravado na pasta temporária
        assertDoesNotThrow(() -> b.carregar());

        assertTrue(b.acervo().listarItens().isEmpty());
        assertTrue(b.usuarios().listarUsuarios().isEmpty());
    }

    @Test
    @DisplayName("persistência de ida e volta completa: salva e recarrega estado com todas as visões")
    void persistenciaIdaEVoltaCompleta() throws Exception {
        Biblioteca b1 = Fabrica.novaBiblioteca();

        // 1. Acervo com itens e exemplares
        b1.acervo().cadastrarItem("L1", "Java Efetivo", "Bloch", "livro", 2019);
        b1.acervo().adicionarExemplar("L1", "T-001");
        b1.acervo().adicionarExemplar("L1", "T-002");
        b1.acervo().cadastrarItem("L2", "Código Limpo", "Martin", "livro", 2009);
        b1.acervo().adicionarExemplar("L2", "T-003");

        // 2. Usuários
        b1.usuarios().cadastrarUsuario("U1", "Alice");
        b1.usuarios().cadastrarUsuario("U2", "Bob");
        b1.usuarios().cadastrarUsuario("U3", "Carlos");
        b1.usuarios().desativarUsuario("U3");

        // 3. Empréstimos: um ativo e um devolvido
        String empAtivo = b1.emprestimos().emprestar("T-001", "U1", HOJE);
        String empDevolvido = b1.emprestimos().emprestar("T-003", "U2", HOJE.minusDays(5));
        b1.emprestimos().devolver("T-003", HOJE);

        // 4. Reserva em L1 (que ficou sem disponível após emprestar T-001 e baixar T-002)
        b1.acervo().baixarExemplar("T-002");
        String resId = b1.emprestimos().reservar("L1", "U2", HOJE);

        // Salvar estado completo
        b1.salvar();

        // Criar uma NOVA biblioteca independente e carregar
        Biblioteca b2 = Fabrica.novaBiblioteca();
        b2.carregar();

        // Conferir ItemView
        ItemView itemL1 = b2.acervo().buscarItem("L1");
        assertEquals("Java Efetivo", itemL1.titulo());
        assertEquals(2, itemL1.totalDeExemplares());
        assertEquals(0, itemL1.exemplaresDisponiveis()); // T-001 emprestado, T-002 indisponível

        var exL1 = b2.acervo().listarExemplares("L1");
        assertEquals(2, exL1.size());
        assertEquals("T-001", exL1.get(0).tombo());
        assertEquals(StatusExemplar.EMPRESTADO, exL1.get(0).status());
        assertEquals("T-002", exL1.get(1).tombo());
        assertEquals(StatusExemplar.INDISPONIVEL, exL1.get(1).status());

        // Conferir UsuarioView
        UsuarioView u1 = b2.usuarios().buscarUsuario("U1");
        assertEquals("Alice", u1.nome());
        assertTrue(u1.ativo());
        assertEquals(1, u1.emprestimosAtivos());

        UsuarioView u3 = b2.usuarios().buscarUsuario("U3");
        assertFalse(u3.ativo());

        // Conferir EmprestimoView
        List<EmprestimoView> empU1 = b2.emprestimos().listarEmprestimosDoUsuario("U1");
        assertEquals(1, empU1.size());
        assertEquals(empAtivo, empU1.get(0).id());
        assertEquals(StatusEmprestimo.ATIVO, empU1.get(0).status());

        List<EmprestimoView> empU2 = b2.emprestimos().listarEmprestimosDoUsuario("U2");
        assertEquals(1, empU2.size());
        assertEquals(empDevolvido, empU2.get(0).id());
        assertEquals(StatusEmprestimo.DEVOLVIDO, empU2.get(0).status());

        // Conferir ReservaView
        List<ReservaView> reservasL1 = b2.emprestimos().listarReservas("L1");
        assertEquals(1, reservasL1.size());
        assertEquals(resId, reservasL1.get(0).id());
        assertEquals("U2", reservasL1.get(0).matricula());
        assertEquals(1, reservasL1.get(0).posicaoNaFila());
    }

    @Test
    @DisplayName("novos IDs de empréstimo e reserva continuam únicos após carregar()")
    void novosIdsContinuamUnicosAposCarregar() throws Exception {
        Biblioteca b1 = Fabrica.novaBiblioteca();
        b1.acervo().cadastrarItem("L1", "Java Efetivo", "Bloch", "livro", 2019);
        b1.acervo().adicionarExemplar("L1", "T-001");
        b1.usuarios().cadastrarUsuario("U1", "Alice");
        b1.usuarios().cadastrarUsuario("U2", "Bob");

        String emp1 = b1.emprestimos().emprestar("T-001", "U1", HOJE);
        String res1 = b1.emprestimos().reservar("L1", "U2", HOJE);

        b1.salvar();

        Biblioteca b2 = Fabrica.novaBiblioteca();
        b2.carregar();

        b2.acervo().cadastrarItem("L2", "Código Limpo", "Martin", "livro", 2009);
        b2.acervo().adicionarExemplar("L2", "T-002");

        String emp2 = b2.emprestimos().emprestar("T-002", "U1", HOJE);
        assertNotEquals(emp1, emp2);

        String res2 = b2.emprestimos().reservar("L2", "U2", HOJE);
        assertNotEquals(res1, res2);
    }

    @Test
    @DisplayName("arquivo corrompido com bytes inválidos lança PersistenciaException e não altera memória")
    void arquivoCorrompidoComBytesInvalidosNaoAlteraMemoria() throws Exception {
        Biblioteca b = Fabrica.novaBiblioteca();
        b.acervo().cadastrarItem("L1", "Java Efetivo", "Bloch", "livro", 2019);

        // Escrever lixo no arquivo da pasta temporária
        Path arq = tempDir.resolve("biblioteca.dat");
        Files.writeString(arq, "CONTEUDO_CORROMPIDO_NAO_SERIALIZADO");

        assertThrows(PersistenciaException.class, () -> b.carregar());

        // Estado em memória deve continuar intacto
        assertEquals(1, b.acervo().listarItens().size());
        assertEquals("Java Efetivo", b.acervo().buscarItem("L1").titulo());
    }

    @Test
    @DisplayName("arquivo com dados inconsistentes (referência inexistente) lança PersistenciaException")
    void arquivoInconsistenteLancaPersistenciaException() throws Exception {
        Biblioteca b = Fabrica.novaBiblioteca();

        // Criar um arquivo com referência inválida de exemplar para item inexistente
        Path arq = tempDir.resolve("biblioteca.dat");
        var dadosInconsistentes = new br.ufpb.dcx.poo.biblioteca.persistencia.DadosPersistencia(
                List.of(), // sem itens!
                List.of(new br.ufpb.dcx.poo.biblioteca.persistencia.DadosPersistencia.ExemplarDTO(
                        "T-001", "ITEM-INEXISTENTE", StatusExemplar.DISPONIVEL)),
                List.of(), List.of(), List.of(), 0L, 0L
        );

        try (OutputStream os = Files.newOutputStream(arq);
             var oos = new java.io.ObjectOutputStream(os)) {
            oos.writeObject(dadosInconsistentes);
        }

        assertThrows(PersistenciaException.class, () -> b.carregar());
    }
}
