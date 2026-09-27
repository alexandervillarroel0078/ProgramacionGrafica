# Ciudad Interactiva — documento de entrega

Simulador de conducción en una ciudad 3D generada por código. Está escrito en Java 17 con LWJGL 3.3.3 (GLFW + OpenGL 3.3 Core) y shaders GLSL. La ciudad y el auto se arman con un cubo, tres mallas redondeadas (esfera, cilindro y cono) y figuras extruidas desde un perfil (la cabina de los autos), cada una en su VAO/VBO, que se escalan, rotan y colorean en cada dibujo. El proyecto no usa imágenes, texturas ni modelos 3D externos.

## 1. Cómo ejecutar

Hace falta JDK 17 o superior, Maven y una placa de video compatible con OpenGL 3.3. Los comandos se ejecutan en la carpeta que contiene `pom.xml`:

```sh
mvn compile exec:exec
```

La primera ejecución descarga las dependencias. La clase principal es `com.graphics.ciudad.Main`, definida en `pom.xml`. El juego abre en un menú de inicio: **ENTER** empieza la partida y **ESC** cierra la ventana. Arranca en un estado pensado para la demostración: **de día** (`Iluminacion.NOCHE_AL_INICIAR`), con la **cámara de seguimiento**, el **minimapa visible** y el auto en la salida (esquina suroeste, mirando al norte).

Pruebas automáticas (no abren ventana):

```sh
mvn test
```

## 2. Diseño de la ciudad

La ciudad está definida por la matriz `MAPA` de [Mapa.java](src/main/java/com/graphics/ciudad/mundo/Mapa.java). Tiene 11 × 11 celdas de 10 unidades (`TAM_CELDA`), así que mide 110 × 110 unidades (`TAMANO`). Cada borde está a 55 unidades del origen (`LIMITE`). Las filas avanzan en Z (de norte a sur) y las columnas en X (de oeste a este). Y es la altura.

- **Valores de `MAPA`:** `0` = calle, `1` = edificio, `2` = parque.
- **Calles:** las filas y columnas pares son calles continuas (6 avenidas horizontales y 6 verticales), por eso toda la red está conectada. `MapaTest` lo comprueba con un recorrido BFS.
- **Manzanas:** las celdas con fila y columna impares son 25 manzanas: **19 edificios y 6 parques**.
- **Edificios:** hay cinco tipos (torre, bloque, escalonado, casa baja y doble), con alturas de 3.2 a 22 y una paleta urbana de paredes y techos. Cada celda sortea su tipo y sus colores con un hash, así que la ciudad sale siempre igual (ver "Edificios: cinco tipos").

Plano con leyenda (norte arriba; la primera fila es Z = -50 y la última Z = +50):

```text
            X:  -50 -40 -30 -20 -10   0  10  20  30  40  50
 fila 0  Z=-50   ·   ·   ·   ·   ·   ·   ·   ·   ·   ·   1
 fila 1  Z=-40   ·  E<   ·   E   ·  P>   ·   E   ·  E^   ·
 fila 2  Z=-30   ·   ·   ·   ·   ·   ·   ·   ·   ·   ·   ·
 fila 3  Z=-20   ·  Pv   ·  E^   ·   E   ·  E>   ·  P>   ·
 fila 4  Z=-10   2   ·   ·   ·   ·   ·   ·   ·   ·   ·   ·
 fila 5  Z=  0   ·  Ev   ·   E   ·   P   ·   E   ·  Ev   ·
 fila 6  Z= 10   ·   ·   ·   ·   ·   ·   ·   ·   ·   ·   ·
 fila 7  Z= 20   ·   E   ·   P   ·   E   ·  Ev   ·   E   ·
 fila 8  Z= 30   ·   ·   ·   ·   ·   ·   ·   ·   ·   ·   ·
 fila 9  Z= 40   A  E<   ·  E>   ·   E   ·   P   ·  E>   ·
 fila 10 Z= 50   ·   ·   ·   ·   ·   ·   ·   ·   3   ·   ·
```

| Símbolo | Significado |
|---|---|
| `·` | Calle (0) |
| `E` | Manzana con edificio (1): acera y edificio de uno de los cinco tipos; planta baja comercial (vidriera, puerta centrada, vidriera y dos toldos) en cada cara a la calle y ventanas en los pisos superiores |
| `P` | Parque (2): acera y césped con senderos en cruz, fuente central, 4 a 6 árboles (pinos y frondosos) y 2 a 4 bancos; pasos peatonales en sus calles vecinas |
| `^` `v` `<` `>` | Farola en la vereda de esa manzana, del lado norte, sur, oeste o este; su brazo lleva la bombilla sobre la calle de ese lado. Son 13 en total (`Iluminacion.LUCES`), ubicadas por reglas: ver "Farolas" más abajo |
| `A` | Salida del auto (-47.5, 40), mirando al norte (`Auto.X_INICIAL`, `Auto.Z_INICIAL`): calle del borde oeste, penúltima fila (`FILA_SALIDA`), a mitad de la última cuadra (ni en el cruce ni sobre un paso peatonal), en el carril derecho a `CARRIL_SALIDA` = 2.5 de la línea amarilla (X = -50), igual que el tráfico. Se calcula con `Mapa.centro()`. R lo devuelve ahí |
| `1` `2` `3` | Entregas en orden (`Entregas.CELDAS_DESTINOS`, convertidas a `DESTINOS` con `Mapa.centro()`): esquina noreste (celda (0, 10) = (50, -50)), borde oeste ((4, 0) = (-50, -10)) y borde sur-este ((10, 8) = (30, 50)) |

`MapaTest` verifica que todos los destinos y la salida caen sobre celdas de calle.

**Farolas: en la vereda, no en el asfalto.** Cada farola se planta sobre la acera de una manzana (edificio o parque), a 0.4 del cordón del lado que da a la calle. Un brazo curvo lleva la bombilla `BRAZO_FAROLA` = 1.5 hacia la calle, así que queda 1.1 por encima de la calzada, a 4.5 de altura. La luz que recibe el shader sale de la bombilla.

**Luz de la farola: foco hacia abajo, no luz puntual.** Una luz puntual es omnidireccional: ilumina igual hacia todos lados, así que también encendía las paredes por encima de la pantalla, que en la realidad la tapa. Por eso `iluminacion.frag` trata cada farola como un foco (cono) con eje (0, −1, 0), con la misma lógica que los faros del auto (`factorCono()`, compartida): el ángulo entre el eje y la dirección bombilla → fragmento decide si llega luz. Dentro de `ANGULO_FAROLA_INTERIOR` (35°) es plena, fuera de `ANGULO_FAROLA_EXTERIOR` (60°) es nula y entre ambos baja con `smoothstep`; además conserva la atenuación por distancia (lineal + cuadrática). Resultado: un círculo de luz en el suelo, pleno en toda la vereda y que se desvanece pasando el centro de la calzada, y las paredes cercanas solo se iluminan por debajo de la bombilla (a unos 3 de altura como máximo), nunca por encima de la pantalla. La bombilla emisiva no cambia. Nada de la farola se apoya en la calzada ni participa en colisiones.

**Modelo de la farola.** Solo usa las mallas de los árboles (cilindro, esfera y cono de `Figuras`), sin figuras nuevas. De abajo hacia arriba:
- **Base:** cilindro corto y ancho, gris oscuro (0.42 de diámetro y 0.45 de alto).
- **Poste:** dos tramos de cilindro gris metálico. El de arriba es más delgado (0.2 → 0.14), así el poste se afina hacia la punta, a 4.15 de altura.
- **Brazo curvo:** tres tramos de cilindro que siguen `PERFIL_BRAZO`: uno empinado, uno suave y uno horizontal a 4.85. Cada tramo se inclina con `uRotacion`, cuyas columnas son el eje lateral, la dirección del tramo y su producto vectorial. Esferas chicas en los codos tapan las uniones.
- **Luminaria:** una pantalla cónica casi negra cuelga de la punta del brazo. Debajo asoma la bombilla, una esfera de 0.32 gris claro de día; de noche es emisiva y de color blanco cálido.

La bombilla se dibuja en `BOMBILLAS[i]`, el mismo punto que `preparar()` envía como `uLuces[i]`: el dibujo y la luz comparten la constante, sin coordenadas duplicadas. El modelo se arma sin OpenGL (`modeloFarola(i, noche)` devuelve la lista de piezas) y `dibujarFarolas()` solo lo recorre.

Las 13 están a mitad de cuadra, lejos de las esquinas donde están los semáforos, los PARE y los pasos peatonales (al menos 3 unidades de cualquier señal), y repartidas por los cinco sectores: Centro 3, Barrio Norte 3, Parque Sur 3, Zona Oeste 2 y Zona Este 2. `FarolasTest` comprueba:
- **Ubicación del poste:** cada farola está en una manzana con calle por el lado indicado, y el poste queda sobre la acera.
- **Ubicación de la bombilla:** queda sobre el borde de la calle, en la punta del brazo.
- **Bombilla dibujada = luz:** hay al menos 9 farolas y, de día y de noche, la bombilla del modelo está exactamente en la posición de la luz de su farola. Solo es emisiva de noche.
- **Modelo conectado:** la base apoya en la acera, el poste llega al primer punto del brazo, el último tramo termina sobre la bombilla y cada rotación es una matriz ortonormal.
- **Sin conflictos:** respeta la separación con semáforos, PARE y carteles, y no hay farolas sobre pasos peatonales.
- **Reparto y colisiones:** hay farolas en todos los sectores y las calles siguen transitables.
- **Foco hacia abajo:** con los valores leídos de `iluminacion.frag`, un punto por encima de la bombilla recibe 0, el suelo justo debajo recibe el máximo (cono pleno) y un punto fuera del cono exterior recibe 0.
- **Atenuación:** a lo largo del eje, la luz disminuye con la distancia.
- **Círculo de luz:** en cada farola llega luz a la vereda hasta la pared del edificio y al centro de la calzada, y la pared a la altura de la pantalla queda a oscuras.

### Edificios: cinco tipos

Antes todos los edificios eran la misma caja con techo plano gris: solo cambiaban la altura y el color, y se veían clonados. Ahora `mundo/Edificio` elige para cada manzana un tipo (`TipoEdificio`) y lo arma con varios **volúmenes** (cajas con paredes) y **piezas de techo**:

| Tipo | Forma | Alturas | Azotea o techo |
|---|---|---|---|
| `TORRE` | Podio de 7 × 7 con el negocio y, encima, una torre angosta de 4.4 × 4.4 | 16 a 22 | Antena (mástil de 3.5) o tanque de agua sobre cuatro patas, mitad y mitad |
| `BLOQUE` | Una caja de 7 × 7, como el edificio de antes | 7 a 11 | Losa, baranda metálica en todo el borde y caja de ascensor corrida hacia una esquina |
| `ESCALONADO` | 2 o 3 niveles apilados de 7, 5.2 y 3.6 de lado, cada uno apoyado en el de abajo | 4 a 5 por nivel | Una losa en cada nivel |
| `CASA_BAJA` | Casa de 1 o 2 pisos (3.2 y 5.2 de pared) | Techo de 2 más | Techo a dos aguas: un prisma triangular color teja, con la cumbrera en X o en Z, y una chimenea |
| `DOBLE` | La manzana se parte en dos volúmenes pegados: uno alto de 4 de ancho y uno bajo de 3, de otro color | 9 a 13 y 4 a 6 | Una losa en cada volumen |

- **Siempre la misma ciudad:** el tipo, los colores, las alturas y la orientación de cada pieza salen de `Variacion.valor` con la fila, la columna y una semilla fija por decisión, sin `Random`. La misma celda da siempre el mismo edificio. `SEMILLA_TIPO` está elegida para que en este mapa los tipos salgan parejos: 4 torres, 4 bloques, 4 escalonados, 4 casas bajas y 3 dobles.
- **Paleta urbana:** las paredes son ladrillo, crema, blanco hueso, gris cemento, terracota, verde agua o amarillo pálido (`PALETA_FACHADAS`). Los techos son gris oscuro, gris claro, teja o verde de terraza (`PALETA_TECHOS`); las casas bajas son siempre de teja.
- **Misma huella:** el volumen de abajo ocupa siempre los 7 × 7 de la manzana y tiene al menos `ALTO_MINIMO_BASE` = 3.2, así que la planta baja comercial entra igual en todos los tipos. Nada pasa de `MEDIA_HUELLA` = 3.65 desde el centro (la losa sobresale `VUELO_CORNISA` = 0.15, como la cubierta de antes). Las colisiones, `Mapa.LIMITE`, el minimapa y los destinos de entrega no cambian.
- **Dibujo:** `Ciudad` le pide a `Edificio` cada edificio. Las piezas se calculan una sola vez al crear la ciudad, y se dibujan con `Cubo` (paredes, losas, baranda, ascensor, patas, chimenea), el cilindro de `Figuras` (antena y tanque) y el prisma de `Figuras` (techo a dos aguas).

