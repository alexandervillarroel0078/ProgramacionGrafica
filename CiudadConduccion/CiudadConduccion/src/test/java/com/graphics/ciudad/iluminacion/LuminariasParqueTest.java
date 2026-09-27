package com.graphics.ciudad.iluminacion; // Prueba las luminarias globo junto al resto de la iluminación.

import com.graphics.ciudad.mundo.Basureros; // Los basureros no chocan con las luminarias.
import com.graphics.ciudad.mundo.Decoracion; // Pasos peatonales.
import com.graphics.ciudad.mundo.Mapa; // Parques y calles.
import com.graphics.ciudad.mundo.Parque; // Luminarias, árboles y bancos.
import com.graphics.ciudad.mundo.Senalizacion; // Semáforos, PARE y carteles.
import java.util.List; // Tipo de las listas.
import java.util.regex.Matcher; // Lectura del shader.
import java.util.regex.Pattern;
import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/**
 * Comprueba las luminarias peatonales tipo GLOBO de los parques sin abrir una ventana OpenGL. Todo se deriva del MAPA
 * actual: vale igual con 13 × 13.
 */
public class LuminariasParqueTest extends TestCase {

    /** Cada parque tiene al menos 1 luminaria y como mucho LUMINARIAS_MAX_POR_PARQUE. */
    public void testCadaParqueTieneLuminaria() {
        assertFalse(Mapa.parques().isEmpty());
        for (int[] p : Mapa.parques()) {
            int n = Parque.luminarias(p[0], p[1]).size();
            assertTrue("parque " + p[0] + "," + p[1] + " sin luminaria", n >= 1);
            assertTrue(n <= Parque.LUMINARIAS_MAX_POR_PARQUE);
        }
    }

    /** Todas entran en uGlobos, y el tamaño del arreglo del shader coincide con Iluminacion.MAX_GLOBOS. */
    public void testEntranEnElShader() throws Exception {
        assertTrue(Parque.LUMINARIAS.size() <= Iluminacion.MAX_GLOBOS);
        String frag = new String(Iluminacion.class.getResourceAsStream("/shaders/iluminacion.frag").readAllBytes(), "UTF-8");
        Matcher m = Pattern.compile("const int MAX_GLOBOS = (\\d+);").matcher(frag);
        assertTrue("falta MAX_GLOBOS en iluminacion.frag", m.find());
        assertEquals(Iluminacion.MAX_GLOBOS, Integer.parseInt(m.group(1)));
        assertTrue("uGlobos debe usar MAX_GLOBOS", frag.contains("uniform vec3 uGlobos[MAX_GLOBOS];"));
    }

    /** El globo mide entre 3 y 3.5 de alto sobre el césped, y la luz está en el centro del globo dibujado. */
    public void testAlturaDelGlobo() {
        float punta = Parque.ALTURA_GLOBO + Parque.DIAMETRO_GLOBO / 2;
        assertTrue("alto " + punta, punta >= 3f && punta <= 3.5f);
        for (float[] l : Parque.LUMINARIAS) {
            assertEquals(Parque.TOPE_CESPED + Parque.ALTURA_GLOBO, l[1], 1e-5f);
        }
    }

    /** Sobre el borde de un sendero: la base no sale del sendero, lejos de la fuente y dentro del césped. */
    public void testSobreUnSendero() {
        for (int[] p : Mapa.parques()) {
            float cx = Mapa.centro(p[1]);
            float cz = Mapa.centro(p[0]);
            for (float[] l : Parque.luminarias(p[0], p[1])) {
                float rx = Math.abs(l[0] - cx);
                float rz = Math.abs(l[1] - cz);
                float lateral = Math.min(rx, rz); // Distancia al eje del sendero más cercano.
                assertTrue("fuera del sendero", lateral + Parque.ANCHO_BASE_GLOBO / 2 <= Parque.ANCHO_SENDERO / 2 + 1e-4f);
                assertTrue("sobre la fuente", Math.hypot(rx, rz) > Parque.RADIO_FUENTE + Parque.ANCHO_BASE_GLOBO / 2);
                assertTrue("fuera del césped", Math.max(rx, rz) + Parque.ANCHO_BASE_GLOBO / 2 <= Parque.MITAD_CESPED);
                assertFalse(Mapa.esCalleEn(l[0], l[1])); // Nunca en la calzada.
                assertFalse(Decoracion.hayPasoSobre(l[0], l[1], Parque.ANCHO_BASE_GLOBO / 2, Parque.ANCHO_BASE_GLOBO / 2));
            }
        }
    }

    /** No choca con árboles, bancos, farolas, semáforos, PARE, carteles, basureros ni con otra luminaria. */
    public void testSinChoques() {
        float globo = Parque.DIAMETRO_GLOBO / 2;
        float base = Parque.ANCHO_BASE_GLOBO / 2;
        for (int[] p : Mapa.parques()) {
            List<float[]> propias = Parque.luminarias(p[0], p[1]);
            for (float[] l : propias) {
                for (float[] a : Parque.arboles(p[0], p[1])) {
                    assertTrue("árbol", Math.hypot(l[0] - a[0], l[1] - a[1]) >= a[4] / 2 + globo);
                }
                for (float[] b : Parque.bancos(p[0], p[1])) {
                    assertTrue("banco", Math.hypot(l[0] - b[0], l[1] - b[1]) >= Parque.MITAD_BANCO + base);
                }
                for (float[] poste : Iluminacion.POSTES) {
                    assertTrue("farola", Math.hypot(l[0] - poste[0], l[1] - poste[1]) >= Parque.MITAD_FAROLA + globo);
                }
                for (float[] s : Senalizacion.SEMAFOROS) {
                    assertTrue("semáforo", Math.hypot(l[0] - s[0], l[1] - s[1]) >= Parque.MITAD_SEMAFORO + globo);
                }
                for (float[] s : Senalizacion.PARES) {
                    assertTrue("PARE", Math.hypot(l[0] - s[0], l[1] - s[1]) >= Parque.MITAD_PARE + globo);
                }
                for (float[] c : Senalizacion.CARTELES_SECTOR) {
                    assertTrue("cartel", Math.hypot(l[0] - c[1], l[1] - c[2]) >= Parque.MITAD_CARTEL + globo);
                }
                for (float[] b : Basureros.UBICACIONES) {
                    assertTrue("basurero", Math.hypot(l[0] - b[0], l[1] - b[2]) >= base + Basureros.RADIO);
                }
                for (float[] otra : propias) {
                    assertTrue("dos luminarias juntas", otra == l || Math.hypot(l[0] - otra[0], l[1] - otra[1]) > 1);
                }
            }
        }
    }

    /** Determinístico: el mismo resultado en cada llamada (sin Random). */
    public void testDeterministico() {
        for (int[] p : Mapa.parques()) {
            List<float[]> a = Parque.luminarias(p[0], p[1]);
            List<float[]> b = Parque.luminarias(p[0], p[1]);
            assertEquals(a.size(), b.size());
            for (int i = 0; i < a.size(); i++) {
                assertEquals(a.get(i)[0], b.get(i)[0], 0f);
                assertEquals(a.get(i)[1], b.get(i)[1], 0f);
            }
        }
    }
}
