package com.example.lostnet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Response;

/**
 * Pruebas de integración entre la capa de red de la app (Retrofit + Gson) y la API.
 * MockWebServer levanta un servidor HTTP local que responde como el backend Flask,
 * así se verifica qué petición manda la app y cómo interpreta la respuesta.
 */
public class LostNetApiIntegrationTest {

    private MockWebServer servidor;
    private LostNetApi api;

    @Before
    public void setUp() throws Exception {
        servidor = new MockWebServer();
        servidor.start();
        api = RetrofitClient.crearApi(servidor.url("/api/").toString());
    }

    @After
    public void tearDown() throws Exception {
        servidor.shutdown();
    }

    private RecordedRequest siguientePeticion() throws InterruptedException {
        RecordedRequest req = servidor.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull("La app no envió ninguna petición", req);
        return req;
    }

    private static RequestBody texto(String valor) {
        return RequestBody.create(MediaType.parse("text/plain"), valor);
    }

    @Test
    public void obtenerReportes_parseaCamposDelBackend() throws Exception {
        servidor.enqueue(new MockResponse().setResponseCode(200).setBody("[{"
                + "\"id\":\"abc123\",\"desc_short\":\"Mochila negra\",\"lat\":21.91,\"lon\":-102.31,"
                + "\"user_id\":\"u1\",\"email\":\"a@uaa.mx\",\"phone\":\"4491234567\",\"category\":\"Mochilas\","
                + "\"status\":\"lost\",\"photo_url\":\"/photos/x.jpg\","
                + "\"expires_at_epoch\":1790000000,\"seconds_until_expiry\":3600}]"));

        Response<List<ReporteModelo>> resp = api.obtenerReportes().execute();

        RecordedRequest req = siguientePeticion();
        assertEquals("GET", req.getMethod());
        assertEquals("/api/reportes", req.getPath());

        assertTrue(resp.isSuccessful());
        ReporteModelo r = resp.body().get(0);
        assertEquals("abc123", r.getId());
        assertEquals("Mochila negra", r.getDescription());
        assertEquals(21.91, r.getLatitude(), 1e-9);
        assertEquals(-102.31, r.getLongitude(), 1e-9);
        assertEquals("Mochilas", r.getCategory());
        assertEquals(1790000000L, r.getExpiresAtEpoch());
        assertEquals(3600L, r.getSecondsUntilExpiry());
    }

    @Test
    public void obtenerPuntosSeguros_parseaLista() throws Exception {
        servidor.enqueue(new MockResponse().setBody(
                "[{\"id\":1,\"nombre\":\"Biblioteca UAA\",\"lat\":21.91,\"lon\":-102.31,\"tipo\":\"Oficial\"}]"));

        Response<List<PuntoSeguroModelo>> resp = api.obtenerPuntosSeguros().execute();

        assertEquals("/api/puntos-seguros", siguientePeticion().getPath());
        assertEquals(1, resp.body().size());
        assertEquals("Biblioteca UAA", resp.body().get(0).getNombre());
        assertEquals("Oficial", resp.body().get(0).getTipo());
    }

    @Test
    public void obtenerAlertas_mandaEmailEnQuery() throws Exception {
        servidor.enqueue(new MockResponse().setBody(
                "[{\"id\":\"a1\",\"message\":\"Objeto cerca\",\"read\":false,"
                        + "\"lat_objeto\":21.9,\"lon_objeto\":-102.3,\"seconds_until_expiry\":7200}]"));

        Response<List<AlertaModelo>> resp = api.obtenerAlertas("yo@uaa.mx").execute();

        RecordedRequest req = siguientePeticion();
        assertEquals("/api/mis-alertas", req.getRequestUrl().encodedPath());
        assertEquals("yo@uaa.mx", req.getRequestUrl().queryParameter("email"));
        AlertaModelo a = resp.body().get(0);
        assertEquals("Objeto cerca", a.getMessage());
        assertFalse(a.isRead());
        assertEquals(7200L, a.getSecondsUntilExpiry());
    }

    @Test
    public void obtenerComentarios_mandaReportId() throws Exception {
        servidor.enqueue(new MockResponse().setBody(
                "[{\"user_name\":\"Ana\",\"text\":\"Lo vi en la cafetería\",\"timestamp\":1700000000}]"));

        Response<List<ComentarioModelo>> resp = api.obtenerComentarios("abc123").execute();

        RecordedRequest req = siguientePeticion();
        assertEquals("abc123", req.getRequestUrl().queryParameter("report_id"));
        assertEquals("Ana", resp.body().get(0).getUserName());
        assertEquals("Lo vi en la cafetería", resp.body().get(0).getText());
    }

