package com.graphics.ciudad.iluminacion; // Agrupa el estado de las luces de la escena.

import com.graphics.ciudad.motor.Figuras; // Cilindro, esfera y cono (los mismos de los árboles) para las farolas.
import com.graphics.ciudad.motor.Malla; // Figura con la que se dibuja cada pieza de la farola.
import com.graphics.ciudad.motor.Shader; // Recibe los uniforms de iluminación.
import com.graphics.ciudad.mundo.Mapa; // Convierte celdas de manzana en coordenadas y da la dirección de cada lado.
import com.graphics.ciudad.vehiculo.Auto; // Aporta la posición y el frente para los faros.
import java.util.ArrayList; // Lista de piezas de cada farola.
import java.util.List; // Tipo de esa lista.
import static org.lwjgl.glfw.GLFW.*; // Incluye las constantes de las teclas N y F.

/**
 * ILUMINACION: sol, farolas y focos del vehículo.
 * Responsable de: el estado día/noche (N) y faros (F), las ubicaciones LUCES de las trece farolas, el envío de
 * esos datos al shader iluminacion.frag, el modelo y el dibujo de las farolas y el texto de estado para el título.
 * Se comunica con: Shader (uniforms uNoche, uFaros, uEmision, uLuces, uAuto, uFrente, uRotacion), Figuras (farolas), Auto
 * (posición y frente de los faros) y Juego (teclas, título y orden de dibujo). Decoracion usa esNoche().
 * El shader calcula iluminación local, sin sombras reales; Sombras agrega manchas oscuras (sombras falsas) bajo los objetos.
 *
 * FAROLAS EN LA VEREDA: el poste se planta sobre la acera de una manzana (edificio o parque), junto al cordón del lado
 * que da a la calle, y un brazo curvo lleva la bombilla BRAZO_FAROLA unidades hacia la calle, como en las calles
 * reales: la luz cae sobre la calzada, pero nada de la farola se apoya en ella ni participa en colisiones.
 *
 * MODELO DE LA FAROLA (sección 4), de abajo hacia arriba, solo con las figuras de los árboles:
 *   base (cilindro corto y ancho) → poste (dos cilindros, el de arriba más delgado) → brazo curvo (tres cilindros
 *   inclinados con uRotacion, unidos por esferas chicas en los codos) → pantalla (cono oscuro) → bombilla (esfera).
 * La bombilla se dibuja EXACTAMENTE en BOMBILLAS[i], el mismo punto que recibe el shader como luz: si se mueve la
 * farola cambian las dos cosas juntas. De día la bombilla es gris claro; de noche es emisiva (blanco cálido).
 */
public class Iluminacion {

    // ==================== 1. ESTADO Y POSICIONES DE LAS LUCES ====================
    private final Shader shader; // Programa que recibe los datos de iluminación.
    private final Figuras figuras; // Cilindro, esfera y cono con los que se dibujan las farolas.
    public static final boolean NOCHE_AL_INICIAR = false; // La demo arranca de día (se ve todo); N cambia a noche.
    private boolean noche = NOCHE_AL_INICIAR; // Inicia la escena de día; la tecla N alterna la iluminación nocturna.
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

