package com.graphics.ciudad.motor; // Agrupa las piezas técnicas: ventana, shaders, geometría y cámara.

import java.io.IOException; // Representa un fallo al leer el archivo del shader.
import java.io.InputStream; // Lee los bytes del recurso dentro del classpath.
import java.io.UncheckedIOException; // Propaga el fallo de lectura sin obligar a declararlo.
import java.nio.charset.StandardCharsets; // Interpreta los archivos GLSL como texto UTF-8.
import java.util.HashMap; // Permite guardar las ubicaciones de las variables del shader.
import java.util.Map; // Define el tipo del diccionario de ubicaciones.
import static org.lwjgl.opengl.GL33.*; // Importa las funciones OpenGL hasta la versión 3.3.

/**
 * SHADER: programa GLSL que ejecuta la GPU.
 * Responsable de: cargar el código GLSL desde src/main/resources/shaders (classpath), normalizarlo
 * para los controladores de Windows, compilar los dos shaders, enlazarlos en un programa y enviar uniforms
 * (vec3, float, int) guardando en caché la ubicación de cada variable.
 * Se comunica con: Juego (lo crea, lo activa y lo libera); Cubo, Camara, Iluminacion, Decoracion, Semaforo,
 * Entregas y Minimapa, que le envían uniforms antes de dibujar.
 */
public class Shader {

    private int programa; // Identificador del programa que une ambos shaders.
    private final Map<String, Integer> uniforms = new HashMap<>(); // Evita buscar repetidamente el mismo uniform.

    // ==================== 1. SHADERS: CÓDIGO QUE EJECUTA LA GPU (CARGA DESDE EL CLASSPATH) ====================

