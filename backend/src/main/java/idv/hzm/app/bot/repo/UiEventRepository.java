package idv.hzm.app.bot.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import idv.hzm.app.bot.entity.UiEvent;

public interface UiEventRepository extends JpaRepository<UiEvent, Long> {

	List<UiEvent> findBySessionIdOrderBySeqIndexAsc(String sessionId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT COALESCE(MAX(e.seqIndex), 0) FROM UiEvent e WHERE e.sessionId = :sessionId")
	int findMaxSeqIndex(String sessionId);
}
