package com.graphics.ciudad.vehiculo; // Permite modificar el estado del auto desde el mismo paquete.

import java.util.function.IntPredicate; // Teclas simuladas.
import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A; // Doblar a la izquierda.
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S; // Frenar / retroceder.
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE; // Freno fuerte.
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W; // Acelerar.

/** Comprueba reinicio, ruedas, dirección y luces del auto sin abrir una ventana OpenGL. */
public class AutoTest extends TestCase {

    /**
     * Tras R el auto queda en el carril derecho: a la derecha del centro de su calle, sin pisar la línea amarilla y
     * sin salirse de la calzada. La derecha del auto es (cos a, -sen a), porque el frente es (-sen a, -cos a).
     */
    public void testSalidaEnElCarrilDerecho() {
        Auto auto = new Auto(); // Auto nuevo, en la salida.
        auto.x = 10; // Lo mueve a otro lugar...
        auto.z = -30;
        auto.angulo = 2; // ...y lo gira.
        auto.reset(); // R.
        float centroX = com.graphics.ciudad.mundo.Mapa.centro(com.graphics.ciudad.mundo.Mapa.indiceCelda(auto.getX())); // Centro de la calle en X.
        float centroZ = com.graphics.ciudad.mundo.Mapa.centro(com.graphics.ciudad.mundo.Mapa.indiceCelda(auto.getZ())); // Y en Z.
        float a = auto.getAngulo(); // Orientación tras el reinicio.
        float lateral = (auto.getX() - centroX) * (float) Math.cos(a) - (auto.getZ() - centroZ) * (float) Math.sin(a); // Distancia hacia la derecha.
        assertTrue("lateral=" + lateral, lateral > 0); // A la derecha del centro.
        assertEquals(Auto.CARRIL_SALIDA, lateral, 1e-4f); // En el centro del carril derecho.
        assertTrue(lateral - Auto.RADIO_AUTO > 0); // No toca la línea amarilla.
        assertTrue(lateral + Auto.RADIO_AUTO < com.graphics.ciudad.mundo.Mapa.TAM_CELDA / 2); // Ni la vereda.
    }

    private static final float DT = 1f / 60; // Paso fijo de simulación.
    private static final IntPredicate NINGUNA = tecla -> false; // Sin teclas presionadas.

    /** Teclas simuladas: devuelve true solo para las indicadas. */
    private static IntPredicate teclas(int... presionadas) {
        return tecla -> { // Consulta de una tecla.
            for (int p : presionadas) { // Revisa las presionadas.
                if (p == tecla) { // Coincide.
                    return true; // Está presionada.
                }
            }
            return false; // No está presionada.
        };
    }

    /** Comprueba que el reinicio restaura la posición y detiene el vehículo. */
    public void testReinicio() {
        Auto auto = new Auto(); // Crea el vehículo sin inicializar OpenGL.
        auto.x = 12; // Simula que el auto se desplazó horizontalmente.
        auto.z = 4; // Simula un desplazamiento en profundidad.
        auto.velocidad = 10; // Simula que el auto está avanzando.
        auto.angulo = 2; // Simula que el vehículo cambió de orientación.
        auto.anguloRueda = 5; // Simula ruedas giradas.
        auto.anguloDireccion = 0.3f; // Simula la dirección girada.
        auto.reset(); // Ejecuta el mismo reinicio que se activa con R.
        assertEquals(Auto.X_INICIAL, auto.getX(), 0f); // Exige recuperar la coordenada X inicial.
        assertEquals(Auto.Z_INICIAL, auto.getZ(), 0f); // Exige recuperar la coordenada Z inicial.
        assertEquals(0f, auto.getVelocidad(), 0f); // Exige que el vehículo quede detenido.
        assertEquals(0f, auto.getAngulo(), 0f); // Exige recuperar la dirección frontal inicial.
        assertEquals(0f, auto.getAnguloRueda(), 0f); // Ruedas en su posición inicial.
        assertEquals(0f, auto.getAnguloDireccion(), 0f); // Dirección derecha.
    }

