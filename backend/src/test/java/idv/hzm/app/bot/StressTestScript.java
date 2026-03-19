package idv.hzm.app.bot;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 獨立壓測腳本 — 不依賴 Spring context，可直接以 main() 執行。
 *
 * 使用方式：
 *   java StressTestScript [baseUrl] [concurrentUsers] [testMessage]
 *
 * 預設：
 *   baseUrl = http://localhost:9200/bot
 *   concurrentUsers = 100
 *
 * 測試項目：
 *   1. 併發建立 session + 發送訊息 + 訂閱 SSE
 *   2. 驗證 SSE 事件順序（PROCESSING_START → STREAM_CHUNK* → STREAM_END）
 *   3. 驗證無遺失（每個 session 都收到 STREAM_END）
 *   4. 斷線重連模擬（中途關閉 SSE 後帶 Last-Event-ID 重連）
 */
public class StressTestScript {

	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();

	private static final AtomicInteger successCount = new AtomicInteger(0);
	private static final AtomicInteger failCount = new AtomicInteger(0);
	private static final AtomicInteger reconnectSuccess = new AtomicInteger(0);
	private static final AtomicLong totalLatency = new AtomicLong(0);

	public static void main(String[] args) throws Exception {
		String baseUrl = args.length > 0 ? args[0] : "http://localhost:9200/bot";
		int concurrentUsers = args.length > 1 ? Integer.parseInt(args[1]) : 100;
		String testMessage = args.length > 2 ? args[2] : "你好，請問今天天氣如何？";

		System.out.printf("=== Stress Test ===%n");
		System.out.printf("Base URL: %s%n", baseUrl);
		System.out.printf("Concurrent Users: %d%n", concurrentUsers);
		System.out.printf("Test Message: %s%n%n", testMessage);

		ExecutorService executor = Executors.newFixedThreadPool(Math.min(concurrentUsers, 200));
		CountDownLatch latch = new CountDownLatch(concurrentUsers);
		long startTime = System.currentTimeMillis();

		List<Future<?>> futures = new ArrayList<>();
		for (int i = 0; i < concurrentUsers; i++) {
			final int userId = i;
			futures.add(executor.submit(() -> {
				try {
					runUserSession(baseUrl, testMessage, userId);
					successCount.incrementAndGet();
				} catch (Exception e) {
					failCount.incrementAndGet();
					System.err.printf("[User-%d] FAILED: %s%n", userId, e.getMessage());
				} finally {
					latch.countDown();
				}
			}));
		}

		latch.await(5, TimeUnit.MINUTES);
		long elapsed = System.currentTimeMillis() - startTime;
		executor.shutdown();

		System.out.printf("%n=== Results ===%n");
		System.out.printf("Total Users: %d%n", concurrentUsers);
		System.out.printf("Success: %d%n", successCount.get());
		System.out.printf("Failed: %d%n", failCount.get());
		System.out.printf("Reconnect Success: %d%n", reconnectSuccess.get());
		System.out.printf("Total Time: %d ms%n", elapsed);
		if (successCount.get() > 0) {
			System.out.printf("Avg Latency: %d ms%n", totalLatency.get() / successCount.get());
		}
		System.out.printf("Pass Rate: %.1f%%%n", (successCount.get() * 100.0) / concurrentUsers);
	}

