# EcoAdmin Movile

Android client for the EcoAdmin hazardous-waste transfer platform.

This repository starts the mobile version of the existing backend at `C:/Users/afp5/Git/servidor_api/servidor_api/ServidorApiRest` and is intentionally aligned with the current server contract instead of inventing a separate API.

---

## 📚 Guía de Estudio: Kotlin y Patrones de Diseño

Este proyecto es un ejemplo completo de una app Android moderna. A continuación se documentan los **conceptos de Kotlin** y **patrones de diseño** que aparecen en el código, organizados para facilitar el estudio para examen.

---

### 1. Conceptos Fundamentales de Kotlin

#### 1.1 Tipos Nulables (Null Safety)

Kotlin distingue en tiempo de compilación entre tipos que pueden ser `null` y los que no:

```kotlin
val nombre: String = "Hola"      // NUNCA puede ser null
val telefono: String? = null     // PUEDE ser null (nullable type)
```

**Operadores clave:**
| Operador | Nombre | Ejemplo | Qué hace |
|----------|--------|---------|----------|
| `?.` | Safe call | `user?.nombre` | Retorna null si user es null, en vez de NPE |
| `?:` | Elvis | `x ?: "default"` | Si x es null, usa el valor de la derecha |
| `!!` | Non-null assertion | `x!!` | Fuerza no-null; lanza NPE si es null |
| `?.let { }` | Safe call + scope | `cookie?.let { save(it) }` | Ejecuta bloque solo si no es null |

**Dónde verlo:** `SessionCookieJar.kt`, `ApiModels.kt`, `ProfileScreen.kt`

#### 1.2 Data Classes

```kotlin
data class LoginUiState(
    val email: String = "",
    val isLoading: Boolean = false
)
```

Auto-generan: `equals()`, `hashCode()`, `toString()`, `copy()`, y `componentN()`.

- **`copy()`** permite crear copias con campos modificados (inmutabilidad):
  ```kotlin
  val nuevo = estado.copy(isLoading = true)  // Solo cambia isLoading
  ```
- **Valores por defecto** evitan constructores telescópicos.

**Dónde verlo:** `ApiModels.kt`, `AppViewModel.kt`, todos los `*UiState`

#### 1.3 Sealed Classes / Sealed Interfaces

```kotlin
sealed interface TransferFormField {
    data class Productor(val id: Long?) : TransferFormField
    data class Gestor(val id: Long?) : TransferFormField
    data class Observaciones(val text: String) : TransferFormField
}
```

- Jerarquía **cerrada**: el compilador conoce todos los subtipos.
- El `when` se vuelve **exhaustivo** (obliga a cubrir todos los casos).
- Ideal para: eventos de UI, estados, acciones de navegación.

**Dónde verlo:** `TransferFormScreen.kt` → `TransferFormField`

#### 1.4 Companion Object (Equivalente a `static` en Java)

```kotlin
class AppPreferences(context: Context) {
    companion object {
        private const val PREFERENCES_NAME = "ecoadmin_preferences"
        const val SESSION_COOKIE_NAME = "JSESSIONID"
    }
}
```

- `companion object` contiene miembros "estáticos" de la clase.
- `const val` = constante en tiempo de compilación (solo tipos primitivos y String).
- Se accede como `AppPreferences.SESSION_COOKIE_NAME`.

**Dónde verlo:** `AppPreferences.kt`, `ServerUrlInterceptor.kt`, `TransferDetailViewModel.kt`

#### 1.5 Object Declaration (Singleton)

```kotlin
object NetworkModule {
    fun provideOkHttpClient(...): OkHttpClient { ... }
}
```

- `object` crea una instancia única (Singleton) garantizada por el lenguaje.
- Se usa para módulos DI, utilidades, y objetos sin estado.

**Dónde verlo:** `NetworkModule.kt`, `DataModule.kt`, `DatabaseModule.kt`

#### 1.6 Extension Functions