    // ---- Piezas del modelo (valores ajustables, en unidades del mundo) ----
    public static final float ANCHO_BASE = 0.42f; // Diámetro de la base: más ancha que el poste, como un zócalo.
    public static final float ALTO_BASE = 0.45f; // Alto de la base sobre la acera.
    public static final float ANCHO_POSTE_ABAJO = 0.2f; // Diámetro del tramo inferior del poste.
    public static final float ANCHO_POSTE_ARRIBA = 0.14f; // Diámetro del tramo superior: el poste se afina hacia arriba.
    public static final float GROSOR_BRAZO = 0.09f; // Diámetro de los tramos del brazo.
    public static final float ANCHO_PANTALLA = 0.8f; // Diámetro de la boca de la pantalla (base del cono).
    public static final float ALTO_PANTALLA = 0.3f; // Alto del cono de la pantalla.
    public static final float SEPARACION_PANTALLA = 0.05f; // La boca de la pantalla queda esto por encima del centro de la bombilla.
    public static final float DIAMETRO_BOMBILLA = 0.32f; // Esfera de la bombilla: asoma por debajo de la pantalla.
    /** Altura del tramo final del brazo: la punta del cono de la pantalla, que cuelga de él (4.85). */
    public static final float ALTURA_BRAZO = ALTURA_BOMBILLA + SEPARACION_PANTALLA + ALTO_PANTALLA;
    // PERFIL DEL BRAZO: puntos {avance, altura} vistos de costado. avance es la fracción de BRAZO_FAROLA recorrida desde
    // el poste hacia la calle (0 = poste, 1 = sobre la bombilla) y altura se mide desde ALTURA_BRAZO (negativa = más
    // abajo). El primer punto es la punta del poste; cada par de puntos seguidos es un tramo recto: uno empinado, uno
    // suave y uno horizontal, que juntos parecen una curva.
    public static final float[][] PERFIL_BRAZO = {{0, -0.7f}, {0.15f, -0.2f}, {0.5f, 0}, {1, 0}};
    public static final float[] COLOR_BASE = {0.18f, 0.19f, 0.21f}; // Gris oscuro.
    public static final float[] COLOR_POSTE = {0.46f, 0.49f, 0.53f}; // Gris metálico (poste, brazo y codos).
    public static final float[] COLOR_PANTALLA = {0.14f, 0.15f, 0.17f}; // Casi negro: tapa la bombilla por arriba.
    public static final float[] COLOR_BOMBILLA_DIA = {0.82f, 0.82f, 0.80f}; // Gris claro, apagada y sin emisión.
    public static final float[] COLOR_BOMBILLA_NOCHE = {1.0f, 0.92f, 0.72f}; // Blanco cálido, emisiva.

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

    /** Altura de la punta del poste, que es el primer punto del brazo (4.15). */
    public static float alturaTopePoste() {
        return ALTURA_BRAZO + PERFIL_BRAZO[0][1]; // Primer punto del perfil.
    }

