package com.example.api;

import com.example.api.activity.application.dto.CreateActivityRequestDto;
import com.example.api.activity.application.dto.LocationDto;
import com.example.api.shared.security.JwtService;
import com.example.api.user.domain.Role;
import com.example.api.user.infrastructure.UserEntity;
import com.example.api.user.infrastructure.UserJpaRepository;
import com.example.api.activityType.infrastructure.ActivityTypeEntity;
import com.example.api.activityType.infrastructure.ActivityTypeJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ActivityControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private ActivityTypeJpaRepository activityTypeJpaRepository;

    private String token;
    private Long activityTypeId;

    @BeforeEach
    void setUp() {
        UserEntity user = new UserEntity(
                "Int",
                "Test",
                "activity-it@entreprise.com",
                "hashed",
                Role.COLLABORATOR);
        user = userJpaRepository.save(user);

        ActivityTypeEntity type = new ActivityTypeEntity();
        type.setName("Sport");
        type = activityTypeJpaRepository.save(type);
        activityTypeId = type.getId();

        token = jwtService.generateToken(user.getId(), user.getEmail(), Role.COLLABORATOR);
    }

    @Test
    void should_return_201_when_authenticated_and_payload_valid() throws Exception {
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
                "Match foot",
                "Terrain synthétique",
                activityTypeId,
                LocalDate.now().plusDays(10),
                LocalTime.of(18, 0),
                LocalTime.of(20, 0),
                14,
                new LocationDto("Stade municipal", null, "44000", "Nantes"));

        mockMvc.perform(post("/activities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Match foot"))
                .andExpect(jsonPath("$.activityType.id").value(activityTypeId.intValue()));
    }

    @Test
    void should_return_401_when_not_authenticated() throws Exception {
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
                "X",
                null,
                activityTypeId,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                5,
                new LocationDto("a", null, "b", "c"));

        mockMvc.perform(post("/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_400_when_title_missing() throws Exception {
        String dateStr = LocalDate.now().plusDays(3).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String json = """
                {
                  "description": "d",
                  "activityTypeId": %d,
                  "date": "%s",
                  "startTime": "10:00:00",
                  "endTime": "11:00:00",
                  "capacity": 5,
                  "location": { "street": "s", "postalCode": "p", "city": "c" }
                }
                """.formatted(activityTypeId, dateStr);

        mockMvc.perform(post("/activities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_404_when_activity_type_unknown() throws Exception {
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
                "T",
                null,
                999_999L,
                LocalDate.now().plusDays(2),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                5,
                new LocationDto("a", null, "b", "c"));

        mockMvc.perform(post("/activities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_return_409_when_same_slot_twice() throws Exception {
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
                "A",
                null,
                activityTypeId,
                LocalDate.now().plusDays(20),
                LocalTime.of(14, 0),
                LocalTime.of(16, 0),
                5,
                new LocationDto("a", null, "b", "c"));

        mockMvc.perform(post("/activities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/activities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }
}
