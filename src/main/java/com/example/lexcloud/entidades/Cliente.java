package com.example.lexcloud.entidades;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_firma", nullable = false)
    private Firma firma;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_cliente", nullable = false)
    private Tipo_Cliente tipoCliente;

    @Column(name = "fecha_vinculacion")
    private LocalDate fechaVinculacion;

    @Column(name = "activo")
    private Boolean activo;

    // =========================================================================
    // 1. IDENTIFICACIÓN Y DATOS BÁSICOS
    // =========================================================================

    @Column(name = "identificacion", length = 50)
    private String identificacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_identificacion")
    private Tipo_Identificacion tipoIdentificacion;

    @Column(name = "nombres", length = 100)
    private String nombres;

    @Column(name = "primer_apellido", length = 100)
    private String primerApellido;

    @Column(name = "segundo_apellido", length = 100)
    private String segundoApellido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_genero")
    private Genero genero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_civil")
    private Estado_Civil estadoCivil;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "lugar_nacimiento", length = 150)
    private String lugarNacimiento;

    @Column(name = "nacionalidad", length = 100)
    private String nacionalidad;

    // =========================================================================
    // 2. UBICACIÓN Y CONTACTO
    // =========================================================================

    @Column(name = "ciudad_residencia", length = 150)
    private String ciudadResidencia;

    @Column(name = "localidad_sector", length = 150)
    private String localidadSector;

    @Column(name = "barrio", length = 150)
    private String barrio;

    @Column(name = "direccion_residencia", length = 250)
    private String direccionResidencia;

    @Column(name = "telefono_residencia", length = 20)
    private String telefonoResidencia;

    @Column(name = "direccion_oficina", length = 250)
    private String direccionOficina;

    @Column(name = "telefono_oficina", length = 20)
    private String telefonoOficina;

    @Column(name = "telefono_movil", length = 20)
    private String telefonoMovil;

    @Column(name = "correo", length = 250)
    private String correo;

    // =========================================================================
    // 3. CARACTERIZACIÓN SOCIOECONÓMICA
    // =========================================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estrato")
    private Estrato_Economico estrato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vivienda")
    private Tipo_Vivienda vivienda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nivel_salarial")
    private Nivel_Salarial nivelSalarial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_empleado")
    private Tipo_Empleado tipoEmpleado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_grado_escolaridad")
    private Grado_Escolaridad gradoEscolaridad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_discapacidad")
    private Discapacidad discapacidad;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vinculo_Caso> vinculoCasos = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Firma getFirma() {
        return firma;
    }

    public void setFirma(Firma firma) {
        this.firma = firma;
    }

    public Tipo_Cliente getTipoCliente() {
        return tipoCliente;
    }

    public void setTipoCliente(Tipo_Cliente tipoCliente) {
        this.tipoCliente = tipoCliente;
    }

    public LocalDate getFechaVinculacion() {
        return fechaVinculacion;
    }

    public void setFechaVinculacion(LocalDate fechaVinculacion) {
        this.fechaVinculacion = fechaVinculacion;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    public Tipo_Identificacion getTipoIdentificacion() {
        return tipoIdentificacion;
    }

    public void setTipoIdentificacion(Tipo_Identificacion tipoIdentificacion) {
        this.tipoIdentificacion = tipoIdentificacion;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getPrimerApellido() {
        return primerApellido;
    }

    public void setPrimerApellido(String primerApellido) {
        this.primerApellido = primerApellido;
    }

    public String getSegundoApellido() {
        return segundoApellido;
    }

    public void setSegundoApellido(String segundoApellido) {
        this.segundoApellido = segundoApellido;
    }

    public Genero getGenero() {
        return genero;
    }

    public void setGenero(Genero genero) {
        this.genero = genero;
    }

    public Estado_Civil getEstadoCivil() {
        return estadoCivil;
    }

    public void setEstadoCivil(Estado_Civil estadoCivil) {
        this.estadoCivil = estadoCivil;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getLugarNacimiento() {
        return lugarNacimiento;
    }

    public void setLugarNacimiento(String lugarNacimiento) {
        this.lugarNacimiento = lugarNacimiento;
    }

    public String getNacionalidad() {
        return nacionalidad;
    }

    public void setNacionalidad(String nacionalidad) {
        this.nacionalidad = nacionalidad;
    }

    public String getCiudadResidencia() {
        return ciudadResidencia;
    }

    public void setCiudadResidencia(String ciudadResidencia) {
        this.ciudadResidencia = ciudadResidencia;
    }

    public String getLocalidadSector() {
        return localidadSector;
    }

    public void setLocalidadSector(String localidadSector) {
        this.localidadSector = localidadSector;
    }

    public String getBarrio() {
        return barrio;
    }

    public void setBarrio(String barrio) {
        this.barrio = barrio;
    }

    public String getDireccionResidencia() {
        return direccionResidencia;
    }

    public void setDireccionResidencia(String direccionResidencia) {
        this.direccionResidencia = direccionResidencia;
    }

    public String getTelefonoResidencia() {
        return telefonoResidencia;
    }

    public void setTelefonoResidencia(String telefonoResidencia) {
        this.telefonoResidencia = telefonoResidencia;
    }

    public String getDireccionOficina() {
        return direccionOficina;
    }

    public void setDireccionOficina(String direccionOficina) {
        this.direccionOficina = direccionOficina;
    }

    public String getTelefonoOficina() {
        return telefonoOficina;
    }

    public void setTelefonoOficina(String telefonoOficina) {
        this.telefonoOficina = telefonoOficina;
    }

    public String getTelefonoMovil() {
        return telefonoMovil;
    }

    public void setTelefonoMovil(String telefonoMovil) {
        this.telefonoMovil = telefonoMovil;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public Estrato_Economico getEstrato() {
        return estrato;
    }

    public void setEstrato(Estrato_Economico estrato) {
        this.estrato = estrato;
    }

    public Tipo_Vivienda getVivienda() {
        return vivienda;
    }

    public void setVivienda(Tipo_Vivienda vivienda) {
        this.vivienda = vivienda;
    }

    public Nivel_Salarial getNivelSalarial() {
        return nivelSalarial;
    }

    public void setNivelSalarial(Nivel_Salarial nivelSalarial) {
        this.nivelSalarial = nivelSalarial;
    }

    public Tipo_Empleado getTipoEmpleado() {
        return tipoEmpleado;
    }

    public void setTipoEmpleado(Tipo_Empleado tipoEmpleado) {
        this.tipoEmpleado = tipoEmpleado;
    }

    public Grado_Escolaridad getGradoEscolaridad() {
        return gradoEscolaridad;
    }

    public void setGradoEscolaridad(Grado_Escolaridad gradoEscolaridad) {
        this.gradoEscolaridad = gradoEscolaridad;
    }

    public Discapacidad getDiscapacidad() {
        return discapacidad;
    }

    public void setDiscapacidad(Discapacidad discapacidad) {
        this.discapacidad = discapacidad;
    }

    public List<Vinculo_Caso> getVinculoCasos() {
        return vinculoCasos;
    }

    public void setVinculoCasos(List<Vinculo_Caso> vinculoCasos) {
        this.vinculoCasos = vinculoCasos;
    }
}