```kotlin
// Función que "extiende" Response sin modificar su código fuente
private fun Response<*>.isUnauthorizedRedirect(): Boolean {
    return code() in 300..399 && headers()["Location"]?.contains("/login") == true
}
```

- Añaden métodos a clases existentes sin herencia.
- `String.toHttpUrl()`, `.orEmpty()`, `.isNullOrBlank()` son extensiones de la stdlib.

**Dónde verlo:** `EcoAdminRepositories.kt`, uso de `.orEmpty()` en toda la UI

#### 1.7 Higher-Order Functions (Funciones de Orden Superior)

```kotlin
// Función que recibe OTRA función como parámetro
private suspend fun <T> safeApiCall(request: suspend () -> Response<T>): Result<T>

// Uso: safeApiCall { api.getTraslados() }
```

- `() -> Unit`: función sin parámetros que no retorna nada.
- `(String) -> Unit`: función que recibe un String.
- Permiten callbacks, estrategias, y DSLs.

**Dónde verlo:** `EcoAdminRepositories.kt`, todos los `onClick`, `onRefresh`, etc.

#### 1.8 Scope Functions

| Función | Referencia | Retorno | Uso típico |
|---------|-----------|---------|------------|
| `let` | `it` | Resultado del lambda | Transformar nullable |
| `apply` | `this` | El mismo objeto | Configurar builders |
| `also` | `it` | El mismo objeto | Side effects |
| `run` | `this` | Resultado del lambda | Ejecutar bloque con contexto |
| `with` | `this` | Resultado del lambda | Múltiples llamadas al mismo objeto |

```kotlin
// apply: configura el builder y retorna el objeto
val client = OkHttpClient.Builder().apply {
    followRedirects(false)
    cookieJar(cookieJar)
}.build()
```

**Dónde verlo:** `NetworkModule.kt` (apply), `SessionCookieJar.kt` (let)

#### 1.9 When Expression

```kotlin
// when como expresión (retorna un valor)
val color = when (estado.uppercase()) {
    "PENDIENTE" -> Color(0xFFF59E0B)
    "EN_TRANSITO" -> Color(0xFF3B82F6)
    else -> Color(0xFF64748B)
}

// when sin argumento (reemplaza if-elseif)
when {
    appState.isLoading -> LoadingScreen()
    !appState.isAuthenticated -> LoginScreen(...)
    else -> MainContent()
}
```

**Dónde verlo:** `EcoAdminApp.kt`, `TransferDetailScreen.kt`, `EcoComponents.kt`

#### 1.10 Coroutines y suspend

```kotlin
// suspend marca una función que puede pausarse sin bloquear el hilo
suspend fun login(email: String, password: String): Result<Unit>

// Se lanza en un scope con ciclo de vida
viewModelScope.launch {
    val result = repository.loadTransfers()  // Se suspende aquí sin bloquear UI
    _uiState.update { it.copy(transfers = result) }
}
```

- `suspend` = función que puede suspenderse (IO, red, etc.).
- `viewModelScope.launch {}` = lanza coroutine ligada al ViewModel.
- `Flow<T>` = stream de datos reactivo (como un Observable).

**Dónde verlo:** Todos los repositorios y ViewModels

#### 1.11 Delegated Properties (by)

```kotlin
// `by` delega el getter/setter a otro objeto
val appState by appViewModel.uiState.collectAsStateWithLifecycle()

// En Compose:
var expanded by remember { mutableStateOf(false) }
```

- `by` evita código boilerplate de get/set.
- `remember { }` preserva el estado entre recomposiciones.

**Dónde verlo:** `EcoAdminApp.kt`, `TransferFormScreen.kt`

#### 1.12 Enum Classes con Propiedades

```kotlin
enum class DashboardPeriod(val label: String, val daysBack: Int?) {
    TODAY("Hoy", 0),
    WEEK("7 días", 7),
    MONTH("30 días", 30),
    ALL("Todo", null)
}
```

