# The White Smile 🦷

Aplicación Android desarrollada en **Kotlin** para el Proyecto de Cátedra de la materia **DSM441**. El proyecto implementa el **módulo de login** con autenticación segura mediante **Firebase Authentication**.

## ✨ Funcionalidades

### Módulo de Login
- **Inicio de sesión con usuario (correo electrónico) y contraseña**, validado en la nube con Firebase Authentication.
- **Registro de nuevos usuarios** directamente desde la app (nombre, correo y contraseña).
- **Recuperación de contraseña**: el usuario ingresa su correo y recibe un enlace de restablecimiento por email.
- **Inicio de sesión con proveedores OAuth** (opcional según enunciado):
  - **Google** Sign-In
  - **GitHub** Sign-In
- **Menú principal** con el logo de la app, mostrado tras un inicio de sesión válido (las opciones aún no están funcionales, según lo pedido en el enunciado).
- **Cerrar sesión** para volver a la pantalla de login.

## 🛠️ Tecnologías

| Tecnología | Uso |
|------------|-----|
| Kotlin | Lenguaje de programación |
| Android SDK (minSdk 24, targetSdk 37) | Plataforma |
| Material 3 | Interfaz de usuario |
| Firebase Authentication | Autenticación de usuarios |
| Firebase Auth OAuth (Google, GitHub) | Login con proveedores |
| Android Credential Manager | Métodos de autenticación modernos |
| MVVM + Kotlin Coroutines | Separación entre vista, estado y datos |
| JUnit 4 + coroutines-test | Pruebas unitarias de los ViewModel |

## 🏗️ Arquitectura

El proyecto sigue el patrón **MVVM** con una separación en capas:

| Capa | Paquete | Responsabilidad |
|------|---------|-----------------|
| Vista | `*Activity.kt` | Pinta la pantalla, escucha los eventos y navega. No accede a Firebase. |
| Presentación | `ui/` | `ViewModel` con el estado (`StateFlow`) y los eventos de una sola vez (`SharedFlow`). |
| Dominio | `domain/` | Contratos y modelos sin dependencias de Android (`ConnectivityChecker`, `EmailValidator`, `SessionUser`, `AuthResult`). |
| Datos | `data/` | Contrato `AuthRepository` e implementaciones concretas: Firebase, estado de red y validación de correo. |
| Utilidades | `core/`, `util/` | Inyección de dependencias manual, extensión de `Task` a corrutinas y traducción de errores. |

Flujo de una acción: la Activity llama a un método del `ViewModel`, este valida, consulta el repositorio y actualiza el estado; la Activity reacciona con `render(state)` o `handleEvent(event)`. Las dependencias se arman a mano en `core/ServiceLocator.kt`.

## 📁 Estructura del proyecto

```
app/src/main/java/com/example/the_white_smile/
├── LoginActivity.kt              # Vista del inicio de sesión
├── RegisterActivity.kt           # Vista del registro
├── MenuActivity.kt               # Vista del menú principal
├── core/
│   ├── ServiceLocator.kt         # Inyección de dependencias manual
│   ├── TaskExtensions.kt         # Task de Firebase -> corrutinas
│   └── ErrorUtils.kt             # Errores que no deben mostrarse
├── data/
│   ├── AuthRepository.kt         # Contrato de autenticación
│   ├── FirebaseAuthRepository.kt # Implementación con Firebase
│   ├── AndroidConnectivityChecker.kt
│   └── AndroidEmailValidator.kt
├── domain/
│   ├── AuthResult.kt
│   ├── SessionUser.kt
│   ├── ConnectivityChecker.kt
│   └── EmailValidator.kt
├── ui/
│   ├── login/                    # LoginViewModel, estado y eventos
│   ├── register/                 # RegisterViewModel, estado y eventos
│   └── menu/                     # MenuViewModel, estado y eventos
└── util/
    ├── FirebaseErrors.kt         # Errores de Firebase en español
    └── NetworkChecker.kt         # Estado de la conexión

app/src/test/java/com/example/the_white_smile/
├── core/MainDispatcherRule.kt    # Dispatcher principal para tests
└── ui/login/LoginViewModelTest.kt # Pruebas del LoginViewModel

app/src/main/res/
├── layout/
│   ├── activity_login.xml        # Diseño del login
│   ├── activity_register.xml     # Diseño del registro
│   └── activity_menu.xml         # Diseño del menú
├── values/                       # Strings, colores, temas
└── drawable/                     # Iconos (Google, GitHub, etc.)
```

## 🔧 Requisitos

- [Android Studio](https://developer.android.com/studio) (versión reciente).
- Cuenta de [Firebase](https://console.firebase.google.com/).
- Dispositivo Android (emulador o físico) con Android 7.0 o superior.

## 🚀 Configuración del proyecto

### 1. Clonar el repositorio
```bash
git clone https://github.com/JonathanSeguraXP/Proyecto_Catedra_DSM441.git
```

### 2. Configurar Firebase
1. Crea un proyecto en [Firebase Console](https://console.firebase.google.com/).
2. Agrega una app Android con el paquete `com.example.the_white_smile`.
3. Descarga el archivo `google-services.json` y colócalo en la carpeta `app/`.
4. En **Authentication → Sign-in method**, habilita:
   - **Email/Password**
   - **Google** (si deseas el botón de Google)
   - **GitHub** (configurando un OAuth App en GitHub, si deseas ese botón)
5. Registra el **SHA-1** de tu firma de depuración en **Configuración del proyecto → Tus apps** para que Google Sign-In funcione.

### 3. Compilar y ejecutar
Abre el proyecto en Android Studio, espera la sincronización de Gradle y presiona **Run ▶**.

## 🧪 Cómo probar

### Pruebas automáticas

```bash
./gradlew :app:testDebugUnitTest
```

`LoginViewModelTest` cubre las validaciones, la falta de conexión, el estado de carga y los casos de éxito y error usando dobles de prueba: no requiere red, Firebase ni emulador.

### Prueba manual

1. En la pantalla de login toca **Registrarse** y crea una cuenta (correo + contraseña).
2. Vuelve al login e **inicia sesión** con esas credenciales.
3. Eres dirigido al **menú principal** con el logo de la app.
4. Para **recuperar la contraseña**, escribe tu correo y toca "¿Olvidaste tu contraseña?".
5. También puedes iniciar con los botones de **Google** o **GitHub**.

## 📋 Enunciado cubierto

- [x] Usuario (correo electrónico) y contraseña.
- [x] Opción para recuperar password (correo de restablecimiento).
- [x] Opciones para registrarse mediante Google y GitHub.
- [x] Pantalla de menú principal tras un usuario válido (aunque las opciones no funcionen aún).
- [x] Logo de la app en la pantalla del menú.
- [x] Módulo de login desarrollado en Kotlin con Android Studio.

## 👥 Integrante

- Jonathan Josue Segura Ramirez SR230847

---

*Proyecto académico — materia DSM441.*
