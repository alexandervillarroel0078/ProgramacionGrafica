package com.graphics.ciudad.iluminacion; // Agrupa el estado de las luces de la escena.

import com.graphics.ciudad.motor.Cubo; // Dibuja postes y bombillas.
import com.graphics.ciudad.motor.Shader; // Recibe los uniforms de iluminación.
import com.graphics.ciudad.mundo.Mapa; // Convierte celdas de manzana en coordenadas y da la dirección de cada lado.
import com.graphics.ciudad.vehiculo.Auto; // Aporta la posición y el frente para los faros.
import static org.lwjgl.glfw.GLFW.*; // Incluye las constantes de las teclas N y F.

/**
 * ILUMINACION: sol, farolas y focos del vehículo.
 * Responsable de: el estado día/noche (N) y faros (F), las ubicaciones LUCES de las trece farolas, el envío de
 * esos datos al shader iluminacion.frag, el dibujo de postes y bombillas y el texto de estado para el título.
 * Se comunica con: Shader (uniforms uNoche, uFaros, uEmision, uLuces, uAuto, uFrente), Cubo (farolas), Auto
 * (posición y frente de los faros) y Juego (teclas, título y orden de dibujo). Decoracion usa esNoche().
 * El shader calcula iluminación local: este ejemplo todavía no proyecta sombras.
 *
 * FAROLAS EN LA VEREDA: el poste se planta sobre la acera de una manzana (edificio o parque), junto al cordón del lado
 * que da a la calle, y un brazo horizontal lleva la bombilla BRAZO_FAROLA unidades hacia la calle, como en las calles
 * reales: la luz cae sobre la calzada, pero nada de la farola se apoya en ella ni participa en colisiones.
 */
public class Iluminacion {

    // ==================== 1. ESTADO Y POSICIONES DE LAS LUCES ====================
    private final Shader shader; // Programa que recibe los datos de iluminación.
    private final Cubo cubo; // Geometría con la que se dibujan las farolas.
    private boolean noche = true; // Inicia la escena con iluminación nocturna.
    private boolean faros = true; // Inicia los focos del auto encendidos.
    public static final int MAX_LUCES = 16; // Tamaño del arreglo uLuces[16] del shader: no se pueden enviar más farolas.

    // ---- Forma y ubicación de las farolas (valores ajustables) ----
    public static final int NORTE = 0; // Lado de la manzana que da a la calle del norte (índice de Mapa.VECINOS).
    public static final int SUR = 1; // Lado que da a la calle del sur.
    public static final int OESTE = 2; // Lado que da a la calle del oeste.
    public static final int ESTE = 3; // Lado que da a la calle del este.
    public static final float MARGEN_POSTE = 0.4f; // Distancia del cordón al poste, hacia adentro de la acera (la acera de un parque mide 0.5).
    public static final float BRAZO_FAROLA = 1.5f; // Largo del brazo horizontal: la bombilla sobresale 1.1 sobre el borde de la calzada.
    public static final float ALTURA_BOMBILLA = 4.5f; // Altura de la bombilla sobre el suelo; el brazo pasa justo por encima.
    public static final float ALTURA_ACERA = 0.3f; // El poste nace sobre la acera, que Ciudad dibuja con 0.3 de alto.

    // FORMATO DE LUCES: cada farola es {fila, columna, lado}. (fila, columna) es una celda de MANZANA (edificio o parque)
    // y lado indica qué borde de esa manzana da a la calle donde va la farola: NORTE, SUR, OESTE o ESTE. El poste se
    // ubica en el centro de ese borde, MARGEN_POSTE hacia adentro de la acera, y la bombilla BRAZO_FAROLA hacia la calle.
    // Se eligieron bordes a mitad de cuadra: lejos de las esquinas, donde están los semáforos, los PARE y los pasos
    // peatonales, y repartidos por los cinco sectores (Centro 3, Barrio Norte 3, Parque Sur 3, Zona Oeste 2, Zona Este 2).
    public static final int[][] LUCES = {
        {3, 3, OESTE}, // Centro: manzana (3,3), frente a la avenida X = -30.
        {3, 5, NORTE}, // Centro: manzana (3,5), frente a la avenida Z = -30.
        {7, 5, SUR}, // Centro: manzana (7,5), frente a la avenida Z = 30.
        {1, 1, OESTE}, // Barrio Norte: manzana (1,1), frente a la calle del borde oeste.
        {1, 3, NORTE}, // Barrio Norte: manzana (1,3), frente a la calle del borde norte.
        {1, 9, ESTE}, // Barrio Norte: manzana (1,9), frente a la calle del borde este.
        {9, 1, SUR}, // Parque Sur: manzana (9,1), frente a la calle del borde sur.
        {9, 7, SUR}, // Parque Sur: parque (9,7), frente a la calle del borde sur.
        {9, 9, ESTE}, // Parque Sur: manzana (9,9), frente a la calle del borde este.
        {3, 1, OESTE}, // Zona Oeste: parque (3,1), frente a la calle del borde oeste.
        {5, 1, SUR}, // Zona Oeste: manzana (5,1), frente a la avenida Z = 10.
        {3, 9, OESTE}, // Zona Este: parque (3,9), frente a la avenida X = 30.
        {5, 9, ESTE} // Zona Este: manzana (5,9), frente a la calle del borde este.
    };
    /** Posición {x, z} de cada poste, calculada desde LUCES. */
    public static final float[][] POSTES = calcularPosiciones(MARGEN_POSTE);
    /** Posición {x, y, z} de cada bombilla (la que recibe el shader), calculada desde LUCES. */
    public static final float[][] BOMBILLAS = calcularBombillas();

