## Context

The current `chatbot-widget.js` injects the chatbot as an iframe into a host website. The existing styling relies on legacy rectangular structures (`6px` border radius), static hex colors, and standard block layouts. On mobile devices, the widget scales to almost full screen, but leaves the toggle button visible, which causes UI overlap and prevents a true native app-like immersive experience.

## Goals / Non-Goals

**Goals:**
- Upgrade the visual geometry to 2026 standards: pill-shaped/circular toggle buttons, high-radius (24px+) floating panels, and glassmorphic headers.
- Switch theme variables from static Hex codes to high-dynamic Oklch or HSL definitions for glowing, ambient light effects.
- Implement a true 100vw/100dvh full-screen architecture for mobile breakpoints.
- Introduce inter-frame communication (`postMessage`) to close the chatbot directly from the iframe on mobile devices.

**Non-Goals:**
- Changing the chatbot's internal business logic or AI streaming handlers.
- Building a completely new widget framework (we will enhance the existing Vanilla JS implementation).

## Decisions

1. **Oklch Color Spaces & CSS Variables**
   - *Rationale*: Oklch allows for beautiful, perceptually uniform gradients and "ambient glow" shadows that mimic light scattering through frosted glass.
2. **Dynamic Toggle Button (Circular & Animated)**
   - *Rationale*: A 54x54 square button is outdated. A 60x60 circular button with inner shadows (`box-shadow: inset...`) and a subtle pulse feels more like an AI "Core" than a standard chat button.
3. **100dvh Fullscreen with Hidden Toggle on Mobile**
   - *Rationale*: Safari and Chrome on iOS/Android handle `100vh` poorly due to dynamic address bars. Using `100dvh` ensures the iframe exactly fits the viewport. Hiding the toggle button (`opacity: 0; pointer-events: none`) when open on mobile provides a completely undisturbed canvas for the Chat UI.
4. **Header-Injected Close Button via postMessage**
   - *Rationale*: Since the external toggle is hidden on mobile, users need a way to close the chat. The iframe `chatbot.html` will contain a close icon in its header, which dispatches `{ type: 'chatbot-close' }` to the parent window. The existing `chatbot-widget.js` already has a listener for this event, making integration trivial.

## Risks / Trade-offs

- [Iframe Cross-Origin Issues] → Mitigation: The `postMessage` protocol is already correctly configured to accept messages, but we must ensure the `window.parent` reference is valid and doesn't trigger CORS blocks when hosted on strict third-party domains.
- [Mobile dVH Support] → Mitigation: `dvh` is supported in all modern browsers (Safari 15.4+, Chrome 108+), which aligns with the 2026 target.
