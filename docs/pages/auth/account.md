# Account page

**Path:** `WEB-INF/jsp/auth/account.jsp`  
**Route:** `/account`  
**Features:** [profile](../../features/auth/profile.md), [change password](../../features/auth/change-password.md)

Available to any authenticated role from the header account chip (avatar + username).

## Profile

First `.card` is a form (`action=profile`) with `.grid-2` fields: editable username and email, read-only role and exam level (`.token-readonly`). Exam level shows `—` for admins. Current password confirms the save. `<ep:csrf/>` and a `.btn-primary` **Save profile**. Labels follow the header language switcher.

## Change password

Second `.card` with `action=password`, current / new / confirm fields, `<ep:csrf/>`, and a `.btn-primary` **Update password**. Errors use `.alert-error`; success after redirect uses `.alert-success` (`?profile=1` or `?changed=1`).
