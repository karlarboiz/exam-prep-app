# Rate Limiting

Sliding window rate limiting for API endpoints to prevent brute force attacks and DoS.

## Overview

API endpoints are protected by rate limiting based on client IP address. The system uses a sliding window algorithm with configurable limits.

## Components

### RateLimiter

Core rate limiting utility using token bucket algorithm.

**Constructor:** `RateLimiter(maxRequests, windowMillis)`

**Key Methods:**
- `tryAcquire(String key)` - Attempts to acquire a token for the given key (returns true if allowed)
- `getRemainingRequests(String key)` - Gets remaining requests in the current window
- `cleanup()` - Removes expired buckets (called periodically)

**Algorithm:** Token bucket with sliding window refill

### ApiRateLimitFilter

Servlet filter that enforces rate limits on API endpoints.

**Protected Endpoints:**
- `/api/access-tokens` (POST)
- `/api/access-tokens/revoke` (POST)

**Rate Limit Key:** Client IP address (supports `X-Forwarded-For` header)

**Filter Order:** Applied early, after `CharacterEncodingFilter`, before `JwtAuthFilter`

## Configuration

**app.properties:**
```properties
# Maximum requests per window
rate.limit.api.max.requests=10

# Window size in minutes
rate.limit.api.window.minutes=1
```

**Defaults:**
- 10 requests per minute per IP address
- 5-minute cleanup interval for expired buckets

## Response Headers

**X-RateLimit-Remaining:** Number of remaining requests in current window

## Error Response

**Status:** 429 Too Many Requests

**Body:**
```json
{
  "error": "Rate limit exceeded. Please try again later."
}
```

## Production Tuning

Adjust limits based on legitimate usage patterns:

**Conservative (High Security):**
```properties
rate.limit.api.max.requests=5
rate.limit.api.window.minutes=1
```

**Moderate:**
```properties
rate.limit.api.max.requests=10
rate.limit.api.window.minutes=1
```

**Generous:**
```properties
rate.limit.api.max.requests=30
rate.limit.api.window.minutes=1
```

## Implementation Details

**Thread Safety:** Uses `ConcurrentHashMap` and `AtomicLong` for lock-free operations

**Memory Management:** Automatic cleanup of expired buckets every 5 minutes

**IP Detection:** Checks `X-Forwarded-For` header (first IP if comma-separated) or falls back to `request.getRemoteAddr()`

## Security Notes

- Limits are per IP address, not per user or session
- Bypassing via IP rotation is possible but rate limiting still provides DoS protection
- API key authentication provides additional security layer
- Consider adding user-level rate limits for authenticated requests if needed
