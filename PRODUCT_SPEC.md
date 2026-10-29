# Neoblogger — Product and Engineering Specification

- **Document ID:** NB-PRD-001
- **Product:** Neoblogger
- **Status:** Draft specification for implementation
- **Version:** 0.1
- **Last updated:** 2026-10-17
- **Primary implementation agent:** GitHub Copilot
- **Target platform:** Android application, minimum Android 8.0 (API 26)
- **Product language:** English

> This document describes intended behavior and acceptance criteria. It does not claim that any feature has already been implemented. Items marked **TBD** require a decision or validation before the relevant release.

## 1. Product summary

Neoblogger is a free, Android-native, offline-first editor for creating distinctive personal blogs and other static websites from a phone. It combines a visual website designer, a source-code editor, and an interactive website preview in one installable application.

The product is intended for people who want creative freedom rather than a rigid blog template. Users must be able to create a site from scratch or import an existing folder/archive of ordinary web files, edit it visually or manually, test it as a functioning website, and export standard HTML/CSS/JavaScript/assets that can be hosted anywhere.

### 1.1 Product principles

1. **Real files, no lock-in.** HTML, CSS, JavaScript, images, fonts, SVG, JSON, and other project resources remain normal files. Neoblogger-specific metadata may assist editing, but is never required to publish the site.
2. **Visual and source editing belong together.** The visual editor must produce useful real code; source edits must update the visual model when they can be interpreted safely.
3. **Do not destroy user work.** Never silently discard unsupported code, overwrite an imported original during conversion, or replace a project with generated markup without an explicit operation and a recoverable backup.
4. **Preview must behave like a website.** Links, form controls, dropdowns, client-side validation, navigation, and project JavaScript should operate in the preview, subject to clear security controls.
5. **Touch-first, keyboard-capable.** The app must be usable on a phone and support a physical keyboard where available.
6. **Offline-first.** Core editing, file management, visual design, code editing, and preview of local assets must not require an account or an internet connection. Remote website resources and advertisements naturally may require internet.
7. **Fast feedback.** The designer and preview are core features, not decorative mockups. Responsiveness and correctness must be tested early.
8. **Small, honest product scope.** Neoblogger is a static-site editor. It is not a hosted blogging service, CMS backend, general-purpose IDE for building arbitrary frameworks, or Git client.

## 2. Confirmed decisions

The following are requirements supplied by the product owner and should not be changed casually by the implementation agent.

| Area | Confirmed decision |
|---|---|
| Product name | Neoblogger |
| Platform | Installable Android app; native application shell |
| Minimum Android | Android 8.0 / API 26 |
| Main implementation | Kotlin |
| Main native UI | Jetpack Compose |
| Product UI language | English throughout |
| Primary use | Creative personal blogs and static websites |
| Main editor areas | `Design`, `Code`, `Preview` tabs |
| Design modes | Free-positioning canvas and responsive/web-flow layout mode |
| Code | HTML, CSS, JavaScript, JSON, SVG, Markdown, and configuration/text files; Emmet and useful editor tools |
| Preview | Interactive page with links, forms, dropdowns, and JavaScript behavior |
| Project lifecycle | Create new projects and import existing projects |
| File handoff | Import/export ordinary files and ZIP archives; no proprietary publish format |
| Offline | Core application features available offline |
| Git | Not required; do not implement Git in the initial product |
| Reload | Preview is refreshed manually by the user in the initial product |
| Navigation | Internal project links should work; external navigation requires confirmation before leaving the project preview |
| Visual synchronization | Bidirectional where supported; unsupported source should not be silently lost |
| Import conversion | Attempt to interpret/convert imported structures for visual editing, with a report and review step before committing potentially destructive changes |
| History | Basic undo/redo for editor actions in the initial release |
| Appearance | Dark theme by default |
| File navigation | Open-file tabs plus a project file explorer |
| Cost model | Core features free; no mandatory account or subscription |
| Ads | At most one launch ad for the lifetime of the current app installation; never repeatedly show ads |
| Delivery target | Debug/test APK during development and a release-ready APK; Google Play readiness may be prepared but publication is not automatic |
| Primary agent | GitHub Copilot |
| Implementation approach | Incremental, working releases with a scalable architecture |

## 3. Product scope and priority vocabulary

- **P0 — Release blocker:** required for a useful first end-to-end release.
- **P1 — Initial product / follow-up release:** planned product capability that may be implemented after the first stable slice, but should be architecturally supported.
- **P2 — Future enhancement:** explicitly not required for the first release.
- **TBD:** no final decision yet. Do not invent a product-owner decision when it affects user data, architecture, privacy, cost, or core workflow.

### 3.1 P0 scope

- Android app shell, English UI, dark theme, navigation between `Design`, `Code`, and `Preview`.
- Create a project and generate a simple static website with an `index.html` entry point and linked CSS.
- Open/import a project from a user-selected folder or ZIP; export a project as ZIP or to a chosen folder.
- Local project storage, save status, explicit save/export operations, and protection against accidental data loss.
- Visual canvas that can select, inspect, move, add, duplicate, and delete a useful starter set of elements.
- Responsive/web-flow and free-positioning layout options.
- HTML/CSS/JavaScript source editing with syntax highlighting, line numbers, search/replace, basic formatting, and Emmet expansion for HTML/CSS.
- Interactive preview that supports local page navigation, forms, dropdowns, buttons, and JavaScript.
- Manual preview reload and browser-like Back/Forward controls within the project.
- Basic undo/redo for visual and source editing operations where the editor can define a safe operation.
- Offline operation for app-owned features and local project assets.
- A security baseline for WebView, imported projects, archive extraction, and external links.
- Unit/integration tests for critical project, editor, save/export, import, and preview behavior.
- One-time launch-ad decision logic, with provider and policy validation treated as release configuration rather than a hardcoded dependency.

