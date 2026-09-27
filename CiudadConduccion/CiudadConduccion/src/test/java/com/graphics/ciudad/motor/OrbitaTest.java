package com.graphics.ciudad.motor; // Prueba la órbita desde su mismo paquete.

import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba las coordenadas esféricas de Orbita, que comparten la cámara orbital del auto y la aérea. */
public class OrbitaTest extends TestCase {

    /** Con φ = 0 la cámara está a ras del centro; con θ = 0, del lado +Z; la distancia al centro siempre es D. */
    public void testCoordenadasEsfericas() {
        Orbita o = new Orbita(0, 0, 10, 0, 1.5f, 1, 100); // θ = 0, φ = 0, D = 10.
        float[] p = o.ojo(3, 1, -2, 0); // Alrededor de (3, 1, -2).
        assertEquals(3f, p[0], 1e-5f); // Sin corrimiento en X.
        assertEquals(1f, p[1], 1e-5f); // A la altura del centro.
        assertEquals(8f, p[2], 1e-5f); // 10 hacia +Z.
        o.colocar(0.7f, 0.9f, 25); // Otro punto cualquiera.
        p = o.ojo(3, 1, -2, 0.4f); // Con un ángulo base (como el del auto).
        double d = Math.sqrt((p[0] - 3) * (p[0] - 3) + (p[1] - 1) * (p[1] - 1) + (p[2] + 2) * (p[2] + 2)); // Distancia al centro.
        assertEquals(25, d, 1e-4); // Es D.
        assertEquals(Math.sin(0.9) * 25, p[1] - 1, 1e-4); // La altura es D · sen φ.
        assertEquals(Math.atan2(p[0] - 3, p[2] + 2), 1.1, 1e-4); // El ángulo horizontal es base + θ.
    }

    /** φ y D nunca salen de sus límites, ni al crear, ni al colocar, ni al girar o acercar. */
    public void testLimites() {
        Orbita o = new Orbita(0, 5, 1000, 0.2f, 1.2f, 4, 30); // Valores iniciales fuera de rango.
        assertEquals(1.2f, o.getElevacion(), 0f); // Recortados al crear.
        assertEquals(30f, o.getDistancia(), 0f);
        o.girar(0, -10); // Muy abajo.
        assertEquals(0.2f, o.getElevacion(), 0f);
        o.acercar(1000); // Muy cerca.
        assertEquals(4f, o.getDistancia(), 0f);
        o.girar(100, 0); // θ no tiene límites.
        assertEquals(100f, o.getAngulo(), 0f);
    }
}
