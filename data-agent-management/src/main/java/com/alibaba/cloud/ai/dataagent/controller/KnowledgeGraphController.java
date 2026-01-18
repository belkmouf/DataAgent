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
package com.alibaba.cloud.ai.dataagent.controller;

import com.alibaba.cloud.ai.dataagent.entity.KnowledgeGraphEdge;
import com.alibaba.cloud.ai.dataagent.entity.KnowledgeGraphNode;
import com.alibaba.cloud.ai.dataagent.service.knowledgegraph.KnowledgeGraphService;
import com.alibaba.cloud.ai.dataagent.vo.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/knowledge-graph")
@CrossOrigin(origins = "*")
@AllArgsConstructor
public class KnowledgeGraphController {

	private final KnowledgeGraphService knowledgeGraphService;

	// ========== Node Endpoints ==========

	@GetMapping("/nodes")
	public ApiResponse<List<KnowledgeGraphNode>> listNodes(@RequestParam(value = "agentId") Integer agentId,
			@RequestParam(value = "nodeType", required = false) String nodeType,
			@RequestParam(value = "keyword", required = false) String keyword) {

		List<KnowledgeGraphNode> result;

		if (StringUtils.hasText(keyword)) {
			result = knowledgeGraphService.searchNodes(agentId, keyword);
		}
		else if (StringUtils.hasText(nodeType)) {
			result = knowledgeGraphService.getNodesByType(agentId, nodeType);
		}
		else {
			result = knowledgeGraphService.getNodesByAgentId(agentId);
		}

		return ApiResponse.success("success list nodes", result);
	}

	@GetMapping("/nodes/{id}")
	public ApiResponse<KnowledgeGraphNode> getNode(@PathVariable(value = "id") Long id) {
		KnowledgeGraphNode node = knowledgeGraphService.getNodeById(id);
		if (node == null) {
			return ApiResponse.error("Node not found");
		}
		return ApiResponse.success("success get node", node);
	}

	@PostMapping("/nodes")
	public ApiResponse<KnowledgeGraphNode> createNode(@RequestBody @Validated KnowledgeGraphNode node) {
		try {
			KnowledgeGraphNode created = knowledgeGraphService.createNode(node);
			return ApiResponse.success("success create node", created);
		}
		catch (Exception e) {
			log.error("Failed to create node: {}", e.getMessage());
			return ApiResponse.error("Failed to create node: " + e.getMessage());
		}
	}

	@PutMapping("/nodes/{id}")
	public ApiResponse<KnowledgeGraphNode> updateNode(@PathVariable(value = "id") Long id,
			@RequestBody KnowledgeGraphNode node) {
		node.setId(id);
		try {
			KnowledgeGraphNode updated = knowledgeGraphService.updateNode(node);
			return ApiResponse.success("success update node", updated);
		}
		catch (Exception e) {
			log.error("Failed to update node: {}", e.getMessage());
			return ApiResponse.error("Failed to update node: " + e.getMessage());
		}
	}

	@DeleteMapping("/nodes/{id}")
	public ApiResponse<Boolean> deleteNode(@PathVariable(value = "id") Long id) {
		try {
			knowledgeGraphService.deleteNode(id);
			return ApiResponse.success("success delete node");
		}
		catch (Exception e) {
			log.error("Failed to delete node: {}", e.getMessage());
			return ApiResponse.error("Failed to delete node: " + e.getMessage());
		}
	}

	@PostMapping("/nodes/batch")
	public ApiResponse<List<KnowledgeGraphNode>> batchCreateNodes(
			@RequestBody List<KnowledgeGraphNode> nodes) {
		try {
			List<KnowledgeGraphNode> created = knowledgeGraphService.batchCreateNodes(nodes);
			return ApiResponse.success("success batch create nodes", created);
		}
		catch (Exception e) {
			log.error("Failed to batch create nodes: {}", e.getMessage());
			return ApiResponse.error("Failed to batch create nodes: " + e.getMessage());
		}
	}

	// ========== Edge Endpoints ==========

	@GetMapping("/edges")
	public ApiResponse<List<KnowledgeGraphEdge>> listEdges(@RequestParam(value = "agentId") Integer agentId) {
		List<KnowledgeGraphEdge> result = knowledgeGraphService.getEdgesByAgentId(agentId);
		return ApiResponse.success("success list edges", result);
	}

	@GetMapping("/edges/by-node/{nodeId}")
	public ApiResponse<List<KnowledgeGraphEdge>> getEdgesByNode(@PathVariable(value = "nodeId") Long nodeId) {
		List<KnowledgeGraphEdge> result = knowledgeGraphService.getEdgesByNode(nodeId);
		return ApiResponse.success("success get edges by node", result);
	}

	@GetMapping("/edges/between")
	public ApiResponse<List<KnowledgeGraphEdge>> getEdgesBetween(
			@RequestParam(value = "sourceNodeId") Long sourceNodeId,
			@RequestParam(value = "targetNodeId") Long targetNodeId) {
		List<KnowledgeGraphEdge> result = knowledgeGraphService.getEdgesBetweenNodes(sourceNodeId, targetNodeId);
		return ApiResponse.success("success get edges between nodes", result);
	}

