package com.graphics.ciudad.trafico; // Agrupa el tráfico autónomo: vehículos y sus rutas.

import com.graphics.ciudad.mundo.Mapa; // Aporta el ancho de la calle para calcular el carril.
import com.graphics.ciudad.motor.Cubo; // Dibuja cada pieza del vehículo.
import com.graphics.ciudad.motor.Shader; // Activa la emisión de las luces traseras de noche.
import com.graphics.ciudad.vehiculo.Cabina; // Cabina trapezoidal compartida con el auto del jugador.
import com.graphics.ciudad.vehiculo.LucesVehiculo; // Ubicación y colores de luces, compartidos con el auto del jugador.
import static com.graphics.ciudad.vehiculo.LucesVehiculo.ALTURA_LUZ; // Altura de las luces.
import static com.graphics.ciudad.vehiculo.LucesVehiculo.FRENTE_LUZ; // Posición local de los faros.
import static com.graphics.ciudad.vehiculo.LucesVehiculo.LADO_LUZ; // Separación lateral de las luces.
import static com.graphics.ciudad.vehiculo.LucesVehiculo.TRASERA_LUZ; // Posición local de las luces traseras.
import java.util.function.BooleanSupplier; // Pregunta a Iluminacion si es de noche, sin copiar esa variable.

/**
 * VEHICULO: un auto autónomo que recorre una ruta cíclica de waypoints.
 * Responsable de: su estado (posición, orientación, velocidad y color), seguir la ruta girando suavemente hacia el
 * próximo waypoint, frenar en las curvas y cuando el jugador está adelante, reiniciarse y dibujarse con piezas.
 * Se comunica con: Trafico (lo crea con una ruta ya validada y lo actualiza con dt), Cubo y Shader (dibujo).
 * LUCES: se encienden solas de noche y se apagan de día. El vehículo no guarda si es de noche: consulta a
 * Iluminacion.esNoche() a través de un BooleanSupplier, así hay una única variable día/noche en todo el juego.
 * La tecla F no las afecta: F solo controla los faros del jugador.
 * Orientación igual que Auto: ángulo en radianes, cero apunta hacia -Z y el frente es (-sen(ángulo), -cos(ángulo)).
 * CARRILES (circulación por la derecha): la ruta llega en centros de calle, pero el vehículo no circula sobre la
 * línea amarilla. Cada tramo se desplaza DESPLAZAMIENTO_CARRIL hacia la DERECHA de su dirección de avance.
 * Con esta convención de ejes, la derecha de una dirección (dx, dz) es (-dz, dx): mirando al norte (0, -1) la derecha
 * es (1, 0) = este, y mirando al oeste (-1, 0) la derecha es (0, -1) = norte. Equivale a (cos(ángulo), -sen(ángulo)).
 */
public class Vehiculo {

    // ==================== 1. CONSTANTES DE CONDUCCIÓN (valores ajustables) ====================
    public static final float RADIO_VEHICULO = 1.65f; // Radio del círculo de colisión: el mismo tamaño que el auto del jugador.
    public static final float VELOCIDAD_GIRO = 2.2f; // Máximo giro por segundo, en radianes: limita qué tan rápido cambia de dirección.
    public static final float RADIO_WAYPOINT = 2; // Al acercarse a menos de esta distancia, pasa al siguiente waypoint.
    public static final float FRACCION_MINIMA_CURVA = 0.3f; // En una curva cerrada circula al 30 % de su velocidad de crucero.
    public static final float DISTANCIA_PRECAUCION = 6; // Si el jugador está adelante a menos de esto, el vehículo se detiene.
    public static final float DESPLAZAMIENTO_CARRIL = Mapa.TAM_CELDA / 4; // 1/4 del ancho de la calle (2.5): centro del carril derecho.
    public static final float DISTANCIA_ANTICIPACION = 4; // Mira este tramo por delante sobre su carril: corrige desvíos sin zigzaguear.

    // ==================== 1b. LUCES DEL VEHÍCULO (valores ajustables) ====================
    // Ubicación, tamaño y colores de faros y luces traseras: están en vehiculo/LucesVehiculo, compartidos con el auto
    // del jugador. El tráfico no frena con luces: de noche muestra luces de posición.

