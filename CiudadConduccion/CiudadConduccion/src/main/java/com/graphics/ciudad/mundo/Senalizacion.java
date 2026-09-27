package com.graphics.ciudad.mundo; // Agrupa lo que forma la ciudad: mapa, edificios, decoración y señales.

import com.graphics.ciudad.motor.Cubo; // Dibuja postes, placas y símbolos.
import com.graphics.ciudad.motor.Figuras; // Cilindros de los semáforos.
import com.graphics.ciudad.motor.Shader; // Lo necesita Semaforo para la emisión de las lentes.
import com.graphics.ciudad.motor.Texto; // Convierte el nombre de cada sector en rectángulos para el cartel.
import java.util.ArrayList; // Listas calculadas de semáforos y señales.
import java.util.Collections; // Publica las listas sin permitir modificarlas desde afuera.
import java.util.Comparator; // Ordena los cruces candidatos a PARE por distancia al Centro.
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
 * Texto (letras del cartel), Cubo, Figuras y Shader, y Decoracion (la llama en cada cuadro y usa INTERSECCIONES_SEMAFORO para
 * los pasos peatonales).
 * Limitación: Cubo solo gira alrededor de Y, así que el octógono del PARE se aproxima con rectángulos superpuestos.
 */
public class Senalizacion {

    // ==================== 1. CONSTANTES DE FORMA Y UBICACIÓN (valores ajustables) ====================
    public static final int SECTOR_SEMAFOROS = 0; // Índice de Mapa.SECTORES donde van los semáforos: 0 = Centro.
    public static final float MARGEN_VEREDA = 0.6f; // Distancia del cordón (borde de la calle) al poste, hacia adentro de la vereda.
    // El auto se detiene ANTES del paso peatonal (no sobre él): semáforo y PARE van detrás de las franjas, al final
    // de la separación y del paso. Así el conductor mira la señal justo donde tiene que frenar.
    public static final float RETROCESO_SEMAFORO = Decoracion.SEPARACION_CRUCE + Decoracion.LARGO_PASO; // 3.5 desde el borde del cruce.
    public static final float RETROCESO_PARE = Decoracion.SEPARACION_CRUCE + Decoracion.LARGO_PASO; // Igual que el semáforo.
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
    public static final int MAX_PARE = 10; // Tope de señales de PARE en toda la ciudad (el enunciado pide entre 6 y 10).
    public static final float RETROCESO_CARTEL = 0.75f; // Del borde de la manzana (la esquina) al cartel, a lo largo de la calle.
    public static final float SEPARACION_CARTEL = 1.5f; // Del cordón al centro del cartel, hacia adentro de la vereda.

    // ==================== 2. LISTAS DE UBICACIONES ====================
    /** Intersecciones con semáforo {fila, columna}: todas las del sector Centro (4 con este mapa). */
    public static final List<int[]> INTERSECCIONES_SEMAFORO = Collections.unmodifiableList(calcularInterseccionesSemaforo());

    /**
     * Carteles de sector {índiceSector, x, z, ángulo}: en la vereda derecha de la ENTRADA PRINCIPAL del sector (ver
     * entradaDeSector()), en la primera manzana del sector, mirando a los autos que entran (ángulo con la convención de
     * Auto). Se calculan con cartelDeSector().
     */
    public static final float[][] CARTELES_SECTOR = calcularCarteles();

    /**
     * PARE: {fila, columna, dFila, dColumna} = intersección y acceso por el que llegan los autos que deben detenerse.
     * Se calculan con calcularUbicacionesPare(): los cruces sin semáforo más cercanos al Centro.
     */
    public static final int[][] UBICACIONES_PARE = calcularUbicacionesPare();

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

    // ==================== 3b. REGLAS: ENTRADAS, CARTELES Y PARE ====================
    // Todo se calcula desde Mapa: con otro tamaño de MAPA las señales se reubican solas con los mismos criterios.

    /**
     * DIRECCIÓN HACIA AFUERA de un sector {dFila, dColumna}: hacia dónde queda el borde de la ciudad visto desde el
     * Centro. Sale del centro del rectángulo del sector: Barrio Norte → norte, Parque Sur → sur, Zona Oeste → oeste,
     * Zona Este → este. El Centro no tiene "afuera": devuelve null.
     */
    static int[] direccionHaciaAfuera(int sector) {
        float[] r = Mapa.SECTORES[sector]; // {xMin, xMax, zMin, zMax}.
        float centroX = (r[0] + r[1]) / 2; // Centro del rectángulo.
        float centroZ = (r[2] + r[3]) / 2;
        if (Math.abs(centroX) < 1e-3f && Math.abs(centroZ) < 1e-3f) { // Rectángulo centrado en el origen: el Centro.
            return null;
        }
        if (Math.abs(centroZ) >= Math.abs(centroX)) { // Franja norte o sur: afuera cambia de fila.
            return new int[] {(int) Math.signum(centroZ), 0};
        }
        return new int[] {0, (int) Math.signum(centroX)}; // Franja oeste o este: afuera cambia de columna.
    }

