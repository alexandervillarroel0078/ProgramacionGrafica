package com.graphics.ciudad.vehiculo; // Agrupa el vehículo del jugador y sus colisiones.

import com.graphics.ciudad.motor.Cubo; // Dibuja cada pieza del vehículo.
import java.util.function.IntPredicate; // Pregunta si una tecla está presionada sin depender de GLFW.
import static org.lwjgl.glfw.GLFW.*; // Incluye las constantes de las teclas de conducción.

/**
 * AUTO: el vehículo del jugador.
 * Responsable de: su estado (posición, orientación y velocidad), la física por cuadro con deltaTime
 * (aceleración, resistencia, freno, límites y giro), el reinicio con R (reset()) y el dibujo con piezas locales.
 * Se comunica con: Colisiones (valida cada posición propuesta), Cubo (dibuja las piezas), Juego (lo actualiza
 * y lo reinicia); Camara, Iluminacion, Entregas y Minimapa leen su estado con los getters.
 * Orden de lectura: variables, reinicio, movimiento, dibujo.
 */
public class Auto {

    // ==================== 0. CONSTANTES DE LA FÍSICA (valores ajustables) ====================
    public static final float VELOCIDAD_MAX = 16; // Velocidad máxima hacia delante, en unidades por segundo (≈ 58 km/h).
    public static final float VELOCIDAD_REVERSA = 6; // Velocidad máxima en reversa, en unidades por segundo (se aplica como -6).
    public static final float ACELERACION = 9; // Aceleración del motor, en unidades por segundo cuadrado.
    public static final float RESISTENCIA = 0.7f; // Pérdida de velocidad normal al rodar, por segundo (decaimiento exponencial).
    public static final float FRENO = 7; // Pérdida de velocidad mientras se mantiene Espacio: diez veces la resistencia normal.
    public static final float VELOCIDAD_GIRO = 0.11f; // Radianes girados por segundo y por unidad de velocidad: el giro crece con la velocidad.

    // ==================== 1. VARIABLES DEL AUTO ====================
    public static final float RADIO_AUTO = 1.65f; // Radio que contiene al vehículo para las colisiones.
    public static final float X_INICIAL = -50; // Columna 0: calle del borde oeste.
    public static final float Z_INICIAL = 50; // Fila 10: calle del borde sur; el auto parte de la esquina suroeste.
    float x = X_INICIAL; // Posición horizontal inicial: centro de una calle.
    float z = Z_INICIAL; // Posición inicial sobre el eje que recorre el fondo de la ciudad.
    float angulo = 0; // Orientación en radianes; cero apunta hacia -Z.
    float velocidad = 0; // Unidades por segundo; un valor negativo significa reversa.

    // ==================== 2. REINICIO ====================

    /** Coloca nuevamente el auto en su punto de partida; Juego lo llama al presionar R. */
    public void reset() {
        x = X_INICIAL; // Recupera la coordenada X de inicio.
        z = Z_INICIAL; // Recupera la coordenada Z de inicio.
        angulo = 0; // Orienta el frente hacia -Z.
        velocidad = 0; // Detiene cualquier movimiento previo.
    }

    /** Devuelve el auto a una posición anterior y lo detiene; Juego lo usa cuando el movimiento chocaría con el tráfico. */
    public void detenerEn(float xAnterior, float zAnterior) {
        x = xAnterior; // Recupera la última posición X sin choque.
        z = zAnterior; // Recupera la última posición Z sin choque.
        velocidad = 0; // El choque detiene al auto, igual que contra una manzana: sin rebote.
    }

    // ==================== 3. MOVIMIENTO POR CUADRO ====================