### 3.2 P1 scope

- Advanced visual elements: tables, video, SVG, forms, media, nav patterns, reusable sections, and broader HTML attributes.
- Asset manager with image preview, folders, relative paths, local image optimization options, and unused-resource reporting.
- More robust CSS inspector including typography, spacing, layout, borders, colors, and responsive rules.
- Import diagnostics and a more advanced conversion review/diff interface.
- Responsive viewport presets, custom viewport sizing, and zoom controls.
- Restore last open project and last open tabs after relaunch.
- Accessibility checks and reports for common issues such as missing image alt text and insufficient labels.
- Google Play release checklist and store-material preparation, if the owner elects to publish.

### 3.3 P2 / explicitly out of scope for initial release

- Git, branches, commits, and remote repository integration.
- Server-side application development, database-backed websites, PHP execution, Node servers, or a hosted CMS.
- Automatic hosting or deployment to a third-party provider.
- Team collaboration, accounts, cloud synchronization, or mandatory online storage.
- A plugin marketplace or arbitrary third-party extensions.
- AI generation features.
- Automatic continuous preview reload (manual reload is the initial requirement; a later option may be added).
- Support for building framework projects that require npm, bundlers, compilation, or a server. These projects may be imported as files and inspected, but Neoblogger must warn that build execution is not supported in the initial product.

## 4. Users and key use cases

### 4.1 Primary user

A creative individual who wants to make a personal blog or distinctive static site from an Android phone, learns by editing real code, and values the ability to publish the resulting files anywhere.

### 4.2 Main user journeys

1. **Start from scratch:** open Neoblogger, create a project, select a starter structure, add visual elements, style them, edit code when desired, test the result in Preview, and export.
2. **Continue an existing website:** select an existing folder or ZIP, import it into a managed project workspace, inspect file structure and import diagnostics, edit in Design or Code, test Preview, and export.
3. **Design visually:** select a page element with one tap, edit it in the inspector, move/resize it using handles or controls, change layout mode, and inspect updated source.
4. **Code manually:** open an HTML/CSS/JS file, edit with syntax highlighting and Emmet, save, and use Design to inspect compatible changes.
5. **Test website interactions:** enter Preview, interact with forms and dropdowns, follow internal links, use Back/Forward, reload manually, and confirm external navigation before leaving the project.
6. **Move the project elsewhere:** export a folder or ZIP containing ordinary web files; extracted files must not require Neoblogger to run.

## 5. Navigation and English UX

### 5.1 Main navigation

The workspace must expose three clear primary tabs, using these exact English labels:

- `Design`
- `Code`
- `Preview`

Suggested first-run/home actions: `New Project`, `Open Project`, `Import ZIP`, and `Recent Projects`. Exact home screen layout is a design choice, but these actions must be easy to discover.

### 5.2 UI language

All application-owned user-facing text must be English, including navigation, dialogs, property names, tooltips, errors, accessibility labels, onboarding, empty states, confirmation screens, and settings. Do not hardcode user-facing strings throughout the source; use Android string resources so localization can be added later. Website text that belongs to the user's own project must never be translated or rewritten by the app.

### 5.3 Visual style

- Dark theme by default.
- High contrast between the canvas, selected element, inspector, code, and app chrome.
- Avoid excessive panels on small displays; use drawers, bottom sheets, or collapsible panels when appropriate.
- Do not force a desktop-style three-column layout onto a phone. The phone layout should give the canvas/code/preview usable space, with inspector and file explorer available contextually.
- Support portrait orientation as the baseline. Landscape support is desirable and should not break essential interactions.
- Provide visible feedback for saving, errors, import progress, unsupported content, and destructive actions.

### 5.4 Core screens

1. **Home / Projects:** create, open, import, recent project list, settings.
2. **Project workspace:** project name and save status; tab bar; file tabs and explorer entry.
3. **Design:** canvas, element selection, element insertion, layout mode switch, inspector, structure/tree panel.
4. **Code:** open-file tabs, syntax-aware editor, search/replace, Emmet action, file selection.
5. **Preview:** page viewport, `Back`, `Forward`, `Reload`, viewport/zoom controls, external navigation confirmation.
6. **Import review:** detected project root, entry page suggestion, asset/file counts, unsupported features, conversion issues, and confirmation before applying conversions.
7. **Export:** export folder/ZIP destination, validation warnings, completion status.
8. **Settings:** theme preference may later expand beyond dark default; launch-ad status must not be user-visible as an ad frequency gimmick.

## 6. Design tab requirements

### 6.1 Canvas model

The canvas is the primary visual interaction surface. It should display an accurate rendering of the selected HTML page and a separate selection/measurement overlay. The project remains standards-based HTML/CSS.

