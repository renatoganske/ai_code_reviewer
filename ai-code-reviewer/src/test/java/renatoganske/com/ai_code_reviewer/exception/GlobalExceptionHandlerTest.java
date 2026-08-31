package renatoganske.com.ai_code_reviewer.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void shouldReturnNotFoundWhenEntityDoesNotExist() {
        // Arrange
        EntityNotFoundException exception = new EntityNotFoundException("Pull request not found");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/pull-requests/999");

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleEntityNotFound(exception, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().message()).isEqualTo("Pull request not found");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/pull-requests/999");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void shouldReturnInternalServerErrorForUnexpectedException() {
        // Arrange
        RuntimeException exception = new RuntimeException("Database connection failed");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/pull-requests");

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpectedException(exception, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");

        assertThat(response.getBody().message()).doesNotContain("Database connection failed");
    }

    @Test
    void shouldReturnBadRequestWhenBusinessRuleIsInvalid() {
        // Arrange
        String message = "Pull request cannot be reviewed";

        InvalidBusinessRuleException exception = new InvalidBusinessRuleException(message);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/pull-requests/999/review");

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleInvalidBusinessRule(exception, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();

        ApiErrorResponse body = response.getBody();

        assertThat(body.status()).isEqualTo(400);
        assertThat(body.error()).isEqualTo("Bad Request");
        assertThat(body.message()).isEqualTo(message);
        assertThat(body.path()).isEqualTo("/api/v1/pull-requests/999/review");
        assertThat(body.timestamp()).isNotNull();
        assertThat(body.fields()).isNull();
    }

    @Test
    void shouldReturnBadRequestWhenRequestValidationFails() {
        // Arrange
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);

        BindingResult bindingResult = mock(BindingResult.class);

        FieldError repositoryError = new FieldError("createPullRequestRequest", "repository", "must not be blank");
        FieldError pullRequestNumberError = new FieldError("createPullRequestRequest", "pullRequestNumber", "must be greater than 0");
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(repositoryError, pullRequestNumberError));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/pull-requests");

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleValidation(exception, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
        assertThat(response.getBody().message()).isEqualTo("Validation failed");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/pull-requests");
        assertThat(response.getBody().timestamp()).isNotNull();

        Map<String, String> fields = response.getBody().fields();

        assertThat(fields).isNotNull()
                .containsEntry("repository", "must not be blank")
                .containsEntry("pullRequestNumber", "must be greater than 0");
    }
}