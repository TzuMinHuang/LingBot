## Why

The current chatbot widget utilizes a legacy 2020s blocky aesthetic (rigid 6px border-radius, static hex colors) and provides a compromised mobile experience where the floating open button remains visible and overlaps content. To meet the refined standards of 2026 Enterprise Applications, we need a fluid, responsive, glassmorphic UI that scales gracefully and offers a deeply immersive full-screen experience on mobile devices without UI fragmentation.

## What Changes

- **Implement Oklch/HSL Color Palettes**: Transition from static Hex variables to high dynamic range Oklch variables for deep, glowing teal environments and ambient shadows.
- **Redesign Widget Trigger**: Upgrade the rectangular toggle button to a circular, animated "smart core" trigger with subtle pulsing states.
- **Floating Panel Aesthetics (Desktop)**: Increase iframe corner radii, replace harsh borders with subtle inner light-strokes, and deploy glassmorphism on headers.
- **BREAKING: Mobile Full-Screen Immersion**: On mobile breakpoints (<= 600px), the widget will expand to 100vw/100dvh seamlessly. The external toggle button will be hidden, and a native application-style "Close" handle will be injected into the iframe header.

## Capabilities

### New Capabilities
- `widget-aesthetics`: Governs the modern visual tokens, glassmorphic styling, animations, and fluid typography for the floating chatbot UI.
- `mobile-fullscreen-adaptation`: Manages the screen orientation layout changes, specifically the edge-to-edge `100dvh` expansion and the inter-frame communication (postMessage) to close the chat natively from within the header.

### Modified Capabilities
<!-- No existing backend or infrastructure specs modified -->

## Impact

- **CSS Variables & Structure**: Complete override of `style.css` variables, media queries, and component geometry.
- **Javascript Initialization**: `chatbot-widget.js` will undergo media query changes and DOM manipulation updates for the hidden toggle button.
- **HTML DOM**: `chatbot.html` will receive a new `<button>` inside `<header>` dedicated to dismissing the chatbot on mobile viewports.
