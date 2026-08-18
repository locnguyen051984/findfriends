package com.phaithanhcong.user.repository;

import com.phaithanhcong.user.model.CallLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CallLogRepository extends JpaRepository<CallLog, Long> {
    List<CallLog> findByCallerIdAndCalleeIdOrCalleeIdAndCallerId(
            Long callerId1, Long calleeId1, Long callerId2, Long calleeId2);
}