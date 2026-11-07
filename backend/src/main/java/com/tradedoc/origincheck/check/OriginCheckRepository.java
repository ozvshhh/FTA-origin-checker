package com.tradedoc.origincheck.check;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OriginCheckRepository extends JpaRepository<OriginCheck, Long> {

    Page<OriginCheck> findByUserId(Long userId, Pageable pageable);

    Page<OriginCheck> findByUserIdAndStatus(Long userId, CheckStatus status, Pageable pageable);

    Optional<OriginCheck> findByIdAndUserId(Long id, Long userId);

    List<OriginCheck> findTop20ByStatusOrderByIdAsc(CheckStatus status);

    /** 끝났는데 agent 쪽 파일이 아직 남은 건 */
    List<OriginCheck> findTop20ByStatusInAndAgentFileIdIsNotNullOrderByIdAsc(List<CheckStatus> statuses);
}
