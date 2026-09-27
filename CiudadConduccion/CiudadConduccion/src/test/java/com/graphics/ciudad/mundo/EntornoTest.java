package com.graphics.ciudad.mundo; // Prueba el campo que rodea la ciudad desde su mismo paquete.

import com.graphics.ciudad.juego.Entregas; // Paradas que deben seguir siendo transitables.
import com.graphics.ciudad.vehiculo.Auto; // Salida y radio del auto.
import com.graphics.ciudad.vehiculo.Colisiones; // Reglas de colisión, que el entorno no debe cambiar.
import java.util.List; // Lista de árboles.
import junit.framework.TestCase; // Proporciona las comprobaciones de JUnit usadas por Maven.

/** Comprueba que el entorno es solo decoración: no cambia Mapa.LIMITE ni las colisiones, y sus árboles quedan afuera. */
public class EntornoTest extends TestCase {

    /** El campo se extiende más allá del borde, pero el límite de la ciudad sigue siendo el mismo. */
    public void testNoCambiaElLimite() {
        assertEquals(110f, Mapa.TAMANO, 0f); // La ciudad sigue midiendo 11 celdas de 10.
        assertEquals(55f, Mapa.LIMITE, 0f); // El borde sigue a 55 del origen.
        assertEquals(Mapa.LIMITE + Entorno.ENTORNO_EXTRA, Entorno.BORDE_CAMPO, 0f); // El campo se suma afuera, no cambia el límite.
        assertTrue(Entorno.ENTORNO_EXTRA >= Mapa.LIMITE); // "Bastante más allá": al menos otra media ciudad por lado.
    }

    /** Las colisiones son las de siempre: las calles se pueden recorrer y el auto no sale al campo ni sube al cordón. */
    public void testNoCambiaLasColisiones() {
        for (float z = -50; z <= 50; z += 0.5f) { // Calle perimetral oeste, pegada al cordón.
            assertTrue(Colisiones.puedeCircular(-50, z)); // Sigue transitable.
            assertTrue(Colisiones.puedeCircular(50, z)); // Y la del este.
        }
        assertTrue(Colisiones.puedeCircular(Auto.X_INICIAL, Auto.Z_INICIAL)); // La salida no cambió.
        for (float[] destino : Entregas.DESTINOS) { // Las entregas siguen accesibles.
            assertTrue(Colisiones.puedeCircular(destino[0], destino[1]));
        }
        float cordon = Mapa.LIMITE + Entorno.ANCHO_CORDON / 2; // Centro del cordón.
        assertFalse(Colisiones.puedeCircular(cordon, 0)); // El auto no sube al cordón...
        assertFalse(Colisiones.puedeCircular(0, -cordon)); // ...de ningún lado.
        float limitePermitido = Mapa.LIMITE - Auto.RADIO_AUTO; // El borde que Colisiones usaba antes del entorno.
        assertTrue(Colisiones.puedeCircular(limitePermitido - 0.01f, 0)); // Justo adentro: se puede.
        assertFalse(Colisiones.puedeCircular(limitePermitido + 0.01f, 0)); // Justo afuera: no, igual que antes.
        for (float[] arbol : Entorno.ARBOLES) { // Tampoco se llega a ningún árbol del campo.
            assertFalse(Colisiones.puedeCircular(arbol[0], arbol[1]));
        }
    }

    /** Los árboles están todos afuera de la ciudad y del cordón, dentro del campo, con medidas de parque. */
    public void testArbolesAfuera() {
        List<float[]> arboles = Entorno.ARBOLES; // Árboles del campo.
        assertEquals(Entorno.CANTIDAD_ARBOLES, arboles.size()); // El sorteo encontró lugar para todos.
        float libre = Mapa.LIMITE + Entorno.DISTANCIA_MIN_ARBOL; // Nadie a menos de esto del borde.
        assertTrue(Entorno.DISTANCIA_MIN_ARBOL > Entorno.ANCHO_CORDON + Parque.COPA_MAXIMA / 2); // La copa no toca el cordón.
        for (float[] a : arboles) {
            float lejania = Math.max(Math.abs(a[0]), Math.abs(a[1])); // Distancia al origen en el eje más largo.
            assertTrue("árbol en " + a[0] + "," + a[1], lejania >= libre); // Afuera de la ciudad y del cordón.
            assertTrue(lejania + a[4] / 2 <= Entorno.BORDE_CAMPO); // Sobre el pasto, sin pasarse del campo.
            assertTrue(a[2] == Parque.PINO || a[2] == Parque.FRONDOSO); // Los mismos tipos que los parques.
            assertTrue(a[4] <= Parque.COPA_MAXIMA); // Copa de tamaño de parque.
        }
    }

    /** Siempre los mismos árboles, sin Random: calcular dos veces da exactamente lo mismo. */
    public void testArbolesDeterministicos() {
        List<float[]> otra = Entorno.calcularArboles(); // Segundo cálculo.
        assertEquals(Entorno.ARBOLES.size(), otra.size());
        for (int i = 0; i < otra.size(); i++) {
            assertTrue(java.util.Arrays.equals(Entorno.ARBOLES.get(i), otra.get(i))); // Mismo árbol, mismo lugar.
        }
        boolean hayPino = false; // Hay variedad de tipos.
        boolean hayFrondoso = false;
        for (float[] a : Entorno.ARBOLES) {
            hayPino |= a[2] == Parque.PINO;
            hayFrondoso |= a[2] == Parque.FRONDOSO;
        }
        assertTrue(hayPino && hayFrondoso);
    }
}
