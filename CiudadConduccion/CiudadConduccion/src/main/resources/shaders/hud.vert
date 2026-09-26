#version 330 core // Selecciona GLSL 3.30 para el contexto OpenGL 3.3.
// SHADER DE VÉRTICES DEL HUD: dibuja en 2D, en píxeles de pantalla, sin cámara ni perspectiva.
// Lo usa Dibujo2D para los paneles y el texto de STBEasyFont. El origen (0, 0) es la esquina SUPERIOR izquierda,
// como en STBEasyFont; OpenGL usa -1..1 con Y hacia arriba, así que la Y se invierte al convertir.
layout (location = 0) in vec2 aPos; // Posición del vértice en píxeles, relativa a uOrigen y antes de escalar.
uniform vec2 uPantalla; // Ancho y alto del framebuffer en píxeles.
uniform vec2 uOrigen; // Dónde se ubica el (0, 0) del dibujo, en píxeles desde arriba a la izquierda.
uniform float uEscala; // Multiplica el tamaño: el texto de STBEasyFont mide unos 7 píxeles de alto con escala 1.

void main() { // Se ejecuta una vez por vértice.
    vec2 pixel = uOrigen + aPos * uEscala; // Escala el dibujo y lo traslada a su lugar en la pantalla.
    float ndcX = pixel.x / uPantalla.x * 2.0 - 1.0; // Convierte 0..ancho en -1..1 (izquierda a derecha).
    float ndcY = 1.0 - pixel.y / uPantalla.y * 2.0; // Convierte 0..alto en 1..-1: la Y de pantalla crece hacia abajo.
    gl_Position = vec4(ndcX, ndcY, 0.0, 1.0); // Sin perspectiva: W = 1.
}
