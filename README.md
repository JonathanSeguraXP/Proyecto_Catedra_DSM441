# The White Smile

Aplicación móvil para Android desarrollada en Kotlin como Proyecto de Cátedra de la
materia DSM441. El proyecto implementa el módulo de autenticación de usuarios mediante
Firebase Authentication e incluye inicio de sesión con correo electrónico y contraseña,
registro de cuentas, recuperación de contraseña, acceso con proveedores externos
(Google y GitHub) y una pantalla de menú principal posterior al inicio de sesión.

## Descripción general

La aplicación está organizada con el patrón Modelo-Vista-ViewModel (MVVM) y una
separación por capas. Las pantallas delegan la lógica en sus respectivos ViewModel,
que exponen el estado mediante flujos (`StateFlow`) y las acciones de una sola vez
mediante eventos (`SharedFlow`). El acceso a los servicios de autenticación se
centraliza en un repositorio, de modo que las Activity no dependen directamente de
Firebase. Esta organización facilita las pruebas unitarias y permite reemplazar la
implementación de datos sin modificar la interfaz de usuario.

## Funcionalidades

### Autenticación

- Inicio de sesión con correo electrónico y contraseña, validado con Firebase Authentication.
- Registro de nuevos usuarios con nombre, correo electrónico y contraseña.
- Recuperación de contraseña mediante un enlace de restablecimiento enviado por correo.
- Inicio de sesión con Google a través de Credential Manager.
- Inicio de sesión con GitHub mediante el flujo OAuth de Firebase.
- Cierre de sesión con confirmación previa.
- Validación del formato del correo electrónico y del estado de la conexión antes de
  realizar cada operación.
- Opción para recordar el correo electrónico en el dispositivo.

### Menú principal

- Acceso restringido a usuarios con una sesión activa; en caso contrario se redirige
  al inicio de sesión.
- Saludo personalizado con el nombre del usuario autenticado.
- Logotipo de la aplicación y grilla de opciones (turnos, tratamientos, perfil,
  historial, contacto y ayuda). La opción Perfil abre su pantalla; el resto se
  muestra como próximamente disponible, conforme al alcance definido en el enunciado.
- Confirmación antes de salir de la aplicación.

### Perfil de usuario

- Visualización del nombre, el correo electrónico y el estado de verificación de la cuenta.
- Edición del nombre con validaciones de campo vacío, longitud máxima y nombre sin cambios.
- Reenvío del correo de verificación de la cuenta.
- Cambio de contraseña mediante un enlace de restablecimiento enviado por correo.
- Actualización automática del estado al volver a la pantalla, y del saludo del menú
  después de editar el nombre.

## Tecnologías utilizadas

| Tecnología | Uso |
|------------|-----|
| Kotlin | Lenguaje de programación |
| Android SDK (minSdk 24, targetSdk 37) | Plataforma |
| Material 3 | Interfaz de usuario |
| Firebase Authentication | Autenticación de usuarios |
| Firebase Auth OAuth (Google, GitHub) | Inicio de sesión con proveedores |
| Android Credential Manager | Gestión de credenciales |
| MVVM y Kotlin Coroutines | Separación entre vista, estado y datos |
| JUnit 4 y coroutines-test | Pruebas unitarias de los ViewModel |

## Arquitectura

El proyecto se divide en las siguientes capas:

| Capa | Paquete | Responsabilidad |
|------|---------|-----------------|
| Vista | `*Activity.kt` | Presenta la pantalla, escucha los eventos y gestiona la navegación. No accede directamente a Firebase. |
| Presentación | `ui/` | ViewModel con el estado (`StateFlow`) y los eventos de una sola vez (`SharedFlow`). |
| Dominio | `domain/` | Contratos y modelos sin dependencias de Android: `ConnectivityChecker`, `EmailValidator`, `SessionUser` y `AuthResult`. |
| Datos | `data/` | Contrato `AuthRepository` e implementaciones concretas: Firebase, estado de la red y validación del correo. |
| Utilidades | `core/`, `util/` | Inyección de dependencias manual, adaptación de `Task` a corrutinas y traducción de errores. |

El flujo de una operación es el siguiente: la Activity invoca un método del ViewModel;
este valida los datos, consulta el repositorio y actualiza el estado; la Activity
reacciona mediante `render(state)` o `handleEvent(event)`. Las dependencias se
construyen manualmente en `core/ServiceLocator.kt`.

## Estructura del proyecto

