package cl.rinconsur.usuarios.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AvatarStorageServiceTest {

    @TempDir
    Path tempDir;

    private AvatarStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new AvatarStorageService(tempDir.toString());
    }

    @Test
    void guardar_imagenJpgValida_creaArchivoEnCarpeta() {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "foto.jpg", "image/jpeg", "contenido-imagen".getBytes()
        );

        String nombre = storageService.guardar(1L, archivo);

        assertTrue(nombre.startsWith("usuario_1_"));
        assertTrue(nombre.endsWith(".jpg"));
        assertTrue(Files.exists(tempDir.resolve(nombre)));
    }

    @Test
    void guardar_imagenPngYWebp_asignaExtensionCorrecta() {
        MockMultipartFile png = new MockMultipartFile("archivo", "a.png", "image/png", "data".getBytes());
        MockMultipartFile webp = new MockMultipartFile("archivo", "b.webp", "image/webp", "data".getBytes());
        MockMultipartFile sinTipo = new MockMultipartFile("archivo", "c.jpg", null, "data".getBytes());

        assertTrue(storageService.guardar(2L, png).endsWith(".png"));
        assertTrue(storageService.guardar(3L, webp).endsWith(".webp"));
        assertTrue(storageService.guardar(4L, sinTipo).endsWith(".jpg"));
    }

    @Test
    void guardar_archivoVacioONulo_lanzaBadRequest() {
        MockMultipartFile vacio = new MockMultipartFile("archivo", "vacio.jpg", "image/jpeg", new byte[0]);

        ResponseStatusException ex1 = assertThrows(ResponseStatusException.class, () -> storageService.guardar(1L, vacio));
        ResponseStatusException ex2 = assertThrows(ResponseStatusException.class, () -> storageService.guardar(1L, null));

        assertEquals(HttpStatus.BAD_REQUEST, ex1.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, ex2.getStatusCode());
    }

    @Test
    void guardar_formatoNoPermitido_lanzaBadRequest() {
        MockMultipartFile pdf = new MockMultipartFile("archivo", "doc.pdf", "application/pdf", "pdf".getBytes());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> storageService.guardar(1L, pdf));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void cargar_archivoExistente_devuelveRecursoLegible() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", "bytes".getBytes());
        String nombre = storageService.guardar(1L, archivo);

        Resource recurso = storageService.cargar(nombre);

        assertNotNull(recurso);
        assertTrue(recurso.exists());
        assertTrue(recurso.isReadable());
    }

    @Test
    void cargar_archivoInexistente_lanzaNotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> storageService.cargar("no_existe.jpg"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void cargar_rutaFueraDeDirectorio_lanzaBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> storageService.cargar("../secreto.txt"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void detectarMediaType_segunExtension() {
        assertEquals(MediaType.IMAGE_JPEG, storageService.detectarMediaType("foto.jpg"));
        assertEquals(MediaType.IMAGE_PNG, storageService.detectarMediaType("foto.png"));
        assertEquals(MediaType.parseMediaType("image/webp"), storageService.detectarMediaType("foto.webp"));
    }

    @Test
    void eliminarSiExiste_borraArchivoYNoFallaConNulo() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", "bytes".getBytes());
        String nombre = storageService.guardar(5L, archivo);
        assertTrue(Files.exists(tempDir.resolve(nombre)));

        storageService.eliminarSiExiste("/api/usuarios/avatares/" + nombre);
        assertFalse(Files.exists(tempDir.resolve(nombre)));

        assertDoesNotThrow(() -> storageService.eliminarSiExiste(null));
        assertDoesNotThrow(() -> storageService.eliminarSiExiste("   "));
    }
}