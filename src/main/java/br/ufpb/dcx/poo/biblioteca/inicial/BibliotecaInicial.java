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
        // Aplicação atômica: só altera a memória se o carregamento e validação forem bem-sucedidos
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
