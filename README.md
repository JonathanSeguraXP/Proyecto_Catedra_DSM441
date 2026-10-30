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
usuario (paciente, dentista o administrador). El administrador puede crear las cuentas
de los dentistas desde la aplicación, sin cerrar su propia sesión, y cada dentista
puede consultar su agenda de citas y marcarlas como completadas cuando ya fueron
atendidas.

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
- Logotipo de la aplicación y grilla de opciones que cambia según el rol (agenda,
  citas, tratamientos, perfil, historial, contacto, ayuda y dentistas). Todas las
  opciones abren su pantalla correspondiente.
- Confirmación antes de salir de la aplicación.

### Roles de usuario

- Tres roles: paciente (`PATIENT`), dentista (`DENTIST`) y administrador (`ADMIN`).
- El rol se guarda en el documento `users/{uid}` de Cloud Firestore, en el campo
  `role`. Los pacientes no requieren documento: si no existe, se asumen `PATIENT`.
- El menú se construye según el rol:
  - **Paciente:** Citas, Tratamientos, Perfil, Historial, Contacto y Ayuda.
  - **Dentista:** Agenda, Tratamientos, Perfil, Contacto y Ayuda.
  - **Administrador:** Tratamientos, Perfil, Contacto, Ayuda y Dentistas.
- Si la lectura del perfil falla, la aplicación continúa como paciente en lugar de
  bloquear el acceso.
- El administrador inicial se crea una sola vez de forma manual (ver configuración).

### Dentistas (administrador)

- Pantalla exclusiva del administrador, accesible desde la opción Dentistas del menú.
- Listado en tiempo real de los dentistas con nombre, especialidad, correo, horario y estado.
- Creación de la cuenta del dentista con nombre, especialidad, correo, contraseña
  temporal y horario de atención. Incluye validaciones de campos vacíos, formato del
  correo, contraseña de al menos 6 caracteres, días de atención y conexión a internet.
- El horario se define con los días de la semana y un rango de entrada y salida en
  bloques de 15 minutos. Por defecto se ofrece de lunes a viernes, de 08:00 a 17:00.
- Cada dentista de la lista muestra su horario y tiene un botón para editarlo sin
  cambiar su cuenta.
- La cuenta se crea con una **instancia secundaria de FirebaseApp**, de modo que el
  administrador no pierde su sesión. Al terminar, la sesión secundaria se cierra.
- El perfil se guarda en `dentists/{uid}` y el rol `DENTIST` en `users/{uid}`. Si el
  guardado del perfil falla, se elimina la cuenta recién creada para no dejarla a medias.
- Interruptor para activar o desactivar un dentista sin eliminar su cuenta. Los
  dentistas inactivos no aparecen en el selector al agendar citas.
- Nota: desactivar un dentista no bloquea su inicio de sesión, porque deshabilitar una
  cuenta de Authentication requiere el SDK de administrador o Cloud Functions.

### Perfil de usuario

- Visualización del nombre, el correo electrónico y el estado de verificación de la cuenta.
- Edición del nombre con validaciones de campo vacío, longitud máxima y nombre sin cambios.
- Reenvío del correo de verificación de la cuenta.
- Cambio de contraseña mediante un enlace de restablecimiento enviado por correo.
- Actualización automática del estado al volver a la pantalla, y del saludo del menú
  después de editar el nombre.

### Citas

- Listado en tiempo real de las citas del usuario autenticado.
- Registro de una nueva cita con dentista, tratamiento, fecha y hora. Solo se ofrecen
  los dentistas activos y el tratamiento se elige del catálogo de la aplicación.
- La duración real de cada tratamiento determina cuántos bloques de 15 minutos ocupa
  la cita; por ejemplo, una limpieza de 30 minutos ocupa dos bloques.
- La agenda se consulta en Firestore (`dentists/{dentista}/slots`) y el diálogo solo
  muestra horas libres. Si otro paciente ocupa un bloque, la hora desaparece y, al
  confirmar, la transacción vuelve a validar para evitar dobles reservas.
- Validaciones de dentista o tratamiento no seleccionado, fecha anterior al momento
  actual, horario fuera de la atención del dentista y solapamiento con otra cita
  pendiente del mismo usuario.
- La cita guarda el identificador y el nombre del dentista, el tratamiento y su
  duración, de modo que la información se conserva aunque el catálogo cambie.
