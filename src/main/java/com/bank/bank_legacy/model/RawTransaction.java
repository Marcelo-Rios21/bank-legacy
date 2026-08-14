package com.bank.bank_legacy.model;

public class RawTransaction {

    private String id;
    private String fecha;
    private String monto;
    private String tipo;

    public RawTransaction() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getMonto() {
        return monto;
    }

    public void setMonto(String monto) {
        this.monto = monto;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    @Override
    public String toString() {
        return "RawTransaction{" +
                "id='" + id + '\'' +
                ", fecha='" + fecha + '\'' +
                ", monto='" + monto + '\'' +
                ", tipo='" + tipo + '\'' +
                '}';
    }
}