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
package com.alibaba.cloud.ai.dataagent.service.datasource.handler.impl;

import com.alibaba.cloud.ai.dataagent.bo.DbConfigBO;
import com.alibaba.cloud.ai.dataagent.entity.Datasource;
import com.alibaba.cloud.ai.dataagent.enums.BizDataSourceTypeEnum;
import com.alibaba.cloud.ai.dataagent.enums.DbAccessTypeEnum;
import com.alibaba.cloud.ai.dataagent.enums.DatabaseDialectEnum;
import com.alibaba.cloud.ai.dataagent.service.datasource.handler.DatasourceTypeHandler;
import org.springframework.stereotype.Component;

/**
 * YouTube datasource type handler
 */
@Component
public class YouTubeDatasourceTypeHandler implements DatasourceTypeHandler {

	@Override
	public String typeName() {
		return BizDataSourceTypeEnum.YOUTUBE.getTypeName();
	}

	@Override
	public String connectionType() {
		return DbAccessTypeEnum.API.getCode();
	}

	@Override
	public String dialectType() {
		return DatabaseDialectEnum.YOUTUBE.getCode();
	}

	@Override
	public boolean hasRequiredConnectionFields(Datasource datasource) {
		// For YouTube, we need:
		// - username field to store API key
		// - databaseName field to store channel handle (e.g., @NateBJones)
		return datasource.getUsername() != null && !datasource.getUsername().isEmpty()
				&& datasource.getDatabaseName() != null && !datasource.getDatabaseName().isEmpty();
	}

	@Override
	public String buildConnectionUrl(Datasource datasource) {
		// For YouTube, we don't use a traditional URL
		// The channel handle is stored in databaseName
		if (datasource.getDatabaseName() != null) {
			return "youtube://api.youtube.com/channel/" + datasource.getDatabaseName();
		}
		return "youtube://api.youtube.com";
	}

	@Override
	public DbConfigBO toDbConfig(Datasource datasource) {
		DbConfigBO config = new DbConfigBO();
		config.setUrl(buildConnectionUrl(datasource));
		config.setUsername(datasource.getUsername()); // API key
		config.setPassword(""); // Not used for YouTube
		config.setConnectionType(connectionType());
		config.setDialectType(dialectType());
		config.setSchema(datasource.getDatabaseName()); // Channel handle
		return config;
	}

	@Override
	public String normalizeTestUrl(Datasource datasource, String url) {
		return buildConnectionUrl(datasource);
	}

}
