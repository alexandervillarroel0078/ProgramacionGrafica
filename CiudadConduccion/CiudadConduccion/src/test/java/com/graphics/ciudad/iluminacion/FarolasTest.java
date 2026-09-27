package com.graphics.ciudad.iluminacion; // Prueba las farolas desde su mismo paquete.

import com.graphics.ciudad.mundo.Decoracion; // Pasos peatonales.
import com.graphics.ciudad.mundo.Mapa; // Celdas, sectores y vecinos.
import com.graphics.ciudad.mundo.Senalizacion; // Semáforos, PARE y carteles.
import com.graphics.ciudad.vehiculo.Colisiones; // Comprueba que las calles sigan transitables.
import java.util.List; // Piezas del modelo de cada farola.
import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba que las farolas estén en la vereda, con la bombilla sobre el borde de la calle, sin abrir OpenGL. */
public class FarolasTest extends TestCase {

    private static final float DISTANCIA_MINIMA_SENAL = Iluminacion.SEPARACION_SENALES; // Separación mínima entre un poste y un semáforo, PARE o cartel (3).
    private static final float EPSILON = 1e-4f; // Tolerancia para comparar valores calculados en float.

    /** Cada farola está en una manzana adyacente a una calle por el lado indicado, y ninguna en celda transitable. */
    public void testPosteEnLaVeredaYBombillaSobreElBorde() {
        int esperadas = 0; // Suma de las cuotas por sector (13).
        for (int cuota : Iluminacion.FAROLAS_POR_SECTOR) {
            esperadas += cuota;
        }
        assertEquals(esperadas, Iluminacion.LUCES.length); // Todas las cuotas se cumplen.
        assertTrue(Iluminacion.LUCES.length >= 9); // Mínimo pedido.
        assertTrue(Iluminacion.LUCES.length <= Iluminacion.MAX_LUCES); // No puede superar el arreglo uLuces del shader.
        for (int i = 0; i < Iluminacion.LUCES.length; i++) { // Revisa cada farola.
            int[] f = Iluminacion.LUCES[i]; // {fila, columna, lado}.
            String que = "farola " + f[0] + "," + f[1] + " lado " + f[2]; // Mensaje de error útil.
            assertFalse(que, Mapa.esCalle(f[0], f[1])); // La celda es una manzana (edificio o parque).
            int[] haciaCalle = Mapa.VECINOS[f[2]]; // Dirección del lado indicado.
            assertTrue(que, Mapa.esCalleSegura(f[0] + haciaCalle[0], f[1] + haciaCalle[1])); // Por ese lado hay una calle.
            float[] poste = Iluminacion.POSTES[i]; // Posición del poste.
            float[] bombilla = Iluminacion.BOMBILLAS[i]; // Posición de la bombilla.
            assertFalse(que + ": poste en la calle", Mapa.esCalleEn(poste[0], poste[1])); // El poste está sobre la acera.
            assertEquals(que, f[0], Mapa.indiceCelda(poste[1])); // El poste está en la fila de su manzana.
            assertEquals(que, f[1], Mapa.indiceCelda(poste[0])); // Y en su columna.
            assertTrue(que + ": bombilla fuera de la calle", Mapa.esCalleEn(bombilla[0], bombilla[2])); // La bombilla queda sobre la calle...
            float cordonX = Mapa.centro(f[1]) + haciaCalle[1] * Mapa.TAM_CELDA / 2; // Borde de la manzana en X (si el lado es este u oeste).
            float cordonZ = Mapa.centro(f[0]) + haciaCalle[0] * Mapa.TAM_CELDA / 2; // Borde de la manzana en Z (si el lado es norte o sur).
            float voladizo = haciaCalle[1] != 0 ? Math.abs(bombilla[0] - cordonX) : Math.abs(bombilla[2] - cordonZ); // Cuánto entra en la calle.
            assertEquals(que, Iluminacion.BRAZO_FAROLA - Iluminacion.MARGEN_POSTE, voladizo, EPSILON); // ...sobre el borde: 1.1 más allá del cordón.
            float brazo = (float) Math.hypot(bombilla[0] - poste[0], bombilla[2] - poste[1]); // Distancia poste → bombilla.
            assertEquals(que, Iluminacion.BRAZO_FAROLA, brazo, EPSILON); // La bombilla está en la punta del brazo.
        }
    }

