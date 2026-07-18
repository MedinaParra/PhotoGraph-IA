# PhotoGraph-IA

Aplicación Android para levantamientos geométricos asistidos por IA mediante fotografías y video, orientada inicialmente a componentes mecánicos industriales como poleas de correas transportadoras.

## Objetivo

Capturar una secuencia alrededor de una pieza en terreno y convertirla en información geométrica útil para inspección, reconstrucción 3D y generación posterior de modelos CAD.

## Alcance inicial

- Captura guiada de fotografías y video desde Android.
- Control básico de cobertura, desenfoque e iluminación.
- Registro de una referencia de escala conocida.
- Organización y exportación de cada levantamiento.
- Preparación de datos para fotogrametría.
- Reconocimiento progresivo de cilindros, ejes, mantos y formas primarias.
- Integración futura con FreeCAD para generar geometría paramétrica.

## Flujo previsto

1. Crear una orden o sesión de levantamiento.
2. Registrar los datos de la pieza y una referencia dimensional.
3. Realizar una captura guiada alrededor del componente.
4. Validar la calidad y cobertura de las imágenes.
5. Procesar la reconstrucción 3D localmente o en un servidor.
6. Detectar formas geométricas y estimar dimensiones.
7. Revisar el resultado antes de exportarlo.

## Plataforma propuesta

- Android
- Kotlin
- CameraX
- Jetpack Compose
- OpenCV
- ARCore, cuando el dispositivo sea compatible
- Motor de fotogrametría por definir
- FreeCAD como entorno de validación y generación CAD

## Estado

Proyecto en etapa inicial de definición y prototipado.

> Las mediciones generadas mediante visión artificial o IA deben conservar su incertidumbre y ser verificadas antes de emplearse en fabricación, mantenimiento o decisiones de ingeniería.
