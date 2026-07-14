package com.colife.api;

import com.colife.api.activity.domain.ActivityCreationPolicy;
import com.colife.api.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActivityCreationPolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, Month.MARCH, 22);
    private static final LocalTime NOW = LocalTime.of(13, 0);

    @Test
    void valid_when_all_conditions_met() {
        assertThatCode(() -> ActivityCreationPolicy.validate(
                TODAY.plusDays(1), TODAY, NOW, 10, LocalTime.of(9, 0), LocalTime.of(11, 0)))
                .doesNotThrowAnyException();
    }

    @Test
    void throws_when_date_is_in_the_past() {
        LocalDate past = TODAY.minusDays(1);
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(11, 0);
        assertThatThrownBy(() -> ActivityCreationPolicy.validate(past, TODAY, NOW, 10, start, end))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void throws_when_same_day_and_start_time_already_passed() {
        LocalTime start = LocalTime.of(12, 0);
        LocalTime end = LocalTime.of(14, 0);
        assertThatThrownBy(() -> ActivityCreationPolicy.validate(TODAY, TODAY, NOW, 10, start, end))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void valid_when_same_day_and_start_time_is_after_now() {
        assertThatCode(() -> ActivityCreationPolicy.validate(
                TODAY, TODAY, NOW, 10, LocalTime.of(14, 0), LocalTime.of(16, 0)))
                .doesNotThrowAnyException();
    }

    @Test
    void throws_when_capacity_is_zero() {
        LocalDate future = TODAY.plusDays(1);
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(11, 0);
        assertThatThrownBy(() -> ActivityCreationPolicy.validate(future, TODAY, NOW, 0, start, end))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void throws_when_end_time_is_before_start_time() {
        LocalDate future = TODAY.plusDays(1);
        LocalTime start = LocalTime.of(11, 0);
        LocalTime end = LocalTime.of(9, 0);
        assertThatThrownBy(() -> ActivityCreationPolicy.validate(future, TODAY, NOW, 10, start, end))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void throws_when_end_time_equals_start_time() {
        LocalDate future = TODAY.plusDays(1);
        LocalTime time = LocalTime.of(10, 0);
        assertThatThrownBy(() -> ActivityCreationPolicy.validate(future, TODAY, NOW, 10, time, time))
                .isInstanceOf(BusinessException.class);
    }
}
