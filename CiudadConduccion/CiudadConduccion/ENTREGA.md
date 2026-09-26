# Ciudad Interactiva — documento de entrega

Simulador de conducción en una ciudad 3D generada por código. Está escrito en Java 17 con LWJGL 3.3.3 (GLFW + OpenGL 3.3 Core) y shaders GLSL. La ciudad y el auto se arman con un solo cubo (VAO/VBO) que se escala, rota y colorea en cada dibujo. El proyecto no usa imágenes, texturas ni modelos 3D externos.

## 1. Cómo ejecutar

Hace falta JDK 17 o superior, Maven y una placa de video compatible con OpenGL 3.3. Los comandos se ejecutan en la carpeta que contiene `pom.xml`:

```sh
mvn compile exec:exec
```

La primera ejecución descarga las dependencias. La clase principal es `com.graphics.ciudad.Main`, definida en `pom.xml`. ESC cierra la ventana.

Pruebas automáticas (no abren ventana):

```sh
mvn test
```

## 2. Diseño de la ciudad

La ciudad está definida por la matriz `MAPA` de [Mapa.java](src/main/java/com/graphics/ciudad/mundo/Mapa.java). Tiene 11 × 11 celdas de 10 unidades (`TAM_CELDA`), así que mide 110 × 110 unidades (`TAMANO`). Cada borde está a 55 unidades del origen (`LIMITE`). Las filas avanzan en Z (de norte a sur) y las columnas en X (de oeste a este). Y es la altura.

- **Valores de `MAPA`:** `0` = calle, `1` = edificio, `2` = parque.
- **Calles:** las filas y columnas pares son calles continuas (6 avenidas horizontales y 6 verticales), por eso toda la red está conectada. `MapaTest` lo comprueba con un recorrido BFS.
- **Manzanas:** las celdas con fila y columna impares son 25 manzanas: **19 edificios y 6 parques**.
- **Edificios:** altura entre 5 y 13 (`Mapa.alturaEdificio`). El color varía según la fila y la columna.

Plano con leyenda (norte arriba; la primera fila es Z = -50 y la última Z = +50):

```text
            X:  -50 -40 -30 -20 -10   0  10  20  30  40  50
 fila 0  Z=-50   ·   *   ·   ·   ·   *   ·   ·   ·   *   1
 fila 1  Z=-40   ·   E   ·   E   ·   P   ·   E   ·   E   ·
 fila 2  Z=-30   ·   ·   ·   *   ·   ·   ·   *   ·   ·   ·
 fila 3  Z=-20   ·   P   ·   E   ·   E   ·   E   ·   P   ·
 fila 4  Z=-10   2   *   ·   ·   ·   *   ·   ·   ·   *   ·
 fila 5  Z=  0   ·   E   ·   E   ·   P   ·   E   ·   E   ·
 fila 6  Z= 10   ·   ·   ·   *   ·   ·   ·   *   ·   ·   ·
 fila 7  Z= 20   ·   E   ·   P   ·   E   ·   E   ·   E   ·
 fila 8  Z= 30   ·   *   ·   ·   ·   *   ·   ·   ·   *   ·
 fila 9  Z= 40   ·   E   ·   E   ·   E   ·   P   ·   E   ·
 fila 10 Z= 50   A   ·   ·   ·   ·   ·   ·   ·   3   ·   ·
```

| Símbolo | Significado |
|---|---|
| `·` | Calle (0) |
| `E` | Manzana con edificio (1): acera, edificio con cubierta, ventanas, paso peatonal y semáforo |
| `P` | Parque (2): acera, césped, cuatro árboles y un banco, paso peatonal y semáforo |
| `*` | Celda de calle con una farola: 13 en total (`Iluminacion.LUCES`), 4 unidades al sur del eje de la calle |
| `A` | Salida del auto (-50, 50), mirando al norte (`Auto.X_INICIAL`, `Auto.Z_INICIAL`) |
| `1` `2` `3` | Entregas en orden (`Entregas.DESTINOS`): esquina noreste (50, -50), borde oeste (-50, -10) y borde sur-este (30, 50) |

