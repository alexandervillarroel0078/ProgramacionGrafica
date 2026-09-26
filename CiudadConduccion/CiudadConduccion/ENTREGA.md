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
 fila 0  Z=-50   ·   ·   ·   ·   ·   ·   ·   ·   ·   ·   1
 fila 1  Z=-40   ·  E<   ·  E^   ·   P   ·   E   ·  E>   ·
 fila 2  Z=-30   ·   ·   ·   ·   ·   ·   ·   ·   ·   ·   ·
 fila 3  Z=-20   ·  P<   ·  E<   ·  E^   ·   E   ·  P<   ·
 fila 4  Z=-10   2   ·   ·   ·   ·   ·   ·   ·   ·   ·   ·
 fila 5  Z=  0   ·  Ev   ·   E   ·   P   ·   E   ·  E>   ·
 fila 6  Z= 10   ·   ·   ·   ·   ·   ·   ·   ·   ·   ·   ·
 fila 7  Z= 20   ·   E   ·   P   ·  Ev   ·   E   ·   E   ·
 fila 8  Z= 30   ·   ·   ·   ·   ·   ·   ·   ·   ·   ·   ·
 fila 9  Z= 40   ·  Ev   ·   E   ·   E   ·  Pv   ·  E>   ·
 fila 10 Z= 50   A   ·   ·   ·   ·   ·   ·   ·   3   ·   ·
