package gg.admin.repo.store;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import gg.data.store.Receipt;

public interface ReceiptAdminRepository extends JpaRepository<Receipt, Long>, ReceiptAdminRepositorySearch {
	Page<Receipt> findAll(Pageable pageable);
}
