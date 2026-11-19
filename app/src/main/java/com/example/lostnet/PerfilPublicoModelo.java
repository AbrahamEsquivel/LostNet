package com.example.lostnet;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

/** Respuesta de GET /api/usuario/perfil-publico: honor y estadísticas reales del usuario. */
public class PerfilPublicoModelo {

    @SerializedName("rol") private String rol;
    @SerializedName("emblema") private String emblema;
    @SerializedName("confianza") private Integer confianza;
    @SerializedName("reportes_publicados") private Integer reportesPublicados;
    @SerializedName("objetos_perdidos") private Integer objetosPerdidos;
    @SerializedName("objetos_encontrados") private Integer objetosEncontrados;
    @SerializedName("reportes_en_revision") private Integer reportesEnRevision;
    @SerializedName("denuncias_recibidas") private Integer denunciasRecibidas;
    @SerializedName("miembro_desde") private String miembroDesde;
    @SerializedName("dias_en_lostnet") private Integer diasEnLostnet;
    @SerializedName("logros") private List<String> logros;

    public PerfilPublicoModelo() { }

    private static int cero(Integer valor) { return valor == null ? 0 : Math.max(0, valor); }

    public String getRol() { return rol == null ? "BUSCADOR" : rol; }
    public String getEmblema() { return emblema; }
    public int getConfianza() { return Math.min(100, cero(confianza)); }
    public int getReportesPublicados() { return cero(reportesPublicados); }
    public int getObjetosPerdidos() { return cero(objetosPerdidos); }
    public int getObjetosEncontrados() { return cero(objetosEncontrados); }
    public int getReportesEnRevision() { return cero(reportesEnRevision); }
    public int getDenunciasRecibidas() { return cero(denunciasRecibidas); }
    public String getMiembroDesde() { return miembroDesde; }
    /** Días desde el registro, o null si el servidor no lo conoce. */
    public Integer getDiasEnLostnet() { return diasEnLostnet; }
    public List<String> getLogros() { return logros == null ? Collections.emptyList() : logros; }
    public boolean tieneLogro(String logro) { return getLogros().contains(logro); }
}
