package com.graphics.ciudad.motor; // Agrupa las piezas técnicas: ventana, shaders, geometría y cámara.

import static org.lwjgl.glfw.GLFW.*; // Permite consultar las flechas que orbitan la cámara.

/**
 * CAMARA: decide desde dónde se observa la ciudad.
 * Responsable de tres modos, que la tecla C recorre en orden: SEGUIMIENTO (detrás del auto) → ORBITAL (alrededor del
 * auto, manejada con el mouse) → AEREA (vista general de la ciudad, también manejada con el mouse).
 * Las dos cámaras que se manejan con el mouse comparten la misma matemática, la clase Orbita (coordenadas esféricas
 * θ, φ y D alrededor de un centro); solo cambian el centro y los límites:
 *  - ORBITAL DEL AUTO: el centro es el auto y θ se suma a su ángulo, así la cámara lo acompaña cuando dobla.
 *    φ entre ELEVACION_MIN y ELEVACION_MAX, D entre DISTANCIA_MIN y DISTANCIA_MAX. Se puede seguir manejando.
 *  - AÉREA: el centro es un punto de la ciudad (al entrar, el origen). φ entre ELEVACION_AEREA_MIN y
 *    ELEVACION_AEREA_MAX, D entre DISTANCIA_AEREA_MIN y DISTANCIA_AEREA_MAX (con la máxima se ve toda la ciudad).
 *    Además, arrastrar con el botón derecho desplaza el centro, que nunca sale de ±límite de la ciudad.
 *    Al entrar al modo la cámara vuelve a la vista general de siempre (la de las capturas).
 * Mouse: arrastrar con el botón izquierdo = girar y elevar; ruedita = acercar y alejar; botón derecho = desplazar
 * (solo en la aérea). En la cámara de seguimiento el mouse no hace nada.
 * Se comunica con: Shader, al que envía uOjo, uObjetivo y uAspecto; Juego, que le pasa la posición y el
 * ángulo del Auto y el tamaño de la Ventana en cada cuadro; Ventana, que le entrega los movimientos del mouse.
 */
public class Camara {

    /** Modos de cámara, en el orden en que los recorre la tecla C. */
    public enum Modo {
        SEGUIMIENTO, // Detrás del auto, a distancia y altura fijas.
        ORBITAL, // Alrededor del auto, controlada con el mouse.
        AEREA // Vista general de toda la ciudad, controlada con el mouse.
    }

    // ==================== MOUSE (valores ajustables, compartidos por la orbital y la aérea) ====================
    public static final float SENSIBILIDAD_GIRO = 0.008f; // Radianes de θ por píxel arrastrado en horizontal.
    public static final float SENSIBILIDAD_ELEVACION = 0.006f; // Radianes de φ por píxel arrastrado en vertical.

    // ==================== CÁMARA ORBITAL DEL AUTO (valores ajustables) ====================
    public static final float ANGULO_INICIAL = 0; // θ al entrar al modo: 0 = justo detrás del auto.
    public static final float ELEVACION_INICIAL = (float) Math.toRadians(25); // φ al entrar al modo.
    public static final float DISTANCIA_INICIAL = 9; // D al entrar al modo.
    public static final float ELEVACION_MIN = (float) Math.toRadians(5); // Casi a ras del suelo, sin atravesarlo.
    public static final float ELEVACION_MAX = (float) Math.toRadians(85); // Casi cenital, sin llegar a 90°.
    public static final float DISTANCIA_MIN = 4; // Lo más cerca del auto (su largo es 2.6).
    public static final float DISTANCIA_MAX = 30; // Lo más lejos.
    public static final float PASO_ZOOM = 1; // Unidades de distancia por cada paso de la ruedita.

