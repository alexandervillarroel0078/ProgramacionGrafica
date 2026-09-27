package com.graphics.ciudad.mundo; // Prueba los edificios desde su mismo paquete.

import java.util.EnumSet; // Conjunto de tipos que aparecen.
import java.util.List; // Listas de volúmenes y piezas.
import java.util.Set; // Tipo del conjunto.
import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba tipos, colores y huella de los edificios sin abrir una ventana OpenGL. */
public class EdificioTest extends TestCase {

    /** La misma celda da siempre el mismo tipo, los mismos colores y las mismas piezas. */
    public void testMismaCeldaMismoEdificio() {
        for (int fila = 0; fila < Mapa.MAPA.length; fila++) { // Filas del mapa.
            for (int columna = 0; columna < Mapa.MAPA[fila].length; columna++) { // Columnas.
                if (Mapa.tipo(fila, columna) != Mapa.EDIFICIO) { // Solo edificios.
                    continue;
                }
                assertEquals(Edificio.tipo(fila, columna), Edificio.tipo(fila, columna)); // Mismo tipo.
                assertEquals(Edificio.colorPared(fila, columna), Edificio.colorPared(fila, columna)); // Mismo color de pared.
                assertEquals(Edificio.colorTecho(fila, columna), Edificio.colorTecho(fila, columna)); // Mismo techo.
                List<Edificio.Pieza> a = Edificio.piezas(fila, columna); // Primera vez.
                List<Edificio.Pieza> b = Edificio.piezas(fila, columna); // Otra vez, como en otro cuadro.
                assertEquals(a.size(), b.size()); // Mismas piezas...
                for (int i = 0; i < a.size(); i++) { // ...en el mismo lugar y del mismo color.
                    assertEquals(a.get(i).forma, b.get(i).forma);
                    assertEquals(a.get(i).x, b.get(i).x, 0f);
                    assertEquals(a.get(i).y, b.get(i).y, 0f);
                    assertEquals(a.get(i).z, b.get(i).z, 0f);
                    assertEquals(a.get(i).sy, b.get(i).sy, 0f);
                    assertTrue(java.util.Arrays.equals(a.get(i).color, b.get(i).color));
                }
            }
        }
    }

    /** En la ciudad aparecen al menos 4 tipos de edificio y al menos 4 colores de pared distintos. */
    public void testVariedad() {
        Set<TipoEdificio> tipos = EnumSet.noneOf(TipoEdificio.class); // Tipos usados.
        boolean[] paredes = new boolean[Edificio.PALETA_FACHADAS.length]; // Colores usados.
        boolean techoNoOscuro = false; // Algún techo que no sea gris oscuro.
        for (int fila = 0; fila < Mapa.MAPA.length; fila++) {
            for (int columna = 0; columna < Mapa.MAPA[fila].length; columna++) {
                if (Mapa.tipo(fila, columna) == Mapa.EDIFICIO) {
                    tipos.add(Edificio.tipo(fila, columna)); // Registra el tipo.
                    paredes[Edificio.colorPared(fila, columna)] = true; // Registra el color.
                    techoNoOscuro |= Edificio.colorTecho(fila, columna) != 0; // Registra el techo.
                }
            }
        }
        assertTrue("tipos=" + tipos, tipos.size() >= 4); // Al menos 4 tipos.
        int colores = 0; // Cantidad de colores de pared usados.
        for (boolean usado : paredes) {
            colores += usado ? 1 : 0;
        }
        assertTrue("colores=" + colores, colores >= 4); // Paleta variada.
        assertTrue(techoNoOscuro); // No todos los techos son gris oscuro.
    }

    /** Ninguna pieza de ningún edificio sale de la huella de su manzana, y todo está sobre la acera. */
    public void testDentroDeLaHuella() {
        assertTrue(Edificio.MEDIA_HUELLA < Mapa.TAM_CELDA / 2); // La huella deja vereda libre.
        for (int fila = 0; fila < Mapa.MAPA.length; fila++) {
            for (int columna = 0; columna < Mapa.MAPA[fila].length; columna++) {
                if (Mapa.tipo(fila, columna) != Mapa.EDIFICIO) {
                    continue;
                }
                float cx = Mapa.centro(columna); // Centro de la manzana.
                float cz = Mapa.centro(fila);
                for (Edificio.Pieza p : Edificio.piezas(fila, columna)) { // Cada pieza.
                    String donde = Edificio.tipo(fila, columna) + " en " + fila + "," + columna; // Para el mensaje.
                    assertTrue(donde, Math.abs(p.x - cx) + p.mitadX() <= Edificio.MEDIA_HUELLA + 1e-4f); // Dentro en X.
                    assertTrue(donde, Math.abs(p.z - cz) + p.mitadZ() <= Edificio.MEDIA_HUELLA + 1e-4f); // Dentro en Z.
                    assertTrue(donde, p.y - p.sy / 2 >= Edificio.ALTURA_ACERA - 1e-4f); // No se hunde en la acera.
                }
                Edificio.Volumen base = Edificio.volumenes(fila, columna).get(0); // La planta baja comercial va acá.
                assertEquals(Edificio.ALTURA_ACERA, base.yBase, 1e-6f); // Apoyada en la acera.
                assertTrue(base.yTope - base.yBase >= Edificio.ALTO_MINIMO_BASE - 1e-4f); // Entra la planta baja con su toldo.
            }
        }
    }