    /** Hay farolas en los cinco sectores, y ninguna coincide con semáforos, PARE, carteles ni pasos peatonales. */
    public void testRepartoYSinConflictos() {
        boolean[] sectores = new boolean[Mapa.SECTORES.length]; // Sectores con al menos una farola.
        for (int i = 0; i < Iluminacion.LUCES.length; i++) { // Revisa cada farola.
            float[] poste = Iluminacion.POSTES[i]; // Posición del poste.
            float[] bombilla = Iluminacion.BOMBILLAS[i]; // Posición de la bombilla.
            sectores[Mapa.sector(poste[0], poste[1])] = true; // Marca el sector.
            for (float[] s : Senalizacion.SEMAFOROS) { // Semáforos.
                assertTrue(Math.hypot(poste[0] - s[0], poste[1] - s[1]) >= DISTANCIA_MINIMA_SENAL); // Lejos de cada cabezal.
            }
            for (float[] p : Senalizacion.PARES) { // Señales de PARE.
                assertTrue(Math.hypot(poste[0] - p[0], poste[1] - p[1]) >= DISTANCIA_MINIMA_SENAL); // Lejos de cada PARE.
            }
            for (float[] c : Senalizacion.CARTELES_SECTOR) { // Carteles de sector.
                assertTrue(Math.hypot(poste[0] - c[1], poste[1] - c[2]) >= DISTANCIA_MINIMA_SENAL); // Lejos de cada cartel.
            }
            assertFalse(Decoracion.hayPasoSobre(poste[0], poste[1], Iluminacion.HOLGURA_POSTE_PASO, Iluminacion.HOLGURA_POSTE_PASO)); // El poste no pisa un paso peatonal.
            assertFalse(Decoracion.hayPasoSobre(bombilla[0], bombilla[2], Iluminacion.HOLGURA_BOMBILLA_PASO, Iluminacion.HOLGURA_BOMBILLA_PASO)); // La bombilla no cuelga sobre un paso.
        }
        for (boolean hay : sectores) { // Revisa los cinco sectores.
            assertTrue(hay); // Todos tienen farolas.
        }
    }

    /**
     * Reglas de ubicación: cada farola es válida (vereda, sector, lejos de señales y pasos), está a mitad de cuadra
     * (centro del borde de su manzana) y cada sector tiene exactamente su cuota.
     */
    public void testReglasDeUbicacion() {
        int[] porSector = new int[Mapa.SECTORES.length]; // Farolas encontradas en cada sector.
        for (int i = 0; i < Iluminacion.LUCES.length; i++) {
            int[] f = Iluminacion.LUCES[i];
            float[] poste = Iluminacion.POSTES[i];
            int sector = Mapa.sector(poste[0], poste[1]);
            porSector[sector]++;
            assertTrue("farola " + i, Iluminacion.farolaValida(f[0], f[1], f[2], sector)); // Cumple todos los criterios.
            int[] haciaCalle = Mapa.VECINOS[f[2]];
            if (haciaCalle[0] != 0) { // Borde norte o sur: la cuadra corre en X.
                assertEquals(Mapa.centro(f[1]), poste[0], EPSILON); // A mitad de cuadra.
            } else { // Borde oeste o este: la cuadra corre en Z.
                assertEquals(Mapa.centro(f[0]), poste[1], EPSILON);
            }
            for (int j = 0; j < i; j++) { // Sin repetir el mismo borde.
                assertFalse(java.util.Arrays.equals(f, Iluminacion.LUCES[j]));
            }
        }
        for (int s = 0; s < porSector.length; s++) {
            assertEquals(Mapa.NOMBRES_SECTORES[s], Iluminacion.FAROLAS_POR_SECTOR[s], porSector[s]); // Cuota del sector.
        }
    }

    /** Busca la única pieza BOMBILLA del modelo de una farola. */
    private static Iluminacion.Pieza bombillaDibujada(List<Iluminacion.Pieza> modelo) {
        Iluminacion.Pieza encontrada = null; // Todavía ninguna.
        for (Iluminacion.Pieza p : modelo) { // Recorre las piezas.
            if (p.parte == Iluminacion.Parte.BOMBILLA) { // Es la bombilla.
                assertNull("una sola bombilla por farola", encontrada); // No hay dos.
                encontrada = p; // La guarda.
            }
        }
        assertNotNull("la farola no dibuja bombilla", encontrada); // Tiene que existir.
        return encontrada; // Pieza de la bombilla.
    }

