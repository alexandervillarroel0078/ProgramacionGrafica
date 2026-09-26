package com.graphics.ciudad.motor; // Agrupa las piezas técnicas: ventana, shaders, geometría, cámara y texto.

/**
 * FIGURAS: las tres mallas redondeadas que usa la ciudad, además del Cubo: esfera, cilindro y cono.
 * Responsable de: generarlas con la resolución elegida, subirlas a la GPU (crear) y liberarlas (eliminar).
 * Se comunica con: Malla (generadores y dibujo), Juego (llama a crear() y eliminar(), igual que con Cubo) y
 * Parque (árboles, fuente y senderos las usan).
 */
public class Figuras {

    // ==================== RESOLUCIÓN (valores ajustables) ====================
    public static final int SECTORES_ESFERA = 12; // Meridianos de la esfera: más = más redonda, más triángulos.
    public static final int ANILLOS_ESFERA = 8; // Paralelos de la esfera, de polo a polo.
    public static final int LADOS_CILINDRO = 10; // Lados del polígono que forma el cilindro.
    public static final int LADOS_CONO = 10; // Lados del polígono de la base del cono.

    public final Malla esfera; // Esfera de diámetro 1.
    public final Malla cilindro; // Cilindro de diámetro 1 y alto 1.
    public final Malla cono; // Cono de base con diámetro 1 y alto 1.

    /** Genera los vértices de las tres figuras; la GPU todavía no se usa. */
    public Figuras(Shader shader) {
        esfera = new Malla(shader, Malla.esfera(SECTORES_ESFERA, ANILLOS_ESFERA)); // 504 vértices.
        cilindro = new Malla(shader, Malla.cilindro(LADOS_CILINDRO)); // 120 vértices.
        cono = new Malla(shader, Malla.cono(LADOS_CONO)); // 60 vértices.
    }

    /** Sube las tres figuras a la GPU; requiere el contexto OpenGL ya creado. */
    public void crear() {
        esfera.crear(); // VAO/VBO de la esfera.
        cilindro.crear(); // VAO/VBO del cilindro.
        cono.crear(); // VAO/VBO del cono.
    }

    /** Libera las tres figuras. */
    public void eliminar() {
        esfera.eliminar(); // Libera la esfera.
        cilindro.eliminar(); // Libera el cilindro.
        cono.eliminar(); // Libera el cono.
    }
}
