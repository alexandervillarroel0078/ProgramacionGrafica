package com.graphics.ciudad.mundo; // Prueba el recorrido peatonal de los parques desde su mismo paquete.

import java.util.ArrayList; // Lista de pasos que llegan a un parque.
import java.util.List; // Tipo de esas listas.
import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/**
 * Coherencia peatonal de los parques: el peatón que baja de un paso pisa pavimento, llega por él a un brazo de la
 * cruz, rodea la fuente por la plaza hasta cualquier otro brazo, y nada (árboles, bancos, basureros, luminarias) se
 * apoya sobre la plaza ni las franjas. Corre también con el 13 × 13 de referencia (segunda pasada de mvn test).
 */
public class ParquePeatonalTest extends TestCase {

    private static final float PASO_MUESTRA = 0.05f; // Separación entre los puntos que se revisan.

    /**
     * Pasos que llegan a un borde del parque, cada uno {desvio, lado, ejeX}: la misma geometría que Decoracion usa para
     * ubicarlos (la celda de calle vecina, al norte o sur si la calle va de oeste a este; al oeste o este si no).
     * Se calcula acá por separado de Parque.franjas(), para que la prueba no repita el código que prueba.
     */
    private static List<float[]> pasosQueLlegan(int fila, int columna) {
        List<float[]> lista = new ArrayList<>();
        for (float[] paso : Decoracion.UBICACIONES_PASOS) {
            boolean ejeX = paso[2] == 1;
            int filaPaso = Mapa.indiceCelda(paso[1]);
            int columnaPaso = Mapa.indiceCelda(paso[0]);
            if (ejeX && columnaPaso == columna && Math.abs(filaPaso - fila) == 1) {
                lista.add(new float[] {paso[0] - Mapa.centro(columna), filaPaso - fila, 1});
            } else if (!ejeX && filaPaso == fila && Math.abs(columnaPaso - columna) == 1) {
                lista.add(new float[] {paso[1] - Mapa.centro(fila), columnaPaso - columna, 0});
            }
        }
        return lista;
    }

    /** Convierte (a lo largo del borde, de través) en coordenadas del mundo según la orientación del borde. */
    private static float[] punto(int fila, int columna, boolean ejeX, float aLoLargo, float deTraves) {
        float cx = Mapa.centro(columna);
        float cz = Mapa.centro(fila);
        return ejeX ? new float[] {cx + aLoLargo, cz + deTraves} : new float[] {cx + deTraves, cz + aLoLargo};
    }

    /**
     * Cada paso que llega a un parque desemboca en pavimento: toda su boca (el largo del paso, recortado al césped) está
     * pavimentada en el borde, y desde cada punto de la boca se puede caminar sobre pavimento, a lo largo del borde,
     * hasta el eje del brazo más cercano.
     */
    public void testCadaPasoDesembocaEnPavimento() {
        int pasos = 0;
        for (int[] p : Mapa.parques()) {
            for (float[] paso : pasosQueLlegan(p[0], p[1])) {
                pasos++;
                boolean ejeX = paso[2] == 1;
                float borde = paso[1] * (Parque.MITAD_CESPED - 0.01f); // Apenas adentro del césped.
                float desde = Math.max(-Parque.MITAD_CESPED, paso[0] - Decoracion.LARGO_PASO / 2);
                float hasta = Math.min(Parque.MITAD_CESPED, paso[0] + Decoracion.LARGO_PASO / 2);
                String que = "parque " + p[0] + "," + p[1] + " paso con desvío " + paso[0];
                for (float s = desde; s <= hasta; s += PASO_MUESTRA) { // Boca del paso.
                    float[] q = punto(p[0], p[1], ejeX, s, borde);
                    assertTrue(que + ": césped en la boca", Parque.enPavimento(p[0], p[1], q[0], q[1]));
                }
                float centroPaso = paso[0];
                for (float s = centroPaso; Math.abs(s) > PASO_MUESTRA; s -= Math.signum(s) * PASO_MUESTRA) { // Hacia el brazo.
                    float[] q = punto(p[0], p[1], ejeX, s, borde);
                    assertTrue(que + ": césped camino al brazo", Parque.enPavimento(p[0], p[1], q[0], q[1]));
                }
            }
        }
        assertTrue("ningún paso llega a un parque", pasos > 0);
    }

    /**
     * La plaza conecta los 4 brazos: todo el anillo entre la fuente y MITAD_PLAZA está pavimentado (se recorre con
     * varios radios y todos los ángulos), el anillo mide al menos PASO_FUENTE, y cada brazo sigue pavimentado desde la
     * plaza hasta el borde del parque.
     */
    public void testPlazaConectaLosCuatroBrazos() {
        assertTrue("paso alrededor de la fuente", Parque.MITAD_PLAZA - Parque.RADIO_FUENTE >= Parque.PASO_FUENTE - 1e-4f);
        // Vértice del octógono regular = apotema / cos(22.5°) ≈ 2.38: la plaza queda lejos del borde del césped.
        assertTrue("la plaza entra en el césped", Parque.MITAD_PLAZA / Math.cos(Math.PI / 8) < Parque.MITAD_CESPED);
        for (int[] p : Mapa.parques()) {
            float cx = Mapa.centro(p[1]);
            float cz = Mapa.centro(p[0]);
            String que = "parque " + p[0] + "," + p[1];
            for (float r = Parque.RADIO_FUENTE + 0.02f; r <= Parque.MITAD_PLAZA; r += 0.1f) { // Anillo alrededor de la fuente.
                for (int k = 0; k < 360; k++) { // Una vuelta completa: pasa por los 4 brazos.
                    double a = Math.toRadians(k);
                    float x = cx + (float) (r * Math.cos(a));
                    float z = cz + (float) (r * Math.sin(a));
                    assertTrue(que + ": césped en el anillo, r=" + r + " ángulo=" + k, Parque.enPavimento(p[0], p[1], x, z));
                }
            }
            for (float d = Parque.MITAD_PLAZA; d <= Parque.MITAD_CESPED; d += PASO_MUESTRA) { // Brazos hasta el borde.
                assertTrue(que + " brazo este", Parque.enPavimento(p[0], p[1], cx + d, cz));
                assertTrue(que + " brazo oeste", Parque.enPavimento(p[0], p[1], cx - d, cz));
                assertTrue(que + " brazo sur", Parque.enPavimento(p[0], p[1], cx, cz + d));
                assertTrue(que + " brazo norte", Parque.enPavimento(p[0], p[1], cx, cz - d));
            }
        }
    }

