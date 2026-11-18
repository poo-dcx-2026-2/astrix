package br.ufpb.dcx.poo.biblioteca.inicial;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoService;
import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoView;
import br.ufpb.dcx.poo.biblioteca.contrato.ReservaView;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusExemplar;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.ExemplarIndisponivelException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.LimiteDeEmprestimosExcedidoException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.OperacaoNaoPermitidaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoNaoEncontradoException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.UsuarioInativoException;
import br.ufpb.dcx.poo.biblioteca.persistencia.DadosPersistencia;

/**
 * Implementação em memória do serviço de empréstimos e reservas.
 */
public class EmprestimosEmMemoria implements EmprestimoService {

    private final AcervoEmMemoria acervo;
    private final UsuariosEmMemoria usuarios;

    private final Map<String, Emprestimo> emprestimosPorId = new LinkedHashMap<>();
    private final Map<String, Emprestimo> emprestimoAtivoDoExemplar = new LinkedHashMap<>();
    private final Map<String, List<Reserva>> reservasPorItem = new LinkedHashMap<>();
    private final Map<String, Reserva> reservasPorId = new LinkedHashMap<>();

    private final AtomicLong contadorEmprestimos = new AtomicLong(0);
    private final AtomicLong contadorReservas = new AtomicLong(0);

    public EmprestimosEmMemoria(AcervoEmMemoria acervo, UsuariosEmMemoria usuarios) {
        this.acervo = acervo;
        this.usuarios = usuarios;
    }

    @Override
    public String emprestar(String tombo, String matricula, LocalDate data)
            throws RecursoNaoEncontradoException, ExemplarIndisponivelException,
                   UsuarioInativoException, LimiteDeEmprestimosExcedidoException {

        // 1. Validação de parâmetros
        if (tombo == null || tombo.isBlank()) {
            throw new DadosInvalidosException("Tombo do exemplar não pode ser nulo ou vazio.");
        }
        if (matricula == null || matricula.isBlank()) {
            throw new DadosInvalidosException("Matrícula do usuário não pode ser nula ou vazia.");
        }
        if (data == null) {
            throw new DadosInvalidosException("Data do empréstimo não pode ser nula.");
        }

        // 2. Tombo ou matrícula inexistente
        Exemplar exemplar = acervo.obterExemplar(tombo);
        Usuario usuario = usuarios.obterUsuario(matricula);

        // 3. Usuário inativo
        if (!usuario.isAtivo()) {
            throw new UsuarioInativoException("Usuário inativo: " + matricula);
        }

        // 4. Exemplar EMPRESTADO, INDISPONIVEL ou RESERVADO para outro usuário
        StatusExemplar status = exemplar.getStatus();
        if (status == StatusExemplar.EMPRESTADO || status == StatusExemplar.INDISPONIVEL) {
            throw new ExemplarIndisponivelException("Exemplar não disponível para empréstimo: " + tombo);
        }

        Reserva reservaAtendida = null;
        if (status == StatusExemplar.RESERVADO) {
            Reserva reservaSeparada = encontrarReservaComExemplarSeparado(exemplar.getTombo());
            if (reservaSeparada == null || !reservaSeparada.getMatricula().equals(usuario.getMatricula())) {
                throw new ExemplarIndisponivelException("Exemplar reservado para outro usuário: " + tombo);
            }
            reservaAtendida = reservaSeparada;
        }

        // 5. Usuário já com LIMITE_DE_EMPRESTIMOS ativos
        if (usuario.getEmprestimosAtivos() >= LIMITE_DE_EMPRESTIMOS) {
            throw new LimiteDeEmprestimosExcedidoException(
                    "Usuário já atingiu o limite de " + LIMITE_DE_EMPRESTIMOS + " empréstimos ativos: " + matricula);
        }

        // Sucesso: atender reserva se estava reservado para este usuário
        if (reservaAtendida != null) {
            removerReservaAtendida(reservaAtendida);
        }

        exemplar.setStatus(StatusExemplar.EMPRESTADO);
        usuario.setEmprestimosAtivos(usuario.getEmprestimosAtivos() + 1);

        long seq = contadorEmprestimos.incrementAndGet();
        String id = "E-" + seq;
        Emprestimo emprestimo = new Emprestimo(id, exemplar.getTombo(), usuario.getMatricula(), data, seq);

        emprestimosPorId.put(id, emprestimo);
        emprestimoAtivoDoExemplar.put(exemplar.getTombo(), emprestimo);

        return id;
    }

