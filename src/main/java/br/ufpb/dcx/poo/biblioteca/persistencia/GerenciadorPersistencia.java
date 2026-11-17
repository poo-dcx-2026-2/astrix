package br.ufpb.dcx.poo.biblioteca.persistencia;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.PersistenciaException;

/**
 * Responsável exclusiva pelo salvamento e carregamento dos dados da biblioteca em arquivo.
 * Isola o domínio de qualquer operação de entrada e saída (E/S).
 */
public class GerenciadorPersistencia {

    private static final String NOME_ARQUIVO = "biblioteca.dat";

    private Path obterCaminhoArquivo() {
        String diretorioProp = System.getProperty("biblioteca.dados", "dados");
        Path dir = Paths.get(diretorioProp);
        return dir.resolve(NOME_ARQUIVO);
    }

    public void salvar(DadosPersistencia dados) throws PersistenciaException {
        Path arquivo = obterCaminhoArquivo();
        try {
            if (arquivo.getParent() != null) {
                Files.createDirectories(arquivo.getParent());
            }
            try (OutputStream os = Files.newOutputStream(arquivo);
                 ObjectOutputStream oos = new ObjectOutputStream(os)) {
                oos.writeObject(dados);
                oos.flush();
            }
        } catch (IOException e) {
            PersistenciaException pe = new PersistenciaException("Falha ao salvar os dados da biblioteca: " + e.getMessage());
            pe.initCause(e);
            throw pe;
        }
    }

    public DadosPersistencia carregar() throws PersistenciaException {
        Path arquivo = obterCaminhoArquivo();
        if (!Files.exists(arquivo)) {
            // Arquivo ou diretório ausente equivale a acervo vazio: não falha, devolve vazio
            return DadosPersistencia.vazio();
        }

        try (InputStream is = Files.newInputStream(arquivo);
             ObjectInputStream ois = new ObjectInputStream(is)) {
            Object obj = ois.readObject();
            if (!(obj instanceof DadosPersistencia dados)) {
                throw new PersistenciaException("Arquivo corrompido: formato incompatível.");
            }
            validarIntegridade(dados);
            return dados;
        } catch (IOException | ClassNotFoundException e) {
            PersistenciaException pe = new PersistenciaException("Arquivo corrompido ou erro de leitura: " + e.getMessage());
            pe.initCause(e);
            throw pe;
        }
    }

    private void validarIntegridade(DadosPersistencia dados) throws PersistenciaException {
        if (dados.itens() == null || dados.exemplares() == null
                || dados.usuarios() == null || dados.emprestimos() == null
                || dados.reservas() == null) {
            throw new PersistenciaException("Arquivo corrompido: coleções obrigatórias nulas.");
        }

        Set<String> codigosItens = new HashSet<>();
        for (DadosPersistencia.ItemDTO item : dados.itens()) {
            if (item.codigo() == null || item.codigo().isBlank()) {
                throw new PersistenciaException("Arquivo corrompido: item com código inválido.");
            }
            codigosItens.add(item.codigo());
        }

        Set<String> tombos = new HashSet<>();
        for (DadosPersistencia.ExemplarDTO ex : dados.exemplares()) {
            if (ex.tombo() == null || ex.tombo().isBlank()) {
                throw new PersistenciaException("Arquivo corrompido: exemplar com tombo inválido.");
            }
            if (!codigosItens.contains(ex.codigoDoItem())) {
                throw new PersistenciaException("Arquivo corrompido: exemplar faz referência a item inexistente: " + ex.codigoDoItem());
            }
            tombos.add(ex.tombo());
        }

        Set<String> matriculas = new HashSet<>();
        for (DadosPersistencia.UsuarioDTO u : dados.usuarios()) {
            if (u.matricula() == null || u.matricula().isBlank()) {
                throw new PersistenciaException("Arquivo corrompido: usuário com matrícula inválida.");
            }
            matriculas.add(u.matricula());
        }

        for (DadosPersistencia.EmprestimoDTO emp : dados.emprestimos()) {
            if (emp.id() == null || emp.id().isBlank()) {
                throw new PersistenciaException("Arquivo corrompido: empréstimo com id inválido.");
            }
            if (!tombos.contains(emp.tombo())) {
                throw new PersistenciaException("Arquivo corrompido: referência a tombo inexistente no empréstimo: " + emp.tombo());
            }
            if (!matriculas.contains(emp.matricula())) {
                throw new PersistenciaException("Arquivo corrompido: referência a usuário inexistente no empréstimo: " + emp.matricula());
            }
        }

        for (DadosPersistencia.ReservaDTO res : dados.reservas()) {
            if (res.id() == null || res.id().isBlank()) {
                throw new PersistenciaException("Arquivo corrompido: reserva com id inválido.");
            }
            if (!codigosItens.contains(res.codigoDoItem())) {
                throw new PersistenciaException("Arquivo corrompido: referência a item inexistente na reserva: " + res.codigoDoItem());
            }
            if (!matriculas.contains(res.matricula())) {
                throw new PersistenciaException("Arquivo corrompido: referência a usuário inexistente na reserva: " + res.matricula());
            }
            if (res.tomboExemplarSeparado() != null && !tombos.contains(res.tomboExemplarSeparado())) {
                throw new PersistenciaException("Arquivo corrompido: referência a tombo inexistente na reserva: " + res.tomboExemplarSeparado());
            }
        }
    }
}
