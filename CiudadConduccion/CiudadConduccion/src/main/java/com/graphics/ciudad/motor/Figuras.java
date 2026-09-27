package com.graphics.ciudad.motor; // Agrupa las piezas técnicas: ventana, shaders, geometría, cámara y texto.

/**
 * FIGURAS: las mallas que usa la ciudad además del Cubo: tres redondeadas (esfera, cilindro y cono) y un prisma
 * triangular (el techo a dos aguas de las casas bajas).
 * Responsable de: generarlas con la resolución elegida, subirlas a la GPU (crear) y liberarlas (eliminar).
 * Se comunica con: Malla (generadores y dibujo), Juego (llama a crear() y eliminar(), igual que con Cubo) y
 * Parque (árboles, fuente y senderos las usan) y Edificio (antena, tanque de agua y techo a dos aguas).
 */
public class Figuras {

    // ==================== RESOLUCIÓN (valores ajustables) ====================
    public static final int SECTORES_ESFERA = 12; // Meridianos de la esfera: más = más redonda, más triángulos.
    public static final int ANILLOS_ESFERA = 8; // Paralelos de la esfera, de polo a polo.
    public static final int LADOS_CILINDRO = 10; // Lados del polígono que forma el cilindro.
    public static final int LADOS_CONO = 10; // Lados del polígono de la base del cono.
    // Prisma triangular unitario: un triángulo visto de costado (puntos {z, y}) extruido a lo largo de X, como la cabina.
    // Base de -0.5 a 0.5 y punta en Y = 0.5: escalado, la base es el alero y la punta la cumbrera del techo.
    public static final float[][] PERFIL_PRISMA = {{-0.5f, -0.5f}, {0.5f, -0.5f}, {0, 0.5f}};

    public final Malla esfera; // Esfera de diámetro 1.
    public final Malla cilindro; // Cilindro de diámetro 1 y alto 1.
    public final Malla cono; // Cono de base con diámetro 1 y alto 1.
    public final Malla prisma; // Prisma triangular de 1 × 1 × 1 (techo a dos aguas).

    /** Genera los vértices de las figuras; la GPU todavía no se usa. */
    public Figuras(Shader shader) {
        esfera = new Malla(shader, Malla.esfera(SECTORES_ESFERA, ANILLOS_ESFERA)); // 504 vértices.
        cilindro = new Malla(shader, Malla.cilindro(LADOS_CILINDRO)); // 120 vértices.
        cono = new Malla(shader, Malla.cono(LADOS_CONO)); // 60 vértices.
        prisma = new Malla(shader, Malla.extruir(PERFIL_PRISMA, 1)); // 24 vértices: 2 tapas triangulares y 3 rectángulos.
    }

    /** Sube las figuras a la GPU; requiere el contexto OpenGL ya creado. */
    public void crear() {
        esfera.crear(); // VAO/VBO de la esfera.
        cilindro.crear(); // VAO/VBO del cilindro.
        cono.crear(); // VAO/VBO del cono.
        prisma.crear(); // VAO/VBO del prisma.
    }

    /** Libera las figuras. */
    public void eliminar() {
        esfera.eliminar(); // Libera la esfera.
        cilindro.eliminar(); // Libera el cilindro.
        cono.eliminar(); // Libera el cono.
        prisma.eliminar(); // Libera el prisma.
    }
}
