package com.graphics.ciudad.mundo; // Agrupa lo que forma la ciudad: mapa, edificios, decoración y semáforos.

import com.graphics.ciudad.motor.Cubo; // Dibuja el poste, la carcasa y las bombillas.
import com.graphics.ciudad.motor.Shader; // Activa la emisión de la bombilla encendida.

/**
 * SEMAFORO: semáforo decorativo animado.
 * Responsable de: dibujar poste, carcasa y tres bombillas, y calcular qué bombilla está encendida según el
 * reloj global del juego (ciclo de CICLO = 12 segundos, con duraciones en constantes). Los semáforos son decorativos; el ejemplo no incluye tráfico autónomo.
 * Se comunica con: Decoracion (lo coloca en cada manzana y le pasa el reloj global de Juego), Cubo (dibuja) y
 * Shader (uEmision hace que la bombilla activa se vea encendida).
 */
public class Semaforo {

    private final Shader shader; // Programa que recibe el interruptor de emisión.
    private final Cubo cubo; // Geometría con la que se construye el semáforo.

    // Duraciones del ciclo rojo → verde → amarillo, en segundos. Cambiarlas aquí ajusta todos los semáforos.
    public static final float DURACION_ROJO = 5; // Segundos con la luz roja encendida.
    public static final float DURACION_VERDE = 5; // Segundos con la luz verde encendida.
    public static final float DURACION_AMARILLO = 2; // Segundos con la luz amarilla encendida.
    public static final float CICLO = DURACION_ROJO + DURACION_VERDE + DURACION_AMARILLO; // Duración de una vuelta completa: 12 s.
    public static final float BRILLO_APAGADA = 0.15f; // Intensidad de las bombillas inactivas: quedan oscuras, solo se intuye su color.
    public static final int ROJA = 0; // Índice de la bombilla roja (arriba).
    public static final int AMARILLA = 1; // Índice de la bombilla amarilla (centro).
    public static final int VERDE = 2; // Índice de la bombilla verde (abajo).

    /** Recibe el shader y el cubo compartidos. */
    public Semaforo(Shader shader, Cubo cubo) {
        this.shader = shader; // Guarda el programa para cambiar uEmision.
        this.cubo = cubo; // Guarda la geometría compartida.
    }

    /** Devuelve qué bombilla está encendida en un instante: ROJA, luego VERDE y por último AMARILLA, en bucle. */
    public static int luzActiva(float tiempo) {
        float fase = tiempo % CICLO; // Repite un ciclo de segundos comprendidos entre 0 y CICLO.
        if (fase < DURACION_ROJO) { // Primer tramo del ciclo: selecciona la bombilla roja.
            return ROJA; // Mantiene rojo durante los primeros DURACION_ROJO segundos.
        }
        if (fase < DURACION_ROJO + DURACION_VERDE) { // Segundo tramo del ciclo: selecciona la bombilla verde.
            return VERDE; // Mantiene verde durante los DURACION_VERDE segundos intermedios.
        }
        return AMARILLA; // Último tramo: selecciona la bombilla amarilla. Mantiene amarillo en los últimos DURACION_AMARILLO segundos del ciclo.
    }

    /** Construye un semáforo decorativo que alterna rojo, verde y amarillo cada CICLO segundos (12 s). */
    public void dibujar(float x, float z, float tiempo) {
        cubo.caja(x, 1.7f, z, 0.18f, 2.8f, 0.18f, 0.18f, 0.20f, 0.22f); // Dibuja el poste sobre la acera.
        cubo.caja(x, 3.1f, z, 0.55f, 1.2f, 0.45f, 0.08f, 0.10f, 0.12f); // Dibuja la carcasa de las tres luces.
        int activa = luzActiva(tiempo); // Calcula una sola vez qué bombilla corresponde a este instante.
        for (int indice = 0; indice < 3; indice++) { // Recorre rojo arriba, amarillo al centro y verde abajo.
            boolean encendida = indice == activa; // Parte de una bombilla apagada salvo la activa. Solo la bombilla de la fase actual se enciende; las otras quedan oscuras.
            float brillo = BRILLO_APAGADA; // Conserva un color tenue cuando la bombilla está apagada.
            shader.entero("uEmision", 0); // Configura inicialmente una superficie sin emisión.
            if (encendida) { // Comprueba si esta bombilla corresponde a la fase activa.
                brillo = 1; // Usa intensidad completa para su color.
                shader.entero("uEmision", 1); // Hace que la bombilla se vea encendida.
            }
            float rojo = 0; // Componente roja inicialmente ausente.
            float verde = 0; // Componente verde inicialmente ausente.
            if (indice < 2) { // Rojo y amarillo necesitan componente roja.
                rojo = brillo; // Añade rojo con la intensidad elegida.
            }
            if (indice > 0) { // Amarillo y verde necesitan componente verde.
                verde = brillo; // Añade verde; rojo más verde produce amarillo.
            }
            float altura = 3.45f - indice * 0.35f; // Separa verticalmente las tres bombillas.
            cubo.caja(x, altura, z - 0.24f, 0.28f, 0.25f, 0.06f, rojo, verde, 0.02f); // Dibuja la bombilla frente a la carcasa.
        }
        shader.entero("uEmision", 0); // Evita que el siguiente objeto herede la emisión del semáforo.
    }
}