    // ==================== CÁMARA AÉREA (valores ajustables) ====================
    // Vista inicial: la de siempre (la de las capturas), con la cámara a FACTOR_RADIO límites del centro sobre el suelo
    // y FACTOR_ALTURA límites de altura, en el ángulo ANGULO_AEREO_INICIAL. En esféricas: φ = atan(altura / radio) ≈ 40°
    // y D = √(radio² + altura²) ≈ 2.43 límites (≈ 134 con límite 55).
    public static final float ANGULO_AEREO_INICIAL = 0.6f; // θ inicial alrededor de la ciudad, en radianes.
    private static final float FACTOR_RADIO = 1.86f; // Radio de la vista inicial en "límites": con la ciudad original (límite 35) daba 65.
    private static final float FACTOR_ALTURA = 1.57f; // Altura de la vista inicial en "límites": con la ciudad original daba 55.
    public static final float ELEVACION_AEREA_MIN = (float) Math.toRadians(20); // Vista oblicua: se ven las fachadas.
    public static final float ELEVACION_AEREA_MAX = (float) Math.toRadians(85); // Casi un plano visto desde arriba.
    public static final float DISTANCIA_AEREA_MIN = 20; // Lo más cerca: una o dos manzanas.
    public static final float FACTOR_DISTANCIA_AEREA_MAX = 2.6f; // DISTANCIA_AEREA_MAX = 2.6 límites (143 con límite 55): toda la ciudad, un poco más lejos que la vista inicial.
    public static final float PASO_ZOOM_AEREO = 6; // Unidades de distancia por paso de ruedita: la ciudad es grande.
    public static final float SENSIBILIDAD_DESPLAZAMIENTO = 0.0015f; // Desplazamiento por píxel, por unidad de distancia (lejos = más rápido).

    // ==================== CÁMARA DE SEGUIMIENTO ====================
    private static final float DISTANCIA_SEGUIMIENTO = 12; // Distancia horizontal de la cámara de seguimiento detrás del auto.
    private static final float ALTURA_SEGUIMIENTO = 9; // Altura de la cámara de seguimiento sobre el suelo.
    private static final float ALTURA_OBJETIVO = 0.8f; // Altura del punto del auto al que mira la cámara (la carrocería).

    // ==================== ESTADO ====================
    private Modo modo = Modo.SEGUIMIENTO; // El juego arranca con la cámara de seguimiento.
    private final Orbita orbitaAuto = new Orbita(ANGULO_INICIAL, ELEVACION_INICIAL, DISTANCIA_INICIAL,
        ELEVACION_MIN, ELEVACION_MAX, DISTANCIA_MIN, DISTANCIA_MAX); // θ, φ y D alrededor del auto.
    private final Orbita orbitaAerea; // θ, φ y D alrededor del centro aéreo.
    private final float limite; // Distancia del origen a cada borde de la ciudad (Mapa.LIMITE).
    private final float elevacionAereaInicial; // φ de la vista inicial.
    private final float distanciaAereaInicial; // D de la vista inicial.
    private float centroX = 0; // Punto de la ciudad que mira la cámara aérea, en X.
    private float centroZ = 0; // Y en Z.

    /** Recibe la distancia del centro a cada borde (Mapa.LIMITE) y ajusta la vista aérea a ese tamaño. */
    public Camara(float limiteCiudad) {
        limite = limiteCiudad; // El centro aéreo nunca sale de ±limite.
        elevacionAereaInicial = (float) Math.atan2(FACTOR_ALTURA, FACTOR_RADIO); // ≈ 40°.
        distanciaAereaInicial = limiteCiudad * (float) Math.hypot(FACTOR_RADIO, FACTOR_ALTURA); // ≈ 134 con límite 55.
        orbitaAerea = new Orbita(ANGULO_AEREO_INICIAL, elevacionAereaInicial, distanciaAereaInicial,
            ELEVACION_AEREA_MIN, ELEVACION_AEREA_MAX, DISTANCIA_AEREA_MIN, getDistanciaAereaMax()); // Vista general.
    }

    /** Indica si está activa la vista aérea de la ciudad; Juego dibuja entonces la flecha sobre el auto. */
    public boolean esAerea() {
        return modo == Modo.AEREA; // La flecha no hace falta en seguimiento ni en la orbital del auto.
    }

    /** Pasa al siguiente modo: seguimiento → orbital del auto → aérea → seguimiento; Juego lo llama al presionar C. */
    public void alternar() {
        modo = Modo.values()[(modo.ordinal() + 1) % Modo.values().length]; // Avanza en el orden del enum y vuelve al inicio.
        if (modo == Modo.AEREA) { // Al entrar a la aérea...
            reiniciarAerea(); // ...arranca con la vista general de siempre.
        }
    }

    /** Vuelve la cámara aérea a la vista inicial: centro en el origen, ángulo, elevación y distancia de las capturas. */
    private void reiniciarAerea() {
        centroX = 0; // Centro de la ciudad.
        centroZ = 0;
        orbitaAerea.colocar(ANGULO_AEREO_INICIAL, elevacionAereaInicial, distanciaAereaInicial); // Vista general.
    }

