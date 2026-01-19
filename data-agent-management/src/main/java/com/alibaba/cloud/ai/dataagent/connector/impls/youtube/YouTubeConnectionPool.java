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

import com.alibaba.cloud.ai.dataagent.bo.DbConfigBO;
import com.alibaba.cloud.ai.dataagent.connector.pool.DBConnectionPool;
import com.alibaba.cloud.ai.dataagent.enums.BizDataSourceTypeEnum;
import com.alibaba.cloud.ai.dataagent.enums.ErrorCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * YouTube API connection pool. Unlike JDBC pools, this manages API credentials.
 */
@Slf4j
@Service("youtubeConnectionPool")
public class YouTubeConnectionPool implements DBConnectionPool {

	private static final ConcurrentHashMap<String, YouTubeConnection> CONNECTION_CACHE = new ConcurrentHashMap<>();

	@Autowired
	private YouTubeApiClient apiClient;

	@Override
	public ErrorCodeEnum ping(DbConfigBO config) {
		try {
			// Use username field to store API key
			String apiKey = config.getUsername();

			if (apiKey == null || apiKey.isEmpty()) {
				return ErrorCodeEnum.PASSWORD_ERROR_28000;
			}

			// Test the API connection
			boolean isValid = apiClient.testConnection(apiKey);

			if (isValid) {
				return ErrorCodeEnum.SUCCESS;
			}
			else {
				return ErrorCodeEnum.DATASOURCE_CONNECTION_FAILURE_08S01;
			}
		}
		catch (Exception e) {
			log.error("Error testing YouTube API connection", e);
			return ErrorCodeEnum.DATASOURCE_CONNECTION_FAILURE_08S01;
		}
	}

	@Override
	public Connection getConnection(DbConfigBO config) {
		String cacheKey = generateCacheKey(config);

		YouTubeConnection youtubeConn = CONNECTION_CACHE.computeIfAbsent(cacheKey, key -> {
			String apiKey = config.getUsername();
			String channelHandle = config.getSchema(); // Use schema field for channel handle

			return new YouTubeConnection(apiKey, channelHandle, apiClient);
		});

		// Return the YouTube connection (which wraps API client)
		// Note: This returns a custom Connection implementation
		return youtubeConn;
	}

	@Override
	public boolean supportedDataSourceType(String type) {
		return BizDataSourceTypeEnum.YOUTUBE.getTypeName().equals(type);
	}

	@Override
	public String getConnectionPoolType() {
		return BizDataSourceTypeEnum.YOUTUBE.getTypeName();
	}

	@Override
	public void close() throws Exception {
		CONNECTION_CACHE.clear();
		log.info("YouTube connection cache cleared");
	}

	private String generateCacheKey(DbConfigBO config) {
		return config.getUsername() + "|" + config.getSchema();
	}

	/**
	 * Custom Connection wrapper for YouTube API
	 */
	public static class YouTubeConnection implements Connection {

		private final String apiKey;

		private final String channelHandle;

		private final YouTubeApiClient apiClient;

		private String channelId;

		public YouTubeConnection(String apiKey, String channelHandle, YouTubeApiClient apiClient) {
			this.apiKey = apiKey;
			this.channelHandle = channelHandle;
			this.apiClient = apiClient;
		}

		public String getApiKey() {
			return apiKey;
		}

		public String getChannelHandle() {
			return channelHandle;
		}

		public String getChannelId() {
			if (channelId == null && channelHandle != null) {
				channelId = apiClient.getChannelIdFromHandle(channelHandle, apiKey);
			}
			return channelId;
		}

		public YouTubeApiClient getApiClient() {
			return apiClient;
		}

		// Implement required Connection methods (most will throw
		// UnsupportedOperationException)
		@Override
		public void close() {
			// No-op for API connections
		}

		@Override
		public boolean isClosed() {
			return false;
		}

		@Override
		public String getCatalog() {
			return channelHandle;
		}

		// All other Connection methods throw UnsupportedOperationException
		@Override
		public java.sql.Statement createStatement() {
			throw new UnsupportedOperationException("YouTube API does not support SQL statements");
		}

		@Override
		public java.sql.PreparedStatement prepareStatement(String sql) {
			throw new UnsupportedOperationException("YouTube API does not support SQL statements");
		}

		@Override
		public java.sql.CallableStatement prepareCall(String sql) {
			throw new UnsupportedOperationException("YouTube API does not support SQL statements");
		}