    /** Cada tipo respeta su forma: la torre es la más alta y angosta, el escalonado se achica y el doble tiene dos alturas. */
    public void testFormaDeCadaTipo() {
        for (int fila = 0; fila < Mapa.MAPA.length; fila++) {
            for (int columna = 0; columna < Mapa.MAPA[fila].length; columna++) {
                if (Mapa.tipo(fila, columna) != Mapa.EDIFICIO) {
                    continue;
                }
                List<Edificio.Volumen> v = Edificio.volumenes(fila, columna); // Volúmenes.
                switch (Edificio.tipo(fila, columna)) {
                    case TORRE:
                        assertTrue(v.get(1).anchoX < v.get(0).anchoX); // Angosta sobre el podio.
                        assertTrue(v.get(1).yTope - Edificio.ALTURA_ACERA >= Edificio.alturaPared(Edificio.PISOS_TORRE_MIN) - 1e-4f); // Alta.
                        break;
                    case ESCALONADO:
                        assertTrue(v.size() >= 2 && v.size() <= 3); // 2 o 3 niveles.
                        for (int i = 1; i < v.size(); i++) {
                            assertTrue(v.get(i).anchoX < v.get(i - 1).anchoX); // Cada uno más chico...
                            assertEquals(v.get(i - 1).yTope, v.get(i).yBase, 1e-4f); // ...y apoyado en el de abajo.
                        }
                        break;
                    case CASA_BAJA:
                        assertEquals(Edificio.TECHO_TEJA, Edificio.colorTecho(fila, columna)); // Techo de teja.
                        boolean prisma = false; // Techo a dos aguas.
                        for (Edificio.Pieza p : Edificio.piezas(fila, columna)) {
                            prisma |= p.forma == Edificio.Forma.PRISMA;
                        }
                        assertTrue(prisma);
                        break;
                    case DOBLE:
                        assertEquals(2, v.size()); // Dos volúmenes...
                        assertTrue(v.get(0).yTope != v.get(1).yTope); // ...de distinta altura.
                        break;
                    default:
                        assertEquals(1, v.size()); // El bloque es una sola caja.
                        break;
                }
            }
        }
    }

    /**
     * Escala (1 u ≈ 1 m): el piso mide lo de uno real (≈ 3), la puerta entra en la planta baja, y la torre más baja
     * supera a la parte más alta de cualquier otro tipo, así sigue leyéndose como torre.
     */
    public void testEscalaDePisosYAlturas() {
        assertTrue(Edificio.ALTO_PISO >= 2.8f && Edificio.ALTO_PISO <= 3.2f); // Piso real.
        assertTrue(Fachada.ALTO_PUERTA < Edificio.ALTO_MINIMO_BASE); // La puerta entra en la planta baja.
        float torreMasBaja = Edificio.alturaPared(Edificio.PISOS_TORRE_MIN); // 24.2.
        float otrosMasAlto = Math.max(Edificio.alturaPared(Edificio.PISOS_BLOQUE_MAX),
            Math.max(Edificio.alturaPared(Edificio.PISOS_DOBLE_ALTA_MAX),
                Edificio.alturaPared(Edificio.PISOS_PRIMER_NIVEL_MAX + (Edificio.NIVELES_MAX - 1) * Edificio.PISOS_POR_NIVEL))); // El más alto de los demás tipos.
        assertTrue(torreMasBaja > otrosMasAlto);
        for (int fila = 0; fila < Mapa.MAPA.length; fila++) { // Ningún techo supera la altura máxima calculada.
            for (int columna = 0; columna < Mapa.MAPA[fila].length; columna++) {
                if (Mapa.tipo(fila, columna) == Mapa.EDIFICIO) {
                    assertTrue(Edificio.alturaTotal(fila, columna) <= Edificio.ALTURA_MAXIMA + 1e-4f);
                }
            }
        }
    }
}
