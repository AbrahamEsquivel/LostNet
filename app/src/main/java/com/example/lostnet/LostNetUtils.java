package com.example.lostnet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Lógica pura de la app (sin dependencias de Android).
 * Al estar separada de las Activities puede probarse con pruebas unitarias en la JVM.
 */
public final class LostNetUtils {

    /** Radio de la búsqueda de reportes cercanos, en metros. */
    public static final double RADIO_CERCANOS_METROS = 2500;

    private static final double RADIO_TIERRA_METROS = 6_371_000;

    private LostNetUtils() {
        // Clase utilitaria — no instanciar
    }

    /** Oculta parte del correo: "josimar@gmail.com" -> "jos***@gmail.com". */
    public static String enmascararEmail(String email) {
        if (email == null || email.isEmpty()) return "Sin Email";
        if (!email.contains("@")) return email;

        String[] partes = email.split("@");
        String nombre = partes[0];
        String dominio = partes.length > 1 ? partes[1] : "";

        if (nombre.length() > 3) {
            return nombre.substring(0, 3) + "***@" + dominio;
        }
        return "***@" + dominio;
    }

    /** Deja visibles solo los 2 primeros y 2 últimos dígitos: "4491234567" -> "44****67". */
    public static String enmascararTelefono(String phone) {
        if (phone == null || phone.isEmpty()) return "Sin Teléfono";
        if (phone.length() > 4) {
            return phone.substring(0, 2) + "****" + phone.substring(phone.length() - 2);
        }
        return "****";
    }

    /**
     * Normaliza un teléfono para la API de WhatsApp: quita espacios, guiones y "+",
     * y agrega la lada de México (52) si no la trae.
     */
    public static String numeroWhatsApp(String telefono) {
        if (telefono == null) return "";
        String numero = telefono.replaceAll("\\s+", "").replaceAll("-", "");
        if (!numero.startsWith("+") && !numero.startsWith("52")) {
            numero = "52" + numero;
        }
        return numero.replace("+", "");
    }

    /** Indica si un reporte de cierta categoría debe mostrarse con el filtro elegido. */
    public static boolean coincideCategoria(String categoriaReporte, String filtro) {
        if (filtro == null || filtro.equals("Todos")) return true;
        String categoria = categoriaReporte != null ? categoriaReporte : "Otros";
        return categoria.equalsIgnoreCase(filtro);
    }

    /** Regresa solo los reportes creados por el usuario indicado. */
    public static List<ReporteModelo> filtrarPorUsuario(List<ReporteModelo> reportes, String userId) {
        List<ReporteModelo> filtrados = new ArrayList<>();
        if (reportes == null || userId == null) return filtrados;
        for (ReporteModelo r : reportes) {
            if (userId.equals(r.getUserId())) {
                filtrados.add(r);
            }
        }
        return filtrados;
    }

    /** Distancia en metros entre dos coordenadas (fórmula de Haversine). */
    public static double distanciaMetros(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return RADIO_TIERRA_METROS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** Reportes de otros usuarios a 2.5 km o menos de la ubicación dada. */
    public static List<ReporteModelo> filtrarAjenosCercanos(List<ReporteModelo> reportes,
                                                            String miEmail,
                                                            double miLat, double miLon) {
        List<ReporteModelo> cercanos = new ArrayList<>();
        if (reportes == null) return cercanos;
        for (ReporteModelo r : reportes) {
            if (miEmail != null && r.getEmail() != null && r.getEmail().equalsIgnoreCase(miEmail)) {
                continue;
            }
            if (distanciaMetros(miLat, miLon, r.getLatitude(), r.getLongitude()) <= RADIO_CERCANOS_METROS) {
                cercanos.add(r);
            }
        }
        return cercanos;
    }

    /** Texto de expiración para la lista de alertas, a partir de los segundos restantes. */
    public static String textoExpiracionAlerta(long segundos) {
        long dias    = TimeUnit.SECONDS.toDays(segundos);
        long horas   = TimeUnit.SECONDS.toHours(segundos)   % 24;
        long minutos = TimeUnit.SECONDS.toMinutes(segundos) % 60;
        if (dias > 0) {
            return "⏳ El reporte se elimina en " + dias + "d " + horas + "h";
        } else if (horas > 0) {
            return "⏳ El reporte se elimina en " + horas + "h " + minutos + "m";
        }
        return "⚠️ El reporte expira muy pronto";
    }

    /** Texto de expiración de un reporte, dado su epoch de expiración y la hora actual (ms). */
    public static String textoExpiracionReporte(long expiraEpochSegundos, long ahoraMillis) {
        long restanteMs = (expiraEpochSegundos * 1000L) - ahoraMillis;
        if (restanteMs <= 0) return "⚠️ Expira pronto";
        long dias    = TimeUnit.MILLISECONDS.toDays(restanteMs);
        long horas   = TimeUnit.MILLISECONDS.toHours(restanteMs)   % 24;
        long minutos = TimeUnit.MILLISECONDS.toMinutes(restanteMs) % 60;
        return dias > 0
                ? "⏳ Se elimina en " + dias + "d " + horas + "h"
                : "⏳ Se elimina en " + horas + "h " + minutos + "m";
    }

    /** Antigüedad corta para el perfil: "Hoy", "12d", "4m" o "1 año"; "—" si no se conoce. */
    public static String textoAntiguedad(Integer dias) {
        if (dias == null || dias < 0) return "—";
        if (dias == 0) return "Hoy";
        if (dias < 30) return dias + "d";
        if (dias < 365) return (dias / 30) + "m";
        int anios = dias / 365;
        return anios + (anios == 1 ? " año" : " años");
    }

    /** Texto del nivel de honor según el rango calculado por el servidor. */
    public static String textoNivelHonor(String rol) {
        if ("SABUESO".equalsIgnoreCase(rol)) return "🕵️‍♂️ Sabueso Comunitario (Héroe Local)";
        if ("PERDEDOR".equalsIgnoreCase(rol)) return "🤦‍♂️ En búsqueda activa (Novato)";
        return "🔍 Buscador Confiable";
    }
}
