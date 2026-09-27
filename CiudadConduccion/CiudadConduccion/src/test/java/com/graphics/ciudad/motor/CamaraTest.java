package com.graphics.ciudad.motor; // Prueba la cámara desde su mismo paquete.

import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba los modos y los límites de la cámara orbital y de la aérea sin abrir una ventana OpenGL. */
public class CamaraTest extends TestCase {

    /** C recorre seguimiento → orbital → aérea → seguimiento; la flecha solo corresponde a la aérea. */
    public void testCicloDeModos() {
        Camara camara = new Camara(55); // Ciudad de límite 55.
        assertEquals(Camara.Modo.SEGUIMIENTO, camara.getModo()); // Arranca en seguimiento.
        camara.alternar(); // C.
        assertEquals(Camara.Modo.ORBITAL, camara.getModo()); // Orbital del auto.
        assertFalse(camara.esAerea()); // Sin flecha en la orbital.
        camara.alternar(); // C.
        assertEquals(Camara.Modo.AEREA, camara.getModo()); // Vista aérea.
        assertTrue(camara.esAerea()); // Con flecha.
        camara.alternar(); // C.
        assertEquals(Camara.Modo.SEGUIMIENTO, camara.getModo()); // Vuelve al inicio.
    }

    /** Arrastres y ruedita extremos nunca sacan la elevación ni la distancia de sus límites. */
    public void testLimitesDeLaOrbital() {
        Camara camara = new Camara(55); // Cámara nueva.
        camara.alternar(); // Modo orbital.
        double[][] movimientos = {{0, -100000}, {0, 100000}, {500, -37}, {-800, 91}, {3, 4}}; // Arrastres enormes y pequeños.
        double[] ruedas = {1000, -1000, 3, -7, 0.5}; // Zooms enormes y pequeños.
        for (int vuelta = 0; vuelta < 20; vuelta++) { // Repite combinaciones.
            for (double[] m : movimientos) { // Cada arrastre.
                camara.arrastrar(m[0], m[1]); // Mueve el mouse.
                comprobarLimites(camara); // Siempre dentro.
            }
            for (double r : ruedas) { // Cada paso de ruedita.
                camara.zoom(r); // Acerca o aleja.
                comprobarLimites(camara); // Siempre dentro.
            }
        }
        camara.arrastrar(0, -100000); // Mouse muy arriba.
        assertEquals(Camara.ELEVACION_MAX, camara.getElevacion(), 1e-6f); // Tope superior: 85°.
        camara.zoom(-1000); // Alejar muchísimo.
        assertEquals(Camara.DISTANCIA_MAX, camara.getDistancia(), 1e-6f); // Tope de distancia.
    }

    /**
     * En la aérea, arrastres, ruedita y desplazamientos extremos nunca sacan la elevación, la distancia ni el centro de
     * sus límites; y al empujar hasta el tope, llegan justo al límite.
     */
    public void testLimitesDeLaAerea() {
        Camara camara = new Camara(55); // Ciudad de límite 55.
        camara.alternar(); // Orbital.
        camara.alternar(); // Aérea.
        double[][] movimientos = {{0, -100000}, {0, 100000}, {500, -37}, {-800, 91}, {3, 4}, {100000, 100000}, {-100000, -100000}};
        double[] ruedas = {1000, -1000, 3, -7, 0.5};
        for (int vuelta = 0; vuelta < 20; vuelta++) { // Repite combinaciones.
            for (double[] m : movimientos) { // Cada gesto con cada botón.
                camara.arrastrar(m[0], m[1]); // Botón izquierdo: girar y elevar.
                comprobarLimitesAerea(camara);
                camara.desplazar(m[0], m[1]); // Botón derecho: mover el centro.
                comprobarLimitesAerea(camara);
            }
            for (double r : ruedas) { // Ruedita.
                camara.zoom(r);
                comprobarLimitesAerea(camara);
            }
        }
        camara.arrastrar(0, -100000); // Mouse muy arriba.
        assertEquals(Camara.ELEVACION_AEREA_MAX, camara.getElevacionAerea(), 1e-6f); // Tope: 85°.
        camara.arrastrar(0, 100000); // Mouse muy abajo.
        assertEquals(Camara.ELEVACION_AEREA_MIN, camara.getElevacionAerea(), 1e-6f); // Piso: 20°.
        camara.zoom(-1000); // Alejar muchísimo.
        assertEquals(camara.getDistanciaAereaMax(), camara.getDistanciaAerea(), 1e-4f); // Tope de distancia.
        camara.zoom(1000); // Acercar muchísimo.
        assertEquals(Camara.DISTANCIA_AEREA_MIN, camara.getDistanciaAerea(), 1e-6f); // Lo más cerca.
        camara.desplazar(1e7, 1e7); // Arrastrar muchísimo con el botón derecho.
        assertEquals(55, Math.max(Math.abs(camara.getCentroX()), Math.abs(camara.getCentroZ())), 1e-4f); // Queda en el borde, no más allá.
    }

