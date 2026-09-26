package com.graphics.ciudad.mundo; // Agrupa lo que forma la ciudad: mapa, edificios, decoración y señales.

import com.graphics.ciudad.motor.Cubo; // Dibuja postes, placas y símbolos.

/**
 * SENALIZACION: señales de tránsito 3D en las esquinas de las manzanas.
 * Responsable de: dibujar la señal de PARE (placa roja con franja blanca) y la señal de dirección (placa azul con una
 * flecha blanca que apunta hacia el Centro), cada una con su poste, a una escala coherente con el auto (≈ 1.4 de alto).
 * Las señales se ubican sobre la acera, dentro de la celda de la manzana: nunca ocupan la calle y no necesitan
 * colisión propia, porque Colisiones ya impide que el auto entre en la manzana.
 * Se comunica con: Decoracion (la llama para cada manzana) y Cubo (todas las piezas son cajas).
 * Limitación: Cubo solo gira alrededor de Y, así que las placas son rectangulares y la palabra PARE se representa con
 * una franja blanca; la punta de la flecha se arma con cajas escalonadas.
 */
public class Senalizacion {

    // ==================== 1. CONSTANTES (valores ajustables) ====================
    public static final float DESPLAZAMIENTO_ESQUINA = 4; // Distancia del centro de la manzana a la esquina de la acera.
    public static final float ALTURA_POSTE = 2.6f; // Alto del poste: la placa queda por encima del techo del auto.
    public static final float GROSOR_POSTE = 0.1f; // Sección cuadrada del poste.
    public static final float LADO_PARE = 0.9f; // Lado de la placa de PARE.
    public static final float ANCHO_DIRECCION = 1.3f; // Ancho de la placa de dirección.
    public static final float ALTO_DIRECCION = 0.55f; // Alto de la placa de dirección.
    public static final float GROSOR_PLACA = 0.06f; // Espesor de las placas.
    private static final float ALTURA_ACERA = 0.3f; // Las señales se apoyan sobre la acera, que Ciudad dibuja con 0.3 de alto.

    private final Cubo cubo; // Geometría compartida.

    /** Recibe el cubo con el que se arman las señales. */
    public Senalizacion(Cubo cubo) {
        this.cubo = cubo; // Guarda la referencia para usarla en cada cuadro.
    }

    /** Coloca las señales de una manzana con centro (x, z): PARE en la esquina suroeste y dirección en la sureste. */
    public void dibujarEnManzana(float x, float z) {
        float d = DESPLAZAMIENTO_ESQUINA; // Nombre corto para las cuentas de posición.
        dibujarPare(x - d, z + d); // Esquina suroeste: mira a la calle del sur (el semáforo está en la noreste).
        boolean haciaEste = x < 0; // Las manzanas del oeste señalan hacia el este (el Centro) y las del este hacia el oeste.
        dibujarDireccion(x + d, z + d, haciaEste); // Esquina sureste: también mira a la calle del sur.
    }

    /** Señal de PARE: poste gris y placa roja con franja blanca, orientada hacia +Z (la calle del sur). */
    public void dibujarPare(float x, float z) {
        dibujarPoste(x, z); // Poste sobre la acera.
        float centroPlaca = ALTURA_ACERA + ALTURA_POSTE - LADO_PARE / 2; // La placa termina a la altura del extremo del poste.
        float frente = z + GROSOR_POSTE / 2 + GROSOR_PLACA / 2; // La placa va delante del poste, del lado de la calle.
        cubo.caja(x, centroPlaca, frente, LADO_PARE, LADO_PARE, GROSOR_PLACA, 0.80f, 0.06f, 0.06f); // Placa roja.
        cubo.caja(x, centroPlaca, frente + GROSOR_PLACA, LADO_PARE * 0.8f, LADO_PARE * 0.22f, 0.02f, 0.95f, 0.95f, 0.95f); // Franja blanca "PARE".
    }

    /** Señal de dirección: placa azul con una flecha blanca hacia el este (haciaEste) o hacia el oeste. */
    public void dibujarDireccion(float x, float z, boolean haciaEste) {
        dibujarPoste(x, z); // Poste sobre la acera.
        float centroPlaca = ALTURA_ACERA + ALTURA_POSTE - ALTO_DIRECCION / 2; // La placa termina en el extremo del poste.
        float frente = z + GROSOR_POSTE / 2 + GROSOR_PLACA / 2; // Delante del poste, del lado de la calle.
        cubo.caja(x, centroPlaca, frente, ANCHO_DIRECCION, ALTO_DIRECCION, GROSOR_PLACA, 0.10f, 0.25f, 0.70f); // Placa azul.
        float sentido = haciaEste ? 1 : -1; // +1 apunta hacia +X (este); -1 hacia -X (oeste).
        float simbolo = frente + GROSOR_PLACA; // Los símbolos van apenas delante de la placa.
        cubo.caja(x - sentido * 0.12f, centroPlaca, simbolo, 0.6f, 0.08f, 0.02f, 1, 1, 1); // Cuerpo de la flecha.
        float[] alturas = {0.30f, 0.20f, 0.10f}; // Punta escalonada: cada caja es más baja, como un triángulo.
        for (int i = 0; i < alturas.length; i++) { // Dibuja los escalones de la punta.
            float puntaX = x + sentido * (0.2f + i * 0.08f); // Cada escalón avanza hacia el extremo de la flecha.
            cubo.caja(puntaX, centroPlaca, simbolo, 0.08f, alturas[i], 0.02f, 1, 1, 1); // Escalón blanco.
        }
    }

    /** Poste gris oscuro apoyado sobre la acera (que está a 0.3 de altura). */
    private void dibujarPoste(float x, float z) {
        cubo.caja(x, ALTURA_ACERA + ALTURA_POSTE / 2, z, GROSOR_POSTE, ALTURA_POSTE, GROSOR_POSTE, 0.35f, 0.37f, 0.40f); // Poste vertical.
    }
}
