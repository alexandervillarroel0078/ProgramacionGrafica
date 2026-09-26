#version 330 core // Selecciona la versión GLSL correspondiente a OpenGL 3.3.
// SHADER DE FRAGMENTOS CON ILUMINACIÓN: calcula el color iluminado de cada fragmento.
// Reemplaza al shader de color plano (plano.frag). Los uniforms de luz los envía Iluminacion.preparar().
// El shader calcula iluminación local: este ejemplo todavía no proyecta sombras.
in vec3 vMundo; // Recibe la posición del fragmento en la ciudad.
in vec3 vNormal; // Recibe la dirección perpendicular a la superficie.
uniform vec3 uColor; // Recibe el color base de la caja.
uniform vec3 uLuces[16]; // Recibe las posiciones de las farolas; 16 es el máximo (Iluminacion.MAX_LUCES).
uniform int uNumLuces; // Recibe cuántas posiciones de uLuces están en uso (Iluminacion.LUCES.length).
uniform vec3 uAuto; // Recibe la posición del auto a la altura de los faros.
uniform vec3 uFrente; // Recibe la dirección hacia la que apunta el vehículo.
uniform int uNoche; // Vale 1 de noche y 0 de día.
uniform int uFaros; // Vale 1 cuando los focos están encendidos.
uniform int uEmision; // Vale 1 si el objeto debe conservar su color sin oscurecerse.
uniform int uMapa; // Vale 1 durante el dibujo del minimapa de Minimapa.
out vec4 color; // Entrega el color RGBA final al framebuffer.

// ==================== CONSTANTES DE ILUMINACIÓN (valores ajustables) ====================
// Ambiente y sol.
const vec3 AMBIENTE_DIA = vec3(0.48); // Luz ambiental diurna que llega a todas las caras (gris neutro).
const vec3 AMBIENTE_NOCHE = vec3(0.12, 0.16, 0.24); // Luz ambiental nocturna: tenue y azulada.
const float INTENSIDAD_SOL_DIA = 0.65; // Fuerza de la luz direccional del sol durante el día.
const float INTENSIDAD_SOL_NOCHE = 0.10; // Luz direccional residual de noche (luna).
const vec3 DIRECCION_SOL = vec3(0.4, 1.0, 0.3); // Hacia dónde está el sol (se normaliza al usarla): alto, algo al este y al sur.
// Farolas: atenuación = 1 + LINEAL · d + CUADRATICA · d². Valores más chicos = mayor alcance.
const float FAROLA_ATENUACION_LINEAL = 0.12; // Término lineal de la atenuación de cada farola.
const float FAROLA_ATENUACION_CUADRATICA = 0.045; // Término cuadrático: domina lejos y define el radio útil de la luz.
const vec3 COLOR_FAROLA = vec3(1.0, 0.73, 0.34); // Tono cálido (sodio) de las farolas.
const float INTENSIDAD_FAROLA = 3.0; // Multiplicador del aporte de cada farola.
// Faros del auto: posición, cono y alcance.
const float FAROS_SEPARACION = 0.55; // Distancia lateral de cada faro al centro del auto.
const float FAROS_AVANCE = 1.36; // Distancia del centro del auto al frente, donde nacen los haces.
const float FAROS_INCLINACION = 0.10; // Cuánto se inclinan los haces hacia el suelo.
const float CONO_BORDE = 0.85; // Coseno del borde exterior del haz (≈ 32° desde el eje): fuera de aquí no hay luz.
const float CONO_CENTRO = 0.97; // Coseno del núcleo del haz (≈ 14°): dentro de aquí la luz es plena.
const float FAROS_ATENUACION_CUADRATICA = 0.04; // Alcance de los faros: atenuación = 1 + 0.04 · d².
const vec3 COLOR_FARO = vec3(1.0, 0.94, 0.72); // Luz frontal blanca y cálida.
const float INTENSIDAD_FARO = 8.0; // Multiplicador del aporte de cada faro.
// Faros del tráfico: mismo cono que los del jugador, pero más débiles y de menor alcance.
const int MAX_FAROS_TRAFICO = 16; // Tamaño de los arreglos de focos de tráfico; debe coincidir con Trafico.MAX_FAROS_TRAFICO.
const float INTENSIDAD_FARO_TRAFICO = 4.0; // Multiplicador de cada foco de tráfico: la mitad que el faro del jugador.
const float ALCANCE_FARO_TRAFICO = 4.0; // Distancia a la que un foco de tráfico rinde la mitad (el del jugador ≈ 5).

uniform vec3 uFarosTrafico[MAX_FAROS_TRAFICO]; // Posición de cada foco de tráfico (dos por vehículo), enviada por Trafico.
uniform vec3 uDireccionFarosTrafico[MAX_FAROS_TRAFICO]; // Dirección de avance del vehículo dueño de cada foco.
uniform int uNumFarosTrafico; // Cuántos focos están en uso; de día vale 0 y el bucle no calcula nada.

