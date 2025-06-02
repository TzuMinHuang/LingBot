package idv.hzm.app.bot.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import idv.hzm.app.bot.entity.RobIntent;

@Repository
public interface RobIntentRepository extends JpaRepository<RobIntent, Long> {
	Optional<RobIntent> findByName(String name);
}
