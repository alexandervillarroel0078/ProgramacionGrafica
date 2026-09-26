package com.graphics.ciudad.mundo; // Agrupa lo que forma la ciudad: mapa, edificios, decoración y semáforos.

import com.graphics.ciudad.motor.Cubo; // Dibuja árboles, bancos, ventanas y franjas.
import com.graphics.ciudad.motor.Shader; // Activa la emisión de las ventanas nocturnas.

/**
 * DECORACION: detalles urbanos de la ciudad terminada.
 * Responsable de: decidir qué decoración corresponde a cada parcela del Mapa y dibujar parques (árboles y
 * banco), ventanas iluminadas, pasos peatonales, semáforos y señales de tránsito.
 * Se comunica con: Mapa (recorre las celdas), Cubo y Shader (dibujo y emisión), Semaforo y Senalizacion (en cada manzana).
 * Juego la dibuja solo en la vista principal; en el minimapa se omite, y le pasa el estado de noche de
 * Iluminacion y el reloj global de Juego (los semáforos siguen ciclando aunque la partida termine).
 */
public class Decoracion {

    private final Shader shader; // Programa que recibe el interruptor de emisión.
    private final Cubo cubo; // Geometría con la que se construyen los detalles.
    private final Semaforo semaforo; // Dibuja los semáforos junto a cada manzana.
    private final Senalizacion senalizacion; // Dibuja las señales de PARE y de dirección en las esquinas.

    /** Recibe el shader y el cubo compartidos y prepara el semáforo reutilizable. */
    public Decoracion(Shader shader, Cubo cubo) {
        this.shader = shader; // Guarda el programa para cambiar uEmision.
        this.cubo = cubo; // Guarda la geometría compartida.
        this.semaforo = new Semaforo(shader, cubo); // Un mismo objeto dibuja todos los semáforos.
        this.senalizacion = new Senalizacion(cubo); // Un mismo objeto dibuja todas las señales.
    }

    // ==================== DECORACIÓN DE LAS MANZANAS ====================

    /** Decide qué decoración corresponde a cada tipo de parcela. */
    public void dibujar(boolean noche, float tiempo) {
        for (int fila = 0; fila < Mapa.MAPA.length; fila++) { // Recorre las filas del mapa.
            for (int columna = 0; columna < Mapa.MAPA[fila].length; columna++) { // Recorre las columnas de esa fila.
                float x = Mapa.centro(columna); // Obtiene el centro horizontal de la parcela.
                float z = Mapa.centro(fila); // Obtiene el centro de la parcela en profundidad.
                int tipo = Mapa.tipo(fila, columna); // Lee el contenido de la celda.
                if (tipo == Mapa.PARQUE) { // Detecta una parcela de parque.
                    dibujarParque(x, z); // Añade árboles y un banco.
                }
                if (tipo == Mapa.EDIFICIO) { // Detecta una parcela con edificio.
                    float altura = Mapa.alturaEdificio(fila, columna); // Recupera la misma altura calculada en Ciudad.
                    dibujarVentanas(x, z, altura, noche); // Coloca ventanas en sus cuatro fachadas.
                }
                if (tipo != Mapa.CALLE) { // La señalización se coloca junto a las manzanas, no en celdas de calle.
                    dibujarPasoPeatonal(x, z); // Añade el cruce pintado sobre la calle contigua.
                    semaforo.dibujar(x + 4, z - 4, tiempo); // Coloca el semáforo dentro de la acera.
                    senalizacion.dibujarEnManzana(x, z); // Añade PARE y la señal de dirección en las esquinas del sur.
                }
            }
        }
    }

    /** Construye cuatro árboles y un banco utilizando cajas. */
    private void dibujarParque(float x, float z) {
        float[] posiciones = {-2.5f, 2.5f}; // Define desplazamientos respecto al centro de la parcela.
        for (float desplazamientoX : posiciones) { // Selecciona el lado izquierdo o derecho del parque.
            for (float desplazamientoZ : posiciones) { // Selecciona el lado delantero o trasero.
                float arbolX = x + desplazamientoX; // Convierte el desplazamiento local en coordenada X mundial.
                float arbolZ = z + desplazamientoZ; // Convierte el desplazamiento local en coordenada Z mundial.
                cubo.caja(arbolX, 1.2f, arbolZ, 0.35f, 2, 0.35f, 0.38f, 0.22f, 0.12f); // Dibuja el tronco marrón.
                cubo.caja(arbolX, 2.7f, arbolZ, 2, 2.3f, 2, 0.12f, 0.42f, 0.23f); // Dibuja la copa verde del árbol.
            }
        }
        cubo.caja(x, 0.65f, z, 3, 0.25f, 0.8f, 0.55f, 0.30f, 0.13f); // Dibuja el asiento de madera del banco.
        cubo.caja(x, 0.4f, z, 2, 0.6f, 0.35f, 0.22f, 0.24f, 0.24f); // Dibuja el soporte oscuro del banco.
        cubo.caja(x, 1, z + 0.35f, 3, 0.7f, 0.15f, 0.55f, 0.30f, 0.13f); // Dibuja el respaldo detrás del asiento.
    }

    /** Distribuye ventanas por pisos en las cuatro paredes del edificio. */
    private void dibujarVentanas(float x, float z, float altura, boolean noche) {
        if (noche) { // Las ventanas simulan habitaciones encendidas en el ambiente nocturno.
            shader.entero("uEmision", 1); // Permite ver el color de las ventanas sin depender de farolas.
        }
        float fachada = Mapa.ANCHO_EDIFICIO / 2 + 0.01f; // Separa la ventana 0.01 de la pared (3.51) para que no parpadee.
        for (float y = 1.7f; y < altura; y += 2) { // Recorre los pisos separados por dos unidades de altura.
            for (float desplazamiento = -2; desplazamiento <= 2; desplazamiento += 2) { // Coloca tres ventanas por fachada.
                cubo.caja(x + desplazamiento, y, z - fachada, 0.8f, 0.9f, 0.04f, 0.95f, 0.75f, 0.38f); // Ventana de la fachada norte.
                cubo.caja(x + desplazamiento, y, z + fachada, 0.8f, 0.9f, 0.04f, 0.95f, 0.75f, 0.38f); // Ventana de la fachada sur.
                cubo.caja(x - fachada, y, z + desplazamiento, 0.04f, 0.9f, 0.8f, 0.95f, 0.75f, 0.38f); // Ventana de la fachada oeste.
                cubo.caja(x + fachada, y, z + desplazamiento, 0.04f, 0.9f, 0.8f, 0.95f, 0.75f, 0.38f); // Ventana de la fachada este.
            }
        }
        shader.entero("uEmision", 0); // Restablece la iluminación normal de los demás elementos.
    }

    /** Dibuja las franjas blancas sobre la calle contigua al norte de la manzana. */
    private void dibujarPasoPeatonal(float x, float z) {
        for (int desplazamiento = -3; desplazamiento <= 3; desplazamiento++) { // Coloca siete franjas paralelas.
            cubo.caja(x + desplazamiento, 0.045f, z - 8, 0.45f, 0.04f, 2, 0.85f, 0.87f, 0.83f); // Eleva la pintura un poco sobre el suelo.
        }
    }
}
