package one.physis.daemon.data.repositories;

import one.physis.daemon.data.entities.Project;
import org.springframework.data.repository.CrudRepository;

public interface ProjectRepository extends CrudRepository<Project, Integer> {
}
