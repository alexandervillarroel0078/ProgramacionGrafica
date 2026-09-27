package com.graphics.ciudad.mundo; // Agrupa lo que forma la ciudad: mapa, edificios, decoración y señales.

import com.graphics.ciudad.motor.Cubo; // Ventanas, puertas, vidrieras y toldos son cajas finas.
import com.graphics.ciudad.motor.Shader; // Emisión de las ventanas encendidas y las vidrieras de noche.
import java.util.ArrayList; // Lista de ventanas de cada edificio.
import java.util.Collections; // Publica las listas sin permitir modificarlas.
import java.util.List; // Tipo de esas listas.

/**
 * FACHADA: todo lo que se ve pegado a las paredes de un edificio.
 * Responsable de:
 *  - PLANTA BAJA: en cada cara que da a una calle, "vidriera | puerta | vidriera": la puerta oscura va CENTRADA y hay
 *    una vidriera de negocio a cada lado, con un toldo de color inclinado sobre cada vidriera (sin tapar la puerta) que
 *    sobresale un poco hacia la vereda. Con la puerta en el centro, ninguna queda junto a una esquina: dos caras vecinas
 *    nunca tienen puertas pegadas. Entre la última vidriera y la esquina queda MARGEN_ESQUINA. El color del toldo
 *    (rojo, verde, azul o naranja, a veces a rayas) se elige por celda con Variacion.
 *  - VENTANAS en cada cara de cada volumen del edificio (Edificio.volumenes), con el patrón de su TipoEdificio: muchas
 *    y chicas en la torre, anchas en el bloque, pocas en la casa baja. Una ventana no se pone si la tapa otro volumen
 *    (la parte baja de un edificio doble, el nivel de abajo de un escalonado) ni sobre el negocio de la planta baja.
 *    De DÍA son vidrio claro (blanco-celeste grisáceo) y reciben la luz del sol como cualquier superficie; de NOCHE
 *    cerca de PORCENTAJE_VENTANAS_ENCENDIDAS están encendidas (emisivas, con tonos de TONOS_VENTANA) y el resto apagadas
 *    (azul-gris muy oscuro). Cada ventana decide con un hash de edificio, volumen, cara, piso y columna: la misma
 *    ventana siempre está igual, sin parpadeos.
 * El estado día/noche no se guarda aquí: Decoracion lo recibe de Juego, que lo lee de Iluminacion.esNoche().
 * Todo va sobre la pared o, como el toldo, sobre la vereda (VUELO_TOLDO): nada llega a la calzada ni cambia colisiones.
 * Se comunica con: Decoracion (la llama para cada edificio), Mapa (celdas, vecinos, ancho del edificio), Edificio y
 * TipoEdificio (volúmenes y patrón de ventanas), Variacion, Cubo y Shader.
 * Coordenadas de una cara: "u" recorre la pared de izquierda a derecha y "afuera" es la distancia desde el centro del
 * edificio en la dirección de la calle; ANCHO_EDIFICIO / 2 = 3.5 es el plano de la pared.
 */
public class Fachada {

