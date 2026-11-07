## [Nombre aquí] Oigan se va a llamar SARA? Comenten

Aplicación Android accesible para graficar funciones con GeoGebra pensada para personas con discapacidad visual.
Permite escribir una función con un teclado matemático compatible con TalkBack, graficarla y **escucharla**


## Características

- **Teclado matemático propio** con tres páginas (`123`, `f(x)` y `abc`). Cada tecla tiene descripción hablada para TalkBack y vibración al pulsarla.
- **Notación en español**: se escribe `sen`, `raiz`, `log`, etc. La app lo traduce al formato de GeoGebra.
- **Parámetros con controles deslizantes**: las letras distintas de `x` e `y` (`a`, `b`, `c`…) se convierten en controles de −10 a 10, (máximo 6).
- **Sonificación de la gráfica**: eje X = tiempo, valor de Y = tono. Si hay varias ramas en un mismo `x` (como en un círculo), suenan hasta 4 tonos a la vez.
- **Voz sintetizada en español** (es-MX) para anunciar estados, errores y valores de parámetros cuando TalkBack no está activo.
- **Sección de aprendizaje** con explicaciones del plano cartesiano y de los tipos de funciones.

## Requisitos

- Android Studio [qué versión tenemos??].
- Dispositivo o emulador con Android 6.0 (Marshmallow) o superior.
- **Conexión a internet**: GeoGebra se carga en línea dentro de un WebView.

## Instalación

##### Por ahora no nos preocupamos por hacerla instalable de otra forma fuera de esta opción, veda?

1. Clona el repositorio:
   ```bash
   git clone [URL del repositorio]
   ```
2. Abre la carpeta en Android Studio y espera a que termine la sincronización de Gradle.
3. Comprueba que el archivo `geogebra.html` esté en `app/src/main/assets/`.
4. Ejecuta la app en un dispositivo o emulador con internet.

## Guía de uso

##### La neta todavía no la sé usar lol

### Ejemplos de entrada

```
| Escribes   | Resultado                                  |
|-----|------|                                            |
| x^2        | Función explícita sin parámetros           |
| a*x^2+b    | Función explícita con parámetros a y b     |
| sen(x)     | Se interpreta como `sin(x)`                |
| log(x)     | Logaritmo de base 10                       |
```

### Notación aceptada

```
| Español                       | GeoGebra                          |
|---------------|---------------|                                   |
| sen, tg, tang, ctg, cosec     | sin, tan, tan, cot, csc           |
| arcsen, arccos, arctan        | asin, acos, atan                  |
| raiz                          | sqrt                              |
| log (base 10), ln (natural)   | log10, ln                         |
```

También se reconocen `cos`, `tan`, `abs`, `exp`, `sinh`, `cosh`, `tanh`, `cbrt`, `floor`, `ceil`, `round`, `sgn` y la constante `pi`.
La multiplicación implícita funciona (`ax` equivale a `a*x`) y las funciones pegadas (`senx` equivale a `sen(x)`).

## Estructura del proyecto

```
app/src/main/
├── java/com/example/sara/
│   ├── MainActivity.kt          # Navegación entre Home, Aprendizaje y Graficar
│   ├── GraficaController.kt     # WebView con GeoGebra, parámetros y audio
│   ├── TecladoMatematico.kt     # Teclado en pantalla de tres páginas
│   ├── AnalizadorFuncion.kt     # Traduce la entrada del usuario al formato de GeoGebra
│   └── AccessibilityEngine.kt   # Voz (TTS), sonificación y vibración
├── assets/
│   └── geogebra.html            # Página con GeoGebra y el puente con Kotlin
└── res/
    ├── layout/                  # activity_main, vista_home, vista_aprendizaje, vista_grafica
    └── drawable/                # btn_morado, btn_amarillo, etc.
```

## Cómo funciona

1. El usuario escribe con `TecladoMatematico`.
2. `AnalizadorFuncion` genera un `PlanGrafica` (modo, curva, evaluación y parámetros) o un mensaje de error.
3. `GraficaController` envía el plan a `geogebra.html` con `evaluateJavascript`.
4. La página responde por el puente `Android.evento` con los eventos `listo`, `grafica`, `error` y `muestras`.
5. Al pulsar Reproducir, la página devuelve muestras de la curva.
6. `AccessibilityEngine` normaliza las muestras, sintetiza el audio y lo reproduce con `AudioTrack`.

## Accesibilidad

- Descripciones de contenido en teclas, pestañas y controles.
- Mensajes de estado en una región *live*, que TalkBack lee automáticamente.
- Voz propia cuando TalkBack no está activo.
- Contraste de las teclas.
- Sin información transmitida solo por color en los mensajes de la gráfica.

## Limitaciones conocidas

- Requiere internet para cargar GeoGebra.
- Solo grafica en 2D (no admite la variable `z`).
- Máximo 6 parámetros por gráfica.
- Al cambiar de pantalla se pierde la gráfica actual.

## Pendientes??

- Explorar de manera táctil la gráfica y vibración al cruzar ejes o puntos.
- Podríamos agregar dictado por voz?
- Conservar la gráfica al cambiar de pantalla.
- Modo sin conexión.

## Licencia y créditos

- Licencia del proyecto: *La documentación que usé de ejemplo decía que se pone licencia, pero no sé a qué carajo se refiere lol*

- Esta app usa [GeoGebra](https://www.geogebra.org), gratuito para uso no comercial y con requisitos de atribución.
