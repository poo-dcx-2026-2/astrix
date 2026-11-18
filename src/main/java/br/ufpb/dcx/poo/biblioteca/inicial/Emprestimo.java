package br.ufpb.dcx.poo.biblioteca.inicial;

import java.time.LocalDate;

import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoService;
import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoView;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusEmprestimo;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;

/**
 * Entidade de domínio representando um empréstimo de exemplar.
 */
public class Emprestimo {

    private final String id;
    private final String tombo;
    private final String matricula;
    private final LocalDate dataDoEmprestimo;
    private final LocalDate dataPrevistaDeDevolucao;
    private LocalDate dataDaDevolucao;
    private StatusEmprestimo status;
    private final long sequencia;

    public Emprestimo(String id, String tombo, String matricula, LocalDate data, long sequencia) {
        if (id == null || id.isBlank()) throw new DadosInvalidosException("Id obrigatório");
        if (tombo == null || tombo.isBlank()) throw new DadosInvalidosException("Tombo obrigatório");
        if (matricula == null || matricula.isBlank()) throw new DadosInvalidosException("Matrícula obrigatória");
        if (data == null) throw new DadosInvalidosException("Data obrigatória");

        this.id = id;
        this.tombo = tombo;
        this.matricula = matricula;
        this.dataDoEmprestimo = data;
        this.dataPrevistaDeDevolucao = data.plusDays(EmprestimoService.PRAZO_EM_DIAS);
        this.dataDaDevolucao = null;
        this.status = StatusEmprestimo.ATIVO;
        this.sequencia = sequencia;
    }

    public Emprestimo(String id, String tombo, String matricula, LocalDate dataDoEmprestimo,
                      LocalDate dataPrevistaDeDevolucao, LocalDate dataDaDevolucao,
                      StatusEmprestimo status, long sequencia) {
        this.id = id;
        this.tombo = tombo;
        this.matricula = matricula;
        this.dataDoEmprestimo = dataDoEmprestimo;
        this.dataPrevistaDeDevolucao = dataPrevistaDeDevolucao;
        this.dataDaDevolucao = dataDaDevolucao;
        this.status = status;
        this.sequencia = sequencia;
    }

    public void finalizar(LocalDate dataDevolucao) {
        if (dataDevolucao == null) {
            throw new DadosInvalidosException("Data de devolução não pode ser nula.");
        }
        if (dataDevolucao.isBefore(dataDoEmprestimo)) {
            throw new DadosInvalidosException("Data de devolução anterior à data do empréstimo.");
        }
        this.dataDaDevolucao = dataDevolucao;
        this.status = StatusEmprestimo.DEVOLVIDO;
    }

    public boolean isAtivo() {
        return this.status == StatusEmprestimo.ATIVO;
    }

    public EmprestimoView toView() {
        return new EmprestimoView(id, tombo, matricula, dataDoEmprestimo,
                dataPrevistaDeDevolucao, dataDaDevolucao, status);
    }

    public EmprestimoView toViewAtrasado() {
        return new EmprestimoView(id, tombo, matricula, dataDoEmprestimo,
                dataPrevistaDeDevolucao, dataDaDevolucao, StatusEmprestimo.ATRASADO);
    }

    public String getId() { return id; }
    public String getTombo() { return tombo; }
    public String getMatricula() { return matricula; }
    public LocalDate getDataDoEmprestimo() { return dataDoEmprestimo; }
    public LocalDate getDataPrevistaDeDevolucao() { return dataPrevistaDeDevolucao; }
    public LocalDate getDataDaDevolucao() { return dataDaDevolucao; }
    public StatusEmprestimo getStatus() { return status; }
    public long getSequencia() { return sequencia; }
}
