package com.graphics.ciudad.mundo; // Prueba el plano de la ciudad desde su mismo paquete.

import com.graphics.ciudad.juego.Entregas; // Aporta las posiciones de las paradas.
import com.graphics.ciudad.vehiculo.Auto; // Aporta la posición inicial del vehículo.
import java.util.ArrayDeque; // Cola de celdas pendientes para el recorrido en anchura (BFS).
import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba el tamaño, el contenido y la conectividad del mapa sin abrir una ventana OpenGL. */
public class MapaTest extends TestCase {

    /** La matriz es cuadrada, de al menos 11 × 11 e impar (bordes de calle), con celdas de 10 como el proyecto original. */
    public void testLimitesDelMapa() {
        assertTrue("filas=" + Mapa.MAPA.length, Mapa.MAPA.length >= 11); // Al menos 11 filas.
        assertEquals(1, Mapa.MAPA.length % 2); // Impar: la primera y la última fila/columna son calle.
        for (int[] fila : Mapa.MAPA) { // Revisa cada fila de la matriz.
            assertEquals(Mapa.MAPA.length, fila.length); // Mapa cuadrado, como supone TAMANO.
        }
        assertEquals(10f, Mapa.TAM_CELDA, 0f); // Mismo tamaño de celda que la ciudad original 7 × 7 (límite 35 = 7 · 10 / 2).
    }

    /** La salida está sobre la calle, a mitad de cuadra en el carril derecho: ni en un cruce ni sobre un paso peatonal. */
    public void testSalidaAMitadDeCuadra() {
        int fila = Mapa.indiceCelda(Auto.Z_INICIAL); // Z → fila.
        int columna = Mapa.indiceCelda(Auto.X_INICIAL); // X → columna.
        assertTrue(Mapa.esCalleSegura(fila, columna)); // Sobre la calle.
        assertFalse(Mapa.esInterseccion(fila, columna)); // No en un cruce.
        assertEquals(Mapa.centro(fila), Auto.Z_INICIAL, 1e-4f); // A mitad de la cuadra (centro de la celda en Z).
        assertEquals(Mapa.centro(columna) + Mapa.TAM_CELDA / 4, Auto.X_INICIAL, 1e-4f); // Carril derecho mirando al norte.
        assertFalse(Decoracion.hayPasoSobre(Auto.X_INICIAL, Auto.Z_INICIAL, 1.1f, 1.6f)); // El auto (≈ 2.2 × 3.2) no pisa un paso.
    }

    /** Comprueba que TAMANO y LIMITE salen de la matriz (filas × TAM_CELDA) y no de un número fijo. */
    public void testLimiteDerivadoDeLaMatriz() {
        assertEquals(Mapa.MAPA.length * Mapa.TAM_CELDA, Mapa.TAMANO, 0f); // Lado de la ciudad a partir de la matriz.
        assertEquals(Mapa.MAPA.length * Mapa.TAM_CELDA / 2, Mapa.LIMITE, 0f); // Mitad del lado.
        assertEquals(-Mapa.LIMITE, Mapa.centro(0) - Mapa.TAM_CELDA / 2, 1e-4f); // La primera celda empieza en el borde oeste/norte.
        assertEquals(Mapa.LIMITE, Mapa.centro(Mapa.MAPA.length - 1) + Mapa.TAM_CELDA / 2, 1e-4f); // La última termina en el borde este/sur.
    }

    /** RADIO_CENTRO es proporcional a LIMITE (25 con el mapa 11 × 11) y cae en un borde de celda. */
    public void testRadioCentroProporcional() {
        if (Mapa.LIMITE == 55) { // Ciudad 11 × 11: el valor de siempre.
            assertEquals(25f, Mapa.RADIO_CENTRO, 0f);
        }
        assertTrue(Math.abs(Mapa.RADIO_CENTRO - Mapa.FRACCION_CENTRO * Mapa.LIMITE) <= Mapa.TAM_CELDA + 1e-4f); // Proporcional (±1 celda).
        float desdeElBorde = (Mapa.RADIO_CENTRO + Mapa.LIMITE) / Mapa.TAM_CELDA; // Celdas desde el borde oeste.
        assertEquals(Math.round(desdeElBorde), desdeElBorde, 1e-4f); // Número entero de celdas: no corta una manzana.
        int adentro = Mapa.indiceCelda(Mapa.RADIO_CENTRO - Mapa.TAM_CELDA / 2); // Última celda del Centro (hacia el este).
        int afuera = Mapa.indiceCelda(Mapa.RADIO_CENTRO + Mapa.TAM_CELDA / 2); // Primera celda de afuera.
        assertEquals(1, adentro % 2); // Adentro, una manzana...
        assertEquals(0, afuera % 2); // ...y afuera, una calle.
        int cruces = 0; // El Centro tiene cruces (y por lo tanto semáforos).
        for (int[] c : Mapa.intersecciones()) {
            cruces += Mapa.sectorDeCelda(c[0], c[1]) == 0 ? 1 : 0;
        }
        assertTrue(cruces > 0);
    }

