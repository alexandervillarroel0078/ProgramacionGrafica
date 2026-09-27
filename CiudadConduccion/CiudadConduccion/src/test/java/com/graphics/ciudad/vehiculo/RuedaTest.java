package com.graphics.ciudad.vehiculo; // Prueba la rueda compartida desde su mismo paquete.

import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba la rueda compartida por el jugador y el tráfico sin abrir una ventana OpenGL. */
public class RuedaTest extends TestCase {

    private static final float LARGO_CARROCERIA = 2.6f; // Medidas de la carrocería de Auto y Vehiculo.
    private static final float ANCHO_CARROCERIA = 1.65f;
    private static final float BASE_CARROCERIA = 0.65f - 0.55f / 2; // 0.375: altura del borde inferior de la carrocería.

    /**
     * Se apoyan en el asfalto (el centro está a RADIO_RUEDA de Y = 0) y quedan bien ubicadas: dentro del largo de la
     * carrocería, asomando por los costados como máximo el ancho del neumático, y con la parte de arriba metida bajo la
     * carrocería (que empieza a 0.375 del suelo).
     */
    public void testUbicacionEnLaCarroceria() {
        assertTrue(Rueda.EJE_RUEDA + Rueda.RADIO_RUEDA <= LARGO_CARROCERIA / 2); // No pasa del paragolpes.
        assertTrue(Rueda.LADO_RUEDA - Rueda.ANCHO_RUEDA / 2 <= ANCHO_CARROCERIA / 2); // La cara interna queda bajo la carrocería.
        assertTrue(Rueda.LADO_RUEDA + Rueda.ANCHO_RUEDA / 2 <= ANCHO_CARROCERIA / 2 + Rueda.ANCHO_RUEDA); // Asoma poco.
        assertTrue(2 * Rueda.RADIO_RUEDA > BASE_CARROCERIA); // El neumático llega hasta la carrocería: no flota.
        assertEquals(2 * Rueda.EJE_RUEDA, Rueda.DISTANCIA_EJES, 0f);
    }

    /** acercarDireccion se mueve como mucho VELOCIDAD_DIRECCION · dt y nunca pasa del tope. */
    public void testAcercarDireccion() {
        float dt = 1f / 60;
        float paso = Rueda.VELOCIDAD_DIRECCION * dt;
        assertEquals(paso, Rueda.acercarDireccion(0, 1, dt), 1e-6f); // Paso limitado.
        assertEquals(0.01f, Rueda.acercarDireccion(0, 0.01f, dt), 1e-6f); // Llega si está cerca.
        assertEquals(Rueda.ANGULO_MAX_DIRECCION, Rueda.acercarDireccion(Rueda.ANGULO_MAX_DIRECCION, 5, 1), 0f); // Tope.
        assertEquals(-Rueda.ANGULO_MAX_DIRECCION, Rueda.acercarDireccion(0, -5, 1), 0f); // Tope del otro lado.
    }
}
