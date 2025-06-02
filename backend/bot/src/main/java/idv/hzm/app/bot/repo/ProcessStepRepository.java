package idv.hzm.app.bot.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import idv.hzm.app.bot.entity.ProcessStep;

@Repository
public interface ProcessStepRepository extends JpaRepository<ProcessStep, Long> {
	// List<ProcessStep> findByProcess_Id(UUID processId);
}
