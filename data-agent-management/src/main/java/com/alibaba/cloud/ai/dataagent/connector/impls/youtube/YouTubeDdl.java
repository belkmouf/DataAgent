/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.dataagent.connector.impls.youtube;

import com.alibaba.cloud.ai.dataagent.bo.schema.*;
import com.alibaba.cloud.ai.dataagent.connector.ddl.Ddl;
import com.alibaba.cloud.ai.dataagent.connector.impls.youtube.YouTubeConnectionPool.YouTubeConnection;
import com.alibaba.cloud.ai.dataagent.enums.BizDataSourceTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.*;

/**
 * YouTube DDL implementation that defines virtual tables for YouTube data
 */
@Slf4j
@Service
public class YouTubeDdl implements Ddl {

	@Autowired
	private YouTubeApiClient apiClient;

	@Override
	public List<DatabaseInfoBO> showDatabases(Connection connection) {
		// For YouTube, we treat each channel as a "database"
		YouTubeConnection ytConn = (YouTubeConnection) connection;
		List<DatabaseInfoBO> databases = new ArrayList<>();

		String channelHandle = ytConn.getChannelHandle();
		if (channelHandle != null) {
			databases.add(DatabaseInfoBO.builder().name(channelHandle).build());
		}

		return databases;
	}

	@Override
	public List<SchemaInfoBO> showSchemas(Connection connection) {
		// YouTube doesn't have schemas
		return Collections.emptyList();
	}

	@Override
	public List<TableInfoBO> showTables(Connection connection, String schema, String tablePattern) {
		// Define virtual tables for YouTube data
		List<TableInfoBO> tables = new ArrayList<>();

		tables.add(TableInfoBO.builder()
			.name("channel_info")
			.description("YouTube channel information including statistics")
			.build());

		tables.add(TableInfoBO.builder()
			.name("videos")
			.description("Recent videos from the YouTube channel")
			.build());

		tables.add(TableInfoBO.builder()
			.name("video_details")
			.description("Detailed statistics for videos including views, likes, and comments")
			.build());

		return tables;
	}

	@Override
	public List<TableInfoBO> fetchTables(Connection connection, String schema, List<String> tables) {
		return showTables(connection, schema, null).stream()
			.filter(table -> tables.contains(table.getName()))
			.toList();
	}

	@Override
	public List<ColumnInfoBO> showColumns(Connection connection, String schema, String table) {
		List<ColumnInfoBO> columns = new ArrayList<>();

		switch (table.toLowerCase()) {
			case "channel_info":
				columns.add(ColumnInfoBO.builder()
					.name("id")
					.type("VARCHAR")
					.description("Channel ID")
					.primary(true)
					.notnull(true)
					.build());
				columns.add(ColumnInfoBO.builder()
					.name("title")
					.type("VARCHAR")
					.description("Channel title")
					.notnull(true)
					.build());
				columns.add(ColumnInfoBO.builder()
					.name("description")
					.type("TEXT")
					.description("Channel description")
					.build());
				columns.add(ColumnInfoBO.builder()
					.name("custom_url")
					.type("VARCHAR")
					.description("Channel custom URL")
					.build());
				columns.add(ColumnInfoBO.builder()
					.name("published_at")
					.type("TIMESTAMP")
					.description("Channel creation date")
					.build());
				columns.add(ColumnInfoBO.builder()
					.name("subscriber_count")
					.type("BIGINT")
					.description("Number of subscribers")
					.build());
				columns.add(ColumnInfoBO.builder()
					.name("video_count")
					.type("BIGINT")
					.description("Total number of videos")
					.build());
				columns.add(ColumnInfoBO.builder()
					.name("view_count")
					.type("BIGINT")
					.description("Total view count")
					.build());
				break;

			case "videos":
				columns.add(ColumnInfoBO.builder()
					.name("video_id")
					.type("VARCHAR")
					.description("Video ID")
					.primary(true)
					.notnull(true)
					.build());
				columns.add(ColumnInfoBO.builder()
					.name("title")
					.type("VARCHAR")
					.description("Video title")
					.notnull(true)
					.build());
				columns.add(ColumnInfoBO.builder()
					.name("description")
					.type("TEXT")
					.description("Video description")
					.build());
				columns.add(ColumnInfoBO.builder()
					.name("published_at")
					.type("TIMESTAMP")
					.description("Video publication date")
					.build());
				columns.add(ColumnInfoBO.builder()
					.name("thumbnail_url")
					.type("VARCHAR")
					.description("Thumbnail URL")
					.build());
				break;

			case "video_details":
				columns.add(ColumnInfoBO.builder()
					.name("video_id")
					.type("VARCHAR")
					.description("Video ID")
					.primary(true)
					.notnull(true)
					.build());
				columns.add(ColumnInfoBO.builder()
					.name("title")
					.type("VARCHAR")
					.description("Video title")
					.build());
				columns.add(ColumnInfoBO.builder()
					.name("view_count")
					.type("BIGINT")
					.description("Number of views")
					.build());
				columns.add(ColumnInfoBO.builder()
					.name("like_count")
					.type("BIGINT")
					.description("Number of likes")
					.build());
				columns.add(ColumnInfoBO.builder()
					.name("comment_count")
					.type("BIGINT")
					.description("Number of comments")
					.build());
				columns.add(ColumnInfoBO.builder()
					.name("duration")
					.type("VARCHAR")
					.description("Video duration (ISO 8601)")
					.build());
				columns.add(ColumnInfoBO.builder()
					.name("published_at")
					.type("TIMESTAMP")
					.description("Publication date")
					.build());
				break;
		}

		return columns;
	}