    // ==================== 2. ESTADO ====================
    private final float[][] ruta; // Waypoints {x, z} en centros de celdas de calle; la ruta es cíclica.
    private final float[][] carriles; // Esquinas del carril derecho: cada waypoint desplazado hacia la derecha de sus tramos.
    private final float velocidadCrucero; // Velocidad en recta, en unidades por segundo.
    private final float rojo; // Componente roja del color de la carrocería.
    private final float verde; // Componente verde del color de la carrocería.
    private final float azul; // Componente azul del color de la carrocería.
    private final BooleanSupplier esNoche; // Consulta el estado día/noche de Iluminacion (tecla N).
    float x; // Posición horizontal actual.
    float z; // Posición en profundidad actual.
    float angulo; // Orientación en radianes; cero apunta hacia -Z.
    float velocidad; // Velocidad actual en unidades por segundo.
    int siguiente; // Índice del waypoint hacia el que se dirige.

    /** Crea el vehículo con su ruta, su velocidad de crucero y su color, y lo coloca al inicio de la ruta. */
    public Vehiculo(float[][] ruta, float velocidadCrucero, float rojo, float verde, float azul, BooleanSupplier esNoche) {
        this.esNoche = esNoche; // Guarda la consulta de día/noche; las luces dependen solo de ella.
        this.ruta = ruta; // Guarda los waypoints que recorrerá en bucle.
        this.carriles = calcularCarriles(ruta, DESPLAZAMIENTO_CARRIL); // Traslada la ruta al carril derecho.
        this.velocidadCrucero = velocidadCrucero; // Guarda la velocidad en recta.
        this.rojo = rojo; // Guarda el color de la carrocería.
        this.verde = verde; // Guarda el color de la carrocería.
        this.azul = azul; // Guarda el color de la carrocería.
        reset(); // Ubica el vehículo en su primer waypoint.
    }

    /**
     * Calcula las esquinas del carril derecho. Cada tramo i va de ruta[i] a ruta[i+1]; su carril es la misma recta
     * desplazada d hacia la derecha. En cada waypoint se juntan el carril del tramo que llega y el del que sale: la
     * esquina es el punto donde se cortan esas dos rectas, W + d · (derechaEntrada + derechaSalida) / (1 + cos θ),
     * con θ el ángulo entre ambos tramos. En un giro de 90° (cos θ = 0) queda W + d · (derechaEntrada + derechaSalida):
     * en un giro a la derecha la esquina queda del lado interior sin llegar a la vereda, y en uno a la izquierda del
     * lado exterior, así el vehículo cambia de carril recién al doblar y no invade la mano contraria antes del cruce.
     */
    static float[][] calcularCarriles(float[][] ruta, float d) {
        int n = ruta.length; // Cantidad de waypoints.
        float[][] esquinas = new float[n][]; // Resultado: una esquina de carril por waypoint.
        for (int i = 0; i < n; i++) { // Recorre cada waypoint.
            float[] anterior = ruta[(i - 1 + n) % n]; // Waypoint previo: el tramo de entrada viene desde aquí.
            float[] actual = ruta[i]; // Waypoint donde se unen los dos tramos.
            float[] posterior = ruta[(i + 1) % n]; // Waypoint siguiente: el tramo de salida va hacia aquí.
            float[] entrada = direccion(anterior, actual); // Dirección unitaria del tramo que llega.
            float[] salida = direccion(actual, posterior); // Dirección unitaria del tramo que sale.
            float derechaEntradaX = -entrada[1]; // Derecha de (dx, dz) = (-dz, dx): componente X.
            float derechaEntradaZ = entrada[0]; // Componente Z de la derecha del tramo de entrada.
            float derechaSalidaX = -salida[1]; // Derecha del tramo de salida, componente X.
            float derechaSalidaZ = salida[0]; // Derecha del tramo de salida, componente Z.
            float coseno = entrada[0] * salida[0] + entrada[1] * salida[1]; // cos θ = producto escalar de las direcciones.
            float divisor = Math.max(1 + coseno, 0.1f); // Evita dividir por cero si la ruta hiciera una vuelta en U.
            float esquinaX = actual[0] + d * (derechaEntradaX + derechaSalidaX) / divisor; // Corte de los dos carriles en X.
            float esquinaZ = actual[1] + d * (derechaEntradaZ + derechaSalidaZ) / divisor; // Corte de los dos carriles en Z.
            esquinas[i] = new float[] {esquinaX, esquinaZ}; // Guarda la esquina del carril.
        }
        return esquinas; // Ruta trasladada al carril derecho.
    }

