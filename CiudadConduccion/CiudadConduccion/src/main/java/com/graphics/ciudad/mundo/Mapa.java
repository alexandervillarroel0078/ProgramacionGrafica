package com.graphics.ciudad.mundo; // Agrupa lo que forma la ciudad: mapa, edificios, decoración y semáforos.

/**
 * MAPA: plano de la ciudad descrito como una matriz de celdas.
 * Responsable de: guardar MAPA (0 = calle, 1 = edificio, 2 = parque), el tamaño de cada celda, los límites
 * calculados a partir de MAPA.length, la conversión entre índices y coordenadas y la altura de cada edificio.
 * Coordenadas: X = izquierda/derecha; Y = altura; Z = profundidad. Las filas corresponden a Z y las columnas a X.
 * Se comunica con: Ciudad y Decoracion (lo recorren para dibujar), Colisiones (lo recorre para bloquear al auto),
 * Juego (pasa LIMITE a Camara) y Minimapa (ajusta la vista superior a LIMITE).
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

    /** Calcula la altura del edificio de una celda; Ciudad y Decoracion usan el mismo valor. */
    public static float alturaEdificio(int fila, int columna) {
        return 5 + (fila * 3 + columna * 7) % 9; // Varía la altura de forma reproducible entre 5 y 13.
    }
}
