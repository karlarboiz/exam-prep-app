# UI language (Tagalog / English)

**Actors:** anyone (login/register) and signed-in ADMIN / USER  
**Routes:** `POST /locale` (public), header switcher on every page  
**Default:** Tagalog (`tl`)

The chrome, labels, buttons, empty states, and student-facing errors can be shown in **Tagalog** or **English**. Question stems, options, explanations, subject names, and exam titles stay as authored (CSE verbal items may already be Filipino in the bank).

## How locale is chosen

1. `locale` cookie (`tl` or `en`) if present
2. Else the signed-in user’s saved `users.locale`
3. Else Tagalog

`LocaleFilter` runs after `JwtAuthFilter` so it can read `currentUser`. It sets JSTL `fmt` locale and a UTF-8 `messages` bundle.

`POST /locale` (CSRF-protected) writes the cookie for one year and, when signed in, updates `users.locale`. It redirects back to `returnTo` (same-origin relative path only).

## Bundles

| File | Language |
|------|----------|
| `src/main/resources/messages.properties` | English fallback |
| `src/main/resources/messages_tl.properties` | Tagalog |

JSP uses `<fmt:message>`. Servlets use `Messages.get` / `Messages.fromException` so login, register, and other mapped errors follow the same locale.

## Out of scope

- Translating question-bank content or weekly email bodies
- Automatic browser `Accept-Language` detection (cookie / saved preference only)