// ==================== FUNCIÓN DE CONO (un foco con smoothstep y atenuación) ====================
// La usan los faros del jugador y los del tráfico, así el cálculo del cono existe una sola vez.
vec3 aporteFoco(vec3 origen, vec3 frente, vec3 normal, float atenuacionCuadratica, float intensidad) {
    vec3 haciaSuperficie = vMundo - origen; // Forma el vector del faro al fragmento.
    float distancia = length(haciaSuperficie); // Mide la distancia recorrida por la luz.
    vec3 eje = normalize(frente + vec3(0.0, -FAROS_INCLINACION, 0.0)); // Inclina el foco ligeramente hacia el suelo.
    float alineacion = dot(normalize(haciaSuperficie), eje); // Un valor cercano a 1 indica el centro del haz.
    float cono = smoothstep(CONO_BORDE, CONO_CENTRO, alineacion); // Suaviza el borde entre el exterior y el interior del foco.
    float difusa = max(dot(normal, -normalize(haciaSuperficie)), 0.0); // Mide cuánto mira la cara hacia el faro.
    float atenuacion = 1.0 + atenuacionCuadratica * distancia * distancia; // Disminuye la intensidad al alejarse.
    vec3 colorFaro = COLOR_FARO; // Define una luz frontal blanca y cálida.
    return colorFaro * cono * difusa * intensidad / atenuacion; // Devuelve el aporte del foco a la iluminación total.
}

// ==================== CÁLCULO DE LUZ EN LA GPU ====================
void main() { // Se ejecuta para cada fragmento visible de una caja.
    if (uEmision == 1 || uMapa == 1) { // Bombillas y minimapa usan colores directos.
        color = vec4(uColor, 1.0); // Conserva el color base con opacidad completa.
        return; // Termina el shader sin calcular iluminación.
    }

    vec3 normal = normalize(vNormal); // Convierte la normal interpolada en un vector unitario.
    vec3 luz = AMBIENTE_DIA; // Define la luz ambiental diurna que llega a todas las caras.
    float intensidadSol = INTENSIDAD_SOL_DIA; // Define la fuerza de la iluminación direccional diurna.
    if (uNoche == 1) { // Ajusta el ambiente si es de noche.
        luz = AMBIENTE_NOCHE; // Usa una luz ambiental tenue y azulada.
        intensidadSol = INTENSIDAD_SOL_NOCHE; // Conserva una pequeña luz direccional nocturna.
    }
    vec3 direccionSol = normalize(DIRECCION_SOL); // Define una fuente lejana por su dirección.
    float incidenciaSol = max(dot(normal, direccionSol), 0.0); // Lambert: una cara recibe más luz si mira al sol.
    luz += vec3(intensidadSol) * incidenciaSol; // Suma la contribución direccional al ambiente.

    if (uNoche == 1) { // Calcula la iluminación de las farolas solo de noche.
        for (int indice = 0; indice < uNumLuces; indice++) { // Acumula el aporte de cada bombilla.
            vec3 haciaLuz = uLuces[indice] - vMundo; // Forma el vector desde la superficie hacia la farola.
            float distancia = length(haciaLuz); // Mide cuántas unidades separan superficie y bombilla.
            float difusa = max(dot(normal, normalize(haciaLuz)), 0.0); // Calcula la incidencia de la luz sobre la cara.
            float atenuacion = 1.0 + FAROLA_ATENUACION_LINEAL * distancia + FAROLA_ATENUACION_CUADRATICA * distancia * distancia; // Reduce el alcance con la distancia.
            vec3 colorFarola = COLOR_FAROLA; // Define el tono cálido de la farola.
            luz += colorFarola * difusa * INTENSIDAD_FAROLA / atenuacion; // Suma el aporte atenuado de esta bombilla.
        }
    }

    if (uFaros == 1) { // Calcula los conos únicamente si están encendidos.
        for (int indice = 0; indice < 2; indice++) { // Repite el cálculo para los dos faros.
            vec3 lateral = vec3(-uFrente.z, 0.0, uFrente.x); // Obtiene la dirección hacia el lado derecho del auto.
            float separacion = -FAROS_SEPARACION; // Selecciona inicialmente el faro izquierdo.
            if (indice == 1) { // Comprueba si corresponde calcular el segundo faro.
                separacion = FAROS_SEPARACION; // Desplaza el segundo faro al lado derecho.
            }
            vec3 origen = uAuto + uFrente * FAROS_AVANCE + lateral * separacion; // Ubica el faro delante de la carrocería.
            luz += aporteFoco(origen, uFrente, normal, FAROS_ATENUACION_CUADRATICA, INTENSIDAD_FARO); // Añade el aporte del foco a la iluminación total.
        }
    }

    // Faros del tráfico: independientes de la tecla F. Trafico envía uNumFarosTrafico = 0 de día.
    float atenuacionTrafico = 1.0 / (ALCANCE_FARO_TRAFICO * ALCANCE_FARO_TRAFICO); // Con d = alcance, 1 + d²/alcance² = 2: rinde la mitad.
    for (int indice = 0; indice < uNumFarosTrafico; indice++) { // Recorre solo los focos en uso.
        luz += aporteFoco(uFarosTrafico[indice], uDireccionFarosTrafico[indice], normal, atenuacionTrafico, INTENSIDAD_FARO_TRAFICO); // Mismo cono, más débil y corto.
    }

    color = vec4(uColor * luz, 1.0); // Multiplica el material por toda la luz acumulada.
}
