package com.graphics.ciudad.mundo; // Agrupa lo que forma la ciudad: mapa, edificios, decoración y señales.

import com.graphics.ciudad.iluminacion.Iluminacion; // Postes de farola: los árboles no deben taparlos.
import com.graphics.ciudad.motor.Cubo; // Senderos y bancos.
import com.graphics.ciudad.motor.Figuras; // Esfera, cilindro y cono para árboles y fuente.
import com.graphics.ciudad.motor.Shader; // Emisión del agua de noche.
import java.util.ArrayList; // Listas de árboles y bancos.
import java.util.Collections; // Publica LUMINARIAS sin permitir modificarla.
import java.util.List; // Tipo de esas listas.

/**
 * PARQUE: contenido de una celda de parque.
 * Responsable de: calcular (una vez, sin azar por cuadro) dónde van los árboles y los bancos de cada parque, y dibujar
 * senderos en cruz, una plaza octogonal alrededor de la fuente central, franjas de acceso donde llegan los pasos
 * peatonales, 4 a 6 árboles (frondosos y pinos), 2 a 4 bancos que miran a la fuente desde el borde de la plaza y 1 o 2
 * luminarias peatonales tipo GLOBO en el borde de los senderos (luces puntuales: ver luminarias()).
 * RECORRIDO PEATONAL (ver sección 3a): senderos, plaza y franjas forman un solo PAVIMENTO conectado. Un peatón que
 * cruza la calle por un paso pisa una franja, sigue por ella hasta un brazo de la cruz, llega a la plaza y la rodea
 * hasta cualquier otro brazo sin pisar césped. Nada se apoya sobre la plaza ni las franjas: árboles, bancos,
 * luminarias y basureros quedan afuera (las luminarias van en el borde de los brazos, como antes).
 * VARIACIÓN DETERMINÍSTICA: cada parque usa su fila y columna para "sortear" disposición, tipo de árbol, altura, tamaño
 * de copa y tono de verde con variacion(), una función que siempre da el mismo número para los mismos datos. Así los
 * parques son distintos entre sí, pero cada uno se ve igual en todos los cuadros y en cada ejecución.
 * ESCALA (1 u ≈ 1 m): el auto mide 1.4 de alto; los frondosos miden de 5 a 7 y los pinos de 5 a 6, como árboles de
 * vereda reales. Crecen hacia ARRIBA (tronco más alto y copa estirada), no hacia los costados: el ancho de la copa
 * sigue sin superar un cuarto del parque (COPA_MAXIMA = 2.5), así no sale del césped.
 * Todo queda dentro de la celda del parque: nada invade la calle ni participa en colisiones (Colisiones ya bloquea la
 * manzana entera). Los árboles evitan los postes de semáforos, PARE, carteles y farolas que están en la acera del
 * parque: con la copa a la altura de esas señales, el tronco se aleja lo suficiente para que la copa no las toque.
 * Se comunica con: Decoracion (lo llama para cada celda de parque), Figuras y Cubo (dibujo), Shader (emisión),
 * Mapa (centro de la celda), Senalizacion e Iluminacion (postes que hay que esquivar). Entorno reutiliza dibujarArbol()
 * para los árboles del campo, y Sombras lee arboles() y bancos() para ubicar sus manchas.
 */
public class Parque {

    // ==================== 1. CONSTANTES (valores ajustables) ====================
    public static final float TOPE_CESPED = 0.37f; // Altura de la cara superior del césped que dibuja Ciudad (0.32 + 0.05).
    public static final float MITAD_CESPED = 4.5f; // El césped mide 9 × 9: 4.5 desde el centro hacia cada lado.
    public static final float ANCHO_SENDERO = 1.4f; // Ancho de cada sendero de la cruz.
    public static final float GROSOR_SENDERO = 0.03f; // Espesor: el sendero apenas sobresale del césped (sin z-fighting).
    public static final float[] COLOR_SENDERO = {0.80f, 0.72f, 0.54f}; // Beige, como tierra apisonada.
    public static final float COPA_MAXIMA = Mapa.TAM_CELDA / 4; // Diámetro máximo de una copa: 1/4 del ancho del parque.
    public static final float RADIO_CENTRO_LIBRE = 2.2f; // Ningún árbol a menos de esto del centro: allí está la fuente.
    public static final int ARBOLES_MIN = 4; // Menor cantidad de árboles por parque.
    public static final int ARBOLES_MAX = 6; // Mayor cantidad de árboles por parque.
    public static final int BANCOS_MIN = 2; // Menor cantidad de bancos por parque.
    public static final int BANCOS_MAX = 4; // Mayor cantidad de bancos por parque.
    // Árboles altos: la copa (de 2 a 7 de altura) está a la altura de semáforos, PARE, carteles y farolas. Un árbol se
    // planta solo si su copa más ancha posible (COPA_MAXIMA / 2) queda a MARGEN_POSTES de la mitad de cada señal.
    public static final float MARGEN_POSTES = 0.2f; // Aire entre el borde de la copa y la señal más cercana.
    public static final float MITAD_SEMAFORO = 0.5f; // Media extensión del cabezal del semáforo (caja, lentes y viseras).
    public static final float MITAD_PARE = Senalizacion.LADO_PARE / 2; // Medio octógono del PARE.
    public static final float MITAD_CARTEL = Senalizacion.ANCHO_CARTEL / 2; // Media placa del cartel de sector.
    public static final float MITAD_FAROLA = Iluminacion.ANCHO_BASE / 2; // Media base de la farola (el brazo va hacia la calle).
    // Altura total de los árboles (del césped a la punta) y qué parte es tronco.
    public static final float ALTURA_FRONDOSO_MIN = 5; // Frondoso: de 5 a 7.
    public static final float ALTURA_FRONDOSO_MAX = 7;
    public static final float ALTURA_PINO_MIN = 5; // Pino: de 5 a 6.
    public static final float ALTURA_PINO_MAX = 6;
    public static final float FRACCION_TRONCO_FRONDOSO = 0.4f; // 2 a 2.8 de tronco: la copa empieza por encima de una persona.
    public static final float FRACCION_TRONCO_PINO = 0.2f; // Los pinos tienen el tronco corto y los conos casi hasta abajo.
    public static final float RADIO_FUENTE = 1.2f; // Radio de la base de la fuente.
    public static final float[] COLOR_AGUA = {0.25f, 0.50f, 0.85f}; // Agua de día (recibe luz).
    public static final float[] COLOR_AGUA_NOCHE = {0.12f, 0.30f, 0.55f}; // Agua de noche: emisiva pero oscura, "levemente" brillante.
    // Lugares posibles para árboles, relativos al centro del parque: 4 esquinas y 8 puntos de borde a los lados de los
    // senderos (el centro queda libre para la fuente). Cada parque recorre esta lista en un orden propio.
    public static final float[][] LUGARES_ARBOL = {
        {3.1f, 3.1f}, {-3.1f, 3.1f}, {-3.1f, -3.1f}, {3.1f, -3.1f}, // Esquinas.
        {3.3f, 1.9f}, {3.3f, -1.9f}, {-3.3f, 1.9f}, {-3.3f, -1.9f}, // Bordes este y oeste, a los lados del sendero.
        {1.9f, 3.3f}, {-1.9f, 3.3f}, {1.9f, -3.3f}, {-1.9f, -3.3f} // Bordes norte y sur, a los lados del sendero.
    };
    public static final int FRONDOSO = 0; // Tipo de árbol: tronco fino y copa de esferas.
    public static final int PINO = 1; // Tipo de árbol: tronco y conos apilados.
    // Formato de un árbol: {x, z, tipo, alturaTronco, diametroCopa, verde, giro, altoCopa}. altoCopa es cuánto sube la
    // copa por encima del tronco: la altura total es alturaTronco + altoCopa.
    public static final int ALTO_COPA = 7; // Índice de altoCopa en el arreglo.
    public static final float MITAD_BANCO = 0.8f; // Medio largo del banco (mide 1.6): radio del círculo que lo contiene.
    public static final float MITAD_FONDO_BANCO = 0.25f; // Medio fondo del asiento (mide 0.5): del centro del banco a su frente.
    public static final float FONDO_RESPALDO = 0.28f; // Del centro del banco a la cara de atrás del respaldo (0.24 + 0.08 / 2).