**Important design constraint:** do not make the site depend on absolute coordinates for every object. Elements should use normal HTML semantics and CSS layouts; free positioning is a specific layout option for selected elements or containers.

### 6.2 Layout modes

- **Free Positioning:** users can position supported elements within an explicit containing block, move/resize them, and configure coordinates/size using handles or numeric properties. Output standard CSS such as `position: absolute` only where appropriate, with a stable containing block and predictable responsive behavior. The inspector must make it clear which container establishes the coordinate system.
- **Responsive / Web Flow:** users can design with normal document flow, Flexbox, Grid, gap, margin, padding, and width constraints. This must be a first-class mode rather than an afterthought.
- A page or container may use different modes where technically supportable. Avoid silently converting an entire page between modes if that would materially change layout.
- **Default layout behavior: TBD.** Recommended behavior: open a canvas-first editor but create new blog pages in responsive flow by default; allow the user to explicitly choose Free Positioning. Validate this default with a working prototype before locking it.

### 6.3 Selection and touch behavior

- One tap selects an element and shows a clear selection outline and relevant quick actions.
- A contextual toolbar provides actions such as `Edit`, `Duplicate`, `Move`, `Delete`, and `Bring Forward`/`Send Backward` when applicable.
- Long press opens a contextual menu; it must not be the only way to access properties.
- Dragging must start from a clear handle or move affordance when possible, to avoid stealing normal text selection and form interaction.
- Canvas pan and zoom must not conflict with element movement. Exact gestures are **TBD** and must be validated on real devices.
- The canvas itself must not use user website scripts as the controller of the editing UI.

### 6.4 Element library

The initial useful element set should include:

- Semantic structure: `header`, `nav`, `main`, `section`, `article`, `aside`, `footer`, `div`, and `figure`.
- Text: headings, paragraph, inline text, blockquote, code, lists, and horizontal rule.
- Links and actions: anchor and button.
- Media: image, video element, and SVG where safely supported.
- Layout: container, flex container, grid container, and spacing/layout helpers that emit normal HTML/CSS.
- Forms: label, input types, textarea, select, checkbox, radio, fieldset, and form.
- Tables and common navigation/dropdown patterns.
- An advanced/custom HTML element option for setting tag name/attributes when safe.

The designer may not fully visually edit every possible HTML element, but it must preserve elements and attributes it does not understand.

### 6.5 Inspector

The inspector must show only relevant properties for the selected node, grouped into sections such as:

- `Content`: text and element-specific content.
- `Link`: URL or local target for anchors.
- `Image`: source, alt text, dimensions, object fit, local asset selection.
- `Layout`: display, position mode, width, height, alignment, gap, overflow.
- `Spacing`: margin and padding.
- `Typography`: font family, size, weight, line height, letter spacing, alignment.
- `Color & Background`: foreground, background, gradients only if supported reliably.
- `Border & Effects`: border, radius, shadow, opacity.
- `Responsive`: breakpoint-aware styles or media-query rules when supported.
- `HTML Attributes`: `id`, `class`, `title`, `role`, `tabindex`, `data-*`, and other safe attributes.
- `Accessibility`: alt text, labels, accessible name, semantic hints.
- `Advanced CSS`: a controlled escape hatch for manual declarations.

Changing a property must update the actual project source representation, not just a temporary visual overlay. If a property cannot be expressed safely, disable it with an explanation rather than making a fake change.

### 6.6 Element tree and layering

Provide a navigable hierarchy/tree of DOM elements. Selecting a tree row must select the corresponding canvas element. Rename labels for clarity (for example, `img.hero-image` or `section#about`) without changing source unless the user edits the actual attributes. Z-order controls apply only where positioning makes them meaningful.

## 7. Code tab requirements

### 7.1 Languages and file types

At minimum, recognize and provide suitable editing support for:

- HTML (`.html`, `.htm`)
- CSS (`.css`)
- JavaScript (`.js`, `.mjs` where practical)
- JSON (`.json`)
- SVG (`.svg`)
- Markdown (`.md`)
- Plain text/configuration files (`.txt`, `.webmanifest`, common config text files)

Unknown text files may open as plain text. Binary assets must not be treated as source text by default.

### 7.2 Required editor capabilities

- Syntax highlighting appropriate to file type.
- Line numbers and readable indentation.
- Undo/redo where supported by the editor engine.
- Find and replace within the current file.
- Basic code formatting/indentation command.
- Emmet expansion for HTML and CSS abbreviations, with a discoverable toolbar/menu action that works without a physical keyboard.
- Autocomplete and close brackets/tags where supported.
- Clear modified/unsaved state.
- File tabs and a way to jump to the project explorer.
- Preserve whitespace and encoding when feasible; UTF-8 is the default for newly created text files.

Candidate implementation: bundle CodeMirror 6 and Emmet locally in the app's own editor assets. The developer must confirm library compatibility, licensing, packaging/build requirements, and real Android input/IME behavior before committing. Do not load editor scripts from a CDN at runtime.

### 7.3 Emmet behavior

- Provide HTML and CSS abbreviation expansion.
- Offer a visible `Expand Emmet` action as mobile keyboards do not always provide Tab or expected desktop shortcuts.
- If the abbreviation cannot be expanded, leave text untouched and present a non-blocking explanation.
- Emmet must work offline using bundled code/resources.
- If feasible, support tab-stops/edit points generated by the chosen Emmet integration.

