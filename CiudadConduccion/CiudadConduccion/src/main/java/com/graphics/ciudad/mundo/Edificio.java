package com.graphics.ciudad.mundo; // Agrupa lo que forma la ciudad: mapa, edificios, decoración y señales.

import com.graphics.ciudad.motor.Cubo; // Paredes, losas, barandas y patas son cajas.
import com.graphics.ciudad.motor.Figuras; // Cilindro (antena, tanque) y prisma triangular (techo a dos aguas).
import java.util.ArrayList; // Listas de volúmenes y piezas.
import java.util.Collections; // Publica las listas sin permitir modificarlas.
import java.util.List; // Tipo de esas listas.

/**
 * EDIFICIO: la forma de cada edificio de la ciudad, para que no parezcan clonados.
 * Responsable de:
 *  - elegir el TIPO de cada manzana (TipoEdificio: torre, bloque, escalonado, casa baja o doble) y sus colores de
 *    pared y techo, con Variacion: la misma celda da siempre el mismo edificio, en cada cuadro y en cada ejecución;
 *  - armar sus VOLÚMENES (las cajas con paredes, donde Fachada pone negocios y ventanas) y las PIEZAS del techo
 *    (losas, baranda, caja de ascensor, antena, tanque de agua, techo a dos aguas);
 *  - dibujarlas con Cubo y Figuras.
 * HUELLA: todo queda dentro de ANCHO_EDIFICIO (7) más el vuelo de la losa (VUELO_CORNISA), como la cubierta de
 * antes. El volumen de la planta baja siempre ocupa la huella completa de 7 × 7, así la planta baja comercial de
 * Fachada entra igual en todos los tipos. Las colisiones no cambian: Colisiones usa la manzana entera.
 * Se comunica con: Mapa (centro de la celda, ancho), Variacion, Ciudad (lo dibuja) y Fachada (lee los volúmenes).
 * La parte de cálculo (tipo, colores, volúmenes, piezas) no usa OpenGL, por eso se prueba sin ventana.
 */
public class Edificio {

    // ==================== 1. PALETA URBANA (valores ajustables) ====================
    public static final float[][] PALETA_FACHADAS = { // Colores de pared.
        {0.62f, 0.30f, 0.22f}, // Ladrillo.
        {0.90f, 0.84f, 0.68f}, // Crema.
        {0.92f, 0.90f, 0.83f}, // Blanco hueso.
        {0.60f, 0.60f, 0.58f}, // Gris cemento.
        {0.80f, 0.45f, 0.30f}, // Terracota.
        {0.52f, 0.76f, 0.70f}, // Verde agua.
        {0.95f, 0.88f, 0.56f} // Amarillo pálido.
    };
    public static final float[][] PALETA_TECHOS = { // Colores de losa y techo.
        {0.24f, 0.25f, 0.27f}, // Gris oscuro (membrana asfáltica).
        {0.68f, 0.68f, 0.66f}, // Gris claro (losa de hormigón).
        {0.66f, 0.30f, 0.20f}, // Teja.
        {0.34f, 0.52f, 0.30f} // Verde (terraza con plantas).
    };
    public static final int TECHO_TEJA = 2; // Índice de la teja en PALETA_TECHOS: el de las casas bajas.
    public static final float[] COLOR_METAL = {0.55f, 0.57f, 0.60f}; // Antena, patas del tanque y baranda.
    public static final float[] COLOR_TANQUE = {0.20f, 0.21f, 0.23f}; // Tanque de agua de plástico negro.

    // ==================== 2. HUELLA Y LOSAS ====================
    public static final float VUELO_CORNISA = 0.15f; // Cuánto sobresale la losa de la pared (la cubierta de antes medía 7.3).
    public static final float MEDIA_HUELLA = Mapa.ANCHO_EDIFICIO / 2 + VUELO_CORNISA; // 3.65: nada del edificio pasa de acá.
    public static final float GROSOR_LOSA = 0.3f; // Espesor de la losa sobre cada volumen.
    public static final float ALTURA_ACERA = 0.3f; // El edificio se apoya sobre la acera.
    public static final float ALTO_MINIMO_BASE = 3.2f; // El volumen de abajo necesita esto para la puerta, las vidrieras y el toldo.