    // ---- Recorrido peatonal: plaza y franjas de acceso (valores ajustables) ----
    // PLAZA: octógono pavimentado alrededor de la fuente, a la altura del sendero. Es la unión de dos cuadrados iguales,
    // uno recto y otro girado 45°: juntos forman un octógono regular cuya APOTEMA (distancia del centro a cada lado) es
    // MITAD_PLAZA. Entre la fuente y cualquier lado queda al menos PASO_FUENTE para caminar, así que los cuatro brazos se
    // conectan rodeando la fuente sin pisar césped. Es octógono y no círculo porque el cilindro de Figuras tiene solo
    // 10 lados (un círculo "de verdad" no saldría); con cajas el octógono sale exacto, y sus lados diagonales quedan
    // paralelos al frente de los bancos.
    public static final float PASO_FUENTE = 1.0f; // Ancho mínimo del anillo pavimentado entre la fuente y el césped.
    public static final float MITAD_PLAZA = RADIO_FUENTE + PASO_FUENTE; // Apotema del octógono: 1.2 + 1.0 = 2.2.
    // FRANJA DE ACCESO: cada paso peatonal llega al borde del parque a 3 del eje del brazo más cercano (el paso está
    // pegado a su cruce, el brazo en el medio de la cuadra). La franja pavimenta el pie de ese borde, desde el brazo
    // hasta el final del paso, con PROFUNDIDAD_FRANJA hacia adentro: el peatón baja del paso y camina sobre pavimento
    // hasta el sendero. Se calculan desde Decoracion.UBICACIONES_PASOS, así que siguen solas al Mapa (ver franjas()).
    public static final float PROFUNDIDAD_FRANJA = 0.9f; // Del borde del césped hacia adentro; los troncos de borde quedan a 3.47 del centro, antes de 3.6.
    // BANCOS: miran a la fuente desde el borde de la plaza, sobre el césped. DISTANCIA_BANCO se deduce de la plaza: las
    // esquinas delanteras del banco (a MITAD_BANCO del centro del frente, a lo largo del lado diagonal) quedan
    // MARGEN_BANCO_PLAZA afuera de los lados rectos del octógono. En un punto a 45°, x = distancia / √2; despejando:
    // √2 · (MITAD_PLAZA + margen) = (distancia − MITAD_FONDO_BANCO) + MITAD_BANCO.
    public static final float MARGEN_BANCO_PLAZA = 0.05f; // Aire entre el banco y el borde de la plaza.
    public static final float DISTANCIA_BANCO =
        (float) Math.sqrt(2) * (MITAD_PLAZA + MARGEN_BANCO_PLAZA) - MITAD_BANCO + MITAD_FONDO_BANCO; // ≈ 2.63 del centro.

    // ---- Luminarias peatonales tipo GLOBO (valores ajustables) ----
    // Poste bajo con una esfera lechosa arriba, a escala de peatón: más baja y más simple que la farola de calle.
    // Como el globo no tiene pantalla, su luz sale hacia TODOS lados: en el shader es una LUZ PUNTUAL (uGlobos).
    public static final float ALTURA_GLOBO = 3.0f; // Centro del globo sobre el césped: la punta queda a 3.225 de alto.
    public static final float DIAMETRO_GLOBO = 0.45f; // Esfera del globo.
    public static final float ANCHO_BASE_GLOBO = 0.24f; // Diámetro de la base (zócalo) del poste.
    public static final float ALTO_BASE_GLOBO = 0.3f; // Alto de esa base.
    public static final float ANCHO_POSTE_GLOBO = 0.1f; // Diámetro del poste: fino, de parque.
    public static final float ANCHO_ANILLO_GLOBO = 0.2f; // Portaglobo: anillo sobre el que se apoya la esfera.
    public static final float ALTO_ANILLO_GLOBO = 0.1f; // Alto del portaglobo.
    public static final float[] COLOR_POSTE_GLOBO = {0.12f, 0.15f, 0.13f}; // Verde casi negro, típico de parque.
    public static final float[] COLOR_GLOBO_DIA = {0.86f, 0.87f, 0.85f}; // Vidrio lechoso apagado (recibe luz).
    public static final float[] COLOR_GLOBO_NOCHE = {1.0f, 0.93f, 0.78f}; // Encendido: emisivo, blanco cálido.
    // UBICACIÓN: sobre el BORDE de un sendero (no en el medio del camino), a DESVIO_LUMINARIA del eje del sendero y a
    // una de las DISTANCIAS_LUMINARIA del centro del parque. Son 4 brazos × 3 distancias × 2 lados = 24 candidatas.
    public static final float DESVIO_LUMINARIA = 0.5f; // Del eje del sendero al poste: la base (0.24) no sale del sendero (1.4).
    public static final float[] DISTANCIAS_LUMINARIA = {2.4f, 3.0f, 3.6f}; // Del centro del parque: pasada la fuente y antes de la acera.
    public static final float MARGEN_LUMINARIA = 0.15f; // Aire mínimo entre el globo o la base y un árbol o un banco.
    public static final float MARGEN_LUMINARIA_POSTES = 0.5f; // Aire entre el globo y un semáforo, PARE, cartel o farola de la acera.
    public static final int LUMINARIAS_MAX_POR_PARQUE = 2; // Tope por parque; baja a 1 si no entran en uGlobos (ver luminariasPorParque()).
    /** Centro {x, y, z} del globo de cada luminaria de todos los parques: lo que Iluminacion envía como uGlobos. */
    public static final List<float[]> LUMINARIAS = Collections.unmodifiableList(calcularLuminarias());

    // ==================== 2. VARIACIÓN DETERMINÍSTICA ====================

