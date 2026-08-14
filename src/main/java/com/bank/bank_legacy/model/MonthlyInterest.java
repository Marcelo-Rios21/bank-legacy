package com.bank.bank_legacy.model;

import java.math.BigDecimal;

public class MonthlyInterest {

    private Long cuentaId;
    private String nombre;
    private BigDecimal saldoInicial;
    private Integer edad;
    private String tipo;
    private BigDecimal interes;
    private BigDecimal saldoFinal;

    public MonthlyInterest() {
    }

    public MonthlyInterest(
            Long cuentaId,
            String nombre,
            BigDecimal saldoInicial,
            Integer edad,
            String tipo,
            BigDecimal interes,
            BigDecimal saldoFinal) {

        this.cuentaId = cuentaId;
        this.nombre = nombre;
        this.saldoInicial = saldoInicial;
        this.edad = edad;
        this.tipo = tipo;
        this.interes = interes;
        this.saldoFinal = saldoFinal;
    }

    public Long getCuentaId() {
        return cuentaId;
    }

    public void setCuentaId(Long cuentaId) {
        this.cuentaId = cuentaId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getSaldoInicial() {
        return saldoInicial;
    }

    public void setSaldoInicial(BigDecimal saldoInicial) {
        this.saldoInicial = saldoInicial;
    }

    public Integer getEdad() {
        return edad;
    }

    public void setEdad(Integer edad) {
        this.edad = edad;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getInteres() {
        return interes;
    }

    public void setInteres(BigDecimal interes) {
        this.interes = interes;
    }

    public BigDecimal getSaldoFinal() {
        return saldoFinal;
    }

    public void setSaldoFinal(BigDecimal saldoFinal) {
        this.saldoFinal = saldoFinal;
    }

    @Override
    public String toString() {
        return "MonthlyInterest{" +
                "cuentaId=" + cuentaId +
                ", nombre='" + nombre + '\'' +
                ", saldoInicial=" + saldoInicial +
                ", edad=" + edad +
                ", tipo='" + tipo + '\'' +
                ", interes=" + interes +
                ", saldoFinal=" + saldoFinal +
                '}';
    }
}