# Ciudad y conducción con OpenGL

Proyecto de enseñanza basado en `AppCamara.java` y `AppLaberinto.java` de tus clases: Java 17, Maven, LWJGL 3.3.3, GLFW, OpenGL 3.3 Core, shaders GLSL, VAO y VBO. Toda la geometría se genera con código; no requiere imágenes ni modelos externos. El documento de entrega (diseño de la ciudad, controles, limitaciones y constantes ajustables) está en [ENTREGA.md](ENTREGA.md).

## Ejecutar

Abre una terminal **en esta carpeta**, donde está `pom.xml`. Necesitas un JDK 17 o superior y Maven (`java -version`, `mvn -version`). La primera compilación descarga las dependencias.

```sh
mvn compile exec:exec
```

Abre `com.graphics.ciudad.Main`; ESC cierra la ventana. En macOS, el perfil Maven agrega `-XstartOnFirstThread`. Si ejecutas desde un IDE en Mac, agrega ese argumento a las opciones de la JVM. Usa `exec:exec`, porque `exec:java` no inicia el proceso de esa forma.

Incluye bibliotecas nativas para macOS Intel/Apple Silicon, Windows x64 y Linux x64/ARM64. Se necesita una sesión gráfica y un controlador compatible con OpenGL 3.3. No está diseñado para ejecutarse en un servidor sin pantalla.

## Organización

```text
com.graphics.ciudad
├── Main, Juego            ← punto de entrada; ciclo entrada → actualizar(dt) → dibujar
├── motor/                 ← Ventana, Shader, Cubo, Camara
├── mundo/                 ← Mapa, Ciudad, Decoracion, Semaforo
├── vehiculo/              ← Auto, Colisiones
├── iluminacion/           ← Iluminacion
└── juego/                 ← Entregas, Minimapa
src/main/resources/shaders ← ciudad.vert, iluminacion.frag, plano.frag
```

El proyecto nació como cuatro etapas encadenadas por herencia (ciudad → auto → iluminación → juego final), en las que cada etapa ampliaba a la anterior con `extends` y `super`. Ahora cada responsabilidad vive en su propia clase y `Juego` las llama en el mismo orden. Los comentarios conservan esa progresión didáctica.

## Cómo leer el código comentado

Cada archivo está dividido en secciones con un título `// ==== ... ====` y empieza con un comentario de cabecera que explica qué hace la clase, de qué es responsable y con qué otras clases se comunica. Cada instrucción tiene un comentario en español; cada método indica su propósito. Los shaders GLSL también están explicados línea por línea, y sus valores ajustables son constantes `const` al inicio de cada archivo.

Se usa una instrucción por línea, condiciones con llaves y cálculos intermedios con nombres descriptivos. El cubo contiene sus 36 vértices explícitos, como en los ejemplos originales. En `Cubo.caja()` y `Auto.pieza()`, los argumentos mantienen este orden: posición X/Y/Z, tamaño X/Y/Z y color rojo/verde/azul; `Cubo.cajaGirada()` agrega el ángulo al final.

## Guion de enseñanza

### Lección 1: crear primero la ciudad

**Resultado:** una ciudad 3D con calles conectadas, marcas viales, edificios, aceras y parques.

1. Leer `Mapa.MAPA`: 0 es calle, 1 edificio y 2 parque. Las filas corresponden a Z y las columnas a X; Y es altura.
2. Explicar `Mapa.centro()`: convierte índices de matriz a coordenadas del mundo. Cada celda mide 10 unidades y el mapa de 11 × 11 ocupa 110 × 110.
3. Revisar `Cubo.crear()`: posiciones y normales en el VBO, atributos en el VAO y 36 vértices.
4. Seguir `Cubo.caja()` y `Cubo.cajaGirada()` desde `Ciudad.dibujar()`: un cubo unitario se transforma en asfalto, edificio o línea.
5. Leer `ciudad.vert`: modelo, cámara y perspectiva. `uMapa` selecciona el segundo tipo de proyección, que usa el minimapa.
6. Recorrer `Juego.ejecutar()`, `iniciar()`, `loop()`, `dibujarFrame()` y `limpiar()`: eventos, actualización, dibujo, presentación y limpieza.

**Ejercicio:** cambiar una manzana de edificio a parque y variar las alturas. Mantener calles conectadas (`MapaTest` lo comprueba). Para ver la ciudad sin luces, pasar `/shaders/plano.frag` en `Juego.SHADER_FRAGMENTOS`.

### Lección 2: crear y mover el auto

**Resultado:** auto rojo con cabina, ruedas y faros, cámara de seguimiento y colisiones con manzanas y borde.

1. Leer `Auto.dibujar()` y `Auto.pieza()`: las piezas usan coordenadas locales que giran y se trasladan juntas.
2. Estudiar `Auto.actualizar(deltaTime)`: aceleración, resistencia, freno y límites de velocidad (constantes al inicio de `Auto`).
3. Relacionar seno/coseno con la dirección del auto. Su frente local es -Z; el giro está en radianes.
4. Explicar el giro proporcional a la velocidad y el cambio de dirección al retroceder.
5. Revisar `Colisiones.puedeCircular()`: círculo contra rectángulo, punto más cercano y distancia al cuadrado.
6. Alternar cámaras con C (`Camara`) para observar la conducción desde arriba.

