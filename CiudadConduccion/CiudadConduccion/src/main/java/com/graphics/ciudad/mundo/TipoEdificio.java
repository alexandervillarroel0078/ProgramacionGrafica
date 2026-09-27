package com.graphics.ciudad.mundo; // Agrupa lo que forma la ciudad: mapa, edificios, decoración y señales.

/**
 * TIPO DE EDIFICIO: la forma general de cada edificio y su patrón de ventanas.
 * Edificio elige un tipo por celda con Variacion (la misma celda da siempre el mismo tipo) y arma sus volúmenes;
 * Fachada usa el patrón para repartir las ventanas en cada cara.
 * Patrón de ventanas: cuántas columnas por cara (si no entran en una cara angosta se usan menos), la distancia entre
 * centros, el ancho y el alto de cada ventana, y la altura de un piso (separación vertical entre filas de ventanas).
 */
public enum TipoEdificio {

    // Medidas en metros (1 u ≈ 1 m). Una ventana real mide ≈ 1.2-1.5 de alto; el piso, Edificio.ALTO_PISO = 3.
    // Con alto 1.4 y el centro de la fila a 1.4 del piso (Fachada.PRIMER_PISO_Y), queda un antepecho de ≈ 0.7.

    /** Alta y angosta sobre un podio con el hall de oficinas; en la azotea, antena o tanque de agua. Ventanas angostas y juntas. */
    TORRE(3, 1.3f, 0.9f, 1.4f, Edificio.ALTO_PISO),
    /** El edificio clásico de altura media, con baranda en la azotea y caja de ascensor. Pocas ventanas, anchas. */
    BLOQUE(2, 3.0f, 2.0f, 1.4f, Edificio.ALTO_PISO),
    /** Dos o tres volúmenes apilados, cada uno más chico que el de abajo, cada uno con su losa. */
    ESCALONADO(3, 1.8f, 1.1f, 1.4f, Edificio.ALTO_PISO),
    /** Casa de 2 pisos con techo a dos aguas (prisma triangular) color teja. Pocas ventanas, más bajas. */
    CASA_BAJA(2, 3.2f, 1.1f, 1.2f, Edificio.ALTO_PISO),
    /** Dos volúmenes de distinta altura que comparten la manzana. */
    DOBLE(3, 1.6f, 0.9f, 1.4f, Edificio.ALTO_PISO);

    public final int columnasVentanas; // Ventanas por piso en una cara ancha.
    public final float separacionVentanas; // Distancia entre centros de ventanas vecinas.
    public final float anchoVentana; // Ancho de cada ventana.
    public final float altoVentana; // Alto de cada ventana.
    public final float alturaPiso; // Separación vertical entre filas de ventanas.

    /** Guarda el patrón de ventanas del tipo. */
    TipoEdificio(int columnasVentanas, float separacionVentanas, float anchoVentana, float altoVentana, float alturaPiso) {
        this.columnasVentanas = columnasVentanas; // Columnas en una cara ancha.
        this.separacionVentanas = separacionVentanas; // Paso horizontal.
        this.anchoVentana = anchoVentana; // Ancho.
        this.altoVentana = altoVentana; // Alto.
        this.alturaPiso = alturaPiso; // Paso vertical.
    }
}