### 7.4 Code editing and source integrity

The code editor must operate on the project's real working file buffer. Source text must not be recreated from a simplified visual model after every keystroke. Use a versioned document/buffer model and parsing/analysis in the background so large files do not freeze the UI.

## 8. Preview tab requirements

### 8.1 Required interaction

Preview must render and run the selected page as a real interactive static website, not as a screenshot. It must support:

- Clicking anchors and navigating to other local project pages.
- Back, Forward, and manual Reload.
- Typing in form inputs and textareas.
- Select menus, checkboxes, radio buttons, date fields where supported by WebView, and file inputs where Android/WebView permits them.
- Form submission and browser-native/client-side validation.
- Buttons, dropdowns, accordions, menu toggles, and JavaScript-powered interactions.
- CSS, images, SVG, fonts and other local resources referenced by the project.
- JavaScript executing in the page context with behavior consistent with WebView's supported browser APIs.
- Responsive viewport presets, with custom sizing/zoom as a follow-up if not in P0.

### 8.2 Browser controls

The Preview area must contain `Back`, `Forward`, `Reload`, and a visible page/viewport context. Disable Back/Forward when there is no corresponding history. Manual Reload is the initial refresh mechanism; do not continuously auto-reload the page after every source edit.

### 8.3 Preview versus Design

Preview and Design must be separate operational modes:

- **Design**: editing tools are enabled; project scripts must not be allowed to control editor chrome. Script execution in the canvas should be disabled or isolated by default. Clicking a site element selects it instead of following links where appropriate.
- **Preview**: selection overlays and editing gestures are removed; normal site interactions take precedence. Users can complete forms, open dropdowns, click buttons, and navigate.

A project preview should be rendered from a consistent snapshot of the latest saved/applied working files. The UI must clearly indicate when edits have not yet been saved or when the preview is showing an older snapshot.

### 8.4 Navigation and external resources

- Internal relative links must resolve within the current project and maintain browser-like history.
- External top-level navigation (HTTP/HTTPS, `mailto:`, `tel:`, or other schemes) must be handled explicitly. Confirm before leaving Preview by default.
- Do not let imported HTML navigate the whole Android activity without user action.
- Full JavaScript support is required for project pages, but JavaScript must never receive an unrestricted native bridge to app internals.
- Remote APIs, external stylesheets/fonts and remote images may not work offline. This must be explained in a useful way, not treated as an application defect.
- A way to disable network access for preview testing is desirable; default project network behavior is **TBD**. Do not silently rewrite remote URLs.

### 8.5 Form security and honesty

- The UI may let users test form fields and client-side validation normally.
- Before a form submission that would navigate to an external origin or send a request externally, show a clear warning/confirmation in accordance with the selected strict form policy.
- Local test submissions may be inspected if a test-capture mode is implemented, but must not be described as delivered to a server.
- Do not claim an email was sent or data was stored remotely unless the site has a working external endpoint and the request actually succeeded.

## 9. Project creation, import, storage, and export

### 9.1 Create a project

- User enters a project name.
- App creates a valid, standards-based static-site folder with at least `index.html` and a linked stylesheet.
- Include a minimal starter; avoid forcing the user into a large template.
- The generated page must have a viewport meta tag, semantic structure, sensible defaults, and responsive behavior.
- Project names must be converted safely to filesystem names; reject path traversal and invalid paths.

### 9.2 Import

Support both:

1. A user-selected existing directory using Android's Storage Access Framework (SAF).
2. A ZIP archive containing a website project.

The import flow must:

- Use the Android system file/directory picker; do not request broad storage permissions when SAF is sufficient.
- Detect likely site entry pages, including root `index.html`; if ambiguous, ask the user to choose.
- Preserve file hierarchy and relative paths.
- Identify likely HTML/CSS/JS files, media, fonts, SVGs, and assets.
- Report missing local resources, unsupported build configuration, suspicious archive paths, oversized files, and any files that could not be imported.
- Detect projects that require build tooling (for example, framework source with a `package.json` and no generated static output); do not pretend they can be previewed if they require compilation.
- Prevent ZIP path traversal (Zip Slip), duplicate-path overwrite, and unsafe extraction outside the project workspace.
- Avoid following symlinks or other filesystem constructs outside the selected project scope.

### 9.3 Conversion and unsupported markup

The product owner selected an import/conversion workflow that attempts to make imported content editable visually. Conversion must be safe and inspectable:

1. Import source into an app-managed working copy; never edit the original import source before confirmation.
2. Parse the real HTML/CSS with standards-aware tooling, not regex-only transformations.
3. Create a conversion report listing nodes, rules, attributes, scripts, or layouts that the visual editor cannot fully represent.
4. Offer preview/diff of transformations that would alter source semantics or structure.
5. Require explicit user confirmation before applying potentially destructive conversions.
6. Keep unsupported nodes/declarations as raw/custom content wherever possible. Never silently delete them merely because the visual inspector lacks a control.
7. Preserve the unconverted imported snapshot so the user can recover.
8. If the editor cannot guarantee a conversion is semantics-preserving, show the limitation and let the user edit the code instead.

