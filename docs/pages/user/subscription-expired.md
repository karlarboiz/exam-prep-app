# Access ended page

**Path:** `WEB-INF/jsp/user/subscription-expired.jsp`  
**Route:** `/user/subscription-expired`  
**Servlet:** `SubscriptionExpiredServlet`  
**Feature:** [enforce-access](../../features/access-grants/enforce-access.md)

Shown when a logged-in `USER` reaches `/user/**` without an active access grant.

## Messaging

- Title: **Access ended** (one-time purchase, not a renewable subscription).
- Shows end date when a past redeemed grant exists.
- Explains that access typically covers prep plus a short post-exam review window (e.g. 3 days after the exam), then ends permanently.
- Includes logout link. Uses `.expired-panel` and `.alert-warning`.

Renewal / re-linking a new token to this account is intentionally not offered.