`EdificioTest` comprueba:
- **Determinismo:** la misma celda da siempre el mismo tipo, los mismos colores y las mismas piezas.
- **Variedad:** hay al menos 4 tipos y 4 colores de pared, y no todos los techos son gris oscuro.
- **Huella:** ninguna pieza sale de la huella de su manzana ni se hunde en la acera, y el volumen de abajo tiene altura para la planta baja.
- **Forma:** la torre es angosta y alta, el escalonado se achica y cada nivel se apoya en el de abajo, la casa baja tiene techo de teja a dos aguas y el doble tiene dos alturas distintas.

### Edificios: planta baja y ventanas

Las fachadas las dibuja `mundo/Fachada`.

**Planta baja: vidriera | puerta | vidriera.** En cada cara que da a una calle, la fila de ventanas del primer piso se reemplaza por un negocio. Sobre los 7 de ancho de la cara, desde el centro hacia cada esquina, hay:

| Tramo | Ancho |
|---|---|
| Media puerta | 0.5 |
| Separación (`SEPARACION_PUERTA`) | 0.2 |
| Vidriera | 2.4 |
| Margen de esquina (`MARGEN_ESQUINA`) | 0.4 |

- **Puerta:** oscura, de 1 × 2 y **centrada**. Así ninguna puerta queda junto a una esquina y dos caras vecinas nunca tienen puertas pegadas.
- **Vidrieras:** una a cada lado de la puerta, de 2.4 × 1.4. De día son vidrio celeste. De noche se ven iluminadas desde adentro: son emisivas, cálidas y menos intensas que una placa blanca, con un degradado vertical, más claro abajo y más tenue arriba. Como `Cubo` pinta cada caja de un solo color, el degradado se arma con `FRANJAS_VIDRIERA` = 6 franjas horizontales cuyo color se interpola entre `COLOR_VIDRIERA_NOCHE_ABAJO` y `COLOR_VIDRIERA_NOCHE_ARRIBA` (`colorVidrieraNoche()`).
- **Toldos:** uno sobre cada vidriera, 0.05 más ancho a cada lado, sin tapar la puerta ni pasar el margen de la esquina. Como `Cubo` solo gira alrededor del eje Y, la inclinación se arma con 3 tiras escalonadas y un faldón.
  - **Vuelo:** sobresalen 0.7, así que su borde queda a 4.2 del centro del edificio: sobre la vereda y antes de los postes de semáforo (4.4) y de farola (4.6). Nunca llegan a la calzada ni cambian colisiones.
  - **Color:** rojo, verde, azul o naranja, y en algunos edificios a rayas con blanco. Se elige por celda y siempre es el mismo.

**Ventanas según el tipo.** `Fachada.ventanas()` recorre cada cara de cada volumen y reparte las ventanas con el patrón de su `TipoEdificio`:

| Tipo | Columnas por cara | Ancho × alto | Altura de piso |
|---|---|---|---|
| `TORRE` | 4, juntas | 0.6 × 0.8 | 1.6: más filas |
| `BLOQUE` | 2, anchas | 2.0 × 0.9 | 2 |
| `ESCALONADO` | 3 | 0.9 × 1.0 | 2 |
| `CASA_BAJA` | 2, pocas | 1.0 × 1.0 | 2 |
| `DOBLE` | 3 | 0.8 × 0.9 | 2 |

- **Caras angostas:** si las columnas no entran en una cara (por ejemplo, la parte baja de un doble), se usan menos, dejando `MARGEN_LATERAL` a cada lado.
- **Pisos alineados:** los pisos se cuentan desde la acera para todo el edificio, así las filas de ventanas de volúmenes vecinos coinciden.
- **Ventanas que no se ponen:** las que no entran entre la base y el tope de su volumen (`MARGEN_VERTICAL`), las que quedarían sobre el negocio o el toldo (por debajo de `TOPE_PLANTA_BAJA` en una cara a la calle) y las que tapa otro volumen del mismo edificio, como la parte baja de un doble o el nivel de abajo de un escalonado. Por eso una casa baja de un piso no tiene ventanas: toda su planta baja es negocio.

**Ventanas según día y noche.**
- **De día:** todas son vidrio claro, blanco-celeste grisáceo, sin emisión. El sol las ilumina como a cualquier superficie.
- **De noche:** alrededor del 65 % está encendida (`PORCENTAJE_VENTANAS_ENCENDIDAS`); hoy son 633 de 979 (65 %).
  - **Encendidas:** emisivas, con tonos de `TONOS_VENTANA`: amarillo cálido 40 %, blanco cálido 30 %, anaranjado suave 20 % y blanco frío 10 %.
  - **Apagadas:** azul-gris muy oscuro.
- **Sin parpadeo:** cada ventana decide con un hash de edificio, volumen, cara, piso y columna (`Variacion.valor`, sin `Random`), así que la misma ventana siempre está igual.
- **Estado día/noche:** se lee de `Iluminacion.esNoche()` y no se duplica.

`FachadaTest` comprueba:
- **Ventanas:** cada una tiene siempre el mismo estado; de noche la fracción encendida queda dentro de ±10 % y aparecen todos los tonos; de día ninguna es emisiva. Además, cada ventana queda dentro de su volumen, nunca sobre el negocio ni tapada por otro volumen, y el patrón cambia con el tipo.
- **Planta baja:** ninguna puerta está a menos de `MARGEN_ESQUINA` de una esquina y las puertas de caras vecinas nunca comparten esquina. Además, las vidrieras y los toldos entran en la cara sin tapar la puerta, y de noche cada franja de la vidriera es igual o más tenue que la de abajo, todas menos intensas que la placa de antes.
- **Toldos:** su vuelo no llega a la calzada y se usan todos los colores, lisos y a rayas.

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

  `SemaforoTest` recorre dos ciclos completos y lo verifica.
- **Modelo del cabezal (coherente con las farolas):** base cilíndrica ancha (`ANCHO_BASE` 0.34, `ALTO_BASE` 0.35) y poste cilíndrico gris oscuro (`ANCHO_POSTE` 0.14) hasta una caja angosta y alta casi negra (`ANCHO_CAJA` 0.42 × `ALTO_CAJA` 1.3 × `PROFUNDIDAD_CAJA` 0.34, desde `BASE_CAJA` = 2.5 de altura, por encima del techo de los autos). En la cara que mira al tráfico hay tres **lentes redondas** (cilindros cortos acostados con `uRotacion`): roja arriba, amarilla al medio y verde abajo. La lente de la fase es **emisiva** y tiene su color intenso (`COLORES_LENTE`); las otras dos usan el mismo color multiplicado por `BRILLO_APAGADA` (0.15), sin emisión, así se ve que hay tres y cuál es cuál. Sobre cada lente hay una **visera**: una placa fina inclinada `INCLINACION_VISERA` = 20° hacia abajo. La espalda de la caja queda lisa, como en un semáforo real.
- **Modelo sin OpenGL:** igual que `modeloFarola()`, `Semaforo.modelo(activa)` devuelve la lista de piezas en coordenadas locales del cabezal y `dibujar()` solo la recorre, girándola hacia el acceso. `SemaforoTest` comprueba que en cada fase hay exactamente una lente emisiva y es la del color de la fase, que las lentes están en orden (rojo arriba, verde abajo) dentro de la caja y en su frente, y que nada sobresale por detrás.

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

**PARE: en cruces sin semáforo, nunca junto a uno.** No se escriben a mano: `calcularUbicacionesPare()` elige **los cruces sin semáforo más cercanos al Centro**, hasta `MAX_PARE` = 10, con estos criterios:
- **Fuera del Centro:** allí hay semáforos.
- **Cruce interior:** los del borde son esquinas y "T" de la calle perimetral, que rodea la ciudad y conserva la prioridad.
- **No en el cruce de entrada del sector:** la entrada principal (la del cartel) es la avenida con prioridad.
- **Cercanía al Centro:** se ordenan por distancia al origen; es donde hay más tránsito cruzado.

Con el mapa 11 × 11 quedan **8** (`UBICACIONES_PARE`):

| Sector | Intersecciones | Autos que detiene |
|---|---|---|
| Barrio Norte | (2,2), (2,4), (2,8) | los que vienen del norte |
| Parque Sur | (8,2), (8,6), (8,8) | los que vienen del sur |
| Zona Oeste | (6,2) | los que vienen del oeste |
| Zona Este | (4,8) | los que vienen del este |

(2,6), (8,4), (4,2) y (6,8) no llevan PARE porque son los cruces de entrada del Barrio Norte, el Parque Sur, la Zona Oeste y la Zona Este.
- **Por qué esos accesos:** frenan a quien llega desde el borde de la ciudad por la calle secundaria (`direccionHaciaAfuera()` del sector).
- **Posición:** también en la esquina de vereda a la derecha del acceso, mirando al auto que llega.
- **Forma:** octógono rojo aproximado con cubos, borde blanco y franja blanca, sobre un poste.
- **Nunca junto a un semáforo:** dos señales que ordenan cosas distintas en el mismo cruce confunden al conductor. `SenalizacionTest` lo verifica.
- **Contramano:** se eliminaron las señales de contramano/dirección.

**Carteles de sector: uno por sector (5).** Están en la **entrada principal** de cada sector (`entradaDeSector()`), calculados con `cartelDeSector()`:
- **Sentido de la entrada:** hacia afuera (al Barrio Norte se entra yendo al norte, a la Zona Oeste yendo al oeste); al Centro se entra desde el sur.
- **Calle de la entrada:** el eje central si es una calle; si no, la avenida contigua al eje del lado derecho del conductor. Con 11 × 11: X = 10 hacia el norte (Centro y Barrio Norte), X = -10 hacia el sur (Parque Sur), Z = -10 hacia el oeste (Zona Oeste) y Z = 10 hacia el este (Zona Este).
- **Posición:** en la vereda derecha, sobre la **primera manzana del sector** que encuentra el conductor, `RETROCESO_CARTEL` = 0.75 después de la esquina y `SEPARACION_CARTEL` = 1.5 adentro del cordón, mirando a los autos que entran.

 Son verdes con borde blanco, estilo vial, y llevan el mismo nombre que el HUD y el minimapa. El texto se genera con STBEasyFont y cada trazo se dibuja como una caja fina sobre la placa.

### Figuras generadas por código

Además del cubo, la ciudad usa tres mallas creadas con seno y coseno, sin imágenes ni modelos externos (`motor/Malla`). Todas son unitarias, como el cubo: 1 de ancho y 1 de alto, centradas en el origen, así que la escala que se pasa al dibujar es su tamaño real.