`MapaTest` verifica que todos los destinos, todas las farolas y la salida caen sobre celdas de calle.

## 3. Controles

| Tecla | Acción |
|---|---|
| W / ↑ | Acelerar |
| S / ↓ | Frenar y, detenido, retroceder |
| A / ← y D / → | Girar (solo con el auto en movimiento; en reversa el giro se invierte) |
| Espacio | Freno fuerte |
| C | Alternar cámara de seguimiento / vista aérea de toda la ciudad |
| N | Alternar día / noche |
| F | Encender / apagar los faros del auto |
| M | Mostrar / ocultar el minimapa |
| R | Reiniciar: auto en la salida, entregas en 0/3 y cronómetro en 0 |
| ESC | Salir |

**Objetivo:** llegar a la marca dorada del destino activo y frenar sobre ella, a menos de 3 unidades y con velocidad menor que 1 unidad/s. Al completar las 3 entregas, el título muestra el tiempo total.

**Título de la ventana:** `Ciudad Interactiva | Vel: X km/h | Luces: ON/OFF | Día/Noche | Entregas: X/3 | Destino: …`

## 4. Funciones implementadas por clase

Paquete `com.graphics.ciudad`:

| Clase | Funciones |
|---|---|
| `Main` | Punto de entrada: crea `Juego` y llama a `ejecutar()`. |
| `Juego` | Ciclo principal entrada → `actualizar(dt)` → dibujar, con dt limitado a 50 ms. Reparte las teclas, compone el título, lleva el reloj global de la animación, fija el orden de dibujo (ciudad, auto, farolas, decoración, destino, minimapa) y libera los recursos. |
| `motor/Ventana` | Ventana GLFW y contexto OpenGL 3.3 Core, callback de teclado, `pulsada()`, tamaño real del framebuffer, título, presentación y cierre. |
| `motor/Shader` | Carga GLSL desde `resources/shaders`, normaliza `#version` para Windows, compila, enlaza y envía uniforms (`vector`, `decimal`, `entero`) con caché de ubicaciones. |
| `motor/Cubo` | Cubo de 36 vértices con normales en un VAO/VBO; `caja()` y `cajaGirada()` lo dibujan con posición, escala, giro y color. |
| `motor/Camara` | Cámara de seguimiento (12 unidades detrás del auto) y vista aérea orbital, cuyo radio y altura son proporcionales a `Mapa.LIMITE`. |
| `mundo/Mapa` | `MAPA` 11 × 11, `TAM_CELDA`, `TAMANO`, `LIMITE`, `centro()`, `indiceCelda()`, `esCalle()`, `esCalleEn()`, `alturaEdificio()`. |
| `mundo/Ciudad` | Base de asfalto, líneas amarillas entre cruces, aceras, edificios con cubierta y césped de los parques. |
| `mundo/Decoracion` | Árboles y banco en los parques, ventanas (encendidas de noche), pasos peatonales y un semáforo por manzana. |
| `mundo/Semaforo` | Ciclo rojo (5 s) → verde (5 s) → amarillo (2 s) en bucle, con duraciones en constantes. Solo la luz activa brilla; las otras quedan oscuras. |
| `vehiculo/Auto` | Estado (x, z, ángulo, velocidad), física por dt (aceleración 9, resistencia exponencial, freno, límites -6..16), giro proporcional a la velocidad, `reset()` y dibujo por piezas (carrocería, cabina, ruedas, faros). |
| `vehiculo/Colisiones` | Círculo del auto (radio 1.65) contra el rectángulo de cada manzana y contra los bordes del mapa. |
| `iluminacion/Iluminacion` | Día/noche, faros, 13 farolas (`LUCES`), envío de uniforms de luz y dibujo de postes y bombillas. |
| `juego/Entregas` | `DESTINOS` con nombres, regla de llegada, progreso, cronómetro, `reset()`, texto del título, marca dorada con baliza giratoria y cuadrado dorado en el minimapa. |
| `juego/Minimapa` | Segundo pase de dibujo ortográfico con el norte arriba y la ciudad completa (escala `Mapa.LIMITE`). Usa scissor y viewport y restaura el estado al terminar. Muestra un indicador cian con punta blanca para la posición y la orientación del auto. |

