## Conceptos clave: Shaders

- **Shader**: un programita chiquito que corre en la tarjeta gráfica (GPU), no en el CPU. La GPU lo ejecuta miles de veces en paralelo, no una sola vez como el código normal.

- **Vértice**: un punto en el espacio (coordenadas x, y, z). Un triángulo necesita 3. Es justo lo que se arma en `crearTriangulo()` con el `float[]` de posiciones.

- **Vertex shader**: procesa cada vértice y calcula dónde va a aparecer en pantalla. Es el `gl_Position = vec4(aPos, 1.0)` del vertex shader — responde "¿dónde está el objeto?".

- **Fragmento**: un "candidato a píxel". Cuando la GPU arma los triángulos con los vértices, cada pedacito de superficie que cubre se convierte en un fragmento.

- **Fragment shader**: calcula el color de cada fragmento. Es la línea `fragColor = vec4(1.0, 0.5, 0.2, 1.0)` (el naranja del triángulo) — responde "¿cómo se ve el objeto?".

- **Diferencia clave**: el vertex shader define **posición/geometría**, el fragment shader define **apariencia/color**. Ambos trabajan juntos: uno decide dónde, el otro decide cómo se ve.