### 9.4 Local storage

Core project data should be stored locally and remain available offline. Use a managed working copy for stable editing and preview unless a tested direct-folder editing approach proves reliable across SAF providers.

**TBD:** Whether users should be able to work live in-place inside an external selected directory or whether import-to-workspace plus export/sync should be the only supported workflow. Recommended initial implementation: managed internal workspace, with explicit import and export using SAF. Do not require broad `MANAGE_EXTERNAL_STORAGE` permissions.

Project source files are the authoritative website content. A local database may store project metadata, recent-project references, settings and indexes, but should not replace HTML/CSS files with a proprietary database format.

### 9.5 Export

- Export project files to a user-selected folder and/or ZIP.
- Preserve relative paths, file names, encodings when feasible, and normal web folder layout.
- Exclude editor-only scratch files, temporary preview artifacts, app caches, credentials, and ad/debug data.
- Include every resource required by the site, subject to external dependencies that cannot be bundled.
- Validate missing local references where practical and show a report before export.
- Never add a Neoblogger runtime requirement to exported files.
- Confirm before overwriting existing files or archives.

### 9.6 Asset manager

The asset manager should support importing and organizing images, fonts, SVG and video assets. It should provide previews when file type support allows, rename/move operations, and updates to paths when moving managed assets. Local optimization and unused-resource detection are P1; any optimization must preserve the original asset or offer a recoverable replacement workflow.

## 10. Bidirectional visual/source synchronization

This is one of the highest-risk components and must be designed before implementation is considered complete.

### 10.1 Source of truth

The project's actual HTML/CSS/JS files and current editor buffers are authoritative. The visual model is a representation of those sources, not a second independent page definition that silently overwrites them.

### 10.2 Required behavior

- Visual property changes update the appropriate HTML attributes and/or CSS declarations.
- Code changes update the visual canvas when parsing succeeds and the constructs are representable.
- CSS changes from stylesheets must be reflected in computed canvas appearance.
- Class names, IDs, attributes and existing class-based styling must be retained.
- Unknown attributes and unrecognized nodes/rules must remain in the source.
- The implementation must avoid reformatting the entire source file for a one-property visual edit wherever possible. Prefer targeted source edits or a source-preserving syntax/CSS representation.
- Parse work must not block text input or dragging. Use debouncing/versioning and discard stale analysis results.
- If a source edit has a parse error or an unsupported construct, keep the user's text, show the error/limitation, and do not replace it with stale generated code.
- If two changes conflict, display a conflict state/review choice rather than silently applying whichever update finishes last.
- Add tests for edits in both directions and for preservation of comments, unknown attributes, custom elements, existing scripts, media queries and nested CSS.

### 10.3 Conversion policy

When the user explicitly requests conversion into a more visually editable structure, Neoblogger may normalize source after displaying a report. Conversion is an explicit operation with backup/undo and must never happen silently during ordinary preview or source editing.

### 10.4 Technical spike required

Before full implementation, build a small proof of concept proving that:

1. Existing HTML can render correctly in the visual canvas.
2. A selected DOM element can be identified reliably.
3. A visual style change writes a targeted source change.
4. A direct CSS/source edit refreshes the rendered appearance.
5. Unrecognized code survives round-trips.
6. The app remains responsive on a mid-range Android phone.

Do not assume a complete WYSIWYG synchronization engine is trivial. If a reliable implementation needs a constrained first set of editable constructs, document the supported set and preserve all other code.

## 11. Undo, redo, and saving

- Provide basic undo/redo for editor actions that can be represented safely: source buffer edits through the code editor's history, and visual actions such as move, resize, text/property change, add, duplicate, and delete.
- For operations affecting multiple files, treat the action as a transaction or provide an explicit confirmation and recovery route.
- Never present `Saved` before writes complete successfully.
- Show unsaved state and failed-write errors.
- Use atomic/temporary write patterns where feasible so a crash does not leave a source file half-written.
- Autosaving drafts to the internal workspace is recommended; export to a user-selected folder remains explicit unless a later direct-sync feature is designed.
- Exact cross-tab shared history semantics are **TBD**. Initial release must at least avoid conflicting history states that silently revert another tab's changes.

## 12. Technical architecture

### 12.1 Recommended high-level structure

Use Kotlin and Jetpack Compose for the app shell, navigation, dialogs, project actions, and Android integration. Use local embedded web tooling where it is materially better suited to HTML editing and rendering. This is still an installable Android application; the core application shell, lifecycle, file operations, permissions, and project management remain native.

Recommended components:

- **Native application shell (Kotlin/Compose):** navigation, app lifecycle, dialogs, settings, project list, file picker integration and tab state.
- **Project service:** project metadata, workspace paths, import/export, save coordination and operation validation.
- **File service:** safe reads/writes, URI access, path normalization, atomic save helpers and archive processing.
- **Editor engine:** source buffers, parsing, CSS/DOM representation, diagnostics and synchronization coordinator.
- **Design runtime:** bundled local HTML/JS/CSS UI inside a dedicated WebView for canvas, selection, drag/resize, overlays and visual inspector integration. Keep untrusted project scripts separate from editor chrome.
- **Code runtime:** bundled CodeMirror 6 plus Emmet and required language tooling in a local WebView or other validated integration.
- **Preview runtime:** separate WebView for executing the user's page in an isolated context with explicit navigation/resource policies.
- **Metadata database:** Room for projects/settings references/indexes where appropriate; DataStore for simple preferences and one-time-ad status.
- **Asset service:** project asset discovery, previews, path updates, optimization and diagnostics.
- **Test suite:** unit tests, JVM/instrumentation tests and manual device checklist.

