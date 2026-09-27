package com.graphics.ciudad.mundo; // Agrupa lo que forma la ciudad: mapa, edificios, decoración y señales.

import com.graphics.ciudad.iluminacion.Iluminacion; // Postes de farola: los árboles no deben taparlos.
import com.graphics.ciudad.motor.Cubo; // Senderos y bancos.
import com.graphics.ciudad.motor.Figuras; // Esfera, cilindro y cono para árboles y fuente.
import com.graphics.ciudad.motor.Shader; // Emisión del agua de noche.
import java.util.ArrayList; // Listas de árboles y bancos.
import java.util.Collections; // Publica LUMINARIAS sin permitir modificarla.
import java.util.List; // Tipo de esas listas.

/**
 * PARQUE: contenido de una celda de parque.
 * Responsable de: calcular (una vez, sin azar por cuadro) dónde van los árboles y los bancos de cada parque, y dibujar
 * senderos en cruz, una fuente central, 4 a 6 árboles (frondosos y pinos), 2 a 4 bancos que miran a la fuente y 1 o 2
 * luminarias peatonales tipo GLOBO en el borde de los senderos (luces puntuales: ver luminarias()).
 * VARIACIÓN DETERMINÍSTICA: cada parque usa su fila y columna para "sortear" disposición, tipo de árbol, altura, tamaño
 * de copa y tono de verde con variacion(), una función que siempre da el mismo número para los mismos datos. Así los
 * parques son distintos entre sí, pero cada uno se ve igual en todos los cuadros y en cada ejecución.
 * ESCALA (1 u ≈ 1 m): el auto mide 1.4 de alto; los frondosos miden de 5 a 7 y los pinos de 5 a 6, como árboles de
 * vereda reales. Crecen hacia ARRIBA (tronco más alto y copa estirada), no hacia los costados: el ancho de la copa
 * sigue sin superar un cuarto del parque (COPA_MAXIMA = 2.5), así no sale del césped.
 * Todo queda dentro de la celda del parque: nada invade la calle ni participa en colisiones (Colisiones ya bloquea la
 * manzana entera). Los árboles evitan los postes de semáforos, PARE, carteles y farolas que están en la acera del
 * parque: con la copa a la altura de esas señales, el tronco se aleja lo suficiente para que la copa no las toque.
 * Se comunica con: Decoracion (lo llama para cada celda de parque), Figuras y Cubo (dibujo), Shader (emisión),
 * Mapa (centro de la celda), Senalizacion e Iluminacion (postes que hay que esquivar). Entorno reutiliza dibujarArbol()
 * para los árboles del campo, y Sombras lee arboles() y bancos() para ubicar sus manchas.
 */
public class Parque {

