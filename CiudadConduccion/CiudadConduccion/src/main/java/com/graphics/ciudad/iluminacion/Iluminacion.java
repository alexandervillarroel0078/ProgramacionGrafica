package com.graphics.ciudad.iluminacion; // Agrupa el estado de las luces de la escena.

import com.graphics.ciudad.motor.Cubo; // Dibuja postes y bombillas.
import com.graphics.ciudad.motor.Shader; // Recibe los uniforms de iluminación.
import com.graphics.ciudad.vehiculo.Auto; // Aporta la posición y el frente para los faros.
import static org.lwjgl.glfw.GLFW.*; // Incluye las constantes de las teclas N y F.

/**
 * ILUMINACION: sol, farolas y focos del vehículo.
 * Responsable de: el estado día/noche (N) y faros (F), las posiciones LUCES de las nueve farolas, el envío de
 * esos datos al shader iluminacion.frag, el dibujo de postes y bombillas y el texto de estado para el título.
 * Se comunica con: Shader (uniforms uNoche, uFaros, uEmision, uLuces, uAuto, uFrente), Cubo (farolas), Auto
 * (posición y frente de los faros) y Juego (teclas, título y orden de dibujo). Decoracion usa esNoche().
 * El shader calcula iluminación local: este ejemplo todavía no proyecta sombras.
 */
public class Iluminacion {

    // ==================== 1. ESTADO Y POSICIONES DE LAS LUCES ====================
    private final Shader shader; // Programa que recibe los datos de iluminación.
    private final Cubo cubo; // Geometría con la que se dibujan las farolas.
    private boolean noche = true; // Inicia la escena con iluminación nocturna.
    private boolean faros = true; // Inicia los focos del auto encendidos.
    public static final int MAX_LUCES = 16; // Tamaño del arreglo uLuces[16] del shader: no se pueden enviar más farolas.
    // Las farolas están sobre las avenidas horizontales (Z = -50, -30, -10, 10, 30), 4 unidades al sur del eje de la
    // calle, junto a la acera: quedan dentro de una celda de calle sin tapar el carril central. Las filas alternan
    // columnas (X = -40, 0, 40 y X = -20, 20) para repartir la luz, incluidos los sectores exteriores de la ciudad 11 × 11.
    public static final float[][] LUCES = { // Cada fila contiene la posición X, Y, Z de una bombilla.
        {-40, 4.5f, -46}, // Farola de la avenida norte, sector oeste.
        {0, 4.5f, -46}, // Farola de la avenida norte, centro.
        {40, 4.5f, -46}, // Farola de la avenida norte, sector este.
        {-20, 4.5f, -26}, // Farola de la segunda avenida, lado oeste.
        {20, 4.5f, -26}, // Farola de la segunda avenida, lado este.
        {-40, 4.5f, -6}, // Farola de la tercera avenida, sector oeste.
        {0, 4.5f, -6}, // Farola de la tercera avenida, junto al parque central.
        {40, 4.5f, -6}, // Farola de la tercera avenida, sector este.
        {-20, 4.5f, 14}, // Farola de la cuarta avenida, lado oeste.
        {20, 4.5f, 14}, // Farola de la cuarta avenida, lado este.
        {-40, 4.5f, 34}, // Farola de la quinta avenida, sector suroeste.
        {0, 4.5f, 34}, // Farola de la quinta avenida, centro sur.
        {40, 4.5f, 34} // Farola de la quinta avenida, sector sureste.
    };

    /** Recibe el shader y el cubo compartidos. */
    public Iluminacion(Shader shader, Cubo cubo) {
        this.shader = shader; // Guarda el programa que recibirá los uniforms.
        this.cubo = cubo; // Guarda la geometría de las farolas.
    }

    // ==================== 2. CONTROLES E INDICADORES ====================

    /** Añade los interruptores de iluminación a los controles de Juego. */
    public void tecla(int key) {
        if (key == GLFW_KEY_N) { // Comprueba la tecla de cambio de ambiente.
            noche = !noche; // Alterna entre noche y día.
        }
        if (key == GLFW_KEY_F) { // Comprueba el interruptor de los focos del auto.
            faros = !faros; // Enciende los faros si estaban apagados y viceversa.
        }
    }

