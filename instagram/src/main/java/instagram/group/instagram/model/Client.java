package instagram.group.instagram.model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "client")
@Getter
@Setter
@NoArgsConstructor

public class Client
{
    @Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_client;

    private String email;
    private String password;
    private String nickname;
    private boolean status;
    private LocalDateTime date_reg;
    private String avatar;
    private String bio;

    @OneToOne(mappedBy = "id_client", cascade = CascadeType.ALL)
    private UI ui;

    @ManyToMany(mappedBy = "members")
    private List<Group> groupes;
}