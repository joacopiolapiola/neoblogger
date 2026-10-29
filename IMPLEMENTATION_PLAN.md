# Neoblogger — Implementation Plan

- **Document ID:** NB-PLAN-001
- **Status:** Proposed phased plan
- **Companion specification:** `PRODUCT_SPEC.md`
- **Development agent:** GitHub Copilot
- **Target:** Android APK, Kotlin, Jetpack Compose, min SDK 26

This plan is a dependency-aware implementation sequence, not a claim that these stages have been completed. A stage is complete only after its exit criteria are demonstrated and recorded.

## Delivery strategy

Build a sequence of small, working vertical slices. Prove the difficult visual/source synchronization design early, while also establishing a functional project/file foundation. Avoid building every screen in parallel before there is a working website end-to-end.

## Implementation status (2026-10-17)

- Initial M1 source has been added: Compose navigation shell, private local website projects, basic source editing/saving, and path-confined WebView resource serving.
- M0/M1 are not complete: the app has not been built or run on a device, the wrapper is not generated, and the editor/Emmet and visual/source technical spikes remain unverified.
- Unit tests cover starter project creation, persistence, naming validation, and restricted file paths. They still need to be executed in an Android/Gradle environment.
- Design, project import/export, and ZIP support remain future work; no placeholder UI is represented as complete functionality.

### Milestone overview

| Milestone | Goal | Release gate |
|---|---|---|
| M0 | Repository/build foundation and technical spikes | Reliable debug build; feasibility findings recorded |
| M1 | Native app shell and local project lifecycle | Create/open/save a simple site offline |
| M2 | Real interactive Preview | Website page works with links/forms/dropdowns/JS in WebView |
| M3 | Code editor with Emmet | Edit real files on phone, save, preview manually |
| M4 | Visual canvas foundation | Select and edit a supported element without corrupting source |
| M5 | Bidirectional synchronization | Source and Design changes stay consistent for supported constructs |
| M6 | Import/export and asset management hardening | Ordinary folder/ZIP round-trip succeeds safely |
| M7 | Reliability, accessibility, security and one-time ad | Test suite + APK build + documented limitations |
| M8 | Release packaging | Signed release artifact can be installed and tested; store readiness only if selected |

---

## M0 — Repository, build foundation, and feasibility spikes

### Tasks

- [ ] Create the Android project with Kotlin, Jetpack Compose, Gradle wrapper, min SDK API 26, and a stable dependency configuration selected at implementation time.
- [ ] Add `PRODUCT_SPEC.md`, `AGENTS.md`, and `IMPLEMENTATION_PLAN.md` at repository root.
- [ ] Create a basic build/run setup guide, app ID placeholder and package structure; treat final application ID/icon/signing ownership as TBD.
- [ ] Establish unit-test and Android instrumentation-test infrastructure.
- [ ] Create a minimal Compose shell with the exact English labels `Design`, `Code`, and `Preview`.
- [ ] Add a short dependency/license review for WebView support libraries, CodeMirror, Emmet, HTML/CSS parsing tools and any ZIP library.
- [ ] Record a reference Android phone/emulator and a reproducible sample site for performance/functional testing.

### Required technical spike: visual/source bridge

- [ ] Render sample HTML/CSS in an isolated Design canvas.
- [ ] Select one element and return a stable element locator/identity.
- [ ] Update one CSS property from the inspector and write the corresponding source change.
- [ ] Change a stylesheet directly and refresh the canvas appearance.
- [ ] Verify that comments, custom attributes, an unrecognized element and a media query survive round-trip.
- [ ] Test canvas touch latency on a representative device.

### Required technical spike: Preview and resource loading

- [ ] Load a managed local project with multiple HTML pages and relative resources.
- [ ] Validate WebViewAssetLoader/secure local URL mapping for project pages and assets.
- [ ] Test internal navigation, forms, dropdowns, JavaScript and Back/Forward.
- [ ] Verify that arbitrary Preview JavaScript has no native bridge and cannot read arbitrary local files.

### Exit criteria

- Project builds and starts on min API 26 or the supported emulator configuration.
- Architecture decision record identifies tested components and limitations.
- No critical assumption about visual/source sync or local Preview remains untested.

