# Admin access grants page

**Path:** `WEB-INF/jsp/admin/access-grants.jsp`  
**Route:** `/admin/access-grants`  
**Feature:** [admin-access-grants](../../features/access-grants/admin-access-grants.md)

Mint form (exam level, duration days, optional plan/source) plus a table of access grants with status badges (`.badge-UNUSED` / `.badge-REDEEMED` / `.badge-REVOKED`) and a Revoke action for non-revoked rows. After create, the raw token and register link are shown once (`.alert-success` + `.token-readonly`). Linked from the admin header nav.