    // ==================== 1. VENTANAS (valores ajustables) ====================
    public static final float PORCENTAJE_VENTANAS_ENCENDIDAS = 0.65f; // Fracción de ventanas encendidas de noche (65 %).
    public static final float[][] TONOS_VENTANA = { // Colores de las ventanas encendidas (emisivos).
        {1.00f, 0.80f, 0.42f}, // Amarillo cálido (lámpara incandescente).
        {1.00f, 0.92f, 0.74f}, // Blanco cálido.
        {1.00f, 0.66f, 0.36f}, // Anaranjado suave.
        {0.80f, 0.90f, 1.00f} // Blanco frío (tubo fluorescente o pantalla).
    };
    public static final float[] PESOS_TONOS = {0.40f, 0.30f, 0.20f, 0.10f}; // Qué tan frecuente es cada tono (suman 1).
    public static final float[] COLOR_VIDRIO_DIA = {0.72f, 0.80f, 0.86f}; // Vidrio de día: blanco-celeste grisáceo, sin emisión.
    public static final float[] COLOR_VENTANA_APAGADA = {0.06f, 0.08f, 0.13f}; // Ventana apagada de noche: azul-gris muy oscuro.
    public static final float PRIMER_PISO_Y = 1.7f; // Altura del centro de las ventanas del primer nivel (planta baja).
    // Cuántas ventanas, de qué tamaño y cada cuánto: TipoEdificio (columnasVentanas, separacionVentanas, anchoVentana,
    // altoVentana, alturaPiso). Los pisos se cuentan desde la acera para todo el edificio, así las filas de ventanas
    // de volúmenes vecinos quedan alineadas.
    public static final float MARGEN_VERTICAL = 0.35f; // Pared libre entre una ventana y la base o el tope de su volumen (y su losa).
    public static final float MARGEN_LATERAL = 0.3f; // Pared libre entre la última ventana y la esquina de su cara.
    public static final float TOPE_PLANTA_BAJA = 2.8f; // En una cara con negocio, ninguna ventana baja de acá (el toldo está en 2.45).

    // ==================== 2. PLANTA BAJA (valores ajustables) ====================
    public static final float[] COLOR_PUERTA = {0.14f, 0.11f, 0.09f}; // Madera muy oscura.
    public static final float ANCHO_PUERTA = 1.0f; // Ancho de la puerta.
    public static final float ALTO_PUERTA = 2.0f; // Alto de la puerta: más que el auto (≈1.4), menos que un piso.
    public static final float MARGEN_ESQUINA = 0.4f; // Pared libre entre cada esquina del edificio y la vidriera más cercana.
    public static final float SEPARACION_PUERTA = 0.2f; // Pared libre entre la puerta y cada vidriera (el marco).
    // Una cara mide ANCHO_EDIFICIO = 7. Desde el centro hacia cada esquina: media puerta (0.5), separación (0.2), vidriera
    // y margen (0.4). La vidriera ocupa lo que queda: 3.5 - 0.4 - 0.5 - 0.2 = 2.4.
    public static final float ANCHO_VIDRIERA = Mapa.ANCHO_EDIFICIO / 2 - MARGEN_ESQUINA - ANCHO_PUERTA / 2 - SEPARACION_PUERTA; // 2.4.
    public static final float CENTRO_VIDRIERA = ANCHO_PUERTA / 2 + SEPARACION_PUERTA + ANCHO_VIDRIERA / 2; // 1.9: centro de cada vidriera sobre el eje u.
    public static final float EXCESO_TOLDO = 0.05f; // El toldo sobresale esto a cada lado de su vidriera (sin llegar a la puerta ni al margen).
    public static final float ALTO_VIDRIERA = 1.4f; // Alto del vidrio.
    public static final float BASE_VIDRIERA = 0.6f; // Altura del borde inferior del vidrio (zócalo).
    public static final float[] COLOR_VIDRIERA_DIA = {0.55f, 0.70f, 0.80f}; // Vidrio de día, sin emisión.
    // De noche la vidriera es vidrio iluminado desde adentro: más clara abajo (donde está la mercadería bajo las
    // lámparas) y más apagada arriba. Como Cubo pinta cada caja de un solo color, el degradado se arma con
    // FRANJAS_VIDRIERA franjas horizontales cuyo color va de COLOR_VIDRIERA_NOCHE_ABAJO a COLOR_VIDRIERA_NOCHE_ARRIBA.
    public static final float[] COLOR_VIDRIERA_NOCHE_ABAJO = {0.86f, 0.70f, 0.46f}; // Franja inferior: cálida, emisiva y menos intensa que antes (1, 0.84, 0.58).
    public static final float[] COLOR_VIDRIERA_NOCHE_ARRIBA = {0.58f, 0.46f, 0.32f}; // Franja superior: la más tenue.
    public static final int FRANJAS_VIDRIERA = 6; // Más franjas = degradado más suave (y más cajas por vidriera).
    public static final float[][] COLORES_TOLDO = { // Colores posibles del toldo.
        {0.78f, 0.12f, 0.10f}, // Rojo.
        {0.10f, 0.50f, 0.22f}, // Verde.
        {0.12f, 0.28f, 0.70f}, // Azul.
        {0.95f, 0.50f, 0.10f} // Naranja.
    };
    public static final float PROBABILIDAD_RAYAS = 0.35f; // Fracción de edificios con toldo a rayas (color y blanco).
    public static final float ANCHO_RAYA = 0.6f; // Ancho de cada raya del toldo.
    public static final float VUELO_TOLDO = 0.7f; // Cuánto sobresale el toldo de la pared: queda sobre la vereda (1.5 de ancho).
    public static final float ALTURA_TOLDO = 2.45f; // Altura del borde del toldo pegado a la pared.
    public static final float CAIDA_TOLDO = 0.3f; // Cuánto baja el toldo desde la pared hasta su borde exterior.
    public static final int ESCALONES_TOLDO = 3; // Cubo solo gira en Y: la inclinación se arma con escalones que bajan.
    private static final float ALTURA_ACERA = 0.3f; // El edificio se apoya sobre la acera (0.3 de alto).
    private static final float SEPARACION_PARED = 0.01f; // Todo lo pegado a la pared se separa 0.01 para que no parpadee.
    private static final float GROSOR_PEGADO = 0.04f; // Espesor de ventanas, puertas y vidrieras.

