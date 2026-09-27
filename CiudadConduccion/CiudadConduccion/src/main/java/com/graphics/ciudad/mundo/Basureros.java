package com.graphics.ciudad.mundo; // Agrupa lo que forma la ciudad: mapa, edificios, decoración y señales.

import com.graphics.ciudad.iluminacion.Iluminacion; // Postes de farola (vía Parque.cercaDeUnPoste) y altura de la acera.
import com.graphics.ciudad.motor.Figuras; // Cilindros del cuerpo y la tapa.
import java.util.ArrayList; // Lista de basureros.
import java.util.Collections; // Publica la lista sin permitir modificarla.
import java.util.List; // Tipo de esa lista.

/**
 * BASUREROS: cestos de basura cilíndricos con tapa, en los parques y en las veredas de los edificios.
 * Responsable de: calcular UNA VEZ dónde va cada basurero (sin Random: todo sale del Mapa, de Decoracion, Parque,
 * Senalizacion e Iluminacion) y dibujarlos. No participan en colisiones: están dentro de las manzanas, que Colisiones
 * ya bloquea enteras.
 *
 * TRES REGLAS DE UBICACIÓN:
 *  1. EN LOS PARQUES: junto a cada banco (o cada BANCOS_POR_BASURERO bancos), al costado y apenas detrás del
 *     respaldo, sobre el césped: ni sobre el pavimento (senderos, plaza y franjas), ni en la fuente. El costado se
 *     sortea una vez por parque (Parque.ladoBasureros()), así cada cesto queda junto a un brazo distinto.
 *  2. EN LAS ESQUINAS DE LOS CRUCES CON PASO PEATONAL: en la vereda, "a la salida del paso", del lado de la cuadra.
 *     Entre el cruce y el paso quedan solo 0.5 de vereda (Decoracion.SEPARACION_CRUCE), así que en la esquina misma
 *     un basurero taparía el paso: por eso va justo pasado el paso, sobre la FRANJA DE MOBILIARIO.
 *  3. JUNTO A ALGUNOS NEGOCIOS: pegado a la pared, en el tramo ciego entre la última vidriera y la esquina del
 *     edificio, como el cesto que un comercio saca a su puerta. Solo en edificios con planta baja COMERCIAL
 *     (UsoPlantaBaja); cuáles negocios, lo decide Variacion.
 *
 * FRANJA DE MOBILIARIO: en una vereda real, la parte que da al cordón es donde van farolas, señales y cestos; la parte
 * junto a la pared queda para caminar y mirar vidrieras. Acá la franja va del borde del toldo (0.7 de la pared) al
 * cordón: un basurero ahí no tapa puertas ni vidrieras. "Tapar" se define como ocupar el frente de una puerta o
 * vidriera a menos de ZONA_FRENTE_NEGOCIO de la pared (el espacio bajo el toldo).
 * SIN BASUREROS EN LAS VEREDAS DE LOS PARQUES: miden 0.5, lo mismo que el basurero; el parque ya tiene los suyos.
 *
 * DISTANCIAS MÍNIMAS (todas con nombre): a farolas, semáforos, PARE y carteles; a los pasos y la franja de vereda
 * donde desembocan; a los árboles; a las luminarias de los parques; y a los demás basureros. Una candidata que no
 * cumple alguna se descarta: por eso las esquinas con semáforo o PARE (la señal está justo en la salida del paso)
 * quedan sin basurero.
 * Se comunica con: Decoracion (lo dibuja y da los pasos), Parque (bancos, árboles, luminarias y postes), Fachada
 * (puerta, vidrieras y toldo), Mapa, Variacion, Figuras y Sombras (mancha bajo cada basurero).
 */
public class Basureros {

    // ==================== 1. CONSTANTES (valores ajustables) ====================
    public static final float DIAMETRO = 0.5f; // Diámetro del cuerpo.
    public static final float RADIO = DIAMETRO / 2; // Radio del cuerpo: lo usan todas las distancias.
    public static final float ALTO = 0.9f; // Alto total, tapa incluida.
    public static final float ALTO_TAPA = 0.08f; // Alto de la tapa.
    public static final float EXCESO_TAPA = 0.04f; // La tapa es esto más ancha que el cuerpo a cada lado.
    public static final float ALTO_ARO = 0.06f; // Aro oscuro cerca de la boca, bajo la tapa (detalle).
    public static final float[] COLOR_CUERPO = {0.16f, 0.36f, 0.24f}; // Verde municipal.
    public static final float[] COLOR_TAPA = {0.20f, 0.22f, 0.24f}; // Gris oscuro.

