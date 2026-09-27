package com.graphics.ciudad.iluminacion; // Agrupa el estado de las luces de la escena.

import com.graphics.ciudad.motor.Cubo; // Estrellas: cajas pequeñas y emisivas.
import com.graphics.ciudad.motor.Figuras; // Esfera: cúpula del cielo y luna.
import com.graphics.ciudad.motor.Shader; // Modo cielo (uCielo), colores del degradado y emisión.
import com.graphics.ciudad.mundo.Variacion; // Hash determinístico para ubicar las estrellas.
import static org.lwjgl.opengl.GL33.*; // glDepthMask.

/**
 * CIELO: fondo con degradado vertical, y de noche estrellas y luna.
 * Responsable de: dibujar una CÚPULA (la esfera de Figuras, muy grande y centrada en la cámara) cuyo color calcula
 * iluminacion.frag según hacia dónde se mira: COLOR_CENIT hacia arriba y COLOR_HORIZONTE a la altura del horizonte.
 * De noche agrega CANTIDAD_ESTRELLAS estrellas (posiciones fijas, sin Random) y una luna (esfera emisiva).
 *
 * POR QUÉ UNA CÚPULA CENTRADA EN LA CÁMARA: el cielo está "infinitamente lejos": al mover la cámara no debería
 * acercarse ni alejarse. Si la esfera se mueve con el ojo, cada dirección de la vista siempre ve el mismo color.
 *
 * POR QUÉ SE DIBUJA PRIMERO Y SIN ESCRIBIR PROFUNDIDAD (glDepthMask(false)):
 *  - El depth buffer guarda, por píxel, la distancia de lo más cercano dibujado hasta ahora; un fragmento nuevo solo
 *    se pinta si está más cerca. El cielo no es un objeto real: es el fondo, y todo lo demás debe verse delante.
 *  - Si la cúpula escribiera su distancia (RADIO_CIELO), cualquier cosa más lejana que ella (por ejemplo el final del
 *    campo visto desde la cámara aérea) fallaría la prueba de profundidad y quedaría tapada por el cielo.
 *  - Sin escribir profundidad, el buffer sigue "vacío" (el valor máximo que dejó glClear) después del cielo, así que
 *    la ciudad que se dibuja después pasa siempre la prueba y lo pinta encima: el cielo queda detrás de todo, sea
 *    cual sea su radio. Por eso también tiene que ir PRIMERO: lo que se dibuja después lo cubre.
 *  - Estrellas y luna siguen la misma regla: se pintan sobre la cúpula en el orden en que se dibujan.
 * No se dibuja en el minimapa (vista desde arriba: no hay cielo).
 * Se comunica con: Juego (lo dibuja al empezar cada cuadro, con la posición de la cámara y el estado de noche de
 * Iluminacion), Shader y el shader iluminacion.frag (rama uCielo), Figuras, Cubo y Variacion.
 */
public class Cielo {