    /** Dirección unitaria {dx, dz} que va del punto desde al punto hasta. */
    private static float[] direccion(float[] desde, float[] hasta) {
        float dx = hasta[0] - desde[0]; // Avance en X.
        float dz = hasta[1] - desde[1]; // Avance en Z.
        float largo = (float) Math.hypot(dx, dz); // Longitud del tramo.
        if (largo == 0) { // Dos waypoints iguales no definen dirección.
            return new float[] {0, -1}; // Usa el norte para no dividir por cero.
        }
        return new float[] {dx / largo, dz / largo}; // Normaliza: largo 1.
    }

    /** Coloca el vehículo en el primer waypoint, mirando hacia el segundo; Trafico lo llama al presionar R. */
    public void reset() {
        x = carriles[0][0]; // Parte del primer waypoint en X (ya sobre su carril derecho).
        z = carriles[0][1]; // Parte del primer waypoint en Z.
        siguiente = 1 % ruta.length; // Se dirige al segundo waypoint (o al mismo si la ruta tuviera uno solo).
        angulo = anguloHacia(carriles[siguiente][0], carriles[siguiente][1]); // Arranca ya orientado hacia su destino.
        velocidad = velocidadCrucero; // Arranca a velocidad de crucero.
    }

    // ==================== 3. MOVIMIENTO POR CUADRO ====================

    /** Avanza el vehículo deltaTime segundos; si jugadorAdelante es true, se detiene para no chocarlo. */
    public void actualizar(float deltaTime, boolean jugadorAdelante) {
        float[] tramo = tramoCarril(); // {inicioX, inicioZ, dirX, dirZ, largo, avance} del tramo actual del carril.
        if (tramo[4] - tramo[5] < RADIO_WAYPOINT) { // Comprueba si ya llegó: le falta menos que RADIO_WAYPOINT.
            siguiente = (siguiente + 1) % ruta.length; // Pasa al siguiente waypoint; el módulo hace la ruta cíclica.
            tramo = tramoCarril(); // Lee el nuevo tramo: aquí empieza el giro hacia el carril siguiente.
        }
        // Persecución sobre la línea del carril: el objetivo es un punto del carril DISTANCIA_ANTICIPACION más adelante
        // de donde está el vehículo proyectado. Si se desvió, apunta de vuelta al carril; si no, sigue derecho.
        float avanceObjetivo = Math.min(tramo[5] + DISTANCIA_ANTICIPACION, tramo[4]); // Nunca más allá de la esquina.
        float objetivoX = tramo[0] + tramo[2] * avanceObjetivo; // Lee el X del punto objetivo sobre el carril.
        float objetivoZ = tramo[1] + tramo[3] * avanceObjetivo; // Lee el Z del punto objetivo sobre el carril.

        float deseado = anguloHacia(objetivoX, objetivoZ); // Orientación que apuntaría directo al punto objetivo.
        float diferencia = normalizar(deseado - angulo); // Cuánto falta girar, entre -π y π (por el lado más corto).
        float giroMaximo = VELOCIDAD_GIRO * deltaTime; // Lo máximo que puede girar en este cuadro.
        angulo += Math.max(-giroMaximo, Math.min(giroMaximo, diferencia)); // Gira suavemente: nunca más que giroMaximo.

        // En recta (diferencia ≈ 0) el coseno vale 1 y circula a velocidad de crucero; en una curva cerrada frena.
        float factorCurva = Math.max(FRACCION_MINIMA_CURVA, (float) Math.cos(diferencia)); // Nunca baja del mínimo.
        velocidad = velocidadCrucero * factorCurva; // Velocidad deseada para este cuadro.
        if (jugadorAdelante) { // Comprueba si el auto del jugador le cierra el paso.
            velocidad = 0; // Espera detenido hasta que el camino quede libre.
        }

        float frenteX = -(float) Math.sin(angulo); // Obtiene la componente X del frente del vehículo.
        float frenteZ = -(float) Math.cos(angulo); // Obtiene la componente Z; con ángulo cero vale -1.
        x += frenteX * velocidad * deltaTime; // Avanza en X según la velocidad y el tiempo.
        z += frenteZ * velocidad * deltaTime; // Avanza en Z según la velocidad y el tiempo.
    }

