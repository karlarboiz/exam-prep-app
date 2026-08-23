# Header layout

**Path:** `WEB-INF/jsp/layout/header.jsp`

Site header: logo, **Tagalog / English** switcher (always visible), main nav, optional user badge. Included by content JSPs. Styles: `.site-header`, `.logo`, `.header-end`, `.lang-switch`, `.main-nav`, `.nav-toggle`.

The switcher posts to `/locale` with CSRF and a same-page `returnTo`. Default language is Tagalog; see [i18n](../../features/i18n/overview.md). Page titles come from `pageTitleKey` + `fmt:message`.

Admin nav includes Dashboard, Subjects, Questions, Exams, Users, **Access grants**, and **Integrity**.
User nav includes Dashboard, **Study plan**, and History.
Both roles include **Account** (`/account`) before the user badge.

## Mobile navigation

At viewports **≤900px**, the horizontal nav collapses behind a hamburger control (`.nav-toggle`). Tapping the toggle opens `.main-nav` as a full-width stacked panel (`.main-nav.is-open`). The toggle uses `aria-expanded` / `aria-controls` for accessibility; choosing a nav link closes the menu. Desktop (≥901px) keeps the inline nav and hides the toggle.
