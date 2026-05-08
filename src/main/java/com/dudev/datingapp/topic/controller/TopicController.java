package com.dudev.datingapp.topic.controller;

import com.dudev.datingapp.common.ApiResponse;
import com.dudev.datingapp.topic.dto.TopicDto;
import com.dudev.datingapp.topic.service.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/topics")
@RequiredArgsConstructor
@Tag(name = "Topics", description = "Conversation topic catalogue (from MongoDB)")
public class TopicController {

    private final TopicService topicService;

    @GetMapping
    @Operation(summary = "List conversation topics, optionally filtered by category")
    public ApiResponse<List<TopicDto>> findAll(@RequestParam(required = false) String category) {
        return ApiResponse.ok(topicService.findAll(category));
    }
}
