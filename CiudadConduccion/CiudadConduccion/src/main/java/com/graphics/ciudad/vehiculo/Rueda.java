package com.graphics.ciudad.vehiculo; // Agrupa el vehículo del jugador, sus colisiones y las piezas compartidas con el tráfico.

import com.graphics.ciudad.motor.Cubo; // Rayos y marca de la llanta.
import com.graphics.ciudad.motor.Figuras; // Cilindros del neumático y la llanta.
import com.graphics.ciudad.motor.Shader; // Rotación de la rueda sobre su eje (uRotacion).

/**
 * RUEDA: las cuatro ruedas redondas de un auto, compartidas por el jugador (Auto) y el tráfico (trafico/Vehiculo).
 * Responsable de: las medidas y colores de la rueda, dónde va cada una en la carrocería, cuánto gira sobre su eje
 * según lo recorrido (giroPorDistancia), cómo se acercan las delanteras al ángulo de dirección pedido
 * (acercarDireccion) y el dibujo: neumático, llanta, dos rayos en cruz y una marca roja que da vueltas.
 * Cada vehículo guarda su propio anguloRueda y anguloDireccion; esta clase no tiene estado.
 * Se comunica con: Auto y Vehiculo (la usan igual), Figuras (cilindro), Cubo (cajas) y Shader (uRotacion).
 * Ejes locales del vehículo: X de lado a lado (+X a la derecha), Z a lo largo con el frente en -Z. Y es la altura:
 * el centro de la rueda está a RADIO_RUEDA del suelo, así el neumático toca el asfalto (Y = 0).
 */
public final class Rueda {

    // ==================== 1. MEDIDAS, UBICACIÓN Y COLORES (valores ajustables) ====================
    public static final float RADIO_RUEDA = 0.32f; // Radio de cada rueda: el centro queda a esta altura y la rueda toca el asfalto (Y = 0).
    public static final float ANCHO_RUEDA = 0.24f; // Ancho del neumático (a lo largo del eje de la rueda).
    public static final float FRACCION_LLANTA = 0.62f; // La llanta gris ocupa el 62 % del diámetro del neumático.
    public static final float LADO_RUEDA = 0.88f; // Del centro del auto al centro de cada rueda, de lado a lado (la carrocería mide 1.65).
    public static final float EJE_RUEDA = 0.82f; // Del centro del auto a cada eje, a lo largo (la carrocería mide 2.6).
    public static final float DISTANCIA_EJES = 2 * EJE_RUEDA; // 1.64: entre el eje delantero y el trasero (modelo de bicicleta).
    public static final float ANGULO_MAX_DIRECCION = (float) Math.toRadians(30); // Máximo giro de las ruedas delanteras: 30°.
    public static final float VELOCIDAD_DIRECCION = 3; // Radianes por segundo con que las delanteras doblan o vuelven al centro.
    public static final float[] COLOR_NEUMATICO = {0.055f, 0.065f, 0.08f}; // Caucho casi negro.
    public static final float[] COLOR_LLANTA = {0.62f, 0.64f, 0.68f}; // Metal gris claro.
    public static final float[] COLOR_RAYO = {0.30f, 0.30f, 0.32f}; // Rayos gris oscuro sobre la llanta.
    public static final float[] COLOR_MARCA_LLANTA = {0.85f, 0.10f, 0.08f}; // Marca roja en la llanta: hace visible la rotación.

    /** Impide crear objetos: solo ofrece constantes y funciones. */
    private Rueda() {
    }

    // ==================== 2. ROTACIÓN Y DIRECCIÓN ====================