### 12.2 Data flow

```text
Compose app shell
  ├── Project service ── File service / SAF / local workspace
  ├── Editor coordinator
  │     ├── HTML/CSS/JS source buffers
  │     ├── Standards-aware parsers / diagnostics
  │     └── Design runtime (trusted editor UI + isolated site canvas)
  ├── Code runtime (CodeMirror + Emmet, bundled locally)
  ├── Preview runtime (separate WebView, project scripts enabled)
  ├── Asset service
  └── Room / DataStore
```

### 12.3 WebView rules

- Bundle editor assets locally; the code editor and Design UI must not depend on a CDN at runtime.
- Use `androidx.webkit` and `WebViewAssetLoader` or an equally well-justified secure local-content mechanism to serve app-owned local assets over an HTTP(S)-style origin. Avoid relying on arbitrary `file://` origins.
- Explicitly disable broad file access, file-origin access to other files, and universal file-origin access in WebView settings.
- Do not use `MIXED_CONTENT_ALWAYS_ALLOW`.
- Keep Design and Preview in separate WebView instances/contexts where needed; Preview must not expose the native editor bridge to imported project code.
- Do not register `addJavascriptInterface` for arbitrary project pages. Any bridge required by the trusted editor runtime must be narrowly scoped, documented, validated, and unavailable to Preview content.
- Enable JavaScript only for the editor runtime and Preview when required. Do not execute arbitrary imported page scripts in the editing shell.
- Control navigation using `WebViewClient`; prevent untrusted URLs from changing the app flow without user interaction.
- Handle file URLs, content URIs, redirects, downloads, iframes and external schemes deliberately rather than allowing all navigation.
- Avoid logging project source, form values, local paths, or private file contents in production logs.

### 12.4 Dependency policy

- Use maintained, compatible libraries and pin dependency versions.
- Check licenses before bundling every third-party dependency and preserve required notices.
- Prefer a small dependency footprint.
- Do not invent dependency coordinates or use deprecated versions simply because a code example exists online.
- Validate current stable Android Gradle Plugin, Kotlin, Compose, AndroidX WebKit, Room, DataStore, CodeMirror and Emmet integration at implementation time.
- Use local build tools and lock/pin versions as supported; builds must be reproducible.

## 13. Data model

The model may evolve, but keep these concepts separate.

### 13.1 Project metadata

Suggested `Project` fields:

- `id`: stable UUID.
- `displayName`: user-facing name.
- `workspacePath` or managed storage URI/reference.
- `entryPagePath`: relative path to the primary HTML page.
- `createdAt`, `lastOpenedAt`, `lastSavedAt`.
- `projectFormatVersion`: version for Neoblogger metadata only, not a proprietary website format.
- `isImported`, `hasUnsavedChanges`, `lastImportReportId`.

### 13.2 File metadata/index

- `projectId`.
- `relativePath` (normalized relative path; never an arbitrary absolute path from archive metadata).
- `fileType` / detected language.
- `lastKnownModifiedAt`, `sizeBytes`, `hash` when useful.
- `isText`, `isAsset`, optional diagnostics.

The actual file bytes remain in normal files within the project workspace.

### 13.3 Editor session

- Open tabs and active tab.
- Active page and selected element locator.
- Current viewport/layout mode.
- Undo/redo state where supported.
- Last displayed preview revision and unsaved status.

Session persistence is useful but may be staged after the core editor works.

### 13.4 Preferences

- Theme preference (default dark).
- Last open project.
- Editor preferences such as indentation and font size.
- Preview viewport preference.
- One-time-ad state, stored persistently per app installation.

### 13.5 One-time ad state

Use explicit states, for example `PENDING`, `COMPLETED_SHOWN`, and `COMPLETED_SKIPPED`. On the first eligible launch, attempt to show at most one ad only after the app's normal initial/loading screen is complete. If the device is offline, the ad is unavailable, consent is unresolved, or the configured provider fails, skip it, persist the completed/skipped state, and never repeatedly retry the launch ad. Do not show another ad in later launches. Clearing app data or reinstalling may reset local state; do not promise cross-reinstall identity tracking.

## 14. Performance and reliability requirements

Design and Preview agility are top product priorities.

- UI interactions must remain responsive during parsing, import, asset scanning and export; run expensive operations off the main thread.
- Dragging and selection should feel immediate on a representative mid-range API 26+ phone. Set a measurable target during the first prototype; use 60 fps as an aspirational canvas target, and document device/scene conditions when benchmarking.
- Avoid reparsing the entire project on every keystroke. Debounce and use incremental or background parsing where practical.
- Avoid refreshing Preview automatically; manual Reload is the expected behavior in the initial product.
- Avoid loading huge asset previews or whole binary files into memory unnecessarily.
- Long imports/exports need progress, cancellation where safe, and useful error messages.
- Failed saves must never be silently ignored.
- Handle process death, low-memory conditions, lifecycle recreation, and storage-provider errors without corrupting project files.
- Performance targets that cannot be demonstrated on a physical device must be reported as unverified rather than claimed as complete.

