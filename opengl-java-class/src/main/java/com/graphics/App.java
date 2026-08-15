package com.graphics;

import org.lwjgl.Version;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class App {

    private long window;

    private int programa;
    private int vao;
    private int vbo;

    public void run() {
        System.out.println("LWJGL " + Version.getVersion() + "!");

        init();
        crearShaders();
        crearTriangulo();
        loop();
        cleanup();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("No se pudo inicializar GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(800, 600, "Programacion Grafica", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("No se pudo crear la ventana GLFW");
        }

        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(win, true);
            }
        });

        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            glfwGetWindowSize(window, pWidth, pHeight);

            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            glfwSetWindowPos(
                window,
                (vidmode.width() - pWidth.get(0)) / 2,
                (vidmode.height() - pHeight.get(0)) / 2
            );
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        GL.createCapabilities();
    }

    // Codigo fuente del vertex shader: por ahora solo recibe la posicion del
    // vertice (atributo 0) y la pasa directo a gl_Position, sin transformaciones
    private static final String VERTEX_SHADER_SRC =
        "#version 330 core\n" +
        "layout (location = 0) in vec3 aPos;\n" +
        "void main() {\n" +
        "    gl_Position = vec4(aPos, 1.0);\n" +
        "}\n";

    // Codigo fuente del fragment shader: pinta todos los fragmentos del
    // triangulo con un color naranja fijo
    private static final String FRAGMENT_SHADER_SRC =
        "#version 330 core\n" +
        "out vec4 FragColor;\n" +
        "void main() {\n" +
        "    FragColor = vec4(1.0, 0.5, 0.2, 1.0);\n" +
        "}\n";

    private void crearShaders() {
        // Compilar por separado el vertex y el fragment shader a partir de su fuente
        int vertexShader = compilarShader(GL_VERTEX_SHADER, VERTEX_SHADER_SRC);
        int fragmentShader = compilarShader(GL_FRAGMENT_SHADER, FRAGMENT_SHADER_SRC);

        // Crear el programa y adjuntarle los dos shaders ya compilados
        programa = glCreateProgram();
        glAttachShader(programa, vertexShader);
        glAttachShader(programa, fragmentShader);
        glLinkProgram(programa);

        // Verificar que el linkeo del programa haya sido exitoso
        if (glGetProgrami(programa, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(programa);
            throw new RuntimeException("Error al linkear el programa de shaders:\n" + log);
        }

        // Una vez linkeado el programa, los shaders individuales ya no hacen falta
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
    }

    // Compila un shader de un tipo dado (vertex o fragment) a partir de su
    // codigo fuente y devuelve su id, o lanza una excepcion con el log si falla
    private int compilarShader(int tipo, String fuente) {
        int shader = glCreateShader(tipo);
        glShaderSource(shader, fuente);
        glCompileShader(shader);

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            throw new RuntimeException("Error al compilar shader:\n" + log);
        }

        return shader;
    }

    private void crearTriangulo() {
        // Posiciones de los 3 vertices del triangulo en coordenadas NDC (-1 a 1)
        float[] vertices = {
             0.0f,  0.5f, 0.0f,
            -0.5f, -0.5f, 0.0f,
             0.5f, -0.5f, 0.0f
        };

        // El VAO guarda la configuracion de los atributos de vertice para
        // no tener que repetirla cada vez que dibujamos
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        // El VBO sube las posiciones de los vertices a la memoria de la GPU
        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        try (MemoryStack stack = stackPush()) {
            FloatBuffer datosVertices = stack.mallocFloat(vertices.length);
            datosVertices.put(vertices).flip();
            glBufferData(GL_ARRAY_BUFFER, datosVertices, GL_STATIC_DRAW);
        }

        // Le decimos a OpenGL como interpretar los datos del VBO: el atributo 0
        // son 3 floats por vertice (x, y, z), sin normalizar
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // Desbindear el VAO para no modificarlo por accidente mas adelante
        glBindVertexArray(0);
    }

    private void loop() {
        glClearColor(0.1f, 0.1f, 0.1f, 0.0f);

        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            drawTriangle();

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void drawTriangle() {
        // Activar nuestro programa de shaders antes de dibujar
        glUseProgram(programa);

        // Bindear el VAO del triangulo (ya trae la configuracion de atributos lista)
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glBindVertexArray(0);
    }

    private void cleanup() {
        // Liberar los recursos de OpenGL que creamos (buffers, arrays y programa)
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        glDeleteProgram(programa);

        // Liberar los callbacks y la ventana de GLFW
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminar GLFW y liberar el callback de errores
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    public static void main(String[] args) {
        new App().run();
    }
}
