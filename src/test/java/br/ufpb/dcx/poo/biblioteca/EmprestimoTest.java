package br.ufpb.dcx.poo.biblioteca;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.ufpb.dcx.poo.biblioteca.contrato.Biblioteca;
import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoView;
import br.ufpb.dcx.poo.biblioteca.contrato.ItemView;
import br.ufpb.dcx.poo.biblioteca.contrato.ReservaView;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusEmprestimo;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusExemplar;
import br.ufpb.dcx.poo.biblioteca.contrato.UsuarioView;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.ExemplarIndisponivelException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.LimiteDeEmprestimosExcedidoException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.OperacaoNaoPermitidaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoNaoEncontradoException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.UsuarioInativoException;

class EmprestimoTest {

    private Biblioteca biblioteca;
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 10);

    @BeforeEach
    void setup() throws Exception {
        biblioteca = Fabrica.novaBiblioteca();

        // Item L1 com um exemplar (para esgotar facilmente)
        biblioteca.acervo().cadastrarItem("L1", "Java Efetivo", "Bloch", "livro", 2019);
        biblioteca.acervo().adicionarExemplar("L1", "T-001");

        // Item L2 com dois exemplares
        biblioteca.acervo().cadastrarItem("L2", "Código Limpo", "Martin", "livro", 2009);
        biblioteca.acervo().adicionarExemplar("L2", "T-002");
        biblioteca.acervo().adicionarExemplar("L2", "T-003");

        // Item L3 sem exemplares
        biblioteca.acervo().cadastrarItem("L3", "Refatoração", "Fowler", "livro", 2004);

        // Usuários
        biblioteca.usuarios().cadastrarUsuario("U1", "Alice");
        biblioteca.usuarios().cadastrarUsuario("U2", "Bob");
        biblioteca.usuarios().cadastrarUsuario("U3", "Carlos");
        biblioteca.usuarios().cadastrarUsuario("U4", "Diana");
        biblioteca.usuarios().cadastrarUsuario("INATIVO", "Inativo");
        biblioteca.usuarios().desativarUsuario("INATIVO");
    }

    // ==========================================
    // 1. EMPRÉSTIMO
    // ==========================================

    @Test
    @DisplayName("emprestar com parâmetros inválidos lança DadosInvalidosException")
    void emprestarParametrosInvalidos() {
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().emprestar(null, "U1", HOJE));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().emprestar("   ", "U1", HOJE));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().emprestar("T-001", null, HOJE));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().emprestar("T-001", "   ", HOJE));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().emprestar("T-001", "U1", null));
    }

    @Test
    @DisplayName("emprestar exemplar ou usuário inexistente lança RecursoNaoEncontradoException")
    void emprestarRecursoInexistente() {
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().emprestar("T-999", "U1", HOJE));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().emprestar("T-001", "U-INEXISTENTE", HOJE));
    }

    @Test
    @DisplayName("emprestar para usuário inativo lança UsuarioInativoException")
    void emprestarUsuarioInativo() {
        assertThrows(UsuarioInativoException.class,
                () -> biblioteca.emprestimos().emprestar("T-001", "INATIVO", HOJE));
    }

    @Test
    @DisplayName("emprestar exemplar indisponível (já emprestado ou baixado) lança ExemplarIndisponivelException")
    void emprestarExemplarIndisponivel() throws Exception {
        biblioteca.emprestimos().emprestar("T-001", "U1", HOJE);
        assertThrows(ExemplarIndisponivelException.class,
                () -> biblioteca.emprestimos().emprestar("T-001", "U2", HOJE));

        biblioteca.acervo().baixarExemplar("T-002");
        assertThrows(ExemplarIndisponivelException.class,
                () -> biblioteca.emprestimos().emprestar("T-002", "U2", HOJE));
    }

    @Test
    @DisplayName("emprestar acima do limite de empréstimos lança LimiteDeEmprestimosExcedidoException")
    void emprestarLimiteExcedido() throws Exception {
        biblioteca.acervo().cadastrarItem("L4", "Livro 4", "Autor", "livro", 2020);
        biblioteca.acervo().adicionarExemplar("L4", "T-010");
        biblioteca.acervo().cadastrarItem("L5", "Livro 5", "Autor", "livro", 2020);
        biblioteca.acervo().adicionarExemplar("L5", "T-011");

        biblioteca.emprestimos().emprestar("T-001", "U1", HOJE);
        biblioteca.emprestimos().emprestar("T-002", "U1", HOJE);
        biblioteca.emprestimos().emprestar("T-003", "U1", HOJE);

        assertThrows(LimiteDeEmprestimosExcedidoException.class,
                () -> biblioteca.emprestimos().emprestar("T-010", "U1", HOJE));
    }

    @Test
    @DisplayName("caminho feliz de empréstimo: reflete em ItemView, UsuarioView e EmprestimoView")
    void emprestarCaminhoFeliz() throws Exception {
        String id = biblioteca.emprestimos().emprestar("T-001", "U1", HOJE);

        assertNotNull(id);
        assertFalse(id.isBlank());

        // Efeito no exemplar e item
        ItemView item = biblioteca.acervo().buscarItem("L1");
        assertEquals(0, item.exemplaresDisponiveis());
        assertEquals(1, item.totalDeExemplares());

        var exemplares = biblioteca.acervo().listarExemplares("L1");
        assertEquals(StatusExemplar.EMPRESTADO, exemplares.get(0).status());

        // Efeito no usuário
        UsuarioView usuario = biblioteca.usuarios().buscarUsuario("U1");
        assertEquals(1, usuario.emprestimosAtivos());

        // Efeito na listagem de empréstimos
        List<EmprestimoView> lista = biblioteca.emprestimos().listarEmprestimosDoUsuario("U1");
        assertEquals(1, lista.size());
        EmprestimoView emp = lista.get(0);
        assertEquals(id, emp.id());
        assertEquals("T-001", emp.tombo());
        assertEquals("U1", emp.matricula());
        assertEquals(HOJE, emp.dataDoEmprestimo());
        assertEquals(HOJE.plusDays(14), emp.dataPrevistaDeDevolucao());
        assertNull(emp.dataDaDevolucao());
        assertEquals(StatusEmprestimo.ATIVO, emp.status());
    }

    // ==========================================
    // 2. DEVOLUÇÃO
    // ==========================================

    @Test
    @DisplayName("devolver com parâmetros inválidos lança DadosInvalidosException")
    void devolverParametrosInvalidos() {
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().devolver(null, HOJE));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().devolver("   ", HOJE));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().devolver("T-001", null));
    }

    @Test
    @DisplayName("devolver exemplar inexistente lança RecursoNaoEncontradoException")
    void devolverExemplarInexistente() {
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().devolver("T-INEXISTENTE", HOJE));
    }

    @Test
    @DisplayName("devolver exemplar sem empréstimo ativo lança OperacaoNaoPermitidaException")
    void devolverSemEmprestimoAtivo() {
        assertThrows(OperacaoNaoPermitidaException.class,
                () -> biblioteca.emprestimos().devolver("T-001", HOJE));
    }

    @Test
    @DisplayName("devolver com data anterior à do empréstimo lança DadosInvalidosException")
    void devolverComDataAnteriorAoEmprestimo() throws Exception {
        biblioteca.emprestimos().emprestar("T-001", "U1", HOJE);
        LocalDate dataPassada = HOJE.minusDays(1);
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().devolver("T-001", dataPassada));
    }

    @Test
    @DisplayName("devolver com sucesso dentro do prazo: status DEVOLVIDO e exemplar DISPONIVEL")
    void devolverSucessoDentroDoPrazo() throws Exception {
        String id = biblioteca.emprestimos().emprestar("T-001", "U1", HOJE);
        LocalDate dataDev = HOJE.plusDays(10);
        biblioteca.emprestimos().devolver("T-001", dataDev);

        UsuarioView u = biblioteca.usuarios().buscarUsuario("U1");
        assertEquals(0, u.emprestimosAtivos());

        ItemView item = biblioteca.acervo().buscarItem("L1");
        assertEquals(1, item.exemplaresDisponiveis());

        List<EmprestimoView> lista = biblioteca.emprestimos().listarEmprestimosDoUsuario("U1");
        assertEquals(1, lista.size());
        EmprestimoView emp = lista.get(0);
        assertEquals(id, emp.id());
        assertEquals(dataDev, emp.dataDaDevolucao());
        assertEquals(StatusEmprestimo.DEVOLVIDO, emp.status());
    }

    @Test
    @DisplayName("devolver com atraso passa status a DEVOLVIDO no histórico do usuário")
    void devolverComAtrasoPassaADevolvido() throws Exception {
        String id = biblioteca.emprestimos().emprestar("T-001", "U1", HOJE);
        LocalDate dataDevAtrasada = HOJE.plusDays(20);
        biblioteca.emprestimos().devolver("T-001", dataDevAtrasada);

        List<EmprestimoView> lista = biblioteca.emprestimos().listarEmprestimosDoUsuario("U1");
        assertEquals(1, lista.size());
        assertEquals(StatusEmprestimo.DEVOLVIDO, lista.get(0).status());
        assertEquals(dataDevAtrasada, lista.get(0).dataDaDevolucao());
    }

    // ==========================================
    // 3. CONSULTAS: LISTAR EMPRÉSTIMOS E ATRASADOS
    // ==========================================

    @Test
    @DisplayName("listarEmprestimosDoUsuario com matrícula inválida ou inexistente")
    void listarEmprestimosDoUsuarioValidacoes() {
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().listarEmprestimosDoUsuario(null));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().listarEmprestimosDoUsuario("   "));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().listarEmprestimosDoUsuario("U-INEXISTENTE"));
    }

    @Test
    @DisplayName("listarEmprestimosDoUsuario ordena por data decrescente e desempata por último registrado")
    void listarEmprestimosDoUsuarioOrdenacao() throws Exception {
        String e1 = biblioteca.emprestimos().emprestar("T-001", "U1", HOJE);
        biblioteca.emprestimos().devolver("T-001", HOJE);

        String e2 = biblioteca.emprestimos().emprestar("T-001", "U1", HOJE); // mesmo dia, registrado depois
        biblioteca.emprestimos().devolver("T-001", HOJE.plusDays(1));

        String e3 = biblioteca.emprestimos().emprestar("T-001", "U1", HOJE.plusDays(5)); // data mais recente

        List<EmprestimoView> lista = biblioteca.emprestimos().listarEmprestimosDoUsuario("U1");
        assertEquals(3, lista.size());
        assertEquals(e3, lista.get(0).id()); // data mais recente
        assertEquals(e2, lista.get(1).id()); // mesmo dia de e1, registrado por último
        assertEquals(e1, lista.get(2).id());
    }

    @Test
    @DisplayName("listarAtrasados: data nula lança DadosInvalidosException")
    void listarAtrasadosDataInvalida() {
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().listarAtrasados(null));
    }

    @Test
    @DisplayName("listarAtrasados: no próprio dia previsto não há atraso; retorna status ATRASADO e ordenado")
    void listarAtrasadosComportamento() throws Exception {
        biblioteca.emprestimos().emprestar("T-001", "U1", HOJE); // previsto HOJE + 14
        biblioteca.emprestimos().emprestar("T-002", "U2", HOJE.minusDays(2)); // previsto HOJE + 12

        LocalDate diaPrevistoU2 = HOJE.minusDays(2).plusDays(14); // HOJE + 12
        // No próprio dia previsto ainda não há atraso
        assertTrue(biblioteca.emprestimos().listarAtrasados(diaPrevistoU2).isEmpty());

        // Um dia após o vencimento de U2
        List<EmprestimoView> atrasados1 = biblioteca.emprestimos().listarAtrasados(diaPrevistoU2.plusDays(1));
        assertEquals(1, atrasados1.size());
        assertEquals("T-002", atrasados1.get(0).tombo());
        assertEquals(StatusEmprestimo.ATRASADO, atrasados1.get(0).status());

        // Após o vencimento de ambos
        LocalDate diaPrevistoU1 = HOJE.plusDays(14);
        List<EmprestimoView> atrasados2 = biblioteca.emprestimos().listarAtrasados(diaPrevistoU1.plusDays(1));
        assertEquals(2, atrasados2.size());
        assertEquals("T-002", atrasados2.get(0).tombo()); // mais antigo na data prevista vem primeiro
        assertEquals("T-001", atrasados2.get(1).tombo());
    }

    @Test
    @DisplayName("listarAtrasados não inclui empréstimos já devolvidos")
    void listarAtrasadosNaoIncluiDevolvidos() throws Exception {
        biblioteca.emprestimos().emprestar("T-001", "U1", HOJE);
        LocalDate muitoDepois = HOJE.plusDays(30);
        biblioteca.emprestimos().devolver("T-001", muitoDepois);

        assertTrue(biblioteca.emprestimos().listarAtrasados(muitoDepois.plusDays(1)).isEmpty());
    }

    // ==========================================
    // 4. RESERVAS
    // ==========================================

    @Test
    @DisplayName("reservar com parâmetros inválidos lança DadosInvalidosException")
    void reservarParametrosInvalidos() {
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().reservar(null, "U1", HOJE));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().reservar("   ", "U1", HOJE));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().reservar("L1", null, HOJE));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().reservar("L1", "   ", HOJE));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().reservar("L1", "U1", null));
    }

    @Test
    @DisplayName("reservar item ou usuário inexistente lança RecursoNaoEncontradoException")
    void reservarRecursoInexistente() {
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().reservar("L-INEXISTENTE", "U1", HOJE));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().reservar("L1", "U-INEXISTENTE", HOJE));
    }

    @Test
    @DisplayName("reservar com usuário inativo lança UsuarioInativoException")
    void reservarUsuarioInativo() {
        assertThrows(UsuarioInativoException.class,
                () -> biblioteca.emprestimos().reservar("L1", "INATIVO", HOJE));
    }

    @Test
    @DisplayName("reservar item que possui exemplar disponível lança OperacaoNaoPermitidaException")
    void reservarItemComExemplarDisponivel() {
        // L1 tem T-001 disponível
        assertThrows(OperacaoNaoPermitidaException.class,
                () -> biblioteca.emprestimos().reservar("L1", "U1", HOJE));
    }

    @Test
    @DisplayName("reservar com sucesso: item sem exemplar ou todos emprestados")
    void reservarComSucesso() throws Exception {
        // L3 não tem nenhum exemplar
        String r1 = biblioteca.emprestimos().reservar("L3", "U1", HOJE);
        assertNotNull(r1);

        // L1: emprestar o único exemplar para esgotar disponibilidade
        biblioteca.emprestimos().emprestar("T-001", "U1", HOJE);
        String r2 = biblioteca.emprestimos().reservar("L1", "U2", HOJE);
        assertNotNull(r2);

        List<ReservaView> reservas = biblioteca.emprestimos().listarReservas("L1");
        assertEquals(1, reservas.size());
        assertEquals(1, reservas.get(0).posicaoNaFila());
        assertEquals("U2", reservas.get(0).matricula());
    }

    @Test
    @DisplayName("usuário que já tem reserva ativa para o item lança OperacaoNaoPermitidaException")
    void reservarDuplicadaParaMesmoItem() throws Exception {
        String r = biblioteca.emprestimos().reservar("L3", "U1", HOJE);
        assertNotNull(r);

        assertThrows(OperacaoNaoPermitidaException.class,
                () -> biblioteca.emprestimos().reservar("L3", "U1", HOJE));
    }

    @Test
    @DisplayName("listarReservas com código inválido ou item inexistente")
    void listarReservasValidacoes() {
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().listarReservas(null));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().listarReservas("   "));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().listarReservas("NAO-EXISTE"));
    }

    @Test
    @DisplayName("fila de reservas: posições 1, 2, 3 por ordem de chegada")
    void filaDeReservasOrdem() throws Exception {
        biblioteca.emprestimos().reservar("L3", "U1", HOJE);
        biblioteca.emprestimos().reservar("L3", "U2", HOJE);
        biblioteca.emprestimos().reservar("L3", "U3", HOJE);

        List<ReservaView> fila = biblioteca.emprestimos().listarReservas("L3");
        assertEquals(3, fila.size());
        assertEquals(1, fila.get(0).posicaoNaFila());
        assertEquals("U1", fila.get(0).matricula());
        assertEquals(2, fila.get(1).posicaoNaFila());
        assertEquals("U2", fila.get(1).matricula());
        assertEquals(3, fila.get(2).posicaoNaFila());
        assertEquals("U3", fila.get(2).matricula());
    }

    @Test
    @DisplayName("cancelarReserva com id inválido ou inexistente")
    void cancelarReservaValidacoes() {
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().cancelarReserva(null));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().cancelarReserva("   "));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().cancelarReserva("R-INEXISTENTE"));
    }

    @Test
    @DisplayName("cancelar reserva reordena as posições na fila")
    void cancelarReservaReordenaFila() throws Exception {
        String r1 = biblioteca.emprestimos().reservar("L3", "U1", HOJE);
        String r2 = biblioteca.emprestimos().reservar("L3", "U2", HOJE);
        String r3 = biblioteca.emprestimos().reservar("L3", "U3", HOJE);

        biblioteca.emprestimos().cancelarReserva(r1);

        List<ReservaView> fila = biblioteca.emprestimos().listarReservas("L3");
        assertEquals(2, fila.size());
        assertEquals(1, fila.get(0).posicaoNaFila());
        assertEquals("U2", fila.get(0).matricula());
        assertEquals(2, fila.get(1).posicaoNaFila());
        assertEquals("U3", fila.get(1).matricula());

        // Cancelar reserva já cancelada lança RecursoNaoEncontradoException
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().cancelarReserva(r1));
    }

    // ==========================================
    // 5. CICLO COMPLETO DE RESERVA (§2.4)
    // ==========================================

    @Test
    @DisplayName("ciclo de reserva: devolução separa exemplar, empréstimo ao reservante, recusa ao outro usuário")
    void cicloCompletoDeReserva() throws Exception {
        // 1. U1 pega o único exemplar de L1
        biblioteca.emprestimos().emprestar("T-001", "U1", HOJE);

        // 2. U2 e U3 entram na fila de reserva de L1
        String resU2 = biblioteca.emprestimos().reservar("L1", "U2", HOJE);
        String resU3 = biblioteca.emprestimos().reservar("L1", "U3", HOJE);

        // 3. U1 devolve T-001 -> exemplar fica RESERVADO para U2 (a mais antiga da fila)
        biblioteca.emprestimos().devolver("T-001", HOJE.plusDays(2));

        var exemplar = biblioteca.acervo().listarExemplares("L1").get(0);
        assertEquals(StatusExemplar.RESERVADO, exemplar.status());

        // A reserva continua ativa aguardando o usuário vir buscar
        assertEquals(2, biblioteca.emprestimos().listarReservas("L1").size());

        // 4. Recusa a outro usuário: U4 e U3 tentam emprestar e são recusados
        assertThrows(ExemplarIndisponivelException.class,
                () -> biblioteca.emprestimos().emprestar("T-001", "U4", HOJE.plusDays(3)));
        assertThrows(ExemplarIndisponivelException.class,
                () -> biblioteca.emprestimos().emprestar("T-001", "U3", HOJE.plusDays(3)));

        // 5. Empréstimo ao reservante: U2 empresta com sucesso
        String empU2 = biblioteca.emprestimos().emprestar("T-001", "U2", HOJE.plusDays(3));
        assertNotNull(empU2);

        // Exemplar passa a EMPRESTADO
        assertEquals(StatusExemplar.EMPRESTADO, biblioteca.acervo().listarExemplares("L1").get(0).status());

        // Reserva de U2 é atendida e sai da fila; U3 agora é a posição 1
        List<ReservaView> filaRestante = biblioteca.emprestimos().listarReservas("L1");
        assertEquals(1, filaRestante.size());
        assertEquals("U3", filaRestante.get(0).matricula());
        assertEquals(1, filaRestante.get(0).posicaoNaFila());

        // Cancelar reserva já atendida lança RecursoNaoEncontradoException
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().cancelarReserva(resU2));
    }

    @Test
    @DisplayName("cancelar reserva com exemplar separado passa o exemplar à próxima reserva sem exemplar")
    void cancelarReservaComExemplarSeparadoPassaParaProximo() throws Exception {
        biblioteca.emprestimos().emprestar("T-001", "U1", HOJE);
        String rU2 = biblioteca.emprestimos().reservar("L1", "U2", HOJE);
        String rU3 = biblioteca.emprestimos().reservar("L1", "U3", HOJE);

        biblioteca.emprestimos().devolver("T-001", HOJE.plusDays(2));

        // T-001 está separado para U2. U2 cancela sua reserva.
        biblioteca.emprestimos().cancelarReserva(rU2);

        // T-001 agora deve estar separado para U3!
        // U4 tenta emprestar -> deve ser recusado
        assertThrows(ExemplarIndisponivelException.class,
                () -> biblioteca.emprestimos().emprestar("T-001", "U4", HOJE.plusDays(3)));

        // U3 empresta -> sucesso!
        String empU3 = biblioteca.emprestimos().emprestar("T-001", "U3", HOJE.plusDays(3));
        assertNotNull(empU3);
    }

    @Test
    @DisplayName("cancelar reserva com exemplar separado sem mais ninguém na fila volta exemplar a DISPONIVEL")
    void cancelarReservaComExemplarSeparadoSemFilaVoltaDisponivel() throws Exception {
        biblioteca.emprestimos().emprestar("T-001", "U1", HOJE);
        String rU2 = biblioteca.emprestimos().reservar("L1", "U2", HOJE);

        biblioteca.emprestimos().devolver("T-001", HOJE.plusDays(2));
        assertEquals(StatusExemplar.RESERVADO, biblioteca.acervo().listarExemplares("L1").get(0).status());

        biblioteca.emprestimos().cancelarReserva(rU2);
        assertEquals(StatusExemplar.DISPONIVEL, biblioteca.acervo().listarExemplares("L1").get(0).status());
    }

    @Test
    @DisplayName("baixar exemplar RESERVADO é permitido e a reserva volta a esperar")
    void baixarExemplarReservadoReservaVoltaAEsperar() throws Exception {
        biblioteca.emprestimos().emprestar("T-001", "U1", HOJE);
        String rU2 = biblioteca.emprestimos().reservar("L1", "U2", HOJE);
        biblioteca.emprestimos().devolver("T-001", HOJE.plusDays(1));

        // T-001 agora está RESERVADO
        assertEquals(StatusExemplar.RESERVADO, biblioteca.acervo().listarExemplares("L1").get(0).status());

        // Baixar o exemplar reservado
        biblioteca.acervo().baixarExemplar("T-001");
        assertEquals(StatusExemplar.INDISPONIVEL, biblioteca.acervo().listarExemplares("L1").get(0).status());

        // A reserva continua na fila aguardando outro exemplar
        List<ReservaView> reservas = biblioteca.emprestimos().listarReservas("L1");
        assertEquals(1, reservas.size());
        assertEquals("U2", reservas.get(0).matricula());
    }

    @Test
    @DisplayName("desativar usuário cancela suas reservas ativas e libera exemplar separado")
    void desativarUsuarioCancelaReservas() throws Exception {
        biblioteca.emprestimos().emprestar("T-001", "U1", HOJE);
        biblioteca.emprestimos().reservar("L1", "U2", HOJE);
        biblioteca.emprestimos().devolver("T-001", HOJE.plusDays(1));

        // T-001 reservado para U2
        assertEquals(StatusExemplar.RESERVADO, biblioteca.acervo().listarExemplares("L1").get(0).status());

        // Desativar U2
        biblioteca.usuarios().desativarUsuario("U2");

        // Exemplar volta a DISPONIVEL e fila fica vazia
        assertEquals(StatusExemplar.DISPONIVEL, biblioteca.acervo().listarExemplares("L1").get(0).status());
        assertTrue(biblioteca.emprestimos().listarReservas("L1").isEmpty());
    }
}