- Cada entrada del enum puede tener propiedades con valor.
- `.entries` (Kotlin 1.9+) lista todas las entradas.

**Dónde verlo:** `DashboardFeature.kt`

#### 1.13 Generic Functions

```kotlin
@Composable
private fun <T> DropdownSelector(
    items: List<T>,
    displayText: (T) -> String,  // Función que convierte T en texto
    ...
) where T : Any {                // Constraint: T no puede ser nullable
```

- `<T>` = tipo genérico.
- `where T : Any` = restricción (T debe ser no-null).
- Permite crear componentes reutilizables.

**Dónde verlo:** `TransferFormScreen.kt`, `safeApiCall` en `EcoAdminRepositories.kt`

---

### 2. Patrones de Diseño en el Proyecto

#### 2.1 MVVM (Model-View-ViewModel)

```
┌──────────────────────────────────────────────────────┐
│  VIEW (Composable)                                    │
│  - Observa uiState via collectAsStateWithLifecycle()  │
│  - Llama funciones del ViewModel en eventos de UI     │
├──────────────────────────────────────────────────────┤
│  VIEWMODEL                                            │
│  - Expone StateFlow<UiState> (solo lectura)           │
│  - Contiene lógica de negocio y transformaciones      │
│  - Llama al Repository para datos                     │
├──────────────────────────────────────────────────────┤
│  MODEL (Repository + API + Database)                  │
│  - Abstrae la fuente de datos (red, local)            │
│  - Retorna Result<T> o Flow<T>                        │
└──────────────────────────────────────────────────────┘
```

**Archivos:** `AppViewModel.kt`, `DashboardFeature.kt`, `TransfersViewModels.kt`

#### 2.2 Repository Pattern

```kotlin
class TransfersRepository(private val api: EcoAdminApi) {
    suspend fun loadTransfers(): Result<List<TrasladoDto>> = safeApiCall { api.getTraslados() }
}
```

- Abstrae la fuente de datos del ViewModel.
- Mañana puedes cambiar Retrofit por GraphQL sin tocar la UI.
- El ViewModel NO sabe si los datos vienen de red, caché, o base de datos.

**Archivo:** `EcoAdminRepositories.kt`

#### 2.3 Dependency Injection (Hilt/Dagger)

```
@HiltAndroidApp        → Inicializa el contenedor DI
@AndroidEntryPoint     → Habilita inyección en Activity
@HiltViewModel         → Permite inyectar dependencias en ViewModel
@Module @InstallIn     → Define cómo crear dependencias
@Provides @Singleton   → Método factory que crea UNA instancia
@Inject constructor    → Pide dependencias automáticamente
```

**Ventaja:** No usas `new` manualmente. Hilt resuelve todo el grafo de dependencias.

**Archivos:** `NetworkModule.kt`, `DataModule.kt`, `DatabaseModule.kt`

#### 2.4 Observer Pattern (StateFlow)

```kotlin
// Publisher (ViewModel)
private val _uiState = MutableStateFlow(DashboardUiState())
val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

// Subscriber (Composable)
val state by viewModel.uiState.collectAsStateWithLifecycle()
```

- El Composable se re-renderiza automáticamente cuando cambia el estado.
- `asStateFlow()` expone solo lectura (encapsulación).

#### 2.5 Singleton Pattern

Implementado de dos formas:
1. **`object` de Kotlin:** `object NetworkModule` → instancia única por lenguaje.
2. **`@Singleton` de Hilt:** garantiza una sola instancia en el contenedor DI.

#### 2.6 Interceptor / Chain of Responsibility

```kotlin
class ServerUrlInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val rewrittenRequest = ...
        return chain.proceed(rewrittenRequest)  // Pasa al siguiente en la cadena
    }
}
```

- Cada interceptor puede modificar request/response.
- `chain.proceed()` delega al siguiente interceptor.
- OkHttp los ejecuta en orden.