    /** Tramo actual del carril: {inicioX, inicioZ, dirX, dirZ, largo, avance}; avance es la proyección del vehículo. */
    private float[] tramoCarril() {
        float[] inicio = carriles[(siguiente - 1 + ruta.length) % ruta.length]; // Esquina de carril de la que viene.
        float[] fin = carriles[siguiente]; // Esquina de carril hacia la que va.
        float[] dir = direccion(inicio, fin); // Dirección unitaria del tramo.
        float largo = (float) Math.hypot(fin[0] - inicio[0], fin[1] - inicio[1]); // Longitud del tramo del carril.
        float avance = (x - inicio[0]) * dir[0] + (z - inicio[1]) * dir[1]; // Producto escalar: cuánto avanzó sobre el tramo.
        return new float[] {inicio[0], inicio[1], dir[0], dir[1], largo, avance}; // Todo lo que necesita actualizar().
    }

    /** Devuelve el tramo actual por el CENTRO de la calle, {desdeX, desdeZ, hastaX, hastaZ}; lo usa TraficoTest. */
    float[] tramoCentral() {
        float[] desde = ruta[(siguiente - 1 + ruta.length) % ruta.length]; // Waypoint (centro de calle) de partida.
        float[] hasta = ruta[siguiente]; // Waypoint (centro de calle) de llegada.
        return new float[] {desde[0], desde[1], hasta[0], hasta[1]}; // Extremos de la línea central del tramo.
    }

    /** Indica si el punto (px, pz) está delante del vehículo y a menos de DISTANCIA_PRECAUCION. */
    public boolean tieneAdelante(float px, float pz) {
        float haciaX = px - x; // Vector del vehículo al punto, en X.
        float haciaZ = pz - z; // Vector del vehículo al punto, en Z.
        float frenteX = -(float) Math.sin(angulo); // Dirección frontal en X.
        float frenteZ = -(float) Math.cos(angulo); // Dirección frontal en Z.
        boolean delante = haciaX * frenteX + haciaZ * frenteZ > 0; // Producto escalar positivo: el punto está hacia el frente.
        boolean cerca = haciaX * haciaX + haciaZ * haciaZ < DISTANCIA_PRECAUCION * DISTANCIA_PRECAUCION; // Compara al cuadrado.
        return delante && cerca; // Solo frena por lo que tiene cerca y adelante.
    }

    /** Calcula el ángulo que apunta desde la posición actual hacia (destinoX, destinoZ). */
    private float anguloHacia(float destinoX, float destinoZ) {
        // El frente es (-sen, -cos): despejando, el ángulo que mira hacia (dx, dz) es atan2(-dx, -dz).
        return (float) Math.atan2(-(destinoX - x), -(destinoZ - z)); // Devuelve radianes entre -π y π.
    }

    /** Lleva un ángulo al intervalo [-π, π] para girar siempre por el lado más corto. */
    private static float normalizar(float angulo) {
        while (angulo > Math.PI) { // Si se pasó de media vuelta positiva...
            angulo -= (float) (2 * Math.PI); // ...resta una vuelta completa.
        }
        while (angulo < -Math.PI) { // Si se pasó de media vuelta negativa...
            angulo += (float) (2 * Math.PI); // ...suma una vuelta completa.
        }
        return angulo; // Ángulo equivalente dentro del intervalo.
    }

    // ==================== 4. DIBUJO ====================

    /** Indica si las luces del vehículo están encendidas: sí de noche y no de día (independiente de la tecla F). */
    public boolean lucesEncendidas() {
        return esNoche.getAsBoolean(); // Lee el estado actual de Iluminacion en cada consulta.
    }

    /** Posición en el mundo {x, y, z} del faro izquierdo (lado 0) o derecho (lado 1): origen de su foco. */
    public float[] posicionFaro(int lado) {
        float localX = lado == 0 ? -LADO_LUZ : LADO_LUZ; // Izquierda o derecha en coordenadas locales.
        float coseno = (float) Math.cos(angulo); // Misma transformación que pieza(): gira con el vehículo.
        float seno = (float) Math.sin(angulo); // Seno de la orientación.
        float mundoX = x + coseno * localX + seno * FRENTE_LUZ; // Posición X del faro en la ciudad.
        float mundoZ = z - seno * localX + coseno * FRENTE_LUZ; // Posición Z del faro en la ciudad.
        return new float[] {mundoX, ALTURA_LUZ, mundoZ}; // Punto desde donde nace el haz.
    }

