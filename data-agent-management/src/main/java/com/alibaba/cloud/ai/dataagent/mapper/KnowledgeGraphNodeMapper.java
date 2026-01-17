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
package com.alibaba.cloud.ai.dataagent.mapper;

import com.alibaba.cloud.ai.dataagent.entity.KnowledgeGraphNode;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KnowledgeGraphNodeMapper {

	/**
	 * Query nodes by agent ID
	 */
	@Select("""
			SELECT * FROM knowledge_graph_node
			WHERE agent_id = #{agentId} AND is_deleted = 0
			ORDER BY created_time DESC
			""")
	List<KnowledgeGraphNode> selectByAgentId(@Param("agentId") Integer agentId);

	/**
	 * Query nodes by agent ID and node type
	 */
	@Select("""
			SELECT * FROM knowledge_graph_node
			WHERE agent_id = #{agentId} AND node_type = #{nodeType} AND is_deleted = 0
			ORDER BY created_time DESC
			""")
	List<KnowledgeGraphNode> selectByAgentIdAndType(@Param("agentId") Integer agentId,
			@Param("nodeType") String nodeType);

	/**
	 * Search nodes by keyword in agent scope
	 */
	@Select("""
			SELECT * FROM knowledge_graph_node
			WHERE agent_id = #{agentId} AND is_deleted = 0
			  AND (node_name LIKE CONCAT('%', #{keyword}, '%')
			    OR display_name LIKE CONCAT('%', #{keyword}, '%')
			    OR description LIKE CONCAT('%', #{keyword}, '%'))
			ORDER BY created_time DESC
			""")
	List<KnowledgeGraphNode> searchInAgent(@Param("agentId") Integer agentId, @Param("keyword") String keyword);

	/**
	 * Insert a new node
	 */
	@Insert("""
			INSERT INTO knowledge_graph_node (agent_id, node_type, node_name, display_name, description,
			properties, embedding_status, error_msg, is_deleted, created_time, updated_time)
			VALUES (#{agentId}, #{nodeType}, #{nodeName}, #{displayName}, #{description},
			#{properties, typeHandler=org.apache.ibatis.type.JsonTypeHandler}, #{embeddingStatus}, #{errorMsg}, #{isDeleted}, NOW(), NOW())
			""")
	@Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
	int insert(KnowledgeGraphNode node);

	/**
	 * Update node by ID
	 */
	@Update("""
			<script>
			UPDATE knowledge_graph_node
			<set>
				<if test="nodeType != null">node_type = #{nodeType},</if>
				<if test="nodeName != null">node_name = #{nodeName},</if>
				<if test="displayName != null">display_name = #{displayName},</if>
				<if test="description != null">description = #{description},</if>
				<if test="properties != null">properties = #{properties, typeHandler=org.apache.ibatis.type.JsonTypeHandler},</if>
				<if test="embeddingStatus != null">embedding_status = #{embeddingStatus},</if>
				<if test="errorMsg != null">error_msg = #{errorMsg},</if>
				<if test="isDeleted != null">is_deleted = #{isDeleted},</if>
				updated_time = NOW()
			</set>
			WHERE id = #{id}
			</script>
			""")
	int updateById(KnowledgeGraphNode node);

	/**
	 * Query node by ID
	 */
	@Select("""
			SELECT * FROM knowledge_graph_node
			WHERE id = #{id} AND is_deleted = 0
			""")
	KnowledgeGraphNode selectById(@Param("id") Long id);

	/**
	 * Logical delete node by ID
	 */
	@Update("""
			UPDATE knowledge_graph_node
			SET is_deleted = 1, updated_time = NOW()
			WHERE id = #{id}
			""")
	int logicalDeleteById(@Param("id") Long id);

	/**
	 * Physical delete node by ID
	 */
	@Delete("""
			DELETE FROM knowledge_graph_node
			WHERE id = #{id}
			""")
	int deleteById(@Param("id") Long id);

	/**
	 * Query nodes by IDs
	 */
	@Select("""
			<script>
			SELECT * FROM knowledge_graph_node
			WHERE id IN
			<foreach collection="ids" item="id" open="(" separator="," close=")">
				#{id}
			</foreach>
			AND is_deleted = 0
			</script>
			""")
	List<KnowledgeGraphNode> selectByIds(@Param("ids") List<Long> ids);

	/**
	 * Count nodes by agent ID
	 */
	@Select("""
			SELECT COUNT(*) FROM knowledge_graph_node
			WHERE agent_id = #{agentId} AND is_deleted = 0
			""")
	int countByAgentId(@Param("agentId") Integer agentId);

}
