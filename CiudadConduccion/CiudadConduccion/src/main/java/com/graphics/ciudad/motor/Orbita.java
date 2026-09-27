package com.graphics.ciudad.motor; // Agrupa las piezas técnicas: ventana, shaders, geometría y cámara.

/**
 * ORBITA: una cámara que gira alrededor de un punto, descripta con COORDENADAS ESFÉRICAS.
 * En lugar de guardar la posición (x, y, z) de la cámara, se guardan tres números:
 *  - θ (angulo): hacia dónde está la cámara, girando alrededor del eje vertical;
 *  - φ (elevacion): cuánto sube sobre el horizonte (0 = a ras del suelo, 90° = justo arriba);
 *  - D (distancia): qué tan lejos está del punto que mira.
 * La posición sale de (D · cos φ · sen θ, D · sen φ, D · cos φ · cos θ) sumada al centro: D · cos φ es la sombra de D
 * sobre el suelo, que se reparte entre X y Z con sen θ y cos θ, y D · sen φ es la altura.
 * Así, girar es sumar a θ, subir es sumar a φ y acercar es restar a D; cada uno se limita por separado. φ nunca llega
 * a 90°: la cámara quedaría justo encima y su eje "arriba" dejaría de estar definido.
 * La usan las dos cámaras que se manejan con el mouse (Camara): la orbital del auto (centro = el auto) y la aérea
 * (centro = un punto de la ciudad). Cada una crea su Orbita con sus propios límites.
 * No usa OpenGL, por eso se prueba sin ventana.
 */
public class Orbita {

    private final float elevacionMin; // φ mínima, en radianes.
    private final float elevacionMax; // φ máxima, en radianes (menor que 90°).
    private final float distanciaMin; // D mínima.
    private final float distanciaMax; // D máxima.
    private float angulo; // θ, en radianes.
    private float elevacion; // φ, en radianes.
    private float distancia; // D.

    /** Crea la órbita con sus límites y la coloca en (angulo, elevacion, distancia), ya limitada. */
    public Orbita(float angulo, float elevacion, float distancia,
                  float elevacionMin, float elevacionMax, float distanciaMin, float distanciaMax) {
        this.elevacionMin = elevacionMin; // Límites fijos de esta cámara.
        this.elevacionMax = elevacionMax;
        this.distanciaMin = distanciaMin;
        this.distanciaMax = distanciaMax;
        colocar(angulo, elevacion, distancia); // Posición inicial.
    }

    /** Lleva la cámara a (angulo, elevacion, distancia), respetando los límites. */
    public void colocar(float angulo, float elevacion, float distancia) {
        this.angulo = angulo; // θ no tiene límites: se puede dar la vuelta entera.
        this.elevacion = limitar(elevacion, elevacionMin, elevacionMax); // φ dentro de su rango.
        this.distancia = limitar(distancia, distanciaMin, distanciaMax); // D dentro de su rango.
    }

    /** Gira la cámara: suma dAngulo a θ y dElevacion a φ (que no sale de sus límites). */
    public void girar(float dAngulo, float dElevacion) {
        angulo += dAngulo; // Vuelta alrededor del centro.
        elevacion = limitar(elevacion + dElevacion, elevacionMin, elevacionMax); // Sube o baja, sin pasarse.
    }

    /** Acerca la cámara (cantidad positiva) o la aleja (negativa), sin salir de [distanciaMin, distanciaMax]. */
    public void acercar(float cantidad) {
        distancia = limitar(distancia - cantidad, distanciaMin, distanciaMax); // Menos distancia = más cerca.
    }

    /**
     * Posición {x, y, z} de la cámara alrededor del centro (cx, cy, cz). anguloBase se suma a θ: la orbital del auto
     * le pasa el ángulo del auto, así la cámara lo acompaña cuando dobla; la aérea pasa 0.
     */
    public float[] ojo(float cx, float cy, float cz, float anguloBase) {
        float total = anguloBase + angulo; // Ángulo horizontal total.
        float horizontal = distancia * (float) Math.cos(elevacion); // D · cos φ: la parte de D sobre el suelo.
        return new float[] {
            cx + (float) Math.sin(total) * horizontal, // X = D · cos φ · sen θ.
            cy + distancia * (float) Math.sin(elevacion), // Y = D · sen φ.
            cz + (float) Math.cos(total) * horizontal // Z = D · cos φ · cos θ.
        };
    }

    /** θ actual, en radianes. */
    public float getAngulo() {
        return angulo;
    }

    /** φ actual, en radianes. */
    public float getElevacion() {
        return elevacion;
    }

    /** D actual. */
    public float getDistancia() {
        return distancia;
    }

    /** Recorta un valor al intervalo [minimo, maximo]. */
    private static float limitar(float valor, float minimo, float maximo) {
        return Math.max(minimo, Math.min(maximo, valor)); // Ni menos que el mínimo ni más que el máximo.
    }
}
