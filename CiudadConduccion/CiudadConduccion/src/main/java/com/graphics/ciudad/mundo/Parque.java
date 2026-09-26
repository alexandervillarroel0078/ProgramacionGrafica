package com.graphics.ciudad.mundo; // Agrupa lo que forma la ciudad: mapa, edificios, decoración y señales.

import com.graphics.ciudad.iluminacion.Iluminacion; // Postes de farola: los árboles no deben taparlos.
import com.graphics.ciudad.motor.Cubo; // Senderos y bancos.
import com.graphics.ciudad.motor.Figuras; // Esfera, cilindro y cono para árboles y fuente.
import com.graphics.ciudad.motor.Shader; // Emisión del agua de noche.
import java.util.ArrayList; // Listas de árboles y bancos.
import java.util.List; // Tipo de esas listas.

/**
 * PARQUE: contenido de una celda de parque.
 * Responsable de: calcular (una vez, sin azar por cuadro) dónde van los árboles y los bancos de cada parque, y dibujar
 * senderos en cruz, una fuente central, 4 a 6 árboles (frondosos y pinos) y 2 a 4 bancos que miran a la fuente.
 * VARIACIÓN DETERMINÍSTICA: cada parque usa su fila y columna para "sortear" disposición, tipo de árbol, altura, tamaño
 * de copa y tono de verde con variacion(), una función que siempre da el mismo número para los mismos datos. Así los
 * parques son distintos entre sí, pero cada uno se ve igual en todos los cuadros y en cada ejecución.
 * ESCALA: el auto mide ≈1.4 de alto y 2.6 de largo, los edificios 5 a 13; los árboles miden entre 3 y 4.5 y la copa
 * no supera un cuarto del ancho del parque (COPA_MAXIMA = 2.5).
 * Todo queda dentro de la celda del parque: nada invade la calle ni participa en colisiones (Colisiones ya bloquea la
 * manzana entera). Los árboles evitan los postes de semáforos, PARE y farolas que están en la acera del parque.
 * Se comunica con: Decoracion (lo llama para cada celda de parque), Figuras y Cubo (dibujo), Shader (emisión),
 * Mapa (centro de la celda), Senalizacion e Iluminacion (postes que hay que esquivar).
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
    public static final float SEPARACION_POSTES = 1.6f; // Un árbol no se planta a menos de esto de un semáforo, PARE o farola.
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

    // ==================== 2. VARIACIÓN DETERMINÍSTICA ====================

    /**
     * Número "aleatorio" entre 0 y 1 que depende solo de sus datos: fracción de sen(combinación) · 43758.5453.
     * Es la misma función de hash que se usa en muchos shaders: sin estado, sin Random, siempre igual.
     */
    public static float variacion(int fila, int columna, int indice, int semilla) {
        double n = Math.sin(fila * 12.9898 + columna * 78.233 + indice * 37.719 + semilla * 4.581) * 43758.5453; // Mezcla los datos.
        return (float) (n - Math.floor(n)); // Se queda con la parte decimal: entre 0 y 1.
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
            if (cercaDeUnPoste(x, z)) { // Semáforo, PARE o farola en la acera cercana.
                continue; // Ese lugar se saltea.
            }
            int i = lista.size(); // Índice del árbol: distingue su variación de la de los demás.
            int tipo = variacion(fila, columna, i, 4) < proporcionPinos ? PINO : FRONDOSO; // Mezcla de tipos.
            float alturaTronco = tipo == PINO ? 0.8f + 0.4f * variacion(fila, columna, i, 5) : 1.4f + 0.6f * variacion(fila, columna, i, 5); // Pinos de tronco corto.
            float diametroCopa = 1.8f + (COPA_MAXIMA - 0.1f - 1.8f) * variacion(fila, columna, i, 6); // Entre 1.8 y 2.4: nunca más de 1/4 del parque.
            float verde = variacion(fila, columna, i, 7); // Tono de verde (0 = claro, 1 = oscuro).
            float giro = (float) (2 * Math.PI * variacion(fila, columna, i, 8)); // Orientación de la copa: rompe la simetría.
            lista.add(new float[] {x, z, tipo, alturaTronco, diametroCopa, verde, giro}); // Guarda el árbol.
        }
        return lista; // Árboles del parque.
    }

    /** Indica si (x, z) está a menos de SEPARACION_POSTES de un semáforo, un PARE o un poste de farola. */
    private static boolean cercaDeUnPoste(float x, float z) {
        for (float[] s : Senalizacion.SEMAFOROS) { // Semáforos del Centro.
            if (Math.hypot(x - s[0], z - s[1]) < SEPARACION_POSTES) { // Demasiado cerca.
                return true; // Se descarta el lugar.
            }
        }
        for (float[] p : Senalizacion.PARES) { // Señales de PARE.
            if (Math.hypot(x - p[0], z - p[1]) < SEPARACION_POSTES) { // Demasiado cerca.
                return true; // Se descarta el lugar.
            }
        }
        for (float[] poste : Iluminacion.POSTES) { // Postes de farola.
            if (Math.hypot(x - poste[0], z - poste[1]) < SEPARACION_POSTES) { // Demasiado cerca.
                return true; // Se descarta el lugar.
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

    // ==================== 4. DIBUJO ====================

    private final Shader shader; // Programa que recibe el interruptor de emisión.
    private final Cubo cubo; // Senderos y bancos.
    private final Figuras figuras; // Esfera, cilindro y cono.
    private final List<List<float[]>> arbolesPorParque = new ArrayList<>(); // Disposiciones calculadas una vez.
    private final List<List<float[]>> bancosPorParque = new ArrayList<>(); // Bancos calculados una vez.
    private final List<int[]> celdas = Mapa.parques(); // Parques del mapa, en el mismo orden que las listas anteriores.

    /** Calcula la disposición de todos los parques una sola vez (no hay azar por cuadro). */
    public Parque(Shader shader, Cubo cubo, Figuras figuras) {
        this.shader = shader; // Guarda el programa para cambiar uEmision.
        this.cubo = cubo; // Guarda la geometría de cajas.
        this.figuras = figuras; // Guarda las figuras redondeadas.
        for (int[] celda : celdas) { // Recorre los parques.
            arbolesPorParque.add(arboles(celda[0], celda[1])); // Árboles de este parque.
            bancosPorParque.add(bancos(celda[0], celda[1])); // Bancos de este parque.
        }
    }

    /** Dibuja el parque de la celda (fila, columna): senderos, fuente, árboles y bancos. */
    public void dibujar(int fila, int columna, boolean noche) {
        int indice = indiceDe(fila, columna); // Posición del parque en las listas calculadas.
        float x = Mapa.centro(columna); // Centro del parque en X.
        float z = Mapa.centro(fila); // Centro del parque en Z.
        dibujarSenderos(x, z); // Cruz de caminos.
        dibujarFuente(x, z, noche); // Fuente en el centro.
        for (float[] arbol : arbolesPorParque.get(indice)) { // Árboles propios del parque.
            if (arbol[2] == PINO) { // Según el tipo sorteado.
                dibujarPino(arbol); // Tronco y conos.
            } else { // Árbol frondoso.
                dibujarFrondoso(arbol); // Tronco y esferas.
            }
        }
        for (float[] banco : bancosPorParque.get(indice)) { // Bancos propios del parque.
            dibujarBanco(banco[0], banco[1], banco[2]); // Mirando a la fuente.
        }
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

    /** Árbol frondoso: tronco cilíndrico fino y copa de tres esferas de distinto tamaño, desplazadas entre sí. */
    private void dibujarFrondoso(float[] a) {
        float x = a[0]; // Posición del tronco en X.
        float z = a[1]; // Posición del tronco en Z.
        float altoTronco = a[3]; // Alto del tronco.
        float copa = a[4]; // Diámetro de la esfera principal.
        float giro = a[6]; // Orientación de la copa.
        figuras.cilindro.dibujar(x, TOPE_CESPED + altoTronco / 2, z, 0.28f, altoTronco, 0.28f, 0.38f, 0.22f, 0.12f); // Dibuja el tronco marrón.
        float r = 0.10f + 0.08f * a[5]; // Tono de verde: un poco de rojo...
        float g = 0.50f - 0.15f * a[5]; // ...más o menos verde según el árbol...
        float b = 0.16f + 0.06f * a[5]; // ...y algo de azul.
        float centroCopa = TOPE_CESPED + altoTronco + copa * 0.35f; // La copa abraza el extremo del tronco.
        figuras.esfera.dibujar(x, centroCopa, z, copa, copa * 0.9f, copa, r, g, b); // Esfera principal, un poco achatada.
        float dx = (float) Math.cos(giro) * 0.35f; // Desplazamiento de las esferas menores, girado por árbol.
        float dz = (float) Math.sin(giro) * 0.35f; // Igual en Z.
        figuras.esfera.dibujar(x + dx, centroCopa + copa * 0.3f, z + dz, copa * 0.7f, copa * 0.65f, copa * 0.7f, r + 0.03f, g + 0.05f, b); // Esfera alta, más clara.
        figuras.esfera.dibujar(x - dx, centroCopa - copa * 0.1f, z - dz, copa * 0.6f, copa * 0.55f, copa * 0.6f, r, g - 0.06f, b); // Esfera baja, más oscura.
    }

    /** Pino: tronco cilíndrico y tres conos apilados que se achican hacia arriba. */
    private void dibujarPino(float[] a) {
        float x = a[0]; // Posición del tronco en X.
        float z = a[1]; // Posición del tronco en Z.
        float altoTronco = a[3]; // Alto del tronco.
        float base = a[4]; // Diámetro del cono inferior.
        figuras.cilindro.dibujar(x, TOPE_CESPED + altoTronco / 2, z, 0.3f, altoTronco, 0.3f, 0.36f, 0.22f, 0.13f); // Tronco.
        float r = 0.06f + 0.04f * a[5]; // Verde oscuro de pino...
        float g = 0.38f - 0.10f * a[5]; // ...que varía por árbol...
        float b = 0.16f + 0.03f * a[5]; // ...con un poco de azul.
        float[] escalas = {1.0f, 0.72f, 0.46f}; // Cada cono es más angosto que el de abajo.
        float[] altos = {1.3f, 1.1f, 0.9f}; // Y más bajo.
        float y = TOPE_CESPED + altoTronco; // Donde empieza el primer cono.
        for (int i = 0; i < escalas.length; i++) { // Apila los conos.
            float alto = altos[i]; // Alto de este cono.
            figuras.cono.dibujar(x, y + alto / 2, z, base * escalas[i], alto, base * escalas[i], r, g + 0.04f * i, b); // Cono, un poco más claro arriba.
            y += alto * 0.55f; // El siguiente se apoya a mitad del anterior: los conos se superponen.
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
