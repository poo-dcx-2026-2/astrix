package br.ufpb.dcx.poo.biblioteca.inicial;

import br.ufpb.dcx.poo.biblioteca.contrato.AcervoService;
import br.ufpb.dcx.poo.biblioteca.contrato.Biblioteca;
import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoService;
import br.ufpb.dcx.poo.biblioteca.contrato.RelatorioService;
import br.ufpb.dcx.poo.biblioteca.contrato.UsuarioService;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.PersistenciaException;
import br.ufpb.dcx.poo.biblioteca.persistencia.DadosPersistencia;
import br.ufpb.dcx.poo.biblioteca.persistencia.GerenciadorPersistencia;

/**
 * Ponto central de acesso aos serviços da biblioteca (Padrão Facade).
 * Reúne os serviços de acervo, usuários, empréstimos e relatórios.
 */
public class BibliotecaInicial implements Biblioteca {

    private final AcervoEmMemoria acervo;
    private final UsuariosEmMemoria usuarios;
    private final EmprestimosEmMemoria emprestimos;
    private final RelatoriosNaoImplementados relatorios;
    private final GerenciadorPersistencia gerenciadorPersistencia;

    public BibliotecaInicial() {
        this.acervo = new AcervoEmMemoria();
        this.usuarios = new UsuariosEmMemoria();
        this.emprestimos = new EmprestimosEmMemoria(this.acervo, this.usuarios);
        this.acervo.setEmprestimos(this.emprestimos);
        this.usuarios.setEmprestimos(this.emprestimos);
        this.relatorios = new RelatoriosNaoImplementados();
        this.gerenciadorPersistencia = new GerenciadorPersistencia();
    }

    @Override
    public AcervoService acervo() { return acervo; }

    @Override
    public UsuarioService usuarios() { return usuarios; }

    @Override
    public EmprestimoService emprestimos() { return emprestimos; }

    @Override
    public RelatorioService relatorios() { return relatorios; }

    @Override
    public void salvar() throws PersistenciaException {
        DadosPersistencia dados = new DadosPersistencia(
                acervo.exportarItens(),
                acervo.exportarExemplares(),
                usuarios.exportarUsuarios(),
                emprestimos.exportarEmprestimos(),
                emprestimos.exportarReservas(),
                emprestimos.getContadorEmprestimos(),
                emprestimos.getContadorReservas()
        );
        gerenciadorPersistencia.salvar(dados);
    }

    @Override
    public void carregar() throws PersistenciaException {
        DadosPersistencia dados = gerenciadorPersistencia.carregar();

        // 1ª fase: recria todo o estado em serviços descartáveis. Se algum dado do
        // arquivo violar uma regra do domínio (por exemplo, um Item com ano inválido),
        // o construtor da entidade falha aqui, sem tocar no estado atual da biblioteca.
        AcervoEmMemoria acervoTeste = new AcervoEmMemoria();
        UsuariosEmMemoria usuariosTeste = new UsuariosEmMemoria();
        EmprestimosEmMemoria emprestimosTeste = new EmprestimosEmMemoria(acervoTeste, usuariosTeste);
        try {
            importarEm(dados, acervoTeste, usuariosTeste, emprestimosTeste);
        } catch (RuntimeException e) {
            PersistenciaException pe = new PersistenciaException(
                    "Arquivo com dados inválidos: " + e.getMessage());
            pe.initCause(e);
            throw pe;
        }

        // 2ª fase: os mesmos dados já foram aceitos na 1ª fase, então importá-los
        // no estado real não falha. A memória só muda depois que tudo foi validado.
        importarEm(dados, acervo, usuarios, emprestimos);
    }

    private static void importarEm(DadosPersistencia dados, AcervoEmMemoria acervo,
                                   UsuariosEmMemoria usuarios, EmprestimosEmMemoria emprestimos) {
        acervo.importar(dados.itens(), dados.exemplares());
        usuarios.importar(dados.usuarios());
        emprestimos.importar(
                dados.emprestimos(),
                dados.reservas(),
                dados.proximoIdEmprestimo(),
                dados.proximoIdReserva()
        );
    }
}
