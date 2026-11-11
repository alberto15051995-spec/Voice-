# Lilith Voice v0.3 — prototipo Android

Asistente personal experimental para Android 8+ (objetivo: Xiaomi 15T Pro / Android 16).

## Novedades

- Detecta por Vosk (modelo local español) «Oye, Lilith» y algunas variantes («oye lilit»).
- Responde con TextToSpeech español de Android. Intenta usar voz femenina si está identificada en las voces del sistema; no garantiza género ni una voz concreta.
- Muestra un orbe animado rojo flotante en la pantalla encendida cuando despierta, con permiso de superposición explícito.
- Después de despertar, entiende prototipos de comandos: «qué hora es», «gracias» y «abre ChatGPT» (este último muestra notificación para que el usuario la pulse).
- Servicio de micrófono en primer plano con notificación persistente y botón de parada.
- No requiere API key ni cuenta externa. Vosk procesa voz localmente; Google/Android TTS puede usar una voz del sistema que tenga componente de red, según ajustes del dispositivo.

## Instalación y prueba

1. Compilar con `.github/workflows/build-apk.yml` desde GitHub Actions.
2. Descargar el artefacto `LilithVoice-v0.3-debug`, extraer e instalar `app-debug.apk`.
3. Abrir Lilith Voice, pulsar «1. Permitir burbuja flotante» y conceder **Mostrar sobre otras aplicaciones**.
4. Pulsar «2. Activar Oye Lilith» y conceder micrófono y notificaciones.
5. Esperar a la notificación «Escuchando Oye Lilith» y decir «Oye, Lilith». Debe aparecer el orbe y sonar una frase de bienvenida si Android TTS está disponible.
6. Intentar «Qué hora es» durante la breve ventana de escucha tras el saludo.
7. Detener desde la aplicación o la notificación. En HyperOS se puede configurar batería sin restricciones si Android detiene el servicio.

## Límites deliberados

- No es todavía un asistente del sistema (`VoiceInteractionService`), no se activa automáticamente tras reiniciar, no controla aplicaciones libremente y no puede superar restricciones de la pantalla bloqueada.
- La detección continua con ASR de Vosk puede gastar batería y equivocarse más que un modelo entrenado de palabra de activación. El usuario debe iniciarla estando desbloqueado.
- La burbuja requiere permiso especial `SYSTEM_ALERT_WINDOW`; no se muestra encima de pantallas protegidas por el sistema ni se fuerza el desbloqueo.
- No incluye una IA conversacional remota. Las respuestas y órdenes son demostraciones limitadas.
- El reconocimiento del comando siguiente al saludo puede variar según la voz TTS y el micrófono del móvil. Debe probarse en hardware real.
- No se ha auditado el binario para distribución pública. Usar como prototipo de desarrollo en dispositivo propio.

## Proyecto

Código Java nativo, Gradle, Vosk Android 0.3.47 y Android TTS. El workflow descarga el modelo español `vosk-model-small-es-0.42` antes de compilar.