    // ==================== 3. DECISIONES DETERMINÍSTICAS (sin azar por cuadro) ====================

    /** Una ventana ya ubicada: a qué volumen, cara, piso y columna pertenece, su centro en el mundo y su tamaño. */
    public static final class Ventana {
        public final int volumen, cara, piso, columna; // Identidad dentro del edificio.
        public final float x, z, y, ancho, alto; // Centro sobre la pared y medidas.

        Ventana(int volumen, int cara, int piso, int columna, float x, float z, float y, float ancho, float alto) {
            this.volumen = volumen;
            this.cara = cara;
            this.piso = piso;
            this.columna = columna;
            this.x = x;
            this.z = z;
            this.y = y;
            this.ancho = ancho;
            this.alto = alto;
        }

        /** Índice único dentro del edificio: combina volumen, cara, piso y columna (cada combinación da otro número). */
        public int indice() {
            return volumen * 10000 + cara * 1000 + piso * 10 + columna; // Menos de 10 columnas y de 100 pisos.
        }
    }

    /** Cuántas columnas del patrón entran en una cara de ese ancho, dejando MARGEN_LATERAL a cada lado (al menos una). */
    public static int columnasQueEntran(TipoEdificio tipo, float anchoCara) {
        int n = tipo.columnasVentanas; // Lo que pide el patrón.
        while (n > 1 && (n - 1) * tipo.separacionVentanas + tipo.anchoVentana > anchoCara - 2 * MARGEN_LATERAL) { // No entran.
            n--; // Una columna menos.
        }
        return n; // Columnas que se dibujan.
    }