    /**
     * Número "aleatorio" entre 0 y 1 que depende solo de sus datos: fracción de sen(combinación) · 43758.5453.
     * Es la misma función de hash que se usa en muchos shaders: sin estado, sin Random, siempre igual.
     */
    public static float variacion(int fila, int columna, int indice, int semilla) {
        return Variacion.valor(fila, columna, indice, semilla); // El hash vive en Variacion, compartido con Fachada.
    }

    /** Número entero entre minimo y maximo (incluidos) elegido con variacion(). */
    private static int entero(int fila, int columna, int semilla, int minimo, int maximo) {
        return minimo + (int) (variacion(fila, columna, 0, semilla) * (maximo - minimo + 1)); // Reparte el intervalo en partes iguales.
    }

    // ==================== 3. DISPOSICIÓN DE ÁRBOLES Y BANCOS ====================

    /**
     * Árboles de un parque: cada uno es {x, z, tipo, alturaTronco, diametroCopa, verde, giro} en coordenadas del mundo.
     * Recorre LUGARES_ARBOL desde un punto de partida propio del parque y toma los primeros que no queden cerca de
     * un poste; la cantidad (4 a 6), el tipo y las medidas salen de variacion().
     */
    public static List<float[]> arboles(int fila, int columna) {
        List<float[]> lista = new ArrayList<>(); // Resultado.
        float cx = Mapa.centro(columna); // Centro del parque en X.
        float cz = Mapa.centro(fila); // Centro del parque en Z.
        int cantidad = entero(fila, columna, 1, ARBOLES_MIN, ARBOLES_MAX); // 4, 5 o 6 árboles.
        int inicio = entero(fila, columna, 2, 0, LUGARES_ARBOL.length - 1); // Por dónde empieza a recorrer los lugares.
        float proporcionPinos = variacion(fila, columna, 0, 3); // Algunos parques tienen más pinos y otros más frondosos.
        for (int k = 0; k < LUGARES_ARBOL.length && lista.size() < cantidad; k++) { // Recorre los lugares posibles.
            float[] lugar = LUGARES_ARBOL[(inicio + k * 5) % LUGARES_ARBOL.length]; // Salta de a 5: mezcla esquinas y bordes.
            float x = cx + lugar[0]; // Posición del tronco en X.
            float z = cz + lugar[1]; // Posición del tronco en Z.
            if (cercaDeUnPoste(x, z, COPA_MAXIMA / 2 + MARGEN_POSTES)) { // Semáforo, PARE o farola en la acera cercana.
                continue; // Ese lugar se saltea.
            }
            int i = lista.size(); // Índice del árbol: distingue su variación de la de los demás.
            int tipo = variacion(fila, columna, i, 4) < proporcionPinos ? PINO : FRONDOSO; // Mezcla de tipos.
            float verde = variacion(fila, columna, i, 7); // Tono de verde (0 = claro, 1 = oscuro).
            float giro = (float) (2 * Math.PI * variacion(fila, columna, i, 8)); // Orientación de la copa: rompe la simetría.
            float vAltura = variacion(fila, columna, i, 5); // Altura dentro del rango del tipo.
            float vCopa = variacion(fila, columna, i, 6); // Ancho de la copa.
            float[] arbol = arbol(x, z, tipo, vAltura, vCopa, verde, giro);
            if (tipo == PINO && pinoEstorba(fila, columna, arbol)) { // Ramas bajas sobre el pavimento o sobre un basurero.
                arbol = arbol(x, z, FRONDOSO, vAltura, vCopa, verde, giro); // Mismo lugar, pero con la copa arriba de 2.
            }
            lista.add(arbol); // Guarda el árbol.
        }
        return lista; // Árboles del parque.
    }

    /**
     * Indica si un PINO en ese lugar molestaría al peatón. El pino tiene ramas casi hasta el suelo (su copa empieza a
     * 1-1.2 de altura), así que su copa cuenta como ocupada a la altura de una persona: no puede quedar encima de un
     * sendero, la plaza o una franja, ni encima del lugar del basurero de un banco. En esos casos el árbol pasa a ser
     * FRONDOSO, cuya copa empieza por encima de 2 (una persona pasa por debajo). No se saltea el lugar: así la cantidad de
     * árboles no cambia.
     */
    private static boolean pinoEstorba(int fila, int columna, float[] pino) {
        float radioCopa = pino[4] / 2; // El cono de abajo es el más ancho.
        if (tocaPavimento(fila, columna, pino[0], pino[1], radioCopa)) {
            return true; // Ramas sobre el camino.
        }
        int lado = ladoBasureros(fila, columna); // Mismo lado que usará Basureros.
        for (float[] banco : bancos(fila, columna)) {
            float[] cesto = lugarBasurero(banco, lado);
            if (Math.hypot(pino[0] - cesto[0], pino[1] - cesto[1]) < radioCopa + Basureros.RADIO) {
                return true; // El basurero quedaría bajo las ramas (Basureros lo descartaría).
            }
        }
        return false;
    }

    /**
     * Árbol {x, z, tipo, alturaTronco, diametroCopa, verde, giro, altoCopa} con sus medidas: vAltura y vCopa (entre 0
     * y 1) eligen la altura total y el ancho de la copa dentro de sus rangos. Lo usan los parques y el campo (Entorno),
     * así los dos tienen árboles de la misma escala.
     */
    public static float[] arbol(float x, float z, int tipo, float vAltura, float vCopa, float verde, float giro) {
        boolean pino = tipo == PINO;
        float minimo = pino ? ALTURA_PINO_MIN : ALTURA_FRONDOSO_MIN; // Rango de altura según el tipo.
        float maximo = pino ? ALTURA_PINO_MAX : ALTURA_FRONDOSO_MAX;
        float altura = minimo + (maximo - minimo) * vAltura; // Altura total del árbol.
        float alturaTronco = altura * (pino ? FRACCION_TRONCO_PINO : FRACCION_TRONCO_FRONDOSO); // Parte que es tronco.
        float diametroCopa = 1.8f + (COPA_MAXIMA - 0.1f - 1.8f) * vCopa; // Entre 1.8 y 2.4: nunca más de 1/4 del parque.
        return new float[] {x, z, tipo, alturaTronco, diametroCopa, verde, giro, altura - alturaTronco}; // El resto es copa.
    }

    /**
     * Indica si un objeto de radio "copa" (ya con su aire) centrado en (x, z) tocaría un semáforo, un PARE, un cartel de
     * sector o una farola. Lo usan los árboles (radio de la copa más ancha), las luminarias y los basureros.
     */
    public static boolean cercaDeUnPoste(float x, float z, float copa) {
        for (float[] s : Senalizacion.SEMAFOROS) { // Semáforos del Centro.
            if (Math.hypot(x - s[0], z - s[1]) < copa + MITAD_SEMAFORO) { // La copa tocaría el cabezal.
                return true; // Se descarta el lugar.
            }
        }
        for (float[] p : Senalizacion.PARES) { // Señales de PARE.
            if (Math.hypot(x - p[0], z - p[1]) < copa + MITAD_PARE) {
                return true;
            }
        }
        for (float[] c : Senalizacion.CARTELES_SECTOR) { // Carteles de sector {sector, x, z, ángulo}.
            if (Math.hypot(x - c[1], z - c[2]) < copa + MITAD_CARTEL) {
                return true;
            }
        }
        for (float[] poste : Iluminacion.POSTES) { // Postes de farola.
            if (Math.hypot(x - poste[0], z - poste[1]) < copa + MITAD_FAROLA) {
                return true;
            }
        }
        return false; // El lugar está libre.
    }

