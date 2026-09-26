package com.graphics.ciudad.motor; // Prueba las mallas generadas desde su mismo paquete.

import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba cantidad de vértices, normales unitarias y tamaño de las figuras generadas, sin abrir OpenGL. */
public class MallaTest extends TestCase {

    private static final float EPSILON = 1e-4f; // Tolerancia para comparar float.

    /** Revisa que cada normal mida 1 y que cada posición quede dentro del cubo unitario [-0.5, 0.5]. */
    private static void comprobarNormalesYLimites(String que, float[] datos) {
        assertEquals(que, 0, datos.length % (3 * Malla.FLOATS_POR_VERTICE)); // Triángulos completos: tres vértices cada uno.
        for (int i = 0; i < datos.length; i += Malla.FLOATS_POR_VERTICE) { // Recorre los vértices.
            float largo = (float) Math.sqrt(datos[i + 3] * datos[i + 3] + datos[i + 4] * datos[i + 4] + datos[i + 5] * datos[i + 5]); // Largo de la normal.
            assertEquals(que + " normal no unitaria en el vértice " + i / 6, 1f, largo, EPSILON); // Normal de longitud 1.
            for (int eje = 0; eje < 3; eje++) { // X, Y y Z de la posición.
                assertTrue(que + " fuera del cubo unitario", Math.abs(datos[i + eje]) <= 0.5f + EPSILON); // Figura unitaria.
            }
        }
    }

    /** Esfera 12 × 8: 6 · 12 · 7 = 504 vértices; la normal apunta desde el centro hacia el vértice. */
    public void testEsfera() {
        float[] datos = Malla.esfera(Figuras.SECTORES_ESFERA, Figuras.ANILLOS_ESFERA); // Figura generada.
        assertEquals(6 * 12 * 7 * Malla.FLOATS_POR_VERTICE, datos.length); // 504 vértices.
        comprobarNormalesYLimites("esfera", datos); // Normales unitarias y tamaño.
        for (int i = 0; i < datos.length; i += Malla.FLOATS_POR_VERTICE) { // Recorre los vértices.
            assertEquals(datos[i + 3] * 0.5f, datos[i], EPSILON); // Posición = normal · radio (en X)...
            assertEquals(datos[i + 4] * 0.5f, datos[i + 1], EPSILON); // ...en Y...
            assertEquals(datos[i + 5] * 0.5f, datos[i + 2], EPSILON); // ...y en Z: la normal es radial.
        }
    }

    /** Cilindro de 10 lados: 12 · 10 = 120 vértices; costado con normales horizontales y tapas verticales. */
    public void testCilindro() {
        float[] datos = Malla.cilindro(Figuras.LADOS_CILINDRO); // Figura generada.
        assertEquals(12 * 10 * Malla.FLOATS_POR_VERTICE, datos.length); // 120 vértices.
        comprobarNormalesYLimites("cilindro", datos); // Normales unitarias y tamaño.
        for (int i = 0; i < datos.length; i += Malla.FLOATS_POR_VERTICE) { // Recorre los vértices.
            boolean tapa = Math.abs(datos[i + 4]) > 0.5f; // Normal vertical: vértice de una tapa.
            if (!tapa) { // Vértice del costado.
                assertEquals(0f, datos[i + 4], EPSILON); // Normal horizontal.
                assertEquals(datos[i + 3] * 0.5f, datos[i], EPSILON); // Posición horizontal = normal · radio.
            } else { // Vértice de una tapa.
                assertEquals(Math.signum(datos[i + 1]), datos[i + 4], EPSILON); // Arriba mira hacia arriba y abajo hacia abajo.
            }
        }
    }

    /** Cono de 10 lados: 6 · 10 = 60 vértices; el costado mira hacia afuera y un poco hacia arriba. */
    public void testCono() {
        float[] datos = Malla.cono(Figuras.LADOS_CONO); // Figura generada.
        assertEquals(6 * 10 * Malla.FLOATS_POR_VERTICE, datos.length); // 60 vértices.
        comprobarNormalesYLimites("cono", datos); // Normales unitarias y tamaño.
        for (int i = 0; i < datos.length; i += Malla.FLOATS_POR_VERTICE) { // Recorre los vértices.
            if (datos[i + 4] > 0) { // Normal del costado (la base tiene normal hacia abajo).
                float esperadoY = 0.5f / (float) Math.sqrt(1 + 0.25); // r / √(h² + r²) con r = 0.5 y h = 1.
                assertEquals(esperadoY, datos[i + 4], EPSILON); // Inclinación correcta del costado.
            } else { // Vértice de la base.
                assertEquals(-1f, datos[i + 4], EPSILON); // Mira hacia abajo.
                assertEquals(-0.5f, datos[i + 1], EPSILON); // Está en la base.
            }
        }
    }

    /** Malla informa la cantidad de vértices de los datos que recibe (sin usar la GPU). */
    public void testCantidadDeVertices() {
        Figuras figuras = new Figuras(new Shader()); // Genera las tres figuras sin OpenGL.
        assertEquals(504, figuras.esfera.cantidadVertices()); // Esfera.
        assertEquals(120, figuras.cilindro.cantidadVertices()); // Cilindro.
        assertEquals(60, figuras.cono.cantidadVertices()); // Cono.
    }
}
