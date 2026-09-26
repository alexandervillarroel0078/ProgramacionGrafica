package com.graphics.ciudad.mundo; // Agrupa lo que forma la ciudad: mapa, edificios, decoración y señales.

import com.graphics.ciudad.motor.Cubo; // Dibuja postes, placas y símbolos.
import com.graphics.ciudad.motor.Shader; // Lo necesita Semaforo para la emisión de las bombillas.
import com.graphics.ciudad.motor.Texto; // Convierte el nombre de cada sector en rectángulos para el cartel.
import java.util.ArrayList; // Listas calculadas de semáforos y señales.
import java.util.Collections; // Publica las listas sin permitir modificarlas desde afuera.
import java.util.List; // Tipo de esas listas.

/**
 * SENALIZACION: señalización vial de la ciudad, ubicada con criterios viales reales.
 * Responsable de: decidir DÓNDE va cada semáforo, cada PARE y cada cartel de sector, y dibujarlos.
 *
 * CRITERIOS (circulación por la derecha):
 *  - SEMÁFOROS solo en las intersecciones del sector Centro (INTERSECCIONES_SEMAFORO): es donde se cruzan las
 *    avenidas con más tránsito; en los barrios basta con prioridad de paso (PARE). Hay un cabezal por acceso.
 *  - Cada cabezal va en la esquina de vereda a la DERECHA de la calle que llega: el conductor mira hacia adelante y a
 *    su derecha, y así la señal no queda tapada por el tránsito contrario. Se ubica al borde del paso peatonal
 *    (donde el auto se detiene) y sus luces miran hacia los autos que se acercan por ese acceso.
 *  - PARE en intersecciones SIN semáforo de los otros sectores (UBICACIONES_PARE), también a la derecha del acceso y
 *    mirando al auto. Nunca junto a un semáforo: dos señales que ordenan cosas distintas en el mismo cruce confunden.
 *  - Un CARTEL por sector en su entrada principal (CARTELES_SECTOR), verde con borde blanco como los carteles viales,
 *    con el mismo nombre que muestran el HUD y el minimapa.
 * Todo se apoya sobre la vereda, dentro de la celda de una manzana: nada ocupa la calle ni participa en colisiones.
 * Se comunica con: Mapa (intersecciones, accesos, sectores), Semaforo (dibuja cada cabezal y coordina colores),
 * Texto (letras del cartel), Cubo y Shader, y Decoracion (la llama en cada cuadro y usa INTERSECCIONES_SEMAFORO para
 * los pasos peatonales).
 * Limitación: Cubo solo gira alrededor de Y, así que el octógono del PARE se aproxima con rectángulos superpuestos.
 */
public class Senalizacion {

    // ==================== 1. CONSTANTES DE FORMA Y UBICACIÓN (valores ajustables) ====================
    public static final int SECTOR_SEMAFOROS = 0; // Índice de Mapa.SECTORES donde van los semáforos: 0 = Centro.
    public static final float MARGEN_VEREDA = 0.6f; // Distancia del cordón (borde de la calle) al poste, hacia adentro de la vereda.
    public static final float RETROCESO_SEMAFORO = Decoracion.LARGO_PASO; // El semáforo se para al borde del paso peatonal, donde frena el auto.
    public static final float RETROCESO_PARE = MARGEN_VEREDA; // El PARE va en la misma esquina del cruce.
    public static final float ALTURA_POSTE = 2.6f; // Alto del poste: la placa queda por encima del techo del auto.
    public static final float GROSOR_POSTE = 0.1f; // Sección cuadrada del poste.
    public static final float LADO_PARE = 0.9f; // Ancho total del octógono rojo del PARE.
    public static final float BORDE_PARE = 0.07f; // Grosor del borde blanco alrededor del octógono.
    public static final float GROSOR_PLACA = 0.06f; // Espesor de las placas.
    public static final float ANCHO_CARTEL = 2.6f; // Ancho de la placa verde de los carteles de sector.
    public static final float ALTO_CARTEL = 0.8f; // Alto de esa placa.
    public static final float BORDE_CARTEL = 0.08f; // Borde blanco del cartel.
    public static final float ALTURA_CARTEL = 2.4f; // Altura del centro de la placa sobre la vereda.
    public static final float[] COLOR_CARTEL = {0.05f, 0.42f, 0.20f}; // Verde vial.
    private static final float ALTURA_ACERA = 0.3f; // Las señales se apoyan sobre la acera, que Ciudad dibuja con 0.3 de alto.