## 15. Security, privacy, and permissions

### 15.1 Permissions

- Use Storage Access Framework for user-selected files and folders whenever possible.
- Do not request broad all-files access (`MANAGE_EXTERNAL_STORAGE`) for ordinary project import/export.
- Request `INTERNET` only if required by the ad provider or if the user expects external resources/navigation. Separate the need for app services from the user's own website's network behavior.
- Request no contacts, location, microphone, camera or unrelated permissions unless a later approved feature actually requires them.
- Image picking should use Android's supported picker where feasible.

### 15.2 Untrusted imported code

Imported HTML, JavaScript, CSS, SVG and archives must be treated as untrusted user content.

- Never run project scripts in the native app shell.
- Do not expose native Java/Kotlin methods, filesystem APIs, project paths, or app-private data to Preview JavaScript.
- Keep WebView access to local file origins disabled; use controlled resource serving and path resolution.
- Limit preview to project-contained relative resources by default for local resolution; handle external resources using explicit, documented rules.
- Validate archive entries and prevent path traversal, symlink escapes and overwrite of unrelated project files.
- Do not evaluate imported SVG/event handlers in trusted editor chrome.
- External navigation and form submissions must follow the user's confirmation policy.
- A user must be able to cancel import/conversion and return to the original files.

### 15.3 Privacy

- No account is required.
- Project files remain local unless the user explicitly exports them or the project itself initiates an external request in Preview.
- No analytics or crash-reporting SDK by default unless explicitly approved later; if added, document the data collected and provide disclosures/consent as applicable.
- Do not log user site contents or form data.
- The ad provider and its data practices are **TBD**. Before release, document required privacy disclosures and consent flows based on the actual SDK, jurisdiction, and distribution channel.

### 15.4 One-time ad requirements and store-policy caveat

The product owner's requirement is one ad at most on the first app launch, never on subsequent launches during the same installation. No banners, recurring interstitials, rewarded ads, ad walls, or ads triggered by ordinary editor actions are in scope.

Implementation rules:

1. Do not put an interstitial video before the Android/system splash or before the app's loading screen.
2. Let the app load its normal startup shell first; the ad must not prevent the user from entering the app when unavailable.
3. Attempt the ad once only. Persist `COMPLETED_SHOWN` or `COMPLETED_SKIPPED`; do not keep prompting.
4. If the ad cannot be loaded or shown, skip it rather than block a fully offline-first app.
5. Do not choose or integrate a provider until its current Android SDK, user-consent requirements, data disclosures, test mode and relevant Google Play policies have been checked.
6. Do not claim Play Store compliance solely because the ad shows only once. Verify the final placement and behavior against the current policy before distribution. If the exact first-open full-screen format is not compliant or harms app review, raise the issue as a release blocker and ask for a product decision; do not silently substitute recurring ads.

**TBD:** Ad network/provider, test IDs, consent approach, app privacy policy, and whether the exact first-open placement is accepted by the planned distribution channel. The app must run and be testable with ads disabled.

## 16. Accessibility

- All app controls need accessible names and appropriate Compose semantics.
- Interactive targets should be comfortably touchable; target at least Android-recommended touch target sizing where practical.
- Maintain readable contrast, focus visibility and scalable text.
- Do not rely on color alone for selection, errors or save status.
- Ensure code editor controls can be reached with TalkBack where feasible and that important editor actions have accessible labels.
- Provide alternative property entry for drag operations (numeric position/size controls where applicable).
- Images inserted through the visual editor should expose an alt-text field; missing alt text should be detectable.
- The editor itself may have limitations with a screen reader due to code/canvas complexity; document tested support and never claim full accessibility without tests.

## 17. Error handling and observability

- Errors must be understandable in English and suggest a recovery action.
- Import/export operations should provide a human-readable report.
- On parse failures, preserve the source and identify the file/line where possible.
- On save failure, clearly show unsaved status and offer retry or export recovery.
- Keep debug logging behind a debug-only mechanism. Logs must not contain sensitive content.
- Capture enough non-sensitive diagnostic context to debug crashes locally; remote telemetry is not approved by default.

## 18. Acceptance criteria

A release cannot be called complete merely because the application launches. It must demonstrate the relevant criteria below on an Android device/emulator and, for key performance and touch behavior, a physical phone when available.

### A. App and project lifecycle

- [ ] App builds as an Android app with minimum SDK API 26.
- [ ] Main UI labels are in English and dark theme is default.
- [ ] User can create a project that contains valid `index.html` and stylesheet files.
- [ ] User can import a valid website folder through SAF.
- [ ] User can import a ZIP without path traversal or overwriting unrelated files.
- [ ] User can open an existing project and switch among Design, Code and Preview.
- [ ] User can export a site to ZIP and use the extracted website without Neoblogger installed.
- [ ] Core project creation/editing/export can be completed while offline.

### B. Design

- [ ] User can select an element with one tap and identify the selected node.
- [ ] User can add, edit, move, duplicate and delete supported elements.
- [ ] User can edit common text, link, image, layout, spacing, typography, color, ID and class properties.
- [ ] User can switch between responsive/web-flow and free-positioning modes.
- [ ] Visual operations update the source representation and survive closing/reopening the project.
- [ ] Unknown elements and unsupported attributes are not silently deleted.

