# Account page

**Path:** `WEB-INF/jsp/auth/account.jsp`  
**Route:** `/account`  
**Feature:** [change password](../../features/auth/change-password.md)

Available to any authenticated role from the header account chip (avatar + username).

## Profile

Read-only `.card` with `.grid-2` fields (username, email, role, exam level). Exam level shows `—` for admins. Fields use `.token-readonly`. Username and email are not editable. Labels follow the header language switcher.

## Change password

Second `.card` with current / new / confirm fields and a `.btn-primary` **Update password**. Errors use `.alert-error`; success after redirect uses `.alert-success`.
