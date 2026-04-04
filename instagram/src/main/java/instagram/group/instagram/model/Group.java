package instagram.group.instagram.model;

import java.time.LocalDateTime;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.CascadeType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "groupe")
@Getter
@Setter
@NoArgsConstructor

public class Group
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_group;
    private String name_group;

    @ManyToOne
    @JoinColumn(name = "admin")
    private Client admin;
    
    private LocalDateTime date_reg;

    @ManyToMany
    @JoinTable(
        name = "group_members",
        joinColumns = @JoinColumn(name = "id_group"),
        inverseJoinColumns = @JoinColumn(name = "id_member")
    )
    private List<Client> members;
}
