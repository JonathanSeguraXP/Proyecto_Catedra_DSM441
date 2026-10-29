# The White Smile

Aplicación móvil para Android desarrollada en Kotlin como Proyecto de Cátedra de la
materia DSM441. El proyecto implementa el módulo de autenticación de usuarios mediante
Firebase Authentication e incluye inicio de sesión con correo electrónico y contraseña,
registro de cuentas, recuperación de contraseña, acceso con proveedores externos
(Google y GitHub), una pantalla de menú principal posterior al inicio de sesión, la
gestión del perfil del usuario, la administración de citas con Cloud Firestore, el
historial de citas pasadas y canceladas, un catálogo de tratamientos con su detalle,
una pantalla de contacto con accesos directos a las aplicaciones del dispositivo,
una sección de ayuda con preguntas frecuentes y un menú que se adapta al rol del
usuario (paciente, dentista o administrador).

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
  historial, contacto y ayuda). Todas las opciones abren su pantalla correspondiente.
- Confirmación antes de salir de la aplicación.

### Roles de usuario

- Tres roles: paciente (`PATIENT`), dentista (`DENTIST`) y administrador (`ADMIN`).
- El rol se guarda en el documento `users/{uid}` de Cloud Firestore, en el campo
  `role`. Los pacientes no requieren documento: si no existe, se asumen `PATIENT`.
- El menú se construye según el rol:
  - **Paciente:** Citas, Tratamientos, Perfil, Historial, Contacto y Ayuda.
  - **Dentista:** Tratamientos, Perfil, Contacto y Ayuda. En las siguientes etapas
    se agregará su agenda.
  - **Administrador:** Tratamientos, Perfil, Contacto y Ayuda. En las siguientes
    etapas se agregará la gestión de dentistas.
- Si la lectura del perfil falla, la aplicación continúa como paciente en lugar de
  bloquear el acceso.
- El administrador inicial se crea una sola vez de forma manual (ver configuración).

### Perfil de usuario

- Visualización del nombre, el correo electrónico y el estado de verificación de la cuenta.
- Edición del nombre con validaciones de campo vacío, longitud máxima y nombre sin cambios.
- Reenvío del correo de verificación de la cuenta.
- Cambio de contraseña mediante un enlace de restablecimiento enviado por correo.
- Actualización automática del estado al volver a la pantalla, y del saludo del menú
  después de editar el nombre.

### Citas

- Listado en tiempo real de las citas del usuario autenticado.
- Registro de una nueva cita con servicio, fecha y hora, mediante selectores nativos.
- Validaciones de servicio vacío, fecha anterior al momento actual y solapamiento con
  otra cita pendiente del mismo usuario.
- Cancelación de citas pendientes con confirmación previa; el registro se conserva con
  estado `CANCELLED` en lugar de eliminarse.
- Estados de la cita: pendiente, cancelada y completada.
- Persistencia en Cloud Firestore, bajo `users/{uid}/appointments`, de modo que cada
  usuario solo accede a sus propios turnos.

### Historial

- Listado de las citas pasadas y canceladas, ordenadas de la más reciente a la más
  antigua.
- Las citas pendientes cuya fecha aún no llega no se muestran en el historial, para
  que sigan disponibles en la pantalla de Citas.
- Reutiliza el repositorio de citas y el adaptador de la pantalla de Citas; desde el
  historial no se puede cancelar.
- Estado vacío, indicador de carga y traducción de los errores de Cloud Firestore.

### Contacto

- Datos del consultorio: teléfono, WhatsApp, correo electrónico, dirección y horario.
- Botones que abren el marcador, WhatsApp con un mensaje predefinido, el cliente de
  correo y la aplicación de mapas mediante Intents implícitos.
- Aviso al usuario si el dispositivo no tiene una aplicación compatible, sin cerrar
  la pantalla.
- Los datos de contacto son de ejemplo y se editan en `res/values/strings.xml`.

### Tratamientos

- Catálogo con los tratamientos disponibles: nombre, resumen y duración aproximada.
- Pantalla de detalle con descripción completa y recomendaciones para el paciente.
- Navegación desde el menú al catálogo y desde cada elemento a su detalle.
- Catálogo local de ejemplo a través del contrato `TreatmentRepository`, de modo que
  puede reemplazarse por otra fuente de datos sin cambiar las pantallas.

### Ayuda

- Preguntas frecuentes desplegables sobre citas, historial, contraseña, verificación
  del correo, perfil, tratamientos y contacto.
- Solo se muestra una respuesta abierta a la vez; al tocar de nuevo la pregunta se
  cierra.
- Botón directo a la pantalla de Contacto para resolver dudas adicionales.
- Catálogo local en `FaqCatalog`, de modo que se pueden agregar preguntas sin cambiar
  la lógica de la pantalla.

## Tecnologías utilizadas