### C. Code and synchronization

- [ ] HTML/CSS/JS files show syntax highlighting and line numbers.
- [ ] HTML and CSS Emmet abbreviations can be expanded by touch as well as keyboard where present.
- [ ] Source edits update the visual appearance after parsing succeeds and the user applies/reloads as designed.
- [ ] Visual property edits update corresponding code.
- [ ] Parse errors preserve the edited source and surface a diagnostic.
- [ ] Conversion previews changes and retains a recoverable original before destructive changes.

### D. Preview

- [ ] A form can be filled and client-side validation can be triggered.
- [ ] A select/dropdown and JavaScript-powered control can be interacted with.
- [ ] Internal links to local project pages work.
- [ ] Back, Forward and Reload behave correctly.
- [ ] External navigation is confirmed before leaving Preview.
- [ ] Project JavaScript runs in Preview without access to an unrestricted native bridge.
- [ ] Design selection overlays do not appear in Preview.
- [ ] Preview correctly loads project-local CSS, image and font resources within the supported scope.

### E. Reliability, privacy, and ads

- [ ] Failed writes never display a false `Saved` state.
- [ ] ZIP path traversal tests pass.
- [ ] WebView security settings are explicitly configured and reviewed.
- [ ] No unnecessary Android permissions are declared.
- [ ] The one-time-ad state ensures at most one attempted/showing event per installation and no repeat ads across launches.
- [ ] With no ad provider/configuration or with no network, all app features remain usable and startup is not blocked.
- [ ] App works without an account, cloud service or Git.
- [ ] Core code paths have tests; release checklist states which manual/device tests were actually performed.

## 19. Risks and mitigation

| Risk | Impact | Mitigation |
|---|---|---|
| Bidirectional visual/source sync is more complex than expected | Core product fails if code and canvas drift | Build an early technical spike; limit visual editing to supported constructs but preserve source; test round-trips |
| Free positioning creates poor mobile responsiveness | Sites look broken on other screens | Make coordinate container explicit; use responsive flow as a full mode; test multiple viewport widths |
| Imported code is not representable in canvas | Data loss or user distrust | Source-preserving AST/model, conversion report/diff, backup, explicit confirmation, raw/custom nodes |
| Arbitrary project JS executes in WebView | Security/privacy risk | Separate Preview from editor; no native bridge to Preview; controlled local resource loading and navigation |
| SAF providers behave differently | Import/export bugs | Use documented SAF APIs, test local storage and available document providers, robust errors |
| App scope becomes too large | Slow or incomplete release | Work in end-to-end vertical slices; prioritize editor and Preview; defer Git/cloud/framework builds |
| Performance on older phones | Editor feels unusable | Off-main-thread processing, avoid full reparses, use measured benchmarks on API 26+ devices |
| Ads create store-review or privacy issues | Release blocked or poor experience | One attempt only, non-blocking, verify provider policy/consent before enabling, ship test builds with ads disabled |
| Site form is mistaken for a backend | Users believe data was delivered | Explain client-side-only behavior and warn on external submissions |
| Third-party editor licenses or dependencies | Legal/build risk | Review licenses, pin versions, bundle notices, compile the smallest proof first |

## 20. Open decisions (TBD)

These decisions must not be silently invented if they materially change implementation:

1. **Default layout mode for a new page:** recommended responsive flow while keeping a canvas-first editor; owner confirmation needed.
2. **External folder workflow:** managed internal working copy plus import/export is recommended; live in-place editing through SAF is unconfirmed.
3. **Ad provider and first-open format:** must be selected and checked against current policies, SDK/consent requirements and privacy disclosures.
4. **Preview outbound network behavior:** decide whether project `fetch`/XHR and remote resources are allowed by default or require a per-project setting. Do not make the choice silently.
5. **Framework/build-based project import:** initial product will not build projects requiring npm/build tools; clarify whether detecting and warning is sufficient.
6. **Maximum import/project size:** define after testing on representative devices.
7. **Minimum target phone/RAM performance reference:** define a physical device for performance sign-off.
8. **Cross-tab undo semantics:** decide how actions spanning Code and Design are grouped after the core sync prototype.
9. **Publication plan:** APK is required; Play Store publication is optional and requires separate policy/compliance review.
10. **Branding:** application icon, accent color, package/application ID, privacy policy URL, and release signing key ownership remain TBD.

## 21. Implementation references

Use official documentation as the starting point and re-check current versions when implementing:

- Android Storage Access Framework: https://developer.android.com/training/data-storage/shared/documents-files
- Android loading local WebView content and `WebViewAssetLoader`: https://developer.android.com/develop/ui/views/layout/webapps/load-local-content
- Android `WebSettings` security reference: https://developer.android.com/reference/android/webkit/WebSettings
- CodeMirror documentation: https://codemirror.net/docs/
- Emmet documentation: https://docs.emmet.io/
- Google Play ads policy: https://support.google.com/googleplay/android-developer/answer/9857753

These references inform implementation choices; they do not replace validation against current dependencies and distribution policies at release time.

---

**End of PRODUCT_SPEC.md**