    /**
     * Todas las ventanas del edificio de la celda: recorre cada volumen, cada cara y cada piso, y descarta las que
     * no entran en el volumen, las que quedan sobre el negocio de la planta baja y las que tapa otro volumen.
     */
    public static List<Ventana> ventanas(int fila, int columna) {
        TipoEdificio tipo = Edificio.tipo(fila, columna); // Patrón de ventanas.
        List<Edificio.Volumen> volumenes = Edificio.volumenes(fila, columna); // Cajas con paredes.
        float centroX = Mapa.centro(columna); // Centro de la manzana.
        float centroZ = Mapa.centro(fila);
        List<Ventana> lista = new ArrayList<>(); // Resultado.
        for (int iv = 0; iv < volumenes.size(); iv++) { // Cada volumen.
            Edificio.Volumen vol = volumenes.get(iv);
            for (int cara = 0; cara < Mapa.VECINOS.length; cara++) { // Norte, sur, oeste y este.
                int[] dir = Mapa.VECINOS[cara]; // {dFila, dColumna}: la columna es X y la fila es Z.
                boolean normalEnX = dir[1] != 0; // La cara mira al oeste o al este.
                float mitad = normalEnX ? vol.anchoX / 2 : vol.anchoZ / 2; // Del centro del volumen a esta pared.
                float anchoCara = normalEnX ? vol.anchoZ : vol.anchoX; // Largo de la pared.
                float hastaCentro = normalEnX ? (vol.x - centroX) * dir[1] : (vol.z - centroZ) * dir[0]; // Corrimiento del volumen hacia afuera.
                boolean enBorde = Math.abs(hastaCentro + mitad - Mapa.ANCHO_EDIFICIO / 2) < 1e-3f; // La pared está en el borde de la huella.
                boolean conNegocio = enBorde && Mapa.esCalleSegura(fila + dir[0], columna + dir[1]); // Planta baja comercial.
                int n = columnasQueEntran(tipo, anchoCara); // Columnas de esta cara.
                for (int piso = 0; ; piso++) { // Pisos desde la acera.
                    float y = PRIMER_PISO_Y + piso * tipo.alturaPiso; // Centro de las ventanas de este piso.
                    float abajo = y - tipo.altoVentana / 2; // Borde inferior.
                    float arriba = y + tipo.altoVentana / 2; // Borde superior.
                    if (arriba > vol.yTope - MARGEN_VERTICAL) { // Ya no entra: los pisos siguientes tampoco.
                        break;
                    }
                    if (abajo < vol.yBase + MARGEN_VERTICAL || (conNegocio && abajo < TOPE_PLANTA_BAJA)) { // Debajo del volumen o sobre el negocio.
                        continue;
                    }
                    for (int col = 0; col < n; col++) { // Columnas centradas en la cara.
                        float u = (col - (n - 1) / 2f) * tipo.separacionVentanas; // Posición a lo largo de la pared.
                        float[] p = puntoEnCara(vol.x, vol.z, cara, u, mitad + SEPARACION_PARED); // Apenas delante de la pared.
                        if (!tapada(volumenes, iv, p, abajo, arriba)) { // Solo las que se ven.
                            lista.add(new Ventana(iv, cara, piso, col, p[0], p[1], y, tipo.anchoVentana, tipo.altoVentana));
                        }
                    }
                }
            }
        }
        return lista; // Ventanas del edificio.
    }

    /** Indica si otro volumen (con su losa) tapa la ventana en el punto p, entre las alturas abajo y arriba. */
    private static boolean tapada(List<Edificio.Volumen> volumenes, int propio, float[] p, float abajo, float arriba) {
        for (int i = 0; i < volumenes.size(); i++) { // Los demás volúmenes.
            Edificio.Volumen otro = volumenes.get(i);
            boolean cruzaEnAltura = abajo < otro.yTope + Edificio.GROSOR_LOSA && arriba > otro.yBase; // Comparten alturas.
            if (i != propio && otro.cubre(p[0], p[1]) && cruzaEnAltura) { // La ventana quedaría adentro del otro.
                return true;
            }
        }
        return false; // Se ve desde afuera.
    }