    // ==================== 1. CONSTANTES (valores ajustables) ====================
    // Radio de la cúpula: debe quedar dentro de PLANO_LEJANO de ciudad.vert (320) o el recorte lejano la cortaría.
    public static final float RADIO_CIELO = 250;
    public static final float[] COLOR_CENIT_DIA = {0.16f, 0.38f, 0.82f}; // Día, mirando hacia arriba: azul intenso.
    public static final float[] COLOR_HORIZONTE_DIA = {0.72f, 0.86f, 0.97f}; // Día, en el horizonte: celeste claro.
    public static final float[] COLOR_CENIT_NOCHE = {0.01f, 0.015f, 0.05f}; // Noche, arriba: azul casi negro.
    public static final float[] COLOR_HORIZONTE_NOCHE = {0.06f, 0.09f, 0.22f}; // Noche, en el horizonte: azul noche.
    // Estrellas: pequeñas cajas emisivas repartidas sobre el cielo, siempre en el mismo lugar.
    public static final int CANTIDAD_ESTRELLAS = 160; // Cantidad de estrellas.
    public static final int SEMILLA_ESTRELLAS = 97; // Cambiarla reparte otro cielo (siempre el mismo para cada semilla).
    public static final float ALTURA_MIN_ESTRELLA = 0.08f; // Componente Y mínima de su dirección (≈ 5°): ninguna bajo el horizonte.
    public static final float DISTANCIA_ESTRELLAS = 0.9f * RADIO_CIELO; // Un poco adentro de la cúpula.
    public static final float TAMANO_MIN_ESTRELLA = 0.5f; // Lado de la estrella más chica.
    public static final float TAMANO_MAX_ESTRELLA = 1.3f; // Lado de la más grande.
    public static final float BRILLO_MIN_ESTRELLA = 0.55f; // Las más tenues (el brillo multiplica el blanco).
    // Luna: esfera emisiva, fija en el cielo.
    public static final float[] DIRECCION_LUNA = {0.45f, 0.32f, -0.83f}; // Hacia el norte-noreste, ≈ 19° sobre el horizonte (se normaliza).
    public static final float DISTANCIA_LUNA = 0.8f * RADIO_CIELO; // Delante de las estrellas.
    public static final float DIAMETRO_LUNA = 11; // ≈ 3° vista desde la cámara: más grande que la real, para que se note.
    public static final float[] COLOR_LUNA = {0.95f, 0.93f, 0.82f}; // Blanco amarillento.

    // Cada estrella es {dirX, dirY, dirZ, tamaño, brillo}; la dirección es unitaria y se escala al dibujar.
    public static final float[][] ESTRELLAS = calcularEstrellas();

    // ==================== 2. ESTRELLAS: POSICIONES DETERMINÍSTICAS ====================

    /**
     * Reparte las estrellas sobre la media esfera de arriba. Elegir la altura Y uniforme (y no el ángulo de elevación)
     * reparte los puntos de forma pareja sobre la superficie de la esfera: si se sorteara el ángulo, se amontonarían
     * cerca del cénit. Con la altura y el ángulo horizontal φ, la dirección es (r cos φ, y, r sen φ), con r = √(1 - y²).
     */
    private static float[][] calcularEstrellas() {
        float[][] estrellas = new float[CANTIDAD_ESTRELLAS][]; // Resultado.
        for (int i = 0; i < CANTIDAD_ESTRELLAS; i++) { // Una estrella por vuelta.
            float y = ALTURA_MIN_ESTRELLA + (1 - ALTURA_MIN_ESTRELLA) * Variacion.valor(i, 0, 0, SEMILLA_ESTRELLAS); // Altura.
            float phi = (float) (2 * Math.PI * Variacion.valor(i, 0, 1, SEMILLA_ESTRELLAS)); // Ángulo horizontal.
            float r = (float) Math.sqrt(1 - y * y); // Radio horizontal: la dirección queda de largo 1.
            float tamano = TAMANO_MIN_ESTRELLA + (TAMANO_MAX_ESTRELLA - TAMANO_MIN_ESTRELLA) * Variacion.valor(i, 0, 2, SEMILLA_ESTRELLAS);
            float brillo = BRILLO_MIN_ESTRELLA + (1 - BRILLO_MIN_ESTRELLA) * Variacion.valor(i, 0, 3, SEMILLA_ESTRELLAS);
            estrellas[i] = new float[] {r * (float) Math.cos(phi), y, r * (float) Math.sin(phi), tamano, brillo}; // Guarda la estrella.
        }
        return estrellas; // Cielo nocturno.
    }

