package com.graphics.ciudad.trafico; // Prueba el tráfico desde su mismo paquete.

import com.graphics.ciudad.motor.Cubo; // Se crea sin tocar la GPU: solo guarda referencias.
import com.graphics.ciudad.motor.Shader; // Se crea sin tocar la GPU: OpenGL se usa recién en crear().
import com.graphics.ciudad.mundo.Mapa; // Comprueba celdas de calle y límites.
import com.graphics.ciudad.vehiculo.Colisiones; // Comprueba que ningún vehículo toque una manzana.
import java.util.List; // Tipo de la lista de vehículos.
import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Simula el tráfico sin abrir una ventana OpenGL. */
public class TraficoTest extends TestCase {

    private static final float DT = 1f / 60; // Paso fijo: 60 cuadros por segundo.
    private static final float SEGUNDOS = 120; // Duración de la simulación.
    private static final float VENTANA_MOVIMIENTO = 5; // Cada 5 s se exige que cada vehículo haya avanzado.
    private static final float AVANCE_MINIMO = 5; // Unidades mínimas recorridas en cada ventana de 5 s.
    private static final float LEJOS = 1000; // Posición del jugador fuera del mapa: no frena a nadie.
    private static final float MARGEN_GIRO = 8; // En los 8 primeros y últimos metros de cada tramo el vehículo está girando.
    private static final float TOLERANCIA_CARRIL = 0.35f; // Error admitido respecto a DESPLAZAMIENTO_CARRIL en los tramos rectos.

    /** Crea el tráfico sin OpenGL. */
    private Trafico nuevoTrafico() {
        Shader shader = new Shader(); // Programa sin compilar: la prueba no dibuja.
        return new Trafico(shader, new Cubo(shader), () -> true); // Las rutas y el movimiento no necesitan la GPU; noche fija.
    }

    /** Durante 120 s los vehículos siempre están en calles, dentro del mapa, sin tocar manzanas, y siguen moviéndose. */
    public void testSimulacionDeDosMinutos() {
        Trafico trafico = nuevoTrafico(); // Tráfico recién creado.
        List<Vehiculo> vehiculos = trafico.getVehiculos(); // Vehículos a observar.
        assertTrue(vehiculos.size() >= 3); // El enunciado pide al menos tres vehículos.
        float limite = Mapa.LIMITE - Vehiculo.RADIO_VEHICULO; // El círculo completo debe quedar dentro del mapa.
        int pasos = Math.round(SEGUNDOS / DT); // Cantidad de cuadros simulados.
        int pasosPorVentana = Math.round(VENTANA_MOVIMIENTO / DT); // Cuadros entre controles de movimiento.
        float[] recorrido = new float[vehiculos.size()]; // Distancia acumulada en la ventana actual.
        for (int paso = 0; paso < pasos; paso++) { // Avanza la simulación cuadro a cuadro.
            float[][] antes = new float[vehiculos.size()][2]; // Posiciones antes del cuadro.
            for (int i = 0; i < vehiculos.size(); i++) { // Guarda la posición de cada vehículo.
                antes[i][0] = vehiculos.get(i).getX(); // X previo.
                antes[i][1] = vehiculos.get(i).getZ(); // Z previo.
            }
            trafico.actualizar(DT, LEJOS, LEJOS); // El jugador está lejos: ningún vehículo tiene que frenar por él.
            for (int i = 0; i < vehiculos.size(); i++) { // Revisa cada vehículo después del cuadro.
                Vehiculo v = vehiculos.get(i); // Vehículo revisado.
                String donde = "vehiculo " + i + " en t=" + (paso * DT) + " (" + v.getX() + ", " + v.getZ() + ")"; // Mensaje de error útil.
                assertTrue(donde, Mapa.esCalleEn(v.getX(), v.getZ())); // Siempre sobre una celda de calle.
                assertTrue(donde, Math.abs(v.getX()) <= limite && Math.abs(v.getZ()) <= limite); // Nunca fuera del mapa.
                assertTrue(donde, Colisiones.puedeCircular(v.getX(), v.getZ())); // Su círculo nunca invade una manzana.
                recorrido[i] += (float) Math.hypot(v.getX() - antes[i][0], v.getZ() - antes[i][1]); // Suma lo avanzado.
                comprobarCarril(v, donde); // En los tramos rectos debe ir por su carril derecho.
            }
            if ((paso + 1) % pasosPorVentana == 0) { // Terminó una ventana de 5 segundos.
                for (int i = 0; i < vehiculos.size(); i++) { // Revisa cada vehículo.
                    assertTrue("vehiculo " + i + " detenido", recorrido[i] > AVANCE_MINIMO); // Debe seguir moviéndose.
                    recorrido[i] = 0; // Empieza a medir la siguiente ventana.
                }
            }
        }
    }