    /** Recibe el shader y las figuras compartidas. */
    public Iluminacion(Shader shader, Figuras figuras) {
        this.shader = shader; // Guarda el programa que recibirá los uniforms.
        this.figuras = figuras; // Guarda la geometría de las farolas.
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

    /** Qué parte de la farola es una pieza; FarolasTest busca la BOMBILLA para compararla con la luz. */
    public enum Parte { BASE, POSTE, CODO, BRAZO, PANTALLA, BOMBILLA }

    /** Figura con la que se dibuja una pieza: las tres mallas redondeadas de Figuras. */
    public enum Forma { CILINDRO, ESFERA, CONO }

    /**
     * PIEZA: una figura ya ubicada en el mundo. (x, y, z) es su centro, (sx, sy, sz) su tamaño antes de rotar y
     * rotacion la matriz uRotacion (nueve números, columna por columna). El modelo se arma sin OpenGL, así una prueba
     * puede revisarlo; dibujarFarolas() solo recorre la lista y la envía a la GPU.
     */
    public static final class Pieza {
        public final Parte parte; // Base, poste, codo, brazo, pantalla o bombilla.
        public final Forma forma; // Cilindro, esfera o cono.
        public final float x, y, z; // Centro en el mundo.
        public final float sx, sy, sz; // Tamaño en cada eje; en los tramos del brazo, sy es el largo.
        public final float[] color; // RGB entre 0 y 1.
        public final float[] rotacion; // Matriz 3 × 3 por columnas para uRotacion.
        public final boolean emisiva; // true = conserva su color sin iluminación (la bombilla de noche).

        Pieza(Parte parte, Forma forma, float x, float y, float z, float sx, float sy, float sz,
              float[] color, float[] rotacion, boolean emisiva) {
            this.parte = parte; // Parte de la farola.
            this.forma = forma; // Figura a usar.
            this.x = x; // Centro X.
            this.y = y; // Centro Y.
            this.z = z; // Centro Z.
            this.sx = sx; // Ancho.
            this.sy = sy; // Alto.
            this.sz = sz; // Profundidad.
            this.color = color; // Color del material.
            this.rotacion = rotacion; // Orientación.
            this.emisiva = emisiva; // Brillo propio.
        }
    }

    /** Arma las piezas de la farola i, de abajo hacia arriba, en coordenadas del mundo. */
    public static List<Pieza> modeloFarola(int i, boolean noche) {
        List<Pieza> piezas = new ArrayList<>(); // Resultado.
        float[] vertical = Shader.IDENTIDAD_3X3; // Piezas verticales: sin rotación extra.
        float x = POSTES[i][0]; // Centro del poste en X.
        float z = POSTES[i][1]; // Centro del poste en Z.
        int[] haciaCalle = Mapa.VECINOS[LUCES[i][2]]; // {dFila, dColumna} del lado de la calle.
        float dx = haciaCalle[1]; // Dirección hacia la calle en X (las columnas son X).
        float dz = haciaCalle[0]; // Dirección hacia la calle en Z (las filas son Z).

        // Base: cilindro corto y ancho apoyado en la acera.
        piezas.add(new Pieza(Parte.BASE, Forma.CILINDRO, x, ALTURA_ACERA + ALTO_BASE / 2, z,
            ANCHO_BASE, ALTO_BASE, ANCHO_BASE, COLOR_BASE, vertical, false));
        // Poste: dos tramos de igual alto; el de arriba más delgado, así se afina hacia la punta.
        float pie = ALTURA_ACERA + ALTO_BASE; // Donde termina la base.
        float tramo = (alturaTopePoste() - pie) / 2; // Alto de cada tramo.
        piezas.add(new Pieza(Parte.POSTE, Forma.CILINDRO, x, pie + tramo / 2, z,
            ANCHO_POSTE_ABAJO, tramo, ANCHO_POSTE_ABAJO, COLOR_POSTE, vertical, false));
        piezas.add(new Pieza(Parte.POSTE, Forma.CILINDRO, x, pie + tramo * 1.5f, z,
            ANCHO_POSTE_ARRIBA, tramo, ANCHO_POSTE_ARRIBA, COLOR_POSTE, vertical, false));

        // Brazo: un cilindro entre cada par de puntos del perfil y una esfera en cada codo, que tapa la unión.
        for (int k = 0; k < PERFIL_BRAZO.length - 1; k++) { // Recorre los tramos (el último punto no abre ninguno).
            float ax = x + dx * PERFIL_BRAZO[k][0] * BRAZO_FAROLA; // Inicio del tramo, X.
            float ay = ALTURA_BRAZO + PERFIL_BRAZO[k][1]; // Inicio del tramo, altura.
            float az = z + dz * PERFIL_BRAZO[k][0] * BRAZO_FAROLA; // Inicio del tramo, Z.
            float bx = x + dx * PERFIL_BRAZO[k + 1][0] * BRAZO_FAROLA; // Fin del tramo, X.
            float by = ALTURA_BRAZO + PERFIL_BRAZO[k + 1][1]; // Fin del tramo, altura.
            float bz = z + dz * PERFIL_BRAZO[k + 1][0] * BRAZO_FAROLA; // Fin del tramo, Z.
            float codo = k == 0 ? ANCHO_POSTE_ARRIBA : GROSOR_BRAZO; // El primer codo también redondea la punta del poste.
            piezas.add(new Pieza(Parte.CODO, Forma.ESFERA, ax, ay, az, codo, codo, codo, COLOR_POSTE, vertical, false));
            float largo = (float) Math.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay) + (bz - az) * (bz - az)); // Largo del tramo.
            float[] giro = rotacionTramo(dx, dz, (bx - ax) / largo, (by - ay) / largo, (bz - az) / largo); // Acuesta el cilindro sobre el tramo.
            piezas.add(new Pieza(Parte.BRAZO, Forma.CILINDRO, (ax + bx) / 2, (ay + by) / 2, (az + bz) / 2,
                GROSOR_BRAZO, largo, GROSOR_BRAZO, COLOR_POSTE, giro, false));
        }

