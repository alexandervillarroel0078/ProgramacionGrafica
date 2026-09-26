package com.graphics.ciudad.vehiculo; // Agrupa el vehículo del jugador, sus colisiones, su indicador, sus luces y su cabina.

import com.graphics.ciudad.motor.Cubo; // Parabrisas y luneta: cajas finas inclinadas.
import com.graphics.ciudad.motor.Malla; // Cabina y ventanillas: figuras extruidas desde un perfil.
import com.graphics.ciudad.motor.Shader; // Rotación extra (uRotacion) para inclinar los vidrios.

/**
 * CABINA: techo, parantes y vidrios de un auto, compartida por el jugador y el tráfico (cada uno con su color).
 * FORMA: un trapecio visto de costado (PERFIL_CABINA), extruido a lo ancho con Malla.extruir(). La base es más larga que
 * el techo; el parabrisas (adelante, -Z local) está más inclinado que la luneta (atrás), como en un auto real. La
 * cabina se pinta del color de la carrocería: así se ven el techo y los parantes.
 * VIDRIOS (azul-gris oscuro, sin emisión), separados SEPARACION_VIDRIO de la cabina para que no parpadeen:
 *  - parabrisas y luneta: cajas finas inclinadas como su cara. Cubo solo gira en Y, así que la inclinación se da con
 *    uRotacion (la matriz extra del shader): sus columnas son el eje lateral, la dirección de la cara y su normal;
 *  - ventanillas: dos por lado, cada una un trapecio fino extruido que sigue la inclinación de los parantes delantero y
 *    trasero, separadas por el parante central (el color de la cabina que queda entre ambas).
 * Coordenadas locales del auto: X lateral, Y altura desde el suelo y Z longitudinal (el frente es -Z). Todo queda dentro
 * del ancho del cuerpo (1.65) y la altura total es casi la del bloque anterior (techo en Y = 1.40).
 * Se comunica con: Malla (extrusión), Cubo y Shader (dibujo), Auto y Vehiculo (la dibujan en su posición) y Juego
 * (crea y libera sus mallas en la GPU).
 */
public class Cabina {

    // ==================== FORMA (valores ajustables) ====================
    /**
     * Perfil lateral de la cabina: puntos {z, y} en orden alrededor del contorno (debe ser convexo). Base de Z = 0.78 a
     * -0.62 (1.40 de largo), techo de -0.12 a 0.55 (0.67). Parabrisas: sube 0.5 en 0.5 de avance (45°); luneta: sube
     * 0.5 en 0.23 (≈ 65°): más vertical. La base está en Y = 0.90, apenas debajo del techo de la carrocería (0.925).
     */
    public static final float[][] PERFIL_CABINA = {
        {0.78f, 0.90f}, // Abajo atrás.
        {-0.62f, 0.90f}, // Abajo adelante.
        {-0.12f, 1.40f}, // Arriba adelante: fin del parabrisas.
        {0.55f, 1.40f} // Arriba atrás: comienzo de la luneta.
    };
    public static final float ANCHO_CABINA = 1.40f; // Un poco menos que la carrocería (1.65).
    public static final float SEPARACION_VIDRIO = 0.012f; // Distancia de cada vidrio a la cara de la cabina: evita el z-fighting.
    public static final float GROSOR_VIDRIO = 0.02f; // Espesor de los vidrios.
    public static final float MARGEN_VIDRIO = 0.07f; // Marco del color de la cabina alrededor de cada vidrio.
    public static final float BASE_VENTANILLA = 0.98f; // Altura del borde inferior de las ventanillas laterales.
    public static final float TECHO_VENTANILLA = 1.33f; // Altura del borde superior de las ventanillas laterales.
    public static final float CENTRO_PARANTE = 0.20f; // Z del parante central, entre las dos ventanillas de cada lado.
    public static final float ANCHO_PARANTE = 0.12f; // Ancho del parante central.
    public static final float[] COLOR_VIDRIO = {0.16f, 0.22f, 0.30f}; // Azul-gris oscuro, sin emisión.

    private final Shader shader; // Programa que recibe uRotacion.
    private final Cubo cubo; // Parabrisas y luneta.
    private final Malla cuerpo; // Trapecio extruido: techo y parantes.
    private final Malla ventanillaDelantera; // Trapecio fino: ventanilla delantera (sirve para ambos lados).
    private final Malla ventanillaTrasera; // Trapecio fino: ventanilla trasera.

