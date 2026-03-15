package idv.hzm.app.bot.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import idv.hzm.app.bot.entity.Session;

public interface SessionsRepository extends JpaRepository<Session, String> {

	List<Session> findByUserIdOrderByStartTimeDesc(String userId);

	Optional<Session> findByIdAndStatus(String id, String status);
}
