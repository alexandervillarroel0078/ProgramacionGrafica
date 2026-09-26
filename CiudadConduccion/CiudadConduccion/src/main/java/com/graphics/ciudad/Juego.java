package com.graphics.ciudad; // Paquete raíz de la versión organizada por composición.

import com.graphics.ciudad.iluminacion.Iluminacion; // Sol, farolas y faros.
import com.graphics.ciudad.juego.Entregas; // Reglas y progreso de la partida.
import com.graphics.ciudad.juego.Minimapa; // Segundo pase de dibujo desde arriba.
import com.graphics.ciudad.motor.Camara; // Cámara de seguimiento y aérea.
import com.graphics.ciudad.motor.Cubo; // Geometría compartida (VAO/VBO).
import com.graphics.ciudad.motor.Shader; // Programa GLSL y envío de uniforms.
import com.graphics.ciudad.motor.Ventana; // Ventana GLFW, teclado y presentación.
import com.graphics.ciudad.mundo.Ciudad; // Asfalto, calles, edificios y parques.
import com.graphics.ciudad.mundo.Decoracion; // Árboles, bancos, ventanas, pasos peatonales y semáforos.
import com.graphics.ciudad.mundo.Mapa; // Aporta el límite de la ciudad a la cámara aérea.
import com.graphics.ciudad.vehiculo.Auto; // Vehículo del jugador.
import org.lwjgl.glfw.GLFWErrorCallback; // Muestra los errores de GLFW en la consola.
import static org.lwjgl.glfw.GLFW.*; // Importa las funciones y constantes de ventanas y teclado.
import static org.lwjgl.opengl.GL33.*; // Importa las funciones OpenGL hasta la versión 3.3.

/**
 * JUEGO: coordina todos los módulos; reemplaza la antigua cadena de herencia por etapas (ciudad → auto → iluminación → juego final).
 * Sigue el flujo de los ejemplos: iniciar, loop, dibujar y limpiar. En cada cuadro: entrada → actualizar(dt) → dibujar.
 * Coordenadas: X = izquierda/derecha; Y = altura; Z = profundidad.
 * Responsable de: crear los módulos, repartir las teclas, actualizar Auto y Entregas en orden, componer el título,
 * decidir el orden de dibujo de la escena y liberar los recursos al salir.
 * Se comunica con: Ventana, Shader, Cubo y Camara (motor); Ciudad y Decoracion (mundo); Auto (vehiculo);
 * Iluminacion; Entregas y Minimapa (juego). Main lo crea y llama a ejecutar().
 * Antes cada etapa ampliaba a la anterior con extends y super; ahora Juego llama a cada módulo en el mismo orden.
 * Los comentarios explican las instrucciones; las llaves solo delimitan bloques.
 */
public class Juego {

    // ==================== 1. VARIABLES DE LA APLICACIÓN: MÓDULOS Y CONSTANTES ====================
    private static final String SHADER_VERTICES = "/shaders/ciudad.vert"; // Transformaciones de vértices, en src/main/resources.
    private static final String SHADER_FRAGMENTOS = "/shaders/iluminacion.frag"; // Iluminación; "/shaders/plano.frag" da colores planos.
    private static final double DT_MAXIMO = 0.05; // Paso de tiempo máximo por cuadro (50 ms): evita que el auto atraviese manzanas tras una pausa.
    private static final String NOMBRE_JUEGO = "Ciudad Interactiva"; // Nombre que encabeza el título de la ventana.

    private final Ventana ventana = new Ventana(); // Ventana GLFW con su contexto OpenGL.
    private final Shader shader = new Shader(); // Programa GLSL compartido por todos los dibujos.
    private final Cubo cubo = new Cubo(shader); // Geometría única con la que se construye todo.
    private final Camara camara = new Camara(Mapa.LIMITE); // Cámara de seguimiento o vista aérea ajustada al tamaño del mapa.
    private final Ciudad ciudad = new Ciudad(cubo); // Ciudad generada a partir del Mapa.
    private final Decoracion decoracion = new Decoracion(shader, cubo); // Detalles urbanos de la ciudad terminada.
    private final Auto auto = new Auto(); // Vehículo del jugador; no necesita OpenGL para existir.
    private final Iluminacion iluminacion = new Iluminacion(shader, cubo); // Día/noche, farolas y faros.
    private final Entregas entregas = new Entregas(shader, cubo); // Destinos, progreso y cronómetro.
    private final Minimapa minimapa = new Minimapa(shader, cubo); // Vista superior en un recuadro.
    private float relojGlobal = 0; // Segundos desde el arranque; no se detiene al ganar ni se reinicia con R. Anima los semáforos.

