package idv.hzm.app.portal.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import idv.hzm.app.portal.entity.Session;

public interface SessionsRepository extends JpaRepository<Session, String> {
}
