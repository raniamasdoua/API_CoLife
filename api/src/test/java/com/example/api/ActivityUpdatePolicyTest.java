package com.example.api;

import com.example.api.activity.domain.Activity;
import com.example.api.activity.domain.ActivityUpdatePolicy;
import com.example.api.activity.domain.Location;
import com.example.api.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActivityUpdatePolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 4, 5);
    private static final LocalTime NOW = LocalTime.of(14, 0);

    private Activity buildActivity(LocalDate date, LocalTime start, LocalTime end) {
        return Activity.builder()
                .id(1L).title("Test").description(null).capacity(10)
                .location(Location.builder().street("r").postalCode("p").city("c").build())
                .typeId(1L).organizerId(1L)
                .date(date).startTime(start).endTime(end)
                .deleted(false).build();
    }

    // ─── validateActivityIsModifiable ───────────────────────────────────────

    @Test
    void should_throw_when_activity_date_is_in_the_past() {
        Activity activity = buildActivity(TODAY.minusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0));

        assertThatThrownBy(() -> ActivityUpdatePolicy.validateActivityIsModifiable(activity, TODAY, NOW))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà passée");
    }

    @Test
    void should_throw_when_activity_is_today_and_already_started() {
        Activity activity = buildActivity(TODAY, LocalTime.of(13, 0), LocalTime.of(15, 0));

        assertThatThrownBy(() -> ActivityUpdatePolicy.validateActivityIsModifiable(activity, TODAY, NOW))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("en cours");
    }

    @Test
    void should_throw_when_activity_starts_exactly_now() {
        Activity activity = buildActivity(TODAY, NOW, NOW.plusHours(1));

        assertThatThrownBy(() -> ActivityUpdatePolicy.validateActivityIsModifiable(activity, TODAY, NOW))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("en cours");
    }

    @Test
    void should_not_throw_when_activity_is_in_the_future() {
        Activity activity = buildActivity(TODAY.plusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0));

        assertThatCode(() -> ActivityUpdatePolicy.validateActivityIsModifiable(activity, TODAY, NOW))
                .doesNotThrowAnyException();
    }

    @Test
    void should_not_throw_when_activity_is_today_but_not_yet_started() {
        Activity activity = buildActivity(TODAY, NOW.plusMinutes(30), NOW.plusHours(2));

        assertThatCode(() -> ActivityUpdatePolicy.validateActivityIsModifiable(activity, TODAY, NOW))
                .doesNotThrowAnyException();
    }

    // ─── validateNewSlot ────────────────────────────────────────────────────

    @Test
    void should_throw_when_new_date_is_in_the_past() {
        assertThatThrownBy(() -> ActivityUpdatePolicy.validateNewSlot(
                TODAY.minusDays(1), TODAY, NOW, LocalTime.of(10, 0), LocalTime.of(12, 0), 10, 3))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("passé");
    }

    @Test
    void should_throw_when_new_start_time_is_before_now_on_today() {
        assertThatThrownBy(() -> ActivityUpdatePolicy.validateNewSlot(
                TODAY, TODAY, NOW, NOW.minusHours(1), NOW.plusHours(1), 10, 3))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("passé");
    }

    @Test
    void should_throw_when_end_time_is_equal_to_start_time() {
        assertThatThrownBy(() -> ActivityUpdatePolicy.validateNewSlot(
                TODAY.plusDays(1), TODAY, NOW, LocalTime.of(12, 0), LocalTime.of(12, 0), 10, 3))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("heure de fin");
    }

    @Test
    void should_throw_when_end_time_is_before_start_time() {
        assertThatThrownBy(() -> ActivityUpdatePolicy.validateNewSlot(
                TODAY.plusDays(1), TODAY, NOW, LocalTime.of(14, 0), LocalTime.of(12, 0), 10, 3))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("heure de fin");
    }

    @Test
    void should_throw_when_capacity_is_less_than_participant_count() {
        assertThatThrownBy(() -> ActivityUpdatePolicy.validateNewSlot(
                TODAY.plusDays(1), TODAY, NOW, LocalTime.of(10, 0), LocalTime.of(12, 0), 2, 5))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("participants actuels");
    }

    @Test
    void should_not_throw_when_capacity_equals_participant_count() {
        assertThatCode(() -> ActivityUpdatePolicy.validateNewSlot(
                TODAY.plusDays(1), TODAY, NOW, LocalTime.of(10, 0), LocalTime.of(12, 0), 5, 5))
                .doesNotThrowAnyException();
    }

    @Test
    void should_not_throw_when_all_fields_are_valid() {
        assertThatCode(() -> ActivityUpdatePolicy.validateNewSlot(
                TODAY.plusDays(1), TODAY, NOW, LocalTime.of(10, 0), LocalTime.of(12, 0), 10, 3))
                .doesNotThrowAnyException();
    }
}
