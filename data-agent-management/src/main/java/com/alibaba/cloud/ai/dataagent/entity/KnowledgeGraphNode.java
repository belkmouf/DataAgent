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
package com.alibaba.cloud.ai.dataagent.entity;

import com.alibaba.cloud.ai.dataagent.enums.EmbeddingStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Knowledge Graph Node Entity Class
 * Represents entities/concepts in the knowledge graph
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeGraphNode {

	private Long id; // Node unique ID

	private Integer agentId; // Associated agent ID

	private String nodeType; // Node type (e.g., Entity, Concept, Table, Column)

	private String nodeName; // Node name

	private String displayName; // Display name for UI

	private String description; // Node description

	private Map<String, Object> properties; // Node properties (JSON format)

	@Builder.Default
	private EmbeddingStatus embeddingStatus = EmbeddingStatus.PENDING; // Vectorization status

	private String errorMsg; // Error message for failed operations

	@Builder.Default
	private Integer isDeleted = 0; // Logical deletion: 0-not deleted, 1-deleted

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	private LocalDateTime createdTime; // Creation time

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	private LocalDateTime updatedTime; // Update time

}
