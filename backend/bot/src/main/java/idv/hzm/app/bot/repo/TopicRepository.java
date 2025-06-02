package idv.hzm.app.bot.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import idv.hzm.app.bot.entity.Topic;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

	Optional<Topic> findByName(String topicName);

}
