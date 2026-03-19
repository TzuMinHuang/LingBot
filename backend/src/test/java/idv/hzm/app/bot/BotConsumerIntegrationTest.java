package idv.hzm.app.bot;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

import idv.hzm.app.bot.config.RedisStreamConfig;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Redis Stream operations used by the consumer pipeline.
 * Verifies XADD writes, XACK acknowledgements, and DLQ routing
 * against a real Redis instance managed by Testcontainers.
 */
@Tag("slow")
class BotConsumerIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

	private static String valueOf(Object obj) {
		return obj == null ? null : obj.toString();
	}

	@Test
	void xadd_writesToRequestStream() {
		Map<String, String> record = Map.of(
				"sessionId", "TEST00000001",
				"content", "hello integration test",
				"interactionId", "int-test-001");

		// XADD to request stream
		var recordId = reactiveRedisTemplate.opsForStream()
				.add(RedisStreamConfig.REQUEST_STREAM, record)
				.block(Duration.ofSeconds(5));

		assertThat(recordId).isNotNull();
		assertThat(recordId.getValue()).isNotBlank();

		// Verify the record exists in the stream
		StepVerifier.create(
				reactiveRedisTemplate.opsForStream()
						.read(StreamReadOptions.empty().count(10),
								StreamOffset.create(RedisStreamConfig.REQUEST_STREAM, ReadOffset.from("0")))
						.cast(MapRecord.class)
						.filter(r -> "TEST00000001".equals(valueOf(r.getValue().get("sessionId"))))
						.next())
				.assertNext(r -> {
					assertThat(valueOf(r.getValue().get("content"))).isEqualTo("hello integration test");
					assertThat(valueOf(r.getValue().get("interactionId"))).isEqualTo("int-test-001");
				})
				.verifyComplete();
	}

	@Test
	void xack_acknowledgesProcessedRecord() {
		String testGroup = "test-ack-group";
		String stream = "stream:test:ack";

		// Create stream and consumer group
		reactiveRedisTemplate.opsForStream()
				.add(stream, Map.of("init", "true"))
				.block(Duration.ofSeconds(5));
		reactiveRedisTemplate.opsForStream()
				.createGroup(stream, testGroup)
				.block(Duration.ofSeconds(5));

		// XADD a record
		reactiveRedisTemplate.opsForStream()
				.add(stream, Map.of("sessionId", "TEST00000002", "content", "ack-test"))
				.block(Duration.ofSeconds(5));

		// Read via consumer group (XREADGROUP)
		StepVerifier.create(
				reactiveRedisTemplate.opsForStream()
						.read(org.springframework.data.redis.connection.stream.Consumer.from(testGroup, "consumer-1"),
								StreamReadOptions.empty().count(10),
								StreamOffset.create(stream, ReadOffset.lastConsumed()))
						.next()
						.flatMap(record ->
								// ACK the record
								reactiveRedisTemplate.opsForStream()
										.acknowledge(testGroup, record)
						))
				.assertNext(ackCount -> assertThat(ackCount).isPositive())
				.verifyComplete();
	}

	@Test
	void dlqRouting_writesFailedRecordToDlqStream() {
		// Simulate DLQ write (same pattern as RequestConsumer.moveToDLQ)
		Map<String, String> dlqRecord = Map.of(
				"sessionId", "TEST00000003",
				"content", "failed message",
				"interactionId", "int-test-003",
				"error", "Simulated LLM timeout",
				"originalId", "0-0",
				"failedAt", String.valueOf(System.currentTimeMillis()));

		var dlqId = reactiveRedisTemplate.opsForStream()
				.add(RedisStreamConfig.BOT_DLQ_STREAM, dlqRecord)
				.block(Duration.ofSeconds(5));

		assertThat(dlqId).isNotNull();

		// Verify DLQ contains the error metadata
		StepVerifier.create(
				reactiveRedisTemplate.opsForStream()
						.read(StreamReadOptions.empty().count(10),
								StreamOffset.create(RedisStreamConfig.BOT_DLQ_STREAM, ReadOffset.from("0")))
						.cast(MapRecord.class)
						.filter(r -> "TEST00000003".equals(valueOf(r.getValue().get("sessionId"))))
						.next())
				.assertNext(r -> {
					assertThat(valueOf(r.getValue().get("error"))).isEqualTo("Simulated LLM timeout");
					assertThat(valueOf(r.getValue().get("originalId"))).isEqualTo("0-0");
					assertThat(valueOf(r.getValue().get("failedAt"))).isNotBlank();
				})
				.verifyComplete();
	}
}
