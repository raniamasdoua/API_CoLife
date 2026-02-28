package com.example.api;

import com.example.api.activity.domain.ActivityRepositoryPort;
import com.example.api.subscription.application.SubscriptionUseCase;
import com.example.api.subscription.domain.SubscriptionRepositoryPort;
import com.example.api.user.domain.UserRepositoryPort;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubscriptionUseCaseTest {
    @Mock
    SubscriptionRepositoryPort subscriptionRepo;
    @Mock
    ActivityRepositoryPort activityRepo;
    @Mock
    UserRepositoryPort userRepo;

    @InjectMocks
    SubscriptionUseCase useCase;
}
