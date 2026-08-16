# Layout

Source stylesheet: `src/main/webapp/css/app.css`

## Page shell

- `body` is a column flex layout (`min-height: 100vh`).
- `.main-content` grows (`flex: 1`); footer stays at the bottom.
- Content width: `.container` max-width **1100px**, horizontal padding `1.5rem` (tightens to `1rem` at ≤768px, plus safe-area insets).

## Regions

| Class | Role |
|-------|------|
| `.site-header` | Top bar with logo + nav |
| `.header-inner` | Flex space-between header content (wraps on small screens) |
| `.nav-toggle` | Hamburger control; visible only ≤900px |
| `.main-nav` | Inline links on desktop; stacked panel when `.is-open` on mobile |
| `.main-content` | Page body |
| `.site-footer` | Centered muted footer |
| `.grid-2` | Two columns; stacks at ≤768px |
| `.stats-grid` | Dashboard stats; `auto-fit` columns, 2-up on small screens |
| `.exam-grid` | Responsive exam cards (`minmax(min(100%, 16rem), 1fr)`) |

## Overflow

Grid and flex children use `min-width: 0` so wide tables, tokens, and question text cannot force the page wider than the viewport. `.card` scrolls horizontally when a table is still too wide. `html` / `body` clip leftover horizontal overflow.

## Breakpoints

| Max width | Behavior |
|-----------|----------|
| **900px** | Header switches to hamburger + collapsible `.main-nav` |
| **768px** | `.grid-2` stacks; tighter padding; timers, score summary, and exam nav stack; tables scroll inside cards |
| **480px** | Compact stat values and full-width large buttons |

## Shared chrome

Include [header](../pages/layout/header.md) and [footer](../pages/layout/footer.md) on app pages. See [header](../pages/layout/header.md) for mobile nav details.
