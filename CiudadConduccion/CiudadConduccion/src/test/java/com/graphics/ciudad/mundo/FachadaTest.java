package com.graphics.ciudad.mundo; // Prueba las fachadas desde su mismo paquete.

import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba ventanas y planta baja sin abrir una ventana OpenGL. */
public class FachadaTest extends TestCase {


    /** Recorre todas las ventanas reales de la ciudad: edificio, cara, piso (desde 1 si la cara da a la calle) y columna. */
    private interface Visitante {
        void visitar(int fila, int columna, int cara, int piso, int col); // Se llama una vez por ventana.
    }

    /** Aplica el visitante a cada ventana que Fachada dibuja. */
    private static void recorrerVentanas(Visitante visitante) {
        for (int fila = 0; fila < Mapa.MAPA.length; fila++) { // Filas del mapa.
            for (int columna = 0; columna < Mapa.MAPA[fila].length; columna++) { // Columnas.
                if (Mapa.tipo(fila, columna) != Mapa.EDIFICIO) { // Solo edificios.
                    continue; // Salta calles y parques.
                }
                float altura = Mapa.alturaEdificio(fila, columna); // Altura del edificio.
                for (int cara = 0; cara < Mapa.VECINOS.length; cara++) { // Cuatro caras.
                    int[] v = Mapa.VECINOS[cara]; // Dirección de la cara.
                    int desde = Mapa.esCalleSegura(fila + v[0], columna + v[1]) ? 1 : 0; // La planta baja comercial ocupa el piso 0.
                    for (int piso = desde; Fachada.PRIMER_PISO_Y + piso * Fachada.ALTURA_PISO < altura; piso++) { // Pisos con ventanas.
                        for (int col = 0; col < Fachada.VENTANAS_POR_FACHADA; col++) { // Columnas de ventanas.
                            visitante.visitar(fila, columna, cara, piso, col); // Una ventana.
                        }
                    }
                }
            }
        }
    }

    /** La misma ventana (edificio, cara, piso, columna) siempre tiene el mismo estado y color. */
    public void testEstadoDeterministico() {
        recorrerVentanas((fila, columna, cara, piso, col) -> { // Cada ventana de la ciudad.
            int primera = Fachada.tonoVentana(fila, columna, cara, piso, col); // Primera consulta.
            for (int repeticion = 0; repeticion < 3; repeticion++) { // Consultas posteriores, como en cuadros siguientes.
                assertEquals(primera, Fachada.tonoVentana(fila, columna, cara, piso, col)); // Nunca cambia: sin parpadeo.
            }
            float[] a = Fachada.colorVentana(fila, columna, cara, piso, col, true); // Color de noche.
            float[] b = Fachada.colorVentana(fila, columna, cara, piso, col, true); // Otra vez.
            assertTrue(java.util.Arrays.equals(a, b)); // Mismo color.
        });
    }

    /** De noche, la fracción de ventanas encendidas está dentro de ±10 % de PORCENTAJE_VENTANAS_ENCENDIDAS, y hay variedad de tonos. */
    public void testPorcentajeEncendidasDeNoche() {
        int[] total = {0}; // Ventanas contadas.
        int[] encendidas = {0}; // Ventanas encendidas.
        boolean[] tonosUsados = new boolean[Fachada.TONOS_VENTANA.length]; // Qué tonos aparecen.
        recorrerVentanas((fila, columna, cara, piso, col) -> { // Cada ventana.
            total[0]++; // Una ventana más.
            int tono = Fachada.tonoVentana(fila, columna, cara, piso, col); // Estado de noche.
            if (tono >= 0) { // Encendida.
                encendidas[0]++; // Cuenta la encendida.
                tonosUsados[tono] = true; // Registra el tono.
                assertEquals(1f, Fachada.colorVentana(fila, columna, cara, piso, col, true)[3], 0f); // Encendida = emisiva.
            } else { // Apagada.
                assertEquals(0f, Fachada.colorVentana(fila, columna, cara, piso, col, true)[3], 0f); // Apagada = sin emisión.
            }
        });
        float fraccion = (float) encendidas[0] / total[0]; // Proporción encendida.
        assertTrue("total=" + total[0], total[0] > 200); // Hay suficientes ventanas para medir.
        assertEquals("encendidas=" + fraccion, Fachada.PORCENTAJE_VENTANAS_ENCENDIDAS, fraccion, 0.10f); // Dentro de ±10 %.
        for (boolean usado : tonosUsados) { // Cada tono.
            assertTrue(usado); // Todos los tonos aparecen en la ciudad.
        }
    }