    /**
     * Bancos de un parque: cada uno es {x, z, angulo}. Van en las diagonales, entre dos senderos, a DISTANCIA_BANCO del
     * centro: sobre el césped, con el frente paralelo al lado diagonal de la plaza y apenas afuera de ella. Miran hacia
     * la fuente: el frente (-Z local) apunta al centro, con ángulo atan2(dx, dz) (convención de Auto).
     */
    public static List<float[]> bancos(int fila, int columna) {
        List<float[]> lista = new ArrayList<>(); // Resultado.
        int cantidad = entero(fila, columna, 9, BANCOS_MIN, BANCOS_MAX); // 2, 3 o 4 bancos.
        int inicio = entero(fila, columna, 10, 0, 3); // Diagonal por la que se empieza.
        float d = DISTANCIA_BANCO / (float) Math.sqrt(2); // Componentes X y Z de un punto a 45°.
        float[][] diagonales = {{d, d}, {-d, d}, {-d, -d}, {d, -d}}; // Las cuatro diagonales, entre los brazos de la cruz.
        for (int k = 0; k < cantidad; k++) { // Toma las primeras "cantidad" diagonales desde el inicio.
            float[] rel = diagonales[(inicio + k) % 4]; // Posición relativa al centro.
            float angulo = (float) Math.atan2(rel[0], rel[1]); // El frente mira hacia (-rel): hacia la fuente.
            lista.add(new float[] {Mapa.centro(columna) + rel[0], Mapa.centro(fila) + rel[1], angulo}); // Guarda el banco.
        }
        return lista; // Bancos del parque.
    }

    /**
     * Costado del banco (1 o -1, en X local) donde va su basurero. Se sortea UNA VEZ POR PARQUE, no por banco: como la X
     * local gira con cada banco, todos los basureros quedan "girados" en el mismo sentido alrededor de la fuente y cada
     * uno junto a un brazo distinto. Si se sorteara por banco, dos bancos vecinos podrían elegir el mismo brazo y sus
     * cestos quedarían a menos de Basureros.DISTANCIA_MIN_ENTRE_BASUREROS: uno de los dos bancos se quedaría sin cesto.
     */
    public static int ladoBasureros(int fila, int columna) {
        return variacion(fila, columna, 0, Basureros.SEMILLA_LADO_BANCO) < 0.5f ? 1 : -1;
    }

    /**
     * Lugar {x, z} del basurero de un banco {x, z, angulo}, del costado "lado": al lado de la punta del banco
     * (MITAD_BANCO + SEPARACION_BANCO + radio del cesto) y RETIRO_BANCO más atrás, lejos de la fuente. Usa la misma
     * transformación que pieza(). Lo usan Basureros (para ubicarlo), arboles() y luminarias() (para dejarle lugar).
     * Solo lee CONSTANTES de Basureros (se copian al compilar), así que no dispara su inicialización antes de tiempo.
     */
    public static float[] lugarBasurero(float[] banco, int lado) {
        float lx = lado * (MITAD_BANCO + Basureros.SEPARACION_BANCO + Basureros.RADIO); // Al costado (X local).
        float lz = Basureros.RETIRO_BANCO; // +Z local: hacia atrás.
        float coseno = (float) Math.cos(banco[2]);
        float seno = (float) Math.sin(banco[2]);
        return new float[] {banco[0] + coseno * lx + seno * lz, banco[1] - seno * lx + coseno * lz};
    }

    // ==================== 3a. RECORRIDO PEATONAL: SENDEROS, PLAZA Y FRANJAS ====================
    // Todo lo pavimentado del parque está a la misma altura (el tope del sendero) y se toca entre sí, así que forma un
    // solo camino. Estas funciones responden "¿este punto (o este círculo) está sobre el pavimento?"; las usan árboles,
    // luminarias, Basureros y las pruebas, para que nada quede apoyado sobre el camino.

    /**
     * "Radio octogonal" de un punto relativo al centro del parque: el mayor entre |x|, |z| y (|x| + |z|) / √2. Vale
     * MITAD_PLAZA sobre todo el borde de la plaza: los dos primeros términos son el cuadrado recto y el tercero, el
     * cuadrado girado 45° (la distancia a una recta diagonal es (|x| + |z|) / √2). Si es menor, el punto está adentro.
     */
    public static float radioOctogonal(float rx, float rz) {
        float ax = Math.abs(rx);
        float az = Math.abs(rz);
        return Math.max(Math.max(ax, az), (ax + az) / (float) Math.sqrt(2));
    }

    /**
     * Indica si un círculo de centro (x, z) y ese radio toca la plaza. Agranda cada lado del octógono en "radio": en los
     * vértices es un poco más estricto que el círculo real (lo deja un poco más lejos), nunca menos.
     */
    public static boolean tocaPlaza(int fila, int columna, float x, float z, float radio) {
        return radioOctogonal(x - Mapa.centro(columna), z - Mapa.centro(fila)) <= MITAD_PLAZA + radio;
    }

    /** Indica si un círculo toca uno de los dos senderos de la cruz (rectángulos de ANCHO_SENDERO × todo el césped). */
    public static boolean tocaSendero(int fila, int columna, float x, float z, float radio) {
        float cx = Mapa.centro(columna);
        float cz = Mapa.centro(fila);
        float a = ANCHO_SENDERO / 2;
        float m = MITAD_CESPED;
        return tocaRectangulo(x, z, radio, cx - a, cx + a, cz - m, cz + m) // Sendero norte-sur.
            || tocaRectangulo(x, z, radio, cx - m, cx + m, cz - a, cz + a); // Sendero oeste-este.
    }

    /** Indica si un círculo toca alguna franja de acceso del parque. */
    public static boolean tocaFranja(int fila, int columna, float x, float z, float radio) {
        for (float[] f : franjas(fila, columna)) {
            if (tocaRectangulo(x, z, radio, f[0], f[1], f[2], f[3])) {
                return true;
            }
        }
        return false;
    }

    /** Indica si un círculo toca cualquier parte del pavimento: senderos, plaza o franjas. */
    public static boolean tocaPavimento(int fila, int columna, float x, float z, float radio) {
        return tocaSendero(fila, columna, x, z, radio) || tocaPlaza(fila, columna, x, z, radio)
            || tocaFranja(fila, columna, x, z, radio);
    }