    /** La bombilla dibujada está exactamente donde el shader recibe la luz de esa farola, de día y de noche. */
    public void testBombillaDibujadaEnLaPosicionDeLaLuz() {
        assertTrue(Iluminacion.LUCES.length >= 9); // Al menos 9 farolas.
        for (int i = 0; i < Iluminacion.LUCES.length; i++) { // Revisa cada farola.
            float[] luz = Iluminacion.BOMBILLAS[i]; // Lo que preparar() envía como uLuces[i].
            for (boolean noche : new boolean[] {false, true}) { // Día y noche.
                Iluminacion.Pieza b = bombillaDibujada(Iluminacion.modeloFarola(i, noche)); // Bombilla del modelo.
                assertEquals(luz[0], b.x, 0f); // Misma X, sin tolerancia: sale de la misma constante.
                assertEquals(luz[1], b.y, 0f); // Misma altura.
                assertEquals(luz[2], b.z, 0f); // Misma Z.
            }
        }
    }

    /** De día la bombilla es gris claro sin emisión; de noche es emisiva y blanco cálido. Nada más brilla. */
    public void testBombillaEmisivaSoloDeNoche() {
        for (int i = 0; i < Iluminacion.LUCES.length; i++) { // Revisa cada farola.
            Iluminacion.Pieza dia = bombillaDibujada(Iluminacion.modeloFarola(i, false)); // Bombilla de día.
            assertFalse(dia.emisiva); // Apagada.
            assertSame(Iluminacion.COLOR_BOMBILLA_DIA, dia.color); // Gris claro.
            Iluminacion.Pieza noche = bombillaDibujada(Iluminacion.modeloFarola(i, true)); // Bombilla de noche.
            assertTrue(noche.emisiva); // Encendida.
            assertSame(Iluminacion.COLOR_BOMBILLA_NOCHE, noche.color); // Blanco cálido.
            for (Iluminacion.Pieza p : Iluminacion.modeloFarola(i, true)) { // El resto de la farola...
                if (p.parte != Iluminacion.Parte.BOMBILLA) {
                    assertFalse(p.emisiva); // ...recibe la iluminación normal.
                }
            }
        }
    }

    /**
     * El modelo está conectado: la base apoya en la acera, el brazo sale de la punta del poste y termina sobre la
     * bombilla, a la altura de la punta de la pantalla, y cada tramo inclinado mide y apunta como indica el perfil.
     */
    public void testModeloConectado() {
        for (int i = 0; i < Iluminacion.LUCES.length; i++) { // Revisa cada farola.
            List<Iluminacion.Pieza> modelo = Iluminacion.modeloFarola(i, false); // Piezas de la farola.
            float[] poste = Iluminacion.POSTES[i]; // Centro del poste.
            float[] luz = Iluminacion.BOMBILLAS[i]; // Centro de la bombilla.
            Iluminacion.Pieza base = modelo.get(0); // La primera pieza es la base.
            assertEquals(Iluminacion.Parte.BASE, base.parte);
            assertEquals(Iluminacion.ALTURA_ACERA, base.y - base.sy / 2, EPSILON); // Apoyada en la acera.
            float tope = 0; // Punto más alto del poste.
            int tramos = 0; // Tramos del brazo encontrados.
            for (Iluminacion.Pieza p : modelo) { // Recorre las piezas.
                if (p.parte == Iluminacion.Parte.POSTE) {
                    assertEquals(poste[0], p.x, EPSILON); // Vertical sobre el centro del poste.
                    assertEquals(poste[1], p.z, EPSILON);
                    tope = Math.max(tope, p.y + p.sy / 2); // Punta del tramo.
                }
                if (p.parte == Iluminacion.Parte.BRAZO) {
                    float[] r = p.rotacion; // Columna 1 = dirección del eje del cilindro.
                    float extremoX = p.x + r[3] * p.sy / 2; // Extremo final del tramo.
                    float extremoY = p.y + r[4] * p.sy / 2;
                    float extremoZ = p.z + r[5] * p.sy / 2;
                    tramos++; // Cuenta el tramo.
                    if (tramos == Iluminacion.PERFIL_BRAZO.length - 1) { // El último tramo...
                        assertEquals(luz[0], extremoX, EPSILON); // ...termina sobre la bombilla...
                        assertEquals(luz[2], extremoZ, EPSILON);
                        assertEquals(Iluminacion.ALTURA_BRAZO, extremoY, EPSILON); // ...en la punta de la pantalla.
                    }
                    float[] c0 = {r[0], r[1], r[2]}, c1 = {r[3], r[4], r[5]}, c2 = {r[6], r[7], r[8]}; // Columnas.
                    assertEquals(0, c0[0] * c1[0] + c0[1] * c1[1] + c0[2] * c1[2], EPSILON); // Perpendiculares...
                    assertEquals(0, c0[0] * c2[0] + c0[1] * c2[1] + c0[2] * c2[2], EPSILON);
                    assertEquals(1, c1[0] * c1[0] + c1[1] * c1[1] + c1[2] * c1[2], EPSILON); // ...y de largo 1.
                }
            }
            assertEquals(Iluminacion.alturaTopePoste(), tope, EPSILON); // El poste llega al primer punto del brazo.
            assertEquals(Iluminacion.PERFIL_BRAZO.length - 1, tramos); // Un tramo por cada par de puntos (3).
        }
    }

