package com.graphics.ciudad.trafico; // Prueba las luces del tráfico desde su mismo paquete.

import com.graphics.ciudad.iluminacion.Iluminacion; // Dueña del estado día/noche (tecla N) y de los faros del jugador (F).
import com.graphics.ciudad.motor.Cubo; // Se crea sin tocar la GPU: solo guarda referencias.
import com.graphics.ciudad.motor.Shader; // Se crea sin tocar la GPU: OpenGL se usa recién en crear().
import com.graphics.ciudad.vehiculo.LucesVehiculo; // Ubicación de las luces, compartida con el jugador.
import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F; // Tecla de los faros del jugador.
import static org.lwjgl.glfw.GLFW.GLFW_KEY_N; // Tecla de día/noche.

/** Comprueba que las luces del tráfico siguen a día/noche y no a la tecla F, sin abrir una ventana OpenGL. */
public class LucesTraficoTest extends TestCase {

    private Iluminacion iluminacion; // Estado real de día/noche, el mismo que usa Juego.
    private Trafico trafico; // Tráfico conectado a esa iluminación.

    /** Crea la iluminación y el tráfico como lo hace Juego: new Trafico(..., iluminacion::esNoche). */
    @Override
    protected void setUp() {
        Shader shader = new Shader(); // Programa sin compilar: la prueba no dibuja.
        Cubo cubo = new Cubo(shader); // Geometría sin subir a la GPU.
        iluminacion = new Iluminacion(shader, cubo); // Empieza de día (NOCHE_AL_INICIAR) y con los faros del jugador encendidos.
        if (!iluminacion.esNoche()) { // Las pruebas parten de noche, que es cuando el tráfico enciende sus luces.
            iluminacion.tecla(GLFW_KEY_N); // Igual que presionar N.
        }
        trafico = new Trafico(shader, cubo, iluminacion::esNoche); // El tráfico consulta a Iluminacion, sin copiar el estado.
    }

    /** Devuelve true si TODOS los vehículos tienen las luces en el estado esperado. */
    private boolean todas(boolean encendidas) {
        for (Vehiculo v : trafico.getVehiculos()) { // Revisa cada vehículo.
            if (v.lucesEncendidas() != encendidas) { // Alguno no coincide.
                return false; // Falla la condición.
            }
        }
        return true; // Todos coinciden.
    }

    /** El juego arranca de día, como conviene para la demo, y con el tráfico con las luces apagadas. */
    public void testArrancaDeDia() {
        Shader shader = new Shader(); // Programa sin compilar.
        Iluminacion recienCreada = new Iluminacion(shader, new Cubo(shader)); // Estado inicial real del juego.
        assertEquals(Iluminacion.NOCHE_AL_INICIAR, recienCreada.esNoche()); // Respeta la constante.
        assertFalse(recienCreada.esNoche()); // La demo empieza de día.
        Trafico traficoDeDia = new Trafico(shader, new Cubo(shader), recienCreada::esNoche); // Tráfico conectado a esa iluminación.
        for (Vehiculo v : traficoDeDia.getVehiculos()) { // Cada vehículo.
            assertFalse(v.lucesEncendidas()); // De día arranca con las luces apagadas.
        }
    }

    /** Con noche activa todas las luces están encendidas; con día, todas apagadas. */
    public void testLucesSiguenDiaYNoche() {
        assertTrue(iluminacion.esNoche()); // setUp dejó la escena de noche.
        assertTrue(todas(true)); // De noche: encendidas.
        iluminacion.tecla(GLFW_KEY_N); // Cambia a día, igual que presionar N.
        assertFalse(iluminacion.esNoche()); // Ahora es de día.
        assertTrue(todas(false)); // De día: apagadas.
        iluminacion.tecla(GLFW_KEY_N); // Vuelve a la noche.
        assertTrue(todas(true)); // Se encienden otra vez.
    }

    /** La tecla F cambia los faros del jugador, pero no las luces del tráfico. */
    public void testTeclaFNoAfectaAlTrafico() {
        boolean farosJugador = iluminacion.farosEncendidos(); // Estado inicial de los faros del jugador.
        iluminacion.tecla(GLFW_KEY_F); // Presiona F de noche.
        assertTrue(farosJugador != iluminacion.farosEncendidos()); // F sí cambió los faros del jugador.
        assertTrue(todas(true)); // El tráfico sigue con luces encendidas.
        iluminacion.tecla(GLFW_KEY_N); // Pasa a día.
        iluminacion.tecla(GLFW_KEY_F); // Presiona F de día.
        assertTrue(todas(false)); // El tráfico sigue apagado: solo lo decide día/noche.
    }

    /** Los faros acompañan la posición y la orientación del vehículo, también durante los giros. */
    public void testFarosSiguenAlVehiculo() {
        float distanciaEsperada = (float) Math.hypot(LucesVehiculo.LADO_LUZ, LucesVehiculo.FRENTE_LUZ); // Del centro a cada faro.
        for (int paso = 0; paso < 60 * 30; paso++) { // Treinta segundos: incluye varias esquinas.
            trafico.actualizar(1f / 60, 1000, 1000); // Jugador lejos.
            for (Vehiculo v : trafico.getVehiculos()) { // Revisa cada vehículo.
                float frenteX = -(float) Math.sin(v.getAngulo()); // Frente actual en X.
                float frenteZ = -(float) Math.cos(v.getAngulo()); // Frente actual en Z.
                for (int lado = 0; lado < Trafico.FAROS_POR_VEHICULO; lado++) { // Faro izquierdo y derecho.
                    float[] faro = v.posicionFaro(lado); // Posición del faro en el mundo.
                    float relX = faro[0] - v.getX(); // Vector del centro al faro, en X.
                    float relZ = faro[2] - v.getZ(); // Vector del centro al faro, en Z.
                    assertEquals(distanciaEsperada, (float) Math.hypot(relX, relZ), 1e-3f); // Siempre a la misma distancia.
                    assertEquals(-LucesVehiculo.FRENTE_LUZ, relX * frenteX + relZ * frenteZ, 1e-3f); // Siempre adelante, girando con él.
                }
            }
        }
        assertTrue(trafico.getVehiculos().size() * Trafico.FAROS_POR_VEHICULO <= Trafico.MAX_FAROS_TRAFICO); // Entran todos en el shader.
    }
}
