package com.graphics.ciudad.mundo; // Prueba la señalización vial desde su mismo paquete.

import com.graphics.ciudad.vehiculo.Colisiones; // Comprueba que las calles sigan siendo transitables.
import java.util.List; // Tipo de las listas de ubicaciones.
import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba ubicación y orientación de semáforos, PARE y carteles, sin abrir una ventana OpenGL. */
public class SenalizacionTest extends TestCase {

    private static final float EPSILON = 1e-4f; // Tolerancia para comparar valores calculados en float.

    /** Comprueba que una señal esté a la derecha del acceso, fuera de la calle y mirando al auto que llega. */
    private static void comprobarEsquinaDerecha(String que, float x, float z, float angulo, int fila, int columna, int dFila, int dColumna) {
        float relX = x - Mapa.centro(columna); // Vector del centro del cruce a la señal, en X.
        float relZ = z - Mapa.centro(fila); // Vector del centro del cruce a la señal, en Z.
        float dX = -dColumna; // Dirección en que viajan los autos del acceso, en X.
        float dZ = -dFila; // Dirección en que viajan, en Z.
        float derecha = relX * -dZ + relZ * dX; // Proyección sobre la derecha del conductor, r = (-dz, dx).
        float atras = relX * -dX + relZ * -dZ; // Proyección sobre el acceso (hacia afuera del cruce).
        assertTrue(que + " no está a la derecha", derecha > Mapa.TAM_CELDA / 2); // Más allá del cordón derecho: sobre la vereda.
        assertTrue(que + " no está en el acceso", atras > Mapa.TAM_CELDA / 2 - EPSILON); // Del lado del acceso, antes del cruce.
        assertFalse(que + " ocupa la calle", Mapa.esCalleEn(x, z)); // Está en una manzana, no en la calle.
        assertEquals(que + " no mira al auto (x)", -dX, -(float) Math.sin(angulo), EPSILON); // El frente apunta hacia -d.
        assertEquals(que + " no mira al auto (z)", -dZ, -(float) Math.cos(angulo), EPSILON); // Igual en Z.
    }

    /** Semáforos: solo en las 4 intersecciones del Centro, uno por acceso, a la derecha y mirando al auto. */
    public void testSemaforos() {
        assertEquals(4, Senalizacion.INTERSECCIONES_SEMAFORO.size()); // Las cuatro del Centro.
        int accesos = 0; // Accesos totales de esas intersecciones.
        for (int[] cruce : Senalizacion.INTERSECCIONES_SEMAFORO) { // Revisa cada cruce con semáforo.
            assertTrue(Mapa.esInterseccion(cruce[0], cruce[1])); // Es una intersección.
            assertEquals("Centro", Mapa.NOMBRES_SECTORES[Mapa.sectorDeCelda(cruce[0], cruce[1])]); // Está en el Centro.
            accesos += Mapa.accesos(cruce[0], cruce[1]).size(); // Suma sus accesos.
        }
        for (int[] cruce : Mapa.intersecciones()) { // Todas las intersecciones del Centro tienen semáforo.
            if (Mapa.sectorDeCelda(cruce[0], cruce[1]) == Senalizacion.SECTOR_SEMAFOROS) { // Intersección del Centro.
                assertTrue(contiene(Senalizacion.INTERSECCIONES_SEMAFORO, cruce[0], cruce[1])); // Está en la lista.
            }
        }
        assertEquals(accesos, Senalizacion.SEMAFOROS.size()); // Un cabezal por acceso.
        for (float[] s : Senalizacion.SEMAFOROS) { // Revisa cada cabezal.
            int fila = (int) s[4]; // Cruce del cabezal.
            int columna = (int) s[5]; // Cruce del cabezal.
            int[] acceso = accesoDe(s, fila, columna); // Acceso al que corresponde.
            comprobarEsquinaDerecha("semáforo", s[0], s[1], s[2], fila, columna, acceso[0], acceso[1]); // Derecha, vereda, orientación.
            assertEquals(acceso[0] != 0 ? 1f : 0f, s[3], 0f); // Grupo norte-sur si el acceso cambia de fila.
        }
    }