    /** Genera las mallas (sin usar la GPU todavía). */
    public Cabina(Shader shader, Cubo cubo) {
        this.shader = shader; // Guarda el programa.
        this.cubo = cubo; // Guarda la geometría de cajas.
        cuerpo = new Malla(shader, Malla.extruir(PERFIL_CABINA, ANCHO_CABINA)); // 36 vértices.
        ventanillaDelantera = new Malla(shader, Malla.extruir(perfilVentanilla(true), GROSOR_VIDRIO)); // Trapecio fino.
        ventanillaTrasera = new Malla(shader, Malla.extruir(perfilVentanilla(false), GROSOR_VIDRIO)); // Trapecio fino.
    }

    /** Sube las mallas a la GPU. */
    public void crear() {
        cuerpo.crear(); // Cabina.
        ventanillaDelantera.crear(); // Ventanilla delantera.
        ventanillaTrasera.crear(); // Ventanilla trasera.
    }

    /** Libera las mallas. */
    public void eliminar() {
        cuerpo.eliminar(); // Cabina.
        ventanillaDelantera.eliminar(); // Ventanilla delantera.
        ventanillaTrasera.eliminar(); // Ventanilla trasera.
    }

    // ==================== GEOMETRÍA DE LOS VIDRIOS ====================

    /** Z del borde de la cabina a la altura y, sobre la recta que une dos puntos del perfil (interpolación lineal). */
    private static float zEnRecta(float[] p, float[] q, float y) {
        float t = (y - p[1]) / (q[1] - p[1]); // Fracción del camino entre p y q (0 en p, 1 en q).
        return p[0] + t * (q[0] - p[0]); // Z en esa fracción.
    }

    /**
     * Perfil {z, y} de una ventanilla: trapecio entre BASE_VENTANILLA y TECHO_VENTANILLA. La delantera va desde el
     * parabrisas (inclinado, con MARGEN_VIDRIO de marco) hasta el parante central; la trasera, desde el parante hasta
     * la luneta. Así siguen la inclinación de los parantes delantero y trasero.
     */
    public static float[][] perfilVentanilla(boolean delantera) {
        float[] abajoAtras = PERFIL_CABINA[0]; // Extremos de la luneta.
        float[] arribaAtras = PERFIL_CABINA[3];
        float[] abajoAdelante = PERFIL_CABINA[1]; // Extremos del parabrisas.
        float[] arribaAdelante = PERFIL_CABINA[2];
        float b = BASE_VENTANILLA; // Altura inferior.
        float t = TECHO_VENTANILLA; // Altura superior.
        if (delantera) { // Del parabrisas al parante central.
            float paranteDelantero = CENTRO_PARANTE - ANCHO_PARANTE / 2; // Borde delantero del parante central.
            return new float[][] {
                {paranteDelantero, b}, // Abajo, junto al parante central.
                {zEnRecta(abajoAdelante, arribaAdelante, b) + MARGEN_VIDRIO, b}, // Abajo, junto al parabrisas.
                {zEnRecta(abajoAdelante, arribaAdelante, t) + MARGEN_VIDRIO, t}, // Arriba, junto al parabrisas.
                {paranteDelantero, t} // Arriba, junto al parante central.
            };
        }
        float paranteTrasero = CENTRO_PARANTE + ANCHO_PARANTE / 2; // Borde trasero del parante central.
        return new float[][] {
            {zEnRecta(abajoAtras, arribaAtras, b) - MARGEN_VIDRIO, b}, // Abajo, junto a la luneta.
            {paranteTrasero, b}, // Abajo, junto al parante central.
            {paranteTrasero, t}, // Arriba, junto al parante central.
            {zEnRecta(abajoAtras, arribaAtras, t) - MARGEN_VIDRIO, t} // Arriba, junto a la luneta.
        };
    }

    // ==================== DIBUJO ====================

