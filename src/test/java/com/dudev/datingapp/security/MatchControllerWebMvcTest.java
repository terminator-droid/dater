package com.dudev.datingapp.security;

import com.dudev.datingapp.match.controller.MatchController;
import com.dudev.datingapp.match.dto.MatchSummaryDto;
import com.dudev.datingapp.match.entity.MatchStatus;
import com.dudev.datingapp.match.service.MatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MatchController.class)
@Import(SecurityConfig.class)
class MatchControllerWebMvcTest {

    static final String USER_ID = "00000000-0000-0000-0000-000000000001";

    @Autowired MockMvc mockMvc;

    @MockitoBean MatchService matchService;
    @MockitoBean JwtService jwtService;
    @MockitoBean CustomUserDetailsService customUserDetailsService;
    @MockitoBean OnlineStatusInterceptor onlineStatusInterceptor;

    @BeforeEach
    void setUp() throws Exception {
        when(onlineStatusInterceptor.preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class), any()))
                .thenReturn(true);
    }

    @Test
    void getMatches_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/matches"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = USER_ID)
    void getMatches_authenticated_returns200() throws Exception {
        UUID matchId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        when(matchService.getMatches(any())).thenReturn(List.of(
                new MatchSummaryDto(matchId, venueId, "Bar Name", LocalDate.now(), MatchStatus.PENDING)
        ));

        mockMvc.perform(get("/api/v1/matches")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = USER_ID)
    void getMatchDetail_authenticated_returns200() throws Exception {
        UUID matchId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        when(matchService.getMatchDetail(any(), any())).thenReturn(
                new com.dudev.datingapp.match.dto.MatchDetailDto(
                        matchId, venueId, "Bar Name", "Малая Бронная ул., 32", LocalDate.now(),
                        MatchStatus.PENDING, "Beer", List.of(), null
                )
        );

        mockMvc.perform(get("/api/v1/matches/" + matchId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.venueName").value("Bar Name"));
    }
}
