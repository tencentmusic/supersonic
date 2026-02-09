# API Migration Guide: DataSync to Connection

## Overview

The DataSync API (`/api/v1/dataSyncConfigs`) is deprecated and will be removed in version 2.0. Please migrate to the new Connection API (`/api/v1/connections`).

**Sunset Date:** 2026-06-01

## Why Migrate?

The new Connection model provides:

| Feature | Old DataSync | New Connection |
|---------|--------------|----------------|
| Lifecycle Management | `enabled: true/false` | `status: ACTIVE/PAUSED/BROKEN/DEPRECATED` |
| Schema Detection | Not supported | Auto-detection with breaking change alerts |
| Event Timeline | Not supported | Full audit trail of all operations |
| State Management | Per-config watermark | Per-stream watermarks with reset capability |
| Quartz Integration | Basic | Full lifecycle management |

## API Endpoint Mapping

| Old Endpoint | New Endpoint | Notes |
|--------------|--------------|-------|
| `POST /api/v1/dataSyncConfigs` | `POST /api/v1/connections` | Create new connection |
| `GET /api/v1/dataSyncConfigs` | `GET /api/v1/connections` | List all connections |
| `GET /api/v1/dataSyncConfigs/{id}` | `GET /api/v1/connections/{id}` | Get by ID |
| `PATCH /api/v1/dataSyncConfigs/{id}` | `PATCH /api/v1/connections/{id}` | Update |
| `DELETE /api/v1/dataSyncConfigs/{id}` | `DELETE /api/v1/connections/{id}` | Delete |
| `POST /api/v1/dataSyncConfigs/{id}:trigger` | `POST /api/v1/connections/{id}:sync` | Trigger sync |
| `POST /api/v1/dataSyncConfigs/{id}:pause` | `POST /api/v1/connections/{id}:pause` | Pause |
| `POST /api/v1/dataSyncConfigs/{id}:resume` | `POST /api/v1/connections/{id}:resume` | Resume |
| `GET /api/v1/dataSyncConfigs/{id}/executions` | `GET /api/v1/connections/{id}/jobs` | Execution history |
| `POST /api/v1/dataSyncConfigs/{id}:discoverSchema` | `POST /api/v1/connections/{id}:discoverSchema` | Schema discovery |

### New Endpoints (Connection-only)

| Endpoint | Description |
|----------|-------------|
| `POST /api/v1/connections/{id}:deprecate` | Mark connection as deprecated |
| `POST /api/v1/connections/{id}:resetState` | Reset sync watermarks |
| `GET /api/v1/connections/{id}/schemaChanges` | Detect schema changes |
| `POST /api/v1/connections/{id}:applySchemaChanges` | Apply discovered schema |
| `GET /api/v1/connections/{id}/timeline` | Event timeline |

## Field Mapping

### DataSyncConfigDO → ConnectionDO

| DataSyncConfig Field | Connection Field | Type Change |
|---------------------|------------------|-------------|
| `id` | `id` | Same |
| `name` | `name` | Same |
| `source_database_id` | `sourceDatabaseId` | Same (camelCase) |
| `target_database_id` | `destinationDatabaseId` | Renamed |
| `enabled` | `status` | `true` → `ACTIVE`, `false` → `PAUSED` |
| `sync_config` (JSON) | `configuredCatalog` (JSON) | Same format |
| `cron_expression` | `cronExpression` | Same |
| `retry_count` | `retryCount` | Same |
| `quartz_job_key` | `quartzJobKey` | Same |
| - | `schemaChangeStatus` | New field |
| - | `discoveredCatalog` | New field |
| - | `state` | New field (per-stream watermarks) |

### Status Values

| DataSyncConfig | Connection |
|----------------|------------|
| `enabled=true` | `status=ACTIVE` |
| `enabled=false` | `status=PAUSED` |
| - | `status=BROKEN` (new: sync failure state) |
| - | `status=DEPRECATED` (new: soft-delete state) |

## Request/Response Examples

### Old: Create DataSyncConfig

```json
POST /api/v1/dataSyncConfigs
{
  "name": "My Sync",
  "sourceDatabaseId": 1,
  "targetDatabaseId": 2,
  "enabled": true,
  "cronExpression": "0 0 2 * * ?",
  "syncConfig": {
    "tables": [
      {"source_table": "users", "target_table": "users", "sync_mode": "FULL"}
    ]
  }
}
```

### New: Create Connection

```json
POST /api/v1/connections
{
  "name": "My Sync",
  "sourceDatabaseId": 1,
  "destinationDatabaseId": 2,
  "scheduleType": "CRON",
  "cronExpression": "0 0 2 * * ?",
  "configuredCatalog": "{\"streams\":[{\"streamName\":\"users\",\"syncMode\":\"FULL\",\"selected\":true}]}"
}
```

## Migration Steps

### 1. Automatic Data Migration

When upgrading to version 1.5+, existing `s2_data_sync_config` records are automatically migrated to `s2_connection`:

```sql
-- Migration runs automatically via Flyway V18__connection_model.sql
-- Existing execution records are linked to new connections
```

### 2. Update API Calls

Replace all API calls in your application:

```typescript
// Before
const response = await fetch('/api/v1/dataSyncConfigs', { method: 'POST', ... });

// After
const response = await fetch('/api/v1/connections', { method: 'POST', ... });
```

### 3. Update Field Names

```typescript
// Before
const config = {
  targetDatabaseId: 2,
  enabled: true,
  syncConfig: '...'
};

// After
const connection = {
  destinationDatabaseId: 2,  // renamed
  status: 'ACTIVE',          // enabled → status
  configuredCatalog: '...'   // renamed
};
```

### 4. Handle New Features

Take advantage of new Connection features:

```typescript
// Schema change detection
const changes = await fetch(`/api/v1/connections/${id}/schemaChanges`);

// Event timeline
const timeline = await fetch(`/api/v1/connections/${id}/timeline`);

// Reset state for specific streams
await fetch(`/api/v1/connections/${id}:resetState`, {
  method: 'POST',
  body: JSON.stringify({ streams: ['users', 'orders'] })
});
```

## Deprecation Headers

Old API responses include these headers:

```
Deprecation: true
Sunset: 2026-06-01
X-Deprecation-Warning: This API is deprecated. Please migrate to /api/v1/connections.
Link: </api/v1/connections>; rel="successor-version"
```

## FAQ

### Q: Will my existing sync jobs continue to work?

Yes. The old API remains functional until version 2.0. However, we strongly recommend migrating before the sunset date.

### Q: What happens to my execution history?

Execution records are automatically linked to the new Connection model. You can view them via `/api/v1/connections/{id}/jobs`.

### Q: Can I use both APIs during migration?

Yes, but be aware that:
- Creating via old API creates both DataSyncConfig and Connection records
- Deleting via new API only affects Connection records
- We recommend completing migration quickly to avoid inconsistency

### Q: How do I handle schema changes?

The new Connection model automatically detects schema changes:
1. Check `schemaChangeStatus` field (NO_CHANGE, NON_BREAKING, BREAKING)
2. For BREAKING changes, review and apply via `:applySchemaChanges` endpoint
3. Use `:resetState` if you need to re-sync from scratch

## Support

For migration assistance, please contact the SuperSonic team or open an issue on GitHub.