    /** Dirección unitaria de la luna. */
    public static float[] direccionLuna() {
        float[] d = DIRECCION_LUNA; // Nombre corto.
        float largo = (float) Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]); // Largo para normalizar.
        return new float[] {d[0] / largo, d[1] / largo, d[2] / largo}; // Vector de largo 1.
    }

    // ==================== 3. DIBUJO ====================

    private final Shader shader; // Recibe el modo cielo, sus colores y la emisión.
    private final Cubo cubo; // Estrellas.
    private final Figuras figuras; // Cúpula y luna.

    /** Recibe el shader y la geometría compartidos. */
    public Cielo(Shader shader, Cubo cubo, Figuras figuras) {
        this.shader = shader; // Guarda el programa.
        this.cubo = cubo; // Guarda el cubo.
        this.figuras = figuras; // Guarda las figuras.
    }

    /** Dibuja el cielo alrededor del ojo de la cámara; debe ser lo primero de la vista principal. */
    public void dibujar(float[] ojo, boolean noche) {
        glDepthMask(false); // El cielo es fondo: se pinta, pero no ocupa lugar en el depth buffer (ver comentario de la clase).
        try { // Asegura que la profundidad vuelva a escribirse aunque algo falle.
            float[] cenit = noche ? COLOR_CENIT_NOCHE : COLOR_CENIT_DIA; // Color de arriba.
            float[] horizonte = noche ? COLOR_HORIZONTE_NOCHE : COLOR_HORIZONTE_DIA; // Color del horizonte.
            shader.entero("uCielo", 1); // iluminacion.frag calcula el degradado en lugar de la iluminación.
            shader.vector("uCieloCenit", cenit[0], cenit[1], cenit[2]); // Extremo de arriba del degradado.
            shader.vector("uCieloHorizonte", horizonte[0], horizonte[1], horizonte[2]); // Extremo de abajo.
            float diametro = 2 * RADIO_CIELO; // La esfera de Figuras mide 1: se escala al diámetro.
            figuras.esfera.dibujar(ojo[0], ojo[1], ojo[2], diametro, diametro, diametro, 0, 0, 0); // Cúpula centrada en la cámara.
            shader.entero("uCielo", 0); // Vuelve al material normal.
            if (noche) { // Estrellas y luna solo de noche.
                shader.entero("uEmision", 1); // Brillan con su propio color: no las ilumina nada.
                dibujarEstrellas(ojo); // Puntos blancos.
                dibujarLuna(ojo); // Disco claro.
                shader.entero("uEmision", 0); // Restablece el material normal.
            }
        } finally {
            shader.entero("uCielo", 0); // Nunca deja el modo cielo activo para la ciudad.
            glDepthMask(true); // El resto de la escena sí escribe profundidad.
        }
    }

    /** Cada estrella es una caja chica en su dirección, a DISTANCIA_ESTRELLAS del ojo. */
    private void dibujarEstrellas(float[] ojo) {
        for (float[] e : ESTRELLAS) { // Recorre el cielo nocturno.
            float x = ojo[0] + e[0] * DISTANCIA_ESTRELLAS; // Posición: ojo + dirección · distancia.
            float y = ojo[1] + e[1] * DISTANCIA_ESTRELLAS; // Altura.
            float z = ojo[2] + e[2] * DISTANCIA_ESTRELLAS; // Profundidad.
            float brillo = e[4]; // Intensidad de esta estrella.
            cubo.caja(x, y, z, e[3], e[3], e[3], brillo, brillo, brillo * 0.95f); // Blanco apenas cálido.
        }
    }

    /** Luna: esfera emisiva en DIRECCION_LUNA. */
    private void dibujarLuna(float[] ojo) {
        float[] d = direccionLuna(); // Dirección unitaria.
        float x = ojo[0] + d[0] * DISTANCIA_LUNA; // Posición en X.
        float y = ojo[1] + d[1] * DISTANCIA_LUNA; // Altura.
        float z = ojo[2] + d[2] * DISTANCIA_LUNA; // Profundidad.
        figuras.esfera.dibujar(x, y, z, DIAMETRO_LUNA, DIAMETRO_LUNA, DIAMETRO_LUNA, COLOR_LUNA[0], COLOR_LUNA[1], COLOR_LUNA[2]); // Disco claro.
    }
}
