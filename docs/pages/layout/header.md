# Header layout

**Path:** `WEB-INF/jsp/layout/header.jsp`

Site header: logo, main nav, optional user badge. Included by content JSPs. Styles: `.site-header`, `.logo`, `.main-nav`, `.nav-toggle`.

Admin nav includes Dashboard, Subjects, Questions, Exams, Users, and **Access grants**.
User nav includes Dashboard and History.
Both roles include **Account** (`/account`) before the user badge.

## Mobile navigation

At viewports **≤900px**, the horizontal nav collapses behind a hamburger control (`.nav-toggle`). Tapping the toggle opens `.main-nav` as a full-width stacked panel (`.main-nav.is-open`). The toggle uses `aria-expanded` / `aria-controls` for accessibility; choosing a nav link closes the menu. Desktop (≥901px) keeps the inline nav and hides the toggle.