**Archivo:** `ServerUrlInterceptor.kt`

#### 2.7 Builder Pattern

```kotlin
OkHttpClient.Builder()
    .followRedirects(false)
    .cookieJar(cookieJar)
    .addInterceptor(serverUrlInterceptor)
    .build()
```

- Construye objetos complejos paso a paso.
- `.build()` produce el objeto final inmutable.
- En Compose, `Modifier` usa una variante (method chaining).

#### 2.8 State Machine (Máquina de Estados)

```kotlin
fun nextStates(currentStatus: String): List<String> = when (currentStatus.uppercase()) {
    "PENDIENTE" -> listOf("EN_TRANSITO")
    "EN_TRANSITO" -> listOf("ENTREGADO")
    "ENTREGADO" -> listOf("COMPLETADO")
    else -> emptyList()
}
```

- Define transiciones válidas entre estados.
- Garantiza que un traslado no puede saltar de PENDIENTE a COMPLETADO.

**Archivo:** `TransfersViewModels.kt`

#### 2.9 Slot API Pattern (Compose)

```kotlin
@Composable
fun EcoAdminTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colorScheme,
        content = content  // "Slot": el caller decide qué va dentro
    )
}
```

- El componente padre define la estructura.
- El caller inyecta contenido via trailing lambda.
- Equivalente al `<ng-content>` de Angular o `{children}` de React.

#### 2.10 Unidirectional Data Flow (UDF)

```
UI Event (click) → ViewModel función → Repository → Actualiza StateFlow → UI se recompone
```

- Los datos fluyen en UNA dirección.
- La UI nunca modifica el estado directamente.
- Los eventos suben (↑), el estado baja (↓).

---

### 3. Jetpack Compose — Conceptos Clave para Examen

| Concepto | Qué es | Ejemplo en el proyecto |
|----------|--------|----------------------|
| `@Composable` | Marca funciones que describen UI | Todas las pantallas |
| `remember {}` | Preserva estado entre recomposiciones | `TransferFormScreen.kt` |
| `LaunchedEffect(key)` | Efecto secundario que se re-ejecuta si key cambia | `EcoAdminApp.kt` |
| `Modifier` | Cadena de decoradores para layout/estilo | Todas las pantallas |
| `State hoisting` | Elevar estado al padre para control | `LoginScreen` recibe state + callbacks |
| `NavHost` + `composable()` | Navegación declarativa | `EcoAdminApp.kt` |
| `collectAsStateWithLifecycle()` | Convierte Flow→State respetando ciclo de vida | Todos los ViewModels |
| `PullToRefreshBox` | Pull-to-refresh nativo Material 3 | `DashboardFeature.kt` |
| `HorizontalPager` | Páginas deslizables | `TransfersKanbanView.kt` |
| `Scaffold` | Estructura base (topBar, bottomBar, FAB, content) | `TransfersListScreen.kt` |

---

### 4. Arquitectura del Proyecto

```text
app/src/main/java/com/ecoadminmovile/
├── EcoAdminApplication.kt   ← Punto entrada Hilt (@HiltAndroidApp)
├── MainActivity.kt          ← Single Activity (@AndroidEntryPoint)
├── EcoAdminApp.kt           ← Composable raíz (navegación completa)
├── app/
│   └── AppViewModel.kt      ← Estado global de autenticación
├── core/
│   ├── di/                   ← Módulos Hilt (NetworkModule, DataModule, DatabaseModule)
│   ├── database/             ← Room (Entities, DAOs, Database)
│   ├── model/                ← DTOs (data classes para API)
│   ├── network/              ← Retrofit API + Interceptors + CookieJar
│   └── preferences/          ← SharedPreferences wrapper
├── data/
│   └── EcoAdminRepositories.kt ← Capa Repository (abstrae API)
├── feature/
│   ├── auth/                 ← Login (ViewModel + Screen)
│   ├── dashboard/            ← Panel métricas (ViewModel + Screen)
│   ├── transfers/            ← Traslados (ViewModels + múltiples Screens)
│   ├── centers/              ← Centros (ViewModel + Screens)
│   └── profile/              ← Perfil (Screen sin ViewModel)
└── ui/
    ├── components/           ← Componentes reutilizables (EcoCard, EcoStatusPill)
    └── theme/                ← Colores, tipografía, tema Material 3
```

