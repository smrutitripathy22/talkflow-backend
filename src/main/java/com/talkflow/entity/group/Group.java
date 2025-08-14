package com.talkflow.entity.group;

import com.talkflow.entity.auth.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat-groups")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String groupName;


    private String groupPicLink;


    private Boolean isDeleted;

    @JoinColumn(name = "created_by_id", nullable = false)
    @ManyToOne
    private User createdBy;

    @CreationTimestamp
    private LocalDateTime createdOn;
}
