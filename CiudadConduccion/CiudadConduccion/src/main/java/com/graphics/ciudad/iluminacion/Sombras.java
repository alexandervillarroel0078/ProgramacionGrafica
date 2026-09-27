package com.graphics.ciudad.iluminacion; // Agrupa el estado de las luces de la escena.

import com.graphics.ciudad.motor.Cubo; // Cada sombra es una caja muy fina.
import com.graphics.ciudad.motor.Shader; // Modo sombra (uSombra), forma y opacidad.
import com.graphics.ciudad.mundo.Edificio; // Huella de los edificios.
import com.graphics.ciudad.mundo.Entorno; // Árboles del campo.
import com.graphics.ciudad.mundo.Mapa; // Celdas de edificio y de parque.
import com.graphics.ciudad.mundo.Parque; // Árboles y bancos de los parques.
import com.graphics.ciudad.trafico.Vehiculo; // Vehículos del tráfico.
import com.graphics.ciudad.vehiculo.Auto; // Auto del jugador.
import java.util.ArrayList; // Lista de sombras fijas.
import java.util.Collections; // Publica la lista sin permitir modificarla.
import java.util.List; // Tipo de esa lista.
import static org.lwjgl.opengl.GL33.*; // Mezcla (blending) y máscara de profundidad.

/**
 * SOMBRAS FALSAS ("blob shadows"): una mancha oscura y semitransparente en el suelo, debajo de cada edificio, auto,
 * árbol y banco. No se calcula desde dónde viene la luz (eso serían sombras reales, mucho más caras): solo se oscurece
 * el suelo bajo el objeto, y eso alcanza para que parezca "apoyado" y no flotando.
 *
 * FORMA CON BORDES DIFUSOS: cada sombra se dibuja con el cubo aplastado (GROSOR_SOMBRA de alto), del tamaño y con el
 * giro del objeto. iluminacion.frag (rama uSombra) recibe la posición LOCAL del fragmento dentro del cubo (-0.5 a 0.5)
 * y calcula una "distancia al centro" d: 0 en el medio y 1 en el borde. La opacidad baja suavemente con smoothstep
 * entre el núcleo y el borde, así la mancha es un degradado circular y nunca un rectángulo con bordes duros.
 * El núcleo (uNucleoSombra) es la parte pareja del centro: en autos, árboles y bancos es NUCLEO_SOMBRA; en los
 * edificios coincide con las paredes, porque lo de adentro queda tapado y solo se ve el degradado sobre la acera.
 * uFormaSombra elige la curva: 2 = elipse (autos, árboles, bancos); 4 = "superelipse", un rectángulo de esquinas
 * redondeadas (edificios, que son cuadrados).
 *
 * BLENDING (MEZCLA): normalmente cada fragmento REEMPLAZA el color del píxel. Con glEnable(GL_BLEND) y
 * glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA), OpenGL COMBINA los dos colores:
 *     final = colorSombra · alfa + colorQueYaEstaba · (1 - alfa)
 * Con un color casi negro y alfa 0.45 el asfalto queda al 55 % de su brillo: se oscurece sin taparlo. Donde el
 * degradado lleva alfa a 0 (el borde), el suelo queda intacto.
 *
 * POR QUÉ SE DESACTIVA LA ESCRITURA DE PROFUNDIDAD (glDepthMask(false)):
 *  - La prueba de profundidad SIGUE activa: la sombra no se pinta encima de las ruedas o las paredes que están
 *    delante de ella (el edificio tapa la parte de la mancha que queda debajo suyo).
 *  - Pero no ESCRIBE su distancia: si lo hiciera, dos sombras superpuestas (dos autos juntos) se cortarían entre sí
 *    con un borde duro, y la parte casi transparente del degradado "taparía" lo que se dibuje después debajo de ella.
 *  - Por eso las sombras van AL FINAL de la escena opaca: la mezcla necesita que el suelo ya esté pintado.
 * ELEVACION_SOMBRA la separa apenas del suelo: si estuviera en el mismo plano, las dos superficies tendrían la misma
 * profundidad y parpadearían (z-fighting). De noche la sombra es más tenue (ALFA_SOMBRA_NOCHE): hay menos luz.
 * Solo se dibujan en la vista principal, no en el minimapa.
 * Se comunica con: Juego (la dibuja al final de la escena), Shader e iluminacion.frag, Cubo, Mapa, Edificio, Parque y
 * Entorno (sombras fijas), Auto y Vehiculo (sombras que siguen posición y orientación).
 */