    // ==================== 3. ALTURAS Y MEDIDAS POR TIPO (valores ajustables) ====================
    // Las alturas son de pared (sin losa ni techo), medidas desde la acera, y se sortean enteras entre MIN y MAX.
    public static final int ALTURA_TORRE_MIN = 16; // Torre: la más alta de la ciudad.
    public static final int ALTURA_TORRE_MAX = 22;
    public static final float ANCHO_TORRE = 4.4f; // Lado de la torre: angosta sobre el podio de 7.
    public static final float ALTURA_PODIO = ALTO_MINIMO_BASE; // Podio comercial de la torre: solo la planta baja.
    public static final float ALTO_ANTENA = 3.5f; // Antena: mástil fino sobre la azotea.
    public static final float GROSOR_ANTENA = 0.12f;
    public static final float DIAMETRO_TANQUE = 1.6f; // Tanque de agua: cilindro sobre cuatro patas.
    public static final float ALTO_TANQUE = 1.3f;
    public static final float ALTO_PATAS = 0.8f;
    public static final float PROBABILIDAD_ANTENA = 0.5f; // Fracción de torres con antena (el resto, con tanque).

    public static final int ALTURA_BLOQUE_MIN = 7; // Bloque: altura media.
    public static final int ALTURA_BLOQUE_MAX = 11;
    public static final float ALTO_BARANDA = 0.5f; // Baranda de la azotea, en todo el borde.
    public static final float GROSOR_BARANDA = 0.1f;
    public static final float ANCHO_ASCENSOR = 2.0f; // Caja de ascensor sobre la azotea.
    public static final float ALTO_ASCENSOR = 1.8f;
    public static final float CORRIMIENTO_ASCENSOR = 1.3f; // Distancia del centro de la azotea a la caja (en X y en Z).

    public static final float[] ANCHOS_NIVELES = {Mapa.ANCHO_EDIFICIO, 5.2f, 3.6f}; // Escalonado: lado de cada nivel.
    public static final int NIVELES_MIN = 2; // Escalonado: 2 o 3 niveles.
    public static final int NIVELES_MAX = 3;
    public static final int ALTURA_NIVEL_MIN = 4; // Alto de cada nivel (el de abajo, al menos ALTO_MINIMO_BASE).
    public static final int ALTURA_NIVEL_MAX = 5;

    public static final int PISOS_CASA_MIN = 1; // Casa baja: 1 o 2 pisos.
    public static final int PISOS_CASA_MAX = 2;
    public static final float ALTO_PLANTA_CASA = ALTO_MINIMO_BASE; // Planta baja de la casa (negocio con toldo).
    public static final float ALTO_PISO_CASA = 2.0f; // Cada piso de más.
    public static final float ALTO_TECHO_CASA = 2.0f; // Del alero a la cumbrera del techo a dos aguas.
    public static final float LADO_CHIMENEA = 0.5f; // Chimenea que asoma del techo.
    public static final float ALTO_CHIMENEA = 1.8f;

    public static final float ANCHO_PARTE_ALTA = 4.0f; // Doble: la parte alta ocupa 4 de los 7; la baja, los otros 3.
    public static final int ALTURA_DOBLE_ALTA_MIN = 9;
    public static final int ALTURA_DOBLE_ALTA_MAX = 13;
    public static final int ALTURA_DOBLE_BAJA_MIN = 4;
    public static final int ALTURA_DOBLE_BAJA_MAX = 6;

    // Semillas de Variacion: cada decisión usa la suya para que no dependan entre sí.
    private static final int SEMILLA_TIPO = 167; // Elegida para que en este MAPA los cinco tipos salgan parejos (4, 4, 4, 4 y 3).
    private static final int SEMILLA_PARED = 32;
    private static final int SEMILLA_TECHO = 33;
    private static final int SEMILLA_ALTURA = 34;
    private static final int SEMILLA_FORMA = 35; // Niveles, pisos, antena o tanque.
    private static final int SEMILLA_LADO = 36; // Hacia qué lado se corre una pieza.
    private static final int SEMILLA_SEGUNDO_COLOR = 37;

