package instagram.group.instagram.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import instagram.group.instagram.model.Client;

public interface ClientRepository extends JpaRepository<Client, Integer> 
{
    Optional<Client> findByEmail(String email);
    Optional<Client> findByNickname(String nickname);
}