    // ==================== 2. INICIO, CICLO Y LIMPIEZA ====================

    /** Organiza las tres fases de la aplicación y garantiza la limpieza si ocurre un error. */
    public void ejecutar() {
        GLFWErrorCallback errores = GLFWErrorCallback.createPrint(System.err); // Prepara mensajes de error en consola.
        errores.set(); // Instala el manejador de errores de GLFW.
        try { // Protege el arranque y el ciclo para poder liberar recursos si fallan.
            iniciar(); // Crea la ventana, los shaders y la geometría.
            loop(); // Actualiza y dibuja hasta que se cierre la ventana.
        } finally { // Este bloque se ejecuta tanto al salir normalmente como al fallar.
            limpiar(); // Libera la GPU, los callbacks y la ventana.
            glfwSetErrorCallback(null); // Desconecta el manejador antes de liberar su memoria.
            errores.free(); // Libera el callback de errores.
        }
    }

    /** Crea la ventana, compila los shaders y sube el cubo a la GPU. */
    private void iniciar() {
        ventana.crear(NOMBRE_JUEGO, this::tecla); // Crea la ventana y entrega cada tecla presionada a tecla().
        shader.crear(SHADER_VERTICES, SHADER_FRAGMENTOS); // Compila y enlaza los shaders que transforman y colorean los vértices.
        cubo.crear(); // Guarda en la GPU el cubo que servirá para todos los objetos.
    }

    /** Mantiene el ciclo continuo de eventos, actualización, dibujo y presentación. */
    private void loop() {
        double tiempoAnterior = glfwGetTime(); // Guarda el instante inicial para calcular el tiempo entre cuadros.
        int cuadros = 0; // Cuenta los cuadros dibujados para la comprobación automática opcional.
        int maxCuadros = Integer.getInteger("demo.frames", 0); // Cero permite jugar; otro valor limita el arranque de prueba.

        while (!ventana.debeCerrarse()) { // Repite mientras no se haya solicitado cerrar.
            ventana.procesarEventos(); // Entrada: procesa teclado, redimensionamiento y botón de cierre.
            double tiempoActual = glfwGetTime(); // Consulta el tiempo actual en segundos.
            float deltaTime = (float) Math.min(tiempoActual - tiempoAnterior, DT_MAXIMO); // Evita saltos de más de 50 ms.
            tiempoAnterior = tiempoActual; // Conserva el instante de este cuadro para la próxima vuelta.
            actualizar(deltaTime); // Actualiza conducción y entregas.
            ventana.actualizarTamano(); // Obtiene los píxeles reales del framebuffer tras un posible redimensionamiento.
            if (ventana.ancho() > 0 && ventana.alto() > 0) { // Evita dibujar y dividir por cero cuando la ventana está minimizada.
                dibujarFrame(); // Genera la imagen de la ciudad para este cuadro.
            }
            ventana.presentar(); // Muestra la imagen terminada intercambiando los buffers.
            cuadros++; // Registra que se completó una vuelta del ciclo.
            if (maxCuadros > 0 && cuadros >= maxCuadros) { // Detecta el final de un arranque automático limitado.
                ventana.cerrar(); // Solicita salir por el mismo camino que un cierre normal.
            }
        }
    }

    /** Libera únicamente los recursos que llegaron a crearse. */
    private void limpiar() {
        shader.eliminar(); // Libera el programa de shaders si existe.
        cubo.eliminar(); // Libera el VBO y el VAO del cubo si existen.
        ventana.destruir(); // Libera callbacks, ventana y GLFW.
    }

    // ==================== 3. TECLADO Y REINICIO (CONTROLES) ====================

    /** Atiende acciones que deben ocurrir una sola vez por pulsación y las reparte entre los módulos. */
    private void tecla(int key) {
        // Antes cada clase sustituía tecla() y conservaba las acciones anteriores mediante super; aquí están todas juntas.
        if (key == GLFW_KEY_ESCAPE) { // Comprueba si se presionó ESC.
            ventana.cerrar(); // Solicita terminar el ciclo principal.
        }

        if (key == GLFW_KEY_C) { // Comprueba si se presionó la tecla de cámara.
            camara.alternar(); // Invierte el modo de cámara actual.
        }

        if (key == GLFW_KEY_R) { // Comprueba si el usuario quiere comenzar de nuevo.
            auto.reset(); // Restaura posición, velocidad y orientación del vehículo.
            entregas.reset(); // Amplía el reinicio del auto: vuelve a la primera parada y al cronómetro en cero.
        }

        iluminacion.tecla(key); // Amplía los controles con N y F; conserva ESC, C y R.
        minimapa.tecla(key); // Amplía las teclas con M; conserva salida, cámara, reinicio y luces.
    }