- También guarda el identificador y el nombre del paciente que la agendó, para que
  el dentista pueda reconocerla en su agenda.
- Cancelación de citas pendientes con confirmación previa; el registro se conserva con
  estado `CANCELLED` y se liberan sus bloques de la agenda del dentista.
- Estados de la cita: pendiente, cancelada y completada.
- Persistencia en Cloud Firestore, bajo `users/{uid}/appointments`, de modo que cada
  usuario solo accede a sus propios turnos. Los bloques ocupados son visibles para
  todos los usuarios autenticados, pero no exponen la cita completa de otros pacientes.

### Agenda del dentista

- Pantalla exclusiva del rol `DENTIST`, accesible desde la opción Agenda del menú.
- Listado en tiempo real de las citas asignadas al dentista, con el nombre del paciente,
  el tratamiento, la fecha y hora, la duración y el estado.
- Las citas se consultan con una consulta de grupo de colecciones
  (`collectionGroup("appointments")`) filtrada por `dentistId`, y se ordenan por fecha
  en el cliente para no requerir un índice compuesto. Firestore sí necesita un índice
  de un solo campo con ámbito de grupo de colecciones para `appointments.dentistId`
  (ver configuración).
- El dentista puede marcar como completada una cita pendiente cuya hora ya pasó. La
  acción pide confirmación. También puede añadir o editar una sugerencia que aparece
  al paciente en su lista de citas.
- Las citas futuras no muestran el botón de completar y el ViewModel vuelve a validar
  la fecha antes de llamar a Firestore.
- Las citas creadas antes de esta versión no tienen `patientId`, por lo que no se pueden
  completar desde la agenda; se muestran con el paciente sin identificar.
- Estado vacío, indicador de carga y traducción de los errores de Cloud Firestore.

### Historial

- Listado de las citas pasadas y canceladas, ordenadas de la más reciente a la más
  antigua.
- Cada cita muestra el dentista con el que se agendó.
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
- El teléfono y WhatsApp del consultorio se configuran en `res/values/strings.xml`.

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

### Barras del sistema

- Todas las pantallas heredan de `BaseActivity`, que reserva el espacio de la
  barra de estado, la cámara, la barra de navegación y el teclado mediante
  `WindowInsetsCompat`, de modo que el contenido nunca quede tapado.
