package instagram.group.instagram.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import instagram.group.instagram.model.Group;

public interface GroupRepository extends JpaRepository<Group, Integer> 
{

}