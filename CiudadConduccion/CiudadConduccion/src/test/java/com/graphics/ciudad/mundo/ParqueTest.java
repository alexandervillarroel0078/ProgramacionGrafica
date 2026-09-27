package com.graphics.ciudad.mundo; // Prueba los parques desde su mismo paquete.

import com.graphics.ciudad.vehiculo.Colisiones; // Comprueba que las calles sigan transitables.
import java.util.HashSet; // Conjunto de disposiciones distintas.
import java.util.List; // Tipo de las listas de árboles y bancos.
import java.util.Set; // Tipo del conjunto.
import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba la disposición de los parques sin abrir una ventana OpenGL. */
public class ParqueTest extends TestCase {

    /** Árboles: 4 a 6 por parque, lejos del centro, con copa de hasta 1/4 del parque y sin salir de la celda. */
    public void testArboles() {
        for (int[] p : Mapa.parques()) { // Cada parque.
            float cx = Mapa.centro(p[1]); // Centro X.
            float cz = Mapa.centro(p[0]); // Centro Z.
            List<float[]> arboles = Parque.arboles(p[0], p[1]); // Árboles del parque.
            String que = "parque " + p[0] + "," + p[1]; // Mensaje de error útil.
            assertTrue(que, arboles.size() >= Parque.ARBOLES_MIN && arboles.size() <= Parque.ARBOLES_MAX); // Entre 4 y 6.
            for (float[] a : arboles) { // Cada árbol.
                float radioCopa = a[4] / 2; // Mitad del diámetro de la copa.
                assertTrue(que, a[4] <= Parque.COPA_MAXIMA); // Copa de hasta 1/4 del ancho del parque.
                assertTrue(que, Math.hypot(a[0] - cx, a[1] - cz) >= Parque.RADIO_CENTRO_LIBRE); // El centro queda libre.
                assertTrue(que, Math.abs(a[0] - cx) + radioCopa <= Mapa.TAM_CELDA / 2); // No sale de la celda en X.
                assertTrue(que, Math.abs(a[1] - cz) + radioCopa <= Mapa.TAM_CELDA / 2); // No sale de la celda en Z.
                assertFalse(que, Mapa.esCalleEn(a[0], a[1])); // El tronco no está en la calle.
                float altura = a[3] + a[Parque.ALTO_COPA]; // Del césped a la punta.
                if (a[2] == Parque.PINO) { // Escala real (1 u ≈ 1 m).
                    assertTrue(que + " pino " + altura, altura >= Parque.ALTURA_PINO_MIN - 1e-4f && altura <= Parque.ALTURA_PINO_MAX + 1e-4f);
                } else {
                    assertTrue(que + " frondoso " + altura, altura >= Parque.ALTURA_FRONDOSO_MIN - 1e-4f && altura <= Parque.ALTURA_FRONDOSO_MAX + 1e-4f);
                }
                assertTrue(que, Math.abs(a[0] - cx) + radioCopa <= Parque.MITAD_CESPED + 1e-4f); // La copa no sale del césped.
                assertTrue(que, Math.abs(a[1] - cz) + radioCopa <= Parque.MITAD_CESPED + 1e-4f);
                for (float[] poste : com.graphics.ciudad.iluminacion.Iluminacion.POSTES) { // La copa no toca ninguna farola.
                    assertTrue(que + " farola", Math.hypot(a[0] - poste[0], a[1] - poste[1]) >= radioCopa + Parque.MITAD_FAROLA);
                }
                for (float[] b : com.graphics.ciudad.iluminacion.Iluminacion.BOMBILLAS) { // Ni la pantalla colgada del brazo.
                    float mitadPantalla = com.graphics.ciudad.iluminacion.Iluminacion.ANCHO_PANTALLA / 2;
                    assertTrue(que + " pantalla", Math.hypot(a[0] - b[0], a[1] - b[2]) >= radioCopa + mitadPantalla);
                }
                for (float[] sem : Senalizacion.SEMAFOROS) { // Ni un semáforo.
                    assertTrue(que + " semáforo", Math.hypot(a[0] - sem[0], a[1] - sem[1]) >= radioCopa + Parque.MITAD_SEMAFORO);
                }
                for (float[] pare : Senalizacion.PARES) { // Ni un PARE.
                    assertTrue(que + " PARE", Math.hypot(a[0] - pare[0], a[1] - pare[1]) >= radioCopa + Parque.MITAD_PARE);
                }
            }
        }
    }