    // ==================== LUZ DE LAS FAROLAS: FOCO HACIA ABAJO ====================
    // Los valores se leen de iluminacion.frag: el shader es la única fuente y el test no puede quedar desactualizado.

    /** Lee una constante float del shader de iluminación, por ejemplo "ANGULO_FAROLA_EXTERIOR". */
    private static float constanteShader(String nombre) throws Exception {
        java.io.InputStream entrada = Iluminacion.class.getResourceAsStream("/shaders/iluminacion.frag"); // Desde el classpath.
        assertNotNull(entrada);
        String texto = new String(entrada.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("const float " + nombre + "\\s*=\\s*([0-9.]+)").matcher(texto);
        assertTrue("falta " + nombre + " en iluminacion.frag", m.find());
        return Float.parseFloat(m.group(1)); // Su valor.
    }

    /** smoothstep de GLSL: 0 bajo a, 1 sobre b y curva suave entre ambos. */
    private static float smoothstep(float a, float b, float x) {
        float t = Math.max(0, Math.min(1, (x - a) / (b - a))); // Posición entre a y b, recortada a 0..1.
        return t * t * (3 - 2 * t); // Curva suave (Hermite), igual que en GLSL.
    }

    /** Factor del cono de la farola (0..1) en un punto: la misma cuenta que factorCono() con eje (0, -1, 0). */
    private static float conoFarola(float[] bombilla, float[] punto) throws Exception {
        float dx = punto[0] - bombilla[0], dy = punto[1] - bombilla[1], dz = punto[2] - bombilla[2]; // Bombilla → punto.
        float alineacion = -dy / (float) Math.sqrt(dx * dx + dy * dy + dz * dz); // Producto punto con (0, -1, 0).
        float exterior = (float) Math.cos(Math.toRadians(constanteShader("ANGULO_FAROLA_EXTERIOR"))); // Cosenos, como el shader.
        float interior = (float) Math.cos(Math.toRadians(constanteShader("ANGULO_FAROLA_INTERIOR")));
        return smoothstep(exterior, interior, alineacion);
    }

    /** Aporte de una farola (sin el color) a un punto con esa normal: réplica del bucle de farolas del shader. */
    private static float aporteFarola(float[] bombilla, float[] punto, float[] normal) throws Exception {
        float lx = bombilla[0] - punto[0], ly = bombilla[1] - punto[1], lz = bombilla[2] - punto[2]; // Punto → bombilla.
        float distancia = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
        float difusa = Math.max((normal[0] * lx + normal[1] * ly + normal[2] * lz) / distancia, 0); // Lambert.
        float atenuacion = 1 + constanteShader("FAROLA_ATENUACION_LINEAL") * distancia
                + constanteShader("FAROLA_ATENUACION_CUADRATICA") * distancia * distancia;
        return conoFarola(bombilla, punto) * difusa * constanteShader("INTENSIDAD_FAROLA") / atenuacion;
    }

    private static final float[] ARRIBA = {0, 1, 0}; // Normal del suelo y de la vereda.
    private static final float[] ABAJO = {0, -1, 0}; // Normal de una cara que mira al suelo (debajo de la pantalla).

    /** Por encima de la bombilla no llega luz; justo debajo llega el máximo; fuera del cono exterior, nada. */
    public void testFarolaEsFocoHaciaAbajo() throws Exception {
        float[] b = Iluminacion.BOMBILLAS[0]; // Una farola cualquiera: todas usan la misma fórmula.
        float[] encima = {b[0], b[1] + 0.2f, b[2]}; // Dentro de la pantalla, en una cara que mira hacia la bombilla.
        assertEquals(0f, aporteFarola(b, encima, ABAJO), 0f); // Una luz puntual sí lo iluminaría.
        float[] costadoAlto = {b[0] + 2, b[1] + 0.01f, b[2]}; // Apenas por encima de la bombilla, a un costado.
        assertEquals(0f, conoFarola(b, costadoAlto), 0f); // Detrás del eje: fuera del cono.

        float[] debajo = {b[0], 0, b[2]}; // Suelo justo bajo la bombilla.
        assertEquals(1f, conoFarola(b, debajo), 0f); // Centro del haz: cono pleno.
        float maximo = aporteFarola(b, debajo, ARRIBA); // Aporte en el centro del círculo de luz.
        assertTrue(maximo > 0);
        for (float h = 0.25f; h <= 10; h += 0.25f) { // Cualquier otro punto del suelo recibe menos.
            assertTrue(aporteFarola(b, new float[] {b[0] + h, 0, b[2]}, ARRIBA) < maximo);
        }

        float angulo = (float) Math.toRadians(constanteShader("ANGULO_FAROLA_EXTERIOR") + 1); // Un grado fuera del borde.
        float[] fuera = {b[0] + b[1] * (float) Math.tan(angulo), 0, b[2]}; // En el suelo, a ese ángulo de la vertical.
        assertEquals(0f, aporteFarola(b, fuera, ARRIBA), 0f);
    }

    /** La luz disminuye con la distancia a lo largo del eje del foco (atenuación lineal + cuadrática). */
    public void testFarolaDisminuyeConLaDistancia() throws Exception {
        float[] b = Iluminacion.BOMBILLAS[0]; // Una farola cualquiera.
        float anterior = Float.MAX_VALUE; // Aporte del punto anterior, más cercano.
        for (float d = 0.5f; d <= b[1]; d += 0.5f) { // Superficies horizontales cada vez más abajo, hasta el suelo.
            float aporte = aporteFarola(b, new float[] {b[0], b[1] - d, b[2]}, ARRIBA);
            assertTrue("a " + d + " de la bombilla", aporte < anterior); // Más lejos, menos luz.
            anterior = aporte;
        }
    }

    /**
     * El círculo de luz cubre toda la vereda (hasta la pared del edificio) y llega al centro de la calzada; en la
     * pared, a la altura de la pantalla, ya no llega luz.
     */
    public void testCirculoDeLuzEnVeredaYCalzada() throws Exception {
        float sobreCalzada = Iluminacion.BRAZO_FAROLA - Iluminacion.MARGEN_POSTE; // Cuánto sobresale la bombilla del cordón.
        float anchoVereda = (Mapa.TAM_CELDA - Mapa.ANCHO_EDIFICIO) / 2; // La vereda más ancha: la de un edificio.
        float haciaPared = sobreCalzada + anchoVereda; // De la bombilla a la pared, hacia adentro de la manzana.
        float haciaCentro = Mapa.TAM_CELDA / 2 - sobreCalzada; // De la bombilla al centro de la calzada.
        for (int i = 0; i < Iluminacion.LUCES.length; i++) { // Revisa cada farola.
            float[] b = Iluminacion.BOMBILLAS[i];
            int[] haciaCalle = Mapa.VECINOS[Iluminacion.LUCES[i][2]]; // {fila, columna}: z sigue a la fila, x a la columna.
            float ux = haciaCalle[1], uz = haciaCalle[0]; // Dirección horizontal hacia la calle.
            float[] bordeVereda = {b[0] - ux * haciaPared, Iluminacion.ALTURA_ACERA, b[2] - uz * haciaPared};
            assertTrue("vereda de la farola " + i, aporteFarola(b, bordeVereda, ARRIBA) > 0);
            float[] centroCalle = {b[0] + ux * haciaCentro, 0, b[2] + uz * haciaCentro};
            assertTrue("calzada de la farola " + i, aporteFarola(b, centroCalle, ARRIBA) > 0);
            float[] paredAlta = {b[0] - ux * haciaPared, Iluminacion.ALTURA_BRAZO, b[2] - uz * haciaPared};
            assertEquals(0f, conoFarola(b, paredAlta), 0f); // A la altura de la pantalla, la pared queda a oscuras.
        }
    }

    /** Las calles siguen transitables: las farolas no agregan obstáculos para el auto ni el tráfico. */
    public void testColisionesIntactas() {
        for (int fila = 0; fila < Mapa.MAPA.length; fila++) { // Recorre las filas.
            for (int columna = 0; columna < Mapa.MAPA[fila].length; columna++) { // Recorre las columnas.
                if (Mapa.esCalle(fila, columna)) { // Solo las calles.
                    assertTrue(Colisiones.puedeCircular(Mapa.centro(columna), Mapa.centro(fila))); // El centro de cada calle es transitable.
                }
            }
        }
    }
}