    /** Modo actual. */
    public Modo getModo() {
        return modo; // SEGUIMIENTO, ORBITAL o AEREA.
    }

    /** Nombre del modo para el HUD. */
    public String nombreModo() {
        switch (modo) { // Un texto por modo.
            case ORBITAL: return "Orbital (mouse)"; // Arrastrar y ruedita.
            case AEREA: return "Aérea"; // Vista general (también con mouse).
            default: return "Seguimiento"; // Detrás del auto.
        }
    }

    /** Órbita que maneja el mouse en el modo actual, o null en seguimiento (donde el mouse no hace nada). */
    private Orbita orbitaActiva() {
        switch (modo) {
            case ORBITAL: return orbitaAuto; // Alrededor del auto.
            case AEREA: return orbitaAerea; // Alrededor del centro aéreo.
            default: return null; // Seguimiento.
        }
    }

    /**
     * Arrastre del mouse con el botón izquierdo, en píxeles (dx a la derecha, dy hacia abajo). En la orbital y en la
     * aérea, mover a la derecha gira la cámara alrededor de su centro y mover hacia arriba la eleva.
     */
    public void arrastrar(double dx, double dy) {
        Orbita orbita = orbitaActiva(); // La cámara que corresponde.
        if (orbita == null) { // En seguimiento el mouse no mueve la cámara.
            return; // Ignora el arrastre.
        }
        // dy es negativo hacia arriba: subir el mouse eleva la cámara. Los límites de φ los aplica la Orbita.
        orbita.girar(-(float) dx * SENSIBILIDAD_GIRO, -(float) dy * SENSIBILIDAD_ELEVACION);
    }

    /** Ruedita del mouse: pasos positivos (hacia adelante) acercan la cámara; en la orbital y en la aérea. */
    public void zoom(double pasos) {
        if (modo == Modo.ORBITAL) { // Alrededor del auto: pasos cortos.
            orbitaAuto.acercar((float) pasos * PASO_ZOOM);
        } else if (modo == Modo.AEREA) { // Sobre la ciudad: pasos largos.
            orbitaAerea.acercar((float) pasos * PASO_ZOOM_AEREO);
        }
    }

    /**
     * Arrastre con el botón derecho, en píxeles: solo en la aérea, desplaza el punto que mira la cámara como si se
     * arrastrara el suelo con la mano (el suelo sigue al cursor). Se mueve en los ejes de la pantalla proyectados al
     * suelo: la derecha de la cámara es (cos θ, -sen θ) y su frente, (-sen θ, -cos θ). Cuanto más lejos está la
     * cámara, más avanza por píxel. El centro nunca sale de ±límite: la vista no se va al vacío.
     */
    public void desplazar(double dx, double dy) {
        if (modo != Modo.AEREA) { // En los otros modos el botón derecho no hace nada.
            return;
        }
        float theta = orbitaAerea.getAngulo(); // Hacia dónde mira la cámara.
        float paso = SENSIBILIDAD_DESPLAZAMIENTO * orbitaAerea.getDistancia(); // Unidades del mundo por píxel.
        float derecha = -(float) dx * paso; // Arrastrar a la derecha lleva el centro a la izquierda.
        float adelante = (float) dy * paso; // Arrastrar hacia abajo acerca lo que estaba más lejos.
        centroX += (float) Math.cos(theta) * derecha - (float) Math.sin(theta) * adelante; // Derecha y frente, en X.
        centroZ += -(float) Math.sin(theta) * derecha - (float) Math.cos(theta) * adelante; // Derecha y frente, en Z.
        centroX = Math.max(-limite, Math.min(limite, centroX)); // Dentro de la ciudad.
        centroZ = Math.max(-limite, Math.min(limite, centroZ));
    }

    /** Elevación actual de la cámara orbital del auto, en radianes. */
    public float getElevacion() {
        return orbitaAuto.getElevacion(); // Entre ELEVACION_MIN y ELEVACION_MAX.
    }

    /** Distancia actual de la cámara orbital al auto. */
    public float getDistancia() {
        return orbitaAuto.getDistancia(); // Entre DISTANCIA_MIN y DISTANCIA_MAX.
    }

