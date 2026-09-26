package com.graphics.ciudad.iluminacion; // Prueba las farolas desde su mismo paquete.

import com.graphics.ciudad.mundo.Decoracion; // Pasos peatonales.
import com.graphics.ciudad.mundo.Mapa; // Celdas, sectores y vecinos.
import com.graphics.ciudad.mundo.Senalizacion; // Semáforos, PARE y carteles.
import com.graphics.ciudad.vehiculo.Colisiones; // Comprueba que las calles sigan transitables.
import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba que las farolas estén en la vereda, con la bombilla sobre el borde de la calle, sin abrir OpenGL. */
public class FarolasTest extends TestCase {

    private static final float DISTANCIA_MINIMA_SENAL = 3; // Separación mínima entre un poste y un semáforo, PARE o cartel.
    private static final float EPSILON = 1e-4f; // Tolerancia para comparar valores calculados en float.

    /** Cada farola está en una manzana adyacente a una calle por el lado indicado, y ninguna en celda transitable. */
    public void testPosteEnLaVeredaYBombillaSobreElBorde() {
        assertEquals(13, Iluminacion.LUCES.length); // Se mantienen 13 farolas.
        assertTrue(Iluminacion.LUCES.length <= Iluminacion.MAX_LUCES); // No puede superar el arreglo uLuces del shader.
        for (int i = 0; i < Iluminacion.LUCES.length; i++) { // Revisa cada farola.
            int[] f = Iluminacion.LUCES[i]; // {fila, columna, lado}.
            String que = "farola " + f[0] + "," + f[1] + " lado " + f[2]; // Mensaje de error útil.
            assertFalse(que, Mapa.esCalle(f[0], f[1])); // La celda es una manzana (edificio o parque).
            int[] haciaCalle = Mapa.VECINOS[f[2]]; // Dirección del lado indicado.
            assertTrue(que, Mapa.esCalleSegura(f[0] + haciaCalle[0], f[1] + haciaCalle[1])); // Por ese lado hay una calle.
            float[] poste = Iluminacion.POSTES[i]; // Posición del poste.
            float[] bombilla = Iluminacion.BOMBILLAS[i]; // Posición de la bombilla.
            assertFalse(que + ": poste en la calle", Mapa.esCalleEn(poste[0], poste[1])); // El poste está sobre la acera.
            assertEquals(que, f[0], Mapa.indiceCelda(poste[1])); // El poste está en la fila de su manzana.
            assertEquals(que, f[1], Mapa.indiceCelda(poste[0])); // Y en su columna.
            assertTrue(que + ": bombilla fuera de la calle", Mapa.esCalleEn(bombilla[0], bombilla[2])); // La bombilla queda sobre la calle...
            float cordonX = Mapa.centro(f[1]) + haciaCalle[1] * Mapa.TAM_CELDA / 2; // Borde de la manzana en X (si el lado es este u oeste).
            float cordonZ = Mapa.centro(f[0]) + haciaCalle[0] * Mapa.TAM_CELDA / 2; // Borde de la manzana en Z (si el lado es norte o sur).
            float voladizo = haciaCalle[1] != 0 ? Math.abs(bombilla[0] - cordonX) : Math.abs(bombilla[2] - cordonZ); // Cuánto entra en la calle.
            assertEquals(que, Iluminacion.BRAZO_FAROLA - Iluminacion.MARGEN_POSTE, voladizo, EPSILON); // ...sobre el borde: 1.1 más allá del cordón.
            float brazo = (float) Math.hypot(bombilla[0] - poste[0], bombilla[2] - poste[1]); // Distancia poste → bombilla.
            assertEquals(que, Iluminacion.BRAZO_FAROLA, brazo, EPSILON); // La bombilla está en la punta del brazo.
        }
    }

    /** Hay farolas en los cinco sectores, y ninguna coincide con semáforos, PARE, carteles ni pasos peatonales. */
    public void testRepartoYSinConflictos() {
        boolean[] sectores = new boolean[Mapa.SECTORES.length]; // Sectores con al menos una farola.
        for (int i = 0; i < Iluminacion.LUCES.length; i++) { // Revisa cada farola.
            float[] poste = Iluminacion.POSTES[i]; // Posición del poste.
            float[] bombilla = Iluminacion.BOMBILLAS[i]; // Posición de la bombilla.
            sectores[Mapa.sector(poste[0], poste[1])] = true; // Marca el sector.
            for (float[] s : Senalizacion.SEMAFOROS) { // Semáforos.
                assertTrue(Math.hypot(poste[0] - s[0], poste[1] - s[1]) >= DISTANCIA_MINIMA_SENAL); // Lejos de cada cabezal.
            }
            for (float[] p : Senalizacion.PARES) { // Señales de PARE.
                assertTrue(Math.hypot(poste[0] - p[0], poste[1] - p[1]) >= DISTANCIA_MINIMA_SENAL); // Lejos de cada PARE.
            }
            for (float[] c : Senalizacion.CARTELES_SECTOR) { // Carteles de sector.
                assertTrue(Math.hypot(poste[0] - c[1], poste[1] - c[2]) >= DISTANCIA_MINIMA_SENAL); // Lejos de cada cartel.
            }
            assertFalse(Decoracion.hayPasoSobre(poste[0], poste[1], 0.3f, 0.3f)); // El poste no pisa un paso peatonal.
            assertFalse(Decoracion.hayPasoSobre(bombilla[0], bombilla[2], 0.35f, 0.35f)); // La bombilla no cuelga sobre un paso.
        }
        for (boolean hay : sectores) { // Revisa los cinco sectores.
            assertTrue(hay); // Todos tienen farolas.
        }
    }

    /** Las calles siguen transitables: las farolas no agregan obstáculos para el auto ni el tráfico. */
    public void testColisionesIntactas() {
        for (int fila = 0; fila < Mapa.MAPA.length; fila++) { // Recorre las filas.
            for (int columna = 0; columna < Mapa.MAPA[fila].length; columna++) { // Recorre las columnas.
                if (Mapa.esCalle(fila, columna)) { // Solo las calles.
                    assertTrue(Colisiones.puedeCircular(Mapa.centro(columna), Mapa.centro(fila))); // El centro de cada calle es transitable.
                }
            }
        }
    }
}