Shaders en `src/main/resources/shaders`:

- `ciudad.vert`: escala, rotación y traslación del cubo; proyección en perspectiva o vista superior para el minimapa.
- `iluminacion.frag`: luz ambiente, sol (Lambert), farolas con atenuación y conos de los faros (`smoothstep`).
- `plano.frag`: shader de color plano de la primera lección, conservado como referencia.

Pruebas en `src/test/java/com/graphics/ciudad`:

- `MapaTest`: límites 110/55, al menos 12 edificios y 4 parques, calles conectadas (BFS) y posiciones sobre calles.
- `ColisionesTest`: calles transitables y obstáculos.
- `AutoTest`: `reset()` del auto.
- `EntregasTest`: 3 entregas, `reset()`, progreso en 0 y primer destino otra vez activo.
- `SemaforoTest`: orden y duraciones del ciclo del semáforo.

## 5. Limitaciones conocidas

- **Iluminación:** es local y sin sombras. La luz de una farola puede atravesar un edificio.
- **Colisiones:** el auto choca solo con manzanas y bordes. Farolas, semáforos y árboles no tienen colisión; las farolas están en la calle, junto a la acera, y el auto puede pasar a través del poste.
- **Choques:** el auto se detiene sin rebote. El círculo de colisión es conservador, por eso el auto frena un poco antes de tocar la acera.
- **Semáforos:** son decorativos (no detienen al auto). No hay tráfico, peatones ni audio.
- **Ruedas:** no giran ni se orientan al doblar.
- **Minimapa:** se reduce en ventanas pequeñas (como máximo 260 px, un tercio del lado menor), siempre en la esquina superior derecha.
- **Título:** puede verse recortado si la ventana es angosta. La velocidad en km/h supone 1 unidad = 1 metro.
- **Entorno:** necesita una sesión gráfica; no funciona en un servidor sin pantalla.

## 6. Recursos externos usados

