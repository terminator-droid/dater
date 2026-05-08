package com.dudev.datingapp.topic.repository;

import com.dudev.datingapp.topic.document.ConversationTopic;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TopicRepository extends MongoRepository<ConversationTopic, String> {

    List<ConversationTopic> findByCategory(String category);
}
