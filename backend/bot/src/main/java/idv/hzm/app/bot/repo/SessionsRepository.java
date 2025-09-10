package idv.hzm.app.bot.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import idv.hzm.app.bot.entity.Session;

public interface SessionsRepository extends JpaRepository<Session, String> {
}
