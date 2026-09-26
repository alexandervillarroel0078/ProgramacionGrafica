package com.graphics.ciudad.vehiculo; // Permite modificar el estado del auto desde el mismo paquete.

import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba el reinicio del auto sin abrir una ventana OpenGL. */
public class AutoTest extends TestCase {

    /** Comprueba que el reinicio restaura la posición y detiene el vehículo. */
    public void testReinicio() {
        Auto auto = new Auto(); // Crea el vehículo sin inicializar OpenGL.
        auto.x = 12; // Simula que el auto se desplazó horizontalmente.
        auto.z = 4; // Simula un desplazamiento en profundidad.
        auto.velocidad = 10; // Simula que el auto está avanzando.
        auto.angulo = 2; // Simula que el vehículo cambió de orientación.
        auto.reset(); // Ejecuta el mismo reinicio que se activa con R.
        assertEquals(Auto.X_INICIAL, auto.getX(), 0f); // Exige recuperar la coordenada X inicial.
        assertEquals(Auto.Z_INICIAL, auto.getZ(), 0f); // Exige recuperar la coordenada Z inicial.
        assertEquals(0f, auto.getVelocidad(), 0f); // Exige que el vehículo quede detenido.
        assertEquals(0f, auto.getAngulo(), 0f); // Exige recuperar la dirección frontal inicial.
    }
}
