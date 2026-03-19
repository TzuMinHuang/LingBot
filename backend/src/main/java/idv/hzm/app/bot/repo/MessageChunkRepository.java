package idv.hzm.app.bot.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import idv.hzm.app.bot.entity.MessageChunk;

public interface MessageChunkRepository extends JpaRepository<MessageChunk, Long> {

	List<MessageChunk> findByMessageIdAndSeqIndexGreaterThanOrderBySeqIndexAsc(String messageId, int seqIndex);

	List<MessageChunk> findByMessageIdOrderBySeqIndexAsc(String messageId);
}
