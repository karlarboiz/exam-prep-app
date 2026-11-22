# HTTP security headers

**Filter:** `SecurityHeadersFilter` (first after encoding)

| Header | Value |
|--------|--------|
| Content-Security-Policy | `default-src 'self'` plus Google Fonts and `img-src` https/data |
| X-Content-Type-Options | `nosniff` |
| X-Frame-Options | `DENY` |
| Referrer-Policy | `no-referrer` (also limits token leakage from leftover `?token=` links) |
| Permissions-Policy | camera / microphone / geolocation off |
| Strict-Transport-Security | set when `WebUtil.isHttps` is true |

`proxy.trust.forwarded` must be true before `X-Forwarded-Proto` is trusted for HSTS and the Secure cookie flag.
