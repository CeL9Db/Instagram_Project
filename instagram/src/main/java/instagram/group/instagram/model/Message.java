package instagram.group.instagram.model;

import java.time.LocalDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Getter;

@Entity
@Table(name = "message")
@Getter
@Setter
@NoArgsConstructor

public class Message
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_message;

    @ManyToOne
    @JoinColumn(name = "fromEntity")
    private Client fromEntity;

    @ManyToOne
    @JoinColumn(name = "toGroup")
    private Group toGroup;

    @ManyToOne
    @JoinColumn(name = "toUser")
    private Client toUser;

    private LocalDateTime date;

    private String message;
}
