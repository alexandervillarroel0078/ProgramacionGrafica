package com.graphics.ciudad.vehiculo; // Prueba la cabina desde su mismo paquete.

import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba la forma de la cabina y de sus ventanillas sin abrir una ventana OpenGL. */
public class CabinaTest extends TestCase {

    private static final float ANCHO_CARROCERIA = 1.65f; // Ancho del cuerpo del auto.
    private static final float TECHO_ANTERIOR = 1.395f; // Altura del techo del bloque celeste que reemplaza.

    /** Base más larga que el techo, parabrisas más inclinado que la luneta, misma altura y dentro del ancho del cuerpo. */
    public void testFormaDelTrapecio() {
        float[][] p = Cabina.PERFIL_CABINA; // {z, y}: abajo atrás, abajo adelante, arriba adelante, arriba atrás.
        float base = Math.abs(p[0][0] - p[1][0]); // Largo de la base.
        float techo = Math.abs(p[3][0] - p[2][0]); // Largo del techo.
        assertTrue(base > techo); // Trapecio: base más larga.
        double inclinacionParabrisas = Math.atan2(Math.abs(p[2][0] - p[1][0]), p[2][1] - p[1][1]); // Ángulo respecto de la vertical.
        double inclinacionLuneta = Math.atan2(Math.abs(p[0][0] - p[3][0]), p[3][1] - p[0][1]); // Ángulo respecto de la vertical.
        assertTrue(inclinacionParabrisas > inclinacionLuneta); // El parabrisas está más acostado.
        assertTrue(p[1][0] < p[0][0]); // El parabrisas está adelante (-Z) y la luneta atrás.
        float alto = Math.max(p[2][1], p[3][1]); // Altura del techo.
        assertEquals(TECHO_ANTERIOR, alto, 0.05f); // Misma altura total aproximada que el bloque anterior.
        float exterior = Cabina.ANCHO_CABINA / 2 + Cabina.SEPARACION_VIDRIO + Cabina.GROSOR_VIDRIO; // Lo más ancho: ventanillas.
        assertTrue(exterior < ANCHO_CARROCERIA / 2); // Nada sobresale del ancho del cuerpo.
        assertTrue(Math.abs(p[0][0]) < 1.3f && Math.abs(p[1][0]) < 1.3f); // Ni del largo del cuerpo (±1.3).
    }

    /** Cada ventanilla queda dentro del costado de la cabina y a su lado del parante central. */
    public void testVentanillasDentroDeLaCabina() {
        float[][] cabina = Cabina.PERFIL_CABINA; // Contorno de la cabina.
        for (boolean delantera : new boolean[] {true, false}) { // Ambas ventanillas.
            float[][] ventanilla = Cabina.perfilVentanilla(delantera); // Trapecio de la ventanilla.
            for (float[] punto : ventanilla) { // Cada esquina.
                assertTrue(dentroDelContorno(cabina, punto[0], punto[1])); // Dentro del costado de la cabina.
                if (delantera) { // Ventanilla delantera: adelante del parante.
                    assertTrue(punto[0] <= Cabina.CENTRO_PARANTE - Cabina.ANCHO_PARANTE / 2 + 1e-6f); // No invade el parante.
                } else { // Ventanilla trasera: atrás del parante.
                    assertTrue(punto[0] >= Cabina.CENTRO_PARANTE + Cabina.ANCHO_PARANTE / 2 - 1e-6f); // No invade el parante.
                }
            }
        }
    }

    /** Indica si (z, y) está dentro del contorno convexo: del mismo lado de todas sus aristas. */
    private static boolean dentroDelContorno(float[][] contorno, float z, float y) {
        int signo = 0; // Signo común de los productos cruz (0 = todavía no visto).
        for (int i = 0; i < contorno.length; i++) { // Cada arista.
            float[] a = contorno[i]; // Inicio de la arista.
            float[] b = contorno[(i + 1) % contorno.length]; // Fin de la arista.
            float cruz = (b[0] - a[0]) * (y - a[1]) - (b[1] - a[1]) * (z - a[0]); // Lado del punto respecto de la arista.
            int s = (int) Math.signum(cruz); // +1, -1 o 0 (sobre la arista).
            if (s != 0) { // Sobre la arista cuenta como adentro.
                if (signo != 0 && s != signo) { // Cambió de lado: está afuera.
                    return false; // Fuera del contorno.
                }
                signo = s; // Recuerda el lado.
            }
        }
        return true; // Del mismo lado de todas las aristas.
    }
}
