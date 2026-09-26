package com.graphics.ciudad.vehiculo; // Agrupa el vehículo del jugador, sus colisiones, su indicador y sus luces.

/**
 * LUCES DE VEHÍCULO: ubicación, tamaño y colores de faros y luces traseras, compartidos por el auto del jugador y los
 * vehículos del tráfico, para que todos se vean coherentes (sin constantes duplicadas).
 * Responsable de: decidir el color y si es emisiva cada bombilla:
 *  - FARO delantero: encendido = blanco cálido brillante, EMISIVO; apagado = gris oscuro, sin emisión.
 *  - LUZ TRASERA: el FRENO tiene prioridad (rojo intenso, emisivo); si no se frena y las luces están encendidas, es luz
 *    de POSICIÓN (rojo tenue, emisivo); si están apagadas, rojo oscuro sin emisión.
 * Quién decide "encendidas": para el jugador, la tecla F (Iluminacion.farosEncendidos()); para el tráfico, la noche.
 * Cada color se devuelve como {r, g, b, emisiva}: emisiva = 1 significa que el shader usa el color tal cual (uEmision),
 * así la bombilla se ve encendida aunque no le llegue luz.
 * Se comunica con: Auto y Vehiculo (dibujan las bombillas con estos datos).
 */
public final class LucesVehiculo {

    // ==================== UBICACIÓN Y TAMAÑO (coordenadas locales del vehículo; el frente es -Z) ====================
    public static final float LADO_LUZ = 0.55f; // Separación lateral de cada luz respecto al centro del vehículo.
    public static final float ALTURA_LUZ = 0.68f; // Altura de faros y luces traseras sobre el suelo.
    public static final float FRENTE_LUZ = -1.32f; // Posición local de los faros: el frente del vehículo es -Z local.
    public static final float TRASERA_LUZ = 1.32f; // Posición local de las luces traseras.
    public static final float[] TAMANO_FARO = {0.38f, 0.2f, 0.07f}; // Ancho, alto y espesor de cada faro.
    public static final float[] TAMANO_TRASERA = {0.35f, 0.17f, 0.07f}; // Ancho, alto y espesor de cada luz trasera.

    // ==================== COLORES (valores ajustables) ====================
    public static final float[] COLOR_FARO_ENCENDIDO = {1, 0.97f, 0.8f}; // Blanco cálido brillante (emisivo).
    public static final float[] COLOR_FARO_APAGADO = {0.30f, 0.29f, 0.26f}; // Gris-beige oscuro: vidrio sin luz.
    public static final float[] COLOR_POSICION = {0.60f, 0.06f, 0.05f}; // Luz de posición: rojo tenue (emisivo).
    public static final float[] COLOR_FRENO = {1.00f, 0.06f, 0.05f}; // Luz de freno: rojo intenso (emisivo).
    public static final float[] COLOR_TRASERA_APAGADA = {0.35f, 0.03f, 0.03f}; // Rojo oscuro: plástico sin luz.

    /** Impide crear objetos: solo ofrece datos y funciones estáticas. */
    private LucesVehiculo() {
    }

    /** Color {r, g, b, emisiva} de un faro delantero: blanco emisivo si está encendido, gris oscuro si no. */
    public static float[] colorFaro(boolean encendido) {
        float[] c = encendido ? COLOR_FARO_ENCENDIDO : COLOR_FARO_APAGADO; // Elige el color.
        return new float[] {c[0], c[1], c[2], encendido ? 1 : 0}; // Solo el faro encendido es emisivo.
    }

    /**
     * Color {r, g, b, emisiva} de una luz trasera. El freno domina: si se frena, rojo intenso emisivo aunque las luces
     * estén apagadas; si no, con las luces encendidas es luz de posición (rojo tenue emisivo) y con las luces apagadas,
     * rojo oscuro sin emisión.
     */
    public static float[] colorTrasera(boolean lucesEncendidas, boolean frenando) {
        if (frenando) { // El freno tiene prioridad sobre la luz de posición.
            return new float[] {COLOR_FRENO[0], COLOR_FRENO[1], COLOR_FRENO[2], 1}; // Rojo intenso, emisivo.
        }
        if (lucesEncendidas) { // Luces de posición.
            return new float[] {COLOR_POSICION[0], COLOR_POSICION[1], COLOR_POSICION[2], 1}; // Rojo tenue, emisivo.
        }
        return new float[] {COLOR_TRASERA_APAGADA[0], COLOR_TRASERA_APAGADA[1], COLOR_TRASERA_APAGADA[2], 0}; // Apagada.
    }
}
