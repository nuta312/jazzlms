package kz.jazz.lms.user.grpc;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import kz.jazz.lms.grpc.user.*;
import kz.jazz.lms.user.dto.UserDto;
import kz.jazz.lms.user.service.NotFoundException;
import kz.jazz.lms.user.service.UserService;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

/**
 * Реализация gRPC-сервиса из user.proto.
 * Слушает отдельный порт (9091). Вызывается course-service'ом при записи на курс.
 */
@GrpcService
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserService users;

    public UserGrpcService(UserService users) { this.users = users; }

    @Override
    public void getUser(GetUserRequest request, StreamObserver<UserResponse> responseObserver) {
        try {
            UserDto dto = users.findById(UUID.fromString(request.getUserId()));
            responseObserver.onNext(toProto(dto));
            responseObserver.onCompleted();
        } catch (NotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Bad UUID").asRuntimeException());
        }
    }

    @Override
    public void userExists(GetUserRequest request, StreamObserver<UserExistsResponse> responseObserver) {
        UserExistsResponse.Builder resp = UserExistsResponse.newBuilder().setExists(false).setActive(false);
        try {
            UserDto dto = users.findById(UUID.fromString(request.getUserId()));
            resp.setExists(true).setActive(dto.active());
        } catch (NotFoundException | IllegalArgumentException ignored) {
            // exists=false
        }
        responseObserver.onNext(resp.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getUsers(GetUsersRequest request, StreamObserver<UsersResponse> responseObserver) {
        UsersResponse.Builder resp = UsersResponse.newBuilder();
        for (String id : request.getUserIdsList()) {
            try {
                resp.addUsers(toProto(users.findById(UUID.fromString(id))));
            } catch (NotFoundException | IllegalArgumentException ignored) {
                // пропускаем несуществующих
            }
        }
        responseObserver.onNext(resp.build());
        responseObserver.onCompleted();
    }

    private static UserResponse toProto(UserDto u) {
        return UserResponse.newBuilder()
                .setId(u.id().toString())
                .setFirstName(u.firstName())
                .setLastName(u.lastName())
                .setEmail(u.email())
                .setUsername(u.username())
                .setUserType(u.userType().name())
                .setActive(u.active())
                .build();
    }
}