| Figura | Cómo se arma | Normal en cada vértice | Vértices |
|---|---|---|---|
| Esfera (12 × 8) | Como un globo terráqueo: 8 franjas de latitud × 12 meridianos, y cada punto es (sen θ cos φ, cos θ, sen θ sen φ) · 0.5 | La dirección del centro al punto: en una esfera es perpendicular a la superficie | 6 · 12 · 7 = 504 |
| Cilindro (10 lados) | Un polígono de 10 lados arriba y otro abajo, unidos por rectángulos, más dos tapas en abanico | En el costado, horizontal hacia afuera (cos φ, 0, sen φ); en las tapas, (0, ±1, 0). Los bordes se repiten con otra normal porque ahí hay una arista | 12 · 10 = 120 |
| Cono (10 lados) | Triángulos de la base a la punta, más la base en abanico | En el costado, (h cos φ, r, h sen φ) normalizada: el producto vectorial entre la recta que sube a la punta y el borde. En la punta se usa el ángulo medio de cada triángulo. En la base, (0, -1, 0) | 6 · 10 = 60 |
| Prisma triangular (`Figuras.prisma`) | El perfil `PERFIL_PRISMA` (un triángulo de base 1 y alto 1) extruido con `Malla.extruir` a 1 de ancho. Escalado, es el techo a dos aguas de las casas bajas | Una por cara, como la extrusión: dos faldones inclinados, la base y las dos tapas triangulares | 12 · 3 − 12 = 24 |
| Extrusión de perfil (`Malla.extruir(perfil, ancho)`) | Un contorno 2D visto de costado (puntos {z, y} en orden, convexo) se "estira" a lo ancho en X: dos tapas con la forma del perfil (en abanico) y un rectángulo por cada lado del contorno que las une | Una por cara (flat shading): el producto cruz de dos lados del triángulo, normalizado; si apunta hacia el centro de la figura se invierte, así todas miran hacia afuera. Las aristas quedan marcadas, como en una carrocería | 12 · n − 12 (36 para 4 puntos) |

Como cada vértice lleva la normal de la superficie curva y no la de su triángulo plano, OpenGL la interpola y la iluminación las muestra redondeadas. `ciudad.vert` corrige las normales con la inversa de la escala, así que siguen siendo correctas al estirar una figura, por ejemplo una copa achatada. `Juego` crea y libera las figuras igual que el cubo. Cada malla enlaza su propio VAO al dibujar, y `Cubo` también, para poder mezclarlas en la misma escena.

**Cabina de los autos (`vehiculo/Cabina`).** Reemplaza el bloque celeste. Es un trapecio visto de costado (`PERFIL_CABINA`) extruido a `ANCHO_CABINA` = 1.40, un poco menos que la carrocería (1.65):
- **Forma:** la base mide 1.40 y el techo 0.67. El parabrisas está a 45° y la luneta a unos 65°, más vertical. El techo queda en Y = 1.40, casi a la misma altura que el bloque anterior (1.395).
- **Color:** la cabina se pinta del color de la carrocería, así se ven el techo y los parantes.
- **Parabrisas y luneta:** cajas finas azul-gris oscuro, sin emisión, separadas 0.012 de la cara para evitar el z-fighting. Siguen la inclinación de su cara con `uRotacion`, cuyas columnas son el eje lateral, la dirección de la cara y su normal.
- **Ventanillas:** dos por lado. Cada una es un trapecio fino extruido que sigue la inclinación de los parantes, y un parante central del color del auto las separa.
- **Tráfico:** usa la misma cabina con su propio color.

`CabinaTest` verifica la forma, que nada sobresalga del ancho del cuerpo y que las ventanillas queden dentro de la cabina y a cada lado del parante.

### Ambiente: campo, cielo y sombras

Antes la ciudad flotaba sobre un fondo azul liso y los objetos parecían despegados del suelo. Ahora hay tres agregados, todos visuales: no cambian `Mapa.LIMITE`, las colisiones ni la escala del minimapa.

**Campo alrededor (`mundo/Entorno`).**
- **Pasto:** se extiende `ENTORNO_EXTRA` = 100 más allá de cada borde (hasta ±155, `BORDE_CAMPO`). Son 4 franjas (norte, sur, oeste y este) que rodean la base de asfalto sin superponerse con ella. Una sola caja grande debajo de la ciudad quedaría casi en el mismo plano que el asfalto y, de lejos, parpadearía (z-fighting).
- **Cordón:** una vereda gris de `ANCHO_CORDON` = 1.2, del alto de las aceras, justo afuera del borde. La calle perimetral termina contra él.
- **Árboles:** 36 (`CANTIDAD_ARBOLES`), los mismos pinos y frondosos de los parques (`Parque.dibujarArbol`). Van entre 6 y 50 unidades afuera del borde. Las posiciones se sortean con `Variacion` y una semilla fija (`SEMILLA_ARBOLES`) por el método de rechazo: se elige un punto del cuadrado exterior y se descarta si cae dentro de la ciudad. Siempre salen iguales.
- **Qué no cambia:** `Colisiones` sigue deteniendo al auto en el borde (`LIMITE - RADIO_AUTO`), así que nunca llega al cordón ni al pasto. En el minimapa no se dibuja.

**Cielo con degradado (`iluminacion/Cielo`).**
- **Cúpula:** es la esfera de `Figuras`, de radio `RADIO_CIELO` = 250 (dentro del plano lejano, `Camara.getPlanoLejano()` ≈ 319) y centrada en la cámara. Así el cielo nunca se acerca ni se aleja, como si estuviera infinitamente lejos. `iluminacion.frag` (rama `uCielo`) calcula el color con la dirección de la mirada: `mix(horizonte, cénit, altura^0.6)`.
  - **De día:** azul intenso arriba y celeste claro en el horizonte.
  - **De noche:** azul casi negro arriba y azul noche en el horizonte, con 160 estrellas (cajas chicas emisivas, en posiciones fijas sobre el horizonte) y una luna (esfera emisiva) hacia el norte-noreste.
- **Se dibuja primero y sin escribir profundidad** (`glDepthMask(false)`). El depth buffer guarda la distancia de lo más cercano en cada píxel. Si la cúpula la escribiera, lo que está más lejos que ella (el final del campo desde la cámara aérea) quedaría tapado por el cielo. Sin escribirla, el buffer sigue vacío y todo lo que se dibuja después queda delante, sea cual sea el radio.
- **Dónde se ve:** desde la cámara de seguimiento la vista apunta hacia abajo y casi no hay cielo en pantalla. Se ve bien en la orbital (C) bajando la cámara con el mouse, y arriba de la aérea.

**Sombras falsas (`iluminacion/Sombras`).**
- **Qué son:** una mancha oscura semitransparente debajo de cada edificio, auto (jugador y tráfico), árbol (de los parques y del campo) y banco. No calculan de dónde viene la luz: solo oscurecen el suelo bajo el objeto para que parezca apoyado.
- **Forma:** cada mancha es el cubo aplastado, del tamaño y con el giro de su objeto. El shader (rama `uSombra`) recibe la posición local del fragmento (`vLocal`, de -0.5 a 0.5) y calcula una distancia al centro d = (|x|ⁿ + |z|ⁿ)^(1/n), con n = 2 (elipse) o n = 4 (rectángulo de esquinas redondeadas, para los edificios). La opacidad baja con `smoothstep` desde el núcleo hasta el borde, así los bordes son difusos y nunca rectangulares. En los edificios el degradado empieza en la pared, que es lo que se ve sobre la acera.
- **Movimiento:** las de autos y tráfico siguen su posición y orientación en cada cuadro. Las demás se calculan una sola vez (`Sombras.FIJAS`).
- **Blending:** con `GL_BLEND` y `glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)` el color final es sombra · α + suelo · (1 − α): el suelo se oscurece sin taparse. α vale `ALFA_SOMBRA_DIA` = 0.45 en el centro, y de noche `ALFA_SOMBRA_NOCHE` = 0.22, más tenue.
- **Sin escribir profundidad:** la prueba de profundidad sigue activa, así que la pared o la rueda que está delante tapa la mancha. Pero la mancha no escribe su distancia: así dos sombras superpuestas no se cortan con un borde duro. Por eso se dibujan al final de la escena opaca, cuando el suelo ya está pintado.
- **Altura:** `ELEVACION_SOMBRA` = 0.06 sobre su superficie (asfalto, acera, césped o pasto), para evitar el z-fighting y quedar por encima de la pintura vial (0.04). Ninguna mancha fija sale de su acera o su césped. En el minimapa no se dibujan.

`EntornoTest` comprueba que `Mapa.LIMITE` sigue siendo medio lado de la matriz (55 con 11 × 11) y las colisiones no cambian: las calles perimetrales siguen transitables, el borde permitido es el de siempre y ni el cordón ni los árboles son alcanzables. También que los árboles están afuera, dentro del campo y son siempre los mismos. `AmbienteTest` comprueba que la cúpula, las estrellas y la luna entran en el plano lejano, que las estrellas están sobre el horizonte y que hay una sombra fija por edificio, árbol y banco, sin salir de su superficie, más tenue de noche.

## 3. Controles

| Tecla | Acción |
|---|---|
| W / ↑ | Acelerar |
| S / ↓ | Frenar y, detenido, retroceder |
| A / ← y D / → | Girar (solo con el auto en movimiento; en reversa el giro se invierte) |
| Espacio | Freno fuerte |
| C | Cambiar de cámara: seguimiento → orbital del auto → aérea de toda la ciudad → seguimiento |
| Mouse, botón izquierdo | En la cámara orbital: arrastrar para girar alrededor del auto (horizontal) y subir o bajar la cámara (vertical). En la aérea: lo mismo, pero alrededor del punto de la ciudad que se está mirando |
| Ruedita del mouse | En la orbital y en la aérea: acercar y alejar (en la aérea, alejada al máximo se ve toda la ciudad) |
| Mouse, botón derecho | En la aérea: arrastrar para desplazar el punto que se mira, como si se arrastrara el suelo con la mano; nunca sale de la ciudad |

