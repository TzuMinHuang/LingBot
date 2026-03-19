## ADDED Requirements

### Requirement: Oklch Color Palette and Glassmorphism
The overarching CSS variables SHALL be refactored to use High Dynamic Range colors to simulate ambient light and depth.

#### Scenario: Visual rendering of the application
- **WHEN** the chatbot widget and iframe are rendered
- **THEN** the elements SHALL exhibit smooth gradients, glassmorphism on the header (`backdrop-filter: blur`), and deep organic shadows (`rgba` based on brand colors rather than pure black).

### Requirement: Modern Widget Geometry
The floating elements SHALL use modern, rounded geometries rather than sharp blocks.

#### Scenario: Toggle button appearance
- **WHEN** the widget is in its collapsed state
- **THEN** the launch button SHALL be perfectly circular (`border-radius: 50%`) and feature a subtle glow effect (`box-shadow`).

#### Scenario: Iframe popup appearance
- **WHEN** the chatbot iframe is opened on desktop
- **THEN** the iframe container SHALL feature a high boundary radius (e.g., 28px) and organic drop shadows, abandoning strict 1px solid borders.