    /** Construye el vehículo con cajas, en el mismo estilo que Auto pero con su propio color. */
    public void dibujar(Cubo cubo, Shader shader, Cabina cabina) {
        pieza(cubo, 0, 0.65f, 0, 1.65f, 0.55f, 2.6f, rojo, verde, azul); // Dibuja la carrocería con el color del vehículo.
        cabina.dibujar(x, z, angulo, rojo, verde, azul); // La misma cabina que el jugador, con el color de este vehículo.
        float[] ladosRuedas = {-0.88f, 0.88f}; // Ubica ruedas a izquierda y derecha.
        float[] ejesRuedas = {-0.82f, 0.82f}; // Ubica las ruedas delanteras y traseras.
        for (float ladoX : ladosRuedas) { // Selecciona uno de los dos lados del vehículo.
            for (float ejeZ : ejesRuedas) { // Selecciona el eje delantero o trasero.
                pieza(cubo, ladoX, 0.38f, ejeZ, 0.24f, 0.58f, 0.6f, 0.055f, 0.065f, 0.08f); // Dibuja una rueda oscura.
            }
        }
        float[] ladosLuces = {-LADO_LUZ, LADO_LUZ}; // Define la separación lateral de las luces.
        boolean encendidas = lucesEncendidas(); // Se consulta una vez por dibujo: de noche encendidas, de día apagadas.
        float[] faro = LucesVehiculo.colorFaro(encendidas); // Blanco emisivo encendido o gris oscuro apagado.
        float[] trasera = LucesVehiculo.colorTrasera(encendidas, false); // Luz de posición de noche; el tráfico no frena con luces.
        float[] tf = LucesVehiculo.TAMANO_FARO; // Medidas del faro.
        float[] tt = LucesVehiculo.TAMANO_TRASERA; // Medidas de la luz trasera.
        for (float ladoX : ladosLuces) { // Repite el dibujo para ambos lados.
            shader.entero("uEmision", (int) faro[3]); // Emisivo solo si está encendido: brilla con su propio color.
            pieza(cubo, ladoX, ALTURA_LUZ, FRENTE_LUZ, tf[0], tf[1], tf[2], faro[0], faro[1], faro[2]); // Dibuja un faro delantero.
            shader.entero("uEmision", (int) trasera[3]); // Evita que la iluminación oscurezca la luz de posición.
            pieza(cubo, ladoX, ALTURA_LUZ, TRASERA_LUZ, tt[0], tt[1], tt[2], trasera[0], trasera[1], trasera[2]); // Dibuja una luz trasera roja.
        }
        shader.entero("uEmision", 0); // Restablece el material normal para el siguiente objeto.
    }

    /** Transforma una pieza del espacio local del vehículo al espacio de la ciudad (igual que Auto.pieza). */
    private void pieza(Cubo cubo, float localX, float y, float localZ, float sx, float sy, float sz, float r, float g, float b) {
        float coseno = (float) Math.cos(angulo); // Calcula el coseno de la orientación.
        float seno = (float) Math.sin(angulo); // Calcula el seno de la misma orientación.
        float mundoX = x + coseno * localX + seno * localZ; // Gira la posición local y suma la posición X del vehículo.
        float mundoZ = z - seno * localX + coseno * localZ; // Gira la posición local y suma la posición Z del vehículo.
        cubo.cajaGirada(mundoX, y, mundoZ, sx, sy, sz, r, g, b, angulo); // Dibuja la pieza con la orientación del vehículo.
    }

    // ==================== 5. CONSULTAS DEL ESTADO ====================

    /** Devuelve la posición horizontal del centro del vehículo. */
    public float getX() {
        return x; // Coordenada X actual.
    }

    /** Devuelve la posición en profundidad del centro del vehículo. */
    public float getZ() {
        return z; // Coordenada Z actual.
    }

    /** Devuelve la orientación en radianes. */
    public float getAngulo() {
        return angulo; // Ángulo actual.
    }

    /** Devuelve la velocidad actual en unidades por segundo. */
    public float getVelocidad() {
        return velocidad; // Velocidad actual.
    }
}