    // ==================== 1. CONSTANTES (valores ajustables) ====================
    public static final float TOPE_CESPED = 0.37f; // Altura de la cara superior del césped que dibuja Ciudad (0.32 + 0.05).
    public static final float MITAD_CESPED = 4.5f; // El césped mide 9 × 9: 4.5 desde el centro hacia cada lado.
    public static final float ANCHO_SENDERO = 1.4f; // Ancho de cada sendero de la cruz.
    public static final float GROSOR_SENDERO = 0.03f; // Espesor: el sendero apenas sobresale del césped (sin z-fighting).
    public static final float[] COLOR_SENDERO = {0.80f, 0.72f, 0.54f}; // Beige, como tierra apisonada.
    public static final float COPA_MAXIMA = Mapa.TAM_CELDA / 4; // Diámetro máximo de una copa: 1/4 del ancho del parque.
    public static final float RADIO_CENTRO_LIBRE = 2.2f; // Ningún árbol a menos de esto del centro: allí está la fuente.
    public static final int ARBOLES_MIN = 4; // Menor cantidad de árboles por parque.
    public static final int ARBOLES_MAX = 6; // Mayor cantidad de árboles por parque.
    public static final int BANCOS_MIN = 2; // Menor cantidad de bancos por parque.
    public static final int BANCOS_MAX = 4; // Mayor cantidad de bancos por parque.
    public static final float DISTANCIA_BANCO = 2.3f; // Distancia del centro a cada banco, entre dos senderos.
    // Árboles altos: la copa (de 2 a 7 de altura) está a la altura de semáforos, PARE, carteles y farolas. Un árbol se
    // planta solo si su copa más ancha posible (COPA_MAXIMA / 2) queda a MARGEN_POSTES de la mitad de cada señal.
    public static final float MARGEN_POSTES = 0.2f; // Aire entre el borde de la copa y la señal más cercana.
    public static final float MITAD_SEMAFORO = 0.5f; // Media extensión del cabezal del semáforo (caja, lentes y viseras).
    public static final float MITAD_PARE = Senalizacion.LADO_PARE / 2; // Medio octógono del PARE.
    public static final float MITAD_CARTEL = Senalizacion.ANCHO_CARTEL / 2; // Media placa del cartel de sector.
    public static final float MITAD_FAROLA = Iluminacion.ANCHO_BASE / 2; // Media base de la farola (el brazo va hacia la calle).
    // Altura total de los árboles (del césped a la punta) y qué parte es tronco.
    public static final float ALTURA_FRONDOSO_MIN = 5; // Frondoso: de 5 a 7.
    public static final float ALTURA_FRONDOSO_MAX = 7;
    public static final float ALTURA_PINO_MIN = 5; // Pino: de 5 a 6.
    public static final float ALTURA_PINO_MAX = 6;
    public static final float FRACCION_TRONCO_FRONDOSO = 0.4f; // 2 a 2.8 de tronco: la copa empieza por encima de una persona.
    public static final float FRACCION_TRONCO_PINO = 0.2f; // Los pinos tienen el tronco corto y los conos casi hasta abajo.
    public static final float RADIO_FUENTE = 1.2f; // Radio de la base de la fuente.
    public static final float[] COLOR_AGUA = {0.25f, 0.50f, 0.85f}; // Agua de día (recibe luz).
    public static final float[] COLOR_AGUA_NOCHE = {0.12f, 0.30f, 0.55f}; // Agua de noche: emisiva pero oscura, "levemente" brillante.
    // Lugares posibles para árboles, relativos al centro del parque: 4 esquinas y 8 puntos de borde a los lados de los
    // senderos (el centro queda libre para la fuente). Cada parque recorre esta lista en un orden propio.
    public static final float[][] LUGARES_ARBOL = {
        {3.1f, 3.1f}, {-3.1f, 3.1f}, {-3.1f, -3.1f}, {3.1f, -3.1f}, // Esquinas.
        {3.3f, 1.9f}, {3.3f, -1.9f}, {-3.3f, 1.9f}, {-3.3f, -1.9f}, // Bordes este y oeste, a los lados del sendero.
        {1.9f, 3.3f}, {-1.9f, 3.3f}, {1.9f, -3.3f}, {-1.9f, -3.3f} // Bordes norte y sur, a los lados del sendero.
    };
    public static final int FRONDOSO = 0; // Tipo de árbol: tronco fino y copa de esferas.
    public static final int PINO = 1; // Tipo de árbol: tronco y conos apilados.
    // Formato de un árbol: {x, z, tipo, alturaTronco, diametroCopa, verde, giro, altoCopa}. altoCopa es cuánto sube la
    // copa por encima del tronco: la altura total es alturaTronco + altoCopa.
    public static final int ALTO_COPA = 7; // Índice de altoCopa en el arreglo.
    public static final float MITAD_BANCO = 0.8f; // Medio largo del banco (mide 1.6): radio del círculo que lo contiene.

    // ---- Luminarias peatonales tipo GLOBO (valores ajustables) ----
    // Poste bajo con una esfera lechosa arriba, a escala de peatón: más baja y más simple que la farola de calle.
    // Como el globo no tiene pantalla, su luz sale hacia TODOS lados: en el shader es una LUZ PUNTUAL (uGlobos).
    public static final float ALTURA_GLOBO = 3.0f; // Centro del globo sobre el césped: la punta queda a 3.225 de alto.
    public static final float DIAMETRO_GLOBO = 0.45f; // Esfera del globo.
    public static final float ANCHO_BASE_GLOBO = 0.24f; // Diámetro de la base (zócalo) del poste.
    public static final float ALTO_BASE_GLOBO = 0.3f; // Alto de esa base.
    public static final float ANCHO_POSTE_GLOBO = 0.1f; // Diámetro del poste: fino, de parque.
    public static final float ANCHO_ANILLO_GLOBO = 0.2f; // Portaglobo: anillo sobre el que se apoya la esfera.
    public static final float ALTO_ANILLO_GLOBO = 0.1f; // Alto del portaglobo.
    public static final float[] COLOR_POSTE_GLOBO = {0.12f, 0.15f, 0.13f}; // Verde casi negro, típico de parque.
    public static final float[] COLOR_GLOBO_DIA = {0.86f, 0.87f, 0.85f}; // Vidrio lechoso apagado (recibe luz).
    public static final float[] COLOR_GLOBO_NOCHE = {1.0f, 0.93f, 0.78f}; // Encendido: emisivo, blanco cálido.
    // UBICACIÓN: sobre el BORDE de un sendero (no en el medio del camino), a DESVIO_LUMINARIA del eje del sendero y a
    // una de las DISTANCIAS_LUMINARIA del centro del parque. Son 4 brazos × 3 distancias × 2 lados = 24 candidatas.
    public static final float DESVIO_LUMINARIA = 0.5f; // Del eje del sendero al poste: la base (0.24) no sale del sendero (1.4).
    public static final float[] DISTANCIAS_LUMINARIA = {2.4f, 3.0f, 3.6f}; // Del centro del parque: pasada la fuente y antes de la acera.
    public static final float MARGEN_LUMINARIA = 0.15f; // Aire mínimo entre el globo o la base y un árbol o un banco.
    public static final float MARGEN_LUMINARIA_POSTES = 0.5f; // Aire entre el globo y un semáforo, PARE, cartel o farola de la acera.
    public static final int LUMINARIAS_MAX_POR_PARQUE = 2; // Tope por parque; baja a 1 si no entran en uGlobos (ver luminariasPorParque()).
    /** Centro {x, y, z} del globo de cada luminaria de todos los parques: lo que Iluminacion envía como uGlobos. */
    public static final List<float[]> LUMINARIAS = Collections.unmodifiableList(calcularLuminarias());