    /** Al entrar a la aérea arranca con la vista de siempre (la de las capturas), aunque antes se la haya movido. */
    public void testAereaArrancaConLaVistaInicial() {
        Camara camara = new Camara(55);
        camara.alternar(); // Orbital.
        camara.alternar(); // Aérea.
        float[] ojo = camara.ojoAereo(); // Posición inicial de la cámara.
        float radio = 55 * 1.86f; // La vista de antes: radio y altura fijos, ángulo 0.6.
        assertEquals(Math.sin(Camara.ANGULO_AEREO_INICIAL) * radio, ojo[0], 1e-3); // Misma X.
        assertEquals(55 * 1.57f, ojo[1], 1e-3); // Misma altura.
        assertEquals(Math.cos(Camara.ANGULO_AEREO_INICIAL) * radio, ojo[2], 1e-3); // Misma Z.
        assertEquals(0f, camara.getCentroX(), 0f); // Mira al centro de la ciudad.
        assertEquals(0f, camara.getCentroZ(), 0f);
        assertTrue(camara.getDistanciaAereaInicial() <= camara.getDistanciaAereaMax()); // Alejada al máximo, se ve al menos lo mismo: toda la ciudad.
        camara.arrastrar(300, -200); // La mueve...
        camara.desplazar(400, 400);
        camara.zoom(5);
        camara.alternar(); // Seguimiento.
        camara.alternar(); // Orbital.
        camara.alternar(); // Aérea otra vez.
        float[] otra = camara.ojoAereo(); // ...y al volver está como al principio.
        for (int i = 0; i < 3; i++) {
            assertEquals(ojo[i], otra[i], 1e-4f);
        }
    }

    /** En seguimiento el mouse no hace nada; en la orbital, el botón derecho tampoco, y nada toca la aérea. */
    public void testMouseSoloEnOrbitalYAerea() {
        Camara camara = new Camara(55); // En seguimiento.
        float elevacion = camara.getElevacion(); // Orbital del auto.
        float distancia = camara.getDistancia();
        float elevacionAerea = camara.getElevacionAerea(); // Aérea.
        float distanciaAerea = camara.getDistanciaAerea();
        camara.arrastrar(100, 100); // Botón izquierdo.
        camara.desplazar(100, 100); // Botón derecho.
        camara.zoom(5); // Ruedita.
        assertEquals(elevacion, camara.getElevacion(), 0f); // Sin cambios.
        assertEquals(distancia, camara.getDistancia(), 0f);
        assertEquals(elevacionAerea, camara.getElevacionAerea(), 0f);
        assertEquals(distanciaAerea, camara.getDistanciaAerea(), 0f);
        assertEquals(0f, camara.getCentroX(), 0f);
        assertEquals(0f, camara.getCentroZ(), 0f);
        camara.alternar(); // Orbital del auto.
        camara.desplazar(100, 100); // El botón derecho no hace nada acá.
        camara.arrastrar(10, 10); // El izquierdo mueve la orbital del auto...
        assertEquals(0f, camara.getCentroX(), 0f); // ...y no la aérea.
        assertEquals(0f, camara.getCentroZ(), 0f);
        assertEquals(elevacionAerea, camara.getElevacionAerea(), 0f);
    }

    /**
     * El plano lejano de ciudad.vert alcanza para ver toda la ciudad con la aérea alejada al máximo y el centro en una
     * esquina: la esquina opuesta está a DISTANCIA_AEREA_MAX + la diagonal de la ciudad.
     */
    public void testPlanoLejanoCubreLaAerea() throws Exception {
        java.io.InputStream entrada = Camara.class.getResourceAsStream("/shaders/ciudad.vert"); // El shader, desde el classpath.
        assertNotNull(entrada);
        String texto = new String(entrada.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("PLANO_LEJANO\\s*=\\s*([0-9.]+)").matcher(texto); // Busca la constante.
        assertTrue(m.find());
        float planoLejano = Float.parseFloat(m.group(1)); // Su valor.
        Camara camara = new Camara(55);
        float diagonal = (float) Math.hypot(2 * 55, 2 * 55); // De una esquina de la ciudad a la opuesta.
        assertTrue("PLANO_LEJANO=" + planoLejano, planoLejano >= camara.getDistanciaAereaMax() + diagonal);
    }

    /** Revisa que elevación y distancia estén dentro de sus límites. */
    private static void comprobarLimites(Camara camara) {
        assertTrue(camara.getElevacion() >= Camara.ELEVACION_MIN - 1e-6f); // No por debajo de 5°.
        assertTrue(camara.getElevacion() <= Camara.ELEVACION_MAX + 1e-6f); // No por encima de 85°.
        assertTrue(camara.getDistancia() >= Camara.DISTANCIA_MIN - 1e-6f); // No más cerca que el mínimo.
        assertTrue(camara.getDistancia() <= Camara.DISTANCIA_MAX + 1e-6f); // No más lejos que el máximo.
    }

    /** Revisa que elevación, distancia y centro de la aérea estén dentro de sus límites. */
    private static void comprobarLimitesAerea(Camara camara) {
        assertTrue(camara.getElevacionAerea() >= Camara.ELEVACION_AEREA_MIN - 1e-6f); // No por debajo de 20°.
        assertTrue(camara.getElevacionAerea() <= Camara.ELEVACION_AEREA_MAX + 1e-6f); // No por encima de 85°.
        assertTrue(camara.getDistanciaAerea() >= Camara.DISTANCIA_AEREA_MIN - 1e-6f); // No más cerca que el mínimo.
        assertTrue(camara.getDistanciaAerea() <= camara.getDistanciaAereaMax() + 1e-4f); // No más lejos que el máximo.
        assertTrue(Math.abs(camara.getCentroX()) <= 55 + 1e-4f); // El centro no sale de la ciudad.
        assertTrue(Math.abs(camara.getCentroZ()) <= 55 + 1e-4f);
    }
}
