package com.dudev.datingapp.topic.dto;

import java.util.List;

public record TopicDto(
        String id,
        String category,
        String text,
        List<String> tags
) {
}