    // ==================== 2. VARIACIÓN DETERMINÍSTICA ====================

    /**
     * Número "aleatorio" entre 0 y 1 que depende solo de sus datos: fracción de sen(combinación) · 43758.5453.
     * Es la misma función de hash que se usa en muchos shaders: sin estado, sin Random, siempre igual.
     */
    public static float variacion(int fila, int columna, int indice, int semilla) {
        return Variacion.valor(fila, columna, indice, semilla); // El hash vive en Variacion, compartido con Fachada.
    }

    /** Número entero entre minimo y maximo (incluidos) elegido con variacion(). */
    private static int entero(int fila, int columna, int semilla, int minimo, int maximo) {
        return minimo + (int) (variacion(fila, columna, 0, semilla) * (maximo - minimo + 1)); // Reparte el intervalo en partes iguales.
    }

    // ==================== 3. DISPOSICIÓN DE ÁRBOLES Y BANCOS ====================

    /**
     * Árboles de un parque: cada uno es {x, z, tipo, alturaTronco, diametroCopa, verde, giro} en coordenadas del mundo.
     * Recorre LUGARES_ARBOL desde un punto de partida propio del parque y toma los primeros que no queden cerca de
     * un poste; la cantidad (4 a 6), el tipo y las medidas salen de variacion().
     */
    public static List<float[]> arboles(int fila, int columna) {
        List<float[]> lista = new ArrayList<>(); // Resultado.
        float cx = Mapa.centro(columna); // Centro del parque en X.
        float cz = Mapa.centro(fila); // Centro del parque en Z.
        int cantidad = entero(fila, columna, 1, ARBOLES_MIN, ARBOLES_MAX); // 4, 5 o 6 árboles.
        int inicio = entero(fila, columna, 2, 0, LUGARES_ARBOL.length - 1); // Por dónde empieza a recorrer los lugares.
        float proporcionPinos = variacion(fila, columna, 0, 3); // Algunos parques tienen más pinos y otros más frondosos.
        for (int k = 0; k < LUGARES_ARBOL.length && lista.size() < cantidad; k++) { // Recorre los lugares posibles.
            float[] lugar = LUGARES_ARBOL[(inicio + k * 5) % LUGARES_ARBOL.length]; // Salta de a 5: mezcla esquinas y bordes.
            float x = cx + lugar[0]; // Posición del tronco en X.
            float z = cz + lugar[1]; // Posición del tronco en Z.
            if (cercaDeUnPoste(x, z, COPA_MAXIMA / 2 + MARGEN_POSTES)) { // Semáforo, PARE o farola en la acera cercana.
                continue; // Ese lugar se saltea.
            }
            int i = lista.size(); // Índice del árbol: distingue su variación de la de los demás.
            int tipo = variacion(fila, columna, i, 4) < proporcionPinos ? PINO : FRONDOSO; // Mezcla de tipos.
            float verde = variacion(fila, columna, i, 7); // Tono de verde (0 = claro, 1 = oscuro).
            float giro = (float) (2 * Math.PI * variacion(fila, columna, i, 8)); // Orientación de la copa: rompe la simetría.
            lista.add(arbol(x, z, tipo, variacion(fila, columna, i, 5), variacion(fila, columna, i, 6), verde, giro)); // Guarda el árbol.
        }
        return lista; // Árboles del parque.
    }

    /**
     * Árbol {x, z, tipo, alturaTronco, diametroCopa, verde, giro, altoCopa} con sus medidas: vAltura y vCopa (entre 0
     * y 1) eligen la altura total y el ancho de la copa dentro de sus rangos. Lo usan los parques y el campo (Entorno),
     * así los dos tienen árboles de la misma escala.
     */
    public static float[] arbol(float x, float z, int tipo, float vAltura, float vCopa, float verde, float giro) {
        boolean pino = tipo == PINO;
        float minimo = pino ? ALTURA_PINO_MIN : ALTURA_FRONDOSO_MIN; // Rango de altura según el tipo.
        float maximo = pino ? ALTURA_PINO_MAX : ALTURA_FRONDOSO_MAX;
        float altura = minimo + (maximo - minimo) * vAltura; // Altura total del árbol.
        float alturaTronco = altura * (pino ? FRACCION_TRONCO_PINO : FRACCION_TRONCO_FRONDOSO); // Parte que es tronco.
        float diametroCopa = 1.8f + (COPA_MAXIMA - 0.1f - 1.8f) * vCopa; // Entre 1.8 y 2.4: nunca más de 1/4 del parque.
        return new float[] {x, z, tipo, alturaTronco, diametroCopa, verde, giro, altura - alturaTronco}; // El resto es copa.
    }

