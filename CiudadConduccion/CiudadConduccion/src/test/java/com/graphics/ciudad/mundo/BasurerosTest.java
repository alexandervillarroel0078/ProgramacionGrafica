package com.graphics.ciudad.mundo; // Prueba los basureros desde su mismo paquete.

import com.graphics.ciudad.iluminacion.Iluminacion; // Postes de farola.
import com.graphics.ciudad.vehiculo.Colisiones; // Las calles siguen transitables.
import java.util.List; // Tipo de las listas.
import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba la ubicación de los basureros sin abrir una ventana OpenGL. Todo se deriva del MAPA: vale con 13 × 13. */
public class BasurerosTest extends TestCase {

    private static final float R = Basureros.RADIO; // Radio del basurero.

    /** Escala pedida: 0.9 de alto y 0.5 de diámetro, con una tapa. */
    public void testEscala() {
        assertEquals(0.9f, Basureros.ALTO, 1e-6f);
        assertEquals(0.5f, Basureros.DIAMETRO, 1e-6f);
        assertTrue(Basureros.ALTO_TAPA > 0 && Basureros.EXCESO_TAPA > 0);
    }

    /** Hay basureros de los tres tipos. */
    public void testHayDeLosTresTipos() {
        int[] n = new int[3];
        for (float[] b : Basureros.UBICACIONES) {
            n[(int) b[3]]++;
        }
        assertTrue("parques", n[Basureros.EN_PARQUE] > 0);
        assertTrue("esquinas", n[Basureros.EN_ESQUINA] > 0);
        assertTrue("negocios", n[Basureros.JUNTO_A_NEGOCIO] > 0);
    }

    /** Ninguno toca la calzada: el círculo entero queda dentro de una manzana. */
    public void testNuncaEnLaCalzada() {
        for (float[] b : Basureros.UBICACIONES) {
            for (int k = 0; k < 16; k++) { // Puntos del borde del basurero.
                double ang = 2 * Math.PI * k / 16;
                float x = b[0] + (float) Math.cos(ang) * R;
                float z = b[2] + (float) Math.sin(ang) * R;
                assertFalse("en la calle: " + b[0] + "," + b[2], Mapa.esCalleEn(x, z));
            }
        }
    }

    /** Ninguno está sobre un paso ni en la franja de vereda donde desemboca. */
    public void testNoTapaPasos() {
        for (float[] b : Basureros.UBICACIONES) {
            assertFalse(Decoracion.hayPasoSobre(b[0], b[2], R, R));
            assertFalse("tapa la salida de un paso", Basureros.tocaPasoOSuSalida(b[0], b[2]));
        }
    }

    /** Los de vereda: solo en manzanas con edificio (no en veredas de parque), fuera del edificio y sin tapar negocios. */
    public void testVeredas() {
        float mitad = Mapa.ANCHO_EDIFICIO / 2;
        for (float[] b : Basureros.UBICACIONES) {
            if (b[3] == Basureros.EN_PARQUE) {
                continue;
            }
            int fila = Mapa.indiceCelda(b[2]);
            int columna = Mapa.indiceCelda(b[0]);
            assertEquals("vereda de parque", Mapa.EDIFICIO, Mapa.tipo(fila, columna));
            float dx = Math.abs(b[0] - Mapa.centro(columna));
            float dz = Math.abs(b[2] - Mapa.centro(fila));
            assertTrue("dentro del edificio", dx >= mitad + R || dz >= mitad + R);
            assertFalse("tapa puerta o vidriera", Basureros.tapaNegocio(fila, columna, b[0], b[2]));
            assertEquals(Iluminacion.ALTURA_ACERA, b[1], 0f); // Apoyado en la acera.
        }
    }

    /** Los de esquina van sobre la franja de mobiliario: entre el borde del toldo y el cordón. */
    public void testEsquinasEnLaFranjaDeMobiliario() {
        for (float[] b : Basureros.UBICACIONES) {
            if (b[3] != Basureros.EN_ESQUINA) {
                continue;
            }
            float dx = Math.abs(b[0] - Mapa.centro(Mapa.indiceCelda(b[0])));
            float dz = Math.abs(b[2] - Mapa.centro(Mapa.indiceCelda(b[2])));
            float haciaCordon = Math.max(dx, dz); // Del centro de la manzana, hacia la calle.
            assertTrue(haciaCordon - R >= Mapa.ANCHO_EDIFICIO / 2 + Fachada.VUELO_TOLDO);
            assertTrue(haciaCordon + R <= Mapa.TAM_CELDA / 2);
        }
    }

