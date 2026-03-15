package idv.hzm.app.bot.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

/**
 * Tracks frequently asked questions using a Redis Sorted Set. Each question is
 * a member; its score is the ask count.
 *
 * Key: bot:faq:frequency
 */
@Service
public class SuggestionService {

	private static final Logger logger = LoggerFactory.getLogger(SuggestionService.class);
	private static final String FAQ_KEY = "bot:faq:frequency";
	private static final int DEFAULT_TOP_N = 10;

	@Autowired
	private StringRedisTemplate redisTemplate;

	/**
	 * Record that a question was asked (increment its score by 1).
	 */
	public void recordQuestion(String question) {
		if (question == null || question.isBlank())
			return;
		String normalized = question.trim();
		redisTemplate.opsForZSet().incrementScore(FAQ_KEY, normalized, 1);
	}

	/**
	 * Get the top N most frequently asked questions.
	 */
	public List<String> getTopSuggestions(int n) {
		Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet().reverseRangeWithScores(FAQ_KEY, 0,
				n - 1);
		if (tuples == null || tuples.isEmpty()) {
			return Collections.emptyList();
		}
		return tuples.stream().map(ZSetOperations.TypedTuple::getValue).toList();
	}

	/**
	 * Get top suggestions with default count.
	 */
	public List<String> getTopSuggestions() {
		return getTopSuggestions(DEFAULT_TOP_N);
	}

	/**
	 * Seed initial FAQ data if the sorted set is empty.
	 */
	public void seedIfEmpty(List<String> defaultQuestions) {
		Long size = redisTemplate.opsForZSet().zCard(FAQ_KEY);
		if (size != null && size > 0)
			return;

		logger.info("Seeding {} default FAQ questions", defaultQuestions.size());
		for (int i = 0; i < defaultQuestions.size(); i++) {
			// Give higher initial score to items earlier in the list
			double score = defaultQuestions.size() - i;
			redisTemplate.opsForZSet().add(FAQ_KEY, defaultQuestions.get(i), score);
		}
	}
}