    // ==================== 2. LISTAS DE UBICACIONES ====================
    /** Intersecciones con semáforo {fila, columna}: todas las del sector Centro (4 con este mapa). */
    public static final List<int[]> INTERSECCIONES_SEMAFORO = Collections.unmodifiableList(calcularInterseccionesSemaforo());

    /**
     * PARE: {fila, columna, dFila, dColumna} = intersección y acceso por el que llegan los autos que deben detenerse.
     * Se eligieron cruces sin semáforo de los cuatro sectores exteriores, deteniendo a quien viene del borde de la ciudad
     * hacia adentro (la calle secundaria); las demás calles de ese cruce tienen prioridad.
     */
    public static final int[][] UBICACIONES_PARE = {
        {2, 2, -1, 0}, // Barrio Norte: cruce (2,2), autos que bajan desde el norte.
        {2, 4, -1, 0}, // Barrio Norte: cruce (2,4), autos que bajan desde el norte.
        {2, 8, -1, 0}, // Barrio Norte: cruce (2,8), autos que bajan desde el norte.
        {8, 2, 1, 0}, // Parque Sur: cruce (8,2), autos que suben desde el sur.
        {8, 6, 1, 0}, // Parque Sur: cruce (8,6), autos que suben desde el sur.
        {8, 8, 1, 0}, // Parque Sur: cruce (8,8), autos que suben desde el sur.
        {4, 2, 0, -1}, // Zona Oeste: cruce (4,2), autos que llegan desde el oeste.
        {6, 2, 0, -1}, // Zona Oeste: cruce (6,2), autos que llegan desde el oeste.
        {4, 8, 0, 1}, // Zona Este: cruce (4,8), autos que llegan desde el este.
        {6, 8, 0, 1} // Zona Este: cruce (6,8), autos que llegan desde el este.
    };

    /**
     * Carteles de sector {índiceSector, x, z, ángulo}: en la vereda derecha de la calle por la que se entra al sector,
     * sobre la franja de vereda paralela a la placa, mirando a los autos que entran (ángulo con la convención de Auto).
     */
    public static final float[][] CARTELES_SECTOR = {
        {0, 16.5f, 24.25f, (float) Math.PI}, // Centro: se entra desde el sur por la avenida X = 10; mira al sur.
        {1, 16.5f, -35.75f, (float) Math.PI}, // Barrio Norte: se entra hacia el norte por X = 10; mira al sur.
        {2, -16.5f, 35.75f, 0}, // Parque Sur: se entra hacia el sur por X = -10; mira al norte.
        {3, -35.75f, 3.5f, (float) (-Math.PI / 2)}, // Zona Oeste: se entra hacia el oeste por Z = 10; mira al este.
        {4, 35.75f, -3.5f, (float) (Math.PI / 2)} // Zona Este: se entra hacia el este por Z = -10; mira al oeste.
    };

    /** Semáforos calculados {x, z, ángulo, grupoNorteSur (1/0), fila, columna}: un cabezal por acceso. */
    public static final List<float[]> SEMAFOROS = Collections.unmodifiableList(calcularSemaforos());

    /** PARE calculados {x, z, ángulo} a partir de UBICACIONES_PARE. */
    public static final List<float[]> PARES = Collections.unmodifiableList(calcularPares());

    // ==================== 3. CÁLCULO DE POSICIONES ====================