    /**
     * ENTRADA PRINCIPAL de un sector {dFila, dColumna, calle}: el sentido en que viaja el auto y la calle por la que
     * entra (una columna si viaja de norte a sur o al revés, una fila si viaja de oeste a este o al revés).
     *  - SENTIDO: hacia afuera (al Barrio Norte se entra yendo al norte). Al Centro se entra desde el sur, yendo al
     *    norte, como el auto que sale de la esquina suroeste.
     *  - CALLE: el eje central si es una calle; si no, la avenida contigua al eje del lado DERECHO del conductor (se
     *    circula por la derecha, y el cartel va a la derecha). Con el mapa 11 × 11 el eje es la fila/columna 5, de
     *    manzanas, así que se usan las avenidas 4 y 6. Si esa calle no tiene una manzana del sector a la derecha para
     *    el cartel, se prueba la siguiente avenida hacia afuera.
     */
    static int[] entradaDeSector(int sector) {
        int[] d = direccionHaciaAfuera(sector); // Sentido de viaje.
        if (d == null) { // Centro.
            d = new int[] {-1, 0}; // Desde el sur, hacia el norte.
        }
        int lado = d[0] != 0 ? -d[0] : d[1]; // Derecha del conductor: columna -dFila, o fila dColumna.
        int eje = Mapa.MAPA.length / 2; // Fila/columna central.
        int n = Mapa.MAPA.length;
        for (int calle = eje % 2 == 0 ? eje : eje + lado; calle >= 0 && calle < n; calle += 2 * lado) { // Calles pares.
            int[] entrada = {d[0], d[1], calle};
            if (celdaDelCartel(entrada, sector) != null) { // Tiene dónde poner el cartel.
                return entrada;
            }
        }
        throw new IllegalStateException("El sector " + Mapa.NOMBRES_SECTORES[sector] + " no tiene una entrada con vereda para su cartel");
    }

    /** Celdas {fila, columna} de la calle de entrada, en el orden en que las recorre el auto (desde el borde opuesto). */
    private static List<int[]> recorridoDeEntrada(int[] entrada) {
        List<int[]> celdas = new ArrayList<>(); // Resultado.
        int n = Mapa.MAPA.length;
        boolean avanzaHaciaIndicesMayores = entrada[0] + entrada[1] > 0; // Hacia el sur o el este.
        for (int paso = 0; paso < n; paso++) { // Toda la calle, de punta a punta.
            int i = avanzaHaciaIndicesMayores ? paso : n - 1 - paso; // Índice a lo largo de la calle.
            celdas.add(entrada[0] != 0 ? new int[] {i, entrada[2]} : new int[] {entrada[2], i}); // Columna fija o fila fija.
        }
        return celdas;
    }

    /**
     * Celda de CALLE junto a la cual va el cartel: la primera del recorrido que tiene a su DERECHA una manzana (edificio
     * o parque) del sector. Es la primera manzana del sector que ve el conductor al entrar. null si no hay.
     */
    private static int[] celdaDelCartel(int[] entrada, int sector) {
        int derechaFila = entrada[1]; // Derecha de (dFila, dColumna) en celdas: (dColumna, -dFila).
        int derechaColumna = -entrada[0];
        for (int[] c : recorridoDeEntrada(entrada)) { // En el orden en que avanza el auto.
            int fila = c[0] + derechaFila; // Manzana a la derecha.
            int columna = c[1] + derechaColumna;
            boolean dentro = fila >= 0 && fila < Mapa.MAPA.length && columna >= 0 && columna < Mapa.MAPA.length;
            if (dentro && !Mapa.esCalle(fila, columna) && Mapa.sectorDeCelda(fila, columna) == sector) {
                return c; // Primera manzana del sector a la derecha.
            }
        }
        return null;
    }

    /**
     * CARTEL DE SECTOR {sector, x, z, ángulo}: en la vereda derecha de la entrada principal, sobre la primera manzana del
     * sector, RETROCESO_CARTEL después de la esquina (apenas se entra) y SEPARACION_CARTEL adentro del cordón. La placa
     * queda de frente a los autos que entran.
     */
    static float[] cartelDeSector(int sector) {
        int[] entrada = entradaDeSector(sector); // Sentido y calle.
        int[] c = celdaDelCartel(entrada, sector); // Tramo de calle junto a la manzana.
        float mitad = Mapa.TAM_CELDA / 2; // Media celda.
        int derechaFila = entrada[1]; // Derecha del conductor, en celdas.
        int derechaColumna = -entrada[0];
        // Hacia el costado: del eje de la calle a la vereda derecha. Hacia adelante: del centro del tramo a la esquina
        // por la que se entra (-mitad en el sentido de viaje) y RETROCESO_CARTEL hacia adentro de la manzana.
        float x = Mapa.centro(c[1]) + derechaColumna * (mitad + SEPARACION_CARTEL) - entrada[1] * (mitad - RETROCESO_CARTEL);
        float z = Mapa.centro(c[0]) + derechaFila * (mitad + SEPARACION_CARTEL) - entrada[0] * (mitad - RETROCESO_CARTEL);
        float angulo = (float) Math.atan2(entrada[1], entrada[0]); // Frente hacia -d: mira al auto (convención de Auto).
        return new float[] {sector, x, z, angulo};
    }

