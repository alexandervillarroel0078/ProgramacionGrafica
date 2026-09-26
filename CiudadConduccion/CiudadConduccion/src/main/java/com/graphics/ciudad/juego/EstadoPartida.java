package com.graphics.ciudad.juego; // Agrupa las reglas de la partida, su estado y el minimapa.

/**
 * ESTADO DE LA PARTIDA: menú de inicio, jugando o en pausa.
 * Responsable de: recordar en qué estado está la partida, cambiarlo con ENTER (empezar) y P (pausa), y calcular
 * el paso de tiempo efectivo: fuera del estado JUGANDO devuelve 0, así el auto, el tráfico, las entregas y los
 * semáforos quedan congelados sin tener que preguntar por la pausa en cada clase.
 * Se comunica con: Juego (le pasa las teclas y usa dtEfectivo()) y Hud (muestra el menú o el cartel de PAUSA).
 */
public class EstadoPartida {

    /** Los tres estados posibles de la partida. */
    public enum Estado {
        MENU, // Pantalla de inicio: la escena se ve de fondo, pero nada se mueve.
        JUGANDO, // Partida en curso: el tiempo avanza.
        PAUSA // Partida detenida con P: nada se mueve hasta volver a presionar P.
    }

    private Estado estado = Estado.MENU; // El juego abre en el menú de inicio.

    /** Sale del menú de inicio; Juego lo llama al presionar ENTER. */
    public void empezar() {
        if (estado == Estado.MENU) { // Solo tiene efecto desde el menú.
            estado = Estado.JUGANDO; // Comienza la partida.
        }
    }

    /** Alterna entre jugar y pausa; Juego lo llama al presionar P. En el menú no hace nada. */
    public void alternarPausa() {
        if (estado == Estado.JUGANDO) { // Durante la partida...
            estado = Estado.PAUSA; // ...la detiene.
        } else if (estado == Estado.PAUSA) { // Si ya estaba en pausa...
            estado = Estado.JUGANDO; // ...la reanuda.
        }
    }

    /** Devuelve el paso de tiempo que deben usar las actualizaciones: deltaTime al jugar y 0 en menú o pausa. */
    public float dtEfectivo(float deltaTime) {
        if (estado == Estado.JUGANDO) { // Solo durante la partida avanza el tiempo del juego.
            return deltaTime; // Tiempo real transcurrido.
        }
        return 0; // Con dt = 0 ninguna fórmula de movimiento cambia posición, ángulo ni cronómetros.
    }

    /** Devuelve el estado actual. */
    public Estado getEstado() {
        return estado; // MENU, JUGANDO o PAUSA.
    }
}