    /** Dibuja la cabina del auto con centro (x, z) y orientación angulo, pintada con el color de su carrocería. */
    public void dibujar(float x, float z, float angulo, float r, float g, float b) {
        cuerpo.dibujarGirada(x, 0, z, 1, 1, 1, r, g, b, angulo); // Techo y parantes; el perfil ya tiene medidas reales.
        float coseno = (float) Math.cos(angulo); // Orientación del auto.
        float seno = (float) Math.sin(angulo); // Orientación del auto.
        float separacionLateral = ANCHO_CABINA / 2 + SEPARACION_VIDRIO; // Las ventanillas quedan apenas fuera del costado.
        for (int lado = -1; lado <= 1; lado += 2) { // Costado izquierdo y derecho.
            float ox = lado * separacionLateral; // Desplazamiento lateral en coordenadas del auto.
            float px = x + coseno * ox; // Mismo giro que Auto.pieza() con Z local = 0.
            float pz = z - seno * ox; // Posición Z.
            ventanillaDelantera.dibujarGirada(px, 0, pz, 1, 1, 1, COLOR_VIDRIO[0], COLOR_VIDRIO[1], COLOR_VIDRIO[2], angulo); // Ventanilla delantera.
            ventanillaTrasera.dibujarGirada(px, 0, pz, 1, 1, 1, COLOR_VIDRIO[0], COLOR_VIDRIO[1], COLOR_VIDRIO[2], angulo); // Ventanilla trasera.
        }
        dibujarVidrioInclinado(x, z, angulo, PERFIL_CABINA[1], PERFIL_CABINA[2]); // Parabrisas: cara delantera.
        dibujarVidrioInclinado(x, z, angulo, PERFIL_CABINA[3], PERFIL_CABINA[0]); // Luneta: cara trasera.
    }

    /**
     * Vidrio sobre la cara inclinada que va del punto p al punto q del perfil. La caja fina se orienta con una matriz
     * en uRotacion cuyas columnas son: X local = eje lateral (1, 0, 0); Y local = dirección de la cara, de p a q;
     * Z local = X × Y, perpendicular a la cara (el espesor del vidrio). El centro se corre SEPARACION_VIDRIO hacia afuera
     * de la cabina, en la dirección de la normal que se aleja del centro del perfil.
     */
    private void dibujarVidrioInclinado(float x, float z, float angulo, float[] p, float[] q) {
        float dz = q[0] - p[0]; // Avance de la cara en Z.
        float dy = q[1] - p[1]; // Subida de la cara en Y.
        float largo = (float) Math.hypot(dz, dy); // Largo de la cara.
        float ey = dy / largo; // Dirección de la cara (unitaria), en Y.
        float ez = dz / largo; // En Z.
        float nY = -ez; // Z local = X × Y = (1, 0, 0) × (0, ey, ez) = (0, -ez, ey): perpendicular a la cara.
        float nZ = ey; // Componente Z de esa perpendicular.
        float medioZ = (p[0] + q[0]) / 2; // Centro de la cara, en Z.
        float medioY = (p[1] + q[1]) / 2; // Centro de la cara, en Y.
        float centroZ = 0; // Centro del perfil, para saber hacia dónde es "afuera".
        float centroY = 0;
        for (float[] punto : PERFIL_CABINA) { // Promedio de los puntos.
            centroZ += punto[0] / PERFIL_CABINA.length; // En Z.
            centroY += punto[1] / PERFIL_CABINA.length; // En Y.
        }
        float haciaAfuera = Math.signum(nY * (medioY - centroY) + nZ * (medioZ - centroZ)); // +1 si la normal ya mira afuera.
        float localY = medioY + haciaAfuera * nY * SEPARACION_VIDRIO; // Centro del vidrio, apenas fuera de la cara.
        float localZ = medioZ + haciaAfuera * nZ * SEPARACION_VIDRIO; // Igual en Z.
        float mundoX = x + (float) Math.sin(angulo) * localZ; // Giro del auto con X local = 0 (igual que Auto.pieza()).
        float mundoZ = z + (float) Math.cos(angulo) * localZ; // Posición Z.
        shader.matriz3("uRotacion", new float[] {1, 0, 0, 0, ey, ez, 0, nY, nZ}); // Columnas: lateral, cara, normal.
        cubo.cajaGirada(mundoX, localY, mundoZ, ANCHO_CABINA - 2 * MARGEN_VIDRIO, largo - 2 * MARGEN_VIDRIO, GROSOR_VIDRIO,
            COLOR_VIDRIO[0], COLOR_VIDRIO[1], COLOR_VIDRIO[2], angulo); // Vidrio: ancho, largo sobre la cara y espesor.
        shader.matriz3("uRotacion", Shader.IDENTIDAD_3X3); // Lo siguiente vuelve a girar solo en Y.
    }
}
