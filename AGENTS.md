# AGENTS.md — Neoblogger Development Rules

These instructions apply to GitHub Copilot and any other coding agent working in this repository. `PRODUCT_SPEC.md` is the primary product contract. `IMPLEMENTATION_PLAN.md` defines the suggested order of work.

## 1. Product truths

- The product is **Neoblogger**, a native Android application for creating static websites and personal blogs.
- The user-facing app UI is English-only in the initial release.
- Use Kotlin and Jetpack Compose for the native shell unless a documented, approved technical constraint makes this impossible.
- The three primary work tabs are `Design`, `Code`, and `Preview`.
- The visual designer and interactive preview are core product features and must not be replaced by screenshots, mock-only panels, or placeholder buttons.
- Support creating new projects and importing existing project folders/ZIP archives.
- Exported sites must consist of normal web files and must not need Neoblogger to run.
- Do not implement Git, cloud accounts, a hosted CMS, or server-side execution in the initial product.
- Core functions must work offline when all project resources are local. External website resources and the optional ad may require connectivity.
- A one-time first-launch ad is the only intended advertising placement. No repeat ads.

## 2. Always follow the product spec

Before implementing a feature:

1. Read the relevant sections of `PRODUCT_SPEC.md` and update the plan/status if scope changes.
2. Identify dependencies, security implications, and acceptance criteria.
3. Implement a working, testable vertical slice instead of generating a broad but hollow skeleton.
4. Add or update automated tests and manual test instructions.
5. Build/compile the project after meaningful changes.
6. Report what is complete, what is tested, and what remains unverified.

Do not silently change a confirmed product decision. If a material decision is marked **TBD**, either implement a conservative, reversible default consistent with the spec and document it, or stop that subtask for owner input. Do not invent privacy, provider, package-identity, or destructive-conversion decisions.

## 3. Engineering principles

- Keep modules cohesive and interfaces explicit. Avoid putting the entire application into one activity, one giant composable, or one JavaScript file.
- Keep source files as the authoritative website data. Do not make a proprietary database schema the only representation of HTML/CSS.
- Build the visual editor around a tested parser/representation and a source-preservation strategy. Never rely on regex alone to transform arbitrary HTML or CSS.
- Preserve unknown tags, attributes, CSS rules, comments and scripts when possible. Conversion that changes semantics must be previewed, backed up, and explicitly confirmed.
- Make project import/export safe and recoverable. Treat imported projects and archives as untrusted input.
- Keep `Design` and `Preview` separate. Preview project scripts must not receive an unrestricted native bridge or access to app-private resources.
- Bundle the editor and its dependencies locally; do not require a CDN for Code or Design.
- Keep expensive parsing, asset scans, image processing, ZIP operations and file I/O off the UI thread.
- Handle file writes and state changes explicitly. Never show a successful save before data has actually been written.
- Avoid global mutable state and hidden bidirectional side effects. Use clear state ownership and versioned updates.
- Pin dependency versions; check current compatibility and licenses; include license notices for bundled third-party software.
- Do not add unapproved analytics, tracking or remote error-reporting services.
- Do not add Git features.

## 4. Android and WebView security rules

- Minimum supported OS is Android 8.0 / API 26.
- Use Android's Storage Access Framework for user-selected directories/files instead of broad all-files storage permission where practical.
- Use `androidx.webkit` / `WebViewAssetLoader` or a justified secure equivalent for app-owned local web assets.
- Explicitly disable broad file access, `allowFileAccessFromFileURLs`, and `allowUniversalAccessFromFileURLs` for WebViews.
- Never set `MIXED_CONTENT_ALWAYS_ALLOW` without a new, documented security review; the default must not enable it.
- Never expose `addJavascriptInterface` to arbitrary imported site pages or Preview content.
- Do not run project scripts in the trusted app shell or in the editing overlay.
- Route external navigation and external form submissions through explicit policy handling.
- Prevent ZIP path traversal, absolute-path entries, symlink escapes and overwriting files outside the selected workspace.
- Do not log source code, form payloads, secrets, personal data or private filesystem paths in release builds.
- Declare only permissions required for actual features.

