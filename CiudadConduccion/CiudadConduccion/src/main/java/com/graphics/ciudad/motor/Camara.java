package com.graphics.ciudad.motor; // Agrupa las piezas técnicas: ventana, shaders, geometría y cámara.

import static org.lwjgl.glfw.GLFW.*; // Permite consultar las flechas que orbitan la cámara.

/**
 * CAMARA: decide desde dónde se observa la ciudad.
 * Responsable de: la cámara orbital (elevada, girando alrededor del centro), la cámara de seguimiento
 * (detrás del auto) y el modo aéreo, que reutiliza la vista orbital; alterna modos con la tecla C.
 * Se comunica con: Shader, al que envía uOjo, uObjetivo y uAspecto; Juego, que le pasa la posición y el
 * ángulo del Auto y el tamaño de la Ventana en cada cuadro.
 */
public class Camara {

    // ==================== TECLADO Y CÁMARA ====================
    private float orbita = 0.6f; // Ángulo inicial de la cámara alrededor de la ciudad, en radianes.
    private boolean camaraAerea = false; // false: seguir el auto; true: observar toda la ciudad.
    private static final float DISTANCIA_SEGUIMIENTO = 12; // Distancia horizontal de la cámara de seguimiento detrás del auto.
    private static final float ALTURA_SEGUIMIENTO = 9; // Altura de la cámara de seguimiento sobre el suelo.
    private static final float ALTURA_OBJETIVO = 0.8f; // Altura del punto del auto al que mira la cámara (la carrocería).
    private static final float FACTOR_RADIO = 1.86f; // Radio de la órbita en "límites": con la ciudad original (límite 35) daba 65.
    private static final float FACTOR_ALTURA = 1.57f; // Altura de la órbita en "límites": con la ciudad original daba 55.
    private final float radioOrbita; // Distancia horizontal de la cámara aérea al centro; crece con la ciudad.
    private final float alturaOrbita; // Altura de la cámara aérea; crece con la ciudad para verla completa.

    /** Recibe la distancia del centro a cada borde (Mapa.LIMITE) y ajusta la vista aérea a ese tamaño. */
    public Camara(float limiteCiudad) {
        radioOrbita = limiteCiudad * FACTOR_RADIO; // Con límite 55 la órbita tiene unas 102 unidades de radio.
        alturaOrbita = limiteCiudad * FACTOR_ALTURA; // Con límite 55 la cámara queda a unas 86 unidades de altura.
    }

    /** Invierte el modo de cámara; Juego lo llama al presionar C. */
    public void alternar() {
        camaraAerea = !camaraAerea; // Invierte el modo de cámara actual.
    }

    /**
     * Mueve la cámara alrededor de la ciudad con las flechas horizontales.
     * Era el control de la primera lección, cuando solo existía la ciudad. En el juego completo las flechas
     * conducen el auto, así que Juego no lo llama y la vista aérea conserva el ángulo inicial.
     */
    public void orbitar(Ventana ventana, float deltaTime) {
        if (ventana.pulsada(GLFW_KEY_LEFT)) { // Comprueba la flecha izquierda.
            orbita -= deltaTime; // Reduce el ángulo a razón de un radián por segundo.
        }
        if (ventana.pulsada(GLFW_KEY_RIGHT)) { // Comprueba la flecha derecha.
            orbita += deltaTime; // Aumenta el ángulo a la misma velocidad.
        }
    }

    /** Elige entre una vista general y una cámara situada detrás del auto. */
    public void configurar(Shader shader, float autoX, float autoZ, float angulo, int ancho, int alto) {
        if (camaraAerea) { // Comprueba si está activa la vista general.
            configurarOrbital(shader, ancho, alto); // Reutiliza la cámara oblicua de la primera lección.
            return; // Evita reemplazarla con la cámara de seguimiento.
        }

        float camaraX = autoX + (float) Math.sin(angulo) * DISTANCIA_SEGUIMIENTO; // Coloca la cámara 12 unidades detrás en X.
        float camaraZ = autoZ + (float) Math.cos(angulo) * DISTANCIA_SEGUIMIENTO; // Coloca la cámara 12 unidades detrás en Z.
        shader.vector("uOjo", camaraX, ALTURA_SEGUIMIENTO, camaraZ); // Envía la posición de la cámara, a 9 unidades de altura.
        shader.vector("uObjetivo", autoX, ALTURA_OBJETIVO, autoZ); // Orienta la cámara hacia la carrocería.
        shader.decimal("uAspecto", (float) ancho / alto); // Mantiene las proporciones al redimensionar la ventana.
    }

    /** Coloca la cámara elevada y orientada hacia el centro de la ciudad. */
    public void configurarOrbital(Shader shader, int ancho, int alto) {
        float camaraX = (float) Math.sin(orbita) * radioOrbita; // Calcula la posición X de una órbita de radio radioOrbita.
        float camaraZ = (float) Math.cos(orbita) * radioOrbita; // Calcula la posición Z de esa misma órbita.
        shader.vector("uOjo", camaraX, alturaOrbita, camaraZ); // Envía la posición de la cámara a alturaOrbita unidades de altura.
        shader.vector("uObjetivo", 0, 0, 0); // Apunta la cámara hacia el origen del mundo.
        shader.decimal("uAspecto", (float) ancho / alto); // Envía la proporción de la imagen para evitar deformaciones.
    }
}