    @Override
    public void devolver(String tombo, LocalDate data)
            throws RecursoNaoEncontradoException, OperacaoNaoPermitidaException {

        // 1. Validação de parâmetros
        if (tombo == null || tombo.isBlank()) {
            throw new DadosInvalidosException("Tombo do exemplar não pode ser nulo ou vazio.");
        }
        if (data == null) {
            throw new DadosInvalidosException("Data de devolução não pode ser nula.");
        }

        // 2. Tombo inexistente
        Exemplar exemplar = acervo.obterExemplar(tombo);

        // 3. Exemplar sem empréstimo ativo
        Emprestimo emprestimo = emprestimoAtivoDoExemplar.get(exemplar.getTombo());
        if (emprestimo == null) {
            throw new OperacaoNaoPermitidaException("Exemplar não possui empréstimo ativo: " + tombo);
        }

        // 4. Data anterior à data do empréstimo
        if (data.isBefore(emprestimo.getDataDoEmprestimo())) {
            throw new DadosInvalidosException("Data de devolução anterior à data do empréstimo.");
        }

        // Finaliza o empréstimo
        emprestimo.finalizar(data);
        emprestimoAtivoDoExemplar.remove(exemplar.getTombo());

        try {
            Usuario usuario = usuarios.obterUsuario(emprestimo.getMatricula());
            usuario.setEmprestimosAtivos(Math.max(0, usuario.getEmprestimosAtivos() - 1));
        } catch (RecursoNaoEncontradoException ignored) {
        }

        // Verificar fila de reservas do item: separar para a mais antiga sem exemplar
        String codigoDoItem = exemplar.getItem().getCodigo();
        List<Reserva> fila = reservasPorItem.get(codigoDoItem);
        Reserva proximaSemExemplar = null;

        if (fila != null) {
            for (Reserva r : fila) {
                if (!r.temExemplarSeparado()) {
                    proximaSemExemplar = r;
                    break;
                }
            }
        }

        if (proximaSemExemplar != null) {
            proximaSemExemplar.separarExemplar(exemplar.getTombo());
            exemplar.setStatus(StatusExemplar.RESERVADO);
        } else {
            exemplar.setStatus(StatusExemplar.DISPONIVEL);
        }
    }

    @Override
    public List<EmprestimoView> listarEmprestimosDoUsuario(String matricula)
            throws RecursoNaoEncontradoException {

        if (matricula == null || matricula.isBlank()) {
            throw new DadosInvalidosException("Matrícula não pode ser nula ou vazia.");
        }

        Usuario usuario = usuarios.obterUsuario(matricula);

        List<Emprestimo> lista = new ArrayList<>();
        for (Emprestimo e : emprestimosPorId.values()) {
            if (e.getMatricula().equals(usuario.getMatricula())) {
                lista.add(e);
            }
        }

        // Do mais recente para o mais antigo por dataDoEmprestimo; no empate, o registrado por último vem primeiro
        lista.sort(Comparator
                .comparing(Emprestimo::getDataDoEmprestimo, Comparator.reverseOrder())
                .thenComparing(Emprestimo::getSequencia, Comparator.reverseOrder()));

        List<EmprestimoView> resultado = new ArrayList<>();
        for (Emprestimo e : lista) {
            resultado.add(e.toView());
        }

        return Collections.unmodifiableList(resultado);
    }

    @Override
    public List<EmprestimoView> listarAtrasados(LocalDate data) {
        if (data == null) {
            throw new DadosInvalidosException("Data de referência não pode ser nula.");
        }

        List<Emprestimo> lista = new ArrayList<>();
        for (Emprestimo e : emprestimosPorId.values()) {
            if (e.isAtivo() && e.getDataPrevistaDeDevolucao().isBefore(data)) {
                lista.add(e);
            }
        }

        // Ordenados pela data prevista, do mais antigo para o mais recente
        lista.sort(Comparator.comparing(Emprestimo::getDataPrevistaDeDevolucao));

        List<EmprestimoView> resultado = new ArrayList<>();
        for (Emprestimo e : lista) {
            resultado.add(e.toViewAtrasado());
        }

        return Collections.unmodifiableList(resultado);
    }

