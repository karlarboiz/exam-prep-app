# Subscription expired page

**Path:** `WEB-INF/jsp/user/subscription-expired.jsp`  
**Route:** `/user/subscription-expired`  
**Servlet:** `SubscriptionExpiredServlet`  
**Feature:** [enforce-access](../../features/access-grants/enforce-access.md)

Shown when a logged-in `USER` reaches `/user/**` without an active access grant. Displays end date when a past redeemed grant exists. Includes logout link. Uses `.expired-panel` and `.alert-warning`.
