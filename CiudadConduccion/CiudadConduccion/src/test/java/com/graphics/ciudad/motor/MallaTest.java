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

    /** Extrusión de un perfil de 4 puntos: 12 · 4 - 12 = 36 vértices, normales unitarias hacia afuera, tapas en ±X. */
    public void testExtrusion() {
        float[][] trapecio = {{0.78f, 0.9f}, {-0.62f, 0.9f}, {-0.12f, 1.4f}, {0.55f, 1.4f}}; // Perfil {z, y} de 4 puntos.
        float[][] alReves = {trapecio[3], trapecio[2], trapecio[1], trapecio[0]}; // El mismo contorno en el otro sentido de giro.
        for (float[][] perfil : new float[][][] {trapecio, alReves}) { // Ambos sentidos deben dar normales hacia afuera.
            float[] datos = Malla.extruir(perfil, 1.4f); // Figura extruida de ancho 1.4.
            assertEquals(36 * Malla.FLOATS_POR_VERTICE, datos.length); // 36 vértices para 4 puntos.
            float centroY = 0; // Centro del perfil en Y.
            float centroZ = 0; // Centro del perfil en Z.
            for (float[] p : perfil) { // Promedio de los puntos.
                centroY += p[1] / perfil.length; // En Y.
                centroZ += p[0] / perfil.length; // En Z.
            }
            for (int i = 0; i < datos.length; i += 3 * Malla.FLOATS_POR_VERTICE) { // Recorre triángulo por triángulo.
                float mx = 0; // Centro del triángulo.
                float my = 0;
                float mz = 0;
                for (int v = 0; v < 3; v++) { // Sus tres vértices.
                    int k = i + v * Malla.FLOATS_POR_VERTICE; // Inicio del vértice.
                    float largo = (float) Math.sqrt(datos[k + 3] * datos[k + 3] + datos[k + 4] * datos[k + 4] + datos[k + 5] * datos[k + 5]); // Largo de la normal.
                    assertEquals(1f, largo, 1e-4f); // Normal unitaria.
                    assertEquals(datos[i + 3], datos[k + 3], 0f); // Flat shading: los tres vértices comparten la normal (X)...
                    assertEquals(datos[i + 4], datos[k + 4], 0f); // ...(Y)...
                    assertEquals(datos[i + 5], datos[k + 5], 0f); // ...(Z).
                    mx += datos[k] / 3; // Centro del triángulo.
                    my += datos[k + 1] / 3;
                    mz += datos[k + 2] / 3;
                }
                float haciaAfuera = datos[i + 3] * mx + datos[i + 4] * (my - centroY) + datos[i + 5] * (mz - centroZ); // Normal · (centro del triángulo - centro de la figura).
                assertTrue(haciaAfuera > 0); // La normal apunta hacia afuera.
                if (Math.abs(datos[i + 3]) > 0.5f) { // Triángulo de una tapa.
                    assertEquals(Math.signum(mx), datos[i + 3], 1e-6f); // La tapa en X = +0.7 mira a +X y la otra a -X.
                }
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