- El tema ajusta el color de los iconos de las barras (`windowLightStatusBar` y
  `windowLightNavigationBar`): oscuros en modo claro y claros en modo oscuro.

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
| Dominio | `domain/` | Contratos y modelos sin dependencias de Android: `ConnectivityChecker`, `EmailValidator`, `SessionUser`, `AuthResult`, `Appointment`, `AppointmentStatus`, `Treatment`, `UserRole`, `UserProfile` y `Dentist`. |
| Datos | `data/` | Contratos `AuthRepository`, `AppointmentRepository`, `TreatmentRepository`, `UserRepository` y `DentistRepository` con sus implementaciones: Firebase Auth, instancia secundaria para crear cuentas, Cloud Firestore, catálogo local, estado de la red y validación del correo. |
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
  DentistAgendaActivity.kt       # Vista de la agenda del dentista
  ContactActivity.kt             # Vista de contacto
  TreatmentsActivity.kt          # Vista del catálogo de tratamientos
  TreatmentDetailActivity.kt     # Vista del detalle de un tratamiento
  HelpActivity.kt                # Vista de ayuda con preguntas frecuentes
  DentistsActivity.kt            # Vista de dentistas del administrador
  BaseActivity.kt                # Margenes del sistema y del teclado en todas las vistas
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
    DentistRepository.kt         # Contrato de administración de dentistas
    DentistAccountCreator.kt     # Contrato para crear cuentas aisladas
    FirebaseDentistAccountCreator.kt # Instancia secundaria de FirebaseApp
    FirestoreDentistRepository.kt    # Dentistas y sus cuentas
    AndroidConnectivityChecker.kt
    AndroidEmailValidator.kt
  domain/
    AuthResult.kt
    SessionUser.kt
    ConnectivityChecker.kt
    EmailValidator.kt
    Appointment.kt               # Modelo y estados de una cita
    AppointmentSchedule.kt       # Bloques de 15 minutos y disponibilidad
    SlotConflictException.kt     # Bloque ocupado por otro paciente
    Treatment.kt                 # Modelo de un tratamiento
    UserRole.kt                  # Roles: paciente, dentista y administrador
    UserProfile.kt               # Perfil del usuario con su rol
    Dentist.kt                   # Dentista con especialidad, estado y horario
  ui/
    login/                       # LoginViewModel, estado y eventos
    register/                    # RegisterViewModel, estado y eventos
    menu/                        # MenuViewModel, estado y eventos
    profile/                     # ProfileViewModel, estado y eventos
    appointments/                # AppointmentsViewModel, disponibilidad y adaptador
    agenda/                      # DentistAgendaViewModel, estado y adaptador
    history/                     # HistoryViewModel, estado y eventos
    treatments/                  # TreatmentsViewModel, detalle y adaptador
    help/                        # HelpViewModel, catálogo y adaptador
    dentists/                    # DentistsViewModel, estado y adaptador
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
  domain/
    AppointmentScheduleTest.kt   # Pruebas de bloques y horarios
  ui/history/
    HistoryViewModelTest.kt      # Pruebas del historial de citas
  ui/agenda/
    DentistAgendaViewModelTest.kt # Pruebas de la agenda del dentista
  ui/treatments/
    TreatmentsViewModelTest.kt   # Pruebas del catálogo y del detalle
  ui/help/
    HelpViewModelTest.kt         # Pruebas de las preguntas desplegables
  ui/dentists/
    DentistsViewModelTest.kt     # Pruebas de cuentas y estados de dentistas
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
    activity_dentist_agenda.xml  # Diseño de la agenda del dentista
    item_dentist_appointment.xml # Cita con paciente y acción de completar
    dialog_new_appointment.xml   # Diálogo de nueva cita
    activity_contact.xml         # Diseño de la pantalla de contacto
    activity_treatments.xml      # Diseño del catálogo de tratamientos
    item_treatment.xml           # Elemento del catálogo
    activity_treatment_detail.xml # Diseño del detalle del tratamiento
    activity_help.xml            # Diseño de la pantalla de ayuda
    item_faq.xml                 # Pregunta desplegable
    activity_dentists.xml        # Lista de dentistas del administrador
    item_dentist.xml             # Dentista con estado y horario
    dialog_new_dentist.xml       # Formulario de nueva cuenta y horario
    dialog_dentist_schedule.xml  # Edición del horario de un dentista
    view_dentist_schedule.xml    # Días y rango horario reutilizable
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
   puede leer su propio perfil; solo el administrador puede modificarlo. El administrador
   gestiona perfiles y horarios de dentistas; cada dentista solo puede actualizar su foto.
   Las citas
   siguen limitadas al usuario dueño del documento, pero el dentista asignado también
   puede leerlas, marcar su estado como `COMPLETED` y guardar una sugerencia visible al
   paciente. Los dentistas son visibles para
   todos los usuarios autenticados; solo el administrador puede modificar sus datos de
   trabajo y cada dentista puede actualizar únicamente `profilePhotoBase64`.
   Las fotos de perfil de pacientes y dentistas se guardan como miniaturas JPEG
   comprimidas en Firestore; no requieren Cloud Storage. Los precios se guardan en
   `treatmentPrices/{treatmentId}` y solo el administrador puede modificarlos. Los
   cambios del catálogo se guardan en `treatments/{treatmentId}`; solo el administrador
   puede modificarlos. Publica las reglas incluidas abajo para habilitar estas acciones.
   El catálogo incluye precios referenciales desde, basados en tarifas publicadas en
   El Salvador; administración puede ajustarlos desde el panel. El costo final de los
   tratamientos variables se confirma tras la evaluación clínica. Al reservar, el
   paciente elige un abono estimado de 25% o 50%; la app lo registra como pendiente de
   pago en clínica, no procesa ni confirma pagos. El teléfono y WhatsApp se editan en
   `res/values/strings.xml`.
   Los bloques ocupados de la agenda (`dentists/{id}/slots`) se leen con sesión
   iniciada, pero cada paciente solo puede crear o borrar los suyos. La agenda del
   dentista usa una consulta de grupo de colecciones sobre `appointments`; la regla
   de esa ruta se aplica a la consulta y el filtro por `dentistId` limita los resultados
   a las citas del dentista autenticado:

