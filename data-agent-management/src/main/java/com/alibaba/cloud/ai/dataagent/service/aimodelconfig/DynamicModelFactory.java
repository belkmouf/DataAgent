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
package com.alibaba.cloud.ai.dataagent.service.aimodelconfig;

import com.alibaba.cloud.ai.dataagent.dto.ModelConfigDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.ai.vertexai.gemini.api.VertexAiGeminiApi;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class DynamicModelFactory {

	/**
	 * 根据 provider 创建对应的 ChatModel 实例
	 * 支持: OpenAI-compatible, Anthropic (Claude), Gemini (Vertex AI)
	 */
	public ChatModel createChatModel(ModelConfigDTO config) {

		log.info("Creating NEW ChatModel instance. Provider: {}, Model: {}, BaseUrl: {}", config.getProvider(),
				config.getModelName(), config.getBaseUrl());

		// 1. 验证参数
		checkBasic(config);

		// 2. 根据 provider 类型路由到不同的实现
		String provider = config.getProvider().toLowerCase();

		if (provider.contains("anthropic") || provider.contains("claude")) {
			return createAnthropicChatModel(config);
		}
		else if (provider.contains("gemini") || provider.contains("vertex")) {
			return createGeminiChatModel(config);
		}
		else {
			// 默认使用 OpenAI-compatible 实现 (支持 OpenAI, DeepSeek, Qwen 等)
			return createOpenAiChatModel(config);
		}
	}

	/**
	 * 创建 OpenAI-compatible ChatModel
	 */
	private ChatModel createOpenAiChatModel(ModelConfigDTO config) {
		OpenAiApi.Builder apiBuilder = OpenAiApi.builder().apiKey(config.getApiKey()).baseUrl(config.getBaseUrl());

		if (StringUtils.hasText(config.getCompletionsPath())) {
			apiBuilder.completionsPath(config.getCompletionsPath());
		}
		OpenAiApi openAiApi = apiBuilder.build();

		OpenAiChatOptions openAiChatOptions = OpenAiChatOptions.builder()
			.model(config.getModelName())
			.temperature(config.getTemperature())
			.maxTokens(config.getMaxTokens())
			.build();

		return OpenAiChatModel.builder().openAiApi(openAiApi).defaultOptions(openAiChatOptions).build();
	}

	/**
	 * 创建 Anthropic (Claude) ChatModel
	 */
	private ChatModel createAnthropicChatModel(ModelConfigDTO config) {
		AnthropicApi anthropicApi = new AnthropicApi(config.getApiKey());

		AnthropicChatOptions anthropicOptions = AnthropicChatOptions.builder()
			.model(config.getModelName())
			.temperature(config.getTemperature())
			.maxTokens(config.getMaxTokens())
			.build();

		return AnthropicChatModel.builder()
			.anthropicApi(anthropicApi)
			.defaultOptions(anthropicOptions)
			.build();
	}

	/**
	 * 创建 Gemini (Vertex AI) ChatModel
	 */
	private ChatModel createGeminiChatModel(ModelConfigDTO config) {
		// Vertex AI Gemini API 需要项目信息
		// baseUrl 格式: https://{region}-aiplatform.googleapis.com/v1/projects/{projectId}/locations/{location}
		VertexAiGeminiApi geminiApi = new VertexAiGeminiApi(
			config.getBaseUrl(),
			config.getApiKey()
		);

		VertexAiGeminiChatOptions geminiOptions = VertexAiGeminiChatOptions.builder()
			.model(config.getModelName())
			.temperature(config.getTemperature().floatValue())
			.maxOutputTokens(config.getMaxTokens())
			.build();

		return new VertexAiGeminiChatModel(geminiApi, geminiOptions);
	}

	private static void checkBasic(ModelConfigDTO config) {
		Assert.hasText(config.getBaseUrl(), "baseUrl must not be empty");
		Assert.hasText(config.getApiKey(), "apiKey must not be empty");
		Assert.hasText(config.getModelName(), "modelName must not be empty");
	}

	/**
	 * 创建 EmbeddingModel
	 * 注意: Anthropic 和 Gemini 本身不直接提供 embedding API
	 * 建议使用 OpenAI-compatible embedding 服务 (如 text-embedding-3-small, Qwen embeddings 等)
	 */
	public EmbeddingModel createEmbeddingModel(ModelConfigDTO config) {
		log.info("Creating NEW EmbeddingModel instance. Provider: {}, Model: {}, BaseUrl: {}", config.getProvider(),
				config.getModelName(), config.getBaseUrl());
		checkBasic(config);

		// 目前统一使用 OpenAI-compatible Embedding API
		// Anthropic 和 Gemini 用户需要配置兼容的 embedding 服务
		OpenAiApi.Builder apiBuilder = OpenAiApi.builder().apiKey(config.getApiKey()).baseUrl(config.getBaseUrl());

		if (StringUtils.hasText(config.getEmbeddingsPath())) {
			apiBuilder.embeddingsPath(config.getEmbeddingsPath());
		}

		OpenAiApi openAiApi = apiBuilder.build();
		return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED,
				OpenAiEmbeddingOptions.builder().model(config.getModelName()).build(),
				RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}

}