**Ejercicio:** modificar `ACELERACION` y `RESISTENCIA` y comparar el manejo. El círculo de colisión es conservador para contener todas las piezas. El choque detiene al auto; no hay rebotes. El paso temporal se limita (`Juego.DT_MAXIMO`) para evitar atravesar una manzana tras una pausa larga.

### Lección 3: focos e iluminación

**Resultado:** ambiente nocturno, trece farolas que iluminan superficies, faros del vehículo y cambio día/noche.

1. Comparar el shader de color plano `plano.frag` con `iluminacion.frag`.
2. Estudiar normales y Lambert: `max(dot(normal, direccionLuz), 0)`.
3. Revisar ambiente y luz direccional del sol.
4. Relacionar las coordenadas de `Iluminacion.LUCES` con los postes de `Iluminacion.dibujarFarolas()`.
5. Analizar cómo disminuye la luz con la distancia (atenuación).
6. Explicar el cono de los faros con producto escalar y `smoothstep`.
7. Comparar la emisión de la bombilla con la luz calculada sobre el suelo. Son fenómenos distintos.

**Ejercicio:** variar el color y la atenuación de las farolas (`COLOR_FAROLA` y `FAROLA_ATENUACION_*` en `iluminacion.frag`). La iluminación es local, sin sombras ni oclusión: una luz puede atravesar un edificio. Implementar shadow maps queda como ampliación. El cielo conserva el mismo fondo para concentrar la comparación día/noche en las superficies.

### Lección 4: ciudad final y minimapa

**Resultado:** parques con árboles y bancos, ventanas iluminadas, pasos peatonales, semáforos animados, minimapa y juego de tres entregas.

1. Estudiar `Decoracion.dibujar()` y sus métodos `dibujarParque()`, `dibujarVentanas()` y `dibujarPasoPeatonal()`, y `Semaforo.dibujar()`: composición de objetos reutilizando cajas y la matriz.
2. Leer `Entregas.DESTINOS` y `Entregas.actualizar()`: acercarse a menos de 3 unidades y frenar a menos de 1 unidad/segundo completa una entrega.
3. Seguir `Juego.dibujarFrame()` y `Minimapa.dibujar()`: primero la cámara principal, después una vista ortográfica en otro viewport.
4. Explicar `glScissor`: permite borrar solo el recuadro del minimapa y su profundidad.
5. Mostrar por qué se restauran viewport, scissor y `uMapa` al terminar.
6. Seguir el indicador cian del auto y su punta blanca; la marca dorada indica el destino activo.

**Ejercicio:** agregar una cuarta entrega sobre una calle o modificar el tamaño del minimapa (`TAMANO_MAX_MINIMAPA`). El minimapa mantiene el norte (-Z) arriba. El destino se muestra también como baliza dorada en el mundo. Al completar las entregas aparece GANASTE en el título; R reinicia.

## Controles

| Tecla | Acción |
|---|---|
| W / S o arriba / abajo | Acelerar / frenar y retroceder |
| A / D o izquierda / derecha | Girar mientras el auto se mueve |
| Espacio | Freno |
| C | Cámara de seguimiento / aérea oblicua |
| R | Reiniciar el auto y las entregas |
| N | Día / noche |
| F | Encender / apagar faros |
| M | Mostrar / ocultar minimapa |
| ESC | Salir |

El título de la ventana muestra velocidad en km/h (se supone una unidad = un metro), faros, día/noche, entregas y destino. Puede truncarse si la ventana es pequeña. La velocidad máxima real es algo menor que el límite por la resistencia aplicada. Los semáforos son decorativos: cambian de color pero no bloquean al vehículo. No hay tráfico, peatones, audio, sombras, modelos importados ni ruedas animadas; este es el alcance del ejemplo didáctico finalizado.

## Verificación

```sh
mvn test
```

Las pruebas de lógica (`MapaTest`, `ColisionesTest`, `AutoTest`, `EntregasTest` y `SemaforoTest`) comprueban el mapa, rutas transitables, manzanas, márgenes de colisión, semáforos, entregas y reinicio sin abrir ventanas. Para un arranque gráfico breve puede pasarse `-Ddemo.frames=6` a la **JVM del juego**; al llegar a ese número de cuadros la ventana se cierra. Esta prueba necesita pantalla y comprueba también compilación/enlace de shaders y errores OpenGL.

Práctica manual: conducir y chocar con una acera; retroceder; cambiar cámara; alternar N/F; redimensionar la ventana; alternar M; completar las tres entregas y reiniciar con R.

Con Java 25 y LWJGL 3.3.3 aparecen advertencias de acceso nativo, Unsafe y versión JNI; para impartir las clases se recomienda usar JDK 17, la versión objetivo de los ejemplos originales. El arranque breve no sustituye completar manualmente el recorrido.
