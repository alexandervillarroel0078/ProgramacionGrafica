package com.graphics.ciudad.mundo; // Prueba las fachadas desde su mismo paquete.

import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba ventanas y planta baja sin abrir una ventana OpenGL. */
public class FachadaTest extends TestCase {

    /** Recorre todas las ventanas reales de la ciudad, tal como Fachada las ubica en cada edificio. */
    private interface Visitante {
        void visitar(int fila, int columna, Fachada.Ventana ventana); // Se llama una vez por ventana.
    }

    /** Aplica el visitante a cada ventana que Fachada dibuja. */
    private static void recorrerVentanas(Visitante visitante) {
        for (int fila = 0; fila < Mapa.MAPA.length; fila++) { // Filas del mapa.
            for (int columna = 0; columna < Mapa.MAPA[fila].length; columna++) { // Columnas.
                if (Mapa.tipo(fila, columna) != Mapa.EDIFICIO) { // Solo edificios.
                    continue; // Salta calles y parques.
                }
                for (Fachada.Ventana v : Fachada.ventanas(fila, columna)) { // Ventanas de este edificio.
                    visitante.visitar(fila, columna, v); // Una ventana.
                }
            }
        }
    }

    /** La misma ventana (edificio, volumen, cara, piso, columna) siempre tiene el mismo estado y color. */
    public void testEstadoDeterministico() {
        recorrerVentanas((fila, columna, v) -> { // Cada ventana de la ciudad.
            int primera = Fachada.tonoVentana(fila, columna, v); // Primera consulta.
            for (int repeticion = 0; repeticion < 3; repeticion++) { // Consultas posteriores, como en cuadros siguientes.
                assertEquals(primera, Fachada.tonoVentana(fila, columna, v)); // Nunca cambia: sin parpadeo.
            }
            float[] a = Fachada.colorVentana(fila, columna, v, true); // Color de noche.
            float[] b = Fachada.colorVentana(fila, columna, v, true); // Otra vez.
            assertTrue(java.util.Arrays.equals(a, b)); // Mismo color.
        });
    }

    /** De noche, la fracción de ventanas encendidas está dentro de ±10 % de PORCENTAJE_VENTANAS_ENCENDIDAS, y hay variedad de tonos. */
    public void testPorcentajeEncendidasDeNoche() {
        int[] total = {0}; // Ventanas contadas.
        int[] encendidas = {0}; // Ventanas encendidas.
        boolean[] tonosUsados = new boolean[Fachada.TONOS_VENTANA.length]; // Qué tonos aparecen.
        recorrerVentanas((fila, columna, v) -> { // Cada ventana.
            total[0]++; // Una ventana más.
            int tono = Fachada.tonoVentana(fila, columna, v); // Estado de noche.
            if (tono >= 0) { // Encendida.
                encendidas[0]++; // Cuenta la encendida.
                tonosUsados[tono] = true; // Registra el tono.
                assertEquals(1f, Fachada.colorVentana(fila, columna, v, true)[3], 0f); // Encendida = emisiva.
            } else { // Apagada.
                assertEquals(0f, Fachada.colorVentana(fila, columna, v, true)[3], 0f); // Apagada = sin emisión.
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
        recorrerVentanas((fila, columna, v) -> { // Cada ventana.
            float[] c = Fachada.colorVentana(fila, columna, v, false); // Color de día.
            assertEquals(0f, c[3], 0f); // Sin emisión: la ilumina el sol.
            assertTrue(c[2] >= c[0]); // Vidrio celeste-grisáceo: el azul no es menor que el rojo (no amarillo).
        });
    }

    /**
     * Cada ventana queda sobre la pared de su volumen, entre su base y su tope, y ninguna queda sobre el negocio de la
     * planta baja ni adentro de otro volumen del mismo edificio.
     */
    public void testVentanasSobreParedesVisibles() {
        recorrerVentanas((fila, columna, v) -> { // Cada ventana.
            java.util.List<Edificio.Volumen> volumenes = Edificio.volumenes(fila, columna); // Volúmenes del edificio.
            Edificio.Volumen propio = volumenes.get(v.volumen); // El volumen donde está.
            assertTrue(v.y - v.alto / 2 >= propio.yBase + Fachada.MARGEN_VERTICAL - 1e-4f); // No baja de su volumen.
            assertTrue(v.y + v.alto / 2 <= propio.yTope - Fachada.MARGEN_VERTICAL + 1e-4f); // Ni sube por encima.
            int[] dir = Mapa.VECINOS[v.cara]; // Hacia dónde mira la ventana.
            float afuera = (v.x - Mapa.centro(columna)) * dir[1] + (v.z - Mapa.centro(fila)) * dir[0]; // Distancia al centro de la manzana.
            boolean enBorde = afuera > Mapa.ANCHO_EDIFICIO / 2 - 1e-3f; // Sobre la pared exterior de la huella.
            boolean conNegocio = enBorde && Mapa.esCalleSegura(fila + dir[0], columna + dir[1]); // Esa cara tiene planta baja comercial.
            assertTrue(!conNegocio || v.y - v.alto / 2 >= Fachada.TOPE_PLANTA_BAJA - 1e-4f); // Nunca sobre el negocio ni el toldo.
            for (int i = 0; i < volumenes.size(); i++) { // Los demás volúmenes.
                Edificio.Volumen otro = volumenes.get(i);
                boolean adentro = i != v.volumen && otro.cubre(v.x, v.z) && v.y > otro.yBase && v.y < otro.yTope; // Tapada.
                assertFalse("ventana tapada en " + fila + "," + columna, adentro);
            }
        });
    }

    /** El patrón cambia con el tipo: la torre tiene más columnas y pisos más bajos, el bloque ventanas más anchas. */
    public void testPatronDeVentanasSegunTipo() {
        assertTrue(TipoEdificio.TORRE.columnasVentanas > TipoEdificio.BLOQUE.columnasVentanas); // Más columnas.
        assertTrue(TipoEdificio.TORRE.alturaPiso < TipoEdificio.BLOQUE.alturaPiso); // Más filas.
        for (TipoEdificio t : TipoEdificio.values()) { // Ninguna ventana es más ancha que la del bloque.
            assertTrue(TipoEdificio.BLOQUE.anchoVentana >= t.anchoVentana);
        }
        assertTrue(TipoEdificio.CASA_BAJA.columnasVentanas <= 2); // Pocas en la casa.
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

    /** De noche la vidriera es más clara abajo que arriba, y menos intensa que la placa plana de antes (1, 0.84, 0.58). */
    public void testVidrieraDeNocheConDegradado() {
        float[] abajo = Fachada.colorVidrieraNoche(0); // Franja inferior.
        float[] arriba = Fachada.colorVidrieraNoche(Fachada.FRANJAS_VIDRIERA - 1); // Franja superior.
        assertTrue(Fachada.FRANJAS_VIDRIERA >= 3); // Hay degradado, no una placa.
        for (int franja = 1; franja < Fachada.FRANJAS_VIDRIERA; franja++) { // Cada franja...
            float[] anterior = Fachada.colorVidrieraNoche(franja - 1);
            float[] actual = Fachada.colorVidrieraNoche(franja);
            for (int i = 0; i < 3; i++) {
                assertTrue(actual[i] <= anterior[i]); // ...es igual o más tenue que la de abajo.
            }
        }
        assertTrue(abajo[0] + abajo[1] + abajo[2] > arriba[0] + arriba[1] + arriba[2]); // Más clara abajo.
        assertTrue(abajo[0] < 1.00f && abajo[1] < 0.84f && abajo[2] < 0.58f); // Menos intensa que antes.
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