    // ---- Tipos (cuarto número de cada ubicación) ----
    public static final int EN_PARQUE = 0; // Junto a un banco.
    public static final int EN_ESQUINA = 1; // A la salida de un paso peatonal.
    public static final int JUNTO_A_NEGOCIO = 2; // Pegado a la pared, al costado de una vidriera.

    // ---- Regla 1: parques ----
    public static final int BANCOS_POR_BASURERO = 1; // 1 = uno por banco; 2 = uno cada dos bancos.
    public static final float SEPARACION_BANCO = 0.15f; // Aire entre la punta del banco y el basurero.
    // RETIRO_BANCO: cuánto más atrás que el banco (lejos de la fuente) va el cesto, para no molestar al sentarse. Con el
    // banco en el borde de la plaza (Parque.DISTANCIA_BANCO ≈ 2.63), 0.15 deja el cesto a 0.92 del tronco de un árbol
    // de borde (con 0.3 quedaría a 0.77, menos que DISTANCIA_MIN_TRONCO, y el banco se quedaría sin cesto).
    public static final float RETIRO_BANCO = 0.15f;
    public static final int SEMILLA_LADO_BANCO = 31; // Elige el costado de los bancos de cada parque (Parque.ladoBasureros()).

    // ---- Regla 2: esquinas ----
    public static final float MARGEN_CORDON = 0.45f; // Del cordón al centro del basurero: sobre la franja de mobiliario.
    public static final float HOLGURA_SALIDA_PASO = 0.4f; // Aire entre el borde del paso y el basurero de la esquina.

    // ---- Regla 3: negocios ----
    public static final float FRACCION_NEGOCIOS = 0.35f; // Fracción de caras con negocio que tienen basurero.
    public static final int SEMILLA_NEGOCIOS = 97; // Cambiarla elige otros negocios (siempre los mismos para cada semilla).
    public static final float HOLGURA_VIDRIERA = 0.1f; // Aire entre el borde de la vidriera y el basurero.
    public static final float SEPARACION_PARED = 0.05f; // Aire entre la pared y el basurero.

    // ---- Distancias mínimas ----
    public static final float MARGEN_POSTES = 0.35f; // Aire hasta la mitad de un semáforo, PARE, cartel o poste de farola.
    public static final float HOLGURA_PASO = 0.3f; // Aire alrededor de un paso y de la franja de vereda donde desemboca.
    public static final float ANCHO_VEREDA = (Mapa.TAM_CELDA - Mapa.ANCHO_EDIFICIO) / 2; // 1.5: del cordón a la pared.
    public static final float ZONA_FRENTE_NEGOCIO = Fachada.VUELO_TOLDO + 0.05f; // Frente de puerta o vidriera: bajo el toldo.
    public static final float DISTANCIA_MIN_TRONCO = 0.8f; // Frondoso: la copa empieza a 2 de alto, solo estorba el tronco.
    public static final float MARGEN_LUMINARIA = 0.4f; // Aire hasta la base de una luminaria de parque.
    public static final float MARGEN_BANCO = 0.1f; // Aire hasta el círculo que contiene a un banco.
    public static final float DISTANCIA_MIN_ENTRE_BASUREROS = 2.0f; // Dos cestos pegados no tienen sentido.

    /** Cada basurero es {x, yBase, z, tipo}: yBase es la superficie donde se apoya (césped o acera). */
    public static final List<float[]> UBICACIONES = Collections.unmodifiableList(calcular());

    // ==================== 2. UBICACIONES ====================

    /** Aplica las tres reglas en orden: parques, esquinas y negocios. Cada candidata respeta a las ya elegidas. */
    static List<float[]> calcular() {
        List<float[]> lista = new ArrayList<>(); // Resultado.
        agregarEnParques(lista);
        agregarEnEsquinas(lista);
        agregarJuntoANegocios(lista);
        return lista;
    }