    /** De día ninguna ventana es emisiva y todas son vidrio claro (no amarillo). */
    public void testDeDiaNingunaEmisiva() {
        recorrerVentanas((fila, columna, cara, piso, col) -> { // Cada ventana.
            float[] c = Fachada.colorVentana(fila, columna, cara, piso, col, false); // Color de día.
            assertEquals(0f, c[3], 0f); // Sin emisión: la ilumina el sol.
            assertTrue(c[2] >= c[0]); // Vidrio celeste-grisáceo: el azul no es menor que el rojo (no amarillo).
        });
    }

    /** Vidriera | puerta | vidriera: todo entra en la cara con MARGEN_ESQUINA y los toldos no tapan la puerta. */
    public void testDistribucionDeLaPlantaBaja() {
        float mitadCara = Mapa.ANCHO_EDIFICIO / 2; // La cara va de u = -3.5 a u = 3.5.
        float[] puerta = Fachada.tramoPuerta(); // Tramo de la puerta.
        assertEquals(0f, (puerta[0] + puerta[1]) / 2, 1e-6f); // La puerta está centrada.
        for (float[] v : Fachada.tramosVidrieras()) { // Cada vidriera.
            assertTrue(v[0] >= -mitadCara + Fachada.MARGEN_ESQUINA - 1e-4f); // Deja el margen de la esquina izquierda.
            assertTrue(v[1] <= mitadCara - Fachada.MARGEN_ESQUINA + 1e-4f); // Deja el margen de la esquina derecha.
            boolean separada = v[1] <= puerta[0] - Fachada.SEPARACION_PUERTA + 1e-4f || v[0] >= puerta[1] + Fachada.SEPARACION_PUERTA - 1e-4f; // No toca la puerta.
            assertTrue(separada); // Hay pared entre la vidriera y la puerta.
        }
        for (float[] t : Fachada.tramosToldos()) { // Cada toldo.
            assertTrue(t[1] <= puerta[0] || t[0] >= puerta[1]); // El toldo no tapa la puerta.
            assertTrue(Math.abs(t[0]) <= mitadCara && Math.abs(t[1]) <= mitadCara); // Tampoco pasa la esquina.
        }
    }