    /**
     * Indica si un objeto de radio "copa" (ya con su aire) centrado en (x, z) tocaría un semáforo, un PARE, un cartel de
     * sector o una farola. Lo usan los árboles (radio de la copa más ancha), las luminarias y los basureros.
     */
    public static boolean cercaDeUnPoste(float x, float z, float copa) {
        for (float[] s : Senalizacion.SEMAFOROS) { // Semáforos del Centro.
            if (Math.hypot(x - s[0], z - s[1]) < copa + MITAD_SEMAFORO) { // La copa tocaría el cabezal.
                return true; // Se descarta el lugar.
            }
        }
        for (float[] p : Senalizacion.PARES) { // Señales de PARE.
            if (Math.hypot(x - p[0], z - p[1]) < copa + MITAD_PARE) {
                return true;
            }
        }
        for (float[] c : Senalizacion.CARTELES_SECTOR) { // Carteles de sector {sector, x, z, ángulo}.
            if (Math.hypot(x - c[1], z - c[2]) < copa + MITAD_CARTEL) {
                return true;
            }
        }
        for (float[] poste : Iluminacion.POSTES) { // Postes de farola.
            if (Math.hypot(x - poste[0], z - poste[1]) < copa + MITAD_FAROLA) {
                return true;
            }
        }
        return false; // El lugar está libre.
    }

    /**
     * Bancos de un parque: cada uno es {x, z, angulo}. Van en las diagonales, entre dos senderos, a DISTANCIA_BANCO del
     * centro, y miran hacia la fuente: el frente (-Z local) apunta al centro, con ángulo atan2(dx, dz) (convención de Auto).
     */
    public static List<float[]> bancos(int fila, int columna) {
        List<float[]> lista = new ArrayList<>(); // Resultado.
        int cantidad = entero(fila, columna, 9, BANCOS_MIN, BANCOS_MAX); // 2, 3 o 4 bancos.
        int inicio = entero(fila, columna, 10, 0, 3); // Diagonal por la que se empieza.
        float d = DISTANCIA_BANCO / (float) Math.sqrt(2); // Componentes X y Z de un punto a 45°.
        float[][] diagonales = {{d, d}, {-d, d}, {-d, -d}, {d, -d}}; // Las cuatro diagonales, entre los brazos de la cruz.
        for (int k = 0; k < cantidad; k++) { // Toma las primeras "cantidad" diagonales desde el inicio.
            float[] rel = diagonales[(inicio + k) % 4]; // Posición relativa al centro.
            float angulo = (float) Math.atan2(rel[0], rel[1]); // El frente mira hacia (-rel): hacia la fuente.
            lista.add(new float[] {Mapa.centro(columna) + rel[0], Mapa.centro(fila) + rel[1], angulo}); // Guarda el banco.
        }
        return lista; // Bancos del parque.
    }

    // ==================== 3b. LUMINARIAS GLOBO ====================

    /**
     * Cuántas luminarias lleva cada parque: LUMINARIAS_MAX_POR_PARQUE si todas entran en el arreglo uGlobos del shader
     * (Iluminacion.MAX_GLOBOS); si no, las que entren repartidas en partes iguales, pero nunca menos de 1. Con 6 parques
     * son 2 (12 luces); con 9 o más parques, 1. Si hubiera más parques que MAX_GLOBOS, LuminariasParqueTest lo detecta.
     */
    public static int luminariasPorParque() {
        int parques = Math.max(1, Mapa.parques().size()); // Evita dividir por cero en un mapa sin parques.
        return Math.max(1, Math.min(LUMINARIAS_MAX_POR_PARQUE, Iluminacion.MAX_GLOBOS / parques));
    }

