# Ciudad Interactiva — documento de entrega

Simulador de conducción en una ciudad 3D generada por código. Está escrito en Java 17 con LWJGL 3.3.3 (GLFW + OpenGL 3.3 Core) y shaders GLSL. La ciudad y el auto se arman con un solo cubo (VAO/VBO) que se escala, rota y colorea en cada dibujo. El proyecto no usa imágenes, texturas ni modelos 3D externos.

## 1. Cómo ejecutar

Hace falta JDK 17 o superior, Maven y una placa de video compatible con OpenGL 3.3. Los comandos se ejecutan en la carpeta que contiene `pom.xml`:

```sh
mvn compile exec:exec
```

La primera ejecución descarga las dependencias. La clase principal es `com.graphics.ciudad.Main`, definida en `pom.xml`. El juego abre en un menú de inicio: **ENTER** empieza la partida y **ESC** cierra la ventana.

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
| `E` | Manzana con edificio (1): acera, edificio con cubierta, ventanas, paso peatonal, semáforo y señales de PARE y dirección |
| `P` | Parque (2): acera, césped, cuatro árboles y un banco, paso peatonal, semáforo y señales |
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
| R | Reiniciar: auto en la salida, entregas en 0/3, cronómetro en 0 y tráfico al inicio de sus rutas |
| ENTER | Empezar la partida desde el menú de inicio |
| P | Pausa / continuar: congela auto, tráfico, entregas y semáforos |
| H | Mostrar / ocultar la ayuda de controles dentro de la ventana |
| ESC | Salir |

**Objetivo:** llegar a la marca dorada del destino activo y frenar sobre ella, a menos de 3 unidades y con velocidad menor que 1 unidad/s. Al completar las 3 entregas, el título muestra el tiempo total.

**Título de la ventana:** `Ciudad Interactiva | Vel: X km/h | Luces: ON/OFF | Día/Noche | Entregas: X/3 | Destino: …`. Además, el HUD dentro de la ventana muestra velocidad, faros, día/noche, entregas, destino y sector.

## 4. Funciones implementadas por clase

Paquete `com.graphics.ciudad`:

| Clase | Funciones |
|---|---|
| `Main` | Punto de entrada: crea `Juego` y llama a `ejecutar()`. |
| `Juego` | Ciclo principal entrada → `actualizar(dt)` → dibujar, con dt limitado a 50 ms y llevado a 0 en menú o pausa. Reparte las teclas, compone el título, lleva el reloj global de la animación, impide que el jugador atraviese el tráfico, fija el orden de dibujo (ciudad, auto, tráfico, farolas, decoración, destino, minimapa, HUD) y libera los recursos. |
| `motor/Ventana` | Ventana GLFW y contexto OpenGL 3.3 Core, callback de teclado, `pulsada()`, tamaño real del framebuffer, título, presentación y cierre. |
| `motor/Shader` | Carga GLSL desde `resources/shaders`, normaliza `#version` para Windows, compila, enlaza y envía uniforms (`vector2`, `vector`, `vector4`, `decimal`, `entero`) con caché de ubicaciones. |
| `motor/Cubo` | Cubo de 36 vértices con normales en un VAO/VBO; `caja()` y `cajaGirada()` lo dibujan con posición, escala, giro y color. |
| `motor/Camara` | Cámara de seguimiento (12 unidades detrás del auto) y vista aérea orbital, cuyo radio y altura son proporcionales a `Mapa.LIMITE`. |
| `mundo/Mapa` | `MAPA` 11 × 11, `TAM_CELDA`, `TAMANO`, `LIMITE`, `centro()`, `indiceCelda()`, `esCalle()`, `esCalleEn()`, `alturaEdificio()`, y los cinco sectores con nombre (`SECTORES`, `NOMBRES_SECTORES`, `sector()`, `nombreSector()`). |
| `mundo/Ciudad` | Base de asfalto, líneas amarillas entre cruces, aceras, edificios con cubierta y césped de los parques. |
| `mundo/Decoracion` | Árboles y banco en los parques, ventanas (encendidas de noche), pasos peatonales, un semáforo y dos señales de tránsito por manzana. |
| `mundo/Senalizacion` | Señal de PARE (placa roja con franja blanca) y señal de dirección (placa azul con flecha hacia el Centro), con poste, sobre la acera de cada manzana. |
| `mundo/Semaforo` | Ciclo rojo (5 s) → verde (5 s) → amarillo (2 s) en bucle, con duraciones en constantes. Solo la luz activa brilla; las otras quedan oscuras. |
| `vehiculo/Auto` | Estado (x, z, ángulo, velocidad), física por dt (aceleración 9, resistencia exponencial, freno, límites -6..16), giro proporcional a la velocidad, `reset()` y dibujo por piezas (carrocería, cabina, ruedas, faros). |
| `vehiculo/Colisiones` | Círculo del auto (radio 1.65) contra el rectángulo de cada manzana y contra los bordes del mapa; círculo contra círculo para el tráfico. |
| `vehiculo/IndicadorJugador` | Flecha cian emisiva que apunta al auto desde la vista aérea; sube y baja y gira con el tiempo. |
| `trafico/Vehiculo` | Auto autónomo: posición, ángulo, velocidad y color; calcula el carril derecho de su ruta (`calcularCarriles`) y lo sigue girando suavemente hacia el próximo waypoint, frena en las curvas y si el jugador está adelante; dibujo por piezas con luces traseras que brillan de noche. |
| `trafico/Trafico` | Cuatro rutas en celdas de calle (`RUTAS_CELDAS`), validadas contra el Mapa al arrancar; crea, actualiza, reinicia y dibuja los vehículos, e indica a `Juego` si un movimiento del jugador lo haría atravesar uno. De noche envía al shader los focos de sus faros (`prepararFaros`). |
| `iluminacion/Iluminacion` | Día/noche, faros, 13 farolas (`LUCES`), envío de uniforms de luz y dibujo de postes y bombillas. |
| `juego/Entregas` | `DESTINOS` con nombres, regla de llegada, progreso, cronómetro, `reset()`, texto del título, marca dorada con baliza giratoria y cuadrado dorado en el minimapa. |
| `juego/Minimapa` | Segundo pase de dibujo ortográfico con el norte arriba y la ciudad completa (escala `Mapa.LIMITE`). Usa scissor y viewport y restaura el estado al terminar. Muestra un indicador cian con punta blanca para la posición y la orientación del auto, el tráfico y las divisiones de los sectores; `aPantalla()` convierte coordenadas del mundo a píxeles para escribir sus nombres. |
| `juego/EstadoPartida` | Estados MENU, JUGANDO y PAUSA; `dtEfectivo()` devuelve 0 fuera de JUGANDO, así todo queda congelado. |
| `interfaz/Dibujo2D` | Pase ortográfico 2D en píxeles con shader y VAO/VBO propios: rectángulos semitransparentes y texto de STBEasyFont. Desactiva la profundidad y activa la mezcla, y lo restaura al terminar. |
| `interfaz/Hud` | Panel de estado (velocidad, faros, día/noche, entregas, destino, sector), ayuda de controles (H), cartel de PAUSA, menú de inicio y nombres de sectores sobre el minimapa. Escala el texto según el tamaño de la ventana. |

Shaders en `src/main/resources/shaders`:

- `ciudad.vert`: escala, rotación y traslación del cubo; proyección en perspectiva o vista superior para el minimapa.
- `iluminacion.frag`: luz ambiente, sol (Lambert), farolas con atenuación y conos de los faros (`smoothstep`). La función `aporteFoco()` calcula un cono y la usan tanto los faros del jugador como los focos del tráfico (`uFarosTrafico`, `uDireccionFarosTrafico`, `uNumFarosTrafico`).
- `plano.frag`: shader de color plano de la primera lección, conservado como referencia.
- `hud.vert` y `hud.frag`: dibujo 2D en píxeles con color RGBA, para el HUD.

