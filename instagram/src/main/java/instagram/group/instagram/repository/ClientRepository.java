package instagram.group.instagram.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import instagram.group.instagram.model.Client;

public interface ClientRepository extends JpaRepository<Client, Integer> 
{
       
}