    /**
     * Posición {x, z, ángulo} de una señal en la esquina de vereda a la DERECHA de un acceso.
     * n = dirección de la calle que llega vista desde el cruce (hacia afuera); los autos viajan en d = -n.
     * Con la convención de ejes del proyecto, la derecha de d = (dx, dz) es r = (-dz, dx).
     * Posición = centro del cruce + n · (media calle + retroceso) + r · (media calle + MARGEN_VEREDA): cae en la manzana
     * diagonal al cruce, sobre la vereda. El ángulo atan2(dx, dz) hace que el frente (-Z local) mire hacia -d, al auto.
     */
    public static float[] esquinaDerecha(int fila, int columna, int dFila, int dColumna, float retroceso) {
        float cruceX = Mapa.centro(columna); // Centro del cruce en X.
        float cruceZ = Mapa.centro(fila); // Centro del cruce en Z.
        float nX = dColumna; // Hacia afuera por el acceso, en X (las columnas son X).
        float nZ = dFila; // Hacia afuera por el acceso, en Z (las filas son Z).
        float dX = -nX; // Dirección en que viajan los autos que llegan, en X.
        float dZ = -nZ; // Dirección en que viajan, en Z.
        float derechaX = -dZ; // Derecha de d: (-dz, dx).
        float derechaZ = dX; // Componente Z de la derecha.
        float mitad = Mapa.TAM_CELDA / 2; // Media calle: del eje al cordón.
        float x = cruceX + nX * (mitad + retroceso) + derechaX * (mitad + MARGEN_VEREDA); // Atrás por el acceso y a la derecha.
        float z = cruceZ + nZ * (mitad + retroceso) + derechaZ * (mitad + MARGEN_VEREDA); // Igual en Z.
        float angulo = (float) Math.atan2(dX, dZ); // Frente hacia (-dX, -dZ): mira al auto que llega.
        return new float[] {x, z, angulo}; // Posición y orientación de la señal.
    }

    /** Todas las intersecciones del sector de los semáforos (el Centro). */
    private static List<int[]> calcularInterseccionesSemaforo() {
        List<int[]> lista = new ArrayList<>(); // Resultado.
        for (int[] cruce : Mapa.intersecciones()) { // Recorre todas las intersecciones del mapa.
            if (Mapa.sectorDeCelda(cruce[0], cruce[1]) == SECTOR_SEMAFOROS) { // Solo las del Centro.
                lista.add(cruce); // Esta intersección lleva semáforos.
            }
        }
        return lista; // Intersecciones con semáforo.
    }

    /** Un cabezal por cada acceso de cada intersección con semáforo. */
    private static List<float[]> calcularSemaforos() {
        List<float[]> lista = new ArrayList<>(); // Resultado.
        for (int[] cruce : INTERSECCIONES_SEMAFORO) { // Recorre los cruces con semáforo.
            for (int[] acceso : Mapa.accesos(cruce[0], cruce[1])) { // Normalmente cuatro accesos.
                float[] p = esquinaDerecha(cruce[0], cruce[1], acceso[0], acceso[1], RETROCESO_SEMAFORO); // Esquina derecha.
                float grupoNorteSur = acceso[0] != 0 ? 1 : 0; // Llega por una calle norte-sur si cambia la fila.
                lista.add(new float[] {p[0], p[1], p[2], grupoNorteSur, cruce[0], cruce[1]}); // Guarda el cabezal.
            }
        }
        return lista; // Cabezales de semáforo.
    }

    /** Convierte UBICACIONES_PARE en posiciones y orientaciones. */
    private static List<float[]> calcularPares() {
        List<float[]> lista = new ArrayList<>(); // Resultado.
        for (int[] pare : UBICACIONES_PARE) { // Recorre las ubicaciones elegidas.
            lista.add(esquinaDerecha(pare[0], pare[1], pare[2], pare[3], RETROCESO_PARE)); // Esquina derecha del acceso.
        }
        return lista; // PARE listos para dibujar.
    }

    // ==================== 4. DIBUJO ====================

    private final Cubo cubo; // Geometría compartida.
    private final Semaforo semaforo; // Dibuja cada cabezal y calcula su color.
    private final float[][][] letrasCartel = new float[CARTELES_SECTOR.length][][]; // Rectángulos de cada nombre (se calculan una vez).

