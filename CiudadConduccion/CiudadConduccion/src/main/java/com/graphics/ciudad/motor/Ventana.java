package com.graphics.ciudad.motor; // Agrupa las piezas técnicas: ventana, shaders, geometría y cámara.

import java.util.function.IntConsumer; // Recibe el método que atenderá cada tecla presionada.
import org.lwjgl.glfw.Callbacks; // Permite liberar los callbacks de la ventana al cerrar.
import org.lwjgl.opengl.GL; // Carga las funciones OpenGL disponibles en el equipo.
import static org.lwjgl.glfw.GLFW.*; // Importa las funciones y constantes de ventanas y teclado.
import static org.lwjgl.opengl.GL33.*; // Importa las funciones OpenGL hasta la versión 3.3.

/**
 * VENTANA: envoltorio de GLFW para la ventana, el contexto OpenGL y el teclado.
 * Responsable de: crear la ventana y el contexto OpenGL 3.3 Core, registrar el callback de teclado,
 * informar el tamaño real del framebuffer (resize), cambiar el título, presentar cada imagen y destruirse.
 * Se comunica con: Juego, que la crea, le entrega el método tecla() y la consulta en cada vuelta del ciclo;
 * Auto, que lee las teclas mantenidas mediante pulsada().
 */
public class Ventana {

    private long ventana; // Identificador que GLFW asigna a la ventana.
    private int ancho = 1100; // Ancho inicial de la ventana; después contiene píxeles del framebuffer.
    private int alto = 760; // Alto inicial de la ventana; después contiene píxeles del framebuffer.
    private final int[] anchoReal = new int[1]; // Reserva espacio para que GLFW escriba el ancho en píxeles.
    private final int[] altoReal = new int[1]; // Reserva espacio para que GLFW escriba el alto en píxeles.

    /** Configura GLFW y OpenGL, igual que las clases de cámara del proyecto original. */
    public void crear(String titulo, IntConsumer alPresionar) {
        if (!glfwInit()) { // Intenta iniciar la biblioteca que administra ventanas y entrada.
            throw new IllegalStateException("No se pudo iniciar GLFW"); // Detiene el arranque si GLFW falla.
        }
        glfwDefaultWindowHints(); // Restablece las opciones de ventana antes de personalizarlas.
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3); // Solicita la versión mayor de OpenGL.
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3); // Solicita OpenGL 3.3.
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE); // Usa el perfil moderno basado en shaders.
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE); // Solicita compatibilidad con contextos modernos de macOS.
        ventana = glfwCreateWindow(ancho, alto, titulo, 0, 0); // Crea una ventana normal.
        if (ventana == 0) { // GLFW devuelve cero cuando no consigue crear la ventana.
            throw new IllegalStateException("No se pudo crear la ventana OpenGL"); // Informa el fallo de creación.
        }
        glfwMakeContextCurrent(ventana); // Selecciona la ventana que recibirá las llamadas OpenGL.
        glfwSwapInterval(1); // Sincroniza la presentación con la pantalla para reducir cortes de imagen.
        GL.createCapabilities(); // Carga las funciones OpenGL del contexto actual.
        glEnable(GL_DEPTH_TEST); // Hace que las superficies cercanas oculten las lejanas.
        glfwSetKeyCallback(ventana, (ventanaEvento, key, scancode, action, mods) -> { // Registra la función que recibe eventos de teclado.
            if (action == GLFW_PRESS) { // Responde una sola vez al presionar, no a las repeticiones de la tecla.
                alPresionar.accept(key); // Entrega la tecla al método tecla() de Juego, que la reparte entre los módulos.
            }
        }); // Termina el registro del callback de teclado.
    }

    /** Indica si el usuario o el programa solicitaron cerrar la ventana. */
    public boolean debeCerrarse() {
        return glfwWindowShouldClose(ventana); // Devuelve true cuando se pidió terminar el ciclo principal.
    }

    /** Solicita terminar el ciclo principal por el mismo camino que un cierre normal. */
    public void cerrar() {
        glfwSetWindowShouldClose(ventana, true); // Solicita terminar el ciclo principal.
    }

    /** Procesa la entrada pendiente: aquí GLFW llama al callback de teclado. */
    public void procesarEventos() {
        glfwPollEvents(); // Procesa teclado, redimensionamiento y botón de cierre.
    }

    /** Consulta el tamaño real en píxeles; responde al redimensionamiento de la ventana. */
    public void actualizarTamano() {
        glfwGetFramebufferSize(ventana, anchoReal, altoReal); // Obtiene los píxeles reales, también en pantallas Retina.
        ancho = anchoReal[0]; // Copia el ancho actual al estado de la aplicación.
        alto = altoReal[0]; // Copia el alto actual al estado de la aplicación.
    }

    /** Muestra la imagen terminada. */
    public void presentar() {
        glfwSwapBuffers(ventana); // Muestra la imagen terminada intercambiando los buffers.
    }

    /** Consulta si una tecla sigue presionada durante el cuadro actual. */
    public boolean pulsada(int key) {
        return glfwGetKey(ventana, key) == GLFW_PRESS; // Devuelve true mientras el usuario mantiene la tecla.
    }

    /** Publica un texto en la barra superior de la ventana. */
    public void titulo(String titulo) {
        if (ventana == 0) { // Sin ventana creada (por ejemplo, en las pruebas) no hay título que cambiar.
            return; // Evita llamar a GLFW sin inicializar.
        }
        glfwSetWindowTitle(ventana, titulo); // Publica el texto en la barra superior de la ventana.
    }

    /** Devuelve el ancho actual del framebuffer en píxeles. */
    public int ancho() {
        return ancho; // Valor copiado en actualizarTamano().
    }

    /** Devuelve el alto actual del framebuffer en píxeles. */
    public int alto() {
        return alto; // Valor copiado en actualizarTamano().
    }

    /** Libera la ventana si llegó a crearse y finaliza GLFW. */
    public void destruir() {
        if (ventana != 0) { // Evita destruir una ventana que no pudo crearse.
            Callbacks.glfwFreeCallbacks(ventana); // Libera el callback de teclado registrado.
            glfwDestroyWindow(ventana); // Destruye la ventana y su contexto OpenGL.
        }
        glfwTerminate(); // Finaliza GLFW y sus recursos globales.
    }
}
