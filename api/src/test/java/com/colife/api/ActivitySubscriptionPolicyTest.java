package com.colife.api;

import com.colife.api.activity.domain.Activity;
import com.colife.api.activity.domain.ActivitySubscriptionPolicy;
import com.colife.api.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActivitySubscriptionPolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, Month.MARCH, 22);
    private static final LocalTime NOW = LocalTime.of(13, 0);

    private static Activity activity(LocalDate date, LocalTime start) {
        return Activity.builder()
                .date(date)
                .startTime(start)
                .build();
    }

    // ─── validateActivityIsOpenForSubscription ────────────────────────────────

    @Test
    void subscription_allowed_for_future_activity() {
        assertThatCode(() -> ActivitySubscriptionPolicy.validateActivityIsOpenForSubscription(
                activity(TODAY.plusDays(3), LocalTime.of(10, 0)), TODAY, NOW))
                .doesNotThrowAnyException();
    }

    @Test
    void subscription_throws_when_activity_date_is_past() {
        Activity pastAct = activity(TODAY.minusDays(1), LocalTime.of(10, 0));
        assertThatThrownBy(() -> ActivitySubscriptionPolicy.validateActivityIsOpenForSubscription(pastAct, TODAY, NOW))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void subscription_allowed_same_day_when_start_is_after_now() {
        assertThatCode(() -> ActivitySubscriptionPolicy.validateActivityIsOpenForSubscription(
                activity(TODAY, LocalTime.of(14, 0)), TODAY, NOW))
                .doesNotThrowAnyException();
    }

    @Test
    void subscription_throws_same_day_when_start_is_before_or_equal_now() {
        Activity startedAct = activity(TODAY, LocalTime.of(12, 0));
        assertThatThrownBy(() -> ActivitySubscriptionPolicy.validateActivityIsOpenForSubscription(startedAct, TODAY, NOW))
                .isInstanceOf(BusinessException.class);
    }

    // ─── validateActivityAllowsUnsubscribe ────────────────────────────────────

    @Test
    void unsubscription_allowed_for_future_activity() {
        assertThatCode(() -> ActivitySubscriptionPolicy.validateActivityAllowsUnsubscribe(
                activity(TODAY.plusDays(3), LocalTime.of(10, 0)), TODAY, NOW))
                .doesNotThrowAnyException();
    }

    @Test
    void unsubscription_throws_when_activity_date_is_past() {
        Activity pastAct = activity(TODAY.minusDays(1), LocalTime.of(10, 0));
        assertThatThrownBy(() -> ActivitySubscriptionPolicy.validateActivityAllowsUnsubscribe(pastAct, TODAY, NOW))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void unsubscription_allowed_same_day_when_start_is_after_now() {
        assertThatCode(() -> ActivitySubscriptionPolicy.validateActivityAllowsUnsubscribe(
                activity(TODAY, LocalTime.of(14, 0)), TODAY, NOW))
                .doesNotThrowAnyException();
    }

    @Test
    void unsubscription_throws_same_day_when_start_has_passed() {
        Activity startedAct = activity(TODAY, LocalTime.of(12, 0));
        assertThatThrownBy(() -> ActivitySubscriptionPolicy.validateActivityAllowsUnsubscribe(startedAct, TODAY, NOW))
                .isInstanceOf(BusinessException.class);
    }
}