```

| Símbolo | Significado |
|---|---|
| `·` | Calle (0) |
| `E` | Manzana con edificio (1): acera, edificio con cubierta y ventanas |
| `P` | Parque (2): acera y césped con senderos en cruz, fuente central, 4 a 6 árboles (pinos y frondosos) y 2 a 4 bancos; pasos peatonales en sus calles vecinas |
| `^` `v` `<` `>` | Farola en la vereda de esa manzana, del lado norte, sur, oeste o este; su brazo lleva la bombilla sobre la calle de ese lado. Son 13 en total (`Iluminacion.LUCES`) |
| `A` | Salida del auto (-50, 50), mirando al norte (`Auto.X_INICIAL`, `Auto.Z_INICIAL`) |
| `1` `2` `3` | Entregas en orden (`Entregas.DESTINOS`): esquina noreste (50, -50), borde oeste (-50, -10) y borde sur-este (30, 50) |

`MapaTest` verifica que todos los destinos y la salida caen sobre celdas de calle.

**Farolas: en la vereda, no en el asfalto.** Cada farola se planta sobre la acera de una manzana (edificio o parque), a 0.4 del cordón del lado que da a la calle. Un brazo horizontal de `BRAZO_FAROLA` = 1.5 lleva la bombilla hacia la calle, así que queda 1.1 por encima de la calzada, a 4.5 de altura. La luz que recibe el shader sale de la bombilla. Nada de la farola se apoya en la calzada ni participa en colisiones.

Las 13 están a mitad de cuadra, lejos de las esquinas donde están los semáforos, los PARE y los pasos peatonales (al menos 3 unidades de cualquier señal), y repartidas por los cinco sectores: Centro 3, Barrio Norte 3, Parque Sur 3, Zona Oeste 2 y Zona Este 2. `FarolasTest` comprueba:
- **Ubicación del poste:** cada farola está en una manzana con calle por el lado indicado, y el poste queda sobre la acera.
- **Ubicación de la bombilla:** queda sobre el borde de la calle, en la punta del brazo.
- **Sin conflictos:** respeta la separación con semáforos, PARE y carteles, y no hay farolas sobre pasos peatonales.
- **Reparto y colisiones:** hay farolas en todos los sectores y las calles siguen transitables.

### Parques

Cada parque (clase `mundo/Parque`) tiene:
- **Senderos:** dos caminos beige en cruz, apenas elevados sobre el césped, que unen los cuatro lados con el centro.
- **Fuente central:** base cilíndrica gris, espejo de agua azul (levemente emisivo de noche), pilar con plato y un chorro.
- **Árboles:** entre 4 y 6, en las esquinas y los bordes; el centro queda libre para la fuente. Hay dos tipos:
  - **Frondoso:** tronco cilíndrico fino y una copa de 3 esferas de distinto tamaño, desplazadas entre sí.
  - **Pino:** tronco y 3 conos apilados que se achican hacia arriba.

  La copa nunca supera un cuarto del ancho del parque (2.5). Con 3 a 4.5 de alto, los árboles son más altos que el auto y más bajos que los edificios.
- **Bancos:** entre 2 y 4 de madera con patas, en las diagonales entre los senderos y mirando hacia la fuente.

Los parques no son copias. Cada uno usa su fila y su columna para "sortear" cuántos árboles tiene, dónde van, la mezcla de pinos y frondosos, las alturas, el tamaño de las copas, los tonos de verde y la cantidad de bancos. Para eso usa `Parque.variacion()`, una función de hash sin azar: da siempre el mismo resultado, así que el parque se ve igual en todos los cuadros y en cada ejecución.

Todo queda dentro de la celda del parque, y los árboles evitan los postes de semáforos, PARE y farolas que hay en su acera. Las colisiones no cambian: la manzana entera ya es un obstáculo. `ParqueTest` verifica cantidades, distancias, tamaño de la copa, orientación de los bancos, que cada parque sea distinto y que las calles sigan transitables.

### Señalización vial

La señalización sigue criterios viales reales, con circulación por la derecha. Todo va sobre la vereda, dentro de la celda de una manzana: nada ocupa la calle ni cambia las colisiones del auto o del tráfico (`SenalizacionTest` lo comprueba).

**Definiciones (en `Mapa`).** Una **intersección** es una celda de calle con calle hacia el norte o el sur **y** hacia el este o el oeste: con este mapa son las 36 celdas de fila y columna pares (`Mapa.intersecciones()`). Un **acceso** es cada calle que llega a una intersección (`Mapa.accesos()`). `Mapa.sectorDeCelda()` indica a qué sector pertenece cada una.

**Semáforos: solo en el Centro.** Las 4 intersecciones del sector Centro, (4,4), (4,6), (6,4) y (6,6) (`INTERSECCIONES_SEMAFORO`), son las únicas del sector y es donde se cruzan las avenidas con más tránsito. En los barrios alcanza con prioridad de paso (PARE). Hay **un cabezal por acceso**: 4 por intersección, **16** en total.
- **A la derecha y mirando al auto:** cada cabezal está en la esquina de vereda a la **derecha** de la calle que llega, porque el conductor mira hacia adelante y a su derecha, y así no lo tapa el tránsito contrario. Está al borde del paso peatonal, donde el auto se detiene, y sus luces miran hacia los autos que se acercan por ese acceso.
- **Cómo se coordinan:** la secuencia es rojo → verde → amarillo con el reloj global del juego. El rojo dura lo mismo que verde + amarillo (7 = 5 + 2), y el grupo este-oeste usa el mismo ciclo desfasado 7 s. Por eso:
  - los accesos opuestos (norte y sur, o este y oeste) muestran siempre el mismo color;
  - mientras norte-sur está en rojo, este-oeste pasa por verde y amarillo, y al revés;
  - nunca hay verde o amarillo en los dos grupos a la vez.

  Solo la luz activa es emisiva; las demás quedan oscuras. `SemaforoTest` recorre dos ciclos completos y lo verifica.

**Pasos peatonales: donde el peatón cruza.** Hay **25** en total:
- **En cada acceso de las intersecciones con semáforo (16):** el semáforo detiene a los autos y le da tiempo al peatón para cruzar.
- **Junto a cada parque (9 más):** en la calle vecina al norte y en la vecina al oeste, pegados al cruce siguiente, porque los parques atraen peatones. Donde coincide con un paso de semáforo no se repite: el parque del Centro ya tiene los suyos.

Cada paso:
- **Posición:** está en la celda de calle vecina a la intersección, pegado al borde del cruce y sin invadirlo.
- **Tamaño:** mide `LARGO_PASO` = 3 en el sentido de circulación y cubre la calle de vereda a vereda: 6 franjas separadas 1.65 cubren 9.15 de los 10 de ancho.
- **Franjas:** son alargadas en el sentido de circulación, a 0.02–0.04 sobre el asfalto para evitar el z-fighting.
- **Orientación:** los hay en calles norte-sur y este-oeste.
- **Línea amarilla:** `Ciudad` no pinta las marcas que quedarían debajo del paso.

`UBICACIONES_PASOS` se genera desde `INTERSECCIONES_SEMAFORO` y la lista de parques.

**PARE: en cruces sin semáforo, nunca junto a uno.** Hay **10** (`UBICACIONES_PARE`):

| Sector | Intersecciones | Autos que detiene |
|---|---|---|
| Barrio Norte | (2,2), (2,4), (2,8) | los que vienen del norte |
| Parque Sur | (8,2), (8,6), (8,8) | los que vienen del sur |
| Zona Oeste | (4,2), (6,2) | los que vienen del oeste |
| Zona Este | (4,8), (6,8) | los que vienen del este |

- **Por qué esos accesos:** frenan a quien llega desde el borde de la ciudad por la calle secundaria.
- **Posición:** también en la esquina de vereda a la derecha del acceso, mirando al auto que llega.
- **Forma:** octógono rojo aproximado con cubos, borde blanco y franja blanca, sobre un poste.
- **Nunca junto a un semáforo:** dos señales que ordenan cosas distintas en el mismo cruce confunden al conductor. `SenalizacionTest` lo verifica.
- **Contramano:** se eliminaron las señales de contramano/dirección.

**Carteles de sector: uno por sector (5).** Están en la entrada principal de cada sector (`CARTELES_SECTOR`), en la vereda derecha de la calle por la que se entra y mirando a los autos que entran. Son verdes con borde blanco, estilo vial, y llevan el mismo nombre que el HUD y el minimapa. El texto se genera con STBEasyFont y cada trazo se dibuja como una caja fina sobre la placa.

### Figuras generadas por código

Además del cubo, la ciudad usa tres mallas creadas con seno y coseno, sin imágenes ni modelos externos (`motor/Malla`). Todas son unitarias, como el cubo: 1 de ancho y 1 de alto, centradas en el origen, así que la escala que se pasa al dibujar es su tamaño real.

| Figura | Cómo se arma | Normal en cada vértice | Vértices |
|---|---|---|---|
| Esfera (12 × 8) | Como un globo terráqueo: 8 franjas de latitud × 12 meridianos, y cada punto es (sen θ cos φ, cos θ, sen θ sen φ) · 0.5 | La dirección del centro al punto: en una esfera es perpendicular a la superficie | 6 · 12 · 7 = 504 |
| Cilindro (10 lados) | Un polígono de 10 lados arriba y otro abajo, unidos por rectángulos, más dos tapas en abanico | En el costado, horizontal hacia afuera (cos φ, 0, sen φ); en las tapas, (0, ±1, 0). Los bordes se repiten con otra normal porque ahí hay una arista | 12 · 10 = 120 |
| Cono (10 lados) | Triángulos de la base a la punta, más la base en abanico | En el costado, (h cos φ, r, h sen φ) normalizada: el producto vectorial entre la recta que sube a la punta y el borde. En la punta se usa el ángulo medio de cada triángulo. En la base, (0, -1, 0) | 6 · 10 = 60 |

Como cada vértice lleva la normal de la superficie curva y no la de su triángulo plano, OpenGL la interpola y la iluminación las muestra redondeadas. `ciudad.vert` corrige las normales con la inversa de la escala, así que siguen siendo correctas al estirar una figura, por ejemplo una copa achatada. `Juego` crea y libera las figuras igual que el cubo. Cada malla enlaza su propio VAO al dibujar, y `Cubo` también, para poder mezclarlas en la misma escena.

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
| `motor/Texto` | Convierte texto en rectángulos con STBEasyFont; lo usan el HUD (2D) y los carteles de sector (3D). |
| `motor/Malla` | Figura genérica en un VAO/VBO con el mismo formato que `Cubo` (posición + normal). Generadores por fórmulas: `esfera(sectores, anillos)` (esfera UV), `cilindro(lados)` con tapas y `cono(lados)` con base, todos unitarios. `dibujar()` / `dibujarGirada()` funcionan como `caja()` / `cajaGirada()`. |
| `motor/Figuras` | Crea la esfera (12 × 8), el cilindro (10 lados) y el cono (10 lados), los sube a la GPU y los libera, igual que `Cubo`. |
| `mundo/Mapa` | `MAPA` 11 × 11, intersecciones (`esInterseccion`, `intersecciones`), accesos (`accesos`), parques (`parques`), `sectorDeCelda`, `TAM_CELDA`, `TAMANO`, `LIMITE`, `centro()`, `indiceCelda()`, `esCalle()`, `esCalleEn()`, `alturaEdificio()`, y los cinco sectores con nombre (`SECTORES`, `NOMBRES_SECTORES`, `sector()`, `nombreSector()`). |
| `mundo/Ciudad` | Base de asfalto, líneas amarillas entre cruces, aceras, edificios con cubierta y césped de los parques. |
| `mundo/Decoracion` | Ventanas (encendidas de noche) y los pasos peatonales de `UBICACIONES_PASOS` (un paso por acceso de cada cruce con semáforo y hasta dos junto a cada parque, en ambas orientaciones); `hayPasoSobre()` permite que `Ciudad` corte la línea amarilla. Delega los parques en `Parque` y la señalización en `Senalizacion`. |
| `mundo/Parque` | Senderos en cruz, fuente central, 4 a 6 árboles (frondosos de esferas y pinos de conos) y 2 a 4 bancos mirando a la fuente. `arboles()` y `bancos()` calculan la disposición de cada parque con `variacion()` (determinística). |
| `mundo/Senalizacion` | Ubica y dibuja la señalización vial: `INTERSECCIONES_SEMAFORO` (las del Centro), un cabezal por acceso (`SEMAFOROS`), `UBICACIONES_PARE` y `CARTELES_SECTOR`. `esquinaDerecha()` calcula la esquina de vereda a la derecha de un acceso y la orientación hacia el auto. |
| `mundo/Semaforo` | Ciclo rojo (7 s) → verde (5 s) → amarillo (2 s) en bucle; `luzParaAcceso()` desfasa el grupo este-oeste 7 s para que sea complementario del norte-sur. Dibuja un cabezal orientado hacia su acceso; solo la luz activa brilla. |
| `vehiculo/Auto` | Estado (x, z, ángulo, velocidad), física por dt (aceleración 9, resistencia exponencial, freno, límites -6..16), giro proporcional a la velocidad, `reset()` y dibujo por piezas (carrocería, cabina, ruedas, faros). |
| `vehiculo/Colisiones` | Círculo del auto (radio 1.65) contra el rectángulo de cada manzana y contra los bordes del mapa; círculo contra círculo para el tráfico. |
| `vehiculo/IndicadorJugador` | Flecha cian emisiva que apunta al auto desde la vista aérea; sube y baja y gira con el tiempo. |
| `trafico/Vehiculo` | Auto autónomo: posición, ángulo, velocidad y color; calcula el carril derecho de su ruta (`calcularCarriles`) y lo sigue girando suavemente hacia el próximo waypoint, frena en las curvas y si el jugador está adelante; dibujo por piezas con luces traseras que brillan de noche. |
| `trafico/Trafico` | Cuatro rutas en celdas de calle (`RUTAS_CELDAS`), validadas contra el Mapa al arrancar; crea, actualiza, reinicia y dibuja los vehículos, e indica a `Juego` si un movimiento del jugador lo haría atravesar uno. De noche envía al shader los focos de sus faros (`prepararFaros`). |
| `iluminacion/Iluminacion` | Día/noche, faros, 13 farolas en la vereda (`LUCES` = manzana + lado; `POSTES` y `BOMBILLAS` se calculan desde ahí), envío de uniforms de luz y dibujo de poste, brazo y bombilla. |
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
- `MallaTest`: esfera, cilindro y cono tienen la cantidad de vértices esperada (504, 120 y 60), normales de largo 1 y dentro del cubo unitario; la esfera tiene normales radiales, el cilindro horizontales en el costado y verticales en las tapas, y el cono tiene la inclinación correcta.
- `ParqueTest`: 4 a 6 árboles por parque, con el centro libre, copa ≤ 1/4 del parque y dentro de la celda; 2 a 4 bancos mirando a la fuente; parques distintos entre sí, con pinos y frondosos; las calles siguen transitables.
- `SemaforoTest`: orden y duraciones del ciclo; en dos ciclos completos, los accesos opuestos siempre muestran el mismo color y norte-sur y este-oeste nunca están a la vez en verde o amarillo.
- `SenalizacionTest`: semáforos solo en las 4 intersecciones del Centro, uno por acceso, a la derecha y mirando al auto; 6 a 10 PARE en cruces sin semáforo, a la derecha y mirando al auto; un cartel por sector dentro de su sector; ningún semáforo, PARE ni cartel sobre la calle, y las calles siguen transitables.
- `PasosPeatonalesTest`: cada paso está sobre calle, en la celda vecina a una intersección y pegado a su borde, con `LARGO_PASO` en el sentido de circulación y de vereda a vereda; hay pasos en ambas orientaciones; cada paso está junto a un cruce con semáforo o a un parque; cada acceso con semáforo y cada parque tienen su paso; ninguna marca amarilla queda debajo.
- `MapaTest.testSectores`: entre 4 y 5 sectores con nombre y ningún punto de la ciudad sin sector.
- `TraficoTest`: 120 s simulados con dt fijo; todos los vehículos siempre en calles, dentro del mapa, sin tocar manzanas, en movimiento y, en los tramos rectos, a `DESPLAZAMIENTO_CARRIL` ± 0.35 a la derecha de la línea central. Además comprueba que dos rutas comparten una calle en sentidos opuestos y la geometría de las esquinas de carril. También frenado ante el jugador, `reset()`, choque con el jugador y rutas inválidas rechazadas.
- `LucesTraficoTest`: con noche activa todos los vehículos tienen las luces encendidas y con día apagadas; la tecla F no las cambia; los faros acompañan posición y orientación durante los giros.
- `JuegoTest`: en menú y en pausa, `actualizar()` no mueve el auto ni el tráfico aunque se mantenga W; jugando, sí.

## 5. Mejoras opcionales implementadas

| Mejora | Qué hace | Dónde |
|---|---|---|
| Figuras redondeadas y parques | Mallas nuevas generadas por fórmulas (esfera, cilindro y cono) con normales suaves, para que la luz del sol, las farolas y los faros las muestren redondeadas. Los parques tienen senderos, fuente, árboles frondosos y pinos, y bancos, con variación determinística entre parques. | `motor/Malla`, `motor/Figuras`, `mundo/Parque` |
| Flecha sobre el auto | En la vista aérea (tecla C), una flecha cian emisiva a 6 unidades de altura apunta al auto, sube y baja y gira despacio; se ve también de noche. No aparece en la cámara de seguimiento ni en el minimapa. | `vehiculo/IndicadorJugador`, `motor/Camara.esAerea`, `Juego` |
| Luces del tráfico | Los vehículos encienden sus luces solos de noche (tecla N) y las apagan de día; la tecla F solo afecta los faros del jugador. Encendidas, los faros son blancos y las traseras rojas, ambas emisivas; apagadas se ven gris oscuro y rojo oscuro. De noche cada vehículo proyecta dos focos reales sobre la calle, con el mismo cono (`smoothstep`) y atenuación que los del jugador, pero más débiles y cortos. El estado día/noche se lee de `Iluminacion` (no está duplicado). | `trafico/Vehiculo`, `trafico/Trafico.prepararFaros`, `iluminacion.frag` (`aporteFoco`) |
| Tráfico autónomo | Cuatro vehículos (amarillo, azul, verde y blanco) recorren rutas cíclicas con giros, en los sectores noroeste, noreste, sureste (con forma de L) y suroeste. Circulan por el carril derecho de su sentido, a `DESPLAZAMIENTO_CARRIL` (2.5) de la línea amarilla. Las rutas 1 y 4 comparten la calle Z = -10 en sentidos opuestos. En cada esquina pasan al carril del tramo siguiente girando suavemente, sin cruzar la línea central de su calle, y frenan en las curvas. Las rutas se validan contra el Mapa, así que nunca atraviesan edificios ni salen de la ciudad. De noche se ven sus luces traseras. Se detienen si el jugador les cierra el paso, y el jugador no puede atravesarlos (círculo contra círculo). R los reinicia. | `trafico/Vehiculo`, `trafico/Trafico`, `vehiculo/Colisiones`, `Juego` |
| HUD dentro de la ventana | Panel semitransparente arriba a la izquierda con velocidad, faros, día/noche, entregas, destino y sector. Se adapta al tamaño de la ventana. El texto lo genera STBEasyFont (sin texturas) en un pase 2D final que restaura profundidad, mezcla y viewport. | `interfaz/Dibujo2D`, `interfaz/Hud`, `hud.vert`, `hud.frag` |
| Ayuda de controles | H muestra u oculta la lista de teclas, abajo a la izquierda. | `interfaz/Hud` |
| Pausa | P congela auto, tráfico, entregas y semáforos (dt = 0) y muestra "PAUSA". | `juego/EstadoPartida`, `Juego`, `interfaz/Hud` |
| Menú de inicio | Al abrir, la escena queda de fondo oscurecida con el título y "Presiona ENTER para empezar". | `juego/EstadoPartida`, `interfaz/Hud` |
| Sectores con nombre | Centro, Barrio Norte, Parque Sur, Zona Oeste y Zona Este. El HUD muestra el sector actual y el minimapa dibuja sus divisiones y sus nombres. | `mundo/Mapa`, `juego/Minimapa`, `interfaz/Hud` |
| Señalización vial | Semáforos coordinados solo en el Centro (un cabezal por acceso, a la derecha y mirando al auto), pasos peatonales en esos accesos y junto a los parques, 10 PARE octogonales en cruces sin semáforo y un cartel verde por sector en su entrada. Ver "Señalización vial" en la sección 2. | `mundo/Senalizacion`, `mundo/Semaforo`, `mundo/Decoracion`, `mundo/Mapa` |

## 6. Limitaciones conocidas

- **Iluminación:** es local y sin sombras. La luz de una farola puede atravesar un edificio.
- **Colisiones:** el auto choca solo con manzanas y bordes. Farolas, semáforos y árboles no tienen colisión, pero todos están sobre la vereda (el brazo de la farola vuela sobre la calzada a 4.6 de altura), dentro de la celda de una manzana: el auto nunca llega hasta ellos, porque la colisión con la manzana lo detiene antes.
- **Choques:** el auto se detiene sin rebote. El círculo de colisión es conservador, por eso el auto frena un poco antes de tocar la acera.
- **Semáforos:** son visuales: ni el tráfico ni el jugador se detienen en rojo (el alcance del enunciado es visual). No hay peatones ni audio.
- **Luces del tráfico:** sus focos iluminan la calle y los edificios, pero no proyectan sombras (como todas las luces del juego). `MAX_FAROS_TRAFICO` limita el tráfico con luces a 8 vehículos; si hubiera más, los restantes circularían con las bombillas encendidas pero sin foco.
- **Tráfico:** las rutas son fijas. Los vehículos no chocan entre sí: van por carriles separados, pero en una intersección donde dos rutas doblan pueden superponerse un instante. Frenan si el jugador está adelante, pero no esquivan ni respetan semáforos.
- **Texto del HUD:** STBEasyFont solo tiene caracteres ASCII, así que las tildes se quitan ("Día" se ve como "Dia", "Presioná" como "Presiona"). La letra es de trazo fino y pixelado.
- **Señales:** `Cubo` solo gira alrededor del eje Y, así que el octógono del PARE se aproxima con tres rectángulos superpuestos y la palabra PARE se representa con una franja blanca. Los carteles de sector sí muestran su nombre (STBEasyFont, sin tildes).
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
| Mover o agregar una farola | `iluminacion/Iluminacion.java` | `LUCES`: cada farola es `{fila, columna, lado}`: una celda de manzana (fila y columna impares) y el lado que da a la calle (`NORTE`, `SUR`, `OESTE`, `ESTE`). El poste y la bombilla se calculan solos; conviene elegir bordes a mitad de cuadra, lejos de semáforos, PARE y pasos (`FarolasTest` lo verifica). Máximo `MAX_LUCES` = 16 |
| Forma de las farolas | `iluminacion/Iluminacion.java` | `BRAZO_FAROLA` (1.5), `MARGEN_POSTE` (0.4, del cordón al poste), `ALTURA_BOMBILLA` (4.5) |
| Agregar un parque o un edificio | `mundo/Mapa.java` | `MAPA`: una celda con fila y columna impares a `2` (parque) o `1` (edificio) |
| Velocidad y manejo del auto | `vehiculo/Auto.java` | `VELOCIDAD_MAX` (16), `VELOCIDAD_REVERSA` (6), `ACELERACION` (9), `RESISTENCIA` (0.7), `FRENO` (7), `VELOCIDAD_GIRO` (0.11) |
| Punto de partida | `vehiculo/Auto.java` | `X_INICIAL`, `Z_INICIAL` (sobre una calle) |
| Tiempos del semáforo | `mundo/Semaforo.java` | `DURACION_VERDE` (5), `DURACION_AMARILLO` (2); `DURACION_ROJO` se calcula como verde + amarillo (7) y `DESFASE_ESTE_OESTE` = rojo, para mantener la coordinación; `BRILLO_APAGADA` (0.15) |
| Dónde hay semáforos | `mundo/Senalizacion.java` | `SECTOR_SEMAFOROS` (0 = Centro): todas las intersecciones de ese sector forman `INTERSECCIONES_SEMAFORO`; `RETROCESO_SEMAFORO` (= `LARGO_PASO`), `MARGEN_VEREDA` (0.6) |
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
| PARE | `mundo/Senalizacion.java` | `UBICACIONES_PARE` (`{fila, columna, dFila, dColumna}`: cruce sin semáforo y acceso), `LADO_PARE` (0.9), `BORDE_PARE` (0.07), `ALTURA_POSTE` (2.6) |
| Carteles de sector | `mundo/Senalizacion.java` | `CARTELES_SECTOR` (`{sector, x, z, ángulo}`, sobre vereda), `ANCHO_CARTEL` (2.6), `ALTO_CARTEL` (0.8), `BORDE_CARTEL` (0.08), `ALTURA_CARTEL` (2.4), `COLOR_CARTEL` |
| Pasos peatonales | `mundo/Decoracion.java` | `LARGO_PASO` (3, en el sentido de circulación), `FRANJAS_PASO` (6), `SEPARACION_FRANJAS` (1.65), `ANCHO_FRANJA` (0.9), `ALTURA_FRANJA` (0.03), `GROSOR_FRANJA` (0.02). `UBICACIONES_PASOS` se genera en `calcularUbicaciones()` desde `INTERSECCIONES_SEMAFORO` y `Mapa.parques()`. Mantener `(FRANJAS_PASO - 1) · SEPARACION_FRANJAS + ANCHO_FRANJA` ≤ 10, o `PasosPeatonalesTest` avisa que la pintura invade la vereda |
| Resolución de las figuras | `motor/Figuras.java` | `SECTORES_ESFERA` (12), `ANILLOS_ESFERA` (8), `LADOS_CILINDRO` (10), `LADOS_CONO` (10): más lados = más redondo y más triángulos |
| Árboles de los parques | `mundo/Parque.java` | `ARBOLES_MIN` / `ARBOLES_MAX` (4 / 6), `COPA_MAXIMA` (1/4 de la celda = 2.5), `RADIO_CENTRO_LIBRE` (2.2), `LUGARES_ARBOL` (esquinas y bordes posibles), `SEPARACION_POSTES` (1.6) |
| Senderos, fuente y bancos | `mundo/Parque.java` | `ANCHO_SENDERO` (1.4), `GROSOR_SENDERO` (0.03), `COLOR_SENDERO`, `RADIO_FUENTE` (1.2), `COLOR_AGUA`, `COLOR_AGUA_NOCHE`, `BANCOS_MIN` / `BANCOS_MAX` (2 / 4), `DISTANCIA_BANCO` (2.3) |
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
