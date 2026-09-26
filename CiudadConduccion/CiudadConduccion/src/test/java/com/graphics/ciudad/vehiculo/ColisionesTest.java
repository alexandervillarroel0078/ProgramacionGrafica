package com.graphics.ciudad.vehiculo; // Prueba las colisiones desde el paquete del vehículo.

import com.graphics.ciudad.juego.Entregas; // Aporta las paradas que deben ser transitables.
import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba las reglas de colisión en la ciudad 11 × 11 sin abrir una ventana OpenGL. */
public class ColisionesTest extends TestCase {

    /** Comprueba que las calles del recorrido conectan el inicio y las entregas. */
    public void testCallesYDestinosTransitables() {
        for (float z = -50; z <= 50; z += 0.25f) { // Recorre posiciones a lo largo de la calle del borde oeste.
            assertTrue(Colisiones.puedeCircular(-50, z)); // Exige que cada punto del tramo sea transitable.
        }
        for (float x = -50; x <= 50; x += 0.25f) { // Recorre la avenida del borde norte.
            assertTrue(Colisiones.puedeCircular(x, -50)); // Comprueba la conexión entre el oeste y la esquina noreste.
        }
        assertTrue(Colisiones.puedeCircular(Auto.X_INICIAL, Auto.Z_INICIAL)); // El punto de partida es transitable.
        for (float[] destino : Entregas.DESTINOS) { // Revisa cada parada.
            assertTrue(Colisiones.puedeCircular(destino[0], destino[1])); // El auto debe poder detenerse en ella.
        }
        assertTrue(Colisiones.puedeCircular(-10, -10)); // Verifica que un cruce interior permite circular.
        assertFalse(Colisiones.puedeCircular(0, 0)); // Verifica que la manzana del origen (parque central) bloquea al vehículo.
    }

    /** Comprueba edificios, parques, aceras y bordes del mapa. */
    public void testObstaculosYLimites() {
        assertFalse(Colisiones.puedeCircular(-20, -20)); // Rechaza el centro de un edificio.
        assertFalse(Colisiones.puedeCircular(-40, -20)); // Rechaza el centro de un parque.
        assertFalse(Colisiones.puedeCircular(-25.5f, -20)); // Rechaza una posición cuyo círculo invade la acera.
        assertFalse(Colisiones.puedeCircular(54, 0)); // Rechaza una posición demasiado cercana al borde derecho.
        assertFalse(Colisiones.puedeCircular(0, -56)); // Rechaza una posición exterior al borde norte.
        assertTrue(Colisiones.puedeCircular(-27, -20)); // Acepta una posición con separación suficiente de la acera.
    }
}
