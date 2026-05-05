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
- [ ] Crear centro (formulario con tipo, NIMA, dirección, contacto)
- [ ] Editar centro (long-press → editar)
- [ ] Eliminar centro (con confirmación)

### Perfil
- [ ] Editar datos personales (nombre, teléfono, DNI, cargo)
- [ ] Cambiar contraseña (modal con contraseña actual + nueva)

---

## Fase 3 — Paridad funcional con la web

Objetivo: cerrar la brecha funcional con la web; que el móvil sea igual de potente.

### QR Scanner (cámara nativa)
- [x] Escanear código QR con CameraX + ML Kit (ya implementado)
- [ ] Al detectar un QR con código/ID de traslado, mostrar diálogo de confirmación
- [ ] Al confirmar, avanzar el traslado a la etapa final de su workflow (COMPLETADO)
- [ ] Feedback visual con vibración y sonido al escanear correctamente

### Dashboard mejorado
- [ ] Métricas en LazyRow (scroll horizontal) para mostrar todos los campos de la web
- [ ] Sección "Residuos por centro" usando el campo `residuosPorCentro` del DTO
- [ ] Actividad reciente (últimos traslados con cambio de estado)

### Detalle de traslado enriquecido
- [ ] Mostrar cantidad y unidad del residuo
- [ ] Mostrar fechas programadas (inicio/fin)
- [ ] Mostrar fecha último cambio de estado
- [ ] Mostrar distancia de la ruta (km)
- [ ] Mostrar email/rol del transportista

### Acciones rápidas en listado
- [ ] Swipe-to-action o long-press en tarjeta de traslado
- [ ] Cambiar estado directamente sin entrar al detalle
- [ ] Ver historial rápido sin entrar al detalle
- [ ] Eliminar con confirmación desde el listado

### Residuos
- [ ] Listado de residuos con alertas FIFO
- [ ] Detalle de residuo con timeline
- [ ] Crear/editar residuo
- [ ] Autocompletado código LER

### Documentos
- [ ] Listado de documentos con filtro por tipo
- [ ] Preview de PDF (WebView o visor externo)
- [ ] Crear documento básico

### Rutas
- [ ] Listado de rutas
- [ ] Mapa con origen/destino (Google Maps o Mapbox)

---

## Fase 4 — Soporte en campo

Objetivo: robustez para uso en zonas con mala conectividad.

- [ ] Cache offline con Room para traslados y centros
- [ ] Cola de reintentos para operaciones de escritura
- [ ] Adjuntos y fotos en traslados
- [ ] Notificaciones push (FCM) alineadas con eventos del servidor

---

## Fase 5 — Calidad y release

Objetivo: preparar la app para distribución.

- [ ] Tests unitarios para ViewModels y Repositories
- [ ] Tests de integración para flujo de login
- [ ] UI tests con Compose Testing
- [ ] Separación de entornos (debug/staging/release) con buildConfigField
- [ ] Firma de release (keystore)
- [ ] CI con GitHub Actions (build + lint + test)
- [ ] Gradle wrapper commiteado

---

## Notas de sesión

Registrar aquí decisiones o bloqueos encontrados en cada sesión:

| Fecha | Sesión | Notas |
|-------|--------|-------|
| 2026-05-04 | #1 | Login con CSRF implementado. URL fija en BuildConfig. App conecta al backend local. |