    /**
     * En la parte recta de un tramo (lejos de ambas esquinas), la distancia a la línea central de la calle debe ser
     * DESPLAZAMIENTO_CARRIL, y del lado DERECHO de la dirección de avance.
     */
    private static void comprobarCarril(Vehiculo v, String donde) {
        float[] tramo = v.tramoCentral(); // {desdeX, desdeZ, hastaX, hastaZ} de la línea central.
        float dx = tramo[2] - tramo[0]; // Avance del tramo en X.
        float dz = tramo[3] - tramo[1]; // Avance del tramo en Z.
        float largo = (float) Math.hypot(dx, dz); // Longitud del tramo.
        float dirX = dx / largo; // Dirección unitaria en X.
        float dirZ = dz / largo; // Dirección unitaria en Z.
        float relX = v.getX() - tramo[0]; // Vector del inicio del tramo al vehículo, en X.
        float relZ = v.getZ() - tramo[1]; // Vector del inicio del tramo al vehículo, en Z.
        float avance = relX * dirX + relZ * dirZ; // Proyección sobre la línea central: cuánto avanzó.
        if (avance < MARGEN_GIRO || avance > largo - MARGEN_GIRO) { // Cerca de una esquina está girando.
            return; // Allí no se exige el carril exacto.
        }
        float derechaX = -dirZ; // Derecha de (dx, dz) = (-dz, dx), la convención de ejes del proyecto.
        float derechaZ = dirX; // Componente Z de la derecha.
        float lateral = relX * derechaX + relZ * derechaZ; // Distancia con signo: positiva = a la derecha de la línea.
        assertEquals("carril de " + donde, Vehiculo.DESPLAZAMIENTO_CARRIL, lateral, TOLERANCIA_CARRIL); // Carril derecho.
    }

    /** Al menos dos rutas recorren un mismo tramo de calle en sentidos opuestos (carriles distintos). */
    public void testHaySentidosOpuestosEnUnaMismaCalle() {
        boolean encontrado = false; // Se vuelve true al encontrar un par de tramos opuestos.
        int[][][] rutas = Trafico.RUTAS_CELDAS; // Rutas en celdas {fila, columna}.
        for (int a = 0; a < rutas.length; a++) { // Primera ruta del par.
            for (int b = a + 1; b < rutas.length; b++) { // Segunda ruta del par.
                for (int i = 0; i < rutas[a].length; i++) { // Tramos de la primera ruta.
                    for (int j = 0; j < rutas[b].length; j++) { // Tramos de la segunda ruta.
                        int[] a1 = rutas[a][i]; // Inicio del tramo de la ruta a.
                        int[] a2 = rutas[a][(i + 1) % rutas[a].length]; // Fin del tramo de la ruta a.
                        int[] b1 = rutas[b][j]; // Inicio del tramo de la ruta b.
                        int[] b2 = rutas[b][(j + 1) % rutas[b].length]; // Fin del tramo de la ruta b.
                        boolean mismoTramo = a1[0] == b2[0] && a1[1] == b2[1] && a2[0] == b1[0] && a2[1] == b1[1]; // Mismos extremos, invertidos.
                        encontrado |= mismoTramo; // Basta con un par.
                    }
                }
            }
        }
        assertTrue(encontrado); // Debe haber una calle con vehículos en ambos sentidos.
    }

