package com.graphics.ciudad.mundo; // Prueba el semáforo desde su mismo paquete.

import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba la secuencia rojo → verde → amarillo y la coordinación entre accesos, sin abrir una ventana OpenGL. */
public class SemaforoTest extends TestCase {

    private static final float PASO_MUESTREO = 0.05f; // Cada cuánto se revisan los colores al recorrer el tiempo.

    /** Comprueba el orden y la duración de cada luz, y que el ciclo se repite. */
    public void testSecuenciaRepetitiva() {
        assertEquals(Semaforo.DURACION_VERDE + Semaforo.DURACION_AMARILLO, Semaforo.DURACION_ROJO, 0f); // Condición de la coordinación.
        assertEquals(14f, Semaforo.CICLO, 0f); // 7 s rojo + 5 s verde + 2 s amarillo.
        assertEquals(Semaforo.ROJA, Semaforo.luzActiva(0)); // El ciclo empieza en rojo.
        assertEquals(Semaforo.ROJA, Semaforo.luzActiva(Semaforo.DURACION_ROJO - 0.01f)); // Rojo hasta el final de su tramo.
        assertEquals(Semaforo.VERDE, Semaforo.luzActiva(Semaforo.DURACION_ROJO)); // Después del rojo viene el verde.
        float finVerde = Semaforo.DURACION_ROJO + Semaforo.DURACION_VERDE; // Instante en que termina el verde.
        assertEquals(Semaforo.VERDE, Semaforo.luzActiva(finVerde - 0.01f)); // Verde hasta el final de su tramo.
        assertEquals(Semaforo.AMARILLA, Semaforo.luzActiva(finVerde)); // Después del verde viene el amarillo.
        assertEquals(Semaforo.AMARILLA, Semaforo.luzActiva(Semaforo.CICLO - 0.01f)); // Amarillo hasta cerrar el ciclo.
        assertEquals(Semaforo.ROJA, Semaforo.luzActiva(Semaforo.CICLO)); // El ciclo vuelve a empezar en rojo.
        assertEquals(Semaforo.VERDE, Semaforo.luzActiva(3 * Semaforo.CICLO + Semaforo.DURACION_ROJO + 1)); // Se repite en vueltas posteriores.
    }

    /**
     * En cada intersección con semáforo, a lo largo de dos ciclos: los accesos opuestos muestran el mismo color y los
     * grupos norte-sur y este-oeste nunca están a la vez en verde o amarillo.
     */
    public void testCoordinacionEntreAccesos() {
        for (float t = 0; t <= 2 * Semaforo.CICLO; t += PASO_MUESTREO) { // Recorre dos ciclos completos.
            for (int[] cruce : Senalizacion.INTERSECCIONES_SEMAFORO) { // Revisa cada intersección con semáforo.
                int norteSur = -1; // Color del grupo norte-sur en este instante (-1: aún no visto).
                int esteOeste = -1; // Color del grupo este-oeste.
                for (float[] s : Senalizacion.SEMAFOROS) { // Busca los cabezales de este cruce.
                    if ((int) s[4] != cruce[0] || (int) s[5] != cruce[1]) { // Otro cruce.
                        continue; // Se salta.
                    }
                    boolean grupoNS = s[3] == 1; // Grupo del cabezal.
                    int luz = Semaforo.luzParaAcceso(t, grupoNS); // Color que muestra.
                    if (grupoNS) { // Accesos norte y sur.
                        assertTrue("opuestos N-S distintos en t=" + t, norteSur == -1 || norteSur == luz); // Norte y sur iguales.
                        norteSur = luz; // Recuerda el color del grupo.
                    } else { // Accesos este y oeste.
                        assertTrue("opuestos E-O distintos en t=" + t, esteOeste == -1 || esteOeste == luz); // Este y oeste iguales.
                        esteOeste = luz; // Recuerda el color del grupo.
                    }
                }
                boolean pasaNS = norteSur != Semaforo.ROJA; // Verde o amarillo en norte-sur.
                boolean pasaEO = esteOeste != Semaforo.ROJA; // Verde o amarillo en este-oeste.
                assertFalse("ambos grupos habilitados en t=" + t, pasaNS && pasaEO); // Nunca a la vez.
                assertTrue("ambos en rojo en t=" + t, pasaNS || pasaEO); // Complementarios: siempre uno habilitado.
            }
        }
    }
}
