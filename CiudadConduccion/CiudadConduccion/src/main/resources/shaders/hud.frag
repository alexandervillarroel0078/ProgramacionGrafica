#version 330 core // Selecciona GLSL 3.30 para el contexto OpenGL 3.3.
// SHADER DE FRAGMENTOS DEL HUD: pinta cada píxel con un color uniforme, con transparencia.
// La mezcla (glBlendFunc) la activa Dibujo2D.comenzar() y la desactiva Dibujo2D.terminar().
uniform vec4 uColor; // Color RGBA; A < 1 deja ver la escena detrás (panel semitransparente).
out vec4 color; // Color final del fragmento.

void main() { // Se ejecuta para cada píxel cubierto por un panel o una letra.
    color = uColor; // El HUD no se ilumina: usa el color tal cual.
}
