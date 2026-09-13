package com.colife.api.notification.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.colife.api.notification.application.NotificationUseCase;
import com.colife.api.notification.application.dto.NotificationResponseDto;
import com.colife.api.notification.application.dto.UnreadCountResponseDto;
import com.colife.api.shared.openapi.OpenApiConfig;
import com.colife.api.shared.security.JwtPrincipal;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "Notifications in-app de l'utilisateur connecté.")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class NotificationController {

    private final NotificationUseCase notificationUseCase;

    public NotificationController(NotificationUseCase notificationUseCase) {
        this.notificationUseCase = notificationUseCase;
    }

    @GetMapping
    @Operation(
            summary = "Lister mes notifications",
            description = "Retourne toutes les notifications de l'utilisateur connecté, les plus récentes en premier."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste retournée"),
            @ApiResponse(responseCode = "401", description = "Authentification requise")
    })
    public ResponseEntity<List<NotificationResponseDto>> list(@AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(notificationUseCase.listNotifications(principal.userId()));
    }

    @GetMapping("/unread-count")
    @Operation(
            summary = "Compter mes notifications non lues",
            description = "Retourne le nombre de notifications non lues de l'utilisateur connecté."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nombre retourné"),
            @ApiResponse(responseCode = "401", description = "Authentification requise")
    })
    public ResponseEntity<UnreadCountResponseDto> unreadCount(@AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(new UnreadCountResponseDto(notificationUseCase.countUnread(principal.userId())));
    }

    @PutMapping("/{notificationId}/read")
    @Operation(
            summary = "Marquer une notification comme lue",
            description = "Seul le destinataire peut marquer sa notification comme lue."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification mise à jour"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "404", description = "Notification non trouvée")
    })
    public ResponseEntity<NotificationResponseDto> markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(notificationUseCase.markAsRead(notificationId, principal.userId()));
    }

    @PutMapping("/read-all")
    @Operation(
            summary = "Marquer toutes mes notifications comme lues",
            description = "Marque toutes les notifications non lues de l'utilisateur connecté comme lues."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Notifications mises à jour"),
            @ApiResponse(responseCode = "401", description = "Authentification requise")
    })
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal JwtPrincipal principal) {
        notificationUseCase.markAllAsRead(principal.userId());
        return ResponseEntity.noContent().build();
    }
}
