## 1. CSS Variables & Structural Aesthetic Updates (`style.css`)

- [x] 1.1 Replace static Hex color tokens with Oklch/HSL high-dynamic equivalents (e.g., `--brand-primary`, `--brand-glow`).
- [x] 1.2 Update `.chatbot header` to use `backdrop-filter: blur(20px)` and refined gradient backgrounds.
- [x] 1.3 Upgrade chat bubble geometries (`border-radius`) from 8px to pill-shaped (20px+).
- [x] 1.4 Implement fluid typography using `clamp()` for chat text and input areas.

## 2. Widget Container Modifications (`chatbot-widget.js`)

- [x] 2.1 Update the floating toggle button (`#chatbot-plugin-toggle`) to a 60x60 circular shape with layered `box-shadow` glows.
- [x] 2.2 Modify the desktop `#chatbot-plugin-iframe` styles to feature a `28px` border radius and remove hard borders in favor of subtle inner strokes.
- [x] 2.3 Update the JS-injected `@media (max-width: 600px)` query to stretch the iframe exactly to `100vw` by `100dvh` at `bottom: 0, right: 0`.
- [x] 2.4 Add CSS rules to hide (`opacity: 0, pointer-events: none`) the toggle button when the iframe enters the `.open` state on mobile constraints.

## 3. Internal Dismissal Control (`chatbot.html` & `ChatUI.js`)

- [x] 3.1 Inject a new `<button id="close-chatbot-btn">` into the `<header>` element of `chatbot.html`.
- [x] 3.2 Add styling for the close button exclusively for mobile contexts within `style.css`.
- [x] 3.3 Attach an event listener in the frontend Javascript to emit `window.parent.postMessage({ type: 'chatbot-close' }, '*')` when the close button is clicked.
- [x] 3.4 Test and verify the parent iframe wrapper successfully intercepts the `postMessage` and transitions the widget back to a collapsed state.
