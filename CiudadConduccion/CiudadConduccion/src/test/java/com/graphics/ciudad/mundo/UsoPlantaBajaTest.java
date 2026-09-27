package com.graphics.ciudad.mundo; // Prueba el uso de la planta baja desde su mismo paquete.

import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba la regla de usos de planta baja sin abrir una ventana OpenGL. Todo se deriva del MAPA: vale con 13 × 13. */
public class UsoPlantaBajaTest extends TestCase {

    /** Cada torre es lobby de oficinas y cada casa baja es casa. */
    public void testTorresSonLobbyYCasasSonCasa() {
        for (int fila = 0; fila < Mapa.MAPA.length; fila++) {
            for (int columna = 0; columna < Mapa.MAPA[fila].length; columna++) {
                if (Mapa.tipo(fila, columna) != Mapa.EDIFICIO) {
                    continue;
                }
                TipoEdificio tipo = Edificio.tipo(fila, columna);
                UsoPlantaBaja uso = UsoPlantaBaja.de(fila, columna);
                if (tipo == TipoEdificio.TORRE) {
                    assertEquals(fila + "," + columna, UsoPlantaBaja.LOBBY_OFICINAS, uso);
                } else if (tipo == TipoEdificio.CASA_BAJA) {
                    assertEquals(fila + "," + columna, UsoPlantaBaja.CASA, uso);
                } else { // Bloque, escalonado o doble: comercio en el Centro y sobre las avenidas; si no, departamentos.
                    boolean zonaComercial = Mapa.sectorDeCelda(fila, columna) == UsoPlantaBaja.SECTOR_COMERCIAL
                        || UsoPlantaBaja.daAAvenidaPrincipal(fila, columna);
                    assertEquals(fila + "," + columna, zonaComercial ? UsoPlantaBaja.COMERCIAL : UsoPlantaBaja.RESIDENCIAL, uso);
                }
            }
        }
    }

    /** Hay al menos 3 usos distintos y sigue habiendo bastante comercio (al menos el 40 % de los edificios). */
    public void testVariedadYBastanteComercio() {
        int[] cuenta = new int[UsoPlantaBaja.values().length];
        int edificios = 0;
        for (int fila = 0; fila < Mapa.MAPA.length; fila++) {
            for (int columna = 0; columna < Mapa.MAPA[fila].length; columna++) {
                if (Mapa.tipo(fila, columna) == Mapa.EDIFICIO) {
                    cuenta[UsoPlantaBaja.de(fila, columna).ordinal()]++;
                    edificios++;
                }
            }
        }
        int usados = 0;
        for (int n : cuenta) {
            usados += n > 0 ? 1 : 0;
        }
        assertTrue("usos distintos: " + usados, usados >= 3);
        float comercio = (float) cuenta[UsoPlantaBaja.COMERCIAL.ordinal()] / edificios;
        assertTrue("comercio: " + comercio, comercio >= 0.40f);
    }

    /** La misma celda da siempre el mismo uso (determinístico, sin azar por cuadro). */
    public void testMismaCeldaMismoUso() {
        for (int fila = 0; fila < Mapa.MAPA.length; fila++) {
            for (int columna = 0; columna < Mapa.MAPA[fila].length; columna++) {
                if (Mapa.tipo(fila, columna) == Mapa.EDIFICIO) {
                    UsoPlantaBaja primero = UsoPlantaBaja.de(fila, columna);
                    for (int i = 0; i < 3; i++) {
                        assertEquals(primero, UsoPlantaBaja.de(fila, columna));
                    }
                }
            }
        }
    }

    /** Las avenidas principales son calles (índice par) junto al eje central: 4 y 6 en el mapa 11 × 11. */
    public void testAvenidasPrincipales() {
        int eje = Mapa.MAPA.length / 2;
        for (int i = 0; i < Mapa.MAPA.length; i++) {
            boolean esperado = i % 2 == 0 && Math.abs(i - eje) <= UsoPlantaBaja.DISTANCIA_AVENIDA;
            assertEquals("índice " + i, esperado, UsoPlantaBaja.esAvenidaPrincipal(i));
        }
    }
}