---

## M1 — Native app shell and local project lifecycle

### Tasks

- [ ] Implement Home/Projects screen with `New Project`, `Open Project`, and `Import ZIP` entry points (import can be visibly disabled until M6 but must be planned).
- [ ] Implement dark theme and English Android string resources.
- [ ] Implement project metadata model and local repository.
- [ ] Create project folder plus minimal `index.html` and linked stylesheet.
- [ ] Implement project names/path validation.
- [ ] Implement the initial managed local workspace.
- [ ] Implement file reads/writes, save status and safe write behavior.
- [ ] Display actionable error state for failed writes.
- [ ] Add basic recent-project list and reopen behavior.

### Tests

- Project creation generates readable and valid files.
- Names that contain invalid path characters or traversal patterns are handled safely.
- Create/save/close/reopen preserves changes.
- A failed write does not leave the UI falsely displaying `Saved`.
- Project can be created and edited in airplane mode.

### Exit criteria

User can create, edit through a basic text path, save, close and reopen a local site with no internet connection.

---

## M2 — Interactive Preview

### Tasks

- [ ] Build a separate Preview runtime/WebView from the trusted editor UI.
- [ ] Load local HTML pages and resolve relative paths for CSS, JS, images, fonts and SVG within the managed project.
- [ ] Add `Back`, `Forward`, and `Reload` controls.
- [ ] Implement project-internal link routing and page history.
- [ ] Verify form input, built-in validation, select menus, checkboxes, radio buttons and JavaScript controls.
- [ ] Add external navigation confirmation.
- [ ] Add external form-submission warning/confirmation according to the strict form policy.
- [ ] Display which project revision is being previewed and warn if newer edits are not saved/applied.
- [ ] Add preview error UI for missing pages/resources and unsupported external dependencies.
- [ ] Add tests with a local deterministic sample site; do not depend on remote sites for core tests.

### Security gates

- [ ] No unrestricted native bridge is visible to Preview content.
- [ ] File-origin settings are explicitly restricted.
- [ ] Arbitrary imported page navigation cannot take over the app without confirmation.
- [ ] External requests and remote dependencies are documented and tested as allowed/blocked according to the selected policy.

### Exit criteria

The sample site behaves like an interactive static website, not a screenshot. Design editing overlays are not present in Preview.

---

## M3 — Code editor and Emmet

### Tasks

- [ ] Add local/bundled CodeMirror 6 resources or another documented editor engine selected after the spike.
- [ ] Implement open-file tabs and project file explorer entry.
- [ ] Add HTML, CSS, JavaScript, JSON, SVG, Markdown and plain-text mode selection.
- [ ] Configure syntax highlighting, line numbers, undo/redo, find/replace and formatting/indentation where supported.
- [ ] Integrate Emmet for HTML/CSS, bundled locally.
- [ ] Add a visible `Expand Emmet` action suitable for touchscreen use.
- [ ] Test software keyboard, cursor positioning, selection, paste, undo and orientation changes on Android.
- [ ] Connect edits to the project file buffer and save system.
- [ ] Ensure no runtime CDN is necessary for editing.

### Tests

- Edits are written to actual project files/buffers.
- Emmet expansion generates expected HTML/CSS on the device.
- App works offline.
- Closing/reopening does not lose saved source.
- Large-ish test source files do not block the Compose main thread during normal typing.

### Exit criteria

A user can edit `index.html` and CSS manually, expand Emmet, save and reload Preview.

---

## M4 — Visual canvas foundation

### Tasks

- [ ] Implement the Design WebView/runtime with a trusted editor shell and a project rendering surface isolated from project scripts.
- [ ] Parse HTML/CSS using standards-aware libraries/representations selected in M0.
- [ ] Provide canvas viewport, pan/zoom, and selection overlays.
- [ ] Implement one-tap element selection and contextual toolbar.
- [ ] Build a small but useful first element palette: heading, paragraph, image, link, button, container, list and section.
- [ ] Implement inspector fields for text, href/src, alt text, ID/class, width/height, margin/padding, typography and colors.
- [ ] Implement add, duplicate, delete and move operations.
- [ ] Implement responsive/web-flow and free-positioning modes with explicit containing block for free positioning.
- [ ] Add an element tree synced with canvas selection.
- [ ] Show unsupported nodes/rules rather than omitting them.

