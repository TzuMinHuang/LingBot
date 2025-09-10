package idv.hzm.app.bot.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import idv.hzm.app.bot.entity.Event;

public interface EventsRepository extends JpaRepository<Event, Long> {
}