Pruebas en `src/test/java/com/graphics/ciudad`:

- `MapaTest`: límites 110/55, al menos 12 edificios y 4 parques, calles conectadas (BFS) y posiciones sobre calles.
- `ColisionesTest`: calles transitables y obstáculos.
- `AutoTest`: `reset()` del auto.
- `EntregasTest`: 3 entregas, `reset()`, progreso en 0 y primer destino otra vez activo.
- `SemaforoTest`: orden y duraciones del ciclo del semáforo.
- `MapaTest.testSectores`: entre 4 y 5 sectores con nombre y ningún punto de la ciudad sin sector.
- `TraficoTest`: 120 s simulados con dt fijo; todos los vehículos siempre en calles, dentro del mapa, sin tocar manzanas, en movimiento y, en los tramos rectos, a `DESPLAZAMIENTO_CARRIL` ± 0.35 a la derecha de la línea central. Además comprueba que dos rutas comparten una calle en sentidos opuestos y la geometría de las esquinas de carril. También frenado ante el jugador, `reset()`, choque con el jugador y rutas inválidas rechazadas.
- `LucesTraficoTest`: con noche activa todos los vehículos tienen las luces encendidas y con día apagadas; la tecla F no las cambia; los faros acompañan posición y orientación durante los giros.
- `JuegoTest`: en menú y en pausa, `actualizar()` no mueve el auto ni el tráfico aunque se mantenga W; jugando, sí.

## 5. Mejoras opcionales implementadas

| Mejora | Qué hace | Dónde |
|---|---|---|
| Flecha sobre el auto | En la vista aérea (tecla C), una flecha cian emisiva a 6 unidades de altura apunta al auto, sube y baja y gira despacio; se ve también de noche. No aparece en la cámara de seguimiento ni en el minimapa. | `vehiculo/IndicadorJugador`, `motor/Camara.esAerea`, `Juego` |
| Luces del tráfico | Los vehículos encienden sus luces solos de noche (tecla N) y las apagan de día; la tecla F solo afecta los faros del jugador. Encendidas, los faros son blancos y las traseras rojas, ambas emisivas; apagadas se ven gris oscuro y rojo oscuro. De noche cada vehículo proyecta dos focos reales sobre la calle, con el mismo cono (`smoothstep`) y atenuación que los del jugador, pero más débiles y cortos. El estado día/noche se lee de `Iluminacion` (no está duplicado). | `trafico/Vehiculo`, `trafico/Trafico.prepararFaros`, `iluminacion.frag` (`aporteFoco`) |
| Tráfico autónomo | Cuatro vehículos (amarillo, azul, verde y blanco) recorren rutas cíclicas con giros, en los sectores noroeste, noreste, sureste (con forma de L) y suroeste. Circulan por el carril derecho de su sentido, a `DESPLAZAMIENTO_CARRIL` (2.5) de la línea amarilla. Las rutas 1 y 4 comparten la calle Z = -10 en sentidos opuestos. En cada esquina pasan al carril del tramo siguiente girando suavemente, sin cruzar la línea central de su calle, y frenan en las curvas. Las rutas se validan contra el Mapa, así que nunca atraviesan edificios ni salen de la ciudad. De noche se ven sus luces traseras. Se detienen si el jugador les cierra el paso, y el jugador no puede atravesarlos (círculo contra círculo). R los reinicia. | `trafico/Vehiculo`, `trafico/Trafico`, `vehiculo/Colisiones`, `Juego` |
| HUD dentro de la ventana | Panel semitransparente arriba a la izquierda con velocidad, faros, día/noche, entregas, destino y sector. Se adapta al tamaño de la ventana. El texto lo genera STBEasyFont (sin texturas) en un pase 2D final que restaura profundidad, mezcla y viewport. | `interfaz/Dibujo2D`, `interfaz/Hud`, `hud.vert`, `hud.frag` |
| Ayuda de controles | H muestra u oculta la lista de teclas, abajo a la izquierda. | `interfaz/Hud` |
| Pausa | P congela auto, tráfico, entregas y semáforos (dt = 0) y muestra "PAUSA". | `juego/EstadoPartida`, `Juego`, `interfaz/Hud` |
| Menú de inicio | Al abrir, la escena queda de fondo oscurecida con el título y "Presiona ENTER para empezar". | `juego/EstadoPartida`, `interfaz/Hud` |
| Sectores con nombre | Centro, Barrio Norte, Parque Sur, Zona Oeste y Zona Este. El HUD muestra el sector actual y el minimapa dibuja sus divisiones y sus nombres. | `mundo/Mapa`, `juego/Minimapa`, `interfaz/Hud` |
| Señalización 3D | En cada manzana, una señal de PARE en la esquina suroeste y una de dirección (flecha hacia el Centro) en la sureste, con poste, sobre la acera: no ocupan la calle ni cambian las colisiones. | `mundo/Senalizacion`, `mundo/Decoracion` |

