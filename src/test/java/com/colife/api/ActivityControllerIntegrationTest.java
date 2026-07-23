package com.colife.api;

import com.colife.api.activity.application.dto.CreateActivityRequestDto;
import com.colife.api.activity.application.dto.LocationDto;
import com.colife.api.activity.domain.LocationType;
import com.colife.api.activity.infrastructure.ActivityEntity;
import com.colife.api.activity.infrastructure.ActivityJpaRepository;
import com.colife.api.activity.infrastructure.LocationEmbeddable;
import com.colife.api.activityType.infrastructure.ActivityTypeEntity;
import com.colife.api.activityType.infrastructure.ActivityTypeJpaRepository;
import com.colife.api.shared.security.JwtPrincipal;
import com.colife.api.subscription.infrastructure.SubscriptionEntity;
import com.colife.api.subscription.infrastructure.SubscriptionJpaRepository;
import com.colife.api.user.domain.Role;
import com.colife.api.user.infrastructure.UserEntity;
import com.colife.api.user.infrastructure.UserJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@Transactional
class ActivityControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private ActivityTypeJpaRepository activityTypeJpaRepository;

    @Autowired
    private ActivityJpaRepository activityJpaRepository;

    @Autowired
    private SubscriptionJpaRepository subscriptionJpaRepository;

    private Long activityTypeId;
    private UUID userId;
    private String userEmail;
    private Long activityId;

    /** Authentification de test : injecte directement un JwtPrincipal (comme le ferait le converter Keycloak). */
    private static RequestPostProcessor authFor(UUID userId, String email, Role role) {
        JwtPrincipal principal = new JwtPrincipal(userId, email, role);
        return authentication(new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }

    private UserEntity createUser(String firstName, String lastName, String email, Role role) {
        UserEntity user = new UserEntity(UUID.randomUUID(), firstName, lastName, email, role);
        return userJpaRepository.save(user);
    }

    @BeforeEach
    void setUp() {
        UserEntity user = createUser("Int", "Test", "activity-it@entreprise.com", Role.COLLABORATOR);
        userId = user.getId();
        userEmail = user.getEmail();

        ActivityTypeEntity type = new ActivityTypeEntity();
        type.setName("Sport");
        type = activityTypeJpaRepository.save(type);
        activityTypeId = type.getId();

        // Activité de base disponible pour les tests update
        LocationEmbeddable loc = new LocationEmbeddable();
        loc.setStreet("1 rue Test");
        loc.setPostalCode("75001");
        loc.setCity("Paris");

        ActivityEntity activity = new ActivityEntity();
        activity.setTitle("Activité de base");
        activity.setCapacity(10);
        activity.setLocation(loc);
        activity.setOrganizer(userJpaRepository.getReferenceById(userId));
        activity.setType(activityTypeJpaRepository.getReferenceById(activityTypeId));
        activity.setDate(LocalDate.now().plusDays(10));
        activity.setStartTime(LocalTime.of(10, 0));
        activity.setEndTime(LocalTime.of(12, 0));
        activity.setDeleted(false);
        activity = activityJpaRepository.save(activity);
        activityId = activity.getId();

        // Enregistrer l'organisateur comme participant
        SubscriptionEntity sub = new SubscriptionEntity();
        sub.setActivityId(activityId);
        sub.setUserId(userId);
        sub.setSubscribedAt(LocalDateTime.now());
        subscriptionJpaRepository.save(sub);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Tests : POST /activities (existants conservés)
    // ═══════════════════════════════════════════════════════════════════════

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
                new LocationDto(null, "Stade municipal", null, "44000", "Nantes"),
                LocationType.OFF_SITE,
                null);

        mockMvc.perform(post("/activities")
                        .with(authFor(userId, userEmail, Role.COLLABORATOR))
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
                "X", null, activityTypeId,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0), LocalTime.of(11, 0), 5,
                new LocationDto(null, "a", null, "b", "c"),
                LocationType.OFF_SITE,
                null);

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
                        .with(authFor(userId, userEmail, Role.COLLABORATOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_404_when_activity_type_unknown() throws Exception {
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
                "T", null, 999_999L,
                LocalDate.now().plusDays(2),
                LocalTime.of(10, 0), LocalTime.of(11, 0), 5,
                new LocationDto(null, "a", null, "b", "c"),
                LocationType.OFF_SITE,
                null);

        mockMvc.perform(post("/activities")
                        .with(authFor(userId, userEmail, Role.COLLABORATOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_return_409_when_same_slot_twice() throws Exception {
        CreateActivityRequestDto dto = new CreateActivityRequestDto(
                "A", null, activityTypeId,
                LocalDate.now().plusDays(20),
                LocalTime.of(14, 0), LocalTime.of(16, 0), 5,
                new LocationDto(null, "a", null, "b", "c"),
                LocationType.OFF_SITE,
                null);

        mockMvc.perform(post("/activities")
                        .with(authFor(userId, userEmail, Role.COLLABORATOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/activities")
                        .with(authFor(userId, userEmail, Role.COLLABORATOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Tests : PUT /activities/{activityId}
    // ═══════════════════════════════════════════════════════════════════════

    /** Corps de mise à jour valide par défaut. */
    private String validUpdateJson() {
        return validUpdateJson(activityTypeId, LocalDate.now().plusDays(15));
    }

    private String validUpdateJson(Long typeId, LocalDate date) {
        return """
                {
                  "title": "Titre modifié",
                  "description": "Description modifiée",
                  "activityTypeId": %d,
                  "date": "%s",
                  "startTime": "14:00:00",
                  "endTime": "16:00:00",
                  "capacity": 10,
                  "location": { "street": "2 rue Modifiée", "postalCode": "75002", "city": "Paris" }
                }
                """.formatted(typeId, date.format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    @Test
    void should_return_200_when_organizer_updates_own_activity() throws Exception {
        mockMvc.perform(put("/activities/" + activityId)
                        .with(authFor(userId, userEmail, Role.COLLABORATOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activityId))
                .andExpect(jsonPath("$.title").value("Titre modifié"))
                .andExpect(jsonPath("$.capacity").value(10));
    }

    @Test
    void should_return_401_when_not_authenticated_for_update() throws Exception {
        mockMvc.perform(put("/activities/" + activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_400_when_title_is_blank_in_update() throws Exception {
        String dateStr = LocalDate.now().plusDays(15).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String json = """
                {
                  "title": "",
                  "activityTypeId": %d,
                  "date": "%s",
                  "startTime": "14:00:00",
                  "endTime": "16:00:00",
                  "capacity": 10,
                  "location": { "street": "r", "postalCode": "p", "city": "c" }
                }
                """.formatted(activityTypeId, dateStr);

        mockMvc.perform(put("/activities/" + activityId)
                        .with(authFor(userId, userEmail, Role.COLLABORATOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_end_time_is_before_start_time() throws Exception {
        String dateStr = LocalDate.now().plusDays(15).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String json = """
                {
                  "title": "Titre",
                  "activityTypeId": %d,
                  "date": "%s",
                  "startTime": "16:00:00",
                  "endTime": "14:00:00",
                  "capacity": 10,
                  "location": { "street": "r", "postalCode": "p", "city": "c" }
                }
                """.formatted(activityTypeId, dateStr);

        mockMvc.perform(put("/activities/" + activityId)
                        .with(authFor(userId, userEmail, Role.COLLABORATOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_403_when_caller_is_not_the_organizer() throws Exception {
        UserEntity otherUser = createUser("Autre", "User", "other@entreprise.com", Role.COLLABORATOR);

        mockMvc.perform(put("/activities/" + activityId)
                        .with(authFor(otherUser.getId(), otherUser.getEmail(), Role.COLLABORATOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void should_return_200_when_admin_updates_another_users_activity() throws Exception {
        UserEntity admin = createUser("Admin", "User", "admin@entreprise.com", Role.ADMIN);

        mockMvc.perform(put("/activities/" + activityId)
                        .with(authFor(admin.getId(), admin.getEmail(), Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activityId));
    }

    @Test
    void should_return_404_when_activity_does_not_exist() throws Exception {
        mockMvc.perform(put("/activities/999999")
                        .with(authFor(userId, userEmail, Role.COLLABORATOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateJson()))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_return_404_when_activity_type_does_not_exist_for_update() throws Exception {
        mockMvc.perform(put("/activities/" + activityId)
                        .with(authFor(userId, userEmail, Role.COLLABORATOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateJson(999_999L, LocalDate.now().plusDays(15))))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_return_400_when_activity_is_already_past() throws Exception {
        // Créer une activité passée directement via JPA (bypass création)
        LocationEmbeddable loc = new LocationEmbeddable();
        loc.setStreet("1 rue Passée");
        loc.setPostalCode("75001");
        loc.setCity("Paris");

        ActivityEntity pastActivity = new ActivityEntity();
        pastActivity.setTitle("Activité passée");
        pastActivity.setCapacity(10);
        pastActivity.setLocation(loc);
        pastActivity.setOrganizer(userJpaRepository.getReferenceById(userId));
        pastActivity.setType(activityTypeJpaRepository.getReferenceById(activityTypeId));
        pastActivity.setDate(LocalDate.now().minusDays(5));
        pastActivity.setStartTime(LocalTime.of(10, 0));
        pastActivity.setEndTime(LocalTime.of(12, 0));
        pastActivity.setDeleted(false);
        pastActivity = activityJpaRepository.save(pastActivity);

        mockMvc.perform(put("/activities/" + pastActivity.getId())
                        .with(authFor(userId, userEmail, Role.COLLABORATOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("passée")));
    }

    @Test
    void should_return_400_when_capacity_is_below_participant_count() throws Exception {
        // Ajouter un 2e participant → participantCount = 2
        SubscriptionEntity extraSub = new SubscriptionEntity();
        extraSub.setActivityId(activityId);
        extraSub.setUserId(UUID.randomUUID());
        extraSub.setSubscribedAt(LocalDateTime.now());
        subscriptionJpaRepository.save(extraSub);

        String dateStr = LocalDate.now().plusDays(15).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String json = """
                {
                  "title": "Titre",
                  "activityTypeId": %d,
                  "date": "%s",
                  "startTime": "14:00:00",
                  "endTime": "16:00:00",
                  "capacity": 1,
                  "location": { "street": "r", "postalCode": "p", "city": "c" }
                }
                """.formatted(activityTypeId, dateStr);

        mockMvc.perform(put("/activities/" + activityId)
                        .with(authFor(userId, userEmail, Role.COLLABORATOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("participants")));
    }

    @Test
    void should_return_409_when_new_slot_overlaps_with_organizers_other_activity() throws Exception {
        // setUp : activité à D+10, 10:00-12:00
        // Créer activité B à D+10, 14:00-16:00
        CreateActivityRequestDto dtoB = new CreateActivityRequestDto(
                "Activité B", null, activityTypeId,
                LocalDate.now().plusDays(10),
                LocalTime.of(14, 0), LocalTime.of(16, 0), 5,
                new LocationDto(null, "2 rue B", null, "75002", "Paris"),
                LocationType.OFF_SITE,
                null);

        String createResponse = mockMvc.perform(post("/activities")
                        .with(authFor(userId, userEmail, Role.COLLABORATOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoB)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long activityBId = objectMapper.readTree(createResponse).get("id").asLong();

        // Essayer de déplacer B à D+10, 11:00-13:00 → chevauche l'activité setUp (10:00-12:00)
        String conflictJson = """
                {
                  "title": "Activité B modifiée",
                  "activityTypeId": %d,
                  "date": "%s",
                  "startTime": "11:00:00",
                  "endTime": "13:00:00",
                  "capacity": 5,
                  "location": { "street": "r", "postalCode": "p", "city": "c" }
                }
                """.formatted(activityTypeId, LocalDate.now().plusDays(10).format(DateTimeFormatter.ISO_LOCAL_DATE));

        mockMvc.perform(put("/activities/" + activityBId)
                        .with(authFor(userId, userEmail, Role.COLLABORATOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(conflictJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("organisateur")));
    }

    @Test
    void should_return_409_when_new_slot_conflicts_with_a_subscribed_participants_schedule() throws Exception {
        // Créer un 2e utilisateur (participant)
        UserEntity participant = createUser("Part", "Icipant", "participant@entreprise.com", Role.COLLABORATOR);

        // Créer une activité séparée à D+15, 14:00-16:00 organisée par une tierce personne
        UserEntity organizer2 = createUser("Other", "Org", "org2@entreprise.com", Role.COLLABORATOR);

        LocationEmbeddable loc = new LocationEmbeddable();
        loc.setStreet("3 rue C");
        loc.setPostalCode("75003");
        loc.setCity("Paris");

        ActivityEntity otherActivity = new ActivityEntity();
        otherActivity.setTitle("Autre activité");
        otherActivity.setCapacity(10);
        otherActivity.setLocation(loc);
        otherActivity.setOrganizer(userJpaRepository.getReferenceById(organizer2.getId()));
        otherActivity.setType(activityTypeJpaRepository.getReferenceById(activityTypeId));
        otherActivity.setDate(LocalDate.now().plusDays(15));
        otherActivity.setStartTime(LocalTime.of(14, 0));
        otherActivity.setEndTime(LocalTime.of(16, 0));
        otherActivity.setDeleted(false);
        otherActivity = activityJpaRepository.save(otherActivity);

        // Inscrire le participant à cette autre activité
        SubscriptionEntity participantSub = new SubscriptionEntity();
        participantSub.setActivityId(otherActivity.getId());
        participantSub.setUserId(participant.getId());
        participantSub.setSubscribedAt(LocalDateTime.now());
        subscriptionJpaRepository.save(participantSub);

        // Inscrire le participant à l'activité de base (setUp)
        SubscriptionEntity mainSub = new SubscriptionEntity();
        mainSub.setActivityId(activityId);
        mainSub.setUserId(participant.getId());
        mainSub.setSubscribedAt(LocalDateTime.now());
        subscriptionJpaRepository.save(mainSub);

        // Essayer de déplacer l'activité de base (D+10) vers D+15, 15:00-17:00 → chevauchement pour le participant
        String conflictJson = """
                {
                  "title": "Activité déplacée",
                  "activityTypeId": %d,
                  "date": "%s",
                  "startTime": "15:00:00",
                  "endTime": "17:00:00",
                  "capacity": 10,
                  "location": { "street": "r", "postalCode": "p", "city": "c" }
                }
                """.formatted(activityTypeId, LocalDate.now().plusDays(15).format(DateTimeFormatter.ISO_LOCAL_DATE));

        mockMvc.perform(put("/activities/" + activityId)
                        .with(authFor(userId, userEmail, Role.COLLABORATOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(conflictJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("participants")));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Tests : DELETE /activities/{activityId}
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    void should_return_204_when_organizer_deletes_own_activity() throws Exception {
        mockMvc.perform(delete("/activities/" + activityId)
                        .with(authFor(userId, userEmail, Role.COLLABORATOR)))
                .andExpect(status().isNoContent());

        assertThat(activityJpaRepository.findById(activityId)).isPresent();
        assertThat(activityJpaRepository.findById(activityId).orElseThrow().isDeleted()).isTrue();
        assertThat(subscriptionJpaRepository.countActiveByActivityId(activityId)).isZero();
    }

    @Test
    void should_return_401_when_not_authenticated_for_delete() throws Exception {
        mockMvc.perform(delete("/activities/" + activityId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_403_when_caller_is_not_organizer_for_delete() throws Exception {
        UserEntity other = createUser("O", "User", "other-del@entreprise.com", Role.COLLABORATOR);

        mockMvc.perform(delete("/activities/" + activityId)
                        .with(authFor(other.getId(), other.getEmail(), Role.COLLABORATOR)))
                .andExpect(status().isForbidden());

        assertThat(activityJpaRepository.findById(activityId).orElseThrow().isDeleted()).isFalse();
    }

    @Test
    void should_return_204_when_admin_deletes_activity() throws Exception {
        UserEntity admin = createUser("Ad", "Min", "admin-del@entreprise.com", Role.ADMIN);

        mockMvc.perform(delete("/activities/" + activityId)
                        .with(authFor(admin.getId(), admin.getEmail(), Role.ADMIN)))
                .andExpect(status().isNoContent());

        assertThat(activityJpaRepository.findById(activityId).orElseThrow().isDeleted()).isTrue();
    }

    @Test
    void should_return_404_when_activity_not_found_for_delete() throws Exception {
        mockMvc.perform(delete("/activities/999999")
                        .with(authFor(userId, userEmail, Role.COLLABORATOR)))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_return_400_when_deleting_past_activity() throws Exception {
        LocationEmbeddable loc = new LocationEmbeddable();
        loc.setStreet("1 rue Passée");
        loc.setPostalCode("75001");
        loc.setCity("Paris");

        ActivityEntity past = new ActivityEntity();
        past.setTitle("Passée");
        past.setCapacity(5);
        past.setLocation(loc);
        past.setOrganizer(userJpaRepository.getReferenceById(userId));
        past.setType(activityTypeJpaRepository.getReferenceById(activityTypeId));
        past.setDate(LocalDate.now().minusDays(3));
        past.setStartTime(LocalTime.of(10, 0));
        past.setEndTime(LocalTime.of(12, 0));
        past.setDeleted(false);
        past = activityJpaRepository.save(past);

        mockMvc.perform(delete("/activities/" + past.getId())
                        .with(authFor(userId, userEmail, Role.COLLABORATOR)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("passée")));

        assertThat(activityJpaRepository.findById(past.getId()).orElseThrow().isDeleted()).isFalse();
    }
}
