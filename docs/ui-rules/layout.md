# Layout

Source stylesheet: `src/main/webapp/css/app.css`

## Page shell

- `body` is a column flex layout (`min-height: 100vh`).
- `.main-content` grows (`flex: 1`); footer stays at the bottom.
- Content width: `.container` max-width **1100px**, horizontal padding `1.5rem`.

## Regions

| Class | Role |
|-------|------|
| `.site-header` | Top bar with logo, destination nav, and account cluster |
| `.header-inner` | Flex header row (wraps on small screens) |
| `.logo` / `.logo-mark` | Wordmark plus small book icon |
| `.nav-toggle` | Hamburger control; visible only ≤900px |
| `.header-nav` | Destination links + account cluster; stacked panel when `.is-open` on mobile |
| `.nav-links` | Primary destinations (active item uses `.is-active`) |
| `.account-menu` | Avatar chip + logout control on the right |
| `.main-content` | Page body |
| `.site-footer` | Centered muted footer |
| `.grid-2` | Two columns; stacks at ≤768px |
| `.stats-grid` | Four-column dashboard stats |
| `.exam-grid` | Responsive exam cards (`minmax(260px, 1fr)`) |

## Breakpoints

| Max width | Behavior |
|-----------|----------|
| **900px** | Header switches to hamburger + collapsible `.header-nav` (`flex: 0 0 100%` so the panel wraps under the logo) |
| **768px** | `.grid-2` stacks to one column |

## Shared chrome

Include [header](../pages/layout/header.md) and [footer](../pages/layout/footer.md) on app pages. See [header](../pages/layout/header.md) for mobile nav details.
