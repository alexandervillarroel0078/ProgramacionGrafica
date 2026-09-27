package com.graphics.ciudad.motor; // Prueba la cámara desde su mismo paquete.

import com.graphics.ciudad.mundo.Mapa; // Tamaño de la ciudad y celdas de calle.
import com.graphics.ciudad.vehiculo.Auto; // Salida del auto.
import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba los modos y los límites de la cámara orbital y de la aérea sin abrir una ventana OpenGL. */
public class CamaraTest extends TestCase {

    /** C recorre seguimiento → orbital → aérea → seguimiento; la flecha solo corresponde a la aérea. */
    public void testCicloDeModos() {
        Camara camara = new Camara(Mapa.LIMITE); // Límite de la ciudad actual.
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
        Camara camara = new Camara(Mapa.LIMITE); // Cámara nueva.
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
        Camara camara = new Camara(Mapa.LIMITE); // Límite de la ciudad actual.
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
        assertEquals(Mapa.LIMITE, Math.max(Math.abs(camara.getCentroX()), Math.abs(camara.getCentroZ())), 1e-4f); // Queda en el borde, no más allá.
    }

    /** Al entrar a la aérea arranca con la vista de siempre (la de las capturas), aunque antes se la haya movido. */
    public void testAereaArrancaConLaVistaInicial() {
        Camara camara = new Camara(Mapa.LIMITE);
        camara.alternar(); // Orbital.
        camara.alternar(); // Aérea.
        float[] ojo = camara.ojoAereo(); // Posición inicial de la cámara.
        float radio = Mapa.LIMITE * 1.86f; // La vista de antes: radio y altura fijos, ángulo 0.6.
        assertEquals(Math.sin(Camara.ANGULO_AEREO_INICIAL) * radio, ojo[0], 1e-3); // Misma X.
        assertEquals(Mapa.LIMITE * 1.57f, ojo[1], 1e-3); // Misma altura.
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
        Camara camara = new Camara(Mapa.LIMITE); // En seguimiento.
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
     * El plano lejano alcanza para ver toda la ciudad con la aérea alejada al máximo y el centro en una esquina: la
     * esquina opuesta está a DISTANCIA_AEREA_MAX + la diagonal de la ciudad. Sale de Mapa.LIMITE y llega al shader
     * como uniform (ya no hay un número fijo en ciudad.vert).
     */
    public void testPlanoLejanoCubreLaAerea() throws Exception {
        Camara camara = new Camara(Mapa.LIMITE);
        float diagonal = (float) Math.hypot(Mapa.TAMANO, Mapa.TAMANO); // De una esquina de la ciudad a la opuesta.
        float planoLejano = camara.getPlanoLejano();
        assertTrue("plano lejano=" + planoLejano, planoLejano >= camara.getDistanciaAereaMax() + diagonal);
        assertTrue(new Camara(2 * Mapa.LIMITE).getPlanoLejano() > planoLejano); // Una ciudad más grande ve más lejos.
        java.io.InputStream entrada = Camara.class.getResourceAsStream("/shaders/ciudad.vert"); // El shader, desde el classpath.
        assertNotNull(entrada);
        String texto = new String(entrada.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(texto.contains("uniform float uPlanoLejano")); // El shader lo recibe...
        assertTrue(texto.contains("lejos = uPlanoLejano")); // ...y lo usa en la proyección.
        assertFalse(texto.contains("const float PLANO_LEJANO")); // Sin la constante vieja.
    }

    // ==================== CÁMARA DE SEGUIMIENTO ====================
    // Ángulo del auto: el frente es (-sen, -cos) y detrás es (sen, cos). Con 0 mira al norte y detrás es +Z (el sur).

    /** En la salida del auto la cámara queda dentro de la ciudad (con margen) y sobre la calle. */
    public void testSeguimientoEnElInicioDentroDelLimite() {
        Camara camara = new Camara(Mapa.LIMITE);
        camara.actualizarSeguimiento(0.016f, Auto.X_INICIAL, Auto.Z_INICIAL, 0); // Primer cuadro, mirando al norte.
        float[] ojo = camara.ojoSeguimiento(Auto.X_INICIAL, Auto.Z_INICIAL, 0);
        float borde = Mapa.LIMITE - Camara.MARGEN_BORDE_CAMARA;
        assertTrue("ojo " + ojo[0] + "," + ojo[2], Math.abs(ojo[0]) <= borde && Math.abs(ojo[2]) <= borde); // Dentro.
        assertTrue(Mapa.esCalleEn(ojo[0], ojo[2])); // Sobre la calle.
    }

    /** Mirando al norte desde la calle del borde sur, detrás está afuera: la cámara se acerca y no pasa el borde. */
    public void testSeguimientoNoSaleDelMapa() {
        Camara camara = new Camara(Mapa.LIMITE);
        int ultima = Mapa.MAPA.length - 1; // Calle del borde sur.
        float x = Mapa.centro(ultima - 1); // Mitad de una cuadra del borde sur.
        float z = Mapa.centro(ultima);
        camara.actualizarSeguimiento(0.016f, x, z, 0);
        float[] ojo = camara.ojoSeguimiento(x, z, 0);
        assertTrue("ojo z=" + ojo[2], ojo[2] <= Mapa.LIMITE - Camara.MARGEN_BORDE_CAMARA + 1e-4f); // No pasa el borde.
        assertTrue(camara.getDistanciaSeguimiento() < Camara.DISTANCIA_SEGUIMIENTO); // Tuvo que recortar.
    }

    /** A 45° en un cruce interior, detrás hay una manzana: el ojo y la línea hasta el auto quedan sobre la calle. */
    public void testSeguimientoA45GradosNoQuedaSobreManzana() {
        int[] cruce = null; // Un cruce interior con una manzana en diagonal hacia el sureste.
        for (int[] c : Mapa.intersecciones()) {
            if (c[0] > 0 && c[1] > 0 && !Mapa.esCalleSegura(c[0] + 1, c[1] + 1)) {
                cruce = c;
                break;
            }
        }
        assertNotNull(cruce);
        float x = Mapa.centro(cruce[1]); // Centro del cruce.
        float z = Mapa.centro(cruce[0]);
        float angulo = (float) (Math.PI / 4); // Frente al noroeste: detrás (+X, +Z) está la manzana del sureste.
        Camara camara = new Camara(Mapa.LIMITE);
        camara.actualizarSeguimiento(0.016f, x, z, angulo);
        float[] ojo = camara.ojoSeguimiento(x, z, angulo);
        assertTrue("ojo " + ojo[0] + "," + ojo[2], Mapa.esCalleEn(ojo[0], ojo[2])); // El ojo no está sobre la manzana.
        for (int i = 0; i <= 100; i++) { // Toda la línea del ojo al auto, vista desde arriba.
            float t = i / 100f;
            assertTrue(Mapa.esCalleEn(x + (ojo[0] - x) * t, z + (ojo[2] - z) * t)); // No atraviesa la manzana.
        }
        assertTrue(camara.getDistanciaSeguimiento() < Camara.DISTANCIA_SEGUIMIENTO); // Tuvo que acercarse.
        assertTrue(camara.getDistanciaSeguimiento() >= Camara.DISTANCIA_SEGUIMIENTO_MIN); // Pero no pegada al auto.
    }

    /** En una calle recta no hay recorte: la distancia es la normal desde el primer cuadro, sin atraso. */
    public void testSeguimientoEnCalleRectaSinRecorte() {
        Camara camara = new Camara(Mapa.LIMITE);
        float x = Mapa.centro(0) + Mapa.TAM_CELDA / 4; // Carril derecho de la calle del borde oeste.
        for (float z = Mapa.centro(Mapa.MAPA.length / 2); z >= Mapa.centro(1); z -= 0.5f) { // Avanza hacia el norte.
            camara.actualizarSeguimiento(0.016f, x, z, 0);
            assertEquals(Camara.DISTANCIA_SEGUIMIENTO, camara.getDistanciaSeguimiento(), 0f); // Sin atraso ni recorte.
            float[] ojo = camara.ojoSeguimiento(x, z, 0);
            assertEquals(x, ojo[0], 1e-4f); // Justo detrás.
            assertEquals(z + Camara.DISTANCIA_SEGUIMIENTO, ojo[2], 1e-4f); // 12 detrás, como antes.
            assertEquals(Camara.ALTURA_SEGUIMIENTO, ojo[1], 0f); // A 9 de altura.
        }
    }

    /** Después de un recorte, la distancia vuelve a la normal de a poco, sin saltos. */
    public void testSeguimientoRecuperaLaDistancia() {
        Camara camara = new Camara(Mapa.LIMITE);
        int ultima = Mapa.MAPA.length - 1;
        camara.actualizarSeguimiento(0.016f, Mapa.centro(ultima - 1), Mapa.centro(ultima), 0); // Recorte en el borde sur.
        float anterior = camara.getDistanciaSeguimiento();
        float x = Mapa.centro(0) + Mapa.TAM_CELDA / 4; // Ahora en una calle recta con lugar.
        float z = Mapa.centro(Mapa.MAPA.length / 2);
        for (int cuadro = 0; cuadro < 120; cuadro++) { // Dos segundos a 60 cuadros por segundo.
            camara.actualizarSeguimiento(1 / 60f, x, z, 0);
            float d = camara.getDistanciaSeguimiento();
            assertTrue(d - anterior <= Camara.VELOCIDAD_ALEJAMIENTO / 60f + 1e-4f); // Nunca salta hacia atrás.
            anterior = d;
        }
        assertEquals(Camara.DISTANCIA_SEGUIMIENTO, camara.getDistanciaSeguimiento(), 0f); // Volvió a la normal.
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
        assertTrue(Math.abs(camara.getCentroX()) <= Mapa.LIMITE + 1e-4f); // El centro no sale de la ciudad.
        assertTrue(Math.abs(camara.getCentroZ()) <= Mapa.LIMITE + 1e-4f);
    }
}
