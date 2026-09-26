#version 330 core // Indica la versión del lenguaje del shader.
// SHADER DE FRAGMENTOS DE COLOR PLANO: define el color plano inicial, sin luces.
// Es el primer paso de la lección; iluminacion.frag reemplaza este shader por la iluminación.
// No necesita uniforms de luz (uNoche, uFaros, uLuces...): el shader inicial utiliza colores planos.
// Para comparar ambos resultados, basta pasar "/shaders/plano.frag" a Shader.crear() en Juego.
uniform vec3 uColor; // Recibe el color RGB enviado por Cubo.cajaGirada().
out vec4 color; // Define el color final que se escribe en la imagen.
void main() { // Se ejecuta para cada fragmento de la geometría dibujada.
    color = vec4(uColor, 1.0); // Copia el material y establece opacidad completa.
}