    // ==================== 4. VOLÚMENES Y PIEZAS ====================

    /** Caja con paredes: centro (x, z), lados, alturas de base y tope (Y del mundo) y color de pared. */
    public static final class Volumen {
        public final float x, z, anchoX, anchoZ, yBase, yTope; // Posición y medidas.
        public final float[] color; // Color de la pared.

        Volumen(float x, float z, float anchoX, float anchoZ, float yBase, float yTope, float[] color) {
            this.x = x; // Centro en X.
            this.z = z; // Centro en Z.
            this.anchoX = anchoX; // Lado en X.
            this.anchoZ = anchoZ; // Lado en Z.
            this.yBase = yBase; // Donde se apoya.
            this.yTope = yTope; // Donde empieza su losa o techo.
            this.color = color; // Pared.
        }

        /** Indica si el punto (px, pz) cae dentro de la planta del volumen (con una tolerancia mínima). */
        public boolean cubre(float px, float pz) {
            return Math.abs(px - x) < anchoX / 2 + 1e-3f && Math.abs(pz - z) < anchoZ / 2 + 1e-3f; // Dentro en ambos ejes.
        }
    }

    /** Forma con la que se dibuja una pieza. */
    public enum Forma { CAJA, CILINDRO, PRISMA }

    /** Una pieza para dibujar: forma, centro, tamaño, color y giro alrededor de Y (0 o 90°). */
    public static final class Pieza {
        public final Forma forma; // Cubo, cilindro o prisma triangular.
        public final float x, y, z, sx, sy, sz, angulo; // Centro, tamaño y giro.
        public final float[] color; // Color del material.

        Pieza(Forma forma, float x, float y, float z, float sx, float sy, float sz, float[] color, float angulo) {
            this.forma = forma;
            this.x = x;
            this.y = y;
            this.z = z;
            this.sx = sx;
            this.sy = sy;
            this.sz = sz;
            this.color = color;
            this.angulo = angulo;
        }

        /** Media extensión en X ya girada: con 90° el ancho en X pasa a ser el de Z. */
        public float mitadX() {
            return (float) (Math.abs(Math.cos(angulo)) * sx + Math.abs(Math.sin(angulo)) * sz) / 2; // Caja girada que la contiene.
        }

        /** Media extensión en Z ya girada. */
        public float mitadZ() {
            return (float) (Math.abs(Math.sin(angulo)) * sx + Math.abs(Math.cos(angulo)) * sz) / 2; // Caja girada que la contiene.
        }
    }

    // ==================== 5. DECISIONES DETERMINÍSTICAS ====================

    /** Entero entre minimo y maximo (incluidos), sorteado con Variacion para esta celda. */
    private static int entero(int fila, int columna, int semilla, int minimo, int maximo) {
        return minimo + (int) (Variacion.valor(fila, columna, 0, semilla) * (maximo - minimo + 1)); // Partes iguales.
    }

    /** Tipo de edificio de la celda: cada tipo tiene la misma probabilidad. */
    public static TipoEdificio tipo(int fila, int columna) {
        TipoEdificio[] tipos = TipoEdificio.values(); // Los cinco tipos.
        return tipos[entero(fila, columna, SEMILLA_TIPO, 0, tipos.length - 1)]; // Siempre el mismo para esta celda.
    }

    /** Índice del color de pared (en PALETA_FACHADAS). */
    public static int colorPared(int fila, int columna) {
        return entero(fila, columna, SEMILLA_PARED, 0, PALETA_FACHADAS.length - 1); // Uno de los siete.
    }

    /** Índice del color de pared de la parte baja de un edificio DOBLE: siempre distinto del principal. */
    public static int colorParedSecundario(int fila, int columna) {
        int salto = entero(fila, columna, SEMILLA_SEGUNDO_COLOR, 1, PALETA_FACHADAS.length - 1); // Nunca 0: otro color.
        return (colorPared(fila, columna) + salto) % PALETA_FACHADAS.length; // Avanza en la paleta.
    }