```
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {
    function isAdmin() {
      return request.auth != null &&
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == "ADMIN";
    }

    match /users/{uid} {
      allow read: if request.auth != null &&
        (request.auth.uid == uid || isAdmin());
      allow create: if isAdmin() ||
        (request.auth != null && request.auth.uid == uid &&
          request.resource.data.keys().hasOnly(['profilePhotoBase64']) &&
          request.resource.data.profilePhotoBase64 is string &&
          request.resource.data.profilePhotoBase64.size() <= 80000);
      allow update: if isAdmin() ||
        (request.auth != null && request.auth.uid == uid &&
          request.resource.data.diff(resource.data).affectedKeys()
            .hasOnly(['profilePhotoBase64']) &&
          request.resource.data.profilePhotoBase64 is string &&
          request.resource.data.profilePhotoBase64.size() <= 80000);
      allow delete: if isAdmin();
    }

    match /dentists/{dentistId} {
      allow read: if request.auth != null;
      allow create, delete: if isAdmin();
      // El dentista puede cambiar únicamente su miniatura de perfil.
      allow update: if isAdmin() ||
        (request.auth != null && request.auth.uid == dentistId &&
          request.resource.data.diff(resource.data).affectedKeys()
            .hasOnly(['profilePhotoBase64']) &&
          request.resource.data.profilePhotoBase64 is string &&
          request.resource.data.profilePhotoBase64.size() <= 80000);

      match /slots/{slotId} {
        // Cualquier paciente autenticado ve la ocupacion, pero no la cita.
        allow read: if request.auth != null;
        // Solo se puede ocupar o liberar un bloque a nombre propio.
        allow create: if request.auth != null &&
          request.resource.data.patientId == request.auth.uid;
        allow delete: if request.auth != null &&
          resource.data.patientId == request.auth.uid;
      }
    }

    match /treatmentPrices/{treatmentId} {
      allow read: if request.auth != null;
      allow create, update, delete: if isAdmin();
    }

    match /treatments/{treatmentId} {
      allow read: if request.auth != null;
      allow create, update, delete: if isAdmin();
    }

    match /users/{uid}/appointments/{appointmentId} {
      // El paciente ve sus citas y el dentista ve las que tiene asignadas.
      allow read: if request.auth != null &&
        (request.auth.uid == uid || resource.data.dentistId == request.auth.uid);
      allow create: if request.auth != null && request.auth.uid == uid &&
        request.resource.data.patientId == request.auth.uid;
      allow delete: if request.auth != null && request.auth.uid == uid;
      // El dentista asignado puede completar la cita o guardar una sugerencia para el paciente.
      allow update: if request.auth != null &&
        ((request.auth.uid == uid &&
            request.resource.data.diff(resource.data).affectedKeys().hasOnly(['status']) &&
            request.resource.data.status == "CANCELLED") ||
          (resource.data.dentistId == request.auth.uid &&
            request.resource.data.diff(resource.data).affectedKeys().hasOnly(['status']) &&
            request.resource.data.status == "COMPLETED") ||
          (resource.data.dentistId == request.auth.uid &&
            request.resource.data.diff(resource.data).affectedKeys().hasOnly(['dentistSuggestion']) &&
            request.resource.data.dentistSuggestion is string &&
            request.resource.data.dentistSuggestion.size() <= 2000));
    }

    // Consulta de grupo utilizada por la agenda del dentista.
    match /{path=**}/appointments/{appointmentId} {
      allow read: if request.auth != null &&
        resource.data.dentistId == request.auth.uid;
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
8. La creación de cuentas de dentistas desde la app usa una instancia secundaria de
   `FirebaseApp`, por lo que no requiere Cloud Functions ni el plan Blaze. Solo debe
   estar habilitado el método de correo electrónico y contraseña.
9. Crear el índice de grupo de colecciones que usa la agenda del dentista. En
   Firestore Database, abrir la pestaña Índices y habilitar el ámbito
   **Grupo de colecciones** para el campo `dentistId` de la colección `appointments`
   (o abrir el enlace que muestra el error de la consulta la primera vez que se abre
   la agenda). Sin este índice, la agenda del dentista no puede cargar las citas.

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
contraseña. `AppointmentScheduleTest` comprueba las reglas puras de la agenda: bloques
de 15 minutos, alineación de las horas, días de atención, rango de entrada y salida,
tratamientos que ocupan varios bloques y descarte de horas ocupadas o ya pasadas.
`AppointmentsViewModelTest` comprueba la observación de las citas y de los dentistas
activos, las validaciones de dentista, tratamiento y fecha, el horario del dentista, el
solapamiento de citas propias, la creación con el tratamiento y el dentista elegidos,
el conflicto cuando otro paciente ocupa el bloque, la disponibilidad de horas libres, la
cancelación, el nombre del paciente que se guarda al agendar y el manejo de errores.
`HistoryViewModelTest` comprueba el filtrado de
las citas pasadas y canceladas, el orden descendente, el estado vacío y el manejo de
errores. `DentistAgendaViewModelTest` verifica la observación y el orden de las citas
asignadas al dentista, el estado vacío, el completado de citas pasadas, el rechazo de
citas futuras, ya completadas o sin paciente, la falta de conexión y el manejo de
errores. `TreatmentsViewModelTest` comprueba el catálogo, la búsqueda por
identificador y la integridad de los datos locales. `HelpViewModelTest` verifica el
catálogo de preguntas y el despliegue de una sola respuesta a la vez.
`MenuViewModelTest` comprueba el saludo, la lectura del rol, el caso de paciente sin
perfil, el manejo del error de lectura y el cierre de sesión.
`DentistsViewModelTest` verifica la lista de dentistas, las validaciones de la nueva
cuenta, la creación, el manejo de errores, la activación o desactivación y la edición
del horario con sus validaciones de días, bloques de 15 minutos y rango.
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
8. Abrir la opción Citas, agendar un turno eligiendo dentista, tratamiento y fecha, y
   comprobar que aparezca en la lista con el dentista, el tratamiento y la duración.
9. Verificar que el selector de hora solo muestre horas libres dentro del horario de
   atención del dentista y que una cita de 60 minutos ocupe el espacio completo.
10. Intentar agendar otra cita con el mismo dentista y el mismo horario desde otra
    cuenta; el bloque debe aparecer ocupado y, si se confirma a la vez, debe mostrarse
    el aviso de que otra persona ocupó el horario.
11. Intentar agendar otra cita propia en el mismo horario y verificar el aviso de
    conflicto.
12. Desactivar un dentista desde la cuenta del administrador y comprobar que ya no
    aparece en el selector de nueva cita.
13. Cancelar una cita pendiente y confirmar el cambio de estado; la hora debe volver a
    quedar libre para ese dentista.
14. Abrir la opción Historial y comprobar que aparezca la cita cancelada, además de
    las citas cuya fecha ya pasó.
15. Abrir la opción Contacto y probar los botones de llamada, WhatsApp, correo y mapa.
16. Abrir la opción Tratamientos, entrar al detalle de un tratamiento y volver a la lista.
17. Abrir la opción Ayuda, desplegar una pregunta y comprobar que se cierre al abrir
    otra; probar el botón Ir a Contacto.
18. Iniciar sesión con la cuenta de administrador y verificar que el menú muestre
    “Panel de administración”; con una cuenta sin perfil debe mostrarse como paciente.
19. Abrir la opción Dentistas, crear una cuenta con nombre, especialidad, correo,
    contraseña y horario, y comprobar que aparezca en la lista sin cerrar la sesión del
    admin.
20. Editar el horario de un dentista desde el botón Horario y comprobar que el resumen
    de la lista cambie y que las horas de las citas respeten el nuevo rango.
21. Cerrar sesión e iniciar sesión con la cuenta del dentista recién creada; debe
    entrar al menú del dentista.
22. Abrir la opción Agenda y comprobar que aparezcan las citas asignadas con el nombre
    del paciente, el tratamiento, la fecha, la duración y el estado.
23. En una cita pendiente cuya hora ya pasó, tocar Marcar completada, confirmar y
    verificar que el estado cambie a Completada. En la cuenta del paciente, la cita
    debe reflejar el cambio.
24. Comprobar que una cita futura no muestre el botón Marcar completada y que una cita
    antigua sin paciente se muestre como paciente sin identificar.
25. Volver a la cuenta de administrador y desactivar al dentista desde el interruptor.
26. Cerrar sesión y confirmar el regreso a la pantalla de inicio de sesión.

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
- [x] Creación de cuentas de dentistas desde la app por parte del administrador.
- [x] Selección de dentista al agendar una cita.
- [x] Horarios de atención por dentista con bloques de 15 minutos.
- [x] Duración real de cada tratamiento y bloqueo de horarios ocupados.
- [x] Agenda del dentista con las citas asignadas a su cuenta.
- [x] Completado de citas atendidas desde el rol dentista.

## Autor

- Jonathan Josue Segura Ramirez, carné SR230847

---

Proyecto académico correspondiente a la materia DSM441.
