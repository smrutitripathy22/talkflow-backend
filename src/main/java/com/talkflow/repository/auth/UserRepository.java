package com.talkflow.repository.auth;

import com.talkflow.entity.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.isVerified = true")
    List<User> getAllRecords();

    @Override
    @Query("SELECT u FROM User u WHERE u.userId = :id AND u.isActive = true AND u.isVerified = true")
    Optional<User> findById(@Param("id") Long id);

    @Override
    @Query("SELECT u FROM User u WHERE u.userId IN :ids AND u.isActive = true AND u.isVerified = true")
    List<User> findAllById(@Param("ids") Iterable<Long> ids);


    @Query("SELECT u FROM User u WHERE u.userId IN :ids AND u.isVerified = true")
    List<User> getAllUsersById(@Param("ids") Iterable<Long> ids);




    @Modifying
    @Transactional
    @Query("DELETE FROM User u WHERE u.isVerified = false AND u.createdAt < :dateTime")
    int deleteUnverifiedOlderThan(LocalDateTime dateTime);


}
