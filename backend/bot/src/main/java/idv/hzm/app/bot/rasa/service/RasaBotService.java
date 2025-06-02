package idv.hzm.app.bot.rasa.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import idv.hzm.app.bot.rasa.dto.RasaNluResponse;

import reactor.core.publisher.Mono;

@Service
public class RasaBotService {

	@Value("${rasa.base-url}")
    private String url;

//	public Mono<List<EventDto>> sendMessage(String userId, String message) {
//		return this.webClient.post().uri("/webhooks/rest/webhook")
//				.bodyValue(Map.of("sender", userId, "message", message)).retrieve().bodyToFlux(RasaNluResponse.class)
//				.map(r -> new EventDto(userId, r.getText())).collectList().map(list -> {
//					list.add(0, new EventDto(userId, message));
//					return list;
//				});// 有可能為空值
//	}

	public Mono<RasaNluResponse> parseMessage(String message) {
		Map<String, String> requestBody = Map.of("text", message);
		final WebClient webClient = WebClient.create(url);
		return webClient.post().uri("/model/parse") // Rasa 預設 NLU API 路徑
				.bodyValue(requestBody).retrieve().bodyToMono(RasaNluResponse.class);
	}

	private final RestTemplate restTemplate = new RestTemplate();
	@Value("${rasa.base-url}")
	private String RASA_URL;

	public RasaNluResponse parse(String message) {
		Map<String, String> body = Map.of("text", message);
		ResponseEntity<RasaNluResponse> response = this.restTemplate.postForEntity(RASA_URL+"/model/parse", body,
				RasaNluResponse.class);
		return response.getBody();
	}
}
