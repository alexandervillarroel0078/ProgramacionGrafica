package com.graphics.ciudad.vehiculo; // Prueba las luces de vehículo desde su mismo paquete.

import com.graphics.ciudad.iluminacion.Iluminacion; // Dueña del estado de la tecla F.
import com.graphics.ciudad.motor.Figuras; // Mallas generadas en memoria, sin tocar la GPU.
import com.graphics.ciudad.motor.Shader; // Se crea sin tocar la GPU.
import java.util.Arrays; // Compara colores.
import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F; // Tecla de los faros.
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE; // Freno fuerte.

/** Comprueba las bombillas de faros y luces traseras del jugador sin abrir una ventana OpenGL. */
public class LucesVehiculoTest extends TestCase {

    /** Con F encendido los faros son emisivos (blanco cálido); con F apagado, gris oscuro sin emisión. */
    public void testFarosSiguenLaTeclaF() {
        Shader shader = new Shader(); // Programa sin compilar.
        Iluminacion iluminacion = new Iluminacion(shader, new Figuras(shader)); // Estado real de la tecla F.
        assertTrue(iluminacion.farosEncendidos()); // El juego arranca con los faros encendidos.
        float[] encendido = LucesVehiculo.colorFaro(iluminacion.farosEncendidos()); // Color con F encendido.
        assertEquals(1f, encendido[3], 0f); // Emisivo.
        assertTrue(Arrays.equals(Arrays.copyOf(encendido, 3), LucesVehiculo.COLOR_FARO_ENCENDIDO)); // Blanco cálido.
        iluminacion.tecla(GLFW_KEY_F); // Apaga los faros, igual que presionar F.
        float[] apagado = LucesVehiculo.colorFaro(iluminacion.farosEncendidos()); // Color con F apagado.
        assertEquals(0f, apagado[3], 0f); // Sin emisión.
        assertTrue(Arrays.equals(Arrays.copyOf(apagado, 3), LucesVehiculo.COLOR_FARO_APAGADO)); // Gris-beige oscuro.
    }

    /** Luces traseras: posición (tenue, emisiva) con F; apagadas sin F; el freno domina en ambos casos. */
    public void testTraserasYFreno() {
        float[] posicion = LucesVehiculo.colorTrasera(true, false); // F encendido, sin freno.
        assertEquals(1f, posicion[3], 0f); // Emisiva.
        assertEquals(LucesVehiculo.COLOR_POSICION[0], posicion[0], 0f); // Rojo tenue.
        float[] apagada = LucesVehiculo.colorTrasera(false, false); // F apagado, sin freno.
        assertEquals(0f, apagada[3], 0f); // Sin emisión.
        assertEquals(LucesVehiculo.COLOR_TRASERA_APAGADA[0], apagada[0], 0f); // Rojo oscuro.
        for (boolean faros : new boolean[] {true, false}) { // Con y sin F.
            float[] freno = LucesVehiculo.colorTrasera(faros, true); // Frenando.
            assertEquals(1f, freno[3], 0f); // Emisiva.
            assertEquals(LucesVehiculo.COLOR_FRENO[0], freno[0], 0f); // Rojo intenso: el freno domina.
        }
        assertTrue(LucesVehiculo.COLOR_FRENO[0] > LucesVehiculo.COLOR_POSICION[0]); // El freno es más intenso que la posición.
    }

    /** Integración con el auto: frenar con Espacio enciende el rojo intenso aunque F esté apagado. */
    public void testFrenoDelAutoConFApagado() {
        Auto auto = new Auto(); // Auto en la salida.
        auto.velocidad = 8; // Avanzando.
        auto.actualizar(1f / 60, tecla -> tecla == GLFW_KEY_SPACE); // Frena con Espacio.
        assertTrue(auto.frenando()); // Estado de freno.
        float[] trasera = LucesVehiculo.colorTrasera(false, auto.frenando()); // F apagado.
        assertEquals(LucesVehiculo.COLOR_FRENO[0], trasera[0], 0f); // Rojo intenso igual.
        assertEquals(1f, trasera[3], 0f); // Emisiva.
    }
}
