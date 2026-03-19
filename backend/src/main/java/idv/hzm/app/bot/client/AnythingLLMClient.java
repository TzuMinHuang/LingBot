package idv.hzm.app.bot.client;

import java.time.Duration;
import java.util.Map;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Flux;

@Component
public class AnythingLLMClient {

	private static final Logger logger = LoggerFactory.getLogger(AnythingLLMClient.class);

	@Value("${app.stream-timeout-minutes:3}")
	private int streamTimeoutMinutes;

	@Value("${anythingllm.base-url}")
	private String baseUrl;

	@Value("${anythingllm.api-key}")
	private String apiKey;

	@Value("${anythingllm.workspace-slug}")
	private String workspaceSlug;

	@Autowired
	private WebClient.Builder webClientBuilder;

	@Autowired
	private ObjectMapper objectMapper;

	private WebClient webClient;

	@PostConstruct
	public void init() {
		this.webClient = webClientBuilder.baseUrl(baseUrl)
				.defaultHeader("Authorization", "Bearer " + apiKey)
				.build();
	}

	/**
	 * Streaming：呼叫 AnythingLLM /stream-chat，回傳每個解析後的 SSE 事件 Map。 close:true 的 chunk
	 * 也會穿透，讓 RequestConsumer 從中取出 sources。 error:true 的事件會被過濾掉。
	 */
	public Flux<Map<String, Object>> chatStream(String message, String sessionId) {
		String url = "/api/v1/workspace/" + workspaceSlug + "/stream-chat";

		Map<String, Object> body = Map.of("message", message, "mode", "chat", "sessionId", sessionId);

		ParameterizedTypeReference<ServerSentEvent<String>> sseType = new ParameterizedTypeReference<>() {
		};

		return webClient.post().uri(url)
				.contentType(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_EVENT_STREAM).bodyValue(body).retrieve()
				.bodyToFlux(sseType).timeout(Duration.ofMinutes(streamTimeoutMinutes)).mapNotNull(ServerSentEvent::data).flatMap(this::parseChunk)
				.doOnError(e -> logger.error("AnythingLLM stream error: {}", e.getMessage()));
	}

	@SuppressWarnings("unchecked")
	private Flux<Map<String, Object>> parseChunk(String json) {
		try {
			Map<String, Object> event = objectMapper.readValue(json, Map.class);
			logger.debug("Parsed chunk from AnythingLLM: {}", json);
			if (Boolean.TRUE.equals(event.get("error"))) {
				logger.warn("AnythingLLM stream error event: {}", event.get("textResponse"));
				return Flux.empty();
			}
			// close:true 穿透，讓 RequestConsumer 取 sources
			return Flux.just(event);
		} catch (Exception e) {
			logger.error("Failed to parse SSE chunk: {} error: {}", json, e.getMessage());
			return Flux.empty();
		}
	}
}