    /** Bancos: 2 a 4 por parque, dentro del césped, fuera de la fuente y con el frente hacia el centro. */
    public void testBancos() {
        for (int[] p : Mapa.parques()) { // Cada parque.
            float cx = Mapa.centro(p[1]); // Centro X.
            float cz = Mapa.centro(p[0]); // Centro Z.
            List<float[]> bancos = Parque.bancos(p[0], p[1]); // Bancos del parque.
            assertTrue(bancos.size() >= Parque.BANCOS_MIN && bancos.size() <= Parque.BANCOS_MAX); // Entre 2 y 4.
            for (float[] b : bancos) { // Cada banco.
                float distancia = (float) Math.hypot(b[0] - cx, b[1] - cz); // Distancia al centro.
                assertTrue(distancia > Parque.RADIO_FUENTE + 0.5f); // No se superpone con la fuente.
                assertTrue(distancia + 0.8f < Parque.MITAD_CESPED); // Queda sobre el césped.
                float frenteX = -(float) Math.sin(b[2]); // Hacia dónde mira el banco, en X.
                float frenteZ = -(float) Math.cos(b[2]); // Hacia dónde mira, en Z.
                float haciaCentro = (frenteX * (cx - b[0]) + frenteZ * (cz - b[1])) / distancia; // Coseno entre frente y dirección a la fuente.
                assertEquals(1f, haciaCentro, 1e-4f); // Mira exactamente hacia la fuente.
            }
        }
    }

    /** Los parques no son copias: sus disposiciones difieren, y hay pinos y frondosos. */
    public void testVariacionEntreParques() {
        Set<String> disposiciones = new HashSet<>(); // Firmas distintas.
        boolean hayPinos = false; // Algún pino.
        boolean hayFrondosos = false; // Algún frondoso.
        for (int[] p : Mapa.parques()) { // Cada parque.
            StringBuilder firma = new StringBuilder(); // Resumen de la disposición relativa al centro.
            for (float[] a : Parque.arboles(p[0], p[1])) { // Árboles.
                firma.append(Math.round(a[0] - Mapa.centro(p[1]))).append(',').append(Math.round(a[1] - Mapa.centro(p[0]))).append(a[2] == Parque.PINO ? "p " : "f "); // Lugar y tipo.
                hayPinos |= a[2] == Parque.PINO; // Registra el tipo.
                hayFrondosos |= a[2] == Parque.FRONDOSO; // Registra el tipo.
            }
            firma.append('|').append(Parque.bancos(p[0], p[1]).size()); // Cantidad de bancos.
            disposiciones.add(firma.toString()); // Guarda la firma.
        }
        assertEquals(Mapa.parques().size(), disposiciones.size()); // Todas las disposiciones son distintas.
        assertTrue(hayPinos && hayFrondosos); // Se usan los dos tipos de árbol.
        assertEquals(Parque.arboles(3, 1).size(), Parque.arboles(3, 1).size()); // Determinístico: mismo resultado cada vez.
        assertEquals(Parque.variacion(3, 1, 2, 5), Parque.variacion(3, 1, 2, 5), 0f); // La variación no usa azar.
    }

    /** Las calles siguen transitables: el parque no agrega obstáculos. */
    public void testColisionesIntactas() {
        for (int fila = 0; fila < Mapa.MAPA.length; fila++) { // Recorre las filas.
            for (int columna = 0; columna < Mapa.MAPA[fila].length; columna++) { // Recorre las columnas.
                if (Mapa.esCalle(fila, columna)) { // Solo las calles.
                    assertTrue(Colisiones.puedeCircular(Mapa.centro(columna), Mapa.centro(fila))); // Centro transitable.
                }
            }
        }
    }

    /**
     * Cada parque tiene al menos 1 árbol y 1 banco. Los árboles se saltean cerca de semáforos, PARE, carteles y
     * farolas; con otro MAPA (por ejemplo 13 × 13) un parque rodeado de señales podría quedarse sin ninguno. Esta
     * prueba corre sobre el MAPA actual: se ejecuta también con 13 × 13 al probar el mapa ampliado.
     */
    public void testCadaParqueTieneArbolYBanco() {
        assertFalse(Mapa.parques().isEmpty());
        for (int[] p : Mapa.parques()) {
            String que = "parque " + p[0] + "," + p[1];
            assertTrue(que + " sin árboles", Parque.arboles(p[0], p[1]).size() >= 1);
            assertTrue(que + " sin bancos", Parque.bancos(p[0], p[1]).size() >= 1);
        }
    }
}
