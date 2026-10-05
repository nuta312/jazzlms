package kz.jazz.lms.course.client;

import io.grpc.StatusRuntimeException;
import kz.jazz.lms.grpc.user.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Обёртка над gRPC-stub'ом user-service.
 * Синхронный вызов: course-service ждёт ответ, как обычный вызов метода,
 * но под капотом это сетевой запрос по HTTP/2 в другой процесс.
 */
@Component
public class UserClient {
    private static final Logger log = LoggerFactory.getLogger(UserClient.class);

    @GrpcClient("user-service")   // имя из application.yml: grpc.client.user-service.address
    private UserServiceGrpc.UserServiceBlockingStub stub;

    public boolean existsAndActive(UUID userId) {
        try {
            UserExistsResponse r = stub.userExists(GetUserRequest.newBuilder().setUserId(userId.toString()).build());
            return r.getExists() && r.getActive();
        } catch (StatusRuntimeException e) {
            log.warn("user-service gRPC call failed: {}", e.getStatus());
            throw new IllegalStateException("user-service unavailable: " + e.getStatus().getCode());
        }
    }

    public Optional<UserResponse> getUser(UUID userId) {
        try {
            return Optional.of(stub.getUser(GetUserRequest.newBuilder().setUserId(userId.toString()).build()));
        } catch (StatusRuntimeException e) {
            return Optional.empty();
        }
    }

    /** Один batch-запрос вместо N одиночных (решаем проблему N+1 по сети). */
    public Map<String, UserResponse> getUsers(List<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        try {
            GetUsersRequest req = GetUsersRequest.newBuilder()
                    .addAllUserIds(ids.stream().map(UUID::toString).toList()).build();
            return stub.getUsers(req).getUsersList().stream()
                    .collect(Collectors.toMap(UserResponse::getId, Function.identity()));
        } catch (StatusRuntimeException e) {
            log.warn("user-service gRPC batch call failed: {}", e.getStatus());
            return Map.of();
        }
    }
}