    /** Recorrer 2π · RADIO_RUEDA gira la rueda exactamente 2π (una vuelta); en reversa el ángulo es negativo. */
    public void testAnguloDeRueda() {
        float circunferencia = (float) (2 * Math.PI * Auto.RADIO_RUEDA); // Distancia de una vuelta completa.
        assertEquals((float) (2 * Math.PI), Auto.giroPorDistancia(circunferencia), 1e-5f); // Una vuelta = 2π.
        assertEquals((float) (-2 * Math.PI), Auto.giroPorDistancia(-circunferencia), 1e-5f); // Hacia atrás: -2π.

        Auto auto = new Auto(); // Sale hacia el norte por la calle del borde oeste (recta de 100 unidades).
        float zInicial = auto.getZ(); // Posición de partida.
        for (int i = 0; i < 120; i++) { // Dos segundos acelerando.
            auto.actualizar(DT, teclas(GLFW_KEY_W)); // W presionada.
        }
        float avanzado = zInicial - auto.getZ(); // Hacia el norte Z disminuye: distancia recorrida.
        assertTrue(avanzado > circunferencia); // Recorrió más de una vuelta de rueda.
        assertEquals(Auto.giroPorDistancia(avanzado), auto.getAnguloRueda(), 1e-3f); // Ángulo = distancia / radio.

        Auto atras = new Auto(); // Otro auto, detenido en la salida.
        for (int i = 0; i < 60; i++) { // Un segundo con S desde detenido: reversa.
            atras.actualizar(DT, teclas(GLFW_KEY_S)); // S presionada.
        }
        assertTrue(atras.getVelocidad() < 0); // Va hacia atrás.
        assertTrue(atras.getAnguloRueda() < 0); // Las ruedas giran hacia atrás: ángulo negativo.
    }

    /** Las delanteras nunca superan ANGULO_MAX_DIRECCION y vuelven a 0 al soltar la tecla. */
    public void testDireccion() {
        Auto auto = new Auto(); // Auto nuevo.
        float maximo = 0; // Mayor ángulo observado.
        for (int i = 0; i < 120; i++) { // Dos segundos con A presionada.
            auto.actualizar(DT, teclas(GLFW_KEY_A)); // Dobla a la izquierda.
            maximo = Math.max(maximo, Math.abs(auto.getAnguloDireccion())); // Registra el máximo.
            assertTrue(Math.abs(auto.getAnguloDireccion()) <= Auto.ANGULO_MAX_DIRECCION + 1e-6f); // Nunca más de 30°.
        }
        assertEquals(Auto.ANGULO_MAX_DIRECCION, maximo, 1e-5f); // Llega al tope manteniendo la tecla.
        assertTrue(auto.getAnguloDireccion() > 0); // Izquierda = positivo.
        for (int i = 0; i < 120; i++) { // Dos segundos sin teclas.
            auto.actualizar(DT, NINGUNA); // Suelta la dirección.
        }
        assertEquals(0f, auto.getAnguloDireccion(), 1e-6f); // Volvió al centro.
    }

    /** Freno: S yendo hacia adelante o Espacio; reversa: velocidad negativa. */
    public void testFrenoYReversa() {
        Auto auto = new Auto(); // Auto nuevo.
        auto.velocidad = 10; // Avanzando.
        auto.actualizar(DT, NINGUNA); // Rueda libre.
        assertFalse(auto.frenando()); // Sin freno.
        assertFalse(auto.enReversa()); // Hacia adelante.
        auto.actualizar(DT, teclas(GLFW_KEY_S)); // S mientras avanza.
        assertTrue(auto.frenando()); // S frena.
        auto.actualizar(DT, teclas(GLFW_KEY_SPACE)); // Espacio.
        assertTrue(auto.frenando()); // Espacio frena.
        auto.actualizar(DT, teclas(GLFW_KEY_W)); // Acelera.
        assertFalse(auto.frenando()); // Se apagan las luces de freno.

        Auto atras = new Auto(); // Auto detenido.
        for (int i = 0; i < 60; i++) { // S desde detenido: entra en reversa.
            atras.actualizar(DT, teclas(GLFW_KEY_S)); // S presionada.
        }
        assertTrue(atras.enReversa()); // Luz de reversa encendida.
        assertFalse(atras.frenando()); // S en reversa acelera hacia atrás: no es freno.
        atras.actualizar(DT, teclas(GLFW_KEY_SPACE)); // Espacio en reversa.
        assertTrue(atras.frenando()); // Espacio siempre enciende las luces de freno.
        for (int i = 0; i < 300; i++) { // Cinco segundos frenando con Espacio.
            atras.actualizar(DT, teclas(GLFW_KEY_SPACE)); // Se detiene.
        }
        assertFalse(atras.enReversa()); // Detenido: la luz de reversa se apaga.
    }
}