    /**
     * Ángulo que gira una rueda al recorrer "distancia" sin patinar. Un radián es el ángulo cuyo arco mide lo mismo que
     * el radio: si la rueda avanza una distancia d, el punto de contacto recorre un arco de largo d sobre su borde, y
     * ese arco corresponde a d / RADIO_RUEDA radianes. Una vuelta completa (2π) recorre la circunferencia, 2π · radio.
     * Una distancia negativa (reversa) da un ángulo negativo: la rueda gira hacia atrás. Con distancia 0 (detenido) no
     * gira nada: por eso cada vehículo le pasa lo que avanzó DE VERDAD en el cuadro, no su velocidad.
     */
    public static float giroPorDistancia(float distancia) {
        return distancia / RADIO_RUEDA; // Ángulo en radianes = arco / radio.
    }

    /**
     * Acerca el ángulo de dirección "actual" al "objetivo" moviéndose como mucho VELOCIDAD_DIRECCION · deltaTime (las
     * ruedas no saltan de golpe) y nunca más allá de ±ANGULO_MAX_DIRECCION. Positivo = hacia la izquierda.
     */
    public static float acercarDireccion(float actual, float objetivo, float deltaTime) {
        float paso = VELOCIDAD_DIRECCION * deltaTime; // Lo máximo que pueden moverse en este cuadro.
        float nuevo = actual + Math.max(-paso, Math.min(paso, objetivo - actual)); // Paso limitado hacia el objetivo.
        return Math.max(-ANGULO_MAX_DIRECCION, Math.min(ANGULO_MAX_DIRECCION, nuevo)); // Nunca más de 30°.
    }

    /**
     * Matriz (por columnas) que orienta el cilindro como rueda y lo hace girar sobre su eje:
     * 1) Ry(-anguloRueda) gira el cilindro alrededor de su propio eje (Y del cilindro);
     * 2) Rz(-90°) acuesta ese eje sobre el X local del vehículo (el eje de la rueda, de lado a lado).
     * Producto Rz(-90°) · Ry(φ) con c = cos φ y s = sen φ: columnas (0, -c, -s), (1, 0, 0) y (0, -s, c).
     * Se usa -anguloRueda porque, con esta orientación, un ángulo positivo movería la parte de arriba de la rueda hacia
     * atrás (+Z local); con el signo cambiado, al avanzar la parte de arriba va hacia el frente, como una rueda real.
     * El giro en Y del vehículo y de la dirección lo aplica después el shader con uGiro.
     */
    public static float[] rotacion(float anguloRueda) {
        float c = (float) Math.cos(-anguloRueda); // Coseno del giro sobre el eje.
        float s = (float) Math.sin(-anguloRueda); // Seno del giro sobre el eje.
        return new float[] {0, -c, -s, 1, 0, 0, 0, -s, c}; // Columnas de Rz(-90°) · Ry(-anguloRueda).
    }

    // ==================== 3. DIBUJO ====================

    /**
     * Dibuja las cuatro ruedas de un vehículo con centro (x, z) y orientación "angulo": en ±LADO_RUEDA de lado a lado y
     * ±EJE_RUEDA a lo largo. Todas giran anguloRueda sobre su eje; las delanteras (Z local negativa) además doblan
     * anguloDireccion.
     */
    public static void dibujarCuatro(Cubo cubo, Figuras figuras, Shader shader, float x, float z, float angulo,
                                     float anguloRueda, float anguloDireccion) {
        for (int lado = -1; lado <= 1; lado += 2) { // Izquierda y derecha.
            for (int eje = -1; eje <= 1; eje += 2) { // Delantero (-1, el frente es -Z) y trasero (+1).
                float direccion = eje < 0 ? anguloDireccion : 0; // Solo doblan las delanteras.
                dibujar(cubo, figuras, shader, x, z, angulo, lado * LADO_RUEDA, eje * EJE_RUEDA, anguloRueda, direccion);
            }
        }
    }

