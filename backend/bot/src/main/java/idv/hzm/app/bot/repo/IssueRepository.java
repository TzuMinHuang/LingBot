package idv.hzm.app.bot.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import idv.hzm.app.bot.entity.Issue;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Integer> {

	public Optional<Issue> findBySessionIdAndStatus(String sessionId, String string);
	// 可自定義查詢，例如：
	// List<Issue> findBySessionId(Integer sessionId);
}