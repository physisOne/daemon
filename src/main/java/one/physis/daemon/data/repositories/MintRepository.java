package one.physis.daemon.data.repositories;

import one.physis.daemon.data.entities.Mint;
import one.physis.daemon.data.entities.Project;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface MintRepository extends CrudRepository<Mint, String> {

   List<Mint> getAllByStateAndProject(int state, Project project);

   List<Mint> getAllByStateAndProjectAndCustomerTransactionIsNull(int state, Project project);
}
