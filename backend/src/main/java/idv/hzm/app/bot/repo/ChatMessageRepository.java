package idv.hzm.app.bot.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import idv.hzm.app.bot.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

	List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);
}
