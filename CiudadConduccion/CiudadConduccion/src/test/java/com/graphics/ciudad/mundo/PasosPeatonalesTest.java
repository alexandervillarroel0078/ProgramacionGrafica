package com.graphics.ciudad.mundo; // Prueba los pasos peatonales desde su mismo paquete.

import java.util.List; // Tipo de la lista de ubicaciones.
import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba ubicación y forma de los pasos peatonales sin abrir una ventana OpenGL. */
public class PasosPeatonalesTest extends TestCase {

    private static final float EPSILON = 1e-4f; // Tolerancia para comparar bordes calculados en float.

    /** Celda {fila, columna} donde está el centro de un paso. */
    private static int[] celda(float[] paso) {
        return new int[] {Mapa.indiceCelda(paso[1]), Mapa.indiceCelda(paso[0])}; // Z da la fila y X la columna.
    }

    /** Indica si la celda es un cruce controlado (con semáforo o con PARE). */
    private static boolean esCruceControlado(int fila, int columna) {
        for (int[] cruce : Decoracion.crucesControlados()) { // Revisa la lista.
            if (cruce[0] == fila && cruce[1] == columna) { // Coincide.
                return true; // Tiene semáforo o PARE.
            }
        }
        return false; // Sin control.
    }

    /** Cada paso está sobre calle, a SEPARACION_CRUCE del borde de un cruce, con LARGO_PASO en el sentido de circulación y de vereda a vereda. */
    public void testUbicacionYForma() {
        List<float[]> pasos = Decoracion.UBICACIONES_PASOS; // Lista generada.
        assertFalse(pasos.isEmpty()); // Debe haber pasos.
        boolean hayEjeX = false; // Algún paso en calle este-oeste.
        boolean hayEjeZ = false; // Algún paso en calle norte-sur.
        for (float[] paso : pasos) { // Revisa cada paso.
            boolean ejeX = paso[2] == 1; // Orientación de la calle que cruza.
            hayEjeX |= ejeX; // Registra la orientación.
            hayEjeZ |= !ejeX; // Registra la orientación.
            float[] mitad = Decoracion.mitadesPaso(ejeX); // Medias medidas del rectángulo del paso.
            String donde = "paso en " + paso[0] + "," + paso[1]; // Mensaje de error útil.
            for (int sx = -1; sx <= 1; sx += 2) { // Esquinas izquierda y derecha.
                for (int sz = -1; sz <= 1; sz += 2) { // Esquinas de arriba y de abajo.
                    assertTrue(donde, Mapa.esCalleEn(paso[0] + sx * (mitad[0] - EPSILON), paso[1] + sz * (mitad[1] - EPSILON))); // Toda la pintura sobre calle.
                }
            }
            float largo = 2 * (ejeX ? mitad[0] : mitad[1]); // Medida en el sentido de circulación.
            float travesia = 2 * (ejeX ? mitad[1] : mitad[0]); // Medida a lo ancho de la calle.
            assertEquals(donde, Decoracion.LARGO_PASO, largo, EPSILON); // LARGO_PASO en el sentido de circulación.
            assertTrue(donde, travesia <= Mapa.TAM_CELDA && travesia >= Mapa.TAM_CELDA - 1); // De vereda a vereda.
            int[] c = celda(paso); // Celda del paso.
            int[] haciaCruce = ejeX ? new int[] {0, paso[0] > Mapa.centro(c[1]) ? 1 : -1} : new int[] {paso[1] > Mapa.centro(c[0]) ? 1 : -1, 0}; // Lado del cruce más cercano.
            assertTrue(donde, Mapa.esInterseccion(c[0] + haciaCruce[0], c[1] + haciaCruce[1])); // La celda vecina es una intersección.
            assertFalse(donde, Mapa.esInterseccion(c[0], c[1])); // El paso no está dentro del cruce.
            float sobreEje = ejeX ? paso[0] : paso[1]; // Coordenada a lo largo de la calle.
            float centroCelda = ejeX ? Mapa.centro(c[1]) : Mapa.centro(c[0]); // Centro de la celda del paso.
            float holgura = Mapa.TAM_CELDA / 2 - Math.abs(sobreEje - centroCelda) - Decoracion.LARGO_PASO / 2; // Asfalto entre el paso y el cruce.
            assertEquals(donde, Decoracion.SEPARACION_CRUCE, holgura, EPSILON); // No tapa la esquina: deja SEPARACION_CRUCE libre.
        }
        assertTrue(hayEjeX && hayEjeZ); // Hay pasos en ambas orientaciones.
    }