    /**
     * Calcula {x, z} a una distancia "haciaAdentro" del cordón, medida desde el borde de la manzana que da a la calle:
     * positiva = sobre la acera; negativa = sobre la calle. El lado da la dirección hacia la calle (Mapa.VECINOS).
     */
    private static float[][] calcularPosiciones(float haciaAdentro) {
        float[][] posiciones = new float[LUCES.length][]; // Una posición por farola.
        for (int i = 0; i < LUCES.length; i++) { // Recorre las farolas.
            int[] farola = LUCES[i]; // {fila, columna, lado}.
            int[] haciaCalle = Mapa.VECINOS[farola[2]]; // {dFila, dColumna} del lado que da a la calle.
            float distancia = Mapa.TAM_CELDA / 2 - haciaAdentro; // Del centro de la manzana al punto buscado.
            float x = Mapa.centro(farola[1]) + haciaCalle[1] * distancia; // Las columnas son X.
            float z = Mapa.centro(farola[0]) + haciaCalle[0] * distancia; // Las filas son Z.
            posiciones[i] = new float[] {x, z}; // Punto en el centro del borde.
        }
        return posiciones; // Posiciones calculadas.
    }

    /** La bombilla está en la punta del brazo: BRAZO_FAROLA más allá del poste, hacia la calle, a ALTURA_BOMBILLA. */
    private static float[][] calcularBombillas() {
        float[][] puntas = calcularPosiciones(MARGEN_POSTE - BRAZO_FAROLA); // Poste + brazo: 1.1 sobre la calzada.
        float[][] bombillas = new float[puntas.length][]; // Agrega la altura.
        for (int i = 0; i < puntas.length; i++) { // Recorre las farolas.
            bombillas[i] = new float[] {puntas[i][0], ALTURA_BOMBILLA, puntas[i][1]}; // {x, y, z}.
        }
        return bombillas; // Posiciones de las bombillas.
    }

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

    /** Indica si los faros del auto están encendidos; lo muestra el HUD. */
    public boolean farosEncendidos() {
        return faros; // Estado elegido con la tecla F.
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
            float x = BOMBILLAS[indice][0]; // Lee la coordenada horizontal de la bombilla (punta del brazo).
            float y = BOMBILLAS[indice][1]; // Lee su altura sobre el suelo.
            float z = BOMBILLAS[indice][2]; // Lee su coordenada en profundidad.
            shader.vector("uLuces[" + indice + "]", x, y, z); // Envía esa posición al arreglo GLSL.
        }

        float frenteX = -(float) Math.sin(auto.getAngulo()); // Calcula hacia dónde apunta el auto en X.
        float frenteZ = -(float) Math.cos(auto.getAngulo()); // Calcula hacia dónde apunta el auto en Z.
        shader.vector("uAuto", auto.getX(), 0.7f, auto.getZ()); // Envía el centro del vehículo a la altura de sus focos.
        shader.vector("uFrente", frenteX, 0, frenteZ); // Envía la dirección frontal del vehículo.
    }

    // ==================== 4. MODELOS DE LAS FAROLAS ====================

    /** Dibuja poste, brazo y bombilla; la bombilla está en la misma posición usada para calcular la luz. */
    public void dibujarFarolas() {
        float alturaBrazo = ALTURA_BOMBILLA + 0.2f; // El brazo pasa apenas por encima de la bombilla, que cuelga de él.
        float altoPoste = alturaBrazo + 0.06f - ALTURA_ACERA; // Del piso de la acera hasta el brazo.
        for (int i = 0; i < LUCES.length; i++) { // Selecciona una farola a la vez.
            float x = POSTES[i][0]; // Lee la posición horizontal del poste.
            float y = BOMBILLAS[i][1]; // Lee la altura de la bombilla.
            float z = POSTES[i][1]; // Lee la posición del poste en profundidad.
            float bombillaX = BOMBILLAS[i][0]; // Punta del brazo en X.
            float bombillaZ = BOMBILLAS[i][2]; // Punta del brazo en Z.
            cubo.caja(x, ALTURA_ACERA + altoPoste / 2, z, 0.18f, altoPoste, 0.18f, 0.20f, 0.24f, 0.28f); // Dibuja el poste delgado y oscuro sobre la acera.
            float centroBrazoX = (x + bombillaX) / 2; // El brazo va del poste a la bombilla: su centro está a mitad de camino.
            float centroBrazoZ = (z + bombillaZ) / 2; // Igual en Z.
            float largoX = Math.abs(bombillaX - x) + 0.12f; // Largo del brazo en X (0.12 de grosor si va en Z).
            float largoZ = Math.abs(bombillaZ - z) + 0.12f; // Largo del brazo en Z (0.12 de grosor si va en X).
            cubo.caja(centroBrazoX, alturaBrazo, centroBrazoZ, largoX, 0.12f, largoZ, 0.20f, 0.24f, 0.28f); // Brazo horizontal hacia la calle.
            if (noche) { // Las bombillas aparentan estar encendidas únicamente de noche.
                shader.entero("uEmision", 1); // Evita que la bombilla sea oscurecida por la iluminación.
            }
            cubo.caja(bombillaX, y, bombillaZ, 0.7f, 0.35f, 0.7f, 1, 0.83f, 0.42f); // Dibuja la bombilla de color cálido en la punta del brazo.
            shader.entero("uEmision", 0); // Restablece el material normal para el siguiente objeto.
        }
    }
}
