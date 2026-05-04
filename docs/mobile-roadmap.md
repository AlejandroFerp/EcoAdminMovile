# Mobile Roadmap

This roadmap translates the backend project plan into a mobile delivery sequence.

## Phase M1: Operational Foundation

Status: implemented in this repository.

Scope:

- Session login over the existing backend.
- Backend URL configuration from the app.
- Dashboard metrics.
- Transfers list and basic detail.
- Centers list.
- Profile screen with logout.

Backend plan alignment:

- Phase 1: security baseline.
- Phase 3: REST API availability.
- Phase 10: expanded data model.

## Phase M2: Workflow Depth

Recommended next.

Scope:

- Transfer filtering by state.
- Transfer history timeline.
- Residues list.
- Role-aware empty states and messaging.
- Basic create/update operations where the backend contract is stable enough for mobile.

Backend plan alignment:

- Phase 12: table UX becomes list filtering and batch-aware mobile actions.
- Phase 13: detail-first flows.
- Phase 16: Swagger-complete contract usage.

## Phase M3: Documents and Geo Flows

Scope:

- QR scanning and transfer lookup.
- PDF access and sharing.
- Map-based center and route views.
- Route-aware transport flows.

Backend plan alignment:

- Phase 14: addresses and maps.
- Phase 15: PDF generation.
- Phase 16: QR extras.

## Phase M4: Field Work Support

Scope:

- Attachments and photos.
- Better offline behavior for field teams.
- Resilient retry queues for weak connectivity.
- Notifications aligned with server-side events.

Backend plan alignment:

- Phase 13.4: photos and attachments.
- Phase 16.1: email/event workflows as the basis for mobile notifications.

## Phase M5: Delivery Hardening

Scope:

- UI tests and API contract tests.
- Gradle wrapper generation.
- CI validation.
- Release signing and environment separation.

This phase should start after the functional slices above are stable enough to freeze their contracts.
