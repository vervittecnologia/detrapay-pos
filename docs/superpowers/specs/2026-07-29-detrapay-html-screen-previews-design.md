# Detrapay HTML Screen Previews — Design

**Date:** 2026-07-29
**Status:** Approved for specification review

## Goal

Create a standalone HTML prototype that presents every relevant Detrapay screen
before any Android implementation begins. The prototype must reproduce the
Figma visual language at component level while preserving the navigation,
content, states, and business flow currently implemented in the Android app.

## Scope

The preview covers the current primary user journeys:

1. Login.
2. Orders home/list.
3. Order details.
4. Payment amount entry.
5. Payment-method selection.
6. Installment selection.
7. Payment review.
8. Payment processing.
9. Payment result.
10. Installment simulation.

Where the current application exposes them, the gallery also presents loaded,
loading, empty, validation-error, service-error, and success states.

Registration screens remain outside the first preview set unless they are
required to make the login-to-orders journey understandable. The prototype
does not change backend contracts, Android navigation, or business rules.

## Design Direction

The Figma file `Detrapay (Copy)` is the visual source of truth for:

- Typography.
- Color palette.
- Spacing.
- Borders, radii, and shadows.
- Icons and image assets.
- Buttons, fields, cards, selectors, status elements, and navigation controls.
- Component proportions and visual states.

The Android application is the functional source of truth for:

- Screen order and navigation.
- Labels and business content.
- Available actions.
- Loading, empty, error, and success behavior.
- Payment-method and installment rules.
- Back-navigation expectations.

When the Figma flow conflicts with the application flow, the application flow
wins and is rendered using the closest matching Figma components.

## Deliverable

Create an isolated preview site under `.artifacts/detrapay-html-previews/`.
It must not modify Android production or test sources.

The site contains:

- A desktop gallery showing all screens as device-sized cards.
- A focused preview mode at a `390 × 844` Android viewport.
- Flow and state filters.
- Previous, next, and direct screen navigation.
- Clickable primary actions that move through the representative happy path.
- A visible screen name and state label outside each device viewport.
- A component showcase for recurring visual primitives.

The preview may use multiple source files, but it must open from one
`index.html` entry point and work without a backend.

## Component Architecture

Shared HTML/CSS components will cover:

- App shell and system/status area.
- Top app bar.
- Primary and secondary buttons.
- Text fields and amount fields.
- Order and summary cards.
- Payment-method options.
- Installment rows and selection states.
- Status chips and feedback banners.
- Loading, empty, error, processing, and result states.
- Bottom navigation where it exists in the current flow.

Design tokens will be represented as CSS custom properties. Screen markup must
reuse these components and tokens instead of duplicating visual definitions.

## Interaction Model

The prototype is a visual and navigation model, not a backend simulation.
Interactions update local prototype state only.

- Selecting an order opens its detail preview.
- Continuing from detail opens amount entry.
- Amount entry advances to payment-method selection.
- Credit selection can advance to installments.
- Review advances to processing and then result.
- Gallery controls can jump directly to any screen or state.
- Error and empty variants remain directly accessible for review.

No network request, authentication request, payment operation, or persistent
storage is performed.

## Visual Validation

Each screen must be captured at `390 × 844` and checked for:

- Component dimensions and alignment.
- Typography hierarchy.
- Color fidelity.
- Spacing and safe-area behavior.
- Overflow or clipped content.
- Consistency between equivalent components.
- Correct rendering of all listed states.

The final handoff includes the HTML entry point and a generated overview image
or contact sheet so every preview can be reviewed quickly.

## Acceptance Criteria

1. The gallery opens locally from a single entry point.
2. Every in-scope screen and state is directly accessible.
3. The happy-path navigation follows the current Android application.
4. Shared components visually follow the Figma reference.
5. The preview is usable at `390 × 844` without horizontal overflow.
6. No Android production or test file is modified.
7. No backend behavior or unavailable contract is invented.
8. Automated browser checks confirm that the entry point loads, navigation
   works, and every registered preview renders.
9. A screenshot review finds no clipped text, unintended overlaps, or missing
   visual assets.

## Out of Scope

- Implementing or refactoring Android screens.
- Changing backend APIs or payment contracts.
- Running real authentication or payment transactions.
- Persisting prototype data.
- Publishing the preview to production hosting.
- Redesigning flows that do not already exist in the app.