    /** Indica si el punto (x, z) está sobre el pavimento (un círculo de radio 0, con el borde incluido). */
    public static boolean enPavimento(int fila, int columna, float x, float z) {
        return tocaPavimento(fila, columna, x, z, 0);
    }

    /**
     * Indica si un círculo toca el rectángulo {xMin..xMax, zMin..zMax}: dx y dz son lo que le falta al centro para
     * entrar al rectángulo en cada eje (0 si ya está dentro de ese intervalo). Con radio 0 cuenta el borde como adentro.
     */
    private static boolean tocaRectangulo(float x, float z, float radio, float xMin, float xMax, float zMin, float zMax) {
        float dx = Math.max(0, Math.max(xMin - x, x - xMax));
        float dz = Math.max(0, Math.max(zMin - z, z - zMax));
        return dx * dx + dz * dz <= radio * radio;
    }

    /**
     * Franjas de acceso de un parque, cada una {xMin, xMax, zMin, zMax} en coordenadas del mundo. Por cada paso peatonal
     * de Decoracion.UBICACIONES_PASOS que desemboca en un borde del parque:
     *  - el paso está en la celda de calle vecina: al norte o al sur si la calle va de oeste a este (ejeX), al oeste
     *    o al este si va de norte a sur;
     *  - "desvio" es cuánto se corre el paso, a lo largo del borde, respecto del eje del brazo (±3 con este diseño);
     *  - la franja va desde el eje del brazo (0) hasta el extremo lejano del paso, recortada al césped, y ocupa
     *    PROFUNDIDAD_FRANJA desde el borde hacia adentro. Así contiene toda la boca del paso y se une con el brazo.
     * Con 13 × 13 (u otro mapa) los pasos cambian y las franjas los siguen solas.
     */
    public static List<float[]> franjas(int fila, int columna) {
        List<float[]> lista = new ArrayList<>();
        float cx = Mapa.centro(columna);
        float cz = Mapa.centro(fila);
        float m = MITAD_CESPED;
        for (float[] paso : Decoracion.UBICACIONES_PASOS) { // {x, z, ejeX}.
            boolean ejeX = paso[2] == 1; // La calle va de oeste a este: el paso llega al borde norte o sur.
            int filaPaso = Mapa.indiceCelda(paso[1]);
            int columnaPaso = Mapa.indiceCelda(paso[0]);
            int lado; // -1: borde norte u oeste; 1: borde sur o este.
            float desvio; // Del eje del brazo al centro del paso, a lo largo del borde.
            if (ejeX && columnaPaso == columna && Math.abs(filaPaso - fila) == 1) {
                lado = filaPaso - fila;
                desvio = paso[0] - cx;
            } else if (!ejeX && filaPaso == fila && Math.abs(columnaPaso - columna) == 1) {
                lado = columnaPaso - columna;
                desvio = paso[1] - cz;
            } else {
                continue; // Este paso no llega a este parque.
            }
            float desde = Math.max(-m, Math.min(0, desvio - Decoracion.LARGO_PASO / 2)); // A lo largo del borde.
            float hasta = Math.min(m, Math.max(0, desvio + Decoracion.LARGO_PASO / 2));
            float borde = lado * m; // Borde del césped, de través.
            float adentro = lado * (m - PROFUNDIDAD_FRANJA); // Límite interior de la franja.
            float tMin = Math.min(borde, adentro);
            float tMax = Math.max(borde, adentro);
            if (ejeX) { // Borde norte o sur: la franja es larga en X.
                lista.add(new float[] {cx + desde, cx + hasta, cz + tMin, cz + tMax});
            } else { // Borde oeste o este: larga en Z.
                lista.add(new float[] {cx + tMin, cx + tMax, cz + desde, cz + hasta});
            }
        }
        return lista;
    }

    // ==================== 3b. LUMINARIAS GLOBO ====================

    /**
     * Cuántas luminarias lleva cada parque: LUMINARIAS_MAX_POR_PARQUE si todas entran en el arreglo uGlobos del shader
     * (Iluminacion.MAX_GLOBOS); si no, las que entren repartidas en partes iguales, pero nunca menos de 1. Con 6 parques
     * son 2 (12 luces); con 9 o más parques, 1. Si hubiera más parques que MAX_GLOBOS, LuminariasParqueTest lo detecta.
     */
    public static int luminariasPorParque() {
        int parques = Math.max(1, Mapa.parques().size()); // Evita dividir por cero en un mapa sin parques.
        return Math.max(1, Math.min(LUMINARIAS_MAX_POR_PARQUE, Iluminacion.MAX_GLOBOS / parques));
    }

