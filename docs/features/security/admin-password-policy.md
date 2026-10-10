# Admin Password Policy

Environment-based admin credentials for secure production deployments.

## Overview

Admin account credentials can be configured via environment variables to avoid hardcoded default passwords in production.

## Configuration

### Development

Default credentials are used if environment variables are not set:
- Username: `admin`
- Password: `admin123`

A warning is printed to stderr on startup.

### Production

**Required Environment Variables:**

```bash
ENVIRONMENT=production
ADMIN_PASSWORD=<secure-password>
```

**Optional:**
```bash
ADMIN_USERNAME=<custom-username>
```

If `ADMIN_PASSWORD` is not set in production mode, the application will fail to start with:
```
IllegalStateException: Admin password must be set via ADMIN_PASSWORD environment variable in production
```

## Deployment Examples

### Docker

```bash
docker run -e ENVIRONMENT=production \
           -e ADMIN_PASSWORD='your-secure-password' \
           -e ADMIN_USERNAME='sysadmin' \
           exam-prep-app
```

### Kubernetes

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: exam-prep-secrets
type: Opaque
stringData:
  admin-password: your-secure-password
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: exam-prep-app
spec:
  template:
    spec:
      containers:
      - name: app
        env:
        - name: ENVIRONMENT
          value: "production"
        - name: ADMIN_PASSWORD
          valueFrom:
            secretKeyRef:
              name: exam-prep-secrets
              key: admin-password
        - name: ADMIN_USERNAME
          value: "sysadmin"
```

### Systemd Service

```ini
[Service]
Environment="ENVIRONMENT=production"
Environment="ADMIN_PASSWORD=your-secure-password"
Environment="ADMIN_USERNAME=sysadmin"
ExecStart=/opt/exam-prep/bin/start.sh
```

## Security Best Practices

1. **Never commit passwords** to version control
2. **Use strong passwords** (16+ characters, mixed case, numbers, symbols)
3. **Rotate credentials** regularly in production
4. **Use secrets management** (Kubernetes Secrets, AWS Secrets Manager, HashiCorp Vault)
5. **Audit admin access** regularly

## Implementation

**SeedData.seedAdminIfMissing():**
- Checks if admin user already exists
- Reads `admin.username` and `admin.password` from AppConfig
- In production mode, requires `admin.password` to be set
- Hashes password with BCrypt before storing

**AppConfig Environment Variables:**
- `ADMIN_USERNAME` → `admin.username`
- `ADMIN_PASSWORD` → `admin.password`

## Future Enhancements

Potential improvements (not yet implemented):
- Force password change on first login
- Password complexity requirements
- Account lockout after failed login attempts
- Two-factor authentication for admin accounts
- Audit logging for admin actions