    /** Índice del color de techo (en PALETA_TECHOS): las casas bajas son siempre de teja. */
    public static int colorTecho(int fila, int columna) {
        if (tipo(fila, columna) == TipoEdificio.CASA_BAJA) { // Techo a dos aguas.
            return TECHO_TEJA; // Teja.
        }
        return entero(fila, columna, SEMILLA_TECHO, 0, PALETA_TECHOS.length - 1); // Gris oscuro, gris claro, teja o verde.
    }

    /** +1 o -1: hacia qué lado se corre una pieza (la caja del ascensor, la parte alta del doble). */
    private static int lado(int fila, int columna, int indice) {
        return Variacion.valor(fila, columna, indice, SEMILLA_LADO) < 0.5f ? -1 : 1; // Mitad y mitad.
    }

    // ==================== 6. FORMA DE CADA TIPO ====================

    /** Volúmenes con paredes del edificio de la celda; el primero siempre ocupa la huella completa desde la acera. */
    public static List<Volumen> volumenes(int fila, int columna) {
        float x = Mapa.centro(columna); // Centro de la manzana en X.
        float z = Mapa.centro(fila); // Centro en Z.
        float ancho = Mapa.ANCHO_EDIFICIO; // Huella completa: 7.
        float[] pared = PALETA_FACHADAS[colorPared(fila, columna)]; // Color principal.
        List<Volumen> lista = new ArrayList<>(); // Resultado.
        switch (tipo(fila, columna)) {
            case TORRE: { // Podio de 7 × 7 con el negocio y, encima, la torre angosta.
                float tope = ALTURA_ACERA + entero(fila, columna, SEMILLA_ALTURA, ALTURA_TORRE_MIN, ALTURA_TORRE_MAX);
                float podio = ALTURA_ACERA + ALTURA_PODIO; // Tope del podio.
                lista.add(new Volumen(x, z, ancho, ancho, ALTURA_ACERA, podio, pared)); // Podio.
                lista.add(new Volumen(x, z, ANCHO_TORRE, ANCHO_TORRE, podio, tope, pared)); // Torre.
                break;
            }
            case BLOQUE: { // Una sola caja de altura media.
                float tope = ALTURA_ACERA + entero(fila, columna, SEMILLA_ALTURA, ALTURA_BLOQUE_MIN, ALTURA_BLOQUE_MAX);
                lista.add(new Volumen(x, z, ancho, ancho, ALTURA_ACERA, tope, pared)); // Bloque.
                break;
            }
            case ESCALONADO: { // Cada nivel se apoya sobre el anterior y es más chico.
                int niveles = entero(fila, columna, SEMILLA_FORMA, NIVELES_MIN, NIVELES_MAX); // 2 o 3.
                float base = ALTURA_ACERA; // El primer nivel arranca en la acera.
                for (int nivel = 0; nivel < niveles; nivel++) { // De abajo hacia arriba.
                    float alto = ALTURA_NIVEL_MIN + (int) (Variacion.valor(fila, columna, nivel, SEMILLA_ALTURA)
                        * (ALTURA_NIVEL_MAX - ALTURA_NIVEL_MIN + 1)); // Alto de este nivel.
                    float lado = ANCHOS_NIVELES[nivel]; // 7, 5.2 y 3.6.
                    lista.add(new Volumen(x, z, lado, lado, base, base + alto, pared)); // Nivel centrado.
                    base += alto; // El siguiente empieza en el tope de este (atraviesa su losa).
                }
                break;
            }
            case CASA_BAJA: { // Uno o dos pisos; el techo es una pieza aparte.
                int pisos = PISOS_CASA_MIN + (int) (Variacion.valor(fila, columna, 2, SEMILLA_FORMA)
                    * (PISOS_CASA_MAX - PISOS_CASA_MIN + 1)); // 1 o 2 (índice 2: independiente de los niveles del escalonado).
                float tope = ALTURA_ACERA + ALTO_PLANTA_CASA + (pisos - 1) * ALTO_PISO_CASA; // Alero.
                lista.add(new Volumen(x, z, ancho, ancho, ALTURA_ACERA, tope, pared)); // Paredes de la casa.
                break;
            }
            case DOBLE: { // La manzana se parte en dos: una parte alta de 4 y una baja de 3, pegadas.
                float[] paredBaja = PALETA_FACHADAS[colorParedSecundario(fila, columna)]; // Otro color.
                float topeAlto = ALTURA_ACERA + entero(fila, columna, SEMILLA_ALTURA, ALTURA_DOBLE_ALTA_MIN, ALTURA_DOBLE_ALTA_MAX);
                float topeBajo = ALTURA_ACERA + entero(fila, columna, SEMILLA_FORMA, ALTURA_DOBLE_BAJA_MIN, ALTURA_DOBLE_BAJA_MAX);
                float anchoBajo = ancho - ANCHO_PARTE_ALTA; // 3.
                int s = lado(fila, columna, 0); // De qué lado queda la parte alta.
                float centroAlto = s * (ancho - ANCHO_PARTE_ALTA) / 2; // ±1.5: pegada a un borde.
                float centroBajo = -s * (ancho - anchoBajo) / 2; // ∓2: pegada al borde opuesto.
                boolean partidoEnX = lado(fila, columna, 1) > 0; // ¿La división corta el eje X o el Z?
                if (partidoEnX) { // Una parte al oeste y otra al este.
                    lista.add(new Volumen(x + centroAlto, z, ANCHO_PARTE_ALTA, ancho, ALTURA_ACERA, topeAlto, pared));
                    lista.add(new Volumen(x + centroBajo, z, anchoBajo, ancho, ALTURA_ACERA, topeBajo, paredBaja));
                } else { // Una parte al norte y otra al sur.
                    lista.add(new Volumen(x, z + centroAlto, ancho, ANCHO_PARTE_ALTA, ALTURA_ACERA, topeAlto, pared));
                    lista.add(new Volumen(x, z + centroBajo, ancho, anchoBajo, ALTURA_ACERA, topeBajo, paredBaja));
                }
                break;
            }
            default:
                throw new IllegalStateException("Tipo sin forma: " + tipo(fila, columna)); // No debería pasar.
        }
        return lista; // Volúmenes del edificio.
    }