En la cámara de seguimiento el mouse no hace nada. Cada vez que se entra a la aérea, arranca con la vista general de siempre (la de las capturas).
| N | Alternar día / noche |
| F | Encender / apagar los faros del auto: los conos de luz sobre la calle, las bombillas delanteras (blancas y emisivas) y las luces de posición traseras (rojo tenue) |
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
| `Juego` | Ciclo principal entrada → `actualizar(dt)` → dibujar, con dt limitado a 50 ms y llevado a 0 en menú o pausa. Reparte las teclas, compone el título, lleva el reloj global de la animación, impide que el jugador atraviese el tráfico, fija el orden de dibujo (cielo, ciudad, campo, auto, tráfico, farolas, decoración, destino, sombras, minimapa, HUD) y libera los recursos. |
| `motor/Ventana` | Ventana GLFW y contexto OpenGL 3.3 Core, callback de teclado, `pulsada()`, tamaño real del framebuffer, título, presentación y cierre, y callbacks de mouse (botones izquierdo y derecho, cursor y ruedita) que entregan a la cámara los arrastres de cada botón y el zoom. |
| `motor/Shader` | Carga GLSL desde `resources/shaders`, normaliza `#version` para Windows, compila, enlaza y envía uniforms (`vector2`, `vector`, `vector4`, `decimal`, `entero`, `matriz3`) con caché de ubicaciones. |
| `motor/Cubo` | Cubo de 36 vértices con normales en un VAO/VBO; `caja()` y `cajaGirada()` lo dibujan con posición, escala, giro y color. |
| `motor/Camara` | Tres modos con C: seguimiento (12 unidades detrás del auto; se acerca si ese punto cae sobre una manzana o fuera de la ciudad y vuelve de a poco: `distanciaLibre()`, `actualizarSeguimiento()`), orbital del auto y vista aérea de la ciudad. Las dos últimas se manejan con el mouse y usan cada una su `Orbita`: la del auto, centrada en el auto y relativa a su ángulo; la aérea, centrada en un punto de la ciudad que el botón derecho desplaza (`desplazar()`, limitado a ±`Mapa.LIMITE`). Al entrar a la aérea vuelve a la vista inicial, proporcional a `Mapa.LIMITE`. `getOjo()` devuelve la última posición de la cámara, donde `Cielo` centra su cúpula. `getPlanoLejano()` calcula la distancia máxima visible desde `Mapa.LIMITE` y la envía a `ciudad.vert` (`uPlanoLejano`). |
| `motor/Orbita` | Coordenadas esféricas compartidas por las dos cámaras con mouse: ángulo θ, elevación φ y distancia D alrededor de un centro, con sus límites. `girar()`, `acercar()`, `colocar()` y `ojo()`, que convierte (θ, φ, D) en la posición (D · cos φ · sen θ, D · sen φ, D · cos φ · cos θ) + centro. |
| `motor/Texto` | Convierte texto en rectángulos con STBEasyFont; lo usan el HUD (2D) y los carteles de sector (3D). |
| `motor/Malla` | Figura genérica en un VAO/VBO con el mismo formato que `Cubo` (posición + normal). Generadores por fórmulas: `esfera(sectores, anillos)` (esfera UV), `cilindro(lados)` con tapas y `cono(lados)` con base, todos unitarios. `dibujar()` / `dibujarGirada()` funcionan como `caja()` / `cajaGirada()`.. `extruir(perfil, ancho)` genera una figura extruida desde un perfil 2D con normales por cara (producto cruz). |
| `motor/Figuras` | Crea la esfera (12 × 8), el cilindro (10 lados), el cono (10 lados) y el prisma triangular del techo a dos aguas, los sube a la GPU y los libera, igual que `Cubo`. |
| `mundo/Mapa` | `MAPA` 11 × 11, intersecciones (`esInterseccion`, `intersecciones`), accesos (`accesos`), parques (`parques`), `sectorDeCelda`, `TAM_CELDA`, `TAMANO`, `LIMITE`, `centro()`, `indiceCelda()`, `esCalle()`, `esCalleEn()`, y los cinco sectores con nombre (`SECTORES`, `NOMBRES_SECTORES`, `sector()`, `nombreSector()`). |
| `mundo/Ciudad` | Base de asfalto, líneas amarillas entre cruces, aceras, edificios (delegados en `Edificio`) y césped de los parques. |
| `mundo/Edificio` | Elige el tipo y los colores de cada edificio con `Variacion` (`tipo()`, `colorPared()`, `colorTecho()`), arma sus volúmenes (`volumenes()`) y las piezas del techo (`piezas()`: losas, baranda, ascensor, antena, tanque, techo a dos aguas y chimenea), y las dibuja. Tiene la paleta urbana y las alturas por tipo. |
| `mundo/TipoEdificio` | Enum con los cinco tipos (`TORRE`, `BLOQUE`, `ESCALONADO`, `CASA_BAJA`, `DOBLE`) y el patrón de ventanas de cada uno. |
| `mundo/Decoracion` | Fachadas de los edificios (delegadas en `Fachada`) y los pasos peatonales de `UBICACIONES_PASOS` (un paso por acceso de cada cruce con semáforo y hasta dos junto a cada parque, en ambas orientaciones); `hayPasoSobre()` permite que `Ciudad` corte la línea amarilla. Delega los parques en `Parque` y la señalización en `Senalizacion`. |
| `mundo/Fachada` | Planta baja comercial en las caras a la calle: vidriera, puerta centrada y vidriera, con un toldo escalonado de color (a veces a rayas) sobre cada vidriera; las vidrieras se iluminan de noche. Ventanas en cada volumen según el patrón del tipo (`ventanas()`), sin tapar el negocio ni quedar dentro de otro volumen: vidrio claro de día; de noche, ≈65 % encendidas con tonos variados, decidido por ventana con un hash. |
| `mundo/Variacion` | Hash determinístico `valor(fila, columna, índice, semilla)` que usan `Parque`, `Fachada` y `Edificio` para variar sin azar por cuadro. |
| `mundo/Parque` | Senderos en cruz, fuente central, 4 a 6 árboles (frondosos de esferas y pinos de conos) y 2 a 4 bancos mirando a la fuente. `arboles()` y `bancos()` calculan la disposición de cada parque con `variacion()` (determinística). `dibujarArbol()` es estático: `Entorno` lo reutiliza para los árboles del campo. |
| `mundo/Entorno` | Campo que rodea la ciudad: 4 franjas de pasto hasta `ENTORNO_EXTRA` más allá del borde, cordón de vereda y `ARBOLES` dispersos (posiciones determinísticas con `Variacion`). Solo decoración: no cambia `Mapa.LIMITE`, colisiones ni minimapa. |
| `mundo/Senalizacion` | Ubica y dibuja la señalización vial: `INTERSECCIONES_SEMAFORO` (las del Centro), un cabezal por acceso (`SEMAFOROS`), `UBICACIONES_PARE` y `CARTELES_SECTOR`, calculados con reglas (`direccionHaciaAfuera()`, `entradaDeSector()`, `cruceDeEntrada()`, `cartelDeSector()`, `calcularUbicacionesPare()`). `esquinaDerecha()` calcula la esquina de vereda a la derecha de un acceso y la orientación hacia el auto. |
| `mundo/Semaforo` | Ciclo rojo (7 s) → verde (5 s) → amarillo (2 s) en bucle; `luzParaAcceso()` desfasa el grupo este-oeste 7 s para que sea complementario del norte-sur. Arma el cabezal como lista de piezas (`modelo()`: base y poste cilíndricos, caja angosta, tres lentes redondas con visera) y lo dibuja orientado hacia su acceso; solo la lente activa es emisiva. |
| `vehiculo/Auto` | Estado (x, z, ángulo, velocidad), física por dt (aceleración 9, resistencia exponencial, freno, límites -6..16), giro proporcional a la velocidad y `reset()`. Ruedas cilíndricas que giran según la distancia recorrida (`anguloRueda += avance / RADIO_RUEDA`), delanteras que doblan hasta 30° y vuelven solas al centro, luces de freno y de reversa (`frenando()`, `enReversa()`).. Las bombillas siguen a la tecla F, leída de `Iluminacion` al dibujar. |
| `vehiculo/LucesVehiculo` | Ubicación, tamaño y colores de faros y luces traseras, compartidos por el jugador y el tráfico. `colorFaro(encendido)` y `colorTrasera(lucesEncendidas, frenando)` devuelven el color y si es emisivo; el freno tiene prioridad sobre la luz de posición. |
| `vehiculo/Cabina` | Cabina de los autos: trapecio extruido (`PERFIL_CABINA`, `ANCHO_CABINA`) del color de la carrocería, con parabrisas y luneta inclinados, y dos ventanillas por lado separadas por un parante central. La comparten el jugador y el tráfico. |
| `vehiculo/Colisiones` | Círculo del auto (radio 1.65) contra el rectángulo de cada manzana y contra los bordes del mapa; círculo contra círculo para el tráfico. |
| `vehiculo/IndicadorJugador` | Flecha cian emisiva que apunta al auto desde la vista aérea; sube y baja y gira con el tiempo. |
| `trafico/Vehiculo` | Auto autónomo: posición, ángulo, velocidad y color; calcula el carril derecho de su ruta (`calcularCarriles`) y lo sigue girando suavemente hacia el próximo waypoint, frena en las curvas y si el jugador está adelante; dibujo por piezas con luces traseras que brillan de noche. |
| `trafico/Trafico` | Cuatro rutas en celdas de calle (`RUTAS_CELDAS`), validadas contra el Mapa al arrancar; crea, actualiza, reinicia y dibuja los vehículos, e indica a `Juego` si un movimiento del jugador lo haría atravesar uno. De noche envía al shader los focos de sus faros (`prepararFaros`). |
| `iluminacion/Cielo` | Cúpula centrada en la cámara con degradado cénit → horizonte (día y noche), `ESTRELLAS` determinísticas y luna emisiva. Se dibuja primero y sin escribir profundidad. |
| `iluminacion/Sombras` | Sombras falsas: manchas con degradado circular bajo edificios, autos, árboles y bancos (`FIJAS` y las de los autos, que siguen posición y giro). Activa la mezcla, no escribe profundidad y restaura el estado al terminar. |
| `iluminacion/Iluminacion` | Día/noche, faros, 13 farolas en la vereda (`LUCES` = manzana + lado, elegidas por `calcularLuces()` con `farolaValida()`; `POSTES` y `BOMBILLAS` se calculan desde ahí), envío de uniforms de luz y modelo de cada farola (`modeloFarola`: base, poste, brazo curvo, pantalla y bombilla con cilindros, esferas y un cono), dibujado por `dibujarFarolas()`. |
| `juego/Entregas` | `DESTINOS` con nombres, regla de llegada, progreso, cronómetro, `reset()`, texto del título, marca dorada con baliza giratoria y cuadrado dorado en el minimapa. |
| `juego/Minimapa` | Segundo pase de dibujo ortográfico con el norte arriba y la ciudad completa (escala `Mapa.LIMITE`). Usa scissor y viewport y restaura el estado al terminar. Muestra un indicador cian con punta blanca para la posición y la orientación del auto, el tráfico y las divisiones de los sectores; `aPantalla()` convierte coordenadas del mundo a píxeles para escribir sus nombres. |
| `juego/EstadoPartida` | Estados MENU, JUGANDO y PAUSA; `dtEfectivo()` devuelve 0 fuera de JUGANDO, así todo queda congelado. |
| `interfaz/Dibujo2D` | Pase ortográfico 2D en píxeles con shader y VAO/VBO propios: rectángulos semitransparentes y texto de STBEasyFont. Desactiva la profundidad y activa la mezcla, y lo restaura al terminar. |
| `interfaz/Hud` | Panel de estado (velocidad, faros, día/noche, entregas, destino, sector), ayuda de controles (H), cartel de PAUSA, menú de inicio y nombres de sectores sobre el minimapa. Escala el texto según el tamaño de la ventana. |

Shaders en `src/main/resources/shaders`:

- `ciudad.vert`: escala, rotación y traslación del cubo; proyección en perspectiva o vista superior para el minimapa. La matriz `uRotacion` (identidad para todo, salvo las ruedas) agrega una rotación en cualquier eje antes del giro en Y. `vLocal` pasa la posición dentro de la figura unitaria, para el degradado de las sombras.
- `iluminacion.frag`: luz ambiente, sol (Lambert), farolas como focos hacia abajo con atenuación y conos de los faros (`smoothstep`). La función `factorCono()` calcula el borde suave de un cono y la usan las farolas y `aporteFoco()`, que a su vez usan tanto los faros del jugador como los focos del tráfico (`uFarosTrafico`, `uDireccionFarosTrafico`, `uNumFarosTrafico`). Dos ramas especiales: `uCielo` (degradado del cielo según la dirección de la mirada) y `uSombra` (mancha con alfa que baja con `smoothstep` hacia el borde).
- `plano.frag`: shader de color plano de la primera lección, conservado como referencia.
- `hud.vert` y `hud.frag`: dibujo 2D en píxeles con color RGBA, para el HUD.

Pruebas en `src/test/java/com/graphics/ciudad`:

