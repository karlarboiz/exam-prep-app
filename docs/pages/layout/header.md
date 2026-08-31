# Header layout

**Path:** `WEB-INF/jsp/layout/header.jsp`

Site header: logo, destination nav, account cluster. Included by content JSPs. Styles: `.site-header`, `.logo`, `.header-nav`, `.nav-links`, `.account-menu`, `.nav-toggle`.

The header splits chrome: destination links sit after the logo; the right side is a compact account pill (avatar initial, username, role, icon logout). The avatar chip links to **Account** (`/account`).

Admin nav includes Dashboard, Subjects, Questions, Exams, Users, **Access** (access grants), and **Integrity**.
User nav includes Dashboard, **Study plan**, and History, each with a small inline icon. The current route uses `.is-active`.

## Mobile navigation

At viewports **≤900px**, the horizontal nav collapses behind a hamburger control (`.nav-toggle`). Tapping the toggle opens `.header-nav` as a full-width stacked panel (`.header-nav.is-open`, `flex: 0 0 100%` so it wraps under the logo). The toggle uses `aria-expanded` / `aria-controls` for accessibility. Desktop (≥901px) keeps the inline nav and hides the toggle. On mobile, logout shows an icon plus a **Log out** label.

Toggle behavior lives in `js/nav-toggle.js` (same-origin file) so it runs under CSP `script-src 'self'` without an inline script.
