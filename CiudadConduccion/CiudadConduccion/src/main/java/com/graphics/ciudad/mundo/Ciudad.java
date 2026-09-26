package com.graphics.ciudad.mundo; // Agrupa lo que forma la ciudad: mapa, edificios, decoración y semáforos.

import com.graphics.ciudad.motor.Cubo; // Dibuja cada elemento como una caja transformada.

/**
 * CIUDAD: transforma el Mapa en geometría.
 * Responsable de: dibujar la base de asfalto, las marcas viales de las calles, las aceras, los edificios
 * con su cubierta y el césped de los parques.
 * Se comunica con: Mapa (lee celdas, centros y alturas) y Cubo (dibuja cada pieza). Juego la dibuja
 * primero en cada pase, antes del auto, las farolas y la decoración.
 */
public class Ciudad {

    private final Cubo cubo; // Geometría compartida con la que se construye toda la ciudad.

    /** Recibe el cubo con el que se dibujará la ciudad. */
    public Ciudad(Cubo cubo) {
        this.cubo = cubo; // Guarda la referencia para usarla en cada cuadro.
    }

    // ==================== CIUDAD A PARTIR DE UNA MATRIZ ====================

    /** Recorre el mapa y transforma cada celda en geometría. */
    public void dibujar() {
        cubo.caja(0, -0.25f, 0, Mapa.TAMANO, 0.5f, Mapa.TAMANO, 0.16f, 0.19f, 0.23f); // Dibuja la base de asfalto con su cara superior en Y=0.
        for (int fila = 0; fila < Mapa.MAPA.length; fila++) { // Recorre el mapa de norte a sur.
            for (int columna = 0; columna < Mapa.MAPA[fila].length; columna++) { // Recorre cada fila de izquierda a derecha.
                float x = Mapa.centro(columna); // Convierte la columna a posición X.
                float z = Mapa.centro(fila); // Convierte la fila a posición Z.
                int tipo = Mapa.tipo(fila, columna); // Lee si la celda es calle, edificio o parque.
                if (tipo == Mapa.CALLE) { // Selecciona las celdas transitables.
                    dibujarMarcasCalle(fila, columna, x, z); // Añade líneas amarillas entre intersecciones.
                } else { // Las demás celdas representan manzanas completas.
                    cubo.caja(x, 0.15f, z, Mapa.TAM_CELDA, 0.3f, Mapa.TAM_CELDA, 0.60f, 0.64f, 0.66f); // Dibuja la acera elevada sobre el asfalto.
                    if (tipo == Mapa.EDIFICIO) { // Selecciona una manzana ocupada por un edificio.
                        float altura = Mapa.alturaEdificio(fila, columna); // Varía la altura de forma reproducible entre 5 y 13.
                        float rojo = 0.28f + columna * 0.045f; // Varía el tono rojo según la columna.
                        float verde = 0.34f + ((fila + columna) % 3) * 0.05f; // Alterna tres tonos de verde entre manzanas vecinas.
                        float azul = 0.48f + fila * 0.025f; // Varía el tono azul según la fila.
                        float ancho = Mapa.ANCHO_EDIFICIO; // Toma el ancho de la base definido en Mapa.
                        cubo.caja(x, altura / 2 + 0.3f, z, ancho, altura, ancho, rojo, verde, azul); // Coloca la base del edificio sobre la acera.
                        cubo.caja(x, altura + 0.45f, z, ancho + 0.3f, 0.3f, ancho + 0.3f, 0.20f, 0.26f, 0.32f); // Añade una cubierta más ancha y oscura.
                    } else { // El tipo 2 representa un parque.
                        cubo.caja(x, 0.32f, z, 9, 0.1f, 9, 0.20f, 0.45f, 0.28f); // Cubre la parcela con césped verde.
                    }
                }
            }
        }
    }

    /** Dibuja las líneas discontinuas de las calles dejando los cruces despejados. */
    private void dibujarMarcasCalle(int fila, int columna, float x, float z) {
        if (fila % 2 == 0 && columna % 2 == 1) { // Identifica un tramo horizontal situado entre cruces.
            for (int desplazamiento = -3; desplazamiento <= 3; desplazamiento += 3) { // Coloca tres marcas en la celda.
                cubo.caja(x + desplazamiento, 0.025f, z, 1.6f, 0.03f, 0.13f, 1, 0.84f, 0.35f); // Dibuja una línea alargada en X.
            }
        }
        if (columna % 2 == 0 && fila % 2 == 1) { // Identifica un tramo vertical situado entre cruces.
            for (int desplazamiento = -3; desplazamiento <= 3; desplazamiento += 3) { // Repite las marcas sobre ese tramo.
                cubo.caja(x, 0.025f, z + desplazamiento, 0.13f, 0.03f, 1.6f, 1, 0.84f, 0.35f); // Dibuja una línea alargada en Z.
            }
        }
    }
}
