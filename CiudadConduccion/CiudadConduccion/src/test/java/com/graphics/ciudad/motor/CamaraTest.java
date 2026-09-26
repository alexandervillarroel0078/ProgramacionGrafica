package com.graphics.ciudad.motor; // Prueba la cámara desde su mismo paquete.

import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba los modos y los límites de la cámara orbital sin abrir una ventana OpenGL. */
public class CamaraTest extends TestCase {

    /** C recorre seguimiento → orbital → aérea → seguimiento; la flecha solo corresponde a la aérea. */
    public void testCicloDeModos() {
        Camara camara = new Camara(55); // Ciudad de límite 55.
        assertEquals(Camara.Modo.SEGUIMIENTO, camara.getModo()); // Arranca en seguimiento.
        camara.alternar(); // C.
        assertEquals(Camara.Modo.ORBITAL, camara.getModo()); // Orbital del auto.
        assertFalse(camara.esAerea()); // Sin flecha en la orbital.
        camara.alternar(); // C.
        assertEquals(Camara.Modo.AEREA, camara.getModo()); // Vista aérea.
        assertTrue(camara.esAerea()); // Con flecha.
        camara.alternar(); // C.
        assertEquals(Camara.Modo.SEGUIMIENTO, camara.getModo()); // Vuelve al inicio.
    }

    /** Arrastres y ruedita extremos nunca sacan la elevación ni la distancia de sus límites. */
    public void testLimitesDeLaOrbital() {
        Camara camara = new Camara(55); // Cámara nueva.
        camara.alternar(); // Modo orbital.
        double[][] movimientos = {{0, -100000}, {0, 100000}, {500, -37}, {-800, 91}, {3, 4}}; // Arrastres enormes y pequeños.
        double[] ruedas = {1000, -1000, 3, -7, 0.5}; // Zooms enormes y pequeños.
        for (int vuelta = 0; vuelta < 20; vuelta++) { // Repite combinaciones.
            for (double[] m : movimientos) { // Cada arrastre.
                camara.arrastrar(m[0], m[1]); // Mueve el mouse.
                comprobarLimites(camara); // Siempre dentro.
            }
            for (double r : ruedas) { // Cada paso de ruedita.
                camara.zoom(r); // Acerca o aleja.
                comprobarLimites(camara); // Siempre dentro.
            }
        }
        camara.arrastrar(0, -100000); // Mouse muy arriba.
        assertEquals(Camara.ELEVACION_MAX, camara.getElevacion(), 1e-6f); // Tope superior: 85°.
        camara.zoom(-1000); // Alejar muchísimo.
        assertEquals(Camara.DISTANCIA_MAX, camara.getDistancia(), 1e-6f); // Tope de distancia.
    }

    /** Fuera del modo orbital el mouse no cambia la cámara. */
    public void testMouseSoloEnOrbital() {
        Camara camara = new Camara(55); // En seguimiento.
        float elevacion = camara.getElevacion(); // Valor inicial.
        float distancia = camara.getDistancia(); // Valor inicial.
        camara.arrastrar(100, 100); // Arrastre.
        camara.zoom(5); // Ruedita.
        assertEquals(elevacion, camara.getElevacion(), 0f); // Sin cambios.
        assertEquals(distancia, camara.getDistancia(), 0f); // Sin cambios.
    }

    /** Revisa que elevación y distancia estén dentro de sus límites. */
    private static void comprobarLimites(Camara camara) {
        assertTrue(camara.getElevacion() >= Camara.ELEVACION_MIN - 1e-6f); // No por debajo de 5°.
        assertTrue(camara.getElevacion() <= Camara.ELEVACION_MAX + 1e-6f); // No por encima de 85°.
        assertTrue(camara.getDistancia() >= Camara.DISTANCIA_MIN - 1e-6f); // No más cerca que el mínimo.
        assertTrue(camara.getDistancia() <= Camara.DISTANCIA_MAX + 1e-6f); // No más lejos que el máximo.
    }
}
