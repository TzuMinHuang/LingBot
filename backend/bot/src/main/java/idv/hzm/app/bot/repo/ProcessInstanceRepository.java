package idv.hzm.app.bot.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import idv.hzm.app.bot.entity.ProcessInstance;

@Repository
public interface ProcessInstanceRepository extends JpaRepository<ProcessInstance, UUID> {

	// public List<ProcessInstance> findByStatus(String status);

	public Optional<ProcessInstance> findByChatSessionIdAndStatus(String chatSessionId, String status);

	// public Optional<ProcessInstance> findById(UUID id);
}