    /** Los de parque: sobre el césped, fuera de senderos y fuente, junto a un banco y sin tocarlo. */
    public void testParques() {
        for (int[] p : Mapa.parques()) {
            float cx = Mapa.centro(p[1]);
            float cz = Mapa.centro(p[0]);
            List<float[]> bancos = Parque.bancos(p[0], p[1]);
            for (float[] b : Basureros.UBICACIONES) {
                if (b[3] != Basureros.EN_PARQUE || Mapa.indiceCelda(b[0]) != p[1] || Mapa.indiceCelda(b[2]) != p[0]) {
                    continue;
                }
                float rx = Math.abs(b[0] - cx);
                float rz = Math.abs(b[2] - cz);
                assertTrue("fuera del césped", Math.max(rx, rz) + R <= Parque.MITAD_CESPED);
                assertTrue("sobre un sendero", Math.min(rx, rz) >= Parque.ANCHO_SENDERO / 2 + R);
                assertTrue("en la fuente", Math.hypot(rx, rz) >= Parque.RADIO_FUENTE + R);
                assertEquals(Parque.TOPE_CESPED, b[1], 0f);
                double cercano = Double.MAX_VALUE;
                for (float[] banco : bancos) {
                    double d = Math.hypot(b[0] - banco[0], b[2] - banco[1]);
                    assertTrue("sobre un banco", d >= Parque.MITAD_BANCO + R);
                    cercano = Math.min(cercano, d);
                }
                assertTrue("lejos de los bancos", cercano <= Parque.MITAD_BANCO + Basureros.SEPARACION_BANCO + R + Basureros.RETIRO_BANCO + 0.01);
            }
        }
    }

    /** Distancias mínimas a farolas, semáforos, PARE, carteles, árboles, luminarias y entre basureros. */
    public void testDistanciasMinimas() {
        List<float[]> todos = Basureros.UBICACIONES;
        for (float[] b : todos) {
            for (float[] poste : Iluminacion.POSTES) {
                assertTrue("farola", Math.hypot(b[0] - poste[0], b[2] - poste[1]) >= R + Parque.MITAD_FAROLA);
            }
            for (float[] s : Senalizacion.SEMAFOROS) {
                assertTrue("semáforo", Math.hypot(b[0] - s[0], b[2] - s[1]) >= R + Parque.MITAD_SEMAFORO);
            }
            for (float[] s : Senalizacion.PARES) {
                assertTrue("PARE", Math.hypot(b[0] - s[0], b[2] - s[1]) >= R + Parque.MITAD_PARE);
            }
            for (float[] c : Senalizacion.CARTELES_SECTOR) {
                assertTrue("cartel", Math.hypot(b[0] - c[1], b[2] - c[2]) >= R + Parque.MITAD_CARTEL);
            }
            for (int[] p : Mapa.parques()) {
                for (float[] a : Parque.arboles(p[0], p[1])) {
                    assertTrue("árbol", Math.hypot(b[0] - a[0], b[2] - a[1]) >= Basureros.distanciaMinimaArbol(a));
                }
            }
            for (float[] l : Parque.LUMINARIAS) {
                assertTrue("luminaria", Math.hypot(b[0] - l[0], b[2] - l[2]) >= R + Parque.ANCHO_BASE_GLOBO / 2);
            }
            for (float[] otro : todos) {
                assertTrue("dos basureros juntos", otro == b || Math.hypot(b[0] - otro[0], b[2] - otro[2]) >= Basureros.DISTANCIA_MIN_ENTRE_BASUREROS);
            }
        }
    }

    /** Determinístico: recalcular da exactamente lo mismo (sin Random). */
    public void testDeterministico() {
        List<float[]> otra = Basureros.calcular();
        assertEquals(Basureros.UBICACIONES.size(), otra.size());
        for (int i = 0; i < otra.size(); i++) {
            for (int k = 0; k < 4; k++) {
                assertEquals(Basureros.UBICACIONES.get(i)[k], otra.get(i)[k], 0f);
            }
        }
    }

    /** Las calles siguen transitables: los basureros no agregan obstáculos. */
    public void testColisionesIntactas() {
        for (int fila = 0; fila < Mapa.MAPA.length; fila++) {
            for (int columna = 0; columna < Mapa.MAPA[fila].length; columna++) {
                if (Mapa.esCalle(fila, columna)) {
                    assertTrue(Colisiones.puedeCircular(Mapa.centro(columna), Mapa.centro(fila)));
                }
            }
        }
    }

    /** Los basureros "junto a negocios" solo van en edificios con planta baja COMERCIAL (no lobbies, departamentos ni casas). */
    public void testNegociosSoloEnComercio() {
        for (float[] b : Basureros.UBICACIONES) {
            if ((int) b[3] != Basureros.JUNTO_A_NEGOCIO) {
                continue;
            }
            int fila = Mapa.indiceCelda(b[2]);
            int columna = Mapa.indiceCelda(b[0]);
            assertEquals(fila + "," + columna, UsoPlantaBaja.COMERCIAL, UsoPlantaBaja.de(fila, columna));
        }
    }
}
