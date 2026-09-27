package com.graphics.ciudad.mundo; // Agrupa lo que forma la ciudad: mapa, edificios, decoración y semáforos.

import java.util.ArrayList; // Listas de intersecciones, accesos y parques.
import java.util.List; // Tipo de esas listas.

/**
 * MAPA: plano de la ciudad descrito como una matriz de celdas.
 * Responsable de: guardar MAPA (0 = calle, 1 = edificio, 2 = parque), el tamaño de cada celda, los límites
 * calculados a partir de MAPA.length y la conversión entre índices y coordenadas. La forma y la altura de cada
 * edificio las decide Edificio.
 * Coordenadas: X = izquierda/derecha; Y = altura; Z = profundidad. Las filas corresponden a Z y las columnas a X.
 * Se comunica con: Ciudad y Decoracion (lo recorren para dibujar), Colisiones (lo recorre para bloquear al auto),
 * Juego (pasa LIMITE a Camara y muestra el sector en el HUD) y Minimapa (ajusta la vista superior a LIMITE y
 * dibuja las divisiones de SECTORES).
 * No usa OpenGL, por eso puede probarse sin ventana.
 */
public final class Mapa {

    public static final int CALLE = 0; // Valor de una celda transitable.
    public static final int EDIFICIO = 1; // Valor de una manzana ocupada por un edificio.
    public static final int PARQUE = 2; // Valor de una manzana con parque.

