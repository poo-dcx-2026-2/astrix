package br.ufpb.dcx.poo.biblioteca.persistencia;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusEmprestimo;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusExemplar;

/**
 * Snapshot serializável do estado completo da biblioteca para persistência em disco.
 */
public record DadosPersistencia(
        List<ItemDTO> itens,
        List<ExemplarDTO> exemplares,
        List<UsuarioDTO> usuarios,
        List<EmprestimoDTO> emprestimos,
        List<ReservaDTO> reservas,
        long proximoIdEmprestimo,
        long proximoIdReserva
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public static DadosPersistencia vazio() {
        return new DadosPersistencia(
                List.of(), List.of(), List.of(), List.of(), List.of(), 0L, 0L
        );
    }

    public record ItemDTO(String codigo, String titulo, String autoria, String categoria, int ano) implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    public record ExemplarDTO(String tombo, String codigoDoItem, StatusExemplar status) implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    public record UsuarioDTO(String matricula, String nome, boolean ativo, int emprestimosAtivos) implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    public record EmprestimoDTO(
            String id,
            String tombo,
            String matricula,
            LocalDate dataDoEmprestimo,
            LocalDate dataPrevistaDeDevolucao,
            LocalDate dataDaDevolucao,
            StatusEmprestimo status,
            long sequencia
    ) implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    public record ReservaDTO(
            String id,
            String codigoDoItem,
            String matricula,
            LocalDate dataDaReserva,
            int posicaoNaFila,
            String tomboExemplarSeparado
    ) implements Serializable {
        private static final long serialVersionUID = 1L;
    }
}
