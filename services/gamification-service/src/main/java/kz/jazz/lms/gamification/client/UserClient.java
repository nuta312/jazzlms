package kz.jazz.lms.gamification.client;

import io.grpc.StatusRuntimeException;
import kz.jazz.lms.grpc.user.GetUsersRequest;
import kz.jazz.lms.grpc.user.UserResponse;
import kz.jazz.lms.grpc.user.UserServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Имена для лидерборда. В Redis лежат только userId, а ученику нельзя читать /api/users,
 * поэтому имена подтягивает сам сервис — одним batch-вызовом gRPC.
 */
@Component
public class UserClient {
    private static final Logger log = LoggerFactory.getLogger(UserClient.class);

    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub stub;

    public Map<String, UserResponse> getUsers(Collection<String> ids) {
        if (ids.isEmpty()) return Map.of();
        try {
            return stub.getUsers(GetUsersRequest.newBuilder().addAllUserIds(ids).build())
                    .getUsersList().stream().collect(Collectors.toMap(UserResponse::getId, Function.identity()));
        } catch (StatusRuntimeException e) {
            log.warn("user-service gRPC call failed: {}", e.getStatus());
            return Map.of();
        }
    }
}
