package com.graphics.ciudad.vehiculo; // Agrupa el vehículo del jugador, sus colisiones y su indicador.

import com.graphics.ciudad.motor.Cubo; // Dibuja las piezas de la flecha.
import com.graphics.ciudad.motor.Shader; // Activa la emisión para que la flecha se vea también de noche.

/**
 * INDICADOR DEL JUGADOR: flecha flotante que señala el auto desde la vista aérea.
 * Responsable de: dibujar sobre el auto una flecha cian que apunta hacia abajo, a ALTURA_INDICADOR unidades, que sube
 * y baja suavemente con el tiempo y gira despacio sobre sí misma. Es emisiva (uEmision = 1): conserva su color de día
 * y de noche, sin depender de las luces. Desde lejos el auto mide pocos píxeles; la flecha permite encontrarlo enseguida.
 * Se comunica con: Juego (la dibuja solo con la cámara aérea, no en la de seguimiento ni en el minimapa), Cubo y Shader.
 * La flecha se arma con cajas: un cuerpo vertical arriba y una punta escalonada (cada escalón más angosto) abajo.
 */
public class IndicadorJugador {

    // ==================== 1. CONSTANTES (valores ajustables) ====================
    public static final float ALTURA_INDICADOR = 6; // Altura de la punta de la flecha sobre el suelo: queda sobre el techo del auto.
    public static final float TAMANO_INDICADOR = 2.5f; // Ancho de la parte más ancha de la flecha; el resto se escala con él.
    public static final float[] COLOR_INDICADOR = {0.1f, 1, 1}; // Cian, el mismo color que marca al auto en el minimapa.
    public static final float AMPLITUD_OSCILACION = 0.6f; // Cuánto sube y baja la flecha respecto a su altura media.
    public static final float FRECUENCIA_OSCILACION = 3; // Rapidez del vaivén: multiplica el tiempo dentro del seno.
    public static final float VELOCIDAD_ROTACION = 1.2f; // Radianes por segundo que gira la flecha alrededor de su eje vertical.
    private static final int ESCALONES_PUNTA = 4; // Cantidad de cajas que forman la punta: más escalones, punta más suave.

    private final Shader shader; // Programa que recibe el interruptor de emisión.
    private final Cubo cubo; // Geometría compartida.

    /** Recibe el shader y el cubo compartidos. */
    public IndicadorJugador(Shader shader, Cubo cubo) {
        this.shader = shader; // Guarda el programa para cambiar uEmision.
        this.cubo = cubo; // Guarda la geometría compartida.
    }

    /** Dibuja la flecha sobre el punto (x, z); tiempo anima la oscilación y el giro. */
    public void dibujar(float x, float z, float tiempo) {
        float t = TAMANO_INDICADOR; // Nombre corto para las medidas proporcionales.
        float r = COLOR_INDICADOR[0]; // Componente roja del color.
        float g = COLOR_INDICADOR[1]; // Componente verde del color.
        float b = COLOR_INDICADOR[2]; // Componente azul del color.
        float punta = ALTURA_INDICADOR + (float) Math.sin(tiempo * FRECUENCIA_OSCILACION) * AMPLITUD_OSCILACION; // Altura de la punta en este instante.
        float giro = tiempo * VELOCIDAD_ROTACION; // Ángulo actual del giro alrededor de Y.
        float altoEscalon = t * 0.15f; // Alto de cada escalón de la punta.
        shader.entero("uEmision", 1); // Color propio, sin iluminación: visible también de noche.
        for (int i = 0; i < ESCALONES_PUNTA; i++) { // Arma la punta de abajo hacia arriba.
            float ancho = t * (i + 1) / ESCALONES_PUNTA; // Abajo angosto (la punta) y arriba ancho: apunta hacia abajo.
            float centroY = punta + altoEscalon * (i + 0.5f); // Cada escalón se apoya sobre el anterior.
            cubo.cajaGirada(x, centroY, z, ancho, altoEscalon, ancho, r, g, b, giro); // Escalón de la punta.
        }
        float baseCuerpo = punta + altoEscalon * ESCALONES_PUNTA; // El cuerpo empieza donde termina la punta.
        float altoCuerpo = t * 0.8f; // Alto del cuerpo vertical de la flecha.
        cubo.cajaGirada(x, baseCuerpo + altoCuerpo / 2, z, t * 0.35f, altoCuerpo, t * 0.35f, r, g, b, giro); // Cuerpo de la flecha.
        shader.entero("uEmision", 0); // Restablece el material normal para el siguiente objeto.
    }
}