    /** Elevación actual de la cámara aérea, en radianes. */
    public float getElevacionAerea() {
        return orbitaAerea.getElevacion(); // Entre ELEVACION_AEREA_MIN y ELEVACION_AEREA_MAX.
    }

    /** Distancia actual de la cámara aérea a su centro. */
    public float getDistanciaAerea() {
        return orbitaAerea.getDistancia(); // Entre DISTANCIA_AEREA_MIN y getDistanciaAereaMax().
    }

    /** Distancia inicial de la cámara aérea (la vista de las capturas, que muestra toda la ciudad). */
    public float getDistanciaAereaInicial() {
        return distanciaAereaInicial;
    }

    /** DISTANCIA_AEREA_MAX: depende del tamaño de la ciudad (FACTOR_DISTANCIA_AEREA_MAX · límite). */
    public float getDistanciaAereaMax() {
        return FACTOR_DISTANCIA_AEREA_MAX * limite; // 143 con límite 55.
    }

    /** Centro de la cámara aérea en X. */
    public float getCentroX() {
        return centroX; // Entre -límite y límite.
    }

    /** Centro de la cámara aérea en Z. */
    public float getCentroZ() {
        return centroZ; // Entre -límite y límite.
    }

    /** Posición {x, y, z} de la cámara aérea (la misma que configurar() envía al shader); para las pruebas. */
    float[] ojoAereo() {
        return orbitaAerea.ojo(centroX, 0, centroZ, 0); // Órbita alrededor del centro, sobre el suelo.
    }

    /**
     * Mueve la cámara alrededor de la ciudad con las flechas horizontales.
     * Era el control de la primera lección, cuando solo existía la ciudad. En el juego completo las flechas
     * conducen el auto, así que Juego no lo llama: la vista aérea se maneja con el mouse.
     */
    public void orbitar(Ventana ventana, float deltaTime) {
        if (ventana.pulsada(GLFW_KEY_LEFT)) { // Comprueba la flecha izquierda.
            orbitaAerea.girar(-deltaTime, 0); // Reduce el ángulo a razón de un radián por segundo.
        }
        if (ventana.pulsada(GLFW_KEY_RIGHT)) { // Comprueba la flecha derecha.
            orbitaAerea.girar(deltaTime, 0); // Aumenta el ángulo a la misma velocidad.
        }
    }

    /** Elige entre la vista aérea, la orbital alrededor del auto y la cámara situada detrás del auto. */
    public void configurar(Shader shader, float autoX, float autoZ, float angulo, int ancho, int alto) {
        if (modo == Modo.AEREA) { // Vista general: órbita alrededor del centro aéreo, sobre el suelo.
            enviar(shader, orbitaAerea.ojo(centroX, 0, centroZ, 0), centroX, 0, centroZ, ancho, alto);
            return; // Evita reemplazarla con la cámara de seguimiento.
        }
        if (modo == Modo.ORBITAL) { // Órbita alrededor de la carrocería; θ relativo al ángulo del auto.
            enviar(shader, orbitaAuto.ojo(autoX, ALTURA_OBJETIVO, autoZ, angulo), autoX, ALTURA_OBJETIVO, autoZ, ancho, alto);
            return; // No sigue con la cámara de seguimiento.
        }
        float camaraX = autoX + (float) Math.sin(angulo) * DISTANCIA_SEGUIMIENTO; // Coloca la cámara 12 unidades detrás en X.
        float camaraZ = autoZ + (float) Math.cos(angulo) * DISTANCIA_SEGUIMIENTO; // Coloca la cámara 12 unidades detrás en Z.
        enviar(shader, new float[] {camaraX, ALTURA_SEGUIMIENTO, camaraZ}, autoX, ALTURA_OBJETIVO, autoZ, ancho, alto); // A 9 de altura, mirando la carrocería.
    }

    /** Envía al shader la posición de la cámara (ojo), el punto que mira y la proporción de la ventana. */
    private static void enviar(Shader shader, float[] ojo, float objetivoX, float objetivoY, float objetivoZ, int ancho, int alto) {
        shader.vector("uOjo", ojo[0], ojo[1], ojo[2]); // Posición de la cámara.
        shader.vector("uObjetivo", objetivoX, objetivoY, objetivoZ); // Punto al que mira.
        shader.decimal("uAspecto", (float) ancho / alto); // Mantiene las proporciones al redimensionar la ventana.
    }
}