## 5. Visual editor rules

- Treat the canvas as a visualization of real HTML/CSS, not a separate mock design document.
- Keep responsive/web-flow and free-positioning modes explicit.
- Selection should not break ordinary preview interaction. In `Design`, selection overlays and edit affordances are expected; in `Preview`, they must be absent.
- A one-tap selection and contextual edit controls must work without long press as the only input method.
- Any property the inspector claims to edit must alter real source/style and survive reopening. Disable or explain unsupported changes instead of faking success.
- Keep user-visible update feedback quick. Avoid full project reparses after every keystroke or pointer move.
- Do not silently move all elements to absolute coordinates or destroy responsive CSS to simplify the renderer.

## 6. Code editor rules

- The editor must run locally/offline; no runtime CDN dependencies.
- Provide visible Emmet expansion controls suitable for Android soft keyboards.
- Preserve the user's source buffer and line endings/formatting where feasible.
- On parse failures, keep the user's source, report diagnostics, and avoid overwriting it with stale generated text.
- Never regenerate an entire file from a simplified DOM model after each user edit.

## 7. Preview rules

- Preview must be an actual interactive WebView-based rendering of the user's website, not a screenshot or static prototype.
- Support local links, browser-like Back/Forward, manual Reload, forms, dropdowns and JavaScript within WebView's capabilities.
- External top-level navigation must require user confirmation by default.
- Do not promise that client-only forms send/store data remotely. Clearly distinguish local validation from actual external delivery.
- Show whether Preview is displaying the latest changes or an older snapshot.
- Manual reload is the initial expected behavior; do not add automatic continuous reload without an explicit scope update.

## 8. Advertising rules

- No ads in normal editor screens and no repeated ads.
- Attempt at most one launch ad per installation, after the app's normal startup/loading UI is complete.
- If the provider is unavailable, the user is offline, consent is unresolved, or the ad cannot be shown, skip it and persist the skipped/completed state. Do not block project access or retry on every launch.
- Keep the ad behind an interface/feature configuration so the app is buildable and testable with ads disabled.
- Provider, consent, privacy disclosures and exact placement require a deliberate review against current policy. Do not assert Google Play compliance without verification.

## 9. Code quality and testing

- Use descriptive names and small, cohesive classes/functions.
- Keep user-facing strings in Android string resources or the editor's localization resource layer. All initial strings must be English.
- Include tests for path normalization, archive extraction safety, import/export, source preservation, conversion reports, visual edits, preview navigation and one-time-ad state transitions.
- Use deterministic test data and avoid relying on external websites for core tests.
- Add a manual device test when behavior depends on WebView, touch input, IME, SAF provider behavior or performance.
- No empty implementations for P0 features. A stub must be clearly labelled as temporary and cannot be presented as complete.
- Do not suppress a failing test just to make CI pass. Fix it or document the actual blocker.
- Before finishing a task, run the relevant tests and an Android build; include the commands and actual outcomes in the handoff.
- Never claim a build, device test, or feature works unless it was actually verified.

## 10. Repository and change management

- Keep `PRODUCT_SPEC.md`, `AGENTS.md`, and `IMPLEMENTATION_PLAN.md` in the repository root.
- Prefer small focused commits/changes with a clear purpose, even if the current workflow does not use Git for the product itself. Repository version control is a development practice, not an in-app Git feature.
- Update documentation when architecture, acceptance criteria, dependencies or setup commands change.
- Do not store release signing keys, ad secrets, API keys, private certificates or user project contents in the repository.
- Keep generated build outputs and local IDE state out of source control.
- Include a reproducible setup/build guide before calling the repository ready for handoff.

## 11. Definition of done

A task is done only when:

1. The implemented behavior matches the task and relevant acceptance criteria.
2. Source changes compile for the supported Android target.
3. Relevant automated tests pass, or a precise reason for unverified tests is recorded.
4. User data and source code are preserved on normal and error paths.
5. No new security-sensitive behavior was added without review.
6. The README/spec/plan is updated when necessary.
7. The handoff lists modified files, build/test outcomes, limitations and next steps.