    /**
     * Cada paso está junto a un cruce controlado (semáforo o PARE) o a un parque; TODOS los accesos de los cruces
     * controlados y cada parque tienen su paso. Se deriva del mapa: vale igual con 13 × 13.
     */
    public void testDondeVanLosPasos() {
        for (float[] paso : Decoracion.UBICACIONES_PASOS) { // Revisa cada paso.
            int[] c = celda(paso); // Celda del paso.
            boolean junto = false; // Se vuelve true si hay un cruce con semáforo o un parque vecino.
            for (int[] v : Mapa.VECINOS) { // Celdas vecinas.
                int f = c[0] + v[0]; // Fila vecina.
                int col = c[1] + v[1]; // Columna vecina.
                boolean dentro = f >= 0 && f < Mapa.MAPA.length && col >= 0 && col < Mapa.MAPA[0].length; // Dentro del mapa.
                junto |= dentro && (esCruceControlado(f, col) || Mapa.tipo(f, col) == Mapa.PARQUE); // Semáforo, PARE o parque.
            }
            assertTrue("paso sin motivo en " + paso[0] + "," + paso[1], junto); // Solo donde corresponde.
        }
        assertTrue(Decoracion.crucesControlados().size() > Senalizacion.INTERSECCIONES_SEMAFORO.size()); // También los de PARE.
        for (int[] cruce : Decoracion.crucesControlados()) { // Cruces con semáforo o PARE.
            for (int[] acceso : Mapa.accesos(cruce[0], cruce[1])) { // Cada acceso.
                float[] esperado = Decoracion.pasoEnAcceso(cruce[0], cruce[1], acceso[0], acceso[1]); // Paso que debería existir.
                assertTrue(Decoracion.hayPasoSobre(esperado[0], esperado[1], 0.01f, 0.01f)); // Existe.
            }
        }
        for (int[] parque : Mapa.parques()) { // Cada parque.
            boolean tienePaso = false; // Se vuelve true si una calle vecina tiene paso.
            for (int[] v : Mapa.VECINOS) { // Calles alrededor del parque.
                float x = Mapa.centro(parque[1] + v[1]); // Centro X de la celda vecina.
                float z = Mapa.centro(parque[0] + v[0]); // Centro Z de la celda vecina.
                tienePaso |= Decoracion.hayPasoSobre(x, z, Mapa.TAM_CELDA / 2 - 0.01f, Mapa.TAM_CELDA / 2 - 0.01f); // Algún paso en esa celda.
            }
            assertTrue("parque sin paso en " + parque[0] + "," + parque[1], tienePaso); // Cada parque tiene al menos uno.
        }
    }

    /** Ninguna marca amarilla visible queda debajo de un paso, y el resto de la línea central se conserva. */
    public void testLineaCentralCortadaEnLosPasos() {
        int marcas = 0; // Cantidad total de marcas visibles.
        for (int fila = 0; fila < Mapa.MAPA.length; fila++) { // Recorre las filas.
            for (int columna = 0; columna < Mapa.MAPA[fila].length; columna++) { // Recorre las columnas.
                if (!Mapa.esCalle(fila, columna)) { // Solo las calles tienen marcas.
                    continue; // Salta las manzanas.
                }
                for (float[] m : Ciudad.marcasDeCelda(fila, columna, Mapa.centro(columna), Mapa.centro(fila))) { // Marcas visibles.
                    assertFalse(Decoracion.hayPasoSobre(m[0], m[1], m[2] / 2, m[3] / 2)); // Ninguna pisa un paso.
                    marcas++; // Cuenta la marca.
                }
            }
        }
        assertTrue(marcas > 100); // La línea central sigue existiendo fuera de los pasos.
    }

    /**
     * El semáforo y el PARE quedan DETRÁS del paso (del lado de donde viene el auto), no sobre las franjas: el auto se
     * detiene antes del paso. Se mide a lo largo de la calle, desde el borde del cruce.
     */
    public void testSenalesDetrasDelPaso() {
        float finDelPaso = Decoracion.SEPARACION_CRUCE + Decoracion.LARGO_PASO; // Del borde del cruce al final del paso.
        assertEquals(finDelPaso, Senalizacion.RETROCESO_SEMAFORO, EPSILON);
        assertEquals(finDelPaso, Senalizacion.RETROCESO_PARE, EPSILON);
        for (int[] pare : Senalizacion.UBICACIONES_PARE) { // Cada PARE tiene su paso delante.
            float[] paso = Decoracion.pasoEnAcceso(pare[0], pare[1], pare[2], pare[3]);
            assertTrue(Decoracion.hayPasoSobre(paso[0], paso[1], 0.01f, 0.01f));
        }
    }
}
