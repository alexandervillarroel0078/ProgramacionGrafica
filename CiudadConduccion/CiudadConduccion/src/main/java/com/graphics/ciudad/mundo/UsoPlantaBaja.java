package com.graphics.ciudad.mundo; // Agrupa lo que forma la ciudad: mapa, edificios, decoración y señales.

/**
 * USO DE LA PLANTA BAJA: qué hay a nivel de la vereda en cada edificio. Fachada lo usa para decidir qué dibuja en las
 * caras que dan a la calle, y Basureros para poner cestos solo junto a los negocios.
 *
 * REGLA (derivada del mapa, sin azar: la misma celda da siempre el mismo uso):
 *  - TORRE → LOBBY_OFICINAS: la torre es de oficinas; su podio es un hall de vidrio.
 *  - CASA_BAJA → CASA: una vivienda con su puerta y, en algunas, un portón de garaje.
 *  - BLOQUE, ESCALONADO y DOBLE → COMERCIAL si el edificio está en el Centro o da a una AVENIDA PRINCIPAL (donde pasa
 *    más gente); si no, RESIDENCIAL (el barrio: ventanas comunes y una puerta de departamentos).
 *
 * AVENIDAS PRINCIPALES: las calles (filas o columnas pares) a DISTANCIA_AVENIDA celdas o menos del eje central de la
 * ciudad, en las dos direcciones. En el mapa 11 × 11 el eje es la fila/columna 5 (manzanas), así que son las calles 4
 * y 6; en el 13 × 13 el eje es la calle 6, y con ella las 4 y 8. Un edificio da a una avenida si alguna de sus caras
 * mira a una celda de esa calle: una cara norte o sur mira a otra FILA; una cara oeste o este, a otra COLUMNA.
 * Con el mapa actual: 8 comerciales de 19 edificios (42 %), 3 residenciales, 4 casas y 4 lobbies. Con 13 × 13, la
 * mitad de los edificios queda comercial (con DISTANCIA_AVENIDA = 1 solo contaría la calle 6 y bajaría a ≈ 35 %).
 * Se comunica con: Edificio (tipo), Mapa (sectores y vecinos), Fachada y Basureros.
 */
public enum UsoPlantaBaja {

    /** Negocio: vidriera | puerta | vidriera, con toldos. */
    COMERCIAL,
    /** Hall de oficinas: vidrio de piso a techo, puerta doble al centro y marquesina plana. */
    LOBBY_OFICINAS,
    /** Departamentos: pared con ventanas comunes, puerta con escalón y un alero chico. */
    RESIDENCIAL,
    /** Casa: puerta con escalón, ventanas y, en algunas, portón de garaje. */
    CASA;

    // ==================== VALORES AJUSTABLES ====================
    public static final int SECTOR_COMERCIAL = 0; // Índice del Centro en Mapa.SECTORES: todo lo que no es torre ni casa es negocio.
    public static final int DISTANCIA_AVENIDA = 2; // Calles a esta distancia del eje (o menos) son avenidas principales.

    /** Uso de la planta baja del edificio de la celda (fila, columna). */
    public static UsoPlantaBaja de(int fila, int columna) {
        switch (Edificio.tipo(fila, columna)) { // Primero manda la forma del edificio.
            case TORRE:
                return LOBBY_OFICINAS; // Torre de oficinas.
            case CASA_BAJA:
                return CASA; // Vivienda.
            default: { // BLOQUE, ESCALONADO o DOBLE: depende de la zona.
                boolean enCentro = Mapa.sectorDeCelda(fila, columna) == SECTOR_COMERCIAL; // Zona comercial.
                return enCentro || daAAvenidaPrincipal(fila, columna) ? COMERCIAL : RESIDENCIAL; // Barrio si no.
            }
        }
    }

    /** Indica si la fila o columna "indice" es una calle de avenida principal (cerca del eje central). */
    public static boolean esAvenidaPrincipal(int indice) {
        int eje = Mapa.MAPA.length / 2; // Fila/columna central (el mapa es cuadrado).
        return indice % 2 == 0 && Math.abs(indice - eje) <= DISTANCIA_AVENIDA; // Calle y cerca del eje.
    }

    /** Indica si alguna cara del edificio mira a una celda de calle que pertenece a una avenida principal. */
    public static boolean daAAvenidaPrincipal(int fila, int columna) {
        for (int[] v : Mapa.VECINOS) { // Norte, sur, oeste y este.
            int f = fila + v[0]; // Celda vecina.
            int c = columna + v[1];
            if (!Mapa.esCalleSegura(f, c)) { // Esa cara no da a una calle.
                continue;
            }
            int indiceCalle = v[0] != 0 ? f : c; // Norte/sur: la calle es una fila; oeste/este: una columna.
            if (esAvenidaPrincipal(indiceCalle)) {
                return true; // Da a la avenida.
            }
        }
        return false; // Solo calles de barrio.
    }

    /** Indica si la planta baja es vidriada (negocio u hall): ahí no van las ventanas comunes del primer piso. */
    public boolean esVidriada() {
        return this == COMERCIAL || this == LOBBY_OFICINAS;
    }
}