    /** Un cartel por sector, en el orden de Mapa.SECTORES. */
    private static float[][] calcularCarteles() {
        float[][] carteles = new float[Mapa.SECTORES.length][];
        for (int sector = 0; sector < carteles.length; sector++) {
            carteles[sector] = cartelDeSector(sector);
        }
        return carteles;
    }

    /** CRUCE DE ENTRADA de un sector: el primer cruce de su entrada principal que ya está dentro del sector. */
    static int[] cruceDeEntrada(int sector) {
        for (int[] c : recorridoDeEntrada(entradaDeSector(sector))) { // En el orden en que avanza el auto.
            if (Mapa.esInterseccion(c[0], c[1]) && Mapa.sectorDeCelda(c[0], c[1]) == sector) {
                return c;
            }
        }
        return null; // El sector no tiene cruces sobre su entrada.
    }

    /** Indica si (fila, columna) es el cruce de entrada de algún sector. */
    private static boolean esCruceDeEntrada(int fila, int columna) {
        for (int sector = 0; sector < Mapa.SECTORES.length; sector++) {
            int[] c = cruceDeEntrada(sector);
            if (c != null && c[0] == fila && c[1] == columna) {
                return true;
            }
        }
        return false;
    }

    /**
     * PARE: el cruce sin semáforo más cercano al Centro, y así sucesivamente hasta MAX_PARE. Criterios:
     *  - FUERA DEL CENTRO: allí hay semáforos, y dos señales que ordenan cosas distintas en un cruce confunden;
     *  - CRUCE INTERIOR: los del borde son esquinas y "T" de la calle perimetral, que rodea la ciudad y conserva la
     *    prioridad;
     *  - NO EN EL CRUCE DE ENTRADA de su sector: la entrada principal (la del cartel) es la avenida con prioridad;
     *  - se detiene a quien viene DESDE EL BORDE hacia adentro (la calle secundaria): el acceso es
     *    direccionHaciaAfuera() del sector; las demás calles del cruce tienen prioridad;
     *  - CERCANÍA AL CENTRO: ordenados por distancia al origen (en empates, de norte a sur y de oeste a este), se toman
     *    los primeros MAX_PARE: es donde hay más tránsito cruzado.
     */
    private static int[][] calcularUbicacionesPare() {
        List<int[]> candidatos = new ArrayList<>(); // {fila, columna, dFila, dColumna}.
        int ultima = Mapa.MAPA.length - 1; // Índice de la calle del borde este/sur.
        for (int[] c : Mapa.intersecciones()) { // De norte a sur y de oeste a este.
            int sector = Mapa.sectorDeCelda(c[0], c[1]);
            int[] afuera = direccionHaciaAfuera(sector); // null en el Centro.
            boolean interior = c[0] > 0 && c[0] < ultima && c[1] > 0 && c[1] < ultima;
            if (sector == SECTOR_SEMAFOROS || afuera == null || !interior || esCruceDeEntrada(c[0], c[1])) {
                continue; // No lleva PARE.
            }
            if (Mapa.esCalleSegura(c[0] + afuera[0], c[1] + afuera[1])) { // Existe la calle que llega desde el borde.
                candidatos.add(new int[] {c[0], c[1], afuera[0], afuera[1]});
            }
        }
        candidatos.sort(Comparator.comparingDouble(u -> Math.hypot(Mapa.centro(u[1]), Mapa.centro(u[0])))); // Estable.
        return candidatos.subList(0, Math.min(MAX_PARE, candidatos.size())).toArray(new int[0][]);
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

    /** Recibe el shader, el cubo con el que se arman las señales y las figuras redondeadas de los semáforos. */
    public Senalizacion(Shader shader, Cubo cubo, Figuras figuras) {
        this.cubo = cubo; // Guarda la referencia para usarla en cada cuadro.
        this.semaforo = new Semaforo(shader, cubo, figuras); // Un mismo objeto dibuja todos los cabezales.
    }

    /** Dibuja semáforos, PARE y carteles; tiempo es el reloj global de Juego (anima los semáforos). */
    public void dibujar(float tiempo) {
        for (float[] s : SEMAFOROS) { // Recorre los cabezales.
            int activa = Semaforo.luzParaAcceso(tiempo, s[3] == 1); // Calcula una sola vez qué bombilla corresponde a este instante y a este grupo.
            semaforo.dibujar(s[0], s[1], s[2], activa); // Base, poste, caja, lentes y viseras mirando al acceso.
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
