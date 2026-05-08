package com.dudev.datingapp.topic;

import com.dudev.datingapp.topic.document.ConversationTopic;
import com.dudev.datingapp.topic.dto.TopicDto;
import com.dudev.datingapp.topic.repository.TopicRepository;
import com.dudev.datingapp.topic.service.TopicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopicServiceTest {

    @Mock private TopicRepository topicRepository;

    private TopicService topicService;

    @BeforeEach
    void setUp() {
        topicService = new TopicService(topicRepository);
    }

    @Test
    void findAll_nullCategory_returnsAllTopics() {
        when(topicRepository.findAll()).thenReturn(List.of(
                topic("t1", "icebreaker", "О чём думал?"),
                topic("t2", "drink", "Что пьёшь?")
        ));

        List<TopicDto> result = topicService.findAll(null);

        assertThat(result).hasSize(2);
    }

    @Test
    void findAll_withCategory_filtersTopics() {
        when(topicRepository.findByCategory("drink")).thenReturn(List.of(
                topic("t2", "drink", "Что пьёшь?")
        ));

        List<TopicDto> result = topicService.findAll("drink");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).category()).isEqualTo("drink");
    }

    @Test
    void findByIds_returnsMatchingTopics() {
        List<String> ids = List.of("t1", "t3");
        when(topicRepository.findAllById(ids)).thenReturn(List.of(
                topic("t1", "icebreaker", "О чём думал?"),
                topic("t3", "venue", "Как часто бываешь здесь?")
        ));

        List<TopicDto> result = topicService.findByIds(ids);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TopicDto::id).containsExactlyInAnyOrder("t1", "t3");
    }

    @Test
    void findAll_blankCategory_returnsAllTopics() {
        when(topicRepository.findAll()).thenReturn(List.of(topic("t1", "icebreaker", "Текст")));

        List<TopicDto> result = topicService.findAll("");

        assertThat(result).hasSize(1);
    }

    private ConversationTopic topic(String id, String category, String text) {
        ConversationTopic t = new ConversationTopic();
        t.setId(id);
        t.setCategory(category);
        t.setText(text);
        t.setTags(List.of());
        t.setLocale("ru");
        return t;
    }
}
