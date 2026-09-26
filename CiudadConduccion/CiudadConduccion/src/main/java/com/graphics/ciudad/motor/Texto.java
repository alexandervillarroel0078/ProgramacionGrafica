package com.graphics.ciudad.motor; // Agrupa las piezas técnicas: ventana, shaders, geometría, cámara y texto.

import java.nio.ByteBuffer; // Memoria nativa donde STBEasyFont escribe los vértices del texto.
import java.text.Normalizer; // Separa las tildes de las letras para convertir el texto a ASCII.
import org.lwjgl.BufferUtils; // Reserva memoria nativa en el orden de bytes de la plataforma.
import static org.lwjgl.stb.STBEasyFont.*; // stb_easy_font_print y stb_easy_font_width.

/**
 * TEXTO: convierte un texto en rectángulos con STBEasyFont, para dibujarlo en 2D (HUD) o en 3D (carteles).
 * STBEasyFont arma cada letra con pequeños cuadriláteros alineados a los ejes, sin texturas ni fuentes externas.
 * Como cada cuadrilátero es un rectángulo recto, también puede dibujarse como una caja fina sobre un cartel 3D.
 * Solo admite ASCII: aAscii() quita las tildes (á → a) antes de generar el texto.
 * Se comunica con: Dibujo2D (HUD) y Senalizacion (nombres de los carteles de sector).
 */
public final class Texto {

    public static final float ALTO_LETRA = 7; // Alto aproximado de una letra de STBEasyFont con escala 1.
    private static final int BYTES_POR_CARACTER = 270; // Espacio que recomienda stb_easy_font por carácter en el peor caso.
    private static final int BYTES_POR_VERTICE = 16; // Formato de STBEasyFont: x, y, z (float) + color (4 bytes).
    private static final int VERTICES_POR_QUAD = 4; // STBEasyFont genera cuadriláteros de cuatro vértices.

    /** Impide crear objetos: solo ofrece funciones estáticas. */
    private Texto() {
    }

    /** Convierte el texto a ASCII: quita tildes (á → a, ñ → n) y reemplaza otros símbolos por '?'. */
    public static String aAscii(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", ""); // Separa y borra las marcas.
        StringBuilder ascii = new StringBuilder(sinTildes.length()); // Resultado en construcción.
        for (char letra : sinTildes.toCharArray()) { // Revisa cada carácter.
            if (letra == '¡' || letra == '¿') { // Los signos de apertura del español no existen en ASCII.
                continue; // Se omiten.
            }
            ascii.append(letra >= 32 && letra < 127 ? letra : '?'); // Conserva lo imprimible; lo demás se marca con '?'.
        }
        return ascii.toString(); // Texto seguro para STBEasyFont.
    }

    /** Ancho del texto en unidades de STBEasyFont (escala 1). */
    public static float ancho(String texto) {
        return stb_easy_font_width(aAscii(texto)); // Ancho que calcula la propia biblioteca.
    }

    /**
     * Devuelve los rectángulos {x0, y0, x1, y1} que forman el texto, en unidades de STBEasyFont (escala 1), con el
     * origen arriba a la izquierda y la Y creciendo hacia abajo.
     */
    public static float[][] rectangulos(String texto) {
        String ascii = aAscii(texto); // STBEasyFont solo conoce los caracteres ASCII 32 a 126.
        ByteBuffer buffer = BufferUtils.createByteBuffer(Math.max(1, ascii.length()) * BYTES_POR_CARACTER); // Espacio seguro.
        int quads = stb_easy_font_print(0, 0, ascii, null, buffer); // Genera los cuadriláteros de las letras.
        float[][] rectangulos = new float[quads][]; // Un rectángulo por cuadrilátero.
        for (int quad = 0; quad < quads; quad++) { // Recorre los cuadriláteros.
            int base = quad * VERTICES_POR_QUAD * BYTES_POR_VERTICE; // Primer byte de este quad.
            float minX = Float.MAX_VALUE; // Borde izquierdo en construcción.
            float minY = Float.MAX_VALUE; // Borde superior en construcción.
            float maxX = -Float.MAX_VALUE; // Borde derecho en construcción.
            float maxY = -Float.MAX_VALUE; // Borde inferior en construcción.
            for (int v = 0; v < VERTICES_POR_QUAD; v++) { // Lee las cuatro esquinas.
                float x = buffer.getFloat(base + v * BYTES_POR_VERTICE); // X de la esquina.
                float y = buffer.getFloat(base + v * BYTES_POR_VERTICE + 4); // Y de la esquina.
                minX = Math.min(minX, x); // Actualiza el borde izquierdo.
                minY = Math.min(minY, y); // Actualiza el borde superior.
                maxX = Math.max(maxX, x); // Actualiza el borde derecho.
                maxY = Math.max(maxY, y); // Actualiza el borde inferior.
            }
            rectangulos[quad] = new float[] {minX, minY, maxX, maxY}; // El quad es un rectángulo alineado a los ejes.
        }
        return rectangulos; // Rectángulos del texto.
    }
}
