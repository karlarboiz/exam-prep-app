# CSRF Protection

Cross-Site Request Forgery (CSRF) protection for state-changing operations.

## Overview

CSRF tokens are required for all POST, PUT, PATCH, and DELETE requests (except public API endpoints). The system generates a unique token per session and validates it on every state-changing request.

## Components

### CsrfUtil

Token generation and validation utility.

**Key Methods:**
- `getToken(HttpServletRequest)` - Gets or generates a CSRF token for the session
- `validateToken(HttpServletRequest)` - Validates token from request parameter or `X-CSRF-Token` header
- `isExemptMethod(HttpServletRequest)` - Checks if HTTP method is exempt (GET, HEAD, OPTIONS, TRACE)

**Token Storage:** Session attribute `_csrf_token`

### CsrfFilter

Servlet filter that validates CSRF tokens on non-exempt requests.

**Exempt Paths:**
- `/api/access-tokens` - Uses API key authentication
- `/api/access-tokens/revoke` - Uses API key authentication

**Filter Order:** Applied after `JwtAuthFilter`, before `SubscriptionFilter`

### JSP Tag

Use `<ep:csrf/>` in forms to include the CSRF token as a hidden field.

**Example:**
```jsp
<%@ taglib prefix="ep" uri="http://examprep.com/tags" %>

<form method="post" action="${ctx}/user/exam/submit">
    <ep:csrf/>
    <!-- other form fields -->
    <button type="submit">Submit</button>
</form>
```

## AJAX Requests

For AJAX/fetch requests, include the token in the `X-CSRF-Token` header:

```javascript
const token = document.querySelector('input[name="_csrf"]').value;

fetch('/user/exam/submit', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'X-CSRF-Token': token
    },
    body: JSON.stringify(data)
});
```

## Error Handling

**Invalid/Missing Token:** Returns HTTP 403 Forbidden

## Implementation Checklist

- [x] CsrfUtil for token generation and validation
- [x] CsrfFilter servlet filter
- [x] web.xml filter configuration
- [x] JSP tag file `/WEB-INF/tags/csrf.tag`
- [x] TLD registration in `examprep.tld`
- [ ] Update existing forms to include `<ep:csrf/>` tag
- [ ] Test CSRF protection on all state-changing endpoints

## Security Notes

- Tokens are URL-safe Base64 encoded (32 random bytes)
- Validation uses constant-time comparison via `SecurityUtil.constantTimeEquals`
- GET requests are never validated (idempotent operations only)
- API endpoints use API key authentication instead of CSRF tokens
