package com.graphics.ciudad; // Prueba Juego desde su mismo paquete (usa sus métodos de acceso para pruebas).

import com.graphics.ciudad.juego.EstadoPartida; // Estados menú, jugando y pausa.
import com.graphics.ciudad.trafico.Vehiculo; // Vehículos del tráfico.
import java.util.List; // Tipo de la lista de vehículos.
import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W; // Tecla de acelerar, simulada.

/** Comprueba la pausa y el menú de inicio sin abrir una ventana OpenGL. */
public class JuegoTest extends TestCase {

    private static final float DT = 0.05f; // Paso de tiempo de cada actualización simulada.
    private static final int CUADROS = 40; // Dos segundos de juego simulado.

    /** Guarda X y Z de cada vehículo del tráfico. */
    private static float[][] posiciones(List<Vehiculo> vehiculos) {
        float[][] copia = new float[vehiculos.size()][2]; // Una fila por vehículo.
        for (int i = 0; i < vehiculos.size(); i++) { // Recorre los vehículos.
            copia[i][0] = vehiculos.get(i).getX(); // X actual.
            copia[i][1] = vehiculos.get(i).getZ(); // Z actual.
        }
        return copia; // Posiciones en este instante.
    }

    /** En el menú y en pausa, actualizar no mueve el auto ni el tráfico aunque se mantenga W; jugando sí. */
    public void testPausaCongelaAutoYTrafico() {
        Juego juego = new Juego(); // Juego sin ventana: los constructores no usan OpenGL.
        juego.usarTeclado(tecla -> tecla == GLFW_KEY_W); // Simula que el usuario mantiene W (acelerar).
        assertEquals(EstadoPartida.Estado.MENU, juego.getEstado().getEstado()); // El juego abre en el menú.

        float autoX = juego.getAuto().getX(); // Posición inicial del auto.
        float autoZ = juego.getAuto().getZ(); // Posición inicial del auto.
        float[][] trafico = posiciones(juego.getTrafico().getVehiculos()); // Posición inicial del tráfico.
        for (int i = 0; i < CUADROS; i++) { // Dos segundos en el menú.
            juego.actualizar(DT); // Actualiza con dt real; el estado lo convierte en 0.
        }
        assertEquals(autoX, juego.getAuto().getX(), 0f); // El auto no se movió en X.
        assertEquals(autoZ, juego.getAuto().getZ(), 0f); // El auto no se movió en Z.
        assertTrue(java.util.Arrays.deepEquals(trafico, posiciones(juego.getTrafico().getVehiculos()))); // El tráfico tampoco.

        juego.getEstado().empezar(); // ENTER: comienza la partida.
        for (int i = 0; i < CUADROS; i++) { // Dos segundos jugando con W presionada.
            juego.actualizar(DT); // Ahora el tiempo avanza.
        }
        assertTrue(juego.getAuto().getZ() < autoZ - 1); // El auto avanzó hacia el norte (-Z).
        assertFalse(java.util.Arrays.deepEquals(trafico, posiciones(juego.getTrafico().getVehiculos()))); // El tráfico se movió.

        juego.getEstado().alternarPausa(); // P: pausa.
        assertEquals(EstadoPartida.Estado.PAUSA, juego.getEstado().getEstado()); // Está en pausa.
        float pausaX = juego.getAuto().getX(); // Posición al pausar.
        float pausaZ = juego.getAuto().getZ(); // Posición al pausar.
        float pausaVelocidad = juego.getAuto().getVelocidad(); // Velocidad al pausar.
        float[][] traficoPausa = posiciones(juego.getTrafico().getVehiculos()); // Tráfico al pausar.
        for (int i = 0; i < CUADROS; i++) { // Dos segundos en pausa con W presionada.
            juego.actualizar(DT); // dt efectivo = 0.
        }
        assertEquals(pausaX, juego.getAuto().getX(), 0f); // El auto quedó congelado en X.
        assertEquals(pausaZ, juego.getAuto().getZ(), 0f); // El auto quedó congelado en Z.
        assertEquals(pausaVelocidad, juego.getAuto().getVelocidad(), 0f); // Conserva la velocidad para reanudar igual.
        assertTrue(java.util.Arrays.deepEquals(traficoPausa, posiciones(juego.getTrafico().getVehiculos()))); // Tráfico congelado.

        juego.getEstado().alternarPausa(); // P otra vez: reanuda.
        juego.actualizar(DT); // Un cuadro jugando.
        assertTrue(juego.getAuto().getZ() < pausaZ); // El auto vuelve a moverse.
    }
}