    /** Cada banco tiene su basurero al costado (a lo sumo a la distancia de Parque.lugarBasurero()). */
    public void testCadaBancoTieneSuBasurero() {
        float alcance = (float) Math.hypot(Parque.MITAD_BANCO + Basureros.SEPARACION_BANCO + Basureros.RADIO, Basureros.RETIRO_BANCO) + 0.01f;
        assertEquals("un basurero por banco", 1, Basureros.BANCOS_POR_BASURERO);
        for (int[] p : Mapa.parques()) {
            for (float[] banco : Parque.bancos(p[0], p[1])) {
                boolean tiene = false;
                for (float[] b : Basureros.UBICACIONES) {
                    tiene |= b[3] == Basureros.EN_PARQUE && Math.hypot(b[0] - banco[0], b[2] - banco[1]) <= alcance;
                }
                assertTrue("banco sin basurero en el parque " + p[0] + "," + p[1], tiene);
            }
        }
    }

    /** Los bancos miran la fuente desde el borde de la plaza: el centro de su frente, a poca distancia del lado diagonal. */
    public void testBancosEnElBordeDeLaPlaza() {
        for (int[] p : Mapa.parques()) {
            float cx = Mapa.centro(p[1]);
            float cz = Mapa.centro(p[0]);
            for (float[] b : Parque.bancos(p[0], p[1])) {
                float frenteX = b[0] - (float) Math.sin(b[2]) * Parque.MITAD_FONDO_BANCO - cx; // Centro del frente.
                float frenteZ = b[1] - (float) Math.cos(b[2]) * Parque.MITAD_FONDO_BANCO - cz;
                float aire = Parque.radioOctogonal(frenteX, frenteZ) - Parque.MITAD_PLAZA;
                assertTrue("banco lejos de la plaza (" + aire + ") en " + p[0] + "," + p[1], aire > 0 && aire <= 0.3f);
            }
        }
    }

    /**
     * Nada pisa el pavimento. Árboles: ni el tronco ni la copa de un pino (ramas a la altura de una persona) tocan
     * senderos, plaza ni franjas. Bancos: ningún punto del asiento o del respaldo está sobre el pavimento. Basureros:
     * fuera del pavimento. Luminarias: van en el borde de un brazo (a propósito), pero no en la plaza ni en las franjas.
     */
    public void testNadaPisaLaPlazaNiLasFranjas() {
        for (int[] p : Mapa.parques()) {
            int f = p[0];
            int c = p[1];
            String que = "parque " + f + "," + c;
            for (float[] a : Parque.arboles(f, c)) {
                assertFalse(que + ": tronco sobre el pavimento", Parque.tocaPavimento(f, c, a[0], a[1], 0.17f));
                if (a[2] == Parque.PINO) {
                    assertFalse(que + ": pino sobre el pavimento", Parque.tocaPavimento(f, c, a[0], a[1], a[4] / 2 - 1e-3f));
                }
            }
            for (float[] b : Parque.bancos(f, c)) {
                float coseno = (float) Math.cos(b[2]);
                float seno = (float) Math.sin(b[2]);
                for (float lx = -Parque.MITAD_BANCO; lx <= Parque.MITAD_BANCO + 1e-4f; lx += 0.1f) { // Largo del banco.
                    for (float lz = -Parque.MITAD_FONDO_BANCO; lz <= Parque.FONDO_RESPALDO + 1e-4f; lz += 0.0265f) { // Del frente al respaldo.
                        float x = b[0] + coseno * lx + seno * lz; // Misma transformación que Parque.pieza().
                        float z = b[1] - seno * lx + coseno * lz;
                        assertFalse(que + ": banco sobre el pavimento", Parque.enPavimento(f, c, x, z));
                    }
                }
            }
            for (float[] b : Basureros.UBICACIONES) {
                if (b[3] == Basureros.EN_PARQUE && Mapa.indiceCelda(b[0]) == c && Mapa.indiceCelda(b[2]) == f) {
                    assertFalse(que + ": basurero sobre el pavimento", Parque.tocaPavimento(f, c, b[0], b[2], Basureros.RADIO));
                }
            }
            for (float[] l : Parque.luminarias(f, c)) {
                float radioBase = Parque.ANCHO_BASE_GLOBO / 2;
                assertFalse(que + ": luminaria en la plaza", Parque.tocaPlaza(f, c, l[0], l[1], radioBase));
                assertFalse(que + ": luminaria en una franja", Parque.tocaFranja(f, c, l[0], l[1], radioBase));
            }
        }
    }
}
