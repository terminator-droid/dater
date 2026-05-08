package com.dudev.datingapp.topic.service;

import com.dudev.datingapp.topic.document.ConversationTopic;
import com.dudev.datingapp.topic.dto.TopicDto;
import com.dudev.datingapp.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;

    public List<TopicDto> findAll(String category) {
        List<ConversationTopic> topics = (category != null && !category.isBlank())
                ? topicRepository.findByCategory(category)
                : topicRepository.findAll();
        return topics.stream().map(this::toDto).toList();
    }

    public List<TopicDto> findByIds(List<String> ids) {
        return topicRepository.findAllById(ids).stream().map(this::toDto).toList();
    }

    public TopicDto toDto(ConversationTopic topic) {
        return new TopicDto(topic.getId(), topic.getCategory(), topic.getText(), topic.getTags());
    }
}