    /**
     * Tono de una ventana de noche: -1 si está apagada, o el índice en TONOS_VENTANA si está encendida.
     * Usa dos valores de Variacion: uno decide encendida/apagada y el otro el tono, repartido según PESOS_TONOS.
     */
    public static int tonoVentana(int fila, int columna, Ventana ventana) {
        int indice = ventana.indice(); // Identidad de la ventana en su edificio.
        if (Variacion.valor(fila, columna, indice, 21) >= PORCENTAJE_VENTANAS_ENCENDIDAS) { // Por encima del umbral...
            return -1; // ...la ventana está apagada.
        }
        float sorteo = Variacion.valor(fila, columna, indice, 22); // Segundo número: elige el tono.
        float acumulado = 0; // Suma de pesos recorridos.
        for (int tono = 0; tono < TONOS_VENTANA.length; tono++) { // Recorre los tonos en orden.
            acumulado += PESOS_TONOS[tono]; // Agrega el peso de este tono.
            if (sorteo < acumulado) { // El número cayó en la franja de este tono.
                return tono; // Tono elegido.
            }
        }
        return TONOS_VENTANA.length - 1; // Por redondeo, el último tono.
    }

    /** Color {r, g, b, emisiva} de una ventana: vidrio de día; encendida (emisiva) o apagada de noche. */
    public static float[] colorVentana(int fila, int columna, Ventana ventana, boolean noche) {
        if (!noche) { // De día todas las ventanas son vidrio.
            return new float[] {COLOR_VIDRIO_DIA[0], COLOR_VIDRIO_DIA[1], COLOR_VIDRIO_DIA[2], 0}; // Sin emisión: las ilumina el sol.
        }
        int tono = tonoVentana(fila, columna, ventana); // Estado de esta ventana.
        if (tono < 0) { // Apagada.
            return new float[] {COLOR_VENTANA_APAGADA[0], COLOR_VENTANA_APAGADA[1], COLOR_VENTANA_APAGADA[2], 0}; // Oscura, sin emisión.
        }
        float[] c = TONOS_VENTANA[tono]; // Color de la luz interior.
        return new float[] {c[0], c[1], c[2], 1}; // Emisiva: se ve encendida aunque no le llegue luz.
    }

    /** Índice del color del toldo del edificio (en COLORES_TOLDO). */
    public static int colorToldo(int fila, int columna) {
        return (int) (Variacion.valor(fila, columna, 0, 23) * COLORES_TOLDO.length); // Reparte los colores en partes iguales.
    }

    /** Indica si el toldo del edificio es a rayas. */
    public static boolean toldoARayas(int fila, int columna) {
        return Variacion.valor(fila, columna, 0, 24) < PROBABILIDAD_RAYAS; // Algunos edificios, siempre los mismos.
    }

    /**
     * Color de la franja "franja" (0 = la de abajo) de una vidriera de noche: interpolación lineal entre
     * COLOR_VIDRIERA_NOCHE_ABAJO y COLOR_VIDRIERA_NOCHE_ARRIBA, t = franja / (FRANJAS_VIDRIERA - 1).
     */
    public static float[] colorVidrieraNoche(int franja) {
        float t = FRANJAS_VIDRIERA > 1 ? (float) franja / (FRANJAS_VIDRIERA - 1) : 0; // 0 abajo, 1 arriba.
        float[] c = new float[3]; // Resultado.
        for (int i = 0; i < 3; i++) { // Rojo, verde y azul.
            c[i] = COLOR_VIDRIERA_NOCHE_ABAJO[i] + (COLOR_VIDRIERA_NOCHE_ARRIBA[i] - COLOR_VIDRIERA_NOCHE_ABAJO[i]) * t; // Mezcla.
        }
        return c; // Color de la franja.
    }

    /** Tramo {uMin, uMax} que ocupa la puerta sobre la cara: siempre centrada. */
    public static float[] tramoPuerta() {
        return new float[] {-ANCHO_PUERTA / 2, ANCHO_PUERTA / 2}; // Del -0.5 al 0.5: lejos de ambas esquinas.
    }