    /**
     * Ninguna puerta está a menos de MARGEN_ESQUINA de una esquina del edificio, y dos caras vecinas nunca tienen sus
     * puertas del lado de la esquina que comparten (una puerta "se inclina" hacia una esquina si su centro está más cerca
     * de ella que el centro de la cara).
     */
    public void testPuertasLejosDeLasEsquinas() {
        float mitad = Mapa.ANCHO_EDIFICIO / 2; // Del centro del edificio a cada pared.
        float pared = mitad + 0.03f; // Plano donde se dibuja la puerta (apenas delante de la pared).
        float[] tramo = Fachada.tramoPuerta(); // Extremos de la puerta sobre el eje u.
        for (int fila = 0; fila < Mapa.MAPA.length; fila++) { // Filas.
            for (int columna = 0; columna < Mapa.MAPA[fila].length; columna++) { // Columnas.
                if (Mapa.tipo(fila, columna) != Mapa.EDIFICIO) { // Solo edificios.
                    continue; // Salta calles y parques.
                }
                float x = Mapa.centro(columna); // Centro del edificio en X.
                float z = Mapa.centro(fila); // Centro del edificio en Z.
                float[][] esquinas = {{x - mitad, z - mitad}, {x + mitad, z - mitad}, {x - mitad, z + mitad}, {x + mitad, z + mitad}}; // Cuatro esquinas.
                float[][] centrosPuerta = new float[Mapa.VECINOS.length][]; // Centro de la puerta de cada cara a la calle.
                for (int cara = 0; cara < Mapa.VECINOS.length; cara++) { // Cada cara.
                    int[] v = Mapa.VECINOS[cara]; // Dirección de la cara.
                    if (!Mapa.esCalleSegura(fila + v[0], columna + v[1])) { // Sin calle no hay puerta.
                        continue; // Siguiente cara.
                    }
                    float[] extremoA = Fachada.puntoEnCara(x, z, cara, tramo[0], pared); // Un extremo de la puerta.
                    float[] extremoB = Fachada.puntoEnCara(x, z, cara, tramo[1], pared); // El otro extremo.
                    centrosPuerta[cara] = Fachada.puntoEnCara(x, z, cara, (tramo[0] + tramo[1]) / 2, pared); // Centro de la puerta.
                    for (float[] e : esquinas) { // Cada esquina del edificio.
                        assertTrue(Math.hypot(extremoA[0] - e[0], extremoA[1] - e[1]) >= Fachada.MARGEN_ESQUINA); // Lejos de la esquina.
                        assertTrue(Math.hypot(extremoB[0] - e[0], extremoB[1] - e[1]) >= Fachada.MARGEN_ESQUINA); // Lejos de la esquina.
                    }
                }
                for (float[] e : esquinas) { // Cada esquina la comparten dos caras vecinas.
                    int inclinadas = 0; // Puertas que se inclinan hacia esta esquina.
                    for (int cara = 0; cara < Mapa.VECINOS.length; cara++) { // Cada cara con puerta.
                        if (centrosPuerta[cara] == null) { // Sin puerta.
                            continue; // Siguiente cara.
                        }
                        float[] centroCara = Fachada.puntoEnCara(x, z, cara, 0, pared); // Centro de la cara.
                        double hastaPuerta = Math.hypot(centrosPuerta[cara][0] - e[0], centrosPuerta[cara][1] - e[1]); // Puerta → esquina.
                        double hastaCentro = Math.hypot(centroCara[0] - e[0], centroCara[1] - e[1]); // Centro de la cara → esquina.
                        if (hastaPuerta < hastaCentro - 1e-4) { // La puerta está corrida hacia esta esquina.
                            inclinadas++; // Cuenta la puerta.
                        }
                    }
                    assertTrue("puertas pegadas en una esquina de " + fila + "," + columna, inclinadas <= 1); // Nunca dos puertas en la misma esquina.
                }
            }
        }
    }

    /** Toldo, puerta y vidriera: colores válidos y el toldo no llega a la calzada. */
    public void testPlantaBaja() {
        float bordeToldo = Mapa.ANCHO_EDIFICIO / 2 + Fachada.VUELO_TOLDO; // Distancia del centro del edificio al borde del toldo.
        assertTrue(bordeToldo < Mapa.TAM_CELDA / 2); // Queda sobre la vereda, antes del cordón (5).
        boolean[] colores = new boolean[Fachada.COLORES_TOLDO.length]; // Colores de toldo usados.
        boolean hayRayas = false; // Algún toldo a rayas.
        boolean hayLisos = false; // Algún toldo liso.
        for (int fila = 0; fila < Mapa.MAPA.length; fila++) { // Filas.
            for (int columna = 0; columna < Mapa.MAPA[fila].length; columna++) { // Columnas.
                if (Mapa.tipo(fila, columna) == Mapa.EDIFICIO) { // Solo edificios.
                    colores[Fachada.colorToldo(fila, columna)] = true; // Registra el color.
                    hayRayas |= Fachada.toldoARayas(fila, columna); // Registra rayas.
                    hayLisos |= !Fachada.toldoARayas(fila, columna); // Registra lisos.
                    assertEquals(Fachada.colorToldo(fila, columna), Fachada.colorToldo(fila, columna)); // Determinístico.
                }
            }
        }
        for (boolean usado : colores) { // Cada color.
            assertTrue(usado); // Todos los colores aparecen.
        }
        assertTrue(hayRayas && hayLisos); // Hay toldos lisos y a rayas.
    }
}
