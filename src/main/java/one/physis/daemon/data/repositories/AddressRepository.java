package one.physis.daemon.data.repositories;

import one.physis.daemon.data.entities.Address;
import one.physis.daemon.data.entities.Project;
import org.springframework.data.repository.CrudRepository;

public interface AddressRepository extends CrudRepository<Address, Integer> {

   Address findTopByTakenAndProject(boolean taken, Project project);
}
