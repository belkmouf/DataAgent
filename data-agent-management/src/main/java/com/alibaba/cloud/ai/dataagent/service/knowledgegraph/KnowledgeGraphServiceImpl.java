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

import com.alibaba.cloud.ai.dataagent.constant.Constant;
import com.alibaba.cloud.ai.dataagent.constant.DocumentMetadataConstant;
import com.alibaba.cloud.ai.dataagent.entity.KnowledgeGraphEdge;
import com.alibaba.cloud.ai.dataagent.entity.KnowledgeGraphNode;
import com.alibaba.cloud.ai.dataagent.enums.EmbeddingStatus;
import com.alibaba.cloud.ai.dataagent.mapper.KnowledgeGraphEdgeMapper;
import com.alibaba.cloud.ai.dataagent.mapper.KnowledgeGraphNodeMapper;
import com.alibaba.cloud.ai.dataagent.service.vectorstore.AgentVectorStoreService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class KnowledgeGraphServiceImpl implements KnowledgeGraphService {

	private final KnowledgeGraphNodeMapper nodeMapper;

	private final KnowledgeGraphEdgeMapper edgeMapper;

	private final AgentVectorStoreService vectorStoreService;

	// ========== Node Operations ==========

	@Override
	public List<KnowledgeGraphNode> getNodesByAgentId(Integer agentId) {
		return nodeMapper.selectByAgentId(agentId);
	}

	@Override
	public List<KnowledgeGraphNode> getNodesByType(Integer agentId, String nodeType) {
		return nodeMapper.selectByAgentIdAndType(agentId, nodeType);
	}

	@Override
	public List<KnowledgeGraphNode> searchNodes(Integer agentId, String keyword) {
		return nodeMapper.searchInAgent(agentId, keyword);
	}

	@Override
	public KnowledgeGraphNode getNodeById(Long nodeId) {
		return nodeMapper.selectById(nodeId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public KnowledgeGraphNode createNode(KnowledgeGraphNode node) {
		if (node.getEmbeddingStatus() == null) {
			node.setEmbeddingStatus(EmbeddingStatus.PENDING);
		}

		if (nodeMapper.insert(node) <= 0) {
			throw new RuntimeException("Failed to create node in database");
		}

		// Auto-vectorize if node has meaningful content
		if (shouldVectorize(node)) {
			try {
				vectorizeNodeInternal(node);
				node.setEmbeddingStatus(EmbeddingStatus.COMPLETED);
				node.setErrorMsg(null);
				nodeMapper.updateById(node);
			}
			catch (Exception e) {
				log.error("Failed to vectorize node {}: {}", node.getId(), e.getMessage());
				node.setEmbeddingStatus(EmbeddingStatus.FAILED);
				node.setErrorMsg("Vectorization failed: " + e.getMessage());
				nodeMapper.updateById(node);
			}
		}

		return node;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public KnowledgeGraphNode updateNode(KnowledgeGraphNode node) {
		KnowledgeGraphNode existing = nodeMapper.selectById(node.getId());
		if (existing == null) {
			throw new RuntimeException("Node not found with id: " + node.getId());
		}

		node.setEmbeddingStatus(EmbeddingStatus.PROCESSING);
		if (nodeMapper.updateById(node) <= 0) {
			throw new RuntimeException("Failed to update node in database");
		}

		// Re-vectorize if content changed
		if (shouldVectorize(node)) {
			try {
				// Delete old embedding
				deleteNodeFromVectorStore(existing);
				// Create new embedding
				vectorizeNodeInternal(node);
				node.setEmbeddingStatus(EmbeddingStatus.COMPLETED);
				node.setErrorMsg(null);
				nodeMapper.updateById(node);
			}
			catch (Exception e) {
				log.error("Failed to re-vectorize node {}: {}", node.getId(), e.getMessage());
				node.setEmbeddingStatus(EmbeddingStatus.FAILED);
				node.setErrorMsg("Re-vectorization failed: " + e.getMessage());
				nodeMapper.updateById(node);
			}
		}

		return node;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteNode(Long nodeId) {
		KnowledgeGraphNode node = nodeMapper.selectById(nodeId);
		if (node == null) {
			return;
		}

		// Delete from vector store first
		try {
			deleteNodeFromVectorStore(node);
		}
		catch (Exception e) {
			log.error("Failed to delete node from vector store: {}", e.getMessage());
		}

		// Logical delete connected edges
		edgeMapper.logicalDeleteByNodeId(nodeId);

		// Logical delete node
		nodeMapper.logicalDeleteById(nodeId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public List<KnowledgeGraphNode> batchCreateNodes(List<KnowledgeGraphNode> nodes) {
		if (CollectionUtils.isEmpty(nodes)) {
			return Collections.emptyList();
		}

		for (KnowledgeGraphNode node : nodes) {
			createNode(node);
		}
		return nodes;
	}

	// ========== Edge Operations ==========

	@Override
	public List<KnowledgeGraphEdge> getEdgesByAgentId(Integer agentId) {
		return edgeMapper.selectByAgentId(agentId);
	}

	@Override
	public List<KnowledgeGraphEdge> getEdgesBySourceNode(Long sourceNodeId) {
		return edgeMapper.selectBySourceNodeId(sourceNodeId);
	}

	@Override
	public List<KnowledgeGraphEdge> getEdgesByTargetNode(Long targetNodeId) {
		return edgeMapper.selectByTargetNodeId(targetNodeId);
	}

	@Override
	public List<KnowledgeGraphEdge> getEdgesBetweenNodes(Long sourceNodeId, Long targetNodeId) {
		return edgeMapper.selectBetweenNodes(sourceNodeId, targetNodeId);
	}

	@Override
	public List<KnowledgeGraphEdge> getEdgesByNode(Long nodeId) {
		return edgeMapper.selectByNodeId(nodeId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public KnowledgeGraphEdge createEdge(KnowledgeGraphEdge edge) {
		// Validate that source and target nodes exist
		if (nodeMapper.selectById(edge.getSourceNodeId()) == null) {
			throw new RuntimeException("Source node not found: " + edge.getSourceNodeId());
		}
		if (nodeMapper.selectById(edge.getTargetNodeId()) == null) {
			throw new RuntimeException("Target node not found: " + edge.getTargetNodeId());
		}

		if (edgeMapper.insert(edge) <= 0) {
			throw new RuntimeException("Failed to create edge in database");
		}
		return edge;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public KnowledgeGraphEdge updateEdge(KnowledgeGraphEdge edge) {
		if (edgeMapper.updateById(edge) <= 0) {
			throw new RuntimeException("Failed to update edge in database");
		}
		return edgeMapper.selectById(edge.getId());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteEdge(Long edgeId) {
		edgeMapper.logicalDeleteById(edgeId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public List<KnowledgeGraphEdge> batchCreateEdges(List<KnowledgeGraphEdge> edges) {
		if (CollectionUtils.isEmpty(edges)) {
			return Collections.emptyList();
		}

		for (KnowledgeGraphEdge edge : edges) {
			createEdge(edge);
		}
		return edges;
	}

	// ========== Graph Traversal & Query Operations ==========

	@Override
	public List<KnowledgeGraphNode> getNeighbors(Long nodeId) {
		List<KnowledgeGraphEdge> edges = edgeMapper.selectByNodeId(nodeId);
		if (CollectionUtils.isEmpty(edges)) {
			return Collections.emptyList();
		}

		Set<Long> neighborIds = new HashSet<>();
		for (KnowledgeGraphEdge edge : edges) {
			if (edge.getSourceNodeId().equals(nodeId)) {
				neighborIds.add(edge.getTargetNodeId());
			}
			else {
				neighborIds.add(edge.getSourceNodeId());
			}
		}

		if (neighborIds.isEmpty()) {
			return Collections.emptyList();
		}

		return nodeMapper.selectByIds(new ArrayList<>(neighborIds));
	}

	@Override
	public List<KnowledgeGraphNode> findShortestPath(Long sourceNodeId, Long targetNodeId) {
		// BFS-based shortest path algorithm
		if (sourceNodeId.equals(targetNodeId)) {
			KnowledgeGraphNode node = nodeMapper.selectById(sourceNodeId);
			return node != null ? List.of(node) : Collections.emptyList();
		}

		Map<Long, Long> parentMap = new HashMap<>();
		Set<Long> visited = new HashSet<>();
		Queue<Long> queue = new LinkedList<>();

		queue.offer(sourceNodeId);
		visited.add(sourceNodeId);
		parentMap.put(sourceNodeId, null);

		while (!queue.isEmpty()) {
			Long currentId = queue.poll();

			if (currentId.equals(targetNodeId)) {
				return reconstructPath(parentMap, sourceNodeId, targetNodeId);
			}

			List<KnowledgeGraphEdge> edges = edgeMapper.selectByNodeId(currentId);
			for (KnowledgeGraphEdge edge : edges) {
				Long neighborId = edge.getSourceNodeId().equals(currentId) ? edge.getTargetNodeId()
						: edge.getSourceNodeId();

				if (!visited.contains(neighborId)) {
					visited.add(neighborId);
					parentMap.put(neighborId, currentId);
					queue.offer(neighborId);
				}
			}
		}

		return Collections.emptyList(); // No path found
	}

	@Override
	public Map<String, Object> getSubgraph(Long nodeId, int depth) {
		Set<Long> visitedNodes = new HashSet<>();
		Set<Long> currentLevel = new HashSet<>();
		currentLevel.add(nodeId);

		for (int i = 0; i < depth; i++) {
			Set<Long> nextLevel = new HashSet<>();
			for (Long currentNodeId : currentLevel) {
				if (!visitedNodes.contains(currentNodeId)) {
					visitedNodes.add(currentNodeId);
					List<KnowledgeGraphEdge> edges = edgeMapper.selectByNodeId(currentNodeId);
					for (KnowledgeGraphEdge edge : edges) {
						Long neighborId = edge.getSourceNodeId().equals(currentNodeId) ? edge.getTargetNodeId()
								: edge.getSourceNodeId();
						nextLevel.add(neighborId);
					}
				}
			}
			currentLevel = nextLevel;
		}

		// Get all nodes
		List<KnowledgeGraphNode> nodes = visitedNodes.isEmpty() ? Collections.emptyList()
				: nodeMapper.selectByIds(new ArrayList<>(visitedNodes));

		// Get all edges between these nodes
		List<KnowledgeGraphEdge> allEdges = new ArrayList<>();
		for (Long id : visitedNodes) {
			List<KnowledgeGraphEdge> edges = edgeMapper.selectByNodeId(id);
			allEdges.addAll(edges.stream()
				.filter(e -> visitedNodes.contains(e.getSourceNodeId()) && visitedNodes.contains(e.getTargetNodeId()))
				.toList());
		}

		Map<String, Object> result = new HashMap<>();
		result.put("nodes", nodes);
		result.put("edges", allEdges.stream().distinct().collect(Collectors.toList()));
		return result;
	}

	@Override
	public List<KnowledgeGraphNode> getNodesByRelationship(Long nodeId, String edgeType) {
		List<KnowledgeGraphEdge> edges = edgeMapper.selectByNodeId(nodeId);
		if (CollectionUtils.isEmpty(edges)) {
			return Collections.emptyList();
		}

		Set<Long> relatedNodeIds = edges.stream()
			.filter(edge -> edge.getEdgeType().equals(edgeType))
			.map(edge -> edge.getSourceNodeId().equals(nodeId) ? edge.getTargetNodeId() : edge.getSourceNodeId())
			.collect(Collectors.toSet());

		if (relatedNodeIds.isEmpty()) {
			return Collections.emptyList();
		}

		return nodeMapper.selectByIds(new ArrayList<>(relatedNodeIds));
	}

	@Override
	public Map<String, Object> getGraphStats(Integer agentId) {
		int nodeCount = nodeMapper.countByAgentId(agentId);
		int edgeCount = edgeMapper.countByAgentId(agentId);

		List<KnowledgeGraphNode> nodes = nodeMapper.selectByAgentId(agentId);
		Map<String, Long> nodeTypeCounts = nodes.stream()
			.collect(Collectors.groupingBy(KnowledgeGraphNode::getNodeType, Collectors.counting()));

		List<KnowledgeGraphEdge> edges = edgeMapper.selectByAgentId(agentId);
		Map<String, Long> edgeTypeCounts = edges.stream()
			.collect(Collectors.groupingBy(KnowledgeGraphEdge::getEdgeType, Collectors.counting()));

		Map<String, Object> stats = new HashMap<>();
		stats.put("nodeCount", nodeCount);
		stats.put("edgeCount", edgeCount);
		stats.put("nodeTypes", nodeTypeCounts);
		stats.put("edgeTypes", edgeTypeCounts);
		stats.put("avgDegree", nodeCount > 0 ? (double) (edgeCount * 2) / nodeCount : 0);

		return stats;
	}

	// ========== Vector Store Integration ==========

	@Override
	public void vectorizeNode(Long nodeId) {
		KnowledgeGraphNode node = nodeMapper.selectById(nodeId);
		if (node == null) {
			throw new RuntimeException("Node not found: " + nodeId);
		}

		node.setEmbeddingStatus(EmbeddingStatus.PROCESSING);
		nodeMapper.updateById(node);

		try {
			vectorizeNodeInternal(node);
			node.setEmbeddingStatus(EmbeddingStatus.COMPLETED);
			node.setErrorMsg(null);
		}
		catch (Exception e) {
			log.error("Failed to vectorize node {}: {}", nodeId, e.getMessage());
			node.setEmbeddingStatus(EmbeddingStatus.FAILED);
			node.setErrorMsg("Vectorization failed: " + e.getMessage());
		}
		nodeMapper.updateById(node);
	}

	@Override
	public void batchVectorizeNodes(List<Long> nodeIds) {
		if (CollectionUtils.isEmpty(nodeIds)) {
			return;
		}

		for (Long nodeId : nodeIds) {
			try {
				vectorizeNode(nodeId);
			}
			catch (Exception e) {
				log.error("Failed to vectorize node {}: {}", nodeId, e.getMessage());
			}
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void refreshNodeEmbeddings(Integer agentId) {
		List<KnowledgeGraphNode> nodes = nodeMapper.selectByAgentId(agentId);
		if (CollectionUtils.isEmpty(nodes)) {
			return;
		}

		for (KnowledgeGraphNode node : nodes) {
			if (shouldVectorize(node)) {
				try {
					vectorizeNode(node.getId());
				}
				catch (Exception e) {
					log.error("Failed to refresh embedding for node {}: {}", node.getId(), e.getMessage());
				}
			}
		}
	}

	@Override
	public List<KnowledgeGraphNode> hybridSearch(Integer agentId, String query, int topK) {
		// TODO: Implement hybrid search combining vector similarity and graph structure
		// For now, return simple keyword search
		List<KnowledgeGraphNode> results = searchNodes(agentId, query);
		return results.size() > topK ? results.subList(0, topK) : results;
	}

	// ========== Private Helper Methods ==========

	private boolean shouldVectorize(KnowledgeGraphNode node) {
		return node.getDescription() != null && !node.getDescription().trim().isEmpty();
	}

	private void vectorizeNodeInternal(KnowledgeGraphNode node) {
		String content = buildNodeContent(node);
		Map<String, Object> metadata = buildNodeMetadata(node);

		Document document = new Document(content, metadata);
		vectorStoreService.addDocuments(node.getAgentId().toString(), List.of(document));
	}

	private void deleteNodeFromVectorStore(KnowledgeGraphNode node) {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put(DocumentMetadataConstant.DB_KG_NODE_ID, node.getId());
		metadata.put(DocumentMetadataConstant.VECTOR_TYPE, DocumentMetadataConstant.KNOWLEDGE_GRAPH);
		vectorStoreService.deleteDocumentsByMetedata(node.getAgentId().toString(), metadata);
	}

	private String buildNodeContent(KnowledgeGraphNode node) {
		StringBuilder content = new StringBuilder();
		content.append("Node: ").append(node.getNodeName()).append("\n");
		if (node.getDisplayName() != null) {
			content.append("Display Name: ").append(node.getDisplayName()).append("\n");
		}
		content.append("Type: ").append(node.getNodeType()).append("\n");
		if (node.getDescription() != null) {
			content.append("Description: ").append(node.getDescription());
		}
		return content.toString();
	}

	private Map<String, Object> buildNodeMetadata(KnowledgeGraphNode node) {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put(Constant.AGENT_ID, node.getAgentId().toString());
		metadata.put(DocumentMetadataConstant.VECTOR_TYPE, DocumentMetadataConstant.KNOWLEDGE_GRAPH);
		metadata.put(DocumentMetadataConstant.DB_KG_NODE_ID, node.getId());
		metadata.put("node_type", node.getNodeType());
		metadata.put("node_name", node.getNodeName());
		if (node.getProperties() != null) {
			metadata.put("properties", node.getProperties());
		}
		return metadata;
	}

	private List<KnowledgeGraphNode> reconstructPath(Map<Long, Long> parentMap, Long sourceId, Long targetId) {
		List<Long> path = new ArrayList<>();
		Long current = targetId;

		while (current != null) {
			path.add(0, current);
			current = parentMap.get(current);
		}

		return nodeMapper.selectByIds(path);
	}

}