    // ==================== 4. ACTUALIZACIÓN POR CUADRO ====================

    /** Actualiza el vehículo y comprueba si llegó y frenó en el destino activo; deltaTime son segundos entre cuadros. */
    private void actualizar(float deltaTime) {
        relojGlobal += deltaTime; // Avanza el reloj de la animación urbana en todos los cuadros.
        auto.actualizar(deltaTime, ventana::pulsada); // Procesa aceleración, giro y colisiones; reemplaza la cámara orbital de la primera lección.
        actualizarTitulo(); // Muestra los controles y la velocidad actual.
        entregas.actualizar(deltaTime, auto); // Añade el objetivo del juego al movimiento del auto.
    }

    /** Compone el texto de la ventana sin mezclarlo con las fórmulas de conducción. */
    private void actualizarTitulo() {
        int kilometrosPorHora = Math.round(Math.abs(auto.getVelocidad()) * 3.6f); // Convierte m/s a km/h y redondea.
        // Formato: "Ciudad Interactiva | Vel: X km/h | Luces: ON/OFF | Día/Noche | Entregas: X/3 | Destino: ...".
        // Los controles (WASD/flechas, Espacio, C, R, N, F, M, ESC) ya no ocupan el título: están en ENTREGA.md.
        String titulo = NOMBRE_JUEGO; // Obtiene el nombre del juego que encabeza el título.
        titulo += " | Vel: " + kilometrosPorHora + " km/h"; // Añade la magnitud de la velocidad.
        titulo += iluminacion.estado(); // Añade los indicadores de faros y día/noche de Iluminacion.
        titulo += " | " + entregas.estado(); // Añade el progreso y el destino activo de Entregas.
        ventana.titulo(titulo); // Publica el texto en la barra superior de la ventana.
    }

    // ==================== 5. DIBUJO: ESCENA FINAL Y DESTINO ====================

    /** Prepara OpenGL, dibuja la vista principal y después el minimapa. */
    private void dibujarFrame() {
        int ancho = ventana.ancho(); // Ancho actual del framebuffer en píxeles.
        int alto = ventana.alto(); // Alto actual del framebuffer en píxeles.
        glViewport(0, 0, ancho, alto); // Utiliza toda el área de la ventana para la escena principal.
        glClearColor(0.12f, 0.20f, 0.30f, 1); // Define un fondo azul oscuro completamente opaco.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Borra la imagen y las distancias del cuadro anterior.
        shader.usar(); // Activa los shaders de esta etapa.
        cubo.enlazar(); // Selecciona los atributos del cubo compartido.
        shader.entero("uMapa", 0); // Selecciona perspectiva normal, no la proyección del minimapa.
        camara.configurar(shader, auto.getX(), auto.getZ(), auto.getAngulo(), ancho, alto); // Actualiza la posición y el objetivo de la cámara.
        iluminacion.preparar(auto); // Envía la iluminación: día/noche, faros y posiciones de las farolas.
        escena(); // Dibuja la ciudad y todos los módulos sobre ella.
        if (glGetError() != GL_NO_ERROR) { // Comprueba si OpenGL reportó una operación inválida.
            throw new IllegalStateException("Error OpenGL al dibujar"); // Hace visible el problema en consola.
        }
        minimapa.dibujar(ancho, alto, auto, this::escena); // Amplía el cuadro: dibuja otra vez la escena en el minimapa.
    }

    /** Dibuja la escena completa; Minimapa la reutiliza para su segundo pase. */
    private void escena() {
        ciudad.dibujar(); // Dibuja asfalto, calles, edificios y parques.
        auto.dibujar(cubo); // Añade el modelo del vehículo sobre la ciudad.
        iluminacion.dibujarFarolas(); // Añade geometría a la ciudad y al auto: postes y bombillas.
        if (!minimapa.enVistaMapa()) { // Los detalles pequeños solo son necesarios en la vista principal.
            decoracion.dibujar(iluminacion.esNoche(), relojGlobal); // Añade árboles, bancos, ventanas y señalización urbana.
        }
        if (entregas.quedanEntregas()) { // Dibuja un objetivo únicamente mientras queden entregas.
            entregas.dibujarDestino(minimapa.enVistaMapa()); // Coloca la marca dorada en la parada activa.
        }
    }
}
