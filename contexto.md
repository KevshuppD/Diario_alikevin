# Contexto del Proyecto: Diario Kevin & Ali (Diario Alikevin)

Este documento proporciona una visión detallada de la arquitectura, tecnologías y funcionalidades del proyecto Android "Diario", diseñado como un espacio compartido para parejas.

## 1. Visión General
**Diario** es una aplicación Android que permite a una pareja compartir mensajes (cartas), fotos (álbumes), eventos de calendario y recetas. Incluye elementos de gamificación como una mascota virtual compartida que reacciona a la interacción de los usuarios.

## 2. Stack Tecnológico
- **Lenguajes**: Kotlin (preferido para UI nueva) y Java (lógica heredada).
- **UI Framework**: Jetpack Compose (migración en curso) y XML (Legacy).
- **Backend**: 
    - **Firebase Auth**: Gestión de usuarios y sesiones.
    - **Cloud Firestore**: Base de datos en tiempo real con persistencia offline.
    - **Firebase Cloud Messaging (FCM)**: Notificaciones push (implementado con la API v1 mediante Google Auth).
- **Media**:
    - **Cloudinary**: Hosting de imágenes (subida firmada desde el cliente).
    - **Coil / Glide**: Carga y caché de imágenes.
    - **UCrop**: Recorte y edición básica de imágenes.
- **Otros**:
    - **WorkManager**: Para comprobaciones de actualización en segundo plano.
    - **OkHttp**: Peticiones de red (GitHub API para actualizaciones, FCM).
    - **GitHub API**: Sistema de auto-actualización personalizado.

## 3. Arquitectura y Estructura
El proyecto sigue una estructura de Android estándar, pero se encuentra en una fase de transición hacia Compose.

### Paquetes Principales (`calendario.kevshupp.diariokevinali`)
- `MainActivity.java`: El punto de entrada central. Maneja la navegación, inicialización de Firebase y actúa como contenedor de Fragments y vistas de Compose.
- `DiarioApp.java`: Clase Application. Configura Firebase, Cloudinary, WorkManager y la caché de Coil.
- `compose/`: Contiene todos los componentes de UI modernos en Jetpack Compose.
    - `MessageFeedCompose.kt`: Pantalla principal del feed de mensajes.
    - `MessageEditorCompose.kt`: Editor de "cartas" con soporte para fotos.
    - `CalendarCompose.kt`, `AlbumCompose.kt`, `RecipeCompose.kt`, etc.
- `managers/` (Lógica delegada):
    - `AlbumManager.kt`: Lógica de subida y edición de fotos del álbum.
    - `RecipeManager.kt`: Gestión de recetas.
    - `UpdateManager.kt`: Lógica para buscar e instalar actualizaciones desde GitHub.
- `models/`:
    - `Message.kt`: Representa mensajes, cartas y álbumes.
    - `Pet.kt`: Estado de la mascota (Felicidad, Nivel, Nombre).
    - `Recipe.kt`, `User.kt`, `CalendarEvent.kt`.

## 4. Funcionalidades Clave

### A. Feed de Mensajes y Cartas
- Los usuarios envían mensajes rápidos o "cartas" (título + contenido + imagen).
- Se muestran en un feed cronológico inverso en Compose.
- Soporte para "Likes" y filtrado por fecha.
- Notificaciones automáticas al enviar contenido.

### B. Mascota Virtual ("Thor")
- Una mascota compartida que aparece en el feed.
- Su felicidad disminuye con el tiempo (decay de 24h) y aumenta con la interacción (enviar mensajes).
- El estado emocional (Feliz/Triste) se refleja en su apariencia en el feed.

### C. Álbum de Fotos
- Permite subir fotos individuales o múltiples.
- Las fotos se almacenan en Cloudinary.
- El feed de álbumes es una vista filtrada de mensajes con tipo `ALBUM`.

### D. Calendario de Eventos
- Registro de fechas importantes compartidas.
- Persistencia en Firestore bajo la colección `calendar_events`.

### E. Sistema de Recetas
- Sección dedicada para guardar recetas favoritas de la pareja.

### F. Actualizaciones Automáticas
- La app consulta un repositorio de GitHub para verificar nuevas versiones.
- Descarga e instala el APK automáticamente si hay una actualización disponible.

## 5. Diseño y Estética
- **Tema**: "Pixel Art" / Retro.
- **Fuentes**: Uso intensivo de tipografía tipo Pixel (ej. `ic_back_pixel`).
- **Colores**: Paleta vibrante pero cohesiva, con soporte para temas claros y oscuros ("Pixel Claro" / "Pixel Oscuro").
- **UI Inmersiva**: La app oculta las barras de sistema para una experiencia a pantalla completa.

## 6. Configuración de Firebase / Firestore
- **Colección `messages`**: Documentos con `partnerId`, `authorId`, `timestamp`, `content`, `imageUrls`.
- **Colección `pets`**: Un documento por `partnerId` que guarda el estado de la mascota.
- **Colección `users`**: Información de perfil (`profileImageUrl`, `name`).
- **Colección `calendar_events`**: Eventos vinculados por `partnerId`.

## 7. Flujo de Trabajo para Cambios
1. **UI**: Priorizar el uso de componentes en `compose/`. Si se modifica algo en el feed, editar `MessageFeedCompose.kt`.
2. **Imágenes**: Usar `MediaManager` de Cloudinary para subidas.
3. **Navegación**: La lógica principal de cambio de pantallas está en `MainActivity.java` mediante el método `showFragment`.
4. **Contexto de Sesión**: Los IDs de usuario y pareja se manejan vía `SharedPreferences` en `DiarioPrefs`.

---
*Nota: Este archivo debe mantenerse actualizado ante cambios estructurales significativos para guiar correctamente a la IA en futuras iteraciones.*
