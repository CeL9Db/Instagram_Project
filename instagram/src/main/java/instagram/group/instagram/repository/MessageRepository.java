package instagram.group.instagram.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import instagram.group.instagram.model.Message;

public interface MessageRepository extends JpaRepository<Message, Integer> 
{
    
}