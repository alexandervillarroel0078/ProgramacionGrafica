package com.graphics.ciudad.mundo; // Prueba el semáforo desde su mismo paquete.

import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba la secuencia rojo → verde → amarillo del semáforo sin abrir una ventana OpenGL. */
public class SemaforoTest extends TestCase {

    /** Comprueba el orden y la duración de cada luz, y que el ciclo se repite. */
    public void testSecuenciaRepetitiva() {
        assertEquals(12f, Semaforo.CICLO, 0f); // 5 s rojo + 5 s verde + 2 s amarillo.
        assertEquals(Semaforo.ROJA, Semaforo.luzActiva(0)); // El ciclo empieza en rojo.
        assertEquals(Semaforo.ROJA, Semaforo.luzActiva(Semaforo.DURACION_ROJO - 0.01f)); // Rojo hasta el final de su tramo.
        assertEquals(Semaforo.VERDE, Semaforo.luzActiva(Semaforo.DURACION_ROJO)); // Después del rojo viene el verde.
        float finVerde = Semaforo.DURACION_ROJO + Semaforo.DURACION_VERDE; // Instante en que termina el verde.
        assertEquals(Semaforo.VERDE, Semaforo.luzActiva(finVerde - 0.01f)); // Verde hasta el final de su tramo.
        assertEquals(Semaforo.AMARILLA, Semaforo.luzActiva(finVerde)); // Después del verde viene el amarillo.
        assertEquals(Semaforo.AMARILLA, Semaforo.luzActiva(Semaforo.CICLO - 0.01f)); // Amarillo hasta cerrar el ciclo.
        assertEquals(Semaforo.ROJA, Semaforo.luzActiva(Semaforo.CICLO)); // El ciclo vuelve a empezar en rojo.
        assertEquals(Semaforo.VERDE, Semaforo.luzActiva(3 * Semaforo.CICLO + 6)); // Se repite en vueltas posteriores.
    }
}