## 6. Limitaciones conocidas

- **Iluminación:** es local y sin sombras. La luz de una farola puede atravesar un edificio.
- **Colisiones:** el auto choca solo con manzanas y bordes. Farolas, semáforos y árboles no tienen colisión; las farolas están en la calle, junto a la acera, y el auto puede pasar a través del poste.
- **Choques:** el auto se detiene sin rebote. El círculo de colisión es conservador, por eso el auto frena un poco antes de tocar la acera.
- **Semáforos:** son decorativos: no detienen al auto ni al tráfico. No hay peatones ni audio.
- **Luces del tráfico:** sus focos iluminan la calle y los edificios, pero no proyectan sombras (como todas las luces del juego). `MAX_FAROS_TRAFICO` limita el tráfico con luces a 8 vehículos; si hubiera más, los restantes circularían con las bombillas encendidas pero sin foco.
- **Tráfico:** las rutas son fijas. Los vehículos no chocan entre sí: van por carriles separados, pero en una intersección donde dos rutas doblan pueden superponerse un instante. Frenan si el jugador está adelante, pero no esquivan ni respetan semáforos.
- **Texto del HUD:** STBEasyFont solo tiene caracteres ASCII, así que las tildes se quitan ("Día" se ve como "Dia", "Presioná" como "Presiona"). La letra es de trazo fino y pixelado.
- **Señales:** `Cubo` solo gira alrededor del eje Y, así que las placas son rectangulares. La palabra PARE se representa con una franja blanca y la punta de la flecha con cajas escalonadas.
- **Ruedas:** no giran ni se orientan al doblar.
- **Minimapa:** se reduce en ventanas pequeñas (como máximo 260 px, un tercio del lado menor), siempre en la esquina superior derecha.
- **Título:** puede verse recortado si la ventana es angosta. La velocidad en km/h supone 1 unidad = 1 metro.
- **Entorno:** necesita una sesión gráfica; no funciona en un servidor sin pantalla.

## 7. Recursos externos usados

