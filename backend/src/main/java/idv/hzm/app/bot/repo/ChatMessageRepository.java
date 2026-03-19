package idv.hzm.app.bot.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import idv.hzm.app.bot.entity.ChatMessage;
import idv.hzm.app.bot.entity.MessageStatus;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

	List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

	Optional<ChatMessage> findByInteractionIdAndRole(String interactionId, idv.hzm.app.bot.entity.MessageRole role);

	@Modifying
	@Query("UPDATE ChatMessage m SET m.status = :status WHERE m.messageId = :messageId")
	int updateStatus(String messageId, MessageStatus status);
}
