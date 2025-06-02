package idv.hzm.app.admin.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import idv.hzm.app.admin.entity.Pair;

public interface PairRepository extends JpaRepository<Pair, Long> {
    List<Pair> findByAgentIdAndStatus(String agentId, String status);
}

