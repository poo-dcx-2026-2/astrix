package br.ufpb.dcx.poo.biblioteca.inicial;

import java.time.LocalDate;

import br.ufpb.dcx.poo.biblioteca.contrato.ReservaView;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;

/**
 * Entidade de domínio representando uma reserva de item.
 */
public class Reserva {

    private final String id;
    private final String codigoDoItem;
    private final String matricula;
    private final LocalDate dataDaReserva;
    private int posicaoNaFila;
    private String tomboExemplarSeparado;

    public Reserva(String id, String codigoDoItem, String matricula, LocalDate dataDaReserva, int posicaoNaFila) {
        if (id == null || id.isBlank()) throw new DadosInvalidosException("Id obrigatório");
        if (codigoDoItem == null || codigoDoItem.isBlank()) throw new DadosInvalidosException("Código do item obrigatório");
        if (matricula == null || matricula.isBlank()) throw new DadosInvalidosException("Matrícula obrigatória");
        if (dataDaReserva == null) throw new DadosInvalidosException("Data da reserva obrigatória");
        if (posicaoNaFila < 1) throw new DadosInvalidosException("Posição na fila deve ser >= 1");

        this.id = id;
        this.codigoDoItem = codigoDoItem;
        this.matricula = matricula;
        this.dataDaReserva = dataDaReserva;
        this.posicaoNaFila = posicaoNaFila;
        this.tomboExemplarSeparado = null;
    }

    public Reserva(String id, String codigoDoItem, String matricula, LocalDate dataDaReserva,
                   int posicaoNaFila, String tomboExemplarSeparado) {
        this.id = id;
        this.codigoDoItem = codigoDoItem;
        this.matricula = matricula;
        this.dataDaReserva = dataDaReserva;
        this.posicaoNaFila = posicaoNaFila;
        this.tomboExemplarSeparado = tomboExemplarSeparado;
    }

    public void setPosicaoNaFila(int posicaoNaFila) {
        if (posicaoNaFila < 1) throw new DadosInvalidosException("Posição na fila deve ser >= 1");
        this.posicaoNaFila = posicaoNaFila;
    }

    public void separarExemplar(String tombo) {
        if (tombo == null || tombo.isBlank()) throw new DadosInvalidosException("Tombo obrigatório");
        this.tomboExemplarSeparado = tombo;
    }

    public void liberarExemplarSeparado() {
        this.tomboExemplarSeparado = null;
    }

    public boolean temExemplarSeparado() {
        return this.tomboExemplarSeparado != null;
    }

    public String getTomboExemplarSeparado() {
        return tomboExemplarSeparado;
    }

    public ReservaView toView() {
        return new ReservaView(id, codigoDoItem, matricula, dataDaReserva, posicaoNaFila);
    }

    public String getId() { return id; }
    public String getCodigoDoItem() { return codigoDoItem; }
    public String getMatricula() { return matricula; }
    public LocalDate getDataDaReserva() { return dataDaReserva; }
    public int getPosicaoNaFila() { return posicaoNaFila; }
}
