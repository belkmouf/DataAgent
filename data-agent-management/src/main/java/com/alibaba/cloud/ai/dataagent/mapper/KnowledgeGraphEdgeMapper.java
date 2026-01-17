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

import com.alibaba.cloud.ai.dataagent.entity.KnowledgeGraphEdge;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KnowledgeGraphEdgeMapper {

	/**
	 * Query edges by agent ID
	 */
	@Select("""
			SELECT * FROM knowledge_graph_edge
			WHERE agent_id = #{agentId} AND is_deleted = 0
			ORDER BY created_time DESC
			""")
	List<KnowledgeGraphEdge> selectByAgentId(@Param("agentId") Integer agentId);

	/**
	 * Query edges by source node ID
	 */
	@Select("""
			SELECT * FROM knowledge_graph_edge
			WHERE source_node_id = #{sourceNodeId} AND is_deleted = 0
			ORDER BY created_time DESC
			""")
	List<KnowledgeGraphEdge> selectBySourceNodeId(@Param("sourceNodeId") Long sourceNodeId);

	/**
	 * Query edges by target node ID
	 */
	@Select("""
			SELECT * FROM knowledge_graph_edge
			WHERE target_node_id = #{targetNodeId} AND is_deleted = 0
			ORDER BY created_time DESC
			""")
	List<KnowledgeGraphEdge> selectByTargetNodeId(@Param("targetNodeId") Long targetNodeId);

	/**
	 * Query edges between two nodes
	 */
	@Select("""
			SELECT * FROM knowledge_graph_edge
			WHERE source_node_id = #{sourceNodeId}
			  AND target_node_id = #{targetNodeId}
			  AND is_deleted = 0
			ORDER BY created_time DESC
			""")
	List<KnowledgeGraphEdge> selectBetweenNodes(@Param("sourceNodeId") Long sourceNodeId,
			@Param("targetNodeId") Long targetNodeId);

	/**
	 * Query edges by edge type
	 */
	@Select("""
			SELECT * FROM knowledge_graph_edge
			WHERE agent_id = #{agentId} AND edge_type = #{edgeType} AND is_deleted = 0
			ORDER BY created_time DESC
			""")
	List<KnowledgeGraphEdge> selectByAgentIdAndType(@Param("agentId") Integer agentId,
			@Param("edgeType") String edgeType);

	/**
	 * Insert a new edge
	 */
	@Insert("""
			INSERT INTO knowledge_graph_edge (agent_id, source_node_id, target_node_id, edge_type,
			edge_name, description, weight, properties, is_deleted, created_time, updated_time)
			VALUES (#{agentId}, #{sourceNodeId}, #{targetNodeId}, #{edgeType},
			#{edgeName}, #{description}, #{weight}, #{properties, typeHandler=org.apache.ibatis.type.JsonTypeHandler}, #{isDeleted}, NOW(), NOW())
			""")
	@Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
	int insert(KnowledgeGraphEdge edge);

	/**
	 * Update edge by ID
	 */
	@Update("""
			<script>
			UPDATE knowledge_graph_edge
			<set>
				<if test="edgeType != null">edge_type = #{edgeType},</if>
				<if test="edgeName != null">edge_name = #{edgeName},</if>
				<if test="description != null">description = #{description},</if>
				<if test="weight != null">weight = #{weight},</if>
				<if test="properties != null">properties = #{properties, typeHandler=org.apache.ibatis.type.JsonTypeHandler},</if>
				<if test="isDeleted != null">is_deleted = #{isDeleted},</if>
				updated_time = NOW()
			</set>
			WHERE id = #{id}
			</script>
			""")
	int updateById(KnowledgeGraphEdge edge);

	/**
	 * Query edge by ID
	 */
	@Select("""
			SELECT * FROM knowledge_graph_edge
			WHERE id = #{id} AND is_deleted = 0
			""")
	KnowledgeGraphEdge selectById(@Param("id") Long id);

	/**
	 * Logical delete edge by ID
	 */
	@Update("""
			UPDATE knowledge_graph_edge
			SET is_deleted = 1, updated_time = NOW()
			WHERE id = #{id}
			""")
	int logicalDeleteById(@Param("id") Long id);

	/**
	 * Physical delete edge by ID
	 */
	@Delete("""
			DELETE FROM knowledge_graph_edge
			WHERE id = #{id}
			""")
	int deleteById(@Param("id") Long id);

	/**
	 * Delete edges by node ID (when deleting a node)
	 */
	@Update("""
			UPDATE knowledge_graph_edge
			SET is_deleted = 1, updated_time = NOW()
			WHERE (source_node_id = #{nodeId} OR target_node_id = #{nodeId})
			  AND is_deleted = 0
			""")
	int logicalDeleteByNodeId(@Param("nodeId") Long nodeId);

	/**
	 * Query all edges connected to a node
	 */
	@Select("""
			SELECT * FROM knowledge_graph_edge
			WHERE (source_node_id = #{nodeId} OR target_node_id = #{nodeId})
			  AND is_deleted = 0
			ORDER BY created_time DESC
			""")
	List<KnowledgeGraphEdge> selectByNodeId(@Param("nodeId") Long nodeId);

	/**
	 * Count edges by agent ID
	 */
	@Select("""
			SELECT COUNT(*) FROM knowledge_graph_edge
			WHERE agent_id = #{agentId} AND is_deleted = 0
			""")
	int countByAgentId(@Param("agentId") Integer agentId);

}
