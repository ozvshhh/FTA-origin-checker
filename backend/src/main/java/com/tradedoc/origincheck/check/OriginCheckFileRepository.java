package com.tradedoc.origincheck.check;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OriginCheckFileRepository extends JpaRepository<OriginCheckFile, Long> {

    List<OriginCheckFile> findByCheckIdOrderBySeqAsc(Long checkId);
}