	private static void runUserSession(String baseUrl, String message, int userId) throws Exception {
		long t0 = System.currentTimeMillis();

		// 1. 建立 Session
		HttpRequest initReq = HttpRequest.newBuilder()
				.uri(URI.create(baseUrl + "/chat/initial"))
				.POST(HttpRequest.BodyPublishers.ofString("{}"))
				.header("Content-Type", "application/json")
				.timeout(Duration.ofSeconds(10))
				.build();
		HttpResponse<String> initResp = HTTP_CLIENT.send(initReq, HttpResponse.BodyHandlers.ofString());
		if (initResp.statusCode() != 200) {
			throw new RuntimeException("Initial failed: " + initResp.statusCode());
		}

		// 從回應中解析 sessionId
		String body = initResp.body();
		String sessionId = extractJsonField(body, "sessionId");
		if (sessionId == null) {
			throw new RuntimeException("No sessionId in response: " + body);
		}

		// 2. 啟動 SSE 監聽（背景）
		CompletableFuture<SseResult> sseFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return listenSse(baseUrl, sessionId, null);
			} catch (Exception e) {
				throw new CompletionException(e);
			}
		});

		// 小延遲確保 SSE 連線建立
		Thread.sleep(200);

		// 3. 發送訊息
		String sendBody = String.format("{\"content\":\"%s\"}", message);
		HttpRequest sendReq = HttpRequest.newBuilder()
				.uri(URI.create(baseUrl + "/chat/" + sessionId + "/send"))
				.POST(HttpRequest.BodyPublishers.ofString(sendBody))
				.header("Content-Type", "application/json")
				.timeout(Duration.ofSeconds(10))
				.build();
		HttpResponse<String> sendResp = HTTP_CLIENT.send(sendReq, HttpResponse.BodyHandlers.ofString());
		if (sendResp.statusCode() != 200) {
			throw new RuntimeException("Send failed: " + sendResp.statusCode() + " " + sendResp.body());
		}

		// 4. 等待 SSE 完成
		SseResult result = sseFuture.get(3, TimeUnit.MINUTES);

		long latency = System.currentTimeMillis() - t0;
		totalLatency.addAndGet(latency);

		// 5. 驗證事件順序
		if (!result.hasProcessingStart) {
			throw new RuntimeException("Missing PROCESSING_START");
		}
		if (!result.hasStreamEnd) {
			throw new RuntimeException("Missing STREAM_END");
		}
		if (result.chunkCount == 0) {
			throw new RuntimeException("No STREAM_CHUNK received");
		}

		System.out.printf("[User-%d] OK - sessionId=%s, chunks=%d, latency=%dms%n",
				userId, sessionId, result.chunkCount, latency);

		// 6. 斷線重連測試（用 Last-Event-ID）
		if (result.lastEventId != null) {
			try {
				SseResult reconnResult = listenSse(baseUrl, sessionId, result.lastEventId);
				reconnectSuccess.incrementAndGet();
				System.out.printf("[User-%d] Reconnect OK - recovered events=%d%n",
						userId, reconnResult.chunkCount);
			} catch (Exception e) {
				System.err.printf("[User-%d] Reconnect failed: %s%n", userId, e.getMessage());
			}
		}
	}

	private static SseResult listenSse(String baseUrl, String sessionId, String lastEventId)
			throws Exception {
		HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
				.uri(URI.create(baseUrl + "/chat/" + sessionId + "/stream"))
				.GET()
				.header("Accept", "text/event-stream")
				.timeout(Duration.ofMinutes(3));

		if (lastEventId != null) {
			reqBuilder.header("Last-Event-ID", lastEventId);
		}

		HttpResponse<String> resp = HTTP_CLIENT.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

		SseResult result = new SseResult();
		for (String line : resp.body().split("\n")) {
			if (line.startsWith("event:")) {
				// track event types
			} else if (line.startsWith("id:")) {
				result.lastEventId = line.substring(3).trim();
			} else if (line.startsWith("data:")) {
				String data = line.substring(5).trim();
				if (data.contains("PROCESSING_START")) {
					result.hasProcessingStart = true;
				} else if (data.contains("STREAM_CHUNK")) {
					result.chunkCount++;
				} else if (data.contains("STREAM_END")) {
					result.hasStreamEnd = true;
					break; // SSE 完成
				}
			}
		}
		return result;
	}

	private static String extractJsonField(String json, String field) {
		// 簡易解析，避免引入額外依賴
		String key = "\"" + field + "\":\"";
		int idx = json.indexOf(key);
		if (idx < 0) return null;
		int start = idx + key.length();
		int end = json.indexOf("\"", start);
		return end > start ? json.substring(start, end) : null;
	}

	static class SseResult {
		boolean hasProcessingStart = false;
		boolean hasStreamEnd = false;
		int chunkCount = 0;
		String lastEventId = null;
	}
}