    /** Recibe el shader y el cubo con el que se arman las señales. */
    public Senalizacion(Shader shader, Cubo cubo) {
        this.cubo = cubo; // Guarda la referencia para usarla en cada cuadro.
        this.semaforo = new Semaforo(shader, cubo); // Un mismo objeto dibuja todos los cabezales.
    }

    /** Dibuja semáforos, PARE y carteles; tiempo es el reloj global de Juego (anima los semáforos). */
    public void dibujar(float tiempo) {
        for (float[] s : SEMAFOROS) { // Recorre los cabezales.
            int activa = Semaforo.luzParaAcceso(tiempo, s[3] == 1); // Calcula una sola vez qué bombilla corresponde a este instante y a este grupo.
            semaforo.dibujar(s[0], s[1], s[2], activa); // Poste, carcasa y bombillas mirando al acceso.
        }
        for (float[] p : PARES) { // Recorre los PARE.
            dibujarPare(p[0], p[1], p[2]); // Octógono rojo con borde blanco.
        }
        for (int i = 0; i < CARTELES_SECTOR.length; i++) { // Recorre los carteles.
            dibujarCartel(i); // Placa verde con el nombre del sector.
        }
    }

    /** Dibuja una pieza en coordenadas locales de una señal (X a lo ancho, Y altura, Z hacia atrás), girada con ella. */
    private void pieza(float x, float z, float angulo, float lx, float ly, float lz, float sx, float sy, float sz, float r, float g, float b) {
        float coseno = (float) Math.cos(angulo); // Coseno de la orientación.
        float seno = (float) Math.sin(angulo); // Seno de la orientación.
        float mundoX = x + coseno * lx + seno * lz; // Misma transformación que Auto.pieza(): gira y traslada.
        float mundoZ = z - seno * lx + coseno * lz; // Posición Z en la ciudad.
        cubo.cajaGirada(mundoX, ly, mundoZ, sx, sy, sz, r, g, b, angulo); // Dibuja la pieza orientada.
    }

    /** Señal de PARE: poste gris y octógono rojo con borde blanco y franja blanca, mirando al auto que llega. */
    public void dibujarPare(float x, float z, float angulo) {
        dibujarPoste(x, z); // Poste sobre la acera.
        float centroPlaca = ALTURA_ACERA + ALTURA_POSTE - LADO_PARE / 2; // La placa termina a la altura del extremo del poste.
        float frente = -(GROSOR_POSTE / 2 + GROSOR_PLACA); // La placa va delante del poste (Z local negativa = hacia el auto).
        octogono(x, z, angulo, centroPlaca, frente, LADO_PARE + 2 * BORDE_PARE, 0.95f, 0.95f, 0.95f); // Borde blanco (detrás, más grande).
        octogono(x, z, angulo, centroPlaca, frente - GROSOR_PLACA, LADO_PARE, 0.80f, 0.06f, 0.06f); // Octógono rojo.
        pieza(x, z, angulo, 0, centroPlaca, frente - 2 * GROSOR_PLACA, LADO_PARE * 0.7f, LADO_PARE * 0.2f, 0.02f, 0.95f, 0.95f, 0.95f); // Franja blanca "PARE".
    }

    /**
     * Aproxima un octógono de ancho total "lado" con tres rectángulos: uno ancho y bajo, uno angosto y alto, y un
     * cuadrado central que rellena las esquinas en diagonal. El lado de un octógono regular mide ancho · 0.414.
     */
    private void octogono(float x, float z, float angulo, float centroY, float lz, float lado, float r, float g, float b) {
        float ladoRecto = lado * 0.414f; // Lado del octógono regular: ancho · (√2 - 1).
        float medio = (lado + ladoRecto) / 2; // Cuadrado que corta las esquinas a 45° aproximadamente.
        pieza(x, z, angulo, 0, centroY, lz, lado, ladoRecto, GROSOR_PLACA, r, g, b); // Franja horizontal.
        pieza(x, z, angulo, 0, centroY, lz, ladoRecto, lado, GROSOR_PLACA, r, g, b); // Franja vertical.
        pieza(x, z, angulo, 0, centroY, lz, medio * 0.9f, medio * 0.9f, GROSOR_PLACA, r, g, b); // Cuadrado que completa las diagonales.
    }