```
app/src/main/java/com/example/the_white_smile/
  LoginActivity.kt               # Vista del inicio de sesión
  RegisterActivity.kt            # Vista del registro
  MenuActivity.kt                # Vista del menú principal
  ProfileActivity.kt             # Vista del perfil de usuario
  core/
    ServiceLocator.kt            # Inyección de dependencias manual
    TaskExtensions.kt            # Adaptación de Task de Firebase a corrutinas
    ErrorUtils.kt                # Errores que no deben mostrarse
  data/
    AuthRepository.kt            # Contrato de autenticación
    FirebaseAuthRepository.kt    # Implementación con Firebase
    AndroidConnectivityChecker.kt
    AndroidEmailValidator.kt
  domain/
    AuthResult.kt
    SessionUser.kt
    ConnectivityChecker.kt
    EmailValidator.kt
  ui/
    login/                       # LoginViewModel, estado y eventos
    register/                    # RegisterViewModel, estado y eventos
    menu/                        # MenuViewModel, estado y eventos
    profile/                     # ProfileViewModel, estado y eventos
  util/
    FirebaseErrors.kt            # Traducción de errores de Firebase
    NetworkChecker.kt            # Estado de la conexión

app/src/test/java/com/example/the_white_smile/
  core/
    MainDispatcherRule.kt        # Dispatcher principal para las pruebas
  testing/
    Fakes.kt                     # Dobles de repositorio, red y validador
  ui/login/
    LoginViewModelTest.kt        # Pruebas del LoginViewModel
  ui/profile/
    ProfileViewModelTest.kt      # Pruebas del ProfileViewModel

app/src/main/res/
  layout/
    activity_login.xml           # Diseño del inicio de sesión
    activity_register.xml        # Diseño del registro
    activity_menu.xml            # Diseño del menú
  values/                        # Cadenas de texto, colores y temas
  drawable/                      # Iconos de la interfaz
```

## Requisitos

- Android Studio en una versión reciente.
- Cuenta de Firebase.
- Dispositivo Android (físico o emulador) con Android 7.0 o superior.
- Conexión a internet para las operaciones de autenticación.

## Configuración del proyecto

### 1. Clonar el repositorio

```bash
git clone https://github.com/JonathanSeguraXP/Proyecto_Catedra_DSM441.git
```

### 2. Configurar Firebase

1. Crear un proyecto en Firebase Console.
2. Agregar una aplicación Android con el paquete `com.example.the_white_smile`.
3. Descargar el archivo `google-services.json` y ubicarlo en la carpeta `app/`.
4. En Authentication, sección Sign-in method, habilitar los métodos:
   - Correo electrónico y contraseña.
   - Google, para el inicio de sesión con Google.
   - GitHub, configurando previamente una OAuth App en GitHub.
5. Registrar el SHA-1 de la firma de depuración en la configuración del proyecto
   para que el inicio de sesión con Google funcione correctamente.

### 3. Compilar y ejecutar

Abrir el proyecto en Android Studio, esperar la sincronización de Gradle y ejecutar
la aplicación con el botón Run.

## Pruebas

### Pruebas unitarias

```bash
./gradlew :app:testDebugUnitTest
```

`LoginViewModelTest` verifica las validaciones de los campos, la falta de conexión,
el estado de carga y los casos de éxito y error. `ProfileViewModelTest` cubre la carga
de la sesión, la edición del nombre, el envío de la verificación y el cambio de
contraseña. Las pruebas utilizan dobles de prueba, por lo que no requieren conexión,
Firebase ni emulador.

### Prueba manual

1. En la pantalla de inicio de sesión, seleccionar Registrarse y crear una cuenta
   con correo electrónico y contraseña.
2. Volver al inicio de sesión e ingresar con esas credenciales.
3. Verificar la redirección al menú principal y el saludo con el nombre del usuario.
4. Probar la recuperación de contraseña ingresando un correo registrado.
5. Probar el inicio de sesión con Google y con GitHub.
6. Abrir la opción Perfil, editar el nombre y verificar que el saludo del menú se actualice.
7. Probar el reenvío del correo de verificación y el cambio de contraseña.
8. Cerrar sesión y confirmar el regreso a la pantalla de inicio de sesión.

## Requisitos del enunciado

- [x] Inicio de sesión con usuario (correo electrónico) y contraseña.
- [x] Recuperación de contraseña mediante correo de restablecimiento.
- [x] Registro de usuarios mediante correo electrónico, Google y GitHub.
- [x] Pantalla de menú principal posterior a un inicio de sesión válido.
- [x] Logotipo de la aplicación en la pantalla del menú.
- [x] Módulo de inicio de sesión desarrollado en Kotlin con Android Studio.

## Autor

- Jonathan Josue Segura Ramirez, carné SR230847

---

Proyecto académico correspondiente a la materia DSM441.