    /**
     * Luminarias de un parque: cada una es {x, z} (el poste; el globo está ALTURA_GLOBO más arriba). REGLAS:
     *  - SOBRE UN SENDERO: en el borde de uno de los cuatro brazos de la cruz (DESVIO_LUMINARIA del eje), a una de las
     *    DISTANCIAS_LUMINARIA del centro: lejos de la fuente y antes de la acera;
     *  - SIN CHOCAR CON ÁRBOLES: el globo está a la altura de las copas, así que se mide contra la copa real de cada árbol;
     *  - SIN CHOCAR CON BANCOS: la base queda fuera del círculo que contiene al banco;
     *  - LEJOS DE LOS POSTES de la acera (semáforos, PARE, carteles y farolas de calle);
     *  - REPARTIDAS: la primera es la primera candidata válida desde un punto de partida propio del parque (variacion());
     *    la segunda, la válida MÁS LEJANA a la primera (suele quedar en el brazo opuesto). Sin azar: siempre igual.
     */
    public static List<float[]> luminarias(int fila, int columna) {
        float cx = Mapa.centro(columna); // Centro del parque en X.
        float cz = Mapa.centro(fila); // Centro del parque en Z.
        List<float[]> arboles = arboles(fila, columna); // Lo que ya ocupa el césped.
        List<float[]> bancos = bancos(fila, columna);
        int[][] brazos = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}}; // Dirección {x, z} de cada brazo: norte, sur, oeste y este.
        List<float[]> validas = new ArrayList<>(); // Candidatas que cumplen todas las reglas, en orden.
        int total = brazos.length * DISTANCIAS_LUMINARIA.length * 2; // 24 candidatas.
        int inicio = entero(fila, columna, 11, 0, total - 1); // Por dónde empieza este parque.
        for (int k = 0; k < total; k++) {
            int n = (inicio + k) % total; // Candidata número n.
            int[] brazo = brazos[n % brazos.length]; // Brazo de la cruz.
            float distancia = DISTANCIAS_LUMINARIA[(n / brazos.length) % DISTANCIAS_LUMINARIA.length]; // Del centro.
            int lado = n < total / 2 ? 1 : -1; // A un lado u otro del eje del sendero.
            // Perpendicular al brazo: (-z, x). Se suma el desvío hacia un costado del sendero.
            float x = cx + brazo[0] * distancia - brazo[1] * lado * DESVIO_LUMINARIA;
            float z = cz + brazo[1] * distancia + brazo[0] * lado * DESVIO_LUMINARIA;
            if (luminariaLibre(x, z, arboles, bancos)) {
                validas.add(new float[] {x, z});
            }
        }
        List<float[]> elegidas = new ArrayList<>(); // Resultado.
        for (int i = 0; i < luminariasPorParque() && !validas.isEmpty(); i++) {
            int mejor = 0; // La primera vez, la primera válida.
            double mejorDistancia = -1;
            for (int v = 0; i > 0 && v < validas.size(); v++) { // Desde la segunda: la más alejada de las ya elegidas.
                double cercana = Double.MAX_VALUE;
                for (float[] e : elegidas) {
                    cercana = Math.min(cercana, Math.hypot(validas.get(v)[0] - e[0], validas.get(v)[1] - e[1]));
                }
                if (cercana > mejorDistancia) { // Estrictamente mayor: en empates queda la primera.
                    mejor = v;
                    mejorDistancia = cercana;
                }
            }
            elegidas.add(validas.remove(mejor));
        }
        return elegidas; // Luminarias del parque.
    }

    /** Aplica las reglas de choque de luminarias(): árboles, bancos y postes de la acera. */
    private static boolean luminariaLibre(float x, float z, List<float[]> arboles, List<float[]> bancos) {
        for (float[] a : arboles) { // El globo (a 3 de altura) está entre las copas: se mide contra la copa real.
            if (Math.hypot(x - a[0], z - a[1]) < a[4] / 2 + DIAMETRO_GLOBO / 2 + MARGEN_LUMINARIA) {
                return false;
            }
        }
        for (float[] b : bancos) { // La base no se apoya sobre el banco.
            if (Math.hypot(x - b[0], z - b[1]) < MITAD_BANCO + ANCHO_BASE_GLOBO / 2 + MARGEN_LUMINARIA) {
                return false;
            }
        }
        return !cercaDeUnPoste(x, z, DIAMETRO_GLOBO / 2 + MARGEN_LUMINARIA_POSTES); // Semáforos, PARE, carteles, farolas.
    }

    /** Todas las luminarias de la ciudad como centro {x, y, z} del globo, parque por parque (orden de Mapa.parques()). */
    private static List<float[]> calcularLuminarias() {
        List<float[]> lista = new ArrayList<>();
        for (int[] celda : Mapa.parques()) {
            for (float[] l : luminarias(celda[0], celda[1])) {
                lista.add(new float[] {l[0], TOPE_CESPED + ALTURA_GLOBO, l[1]}); // El mismo punto que dibuja dibujarLuminaria().
            }
        }
        return lista;
    }

    // ==================== 4. DIBUJO ====================

    private final Shader shader; // Programa que recibe el interruptor de emisión.
    private final Cubo cubo; // Senderos y bancos.
    private final Figuras figuras; // Esfera, cilindro y cono.
    private final List<List<float[]>> arbolesPorParque = new ArrayList<>(); // Disposiciones calculadas una vez.
    private final List<List<float[]>> bancosPorParque = new ArrayList<>(); // Bancos calculados una vez.
    private final List<List<float[]>> luminariasDeCadaParque = new ArrayList<>(); // Luminarias globo calculadas una vez.
    private final List<int[]> celdas = Mapa.parques(); // Parques del mapa, en el mismo orden que las listas anteriores.

    /** Calcula la disposición de todos los parques una sola vez (no hay azar por cuadro). */
    public Parque(Shader shader, Cubo cubo, Figuras figuras) {
        this.shader = shader; // Guarda el programa para cambiar uEmision.
        this.cubo = cubo; // Guarda la geometría de cajas.
        this.figuras = figuras; // Guarda las figuras redondeadas.
        for (int[] celda : celdas) { // Recorre los parques.
            arbolesPorParque.add(arboles(celda[0], celda[1])); // Árboles de este parque.
            bancosPorParque.add(bancos(celda[0], celda[1])); // Bancos de este parque.
            luminariasDeCadaParque.add(luminarias(celda[0], celda[1])); // Luminarias globo de este parque.
        }
    }

    /** Dibuja el parque de la celda (fila, columna): senderos, fuente, árboles, bancos y luminarias. */
    public void dibujar(int fila, int columna, boolean noche) {
        int indice = indiceDe(fila, columna); // Posición del parque en las listas calculadas.
        float x = Mapa.centro(columna); // Centro del parque en X.
        float z = Mapa.centro(fila); // Centro del parque en Z.
        dibujarSenderos(x, z); // Cruz de caminos.
        dibujarFuente(x, z, noche); // Fuente en el centro.
        for (float[] arbol : arbolesPorParque.get(indice)) { // Árboles propios del parque.
            dibujarArbol(figuras, arbol, TOPE_CESPED); // Pino o frondoso, apoyado sobre el césped.
        }
        for (float[] banco : bancosPorParque.get(indice)) { // Bancos propios del parque.
            dibujarBanco(banco[0], banco[1], banco[2]); // Mirando a la fuente.
        }
        for (float[] luminaria : luminariasDeCadaParque.get(indice)) { // Luminarias globo en el borde de los senderos.
            dibujarLuminaria(luminaria[0], luminaria[1], noche); // Encendida solo de noche.
        }
    }

    /**
     * Luminaria GLOBO: base, poste fino, portaglobo y esfera. El centro de la esfera es el mismo punto que
     * calcularLuminarias() guarda en LUMINARIAS y el shader usa como luz puntual. De noche el globo es emisivo.
     */
    private void dibujarLuminaria(float x, float z, boolean noche) {
        float[] c = COLOR_POSTE_GLOBO; // Poste, base y portaglobo del mismo color.
        float yGlobo = TOPE_CESPED + ALTURA_GLOBO; // Centro del globo.
        float pie = TOPE_CESPED + ALTO_BASE_GLOBO; // Donde termina la base.
        float yAnillo = yGlobo - DIAMETRO_GLOBO / 2 - ALTO_ANILLO_GLOBO / 2 + 0.03f; // El globo se apoya (y se hunde apenas) en el anillo.
        float altoPoste = yAnillo - pie; // Del zócalo al portaglobo.
        figuras.cilindro.dibujar(x, TOPE_CESPED + ALTO_BASE_GLOBO / 2, z, ANCHO_BASE_GLOBO, ALTO_BASE_GLOBO, ANCHO_BASE_GLOBO, c[0], c[1], c[2]); // Base.
        figuras.cilindro.dibujar(x, pie + altoPoste / 2, z, ANCHO_POSTE_GLOBO, altoPoste, ANCHO_POSTE_GLOBO, c[0], c[1], c[2]); // Poste.
        figuras.cilindro.dibujar(x, yAnillo, z, ANCHO_ANILLO_GLOBO, ALTO_ANILLO_GLOBO, ANCHO_ANILLO_GLOBO, c[0], c[1], c[2]); // Portaglobo.
        float[] globo = noche ? COLOR_GLOBO_NOCHE : COLOR_GLOBO_DIA; // Encendido solo de noche, como las farolas.
        shader.entero("uEmision", noche ? 1 : 0); // De noche el globo tiene luz propia.
        figuras.esfera.dibujar(x, yGlobo, z, DIAMETRO_GLOBO, DIAMETRO_GLOBO, DIAMETRO_GLOBO, globo[0], globo[1], globo[2]); // Globo.
        shader.entero("uEmision", 0); // Restablece el material normal.
    }

    /** Busca la posición del parque en la lista de celdas. */
    private int indiceDe(int fila, int columna) {
        for (int i = 0; i < celdas.size(); i++) { // Recorre los parques.
            if (celdas.get(i)[0] == fila && celdas.get(i)[1] == columna) { // Coincide.
                return i; // Posición encontrada.
            }
        }
        throw new IllegalArgumentException("No hay parque en " + fila + "," + columna); // Decoracion solo llama con parques.
    }

    /** Dos senderos beige en cruz que unen los cuatro lados del parque con el centro. */
    private void dibujarSenderos(float x, float z) {
        float y = TOPE_CESPED + GROSOR_SENDERO / 2; // Apoyado sobre el césped, apenas elevado.
        float largo = 2 * MITAD_CESPED; // De borde a borde del césped.
        cubo.caja(x, y, z, largo, GROSOR_SENDERO, ANCHO_SENDERO, COLOR_SENDERO[0], COLOR_SENDERO[1], COLOR_SENDERO[2]); // Sendero oeste-este.
        cubo.caja(x, y, z, ANCHO_SENDERO, GROSOR_SENDERO, largo, COLOR_SENDERO[0], COLOR_SENDERO[1], COLOR_SENDERO[2]); // Sendero norte-sur (el cruce queda bajo la fuente).
    }

    /** Fuente: base gris, agua azul (levemente emisiva de noche), pilar con plato y chorro. */
    private void dibujarFuente(float x, float z, boolean noche) {
        float altoBase = 0.45f; // Alto del borde de piedra.
        figuras.cilindro.dibujar(x, TOPE_CESPED + altoBase / 2, z, 2 * RADIO_FUENTE, altoBase, 2 * RADIO_FUENTE, 0.60f, 0.62f, 0.65f); // Base de piedra.
        float[] agua = noche ? COLOR_AGUA_NOCHE : COLOR_AGUA; // Color del agua según el momento del día.
        if (noche) { // De noche el agua brilla un poco, como iluminada desde adentro.
            shader.entero("uEmision", 1); // Color propio, sin depender de las luces.
        }
        float alturaAgua = TOPE_CESPED + altoBase + 0.01f; // Apenas sobre el borde: se ve el anillo gris alrededor.
        figuras.cilindro.dibujar(x, alturaAgua, z, 2 * RADIO_FUENTE - 0.4f, 0.04f, 2 * RADIO_FUENTE - 0.4f, agua[0], agua[1], agua[2]); // Espejo de agua.
        shader.entero("uEmision", 0); // El pilar es de piedra: recibe luz normal.
        figuras.cilindro.dibujar(x, alturaAgua + 0.4f, z, 0.3f, 0.8f, 0.3f, 0.60f, 0.62f, 0.65f); // Pilar central.
        figuras.cilindro.dibujar(x, alturaAgua + 0.82f, z, 0.8f, 0.1f, 0.8f, 0.60f, 0.62f, 0.65f); // Plato superior.
        if (noche) { // El chorro también brilla de noche.
            shader.entero("uEmision", 1); // Color propio.
        }
        figuras.cilindro.dibujar(x, alturaAgua + 1.2f, z, 0.12f, 0.7f, 0.12f, agua[0] + 0.2f, agua[1] + 0.2f, agua[2]); // Chorro que sube.
        figuras.esfera.dibujar(x, alturaAgua + 1.6f, z, 0.35f, 0.3f, 0.35f, agua[0] + 0.3f, agua[1] + 0.3f, agua[2]); // Salpicadura en la punta.
        shader.entero("uEmision", 0); // Restablece el material normal para el siguiente objeto.
    }

    /**
     * Dibuja un árbol {x, z, tipo, alturaTronco, diametroCopa, verde, giro} apoyado a la altura base: pino o frondoso
     * según su tipo. Es estático para que Entorno reutilice los mismos árboles en el campo que rodea la ciudad.
     */
    public static void dibujarArbol(Figuras figuras, float[] arbol, float base) {
        if (arbol[2] == PINO) { // Según el tipo sorteado.
            dibujarPino(figuras, arbol, base); // Tronco y conos.
        } else { // Árbol frondoso.
            dibujarFrondoso(figuras, arbol, base); // Tronco y esferas.
        }
    }

    /**
     * Árbol frondoso: tronco cilíndrico y copa de tres esferas de distinto tamaño, desplazadas entre sí.
     * Las medidas verticales de la copa se escriben en "copas" (múltiplos del diámetro) y se estiran con k: sin estirar,
     * la copa sube ALTO_COPA_FRONDOSO copas por encima del tronco (0.35 + 0.30 + 0.65 / 2 = 0.975); con
     * k = altoCopa / (0.975 · copa) la punta queda justo en alturaTronco + altoCopa, sin cambiar el ancho.
     */
    private static void dibujarFrondoso(Figuras figuras, float[] a, float base) {
        float x = a[0]; // Posición del tronco en X.
        float z = a[1]; // Posición del tronco en Z.
        float altoTronco = a[3]; // Alto del tronco.
        float copa = a[4]; // Diámetro (ancho) de la esfera principal.
        float giro = a[6]; // Orientación de la copa.
        float k = a[ALTO_COPA] / (ALTO_COPA_FRONDOSO * copa); // Cuánto se estira la copa hacia arriba.
        float v = copa * k; // Una "copa" en vertical.
        figuras.cilindro.dibujar(x, base + altoTronco / 2, z, 0.34f, altoTronco, 0.34f, 0.38f, 0.22f, 0.12f); // Dibuja el tronco marrón.
        float r = 0.10f + 0.08f * a[5]; // Tono de verde: un poco de rojo...
        float g = 0.50f - 0.15f * a[5]; // ...más o menos verde según el árbol...
        float b = 0.16f + 0.06f * a[5]; // ...y algo de azul.
        float centroCopa = base + altoTronco + v * 0.35f; // La copa abraza el extremo del tronco.
        figuras.esfera.dibujar(x, centroCopa, z, copa, v * 0.9f, copa, r, g, b); // Esfera principal.
        float dx = (float) Math.cos(giro) * 0.35f; // Desplazamiento de las esferas menores, girado por árbol.
        float dz = (float) Math.sin(giro) * 0.35f; // Igual en Z.
        figuras.esfera.dibujar(x + dx, centroCopa + v * 0.3f, z + dz, copa * 0.7f, v * 0.65f, copa * 0.7f, r + 0.03f, g + 0.05f, b); // Esfera alta, más clara: su punta es la del árbol.
        figuras.esfera.dibujar(x - dx, centroCopa - v * 0.1f, z - dz, copa * 0.6f, v * 0.55f, copa * 0.6f, r, g - 0.06f, b); // Esfera baja, más oscura.
    }

    /** Cuántas "copas" sube la copa del frondoso sin estirar: centro 0.35 + esfera alta 0.30 + su mitad 0.325. */
    private static final float ALTO_COPA_FRONDOSO = 0.35f + 0.30f + 0.65f / 2;
    /** Alto de cada cono del pino, de abajo hacia arriba (sin estirar); cada uno se apoya a SOLAPE_CONOS del anterior. */
    private static final float[] ALTOS_CONOS = {1.3f, 1.1f, 0.9f};
    private static final float SOLAPE_CONOS = 0.55f; // El siguiente cono empieza al 55 % del anterior: se superponen.
    /** Alto de la pila de conos sin estirar: 1.3 · 0.55 + 1.1 · 0.55 + 0.9 = 2.22. */
    private static final float ALTO_PILA_CONOS = (ALTOS_CONOS[0] + ALTOS_CONOS[1]) * SOLAPE_CONOS + ALTOS_CONOS[2];

    /** Pino: tronco cilíndrico y tres conos apilados que se achican hacia arriba; la pila se estira hasta altoCopa. */
    private static void dibujarPino(Figuras figuras, float[] a, float base) {
        float x = a[0]; // Posición del tronco en X.
        float z = a[1]; // Posición del tronco en Z.
        float altoTronco = a[3]; // Alto del tronco.
        float anchoCono = a[4]; // Diámetro del cono inferior.
        float k = a[ALTO_COPA] / ALTO_PILA_CONOS; // Estiramiento vertical: la punta queda en alturaTronco + altoCopa.
        figuras.cilindro.dibujar(x, base + altoTronco / 2, z, 0.3f, altoTronco, 0.3f, 0.36f, 0.22f, 0.13f); // Tronco.
        float r = 0.06f + 0.04f * a[5]; // Verde oscuro de pino...
        float g = 0.38f - 0.10f * a[5]; // ...que varía por árbol...
        float b = 0.16f + 0.03f * a[5]; // ...con un poco de azul.
        float[] escalas = {1.0f, 0.72f, 0.46f}; // Cada cono es más angosto que el de abajo.
        float y = base + altoTronco; // Donde empieza el primer cono.
        for (int i = 0; i < escalas.length; i++) { // Apila los conos.
            float alto = ALTOS_CONOS[i] * k; // Alto de este cono, estirado.
            figuras.cono.dibujar(x, y + alto / 2, z, anchoCono * escalas[i], alto, anchoCono * escalas[i], r, g + 0.04f * i, b); // Cono, un poco más claro arriba.
            y += alto * SOLAPE_CONOS; // El siguiente se apoya sobre este: los conos se superponen.
        }
    }

    /** Banco de madera con patas, orientado con su frente (-Z local) hacia la fuente. */
    private void dibujarBanco(float x, float z, float angulo) {
        float y = TOPE_CESPED; // El banco se apoya sobre el césped.
        pieza(x, z, angulo, 0, y + 0.45f, 0, 1.6f, 0.1f, 0.5f, 0.55f, 0.30f, 0.13f); // Dibuja el asiento de madera del banco.
        pieza(x, z, angulo, 0, y + 0.75f, 0.24f, 1.6f, 0.45f, 0.08f, 0.55f, 0.30f, 0.13f); // Dibuja el respaldo detrás del asiento (+Z local: lejos de la fuente).
        for (int lado = -1; lado <= 1; lado += 2) { // Patas izquierdas y derechas.
            for (int fondo = -1; fondo <= 1; fondo += 2) { // Patas delanteras y traseras.
                pieza(x, z, angulo, lado * 0.7f, y + 0.2f, fondo * 0.18f, 0.08f, 0.4f, 0.08f, 0.22f, 0.24f, 0.24f); // Dibuja el soporte oscuro del banco.
            }
        }
    }

    /** Dibuja una caja en coordenadas locales del banco, girada con él (misma transformación que Auto.pieza). */
    private void pieza(float x, float z, float angulo, float lx, float ly, float lz, float sx, float sy, float sz, float r, float g, float b) {
        float coseno = (float) Math.cos(angulo); // Coseno de la orientación.
        float seno = (float) Math.sin(angulo); // Seno de la orientación.
        float mundoX = x + coseno * lx + seno * lz; // Gira la posición local y la traslada.
        float mundoZ = z - seno * lx + coseno * lz; // Igual en Z.
        cubo.cajaGirada(mundoX, ly, mundoZ, sx, sy, sz, r, g, b, angulo); // Caja orientada con el banco.
    }
}