- `MapaTest`: límites 110/55, al menos 12 edificios y 4 parques, calles conectadas (BFS) y posiciones sobre calles.
- `ColisionesTest`: calles transitables y obstáculos.
- `AutoTest`: `reset()` del auto, que lo deja en el carril derecho (a la derecha del centro de su calle, sin tocar la línea amarilla ni la vereda); recorrer 2π · `RADIO_RUEDA` gira la rueda 2π (y negativo en reversa); las ruedas delanteras nunca pasan `ANGULO_MAX_DIRECCION` y vuelven a 0 al soltar; luces de freno (S hacia adelante o Espacio) y de reversa según velocidad y teclas.
- `CamaraTest`: C recorre los tres modos; la elevación y la distancia de la cámara orbital nunca salen de sus límites; en la aérea, la elevación, la distancia y el centro nunca salen de los suyos (y llegan justo al tope); la aérea arranca con la vista de siempre cada vez que se entra; en seguimiento el mouse no hace nada y en la orbital el botón derecho tampoco, ni se toca la aérea; el plano lejano (`getPlanoLejano()`, enviado como `uPlanoLejano`) alcanza para la aérea alejada al máximo con el centro en una esquina; la cámara de seguimiento queda dentro del límite en la salida, a 45° en un cruce el ojo y la línea hasta el auto no pasan sobre la manzana, en una calle recta la distancia es la normal (12) sin atraso, y después de un recorte vuelve a 12 sin saltos.
- `OrbitaTest`: la posición sale de las coordenadas esféricas (distancia al centro = D, altura = D · sen φ, ángulo horizontal = base + θ) y φ y D nunca salen de sus límites.
- `EntregasTest`: 3 entregas, `reset()`, progreso en 0 y primer destino otra vez activo.
- `MallaTest`: esfera, cilindro y cono tienen la cantidad de vértices esperada (504, 120 y 60), normales de largo 1 y dentro del cubo unitario; la esfera tiene normales radiales, el cilindro horizontales en el costado y verticales en las tapas, y el cono tiene la inclinación correcta; la extrusión de un perfil de 4 puntos tiene 36 vértices, normales unitarias hacia afuera (con el contorno en cualquier sentido de giro), iguales en los tres vértices de cada triángulo, y tapas mirando a ±X.
- `CabinaTest`: base más larga que el techo, parabrisas más inclinado que la luneta, misma altura aproximada que el bloque anterior, nada fuera del ancho del cuerpo, y ventanillas dentro de la cabina y a cada lado del parante central.
- `ParqueTest`: 4 a 6 árboles por parque, con el centro libre, copa ≤ 1/4 del parque y dentro de la celda; 2 a 4 bancos mirando a la fuente; parques distintos entre sí, con pinos y frondosos; las calles siguen transitables.
- `EdificioTest`: la misma celda da siempre el mismo tipo, colores y piezas; hay al menos 4 tipos y 4 colores de pared, y no todos los techos son gris oscuro; ninguna pieza sale de la huella de su manzana (`MEDIA_HUELLA`) ni se hunde en la acera; cada tipo respeta su forma (torre angosta y alta, escalonado que se achica, casa con techo de teja a dos aguas, doble con dos alturas).
- `FachadaTest`: cada ventana tiene siempre el mismo estado; cada ventana queda dentro de su volumen, nunca sobre el negocio ni tapada por otro volumen; el patrón de ventanas cambia con el tipo; de noche, la fracción encendida está dentro de ±10 % de `PORCENTAJE_VENTANAS_ENCENDIDAS` y aparecen todos los tonos; de día ninguna ventana es emisiva; vidrieras y toldos entran en la cara con `MARGEN_ESQUINA` y no tapan la puerta; ninguna puerta está a menos de `MARGEN_ESQUINA` de una esquina y las puertas de caras vecinas nunca comparten esquina; el toldo no llega a la calzada y se usan todos los colores, lisos y a rayas.
- `SemaforoTest`: orden y duraciones del ciclo; en dos ciclos completos, los accesos opuestos siempre muestran el mismo color y norte-sur y este-oeste nunca están a la vez en verde o amarillo; en cada fase, exactamente una lente emisiva y del color de la fase; lentes ordenadas en el frente de la caja y espalda lisa.
- `SenalizacionTest`: semáforos en todas las intersecciones del Centro y solo ahí, uno por acceso, a la derecha y mirando al auto; 6 a 10 PARE en cruces sin semáforo, a la derecha y mirando al auto; un cartel por sector dentro de su sector; ningún semáforo, PARE ni cartel sobre la calle, y las calles siguen transitables. Reglas: cada PARE está en un cruce interior, fuera del Centro, que no es de entrada, detiene a quien viene del borde y ningún cruce descartado está más cerca del Centro; cada cartel está a la derecha de la entrada de su sector, sobre la primera manzana del sector y mirando al auto.
- `PasosPeatonalesTest`: cada paso está sobre calle, en la celda vecina a una intersección y pegado a su borde, con `LARGO_PASO` en el sentido de circulación y de vereda a vereda; hay pasos en ambas orientaciones; cada paso está junto a un cruce con semáforo o a un parque; cada acceso con semáforo y cada parque tienen su paso; ninguna marca amarilla queda debajo.
- `MapaTest.testSectores`: entre 4 y 5 sectores con nombre y ningún punto de la ciudad sin sector.
- `TraficoTest`: 120 s simulados con dt fijo; todos los vehículos siempre en calles, dentro del mapa, sin tocar manzanas, en movimiento y, en los tramos rectos, a `DESPLAZAMIENTO_CARRIL` ± 0.35 a la derecha de la línea central. Además comprueba que dos rutas comparten una calle en sentidos opuestos y la geometría de las esquinas de carril. También frenado ante el jugador, `reset()`, choque con el jugador y rutas inválidas rechazadas.
- `LucesTraficoTest`: con noche activa todos los vehículos tienen las luces encendidas y con día apagadas; la tecla F no las cambia; los faros acompañan posición y orientación durante los giros.
- `LucesVehiculoTest`: con F encendido los faros del jugador son emisivos (blanco cálido) y con F apagado no (gris oscuro); las traseras son luz de posición emisiva con F y rojo oscuro sin F; el freno domina en ambos casos, también con Espacio y F apagado.
- `EntornoTest`: el campo no cambia `Mapa.LIMITE` (55) ni las colisiones (calles perimetrales transitables, mismo borde permitido, cordón y árboles inalcanzables); los árboles están todos afuera de la ciudad y del cordón, dentro del campo, con tipos y copas de parque, y son siempre los mismos.
- `AmbienteTest`: la cúpula, las estrellas y la luna entran en el plano lejano; las estrellas tienen dirección unitaria y están sobre el horizonte; hay una sombra fija por edificio, árbol y banco, ninguna sale de su acera o su césped ni cae en la calle; las sombras quedan apenas sobre el suelo y son más tenues de noche.
- `JuegoTest`: en menú y en pausa, `actualizar()` no mueve el auto ni el tráfico aunque se mantenga W; jugando, sí.

## 5. Mejoras opcionales implementadas

| Mejora | Qué hace | Dónde |
|---|---|---|
| Ambiente | Campo verde con cordón y árboles alrededor de la ciudad, cielo con degradado (de noche con estrellas y luna) y sombras falsas con bordes difusos bajo edificios, autos, árboles y bancos. Ver "Ambiente" en la sección 2. | `mundo/Entorno`, `iluminacion/Cielo`, `iluminacion/Sombras`, `iluminacion.frag` (`uCielo`, `uSombra`) |
| Edificios variados | Cinco tipos de edificio (torre con antena o tanque, bloque con baranda y ascensor, escalonado, casa baja con techo de teja a dos aguas y doble), elegidos por celda con un hash, con una paleta urbana de paredes y techos y ventanas según el tipo. Respetan la huella de la manzana: las colisiones no cambian. | `mundo/Edificio`, `mundo/TipoEdificio`, `mundo/Fachada`, `motor/Figuras` |
| Figuras redondeadas y parques | Mallas nuevas generadas por fórmulas (esfera, cilindro y cono) con normales suaves, para que la luz del sol, las farolas y los faros las muestren redondeadas. Los parques tienen senderos, fuente, árboles frondosos y pinos, y bancos, con variación determinística entre parques. | `motor/Malla`, `motor/Figuras`, `mundo/Parque` |
| Farolas redondeadas | Las farolas dejan de ser cubos: base ancha, poste que se afina, brazo curvo de tres tramos inclinados con `uRotacion`, pantalla cónica oscura y bombilla esférica, emisiva solo de noche, en la misma posición que la luz. | `iluminacion/Iluminacion` |
| Semáforos redondeados | Con el mismo estilo que las farolas: base y poste cilíndricos, caja angosta y alta, tres lentes redondas (la activa emisiva y las apagadas del mismo color muy oscuro) con visera inclinada, y espalda lisa. | `mundo/Semaforo` |
| Auto mejorado | Ruedas cilíndricas con llanta, rayos y una marca roja que gira según la distancia recorrida; delanteras que doblan con A/D; luces de freno (rojo intenso) y de reversa (blanca), emisivas, de día y de noche. Las bombillas siguen a la tecla F: con F, faros blancos emisivos y luces de posición traseras en rojo tenue; sin F, apagadas (el freno sigue funcionando). Resuelve la limitación original "no hay ruedas animadas". | `vehiculo/Auto`, `vehiculo/LucesVehiculo`, `ciudad.vert` (`uRotacion`) |
| Cabina con forma | La cabina deja de ser un bloque celeste: es un trapecio extruido desde un perfil lateral (`Malla.extruir`), con parabrisas y luneta inclinados, dos ventanillas por lado y un parante central, del color de cada auto. | `motor/Malla`, `vehiculo/Cabina`, `vehiculo/Auto`, `trafico/Vehiculo` |
| Cámara orbital del auto | Nuevo modo de C: la cámara gira alrededor del auto con el mouse (arrastrar = girar y elevar, ruedita = zoom), con límites de elevación (5° a 85°) y de distancia (4 a 30); acompaña al auto cuando dobla y se puede seguir manejando. No afecta al minimapa ni al HUD. | `motor/Camara`, `motor/Ventana` |
| Cámara aérea con mouse | La vista aérea deja de ser fija: con el botón izquierdo se gira alrededor de la ciudad y se cambia la elevación (20° a 85°), con la ruedita se acerca y aleja (20 a 2.6 límites, donde se ve toda la ciudad) y con el botón derecho se desplaza el punto que se mira, sin salir de la ciudad. Reutiliza la matemática de la orbital del auto (clase `Orbita`) y al entrar arranca con la vista de siempre. No cambia el minimapa ni el HUD (solo la ayuda H). | `motor/Orbita`, `motor/Camara`, `motor/Ventana`, `interfaz/Hud`, `ciudad.vert` (`uPlanoLejano`) |
| Flecha sobre el auto | En la vista aérea (tecla C), una flecha cian emisiva a 6 unidades de altura apunta al auto, sube y baja y gira despacio; se ve también de noche. No aparece en la cámara de seguimiento ni en el minimapa. | `vehiculo/IndicadorJugador`, `motor/Camara.esAerea`, `Juego` |
| Luces del tráfico | Los vehículos encienden sus luces solos de noche (tecla N) y las apagan de día; la tecla F solo afecta los faros del jugador. Encendidas, los faros son blancos y las traseras rojas, ambas emisivas; apagadas se ven gris oscuro y rojo oscuro. De noche cada vehículo proyecta dos focos reales sobre la calle, con el mismo cono (`smoothstep`) y atenuación que los del jugador, pero más débiles y cortos. El estado día/noche se lee de `Iluminacion` (no está duplicado). | `trafico/Vehiculo`, `trafico/Trafico.prepararFaros`, `iluminacion.frag` (`aporteFoco`) |
| Tráfico autónomo | Cuatro vehículos (amarillo, azul, verde y blanco) recorren rutas cíclicas con giros, en los sectores noroeste, noreste, sureste (con forma de L) y suroeste. Circulan por el carril derecho de su sentido, a `DESPLAZAMIENTO_CARRIL` (2.5) de la línea amarilla. Las rutas 1 y 4 comparten la calle Z = -10 en sentidos opuestos. En cada esquina pasan al carril del tramo siguiente girando suavemente, sin cruzar la línea central de su calle, y frenan en las curvas. Las rutas se validan contra el Mapa, así que nunca atraviesan edificios ni salen de la ciudad. De noche se ven sus luces traseras. Se detienen si el jugador les cierra el paso, y el jugador no puede atravesarlos (círculo contra círculo). R los reinicia. | `trafico/Vehiculo`, `trafico/Trafico`, `vehiculo/Colisiones`, `Juego` |
| HUD dentro de la ventana | Panel semitransparente arriba a la izquierda con velocidad, faros, día/noche, entregas, destino y sector. Se adapta al tamaño de la ventana. El texto lo genera STBEasyFont (sin texturas) en un pase 2D final que restaura profundidad, mezcla y viewport. | `interfaz/Dibujo2D`, `interfaz/Hud`, `hud.vert`, `hud.frag` |
| Ayuda de controles | H muestra u oculta la lista de teclas, abajo a la izquierda. | `interfaz/Hud` |
| Pausa | P congela auto, tráfico, entregas y semáforos (dt = 0) y muestra "PAUSA". | `juego/EstadoPartida`, `Juego`, `interfaz/Hud` |
| Menú de inicio | Al abrir, la escena queda de fondo oscurecida con el título y "Presiona ENTER para empezar". | `juego/EstadoPartida`, `interfaz/Hud` |
| Sectores con nombre | Centro, Barrio Norte, Parque Sur, Zona Oeste y Zona Este. El HUD muestra el sector actual y el minimapa dibuja sus divisiones y sus nombres. | `mundo/Mapa`, `juego/Minimapa`, `interfaz/Hud` |
| Señalización vial | Semáforos coordinados solo en el Centro (un cabezal por acceso, a la derecha y mirando al auto), pasos peatonales en esos accesos y junto a los parques, PARE octogonales en los cruces sin semáforo más cercanos al Centro (8 con 11 × 11) y un cartel verde por sector en su entrada. Ver "Señalización vial" en la sección 2. | `mundo/Senalizacion`, `mundo/Semaforo`, `mundo/Decoracion`, `mundo/Mapa` |

## 6. Limitaciones conocidas