    /** Recorre las calles en anchura (BFS) desde la posición inicial del auto y exige llegar a cada destino. */
    public void testDestinosAlcanzablesDesdeElInicio() {
        int filaInicio = Mapa.indiceCelda(Auto.Z_INICIAL); // Las filas avanzan en Z.
        int columnaInicio = Mapa.indiceCelda(Auto.X_INICIAL); // Las columnas avanzan en X.
        assertTrue(Mapa.esCalleSegura(filaInicio, columnaInicio)); // El recorrido debe partir de una calle.
        boolean[][] alcanzada = new boolean[Mapa.MAPA.length][Mapa.MAPA[0].length]; // Calles a las que se llega desde el inicio.
        ArrayDeque<int[]> pendientes = new ArrayDeque<>(); // Cola del BFS.
        alcanzada[filaInicio][columnaInicio] = true; // Marca el origen.
        pendientes.add(new int[] {filaInicio, columnaInicio}); // Lo coloca en la cola.
        while (!pendientes.isEmpty()) { // Continúa mientras queden celdas por expandir.
            int[] actual = pendientes.poll(); // Toma la celda más antigua.
            for (int[] paso : Mapa.VECINOS) { // Norte, sur, oeste y este.
                int fila = actual[0] + paso[0]; // Fila del vecino.
                int columna = actual[1] + paso[1]; // Columna del vecino.
                if (Mapa.esCalleSegura(fila, columna) && !alcanzada[fila][columna]) { // Solo calles nuevas dentro del mapa.
                    alcanzada[fila][columna] = true; // Marca el vecino.
                    pendientes.add(new int[] {fila, columna}); // Lo agrega a la cola.
                }
            }
        }
        for (float[] destino : Entregas.DESTINOS) { // Revisa cada parada.
            int fila = Mapa.indiceCelda(destino[1]); // Z → fila.
            int columna = Mapa.indiceCelda(destino[0]); // X → columna.
            assertTrue("destino " + destino[0] + "," + destino[1], Mapa.esCalleSegura(fila, columna) && alcanzada[fila][columna]); // Se llega por calle.
        }
    }

    /** Comprueba que hay al menos 12 manzanas con edificio y 4 parques. */
    public void testCantidadDeManzanas() {
        int edificios = 0; // Contador de celdas con edificio.
        int parques = 0; // Contador de celdas con parque.
        for (int[] fila : Mapa.MAPA) { // Recorre las filas.
            for (int celda : fila) { // Recorre las celdas de la fila.
                if (celda == Mapa.EDIFICIO) { // Detecta un edificio.
                    edificios++; // Suma una manzana edificada.
                }
                if (celda == Mapa.PARQUE) { // Detecta un parque.
                    parques++; // Suma un parque.
                }
            }
        }
        assertTrue("edificios=" + edificios, edificios >= 12); // Exige el mínimo de manzanas con edificio.
        assertTrue("parques=" + parques, parques >= 4); // Exige el mínimo de parques.
    }