### Performance targets

- Measure selection latency, drag latency, parsing time and memory on a documented device.
- Keep all expensive parse/asset operations off main thread.
- Avoid rebuilding the entire page model on every pointer move.
- Establish an actual benchmark baseline before claiming 60 fps or other performance targets.

### Tests

- Selecting an element identifies the correct HTML node.
- Editing text/attributes/styles persists in project sources.
- A selected element can be moved without unexpectedly moving unrelated nodes.
- Free-positioning coordinates are relative to the documented container.
- Responsive mode uses normal layout properties where appropriate.

### Exit criteria

The user can visually edit a sample static page and the project remains valid, readable HTML/CSS.

---

## M5 — Bidirectional synchronization and conversion safety

### Tasks

- [ ] Add versioned source buffers and synchronization coordinator.
- [ ] Implement background parsing and stale-result rejection.
- [ ] Update Design rendering after supported source edits.
- [ ] Update source after supported visual edits using targeted source changes where feasible.
- [ ] Preserve unknown attributes, comments, custom nodes, scripts and CSS rules.
- [ ] Display parse diagnostics without replacing user source.
- [ ] Add conversion report and preview/diff workflow for imported structures requiring normalization.
- [ ] Keep a recoverable original snapshot before applying a conversion.
- [ ] Add conflict states for simultaneous or overlapping changes.
- [ ] Implement the selected first-stage undo/redo for common visual operations and validate interaction with source editor undo.

### Round-trip test matrix

- HTML comments and whitespace.
- Custom/data attributes and unknown elements.
- Inline and external CSS.
- Nested Flexbox/Grid.
- Media queries.
- Absolute/fixed/sticky positioning.
- CSS variables.
- Pseudo-elements and pseudo-classes.
- Inline event handlers/scripts and external JS references.
- SVG and images.

Unsupported constructs may be code-only; they must not be silently deleted or rewritten. Record known limitations clearly.

### Exit criteria

Bidirectional updates work for a documented supported subset and tests demonstrate source preservation outside that subset.

---

## M6 — Import, export, file explorer and assets

### Tasks

- [ ] Add SAF file and directory picking.
- [ ] Import folder into managed workspace with hierarchy preserved.
- [ ] Import ZIP with protection against Zip Slip/path traversal, symlinks and duplicate-path attacks.
- [ ] Detect entry page and ask user if ambiguous.
- [ ] Generate diagnostics for missing resources and unsupported build-based projects.
- [ ] Add conversion review and original snapshot retention to import flow.
- [ ] Add project explorer and file tab operations.
- [ ] Add export to folder and ZIP with overwrite confirmation.
- [ ] Implement asset manager baseline: browse assets, preview supported assets, rename/move, update relative references when managed operations are safe.
- [ ] Add optional image optimization and unused-resource report only after safe originals/recovery behavior exists.
- [ ] Add import/export cancellation and progress reporting for longer operations.

### Tests

- Import and export a multi-page website with nested folders and relative assets.
- Round-trip ZIP includes required ordinary website files.
- ZIP entries cannot write outside the workspace.
- Invalid ZIP/import fails without corrupting an existing project.
- Exported site opens outside Neoblogger.
- Overwrite prompts and cancel behavior work.

### Exit criteria

Neoblogger can create new sites and work on ordinary existing static website directories/archives safely.

---

## M7 — Quality, performance, accessibility, security and ads

### Tasks

- [ ] Add unit tests for file/path utilities, model operations, import/export and one-time-ad state machine.
- [ ] Add Android integration tests for app navigation, project lifecycle and WebView behavior.
- [ ] Add accessibility labels, touch-target review, focus order, contrast and scalable text checks.
- [ ] Run the security checklist for WebView settings, navigation, archives, external requests and logging.
- [ ] Run device tests on min API 26 and a current Android version where available.
- [ ] Test low-memory, process recreation, long filenames, Unicode names and unavailable SAF provider behaviors.
- [ ] Measure canvas performance on representative sample pages and record device details/results.
- [ ] Add a one-time launch-ad interface and state machine with ads disabled by default in debug/test builds.
- [ ] Select an ad provider only after policy, SDK, test mode, consent and privacy disclosures are confirmed.
- [ ] Ensure the ad cannot block startup and cannot display again after shown/skipped state.
- [ ] Provide a build variant/configuration that functions with no ad SDK/configuration.

