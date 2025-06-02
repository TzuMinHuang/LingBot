package idv.hzm.app.bot.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import idv.hzm.app.bot.entity.ProcessEventLog;

@Repository
public interface ProcessEventLogRepository extends JpaRepository<ProcessEventLog, Long> {
	// List<ProcessEventLog> findByProcess_Id(UUID processId);

	// List<ProcessEventLog> findByStep_Id(Long stepId);
}
