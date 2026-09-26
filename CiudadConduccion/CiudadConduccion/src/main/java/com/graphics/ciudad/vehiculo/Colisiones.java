package com.graphics.ciudad.vehiculo; // Agrupa el vehículo del jugador y sus colisiones.

import com.graphics.ciudad.mundo.Mapa; // Proporciona las manzanas y los límites de la ciudad.

/**
 * COLISIONES: decide si el auto cabe en una posición.
 * Responsable de: la prueba círculo contra rectángulo entre el auto (radio RADIO_AUTO) y cada manzana,
 * usando el punto más cercano y la distancia al cuadrado, y de impedir salir por los bordes del mapa.
 * También ofrece la prueba círculo contra círculo que usa Trafico para que el jugador no atraviese otros vehículos.
 * Se comunica con: Mapa (celdas y límites), Auto (lo consulta antes de moverse) y Trafico. No usa OpenGL.
 * El círculo de colisión es conservador para contener todas las piezas; el choque detiene al auto, sin rebotes.
 */
public final class Colisiones {

    // ==================== COLISIONES CON LA CIUDAD ====================

    /** Impide crear objetos: la prueba no guarda estado. */
    private Colisiones() {
    }

    /** Comprueba si el círculo del auto cabe en una posición sin tocar manzanas ni bordes. */
    public static boolean puedeCircular(float x, float z) {
        float limitePermitido = Mapa.LIMITE - Auto.RADIO_AUTO; // Reserva espacio para que el auto completo quede dentro.

        if (Math.abs(x) > limitePermitido || Math.abs(z) > limitePermitido) { // Detecta salida por cualquier borde.
            return false; // Rechaza la posición exterior.
        }

        for (int fila = 0; fila < Mapa.MAPA.length; fila++) { // Recorre las filas del mapa.
            for (int columna = 0; columna < Mapa.MAPA[fila].length; columna++) { // Recorre las celdas de cada fila.
                if (Mapa.esCalle(fila, columna)) { // Una celda con cero representa calle.
                    continue; // Omite la calle porque no es un obstáculo.
                }

                float centroX = Mapa.centro(columna); // Convierte la columna al centro X de la manzana.
                float centroZ = Mapa.centro(fila); // Convierte la fila al centro Z de la manzana.
                float mitadCelda = Mapa.TAM_CELDA / 2; // La manzana ocupa la celda completa: 5 unidades a cada lado de su centro.
                float cercaX = Math.max(centroX - mitadCelda, Math.min(x, centroX + mitadCelda)); // Busca el X más cercano dentro de la acera.
                float cercaZ = Math.max(centroZ - mitadCelda, Math.min(z, centroZ + mitadCelda)); // Busca el Z más cercano dentro de la acera.
                float distanciaX = x - cercaX; // Calcula la separación horizontal del auto al rectángulo.
                float distanciaZ = z - cercaZ; // Calcula la separación en profundidad al rectángulo.
                float distanciaCuadrada = distanciaX * distanciaX + distanciaZ * distanciaZ; // Aplica Pitágoras sin raíz.

                if (distanciaCuadrada < Auto.RADIO_AUTO * Auto.RADIO_AUTO) { // Comprueba si el círculo invade la manzana.
                    return false; // Rechaza el movimiento que produciría una colisión.
                }
            }
        }

        return true; // Acepta la posición porque no se encontró ningún obstáculo.
    }

    /** Círculo contra círculo: dos objetos se tocan si la distancia entre centros es menor que la suma de sus radios. */
    public static boolean circulosSeSolapan(float x1, float z1, float radio1, float x2, float z2, float radio2) {
        float distanciaX = x1 - x2; // Separación horizontal entre los centros.
        float distanciaZ = z1 - z2; // Separación en profundidad entre los centros.
        float sumaRadios = radio1 + radio2; // Distancia mínima permitida entre centros.
        return distanciaX * distanciaX + distanciaZ * distanciaZ < sumaRadios * sumaRadios; // Compara al cuadrado para evitar la raíz.
    }
}
