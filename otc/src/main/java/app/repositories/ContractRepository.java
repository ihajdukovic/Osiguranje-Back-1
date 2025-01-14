package app.repositories;

import app.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractRepository  extends JpaRepository<Contract, Long> {
}
