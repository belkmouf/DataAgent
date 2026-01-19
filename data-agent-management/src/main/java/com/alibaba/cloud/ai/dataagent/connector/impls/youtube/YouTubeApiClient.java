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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * YouTube Data API v3 client for fetching channel data
 */
@Slf4j
@Component
public class YouTubeApiClient {

	private static final String BASE_URL = "https://www.googleapis.com/youtube/v3";

	private final RestTemplate restTemplate;

	private final ObjectMapper objectMapper;

	public YouTubeApiClient() {
		this.restTemplate = new RestTemplate();
		this.objectMapper = new ObjectMapper();
	}

	/**
	 * Get channel ID from channel handle (e.g., @NateBJones)
	 */
	public String getChannelIdFromHandle(String handle, String apiKey) {
		try {
			// Remove @ if present
			String channelHandle = handle.startsWith("@") ? handle.substring(1) : handle;

			String url = String.format("%s/search?part=snippet&type=channel&q=%s&key=%s", BASE_URL, channelHandle,
					apiKey);

			String response = restTemplate.getForObject(url, String.class);
			JsonNode root = objectMapper.readTree(response);

			if (root.has("items") && root.get("items").size() > 0) {
				return root.get("items").get(0).get("id").get("channelId").asText();
			}
		}
		catch (Exception e) {
			log.error("Error getting channel ID from handle: {}", handle, e);
		}
		return null;
	}

	/**
	 * Get channel details
	 */
	public Map<String, Object> getChannelDetails(String channelId, String apiKey) {
		try {
			String url = String.format("%s/channels?part=snippet,statistics,contentDetails&id=%s&key=%s", BASE_URL,
					channelId, apiKey);

			String response = restTemplate.getForObject(url, String.class);
			JsonNode root = objectMapper.readTree(response);

			if (root.has("items") && root.get("items").size() > 0) {
				JsonNode channel = root.get("items").get(0);
				Map<String, Object> details = new HashMap<>();

				// Basic info
				details.put("id", channel.get("id").asText());
				details.put("title", channel.get("snippet").get("title").asText());
				details.put("description", channel.get("snippet").get("description").asText());
				details.put("customUrl",
						channel.get("snippet").has("customUrl") ? channel.get("snippet").get("customUrl").asText()
								: "");
				details.put("publishedAt", channel.get("snippet").get("publishedAt").asText());

				// Statistics
				JsonNode stats = channel.get("statistics");
				details.put("subscriberCount", stats.get("subscriberCount").asText());
				details.put("videoCount", stats.get("videoCount").asText());
				details.put("viewCount", stats.get("viewCount").asText());

				return details;
			}
		}
		catch (Exception e) {
			log.error("Error getting channel details for ID: {}", channelId, e);
		}
		return new HashMap<>();
	}

	/**
	 * Get recent videos from channel
	 */
	public List<Map<String, Object>> getChannelVideos(String channelId, String apiKey, int maxResults) {
		List<Map<String, Object>> videos = new ArrayList<>();
		try {
			String url = String.format(
					"%s/search?part=snippet&channelId=%s&order=date&type=video&maxResults=%d&key=%s", BASE_URL,
					channelId, maxResults, apiKey);

			String response = restTemplate.getForObject(url, String.class);
			JsonNode root = objectMapper.readTree(response);

			if (root.has("items")) {
				for (JsonNode item : root.get("items")) {
					Map<String, Object> video = new HashMap<>();
					video.put("videoId", item.get("id").get("videoId").asText());
					video.put("title", item.get("snippet").get("title").asText());
					video.put("description", item.get("snippet").get("description").asText());
					video.put("publishedAt", item.get("snippet").get("publishedAt").asText());
					video.put("thumbnailUrl", item.get("snippet").get("thumbnails").get("default").get("url").asText());
					videos.add(video);
				}
			}
		}
		catch (Exception e) {
			log.error("Error getting videos for channel: {}", channelId, e);
		}
		return videos;
	}

	/**
	 * Get detailed video statistics
	 */
	public Map<String, Object> getVideoDetails(String videoId, String apiKey) {
		try {
			String url = String.format("%s/videos?part=snippet,statistics,contentDetails&id=%s&key=%s", BASE_URL,
					videoId, apiKey);

			String response = restTemplate.getForObject(url, String.class);
			JsonNode root = objectMapper.readTree(response);

			if (root.has("items") && root.get("items").size() > 0) {
				JsonNode video = root.get("items").get(0);
				Map<String, Object> details = new HashMap<>();

				details.put("videoId", video.get("id").asText());
				details.put("title", video.get("snippet").get("title").asText());
				details.put("description", video.get("snippet").get("description").asText());
				details.put("publishedAt", video.get("snippet").get("publishedAt").asText());

				// Statistics
				JsonNode stats = video.get("statistics");
				details.put("viewCount", stats.has("viewCount") ? stats.get("viewCount").asText() : "0");
				details.put("likeCount", stats.has("likeCount") ? stats.get("likeCount").asText() : "0");
				details.put("commentCount", stats.has("commentCount") ? stats.get("commentCount").asText() : "0");

				// Duration
				details.put("duration", video.get("contentDetails").get("duration").asText());

				return details;
			}
		}
		catch (Exception e) {
			log.error("Error getting video details for ID: {}", videoId, e);
		}
		return new HashMap<>();
	}

	/**
	 * Test API connection
	 */
	public boolean testConnection(String apiKey) {
		try {
			String url = String.format("%s/videos?part=id&chart=mostPopular&maxResults=1&key=%s", BASE_URL, apiKey);
			String response = restTemplate.getForObject(url, String.class);
			JsonNode root = objectMapper.readTree(response);
			return root.has("items");
		}
		catch (Exception e) {
			log.error("Error testing YouTube API connection", e);
			return false;
		}
	}

}