    @Test
    public void enviarComentario_mandaJson() throws Exception {
        servidor.enqueue(new MockResponse().setResponseCode(201).setBody("{\"ok\":true}"));

        HashMap<String, Object> body = new HashMap<>();
        body.put("report_id", "abc123");
        body.put("user_name", "Ana");
        body.put("text", "Hola");
        Response<Void> resp = api.enviarComentario(body).execute();

        RecordedRequest req = siguientePeticion();
        assertEquals("POST", req.getMethod());
        assertEquals("/api/comentar", req.getPath());
        assertTrue(req.getHeader("Content-Type").startsWith("application/json"));
        String json = req.getBody().readUtf8();
        assertTrue(json.contains("\"report_id\":\"abc123\""));
        assertTrue(json.contains("\"text\":\"Hola\""));
        assertTrue(resp.isSuccessful());
    }

    @Test
    public void enviarReporte_mandaMultipartConTodosLosCampos() throws Exception {
        servidor.enqueue(new MockResponse().setResponseCode(201).setBody("{\"status\":\"ok\"}"));

        MultipartBody.Part foto = MultipartBody.Part.createFormData("foto", "foto.jpg",
                RequestBody.create(MediaType.parse("image/jpeg"), new byte[]{1, 2, 3}));
        Response<ResponseBody> resp = api.enviarReporte(
                texto("u1"), texto("a@uaa.mx"), texto("4491234567"), texto("Cartera café"),
                texto("Carteras"), texto("21.91"), texto("-102.31"),
                texto("¿Color?"), texto("café"), texto("lost"), foto).execute();

        RecordedRequest req = siguientePeticion();
        assertEquals("POST", req.getMethod());
        assertEquals("/api/reportar", req.getPath());
        assertTrue(req.getHeader("Content-Type").startsWith("multipart/form-data"));
        String cuerpo = req.getBody().readUtf8();
        String[] campos = {"user_id", "email", "phone", "description", "category",
                "latitude", "longitude", "security_question", "security_answer", "status"};
        for (String campo : campos) {
            assertTrue("Falta el campo " + campo, cuerpo.contains("name=\"" + campo + "\""));
        }
        assertTrue(cuerpo.contains("name=\"foto\"; filename=\"foto.jpg\""));
        assertTrue(resp.isSuccessful());
    }

    @Test
    public void enviarReporte_limiteDiario_regresa429() throws Exception {
        servidor.enqueue(new MockResponse().setResponseCode(429)
                .setBody("{\"error\":\"Límite de 5 reportes por día alcanzado\"}"));

        Response<ResponseBody> resp = api.enviarReporte(
                texto("u1"), texto("a@uaa.mx"), texto("1"), texto("x"), texto("Otros"),
                texto("0"), texto("0"), texto("q"), texto("a"), texto("lost"), null).execute();

        assertFalse(resp.isSuccessful());
        assertEquals(429, resp.code());
        assertTrue(resp.errorBody().string().contains("Límite"));
    }

    @Test
    public void borrarReporte_usaDeleteConId() throws Exception {
        servidor.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true}"));

        Response<ResponseBody> resp = api.borrarReporte("abc123").execute();

        RecordedRequest req = siguientePeticion();
        assertEquals("DELETE", req.getMethod());
        assertEquals("/api/reportes/abc123", req.getPath());
        assertTrue(resp.isSuccessful());
    }

    @Test
    public void obtenerTipoUsuario_parseaTipo() throws Exception {
        servidor.enqueue(new MockResponse().setBody("{\"tipo\":\"sabueso\"}"));

        Response<UsuarioGamificacion> resp = api.obtenerTipoUsuario("yo@uaa.mx").execute();

        RecordedRequest req = siguientePeticion();
        assertEquals("/api/usuario/tipo", req.getRequestUrl().encodedPath());
        assertEquals("yo@uaa.mx", req.getRequestUrl().queryParameter("email"));
        assertEquals("sabueso", resp.body().getTipo());
    }

    @Test
    public void errorDelServidor_noRevientaLaApp() throws Exception {
        servidor.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));

        Response<List<ReporteModelo>> resp = api.obtenerReportes().execute();

        assertFalse(resp.isSuccessful());
        assertEquals(500, resp.code());
    }
}
