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

    /**
     * En cada fase del ciclo hay exactamente una lente emisiva, y es la del color de la fase con su color intenso; las
     * otras dos existen, con el mismo color muy oscuro y sin emisión. Nada más del cabezal brilla.
     */
    public void testUnaLenteEmisivaPorFase() {
        for (float t = 0; t < Semaforo.CICLO; t += PASO_MUESTREO) { // Recorre un ciclo completo.
            int activa = Semaforo.luzActiva(t); // Color de la fase en este instante.
            int emisivas = 0; // Lentes encendidas encontradas.
            int lentes = 0; // Lentes encontradas.
            for (Semaforo.Pieza p : Semaforo.modelo(activa)) { // Recorre el cabezal.
                if (p.parte != Semaforo.Parte.LENTE) { // Base, poste, caja y viseras...
                    assertFalse(p.emisiva); // ...reciben la iluminación normal.
                    continue;
                }
                lentes++;
                float[] intenso = Semaforo.COLORES_LENTE[p.luz]; // Color propio de esta lente.
                if (p.emisiva) { // La encendida...
                    emisivas++;
                    assertEquals("t=" + t, activa, p.luz); // ...es la de la fase...
                    assertSame(intenso, p.color); // ...con su color intenso.
                } else { // Las apagadas: mismo color, muy oscuro.
                    for (int c = 0; c < 3; c++) {
                        assertEquals(intenso[c] * Semaforo.BRILLO_APAGADA, p.color[c], 1e-6f);
                    }
                }
            }
            assertEquals(3, lentes); // Siempre se ven las tres.
            assertEquals("t=" + t, 1, emisivas); // Exactamente una encendida.
        }
    }

    /** Rojo arriba, amarillo al medio y verde abajo, todas dentro del alto de la caja y en su cara delantera. */
    public void testLentesOrdenadasEnElFrente() {
        assertTrue(Semaforo.alturaLente(Semaforo.ROJA) > Semaforo.alturaLente(Semaforo.AMARILLA));
        assertTrue(Semaforo.alturaLente(Semaforo.AMARILLA) > Semaforo.alturaLente(Semaforo.VERDE));
        float techo = Semaforo.BASE_CAJA + Semaforo.ALTO_CAJA; // Borde superior de la caja.
        for (Semaforo.Pieza p : Semaforo.modelo(Semaforo.ROJA)) {
            if (p.parte == Semaforo.Parte.LENTE) {
                assertEquals(Semaforo.alturaLente(p.luz), p.y, 0f); // Cada lente a la altura de su color.
                assertTrue(p.y - Semaforo.DIAMETRO_LENTE / 2 > Semaforo.BASE_CAJA); // No se sale por abajo...
                assertTrue(p.y + Semaforo.DIAMETRO_LENTE / 2 < techo); // ...ni por arriba.
                assertTrue(p.z < -Semaforo.PROFUNDIDAD_CAJA / 2); // Delante de la caja, hacia el tráfico.
            }
            if (p.parte == Semaforo.Parte.VISERA) {
                assertTrue(p.y < techo); // Las viseras quedan bajo el techo de la caja.
            }
        }
    }

    /** La espalda de la caja queda lisa: ninguna pieza sobresale por detrás (Z local positiva). */
    public void testEspaldaLisa() {
        for (Semaforo.Pieza p : Semaforo.modelo(Semaforo.VERDE)) {
            if (p.parte == Semaforo.Parte.LENTE || p.parte == Semaforo.Parte.VISERA) {
                assertTrue(p.parte + " por detrás del frente", p.z < -Semaforo.PROFUNDIDAD_CAJA / 2); // Solo en el frente.
            } else {
                assertEquals(0f, p.z, 0f); // Base, poste y caja centrados: la espalda es la cara trasera de la caja.
            }
        }
    }
}