    @Override
    public String reservar(String codigoDoItem, String matricula, LocalDate data)
            throws RecursoNaoEncontradoException, OperacaoNaoPermitidaException {

        // 1. Validação de parâmetros
        if (codigoDoItem == null || codigoDoItem.isBlank()) {
            throw new DadosInvalidosException("Código do item não pode ser nulo ou vazio.");
        }
        if (matricula == null || matricula.isBlank()) {
            throw new DadosInvalidosException("Matrícula não pode ser nula ou vazia.");
        }
        if (data == null) {
            throw new DadosInvalidosException("Data da reserva não pode ser nula.");
        }

        // 2. Item ou usuário inexistente
        Item item = acervo.obterItem(codigoDoItem);
        Usuario usuario = usuarios.obterUsuario(matricula);

        // 3. Usuário inativo
        if (!usuario.isAtivo()) {
            throw new UsuarioInativoException("Usuário inativo não pode realizar reservas: " + matricula);
        }

        // 4. Item com exemplar disponível, ou usuário que já tem reserva ativa para o item
        if (item.getExemplaresDisponiveis() > 0) {
            throw new OperacaoNaoPermitidaException(
                    "Não é permitido reservar item que possui exemplar disponível: " + codigoDoItem);
        }

        List<Reserva> fila = reservasPorItem.computeIfAbsent(item.getCodigo(), k -> new ArrayList<>());
        boolean jaTemReserva = fila.stream().anyMatch(r -> r.getMatricula().equals(usuario.getMatricula()));
        if (jaTemReserva) {
            throw new OperacaoNaoPermitidaException(
                    "Usuário já possui reserva ativa para este item: " + matricula);
        }

        long seq = contadorReservas.incrementAndGet();
        String id = "R-" + seq;
        Reserva reserva = new Reserva(id, item.getCodigo(), usuario.getMatricula(), data, fila.size() + 1);

        fila.add(reserva);
        reservasPorId.put(id, reserva);

        return id;
    }

    @Override
    public List<ReservaView> listarReservas(String codigoDoItem)
            throws RecursoNaoEncontradoException {

        if (codigoDoItem == null || codigoDoItem.isBlank()) {
            throw new DadosInvalidosException("Código do item não pode ser nulo ou vazio.");
        }

        Item item = acervo.obterItem(codigoDoItem);

        List<Reserva> fila = reservasPorItem.getOrDefault(item.getCodigo(), List.of());
        List<ReservaView> resultado = new ArrayList<>();
        for (Reserva r : fila) {
            resultado.add(r.toView());
        }

        resultado.sort(Comparator.comparingInt(ReservaView::posicaoNaFila));
        return Collections.unmodifiableList(resultado);
    }

    @Override
    public void cancelarReserva(String idDaReserva) throws RecursoNaoEncontradoException {
        if (idDaReserva == null || idDaReserva.isBlank()) {
            throw new DadosInvalidosException("Id da reserva não pode ser nulo ou vazio.");
        }

        Reserva reserva = reservasPorId.get(idDaReserva.trim());
        if (reserva == null) {
            throw new RecursoNaoEncontradoException("Reserva não encontrada: " + idDaReserva);
        }

        reservasPorId.remove(reserva.getId());

        List<Reserva> fila = reservasPorItem.get(reserva.getCodigoDoItem());
        if (fila != null) {
            fila.remove(reserva);
            atualizarPosicoesFila(fila);
        }

        String tomboSeparado = reserva.getTomboExemplarSeparado();
        if (tomboSeparado != null) {
            try {
                Exemplar exemplar = acervo.obterExemplar(tomboSeparado);
                Reserva proximaSemExemplar = null;
                if (fila != null) {
                    for (Reserva r : fila) {
                        if (!r.temExemplarSeparado()) {
                            proximaSemExemplar = r;
                            break;
                        }
                    }
                }

                if (proximaSemExemplar != null) {
                    proximaSemExemplar.separarExemplar(tomboSeparado);
                    exemplar.setStatus(StatusExemplar.RESERVADO);
                } else {
                    exemplar.setStatus(StatusExemplar.DISPONIVEL);
                }
            } catch (RecursoNaoEncontradoException ignored) {
            }
        }
    }