    /**
     * Rueda redonda en (localX, localZ) del vehículo: neumático (cilindro oscuro), llanta (cilindro gris más chico y
     * apenas más ancho, se ve de ambos lados), dos rayos en cruz y una marca roja descentrada: la marca da vueltas y
     * hace evidente la rotación. direccion es el giro extra en Y de la rueda respecto del vehículo.
     */
    public static void dibujar(Cubo cubo, Figuras figuras, Shader shader, float x, float z, float angulo,
                               float localX, float localZ, float anguloRueda, float direccion) {
        float coseno = (float) Math.cos(angulo); // Orientación del vehículo.
        float seno = (float) Math.sin(angulo);
        float ruedaX = x + coseno * localX + seno * localZ; // Centro de la rueda en el mundo (misma transformación que las piezas).
        float ruedaZ = z - seno * localX + coseno * localZ; // Centro de la rueda en Z.
        float giroY = angulo + direccion; // La rueda mira hacia donde apunta el vehículo más el giro de la dirección.
        float[] rotacion = rotacion(anguloRueda); // Acuesta el cilindro y lo hace rodar.
        float diametro = 2 * RADIO_RUEDA; // Diámetro del neumático.
        shader.matriz3("uRotacion", rotacion); // Todo lo que sigue gira con la rueda.
        figuras.cilindro.dibujarGirada(ruedaX, RADIO_RUEDA, ruedaZ, diametro, ANCHO_RUEDA, diametro,
            COLOR_NEUMATICO[0], COLOR_NEUMATICO[1], COLOR_NEUMATICO[2], giroY); // Neumático: el cilindro tiene su eje en Y antes de rotar.
        float llanta = diametro * FRACCION_LLANTA; // Diámetro de la llanta.
        figuras.cilindro.dibujarGirada(ruedaX, RADIO_RUEDA, ruedaZ, llanta, ANCHO_RUEDA + 0.02f, llanta,
            COLOR_LLANTA[0], COLOR_LLANTA[1], COLOR_LLANTA[2], giroY); // Llanta gris, sobresale 0.01 de cada lado.
        cubo.cajaGirada(ruedaX, RADIO_RUEDA, ruedaZ, llanta * 0.9f, ANCHO_RUEDA + 0.04f, 0.06f,
            COLOR_RAYO[0], COLOR_RAYO[1], COLOR_RAYO[2], giroY); // Rayo en una dirección.
        cubo.cajaGirada(ruedaX, RADIO_RUEDA, ruedaZ, 0.06f, ANCHO_RUEDA + 0.04f, llanta * 0.9f,
            COLOR_RAYO[0], COLOR_RAYO[1], COLOR_RAYO[2], giroY); // Rayo perpendicular.
        // Marca descentrada: su centro está a d del eje, sobre el X del cilindro. Ese punto se rota igual que la rueda
        // (columna 0 de la matriz = hacia dónde queda el X del cilindro en coordenadas del vehículo) y después con él.
        float d = llanta * 0.3f; // Distancia de la marca al centro de la rueda.
        float marcaLocalX = localX + rotacion[0] * d; // Desplazamiento de la marca en X local del vehículo.
        float marcaY = RADIO_RUEDA + rotacion[1] * d; // Altura de la marca: sube y baja al girar.
        float marcaLocalZ = localZ + rotacion[2] * d; // Desplazamiento en Z local (sin contar la dirección: la diferencia es mínima).
        float marcaX = x + coseno * marcaLocalX + seno * marcaLocalZ; // Marca en el mundo, X.
        float marcaZ = z - seno * marcaLocalX + coseno * marcaLocalZ; // Marca en el mundo, Z.
        cubo.cajaGirada(marcaX, marcaY, marcaZ, 0.1f, ANCHO_RUEDA + 0.06f, 0.1f,
            COLOR_MARCA_LLANTA[0], COLOR_MARCA_LLANTA[1], COLOR_MARCA_LLANTA[2], giroY); // Marca roja que da vueltas.
        shader.matriz3("uRotacion", Shader.IDENTIDAD_3X3); // Lo siguiente vuelve a girar solo en Y.
    }
}