    /** Cartel de sector: dos postes, placa verde con borde blanco y el nombre del sector en letras blancas. */
    private void dibujarCartel(int indice) {
        float[] c = CARTELES_SECTOR[indice]; // {sector, x, z, ángulo}.
        String nombre = Mapa.NOMBRES_SECTORES[(int) c[0]]; // Mismo nombre que el HUD y el minimapa.
        float x = c[1]; // Centro del cartel en X.
        float z = c[2]; // Centro del cartel en Z.
        float angulo = c[3]; // Orientación: mira a los autos que entran al sector.
        float centroPlaca = ALTURA_ACERA + ALTURA_CARTEL; // Altura del centro de la placa.
        float separacionPostes = ANCHO_CARTEL / 2 - 0.15f; // Los postes quedan cerca de los extremos de la placa.
        for (int lado = -1; lado <= 1; lado += 2) { // Poste izquierdo y derecho.
            float altoPoste = centroPlaca - ALTURA_ACERA; // Del piso de la acera al centro de la placa.
            pieza(x, z, angulo, lado * separacionPostes, ALTURA_ACERA + altoPoste / 2, GROSOR_POSTE, GROSOR_POSTE, altoPoste, GROSOR_POSTE, 0.35f, 0.37f, 0.40f); // Poste.
        }
        pieza(x, z, angulo, 0, centroPlaca, 0, ANCHO_CARTEL + 2 * BORDE_CARTEL, ALTO_CARTEL + 2 * BORDE_CARTEL, GROSOR_PLACA, 0.95f, 0.95f, 0.95f); // Borde blanco.
        pieza(x, z, angulo, 0, centroPlaca, -GROSOR_PLACA, ANCHO_CARTEL, ALTO_CARTEL, GROSOR_PLACA, COLOR_CARTEL[0], COLOR_CARTEL[1], COLOR_CARTEL[2]); // Placa verde.
        if (letrasCartel[indice] == null) { // La primera vez convierte el nombre en rectángulos (STBEasyFont).
            letrasCartel[indice] = Texto.rectangulos(nombre); // Se guardan: el nombre no cambia.
        }
        float anchoTexto = Texto.ancho(nombre); // Ancho del nombre en unidades de STBEasyFont.
        float escala = Math.min((ANCHO_CARTEL - 0.3f) / anchoTexto, (ALTO_CARTEL - 0.2f) / Texto.ALTO_LETRA); // Entra a lo ancho y a lo alto.
        for (float[] letra : letrasCartel[indice]) { // Cada rectángulo de las letras.
            float centroX = ((letra[0] + letra[2]) / 2 - anchoTexto / 2) * escala; // Centro horizontal, con el texto centrado.
            float centroY = -((letra[1] + letra[3]) / 2 - Texto.ALTO_LETRA / 2) * escala; // STBEasyFont mide Y hacia abajo: se invierte.
            float ancho = Math.max((letra[2] - letra[0]) * escala, 0.01f); // Ancho del trazo.
            float alto = Math.max((letra[3] - letra[1]) * escala, 0.01f); // Alto del trazo.
            // El X local apunta a la IZQUIERDA de quien mira el cartel (la derecha del conductor es -X local), por eso se
            // invierte centroX: así el texto se lee de izquierda a derecha desde el auto.
            pieza(x, z, angulo, -centroX, centroPlaca + centroY, -2 * GROSOR_PLACA, ancho, alto, 0.02f, 1, 1, 1); // Trazo blanco.
        }
    }

    /** Poste gris oscuro apoyado sobre la acera (que está a 0.3 de altura). */
    private void dibujarPoste(float x, float z) {
        cubo.caja(x, ALTURA_ACERA + ALTURA_POSTE / 2, z, GROSOR_POSTE, ALTURA_POSTE, GROSOR_POSTE, 0.35f, 0.37f, 0.40f); // Poste vertical.
    }
}