    void liberarExemplarSeparado(String tombo) {
        Reserva reserva = encontrarReservaComExemplarSeparado(tombo);
        if (reserva != null) {
            reserva.liberarExemplarSeparado();
        }
    }

    void cancelarReservasDoUsuario(String matricula) {
        List<String> idsParaCancelar = new ArrayList<>();
        for (Reserva r : reservasPorId.values()) {
            if (r.getMatricula().equals(matricula)) {
                idsParaCancelar.add(r.getId());
            }
        }
        for (String id : idsParaCancelar) {
            try {
                cancelarReserva(id);
            } catch (RecursoNaoEncontradoException ignored) {
            }
        }
    }

    private Reserva encontrarReservaComExemplarSeparado(String tombo) {
        for (Reserva r : reservasPorId.values()) {
            if (tombo.equals(r.getTomboExemplarSeparado())) {
                return r;
            }
        }
        return null;
    }

    private void removerReservaAtendida(Reserva reserva) {
        reservasPorId.remove(reserva.getId());
        List<Reserva> fila = reservasPorItem.get(reserva.getCodigoDoItem());
        if (fila != null) {
            fila.remove(reserva);
            atualizarPosicoesFila(fila);
        }
    }

    private void atualizarPosicoesFila(List<Reserva> fila) {
        for (int i = 0; i < fila.size(); i++) {
            fila.get(i).setPosicaoNaFila(i + 1);
        }
    }

    List<DadosPersistencia.EmprestimoDTO> exportarEmprestimos() {
        List<DadosPersistencia.EmprestimoDTO> lista = new ArrayList<>();
        for (Emprestimo e : emprestimosPorId.values()) {
            lista.add(new DadosPersistencia.EmprestimoDTO(
                    e.getId(), e.getTombo(), e.getMatricula(),
                    e.getDataDoEmprestimo(), e.getDataPrevistaDeDevolucao(),
                    e.getDataDaDevolucao(), e.getStatus(), e.getSequencia()
            ));
        }
        return lista;
    }

    List<DadosPersistencia.ReservaDTO> exportarReservas() {
        List<DadosPersistencia.ReservaDTO> lista = new ArrayList<>();
        for (Reserva r : reservasPorId.values()) {
            lista.add(new DadosPersistencia.ReservaDTO(
                    r.getId(), r.getCodigoDoItem(), r.getMatricula(),
                    r.getDataDaReserva(), r.getPosicaoNaFila(),
                    r.getTomboExemplarSeparado()
            ));
        }
        return lista;
    }

    long getContadorEmprestimos() {
        return contadorEmprestimos.get();
    }

    long getContadorReservas() {
        return contadorReservas.get();
    }

    void importar(List<DadosPersistencia.EmprestimoDTO> empList,
                  List<DadosPersistencia.ReservaDTO> resList,
                  long proximoIdEmp, long proximoIdRes) {

        emprestimosPorId.clear();
        emprestimoAtivoDoExemplar.clear();
        reservasPorItem.clear();
        reservasPorId.clear();

        contadorEmprestimos.set(proximoIdEmp);
        contadorReservas.set(proximoIdRes);

        for (DadosPersistencia.EmprestimoDTO dto : empList) {
            Emprestimo emp = new Emprestimo(
                    dto.id(), dto.tombo(), dto.matricula(),
                    dto.dataDoEmprestimo(), dto.dataPrevistaDeDevolucao(),
                    dto.dataDaDevolucao(), dto.status(), dto.sequencia()
            );
            emprestimosPorId.put(emp.getId(), emp);
            if (emp.isAtivo()) {
                emprestimoAtivoDoExemplar.put(emp.getTombo(), emp);
            }
        }

        for (DadosPersistencia.ReservaDTO dto : resList) {
            Reserva res = new Reserva(
                    dto.id(), dto.codigoDoItem(), dto.matricula(),
                    dto.dataDaReserva(), dto.posicaoNaFila(), dto.tomboExemplarSeparado()
            );
            reservasPorId.put(res.getId(), res);
            reservasPorItem.computeIfAbsent(res.getCodigoDoItem(), k -> new ArrayList<>()).add(res);
        }

        for (List<Reserva> fila : reservasPorItem.values()) {
            fila.sort(Comparator.comparingInt(Reserva::getPosicaoNaFila));
        }
    }
}
