# Plan de Acción — EcoAdmin Móvil

Checklist de implementación para completar en múltiples sesiones de trabajo.
Marcar con `[x]` cada tarea completada y commitear el progreso.

---

## Fase 1 — Interactividad básica

Objetivo: que el usuario en campo pueda operar traslados sin volver a la web.

- [x] Login con CSRF (POST /login funcional)
- [x] URL de servidor fija en BuildConfig (sin campo manual)
- [ ] Pull-to-refresh en Dashboard
- [ ] Pull-to-refresh en Traslados
- [ ] Pull-to-refresh en Centros
- [ ] Selector de periodo en Dashboard (Hoy / 7d / 30d / Todo)
- [ ] Búsqueda/filtro en listado de Traslados (por estado, texto)
- [ ] Búsqueda/filtro en listado de Centros (por nombre, tipo)
- [ ] Cambiar estado de traslado (tap → bottom sheet con transiciones válidas)
- [ ] Detalle de centro (tap en tarjeta → pantalla de detalle)

---

## Fase 2 — CRUD móvil

Objetivo: crear y editar registros directamente desde la app.

### Traslados
- [ ] Crear traslado (formulario con selección de productor, gestor, residuo, transportista)
- [ ] Editar traslado (long-press → opciones de edición)
- [ ] Eliminar traslado (confirmación antes de borrar)
- [ ] Ver historial de eventos del traslado

### Centros
- [ ] Crear centro (formulario con tipo, NIMA, dirección, contacto)
- [ ] Editar centro (long-press → editar)
- [ ] Eliminar centro (con confirmación)

### Perfil
- [ ] Editar datos personales (nombre, teléfono, DNI, cargo)
- [ ] Cambiar contraseña (modal con contraseña actual + nueva)

---

## Fase 3 — Features avanzados

Objetivo: cerrar la brecha funcional con la web.

### QR Scanner
- [ ] Escanear código QR de traslado (CameraX + ML Kit o ZXing)
- [ ] Confirmar entrada/salida desde QR

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
