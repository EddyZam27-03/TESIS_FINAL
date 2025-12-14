# 📱 GUÍA COMPLETA DE DESARROLLO - APP "ENSENANDO"
## Sistema de Aprendizaje de Lengua de Señas con Detección de Gestos

**Plataforma:** Android (Kotlin)  
**Arquitectura:** MVVM + Repository Pattern  
**Base de Datos:** Room (SQLite) + Retrofit (MySQL)  
**Detección de Gestos:** MediaPipe/TFLite  
**Sincronización:** Offline-First con WorkManager

---

## 📋 ÍNDICE DE CONTENIDO

1. [Arquitectura y Estructura del Proyecto](#1-arquitectura-y-estructura-del-proyecto)
2. [Pantallas y Navegación Detallada](#2-pantallas-y-navegación-detallada)
3. [Roles de Usuario y Permisos](#3-roles-de-usuario-y-permisos)
4. [Funcionalidades por Pantalla](#4-funcionalidades-por-pantalla)
5. [Base de Datos y Entidades](#5-base-de-datos-y-entidades)
6. [Sincronización Offline-First](#6-sincronización-offline-first)
7. [Sistema de Logros](#7-sistema-de-logros)
8. [Detección de Gestos con TFLite](#8-detección-de-gestos-con-tflite)
9. [Temas Dark/Light Mode](#9-temas-darklight-mode)
10. [Validaciones y Manejo de Errores](#10-validaciones-y-manejo-de-errores)
11. [Componentes Técnicos de Android](#11-componentes-técnicos-de-android)
12. [Endpoints de la API](#12-endpoints-de-la-api)
13. [Dependencias y Librerías](#13-dependencias-y-librerías)

---

## 1. ARQUITECTURA Y ESTRUCTURA DEL PROYECTO

### 1.1 Estructura de Paquetes

```
com.ensenando.app/
├── data/
│   ├── local/
│   │   ├── database/
│   │   │   ├── AppDatabase.kt
│   │   │   └── Converters.kt
│   │   ├── entities/
│   │   │   ├── UsuarioEntity.kt
│   │   │   ├── GestoEntity.kt
│   │   │   ├── ModuloEntity.kt
│   │   │   ├── SubmoduloEntity.kt
│   │   │   ├── UsuarioGestoEntity.kt
│   │   │   ├── LogroEntity.kt
│   │   │   ├── UsuarioLogroEntity.kt
│   │   │   ├── DocenteEstudianteEntity.kt
│   │   │   ├── HistorialIntentoEntity.kt (NUEVO)
│   │   │   └── ConfigEntity.kt (NUEVO)
│   │   └── dao/
│   │       ├── UsuarioDao.kt
│   │       ├── GestoDao.kt
│   │       ├── ModuloDao.kt
│   │       ├── UsuarioGestoDao.kt
│   │       ├── LogroDao.kt
│   │       ├── UsuarioLogroDao.kt
│   │       ├── DocenteEstudianteDao.kt
│   │       ├── HistorialIntentoDao.kt (NUEVO)
│   │       └── ConfigDao.kt (NUEVO)
│   ├── remote/
│   │   ├── api/
│   │   │   ├── ApiService.kt
│   │   │   └── ApiClient.kt
│   │   └── models/
│   │       ├── UsuarioResponse.kt
│   │       ├── GestoResponse.kt
│   │       ├── LogrosResponse.kt
│   │       └── SyncResponse.kt
│   └── repositories/
│       ├── UsuarioRepository.kt
│       ├── GestoRepository.kt
│       ├── ProgresoRepository.kt
│       ├── LogroRepository.kt
│       ├── DocenteEstudianteRepository.kt
│       ├── HistorialIntentoRepository.kt (NUEVO)
│       └── ConfigRepository.kt (NUEVO)
├── domain/
│   └── models/
│       ├── Usuario.kt
│       ├── Gesto.kt
│       ├── Logro.kt
│       └── Progreso.kt
├── ui/
│   ├── auth/
│   │   ├── AuthActivity.kt
│   │   ├── LoginFragment.kt
│   │   └── RegisterFragment.kt
│   ├── main/
│   │   ├── MainActivity.kt
│   │   └── MainViewModel.kt
│   ├── home/
│   │   ├── HomeFragment.kt
│   │   ├── HomeViewModel.kt
│   │   └── adapters/
│   │       ├── ModuloAdapter.kt
│   │       ├── SubmoduloAdapter.kt
│   │       └── GestoAdapter.kt
│   ├── gesto/
│   │   ├── ActivityFragment.kt
│   │   ├── ActivityViewModel.kt
│   │   └── HistorialIntentoAdapter.kt (NUEVO)
│   ├── camera/
│   │   ├── CameraActivity.kt
│   │   └── GestureRecognitionManager.kt
│   ├── logros/
│   │   ├── LogrosFragment.kt
│   │   ├── LogrosViewModel.kt
│   │   ├── LogroDetailFragment.kt (NUEVO)
│   │   └── adapters/
│   │       └── LogrosAdapter.kt
│   ├── profile/
│   │   ├── ProfileFragment.kt
│   │   ├── ProfileViewModel.kt
│   │   ├── EditProfileDialogFragment.kt (NUEVO)
│   │   ├── ChangePasswordDialogFragment.kt (NUEVO)
│   │   ├── BuscarDocenteFragment.kt
│   │   └── adapters/
│   │       └── SolicitudAdapter.kt
│   ├── admin/
│   │   ├── AdminFragment.kt
│   │   ├── AdminViewModel.kt
│   │   └── adapters/
│   │       ├── DocenteAdminAdapter.kt
│   │       ├── EstudianteAdminAdapter.kt
│   │       └── RelacionAdminAdapter.kt
│   ├── reportes/
│   │   ├── ReportesFragment.kt (NUEVO)
│   │   ├── ReportesViewModel.kt (NUEVO)
│   │   ├── ReporteDialogFragment.kt
│   │   └── PdfGenerator.kt
│   ├── settings/
│   │   ├── SettingsFragment.kt (NUEVO)
│   │   └── SettingsViewModel.kt (NUEVO)
│   └── docente/
│       ├── DocenteDashboardFragment.kt (NUEVO)
│       └── DocenteViewModel.kt (NUEVO)
├── sync/
│   ├── SyncManager.kt
│   └── SyncWorker.kt
├── utils/
│   ├── NetworkUtils.kt
│   ├── NotificationManager.kt (NUEVO)
│   └── ThemeUtils.kt (NUEVO)
└── di/
    └── AppModule.kt (Koin/Dagger Hilt)
```

### 1.2 Arquitectura MVVM

- **View:** Fragments y Activities
- **ViewModel:** Lógica de presentación y estado
- **Repository:** Acceso a datos (local y remoto)
- **Model:** Entidades y DTOs

---

## 2. PANTALLAS Y NAVEGACIÓN DETALLADA

### 2.1 Jerarquía de Navegación

```
AuthActivity (Pantalla Inicial)
    ├── LoginFragment
    └── RegisterFragment
         ↓ (Login exitoso)
MainActivity
    ├── BottomNavigationView
    │   ├── Home (HomeFragment)
    │   ├── Gestos (HomeFragment - misma vista)
    │   ├── Logros (LogrosFragment)
    │   └── Perfil (ProfileFragment)
    │
    ├── HomeFragment
    │   ├── → ActivityFragment (Detalle de Gesto)
    │   │       └── → CameraActivity (Practicar)
    │   └── → LogroDetailFragment (Desde logros recientes)
    │
    ├── LogrosFragment
    │   └── → LogroDetailFragment
    │
    ├── ProfileFragment
    │   ├── → BuscarDocenteFragment (Solo estudiantes)
    │   ├── → ReportesFragment (Generar reporte)
    │   ├── → EditProfileDialogFragment
    │   └── → ChangePasswordDialogFragment
    │
    ├── AdminFragment (Solo administradores)
    │   └── → ReportesFragment (Reportes globales)
    │
    ├── DocenteDashboardFragment (Solo docentes)
    │   └── → ReportesFragment (Reportes por estudiante)
    │
    └── SettingsFragment (Desde menú)
```

### 2.2 Pantalla 1: Login/Registro (AuthActivity)

**Archivo:** `AuthActivity.kt`, `LoginFragment.kt`, `RegisterFragment.kt`

#### 2.2.1 LoginFragment

**Layout:** `fragment_login.xml`

**Componentes UI:**
- `EditText` para correo (`etEmail`)
- `EditText` para contraseña (`etPassword`) - tipo password
- `Button` "Iniciar Sesión" (`btnLogin`)
- `Button` "Registrarse" (`btnRegister`) - navega a RegisterFragment
- `ProgressBar` (`progressBar`) - visible durante autenticación
- `TextView` para errores (`tvError`) - opcional, o usar Toast

**Funcionalidades:**
1. **Validación Local:**
   - Verificar que correo no esté vacío
   - Verificar que contraseña no esté vacía
   - Validar formato de correo (regex básico)
   - Mostrar mensajes de error con Toast si falla

2. **Autenticación:**
   - Buscar usuario en SQLite local (`usuarios` table)
   - Si no existe localmente y hay conexión, intentar autenticación remota
   - Guardar usuario en SQLite con `sync_status = "pending"` si es nuevo
   - Mostrar `ProgressBar` durante autenticación

3. **Navegación:**
   - Si login exitoso → `MainActivity`
   - Si falla → mostrar Toast con error
   - Botón "Registrarse" → `RegisterFragment` (mismo Activity)

**Validaciones Específicas:**
- Correo: formato válido (contiene @ y dominio)
- Contraseña: mínimo 6 caracteres (según documento)

#### 2.2.2 RegisterFragment

**Layout:** `fragment_register.xml`

**Componentes UI:**
- `EditText` para nombre (`etNombre`)
- `EditText` para correo (`etEmail`)
- `EditText` para contraseña (`etPassword`)
- `EditText` para confirmar contraseña (`etConfirmPassword`)
- `Spinner` o `RadioGroup` para rol (`spinnerRol`) - Estudiante, Docente, Administrador
- `Button` "Registrarse" (`btnRegister`)
- `Button` "Volver a Login" (`btnBackToLogin`)
- `ProgressBar` (`progressBar`)

**Funcionalidades:**
1. **Validación:**
   - Todos los campos requeridos
   - Correo válido
   - Contraseña mínimo 6 caracteres
   - Contraseñas coinciden
   - Mostrar errores con Toast

2. **Registro:**
   - Guardar en SQLite local (`usuarios` table)
   - Campos: `nombre`, `correo`, `contraseña` (hash), `rol`, `fecha_registro`, `sync_status = "pending"`
   - Si hay conexión, sincronizar inmediatamente
   - Si no hay conexión, guardar localmente para sincronizar después

3. **Navegación:**
   - Registro exitoso → `MainActivity`
   - Botón "Volver" → `LoginFragment`

### 2.3 Pantalla 2: Dashboard/Home (HomeFragment)

**Archivo:** `HomeFragment.kt`, `HomeViewModel.kt`

**Layout:** `fragment_home.xml`

#### 2.3.1 Componentes UI Requeridos

**Sección Superior - Resumen de Progreso:**
- `CardView` con:
  - `TextView` "Total de Gestos" (`tvTotalGestos`)
  - `TextView` "Gestos Aprendidos" (`tvGestosAprendidos`)
  - `TextView` "Promedio de Progreso" (`tvPromedio`) - formato: "XX%"

**Sección Logros Recientes (FALTANTE - IMPLEMENTAR):**
- `CardView` "Logros Recientes"
- `RecyclerView` horizontal con máximo 3 logros
- Cada item muestra: icono, título, fecha obtenido
- Click en logro → `LogroDetailFragment`

**Sección Notificaciones (FALTANTE - IMPLEMENTAR):**
- `CardView` con badge de notificaciones pendientes
- Solo para estudiantes: solicitudes de docentes pendientes
- Solo para docentes: solicitudes de estudiantes pendientes
- Click → `ProfileFragment` (sección de solicitudes)

**Indicador de Conexión (FALTANTE - IMPLEMENTAR):**
- `ImageView` o `TextView` en Toolbar o esquina superior
- Verde = online, Rojo = offline
- Actualizar en tiempo real según estado de red

**Botones de Acceso Rápido (FALTANTE - IMPLEMENTAR):**
- `CardView` con GridLayout o RecyclerView:
  - Botón "Gestos" → scroll a sección de gestos
  - Botón "Logros" → `LogrosFragment`
  - Botón "Reportes" → `ReportesFragment`
  - Botón "Perfil" → `ProfileFragment`
  - Botón "Configuración" → `SettingsFragment`

**Sección Módulos de Aprendizaje:**
- `RecyclerView` vertical con `ModuloAdapter`
- Organización jerárquica:
  - **Módulo** (Básico, Social, Académico)
    - **Submódulo** (expandible/collapsible)
      - **Gesto** (clickable → `ActivityFragment`)

#### 2.3.2 Funcionalidades Implementadas

1. **Carga de Datos:**
   - Cargar primero desde SQLite local
   - Sincronizar en background si hay conexión
   - Actualizar UI cuando lleguen datos remotos

2. **Cálculo de Progreso:**
   - Total gestos: contar todos los gestos en BD
   - Gestos aprendidos: contar `usuario_gestos` con `estado = "Aprendido"`
   - Promedio: sumar todos los `porcentaje` de `usuario_gestos` / total

3. **Navegación:**
   - Click en gesto → `ActivityFragment` con `id_gesto` como argumento

#### 2.3.3 Funcionalidades Faltantes (IMPLEMENTAR)

1. **Logros Recientes:**
   ```kotlin
   // En HomeViewModel
   fun getLogrosRecientes(): LiveData<List<Logro>> {
       return logroRepository.getLogrosRecientes(3) // Máximo 3
   }
   ```

2. **Notificaciones Pendientes:**
   ```kotlin
   fun getNotificacionesPendientes(): LiveData<Int> {
       return docenteEstudianteRepository.getSolicitudesPendientesCount()
   }
   ```

3. **Indicador de Conexión:**
   ```kotlin
   fun isOnline(): LiveData<Boolean> {
       return NetworkUtils.observeConnection(context)
   }
   ```

### 2.4 Pantalla 3: Lista de Gestos (HomeFragment - misma vista)

**Archivo:** `GestoAdapter.kt`

**Layout Item:** `item_gesto.xml`

#### 2.4.1 Componentes UI Requeridos (ACTUALMENTE FALTANTES)

**Campos a Mostrar (5 campos según documento):**

1. **Nombre del Gesto** (`tvGestoNombre`) ✅ IMPLEMENTADO
   - `TextView` con `gestos.nombre`

2. **Categoría** (`tvCategoria`) ❌ FALTANTE
   - `TextView` o `Chip` con `gestos.categoria`
   - Mostrar jerarquía: Módulo > Submódulo

3. **Dificultad** (`tvDificultad`) ❌ FALTANTE
   - `TextView` o `Chip` con `gestos.dificultad`
   - Colores: Fácil (verde), Medio (amarillo), Difícil (rojo)

4. **Porcentaje de Progreso** (`progressBar`, `tvPorcentaje`) ❌ FALTANTE
   - `ProgressBar` horizontal con `usuario_gestos.porcentaje`
   - `TextView` con formato "XX%"

5. **Estado** (`chipEstado`) ❌ FALTANTE
   - `Chip` o `Badge` con `usuario_gestos.estado`
   - "Pendiente" (gris) o "Aprendido" (verde)

**Botón "Practicar"** ❌ FALTANTE
- `Button` o `FloatingActionButton` en cada item
- Click → `CameraActivity` directamente

#### 2.4.2 Filtros (FALTANTE - IMPLEMENTAR)

**Layout:** Agregar en `fragment_home.xml` antes del RecyclerView

- `ChipGroup` para filtrar por categoría
- `Spinner` para filtrar por dificultad (Todas, Fácil, Medio, Difícil)
- `SearchView` en Toolbar para buscar por nombre

**Funcionalidad:**
```kotlin
// En HomeViewModel
fun filtrarGestos(categoria: String?, dificultad: String?, nombre: String?) {
    val gestosFiltrados = gestoRepository.filtrarGestos(categoria, dificultad, nombre)
    _gestos.value = gestosFiltrados
}
```

### 2.5 Pantalla 4: Detalle de Gesto (ActivityFragment)

**Archivo:** `ActivityFragment.kt`, `ActivityViewModel.kt`

**Layout:** `fragment_activity.xml`

#### 2.5.1 Componentes UI Implementados

1. **Nombre del Gesto** (`tvGestoNombre`) ✅
   - `TextView` con `gestos.nombre`

2. **Barra de Progreso** (`progressBar`, `tvPorcentaje`) ✅
   - `ProgressBar` circular o horizontal
   - `TextView` con formato "XX%"
   - Valor de `usuario_gestos.porcentaje` (0-100)

3. **Video del Gesto** (`videoView`) ✅
   - `VideoView` o `ExoPlayer`
   - Cargar desde `assets/INFO/GESTOS/[nombre_gesto].mp4`
   - Sistema robusto de carga con fallback

4. **Botón "Practicar"** (`btnPracticar`) ✅
   - `Button` que abre `CameraActivity`
   - Pasar `id_gesto` como extra

#### 2.5.2 Componentes UI Faltantes (IMPLEMENTAR)

1. **Categoría Visible** ❌
   - `TextView` o `Chip` mostrando módulo y submódulo

2. **Dificultad Visible** ❌
   - `TextView` o `Chip` con `gestos.dificultad`
   - Colores según dificultad

3. **Descripción del Gesto** ❌
   - `TextView` con descripción (requiere agregar campo `descripcion` en BD)

4. **Historial de Intentos (Últimos 5)** ❌
   - `RecyclerView` con `HistorialIntentoAdapter`
   - Mostrar: fecha, porcentaje obtenido, estado (éxito/fallo)
   - Layout: `item_historial_intento.xml`

**Layout Historial:**
```xml
<LinearLayout>
    <TextView android:id="@+id/tvFechaIntento" />
    <TextView android:id="@+id/tvPorcentajeObtenido" />
    <ImageView android:id="@+id/ivEstado" /> <!-- check/cross -->
</LinearLayout>
```

#### 2.5.3 Funcionalidades

1. **Actualización de Progreso:**
   - Al practicar y obtener resultado → actualizar `usuario_gestos.porcentaje`
   - Si porcentaje ≥ 80% → cambiar `estado = "Aprendido"`
   - Guardar localmente con `sync_status = "pending"`
   - Sincronizar en background

2. **Guardar Intento en Historial:**
   ```kotlin
   // En ActivityViewModel después de práctica
   fun guardarIntento(idGesto: Int, porcentaje: Int) {
       viewModelScope.launch {
           historialRepository.insertIntento(
               idUsuario = usuarioActual.id,
               idGesto = idGesto,
               porcentaje = porcentaje,
               fecha = Date()
           )
       }
   }
   ```

### 2.6 Pantalla 5: Practicar Gesto (CameraActivity)

**Archivo:** `CameraActivity.kt`, `GestureRecognitionManager.kt`

**Layout:** `activity_camera.xml`

#### 2.6.1 Componentes UI

- `TextureView` o `CameraX PreviewView` para cámara
- `TextView` para instrucciones (`tvInstrucciones`)
- `TextView` para resultado (`tvResultado`) - porcentaje de acierto
- `ProgressBar` circular durante detección
- `Button` "Finalizar" (`btnFinalizar`)
- `Button` "Reintentar" (`btnReintentar`)

#### 2.6.2 Funcionalidades

1. **Inicialización:**
   - Recibir `id_gesto` como extra
   - Cargar modelo TFLite/MediaPipe para el gesto específico
   - Inicializar cámara con `CameraX`

2. **Detección en Tiempo Real:**
   - Procesar frames con `ImageAnalysis`
   - Usar `HandDetector` y `GestureRecognitionManager`
   - Comparar gesto detectado con gesto objetivo
   - Calcular porcentaje de similitud (0-100%)

3. **Actualización de Progreso:**
   - Solo incrementar, nunca decrementar
   - Si nuevo porcentaje > porcentaje actual → actualizar
   - Si porcentaje ≥ 80% → `estado = "Aprendido"`
   - Guardar en `usuario_gestos` con `sync_status = "pending"`

4. **Guardar Intento:**
   - Insertar en `historial_intentos`:
     - `id_usuario`
     - `id_gesto`
     - `porcentaje_obtenido`
     - `fecha_intento` (timestamp)
     - `sync_status = "pending"`

5. **Navegación:**
   - Botón "Finalizar" → volver a `ActivityFragment`
   - Mostrar resultado final antes de cerrar

### 2.7 Pantalla 6: Logros (LogrosFragment)

**Archivo:** `LogrosFragment.kt`, `LogrosViewModel.kt`, `LogrosAdapter.kt`

**Layout:** `fragment_logros.xml`

#### 2.7.1 Componentes UI Implementados

- `RecyclerView` con `LogrosAdapter` ✅
- `TextView` "Total de Logros Desbloqueados" (`tvTotalLogros`) ✅
- `Button` "Refresh" (`btnRefresh`) ✅

**Layout Item:** `item_logro.xml`

**Campos Mostrados (5 campos según documento):**

1. **Icono** (`ivLogroIcon`) ✅
   - `ImageView` con `star_big_on` (obtenido) o `star_big_off` (pendiente)
   - Alpha 1.0 (obtenido) o 0.5 (pendiente)

2. **Título** (`tvLogroNombre`) ✅
   - `TextView` con `logros.titulo` o `logros.nombre`

3. **Descripción** (`tvLogroDescripcion`) ✅
   - `TextView` con `logros.descripcion`

4. **Fecha Obtenido** (`tvFechaDesbloqueo`) ✅
   - `TextView` con `logros.fecha_obtenido` o `logros.fechaDesbloqueo`
   - Formato: "dd/MM/yyyy" o "Hace X días"

5. **Estado** ✅
   - Visual con alpha e icono
   - ProgressBar con `logros.porcentajeAvance` si está pendiente

#### 2.7.2 Funcionalidades Implementadas

1. **Carga de Logros:**
   - Cargar desde SQLite local
   - Sincronizar en background

2. **Visualización:**
   - Mostrar todos los logros (obtenidos y pendientes)
   - Scroll infinito

#### 2.7.3 Funcionalidades Faltantes (IMPLEMENTAR)

1. **Botón "Ver Detalle"** ❌
   - Agregar `Button` en `item_logro.xml`
   - Click → `LogroDetailFragment` con `id_logro` como argumento

2. **Categorías Visibles** ❌
   - Agregar `Chip` o `TextView` con categoría del logro
   - 6 categorías: Progreso Básico, Aprendizaje y Tareas, Rendimiento, Frecuencia y Hábitos, Participación y Comunidad, Dominio del Contenido

3. **Filtro por Categoría** ❌
   - `ChipGroup` en Toolbar para filtrar

### 2.8 Pantalla 7: Detalle de Logro (LogroDetailFragment) - NUEVO

**Archivo:** `LogroDetailFragment.kt` (CREAR)

**Layout:** `fragment_logro_detail.xml`

#### 2.8.1 Componentes UI

- `ImageView` grande con icono del logro (`ivLogroIconGrande`)
- `TextView` título (`tvLogroTitulo`)
- `TextView` descripción (`tvLogroDescripcion`)
- `Chip` categoría (`chipCategoria`)
- `TextView` fecha obtenido (`tvFechaObtenido`)
- `Button` "Compartir" (`btnCompartir`) - opcional, usar Intent.ACTION_SEND

#### 2.8.2 Funcionalidades

1. **Cargar Datos:**
   - Recibir `id_logro` como argumento
   - Cargar logro desde BD

2. **Compartir:**
   - Intent con texto: "¡He obtenido el logro [título] en Ensenando!"

3. **Navegación:**
   - Botón back → `LogrosFragment`

### 2.9 Pantalla 8: Perfil (ProfileFragment)

**Archivo:** `ProfileFragment.kt`, `ProfileViewModel.kt`

**Layout:** `fragment_profile.xml`

#### 2.9.1 Componentes UI Implementados

- `TextView` nombre (`tvNombre`) ✅
- `TextView` correo (`tvCorreo`) ✅
- `TextView` rol (`tvRol`) ✅
- `TextView` fecha de registro (`tvFechaRegistro`) ✅
- `Button` "Ver Logros" (`btnVerLogros`) ✅ → `LogrosFragment`
- `Button` "Buscar Docente" (`btnBuscarDocente`) ✅ → `BuscarDocenteFragment` (solo estudiantes)
- `Button` "Ver/Generar Reporte" (`btnReporte`) ✅ → genera PDF
- `Button` "Cerrar Sesión" (`btnCerrarSesion`) ✅
- `RecyclerView` solicitudes (`rvSolicitudes`) ✅

#### 2.9.2 Componentes UI Faltantes (IMPLEMENTAR)

1. **Botón Editar Perfil** ❌
   - `Button` "Editar Perfil" (`btnEditarPerfil`)
   - Click → `EditProfileDialogFragment`

2. **Botón Cambiar Contraseña** ❌
   - `Button` "Cambiar Contraseña" (`btnCambiarPassword`)
   - Click → `ChangePasswordDialogFragment`

3. **Indicador de Progreso Total** ❌
   - `CardView` con:
     - Total gestos aprendidos / Total gestos
     - Total logros obtenidos / Total logros
     - Gráfico circular o barras

#### 2.9.3 Funcionalidades

1. **Edición de Perfil:**
   - `EditProfileDialogFragment`:
     - `EditText` para nombre (pre-llenado)
     - `Button` "Guardar" y "Cancelar"
     - Validar nombre no vacío
     - Actualizar en BD local con `sync_status = "pending"`

2. **Cambio de Contraseña:**
   - `ChangePasswordDialogFragment`:
     - `EditText` contraseña actual
     - `EditText` nueva contraseña
     - `EditText` confirmar nueva contraseña
     - Validar: contraseña actual correcta, nueva contraseña mínimo 6 caracteres, coinciden
     - Actualizar en BD local con `sync_status = "pending"`

3. **Solicitudes:**
   - Mostrar según rol:
     - **Estudiante:** Solicitudes enviadas a docentes
     - **Docente:** Solicitudes recibidas de estudiantes
   - Usar `SolicitudAdapter`

### 2.10 Pantalla 9: Buscar Docente (BuscarDocenteFragment)

**Archivo:** `BuscarDocenteFragment.kt`

**Layout:** `fragment_buscar_docente.xml`

#### 2.10.1 Componentes UI

- `SearchView` o `EditText` para buscar (`etBuscarDocente`)
- `RecyclerView` con lista de docentes (`rvDocentes`)
- `Adapter` para mostrar docentes

#### 2.10.2 Funcionalidades

1. **Búsqueda:**
   - Buscar docentes por nombre o correo
   - Filtrar en tiempo real mientras escribe

2. **Enviar Solicitud:**
   - Click en docente → enviar solicitud
   - Insertar en `docente_estudiante`:
     - `id_docente`
     - `id_estudiante` (usuario actual)
     - `estado = "Pendiente"`
     - `sync_status = "pending"`

3. **Navegación:**
   - Botón back → `ProfileFragment`

### 2.11 Pantalla 10: Solicitudes (ProfileFragment - sección)

**Archivo:** `SolicitudAdapter.kt`

**Layout Item:** `item_solicitud.xml`

#### 2.11.1 Componentes UI Implementados

- `TextView` nombre (`tvDocenteNombre` o `tvEstudianteNombre`) ✅
- `TextView` estado (`tvEstado`) ✅ - Pendiente (amarillo), Aceptado (verde), Rechazado (rojo)
- `Button` "Aceptar" (`btnAceptar`) ✅ - solo para docentes con solicitudes pendientes
- `Button` "Rechazar" (`btnRechazar`) ✅ - solo para docentes con solicitudes pendientes

#### 2.11.2 Componentes UI Faltantes (IMPLEMENTAR)

1. **Correo Visible** ❌
   - `TextView` con correo del usuario (`tvCorreo`)

2. **Fecha de Solicitud** ❌
   - `TextView` con fecha (`tvFechaSolicitud`)
   - Formato: "dd/MM/yyyy HH:mm" o "Hace X días"

#### 2.11.3 Funcionalidades

1. **Aceptar/Rechazar:**
   - Actualizar `docente_estudiante.estado`
   - Guardar con `sync_status = "pending"`
   - Sincronizar en background

### 2.12 Pantalla 11: Administración (AdminFragment)

**Archivo:** `AdminFragment.kt`, `AdminViewModel.kt`

**Layout:** `fragment_admin.xml`

#### 2.12.1 Componentes UI Implementados

- `TabLayout` o `ViewPager2` con 3 tabs:
  1. **Docentes** (`rvDocentes`) ✅
  2. **Estudiantes** (`rvEstudiantes`) ✅
  3. **Relaciones** (`rvRelaciones`) ✅

- `SearchView` para buscar ✅

#### 2.12.2 Funcionalidades Implementadas

1. **Ver Lista de Docentes:**
   - `RecyclerView` con `DocenteAdminAdapter`
   - Mostrar: nombre, correo, fecha registro

2. **Ver Lista de Estudiantes:**
   - `RecyclerView` con `EstudianteAdminAdapter`
   - Mostrar: nombre, correo, fecha registro

3. **Ver Relaciones:**
   - `RecyclerView` con `RelacionAdminAdapter`
   - Mostrar: nombre docente, nombre estudiante, estado
   - Botón "Eliminar" relación ✅

#### 2.12.3 Funcionalidades Faltantes (IMPLEMENTAR)

1. **CRUD de Usuarios** ❌
   - Botón "Crear Usuario" → DialogFragment
   - Botón "Editar" en cada item → DialogFragment
   - Botón "Eliminar" en cada item → ConfirmDialog

2. **Modificar Relaciones** ❌
   - Botón "Editar" relación → cambiar docente o estudiante

3. **Gestión de Gestos y Logros** ❌
   - Tabs adicionales: "Gestos", "Logros"
   - CRUD completo de gestos y logros

4. **Reportes Globales** ❌
   - Botón "Generar Reporte Global" → `ReportesFragment` con datos de todos los usuarios

### 2.13 Pantalla 12: Dashboard Docente (DocenteDashboardFragment) - NUEVO

**Archivo:** `DocenteDashboardFragment.kt`, `DocenteViewModel.kt` (CREAR)

**Layout:** `fragment_docente_dashboard.xml`

#### 2.13.1 Componentes UI

**Sección Estudiantes Vinculados:**
- `RecyclerView` con lista de estudiantes
- Cada item muestra:
  - Nombre estudiante
  - Progreso total (porcentaje)
  - Última actividad (fecha)

**Sección Alertas:**
- `CardView` "Estudiantes Rezagados"
- `RecyclerView` con estudiantes con progreso < 50%
- Badge rojo con cantidad

**Sección Progreso por Categoría:**
- `RecyclerView` o gráfico mostrando:
  - Progreso promedio por categoría de gestos
  - Para cada estudiante vinculado

**Botón "Generar Reporte"** → `ReportesFragment` con filtro por estudiante

#### 2.13.2 Funcionalidades

1. **Cargar Estudiantes Vinculados:**
   ```kotlin
   fun getEstudiantesVinculados(): LiveData<List<Estudiante>> {
       return docenteEstudianteRepository.getEstudiantesAceptados(idDocente)
   }
   ```

2. **Alertas de Rezagados:**
   ```kotlin
   fun getEstudiantesRezagados(): LiveData<List<Estudiante>> {
       return progresoRepository.getEstudiantesConProgresoMenorA(50)
   }
   ```

3. **Progreso por Categoría:**
   ```kotlin
   fun getProgresoPorCategoria(idEstudiante: Int): LiveData<Map<String, Double>> {
       return progresoRepository.getProgresoPromedioPorCategoria(idEstudiante)
   }
   ```

### 2.14 Pantalla 13: Reportes (ReportesFragment)

**Archivo:** `ReportesFragment.kt`, `ReportesViewModel.kt` (CREAR)

**Layout:** `fragment_reportes.xml`

#### 2.14.1 Componentes UI

**Filtros (Solo Docente y Administrador):**
- `Spinner` para seleccionar estudiante (solo docentes)
- `Spinner` para seleccionar categoría
- `DatePicker` para rango de fechas (desde - hasta)
- `Button` "Aplicar Filtros"

**Visualización:**
- `ViewPager2` o `ScrollView` con:
  1. **Gráfico de Barras:** Progreso por categoría
  2. **Gráfico de Torta:** Distribución de logros
  3. **Gráfico de Línea:** Progreso en el tiempo
  4. **Tabla de Datos:** Lista paginada (5 items por pantalla)

**Botones:**
- `Button` "Generar PDF" → `PdfGenerator`
- `Button` "Compartir" → Intent.ACTION_SEND

#### 2.14.2 Funcionalidades por Rol

**Estudiante:**
- Solo su propio progreso
- Sin filtros
- Gráficos básicos

**Docente:**
- Progreso de estudiantes vinculados
- Filtro por estudiante
- Filtro por categoría
- Alertas de estudiantes rezagados

**Administrador:**
- Progreso global de todos los usuarios
- Filtros avanzados
- Estadísticas del sistema:
  - Días activos por usuario
  - Streaks semanales/mensuales
  - Frecuencia de uso por categoría
  - Tasa de completación de gestos

#### 2.14.3 Paginación

- Mostrar 5 estudiantes/logros por pantalla
- Scroll para cargar más
- Botones "Anterior" / "Siguiente"

### 2.15 Pantalla 14: Configuración (SettingsFragment) - NUEVO

**Archivo:** `SettingsFragment.kt`, `SettingsViewModel.kt` (CREAR)

**Layout:** `fragment_settings.xml`

#### 2.15.1 Componentes UI

**Sección Tema:**
- `Switch` "Modo Oscuro" (`switchDarkMode`)
- Descripción: "Activar modo oscuro"
- Guardar preferencia en `config` table

**Sección Sincronización:**
- `Button` "Sincronizar Ahora" (`btnSyncNow`)
- `TextView` estado de sincronización (`tvSyncStatus`)
  - "Sincronizado" (verde)
  - "Pendiente: X elementos" (amarillo)
  - "Error" (rojo)
- `TextView` última sincronización (`tvLastSync`)

**Sección Conexión:**
- `TextView` indicador de conexión (`tvConnectionStatus`)
  - "Online" (verde) / "Offline" (rojo)
- Icono visual

**Sección Notificaciones:**
- `Switch` "Notificaciones de Logros" (`switchNotificacionesLogros`)
- `Switch` "Notificaciones de Solicitudes" (`switchNotificacionesSolicitudes`)
- `Switch` "Recordatorios Diarios" (`switchRecordatorios`)

#### 2.15.2 Funcionalidades

1. **Cambio de Tema:**
   ```kotlin
   fun cambiarTema(modoOscuro: Boolean) {
       val modo = if (modoOscuro) {
           AppCompatDelegate.MODE_NIGHT_YES
       } else {
           AppCompatDelegate.MODE_NIGHT_NO
       }
       AppCompatDelegate.setDefaultNightMode(modo)
       configRepository.guardarTema(modoOscuro)
   }
   ```

2. **Sincronización Manual:**
   ```kotlin
   fun sincronizarAhora() {
       viewModelScope.launch {
           syncManager.sincronizarInmediatamente()
       }
   }
   ```

3. **Guardar Preferencias:**
   - Guardar en tabla `config`:
     - `tema` (dark/light)
     - `notificaciones_logros` (boolean)
     - `notificaciones_solicitudes` (boolean)
     - `recordatorios` (boolean)

---

## 3. ROLES DE USUARIO Y PERMISOS

### 3.1 Estudiante

**Acceso a Pantallas:**
- ✅ Home/Dashboard
- ✅ Lista de Gestos
- ✅ Detalle de Gesto
- ✅ Practicar Gesto (CameraActivity)
- ✅ Logros
- ✅ Detalle de Logro
- ✅ Perfil
- ✅ Buscar Docente
- ✅ Solicitudes (enviadas)
- ✅ Reportes (solo propios)
- ✅ Configuración

**Funcionalidades Específicas:**
- Practicar gestos con TFLite
- Ver progreso personal
- Ver logros personales
- Enviar solicitudes a docentes
- Generar reporte personal (PDF)
- Editar perfil propio
- Cambiar contraseña propia

**Restricciones:**
- ❌ No puede ver progreso de otros estudiantes
- ❌ No puede aceptar/rechazar solicitudes
- ❌ No puede gestionar usuarios
- ❌ No puede ver reportes globales

### 3.2 Docente

**Hereda todo de Estudiante +**

**Acceso Adicional:**
- ✅ Dashboard Docente (`DocenteDashboardFragment`)
- ✅ Ver solicitudes recibidas de estudiantes
- ✅ Aceptar/Rechazar solicitudes
- ✅ Ver progreso de estudiantes vinculados
- ✅ Generar reportes por estudiante
- ✅ Ver alertas de estudiantes rezagados

**Funcionalidades Específicas:**
- Ver lista de estudiantes vinculados
- Ver progreso por categoría de cada estudiante
- Recibir alertas de estudiantes con progreso < 50%
- Generar reportes filtrados por estudiante y categoría
- Ver historial de logros de estudiantes

**Restricciones:**
- ❌ No puede gestionar usuarios (crear/editar/eliminar)
- ❌ No puede ver reportes globales del sistema
- ❌ No puede gestionar gestos o logros

### 3.3 Administrador

**Hereda todo de Docente +**

**Acceso Adicional:**
- ✅ AdminFragment
- ✅ CRUD completo de usuarios
- ✅ Gestión de relaciones docente-estudiante
- ✅ Gestión de gestos y logros (CRUD)
- ✅ Reportes globales del sistema
- ✅ Estadísticas avanzadas

**Funcionalidades Específicas:**
- Crear, editar, eliminar usuarios
- Modificar roles de usuarios
- Ver todas las relaciones docente-estudiante
- Eliminar relaciones
- Crear, editar, eliminar gestos
- Crear, editar, eliminar logros
- Ver estadísticas globales:
  - Progreso promedio por categoría
  - Logros obtenidos por categoría
  - Frecuencia de uso
  - Días activos por usuario
  - Streaks semanales/mensuales

**Sin Restricciones:**
- Acceso completo a todas las funcionalidades

---

## 4. FUNCIONALIDADES POR PANTALLA

### 4.1 Búsqueda y Filtros

#### 4.1.1 Lista de Gestos
- **Búsqueda por nombre:** `SearchView` en Toolbar
- **Filtro por categoría:** `ChipGroup` con chips de categorías
- **Filtro por dificultad:** `Spinner` (Todas, Fácil, Medio, Difícil)
- **Filtro por estado:** `ChipGroup` (Todos, Pendiente, Aprendido)

#### 4.1.2 Logros
- **Filtro por categoría:** `ChipGroup` con 6 categorías
- **Filtro por estado:** `ChipGroup` (Todos, Obtenidos, Pendientes)

#### 4.1.3 Administración
- **Búsqueda de usuarios:** `SearchView` por nombre o correo
- **Filtro por rol:** `Spinner` (Todos, Estudiante, Docente, Administrador)

#### 4.1.4 Reportes (Docente/Administrador)
- **Filtro por estudiante:** `Spinner` (solo docentes)
- **Filtro por categoría:** `Spinner`
- **Filtro por rango de fechas:** `DatePicker` (desde - hasta)

### 4.2 Progreso y Seguimiento

#### 4.2.1 Registro de Progreso
- **Tabla:** `usuario_gestos`
- **Campos:**
  - `id_usuario`
  - `id_gesto`
  - `porcentaje` (0-100)
  - `estado` ("Pendiente" o "Aprendido")
  - `sync_status` ("pending" o "synced")
  - `lastUpdated` (timestamp)

#### 4.2.2 Lógica de Progreso
- **Solo incremento:** Nunca decrementar porcentaje
- **Umbral de aprendizaje:** Si `porcentaje ≥ 80%` → `estado = "Aprendido"`
- **Actualización:** Después de cada práctica con TFLite

#### 4.2.3 Visualización de Progreso
- **Home:** Total gestos, gestos aprendidos, promedio
- **Lista de gestos:** ProgressBar y porcentaje en cada item
- **Detalle de gesto:** ProgressBar circular/horizontal
- **Perfil:** Indicador de progreso total

#### 4.2.4 Historial de Intentos
- **Tabla:** `historial_intentos` (NUEVA)
- **Campos:**
  - `id_historial` (PK)
  - `id_usuario`
  - `id_gesto`
  - `porcentaje_obtenido`
  - `fecha_intento` (timestamp)
  - `sync_status`
- **Visualización:** Últimos 5 intentos en `ActivityFragment`

### 4.3 Modos de Visualización

#### 4.3.1 Lista de Gestos
- **Vista por defecto:** Lista vertical con RecyclerView
- **Organización:** Módulos → Submódulos → Gestos (expandible)

#### 4.3.2 Logros
- **Vista por defecto:** Grid o Lista vertical
- **Agrupación:** Por categoría (opcional)

#### 4.3.3 Reportes
- **Vista de gráficos:**
  - Barras: Progreso por categoría
  - Torta: Distribución de logros
  - Línea: Progreso en el tiempo
- **Vista de tabla:** Lista paginada (5 items)

### 4.4 Reproducción de Videos

#### 4.4.1 Carga de Videos
- **Ubicación:** `assets/INFO/GESTOS/[nombre_gesto].mp4`
- **Componente:** `VideoView` o `ExoPlayer`
- **Sistema robusto:**
  - Intentar cargar desde assets
  - Si falla, intentar desde URL remota (opcional)
  - Mostrar error si no se encuentra

#### 4.4.2 Controles
- Play/Pause
- Seek bar
- Volumen
- Pantalla completa (opcional)

### 4.5 Cuestionarios (NO MENCIONADO EN DOCUMENTO)
- No implementar (no está en especificaciones)

---

## 5. BASE DE DATOS Y ENTIDADES

### 5.1 Tablas Existentes

#### 5.1.1 usuarios
```kotlin
@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey val id: Int,
    val nombre: String,
    val correo: String,
    val contraseña: String, // Hash
    val rol: String, // "Estudiante", "Docente", "Administrador"
    val fecha_registro: String,
    val sync_status: String // "pending" o "synced"
)
```

#### 5.1.2 gestos
```kotlin
@Entity(tableName = "gestos")
data class GestoEntity(
    @PrimaryKey val id: Int,
    val nombre: String,
    val categoria: String,
    val dificultad: String, // "Fácil", "Medio", "Difícil"
    val descripcion: String?, // NUEVO - agregar campo
    val sync_status: String
)
```

#### 5.1.3 modulos
```kotlin
@Entity(tableName = "modulos")
data class ModuloEntity(
    @PrimaryKey val id: Int,
    val nombre: String // "Básico", "Social", "Académico"
)
```

#### 5.1.4 submódulos
```kotlin
@Entity(tableName = "submodulos")
data class SubmoduloEntity(
    @PrimaryKey val id: Int,
    val id_modulo: Int,
    val nombre: String
)
```

#### 5.1.5 usuario_gestos
```kotlin
@Entity(tableName = "usuario_gestos")
data class UsuarioGestoEntity(
    @PrimaryKey val id: Int,
    val id_usuario: Int,
    val id_gesto: Int,
    val porcentaje: Int, // 0-100
    val estado: String, // "Pendiente" o "Aprendido"
    val sync_status: String,
    val lastUpdated: String // timestamp
)
```

#### 5.1.6 logros
```kotlin
@Entity(tableName = "logros")
data class LogroEntity(
    @PrimaryKey val id: Int,
    val nombre: String,
    val titulo: String,
    val descripcion: String,
    val categoria: String, // 6 categorías
    val condicion: String, // JSON o texto con condición
    val sync_status: String
)
```

#### 5.1.7 usuario_logros
```kotlin
@Entity(tableName = "usuario_logros")
data class UsuarioLogroEntity(
    @PrimaryKey val id: Int,
    val id_usuario: Int,
    val id_logro: Int,
    val fecha_obtenido: String, // timestamp
    val porcentajeAvance: Int, // 0-100
    val sync_status: String
)
```

#### 5.1.8 docente_estudiante
```kotlin
@Entity(tableName = "docente_estudiante")
data class DocenteEstudianteEntity(
    @PrimaryKey val id: Int,
    val id_docente: Int,
    val id_estudiante: Int,
    val estado: String, // "Pendiente", "Aceptado", "Rechazado"
    val fecha_solicitud: String, // NUEVO - agregar campo
    val sync_status: String,
    val lastUpdated: String
)
```

### 5.2 Tablas Nuevas (IMPLEMENTAR)

#### 5.2.1 historial_intentos
```kotlin
@Entity(tableName = "historial_intentos")
data class HistorialIntentoEntity(
    @PrimaryKey(autoGenerate = true) val id_historial: Int = 0,
    val id_usuario: Int,
    val id_gesto: Int,
    val porcentaje_obtenido: Int, // 0-100
    val fecha_intento: String, // timestamp
    val sync_status: String // "pending" o "synced"
)
```

**DAO:**
```kotlin
@Dao
interface HistorialIntentoDao {
    @Query("SELECT * FROM historial_intentos WHERE id_usuario = :idUsuario AND id_gesto = :idGesto ORDER BY fecha_intento DESC LIMIT 5")
    suspend fun getUltimosIntentos(idUsuario: Int, idGesto: Int): List<HistorialIntentoEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntento(intento: HistorialIntentoEntity)
    
    @Query("SELECT * FROM historial_intentos WHERE sync_status = 'pending'")
    suspend fun getPendingIntentos(): List<HistorialIntentoEntity>
}
```

#### 5.2.2 config
```kotlin
@Entity(tableName = "config")
data class ConfigEntity(
    @PrimaryKey val clave: String,
    val valor: String
)
```

**DAO:**
```kotlin
@Dao
interface ConfigDao {
    @Query("SELECT valor FROM config WHERE clave = :clave")
    suspend fun getValor(clave: String): String?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: ConfigEntity)
    
    @Query("SELECT * FROM config WHERE clave = 'tema'")
    suspend fun getTema(): ConfigEntity?
}
```

### 5.3 Relaciones y Foreign Keys

```kotlin
@Database(
    entities = [
        UsuarioEntity::class,
        GestoEntity::class,
        ModuloEntity::class,
        SubmoduloEntity::class,
        UsuarioGestoEntity::class,
        LogroEntity::class,
        UsuarioLogroEntity::class,
        DocenteEstudianteEntity::class,
        HistorialIntentoEntity::class, // NUEVO
        ConfigEntity::class // NUEVO
    ],
    version = 2, // Incrementar versión
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    // DAOs...
}
```

### 5.4 Migraciones

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Crear tabla historial_intentos
        database.execSQL("""
            CREATE TABLE historial_intentos (
                id_historial INTEGER PRIMARY KEY AUTOINCREMENT,
                id_usuario INTEGER NOT NULL,
                id_gesto INTEGER NOT NULL,
                porcentaje_obtenido INTEGER NOT NULL,
                fecha_intento TEXT NOT NULL,
                sync_status TEXT NOT NULL
            )
        """)
        
        // Crear tabla config
        database.execSQL("""
            CREATE TABLE config (
                clave TEXT PRIMARY KEY,
                valor TEXT NOT NULL
            )
        """)
        
        // Agregar campo descripcion a gestos
        database.execSQL("ALTER TABLE gestos ADD COLUMN descripcion TEXT")
        
        // Agregar campo fecha_solicitud a docente_estudiante
        database.execSQL("ALTER TABLE docente_estudiante ADD COLUMN fecha_solicitud TEXT")
    }
}
```

---

## 6. SINCRONIZACIÓN OFFLINE-FIRST

### 6.1 Estrategia Offline-First

**Principio:** Toda operación se guarda primero localmente, luego se sincroniza.

#### 6.1.1 Flujo de Sincronización

1. **Operación Local:**
   - Usuario realiza acción (practicar gesto, aceptar solicitud, etc.)
   - Guardar en SQLite con `sync_status = "pending"`
   - UI se actualiza inmediatamente

2. **Sincronización en Background:**
   - Si hay conexión → sincronizar inmediatamente
   - Si no hay conexión → marcar para sincronizar después
   - WorkManager ejecuta sincronización cada 15 minutos

3. **Sincronización Remota:**
   - Obtener solo datos con `sync_status = "pending"`
   - Enviar a servidor MySQL
   - Si éxito → actualizar `sync_status = "synced"`
   - Si error → mantener "pending" para reintentar

### 6.2 Archivos de Sincronización

#### 6.2.1 SyncManager.kt
```kotlin
class SyncManager(
    private val progresoRepository: ProgresoRepository,
    private val logroRepository: LogroRepository,
    private val docenteEstudianteRepository: DocenteEstudianteRepository,
    private val historialRepository: HistorialIntentoRepository,
    private val context: Context
) {
    fun startPeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncWork = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueue(syncWork)
    }
    
    suspend fun sincronizarInmediatamente() {
        if (NetworkUtils.isConnected(context)) {
            syncProgreso()
            syncLogros()
            syncRelaciones()
            syncHistorial()
        }
    }
    
    private suspend fun syncProgreso() {
        val pending = progresoRepository.getPendingProgreso()
        // Enviar a servidor y actualizar sync_status
    }
    
    // Similar para otros repositorios...
}
```

#### 6.2.2 SyncWorker.kt
```kotlin
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val syncManager = SyncManager(...)
            syncManager.sincronizarInmediatamente()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
```

### 6.3 Resolución de Conflictos (FALTANTE - IMPLEMENTAR)

#### 6.3.1 usuario_gestos
- **Regla:** Mantener porcentaje más alto
```kotlin
fun resolverConflictoProgreso(local: UsuarioGestoEntity, remote: UsuarioGestoEntity): UsuarioGestoEntity {
    return if (local.porcentaje > remote.porcentaje) {
        local.copy(sync_status = "synced")
    } else {
        remote.copy(sync_status = "synced")
    }
}
```

#### 6.3.2 usuario_logros
- **Regla:** Mantener fecha más reciente
```kotlin
fun resolverConflictoLogro(local: UsuarioLogroEntity, remote: UsuarioLogroEntity): UsuarioLogroEntity {
    val localDate = Date(local.fecha_obtenido)
    val remoteDate = Date(remote.fecha_obtenido)
    return if (localDate.after(remoteDate)) {
        local.copy(sync_status = "synced")
    } else {
        remote.copy(sync_status = "synced")
    }
}
```

### 6.4 Indicador Visual de Sincronización (FALTANTE - IMPLEMENTAR)

**Ubicación:** Toolbar de `MainActivity` o `HomeFragment`

**Componente:**
- `ImageView` con icono de sincronización
- Animación rotatoria durante sincronización
- Badge con cantidad de elementos pendientes
- Color: Verde (sincronizado), Amarillo (pendiente), Rojo (error)

**Implementación:**
```kotlin
fun actualizarIndicadorSincronizacion() {
    viewModelScope.launch {
        val pendientes = syncManager.getPendingCount()
        if (pendientes > 0) {
            // Mostrar badge con número
            // Icono amarillo
        } else {
            // Sin badge
            // Icono verde
        }
    }
}
```

---

## 7. SISTEMA DE LOGROS

### 7.1 Categorías de Logros (6 categorías)

1. **📘 Progreso Básico**
   - Primer gesto aprendido
   - 10 gestos aprendidos
   - 25 gestos aprendidos
   - 50 gestos aprendidos

2. **📚 Aprendizaje y Tareas**
   - 5 días consecutivos de práctica
   - 10 días consecutivos de práctica
   - 50% de promedio de progreso
   - 75% de promedio de progreso

3. **🎯 Rendimiento**
   - 100% en un gesto
   - 10 gestos al 80%
   - 20 gestos al 80%
   - Todos los gestos aprendidos

4. **🔁 Frecuencia y Hábitos**
   - 7 días activos en un mes
   - 15 días activos en un mes
   - 30 días activos en un mes
   - Streak de 30 días

5. **⭐ Participación y Comunidad**
   - Enviar primera solicitud a docente
   - Vincularse con un docente
   - Completar perfil

6. **🧠 Dominio del Contenido**
   - Dominar categoría Básico (100%)
   - Dominar categoría Social (100%)
   - Dominar categoría Académico (100%)
   - Dominar todas las categorías

### 7.2 Detección Automática de Logros (FALTANTE - IMPLEMENTAR)

**Archivo:** `LogroRepository.kt`

**Función a Implementar:**
```kotlin
suspend fun verificarYDesbloquearLogros(idUsuario: Int): Result<List<LogrosResponse>> {
    val logrosDesbloqueados = mutableListOf<LogrosResponse>()
    
    // 1. Obtener progreso del usuario
    val progreso = progresoRepository.getProgresoUsuario(idUsuario)
    val logros = logroDao.getAllLogros()
    
    // 2. Verificar cada logro
    for (logro in logros) {
        val yaObtenido = usuarioLogroDao.existeLogro(idUsuario, logro.id)
        if (!yaObtenido && cumpleCondicion(logro, progreso)) {
            // 3. Insertar en usuario_logros
            val usuarioLogro = UsuarioLogroEntity(
                id_usuario = idUsuario,
                id_logro = logro.id,
                fecha_obtenido = Date().toString(),
                porcentajeAvance = 100,
                sync_status = "pending"
            )
            usuarioLogroDao.insert(usuarioLogro)
            logrosDesbloqueados.add(convertirAResponse(logro))
            
            // 4. Notificar al usuario
            NotificationManager.mostrarNotificacionLogro(logro)
        }
    }
    
    return Result.success(logrosDesbloqueados)
}

private fun cumpleCondicion(logro: LogroEntity, progreso: ProgresoUsuario): Boolean {
    return when (logro.categoria) {
        "Progreso Básico" -> verificarProgresoBasico(logro, progreso)
        "Aprendizaje y Tareas" -> verificarAprendizaje(logro, progreso)
        "Rendimiento" -> verificarRendimiento(logro, progreso)
        "Frecuencia y Hábitos" -> verificarFrecuencia(logro, progreso)
        "Participación y Comunidad" -> verificarParticipacion(logro, progreso)
        "Dominio del Contenido" -> verificarDominio(logro, progreso)
        else -> false
    }
}
```

### 7.3 Llamadas a Verificación

**Lugares donde verificar logros:**
1. Después de actualizar progreso de gesto (`ProgresoRepository.updateProgreso()`)
2. Después de vincularse con docente (`DocenteEstudianteRepository.aceptarSolicitud()`)
3. Periódicamente (cada vez que se abre la app o cada 24 horas)

### 7.4 Notificaciones de Logros (FALTANTE - IMPLEMENTAR)

**Archivo:** `NotificationManager.kt` (CREAR)

```kotlin
object NotificationManager {
    fun mostrarNotificacionLogro(logro: LogroEntity, context: Context) {
        // Toast o Snackbar
        Toast.makeText(context, "¡Has obtenido el logro: ${logro.titulo}!", Toast.LENGTH_LONG).show()
        
        // Opcional: Notificación push
        if (configRepository.getNotificacionesLogros()) {
            crearNotificacionPush(context, logro)
        }
    }
}
```

---

## 8. DETECCIÓN DE GESTOS CON TFLITE

### 8.1 Arquitectura de Detección

**Archivos:**
- `CameraActivity.kt` - UI y cámara
- `GestureRecognitionManager.kt` - Lógica de reconocimiento
- `HandDetector.kt` - Detección de manos (MediaPipe)

### 8.2 Flujo de Detección

1. **Inicialización:**
   - Cargar modelo TFLite para gesto específico
   - Inicializar `CameraX` con `Preview` y `ImageAnalysis`

2. **Procesamiento de Frames:**
   - Capturar frame de cámara
   - Convertir a formato requerido por modelo
   - Ejecutar inferencia con TFLite
   - Obtener predicción (probabilidad de gesto)

3. **Comparación:**
   - Comparar gesto detectado con gesto objetivo
   - Calcular porcentaje de similitud (0-100%)

4. **Actualización:**
   - Mostrar porcentaje en UI
   - Si porcentaje ≥ umbral (ej: 70%) → considerar exitoso
   - Actualizar progreso en BD

### 8.3 Modelo TFLite

**Ubicación:** `assets/models/gesto_[id].tflite`

**Carga:**
```kotlin
private fun cargarModelo(idGesto: Int): Interpreter {
    val modelFile = "models/gesto_$idGesto.tflite"
    val inputStream = assets.open(modelFile)
    val model = FileUtil.loadMappedFile(context, modelFile)
    return Interpreter(model)
}
```

### 8.4 MediaPipe (Alternativa)

Si se usa MediaPipe en lugar de TFLite:
- Usar `HandLandmarker` de MediaPipe
- Extraer landmarks de manos
- Comparar con gesto de referencia usando distancia euclidiana

---

## 9. TEMAS DARK/LIGHT MODE

### 9.1 Recursos de Temas

**Archivos:**
- `res/values/themes.xml` - Tema Light
- `res/values-night/themes.xml` - Tema Dark
- `res/values/colors.xml` - Colores Light
- `res/values-night/colors.xml` - Colores Dark

### 9.2 Tema Base

**themes.xml (Light):**
```xml
<style name="Theme.Ensenando" parent="Theme.MaterialComponents.DayNight.NoActionBar">
    <item name="colorPrimary">@color/primary</item>
    <item name="colorPrimaryVariant">@color/primaryVariant</item>
    <item name="colorOnPrimary">@color/onPrimary</item>
    <item name="colorSecondary">@color/secondary</item>
    <item name="colorBackground">@color/background</item>
    <item name="colorSurface">@color/surface</item>
</style>
```

**themes.xml (Dark):**
```xml
<style name="Theme.Ensenando" parent="Theme.MaterialComponents.DayNight.NoActionBar">
    <!-- Mismos items con colores oscuros -->
</style>
```

### 9.3 Control Manual (FALTANTE - IMPLEMENTAR)

**Archivo:** `ThemeUtils.kt` (CREAR)

```kotlin
object ThemeUtils {
    fun aplicarTema(context: Context, modoOscuro: Boolean) {
        val modo = if (modoOscuro) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(modo)
    }
    
    fun obtenerTemaGuardado(context: Context): Boolean {
        val config = configRepository.getTema()
        return config?.valor == "dark"
    }
}
```

**En SettingsFragment:**
```kotlin
switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
    ThemeUtils.aplicarTema(requireContext(), isChecked)
    configRepository.guardarTema(isChecked)
}
```

**En Application o MainActivity:**
```kotlin
override fun onCreate() {
    super.onCreate()
    val temaOscuro = ThemeUtils.obtenerTemaGuardado(this)
    ThemeUtils.aplicarTema(this, temaOscuro)
}
```

---

## 10. VALIDACIONES Y MANEJO DE ERRORES

### 10.1 Validaciones de Formularios

#### 10.1.1 Login
- **Correo:** No vacío, formato válido (contiene @ y dominio)
- **Contraseña:** No vacía, mínimo 6 caracteres
- **Mensajes de error:** Toast con mensaje específico

#### 10.1.2 Registro
- **Nombre:** No vacío, mínimo 2 caracteres
- **Correo:** No vacío, formato válido, único en BD
- **Contraseña:** No vacía, mínimo 6 caracteres
- **Confirmar contraseña:** Debe coincidir con contraseña
- **Rol:** Debe seleccionarse

#### 10.1.3 Editar Perfil
- **Nombre:** No vacío, mínimo 2 caracteres

#### 10.1.4 Cambiar Contraseña
- **Contraseña actual:** Debe ser correcta
- **Nueva contraseña:** No vacía, mínimo 6 caracteres
- **Confirmar contraseña:** Debe coincidir

### 10.2 Manejo de Errores de Red

**Archivo:** `NetworkUtils.kt`

```kotlin
object NetworkUtils {
    fun isConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        return network != null
    }
    
    fun manejarErrorRed(error: Throwable): String {
        return when (error) {
            is UnknownHostException -> "No hay conexión a internet"
            is SocketTimeoutException -> "Tiempo de espera agotado"
            is IOException -> "Error de conexión"
            else -> "Error desconocido: ${error.message}"
        }
    }
}
```

### 10.3 Manejo de Errores de Base de Datos

```kotlin
try {
    usuarioRepository.insertUsuario(usuario)
} catch (e: SQLiteConstraintException) {
    // Usuario ya existe
    mostrarError("El correo ya está registrado")
} catch (e: Exception) {
    mostrarError("Error al guardar: ${e.message}")
}
```

### 10.4 Mensajes de Error Específicos

**Login:**
- "Correo o contraseña incorrectos"
- "Campos vacíos"
- "Formato de correo inválido"

**Registro:**
- "El correo ya está registrado"
- "Las contraseñas no coinciden"
- "Contraseña muy corta (mínimo 6 caracteres)"

**Sincronización:**
- "Error de sincronización. Los datos se guardaron localmente."
- "Sin conexión. Sincronización pendiente."

**Detección de Gestos:**
- "Error al cargar modelo"
- "Error al inicializar cámara"
- "Permisos de cámara denegados"

---

## 11. COMPONENTES TÉCNICOS DE ANDROID

### 11.1 Navegación

**Componente:** Navigation Component (Jetpack)

**Archivo:** `res/navigation/nav_graph.xml`

```xml
<navigation>
    <fragment id="home" ... />
    <fragment id="logros" ... />
    <fragment id="profile" ... />
    <fragment id="activity" ...>
        <argument name="idGesto" type="int" />
    </fragment>
    <fragment id="logroDetail" ...>
        <argument name="idLogro" type="int" />
    </fragment>
    <!-- Más fragments... -->
</navigation>
```

**Uso:**
```kotlin
findNavController().navigate(
    HomeFragmentDirections.actionHomeToActivity(idGesto)
)
```

### 11.2 RecyclerView y Adapters

**Ejemplo: GestoAdapter**
```kotlin
class GestoAdapter(
    private val gestos: List<Gesto>,
    private val onItemClick: (Gesto) -> Unit
) : RecyclerView.Adapter<GestoAdapter.ViewHolder>() {
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val gesto = gestos[position]
        holder.bind(gesto)
    }
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(gesto: Gesto) {
            itemView.tvGestoNombre.text = gesto.nombre
            itemView.tvCategoria.text = gesto.categoria
            itemView.tvDificultad.text = gesto.dificultad
            itemView.progressBar.progress = gesto.porcentaje
            itemView.tvPorcentaje.text = "${gesto.porcentaje}%"
            itemView.chipEstado.text = gesto.estado
            
            itemView.setOnClickListener { onItemClick(gesto) }
        }
    }
}
```

### 11.3 ViewModel y LiveData

**Ejemplo: HomeViewModel**
```kotlin
class HomeViewModel(
    private val gestoRepository: GestoRepository,
    private val progresoRepository: ProgresoRepository
) : ViewModel() {
    
    private val _gestos = MutableLiveData<List<Gesto>>()
    val gestos: LiveData<List<Gesto>> = _gestos
    
    private val _progreso = MutableLiveData<ProgresoResumen>()
    val progreso: LiveData<ProgresoResumen> = _progreso
    
    init {
        cargarDatos()
    }
    
    private fun cargarDatos() {
        viewModelScope.launch {
            _gestos.value = gestoRepository.getAllGestos()
            _progreso.value = progresoRepository.getResumenProgreso()
        }
    }
}
```

### 11.4 Room Database

**AppDatabase.kt**
```kotlin
@Database(entities = [...], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun gestoDao(): GestoDao
    // Más DAOs...
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "ensenando_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

### 11.5 Retrofit para API

**ApiService.kt**
```kotlin
interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<UsuarioResponse>
    
    @POST("sync/progreso")
    suspend fun syncProgreso(@Body progreso: List<UsuarioGestoResponse>): Response<SyncResponse>
    
    // Más endpoints...
}
```

**ApiClient.kt**
```kotlin
object ApiClient {
    private const val BASE_URL = "https://api.ensenando.com/"
    
    val apiService: ApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)
}
```

### 11.6 WorkManager para Sincronización

**SyncWorker.kt**
```kotlin
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val syncManager = SyncManager(...)
            syncManager.sincronizarInmediatamente()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
```

### 11.7 CameraX para Cámara

**CameraActivity.kt**
```kotlin
class CameraActivity : AppCompatActivity() {
    private lateinit var cameraProviderFuture: ProcessCameraProvider
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().build()
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        
        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            procesarFrame(imageProxy)
        }
        
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
    }
}
```

### 11.8 ExoPlayer para Videos

**Carga de Video:**
```kotlin
private fun cargarVideo(nombreGesto: String) {
    val videoPath = "INFO/GESTOS/$nombreGesto.mp4"
    val assetFileDescriptor = assets.openFd(videoPath)
    
    val player = ExoPlayer.Builder(context).build()
    val mediaItem = MediaItem.fromUri(assetFileDescriptor.fileDescriptor)
    player.setMediaItem(mediaItem)
    player.prepare()
    player.playWhenReady = true
    
    videoView.player = player
}
```

### 11.9 PDF Generation

**PdfGenerator.kt**
```kotlin
object PdfGenerator {
    fun generarReportePDF(context: Context, usuario: Usuario, progreso: Progreso): File {
        val document = PdfDocument()
        val pageInfo = PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()
        
        // Dibujar contenido...
        canvas.drawText("Reporte de Progreso", 50f, 50f, paint)
        // Más contenido...
        
        document.finishPage(page)
        
        val file = File(context.getExternalFilesDir(null), "reporte_${usuario.id}.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()
        
        return file
    }
}
```

### 11.10 Gráficos (MPAndroidChart)

**Dependencia:**
```gradle
implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
```

**Uso:**
```kotlin
val barChart: BarChart = findViewById(R.id.barChart)
val entries = listOf(
    BarEntry(0f, 75f), // Categoría 1: 75%
    BarEntry(1f, 60f), // Categoría 2: 60%
    // Más datos...
)
val dataSet = BarDataSet(entries, "Progreso por Categoría")
val data = BarData(dataSet)
barChart.data = data
barChart.invalidate()
```

---

## 12. ENDPOINTS DE LA API

### 12.1 Configuración Base

**Base URL:** Configurar según el servidor (ej: `https://api.ensenando.com/` o `http://localhost/api/`)

**Autenticación:** JWT (JSON Web Token)
- Todos los endpoints (excepto `login.php` y `register.php`) requieren autenticación
- Token se envía en header: `Authorization: Bearer {token}`
- Tokens expiran después de 7 días
- El token se obtiene en `login.php` o `register.php`

**Headers Comunes:**
```
Content-Type: application/json
Authorization: Bearer {token}
```

**Formato de Respuesta:**
```json
{
  "success": true/false,
  "message": "Mensaje descriptivo",
  "data": { ... } // Opcional
}
```

**Códigos de Estado HTTP:**
- `200` - Éxito
- `400` - Solicitud incorrecta (parámetros faltantes o inválidos)
- `401` - No autenticado (token faltante o inválido)
- `403` - Sin permisos (rol insuficiente)
- `404` - Recurso no encontrado
- `405` - Método HTTP no permitido
- `409` - Conflicto (recurso ya existe)
- `500` - Error del servidor

### 12.2 Autenticación

#### POST /login.php
Iniciar sesión (NO requiere autenticación)

**Request Body:**
```json
{
  "correo": "usuario@example.com",
  "contrasena": "password123"
}
```

**Response (200):**
```json
{
  "success": true,
  "message": "Login exitoso",
  "usuario": {
    "id_usuario": 1,
    "nombre": "Juan Pérez",
    "correo": "usuario@example.com",
    "rol": "estudiante",
    "fecha_registro": "2025-01-01 00:00:00"
  },
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Errores:**
- `400` - Correo y contraseña son requeridos
- `401` - Credenciales inválidas

**Implementación Retrofit:**
```kotlin
@POST("login.php")
suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

data class LoginRequest(
    val correo: String,
    val contrasena: String
)

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val usuario: UsuarioResponse,
    val token: String
)
```

#### POST /register.php
Registrar nuevo usuario (NO requiere autenticación)

**Request Body:**
```json
{
  "nombre": "Juan Pérez",
  "correo": "usuario@example.com",
  "contrasena": "password123",
  "rol": "estudiante"
}
```

**Roles válidos:** `"estudiante"`, `"docente"`, `"administrador"`

**Response (200):**
```json
{
  "success": true,
  "message": "Registro exitoso",
  "usuario": {
    "id_usuario": 1,
    "nombre": "Juan Pérez",
    "correo": "usuario@example.com",
    "rol": "estudiante",
    "fecha_registro": "2025-01-15 10:30:00"
  },
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Errores:**
- `400` - Todos los campos son requeridos / Rol inválido
- `409` - El correo ya está registrado
- `500` - Error al registrar usuario

**Implementación Retrofit:**
```kotlin
@POST("register.php")
suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

data class RegisterRequest(
    val nombre: String,
    val correo: String,
    val contrasena: String,
    val rol: String
)
```

### 12.3 Gestos

#### GET /gestos.php
Obtener todos los gestos (requiere autenticación)

**Headers:** `Authorization: Bearer {token}`

**Response (200):**
```json
{
  "success": true,
  "message": "Gestos obtenidos exitosamente",
  "gestos": [
    {
      "id_gesto": 1,
      "nombre": "Hola",
      "dificultad": "Fácil",
      "categoria": "BASICO"
    },
    {
      "id_gesto": 2,
      "nombre": "Gracias",
      "dificultad": "Fácil",
      "categoria": "SOCIAL"
    }
  ]
}
```

**Implementación Retrofit:**
```kotlin
@GET("gestos.php")
suspend fun getGestos(@Header("Authorization") token: String): Response<GestosResponse>

data class GestosResponse(
    val success: Boolean,
    val message: String,
    val gestos: List<GestoResponse>
)

data class GestoResponse(
    val id_gesto: Int,
    val nombre: String,
    val dificultad: String,
    val categoria: String
)
```

#### GET /listar_gestos.php
Alias de `/gestos.php` (mismo comportamiento)

### 12.4 Progreso de Usuario

#### GET /obtener_gestos_usuario.php
Obtener progreso del usuario (requiere autenticación)

**Headers:** `Authorization: Bearer {token}`

**Query Parameters:**
- `id_usuario` (opcional) - Si no se proporciona, usa el usuario del token
- `usuario_id` (opcional) - Alias de `id_usuario`

**Permisos:**
- Usuario puede ver su propio progreso
- Docente y Administrador pueden ver cualquier progreso

**Response (200):**
```json
[
  {
    "id_usuario": 1,
    "id_gesto": 1,
    "porcentaje": 85,
    "estado": "aprendido"
  },
  {
    "id_usuario": 1,
    "id_gesto": 2,
    "porcentaje": 45,
    "estado": "pendiente"
  }
]
```

**Errores:**
- `403` - No tiene permisos para ver este progreso

**Implementación Retrofit:**
```kotlin
@GET("obtener_gestos_usuario.php")
suspend fun getProgresoUsuario(
    @Header("Authorization") token: String,
    @Query("id_usuario") idUsuario: Int? = null
): Response<List<UsuarioGestoResponse>>

data class UsuarioGestoResponse(
    val id_usuario: Int,
    val id_gesto: Int,
    val porcentaje: Int,
    val estado: String
)
```

#### POST /actualizar_progreso_gesto.php
Actualizar progreso de un gesto (requiere autenticación)

**Headers:** `Authorization: Bearer {token}`

**Request Body:**
```json
{
  "id_usuario": 1,  // Opcional, usa usuario del token si no se proporciona
  "id_gesto": 1,
  "porcentaje": 85,
  "estado": "aprendido"  // "pendiente" o "aprendido"
}
```

**Permisos:**
- Usuario puede actualizar su propio progreso
- Docente y Administrador pueden actualizar cualquier progreso

**Response (200):**
```json
{
  "success": true,
  "message": "Progreso actualizado exitosamente",
  "data": {
    "id_usuario": 1,
    "id_gesto": 1,
    "porcentaje": 85,
    "estado": "aprendido"
  }
}
```

**Errores:**
- `400` - id_gesto, porcentaje y estado son requeridos
- `403` - No tiene permisos para actualizar este progreso

**Implementación Retrofit:**
```kotlin
@POST("actualizar_progreso_gesto.php")
suspend fun actualizarProgreso(
    @Header("Authorization") token: String,
    @Body request: ActualizarProgresoRequest
): Response<ActualizarProgresoResponse>

data class ActualizarProgresoRequest(
    val id_usuario: Int? = null,
    val id_gesto: Int,
    val porcentaje: Int,
    val estado: String
)
```

#### GET /obtener_home_data.php
Obtener datos para el dashboard/home (requiere autenticación)

**Headers:** `Authorization: Bearer {token}`

**Query Parameters:**
- `id_usuario` (opcional) - Si no se proporciona, usa el usuario del token
- `usuario_id` (opcional) - Alias de `id_usuario`
- `categoria` (opcional) - Filtrar por categoría

**Response (200):**
```json
{
  "success": true,
  "message": "Datos obtenidos exitosamente",
  "usuario": {
    "id_usuario": 1,
    "nombre": "Juan Pérez",
    "correo": "usuario@example.com",
    "rol": "estudiante"
  },
  "estadisticas": {
    "tiempo_total_minutos": 1200,
    "promedio_progreso": 75,
    "actividades_incompletas": 5,
    "gestos_aprendidos": 20
  },
  "actividades": [
    {
      "id_gesto": 1,
      "nombre": "Hola",
      "categoria": "BASICO",
      "dificultad": "Fácil",
      "porcentaje": 85,
      "estado": "aprendido"
    }
  ],
  "categorias": [
    {
      "categoria": "BASICO",
      "total": 30,
      "aprendidos": 15
    },
    {
      "categoria": "SOCIAL",
      "total": 25,
      "aprendidos": 5
    }
  ]
}
```

**Implementación Retrofit:**
```kotlin
@GET("obtener_home_data.php")
suspend fun getHomeData(
    @Header("Authorization") token: String,
    @Query("id_usuario") idUsuario: Int? = null,
    @Query("categoria") categoria: String? = null
): Response<HomeDataResponse>
```

### 12.5 Sincronización

#### POST /sync.php
Sincronizar datos offline (requiere autenticación)

**Headers:** `Authorization: Bearer {token}`

**Request Body:**
```json
{
  "usuario_gestos": [
    {
      "id_usuario": 1,
      "id_gesto": 1,
      "porcentaje": 85,
      "estado": "aprendido",
      "last_updated": 1234567890
    }
  ],
  "docente_estudiante": [
    {
      "id_docente": 2,
      "id_estudiante": 1,
      "estado": "aceptado",
      "last_updated": 1234567890
    }
  ]
}
```

**Response (200):**
```json
{
  "usuario_gestos": [
    {
      "id_usuario": 1,
      "id_gesto": 1,
      "porcentaje": 85,
      "estado": "aprendido"
    }
  ],
  "docente_estudiante": [
    {
      "id_docente": 2,
      "id_estudiante": 1,
      "estado": "aceptado"
    }
  ]
}
```

**Nota:** La respuesta NO está envuelta en `success/message/data`, devuelve directamente el objeto.

**Lógica de Sincronización:**
- Para `usuario_gestos`: Solo actualiza si el nuevo porcentaje es mayor o si cambia el estado a "aprendido"
- Para `docente_estudiante`: Actualiza o inserta según corresponda

**Implementación Retrofit:**
```kotlin
@POST("sync.php")
suspend fun sync(
    @Header("Authorization") token: String,
    @Body request: SyncRequest
): Response<SyncResponse>

data class SyncRequest(
    val usuario_gestos: List<UsuarioGestoSyncItem>? = null,
    val docente_estudiante: List<DocenteEstudianteSyncItem>? = null
)

data class SyncResponse(
    val usuario_gestos: List<UsuarioGestoResponse>? = null,
    val docente_estudiante: List<DocenteEstudianteResponse>? = null
)
```

### 12.6 Relaciones Docente-Estudiante

#### GET /listar_solicitudes_docente.php
Obtener solicitudes recibidas por un docente (requiere autenticación)

**Headers:** `Authorization: Bearer {token}`

**Query Parameters:**
- `id_docente` (opcional) - Si no se proporciona, usa el usuario del token
- `docente_id` (opcional) - Alias de `id_docente`
- `id_usuario` (opcional) - Alias de `id_docente`
- `usuario_id` (opcional) - Alias de `id_docente`
- `estado` (opcional) - Filtrar por estado ("pendiente", "aceptado", "rechazado")

**Permisos:**
- Solo el docente puede ver sus propias solicitudes
- Administrador puede ver cualquier solicitud

**Response (200):**
```json
{
  "success": true,
  "message": "Solicitudes obtenidas exitosamente",
  "solicitudes": [
    {
      "id_docente": 2,
      "id_estudiante": 1,
      "estado": "pendiente",
      "estudiante": {
        "id_usuario": 1,
        "nombre": "Juan Pérez",
        "correo": "juan@example.com"
      }
    }
  ],
  "total": 1
}
```

**Implementación Retrofit:**
```kotlin
@GET("listar_solicitudes_docente.php")
suspend fun getSolicitudesDocente(
    @Header("Authorization") token: String,
    @Query("id_docente") idDocente: Int? = null,
    @Query("estado") estado: String? = null
): Response<SolicitudesDocenteResponse>
```

#### GET /consultar_solicitud_estudiante.php
Obtener solicitudes enviadas por un estudiante (requiere autenticación)

**Headers:** `Authorization: Bearer {token}`

**Query Parameters:**
- `id_estudiante` (opcional) - Si no se proporciona, usa el usuario del token
- `estudiante_id` (opcional) - Alias de `id_estudiante`
- `id_usuario` (opcional) - Alias de `id_estudiante`
- `usuario_id` (opcional) - Alias de `id_estudiante`

**Permisos:**
- Estudiante puede ver sus propias solicitudes
- Docente y Administrador pueden ver cualquier solicitud

**Response (200):**
```json
{
  "success": true,
  "message": "Solicitudes obtenidas exitosamente",
  "solicitudes": [
    {
      "id_docente": 2,
      "id_estudiante": 1,
      "estado": "pendiente",
      "docente": {
        "id_usuario": 2,
        "nombre": "Profesor García",
        "correo": "profesor@example.com"
      }
    }
  ],
  "docente_actual": {
    "id_usuario": 2,
    "nombre": "Profesor García",
    "correo": "profesor@example.com"
  },
  "total": 1
}
```

**Implementación Retrofit:**
```kotlin
@GET("consultar_solicitud_estudiante.php")
suspend fun getSolicitudesEstudiante(
    @Header("Authorization") token: String,
    @Query("id_estudiante") idEstudiante: Int? = null
): Response<SolicitudesEstudianteResponse>
```

#### POST /enviar_solicitud_docente.php
Enviar solicitud de estudiante a docente (requiere autenticación)

**Headers:** `Authorization: Bearer {token}`

**Request Body:**
```json
{
  "id_docente": 2,
  "id_estudiante": 1  // Opcional, usa usuario del token si es estudiante
}
```

**Permisos:**
- Solo estudiantes pueden enviar solicitudes
- Administrador también puede enviar solicitudes

**Response (200):**
```json
{
  "success": true,
  "message": "Solicitud enviada exitosamente",
  "data": null
}
```

**Errores:**
- `400` - id_docente e id_estudiante son requeridos / El docente especificado no existe o no es docente
- `403` - Solo estudiantes pueden enviar solicitudes
- `409` - Ya existe una relación entre este docente y estudiante

**Implementación Retrofit:**
```kotlin
@POST("enviar_solicitud_docente.php")
suspend fun enviarSolicitud(
    @Header("Authorization") token: String,
    @Body request: EnviarSolicitudRequest
): Response<EnviarSolicitudResponse>

data class EnviarSolicitudRequest(
    val id_docente: Int,
    val id_estudiante: Int? = null
)
```

#### POST /responder_solicitud.php
Aceptar o rechazar solicitud (requiere autenticación)

**Headers:** `Authorization: Bearer {token}`

**Request Body:**
```json
{
  "id_docente": 2,  // Opcional, usa usuario del token si es docente
  "id_estudiante": 1,
  "accion": "aceptar"  // "aceptar" o "rechazar"
}
```

**Permisos:**
- Solo el docente puede responder sus solicitudes
- Administrador puede responder cualquier solicitud

**Response (200):**
```json
{
  "success": true,
  "message": "Solicitud respondida exitosamente",
  "data": null
}
```

**Errores:**
- `400` - id_docente, id_estudiante y accion son requeridos / La acción debe ser "aceptar" o "rechazar"
- `403` - Solo el docente puede responder la solicitud / No tiene permisos para responder esta solicitud
- `404` - No se encontró la solicitud
- `409` - Esta solicitud ya fue respondida

**Implementación Retrofit:**
```kotlin
@POST("responder_solicitud.php")
suspend fun responderSolicitud(
    @Header("Authorization") token: String,
    @Body request: ResponderSolicitudRequest
): Response<ResponderSolicitudResponse>

data class ResponderSolicitudRequest(
    val id_docente: Int? = null,
    val id_estudiante: Int,
    val accion: String  // "aceptar" o "rechazar"
)
```

#### POST /eliminar_relacion_docente.php
Eliminar relación docente-estudiante (requiere autenticación)

**Headers:** `Authorization: Bearer {token}`

**Request Body:**
```json
{
  "id_docente": 2,
  "id_estudiante": 1
}
```

**Permisos:**
- Docente, estudiante o administrador pueden eliminar la relación

**Response (200):**
```json
{
  "success": true,
  "message": "Relación eliminada exitosamente",
  "data": null
}
```

**Errores:**
- `400` - id_docente e id_estudiante son requeridos
- `403` - No tiene permisos para eliminar esta relación
- `404` - No se encontró la relación

### 12.7 Logros

#### GET /obtener_logros_usuarios.php
Obtener logros de un usuario (requiere autenticación)

**Headers:** `Authorization: Bearer {token}`

**Query Parameters:**
- `id_usuario` (opcional) - Si no se proporciona, usa el usuario del token
- `usuario_id` (opcional) - Alias de `id_usuario`
- `id_estudiante` (opcional) - Alias de `id_usuario`
- `id_admin` (opcional) - Para filtros de administrador

**Permisos:**
- Usuario puede ver sus propios logros
- Docente y Administrador pueden ver cualquier logro

**Response (200):**
```json
[
  {
    "id_usuario": 1,
    "id_logro": 1,
    "titulo": "Primer gesto aprendido",
    "descripcion": "Completa tu primer gesto con al menos 80%",
    "fecha_obtenido": "2025-01-15 10:30:00",
    "nombre": "Juan Pérez",
    "correo": "juan@example.com",
    "desbloqueado": true
  }
]
```

**Nota:** La respuesta es un array directo, NO está envuelta en `success/message/data`.

**Implementación Retrofit:**
```kotlin
@GET("obtener_logros_usuarios.php")
suspend fun getLogrosUsuario(
    @Header("Authorization") token: String,
    @Query("id_usuario") idUsuario: Int? = null
): Response<List<LogroUsuarioResponse>>

data class LogroUsuarioResponse(
    val id_usuario: Int,
    val id_logro: Int,
    val titulo: String,
    val descripcion: String,
    val fecha_obtenido: String,
    val nombre: String,
    val correo: String,
    val desbloqueado: Boolean
)
```

#### POST /desbloquear_logro.php
Desbloquear un logro para un usuario (requiere autenticación)

**Headers:** `Authorization: Bearer {token}`

**Request Body:**
```json
{
  "id_usuario": 1,  // Opcional, usa usuario del token si no se proporciona
  "id_logro": 1,
  "fecha_obtenido": "2025-01-15 10:30:00"  // Opcional, usa fecha actual si no se proporciona
}
```

**Permisos:**
- Usuario puede desbloquear sus propios logros
- Docente y Administrador pueden desbloquear cualquier logro

**Response (200):**
```json
{
  "success": true,
  "message": "Logro desbloqueado exitosamente",
  "data": {
    "id_usuario": 1,
    "id_logro": 1,
    "fecha_obtenido": "2025-01-15 10:30:00",
    "logro": {
      "id_logro": 1,
      "titulo": "Primer gesto aprendido",
      "descripcion": "Completa tu primer gesto con al menos 80%"
    }
  }
}
```

**Errores:**
- `400` - id_logro es requerido
- `403` - No tiene permisos para desbloquear este logro
- `404` - Usuario no encontrado / Logro no encontrado

**Nota:** Si el logro ya está desbloqueado, actualiza la fecha si es más reciente.

**Implementación Retrofit:**
```kotlin
@POST("desbloquear_logro.php")
suspend fun desbloquearLogro(
    @Header("Authorization") token: String,
    @Body request: DesbloquearLogroRequest
): Response<DesbloquearLogroResponse>

data class DesbloquearLogroRequest(
    val id_usuario: Int? = null,
    val id_logro: Int,
    val fecha_obtenido: String? = null
)
```

### 12.8 Docentes y Estudiantes

#### GET /listar_docentes.php
Obtener lista de todos los docentes (requiere autenticación)

**Headers:** `Authorization: Bearer {token}`

**Response (200):**
```json
[
  {
    "id_usuario": 2,
    "nombre": "Profesor García",
    "correo": "profesor@example.com",
    "rol": "docente",
    "fecha_registro": "2025-01-01 00:00:00"
  }
]
```

**Nota:** La respuesta es un array directo, NO está envuelta en `success/message/data`.

**Implementación Retrofit:**
```kotlin
@GET("listar_docentes.php")
suspend fun listarDocentes(
    @Header("Authorization") token: String
): Response<List<UsuarioResponse>>
```

#### GET /listar_estudiantes_docente.php
Obtener estudiantes vinculados a un docente (requiere autenticación)

**Headers:** `Authorization: Bearer {token}`

**Query Parameters:**
- `id_docente` (opcional) - Si no se proporciona, usa el usuario del token
- `docente_id` (opcional) - Alias de `id_docente`
- `id_usuario` (opcional) - Alias de `id_docente`
- `usuario_id` (opcional) - Alias de `id_docente`

**Permisos:**
- Solo el docente puede ver sus estudiantes
- Administrador puede ver cualquier lista

**Response (200):**
```json
[
  {
    "id_usuario": 1,
    "nombre": "Juan Pérez",
    "correo": "juan@example.com",
    "rol": "estudiante",
    "fecha_registro": "2025-01-01 00:00:00"
  }
]
```

**Nota:** Solo devuelve estudiantes con relación "aceptada".

**Implementación Retrofit:**
```kotlin
@GET("listar_estudiantes_docente.php")
suspend fun listarEstudiantesDocente(
    @Header("Authorization") token: String,
    @Query("id_docente") idDocente: Int? = null
): Response<List<UsuarioResponse>>
```

#### GET /obtener_progreso_estudiante_docente.php
Obtener progreso detallado de un estudiante (para docentes) (requiere autenticación)

**Headers:** `Authorization: Bearer {token}`

**Query Parameters:**
- `id_docente` (opcional) - Si no se proporciona, usa el usuario del token
- `id_estudiante` (requerido)

**Permisos:**
- Solo el docente puede ver el progreso de sus estudiantes
- Administrador puede ver cualquier progreso

**Response (200):**
```json
{
  "tiempoTotal": 0,
  "leccionesCompletadas": 20,
  "totalLecciones": 118,
  "precision": 0.75,
  "rachaDias": 0,
  "progreso": [
    {
      "id_usuario": 1,
      "nombre": "Juan Pérez",
      "correo": "juan@example.com",
      "id_gesto": 1,
      "nombre_gesto": "Hola",
      "categoria": "BASICO",
      "dificultad": "Fácil",
      "porcentaje": 85,
      "estado": "aprendido",
      "total_gestos": 118,
      "gestos_aprendidos": 20,
      "promedio_progreso": 75.5
    }
  ]
}
```

**Errores:**
- `400` - id_estudiante es requerido
- `403` - No tiene permisos para ver este progreso / No existe una relación activa con este estudiante
- `404` - Estudiante no encontrado

**Implementación Retrofit:**
```kotlin
@GET("obtener_progreso_estudiante_docente.php")
suspend fun getProgresoEstudianteDocente(
    @Header("Authorization") token: String,
    @Query("id_docente") idDocente: Int? = null,
    @Query("id_estudiante") idEstudiante: Int
): Response<ProgresoEstudianteResponse>
```

#### GET /buscar_estudiante.php
Buscar estudiantes por nombre o correo (requiere autenticación)

**Headers:** `Authorization: Bearer {token}`

**Query Parameters:**
- `busqueda` (opcional) - Buscar por nombre o correo (LIKE)
- `correo` (opcional) - Buscar por correo exacto

**Permisos:**
- Solo docentes y administradores pueden buscar estudiantes

**Response (200):**
```json
[
  {
    "id_usuario": 1,
    "nombre": "Juan Pérez",
    "correo": "juan@example.com",
    "rol": "estudiante",
    "fecha_registro": "2025-01-01 00:00:00"
  }
]
```

**Errores:**
- `400` - busqueda o correo es requerido
- `403` - No tiene permisos para buscar estudiantes

**Implementación Retrofit:**
```kotlin
@GET("buscar_estudiante.php")
suspend fun buscarEstudiante(
    @Header("Authorization") token: String,
    @Query("busqueda") busqueda: String? = null,
    @Query("correo") correo: String? = null
): Response<List<UsuarioResponse>>
```

### 12.9 Reportes

#### GET /reporte.php
Generar reporte de progreso (requiere autenticación)

**Headers:** `Authorization: Bearer {token}`

**Query Parameters:**
- `id_usuario` (opcional) - Si no se proporciona, usa el usuario del token
- `formato` (opcional) - "pdf" o "csv" (por defecto: "pdf")

**Permisos:**
- Usuario puede ver su propio reporte
- Docente puede ver reportes de estudiantes vinculados (relación aceptada)
- Administrador puede ver cualquier reporte

**Response (200):**
Si `formato = "csv"`:
- Content-Type: `text/csv; charset=utf-8`
- Content-Disposition: `attachment; filename="reporte_{id_usuario}.csv"`
- Archivo CSV con datos

Si `formato = "pdf"` o no especificado:
```json
{
  "success": true,
  "message": "Reporte generado",
  "data": {
    "usuario": {
      "nombre": "Juan Pérez",
      "correo": "juan@example.com",
      "rol": "estudiante"
    },
    "progresos": [
      {
        "id_gesto": 1,
        "nombre": "Hola",
        "porcentaje": 85,
        "estado": "aprendido"
      }
    ]
  }
}
```

**Errores:**
- `403` - No tiene permisos para ver este reporte / Solo puede ver reportes de sus estudiantes / El estudiante no tiene una relación aceptada con este docente
- `404` - Usuario no encontrado

**Implementación Retrofit:**
```kotlin
@GET("reporte.php")
suspend fun generarReporte(
    @Header("Authorization") token: String,
    @Query("id_usuario") idUsuario: Int? = null,
    @Query("formato") formato: String = "pdf"
): Response<ReporteResponse>
```

### 12.10 Interceptor de Autenticación

**Implementación de Interceptor para Retrofit:**

```kotlin
class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // No agregar token a login y register
        if (originalRequest.url.encodedPath.contains("login.php") ||
            originalRequest.url.encodedPath.contains("register.php")) {
            return chain.proceed(originalRequest)
        }
        
        val token = tokenManager.getToken()
        if (token != null) {
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            return chain.proceed(newRequest)
        }
        
        return chain.proceed(originalRequest)
    }
}

// En ApiClient.kt
val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(AuthInterceptor(tokenManager))
    .addInterceptor(HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    })
    .build()

val retrofit = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .client(okHttpClient)
    .addConverterFactory(GsonConverterFactory.create())
    .build()
```

### 12.11 Manejo de Errores

**Clase para manejar errores de la API:**

```kotlin
object ApiErrorHandler {
    fun handleError(response: Response<*>): String {
        return when (response.code()) {
            400 -> "Solicitud incorrecta: ${response.message()}"
            401 -> "No autenticado. Por favor, inicia sesión nuevamente."
            403 -> "No tienes permisos para realizar esta acción."
            404 -> "Recurso no encontrado."
            409 -> "Conflicto: ${response.message()}"
            500 -> "Error del servidor. Intenta más tarde."
            else -> "Error desconocido: ${response.code()}"
        }
    }
    
    fun parseErrorBody(response: Response<*>): String? {
        return try {
            val errorBody = response.errorBody()?.string()
            val errorJson = JSONObject(errorBody ?: "{}")
            errorJson.getString("message")
        } catch (e: Exception) {
            null
        }
    }
}
```

---

## 13. DEPENDENCIAS Y LIBRERÍAS

### 12.1 build.gradle (Module: app)

```gradle
dependencies {
    // Core Android
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    
    // Navigation
    implementation 'androidx.navigation:navigation-fragment-ktx:2.7.6'
    implementation 'androidx.navigation:navigation-ui-ktx:2.7.6'
    
    // ViewModel y LiveData
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.7.0'
    implementation 'androidx.fragment:fragment-ktx:1.6.2'
    
    // Room
    implementation 'androidx.room:room-runtime:2.6.1'
    implementation 'androidx.room:room-ktx:2.6.1'
    kapt 'androidx.room:room-compiler:2.6.1'
    
    // Retrofit
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'
    
    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3'
    
    // WorkManager
    implementation 'androidx.work:work-runtime-ktx:2.9.0'
    
    // CameraX
    implementation 'androidx.camera:camera-core:1.3.1'
    implementation 'androidx.camera:camera-camera2:1.3.1'
    implementation 'androidx.camera:camera-lifecycle:1.3.1'
    implementation 'androidx.camera:camera-view:1.3.1'
    
    // TFLite
    implementation 'org.tensorflow:tensorflow-lite:2.14.0'
    implementation 'org.tensorflow:tensorflow-lite-support:0.4.4'
    
    // MediaPipe (opcional)
    // implementation 'com.google.mediapipe:solution-core:0.10.0'
    
    // ExoPlayer
    implementation 'com.google.android.exoplayer:exoplayer:2.19.1'
    
    // PDF Generation
    implementation 'com.itextpdf:itextpdf:5.5.13.3'
    
    // Gráficos
    implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
    
    // Dependency Injection (Koin)
    implementation 'io.insert-koin:koin-android:3.5.3'
    implementation 'io.insert-koin:koin-androidx-viewmodel:3.5.3'
    
    // Testing
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}
```

### 12.2 Permisos en AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
    android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" 
    android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<uses-feature android:name="android.hardware.camera" android:required="true" />
<uses-feature android:name="android.hardware.camera.autofocus" android:required="false" />
```

---

## 📝 RESUMEN DE IMPLEMENTACIÓN

### ✅ Funcionalidades Completamente Implementadas
1. Autenticación (Login/Registro)
2. Base de datos local (Room)
3. Sincronización offline-first básica
4. Detección de gestos con TFLite/MediaPipe
5. Práctica de gestos con cámara
6. Progreso de gestos (porcentaje, estado)
7. Solicitudes docente-estudiante básicas
8. Navegación básica
9. Estructura de roles

### ⚠️ Funcionalidades Parcialmente Implementadas (Completar)
1. Dashboard/Home - Agregar logros recientes, notificaciones, indicador conexión
2. Lista de gestos - Agregar dificultad, porcentaje, estado, filtros
3. Logros - Implementar detección automática, notificaciones, detalle
4. Perfil - Agregar edición, cambio contraseña, indicador progreso
5. Reportes - Agregar gráficos, filtros, estadísticas avanzadas
6. Administración - Agregar CRUD usuarios, gestión gestos/logros
7. Docente - Agregar dashboard, visualización progreso estudiantes, alertas

### ❌ Funcionalidades No Implementadas (Crear desde cero)
1. Pantalla de Configuración
2. Control manual de tema Dark/Light
3. Historial de intentos de gestos
4. Detección automática de logros (lógica completa)
5. Pantalla de detalle de logro
6. Sistema de notificaciones
7. Indicador visual de sincronización
8. Gráficos en reportes
9. Filtros avanzados en reportes
10. Alertas de estudiantes rezagados
11. Resolución de conflictos en sincronización

---

## 🎯 PRIORIDADES DE IMPLEMENTACIÓN

### 🔴 PRIORIDAD ALTA (Implementar primero)
1. Detección automática de logros
2. Agregar campos faltantes en lista de gestos
3. Implementar pantalla de configuración
4. Agregar historial de intentos

### 🟡 PRIORIDAD MEDIA
5. Mejorar Dashboard/Home
6. Implementar filtros en lista de gestos
7. Agregar pantalla de detalle de logro
8. Mejorar reportes con gráficos
9. Implementar edición de perfil

### 🟢 PRIORIDAD BAJA
10. Sistema de notificaciones
11. Indicador visual de sincronización
12. Alertas de estudiantes rezagados
13. Estadísticas avanzadas (Administrador)
14. Resolución de conflictos en sincronización

---

**Fin de la Guía de Desarrollo**

Esta guía debe ser seguida al pie de la letra para implementar la aplicación completa según las especificaciones del documento `analisis.md`.