public class Sombras {

    // ==================== 1. CONSTANTES (valores ajustables) ====================
    public static final float[] COLOR_SOMBRA = {0.02f, 0.03f, 0.05f}; // Casi negro, apenas azulado.
    public static final float ALFA_SOMBRA_DIA = 0.45f; // Opacidad en el centro de la mancha, de día.
    public static final float ALFA_SOMBRA_NOCHE = 0.22f; // De noche, más tenue.
    public static final float ELEVACION_SOMBRA = 0.06f; // Altura sobre la superficie: evita el z-fighting (y queda sobre la pintura vial, a 0.04).
    public static final float GROSOR_SOMBRA = 0.01f; // Alto de la caja aplastada.
    public static final float FORMA_REDONDA = 2; // Exponente de la curva: 2 = elipse.
    public static final float FORMA_CUADRADA = 4; // 4 = rectángulo de esquinas redondeadas.
    public static final float NUCLEO_SOMBRA = 0.35f; // Fracción del centro con sombra pareja; de ahí al borde se difumina.
    public static final float ESCALA_SOMBRA_EDIFICIO = 1.4f; // La mancha mide 1.4 veces la base del edificio: 9.8, apenas menos que la acera (10).
    public static final float[] SOMBRA_AUTO = {2.3f, 3.5f}; // Ancho y largo de la mancha de un auto (el auto mide 1.65 × 2.6).
    public static final float ESCALA_SOMBRA_ARBOL = 1.25f; // Diámetro de la mancha respecto de la copa.
    public static final float[] SOMBRA_BANCO = {2.1f, 1.0f}; // Ancho y fondo de la mancha de un banco (mide 1.6 × 0.5).

    // Sombras de lo que no se mueve, calculadas una vez: cada una es {x, y, z, anchoX, anchoZ, angulo, forma, nucleo}.
    public static final List<float[]> FIJAS = Collections.unmodifiableList(calcularFijas());

    // ==================== 2. SOMBRAS FIJAS ====================

    /** Edificios, árboles y bancos de los parques, y árboles del campo. */
    private static List<float[]> calcularFijas() {
        List<float[]> lista = new ArrayList<>(); // Resultado.
        float ladoEdificio = Mapa.ANCHO_EDIFICIO * ESCALA_SOMBRA_EDIFICIO; // Todos los edificios tienen base de 7 × 7.
        float nucleoEdificio = 1 / ESCALA_SOMBRA_EDIFICIO; // La pared está a 1/1.4 ≈ 0.71 del borde de la mancha: ahí empieza el degradado.
        for (int fila = 0; fila < Mapa.MAPA.length; fila++) { // Recorre el mapa.
            for (int columna = 0; columna < Mapa.MAPA[fila].length; columna++) {
                if (Mapa.tipo(fila, columna) == Mapa.EDIFICIO) { // Manzana con edificio.
                    float y = Edificio.ALTURA_ACERA + ELEVACION_SOMBRA; // Sobre la acera, donde se apoya el edificio.
                    lista.add(new float[] {Mapa.centro(columna), y, Mapa.centro(fila), ladoEdificio, ladoEdificio, 0, FORMA_CUADRADA, nucleoEdificio});
                }
            }
        }
        for (int[] celda : Mapa.parques()) { // Árboles y bancos de cada parque, apoyados en el césped.
            float y = Parque.TOPE_CESPED + ELEVACION_SOMBRA; // Sobre el césped.
            float cx = Mapa.centro(celda[1]); // Centro del parque en X.
            float cz = Mapa.centro(celda[0]); // Centro del parque en Z.
            for (float[] a : Parque.arboles(celda[0], celda[1])) { // Mismos árboles que dibuja Parque.
                // Una mancha que pasara el borde del césped quedaría flotando sobre la acera, que está más baja:
                // se achica para que no salga del césped.
                float lejania = Math.max(Math.abs(a[0] - cx), Math.abs(a[1] - cz)); // Distancia al centro, en el eje más largo.
                float diametro = Math.min(a[4] * ESCALA_SOMBRA_ARBOL, 2 * (Parque.MITAD_CESPED - lejania)); // No sale del césped.
                lista.add(new float[] {a[0], y, a[1], diametro, diametro, 0, FORMA_REDONDA, NUCLEO_SOMBRA});
            }
            for (float[] b : Parque.bancos(celda[0], celda[1])) { // Bancos: siguen su orientación.
                lista.add(new float[] {b[0], y, b[1], SOMBRA_BANCO[0], SOMBRA_BANCO[1], b[2], FORMA_REDONDA, NUCLEO_SOMBRA});
            }
        }
        for (float[] a : Entorno.ARBOLES) { // Árboles del campo, sobre el pasto.
            float diametro = a[4] * ESCALA_SOMBRA_ARBOL; // Proporcional a la copa.
            lista.add(new float[] {a[0], Entorno.ALTURA_CAMPO + ELEVACION_SOMBRA, a[1], diametro, diametro, 0, FORMA_REDONDA, NUCLEO_SOMBRA});
        }
        return lista; // Sombras fijas.
    }