    /** Recorre las calles en anchura (BFS) desde la primera calle y exige alcanzar todas. */
    public void testCallesConectadas() {
        int filas = Mapa.MAPA.length; // Cantidad de filas de la matriz.
        int columnas = Mapa.MAPA[0].length; // Cantidad de columnas de la matriz.
        boolean[][] visitada = new boolean[filas][columnas]; // Marca las calles ya alcanzadas.
        ArrayDeque<int[]> pendientes = new ArrayDeque<>(); // Celdas alcanzadas cuyos vecinos faltan revisar.
        int totalCalles = 0; // Cantidad de celdas de calle del mapa.
        for (int fila = 0; fila < filas; fila++) { // Recorre las filas.
            for (int columna = 0; columna < columnas; columna++) { // Recorre las columnas.
                if (Mapa.esCalle(fila, columna)) { // Cuenta solo las celdas transitables.
                    totalCalles++; // Suma una calle.
                    if (pendientes.isEmpty() && totalCalles == 1) { // La primera calle encontrada inicia el recorrido.
                        visitada[fila][columna] = true; // Marca el origen del BFS.
                        pendientes.add(new int[] {fila, columna}); // Lo coloca en la cola.
                    }
                }
            }
        }
        int alcanzadas = 0; // Calles a las que se llegó desde el origen.
        int[][] vecinos = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}; // Norte, sur, oeste y este: el auto no cruza en diagonal.
        while (!pendientes.isEmpty()) { // Continúa mientras queden celdas por expandir.
            int[] actual = pendientes.poll(); // Toma la celda más antigua de la cola.
            alcanzadas++; // Cuenta la celda como alcanzada.
            for (int[] paso : vecinos) { // Revisa las cuatro celdas vecinas.
                int fila = actual[0] + paso[0]; // Fila del vecino.
                int columna = actual[1] + paso[1]; // Columna del vecino.
                boolean dentro = fila >= 0 && fila < filas && columna >= 0 && columna < columnas; // Evita salir de la matriz.
                if (dentro && !visitada[fila][columna] && Mapa.esCalle(fila, columna)) { // Solo avanza por calles nuevas.
                    visitada[fila][columna] = true; // Marca el vecino para no repetirlo.
                    pendientes.add(new int[] {fila, columna}); // Lo agrega a la cola.
                }
            }
        }
        assertTrue(totalCalles > 0); // El mapa debe tener calles.
        assertEquals(totalCalles, alcanzadas); // Todas las calles forman una única red conectada.
    }

    /** Comprueba que hay entre 4 y 5 sectores con nombre y que cada punto de la ciudad pertenece a uno. */
    public void testSectores() {
        assertEquals(Mapa.SECTORES.length, Mapa.NOMBRES_SECTORES.length); // Cada sector tiene su nombre.
        assertTrue(Mapa.SECTORES.length >= 4 && Mapa.SECTORES.length <= 5); // Entre 4 y 5 sectores.
        assertEquals("Centro", Mapa.nombreSector(0, 0)); // El origen está en el Centro.
        assertEquals("Barrio Norte", Mapa.nombreSector(0, Mapa.centro(0))); // La avenida norte pertenece al Barrio Norte.
        float[] tercera = Entregas.DESTINOS[Entregas.DESTINOS.length - 1]; // Última entrega.
        assertEquals("Parque Sur", Mapa.nombreSector(tercera[0], tercera[1])); // La tercera entrega está en el sur.
        for (float x = -Mapa.LIMITE; x <= Mapa.LIMITE; x += 2.5f) { // Recorre la ciudad en X.
            for (float z = -Mapa.LIMITE; z <= Mapa.LIMITE; z += 2.5f) { // Recorre la ciudad en Z.
                assertTrue(x + "," + z, Mapa.sector(x, z) >= 0); // Ningún punto queda sin sector.
            }
        }
        assertEquals(-1, Mapa.sector(0, Mapa.LIMITE + 1)); // Fuera del mapa no hay sector.
    }

    /** Comprueba que destinos y posición inicial caen en celdas de calle. */
    public void testPosicionesSobreCalles() {
        for (float[] destino : Entregas.DESTINOS) { // Revisa cada parada.
            assertTrue("destino " + destino[0] + "," + destino[1], Mapa.esCalleEn(destino[0], destino[1])); // La parada debe estar sobre calle.
        }
        // Las farolas ya no van en la calle sino en la vereda: su ubicación se prueba en iluminacion/FarolasTest.
        assertTrue(Mapa.esCalleEn(Auto.X_INICIAL, Auto.Z_INICIAL)); // El auto debe comenzar sobre calle.
        assertEquals(Entregas.DESTINOS.length, Entregas.NOMBRES_DESTINOS.length); // Cada parada tiene su nombre para el título.
    }
}