| Tecnología | Uso |
|------------|-----|
| Kotlin | Lenguaje de programación |
| Android SDK (minSdk 24, targetSdk 37) | Plataforma |
| Material 3 | Interfaz de usuario |
| Firebase Authentication | Autenticación de usuarios |
| Cloud Firestore | Persistencia de las citas, los perfiles y los roles |
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
| Dominio | `domain/` | Contratos y modelos sin dependencias de Android: `ConnectivityChecker`, `EmailValidator`, `SessionUser`, `AuthResult`, `Appointment`, `AppointmentStatus`, `Treatment`, `UserRole` y `UserProfile`. |
| Datos | `data/` | Contratos `AuthRepository`, `AppointmentRepository`, `TreatmentRepository` y `UserRepository` con sus implementaciones: Firebase Auth, Cloud Firestore, catálogo local, estado de la red y validación del correo. |
| Utilidades | `core/`, `util/` | Inyección de dependencias manual, adaptación de `Task` a corrutinas, formatos de fecha y traducción de errores de Auth y Firestore. |

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
  AppointmentsActivity.kt        # Vista de las citas
  HistoryActivity.kt             # Vista del historial de citas
  ContactActivity.kt             # Vista de contacto
  TreatmentsActivity.kt          # Vista del catálogo de tratamientos
  TreatmentDetailActivity.kt     # Vista del detalle de un tratamiento
  HelpActivity.kt                # Vista de ayuda con preguntas frecuentes
  core/
    ServiceLocator.kt            # Inyección de dependencias manual
    TaskExtensions.kt            # Adaptación de Task de Firebase a corrutinas
    ErrorUtils.kt                # Errores que no deben mostrarse
  data/
    AuthRepository.kt            # Contrato de autenticación
    FirebaseAuthRepository.kt    # Implementación con Firebase
    AppointmentRepository.kt     # Contrato de las citas
    FirestoreAppointmentRepository.kt  # Implementación con Cloud Firestore
    TreatmentRepository.kt       # Contrato del catálogo de tratamientos
    LocalTreatmentRepository.kt  # Catálogo local de ejemplo
    UserRepository.kt            # Contrato de perfiles y roles
    FirestoreUserRepository.kt   # Perfiles en users/{uid}
    AndroidConnectivityChecker.kt
    AndroidEmailValidator.kt
  domain/
    AuthResult.kt
    SessionUser.kt
    ConnectivityChecker.kt
    EmailValidator.kt
    Appointment.kt               # Modelo y estados de una cita
    Treatment.kt                 # Modelo de un tratamiento
    UserRole.kt                  # Roles: paciente, dentista y administrador
    UserProfile.kt               # Perfil del usuario con su rol
  ui/
    login/                       # LoginViewModel, estado y eventos
    register/                    # RegisterViewModel, estado y eventos
    menu/                        # MenuViewModel, estado y eventos
    profile/                     # ProfileViewModel, estado y eventos
    appointments/                # AppointmentsViewModel, adaptador y eventos
    history/                     # HistoryViewModel, estado y eventos
    treatments/                  # TreatmentsViewModel, detalle y adaptador
    help/                        # HelpViewModel, catálogo y adaptador
  util/
    FirebaseErrors.kt            # Traducción de errores de Firebase Auth
    FirestoreErrors.kt           # Traducción de errores de Cloud Firestore
    DateFormats.kt               # Formatos de fecha y hora
    ContactLinks.kt              # Enlaces de teléfono, WhatsApp, correo y mapas
    NetworkChecker.kt            # Estado de la conexión

app/src/test/java/com/example/the_white_smile/
  core/
    MainDispatcherRule.kt        # Dispatcher principal para las pruebas
  testing/
    Fakes.kt                     # Dobles de repositorio, red y validador
  ui/login/
    LoginViewModelTest.kt        # Pruebas del LoginViewModel
  ui/menu/
    MenuViewModelTest.kt         # Pruebas del menú y de los roles
  ui/profile/
    ProfileViewModelTest.kt      # Pruebas del ProfileViewModel
  ui/appointments/
    AppointmentsViewModelTest.kt # Pruebas del AppointmentsViewModel
  ui/history/
    HistoryViewModelTest.kt      # Pruebas del historial de citas
  ui/treatments/
    TreatmentsViewModelTest.kt   # Pruebas del catálogo y del detalle
  ui/help/
    HelpViewModelTest.kt         # Pruebas de las preguntas desplegables
  util/
    ContactLinksTest.kt          # Pruebas de los enlaces externos

app/src/main/res/
  layout/
    activity_login.xml           # Diseño del inicio de sesión
    activity_register.xml        # Diseño del registro
    activity_menu.xml            # Diseño del menú
    activity_profile.xml         # Diseño del perfil
    activity_appointments.xml    # Diseño de la lista de citas
    item_appointment.xml         # Elemento de la lista de citas
    activity_history.xml         # Diseño del historial de citas
    dialog_new_appointment.xml   # Diálogo de nueva cita
    activity_contact.xml         # Diseño de la pantalla de contacto
    activity_treatments.xml      # Diseño del catálogo de tratamientos
    item_treatment.xml           # Elemento del catálogo
    activity_treatment_detail.xml # Diseño del detalle del tratamiento
    activity_help.xml            # Diseño de la pantalla de ayuda
    item_faq.xml                 # Pregunta desplegable
  values/                        # Cadenas de texto, arreglos, colores y temas
  drawable/                      # Iconos de la interfaz