        // Luminaria: pantalla (cono con la boca hacia abajo) colgada del brazo y bombilla (esfera) asomando debajo.
        float[] luz = BOMBILLAS[i]; // El MISMO punto que preparar() envía como uLuces[i]: no se duplican coordenadas.
        piezas.add(new Pieza(Parte.PANTALLA, Forma.CONO, luz[0], luz[1] + SEPARACION_PANTALLA + ALTO_PANTALLA / 2, luz[2],
            ANCHO_PANTALLA, ALTO_PANTALLA, ANCHO_PANTALLA, COLOR_PANTALLA, vertical, false));
        float[] colorBombilla = noche ? COLOR_BOMBILLA_NOCHE : COLOR_BOMBILLA_DIA; // Encendida solo de noche.
        piezas.add(new Pieza(Parte.BOMBILLA, Forma.ESFERA, luz[0], luz[1], luz[2],
            DIAMETRO_BOMBILLA, DIAMETRO_BOMBILLA, DIAMETRO_BOMBILLA, colorBombilla, vertical, noche));
        return piezas; // Farola completa.
    }

    /**
     * Matriz que acuesta el cilindro (su eje es Y) sobre la dirección unitaria (ex, ey, ez) de un tramo del brazo.
     * Sus columnas dicen adónde va cada eje local: X = lateral horizontal (-dz, 0, dx), perpendicular a la calle;
     * Y = el tramo; Z = lateral × tramo (producto vectorial). Con Z calculada así es una rotación pura, sin espejar.
     */
    static float[] rotacionTramo(float dx, float dz, float ex, float ey, float ez) {
        float lx = -dz; // Eje lateral en X (su componente Y es 0).
        float lz = dx; // Eje lateral en Z.
        float nx = -lz * ey; // Producto vectorial (lx, 0, lz) × (ex, ey, ez), componente X.
        float ny = lz * ex - lx * ez; // Componente Y.
        float nz = lx * ey; // Componente Z.
        return new float[] {lx, 0, lz, ex, ey, ez, nx, ny, nz}; // Tres columnas.
    }

    /** Dibuja cada farola pieza por pieza; la bombilla está en la misma posición usada para calcular la luz. */
    public void dibujarFarolas() {
        for (int i = 0; i < LUCES.length; i++) { // Selecciona una farola a la vez.
            for (Pieza p : modeloFarola(i, noche)) { // Recorre sus piezas.
                Malla malla = figuras.cilindro; // Figura de la pieza: cilindro por defecto...
                if (p.forma == Forma.ESFERA) {
                    malla = figuras.esfera; // ...esfera para codos y bombilla...
                } else if (p.forma == Forma.CONO) {
                    malla = figuras.cono; // ...cono para la pantalla.
                }
                shader.matriz3("uRotacion", p.rotacion); // Inclina los tramos del brazo; identidad para lo demás.
                shader.entero("uEmision", p.emisiva ? 1 : 0); // Solo la bombilla de noche ignora la iluminación.
                malla.dibujar(p.x, p.y, p.z, p.sx, p.sy, p.sz, p.color[0], p.color[1], p.color[2]); // Envía la pieza.
            }
        }
        shader.matriz3("uRotacion", Shader.IDENTIDAD_3X3); // Restablece la rotación para el siguiente objeto.
        shader.entero("uEmision", 0); // Restablece el material normal para el siguiente objeto.
    }
}