    /**
     * Regla 1: al costado de cada banco, apenas detrás. Todos los bancos de un parque usan el MISMO costado
     * (Parque.ladoBasureros()): si cada banco sorteara el suyo, dos bancos vecinos podrían dejar sus cestos a ambos
     * lados del mismo brazo, a 1.98 entre sí (menos que DISTANCIA_MIN_ENTRE_BASUREROS), y uno se quedaría sin cesto.
     * Árboles y luminarias ya le dejan ese lugar libre (Parque.arboles() y luminarias()); el otro costado queda como
     * reserva por si una señal de la acera lo ocupa.
     */
    private static void agregarEnParques(List<float[]> lista) {
        for (int[] celda : Mapa.parques()) {
            List<float[]> bancos = Parque.bancos(celda[0], celda[1]);
            int primero = Parque.ladoBasureros(celda[0], celda[1]); // Un costado para todo el parque.
            for (int i = 0; i < bancos.size(); i += BANCOS_POR_BASURERO) {
                float[] b = bancos.get(i); // {x, z, angulo}; el frente (-Z local) mira a la fuente.
                for (int lado : new int[] {primero, -primero}) {
                    float[] lugar = Parque.lugarBasurero(b, lado); // Al costado y RETIRO_BANCO más atrás.
                    float x = lugar[0];
                    float z = lugar[1];
                    if (libreEnParque(celda[0], celda[1], x, z) && libre(x, z, lista)) {
                        lista.add(new float[] {x, Parque.TOPE_CESPED, z, EN_PARQUE});
                        break; // Un basurero por banco.
                    }
                }
            }
        }
    }

    /**
     * Regla 2: para cada paso peatonal y cada una de sus dos puntas (las veredas de ambos lados de la calle), un
     * basurero sobre la franja de mobiliario, justo pasado el paso en el sentido que se aleja del cruce.
     */
    private static void agregarEnEsquinas(List<float[]> lista) {
        for (float[] paso : Decoracion.UBICACIONES_PASOS) { // {x, z, ejeX}.
            boolean ejeX = paso[2] == 1; // La calle va de oeste a este: se avanza en X y se cruza en Z.
            float s = ejeX ? paso[0] : paso[1]; // Coordenada a lo largo de la calle.
            float t = ejeX ? paso[1] : paso[0]; // Coordenada a lo ancho.
            float centroCalle = Mapa.centro(Mapa.indiceCelda(s)); // Centro del tramo de calle, a lo largo.
            float haciaCruce = Math.signum(s - centroCalle); // El paso está corrido hacia su cruce.
            float sBasurero = s - haciaCruce * (Decoracion.LARGO_PASO / 2 + HOLGURA_SALIDA_PASO + RADIO); // A la salida.
            float ejeCalle = Mapa.centro(Mapa.indiceCelda(t)); // Eje de la calle, a lo ancho.
            for (int lado = -1; lado <= 1; lado += 2) { // Las dos veredas que une el paso.
                float tBasurero = ejeCalle + lado * (Mapa.TAM_CELDA / 2 + MARGEN_CORDON); // Pasando el cordón.
                float x = ejeX ? sBasurero : tBasurero;
                float z = ejeX ? tBasurero : sBasurero;
                if (libreEnVereda(x, z) && libre(x, z, lista)) {
                    lista.add(new float[] {x, Iluminacion.ALTURA_ACERA, z, EN_ESQUINA});
                }
            }
        }
    }

    /** Regla 3: en algunas caras con negocio, pegado a la pared, entre la vidriera y la esquina del edificio. */
    private static void agregarJuntoANegocios(List<float[]> lista) {
        float finVidriera = Fachada.tramosVidrieras()[1][1]; // 3.1: donde termina la vidriera derecha.
        for (int fila = 0; fila < Mapa.MAPA.length; fila++) {
            for (int columna = 0; columna < Mapa.MAPA[fila].length; columna++) {
                if (Mapa.tipo(fila, columna) != Mapa.EDIFICIO || UsoPlantaBaja.de(fila, columna) != UsoPlantaBaja.COMERCIAL) {
                    continue; // Solo los edificios con negocio en la planta baja (no lobbies, departamentos ni casas).
                }
                for (int cara = 0; cara < Mapa.VECINOS.length; cara++) {
                    int[] v = Mapa.VECINOS[cara];
                    if (!Mapa.esCalleSegura(fila + v[0], columna + v[1])) {
                        continue; // Sin calle no hay negocio.
                    }
                    if (Variacion.valor(fila, columna, cara, SEMILLA_NEGOCIOS) >= FRACCION_NEGOCIOS) {
                        continue; // Este negocio no saca cesto.
                    }
                    int lado = Variacion.valor(fila, columna, cara + Mapa.VECINOS.length, SEMILLA_NEGOCIOS) < 0.5f ? 1 : -1;
                    float u = lado * (finVidriera + HOLGURA_VIDRIERA + RADIO); // Tramo ciego junto a la esquina.
                    float afuera = Mapa.ANCHO_EDIFICIO / 2 + SEPARACION_PARED + RADIO; // Pegado a la pared.
                    float[] p = Fachada.puntoEnCara(Mapa.centro(columna), Mapa.centro(fila), cara, u, afuera);
                    if (libreEnVereda(p[0], p[1]) && libre(p[0], p[1], lista)) {
                        lista.add(new float[] {p[0], Iluminacion.ALTURA_ACERA, p[1], JUNTO_A_NEGOCIO});
                    }
                }
            }
        }
    }