    /**
     * Luminarias de un parque: cada una es {x, z} (el poste; el globo está ALTURA_GLOBO más arriba). REGLAS:
     *  - SOBRE UN SENDERO: en el borde de uno de los cuatro brazos de la cruz (DESVIO_LUMINARIA del eje), a una de las
     *    DISTANCIAS_LUMINARIA del centro: lejos de la fuente y antes de la acera;
     *  - SIN CHOCAR CON ÁRBOLES: el globo está a la altura de las copas, así que se mide contra la copa real de cada árbol;
     *  - SIN CHOCAR CON BANCOS: la base queda fuera del círculo que contiene al banco;
     *  - LEJOS DE LOS POSTES de la acera (semáforos, PARE, carteles y farolas de calle);
     *  - FUERA DE LA PLAZA Y DE LAS FRANJAS: ahí camina la gente que rodea la fuente o baja de un paso;
     *  - SIN QUITARLE EL LUGAR A UN BASURERO (lugarBasurero()), con la misma distancia que exige Basureros;
     *  - REPARTIDAS: la primera es la primera candidata válida desde un punto de partida propio del parque (variacion());
     *    la segunda, la válida MÁS LEJANA a la primera (suele quedar en el brazo opuesto). Sin azar: siempre igual.
     */
    public static List<float[]> luminarias(int fila, int columna) {
        float cx = Mapa.centro(columna); // Centro del parque en X.
        float cz = Mapa.centro(fila); // Centro del parque en Z.
        List<float[]> arboles = arboles(fila, columna); // Lo que ya ocupa el césped.
        List<float[]> bancos = bancos(fila, columna);
        int[][] brazos = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}}; // Dirección {x, z} de cada brazo: norte, sur, oeste y este.
        List<float[]> validas = new ArrayList<>(); // Candidatas que cumplen todas las reglas, en orden.
        int total = brazos.length * DISTANCIAS_LUMINARIA.length * 2; // 24 candidatas.
        int inicio = entero(fila, columna, 11, 0, total - 1); // Por dónde empieza este parque.
        for (int k = 0; k < total; k++) {
            int n = (inicio + k) % total; // Candidata número n.
            int[] brazo = brazos[n % brazos.length]; // Brazo de la cruz.
            float distancia = DISTANCIAS_LUMINARIA[(n / brazos.length) % DISTANCIAS_LUMINARIA.length]; // Del centro.
            int lado = n < total / 2 ? 1 : -1; // A un lado u otro del eje del sendero.
            // Perpendicular al brazo: (-z, x). Se suma el desvío hacia un costado del sendero.
            float x = cx + brazo[0] * distancia - brazo[1] * lado * DESVIO_LUMINARIA;
            float z = cz + brazo[1] * distancia + brazo[0] * lado * DESVIO_LUMINARIA;
            if (luminariaLibre(fila, columna, x, z, arboles, bancos)) {
                validas.add(new float[] {x, z});
            }
        }
        List<float[]> elegidas = new ArrayList<>(); // Resultado.
        for (int i = 0; i < luminariasPorParque() && !validas.isEmpty(); i++) {
            int mejor = 0; // La primera vez, la primera válida.
            double mejorDistancia = -1;
            for (int v = 0; i > 0 && v < validas.size(); v++) { // Desde la segunda: la más alejada de las ya elegidas.
                double cercana = Double.MAX_VALUE;
                for (float[] e : elegidas) {
                    cercana = Math.min(cercana, Math.hypot(validas.get(v)[0] - e[0], validas.get(v)[1] - e[1]));
                }
                if (cercana > mejorDistancia) { // Estrictamente mayor: en empates queda la primera.
                    mejor = v;
                    mejorDistancia = cercana;
                }
            }
            elegidas.add(validas.remove(mejor));
        }
        return elegidas; // Luminarias del parque.
    }

    /** Aplica las reglas de choque de luminarias(): plaza, franjas, árboles, bancos, basureros y postes de la acera. */
    private static boolean luminariaLibre(int fila, int columna, float x, float z, List<float[]> arboles, List<float[]> bancos) {
        float radioBase = ANCHO_BASE_GLOBO / 2; // Lo que la luminaria ocupa en el piso.
        if (tocaPlaza(fila, columna, x, z, radioBase) || tocaFranja(fila, columna, x, z, radioBase)) {
            return false; // Estorbaría el paso alrededor de la fuente o a la salida de un paso peatonal.
        }
        int lado = ladoBasureros(fila, columna);
        for (float[] b : bancos) { // Deja libre el lugar del basurero de cada banco (misma regla que Basureros.libre()).
            float[] cesto = lugarBasurero(b, lado);
            if (Math.hypot(x - cesto[0], z - cesto[1]) < radioBase + Basureros.RADIO + Basureros.MARGEN_LUMINARIA) {
                return false;
            }
        }
        for (float[] a : arboles) { // El globo (a 3 de altura) está entre las copas: se mide contra la copa real.
            if (Math.hypot(x - a[0], z - a[1]) < a[4] / 2 + DIAMETRO_GLOBO / 2 + MARGEN_LUMINARIA) {
                return false;
            }
        }
        for (float[] b : bancos) { // La base no se apoya sobre el banco.
            if (Math.hypot(x - b[0], z - b[1]) < MITAD_BANCO + ANCHO_BASE_GLOBO / 2 + MARGEN_LUMINARIA) {
                return false;
            }
        }
        return !cercaDeUnPoste(x, z, DIAMETRO_GLOBO / 2 + MARGEN_LUMINARIA_POSTES); // Semáforos, PARE, carteles, farolas.
    }

    /** Todas las luminarias de la ciudad como centro {x, y, z} del globo, parque por parque (orden de Mapa.parques()). */
    private static List<float[]> calcularLuminarias() {
        List<float[]> lista = new ArrayList<>();
        for (int[] celda : Mapa.parques()) {
            for (float[] l : luminarias(celda[0], celda[1])) {
                lista.add(new float[] {l[0], TOPE_CESPED + ALTURA_GLOBO, l[1]}); // El mismo punto que dibuja dibujarLuminaria().
            }
        }
        return lista;
    }

    // ==================== 4. DIBUJO ====================

    private final Shader shader; // Programa que recibe el interruptor de emisión.
    private final Cubo cubo; // Senderos y bancos.
    private final Figuras figuras; // Esfera, cilindro y cono.
    private final List<List<float[]>> arbolesPorParque = new ArrayList<>(); // Disposiciones calculadas una vez.
    private final List<List<float[]>> bancosPorParque = new ArrayList<>(); // Bancos calculados una vez.
    private final List<List<float[]>> luminariasDeCadaParque = new ArrayList<>(); // Luminarias globo calculadas una vez.
    private final List<List<float[]>> franjasPorParque = new ArrayList<>(); // Franjas de acceso calculadas una vez.
    private final List<int[]> celdas = Mapa.parques(); // Parques del mapa, en el mismo orden que las listas anteriores.

    /** Calcula la disposición de todos los parques una sola vez (no hay azar por cuadro). */
    public Parque(Shader shader, Cubo cubo, Figuras figuras) {
        this.shader = shader; // Guarda el programa para cambiar uEmision.
        this.cubo = cubo; // Guarda la geometría de cajas.
        this.figuras = figuras; // Guarda las figuras redondeadas.
        for (int[] celda : celdas) { // Recorre los parques.
            arbolesPorParque.add(arboles(celda[0], celda[1])); // Árboles de este parque.
            bancosPorParque.add(bancos(celda[0], celda[1])); // Bancos de este parque.
            luminariasDeCadaParque.add(luminarias(celda[0], celda[1])); // Luminarias globo de este parque.
            franjasPorParque.add(franjas(celda[0], celda[1])); // Franjas donde llegan los pasos peatonales.
        }
    }

    /** Dibuja el parque de la celda (fila, columna): pavimento, fuente, árboles, bancos y luminarias. */
    public void dibujar(int fila, int columna, boolean noche) {
        int indice = indiceDe(fila, columna); // Posición del parque en las listas calculadas.
        float x = Mapa.centro(columna); // Centro del parque en X.
        float z = Mapa.centro(fila); // Centro del parque en Z.
        dibujarPavimento(x, z, franjasPorParque.get(indice)); // Cruz de senderos, plaza y franjas de acceso.
        dibujarFuente(x, z, noche); // Fuente en el centro.
        for (float[] arbol : arbolesPorParque.get(indice)) { // Árboles propios del parque.
            dibujarArbol(figuras, arbol, TOPE_CESPED); // Pino o frondoso, apoyado sobre el césped.
        }
        for (float[] banco : bancosPorParque.get(indice)) { // Bancos propios del parque.
            dibujarBanco(banco[0], banco[1], banco[2]); // Mirando a la fuente.
        }
        for (float[] luminaria : luminariasDeCadaParque.get(indice)) { // Luminarias globo en el borde de los senderos.
            dibujarLuminaria(luminaria[0], luminaria[1], noche); // Encendida solo de noche.
        }
    }

    /**
     * Luminaria GLOBO: base, poste fino, portaglobo y esfera. El centro de la esfera es el mismo punto que
     * calcularLuminarias() guarda en LUMINARIAS y el shader usa como luz puntual. De noche el globo es emisivo.
     */
    private void dibujarLuminaria(float x, float z, boolean noche) {
        float[] c = COLOR_POSTE_GLOBO; // Poste, base y portaglobo del mismo color.
        float yGlobo = TOPE_CESPED + ALTURA_GLOBO; // Centro del globo.
        float pie = TOPE_CESPED + ALTO_BASE_GLOBO; // Donde termina la base.
        float yAnillo = yGlobo - DIAMETRO_GLOBO / 2 - ALTO_ANILLO_GLOBO / 2 + 0.03f; // El globo se apoya (y se hunde apenas) en el anillo.
        float altoPoste = yAnillo - pie; // Del zócalo al portaglobo.
        figuras.cilindro.dibujar(x, TOPE_CESPED + ALTO_BASE_GLOBO / 2, z, ANCHO_BASE_GLOBO, ALTO_BASE_GLOBO, ANCHO_BASE_GLOBO, c[0], c[1], c[2]); // Base.
        figuras.cilindro.dibujar(x, pie + altoPoste / 2, z, ANCHO_POSTE_GLOBO, altoPoste, ANCHO_POSTE_GLOBO, c[0], c[1], c[2]); // Poste.
        figuras.cilindro.dibujar(x, yAnillo, z, ANCHO_ANILLO_GLOBO, ALTO_ANILLO_GLOBO, ANCHO_ANILLO_GLOBO, c[0], c[1], c[2]); // Portaglobo.
        float[] globo = noche ? COLOR_GLOBO_NOCHE : COLOR_GLOBO_DIA; // Encendido solo de noche, como las farolas.
        shader.entero("uEmision", noche ? 1 : 0); // De noche el globo tiene luz propia.
        figuras.esfera.dibujar(x, yGlobo, z, DIAMETRO_GLOBO, DIAMETRO_GLOBO, DIAMETRO_GLOBO, globo[0], globo[1], globo[2]); // Globo.
        shader.entero("uEmision", 0); // Restablece el material normal.
    }

    /** Busca la posición del parque en la lista de celdas. */
    private int indiceDe(int fila, int columna) {
        for (int i = 0; i < celdas.size(); i++) { // Recorre los parques.
            if (celdas.get(i)[0] == fila && celdas.get(i)[1] == columna) { // Coincide.
                return i; // Posición encontrada.
            }
        }
        throw new IllegalArgumentException("No hay parque en " + fila + "," + columna); // Decoracion solo llama con parques.
    }

    /**
     * Pavimento beige del parque, todo a la misma altura: dos senderos en cruz que unen los cuatro lados con el centro,
     * la plaza octogonal alrededor de la fuente (un cuadrado recto más uno girado 45°) y las franjas de acceso. Las
     * piezas se superponen, pero como tienen el mismo color y la misma altura el solape no se nota.
     */
    private void dibujarPavimento(float x, float z, List<float[]> franjas) {
        float y = TOPE_CESPED + GROSOR_SENDERO / 2; // Apoyado sobre el césped, apenas elevado.
        float largo = 2 * MITAD_CESPED; // De borde a borde del césped.
        float[] c = COLOR_SENDERO;
        cubo.caja(x, y, z, largo, GROSOR_SENDERO, ANCHO_SENDERO, c[0], c[1], c[2]); // Sendero oeste-este.
        cubo.caja(x, y, z, ANCHO_SENDERO, GROSOR_SENDERO, largo, c[0], c[1], c[2]); // Sendero norte-sur (el cruce queda bajo la fuente).
        float ladoPlaza = 2 * MITAD_PLAZA; // Lado de cada cuadrado = 2 · apotema del octógono.
        cubo.caja(x, y, z, ladoPlaza, GROSOR_SENDERO, ladoPlaza, c[0], c[1], c[2]); // Cuadrado recto de la plaza.
        cubo.cajaGirada(x, y, z, ladoPlaza, GROSOR_SENDERO, ladoPlaza, c[0], c[1], c[2], (float) (Math.PI / 4)); // Girado 45°: completa el octógono.
        for (float[] f : franjas) { // {xMin, xMax, zMin, zMax}.
            cubo.caja((f[0] + f[1]) / 2, y, (f[2] + f[3]) / 2, f[1] - f[0], GROSOR_SENDERO, f[3] - f[2], c[0], c[1], c[2]); // Franja de acceso.
        }
    }

    /** Fuente: base gris, agua azul (levemente emisiva de noche), pilar con plato y chorro. */
    private void dibujarFuente(float x, float z, boolean noche) {
        float altoBase = 0.45f; // Alto del borde de piedra.
        figuras.cilindro.dibujar(x, TOPE_CESPED + altoBase / 2, z, 2 * RADIO_FUENTE, altoBase, 2 * RADIO_FUENTE, 0.60f, 0.62f, 0.65f); // Base de piedra.
        float[] agua = noche ? COLOR_AGUA_NOCHE : COLOR_AGUA; // Color del agua según el momento del día.
        if (noche) { // De noche el agua brilla un poco, como iluminada desde adentro.
            shader.entero("uEmision", 1); // Color propio, sin depender de las luces.
        }
        float alturaAgua = TOPE_CESPED + altoBase + 0.01f; // Apenas sobre el borde: se ve el anillo gris alrededor.
        figuras.cilindro.dibujar(x, alturaAgua, z, 2 * RADIO_FUENTE - 0.4f, 0.04f, 2 * RADIO_FUENTE - 0.4f, agua[0], agua[1], agua[2]); // Espejo de agua.
        shader.entero("uEmision", 0); // El pilar es de piedra: recibe luz normal.
        figuras.cilindro.dibujar(x, alturaAgua + 0.4f, z, 0.3f, 0.8f, 0.3f, 0.60f, 0.62f, 0.65f); // Pilar central.
        figuras.cilindro.dibujar(x, alturaAgua + 0.82f, z, 0.8f, 0.1f, 0.8f, 0.60f, 0.62f, 0.65f); // Plato superior.
        if (noche) { // El chorro también brilla de noche.
            shader.entero("uEmision", 1); // Color propio.
        }
        figuras.cilindro.dibujar(x, alturaAgua + 1.2f, z, 0.12f, 0.7f, 0.12f, agua[0] + 0.2f, agua[1] + 0.2f, agua[2]); // Chorro que sube.
        figuras.esfera.dibujar(x, alturaAgua + 1.6f, z, 0.35f, 0.3f, 0.35f, agua[0] + 0.3f, agua[1] + 0.3f, agua[2]); // Salpicadura en la punta.
        shader.entero("uEmision", 0); // Restablece el material normal para el siguiente objeto.
    }

    /**
     * Dibuja un árbol {x, z, tipo, alturaTronco, diametroCopa, verde, giro} apoyado a la altura base: pino o frondoso
     * según su tipo. Es estático para que Entorno reutilice los mismos árboles en el campo que rodea la ciudad.
     */
    public static void dibujarArbol(Figuras figuras, float[] arbol, float base) {
        if (arbol[2] == PINO) { // Según el tipo sorteado.
            dibujarPino(figuras, arbol, base); // Tronco y conos.
        } else { // Árbol frondoso.
            dibujarFrondoso(figuras, arbol, base); // Tronco y esferas.
        }
    }

    /**
     * Árbol frondoso: tronco cilíndrico y copa de tres esferas de distinto tamaño, desplazadas entre sí.
     * Las medidas verticales de la copa se escriben en "copas" (múltiplos del diámetro) y se estiran con k: sin estirar,
     * la copa sube ALTO_COPA_FRONDOSO copas por encima del tronco (0.35 + 0.30 + 0.65 / 2 = 0.975); con
     * k = altoCopa / (0.975 · copa) la punta queda justo en alturaTronco + altoCopa, sin cambiar el ancho.
     */
    private static void dibujarFrondoso(Figuras figuras, float[] a, float base) {
        float x = a[0]; // Posición del tronco en X.
        float z = a[1]; // Posición del tronco en Z.
        float altoTronco = a[3]; // Alto del tronco.
        float copa = a[4]; // Diámetro (ancho) de la esfera principal.
        float giro = a[6]; // Orientación de la copa.
        float k = a[ALTO_COPA] / (ALTO_COPA_FRONDOSO * copa); // Cuánto se estira la copa hacia arriba.
        float v = copa * k; // Una "copa" en vertical.
        figuras.cilindro.dibujar(x, base + altoTronco / 2, z, 0.34f, altoTronco, 0.34f, 0.38f, 0.22f, 0.12f); // Dibuja el tronco marrón.
        float r = 0.10f + 0.08f * a[5]; // Tono de verde: un poco de rojo...
        float g = 0.50f - 0.15f * a[5]; // ...más o menos verde según el árbol...
        float b = 0.16f + 0.06f * a[5]; // ...y algo de azul.
        float centroCopa = base + altoTronco + v * 0.35f; // La copa abraza el extremo del tronco.
        figuras.esfera.dibujar(x, centroCopa, z, copa, v * 0.9f, copa, r, g, b); // Esfera principal.
        float dx = (float) Math.cos(giro) * 0.35f; // Desplazamiento de las esferas menores, girado por árbol.
        float dz = (float) Math.sin(giro) * 0.35f; // Igual en Z.
        figuras.esfera.dibujar(x + dx, centroCopa + v * 0.3f, z + dz, copa * 0.7f, v * 0.65f, copa * 0.7f, r + 0.03f, g + 0.05f, b); // Esfera alta, más clara: su punta es la del árbol.
        figuras.esfera.dibujar(x - dx, centroCopa - v * 0.1f, z - dz, copa * 0.6f, v * 0.55f, copa * 0.6f, r, g - 0.06f, b); // Esfera baja, más oscura.
    }

    /** Cuántas "copas" sube la copa del frondoso sin estirar: centro 0.35 + esfera alta 0.30 + su mitad 0.325. */
    private static final float ALTO_COPA_FRONDOSO = 0.35f + 0.30f + 0.65f / 2;
    /** Alto de cada cono del pino, de abajo hacia arriba (sin estirar); cada uno se apoya a SOLAPE_CONOS del anterior. */
    private static final float[] ALTOS_CONOS = {1.3f, 1.1f, 0.9f};
    private static final float SOLAPE_CONOS = 0.55f; // El siguiente cono empieza al 55 % del anterior: se superponen.
    /** Alto de la pila de conos sin estirar: 1.3 · 0.55 + 1.1 · 0.55 + 0.9 = 2.22. */
    private static final float ALTO_PILA_CONOS = (ALTOS_CONOS[0] + ALTOS_CONOS[1]) * SOLAPE_CONOS + ALTOS_CONOS[2];

    /** Pino: tronco cilíndrico y tres conos apilados que se achican hacia arriba; la pila se estira hasta altoCopa. */
    private static void dibujarPino(Figuras figuras, float[] a, float base) {
        float x = a[0]; // Posición del tronco en X.
        float z = a[1]; // Posición del tronco en Z.
        float altoTronco = a[3]; // Alto del tronco.
        float anchoCono = a[4]; // Diámetro del cono inferior.
        float k = a[ALTO_COPA] / ALTO_PILA_CONOS; // Estiramiento vertical: la punta queda en alturaTronco + altoCopa.
        figuras.cilindro.dibujar(x, base + altoTronco / 2, z, 0.3f, altoTronco, 0.3f, 0.36f, 0.22f, 0.13f); // Tronco.
        float r = 0.06f + 0.04f * a[5]; // Verde oscuro de pino...
        float g = 0.38f - 0.10f * a[5]; // ...que varía por árbol...
        float b = 0.16f + 0.03f * a[5]; // ...con un poco de azul.
        float[] escalas = {1.0f, 0.72f, 0.46f}; // Cada cono es más angosto que el de abajo.
        float y = base + altoTronco; // Donde empieza el primer cono.
        for (int i = 0; i < escalas.length; i++) { // Apila los conos.
            float alto = ALTOS_CONOS[i] * k; // Alto de este cono, estirado.
            figuras.cono.dibujar(x, y + alto / 2, z, anchoCono * escalas[i], alto, anchoCono * escalas[i], r, g + 0.04f * i, b); // Cono, un poco más claro arriba.
            y += alto * SOLAPE_CONOS; // El siguiente se apoya sobre este: los conos se superponen.
        }
    }

    /** Banco de madera con patas, orientado con su frente (-Z local) hacia la fuente. */
    private void dibujarBanco(float x, float z, float angulo) {
        float y = TOPE_CESPED; // El banco se apoya sobre el césped.
        pieza(x, z, angulo, 0, y + 0.45f, 0, 1.6f, 0.1f, 0.5f, 0.55f, 0.30f, 0.13f); // Dibuja el asiento de madera del banco.
        pieza(x, z, angulo, 0, y + 0.75f, 0.24f, 1.6f, 0.45f, 0.08f, 0.55f, 0.30f, 0.13f); // Dibuja el respaldo detrás del asiento (+Z local: lejos de la fuente).
        for (int lado = -1; lado <= 1; lado += 2) { // Patas izquierdas y derechas.
            for (int fondo = -1; fondo <= 1; fondo += 2) { // Patas delanteras y traseras.
                pieza(x, z, angulo, lado * 0.7f, y + 0.2f, fondo * 0.18f, 0.08f, 0.4f, 0.08f, 0.22f, 0.24f, 0.24f); // Dibuja el soporte oscuro del banco.
            }
        }
    }

    /** Dibuja una caja en coordenadas locales del banco, girada con él (misma transformación que Auto.pieza). */
    private void pieza(float x, float z, float angulo, float lx, float ly, float lz, float sx, float sy, float sz, float r, float g, float b) {
        float coseno = (float) Math.cos(angulo); // Coseno de la orientación.
        float seno = (float) Math.sin(angulo); // Seno de la orientación.
        float mundoX = x + coseno * lx + seno * lz; // Gira la posición local y la traslada.
        float mundoZ = z - seno * lx + coseno * lz; // Igual en Z.
        cubo.cajaGirada(mundoX, ly, mundoZ, sx, sy, sz, r, g, b, angulo); // Caja orientada con el banco.
    }
}