- **Iluminación:** es local y sin sombras reales. La luz de una farola puede atravesar un edificio (solo por debajo de la bombilla, dentro de su cono). Las sombras falsas son manchas debajo de cada objeto: no dependen de la dirección del sol ni de las farolas, y se oscurecen dos veces donde dos manchas se superponen.
- **Cielo y campo:** desde la cámara de seguimiento casi no se ve el cielo (la vista mira hacia abajo). Desde la aérea alejada al máximo, el borde lejano del campo puede quedar más allá del plano lejano y recortarse; detrás se ve el cielo.
- **Colisiones:** el auto choca solo con manzanas y bordes. Farolas, semáforos y árboles no tienen colisión, pero todos están sobre la vereda (el brazo de la farola cruza el cordón a 4.7 de altura y lo más bajo sobre la calzada es la bombilla, a 4.34), dentro de la celda de una manzana: el auto nunca llega hasta ellos, porque la colisión con la manzana lo detiene antes.
- **Choques:** el auto se detiene sin rebote. El círculo de colisión es conservador, por eso el auto frena un poco antes de tocar la acera.
- **Semáforos:** son visuales: ni el tráfico ni el jugador se detienen en rojo (el alcance del enunciado es visual). No hay peatones ni audio.
- **Luces del tráfico:** sus focos iluminan la calle y los edificios, pero no proyectan sombras (como todas las luces del juego). `MAX_FAROS_TRAFICO` limita el tráfico con luces a 8 vehículos; si hubiera más, los restantes circularían con las bombillas encendidas pero sin foco.
- **Tráfico:** las rutas son fijas. Los vehículos no chocan entre sí: van por carriles separados, pero en una intersección donde dos rutas doblan pueden superponerse un instante. Frenan si el jugador está adelante, pero no esquivan ni respetan semáforos.
- **Texto del HUD:** STBEasyFont solo tiene caracteres ASCII, así que las tildes se quitan ("Día" se ve como "Dia", "Presioná" como "Presiona"). La letra es de trazo fino y pixelado.
- **Señales:** `Cubo` solo gira alrededor del eje Y, así que el octógono del PARE se aproxima con tres rectángulos superpuestos y la palabra PARE se representa con una franja blanca. Los carteles de sector sí muestran su nombre (STBEasyFont, sin tildes).
- **Ruedas:** ya no es una limitación: giran según la distancia recorrida y las delanteras doblan. Es solo visual; la física del giro del auto no cambió.
- **Minimapa:** se reduce en ventanas pequeñas (como máximo 260 px, un tercio del lado menor), siempre en la esquina superior derecha.
- **Título:** puede verse recortado si la ventana es angosta. La velocidad en km/h supone 1 unidad = 1 metro.
- **Entorno:** necesita una sesión gráfica; no funciona en un servidor sin pantalla.

## 7. Recursos externos usados