    // Ciudad de 11 × 11 celdas: las filas y columnas pares son calles continuas, así todas quedan conectadas;
    // las celdas con fila y columna impares son las 25 manzanas (19 edificios y 6 parques).
    public static final int[][] MAPA = { // Matriz: 0 = calle, 1 = edificio, 2 = parque.
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Fila norte: calle continua.
        {0, 1, 0, 1, 0, 2, 0, 1, 0, 1, 0}, // Primera fila de manzanas, separadas por calles; parque al centro.
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Segunda avenida horizontal.
        {0, 2, 0, 1, 0, 1, 0, 1, 0, 2, 0}, // Segunda fila de manzanas: parques en los extremos oeste y este.
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Tercera avenida horizontal.
        {0, 1, 0, 1, 0, 2, 0, 1, 0, 1, 0}, // Fila central de manzanas: parque central en el origen.
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Cuarta avenida horizontal.
        {0, 1, 0, 2, 0, 1, 0, 1, 0, 1, 0}, // Cuarta fila de manzanas.
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Quinta avenida horizontal.
        {0, 1, 0, 1, 0, 1, 0, 2, 0, 1, 0}, // Última fila de manzanas.
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0} // Calle del borde sur.
    };

    public static final float TAM_CELDA = 10; // Ancho y profundidad de cada celda del mapa.
    public static final float TAMANO = MAPA.length * TAM_CELDA; // Lado completo de la ciudad: 11 celdas × 10 = 110 unidades.
    public static final float LIMITE = TAMANO / 2; // Distancia del origen a cada borde: el mapa mide 110 unidades, el límite es 55.
    public static final float ANCHO_EDIFICIO = 7; // Ancho y profundidad de la base de cada edificio dentro de su manzana de 10.

    // ==================== SECTORES CON NOMBRE (valores ajustables) ====================
    // La ciudad se divide en cinco sectores rectangulares. El Centro es el cuadrado central de 5 × 5 celdas; alrededor,
    // el norte y el sur ocupan todo el ancho y el oeste y el este completan las franjas laterales. Los bordes (±25)
    // coinciden con el límite entre una calle y una manzana, así cada manzana pertenece a un solo sector.
    public static final float RADIO_CENTRO = 25; // Distancia del origen al borde del sector Centro.
    public static final String[] NOMBRES_SECTORES = { // Nombre de cada sector, en el mismo orden que SECTORES.
        "Centro", // Cuadrado central, con el parque del origen.
        "Barrio Norte", // Franja norte (Z negativa), de borde a borde.
        "Parque Sur", // Franja sur (Z positiva), de borde a borde.
        "Zona Oeste", // Franja lateral oeste, entre el norte y el sur (nombre corto: entra en el minimapa).
        "Zona Este" // Franja lateral este, entre el norte y el sur.
    };
    public static final float[][] SECTORES = { // Cada fila es un rectángulo {xMin, xMax, zMin, zMax}; se revisan en orden.
        {-RADIO_CENTRO, RADIO_CENTRO, -RADIO_CENTRO, RADIO_CENTRO}, // Centro.
        {-LIMITE, LIMITE, -LIMITE, -RADIO_CENTRO}, // Barrio Norte.
        {-LIMITE, LIMITE, RADIO_CENTRO, LIMITE}, // Parque Sur.
        {-LIMITE, -RADIO_CENTRO, -RADIO_CENTRO, RADIO_CENTRO}, // Zona Oeste.
        {RADIO_CENTRO, LIMITE, -RADIO_CENTRO, RADIO_CENTRO} // Zona Este.
    };

    /** Impide crear objetos: Mapa solo ofrece datos y cálculos estáticos. */
    private Mapa() {
    }

    /** Convierte el índice de una fila o columna al centro de su celda. */
    public static float centro(int indice) {
        return -LIMITE + TAM_CELDA * (indice + 0.5f); // Parte del borde negativo y avanza hasta la mitad de la celda.
    }

    /** Operación inversa de centro(): convierte una coordenada X o Z al índice de la celda que la contiene. */
    public static int indiceCelda(float coordenada) {
        return (int) Math.floor((coordenada + LIMITE) / TAM_CELDA); // Desplaza el borde negativo a cero y divide por el ancho de celda.
    }

    /** Devuelve el contenido de una celda: calle, edificio o parque. */
    public static int tipo(int fila, int columna) {
        return MAPA[fila][columna]; // Lee si la celda es calle, edificio o parque.
    }

    /** Indica si una celda es transitable. */
    public static boolean esCalle(int fila, int columna) {
        return MAPA[fila][columna] == CALLE; // Una celda con cero representa calle.
    }

    /** Indica si un punto del mundo (x, z) cae dentro del mapa y sobre una celda de calle. */
    public static boolean esCalleEn(float x, float z) {
        int fila = indiceCelda(z); // Las filas avanzan en Z.
        int columna = indiceCelda(x); // Las columnas avanzan en X.
        boolean dentro = fila >= 0 && fila < MAPA.length && columna >= 0 && columna < MAPA[fila].length; // Evita leer fuera de la matriz.
        return dentro && esCalle(fila, columna); // Solo es calle si está dentro y la celda vale cero.
    }

    /** Devuelve el índice del sector que contiene el punto (x, z), o -1 si está fuera de la ciudad. */
    public static int sector(float x, float z) {
        for (int indice = 0; indice < SECTORES.length; indice++) { // Revisa los sectores en orden: el primero que coincide gana.
            float[] r = SECTORES[indice]; // Rectángulo {xMin, xMax, zMin, zMax} del sector.
            if (x >= r[0] && x <= r[1] && z >= r[2] && z <= r[3]) { // Comprueba si el punto está dentro.
                return indice; // Sector encontrado.
            }
        }
        return -1; // El punto está fuera del mapa.
    }

    /** Devuelve el nombre del sector que contiene el punto (x, z); el HUD lo muestra como "Sector: ...". */
    public static String nombreSector(float x, float z) {
        int indice = sector(x, z); // Busca el sector.
        return indice >= 0 ? NOMBRES_SECTORES[indice] : "Fuera de la ciudad"; // Nombre o aviso.
    }

    // ==================== INTERSECCIONES, ACCESOS Y PARQUES ====================
    // Una INTERSECCIÓN es una celda de calle que tiene calle hacia el norte o el sur Y calle hacia el este o el oeste:
    // allí se cruzan dos calles. Con este MAPA son las celdas de fila y columna pares (36 en total).
    // Un ACCESO es cada calle que llega a una intersección; se describe con el desplazamiento {dFila, dColumna} de la
    // celda vecina por la que llegan los autos: {-1, 0} = llegan desde el norte, {1, 0} = desde el sur,
    // {0, -1} = desde el oeste y {0, 1} = desde el este.
    public static final int[][] VECINOS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}; // Norte, sur, oeste y este.

    /** Indica si (fila, columna) está dentro de la matriz y es calle. */
    public static boolean esCalleSegura(int fila, int columna) {
        boolean dentro = fila >= 0 && fila < MAPA.length && columna >= 0 && columna < MAPA[fila].length; // Evita salir de la matriz.
        return dentro && esCalle(fila, columna); // Fuera del mapa no hay calle.
    }

    /** Indica si la celda es una intersección: calle con calle al norte o sur y calle al este u oeste. */
    public static boolean esInterseccion(int fila, int columna) {
        if (!esCalleSegura(fila, columna)) { // Una manzana nunca es intersección.
            return false; // Descarta la celda.
        }
        boolean norteSur = esCalleSegura(fila - 1, columna) || esCalleSegura(fila + 1, columna); // Hay calle vertical.
        boolean esteOeste = esCalleSegura(fila, columna - 1) || esCalleSegura(fila, columna + 1); // Hay calle horizontal.
        return norteSur && esteOeste; // Se cruzan dos calles.
    }

    /** Lista todas las intersecciones como {fila, columna}, de norte a sur y de oeste a este. */
    public static List<int[]> intersecciones() {
        List<int[]> lista = new ArrayList<>(); // Resultado.
        for (int fila = 0; fila < MAPA.length; fila++) { // Recorre las filas.
            for (int columna = 0; columna < MAPA[fila].length; columna++) { // Recorre las columnas.
                if (esInterseccion(fila, columna)) { // Solo los cruces.
                    lista.add(new int[] {fila, columna}); // Guarda la intersección.
                }
            }
        }
        return lista; // Intersecciones del mapa.
    }

    /** Devuelve el índice de sector (en SECTORES) de una celda, usando su centro. */
    public static int sectorDeCelda(int fila, int columna) {
        return sector(centro(columna), centro(fila)); // Las columnas son X y las filas son Z.
    }

    /** Lista los accesos {dFila, dColumna} de una intersección: las calles vecinas por las que llegan autos. */
    public static List<int[]> accesos(int fila, int columna) {
        List<int[]> lista = new ArrayList<>(); // Resultado.
        for (int[] vecino : VECINOS) { // Revisa norte, sur, oeste y este.
            if (esCalleSegura(fila + vecino[0], columna + vecino[1])) { // Hay una calle que llega por ese lado.
                lista.add(vecino); // Es un acceso.
            }
        }
        return lista; // Accesos de la intersección.
    }

    /** Lista las celdas de parque como {fila, columna}. */
    public static List<int[]> parques() {
        List<int[]> lista = new ArrayList<>(); // Resultado.
        for (int fila = 0; fila < MAPA.length; fila++) { // Recorre las filas.
            for (int columna = 0; columna < MAPA[fila].length; columna++) { // Recorre las columnas.
                if (MAPA[fila][columna] == PARQUE) { // Solo los parques.
                    lista.add(new int[] {fila, columna}); // Guarda el parque.
                }
            }
        }
        return lista; // Parques del mapa.
    }
}
