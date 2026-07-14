package com.colife.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.colife.api.carpool.application.CarpoolUseCase;
import com.colife.api.carpool.application.dto.ActivityCarpoolsResponseDto;
import com.colife.api.carpool.application.dto.CarpoolDetailDto;
import com.colife.api.carpool.application.dto.CarpoolRequestDto;
import com.colife.api.carpool.domain.CarpoolStatus;
import com.colife.api.carpool.presentation.CarpoolController;
import com.colife.api.shared.security.JwtPrincipal;
import com.colife.api.user.domain.Role;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarpoolControllerTest {

    private static final UUID DRIVER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PASSENGER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private CarpoolUseCase carpoolUseCase;

    @InjectMocks
    private CarpoolController controller;

    private JwtPrincipal driverPrincipal() {
        return new JwtPrincipal(DRIVER_ID, "driver@test.com", Role.COLLABORATOR);
    }

    private JwtPrincipal passengerPrincipal() {
        return new JwtPrincipal(PASSENGER_ID, "passenger@test.com", Role.COLLABORATOR);
    }

    private CarpoolDetailDto sampleDetail() {
        return new CarpoolDetailDto(
                1L, 10L, DRIVER_ID, "Jean Dupont",
                LocalTime.of(9, 0), 3, 1, 2,
                CarpoolStatus.ACTIVE, List.of()
        );
    }

    @Test
    void list_should_return_200_with_carpools() {
        ActivityCarpoolsResponseDto dto = new ActivityCarpoolsResponseDto(
                List.of(sampleDetail()), "NONE", null
        );
        when(carpoolUseCase.listCarpools(10L, DRIVER_ID)).thenReturn(dto);

        ResponseEntity<ActivityCarpoolsResponseDto> response = controller.list(10L, driverPrincipal());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().carpools()).hasSize(1);
    }

    @Test
    void create_should_return_201_with_carpool_detail() {
        CarpoolRequestDto requestDto = new CarpoolRequestDto(LocalTime.of(9, 0), 3);
        when(carpoolUseCase.createCarpool(10L, DRIVER_ID, requestDto)).thenReturn(sampleDetail());

        ResponseEntity<CarpoolDetailDto> response = controller.create(10L, driverPrincipal(), requestDto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().driverId()).isEqualTo(DRIVER_ID);
        verify(carpoolUseCase).createCarpool(10L, DRIVER_ID, requestDto);
    }

    @Test
    void update_should_return_200_with_updated_detail() {
        CarpoolRequestDto requestDto = new CarpoolRequestDto(LocalTime.of(10, 0), 4);
        when(carpoolUseCase.updateCarpoolByDriver(10L, 1L, DRIVER_ID, requestDto)).thenReturn(sampleDetail());

        ResponseEntity<CarpoolDetailDto> response = controller.update(10L, 1L, driverPrincipal(), requestDto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        verify(carpoolUseCase).updateCarpoolByDriver(10L, 1L, DRIVER_ID, requestDto);
    }

    @Test
    void cancel_should_return_204_no_content() {
        doNothing().when(carpoolUseCase).cancelCarpoolByDriver(10L, 1L, DRIVER_ID);

        ResponseEntity<Void> response = controller.cancel(10L, 1L, driverPrincipal());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(carpoolUseCase).cancelCarpoolByDriver(10L, 1L, DRIVER_ID);
    }

    @Test
    void join_should_return_201_with_carpool_detail() {
        when(carpoolUseCase.joinCarpool(10L, 1L, PASSENGER_ID)).thenReturn(sampleDetail());

        ResponseEntity<CarpoolDetailDto> response = controller.join(10L, 1L, passengerPrincipal());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        verify(carpoolUseCase).joinCarpool(10L, 1L, PASSENGER_ID);
    }

    @Test
    void leave_should_return_204_no_content() {
        doNothing().when(carpoolUseCase).leaveCarpool(10L, 1L, PASSENGER_ID);

        ResponseEntity<Void> response = controller.leave(10L, 1L, passengerPrincipal());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(carpoolUseCase).leaveCarpool(10L, 1L, PASSENGER_ID);
    }
}