	@Override
	public List<ForeignKeyInfoBO> showForeignKeys(Connection connection, String schema, List<String> tables) {
		// No foreign keys in YouTube data
		return Collections.emptyList();
	}

	@Override
	public List<String> sampleColumn(Connection connection, String schema, String table, String column) {
		// Return sample data for a specific column
		try {
			YouTubeConnection ytConn = (YouTubeConnection) connection;
			String channelId = ytConn.getChannelId();

			if (channelId == null) {
				return Collections.emptyList();
			}

			if ("channel_info".equals(table)) {
				Map<String, Object> channelData = apiClient.getChannelDetails(channelId, ytConn.getApiKey());
				if (channelData.containsKey(column)) {
					return List.of(String.valueOf(channelData.get(column)));
				}
			}
		}
		catch (Exception e) {
			log.error("Error sampling column: {}.{}", table, column, e);
		}

		return Collections.emptyList();
	}

	@Override
	public ResultSetBO scanTable(Connection connection, String schema, String table) {
		try {
			YouTubeConnection ytConn = (YouTubeConnection) connection;
			String channelId = ytConn.getChannelId();

			if (channelId == null) {
				return ResultSetBO.builder().columns(new ArrayList<>()).rows(new ArrayList<>()).build();
			}

			switch (table.toLowerCase()) {
				case "channel_info":
					return scanChannelInfo(ytConn, channelId);
				case "videos":
					return scanVideos(ytConn, channelId);
				case "video_details":
					return scanVideoDetails(ytConn, channelId);
			}
		}
		catch (Exception e) {
			log.error("Error scanning table: {}", table, e);
		}

		return ResultSetBO.builder().columns(new ArrayList<>()).rows(new ArrayList<>()).build();
	}

	private ResultSetBO scanChannelInfo(YouTubeConnection ytConn, String channelId) {
		Map<String, Object> channelData = apiClient.getChannelDetails(channelId, ytConn.getApiKey());

		List<String> columns = List.of("id", "title", "description", "custom_url", "published_at",
				"subscriber_count", "video_count", "view_count");

		List<List<String>> rows = new ArrayList<>();
		List<String> row = new ArrayList<>();
		for (String column : columns) {
			row.add(String.valueOf(channelData.getOrDefault(column, "")));
		}
		rows.add(row);

		return ResultSetBO.builder().columns(columns).rows(rows).build();
	}

	private ResultSetBO scanVideos(YouTubeConnection ytConn, String channelId) {
		List<Map<String, Object>> videos = apiClient.getChannelVideos(channelId, ytConn.getApiKey(), 20);

		List<String> columns = List.of("video_id", "title", "description", "published_at", "thumbnail_url");

		List<List<String>> rows = new ArrayList<>();
		for (Map<String, Object> video : videos) {
			List<String> row = new ArrayList<>();
			for (String column : columns) {
				row.add(String.valueOf(video.getOrDefault(column, "")));
			}
			rows.add(row);
		}

		return ResultSetBO.builder().columns(columns).rows(rows).build();
	}

	private ResultSetBO scanVideoDetails(YouTubeConnection ytConn, String channelId) {
		List<Map<String, Object>> videos = apiClient.getChannelVideos(channelId, ytConn.getApiKey(), 20);

		List<String> columns = List.of("video_id", "title", "view_count", "like_count", "comment_count", "duration",
				"published_at");

		List<List<String>> rows = new ArrayList<>();
		for (Map<String, Object> video : videos) {
			String videoId = (String) video.get("videoId");
			Map<String, Object> details = apiClient.getVideoDetails(videoId, ytConn.getApiKey());

			List<String> row = new ArrayList<>();
			for (String column : columns) {
				row.add(String.valueOf(details.getOrDefault(column, "")));
			}
			rows.add(row);
		}

		return ResultSetBO.builder().columns(columns).rows(rows).build();
	}

	@Override
	public BizDataSourceTypeEnum getDataSourceType() {
		return BizDataSourceTypeEnum.YOUTUBE;
	}

}