		@Override
		public String nativeSQL(String sql) {
			throw new UnsupportedOperationException("YouTube API does not support SQL");
		}

		@Override
		public void setAutoCommit(boolean autoCommit) {
			// No-op
		}

		@Override
		public boolean getAutoCommit() {
			return true;
		}

		@Override
		public void commit() {
			// No-op
		}

		@Override
		public void rollback() {
			// No-op
		}

		@Override
		public java.sql.DatabaseMetaData getMetaData() {
			throw new UnsupportedOperationException("YouTube API does not provide database metadata");
		}

		@Override
		public void setReadOnly(boolean readOnly) {
			// No-op
		}

		@Override
		public boolean isReadOnly() {
			return true;
		}

		@Override
		public void setCatalog(String catalog) {
			// No-op
		}

		@Override
		public void setTransactionIsolation(int level) {
			// No-op
		}

		@Override
		public int getTransactionIsolation() {
			return Connection.TRANSACTION_NONE;
		}

		@Override
		public java.sql.SQLWarning getWarnings() {
			return null;
		}

		@Override
		public void clearWarnings() {
			// No-op
		}

		@Override
		public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency) {
			throw new UnsupportedOperationException("YouTube API does not support SQL statements");
		}

		@Override
		public java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) {
			throw new UnsupportedOperationException("YouTube API does not support SQL statements");
		}

		@Override
		public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) {
			throw new UnsupportedOperationException("YouTube API does not support SQL statements");
		}

		@Override
		public java.util.Map<String, Class<?>> getTypeMap() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setTypeMap(java.util.Map<String, Class<?>> map) {
			// No-op
		}

		@Override
		public void setHoldability(int holdability) {
			// No-op
		}

		@Override
		public int getHoldability() {
			return 0;
		}

		@Override
		public java.sql.Savepoint setSavepoint() {
			throw new UnsupportedOperationException();
		}

		@Override
		public java.sql.Savepoint setSavepoint(String name) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void rollback(java.sql.Savepoint savepoint) {
			// No-op
		}

		@Override
		public void releaseSavepoint(java.sql.Savepoint savepoint) {
			// No-op
		}

		@Override
		public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency,
				int resultSetHoldability) {
			throw new UnsupportedOperationException();
		}

		@Override
		public java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
				int resultSetHoldability) {
			throw new UnsupportedOperationException();
		}

		@Override
		public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
				int resultSetHoldability) {
			throw new UnsupportedOperationException();
		}

		@Override
		public java.sql.PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) {
			throw new UnsupportedOperationException();
		}

		@Override
		public java.sql.PreparedStatement prepareStatement(String sql, int[] columnIndexes) {
			throw new UnsupportedOperationException();
		}

		@Override
		public java.sql.PreparedStatement prepareStatement(String sql, String[] columnNames) {
			throw new UnsupportedOperationException();
		}

		@Override
		public java.sql.Clob createClob() {
			throw new UnsupportedOperationException();
		}

		@Override
		public java.sql.Blob createBlob() {
			throw new UnsupportedOperationException();
		}

		@Override
		public java.sql.NClob createNClob() {
			throw new UnsupportedOperationException();
		}

		@Override
		public java.sql.SQLXML createSQLXML() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isValid(int timeout) {
			return apiClient.testConnection(apiKey);
		}

		@Override
		public void setClientInfo(String name, String value) {
			// No-op
		}

		@Override
		public void setClientInfo(java.util.Properties properties) {
			// No-op
		}

		@Override
		public String getClientInfo(String name) {
			return null;
		}

		@Override
		public java.util.Properties getClientInfo() {
			return new java.util.Properties();
		}

		@Override
		public java.sql.Array createArrayOf(String typeName, Object[] elements) {
			throw new UnsupportedOperationException();
		}

		@Override
		public java.sql.Struct createStruct(String typeName, Object[] attributes) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setSchema(String schema) {
			// No-op
		}

		@Override
		public String getSchema() {
			return channelHandle;
		}

		@Override
		public void abort(java.util.concurrent.Executor executor) {
			// No-op
		}

		@Override
		public void setNetworkTimeout(java.util.concurrent.Executor executor, int milliseconds) {
			// No-op
		}

		@Override
		public int getNetworkTimeout() {
			return 0;
		}

		@Override
		public <T> T unwrap(Class<T> iface) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) {
			return false;
		}

	}

}
