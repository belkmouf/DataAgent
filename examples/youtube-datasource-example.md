# YouTube Data Source Example

This example demonstrates how to connect to the YouTube channel @NateBJones and analyze its data.

## Step 1: Add YouTube Data Source

Via API:
```bash
curl -X POST http://localhost:8080/api/datasource \
  -H "Content-Type: application/json" \
  -d '{
    "name": "NateBJones Channel",
    "type": "youtube",
    "username": "YOUR_YOUTUBE_API_KEY",
    "databaseName": "@NateBJones",
    "description": "Analytics for NateBJones YouTube channel"
  }'
```

Or via the DataAgent UI:
1. Navigate to Data Sources
2. Click "Add Data Source"
3. Select type: "YouTube"
4. Fill in the fields:
   - Name: "NateBJones Channel"
   - API Key (username field): Your YouTube Data API v3 key
   - Channel Handle (database name field): "@NateBJones"
5. Click "Test Connection"
6. Click "Save"

## Step 2: Query Channel Information

```sql
SELECT * FROM channel_info;
```

Expected result:
| id | title | subscriber_count | video_count | view_count |
|----|-------|------------------|-------------|------------|
| UCxxx... | Channel Name | 50000 | 234 | 5000000 |

## Step 3: Find Top Videos

```sql
SELECT title, view_count, like_count, comment_count
FROM video_details
ORDER BY view_count DESC
LIMIT 10;
```

## Step 4: Analyze Engagement

```sql
SELECT
    title,
    view_count,
    like_count,
    comment_count,
    (like_count * 100.0 / NULLIF(view_count, 0)) as engagement_rate
FROM video_details
WHERE view_count > 0
ORDER BY engagement_rate DESC
LIMIT 10;
```

## Step 5: Track Publishing Frequency

```sql
SELECT
    DATE(published_at) as publish_date,
    COUNT(*) as videos_published
FROM videos
GROUP BY DATE(published_at)
ORDER BY publish_date DESC
LIMIT 30;
```

## Advanced: Multi-Channel Comparison

If you have multiple YouTube channels configured:

```sql
-- Compare subscriber growth across channels
SELECT
    c1.title as channel_name,
    c1.subscriber_count,
    c1.video_count,
    (c1.subscriber_count / NULLIF(c1.video_count, 0)) as subscribers_per_video
FROM channel_info c1
ORDER BY subscribers_per_video DESC;
```

## API Quota Management

YouTube Data API v3 has daily quotas. Each operation costs:
- `channel_info` query: ~3-5 units
- `videos` query: ~100 units (for 50 videos)
- `video_details` query: ~100 units (for 50 videos)

Default daily quota: 10,000 units

## Notes

- The YouTube integration is read-only
- Data is fetched in real-time from YouTube API
- Maximum 50 videos returned per query (YouTube API limitation)
- Channel handle must include @ symbol (e.g., @NateBJones)
