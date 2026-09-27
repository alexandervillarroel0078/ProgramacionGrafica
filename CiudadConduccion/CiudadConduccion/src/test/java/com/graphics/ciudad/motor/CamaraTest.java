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
        float[] ojo = camara.ojoSeguimiento(Auto.X_INICIAL, Auto.Z_INICIAL);
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
        float[] ojo = camara.ojoSeguimiento(x, z);
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
        float[] ojo = camara.ojoSeguimiento(x, z);
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
            float[] ojo = camara.ojoSeguimiento(x, z);
            assertEquals(x, ojo[0], 1e-4f); // Justo detrás.
            assertEquals(z + Camara.DISTANCIA_SEGUIMIENTO, ojo[2], 1e-4f); // 8 detrás.
            assertEquals(Camara.alturaSeguimiento(Camara.DISTANCIA_SEGUIMIENTO), ojo[1], 0f); // ≈ 3.5 de altura.
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

    /** Carril derecho de la calle del borde oeste, a mitad de la ciudad: hacia el norte hay lugar de sobra. */
    private static final float X_LIBRE = Mapa.centro(0) + Mapa.TAM_CELDA / 4;
    private static final float Z_LIBRE = Mapa.centro(Mapa.MAPA.length / 2);

    /** Ángulo de la cámara tras unos segundos a ciertos FPS, con el auto quieto en X_LIBRE, Z_LIBRE y ángulo angulo(t). */
    private static float anguloTras(float segundos, int fps, java.util.function.DoubleUnaryOperator angulo) {
        Camara camara = new Camara(Mapa.LIMITE);
        camara.actualizarSeguimiento(0, X_LIBRE, Z_LIBRE, 0); // Primer cuadro: se coloca detrás, mirando al norte.
        int cuadros = Math.round(segundos * fps);
        for (int i = 1; i <= cuadros; i++) {
            camara.actualizarSeguimiento(1f / fps, X_LIBRE, Z_LIBRE, (float) angulo.applyAsDouble(i / (double) fps));
        }
        return camara.getAnguloCamara();
    }

    /**
     * exp(−K_GIRO · dt) no depende de los FPS: si el auto gira 90° de golpe, medio segundo después la cámara llegó al
     * mismo ángulo a 30, 60 y 144 FPS, el que da la fórmula continua 90° · (1 − e^(−K_GIRO · 0.5)).
     */
    public void testSuavizadoIgualA30_60Y144Fps() {
        float esperado = (float) (Math.PI / 2 * (1 - Math.exp(-Camara.K_GIRO * 0.5)));
        for (int fps : new int[] {30, 60, 144}) {
            float angulo = anguloTras(0.5f, fps, t -> Math.PI / 2); // Giro instantáneo de 90° a la izquierda.
            assertEquals("fps=" + fps, esperado, angulo, 1e-4f);
            assertTrue(angulo < Math.PI / 2); // Todavía no llegó: la cámara lo persigue, no lo copia.
        }
        // Doblando a fondo a 16 u/s (≈ 101°/s) el atraso casi no depende de los FPS: menos de 2° entre 30 y 144.
        double omega = 16 * Auto.VELOCIDAD_GIRO;
        float a30 = anguloTras(1, 30, t -> omega * t);
        float a144 = anguloTras(1, 144, t -> omega * t);
        assertEquals(a30, a144, Math.toRadians(2));
        float atraso = (float) omega - a144; // Cuánto queda atrás la cámara al doblar.
        assertTrue("atraso=" + Math.toDegrees(atraso), atraso > Math.toRadians(10) && atraso < Math.toRadians(25)); // ≈ 20°: se ve el costado.
    }

    /** La cámara gira por el camino corto: de 170° a −170° son 20°, no 340°. */
    public void testSuavizadoGiraPorElCaminoCorto() {
        assertEquals(0.2f, Camara.diferenciaAngular(0.1f, (float) (2 * Math.PI - 0.1)), 1e-5f);
        assertEquals((float) (2 * Math.PI - 6), Camara.diferenciaAngular(-3, 3), 1e-5f);
        for (float a = -20; a <= 20; a += 0.37f) { // Cualquier par de ángulos: el resultado queda en (−π, π].
            float diferencia = Camara.diferenciaAngular(a, 1.3f);
            assertTrue(diferencia > -Math.PI - 1e-6 && diferencia <= Math.PI + 1e-6);
            assertEquals(0, Math.sin(diferencia) - Math.sin(a - 1.3f), 1e-4); // Mismo giro, módulo una vuelta.
        }
        float desde = (float) Math.toRadians(170);
        float hasta = (float) Math.toRadians(-170);
        Camara camara = new Camara(Mapa.LIMITE);
        camara.actualizarSeguimiento(0, X_LIBRE, Z_LIBRE, desde); // Se coloca en 170°.
        camara.actualizarSeguimiento(1 / 60f, X_LIBRE, Z_LIBRE, hasta); // El auto pasa a −170°.
        float giro = Camara.diferenciaAngular(camara.getAnguloCamara(), desde);
        assertTrue("giro=" + Math.toDegrees(giro), giro > 0 && giro < Math.toRadians(20)); // Poco y hacia 180°.
    }

    /**
     * La cámara mira siempre INCLINACION hacia abajo, aunque el recorte cambie la distancia: en una calle recta, en el
     * borde sur (recorta) y a 45° en un cruce (recorta más), la altura baja junto con la distancia.
     */
    public void testInclinacionConstanteAunqueCambieLaDistancia() {
        int ultima = Mapa.MAPA.length - 1;
        int[] cruce = null; // Un cruce interior con una manzana en diagonal hacia el sureste.
        for (int[] c : Mapa.intersecciones()) {
            if (c[0] > 0 && c[1] > 0 && !Mapa.esCalleSegura(c[0] + 1, c[1] + 1)) {
                cruce = c;
                break;
            }
        }
        assertNotNull(cruce);
        float[][] casos = { // {x, z, ángulo}
            {X_LIBRE, Z_LIBRE, 0},
            {Mapa.centro(ultima - 1), Mapa.centro(ultima), 0},
            {Mapa.centro(cruce[1]), Mapa.centro(cruce[0]), (float) (Math.PI / 4)},
        };
        java.util.Set<Float> distancias = new java.util.HashSet<>();
        for (float[] caso : casos) {
            Camara camara = new Camara(Mapa.LIMITE);
            camara.reiniciarSeguimiento(caso[0], caso[1], caso[2]);
            float[] ojo = camara.ojoSeguimiento(caso[0], caso[1]);
            float[] objetivo = Camara.objetivoSeguimiento(caso[0], caso[1], caso[2]);
            float horizontal = (float) Math.hypot(objetivo[0] - ojo[0], objetivo[2] - ojo[2]);
            float inclinacion = (float) Math.atan2(ojo[1] - objetivo[1], horizontal); // Hacia abajo, positiva.
            assertEquals("d=" + camara.getDistanciaSeguimiento(), Camara.INCLINACION, inclinacion, 1e-4f);
            assertEquals(camara.getDistanciaSeguimiento() + Camara.ADELANTE, horizontal, 1e-3f); // Mira ADELANTE del auto.
            distancias.add(camara.getDistanciaSeguimiento());
        }
        assertEquals(3, distancias.size()); // Las tres distancias fueron distintas: la prueba cubrió el recorte.
        assertTrue(Camara.alturaSeguimiento(Camara.DISTANCIA_SEGUIMIENTO_MIN) >= Camara.ALTURA_MINIMA_SEGUIMIENTO);
    }

    /**
     * Tras R (reiniciarSeguimiento) la cámara queda justo detrás del auto en la salida, sin barrer desde donde estaba:
     * el ángulo es el del auto y la distancia, toda la libre, desde ese mismo cuadro y en los siguientes.
     */
    public void testReinicioSinBarrido() {
        Camara camara = new Camara(Mapa.LIMITE);
        int ultima = Mapa.MAPA.length - 1;
        camara.actualizarSeguimiento(0, X_LIBRE, Z_LIBRE, 0);
        for (int i = 0; i < 10; i++) { // Dobla hacia el oeste junto al borde sur: queda con atraso.
            camara.actualizarSeguimiento(1 / 60f, Mapa.centro(ultima - 1), Mapa.centro(ultima), (float) (Math.PI / 2 + i * 0.03));
        }
        assertTrue(Math.abs(camara.getAnguloCamara() - Math.PI / 2) > 0.1); // Tenía atraso.
        camara.reiniciarSeguimiento(Auto.X_INICIAL, Auto.Z_INICIAL, 0); // R.
        for (int cuadro = 0; cuadro < 3; cuadro++) { // El mismo cuadro y los siguientes: nada se mueve.
            assertEquals(0f, camara.getAnguloCamara(), 0f); // Alineada con el auto.
            float[] ojo = camara.ojoSeguimiento(Auto.X_INICIAL, Auto.Z_INICIAL);
            assertEquals(Auto.X_INICIAL, ojo[0], 1e-4f); // Justo detrás...
            assertEquals(Auto.Z_INICIAL + Camara.DISTANCIA_SEGUIMIENTO, ojo[2], 1e-4f); // ...a la distancia normal.
            camara.actualizarSeguimiento(1 / 60f, Auto.X_INICIAL, Auto.Z_INICIAL, 0);
        }
    }

    /** Al volver al seguimiento con C, la cámara se coloca detrás del auto sin barrido, aunque el auto haya girado. */
    public void testVolverConCSinBarrido() {
        Camara camara = new Camara(Mapa.LIMITE);
        camara.actualizarSeguimiento(0, X_LIBRE, Z_LIBRE, 0);
        camara.alternar(); // Orbital.
        camara.alternar(); // Aérea.
        camara.alternar(); // Seguimiento otra vez; mientras tanto el auto giró 180°.
        camara.actualizarSeguimiento(1 / 60f, X_LIBRE, Z_LIBRE, (float) Math.PI);
        assertEquals((float) Math.PI, camara.getAnguloCamara(), 0f); // Sin atraso.
        assertEquals(camara.distanciaLibre(X_LIBRE, Z_LIBRE, (float) Math.PI), camara.getDistanciaSeguimiento(), 0f); // Toda la libre.
    }

    /**
     * Retrocediendo en recta desde la salida hacia el borde sur a 60 FPS: mientras la cámara está recortada (entre la
     * distancia mínima y la normal), el ojo queda quieto frente al borde (|ΔojoZ| < 0.01) y la distancia nunca sube.
     * Con escalones de 0.25 el ojo iba +0.10, +0.10, −0.15: el temblor en reversa.
     */
    public void testReversaHaciaElBordeSinTemblor() {
        Camara camara = new Camara(Mapa.LIMITE);
        Auto auto = new Auto(); // En la salida, mirando al norte: detrás está el borde sur.
        java.util.function.IntPredicate s = tecla -> tecla == org.lwjgl.glfw.GLFW.GLFW_KEY_S; // S sostenida.
        camara.actualizarSeguimiento(0, auto.getX(), auto.getZ(), auto.getAngulo());
        float anteriorD = camara.getDistanciaSeguimiento();
        float anteriorZ = camara.ojoSeguimiento(auto.getX(), auto.getZ())[2];
        boolean anteriorRecortada = false;
        int recortados = 0; // Cuadros comprobados con la cámara recortada.
        for (int cuadro = 0; cuadro < 240; cuadro++) { // Cuatro segundos: llega hasta el borde.
            auto.actualizar(1 / 60f, s);
            camara.actualizarSeguimiento(1 / 60f, auto.getX(), auto.getZ(), auto.getAngulo());
            float d = camara.getDistanciaSeguimiento();
            float z = camara.ojoSeguimiento(auto.getX(), auto.getZ())[2];
            boolean recortada = d > Camara.DISTANCIA_SEGUIMIENTO_MIN + 1e-4f && d < Camara.DISTANCIA_SEGUIMIENTO;
            assertTrue("cuadro " + cuadro + ": d sube de " + anteriorD + " a " + d, d <= anteriorD + 1e-6f);
            if (recortada && anteriorRecortada) {
                assertEquals("cuadro " + cuadro + ", d=" + d, anteriorZ, z, 0.01f); // Quieto frente al borde.
                recortados++;
            }
            anteriorD = d;
            anteriorZ = z;
            anteriorRecortada = recortada;
        }
        assertTrue("recortados=" + recortados, recortados > 30); // La prueba cubrió el recorte en reversa.
        assertEquals(Camara.DISTANCIA_SEGUIMIENTO_MIN, camara.getDistanciaSeguimiento(), 1e-4f); // Terminó contra el borde.
    }

    /**
     * Doblando a la izquierda en el centro de un cruce con una manzana al sureste, el rayo de atrás barre esa manzana:
     * la distancia baja siguiendo la distancia exacta hasta su borde, (TAM_CELDA / 2) / min(sen a, cos a) con a el
     * ángulo de la cámara, sin escalones de 0.25 (antes se desviaba hasta 0.25 de ella).
     */
    public void testGiroALaIzquierdaEnElCruceSinEscalones() {
        int[] cruce = null; // El mismo cruce del caso a 45°.
        for (int[] c : Mapa.intersecciones()) {
            if (c[0] > 0 && c[1] > 0 && !Mapa.esCalleSegura(c[0] + 1, c[1] + 1)) {
                cruce = c;
                break;
            }
        }
        assertNotNull(cruce);
        float x = Mapa.centro(cruce[1]); // Centro del cruce.
        float z = Mapa.centro(cruce[0]);
        double omega = 16 * Auto.VELOCIDAD_GIRO; // Doblando a fondo a velocidad máxima (≈ 101°/s).
        Camara camara = new Camara(Mapa.LIMITE);
        camara.actualizarSeguimiento(0, x, z, 0); // Mirando al norte.
        float anterior = camara.getDistanciaSeguimiento();
        int bajando = 0; // Cuadros en que la distancia bajó.
        for (int cuadro = 1; cuadro <= 90; cuadro++) {
            float angulo = (float) Math.min(Math.PI / 2, omega * cuadro / 60); // Gira a la izquierda hasta 90°.
            camara.actualizarSeguimiento(1 / 60f, x, z, angulo);
            float d = camara.getDistanciaSeguimiento();
            if (d < anterior) { // Se está acercando: vale lo que da el recorte en ese cuadro.
                float a = camara.getAnguloCamara();
                float exacta = Math.min(Camara.DISTANCIA_SEGUIMIENTO,
                    Mapa.TAM_CELDA / 2 / (float) Math.min(Math.sin(a), Math.cos(a))); // Hasta el borde de la manzana.
                assertEquals("cuadro " + cuadro + ", a=" + Math.toDegrees(a), exacta, d, 0.01f);
                bajando++;
            }
            anterior = d;
        }
        assertTrue("bajando=" + bajando, bajando >= 3); // La prueba cubrió el recorte.
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

    /**
     * La aérea nunca queda dentro de un edificio: con la distancia y la elevación mínimas el ojo baja a ≈ 6.8, y las
     * torres llegan a ≈ 37. Se recorre la ciudad con el botón derecho, girando en cada fila, y cada vez que el ojo cae
     * sobre una manzana con edificio debe quedar por encima de su techo.
     */
    public void testAereaNoEntraEnLosEdificios() {
        Camara camara = new Camara(Mapa.LIMITE);
        camara.alternar(); // Orbital.
        camara.alternar(); // Aérea.
        camara.arrastrar(0, 100000); // Elevación mínima.
        camara.zoom(1000); // Distancia mínima.
        int sobreEdificios = 0; // Cuántas veces el ojo cayó sobre un edificio.
        camara.desplazar(1e7, 1e7); // Empieza en una esquina.
        for (int fila = 0; fila < 40; fila++) {
            camara.arrastrar(97, 0); // Otro ángulo en cada fila.
            for (int paso = 0; paso < 40; paso++) {
                camara.desplazar(fila % 2 == 0 ? -90 : 90, 0); // Avanza por la fila.
                float[] ojo = camara.ojoAereo();
                int f = Mapa.indiceCelda(ojo[2]);
                int c = Mapa.indiceCelda(ojo[0]);
                if (f >= 0 && f < Mapa.MAPA.length && c >= 0 && c < Mapa.MAPA.length && Mapa.tipo(f, c) == Mapa.EDIFICIO) {
                    sobreEdificios++;
                    float techo = com.graphics.ciudad.mundo.Edificio.alturaTotal(f, c);
                    assertTrue("ojo a " + ojo[1] + " sobre techo " + techo, ojo[1] >= techo + Camara.MARGEN_TECHO - 1e-4f);
                }
            }
            camara.desplazar(0, 90); // Siguiente fila.
        }
        assertTrue("el recorrido no pasó sobre edificios", sobreEdificios > 20); // La prueba revisó casos reales.
    }

    /** Las marcas del minimapa quedan por encima del edificio más alto y dentro de la profundidad del shader (100). */
    public void testMarcasDelMinimapaSobreLosEdificios() {
        float maximo = com.graphics.ciudad.mundo.Edificio.ALTURA_MAXIMA;
        assertTrue(com.graphics.ciudad.juego.Minimapa.ALTURA_DIVISION > maximo);
        assertTrue(com.graphics.ciudad.juego.Minimapa.ALTURA_DESTINO > com.graphics.ciudad.juego.Minimapa.ALTURA_DIVISION);
        assertTrue(com.graphics.ciudad.juego.Minimapa.ALTURA_INDICADOR > com.graphics.ciudad.juego.Minimapa.ALTURA_DESTINO);
        assertTrue(com.graphics.ciudad.juego.Minimapa.ALTURA_PUNTA < 100); // ESCALA_ALTURA_MAPA de ciudad.vert.
    }

    /** El plano lejano también cubre la punta de la torre más alta vista desde la aérea más lejana. */
    public void testPlanoLejanoCubreLasTorres() {
        Camara camara = new Camara(Mapa.LIMITE);
        float horizontal = camara.getDistanciaAereaMax() + (float) Math.hypot(Mapa.TAMANO, Mapa.TAMANO); // Peor caso.
        float conAltura = (float) Math.hypot(horizontal, com.graphics.ciudad.mundo.Edificio.ALTURA_MAXIMA); // Hasta la punta.
        assertTrue(camara.getPlanoLejano() >= conAltura);
    }
}
