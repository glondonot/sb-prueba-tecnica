package com.segurosbolivar.polizas.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Traduce excepciones a respuestas RFC 9457 (Problem Details) con mensajes de negocio claros.
 * <ul>
 *   <li>404: recurso inexistente</li>
 *   <li>409: la operación no aplica al estado actual (o conflicto de concurrencia)</li>
 *   <li>422: la solicitud viola una regla de negocio</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ProblemDetail manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage());
    }

    @ExceptionHandler(EstadoInvalidoException.class)
    ProblemDetail manejarEstadoInvalido(EstadoInvalidoException ex) {
        return problema(HttpStatus.CONFLICT, "Estado inválido para la operación", ex.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    ProblemDetail manejarReglaNegocio(ReglaNegocioException ex) {
        return problema(HttpStatus.UNPROCESSABLE_CONTENT, "Regla de negocio incumplida", ex.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail manejarConcurrencia(ObjectOptimisticLockingFailureException ex) {
        return problema(HttpStatus.CONFLICT, "Conflicto de concurrencia",
                "El recurso fue modificado por otra operación. Consulte su estado actual e intente de nuevo.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail manejarInesperado(Exception ex) {
        log.error("Error no controlado", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrió un error inesperado. Si persiste, contacte al equipo de soporte.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        List<Map<String, String>> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("campo", e.getField(), "mensaje", String.valueOf(e.getDefaultMessage())))
                .toList();
        ProblemDetail body = problema(HttpStatus.BAD_REQUEST, "Solicitud inválida",
                "Uno o más campos no cumplen las validaciones");
        body.setProperty("errores", errores);
        return ResponseEntity.badRequest().body(body);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        String parametro = ex instanceof MethodArgumentTypeMismatchException m ? m.getName() : ex.getPropertyName();
        String detalle = "Valor '%s' inválido para '%s'".formatted(ex.getValue(), parametro);
        Class<?> tipo = ex.getRequiredType();
        if (tipo != null && tipo.isEnum()) {
            detalle += ". Valores permitidos: " + Arrays.toString(tipo.getEnumConstants());
        }
        return ResponseEntity.badRequest().body(problema(HttpStatus.BAD_REQUEST, "Parámetro inválido", detalle));
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalle) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detalle);
        problem.setTitle(titulo);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
