package gg.admin.repo.manage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import gg.data.manage.SlotManagement;

public interface AdminSlotManagementsRepository extends JpaRepository<SlotManagement, Long> {
	@Query("select slot from SlotManagement slot "
		+ "where slot.endTime > :nowTime or slot.startTime > :nowTime or slot.endTime = null "
		+ "order by slot.startTime desc")
	List<SlotManagement> findAfterNowSlotManagement(@Param("nowTime") LocalDateTime nowTime);

	List<SlotManagement> findAllByOrderByCreatedAtDesc();

	Optional<SlotManagement> findFirstByOrderByIdDesc();

}
