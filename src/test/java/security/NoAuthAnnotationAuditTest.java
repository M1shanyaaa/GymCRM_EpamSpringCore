package security;

import com.epam.gym.controller.AuthController;
import com.epam.gym.controller.TraineeController;
import com.epam.gym.controller.TrainerController;
import com.epam.gym.controller.TrainingController;
import com.epam.gym.dto.request.*;
import com.epam.gym.model.TrainingTypeName;
import com.epam.gym.security.NoAuth;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against regressions where a public endpoint silently loses its
 * {@link NoAuth} marker (blocking legitimate calls), or a secured endpoint
 * silently gains it (bypassing authentication).
 */
class NoAuthAnnotationAuditTest {

    @Test
    void publicEndpoints_mustBeAnnotatedNoAuth() throws NoSuchMethodException {
        assertNoAuth(AuthController.class.getMethod("login", LoginRequest.class));
        assertNoAuth(AuthController.class.getMethod("changePassword", String.class, ChangePasswordRequest.class));
        assertNoAuth(TraineeController.class.getMethod("register", TraineeRegistrationRequest.class));
        assertNoAuth(TrainerController.class.getMethod("register", TrainerRegistrationRequest.class));
        assertNoAuth(TrainingController.class.getMethod("addTraining", AddTrainingRequest.class));
        assertNoAuth(TrainingController.class.getMethod("getTrainingTypes"));
    }

    @Test
    void securedEndpoints_mustNotBeAnnotatedNoAuth() throws NoSuchMethodException {
        // TraineeController
        assertRequiresAuth(TraineeController.class.getMethod("getProfile", String.class));
        assertRequiresAuth(TraineeController.class.getMethod("update", String.class, UpdateTraineeRequest.class));
        assertRequiresAuth(TraineeController.class.getMethod("delete", String.class));
        assertRequiresAuth(TraineeController.class.getMethod("setActive", String.class, ActivateRequest.class));
        assertRequiresAuth(TraineeController.class.getMethod("updateTrainers", String.class, UpdateTraineeTrainersRequest.class));
        assertRequiresAuth(TraineeController.class.getMethod("getTrainings",
                String.class, LocalDate.class, LocalDate.class, String.class, TrainingTypeName.class));

        // TrainerController
        assertRequiresAuth(TrainerController.class.getMethod("getProfile", String.class));
        assertRequiresAuth(TrainerController.class.getMethod("update", String.class, UpdateTrainerRequest.class));
        assertRequiresAuth(TrainerController.class.getMethod("setActive", String.class, ActivateRequest.class));
        assertRequiresAuth(TrainerController.class.getMethod("getUnassigned", String.class));
        assertRequiresAuth(TrainerController.class.getMethod("getTrainings",
                String.class, LocalDate.class, LocalDate.class, String.class));
    }

    private void assertNoAuth(Method method) {
        assertThat(method.isAnnotationPresent(NoAuth.class))
                .as("%s must be @NoAuth", method)
                .isTrue();
    }

    private void assertRequiresAuth(Method method) {
        assertThat(method.isAnnotationPresent(NoAuth.class))
                .as("%s must require authentication (no @NoAuth)", method)
                .isFalse();
    }
}