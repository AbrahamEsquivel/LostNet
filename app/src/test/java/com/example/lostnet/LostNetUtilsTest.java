package com.example.lostnet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

/** Pruebas unitarias de la lógica pura de la app (se ejecutan en la JVM, sin emulador). */
public class LostNetUtilsTest {

    private static final double LAT_UAA = 21.9139;
    private static final double LON_UAA = -102.3155;

    private static List<ReporteModelo> reportes(String json) {
        return new Gson().fromJson(json, new TypeToken<List<ReporteModelo>>() { }.getType());
    }

    // ---------- Enmascarado de datos de contacto ----------

    @Test
    public void enmascararEmail_nombreLargo_dejaTresLetras() {
        assertEquals("jos***@gmail.com", LostNetUtils.enmascararEmail("josimar@gmail.com"));
    }

    @Test
    public void enmascararEmail_nombreCorto_ocultaTodo() {
        assertEquals("***@uaa.mx", LostNetUtils.enmascararEmail("ana@uaa.mx"));
    }

    @Test
    public void enmascararEmail_nuloOVacio() {
        assertEquals("Sin Email", LostNetUtils.enmascararEmail(null));
        assertEquals("Sin Email", LostNetUtils.enmascararEmail(""));
    }

    @Test
    public void enmascararEmail_sinArroba_regresaIgual() {
        assertEquals("usuario", LostNetUtils.enmascararEmail("usuario"));
    }

    @Test
    public void enmascararTelefono_largo_muestraExtremos() {
        assertEquals("44****67", LostNetUtils.enmascararTelefono("4491234567"));
    }

    @Test
    public void enmascararTelefono_cortoONulo() {
        assertEquals("****", LostNetUtils.enmascararTelefono("1234"));
        assertEquals("Sin Teléfono", LostNetUtils.enmascararTelefono(null));
    }

    // ---------- WhatsApp ----------

    @Test
    public void numeroWhatsApp_agregaLadaMexico() {
        assertEquals("524491234567", LostNetUtils.numeroWhatsApp("449 123-4567"));
    }

    @Test
    public void numeroWhatsApp_conMas_quitaSimbolo() {
        assertEquals("524491234567", LostNetUtils.numeroWhatsApp("+52 449 123 4567"));
    }

    @Test
    public void numeroWhatsApp_yaConLada_noDuplica() {
        assertEquals("524491234567", LostNetUtils.numeroWhatsApp("524491234567"));
    }

    // ---------- Filtros ----------

    @Test
    public void coincideCategoria_todosMuestraCualquiera() {
        assertTrue(LostNetUtils.coincideCategoria("Mascotas", "Todos"));
        assertTrue(LostNetUtils.coincideCategoria(null, "Todos"));
    }

    @Test
    public void coincideCategoria_ignoraMayusculas() {
        assertTrue(LostNetUtils.coincideCategoria("electrónicos", "Electrónicos"));
        assertFalse(LostNetUtils.coincideCategoria("Llaves", "Electrónicos"));
    }

    @Test
    public void coincideCategoria_sinCategoriaCuentaComoOtros() {
        assertTrue(LostNetUtils.coincideCategoria(null, "Otros"));
    }

    @Test
    public void filtrarPorUsuario_soloLosMios() {
        List<ReporteModelo> lista = reportes("[{\"id\":\"1\",\"user_id\":\"u1\"},"
                + "{\"id\":\"2\",\"user_id\":\"u2\"},{\"id\":\"3\",\"user_id\":\"u1\"},{\"id\":\"4\"}]");
        List<ReporteModelo> mios = LostNetUtils.filtrarPorUsuario(lista, "u1");
        assertEquals(2, mios.size());
        assertEquals("1", mios.get(0).getId());
        assertEquals("3", mios.get(1).getId());
    }

    @Test
    public void filtrarPorUsuario_entradasNulas() {
        assertTrue(LostNetUtils.filtrarPorUsuario(null, "u1").isEmpty());
        assertTrue(LostNetUtils.filtrarPorUsuario(Collections.<ReporteModelo>emptyList(), null).isEmpty());
    }

    // ---------- Distancia y cercanía ----------

    @Test
    public void distancia_mismoPunto_esCero() {
        assertEquals(0.0, LostNetUtils.distanciaMetros(LAT_UAA, LON_UAA, LAT_UAA, LON_UAA), 0.001);
    }

    @Test
    public void distancia_unGradoDeLatitud_aprox111Km() {
        assertEquals(111_195, LostNetUtils.distanciaMetros(0, 0, 1, 0), 50);
    }

    @Test
    public void filtrarAjenosCercanos_excluyeMiosYLejanos() {
        // r1: ajeno a ~1 km  -> entra
        // r2: mío (mismo correo, otra capitalización) -> fuera
        // r3: ajeno a ~11 km -> fuera
        List<ReporteModelo> lista = reportes("["
                + "{\"id\":\"r1\",\"email\":\"otro@uaa.mx\",\"lat\":21.9229,\"lon\":-102.3155},"
                + "{\"id\":\"r2\",\"email\":\"YO@uaa.mx\",\"lat\":21.9139,\"lon\":-102.3155},"
                + "{\"id\":\"r3\",\"email\":\"otro@uaa.mx\",\"lat\":22.0139,\"lon\":-102.3155}]");
        List<ReporteModelo> cercanos = LostNetUtils.filtrarAjenosCercanos(lista, "yo@uaa.mx", LAT_UAA, LON_UAA);
        assertEquals(1, cercanos.size());
        assertEquals("r1", cercanos.get(0).getId());
    }

    // ---------- Textos de expiración ----------

    @Test
    public void textoExpiracionAlerta_conDias() {
        assertEquals("⏳ El reporte se elimina en 2d 3h",
                LostNetUtils.textoExpiracionAlerta(2 * 86_400 + 3 * 3_600 + 59));
    }

    @Test
    public void textoExpiracionAlerta_soloHoras() {
        assertEquals("⏳ El reporte se elimina en 5h 30m",
                LostNetUtils.textoExpiracionAlerta(5 * 3_600 + 30 * 60));
    }

    @Test
    public void textoExpiracionAlerta_menosDeUnaHora() {
        assertEquals("⚠️ El reporte expira muy pronto", LostNetUtils.textoExpiracionAlerta(1_200));
    }

    @Test
    public void textoExpiracionReporte_futuro() {
        long ahoraMs = 1_000_000_000_000L;
        long expira = ahoraMs / 1000 + 86_400 + 2 * 3_600;
        assertEquals("⏳ Se elimina en 1d 2h", LostNetUtils.textoExpiracionReporte(expira, ahoraMs));
    }

    @Test
    public void textoExpiracionReporte_yaVencido() {
        long ahoraMs = 1_000_000_000_000L;
        assertEquals("⚠️ Expira pronto", LostNetUtils.textoExpiracionReporte(ahoraMs / 1000 - 10, ahoraMs));
    }
}
