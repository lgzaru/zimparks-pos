package com.tenten.zimparks.shift;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ShiftRepository extends JpaRepository<Shift, String> {
    Optional<Shift> findTopByOperatorOrderByStartFullDesc(String operator);
    List<Shift> findByStatusAndOperatorIn(String status, List<String> operators);
}