    // ==================== 3. DIBUJO ====================

    private final Shader shader; // Recibe el modo sombra.
    private final Cubo cubo; // Geometría de cada mancha.

    /** Recibe el shader y el cubo compartidos. */
    public Sombras(Shader shader, Cubo cubo) {
        this.shader = shader; // Guarda el programa.
        this.cubo = cubo; // Guarda el cubo.
    }

    /** Dibuja todas las sombras; va después de todo lo opaco de la vista principal. */
    public void dibujar(Auto auto, List<Vehiculo> vehiculos, boolean noche) {
        glEnable(GL_BLEND); // Activa la mezcla: la sombra se combina con el suelo.
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA); // final = sombra · alfa + suelo · (1 - alfa).
        glDepthMask(false); // Prueba la profundidad, pero no la escribe (ver comentario de la clase).
        try { // Asegura que el estado opaco vuelva aunque algo falle.
            shader.entero("uSombra", 1); // iluminacion.frag calcula el degradado circular.
            shader.decimal("uAlfa", noche ? ALFA_SOMBRA_NOCHE : ALFA_SOMBRA_DIA); // Opacidad en el centro.
            for (float[] s : FIJAS) { // Edificios, árboles y bancos.
                mancha(s[0], s[1], s[2], s[3], s[4], s[5], s[6], s[7]); // Una mancha por objeto.
            }
            float y = ELEVACION_SOMBRA; // Los autos están sobre el asfalto (Y = 0).
            mancha(auto.getX(), y, auto.getZ(), SOMBRA_AUTO[0], SOMBRA_AUTO[1], auto.getAngulo(), FORMA_REDONDA, NUCLEO_SOMBRA); // Jugador.
            for (Vehiculo v : vehiculos) { // Tráfico.
                mancha(v.getX(), y, v.getZ(), SOMBRA_AUTO[0], SOMBRA_AUTO[1], v.getAngulo(), FORMA_REDONDA, NUCLEO_SOMBRA); // Sigue posición y giro.
            }
        } finally {
            shader.entero("uSombra", 0); // Vuelve al material normal.
            shader.decimal("uAlfa", 1); // Opacidad completa.
            glDepthMask(true); // Vuelve a escribir profundidad.
            glDisable(GL_BLEND); // La escena 3D es opaca: sin mezcla.
        }
    }

    /** Una mancha: caja aplastada de anchoX × anchoZ, girada como el objeto; el shader le da el degradado. */
    private void mancha(float x, float y, float z, float anchoX, float anchoZ, float angulo, float forma, float nucleo) {
        shader.decimal("uFormaSombra", forma); // Elipse o rectángulo redondeado.
        shader.decimal("uNucleoSombra", nucleo); // Dónde empieza a difuminarse.
        cubo.cajaGirada(x, y, z, anchoX, GROSOR_SOMBRA, anchoZ, COLOR_SOMBRA[0], COLOR_SOMBRA[1], COLOR_SOMBRA[2], angulo);
    }
}