    /**
     * Todas las piezas del edificio: primero sus volúmenes (paredes) y después el techo de cada uno. Las losas planas
     * sobresalen VUELO_CORNISA; la casa baja lleva un prisma triangular (techo a dos aguas) y una chimenea.
     */
    public static List<Pieza> piezas(int fila, int columna) {
        TipoEdificio tipo = tipo(fila, columna); // Forma general.
        List<Volumen> volumenes = volumenes(fila, columna); // Paredes.
        float[] techo = PALETA_TECHOS[colorTecho(fila, columna)]; // Color de losas y techos.
        List<Pieza> lista = new ArrayList<>(); // Resultado.
        for (Volumen v : volumenes) { // Paredes: una caja por volumen.
            lista.add(new Pieza(Forma.CAJA, v.x, (v.yBase + v.yTope) / 2, v.z, v.anchoX, v.yTope - v.yBase, v.anchoZ, v.color, 0));
        }
        if (tipo == TipoEdificio.CASA_BAJA) { // Techo a dos aguas en lugar de losa.
            agregarTechoCasa(fila, columna, volumenes.get(0), lista);
            return lista;
        }
        for (Volumen v : volumenes) { // Una losa sobre cada volumen, un poco más ancha.
            lista.add(new Pieza(Forma.CAJA, v.x, v.yTope + GROSOR_LOSA / 2, v.z,
                v.anchoX + 2 * VUELO_CORNISA, GROSOR_LOSA, v.anchoZ + 2 * VUELO_CORNISA, techo, 0));
        }
        Volumen alto = volumenes.get(volumenes.size() - 1); // En la torre y el escalonado, el de arriba.
        if (tipo == TipoEdificio.DOBLE) { // En el doble, el primero es la parte alta.
            alto = volumenes.get(0);
        }
        float azotea = alto.yTope + GROSOR_LOSA; // Piso de la azotea.
        if (tipo == TipoEdificio.TORRE) { // Antena o tanque de agua.
            if (Variacion.valor(fila, columna, 1, SEMILLA_FORMA) < PROBABILIDAD_ANTENA) {
                agregarAntena(alto, azotea, lista);
            } else {
                agregarTanque(alto, azotea, lista);
            }
        } else if (tipo == TipoEdificio.BLOQUE) { // Baranda y caja de ascensor.
            agregarBaranda(alto, azotea, lista);
            float dx = lado(fila, columna, 2) * CORRIMIENTO_ASCENSOR; // Corrida hacia una esquina.
            float dz = lado(fila, columna, 3) * CORRIMIENTO_ASCENSOR;
            lista.add(new Pieza(Forma.CAJA, alto.x + dx, azotea + ALTO_ASCENSOR / 2, alto.z + dz,
                ANCHO_ASCENSOR, ALTO_ASCENSOR, ANCHO_ASCENSOR, alto.color, 0)); // Caja de ascensor, color de la pared.
        }
        return lista; // Piezas del edificio.
    }

