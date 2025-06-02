package idv.hzm.app.bot.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import idv.hzm.app.bot.entity.RobExample;

@Repository
public interface RobExampleRepository extends JpaRepository<RobExample, Long> {
	// public List<RobExample> findByIntent_Name(String intentName);
}
