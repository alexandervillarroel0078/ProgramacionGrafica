package com.graphics.ciudad.mundo; // Mapas de referencia para las pruebas, junto a Mapa.

/**
 * MAPAS DE PRUEBA: ciudades fijas para que las pruebas y la defensa sean reproducibles.
 * MAPA_13 es la ciudad 13 × 13 de referencia. pom.xml corre todas las pruebas una segunda vez con
 * -Dciudad.mapa=com.graphics.ciudad.mundo.MapasDePrueba#MAPA_13 (Mapa.PROPIEDAD_MAPA), así que cada "mvn test" prueba
 * también esta ciudad sin editar Mapa.java. Las mismas filas están en ENTREGA.md, listas para pegar en Mapa.MAPA
 * (MapaTest comprueba que ambas copias coincidan).
 * Reglas que cumple: cuadrado impar, calles en filas y columnas pares, 36 manzanas (26 edificios y 10 parques) y
 * ningún parque en {11, 1}, la manzana junto a la salida del auto (ver MapaTest.testSinParqueJuntoALaSalida).
 */
public final class MapasDePrueba {

    /** Nombre del campo tal como lo espera Mapa.PROPIEDAD_MAPA. */
    public static final String PROPIEDAD_13 = "com.graphics.ciudad.mundo.MapasDePrueba#MAPA_13";

    // Ciudad de 13 × 13 celdas: 0 = calle, 1 = edificio, 2 = parque.
    public static final int[][] MAPA_13 = {
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Fila 0: calle del borde norte.
        {0, 1, 0, 1, 0, 2, 0, 1, 0, 1, 0, 1, 0}, // Fila 1: parque en la columna 5.
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Fila 2.
        {0, 2, 0, 1, 0, 1, 0, 1, 0, 1, 0, 2, 0}, // Fila 3: parques en los extremos oeste y este.
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Fila 4.
        {0, 1, 0, 1, 0, 2, 0, 1, 0, 2, 0, 1, 0}, // Fila 5: parques en las columnas 5 y 9.
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Fila 6: calle central (eje de la ciudad).
        {0, 1, 0, 2, 0, 1, 0, 1, 0, 2, 0, 1, 0}, // Fila 7: parques en las columnas 3 y 9.
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Fila 8.
        {0, 1, 0, 1, 0, 2, 0, 1, 0, 1, 0, 1, 0}, // Fila 9: parque en la columna 5.
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, // Fila 10.
        {0, 1, 0, 1, 0, 1, 0, 2, 0, 2, 0, 1, 0}, // Fila 11: la salida está a su izquierda (columna 0): {11, 1} es edificio.
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0} // Fila 12: calle del borde sur.
    };

    /** Impide crear objetos: solo guarda datos. */
    private MapasDePrueba() {
    }
}
