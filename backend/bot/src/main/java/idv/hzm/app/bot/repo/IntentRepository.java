package idv.hzm.app.bot.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import idv.hzm.app.bot.entity.Intent;
import idv.hzm.app.bot.entity.IntentInfoDTO;

@Repository
public interface IntentRepository extends JpaRepository<Intent, Integer> {
	@Query(value = """
			SELECT
			    t1.id AS intentId,
			    t1.name AS intentName,
			    t2.id AS subtopicId,
			    t2.name AS subtopicName,
			    t2.status AS subtopicStatus,
			    t2.priority AS subtopicPriority,
			    t3.id AS topicId,
			    t3.name AS topicName,
			    t3.status AS topicStatus
			FROM intent t1
			JOIN subtopic t2 ON t2.id = t1.subtopic_id 
			JOIN topic t3 ON t3.id = t2.topic_id
			WHERE t1.name = :intentName
			""", nativeQuery = true)
	List<IntentInfoDTO> findIntentDetailByName(@Param("intentName") String intentName);
}	

