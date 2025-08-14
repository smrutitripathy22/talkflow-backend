package com.talkflow.repository;


import com.talkflow.entity.connection.ConnectionStatus;
import com.talkflow.entity.connection.UserConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserConnectionRepo extends JpaRepository<UserConnection, Long> {
    @Query("  Select uc from UserConnection uc " + " where (uc.receiverId = :id or uc.senderId =:id) " + " and uc.status = 'ACCEPTED' ")
    List<UserConnection> getAllAcceptedRequest(@Param("id") Long id);

    @Query("  Select uc from UserConnection uc " + " where uc.receiverId = :id " + " and uc.status = 'PENDING' ")
    List<UserConnection> getAllPendingRequest(@Param("id") Long id);

    @Query("  Select uc from UserConnection uc " + " where uc.blockedByUserId = :id " + " and uc.status = 'BLOCKED' ")
    List<UserConnection> getAllBlockedRequest(@Param("id") Long id);

    @Query("  Select uc from UserConnection uc " + " where uc.senderId = :id " + " and uc.status = 'PENDING' ")
    List<UserConnection> getAllSentRequest(@Param("id") Long id);

    @Query("SELECT uc FROM UserConnection uc " + "WHERE (uc.receiverId = :id OR uc.senderId = :id) " + "AND uc.status IN :statuses")
    List<UserConnection> getAllRequestWithSomeConnection(@Param("id") Long id, @Param("statuses") Collection<ConnectionStatus> statuses);


    @Query("  Select uc from UserConnection uc " + " where (uc.receiverId = :senderId and uc.senderId =:receiverId) " + " or (uc.receiverId = :receiverId and uc.senderId =:senderId) ")
    Optional<UserConnection> areUserConnected(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);

    @Query("SELECT uc FROM UserConnection uc " + "WHERE ((uc.senderId = :userId1 AND uc.receiverId = :userId2) " + "   OR (uc.senderId = :userId2 AND uc.receiverId = :userId1)) " + "AND uc.status = 'ACCEPTED'")
    Optional<UserConnection> findAcceptedConnectionBetween(@Param("userId1") Long userId1, @Param("userId2") Long userId2);


}