    /** Tramos {uMin, uMax} de las dos vidrieras, una a cada lado de la puerta. */
    public static float[][] tramosVidrieras() {
        return new float[][] { // Izquierda y derecha, simétricas.
            {-CENTRO_VIDRIERA - ANCHO_VIDRIERA / 2, -CENTRO_VIDRIERA + ANCHO_VIDRIERA / 2}, // Vidriera izquierda: de -3.1 a -0.7.
            {CENTRO_VIDRIERA - ANCHO_VIDRIERA / 2, CENTRO_VIDRIERA + ANCHO_VIDRIERA / 2} // Vidriera derecha: de 0.7 a 3.1.
        };
    }

    /** Tramos {uMin, uMax} de los dos toldos: cada uno cubre su vidriera y EXCESO_TOLDO de más a cada lado. */
    public static float[][] tramosToldos() {
        float[][] vidrieras = tramosVidrieras(); // Parte de las vidrieras.
        return new float[][] { // Un toldo por vidriera: la puerta queda al descubierto entre ambos.
            {vidrieras[0][0] - EXCESO_TOLDO, vidrieras[0][1] + EXCESO_TOLDO}, // Toldo izquierdo.
            {vidrieras[1][0] - EXCESO_TOLDO, vidrieras[1][1] + EXCESO_TOLDO} // Toldo derecho.
        };
    }

    /** Punto {x, z} del mundo sobre la cara "cara" de un edificio con centro (x, z): u a lo largo, afuera hacia la calle. */
    public static float[] puntoEnCara(float x, float z, int cara, float u, float afuera) {
        int[] v = Mapa.VECINOS[cara]; // {dFila, dColumna}: la columna es X y la fila es Z.
        float nx = v[1]; // Normal de la cara en X.
        float nz = v[0]; // Normal de la cara en Z.
        float tx = -nz; // Eje "u" a lo largo de la pared, en X (perpendicular a la normal).
        float tz = nx; // Eje "u" en Z.
        return new float[] {x + nx * afuera + tx * u, z + nz * afuera + tz * u}; // Centro + normal · afuera + eje u · u.
    }

    // ==================== 4. DIBUJO ====================

    private final Shader shader; // Programa que recibe el interruptor de emisión.
    private final Cubo cubo; // Geometría compartida.
    private final List<List<Ventana>> ventanasPorCelda = new ArrayList<>(); // Ventanas calculadas una sola vez (fila · ancho + columna).

    /** Recibe el shader y el cubo compartidos y ubica de una vez las ventanas de todos los edificios. */
    public Fachada(Shader shader, Cubo cubo) {
        this.shader = shader; // Guarda el programa para cambiar uEmision.
        this.cubo = cubo; // Guarda la geometría compartida.
        for (int fila = 0; fila < Mapa.MAPA.length; fila++) { // Recorre el mapa.
            for (int columna = 0; columna < Mapa.MAPA[fila].length; columna++) {
                boolean hayEdificio = Mapa.tipo(fila, columna) == Mapa.EDIFICIO; // Solo las manzanas con edificio.
                ventanasPorCelda.add(hayEdificio ? Collections.unmodifiableList(ventanas(fila, columna)) : Collections.emptyList());
            }
        }
    }

    /** Dibuja la planta baja de las caras a la calle y las ventanas del edificio de la celda (fila, columna), con centro (x, z). */
    public void dibujar(int fila, int columna, float x, float z, boolean noche) {
        for (int cara = 0; cara < Mapa.VECINOS.length; cara++) { // Norte, sur, oeste y este (mismo orden que Mapa.VECINOS).
            int[] vecino = Mapa.VECINOS[cara]; // {dFila, dColumna} hacia afuera de esta cara.
            if (Mapa.esCalleSegura(fila + vecino[0], columna + vecino[1])) { // Las caras a la calle tienen negocio en planta baja.
                dibujarPlantaBaja(fila, columna, x, z, cara, noche); // Puerta, vidriera (iluminada de noche) y toldo.
            }
        }
        for (Ventana v : ventanasPorCelda.get(fila * Mapa.MAPA[0].length + columna)) { // Ventanas ya ubicadas.
            float[] c = colorVentana(fila, columna, v, noche); // Vidrio, encendida o apagada.
            shader.entero("uEmision", (int) c[3]); // Las ventanas encendidas simulan habitaciones con luz en el ambiente nocturno.
            cajaEnPunto(v.x, v.z, v.cara, v.y, v.ancho, v.alto, GROSOR_PEGADO, c[0], c[1], c[2]); // Ventana sobre su pared.
        }
        shader.entero("uEmision", 0); // Restablece la iluminación normal de los demás elementos.
    }

