# YouTube Data Source Integration

This guide explains how to integrate YouTube channels as data sources in DataAgent.

## Overview

The YouTube integration allows you to analyze YouTube channel data including:
- Channel statistics (subscribers, views, video count)
- Video information (title, description, publication date)
- Video analytics (views, likes, comments)

## Prerequisites

1. **YouTube Data API v3 Key**: You need a YouTube Data API v3 key from Google Cloud Console
   - Go to [Google Cloud Console](https://console.cloud.google.com/)
   - Create a new project or select an existing one
   - Enable the YouTube Data API v3
   - Create credentials (API Key)
   - Copy the API key

## Configuration

### Adding a YouTube Data Source

When adding a YouTube data source in DataAgent, use the following field mappings:

| Field | Description | Example |
|-------|-------------|---------|
| **Type** | Data source type | `youtube` |
| **Name** | Friendly name for the datasource | `NateBJones Channel` |
| **Username** | Your YouTube Data API key | `AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXX` |
| **Password** | Not used (leave empty) | `` |
| **Database Name** | YouTube channel handle | `@NateBJones` |
| **Host** | Not used (leave empty) | `` |
| **Port** | Not used (leave empty) | `` |

### Example Configuration

```json
{
  "name": "NateBJones Channel Analytics",
  "type": "youtube",
  "username": "YOUR_YOUTUBE_API_KEY_HERE",
  "databaseName": "@NateBJones",
  "description": "Analytics for NateBJones YouTube channel"
}
```

## Available Tables

The YouTube connector provides three virtual tables:

### 1. channel_info

Contains channel-level statistics and information.

**Columns:**
- `id` (VARCHAR) - Channel ID
- `title` (VARCHAR) - Channel title
- `description` (TEXT) - Channel description
- `custom_url` (VARCHAR) - Channel custom URL
- `published_at` (TIMESTAMP) - Channel creation date
- `subscriber_count` (BIGINT) - Number of subscribers
- `video_count` (BIGINT) - Total number of videos
- `view_count` (BIGINT) - Total view count

**Example Query:**
```sql
SELECT title, subscriber_count, video_count, view_count
FROM channel_info;
```

### 2. videos

Contains information about recent videos from the channel.

**Columns:**
- `video_id` (VARCHAR) - Unique video ID
- `title` (VARCHAR) - Video title
- `description` (TEXT) - Video description
- `published_at` (TIMESTAMP) - Video publication date
- `thumbnail_url` (VARCHAR) - Thumbnail image URL

**Example Query:**
```sql
SELECT video_id, title, published_at
FROM videos
ORDER BY published_at DESC
LIMIT 10;
```

### 3. video_details

Contains detailed analytics for each video.

**Columns:**
- `video_id` (VARCHAR) - Unique video ID
- `title` (VARCHAR) - Video title
- `view_count` (BIGINT) - Number of views
- `like_count` (BIGINT) - Number of likes
- `comment_count` (BIGINT) - Number of comments
- `duration` (VARCHAR) - Video duration (ISO 8601 format)
- `published_at` (TIMESTAMP) - Publication date

**Example Query:**
```sql
SELECT title, view_count, like_count, comment_count
FROM video_details
ORDER BY view_count DESC
LIMIT 10;
```

## Example Queries

### Get channel overview
```sql
SELECT * FROM channel_info;
```

### Find most viewed videos
```sql
SELECT title, view_count, like_count
FROM video_details
ORDER BY view_count DESC
LIMIT 5;
```

### Calculate engagement rate
```sql
SELECT
    title,
    view_count,
    like_count,
    comment_count,
    (like_count * 100.0 / view_count) as engagement_rate
FROM video_details
WHERE view_count > 0
ORDER BY engagement_rate DESC
LIMIT 10;
```

### Recent videos
```sql
SELECT title, published_at
FROM videos
ORDER BY published_at DESC
LIMIT 20;
```

## API Quotas

Be aware of YouTube Data API v3 quotas:
- Default quota: 10,000 units per day
- Each API call costs different units (typically 1-100 units)
- Monitor your usage in Google Cloud Console

## Troubleshooting

### Common Issues

1. **Connection Failed**
   - Verify your API key is correct
   - Ensure YouTube Data API v3 is enabled in Google Cloud Console
   - Check if your API key has the necessary permissions

2. **Channel Not Found**
   - Verify the channel handle is correct (e.g., `@NateBJones`)
   - Ensure the channel is public

3. **Quota Exceeded**
   - Wait until the daily quota resets (midnight Pacific Time)
   - Request a quota increase in Google Cloud Console

## Limitations

- Read-only access (no write operations)
- Limited to public channel data
- Subject to YouTube API quotas
- Maximum 50 videos returned per query (API limitation)

## Security Notes

- Store API keys securely
- Never commit API keys to version control
- Use environment variables or secure vaults for production deployments
- Regularly rotate API keys

## Support

For issues or questions:
- Check the [YouTube Data API documentation](https://developers.google.com/youtube/v3)
- Review API quota limits
- Verify channel handle format