| Recurso | Uso |
|---|---|
| [LWJGL 3.3.3](https://www.lwjgl.org/): `lwjgl`, `lwjgl-glfw`, `lwjgl-opengl` y sus bibliotecas nativas (Windows, Linux, macOS) | Ventana, teclado y acceso a OpenGL 3.3 desde Java |
| JUnit 3.8.1 | Pruebas automáticas (`mvn test`) |
| Maven, con `maven-compiler-plugin` 3.13.0 y `exec-maven-plugin` 3.1.0 | Compilación, dependencias y ejecución |
| JDK 17 | Lenguaje y máquina virtual |

No se usan texturas, imágenes, modelos 3D, fuentes ni sonidos externos. Toda la geometría y los colores se generan en el código.

## 7. Cambios en vivo: qué tocar

Todos los valores ajustables son constantes con nombre al inicio de su archivo. Después de editar, volvé a ejecutar `mvn compile exec:exec`: los shaders se copian al classpath en `compile` y se cargan al arrancar.

### Ciudad y juego (Java)

| Cambio | Archivo | Constante o dato |
|---|---|---|
| Mover o agregar una farola | `iluminacion/Iluminacion.java` | `LUCES`: `{x, 4.5f, z}` sobre una calle; máximo `MAX_LUCES` = 16 |
| Agregar un parque o un edificio | `mundo/Mapa.java` | `MAPA`: una celda con fila y columna impares a `2` (parque) o `1` (edificio) |
| Velocidad y manejo del auto | `vehiculo/Auto.java` | `VELOCIDAD_MAX` (16), `VELOCIDAD_REVERSA` (6), `ACELERACION` (9), `RESISTENCIA` (0.7), `FRENO` (7), `VELOCIDAD_GIRO` (0.11) |
| Punto de partida | `vehiculo/Auto.java` | `X_INICIAL`, `Z_INICIAL` (sobre una calle) |
| Tiempos del semáforo | `mundo/Semaforo.java` | `DURACION_ROJO` (5), `DURACION_VERDE` (5), `DURACION_AMARILLO` (2), `BRILLO_APAGADA` (0.15) |
| Agregar o mover una entrega | `juego/Entregas.java` | `DESTINOS` `{x, z}` sobre calle y su nombre en `NOMBRES_DESTINOS` |
| Qué tan cerca y lento hay que llegar | `juego/Entregas.java` | `RADIO_LLEGADA` (3), `VELOCIDAD_LLEGADA` (1) |
| Tamaño del minimapa | `juego/Minimapa.java` | `TAMANO_MAX_MINIMAPA` (260 px), `MARGEN_MINIMAPA` (18 px), `BORDE_MINIMAPA` (3 px), `MARGEN_MAPA` (zoom), `ESCALA_INDICADOR` (1.5) |
| Cámara de seguimiento | `motor/Camara.java` | `DISTANCIA_SEGUIMIENTO` (12), `ALTURA_SEGUIMIENTO` (9), `ALTURA_OBJETIVO` (0.8) |
| Cámara aérea | `motor/Camara.java` | `FACTOR_RADIO` (1.86), `FACTOR_ALTURA` (1.57), proporcionales a `Mapa.LIMITE` |

### Iluminación y proyección (shaders GLSL en `src/main/resources/shaders`)

| Cambio | Archivo | Constante (valor) |
|---|---|---|
| Luz ambiente de día y de noche | `iluminacion.frag` | `AMBIENTE_DIA` (0.48), `AMBIENTE_NOCHE` (0.12, 0.16, 0.24) |
| Intensidad y dirección del sol | `iluminacion.frag` | `INTENSIDAD_SOL_DIA` (0.65), `INTENSIDAD_SOL_NOCHE` (0.10), `DIRECCION_SOL` (0.4, 1.0, 0.3) |
| Alcance de las farolas | `iluminacion.frag` | `FAROLA_ATENUACION_LINEAL` (0.12), `FAROLA_ATENUACION_CUADRATICA` (0.045); más chico = más alcance |
| Color y fuerza de las farolas | `iluminacion.frag` | `COLOR_FAROLA` (1.0, 0.73, 0.34), `INTENSIDAD_FAROLA` (3.0) |
| Ángulo del cono de los faros | `iluminacion.frag` | `CONO_BORDE` (0.85 ≈ 32°), `CONO_CENTRO` (0.97 ≈ 14°); son cosenos, así que más cerca de 1 = cono más angosto |
| Alcance y fuerza de los faros | `iluminacion.frag` | `FAROS_ATENUACION_CUADRATICA` (0.04), `INTENSIDAD_FARO` (8.0), `COLOR_FARO` (1.0, 0.94, 0.72) |
| Posición e inclinación de los faros | `iluminacion.frag` | `FAROS_SEPARACION` (0.55), `FAROS_AVANCE` (1.36), `FAROS_INCLINACION` (0.10) |
| Campo visual de la cámara | `ciudad.vert` | `CAMPO_VISUAL` (55°) |
| Distancia de dibujo | `ciudad.vert` | `PLANO_CERCANO` (0.1), `PLANO_LEJANO` (250); este último debe cubrir la ciudad desde la vista aérea |
| Orden de alturas en el minimapa | `ciudad.vert` | `ESCALA_ALTURA_MAPA` (100) |