**Principio clave:** Organización por **feature** (no por tipo). Cada carpeta feature es autosuficiente.

---

### 5. Resumen Rápido para Examen

| Pregunta frecuente | Respuesta |
|-------------------|-----------|
| ¿Diferencia `val` vs `var`? | `val` = inmutable (como `final`), `var` = mutable |
| ¿Qué genera `data class`? | `equals()`, `hashCode()`, `toString()`, `copy()`, `componentN()` |
| ¿Qué es `sealed`? | Jerarquía cerrada → `when` exhaustivo |
| ¿Qué es `companion object`? | Contenedor de miembros "estáticos" |
| ¿Qué es `suspend`? | Función que puede pausarse sin bloquear el hilo |
| ¿Qué es `Flow`? | Stream reactivo de datos (como Observable) |
| ¿Qué es `StateFlow`? | Flow con valor actual (hot stream) para UI |
| ¿Qué es `by`? | Property delegation (delega get/set) |
| ¿Qué hace `?.let { }`? | Ejecuta bloque solo si no es null |
| ¿Qué hace `.copy()`? | Crea copia con campos modificados (inmutabilidad) |
| ¿Qué es `@Composable`? | Función que declara UI (no retorna View) |
| ¿Qué es Hilt? | Framework DI que genera código en compilación |
| ¿Qué patrón usa ViewModel+StateFlow? | MVVM + Observer |
| ¿Qué es UDF? | Unidirectional Data Flow: eventos ↑, estado ↓ |

---

## Current Scope

This first mobile slice includes:

- Session-based sign-in against the existing Spring Security form login.
- Runtime-configurable backend URL, with `http://10.0.2.2:8080/` as the emulator default.
- Dashboard metrics from `/api/estadisticas`.
- Transfers list, detail, form, kanban view, and QR scanner.
- Centers list and detail from `/api/centros`.
- Authenticated profile snapshot from `/api/perfil`.

## Technical Direction

The app follows a pragmatic Kotlin Android setup:

- Kotlin + Jetpack Compose + Material 3.
- ViewModel-driven UI state (MVVM pattern).
- Repository layer over Retrofit + OkHttp.
- Room database for offline caching.
- Hilt for dependency injection.
- Session persistence through a dedicated `JSESSIONID` cookie jar.
- Runtime host switching through an interceptor, so the base server URL can be changed from the login screen without rebuilding the app.

This is intentionally a thin mobile client over the existing backend rules.

## Backend Integration Notes

- Authentication is form login, not JWT.
- The mobile client posts to `/login` with `application/x-www-form-urlencoded` fields.
- The backend uses `JSESSIONID`, so the app persists the session cookie locally.
- `/api/**` is already exempt from CSRF in the backend security config, which makes the current session approach viable for Android.
- CORS is not a blocker for the native app. It only matters for browser clients.

## Local Setup

1. Open the project in Android Studio.
2. Use JDK 21 for Android Gradle builds.
3. Start the backend from `ServidorApiRest` on port `8080`.
4. Run the Android emulator or connect a device.
5. Sign in with the same credentials you use in the web application.

If you run on a physical device, replace the default backend URL with the LAN IP of the machine hosting the Spring Boot server.

## What Is Not Implemented Yet

- Offline-first sync with Room.
- Attachments, photos upload.
- Push notifications.
- Automated tests.

See [docs/mobile-roadmap.md](docs/mobile-roadmap.md) for the phase mapping.