    /**
     * Dibuja una caja pegada a una cara: u = posición a lo largo de la pared, afuera = distancia desde el centro del
     * edificio hacia la calle, y = altura del centro; anchoU, alto y grosor son sus medidas.
     */
    private void cajaEnCara(float x, float z, int cara, float u, float afuera, float y, float anchoU, float alto, float grosor, float r, float g, float b) {
        float[] centro = puntoEnCara(x, z, cara, u, afuera); // Centro de la caja sobre la cara.
        cajaEnPunto(centro[0], centro[1], cara, y, anchoU, alto, grosor, r, g, b); // Caja alineada a esa cara.
    }

    /** Dibuja una caja centrada en (cx, y, cz), con anchoU a lo largo de la cara y grosor en la dirección de su normal. */
    private void cajaEnPunto(float cx, float cz, int cara, float y, float anchoU, float alto, float grosor, float r, float g, float b) {
        float nx = Mapa.VECINOS[cara][1]; // Normal de la cara en X (la columna es X).
        float sx = nx != 0 ? grosor : anchoU; // Si la cara mira al este u oeste, el grosor va en X y el ancho en Z.
        float sz = nx != 0 ? anchoU : grosor; // Y al revés si mira al norte o al sur.
        cubo.caja(cx, y, cz, sx, alto, sz, r, g, b); // Caja alineada a los ejes, pegada a la cara.
    }

    /** Planta baja comercial "vidriera | puerta | vidriera": puerta centrada y un toldo inclinado sobre cada vidriera. */
    private void dibujarPlantaBaja(int fila, int columna, float x, float z, int cara, boolean noche) {
        float pared = Mapa.ANCHO_EDIFICIO / 2 + SEPARACION_PARED + GROSOR_PEGADO / 2; // Plano apenas delante de la pared.
        cajaEnCara(x, z, cara, 0, pared, ALTURA_ACERA + ALTO_PUERTA / 2, ANCHO_PUERTA, ALTO_PUERTA, GROSOR_PEGADO,
            COLOR_PUERTA[0], COLOR_PUERTA[1], COLOR_PUERTA[2]); // Puerta oscura CENTRADA, apoyada en la acera: lejos de las esquinas.
        shader.entero("uEmision", noche ? 1 : 0); // De noche las vidrieras brillan con luz cálida propia.
        for (float[] tramo : tramosVidrieras()) { // Vidriera izquierda y derecha.
            float uVidriera = (tramo[0] + tramo[1]) / 2; // Centro de la vidriera sobre la cara.
            if (!noche) { // De día: un solo vidrio, iluminado por el sol.
                cajaEnCara(x, z, cara, uVidriera, pared, BASE_VIDRIERA + ALTO_VIDRIERA / 2, ANCHO_VIDRIERA, ALTO_VIDRIERA, GROSOR_PEGADO,
                    COLOR_VIDRIERA_DIA[0], COLOR_VIDRIERA_DIA[1], COLOR_VIDRIERA_DIA[2]); // Vidriera del negocio.
                continue;
            }
            float altoFranja = ALTO_VIDRIERA / FRANJAS_VIDRIERA; // Las franjas cubren justo el alto del vidrio.
            for (int franja = 0; franja < FRANJAS_VIDRIERA; franja++) { // De abajo hacia arriba.
                float[] c = colorVidrieraNoche(franja); // Más clara abajo, más tenue arriba.
                float y = BASE_VIDRIERA + (franja + 0.5f) * altoFranja; // Centro de la franja.
                cajaEnCara(x, z, cara, uVidriera, pared, y, ANCHO_VIDRIERA, altoFranja, GROSOR_PEGADO, c[0], c[1], c[2]); // Franja del vidrio.
            }
        }
        shader.entero("uEmision", 0); // Los toldos son tela: reciben luz normal.
        for (float[] tramo : tramosToldos()) { // Un toldo sobre cada vidriera; la puerta queda descubierta en el medio.
            dibujarToldo(fila, columna, x, z, cara, (tramo[0] + tramo[1]) / 2, tramo[1] - tramo[0]); // Toldo sobre la vidriera.
        }
    }

