package com.dudev.datingapp.topic.document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "conversation_topics")
@Getter
@Setter
@NoArgsConstructor
public class ConversationTopic {

    @Id
    private String id;

    private String category;

    private String text;

    private List<String> tags;

    private String locale;
}