| Recurso | Uso |
|---|---|
| [LWJGL 3.3.3](https://www.lwjgl.org/): `lwjgl`, `lwjgl-glfw`, `lwjgl-opengl` y sus bibliotecas nativas (Windows, Linux, macOS) | Ventana, teclado y acceso a OpenGL 3.3 desde Java |
| LWJGL 3.3.3 `lwjgl-stb` (STBEasyFont) y sus bibliotecas nativas | Texto del HUD: convierte cada letra en cuadriláteros de color, sin texturas ni archivos de fuentes |
| JUnit 3.8.1 | Pruebas automáticas (`mvn test`) |
| Maven, con `maven-compiler-plugin` 3.13.0 y `exec-maven-plugin` 3.1.0 | Compilación, dependencias y ejecución |
| JDK 17 | Lenguaje y máquina virtual |

No se usan texturas, imágenes, modelos 3D, archivos de fuentes ni sonidos externos. Toda la geometría y los colores se generan en el código.

## 8. Cambios en vivo: qué tocar

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
| Rutas del tráfico | `trafico/Trafico.java` | `RUTAS_CELDAS`: listas cíclicas de cruces `{fila, columna}` (pares); cada tramo debe ir en línea recta por calle, o el juego se detiene al arrancar con un mensaje |
| Velocidad y color de los vehículos | `trafico/Trafico.java` | `VELOCIDADES` (7, 6, 8, 6.5), `COLORES` |
| Manejo del tráfico | `trafico/Vehiculo.java` | `VELOCIDAD_GIRO` (2.2 rad/s), `RADIO_WAYPOINT` (2), `FRACCION_MINIMA_CURVA` (0.3), `DISTANCIA_PRECAUCION` (6), `DISTANCIA_ANTICIPACION` (4) |
| Luces del tráfico (shader) | `iluminacion.frag` | `INTENSIDAD_FARO_TRAFICO` (4.0, la mitad que el jugador), `ALCANCE_FARO_TRAFICO` (4.0: a esa distancia el foco rinde la mitad), `MAX_FAROS_TRAFICO` (16) |
| Máximo de focos (Java) | `trafico/Trafico.java` | `MAX_FAROS_TRAFICO` (16): debe ser igual al del shader; `FAROS_POR_VEHICULO` (2) |
| Colores y ubicación de las luces del tráfico | `trafico/Vehiculo.java` | `COLOR_FARO_ENCENDIDO`, `COLOR_FARO_APAGADO`, `COLOR_TRASERA_ENCENDIDA`, `COLOR_TRASERA_APAGADA`, `LADO_LUZ` (0.55), `ALTURA_LUZ` (0.68), `FRENTE_LUZ` (-1.32), `TRASERA_LUZ` (1.32) |
| Carril del tráfico | `trafico/Vehiculo.java` | `DESPLAZAMIENTO_CARRIL` (`TAM_CELDA / 4` = 2.5): distancia del centro del carril derecho a la línea amarilla. Con más de ≈ 3.3 el vehículo tocaría la vereda (`TraficoTest` lo detectaría) |
| Flecha sobre el auto (vista aérea) | `vehiculo/IndicadorJugador.java` | `ALTURA_INDICADOR` (6), `TAMANO_INDICADOR` (2.5), `COLOR_INDICADOR` (cian), `AMPLITUD_OSCILACION` (0.6), `FRECUENCIA_OSCILACION` (3), `VELOCIDAD_ROTACION` (1.2) |
| Sectores | `mundo/Mapa.java` | `NOMBRES_SECTORES`, `SECTORES` (rectángulos `{xMin, xMax, zMin, zMax}`), `RADIO_CENTRO` (25) |
| Tamaño y estilo del HUD | `interfaz/Hud.java` | `ALTO_REFERENCIA` (380: escala 2 con 760 px de alto), `ESCALA_MINIMA` (1), `ESCALA_MAXIMA` (3), `MARGEN` (12), `RELLENO` (8), `ALTO_LINEA` (11), `ALFA_PANEL` (0.55), `ALFA_MENU` (0.7), `AYUDA` |
| Señales de tránsito | `mundo/Senalizacion.java` | `ALTURA_POSTE` (2.6), `LADO_PARE` (0.9), `ANCHO_DIRECCION` (1.3), `ALTO_DIRECCION` (0.55), `DESPLAZAMIENTO_ESQUINA` (4) |
| Divisiones de sectores en el minimapa | `juego/Minimapa.java` | `GROSOR_DIVISION` (0.7), `ALTURA_DIVISION` (22) |

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
