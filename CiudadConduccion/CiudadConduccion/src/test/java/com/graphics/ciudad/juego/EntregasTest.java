package com.graphics.ciudad.juego; // Prueba las reglas de la partida desde su mismo paquete.

import com.graphics.ciudad.motor.Cubo; // Se crea sin tocar la GPU: solo guarda referencias.
import com.graphics.ciudad.motor.Shader; // Se crea sin tocar la GPU: OpenGL se usa recién en crear().
import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba el progreso de las entregas y su reinicio sin abrir una ventana OpenGL. */
public class EntregasTest extends TestCase {

    /** Crea un objeto Entregas cuyo dibujo no se usa en la prueba. */
    private Entregas nuevasEntregas() {
        Shader shader = new Shader(); // Programa sin compilar: la prueba no dibuja.
        return new Entregas(shader, new Cubo(shader)); // Las reglas no necesitan OpenGL.
    }

    /** Completa las tres entregas, reinicia y verifica progreso 0 y el primer destino activo. */
    public void testCompletarYReiniciar() {
        Entregas entregas = nuevasEntregas(); // Partida nueva.
        assertEquals(0, entregas.getEntregas()); // Empieza sin entregas.
        entregas.actualizar(0.1f, 0, 0, 0); // Detenido lejos del destino: no cuenta.
        assertEquals(0, entregas.getEntregas()); // La entrega exige estar cerca.
        float[] primero = Entregas.DESTINOS[0]; // Primera parada.
        entregas.actualizar(0.1f, primero[0], primero[1], 5); // Llega a la parada sin frenar.
        assertEquals(0, entregas.getEntregas()); // La entrega exige velocidad menor que 1.

        for (int indice = 0; indice < Entregas.DESTINOS.length; indice++) { // Visita cada parada en orden.
            float[] destino = entregas.destinoActual(); // Destino activo en este momento.
            assertSame(Entregas.DESTINOS[indice], destino); // Las paradas se activan en el orden de DESTINOS.
            entregas.actualizar(0.1f, destino[0] + 1, destino[1] - 1, 0.5f); // Frena a menos de 3 unidades de la marca.
            assertEquals(indice + 1, entregas.getEntregas()); // Cada llegada suma una entrega.
        }
        assertFalse(entregas.quedanEntregas()); // Tras la tercera entrega la partida terminó.
        assertNull(entregas.destinoActual()); // Ya no hay destino activo.
        assertTrue(entregas.estado().contains("GANASTE")); // El título anuncia la victoria.

        entregas.reset(); // Mismo reinicio que la tecla R.
        assertEquals(0, entregas.getEntregas()); // El progreso vuelve a cero.
        assertSame(Entregas.DESTINOS[0], entregas.destinoActual()); // El destino activo vuelve a ser el primero.
        assertEquals(0f, entregas.getTiempo(), 0f); // El cronómetro de la partida vuelve a cero.
        assertTrue(entregas.estado().startsWith("Entregas: 0/" + Entregas.DESTINOS.length + " | Destino: ")); // El título vuelve al formato de partida en curso.
    }
}