| Recurso | Uso |
|---|---|
| Proyecto base del curso: las etapas `clase1` a `clase4` del docente (ciudad, auto, iluminación y juego final, encadenadas por herencia), basadas en los ejemplos `AppCamara.java` y `AppLaberinto.java` | Punto de partida. Este proyecto las reorganizó por composición (`com.graphics.ciudad`) y las amplió; las etapas originales ya no se incluyen |
| [LWJGL 3.3.3](https://www.lwjgl.org/) — `lwjgl-glfw` (GLFW) y sus bibliotecas nativas (Windows, Linux, macOS) | Ventana, contexto OpenGL y teclado |
| LWJGL 3.3.3 — `lwjgl-opengl` (OpenGL 3.3 Core) y sus nativas | Todas las llamadas de dibujo, shaders, VAO/VBO |
| LWJGL 3.3.3 — `lwjgl-stb` (STB / STBEasyFont) y sus nativas | Texto del HUD y de los carteles de sector: convierte cada letra en cuadriláteros, sin texturas ni archivos de fuentes |
| JUnit 3.8.1 | Pruebas automáticas (`mvn test`) |
| Maven, con `maven-compiler-plugin` 3.13.0 y `exec-maven-plugin` 3.1.0 | Compilación, dependencias y ejecución |
| JDK 17 | Lenguaje y máquina virtual |

No se usan texturas, imágenes, modelos 3D, archivos de fuentes ni sonidos externos. Toda la geometría y los colores se generan en el código.

### Herramientas de asistencia

<!-- TODO: completar con las herramientas de asistencia usadas (por ejemplo, asistentes de IA o de código), para qué se usaron y en qué partes del proyecto. -->

## 8. Cambios en vivo: qué tocar

Todos los valores ajustables son constantes con nombre al inicio de su archivo. Después de editar, volvé a ejecutar `mvn compile exec:exec`: los shaders se copian al classpath en `compile` y se cargan al arrancar.

### Ciudad y juego (Java)

| Cambio | Archivo | Constante o dato |
|---|---|---|
| Cantidad y reparto de farolas | `iluminacion/Iluminacion.java` | `FAROLAS_POR_SECTOR` ({3, 3, 3, 2, 2}: Centro, Barrio Norte, Parque Sur, Zona Oeste, Zona Este; la suma no puede pasar de `MAX_LUCES` = 16), `SEPARACION_SENALES` (3: del poste a un semáforo, PARE o cartel), `HOLGURA_POSTE_PASO` (0.3) y `HOLGURA_BOMBILLA_PASO` (0.35). `LUCES` (`{fila, columna, lado}`) no se escribe a mano: `farolaValida()` acepta el centro de un borde de manzana que da a la calle (en la vereda, a mitad de cuadra), dentro del sector, lejos de señales y fuera de los pasos; `calcularLuces()` elige, sector por sector, la candidata más alejada de las farolas ya ubicadas (`FarolasTest` lo verifica) |
| Ubicación de la bombilla (y de la luz) | `iluminacion/Iluminacion.java` | `BRAZO_FAROLA` (1.5), `MARGEN_POSTE` (0.4, del cordón al poste), `ALTURA_BOMBILLA` (4.5). Mueven la bombilla dibujada y la luz juntas |
| Forma de las farolas | `iluminacion/Iluminacion.java` | `ANCHO_BASE` (0.42), `ALTO_BASE` (0.45), `ANCHO_POSTE_ABAJO` (0.2), `ANCHO_POSTE_ARRIBA` (0.14), `GROSOR_BRAZO` (0.09), `PERFIL_BRAZO` (puntos {avance, altura} de los 3 tramos del brazo), `ANCHO_PANTALLA` (0.8), `ALTO_PANTALLA` (0.3), `SEPARACION_PANTALLA` (0.05), `DIAMETRO_BOMBILLA` (0.32) |
| Colores de las farolas | `iluminacion/Iluminacion.java` | `COLOR_BASE` (gris oscuro), `COLOR_POSTE` (gris metálico), `COLOR_PANTALLA` (casi negro), `COLOR_BOMBILLA_DIA` (gris claro), `COLOR_BOMBILLA_NOCHE` (blanco cálido, emisivo) |
| Agregar un parque o un edificio | `mundo/Mapa.java` | `MAPA`: una celda con fila y columna impares a `2` (parque) o `1` (edificio) |
| Ampliar el mapa: solo `Mapa.MAPA` | `mundo/Mapa.java` | Agregar filas y columnas manteniendo la matriz cuadrada, impar, con calles en filas y columnas pares (por ejemplo 13 × 13). Se adapta solo: **límites** (`TAMANO` y `LIMITE` salen de `MAPA.length × TAM_CELDA`); **colisiones** (`Colisiones` recorre la matriz y usa `LIMITE`); **cámaras** (la aérea escala su vista, su distancia máxima y el desplazamiento con `LIMITE`; la de seguimiento se recorta contra las celdas y el borde); **minimapa** (`uMitadMapa` = `LIMITE` + margen); **plano lejano** (`Camara.getPlanoLejano()` = distancia aérea máxima + diagonal + margen, enviado como `uPlanoLejano`); **inicio** (`Auto` usa `Mapa.centro()` sobre la penúltima fila); **destinos** (`CELDAS_DESTINOS` usa la última fila y columna); **sectores** (`RADIO_CENTRO` es proporcional a `LIMITE`: 25 con 11 × 11, 35 con 13 × 13); **semáforos** (todos los cruces del Centro); **PARE** (los cruces sin semáforo más cercanos al Centro); **carteles** (en la entrada principal de cada sector); **farolas** (reglas de `farolaValida()` y reparto de `calcularLuces()`); también el suelo, el campo y los pasos peatonales. Los tests derivan sus valores de `Mapa`. Probado con 13 × 13: `mvn test` y `-Ddemo.frames=6` sin fallos |
| Velocidad y manejo del auto | `vehiculo/Auto.java` | `VELOCIDAD_MAX` (16), `VELOCIDAD_REVERSA` (6), `ACELERACION` (9), `RESISTENCIA` (0.7), `FRENO` (7), `VELOCIDAD_GIRO` (0.11) |
| Ruedas y dirección | `vehiculo/Auto.java` | `RADIO_RUEDA` (0.32), `ANCHO_RUEDA` (0.24), `FRACCION_LLANTA` (0.62), `ANGULO_MAX_DIRECCION` (30°), `VELOCIDAD_DIRECCION` (3 rad/s), `COLOR_NEUMATICO`, `COLOR_LLANTA`, `COLOR_MARCA_LLANTA` |
| Luces del auto: faros, posición y freno | `vehiculo/LucesVehiculo.java` (compartidas con el tráfico) | `COLOR_FARO_ENCENDIDO` (blanco cálido, emisivo), `COLOR_FARO_APAGADO` (gris-beige oscuro), `COLOR_POSICION` (rojo tenue, emisivo), `COLOR_FRENO` (rojo intenso, emisivo), `COLOR_TRASERA_APAGADA` (rojo oscuro), `LADO_LUZ` (0.55), `ALTURA_LUZ` (0.68), `FRENTE_LUZ` (-1.32), `TRASERA_LUZ` (1.32), `TAMANO_FARO`, `TAMANO_TRASERA` |
| Luz de reversa | `vehiculo/Auto.java` | `COLOR_REVERSA_APAGADA`, `COLOR_REVERSA` (blanca, emisiva), `UMBRAL_MOVIMIENTO` (0.1) |
| Cámara orbital del auto | `motor/Camara.java` | `ANGULO_INICIAL` (0 = detrás), `ELEVACION_INICIAL` (25°), `DISTANCIA_INICIAL` (9), `ELEVACION_MIN` / `ELEVACION_MAX` (5° / 85°), `DISTANCIA_MIN` / `DISTANCIA_MAX` (4 / 30), `PASO_ZOOM` (1) |
| Sensibilidad del mouse (orbital y aérea) | `motor/Camara.java` | `SENSIBILIDAD_GIRO` (0.008 rad/px), `SENSIBILIDAD_ELEVACION` (0.006 rad/px) |
| Punto de partida | `vehiculo/Auto.java` | `COLUMNA_SALIDA` (0) y `FILA_SALIDA` (penúltima fila, impar: mitad de cuadra); `X_INICIAL` y `Z_INICIAL` se calculan con `Mapa.centro()`; `CARRIL_SALIDA` (2.5, como el carril del tráfico) |
| Tiempos del semáforo | `mundo/Semaforo.java` | `DURACION_VERDE` (5), `DURACION_AMARILLO` (2); `DURACION_ROJO` se calcula como verde + amarillo (7) y `DESFASE_ESTE_OESTE` = rojo, para mantener la coordinación; `BRILLO_APAGADA` (0.15: brillo de las lentes apagadas) |
| Forma del semáforo | `mundo/Semaforo.java` | `ANCHO_BASE` (0.34), `ALTO_BASE` (0.35), `ANCHO_POSTE` (0.14), `ANCHO_CAJA` (0.42), `ALTO_CAJA` (1.3), `PROFUNDIDAD_CAJA` (0.34), `BASE_CAJA` (2.5), `DIAMETRO_LENTE` (0.3), `GROSOR_LENTE` (0.05), `SEPARACION_LENTES` (0.38; con la caja de 1.3 y lentes de 0.3, hasta 0.5), `LARGO_VISERA` (0.22), `GROSOR_VISERA` (0.025), `EXCESO_VISERA` (0.06), `HOLGURA_VISERA` (0.03), `INCLINACION_VISERA` (20°). `SemaforoTest` avisa si las lentes se salen de la caja |
| Colores del semáforo | `mundo/Semaforo.java` | `COLOR_BASE`, `COLOR_POSTE` (grises oscuros), `COLOR_CAJA` (casi negro, también las viseras), `COLORES_LENTE` (rojo, ámbar y verde encendidos; apagados = × `BRILLO_APAGADA`) |
| Dónde hay semáforos | `mundo/Senalizacion.java` | `SECTOR_SEMAFOROS` (0 = Centro): todas las intersecciones de ese sector forman `INTERSECCIONES_SEMAFORO`; `RETROCESO_SEMAFORO` (= `LARGO_PASO`), `MARGEN_VEREDA` (0.6) |
| Agregar o mover una entrega | `juego/Entregas.java` | `CELDAS_DESTINOS` `{fila, columna}` de calle (se convierten a `DESTINOS` con `Mapa.centro()`, como las rutas de `Trafico`) y su nombre en `NOMBRES_DESTINOS` |
| Qué tan cerca y lento hay que llegar | `juego/Entregas.java` | `RADIO_LLEGADA` (3), `VELOCIDAD_LLEGADA` (1) |
| Tamaño del minimapa | `juego/Minimapa.java` | `TAMANO_MAX_MINIMAPA` (260 px), `MARGEN_MINIMAPA` (18 px), `BORDE_MINIMAPA` (3 px), `MARGEN_MAPA` (zoom), `ESCALA_INDICADOR` (1.5) |
| Cámara de seguimiento | `motor/Camara.java` | `DISTANCIA_SEGUIMIENTO` (12), `ALTURA_SEGUIMIENTO` (9), `ALTURA_OBJETIVO` (0.8). Recorte: `DISTANCIA_SEGUIMIENTO_MIN` (1.5), `MARGEN_BORDE_CAMARA` (1: cuánto adentro del borde queda la cámara), `PASO_RECORTE` (0.25), `VELOCIDAD_ALEJAMIENTO` (12 unidades/s para volver a la distancia normal; acercarse es inmediato) |
| Vista inicial de la cámara aérea | `motor/Camara.java` | `ANGULO_AEREO_INICIAL` (0.6 rad), `FACTOR_RADIO` (1.86), `FACTOR_ALTURA` (1.57), proporcionales a `Mapa.LIMITE`: la cámara arranca a 1.86 límites del centro sobre el suelo y 1.57 de altura (≈ 40° de elevación y 134 de distancia) |
| Mouse en la cámara aérea | `motor/Camara.java` | `ELEVACION_AEREA_MIN` / `ELEVACION_AEREA_MAX` (20° / 85°), `DISTANCIA_AEREA_MIN` (20), `FACTOR_DISTANCIA_AEREA_MAX` (2.6: la distancia máxima es 2.6 · `Mapa.LIMITE` = 143, algo más que la vista inicial; el plano lejano se ajusta solo), `PASO_ZOOM_AEREO` (6 por paso de ruedita), `SENSIBILIDAD_DESPLAZAMIENTO` (0.0015 por píxel y por unidad de distancia: lejos, el botón derecho mueve más rápido) |
| Rutas del tráfico | `trafico/Trafico.java` | `RUTAS_CELDAS`: listas cíclicas de cruces `{fila, columna}` (pares); cada tramo debe ir en línea recta por calle, o el juego se detiene al arrancar con un mensaje |
| Velocidad y color de los vehículos | `trafico/Trafico.java` | `VELOCIDADES` (7, 6, 8, 6.5), `COLORES` |
| Manejo del tráfico | `trafico/Vehiculo.java` | `VELOCIDAD_GIRO` (2.2 rad/s), `RADIO_WAYPOINT` (2), `FRACCION_MINIMA_CURVA` (0.3), `DISTANCIA_PRECAUCION` (6), `DISTANCIA_ANTICIPACION` (4) |
| Luces del tráfico (shader) | `iluminacion.frag` | `INTENSIDAD_FARO_TRAFICO` (4.0, la mitad que el jugador), `ALCANCE_FARO_TRAFICO` (4.0: a esa distancia el foco rinde la mitad), `MAX_FAROS_TRAFICO` (16) |
| Máximo de focos (Java) | `trafico/Trafico.java` | `MAX_FAROS_TRAFICO` (16): debe ser igual al del shader; `FAROS_POR_VEHICULO` (2) |
| Luces del tráfico | `vehiculo/LucesVehiculo.java` | Las mismas constantes que el auto del jugador; de noche el tráfico muestra faros encendidos y luces de posición |
| Carril del tráfico | `trafico/Vehiculo.java` | `DESPLAZAMIENTO_CARRIL` (`TAM_CELDA / 4` = 2.5): distancia del centro del carril derecho a la línea amarilla. Con más de ≈ 3.3 el vehículo tocaría la vereda (`TraficoTest` lo detectaría) |
| Flecha sobre el auto (vista aérea) | `vehiculo/IndicadorJugador.java` | `ALTURA_INDICADOR` (6), `TAMANO_INDICADOR` (2.5), `COLOR_INDICADOR` (cian), `AMPLITUD_OSCILACION` (0.6), `FRECUENCIA_OSCILACION` (3), `VELOCIDAD_ROTACION` (1.2) |
| Sectores | `mundo/Mapa.java` | `NOMBRES_SECTORES`, `SECTORES` (rectángulos `{xMin, xMax, zMin, zMax}`), `FRACCION_CENTRO` (5/11): `RADIO_CENTRO` = `radioDelCentro(FRACCION_CENTRO · LIMITE)`, el borde válido más cercano, con una manzana adentro y una calle afuera (25 con 11 × 11) |
| Tamaño y estilo del HUD | `interfaz/Hud.java` | `ALTO_REFERENCIA` (380: escala 2 con 760 px de alto), `ESCALA_MINIMA` (1), `ESCALA_MAXIMA` (3), `MARGEN` (12), `RELLENO` (8), `ALTO_LINEA` (11), `ALFA_PANEL` (0.55), `ALFA_MENU` (0.7), `AYUDA` |
| PARE | `mundo/Senalizacion.java` | `MAX_PARE` (10); `UBICACIONES_PARE` (`{fila, columna, dFila, dColumna}`) lo calcula `calcularUbicacionesPare()`; `LADO_PARE` (0.9), `BORDE_PARE` (0.07), `ALTURA_POSTE` (2.6) |
| Carteles de sector | `mundo/Senalizacion.java` | `RETROCESO_CARTEL` (0.75, de la esquina al cartel), `SEPARACION_CARTEL` (1.5, del cordón al cartel); `CARTELES_SECTOR` (`{sector, x, z, ángulo}`) lo calcula `cartelDeSector()`; `ANCHO_CARTEL` (2.6), `ALTO_CARTEL` (0.8), `BORDE_CARTEL` (0.08), `ALTURA_CARTEL` (2.4), `COLOR_CARTEL` |
| Pasos peatonales | `mundo/Decoracion.java` | `LARGO_PASO` (3, en el sentido de circulación), `FRANJAS_PASO` (6), `SEPARACION_FRANJAS` (1.65), `ANCHO_FRANJA` (0.9), `ALTURA_FRANJA` (0.03), `GROSOR_FRANJA` (0.02). `UBICACIONES_PASOS` se genera en `calcularUbicaciones()` desde `INTERSECCIONES_SEMAFORO` y `Mapa.parques()`. Mantener `(FRANJAS_PASO - 1) · SEPARACION_FRANJAS + ANCHO_FRANJA` ≤ 10, o `PasosPeatonalesTest` avisa que la pintura invade la vereda |
| Colores de paredes y techos | `mundo/Edificio.java` | `PALETA_FACHADAS` (ladrillo, crema, blanco hueso, gris cemento, terracota, verde agua, amarillo pálido), `PALETA_TECHOS` (gris oscuro, gris claro, teja, verde), `TECHO_TEJA` (índice del techo de las casas), `COLOR_METAL`, `COLOR_TANQUE` |
| Qué tipo sale en cada manzana | `mundo/Edificio.java` | `SEMILLA_TIPO` (167): cambiarla sortea otra ciudad (siempre la misma para cada semilla). `EdificioTest` avisa si quedan menos de 4 tipos |
| Torre | `mundo/Edificio.java` | `ALTURA_TORRE_MIN` / `ALTURA_TORRE_MAX` (16 / 22), `ANCHO_TORRE` (4.4), `ALTURA_PODIO` (3.2), `PROBABILIDAD_ANTENA` (0.5), `ALTO_ANTENA` (3.5), `GROSOR_ANTENA` (0.12), `DIAMETRO_TANQUE` (1.6), `ALTO_TANQUE` (1.3), `ALTO_PATAS` (0.8) |
| Bloque | `mundo/Edificio.java` | `ALTURA_BLOQUE_MIN` / `ALTURA_BLOQUE_MAX` (7 / 11), `ALTO_BARANDA` (0.5), `GROSOR_BARANDA` (0.1), `ANCHO_ASCENSOR` (2), `ALTO_ASCENSOR` (1.8), `CORRIMIENTO_ASCENSOR` (1.3) |
| Escalonado | `mundo/Edificio.java` | `ANCHOS_NIVELES` (7, 5.2, 3.6), `NIVELES_MIN` / `NIVELES_MAX` (2 / 3), `ALTURA_NIVEL_MIN` / `ALTURA_NIVEL_MAX` (4 / 5) |
| Casa baja | `mundo/Edificio.java` | `PISOS_CASA_MIN` / `PISOS_CASA_MAX` (1 / 2), `ALTO_PLANTA_CASA` (3.2), `ALTO_PISO_CASA` (2), `ALTO_TECHO_CASA` (2), `LADO_CHIMENEA` (0.5), `ALTO_CHIMENEA` (1.8) |
| Doble | `mundo/Edificio.java` | `ANCHO_PARTE_ALTA` (4 de los 7), `ALTURA_DOBLE_ALTA_MIN` / `_MAX` (9 / 13), `ALTURA_DOBLE_BAJA_MIN` / `_MAX` (4 / 6) |
| Huella y losas | `mundo/Edificio.java` | `VUELO_CORNISA` (0.15; `MEDIA_HUELLA` = 3.5 + vuelo debe quedar < 5 o `EdificioTest` avisa), `GROSOR_LOSA` (0.3), `ALTO_MINIMO_BASE` (3.2: la planta baja con su toldo) |
| Ventanas según el tipo | `mundo/TipoEdificio.java` | Los cinco números de cada tipo: columnas por cara, separación, ancho, alto y altura de piso (por ejemplo `TORRE(4, 1.0f, 0.6f, 0.8f, 1.6f)`) |
| Dónde van las ventanas | `mundo/Fachada.java` | `PRIMER_PISO_Y` (1.7), `MARGEN_VERTICAL` (0.35), `MARGEN_LATERAL` (0.3), `TOPE_PLANTA_BAJA` (2.8) |
| Ventanas de noche | `mundo/Fachada.java` | `PORCENTAJE_VENTANAS_ENCENDIDAS` (0.65), `TONOS_VENTANA` (amarillo cálido, blanco cálido, anaranjado, blanco frío), `PESOS_TONOS` (0.4, 0.3, 0.2, 0.1), `COLOR_VENTANA_APAGADA` |
| Ventanas de día | `mundo/Fachada.java` | `COLOR_VIDRIO_DIA` (blanco-celeste grisáceo, sin emisión) |
| Toldos | `mundo/Fachada.java` | `COLORES_TOLDO` (rojo, verde, azul, naranja), `PROBABILIDAD_RAYAS` (0.35), `ANCHO_RAYA` (0.6), `EXCESO_TOLDO` (0.05 a cada lado de la vidriera), `VUELO_TOLDO` (0.7; mantener ≤ 0.8 para no tocar los postes de semáforo), `ALTURA_TOLDO` (2.45), `CAIDA_TOLDO` (0.3), `ESCALONES_TOLDO` (3) |
| Puerta y vidrieras | `mundo/Fachada.java` | `ANCHO_PUERTA` (1, siempre centrada), `ALTO_PUERTA` (2), `SEPARACION_PUERTA` (0.2), `MARGEN_ESQUINA` (0.4); `ANCHO_VIDRIERA` se calcula como 3.5 − margen − media puerta − separación (2.4) y `CENTRO_VIDRIERA` (1.9); `ALTO_VIDRIERA` (1.4), `COLOR_VIDRIERA_DIA`, `COLOR_VIDRIERA_NOCHE_ABAJO` / `COLOR_VIDRIERA_NOCHE_ARRIBA` (degradado de noche), `FRANJAS_VIDRIERA` (6) |
| Resolución de las figuras | `motor/Figuras.java` | `SECTORES_ESFERA` (12), `ANILLOS_ESFERA` (8), `LADOS_CILINDRO` (10), `LADOS_CONO` (10), `PERFIL_PRISMA` (triángulo del techo a dos aguas): más lados = más redondo y más triángulos |
| Forma de la cabina | `vehiculo/Cabina.java` | `PERFIL_CABINA` (puntos {z, y} del contorno lateral; debe ser convexo), `ANCHO_CABINA` (1.40) |
| Vidrios de la cabina | `vehiculo/Cabina.java` | `COLOR_VIDRIO` (azul-gris oscuro), `SEPARACION_VIDRIO` (0.012), `GROSOR_VIDRIO` (0.02), `MARGEN_VIDRIO` (0.07), `BASE_VENTANILLA` (0.98), `TECHO_VENTANILLA` (1.33), `CENTRO_PARANTE` (0.20), `ANCHO_PARANTE` (0.12) |
| Color del auto del jugador | `vehiculo/Auto.java` | `COLOR_CARROCERIA` (rojo: carrocería y cabina) |
| Árboles de los parques | `mundo/Parque.java` | `ARBOLES_MIN` / `ARBOLES_MAX` (4 / 6), `COPA_MAXIMA` (1/4 de la celda = 2.5), `RADIO_CENTRO_LIBRE` (2.2), `LUGARES_ARBOL` (esquinas y bordes posibles), `SEPARACION_POSTES` (1.6) |
| Senderos, fuente y bancos | `mundo/Parque.java` | `ANCHO_SENDERO` (1.4), `GROSOR_SENDERO` (0.03), `COLOR_SENDERO`, `RADIO_FUENTE` (1.2), `COLOR_AGUA`, `COLOR_AGUA_NOCHE`, `BANCOS_MIN` / `BANCOS_MAX` (2 / 4), `DISTANCIA_BANCO` (2.3) |
| Divisiones de sectores en el minimapa | `juego/Minimapa.java` | `GROSOR_DIVISION` (0.7), `ALTURA_DIVISION` (22) |
| Campo alrededor de la ciudad | `mundo/Entorno.java` | `ENTORNO_EXTRA` (100: cuánto sigue el pasto más allá del borde; no cambia `Mapa.LIMITE`), `ALTURA_CAMPO` (0), `COLOR_CAMPO`, `ANCHO_CORDON` (1.2), `ALTO_CORDON` (0.3), `COLOR_CORDON` |
| Árboles del campo | `mundo/Entorno.java` | `CANTIDAD_ARBOLES` (36), `DISTANCIA_MIN_ARBOL` (6 desde el borde; debe superar cordón + media copa o `EntornoTest` avisa), `ANCHO_FRANJA_ARBOLES` (50), `SEMILLA_ARBOLES` (211: otra semilla, otra disposición, siempre la misma) |
| Colores del cielo | `iluminacion/Cielo.java` | `COLOR_CENIT_DIA` (azul intenso), `COLOR_HORIZONTE_DIA` (celeste claro), `COLOR_CENIT_NOCHE` (azul casi negro), `COLOR_HORIZONTE_NOCHE` (azul noche); `RADIO_CIELO` (250, menor que el plano lejano ≈ 319) |
| Estrellas y luna | `iluminacion/Cielo.java` | `CANTIDAD_ESTRELLAS` (160), `SEMILLA_ESTRELLAS` (97), `ALTURA_MIN_ESTRELLA` (0.08 ≈ 5° sobre el horizonte), `TAMANO_MIN_ESTRELLA` / `TAMANO_MAX_ESTRELLA` (0.5 / 1.3), `BRILLO_MIN_ESTRELLA` (0.55); `DIRECCION_LUNA` (norte-noreste, ≈ 19° de altura), `DIAMETRO_LUNA` (11), `COLOR_LUNA` |
| Sombras falsas | `iluminacion/Sombras.java` | `ALFA_SOMBRA_DIA` (0.45) / `ALFA_SOMBRA_NOCHE` (0.22, más tenue), `COLOR_SOMBRA`, `ELEVACION_SOMBRA` (0.06 sobre la superficie: más chico puede parpadear o quedar bajo la pintura vial), `NUCLEO_SOMBRA` (0.35: dónde empieza el difuminado), `FORMA_REDONDA` (2) / `FORMA_CUADRADA` (4) |
| Tamaño de cada sombra | `iluminacion/Sombras.java` | `ESCALA_SOMBRA_EDIFICIO` (1.4 · base de 7 = 9.8; mantener < 10/7 para no salir de la acera), `SOMBRA_AUTO` (2.3 × 3.5), `ESCALA_SOMBRA_ARBOL` (1.25 · copa), `SOMBRA_BANCO` (2.1 × 1.0) |

### Iluminación y proyección (shaders GLSL en `src/main/resources/shaders`)

| Cambio | Archivo | Constante (valor) |
|---|---|---|
| Luz ambiente de día y de noche | `iluminacion.frag` | `AMBIENTE_DIA` (0.48), `AMBIENTE_NOCHE` (0.12, 0.16, 0.24) |
| Intensidad y dirección del sol | `iluminacion.frag` | `INTENSIDAD_SOL_DIA` (0.65), `INTENSIDAD_SOL_NOCHE` (0.10), `DIRECCION_SOL` (0.4, 1.0, 0.3) |
| Alcance de las farolas | `iluminacion.frag` | `FAROLA_ATENUACION_LINEAL` (0.12), `FAROLA_ATENUACION_CUADRATICA` (0.045); más chico = más alcance |
| Color y fuerza de las farolas | `iluminacion.frag` | `COLOR_FAROLA` (1.0, 0.73, 0.34), `INTENSIDAD_FAROLA` (3.0) |
| Tamaño del círculo de luz de las farolas | `iluminacion.frag` | `ANGULO_FAROLA_INTERIOR` (35°, luz plena), `ANGULO_FAROLA_EXTERIOR` (60°, fin del borde suave); en grados desde la vertical. Más grande = círculo más ancho; el exterior debe quedar por debajo de 90° o la luz volvería a subir por las paredes. `FarolasTest` verifica que siga cubriendo vereda y calzada |
| Dirección del foco de las farolas | `iluminacion.frag` | `DIRECCION_FAROLA` (0, −1, 0): recto hacia abajo |
| Ángulo del cono de los faros | `iluminacion.frag` | `CONO_BORDE` (0.85 ≈ 32°), `CONO_CENTRO` (0.97 ≈ 14°); son cosenos, así que más cerca de 1 = cono más angosto |
| Alcance y fuerza de los faros | `iluminacion.frag` | `FAROS_ATENUACION_CUADRATICA` (0.04), `INTENSIDAD_FARO` (8.0), `COLOR_FARO` (1.0, 0.94, 0.72) |
| Posición e inclinación de los faros | `iluminacion.frag` | `FAROS_SEPARACION` (0.55), `FAROS_AVANCE` (1.36), `FAROS_INCLINACION` (0.10) |
| Campo visual de la cámara | `ciudad.vert` | `CAMPO_VISUAL` (55°) |
| Distancia de dibujo | `ciudad.vert` y `motor/Camara.java` | `PLANO_CERCANO` (0.1, en el shader). El plano lejano ya no es un número fijo: `Camara.getPlanoLejano()` = distancia aérea máxima + diagonal de la ciudad + `MARGEN_PLANO_LEJANO` (20) ≈ 319 con límite 55, y llega al shader como `uPlanoLejano`. `CamaraTest` lo verifica |
| Orden de alturas en el minimapa | `ciudad.vert` | `ESCALA_ALTURA_MAPA` (100) |
| Reparto del degradado del cielo | `iluminacion.frag` | `CURVA_CIELO` (0.6): exponente de la altura; más chico = el azul intenso baja más cerca del horizonte |

## 9. Guion de demostración

Sigue el orden del enunciado. Antes de empezar, conviene tener la ventana en tamaño normal (1100 × 760).

| Paso | Acción | Qué se ve |
|---|---|---|
| 0 | `mvn compile exec:exec` y **ENTER** | Menú de inicio con el título; al presionar ENTER, la partida arranca de día, con la cámara de seguimiento, el minimapa arriba a la derecha y el HUD arriba a la izquierda |
| 1 | **W** para acelerar, **A / D** para girar, **S** para frenar y retroceder, **Espacio** para el freno fuerte | El auto acelera y dobla. Velocidad en el título y en el HUD. El tráfico circula por su carril |
| 2 | Chocar contra una vereda o manzana, y acercarse a un vehículo del tráfico | El auto se detiene sin atravesar la manzana ni al otro vehículo. El vehículo del tráfico frena si el auto está adelante |
| 3 | **C** (tres veces) | Cámara orbital del auto (arrastrar con el mouse para girar y elevar, ruedita para acercar); después la vista aérea de toda la ciudad, rodeada de campo con árboles y con sombras suaves bajo edificios, árboles y autos, con la flecha cian sobre el auto: arrastrar con el botón izquierdo para girar alrededor de la ciudad, ruedita para acercar y botón derecho para recorrerla; al final vuelve la cámara de seguimiento |
| 3b | En la cámara orbital (**C** una vez), girar hasta ver el auto de costado y manejar: **W**, **A/D**, **S** y **Espacio** | Las ruedas giran (la marca roja da vueltas) y las delanteras doblan con A/D. Mirando la cola del auto: al frenar se encienden las luces de freno y al ir marcha atrás, la luz blanca de reversa. Funciona de día y de noche |
| 4 | **N** | Noche: cielo azul oscuro con estrellas y luna (se ven con la orbital baja), farolas, ventanas encendidas (≈65 %, tonos variados), vidrieras iluminadas, luces del tráfico con sus focos sobre la calle. El HUD y el título muestran "Noche" |
| 5 | **F** (y otra vez **F**) | Se apagan y encienden los faros del jugador: los conos sobre la calle, las bombillas delanteras (blancas y emisivas) y las luces de posición traseras. Frenando con S o Espacio, las traseras pasan a rojo intenso aunque F esté apagado. Las luces del tráfico no cambian |
| 6 | **M** (y otra vez **M**) | Se oculta y vuelve el minimapa, con la ciudad completa, el norte arriba, los sectores con nombre, el auto (cian con punta blanca) y el destino dorado |
| 7 | Redimensionar la ventana (achicar y agrandar) | La escena mantiene sus proporciones, el minimapa sigue en la esquina superior derecha y el HUD se adapta |
| 8 | Completar las 3 entregas: frenar sobre cada marca dorada | Recorrido sugerido. **1:** ir al norte por el borde oeste, y al este por el borde norte hasta la esquina noreste. **2:** volver al oeste por el borde norte, y al sur por el borde oeste hasta Z = -10. **3:** seguir al sur hasta el borde sur, y al este hasta X = 30. El título y el HUD muestran "Entregas: X/3" y el destino; al final aparece GANASTE con el tiempo |
| 9 | **R** y nuevo recorrido | Auto en la salida, entregas en 0/3, cronómetro en 0 y tráfico al inicio de sus rutas; se puede volver a jugar |
| Extra | **H** (ayuda) y **P** (pausa) | Lista de controles en pantalla; en pausa todo se congela y aparece "PAUSA" |

## 10. Capturas

Las capturas van en `docs/capturas/`. Si todavía no se tomaron, las imágenes de abajo aparecen como enlaces rotos.

| Captura | Qué mostrar |
|---|---|
| ![De día](docs/capturas/dia.png) | `dia.png`: cámara de seguimiento de día, con edificios de distintos tipos (una torre, una casa baja), toldos, tráfico y HUD |
| ![De noche](docs/capturas/noche.png) | `noche.png`: la misma zona de noche, con farolas, ventanas encendidas y luces del tráfico |
| ![Minimapa](docs/capturas/minimapa.png) | `minimapa.png`: minimapa con sectores, auto y destino (puede ser un recorte de la esquina superior derecha) |
| ![Parque](docs/capturas/parque.png) | `parque.png`: un parque con fuente, senderos, árboles y bancos |