```

## Requisitos

- Android Studio en una versión reciente.
- Cuenta de Firebase.
- Dispositivo Android (físico o emulador) con Android 7.0 o superior.
- Conexión a internet para las operaciones de autenticación y de citas.

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
6. En Firestore Database, crear la base de datos y publicar las reglas. Cada usuario
   puede leer su propio perfil; solo el administrador puede modificarlo. Las citas
   siguen limitadas al usuario dueño del documento:

```
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {
    function isAdmin() {
      return request.auth != null &&
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == "ADMIN";
    }

    match /users/{uid} {
      allow read: if request.auth != null && request.auth.uid == uid;
      allow write: if request.auth != null && (request.auth.uid == uid || isAdmin());
    }

    match /users/{uid}/appointments/{appointmentId} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
    }
  }
}
```

7. Crear el administrador inicial, una sola vez, desde Firebase Console:
   1. En Authentication, agregar un usuario con correo y contraseña.
   2. Copiar el UID de esa cuenta.
   3. En Firestore, crear el documento `users/{uid}` con los campos
      `email` (cadena), `displayName` (cadena) y `role` (`"ADMIN"`).
   4. Iniciar sesión en la app con esa cuenta: el menú mostrará
      “Panel de administración”.
   La escritura desde la consola omite las reglas, por lo que este paso siempre
   funciona aunque las reglas estén publicadas.

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
contraseña. `AppointmentsViewModelTest` comprueba la observación de las citas, las
validaciones de servicio y fecha, el solapamiento de horarios, la creación, la
cancelación y el manejo de errores. `HistoryViewModelTest` comprueba el filtrado de
las citas pasadas y canceladas, el orden descendente, el estado vacío y el manejo de
errores. `TreatmentsViewModelTest` comprueba el catálogo, la búsqueda por
identificador y la integridad de los datos locales. `HelpViewModelTest` verifica el
catálogo de preguntas y el despliegue de una sola respuesta a la vez.
`MenuViewModelTest` comprueba el saludo, la lectura del rol, el caso de paciente sin
perfil, el manejo del error de lectura y el cierre de sesión.
`ContactLinksTest` verifica la construcción de los enlaces de teléfono, WhatsApp,
correo y mapas. Las pruebas utilizan dobles de prueba, por lo que no requieren
conexión, Firebase ni emulador.

### Prueba manual

1. En la pantalla de inicio de sesión, seleccionar Registrarse y crear una cuenta
   con correo electrónico y contraseña.
2. Volver al inicio de sesión e ingresar con esas credenciales.
3. Verificar la redirección al menú principal y el saludo con el nombre del usuario.
4. Probar la recuperación de contraseña ingresando un correo registrado.
5. Probar el inicio de sesión con Google y con GitHub.
6. Abrir la opción Perfil, editar el nombre y verificar que el saludo del menú se actualice.
7. Probar el reenvío del correo de verificación y el cambio de contraseña.
8. Abrir la opción Citas, agendar un turno y comprobar que aparezca en la lista.
9. Intentar agendar otra cita en el mismo horario y verificar el aviso de conflicto.
10. Cancelar una cita pendiente y confirmar el cambio de estado.
11. Abrir la opción Historial y comprobar que aparezca la cita cancelada, además de
    las citas cuya fecha ya pasó.
12. Abrir la opción Contacto y probar los botones de llamada, WhatsApp, correo y mapa.
13. Abrir la opción Tratamientos, entrar al detalle de un tratamiento y volver a la lista.
14. Abrir la opción Ayuda, desplegar una pregunta y comprobar que se cierre al abrir
    otra; probar el botón Ir a Contacto.
15. Iniciar sesión con la cuenta de administrador y verificar que el menú muestre
    “Panel de administración”; con una cuenta sin perfil debe mostrarse como paciente.
16. Cerrar sesión y confirmar el regreso a la pantalla de inicio de sesión.

## Requisitos del enunciado

- [x] Inicio de sesión con usuario (correo electrónico) y contraseña.
- [x] Recuperación de contraseña mediante correo de restablecimiento.
- [x] Registro de usuarios mediante correo electrónico, Google y GitHub.
- [x] Pantalla de menú principal posterior a un inicio de sesión válido.
- [x] Logotipo de la aplicación en la pantalla del menú.
- [x] Módulo de inicio de sesión desarrollado en Kotlin con Android Studio.
- [x] Gestión de citas con persistencia en Cloud Firestore.
- [x] Historial de citas pasadas y canceladas.
- [x] Pantalla de contacto con accesos directos a teléfono, WhatsApp, correo y mapas.
- [x] Catálogo de tratamientos con pantalla de detalle.
- [x] Pantalla de ayuda con preguntas frecuentes desplegables.
- [x] Roles de paciente, dentista y administrador con menú adaptado.

## Autor

- Jonathan Josue Segura Ramirez, carné SR230847

---

Proyecto académico correspondiente a la materia DSM441.
