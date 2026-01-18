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
package com.alibaba.cloud.ai.dataagent.service.knowledgegraph;

import com.alibaba.cloud.ai.dataagent.entity.KnowledgeGraphEdge;
import com.alibaba.cloud.ai.dataagent.entity.KnowledgeGraphNode;

import java.util.List;
import java.util.Map;

/**
 * Knowledge Graph Service Interface
 * Provides operations for managing nodes, edges, and graph traversal
 */
public interface KnowledgeGraphService {

	// ========== Node Operations ==========

	/**
	 * Get all nodes for an agent
	 */
	List<KnowledgeGraphNode> getNodesByAgentId(Integer agentId);

	/**
	 * Get nodes by type
	 */
	List<KnowledgeGraphNode> getNodesByType(Integer agentId, String nodeType);

	/**
	 * Search nodes by keyword
	 */
	List<KnowledgeGraphNode> searchNodes(Integer agentId, String keyword);

	/**
	 * Get node by ID
	 */
	KnowledgeGraphNode getNodeById(Long nodeId);

	/**
	 * Create a new node
	 */
	KnowledgeGraphNode createNode(KnowledgeGraphNode node);

	/**
	 * Update a node
	 */
	KnowledgeGraphNode updateNode(KnowledgeGraphNode node);

	/**
	 * Delete a node (logical deletion)
	 */
	void deleteNode(Long nodeId);

	/**
	 * Batch create nodes
	 */
	List<KnowledgeGraphNode> batchCreateNodes(List<KnowledgeGraphNode> nodes);

	// ========== Edge Operations ==========

	/**
	 * Get all edges for an agent
	 */
	List<KnowledgeGraphEdge> getEdgesByAgentId(Integer agentId);

	/**
	 * Get edges by source node
	 */
	List<KnowledgeGraphEdge> getEdgesBySourceNode(Long sourceNodeId);

	/**
	 * Get edges by target node
	 */
	List<KnowledgeGraphEdge> getEdgesByTargetNode(Long targetNodeId);

	/**
	 * Get edges between two nodes
	 */
	List<KnowledgeGraphEdge> getEdgesBetweenNodes(Long sourceNodeId, Long targetNodeId);

	/**
	 * Get all edges connected to a node (incoming and outgoing)
	 */
	List<KnowledgeGraphEdge> getEdgesByNode(Long nodeId);

	/**
	 * Create a new edge
	 */
	KnowledgeGraphEdge createEdge(KnowledgeGraphEdge edge);

	/**
	 * Update an edge
	 */
	KnowledgeGraphEdge updateEdge(KnowledgeGraphEdge edge);

	/**
	 * Delete an edge (logical deletion)
	 */
	void deleteEdge(Long edgeId);

	/**
	 * Batch create edges
	 */
	List<KnowledgeGraphEdge> batchCreateEdges(List<KnowledgeGraphEdge> edges);

	// ========== Graph Traversal & Query Operations ==========

	/**
	 * Find neighbors of a node (1-hop traversal)
	 * @param nodeId The starting node
	 * @return List of connected nodes
	 */
	List<KnowledgeGraphNode> getNeighbors(Long nodeId);

	/**
	 * Find shortest path between two nodes
	 * @param sourceNodeId Start node
	 * @param targetNodeId End node
	 * @return List of nodes representing the path, or empty if no path exists
	 */
	List<KnowledgeGraphNode> findShortestPath(Long sourceNodeId, Long targetNodeId);

	/**
	 * Get subgraph around a node (n-hop neighborhood)
	 * @param nodeId The center node
	 * @param depth The number of hops (default 2)
	 * @return Map with "nodes" and "edges" keys
	 */
	Map<String, Object> getSubgraph(Long nodeId, int depth);

	/**
	 * Find nodes by relationship type
	 * @param nodeId Starting node
	 * @param edgeType Relationship type to follow
	 * @return List of connected nodes via the specified relationship
	 */
	List<KnowledgeGraphNode> getNodesByRelationship(Long nodeId, String edgeType);

	/**
	 * Get graph statistics for an agent
	 * @return Map with statistics like nodeCount, edgeCount, nodeTypes, edgeTypes
	 */
	Map<String, Object> getGraphStats(Integer agentId);

	// ========== Vector Store Integration ==========

	/**
	 * Vectorize a node (convert to embedding and store in vector store)
	 */
	void vectorizeNode(Long nodeId);

	/**
	 * Batch vectorize nodes
	 */
	void batchVectorizeNodes(List<Long> nodeIds);

	/**
	 * Refresh all node embeddings for an agent
	 */
	void refreshNodeEmbeddings(Integer agentId);

	/**
	 * Hybrid search: Vector similarity + Graph structure
	 * @param agentId The agent
	 * @param query The search query
	 * @param topK Number of results
	 * @return List of nodes ranked by relevance
	 */
	List<KnowledgeGraphNode> hybridSearch(Integer agentId, String query, int topK);

}
