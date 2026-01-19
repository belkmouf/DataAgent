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
import com.alibaba.cloud.ai.dataagent.connector.DbQueryParameter;
import com.alibaba.cloud.ai.dataagent.connector.accessor.AbstractAccessor;
import com.alibaba.cloud.ai.dataagent.connector.ddl.Ddl;
import com.alibaba.cloud.ai.dataagent.connector.ddl.DdlFactory;
import com.alibaba.cloud.ai.dataagent.connector.pool.DBConnectionPoolFactory;
import com.alibaba.cloud.ai.dataagent.enums.BizDataSourceTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;

/**
 * YouTube accessor for handling YouTube data operations
 */
@Slf4j
@Service("youtubeAccessor")
public class YouTubeAccessor extends AbstractAccessor {

	private static final String ACCESSOR_TYPE = "YouTube_Accessor";

	private final DdlFactory ddlFactory;

	protected YouTubeAccessor(DdlFactory ddlFactory, DBConnectionPoolFactory poolFactory) {
		super(ddlFactory, poolFactory.getPoolByDbType(BizDataSourceTypeEnum.YOUTUBE.getTypeName()));
		this.ddlFactory = ddlFactory;
	}

	@Override
	public String getAccessorType() {
		return ACCESSOR_TYPE;
	}

	@Override
	public boolean supportedDataSourceType(String type) {
		return BizDataSourceTypeEnum.YOUTUBE.getTypeName().equalsIgnoreCase(type);
	}

	@Override
	public <T> T accessDb(DbConfigBO dbConfig, String method, DbQueryParameter param) throws Exception {
		try (Connection connection = getConnection(dbConfig)) {
			// Get the Ddl executor directly (not cast to AbstractJdbcDdl since YouTube uses Ddl interface)
			Ddl ddlExecutor = ddlFactory.getDdlExecutorByDbConfig(dbConfig);

			switch (method) {
				case "showDatabases":
					return (T) ddlExecutor.showDatabases(connection);
				case "showSchemas":
					return (T) ddlExecutor.showSchemas(connection);
				case "showTables":
					return (T) ddlExecutor.showTables(connection, param.getSchema(), param.getTablePattern());
				case "fetchTables":
					return (T) ddlExecutor.fetchTables(connection, param.getSchema(), param.getTables());
				case "showColumns":
					return (T) ddlExecutor.showColumns(connection, param.getSchema(), param.getTable());
				case "showForeignKeys":
					return (T) ddlExecutor.showForeignKeys(connection, param.getSchema(), param.getTables());
				case "sampleColumn":
					return (T) ddlExecutor.sampleColumn(connection, param.getSchema(), param.getTable(),
							param.getColumn());
				case "scanTable":
					return (T) ddlExecutor.scanTable(connection, param.getSchema(), param.getTable());
				case "executeSqlAndReturnObject":
					// YouTube doesn't support SQL execution
					throw new UnsupportedOperationException("YouTube API does not support SQL execution");
				default:
					throw new UnsupportedOperationException("Unknown method: " + method);
			}
		}
		catch (Exception e) {
			log.error("Error accessing YouTube API with method: {}, reason: {}", method, e.getMessage());
			throw e;
		}
	}

}
