---
status: active
module: common/storage, headless/server
audience: [developer, ops]
last-updated: 2026-04-17
---

# FileStorage Migration Runbook

## Overview

SuperSonic's export/report artifacts are written through a pluggable `FileStorage` SPI.
Three backends: `local` (default), `oss` (Aliyun OSS), `s3` (AWS S3 or MinIO).
Selected via `s2.storage.type` in `application.yaml` or env var `S2_STORAGE_TYPE`.

---

## Deploying each backend

### Local (default, dev / single-instance)

```yaml
s2:
  storage:
    type: local
    local:
      root-dir: /var/lib/supersonic/exports
```

Single-instance only (or shared NFS mount). No credentials needed.

### Aliyun OSS

```yaml
s2:
  storage:
    type: oss
    prefix: exports
    oss:
      endpoint: https://oss-cn-hangzhou.aliyuncs.com
      bucket: supersonic-prod
      access-key-id: ${S2_STORAGE_OSS_AK}
      access-key-secret: ${S2_STORAGE_OSS_SK}
```

Provision the bucket with a lifecycle rule deleting objects under `exports/` after 30 days.

### AWS S3 / MinIO

```yaml
s2:
  storage:
    type: s3
    prefix: exports
    s3:
      region: us-east-1
      bucket: supersonic-prod
      access-key: ${S2_STORAGE_S3_AK}
      secret-key: ${S2_STORAGE_S3_SK}
      # For MinIO or non-AWS S3-compatible endpoints:
      # endpoint: https://minio.example.com
      # path-style: true
```

IAM policy: grant `s3:GetObject`, `s3:PutObject`, `s3:DeleteObject` on
`arn:aws:s3:::supersonic-prod/exports/*`.

---

## Migration from pre-SPI deployments

Prior to this change, `s2_export_task.file_location` stored absolute filesystem paths
like `/tmp/supersonic-export/export_42_20260417103000.xlsx`. After deploying:

- **New rows** use storage keys: `exports/7/20260417/42/export_42_20260417103000.xlsx`
- **Old rows** still reference absolute paths on the original instance.
  Downloads from other instances will fail — accept the failure, let rows expire
  via `ExportFileCleanupTask` (T+7 days by default).

No data migration script is required.

---

## Rollback

Switch back to local with zero downtime:

1. Set `S2_STORAGE_TYPE=local` in the environment.
2. Rolling-restart the fleet.
3. Any exports uploaded to the previous backend will fail on download; users retry and
   new exports land on local disk.

Full code rollback:

```bash
git revert <commit range for Tasks 1..9>
```

---

## Verification checklist (post-deploy)

- [ ] `GET /actuator/health` reports `UP`.
- [ ] Startup log shows: `FileStorage: selecting <Impl> (s2.storage.type=<type>)`.
- [ ] Submit an export via `POST /api/v1/exportTasks`, wait for `SUCCESS` status.
- [ ] `s2_export_task.file_location` matches `exports/<tenantId>/<yyyyMMdd>/<id>/<name>.{xlsx,csv}`.
- [ ] `GET /api/v1/exportTasks/{id}:download` returns 200 + bytes (local) or 302 + OSS/S3 URL (cloud).
- [ ] After 7 days, `ExportFileCleanupTask` (03:00) deletes the objects from the bucket.

---

## Metrics & alerts

Export success/failure surfaces in existing metrics. No new metrics added — flaky backends
show as elevated error rate in `s2_template_report_export_seconds{outcome="error"}`.
