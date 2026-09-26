package com.graphics.ciudad.motor; // Agrupa las piezas técnicas: ventana, shaders, geometría y cámara.

import static org.lwjgl.glfw.GLFW.*; // Permite consultar las flechas que orbitan la cámara.

/**
 * CAMARA: decide desde dónde se observa la ciudad.
 * Responsable de tres modos, que la tecla C recorre en orden: SEGUIMIENTO (detrás del auto) → ORBITAL (alrededor del
 * auto, manejada con el mouse) → AEREA (vista general de la ciudad, que reutiliza la órbita alrededor del centro).
 * CÁMARA ORBITAL DEL AUTO: usa coordenadas esféricas centradas en el auto. Un punto a distancia D, con ángulo
 * horizontal θ y elevación φ sobre el horizonte, está en (D · cos φ · sen θ, D · sen φ, D · cos φ · cos θ) respecto del
 * centro. θ se suma al ángulo del auto, así la cámara lo acompaña cuando dobla; φ se limita entre ELEVACION_MIN y
 * ELEVACION_MAX (nunca vertical: a 90° el eje "arriba" de la cámara quedaría indefinido) y D entre DISTANCIA_MIN y
 * DISTANCIA_MAX. Arrastrar con el botón izquierdo cambia θ y φ; la ruedita cambia D. Se puede seguir manejando.
 * Se comunica con: Shader, al que envía uOjo, uObjetivo y uAspecto; Juego, que le pasa la posición y el
 * ángulo del Auto y el tamaño de la Ventana en cada cuadro; Ventana, que le entrega los movimientos del mouse.
 */
public class Camara {

    /** Modos de cámara, en el orden en que los recorre la tecla C. */
    public enum Modo {
        SEGUIMIENTO, // Detrás del auto, a distancia y altura fijas.
        ORBITAL, // Alrededor del auto, controlada con el mouse.
        AEREA // Vista general de toda la ciudad.
    }

    // ==================== CÁMARA ORBITAL DEL AUTO (valores ajustables) ====================
    public static final float ANGULO_INICIAL = 0; // θ al entrar al modo: 0 = justo detrás del auto.
    public static final float ELEVACION_INICIAL = (float) Math.toRadians(25); // φ al entrar al modo.
    public static final float DISTANCIA_INICIAL = 9; // D al entrar al modo.
    public static final float ELEVACION_MIN = (float) Math.toRadians(5); // Casi a ras del suelo, sin atravesarlo.
    public static final float ELEVACION_MAX = (float) Math.toRadians(85); // Casi cenital, sin llegar a 90°.
    public static final float DISTANCIA_MIN = 4; // Lo más cerca del auto (su largo es 2.6).
    public static final float DISTANCIA_MAX = 30; // Lo más lejos.
    public static final float SENSIBILIDAD_GIRO = 0.008f; // Radianes de θ por píxel arrastrado en horizontal.
    public static final float SENSIBILIDAD_ELEVACION = 0.006f; // Radianes de φ por píxel arrastrado en vertical.
    public static final float PASO_ZOOM = 1; // Unidades de distancia por cada paso de la ruedita.

    // ==================== TECLADO Y CÁMARA ====================
    private float orbita = 0.6f; // Ángulo inicial de la cámara alrededor de la ciudad, en radianes.
    private Modo modo = Modo.SEGUIMIENTO; // El juego arranca con la cámara de seguimiento.
    private float anguloOrbital = ANGULO_INICIAL; // θ: ángulo horizontal alrededor del auto, relativo a su orientación.
    private float elevacion = ELEVACION_INICIAL; // φ: ángulo sobre el horizonte.
    private float distancia = DISTANCIA_INICIAL; // D: distancia al auto.
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

    /** Indica si está activa la vista aérea de la ciudad; Juego dibuja entonces la flecha sobre el auto. */
    public boolean esAerea() {
        return modo == Modo.AEREA; // La flecha no hace falta en seguimiento ni en la orbital del auto.
    }

    /** Pasa al siguiente modo: seguimiento → orbital del auto → aérea → seguimiento; Juego lo llama al presionar C. */
    public void alternar() {
        modo = Modo.values()[(modo.ordinal() + 1) % Modo.values().length]; // Avanza en el orden del enum y vuelve al inicio.
    }

    /** Modo actual. */
    public Modo getModo() {
        return modo; // SEGUIMIENTO, ORBITAL o AEREA.
    }

    /** Nombre del modo para el HUD. */
    public String nombreModo() {
        switch (modo) { // Un texto por modo.
            case ORBITAL: return "Orbital (mouse)"; // Arrastrar y ruedita.
            case AEREA: return "Aérea"; // Vista general.
            default: return "Seguimiento"; // Detrás del auto.
        }
    }

