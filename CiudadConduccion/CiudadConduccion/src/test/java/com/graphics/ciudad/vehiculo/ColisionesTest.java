package com.graphics.ciudad.vehiculo; // Prueba las colisiones desde el paquete del vehículo.

import com.graphics.ciudad.juego.Entregas; // Aporta las paradas que deben ser transitables.
import com.graphics.ciudad.mundo.Mapa; // Celdas, medidas y límites de la ciudad.
import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba las reglas de colisión sin abrir una ventana OpenGL; las posiciones salen de Mapa, no de números fijos. */
public class ColisionesTest extends TestCase {

    /** Comprueba que las calles de los bordes conectan el inicio y las entregas. */
    public void testCallesYDestinosTransitables() {
        float primera = Mapa.centro(0); // Centro de la calle del borde oeste / norte (-50 con el mapa 11 × 11).
        float ultima = Mapa.centro(Mapa.MAPA.length - 1); // Centro de la calle del borde este / sur (50).
        for (float z = primera; z <= ultima; z += 0.25f) { // Recorre posiciones a lo largo de la calle del borde oeste.
            assertTrue(Colisiones.puedeCircular(primera, z)); // Exige que cada punto del tramo sea transitable.
        }
        for (float x = primera; x <= ultima; x += 0.25f) { // Recorre la avenida del borde norte.
            assertTrue(Colisiones.puedeCircular(x, primera)); // Comprueba la conexión entre el oeste y la esquina noreste.
        }
        assertTrue(Colisiones.puedeCircular(Auto.X_INICIAL, Auto.Z_INICIAL)); // El punto de partida es transitable.
        for (float[] destino : Entregas.DESTINOS) { // Revisa cada parada.
            assertTrue(Colisiones.puedeCircular(destino[0], destino[1])); // El auto debe poder detenerse en ella.
        }
    }

    /** En el centro de cada celda se puede circular si y solo si es calle: todas las manzanas bloquean. */
    public void testCadaManzanaBloquea() {
        for (int fila = 0; fila < Mapa.MAPA.length; fila++) { // Recorre todas las celdas.
            for (int columna = 0; columna < Mapa.MAPA[fila].length; columna++) {
                float x = Mapa.centro(columna); // Columna → X.
                float z = Mapa.centro(fila); // Fila → Z.
                assertEquals(fila + "," + columna, Mapa.esCalle(fila, columna), Colisiones.puedeCircular(x, z)); // Calle libre, manzana bloqueada.
            }
        }
    }

    /** Comprueba el borde de una manzana (la acera) y los bordes del mapa. */
    public void testObstaculosYLimites() {
        int[] manzana = primeraManzana(Mapa.EDIFICIO); // Primera manzana con edificio, de norte a sur y de oeste a este.
        float bordeOeste = Mapa.centro(manzana[1]) - Mapa.TAM_CELDA / 2; // Borde oeste de su acera.
        float z = Mapa.centro(manzana[0]); // A la altura del centro de la manzana.
        assertFalse(Colisiones.puedeCircular(bordeOeste - 0.5f, z)); // Rechaza una posición cuyo círculo invade la acera.
        assertTrue(Colisiones.puedeCircular(bordeOeste - 2, z)); // Acepta una posición con separación suficiente de la acera.
        float calle = Mapa.centro(0); // Centro de una calle del borde.
        assertFalse(Colisiones.puedeCircular(Mapa.LIMITE - 1, calle)); // Rechaza una posición demasiado cercana al borde este.
        assertFalse(Colisiones.puedeCircular(calle, -Mapa.LIMITE - 1)); // Rechaza una posición exterior al borde norte.
        assertFalse(Colisiones.puedeCircular(Mapa.LIMITE + 10, Mapa.LIMITE + 10)); // Rechaza el campo exterior.
    }

    /** Busca la primera celda de un tipo. */
    private static int[] primeraManzana(int tipo) {
        for (int fila = 0; fila < Mapa.MAPA.length; fila++) {
            for (int columna = 0; columna < Mapa.MAPA[fila].length; columna++) {
                if (Mapa.tipo(fila, columna) == tipo) {
                    return new int[] {fila, columna};
                }
            }
        }
        fail("No hay celdas de tipo " + tipo);
        return null;
    }
}