    /** Antena: una base chica y un mástil fino en el centro de la azotea. */
    private static void agregarAntena(Volumen v, float azotea, List<Pieza> lista) {
        lista.add(new Pieza(Forma.CAJA, v.x, azotea + 0.15f, v.z, 0.6f, 0.3f, 0.6f, COLOR_METAL, 0)); // Base.
        lista.add(new Pieza(Forma.CILINDRO, v.x, azotea + ALTO_ANTENA / 2, v.z,
            GROSOR_ANTENA, ALTO_ANTENA, GROSOR_ANTENA, COLOR_METAL, 0)); // Mástil.
    }

    /** Tanque de agua: un cilindro sobre cuatro patas, en el centro de la azotea. */
    private static void agregarTanque(Volumen v, float azotea, List<Pieza> lista) {
        float separacion = DIAMETRO_TANQUE / 2 - 0.2f; // Patas debajo del borde del tanque.
        for (int i = -1; i <= 1; i += 2) { // Dos filas de patas.
            for (int k = -1; k <= 1; k += 2) { // Dos patas por fila.
                lista.add(new Pieza(Forma.CAJA, v.x + i * separacion, azotea + ALTO_PATAS / 2, v.z + k * separacion,
                    0.12f, ALTO_PATAS, 0.12f, COLOR_METAL, 0)); // Pata.
            }
        }
        lista.add(new Pieza(Forma.CILINDRO, v.x, azotea + ALTO_PATAS + ALTO_TANQUE / 2, v.z,
            DIAMETRO_TANQUE, ALTO_TANQUE, DIAMETRO_TANQUE, COLOR_TANQUE, 0)); // Tanque.
    }

    /** Baranda: cuatro tiras finas sobre el borde de la losa (que sobresale VUELO_CORNISA). */
    private static void agregarBaranda(Volumen v, float azotea, List<Pieza> lista) {
        float y = azotea + ALTO_BARANDA / 2; // Centro de la baranda.
        float bordeX = v.anchoX / 2 + VUELO_CORNISA - GROSOR_BARANDA / 2; // Justo sobre el borde de la losa.
        float bordeZ = v.anchoZ / 2 + VUELO_CORNISA - GROSOR_BARANDA / 2;
        float largoX = v.anchoX + 2 * VUELO_CORNISA; // Largo de las tiras norte y sur.
        float largoZ = v.anchoZ + 2 * VUELO_CORNISA; // Largo de las tiras oeste y este.
        lista.add(new Pieza(Forma.CAJA, v.x, y, v.z - bordeZ, largoX, ALTO_BARANDA, GROSOR_BARANDA, COLOR_METAL, 0)); // Norte.
        lista.add(new Pieza(Forma.CAJA, v.x, y, v.z + bordeZ, largoX, ALTO_BARANDA, GROSOR_BARANDA, COLOR_METAL, 0)); // Sur.
        lista.add(new Pieza(Forma.CAJA, v.x - bordeX, y, v.z, GROSOR_BARANDA, ALTO_BARANDA, largoZ, COLOR_METAL, 0)); // Oeste.
        lista.add(new Pieza(Forma.CAJA, v.x + bordeX, y, v.z, GROSOR_BARANDA, ALTO_BARANDA, largoZ, COLOR_METAL, 0)); // Este.
    }