    // ==================== 3. REGLAS DE DISTANCIA ====================

    /** En un parque: sobre el césped, fuera del pavimento (senderos, plaza y franjas) y sin tocar ningún banco. */
    static boolean libreEnParque(int fila, int columna, float x, float z) {
        float rx = Math.abs(x - Mapa.centro(columna)); // Distancia al centro del parque en X.
        float rz = Math.abs(z - Mapa.centro(fila)); // Y en Z.
        if (rx + RADIO > Parque.MITAD_CESPED || rz + RADIO > Parque.MITAD_CESPED) {
            return false; // Saldría del césped.
        }
        if (Parque.tocaPavimento(fila, columna, x, z, RADIO)) {
            return false; // Tocaría el camino de los peatones (la fuente está dentro de la plaza, así que también queda excluida).
        }
        for (float[] b : Parque.bancos(fila, columna)) {
            if (Math.hypot(x - b[0], z - b[1]) < Parque.MITAD_BANCO + RADIO + MARGEN_BANCO) {
                return false; // Encima de un banco.
            }
        }
        return true;
    }

    /**
     * En la vereda de un edificio: dentro de la manzana (nunca en la calzada), fuera del edificio, lejos de los pasos
     * y de la franja de vereda donde desembocan, y sin tapar puertas ni vidrieras.
     */
    static boolean libreEnVereda(float x, float z) {
        int fila = Mapa.indiceCelda(z);
        int columna = Mapa.indiceCelda(x);
        boolean dentro = fila >= 0 && fila < Mapa.MAPA.length && columna >= 0 && columna < Mapa.MAPA[fila].length;
        if (!dentro || Mapa.tipo(fila, columna) != Mapa.EDIFICIO) {
            return false; // Solo veredas de edificios (las de los parques son muy angostas).
        }
        float dx = Math.abs(x - Mapa.centro(columna)); // Del centro de la manzana.
        float dz = Math.abs(z - Mapa.centro(fila));
        if (dx + RADIO > Mapa.TAM_CELDA / 2 || dz + RADIO > Mapa.TAM_CELDA / 2) {
            return false; // Pasaría el cordón: quedaría en la calzada.
        }
        if (dx < Mapa.ANCHO_EDIFICIO / 2 + RADIO && dz < Mapa.ANCHO_EDIFICIO / 2 + RADIO) {
            return false; // Tocaría el edificio.
        }
        return !tocaPasoOSuSalida(x, z) && !tapaNegocio(fila, columna, x, z);
    }

    /**
     * Indica si el basurero toca un paso peatonal o la FRANJA DE VEREDA donde desemboca: el rectángulo del paso,
     * estirado a lo ancho hasta las paredes de enfrente (media calle + vereda), con HOLGURA_PASO alrededor.
     */
    public static boolean tocaPasoOSuSalida(float x, float z) {
        for (float[] paso : Decoracion.UBICACIONES_PASOS) {
            boolean ejeX = paso[2] == 1;
            float largo = Decoracion.LARGO_PASO / 2 + HOLGURA_PASO; // A lo largo de la calle.
            float travesia = Mapa.TAM_CELDA / 2 + ANCHO_VEREDA + HOLGURA_PASO; // De pared a pared.
            float mitadX = ejeX ? largo : travesia;
            float mitadZ = ejeX ? travesia : largo;
            if (Math.abs(x - paso[0]) < mitadX + RADIO && Math.abs(z - paso[1]) < mitadZ + RADIO) {
                return true;
            }
        }
        return false;
    }

