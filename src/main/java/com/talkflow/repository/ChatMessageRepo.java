package com.talkflow.repository;

import com.talkflow.entity.chatMessage.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepo extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatId(String chatId);

    @Query("SELECT cm FROM ChatMessage cm " + "WHERE (cm.sender.userId = :userId OR cm.recipient.userId = :userId) " + "AND cm.sender IS NOT NULL AND cm.recipient IS NOT NULL")
    List<ChatMessage> findChatsByUserId(@Param("userId") Long userId);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.group.id = :groupId")
    List<ChatMessage> findChatsByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT COUNT(cm) > 0 FROM ChatMessage cm " + "JOIN cm.recipient r " + "WHERE (cm.sender.userId = :userId OR cm.recipient.userId = :userId) " + "AND cm.sender IS NOT NULL AND cm.recipient IS NOT NULL " + "AND r.isActive = true")
    boolean existsChatWithActiveRecipient(@Param("userId") Long userId);


}