    /** Actualiza la conducción; deltaTime contiene los segundos transcurridos entre cuadros. */
    public void actualizar(float deltaTime, IntPredicate pulsada) {
        float acelerador = 0; // Sin teclas pulsadas no se aplica aceleración del motor.

        if (pulsada.test(GLFW_KEY_W) || pulsada.test(GLFW_KEY_UP)) { // Acepta W o flecha arriba para avanzar.
            acelerador += 1; // Solicita aceleración hacia delante.
        }

        if (pulsada.test(GLFW_KEY_S) || pulsada.test(GLFW_KEY_DOWN)) { // Acepta S o flecha abajo para retroceder.
            acelerador -= 1; // Primero reduce la velocidad positiva y después entra en reversa.
        }

        velocidad += acelerador * ACELERACION * deltaTime; // Integra la aceleración de 9 unidades por segundo cuadrado.
        float resistencia = RESISTENCIA; // Define la pérdida de velocidad normal al rodar.

        if (pulsada.test(GLFW_KEY_SPACE)) { // Detecta si el usuario mantiene presionado el freno.
            resistencia = FRENO; // Aumenta la pérdida de velocidad para detenerse rápidamente.
        }

        float factorFrenado = (float) Math.exp(-resistencia * deltaTime); // Calcula la fracción de velocidad conservada.
        velocidad *= factorFrenado; // Aplica resistencia de forma proporcional al tiempo transcurrido.
        velocidad = Math.max(-VELOCIDAD_REVERSA, Math.min(VELOCIDAD_MAX, velocidad)); // Limita la reversa a -6 y el avance a 16.
        float direccion = 0; // Sin dirección presionada el volante permanece recto.

        if (pulsada.test(GLFW_KEY_A) || pulsada.test(GLFW_KEY_LEFT)) { // Comprueba el giro hacia la izquierda.
            direccion += 1; // Selecciona el sentido positivo de rotación.
        }

        if (pulsada.test(GLFW_KEY_D) || pulsada.test(GLFW_KEY_RIGHT)) { // Comprueba el giro hacia la derecha.
            direccion -= 1; // Selecciona el sentido negativo de rotación.
        }

        angulo += direccion * velocidad * VELOCIDAD_GIRO * deltaTime; // Gira según la velocidad; en reversa invierte el giro.
        float frenteX = -(float) Math.sin(angulo); // Obtiene la componente X del frente del vehículo.
        float frenteZ = -(float) Math.cos(angulo); // Obtiene la componente Z; con ángulo cero vale -1.
        float siguienteX = x + frenteX * velocidad * deltaTime; // Propone la nueva posición X.
        float siguienteZ = z + frenteZ * velocidad * deltaTime; // Propone la nueva posición Z.

        if (Colisiones.puedeCircular(siguienteX, siguienteZ)) { // Comprueba la posición antes de mover el auto.
            x = siguienteX; // Acepta el desplazamiento horizontal.
            z = siguienteZ; // Acepta el desplazamiento en profundidad.
        } else { // La posición propuesta invadiría una manzana o saldría del mapa.
            velocidad = 0; // Detiene el auto conservando su última posición válida.
        }
    }

    // ==================== 4. DIBUJO DEL AUTO ====================

    /** Construye el auto con cajas; argumentos: posición XYZ, tamaño XYZ y color RGB. */
    public void dibujar(Cubo cubo) {
        pieza(cubo, 0, 0.65f, 0, 1.65f, 0.55f, 2.6f, 0.95f, 0.24f, 0.12f); // Dibuja la carrocería roja.
        pieza(cubo, 0, 1.12f, 0.12f, 1.3f, 0.55f, 1.25f, 0.22f, 0.65f, 0.78f); // Dibuja la cabina azulada.
        float[] ladosRuedas = {-0.88f, 0.88f}; // Ubica ruedas a izquierda y derecha del auto.
        float[] ejesRuedas = {-0.82f, 0.82f}; // Ubica las ruedas delanteras y traseras.

        for (float ladoX : ladosRuedas) { // Selecciona uno de los dos lados del vehículo.
            for (float ejeZ : ejesRuedas) { // Selecciona el eje delantero o trasero.
                pieza(cubo, ladoX, 0.38f, ejeZ, 0.24f, 0.58f, 0.6f, 0.055f, 0.065f, 0.08f); // Dibuja una rueda oscura.
            }
        }

        float[] ladosFaros = {-0.55f, 0.55f}; // Define la separación lateral de las luces.
        for (float ladoX : ladosFaros) { // Repite el dibujo para ambos lados.
            pieza(cubo, ladoX, 0.68f, -1.32f, 0.38f, 0.2f, 0.07f, 1, 0.95f, 0.65f); // Dibuja un faro delantero claro.
            pieza(cubo, ladoX, 0.68f, 1.32f, 0.35f, 0.17f, 0.07f, 0.85f, 0.05f, 0.05f); // Dibuja una luz trasera roja.
        }
    }

    /** Transforma una pieza del espacio local del auto al espacio de la ciudad. */
    private void pieza(Cubo cubo, float localX, float y, float localZ, float sx, float sy, float sz, float r, float g, float b) {
        float coseno = (float) Math.cos(angulo); // Calcula el coseno de la orientación del auto.
        float seno = (float) Math.sin(angulo); // Calcula el seno de la misma orientación.
        float mundoX = x + coseno * localX + seno * localZ; // Gira la posición local y suma la posición X del auto.
        float mundoZ = z - seno * localX + coseno * localZ; // Gira la posición local y suma la posición Z del auto.
        cubo.cajaGirada(mundoX, y, mundoZ, sx, sy, sz, r, g, b, angulo); // Dibuja la pieza con la orientación del vehículo.
    }

    // ==================== 5. CONSULTAS DEL ESTADO ====================

    /** Devuelve la posición horizontal del centro del auto. */
    public float getX() {
        return x; // Coordenada X actual.
    }

    /** Devuelve la posición en profundidad del centro del auto. */
    public float getZ() {
        return z; // Coordenada Z actual.
    }

    /** Devuelve la orientación en radianes; cero apunta hacia -Z. */
    public float getAngulo() {
        return angulo; // Ángulo actual.
    }

    /** Devuelve la velocidad en unidades por segundo; negativa en reversa. */
    public float getVelocidad() {
        return velocidad; // Velocidad actual.
    }
}
