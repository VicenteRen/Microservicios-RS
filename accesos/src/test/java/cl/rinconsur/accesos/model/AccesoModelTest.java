package cl.rinconsur.accesos.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccesoModelNormalizacionTest {

    @Test
    void normalizarDatos_eliminaEspaciosYConvierteCodigoAMayusculas() {
        Acceso acceso = new Acceso();
        acceso.setCodigo("  a1  ");
        acceso.setNombre("  Acceso Norte  ");
        acceso.setActivo(false);

        acceso.normalizarDatos();

        assertAll(
            () -> assertEquals("A1", acceso.getCodigo()),
            () -> assertEquals("Acceso Norte", acceso.getNombre()),
            () -> assertFalse(acceso.getActivo())
        );
    }

    @Test
    void normalizarDatos_siActivoEsNuloLoActivaPorDefecto() {
        Acceso acceso = new Acceso();
        acceso.setCodigo(" b2 ");
        acceso.setNombre(" Entrada Sur ");
        acceso.setActivo(null);

        acceso.normalizarDatos();

        assertAll(
            () -> assertEquals("B2", acceso.getCodigo()),
            () -> assertEquals("Entrada Sur", acceso.getNombre()),
            () -> assertTrue(acceso.getActivo())
        );
    }

    @Test
    void normalizarDatos_admiteCodigoYNombreNulosSinLanzarExcepcion() {
        Acceso acceso = new Acceso();
        acceso.setCodigo(null);
        acceso.setNombre(null);
        acceso.setActivo(null);

        assertDoesNotThrow(acceso::normalizarDatos);

        assertAll(
            () -> assertNull(acceso.getCodigo()),
            () -> assertNull(acceso.getNombre()),
            () -> assertTrue(acceso.getActivo())
        );
    }
}