    /**
     * Toldo inclinado: ESCALONES_TOLDO tiras que se alejan de la pared y bajan un poco cada una, más un faldón vertical
     * en el borde. Sobresale VUELO_TOLDO = 0.7: su borde queda a 4.2 del centro, sobre la vereda (que llega a 5) y antes
     * de los postes de semáforo (4.4) y farola (4.6). Si el edificio tiene toldo a rayas, cada tira alterna color y blanco.
     */
    private void dibujarToldo(int fila, int columna, float x, float z, int cara, float uCentro, float anchoToldo) {
        float[] color = COLORES_TOLDO[colorToldo(fila, columna)]; // Color del toldo de este edificio.
        boolean rayas = toldoARayas(fila, columna); // ¿Alterna con blanco?
        float fondoEscalon = VUELO_TOLDO / ESCALONES_TOLDO; // Profundidad de cada tira.
        float pared = Mapa.ANCHO_EDIFICIO / 2; // Plano de la pared.
        int rayasPorEscalon = rayas ? Math.max(1, Math.round(anchoToldo / ANCHO_RAYA)) : 1; // Una sola pieza si es liso.
        float anchoRaya = anchoToldo / rayasPorEscalon; // Ancho real de cada raya (reparte el toldo en partes iguales).
        for (int e = 0; e < ESCALONES_TOLDO; e++) { // De la pared hacia afuera.
            float afuera = pared + (e + 0.5f) * fondoEscalon; // Centro de la tira, cada vez más lejos de la pared.
            float y = ALTURA_TOLDO - (e + 0.5f) * CAIDA_TOLDO / ESCALONES_TOLDO; // Y cada vez más baja: forma la pendiente.
            for (int k = 0; k < rayasPorEscalon; k++) { // Rayas a lo ancho del toldo.
                float u = uCentro - anchoToldo / 2 + (k + 0.5f) * anchoRaya; // Centro de la raya.
                boolean blanca = rayas && k % 2 == 1; // Una de cada dos rayas es blanca.
                float r = blanca ? 0.95f : color[0]; // Color de la raya.
                float g = blanca ? 0.95f : color[1]; // Color de la raya.
                float b = blanca ? 0.92f : color[2]; // Color de la raya.
                cajaEnCara(x, z, cara, u, afuera, y, anchoRaya, 0.06f, fondoEscalon + 0.02f, r, g, b); // Tira del toldo (se superponen apenas).
            }
        }
        float bordeExterior = pared + VUELO_TOLDO - 0.02f; // Faldón en el extremo del toldo.
        float yFaldon = ALTURA_TOLDO - CAIDA_TOLDO - 0.1f; // Cuelga debajo de la última tira.
        cajaEnCara(x, z, cara, uCentro, bordeExterior, yFaldon, anchoToldo, 0.2f, 0.04f, color[0], color[1], color[2]); // Faldón del toldo.
    }
}
