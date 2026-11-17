package br.ufpb.dcx.poo.biblioteca.inicial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.ufpb.dcx.poo.biblioteca.contrato.UsuarioService;
import br.ufpb.dcx.poo.biblioteca.contrato.UsuarioView;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.OperacaoNaoPermitidaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoDuplicadoException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoNaoEncontradoException;
import br.ufpb.dcx.poo.biblioteca.persistencia.DadosPersistencia;

public class UsuariosEmMemoria implements UsuarioService {

    private final Map<String, Usuario> usuariosPorMatricula = new LinkedHashMap<>();
    private EmprestimosEmMemoria emprestimos;

    public void setEmprestimos(EmprestimosEmMemoria emprestimos) {
        this.emprestimos = emprestimos;
    }

    @Override
    public void cadastrarUsuario(String matricula, String nome)
            throws RecursoDuplicadoException {

        if (matricula == null || matricula.isBlank()) {
            throw new DadosInvalidosException("A matrícula é obrigatória.");
        }
        if (nome == null || nome.isBlank()) {
            throw new DadosInvalidosException("O nome é obrigatório.");
        }

        String matriculaFormatada = matricula.trim();
        if (usuariosPorMatricula.containsKey(matriculaFormatada)) {
            throw new RecursoDuplicadoException("Já existe usuário com a matrícula " + matriculaFormatada);
        }

        Usuario usuario = new Usuario(matriculaFormatada, nome.trim());
        usuariosPorMatricula.put(matriculaFormatada, usuario);
    }

    @Override
    public UsuarioView buscarUsuario(String matricula) throws RecursoNaoEncontradoException {
        if (matricula == null || matricula.isBlank()) {
            throw new DadosInvalidosException("Matrícula não pode ser nula ou vazia.");
        }

        Usuario usuario = usuariosPorMatricula.get(matricula.trim());
        if (usuario == null) {
            throw new RecursoNaoEncontradoException("Usuário não encontrado: " + matricula);
        }

        return paraView(usuario);
    }

    @Override
    public List<UsuarioView> listarUsuarios() {
        List<UsuarioView> resultado = new ArrayList<>();
        for (Usuario usuario : usuariosPorMatricula.values()) {
            resultado.add(paraView(usuario));
        }
        resultado.sort(Comparator.comparing(UsuarioView::nome, String.CASE_INSENSITIVE_ORDER));
        return Collections.unmodifiableList(resultado);
    }

    @Override
    public void desativarUsuario(String matricula)
            throws RecursoNaoEncontradoException, OperacaoNaoPermitidaException {

        if (matricula == null || matricula.isBlank()) {
            throw new DadosInvalidosException(
                    "Matrícula não pode ser nula ou vazia.");
        }

        Usuario usuario = usuariosPorMatricula.get(matricula.trim());

        if (usuario == null) {
            throw new RecursoNaoEncontradoException(
                    "Usuário não encontrado: " + matricula);
        }

        if (usuario.getEmprestimosAtivos() > 0) {
            throw new OperacaoNaoPermitidaException(
                    "Não é possível desativar um usuário com empréstimos ativos.");
        }

        // Cancelar reservas ativas do usuário ao ser desativado (decisão documentada da equipe)
        if (emprestimos != null) {
            emprestimos.cancelarReservasDoUsuario(matricula.trim());
        }

        usuario.setAtivo(false);
    }

    @Override
    public void reativarUsuario(String matricula)
            throws RecursoNaoEncontradoException {

        if (matricula == null || matricula.isBlank()) {
            throw new DadosInvalidosException(
                    "Matrícula não pode ser nula ou vazia.");
        }

        Usuario usuario = usuariosPorMatricula.get(matricula.trim());

        if (usuario == null) {
            throw new RecursoNaoEncontradoException(
                    "Usuário não encontrado: " + matricula);
        }

        usuario.setAtivo(true);
    }

    private UsuarioView paraView(Usuario usuario) {
        return new UsuarioView(
                usuario.getMatricula(),
                usuario.getNome(),
                usuario.isAtivo(),
                usuario.getEmprestimosAtivos());
    }

    Usuario obterUsuario(String matricula)
            throws RecursoNaoEncontradoException {

        if (matricula == null || matricula.isBlank()) {
            throw new DadosInvalidosException(
                    "Matrícula não pode ser nula ou vazia.");
        }

        Usuario usuario = usuariosPorMatricula.get(matricula.trim());

        if (usuario == null) {
            throw new RecursoNaoEncontradoException(
                    "Usuário não encontrado: " + matricula);
        }

        return usuario;
    }

    List<DadosPersistencia.UsuarioDTO> exportarUsuarios() {
        List<DadosPersistencia.UsuarioDTO> lista = new ArrayList<>();
        for (Usuario u : usuariosPorMatricula.values()) {
            lista.add(new DadosPersistencia.UsuarioDTO(
                    u.getMatricula(), u.getNome(), u.isAtivo(), u.getEmprestimosAtivos()
            ));
        }
        return lista;
    }

    void importar(List<DadosPersistencia.UsuarioDTO> dtos) {
        usuariosPorMatricula.clear();
        for (DadosPersistencia.UsuarioDTO dto : dtos) {
            Usuario u = new Usuario(dto.matricula(), dto.nome());
            u.setAtivo(dto.ativo());
            u.setEmprestimosAtivos(dto.emprestimosAtivos());
            usuariosPorMatricula.put(u.getMatricula(), u);
        }
    }
}
