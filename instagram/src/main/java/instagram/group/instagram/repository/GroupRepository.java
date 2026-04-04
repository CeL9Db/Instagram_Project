package instagram.group.instagram.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import instagram.group.instagram.model.Group;

public interface GroupRepository extends JpaRepository<Group, Integer> 
{
    @Query("SELECT g FROM Group g LEFT JOIN FETCH g.members WHERE g.name_group = :name_group")
    Optional<Group> findByName_group(@Param("name_group") String name_group);

    @Query("SELECT DISTINCT g FROM Group g JOIN g.members m WHERE m.nickname = :nickname")
    List<Group> findByMemberNickname(@Param("nickname") String nickname);
}