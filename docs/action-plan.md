# Plan de Acción — EcoAdmin Móvil

Checklist de implementación para completar en múltiples sesiones de trabajo.
Marcar con `[x]` cada tarea completada y commitear el progreso.

---

## Fase 1 — Interactividad básica

Objetivo: que el usuario en campo pueda operar traslados sin volver a la web.

- [x] Login con CSRF (POST /login funcional)
- [x] URL de servidor fija en BuildConfig (sin campo manual)
- [x] Pull-to-refresh en Dashboard
- [x] Pull-to-refresh en Traslados
- [x] Pull-to-refresh en Centros
- [x] Selector de periodo en Dashboard (Hoy / 7d / 30d / Todo)
- [x] Búsqueda/filtro en listado de Traslados (por estado, texto)
- [x] Búsqueda/filtro en listado de Centros (por nombre, tipo)
- [x] Cambiar estado de traslado (tap → bottom sheet con transiciones válidas)
- [x] Detalle de centro (tap en tarjeta → pantalla de detalle)

---

## Fase 2 — CRUD móvil

Objetivo: crear y editar registros directamente desde la app.

### Traslados
- [x] Crear traslado (formulario con selección de productor, gestor, residuo, transportista)
- [x] Editar traslado (long-press → opciones de edición)
- [x] Eliminar traslado (confirmación antes de borrar)
- [x] Ver historial de eventos del traslado

### Centros
- [x] Crear centro (formulario con tipo, NIMA, dirección, contacto)
- [x] Editar centro (long-press → editar)
- [x] Eliminar centro (con confirmación)

### Perfil
- [x] Editar datos personales (nombre, teléfono, DNI, cargo)
- [x] Cambiar contraseña (modal con contraseña actual + nueva)

---

## Fase 3 — Paridad funcional con la web

Objetivo: cerrar la brecha funcional con la web; que el móvil sea igual de potente.

### QR Scanner (cámara nativa)
- [x] Escanear código QR con CameraX + ML Kit (ya implementado)
- [x] Al detectar un QR con código/ID de traslado, mostrar diálogo de confirmación
- [x] Al confirmar, avanzar el traslado a la etapa final de su workflow (COMPLETADO)
- [x] Feedback visual con vibración y sonido al escanear correctamente

### Dashboard mejorado
- [x] Métricas en LazyRow (scroll horizontal) para mostrar todos los campos de la web
- [x] Sección "Residuos por centro" usando el campo `residuosPorCentro` del DTO
- [x] Actividad reciente (últimos traslados con cambio de estado)

### Detalle de traslado enriquecido
- [x] Mostrar cantidad y unidad del residuo
- [x] Mostrar fechas programadas (inicio/fin)
- [x] Mostrar fecha último cambio de estado
- [x] Mostrar distancia de la ruta (km)
- [x] Mostrar email/rol del transportista

### Acciones rápidas en listado
- [x] Swipe-to-action o long-press en tarjeta de traslado
- [x] Cambiar estado directamente sin entrar al detalle
- [x] Ver historial rápido sin entrar al detalle
- [x] Eliminar con confirmación desde el listado

### Residuos
- [x] Listado de residuos con alertas FIFO
- [x] Detalle de residuo con timeline
- [x] Crear/editar residuo
- [x] Autocompletado código LER

### Documentos
- [x] Listado de documentos con filtro por tipo
- [x] Preview de PDF (WebView o visor externo)
- [x] Crear documento básico

### Rutas
- [x] Listado de rutas
- [x] Mapa con origen/destino (Google Maps o Mapbox)

---

## Fase 4 — Soporte en campo

Objetivo: robustez para uso en zonas con mala conectividad.

- [x] Cache offline con Room para traslados y centros
- [x] Cola de reintentos para operaciones de escritura
- [x] Adjuntos y fotos en traslados
- [x] Notificaciones push (FCM) alineadas con eventos del servidor

---

## Fase 5 — Calidad y release

Objetivo: preparar la app para distribución.

- [x] Tests unitarios para ViewModels y Repositories
- [x] Tests de integración para flujo de login
- [x] UI tests con Compose Testing
- [x] Separación de entornos (debug/staging/release) con buildConfigField
- [x] Firma de release (keystore)
- [x] CI con GitHub Actions (build + lint + test)
- [x] Gradle wrapper commiteado

---

## Notas de sesión

Registrar aquí decisiones o bloqueos encontrados en cada sesión:

| Fecha | Sesión | Notas |
|-------|--------|-------|
| 2026-05-04 | #1 | Login con CSRF implementado. URL fija en BuildConfig. App conecta al backend local. |
| 2026-05-07 | #N | Fase 2 completada: CRUD centros (crear/editar/eliminar con formulario), perfil editable (nombre, tfno, DNI, cargo) y cambio de contraseña con diálogo modal. |
