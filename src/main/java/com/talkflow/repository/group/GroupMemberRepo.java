package com.talkflow.repository.group;

import com.talkflow.entity.auth.User;
import com.talkflow.entity.group.Group;
import com.talkflow.entity.group.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupMemberRepo extends JpaRepository<GroupMember,Long> {
    List<GroupMember> findAllByGroupId(Long groupId);

    @Query("SELECT COUNT(gm) > 0 FROM GroupMember gm " +
            "WHERE gm.group.id = :groupId AND gm.user.userId = :userId ")
    boolean existsByGroupIdAndUserId(@Param("groupId") Long groupId,
                                     @Param("userId") Long userId);

    @Query("SELECT DISTINCT gm.group FROM GroupMember gm WHERE gm.user.userId = :userId")
    List<Group> findAllGroupByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT gm.user FROM GroupMember gm WHERE gm.group.id = :groupId and gm.user.isActive = true")
    List<User> getAllUsersInGroup(@Param("groupId") Long groupId);

}