    /** Lee un archivo GLSL empaquetado junto a las clases, por ejemplo "/shaders/ciudad.vert". */
    public static String cargarFuente(String ruta) {
        try (InputStream entrada = Shader.class.getResourceAsStream(ruta)) { // Abre el recurso y lo cierra al terminar.
            if (entrada == null) { // getResourceAsStream devuelve null si el archivo no está en el classpath.
                throw new IllegalStateException("No se encontró el shader " + ruta); // Informa qué archivo falta.
            }
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8); // Convierte los bytes en texto GLSL.
        } catch (IOException error) { // Captura errores de lectura del archivo.
            throw new UncheckedIOException(error); // Detiene el arranque con la causa original.
        }
    }

    // ==================== 2. COMPILAR Y ENLAZAR ====================

    /** Une los shaders en un programa ejecutable y libera los objetos intermedios. */
    public void crear(String rutaVertices, String rutaFragmentos) {
        int shaderVertices = compilar(GL_VERTEX_SHADER, cargarFuente(rutaVertices)); // Compila las transformaciones geométricas.
        int shaderFragmentos = 0; // Permite saber si el segundo shader llegó a crearse.
        try { // Garantiza que ambos objetos intermedios se liberen incluso ante un fallo.
            shaderFragmentos = compilar(GL_FRAGMENT_SHADER, cargarFuente(rutaFragmentos)); // Compila el color de la etapa actual.
            programa = glCreateProgram(); // Crea el contenedor que enlazará ambos shaders.
            glAttachShader(programa, shaderVertices); // Adjunta el shader que calcula posiciones.
            glAttachShader(programa, shaderFragmentos); // Adjunta el shader que calcula colores.
            glLinkProgram(programa); // Enlaza las entradas y salidas de ambos shaders.
            if (glGetProgrami(programa, GL_LINK_STATUS) == GL_FALSE) { // Comprueba que el enlace haya sido válido.
                throw new IllegalStateException(glGetProgramInfoLog(programa)); // Muestra incompatibilidades del programa.
            }
        } finally { // Ya no hacen falta objetos shader separados después del enlace.
            glDeleteShader(shaderVertices); // Libera el objeto del shader de vértices.
            if (shaderFragmentos != 0) { // Comprueba que el segundo objeto exista.
                glDeleteShader(shaderFragmentos); // Libera el objeto del shader de fragmentos.
            }
        }
    }

    /** Compila un shader y comunica el mensaje del controlador si hay un error GLSL. */
    private int compilar(int tipo, String fuente) {
        fuente = fuente.replace("\r", "").strip(); // Quita retornos de carro de Windows y espacios en los extremos.
        fuente = fuente.replaceFirst("^(#version\\s+\\d+(\\s+core)?)\\s*", "$1\n"); // Deja #version solo en su línea.
        int shader = glCreateShader(tipo); // Reserva un shader del tipo vértice o fragmento.
        glShaderSource(shader, fuente); // Entrega a OpenGL el texto GLSL.
        glCompileShader(shader); // Solicita la compilación para esta GPU.
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) { // Consulta si la compilación falló.
            String error = glGetShaderInfoLog(shader); // Recupera el detalle del error antes de borrar el objeto.
            glDeleteShader(shader); // Libera el shader que no pudo compilarse.
            throw new IllegalStateException(error); // Detiene la aplicación con el mensaje de diagnóstico.
        }
        return shader; // Devuelve el identificador del shader compilado.
    }

    /** Activa este programa para los próximos dibujos. */
    public void usar() {
        glUseProgram(programa); // Activa los shaders de esta etapa.
    }

    /** Libera el programa si llegó a crearse. */
    public void eliminar() {
        if (programa != 0) { // Comprueba si existe un programa en la GPU.
            glDeleteProgram(programa); // Libera el programa de shaders.
        }
    }

    // ==================== 3. ENVIAR UNIFORMS ====================

    /** Busca un uniform una vez y guarda su ubicación para los siguientes dibujos. */
    public int uniform(String nombre) {
        if (!uniforms.containsKey(nombre)) { // Revisa si esta variable todavía no fue localizada.
            int ubicacion = glGetUniformLocation(programa, nombre); // Pregunta a OpenGL por la dirección del uniform.
            uniforms.put(nombre, ubicacion); // Guarda la respuesta; -1 significa que el shader no usa esa variable.
        }
        return uniforms.get(nombre); // Recupera la ubicación guardada; OpenGL ignora envíos a -1.
    }

    /** Envía dos números reales a una variable vec2 del shader (lo usa el HUD para tamaños y posiciones 2D). */
    public void vector2(String nombre, float x, float y) {
        glUniform2f(uniform(nombre), x, y); // Escribe las dos componentes en el programa activo.
    }

    /** Envía cuatro números reales a una variable vec4 del shader (lo usa el HUD para colores con transparencia). */
    public void vector4(String nombre, float x, float y, float z, float w) {
        glUniform4f(uniform(nombre), x, y, z, w); // Escribe las cuatro componentes en el programa activo.
    }

    /**
     * Matriz identidad 3 × 3 en orden de columnas (el que usa OpenGL): no rota nada.
     * uRotacion debe valer esto para todo lo que no sea una rueda; un uniform sin asignar vale 0 y aplastaría la figura.
     */
    public static final float[] IDENTIDAD_3X3 = {1, 0, 0, 0, 1, 0, 0, 0, 1};

    /** Envía una matriz 3 × 3 (nueve números, columna por columna) a una variable mat3 del shader. */
    public void matriz3(String nombre, float[] columnas) {
        glUniformMatrix3fv(uniform(nombre), false, columnas); // false: los datos ya están por columnas, no hay que trasponer.
    }

    /** Envía tres números reales a una variable vec3 del shader. */
    public void vector(String nombre, float x, float y, float z) {
        glUniform3f(uniform(nombre), x, y, z); // Escribe las tres componentes en el programa activo.
    }

    /** Envía un número real a una variable float del shader. */
    public void decimal(String nombre, float valor) {
        glUniform1f(uniform(nombre), valor); // Actualiza el escalar identificado por su nombre.
    }

    /** Envía un número entero a una variable int del shader. */
    public void entero(String nombre, int valor) {
        glUniform1i(uniform(nombre), valor); // Actualiza un entero, usado también para interruptores 0/1.
    }
}