    /**
     * Indica si el basurero tapa la puerta o una vidriera de alguna cara con negocio del edificio (fila, columna): si
     * está a menos de ZONA_FRENTE_NEGOCIO de la pared y su ancho, a lo largo de la cara, se superpone con la puerta o
     * con una vidriera. Vale para todos los usos de planta baja: el vidrio del lobby, las ventanas de planta baja y el
     * portón de garaje ocupan el mismo tramo (de -3.1 a 3.1) y el escalón, la marquesina y el alero no pasan del toldo.
     */
    public static boolean tapaNegocio(int fila, int columna, float x, float z) {
        List<float[]> tramos = new ArrayList<>(); // Puerta y vidrieras {uMin, uMax}.
        tramos.add(Fachada.tramoPuerta());
        Collections.addAll(tramos, Fachada.tramosVidrieras());
        for (int cara = 0; cara < Mapa.VECINOS.length; cara++) {
            int[] v = Mapa.VECINOS[cara];
            if (!Mapa.esCalleSegura(fila + v[0], columna + v[1])) {
                continue; // Esa cara no tiene negocio.
            }
            float nx = v[1]; // Normal de la cara (columnas = X, filas = Z).
            float nz = v[0];
            float relX = x - Mapa.centro(columna);
            float relZ = z - Mapa.centro(fila);
            float afuera = relX * nx + relZ * nz; // Distancia desde el centro del edificio hacia la calle.
            float u = relX * -nz + relZ * nx; // Posición a lo largo de la cara (mismo eje u que Fachada.puntoEnCara).
            float desdePared = afuera - Mapa.ANCHO_EDIFICIO / 2 - RADIO; // Del borde del basurero a la pared.
            if (desdePared < 0 || desdePared >= ZONA_FRENTE_NEGOCIO) {
                continue; // Detrás de la pared (otra cara) o sobre la franja de mobiliario.
            }
            for (float[] tramo : tramos) {
                if (u + RADIO > tramo[0] && u - RADIO < tramo[1]) {
                    return true; // Delante de la puerta o de una vidriera.
                }
            }
        }
        return false;
    }

    /**
     * Reglas comunes: lejos de semáforos, PARE, carteles y farolas (Parque.cercaDeUnPoste), de los árboles, de las
     * luminarias de los parques y de los basureros ya ubicados.
     */
    static boolean libre(float x, float z, List<float[]> yaUbicados) {
        if (Parque.cercaDeUnPoste(x, z, RADIO + MARGEN_POSTES)) {
            return false;
        }
        for (int[] celda : Mapa.parques()) {
            for (float[] a : Parque.arboles(celda[0], celda[1])) {
                if (Math.hypot(x - a[0], z - a[1]) < distanciaMinimaArbol(a)) {
                    return false;
                }
            }
        }
        for (float[] l : Parque.LUMINARIAS) { // {x, y, z} del globo, justo encima de la base.
            if (Math.hypot(x - l[0], z - l[2]) < Parque.ANCHO_BASE_GLOBO / 2 + RADIO + MARGEN_LUMINARIA) {
                return false;
            }
        }
        for (float[] b : yaUbicados) {
            if (Math.hypot(x - b[0], z - b[2]) < DISTANCIA_MIN_ENTRE_BASUREROS) {
                return false;
            }
        }
        return true;
    }

    /**
     * Distancia mínima del centro de un basurero al tronco del árbol a = {x, z, tipo, alturaTronco, diametroCopa, ...}.
     * El PINO tiene ramas casi hasta el suelo: el basurero queda fuera de su copa. El FRONDOSO tiene la copa por encima
     * de 2 de alto (el basurero mide 0.9): alcanza con no tocar el tronco.
     */
    public static float distanciaMinimaArbol(float[] a) {
        return a[2] == Parque.PINO ? a[4] / 2 + RADIO : DISTANCIA_MIN_TRONCO;
    }

    // ==================== 4. DIBUJO ====================

    private final Figuras figuras; // Cilindros del cuerpo, el aro y la tapa.

    /** Recibe las figuras compartidas; las ubicaciones ya están en UBICACIONES. */
    public Basureros(Figuras figuras) {
        this.figuras = figuras;
    }

    /** Dibuja cada basurero: cuerpo cilíndrico, un aro oscuro cerca de la boca y la tapa, un poco más ancha. */
    public void dibujar() {
        float[] c = COLOR_CUERPO;
        float[] t = COLOR_TAPA;
        float altoCuerpo = ALTO - ALTO_TAPA; // La tapa completa el alto total.
        float anchoTapa = DIAMETRO + 2 * EXCESO_TAPA;
        for (float[] b : UBICACIONES) {
            float base = b[1]; // Césped o acera.
            figuras.cilindro.dibujar(b[0], base + altoCuerpo / 2, b[2], DIAMETRO, altoCuerpo, DIAMETRO, c[0], c[1], c[2]); // Cuerpo.
            figuras.cilindro.dibujar(b[0], base + altoCuerpo - ALTO_ARO / 2, b[2], DIAMETRO + 0.02f, ALTO_ARO, DIAMETRO + 0.02f, t[0], t[1], t[2]); // Aro.
            figuras.cilindro.dibujar(b[0], base + altoCuerpo + ALTO_TAPA / 2, b[2], anchoTapa, ALTO_TAPA, anchoTapa, t[0], t[1], t[2]); // Tapa.
        }
    }
}
