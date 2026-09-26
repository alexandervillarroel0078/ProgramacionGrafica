package com.graphics.ciudad.mundo; // Prueba el plano de la ciudad desde su mismo paquete.

import com.graphics.ciudad.iluminacion.Iluminacion; // Aporta las posiciones de las farolas.
import com.graphics.ciudad.juego.Entregas; // Aporta las posiciones de las paradas.
import com.graphics.ciudad.vehiculo.Auto; // Aporta la posición inicial del vehículo.
import java.util.ArrayDeque; // Cola de celdas pendientes para el recorrido en anchura (BFS).
import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba el tamaño, el contenido y la conectividad del mapa sin abrir una ventana OpenGL. */
public class MapaTest extends TestCase {

    /** Comprueba que los límites se calculan desde MAPA.length y TAM_CELDA. */
    public void testLimitesDelMapa() {
        assertEquals(11, Mapa.MAPA.length); // La ciudad tiene 11 filas.
        for (int[] fila : Mapa.MAPA) { // Revisa cada fila de la matriz.
            assertEquals(11, fila.length); // Exige 11 columnas: el mapa es cuadrado, como supone TAMANO.
        }
        assertEquals(110f, Mapa.TAMANO, 0f); // 11 celdas de 10 unidades.
        assertEquals(55f, Mapa.LIMITE, 0f); // Mitad del lado: distancia del origen a cada borde.
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

    /** Comprueba que destinos, farolas y posición inicial caen en celdas de calle. */
    public void testPosicionesSobreCalles() {
        for (float[] destino : Entregas.DESTINOS) { // Revisa cada parada.
            assertTrue("destino " + destino[0] + "," + destino[1], Mapa.esCalleEn(destino[0], destino[1])); // La parada debe estar sobre calle.
        }
        assertTrue(Iluminacion.LUCES.length >= 9); // Exige al menos nueve farolas.
        assertTrue(Iluminacion.LUCES.length <= Iluminacion.MAX_LUCES); // No puede superar el arreglo uLuces del shader.
        for (float[] luz : Iluminacion.LUCES) { // Revisa cada farola.
            assertTrue("farola " + luz[0] + "," + luz[2], Mapa.esCalleEn(luz[0], luz[2])); // X y Z de la farola deben caer en calle.
        }
        assertTrue(Mapa.esCalleEn(Auto.X_INICIAL, Auto.Z_INICIAL)); // El auto debe comenzar sobre calle.
        assertEquals(Entregas.DESTINOS.length, Entregas.NOMBRES_DESTINOS.length); // Cada parada tiene su nombre para el título.
    }
}