    /** Las esquinas de carril de un giro de 90° quedan a DESPLAZAMIENTO_CARRIL de ambas líneas centrales. */
    public void testEsquinasDeCarril() {
        float d = Vehiculo.DESPLAZAMIENTO_CARRIL; // Desplazamiento lateral del carril.
        float[][] cuadrado = {{0, 0}, {0, -20}, {20, -20}, {20, 0}}; // Recorrido: norte, este, sur y oeste (giros a la derecha).
        float[][] esquinas = Vehiculo.calcularCarriles(cuadrado, d); // Esquinas del carril derecho.
        assertEquals(d, esquinas[1][0], 1e-5f); // Yendo al norte, la derecha es +X: la esquina está en X = d.
        assertEquals(-20 + d, esquinas[1][1], 1e-5f); // Saliendo al este, la derecha es +Z: la esquina está en Z = -20 + d.
        float[][] recto = {{0, 0}, {0, -20}, {0, -40}}; // Un waypoint en medio de una recta.
        float[][] enRecta = Vehiculo.calcularCarriles(recto, d); // El carril no debe torcerse en una recta.
        assertEquals(d, enRecta[1][0], 1e-5f); // Mismo desplazamiento d, no 2·d.
    }

    /** El vehículo se detiene si el jugador está justo adelante, y reset() lo devuelve al inicio de la ruta. */
    public void testFrenaAnteElJugadorYReinicia() {
        Trafico trafico = nuevoTrafico(); // Tráfico recién creado.
        Vehiculo v = trafico.getVehiculos().get(0); // Primer vehículo.
        float frenteX = -(float) Math.sin(v.getAngulo()); // Dirección frontal en X.
        float frenteZ = -(float) Math.cos(v.getAngulo()); // Dirección frontal en Z.
        float jugadorX = v.getX() + frenteX * 4; // Jugador cuatro unidades delante.
        float jugadorZ = v.getZ() + frenteZ * 4; // Jugador cuatro unidades delante.
        float xInicial = v.getX(); // Posición antes de actualizar.
        float zInicial = v.getZ(); // Posición antes de actualizar.
        trafico.actualizar(DT, jugadorX, jugadorZ); // Actualiza con el jugador cerrando el paso.
        assertEquals(xInicial, v.getX(), 1e-6f); // No avanzó en X.
        assertEquals(zInicial, v.getZ(), 1e-6f); // No avanzó en Z.
        for (int i = 0; i < 600; i++) { // Diez segundos sin jugador cerca.
            trafico.actualizar(DT, LEJOS, LEJOS); // Se mueve libremente.
        }
        assertTrue(Math.hypot(v.getX() - xInicial, v.getZ() - zInicial) > 1); // Se alejó del inicio.
        trafico.reset(); // Mismo reinicio que la tecla R.
        assertEquals(xInicial, v.getX(), 1e-6f); // Vuelve al primer waypoint en X.
        assertEquals(zInicial, v.getZ(), 1e-6f); // Vuelve al primer waypoint en Z.
    }

    /** El jugador no puede meterse dentro de un vehículo, pero sí alejarse de él. */
    public void testJugadorNoAtraviesaVehiculos() {
        Trafico trafico = nuevoTrafico(); // Tráfico recién creado.
        Vehiculo v = trafico.getVehiculos().get(0); // Primer vehículo, en su posición inicial.
        float vx = v.getX(); // Centro del vehículo en X.
        float vz = v.getZ(); // Centro del vehículo en Z.
        assertTrue(trafico.bloquea(vx + 5, vz, vx + 2, vz, 1.65f)); // Acercarse hasta solaparse está prohibido.
        assertFalse(trafico.bloquea(vx + 2, vz, vx + 3, vz, 1.65f)); // Alejarse está permitido aunque siga solapado.
        assertFalse(trafico.bloquea(vx + 9, vz, vx + 8, vz, 1.65f)); // Acercarse sin tocarlo está permitido.
    }

    /** Una ruta diagonal o que pase por una manzana se rechaza al construirla. */
    public void testValidacionDeRutas() {
        try { // Ruta con un tramo diagonal.
            Trafico.validarRuta(new int[][] {{0, 0}, {2, 2}}); // Debe fallar.
            fail("Se aceptó un tramo diagonal"); // No debería llegar aquí.
        } catch (IllegalStateException esperado) { // El error es el comportamiento correcto.
            assertTrue(esperado.getMessage().contains("diagonal")); // Explica la causa.
        }
        try { // Ruta que baja por la columna 1, que tiene manzanas.
            Trafico.validarRuta(new int[][] {{0, 1}, {2, 1}}); // Debe fallar.
            fail("Se aceptó una ruta a través de una manzana"); // No debería llegar aquí.
        } catch (IllegalStateException esperado) { // El error es el comportamiento correcto.
            assertTrue(esperado.getMessage().contains("manzana")); // Explica la causa.
        }
    }
}