	@PostMapping("/edges")
	public ApiResponse<KnowledgeGraphEdge> createEdge(@RequestBody @Validated KnowledgeGraphEdge edge) {
		try {
			KnowledgeGraphEdge created = knowledgeGraphService.createEdge(edge);
			return ApiResponse.success("success create edge", created);
		}
		catch (Exception e) {
			log.error("Failed to create edge: {}", e.getMessage());
			return ApiResponse.error("Failed to create edge: " + e.getMessage());
		}
	}

	@PutMapping("/edges/{id}")
	public ApiResponse<KnowledgeGraphEdge> updateEdge(@PathVariable(value = "id") Long id,
			@RequestBody KnowledgeGraphEdge edge) {
		edge.setId(id);
		try {
			KnowledgeGraphEdge updated = knowledgeGraphService.updateEdge(edge);
			return ApiResponse.success("success update edge", updated);
		}
		catch (Exception e) {
			log.error("Failed to update edge: {}", e.getMessage());
			return ApiResponse.error("Failed to update edge: " + e.getMessage());
		}
	}

	@DeleteMapping("/edges/{id}")
	public ApiResponse<Boolean> deleteEdge(@PathVariable(value = "id") Long id) {
		try {
			knowledgeGraphService.deleteEdge(id);
			return ApiResponse.success("success delete edge");
		}
		catch (Exception e) {
			log.error("Failed to delete edge: {}", e.getMessage());
			return ApiResponse.error("Failed to delete edge: " + e.getMessage());
		}
	}

	// ========== Graph Query Endpoints ==========

	@GetMapping("/query/neighbors/{nodeId}")
	public ApiResponse<List<KnowledgeGraphNode>> getNeighbors(@PathVariable(value = "nodeId") Long nodeId) {
		List<KnowledgeGraphNode> result = knowledgeGraphService.getNeighbors(nodeId);
		return ApiResponse.success("success get neighbors", result);
	}

	@GetMapping("/query/shortest-path")
	public ApiResponse<List<KnowledgeGraphNode>> findShortestPath(
			@RequestParam(value = "sourceNodeId") Long sourceNodeId,
			@RequestParam(value = "targetNodeId") Long targetNodeId) {
		List<KnowledgeGraphNode> result = knowledgeGraphService.findShortestPath(sourceNodeId, targetNodeId);
		return ApiResponse.success("success find shortest path", result);
	}

	@GetMapping("/query/subgraph/{nodeId}")
	public ApiResponse<Map<String, Object>> getSubgraph(@PathVariable(value = "nodeId") Long nodeId,
			@RequestParam(value = "depth", defaultValue = "2") int depth) {
		Map<String, Object> result = knowledgeGraphService.getSubgraph(nodeId, depth);
		return ApiResponse.success("success get subgraph", result);
	}

	@GetMapping("/query/by-relationship/{nodeId}")
	public ApiResponse<List<KnowledgeGraphNode>> getNodesByRelationship(@PathVariable(value = "nodeId") Long nodeId,
			@RequestParam(value = "edgeType") String edgeType) {
		List<KnowledgeGraphNode> result = knowledgeGraphService.getNodesByRelationship(nodeId, edgeType);
		return ApiResponse.success("success get nodes by relationship", result);
	}

	@GetMapping("/stats")
	public ApiResponse<Map<String, Object>> getGraphStats(@RequestParam(value = "agentId") Integer agentId) {
		Map<String, Object> result = knowledgeGraphService.getGraphStats(agentId);
		return ApiResponse.success("success get graph stats", result);
	}

	// ========== Vector Store Integration Endpoints ==========

	@PostMapping("/vectorize/node/{nodeId}")
	public ApiResponse<Boolean> vectorizeNode(@PathVariable(value = "nodeId") Long nodeId) {
		try {
			knowledgeGraphService.vectorizeNode(nodeId);
			return ApiResponse.success("success vectorize node");
		}
		catch (Exception e) {
			log.error("Failed to vectorize node: {}", e.getMessage());
			return ApiResponse.error("Failed to vectorize node: " + e.getMessage());
		}
	}

	@PostMapping("/vectorize/batch")
	public ApiResponse<Boolean> batchVectorizeNodes(@RequestBody List<Long> nodeIds) {
		try {
			knowledgeGraphService.batchVectorizeNodes(nodeIds);
			return ApiResponse.success("success batch vectorize nodes");
		}
		catch (Exception e) {
			log.error("Failed to batch vectorize nodes: {}", e.getMessage());
			return ApiResponse.error("Failed to batch vectorize nodes: " + e.getMessage());
		}
	}

	@PostMapping("/vectorize/refresh")
	public ApiResponse<Boolean> refreshNodeEmbeddings(@RequestParam(value = "agentId") Integer agentId) {
		try {
			knowledgeGraphService.refreshNodeEmbeddings(agentId);
			return ApiResponse.success("success refresh node embeddings");
		}
		catch (Exception e) {
			log.error("Failed to refresh node embeddings: {}", e.getMessage());
			return ApiResponse.error("Failed to refresh node embeddings: " + e.getMessage());
		}
	}

	@GetMapping("/search/hybrid")
	public ApiResponse<List<KnowledgeGraphNode>> hybridSearch(@RequestParam(value = "agentId") Integer agentId,
			@RequestParam(value = "query") String query, @RequestParam(value = "topK", defaultValue = "10") int topK) {
		List<KnowledgeGraphNode> result = knowledgeGraphService.hybridSearch(agentId, query, topK);
		return ApiResponse.success("success hybrid search", result);
	}

}