    /** Prepara el texto que Juego incorpora al título de la ventana. */
    public String estado() {
        String ambiente = "Día"; // Usa día como texto inicial.
        if (noche) { // Comprueba si está seleccionado el ambiente nocturno.
            ambiente = "Noche"; // Reemplaza el texto por el estado real.
        }
        String estadoFaros = "OFF"; // Usa apagado como texto inicial de los focos.
        if (faros) { // Comprueba si las luces del auto están activas.
            estadoFaros = "ON"; // Indica que los faros están encendidos.
        }
        return " | Luces: " + estadoFaros + " | " + ambiente; // Devuelve ambos indicadores con el formato del título.
    }

    /** Indica si la escena está en modo nocturno; Decoracion lo usa para las ventanas. */
    public boolean esNoche() {
        return noche; // Estado elegido con la tecla N.
    }

    // ==================== 3. DATOS QUE RECIBE EL SHADER ====================

    /** Actualiza los uniforms de iluminación antes de dibujar la ciudad. */
    public void preparar(Auto auto) {
        shader.entero("uNoche", 0); // Inicialmente configura iluminación diurna.
        if (noche) { // Revisa el modo elegido por el usuario.
            shader.entero("uNoche", 1); // Comunica al shader que debe usar iluminación nocturna.
        }
        shader.entero("uFaros", 0); // Inicialmente desactiva los conos de luz del auto.
        if (faros) { // Revisa el interruptor de los faros.
            shader.entero("uFaros", 1); // Activa el cálculo de los dos focos en el shader.
        }
        shader.entero("uEmision", 0); // Hace que los objetos normales reciban iluminación.

        int cantidad = Math.min(LUCES.length, MAX_LUCES); // Nunca envía más posiciones de las que caben en uLuces.
        shader.entero("uNumLuces", cantidad); // Indica al shader cuántas farolas debe recorrer.
        for (int indice = 0; indice < cantidad; indice++) { // Recorre todas las bombillas definidas en LUCES.
            float x = LUCES[indice][0]; // Lee la coordenada horizontal de la bombilla.
            float y = LUCES[indice][1]; // Lee su altura sobre el suelo.
            float z = LUCES[indice][2]; // Lee su coordenada en profundidad.
            shader.vector("uLuces[" + indice + "]", x, y, z); // Envía esa posición al arreglo GLSL.
        }

        float frenteX = -(float) Math.sin(auto.getAngulo()); // Calcula hacia dónde apunta el auto en X.
        float frenteZ = -(float) Math.cos(auto.getAngulo()); // Calcula hacia dónde apunta el auto en Z.
        shader.vector("uAuto", auto.getX(), 0.7f, auto.getZ()); // Envía el centro del vehículo a la altura de sus focos.
        shader.vector("uFrente", frenteX, 0, frenteZ); // Envía la dirección frontal del vehículo.
    }

    // ==================== 4. MODELOS DE LAS FAROLAS ====================

    /** Dibuja postes y bombillas en las mismas posiciones usadas para calcular la luz. */
    public void dibujarFarolas() {
        for (float[] luz : LUCES) { // Selecciona una farola a la vez.
            float x = luz[0]; // Lee la posición horizontal del poste.
            float y = luz[1]; // Lee la altura de la bombilla.
            float z = luz[2]; // Lee la posición del poste en profundidad.
            cubo.caja(x, 2.2f, z, 0.18f, 4.4f, 0.18f, 0.20f, 0.24f, 0.28f); // Dibuja el poste delgado y oscuro.
            if (noche) { // Las bombillas aparentan estar encendidas únicamente de noche.
                shader.entero("uEmision", 1); // Evita que la bombilla sea oscurecida por la iluminación.
            }
            cubo.caja(x, y, z, 0.7f, 0.35f, 0.7f, 1, 0.83f, 0.42f); // Dibuja la bombilla de color cálido.
            shader.entero("uEmision", 0); // Restablece el material normal para el siguiente objeto.
        }
    }
}
