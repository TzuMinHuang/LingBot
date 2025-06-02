package idv.hzm.app.portal.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import idv.hzm.app.portal.entity.Event;

public interface EventsRepository extends JpaRepository<Event, Long> {
}