    /**
     * Arrastre del mouse con el botón izquierdo, en píxeles (dx a la derecha, dy hacia abajo). Solo actúa en el modo
     * orbital: mover a la derecha gira la cámara alrededor del auto y mover hacia arriba la eleva.
     */
    public void arrastrar(double dx, double dy) {
        if (modo != Modo.ORBITAL) { // En los otros modos el mouse no mueve la cámara.
            return; // Ignora el arrastre.
        }
        anguloOrbital -= (float) dx * SENSIBILIDAD_GIRO; // Arrastrar a la derecha rota la vista hacia la derecha.
        elevacion -= (float) dy * SENSIBILIDAD_ELEVACION; // dy es negativo hacia arriba: subir el mouse eleva la cámara.
        elevacion = Math.max(ELEVACION_MIN, Math.min(ELEVACION_MAX, elevacion)); // Nunca por debajo de 5° ni por encima de 85°.
    }

    /** Ruedita del mouse: pasos positivos (hacia adelante) acercan la cámara; solo en el modo orbital. */
    public void zoom(double pasos) {
        if (modo != Modo.ORBITAL) { // La ruedita no afecta a los otros modos.
            return; // Ignora el zoom.
        }
        distancia -= (float) pasos * PASO_ZOOM; // Acerca o aleja.
        distancia = Math.max(DISTANCIA_MIN, Math.min(DISTANCIA_MAX, distancia)); // Entre 4 y 30.
    }

    /** Elevación actual de la cámara orbital, en radianes. */
    public float getElevacion() {
        return elevacion; // Entre ELEVACION_MIN y ELEVACION_MAX.
    }

    /** Distancia actual de la cámara orbital al auto. */
    public float getDistancia() {
        return distancia; // Entre DISTANCIA_MIN y DISTANCIA_MAX.
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

    /** Elige entre una vista general, la orbital alrededor del auto y una cámara situada detrás del auto. */
    public void configurar(Shader shader, float autoX, float autoZ, float angulo, int ancho, int alto) {
        if (modo == Modo.AEREA) { // Comprueba si está activa la vista general.
            configurarOrbital(shader, ancho, alto); // Reutiliza la cámara oblicua de la primera lección.
            return; // Evita reemplazarla con la cámara de seguimiento.
        }
        if (modo == Modo.ORBITAL) { // Cámara esférica alrededor del auto.
            configurarOrbitalAuto(shader, autoX, autoZ, angulo, ancho, alto); // Usa θ, φ y D.
            return; // No sigue con la cámara de seguimiento.
        }

        float camaraX = autoX + (float) Math.sin(angulo) * DISTANCIA_SEGUIMIENTO; // Coloca la cámara 12 unidades detrás en X.
        float camaraZ = autoZ + (float) Math.cos(angulo) * DISTANCIA_SEGUIMIENTO; // Coloca la cámara 12 unidades detrás en Z.
        shader.vector("uOjo", camaraX, ALTURA_SEGUIMIENTO, camaraZ); // Envía la posición de la cámara, a 9 unidades de altura.
        shader.vector("uObjetivo", autoX, ALTURA_OBJETIVO, autoZ); // Orienta la cámara hacia la carrocería.
        shader.decimal("uAspecto", (float) ancho / alto); // Mantiene las proporciones al redimensionar la ventana.
    }

    /**
     * Cámara orbital del auto: convierte (θ, φ, D) en una posición. El ángulo horizontal total es el del auto más θ:
     * con θ = 0 la cámara queda detrás, en la dirección (sen(ángulo), cos(ángulo)), opuesta al frente del auto.
     */
    private void configurarOrbitalAuto(Shader shader, float autoX, float autoZ, float angulo, int ancho, int alto) {
        float total = angulo + anguloOrbital; // Relativo al auto: si el auto dobla, la cámara lo acompaña.
        float horizontal = distancia * (float) Math.cos(elevacion); // Proyección de D sobre el suelo: D · cos φ.
        float camaraX = autoX + (float) Math.sin(total) * horizontal; // X = D · cos φ · sen θ.
        float camaraZ = autoZ + (float) Math.cos(total) * horizontal; // Z = D · cos φ · cos θ.
        float camaraY = ALTURA_OBJETIVO + distancia * (float) Math.sin(elevacion); // Y = D · sen φ, sobre la carrocería.
        shader.vector("uOjo", camaraX, camaraY, camaraZ); // Posición de la cámara.
        shader.vector("uObjetivo", autoX, ALTURA_OBJETIVO, autoZ); // Siempre mira al auto.
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
