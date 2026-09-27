package com.graphics.ciudad.trafico; // Prueba las ruedas del tráfico desde su mismo paquete.

import com.graphics.ciudad.motor.Cubo; // Se crea sin tocar la GPU: solo guarda referencias.
import com.graphics.ciudad.motor.Shader; // Se crea sin tocar la GPU: OpenGL se usa recién en crear().
import com.graphics.ciudad.vehiculo.Rueda; // Radio de la rueda y ángulo máximo de dirección.
import java.util.List; // Tipo de la lista de vehículos.
import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba la rotación y la dirección de las ruedas del tráfico sin abrir una ventana OpenGL. */
public class RuedasTraficoTest extends TestCase {

    private static final float DT = 1f / 60; // Paso fijo: 60 cuadros por segundo.
    private static final float SEGUNDOS = 60; // Duración de la simulación: cada vehículo dobla varias veces.
    private static final float LEJOS = 1000; // Jugador fuera del mapa: no frena a nadie.
    private static final float TOLERANCIA = 1e-4f; // Error de redondeo admitido.

    /** Crea el tráfico sin OpenGL. */
    private static Trafico nuevoTrafico() {
        Shader shader = new Shader(); // Programa sin compilar: la prueba no dibuja.
        return new Trafico(shader, new Cubo(shader), () -> false);
    }

    /**
     * En cada cuadro, el ángulo de cada rueda avanza exactamente distancia recorrida / RADIO_RUEDA: si el vehículo
     * frenó avanza menos, y si no se movió no cambia. Además, en total, las ruedas dieron vueltas.
     */
    public void testRuedaAvanzaDistanciaSobreRadio() {
        Trafico trafico = nuevoTrafico();
        List<Vehiculo> vehiculos = trafico.getVehiculos();
        for (int paso = 0; paso < Math.round(SEGUNDOS / DT); paso++) {
            float[][] antes = new float[vehiculos.size()][3]; // {x, z, anguloRueda} antes del cuadro.
            for (int i = 0; i < vehiculos.size(); i++) {
                Vehiculo v = vehiculos.get(i);
                antes[i] = new float[] {v.getX(), v.getZ(), v.getAnguloRueda()};
            }
            trafico.actualizar(DT, LEJOS, LEJOS); // Incluye frenadas entre vehículos en los cruces.
            for (int i = 0; i < vehiculos.size(); i++) {
                Vehiculo v = vehiculos.get(i);
                float distancia = (float) Math.hypot(v.getX() - antes[i][0], v.getZ() - antes[i][1]); // Avance real.
                float giro = v.getAnguloRueda() - antes[i][2]; // Lo que giró la rueda en el cuadro.
                assertEquals("vehiculo " + i + " cuadro " + paso, distancia / Rueda.RADIO_RUEDA, giro, 1e-3f);
            }
        }
        for (Vehiculo v : vehiculos) {
            assertTrue("las ruedas no giraron", v.getAnguloRueda() > 2 * Math.PI); // Más de una vuelta en un minuto.
        }
    }

    /** Detenido ante un obstáculo pegado (hueco 0), el vehículo no avanza y sus ruedas no giran. */
    public void testDetenidoNoGiraLaRueda() {
        Vehiculo v = nuevoTrafico().getVehiculos().get(0);
        v.actualizar(DT, Vehiculo.SIN_OBSTACULO); // Un cuadro normal: las ruedas ya giraron algo.
        float anguloAntes = v.getAnguloRueda();
        assertTrue(anguloAntes > 0);
        for (int paso = 0; paso < 120; paso++) { // Dos segundos con el obstáculo tocándolo.
            float x = v.getX();
            float z = v.getZ();
            v.actualizar(DT, 0); // Hueco 0: se detiene en el lugar.
            assertEquals(x, v.getX(), 0f); // No se movió...
            assertEquals(z, v.getZ(), 0f);
            assertEquals(anguloAntes, v.getAnguloRueda(), 0f); // ...y la rueda tampoco giró.
        }
        v.actualizar(0, Vehiculo.SIN_OBSTACULO); // En pausa (dt = 0) tampoco cambia nada.
        assertEquals(anguloAntes, v.getAnguloRueda(), 0f);
    }

    /**
     * Las delanteras nunca superan ANGULO_MAX_DIRECCION; en las curvas doblan de verdad (más de 10°) y en las rectas
     * vuelven a 0.
     */
    public void testDelanterasNoSuperanElMaximo() {
        Trafico trafico = nuevoTrafico();
        List<Vehiculo> vehiculos = trafico.getVehiculos();
        float[] maximo = new float[vehiculos.size()];
        boolean[] volvioAlCentroTrasDoblar = new boolean[vehiculos.size()];
        for (int paso = 0; paso < Math.round(SEGUNDOS / DT); paso++) {
            trafico.actualizar(DT, LEJOS, LEJOS);
            for (int i = 0; i < vehiculos.size(); i++) {
                float d = Math.abs(vehiculos.get(i).getAnguloDireccion());
                assertTrue("vehiculo " + i + ": " + Math.toDegrees(d) + "°", d <= Rueda.ANGULO_MAX_DIRECCION + TOLERANCIA);
                maximo[i] = Math.max(maximo[i], d);
                volvioAlCentroTrasDoblar[i] |= maximo[i] > Math.toRadians(10) && d < TOLERANCIA; // Dobló y enderezó.
            }
        }
        for (int i = 0; i < vehiculos.size(); i++) {
            assertTrue("vehiculo " + i + " nunca dobló", maximo[i] > Math.toRadians(10));
            assertTrue("vehiculo " + i + " no volvió a 0 en recta", volvioAlCentroTrasDoblar[i]);
        }
    }

    /** Modelo de bicicleta: sin giro, ruedas derechas; girando a la izquierda, positivo; nunca más del tope. */
    public void testDireccionPorGiro() {
        assertEquals(0f, Vehiculo.direccionPorGiro(0, 8), 0f); // Recta.
        assertEquals(0f, Vehiculo.direccionPorGiro(0, 0), 0f); // Detenido y sin girar.
        float izquierda = Vehiculo.direccionPorGiro(1, 8); // 1 rad/s a 8 u/s: tan δ = 1.64 / 8.
        assertEquals((float) Math.atan(Rueda.DISTANCIA_EJES / 8), izquierda, TOLERANCIA);
        assertEquals(-izquierda, Vehiculo.direccionPorGiro(-1, 8), TOLERANCIA); // Derecha: mismo ángulo, negativo.
        assertEquals(Rueda.ANGULO_MAX_DIRECCION, Vehiculo.direccionPorGiro(Vehiculo.VELOCIDAD_GIRO, 0.5f), TOLERANCIA); // Tope.
    }
}