### Release blockers

- Data loss or silent source corruption.
- Arbitrary imported Preview JS reaching native app methods/private files.
- Unsafe archive extraction.
- Visual edits pretending to work without updating source.
- Preview forms/interactions that are non-functional despite being presented as supported.
- Ads recurring or blocking users when unavailable.
- Build cannot be reproduced from documented setup.

### Exit criteria

P0 acceptance criteria in `PRODUCT_SPEC.md` pass or each remaining limitation is expressly identified and approved before release.

---

## M8 — APK packaging and handoff

### Tasks

- [ ] Create debug APK for device testing.
- [ ] Create a release build configuration with application ID and versioning once approved.
- [ ] Document signing-key creation and safe storage; never commit signing secrets.
- [ ] Produce install/update/uninstall and data-retention test notes.
- [ ] Test exported websites outside the app.
- [ ] Prepare a release checklist with actual build/test outcomes and known limitations.
- [ ] If Google Play distribution is selected, separately validate current target SDK, permissions, ads, privacy policy, Data safety disclosures, store listing and all relevant Play policies.

### Exit criteria

A release APK is generated and verified on a physical or supported emulator device, and the final handoff clearly states exactly which checks were run. Google Play publication is not considered complete simply because an APK exists.

---

## Suggested repository structure

The exact structure may evolve during M0, but keep boundaries clear. One possible structure:

```text
Neoblogger/
├── AGENTS.md
├── PRODUCT_SPEC.md
├── IMPLEMENTATION_PLAN.md
├── README.md
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/                       # Wrapper and version catalog
├── app/                          # Native Android shell and composition root
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/.../app/     # Activities, Compose navigation, app state
│       │   ├── java/.../project/ # Project repository/services
│       │   ├── java/.../storage/ # SAF, workspace, file operations
│       │   ├── java/.../preview/ # WebView configuration and navigation
│       │   ├── java/.../ads/     # One-time-ad abstraction/state (provider TBD)
│       │   ├── res/              # English strings, theme, icons
│       │   └── assets/
│       │       ├── code-editor/  # Bundled CodeMirror/Emmet assets and licenses
│       │       └── design-editor/# Trusted visual editor runtime bundle
│       ├── test/                 # JVM unit tests
│       └── androidTest/          # Device/instrumentation tests
├── editor-runtime/               # Optional local web build source, if separated
│   ├── design/
│   └── code/
└── docs/                          # Architecture decisions, security checklist, test notes
```

Do not force this exact tree if Gradle/module organization makes a better maintainable design. Document any meaningful deviation.

## Risk register to revisit at each milestone

| Risk | Early check | Required response if unresolved |
|---|---|---|
| Source-preserving visual edit not feasible for all CSS/HTML | M0 spike and M5 round-trip tests | Limit visual editing to tested subset, preserve unsupported source |
| Canvas performance too slow | M0/M4 measurements | Reduce full rerenders, incremental parsing, simplify overlays; do not fake results |
| Local Preview cannot resolve paths safely | M0 WebView spike | Implement explicit project resource mapper; do not enable broad file access |
| SAF provider limitations | M1/M6 device tests | Use managed workspace with import/export rather than claim live edits work everywhere |
| One-time ad incompatible with policy/privacy expectations | M7 policy check | Leave ads disabled and raise a product decision; do not add recurring ads |
| Scope expansion delays usable release | Every milestone | Keep a minimal end-to-end site workflow working at all times |

## Definition of a completed milestone

A milestone is complete only when its exit criteria are demonstrated, relevant tests/builds were run, known limitations are documented, and `PRODUCT_SPEC.md` remains accurate. The agent must not claim success for device behavior that it has not actually tested.