    /** Busca a qué acceso del cruce pertenece un cabezal (el que reproduce su posición). */
    private static int[] accesoDe(float[] s, int fila, int columna) {
        for (int[] acceso : Mapa.accesos(fila, columna)) { // Prueba cada acceso.
            float[] p = Senalizacion.esquinaDerecha(fila, columna, acceso[0], acceso[1], Senalizacion.RETROCESO_SEMAFORO); // Posición esperada.
            if (Math.abs(p[0] - s[0]) < EPSILON && Math.abs(p[1] - s[1]) < EPSILON) { // Coincide.
                return acceso; // Es este acceso.
            }
        }
        fail("cabezal sin acceso en " + s[0] + "," + s[1]); // No debería ocurrir.
        return null; // Inalcanzable.
    }

    /** Indica si la lista contiene la celda (fila, columna). */
    private static boolean contiene(List<int[]> lista, int fila, int columna) {
        for (int[] c : lista) { // Revisa la lista.
            if (c[0] == fila && c[1] == columna) { // Coincide.
                return true; // Está.
            }
        }
        return false; // No está.
    }

    /** PARE: entre 6 y 10, en intersecciones sin semáforo, a la derecha del acceso y mirando al auto. */
    public void testPare() {
        int cantidad = Senalizacion.UBICACIONES_PARE.length; // Cantidad de PARE.
        assertTrue(cantidad >= 6 && cantidad <= 10); // Entre 6 y 10.
        for (int i = 0; i < cantidad; i++) { // Revisa cada PARE.
            int[] u = Senalizacion.UBICACIONES_PARE[i]; // {fila, columna, dFila, dColumna}.
            assertTrue(Mapa.esInterseccion(u[0], u[1])); // Está en una intersección.
            assertFalse("PARE junto a semáforo", contiene(Senalizacion.INTERSECCIONES_SEMAFORO, u[0], u[1])); // Nunca con semáforo.
            assertTrue(Mapa.esCalleSegura(u[0] + u[2], u[1] + u[3])); // El acceso existe.
            float[] p = Senalizacion.PARES.get(i); // Posición calculada.
            comprobarEsquinaDerecha("PARE", p[0], p[1], p[2], u[0], u[1], u[2], u[3]); // Derecha, vereda, orientación.
        }
    }

    /** Carteles: uno por sector, dentro de su sector, sin ocupar la calle ni con postes ni con la placa. */
    public void testCartelesDeSector() {
        assertEquals(Mapa.SECTORES.length, Senalizacion.CARTELES_SECTOR.length); // Uno por sector.
        boolean[] visto = new boolean[Mapa.SECTORES.length]; // Sectores ya cubiertos.
        for (float[] c : Senalizacion.CARTELES_SECTOR) { // Revisa cada cartel.
            int sector = (int) c[0]; // Sector del cartel.
            assertFalse(visto[sector]); // No repetido.
            visto[sector] = true; // Marca el sector.
            assertEquals(sector, Mapa.sector(c[1], c[2])); // Está dentro de su propio sector.
            float ejeX = (float) Math.cos(c[3]); // Dirección de la placa (X local) en X.
            float ejeZ = -(float) Math.sin(c[3]); // Dirección de la placa en Z.
            for (int lado = -1; lado <= 1; lado++) { // Extremos y centro de la placa.
                float x = c[1] + ejeX * lado * (Senalizacion.ANCHO_CARTEL / 2 + Senalizacion.BORDE_CARTEL); // Punto de la placa.
                float z = c[2] + ejeZ * lado * (Senalizacion.ANCHO_CARTEL / 2 + Senalizacion.BORDE_CARTEL); // Punto de la placa.
                assertFalse("cartel sobre la calle", Mapa.esCalleEn(x, z)); // Ni postes ni placa sobre la calle.
            }
        }
    }

    /** Las calles siguen transitables: la señalización no agrega obstáculos (todas las calles pasan la colisión). */
    public void testColisionesIntactas() {
        for (int fila = 0; fila < Mapa.MAPA.length; fila++) { // Recorre las filas.
            for (int columna = 0; columna < Mapa.MAPA[fila].length; columna++) { // Recorre las columnas.
                if (Mapa.esCalle(fila, columna)) { // Solo las calles.
                    assertTrue(Colisiones.puedeCircular(Mapa.centro(columna), Mapa.centro(fila))); // El centro de cada calle es transitable.
                }
            }
        }
        for (float[] s : Senalizacion.SEMAFOROS) { // Ningún semáforo en calle.
            assertFalse(Mapa.esCalleEn(s[0], s[1])); // Sobre la vereda.
        }
        for (float[] p : Senalizacion.PARES) { // Ningún PARE en calle.
            assertFalse(Mapa.esCalleEn(p[0], p[1])); // Sobre la vereda.
        }
    }
}