    /**
     * Techo a dos aguas: un prisma triangular (Figuras.prisma) que cubre la casa con un alero de VUELO_CORNISA. Sin
     * giro, la cumbrera va a lo largo de X; girado 90°, a lo largo de Z. La chimenea asoma de uno de los faldones.
     */
    private static void agregarTechoCasa(int fila, int columna, Volumen v, List<Pieza> lista) {
        float lado = Mapa.ANCHO_EDIFICIO + 2 * VUELO_CORNISA; // 7.3: la base del prisma.
        boolean cumbreraEnX = lado(fila, columna, 4) > 0; // Orientación del techo.
        float angulo = cumbreraEnX ? 0 : (float) (Math.PI / 2); // Giro alrededor de Y.
        lista.add(new Pieza(Forma.PRISMA, v.x, v.yTope + ALTO_TECHO_CASA / 2, v.z, lado, ALTO_TECHO_CASA, lado,
            PALETA_TECHOS[TECHO_TEJA], angulo)); // Techo de teja.
        float aLoLargo = lado(fila, columna, 5) * 1.8f; // Posición de la chimenea sobre la cumbrera.
        float alCostado = 1.5f; // Distancia a la cumbrera: sobre un faldón.
        float cx = v.x + (cumbreraEnX ? aLoLargo : alCostado); // Centro de la chimenea.
        float cz = v.z + (cumbreraEnX ? alCostado : aLoLargo);
        lista.add(new Pieza(Forma.CAJA, cx, v.yTope + 0.3f + ALTO_CHIMENEA / 2, cz,
            LADO_CHIMENEA, ALTO_CHIMENEA, LADO_CHIMENEA, v.color, 0)); // Chimenea del color de la pared.
    }

    // ==================== 7. DIBUJO ====================

    private final Cubo cubo; // Cajas.
    private final Figuras figuras; // Cilindro y prisma.
    private final List<List<Pieza>> piezasPorCelda = new ArrayList<>(); // Piezas calculadas una sola vez (fila · ancho + columna).

    /** Recibe la geometría compartida y calcula de una vez las piezas de todos los edificios. */
    public Edificio(Cubo cubo, Figuras figuras) {
        this.cubo = cubo;
        this.figuras = figuras;
        for (int fila = 0; fila < Mapa.MAPA.length; fila++) { // Recorre el mapa.
            for (int columna = 0; columna < Mapa.MAPA[fila].length; columna++) {
                boolean hayEdificio = Mapa.tipo(fila, columna) == Mapa.EDIFICIO; // Solo las manzanas con edificio.
                piezasPorCelda.add(hayEdificio ? Collections.unmodifiableList(piezas(fila, columna)) : Collections.emptyList());
            }
        }
    }

    /** Dibuja el edificio de la celda (fila, columna). */
    public void dibujar(int fila, int columna) {
        for (Pieza p : piezasPorCelda.get(fila * Mapa.MAPA[0].length + columna)) { // Piezas ya calculadas.
            float[] c = p.color; // Color de la pieza.
            switch (p.forma) {
                case CAJA:
                    cubo.cajaGirada(p.x, p.y, p.z, p.sx, p.sy, p.sz, c[0], c[1], c[2], p.angulo);
                    break;
                case CILINDRO:
                    figuras.cilindro.dibujarGirada(p.x, p.y, p.z, p.sx, p.sy, p.sz, c[0], c[1], c[2], p.angulo);
                    break;
                case PRISMA:
                    figuras.prisma.dibujarGirada(p.x, p.y, p.z, p.sx, p.sy, p.sz, c[0], c[1], c[2], p.angulo);
                    break;
                default:
                    break;
            }
        }
    }
}
