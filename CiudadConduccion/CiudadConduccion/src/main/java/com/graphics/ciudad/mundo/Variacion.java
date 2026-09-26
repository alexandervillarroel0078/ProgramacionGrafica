package com.graphics.ciudad.mundo; // Agrupa lo que forma la ciudad: mapa, edificios, decoración y señales.

/**
 * VARIACION: números "aleatorios" determinísticos para dar variedad a la ciudad sin azar por cuadro.
 * valor() mezcla sus datos con seno y se queda con la parte decimal: fracción de sen(combinación) · 43758.5453.
 * Es la misma función de hash que se usa en muchos shaders: sin estado y sin java.util.Random, así que los mismos
 * datos (por ejemplo, edificio + piso + columna de una ventana) dan siempre el mismo número, en cada cuadro y en cada
 * ejecución. Nada parpadea y cada parque, edificio o ventana conserva su aspecto.
 * Se comunica con: Parque (árboles y bancos) y Fachada (ventanas, puertas y toldos).
 */
public final class Variacion {

    /** Impide crear objetos: solo ofrece funciones estáticas. */
    private Variacion() {
    }

    /** Número entre 0 (incluido) y 1 (excluido) que depende solo de fila, columna, índice y semilla. */
    public static float valor(int fila, int columna, int indice, int semilla) {
        double n = Math.sin(fila * 12.9898 + columna * 78.233 + indice * 37.719 + semilla * 4.581) * 43758.5453; // Mezcla los datos.
        return (float) (n - Math.floor(n)); // Se queda con la parte decimal: entre 0 y 1.
    }
}
