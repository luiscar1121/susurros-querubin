---
name: Sonic Clarity SQ 01
colors:
  surface: '#0b1326'
  surface-dim: '#0b1326'
  surface-bright: '#31394d'
  surface-container-lowest: '#060e20'
  surface-container-low: '#131b2e'
  surface-container: '#171f33'
  surface-container-high: '#222a3d'
  surface-container-highest: '#2d3449'
  on-surface: '#dae2fd'
  on-surface-variant: '#c3caad'
  inverse-surface: '#dae2fd'
  inverse-on-surface: '#283044'
  outline: '#8d9479'
  outline-variant: '#434933'
  surface-tint: '#a1d800'
  primary: '#BEFF00'
  on-primary: '#253500'
  primary-container: '#b8f600'
  on-primary-container: '#506e00'
  inverse-primary: '#4b6700'
  secondary: '#d2bbff'
  on-secondary: '#3d147e'
  secondary-container: '#563398'
  on-secondary-container: '#c6aaff'
  tertiary: '#1E1B4B'
  on-tertiary: '#ffffff'
  tertiary-container: '#e3dfff'
  on-tertiary-container: '#615f93'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#b8f600'
  primary-fixed-dim: '#a1d800'
  on-primary-fixed: '#141f00'
  on-primary-fixed-variant: '#384e00'
  secondary-fixed: '#eaddff'
  secondary-fixed-dim: '#d2bbff'
  on-secondary-fixed: '#25005a'
  on-secondary-fixed-variant: '#543196'
  tertiary-fixed: '#e3dfff'
  tertiary-fixed-dim: '#c4c1fb'
  on-tertiary-fixed: '#181445'
  on-tertiary-fixed-variant: '#444173'
  background: '#0b1326'
  on-background: '#dae2fd'
  surface-variant: '#2d3449'
  success: '#BEFF00'
typography:
  headline-lg:
    fontFamily: Inter
    fontSize: 48px
    fontWeight: '700'
    lineHeight: '1.1'
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: '1.2'
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: '1.3'
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: '1.6'
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.5'
  label-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: '1'
    letterSpacing: 0.05em
  label-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: '1'
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  margin-page: 2rem
  gutter: 1rem
  stack-sm: 0.5rem
  stack-md: 1rem
  stack-lg: 2rem
  container-padding: 1.5rem
  base-unit: 8px
  margin-page-desktop: 32px
  margin-page-mobile: 16px
  stack-xl: 48px
---

## Brand & Style

This design system is built on the principle of **Sonic Clarity**—a visual metaphor for high-fidelity audio where every element is crisp, distinct, and intentional. The aesthetic balances a deep, technical foundation with electric accents, positioning the product as both a professional power tool and an energetic, forward-thinking platform.

The style is a hybrid of **Corporate Modern** and **High-Contrast Bold**. It utilizes a dark-mode-first approach to reduce visual noise and eye strain, allowing the vibrant lime and lavender accents to guide user attention with surgical precision. The interface should feel expansive, rhythmic, and highly responsive.

## Colors

The palette is engineered for maximum legibility in dark environments. 

- **Primary (#BEFF00):** A high-visibility lime green used for critical actions, success states, and key brand moments. It represents energy and "on" states.
- **Secondary (#B794FF):** A soft lavender used to balance the intensity of the lime. This is ideal for secondary actions, accents, and categorization.
- **Tertiary (#1E1B4B):** A deep indigo used for surface elevations and container backgrounds to provide depth without resorting to pure blacks.
- **Neutral (#0F172A):** The foundation charcoal-navy, providing a stable, professional backdrop that makes the accent colors vibrate.

Background surfaces should use a hierarchy of the Neutral and Tertiary tones to create a sense of depth and focus.

## Typography

This design system utilizes **Inter** exclusively to maintain a systematic and utilitarian feel. The type scale is optimized for high-density information displays. 

Headlines should use tighter letter spacing and heavier weights to command attention against the dark background. Body text maintains a generous line height (1.5x - 1.6x) to ensure readability. Labels and UI metadata are set with increased tracking to improve scanning speed at smaller sizes.

## Layout & Spacing

The layout follows a **Fluid Grid** model with a 12-column structure for desktop and a 4-column structure for mobile. 

- **Rhythm:** An 8px base unit drives all spacing decisions.
- **Margins:** Page margins are generous (32px on desktop) to reinforce the feeling of clarity and focus.
- **Density:** Components within cards use a "tight-stack" approach (8px-12px) to keep related data points grouped, while larger sections are separated by significant "breathing room" (32px-48px).

## Elevation & Depth

Hierarchy is established through **Tonal Layering** and **Subtle Elevations**. Instead of heavy shadows, depth is achieved by stepping up the brightness of the background color:

1.  **Level 0 (Base):** #0F172A (The main application background).
2.  **Level 1 (Cards/Containers):** #1E1B4B (Tertiary color, used for primary content blocks).
3.  **Level 2 (Popovers/Modals):** A slightly lighter tint of the Tertiary color with a very soft, 15% opacity black shadow (20px blur).

A 1px "inner glow" or subtle border (#FFFFFF at 5% opacity) can be used on Level 1 containers to define edges without adding visual weight.

## Shapes

The shape language is consistently **Rounded**, using a 16px (1rem) radius for primary cards and 12px (0.75rem) for buttons and input fields. This softened geometry contrasts against the sharp, technical colors to make the professional tool feel accessible and human. 

Interactive elements like tags and status indicators may utilize "Pill" shapes (full radius) to distinguish them from structural layout components.

## Components

- **Buttons:** Primary buttons use a solid #BEFF00 fill with dark text. Secondary buttons use an outlined style or a subtle #B794FF ghost fill. All buttons feature a 12px radius.
- **Inputs:** Text fields use a dark Tertiary background with a 1px border that glows Primary Lime on focus.
- **Cards:** Use #1E1B4B backgrounds. Content should have 24px internal padding.
- **Navigation:** Use high-contrast icons (Primary or Secondary colors) on the dark background. The "Active" state should be indicated by a solid Primary color block or glow.
- **Chips/Labels:** Small, high-contrast badges used for status or categorization. Text inside should be uppercase with 0.05em tracking for clarity.