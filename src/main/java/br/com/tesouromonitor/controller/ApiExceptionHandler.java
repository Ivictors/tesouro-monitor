package br.com.tesouromonitor.controller;

import br.com.tesouromonitor.service.AtivoCarteiraNaoEncontradoException;
import br.com.tesouromonitor.service.AlertaNaoEncontradoException;
import br.com.tesouromonitor.service.IntervaloDataInvalidoException;
import br.com.tesouromonitor.service.CotacaoNaoEncontradaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AtivoCarteiraNaoEncontradoException.class)
    public ResponseEntity<ProblemDetail> tratarNaoEncontrado(AtivoCarteiraNaoEncontradoException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Ativo não encontrado");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(CotacaoNaoEncontradaException.class)
    public ResponseEntity<ProblemDetail> tratarCotacaoAusente(CotacaoNaoEncontradaException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Cotação indisponível");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(AlertaNaoEncontradoException.class)
    public ResponseEntity<ProblemDetail> tratarAlertaNaoEncontrado(AlertaNaoEncontradoException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Alerta não encontrado");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(IntervaloDataInvalidoException.class)
    public ResponseEntity<ProblemDetail> tratarIntervaloInvalido(IntervaloDataInvalidoException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Intervalo de datas inválido");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> tratarValidacao(MethodArgumentNotValidException exception) {
        java.util.List<String> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Os dados enviados são inválidos.");
        problem.setTitle("Erro de validação");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> tratarParametroInvalido(ConstraintViolationException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Os parâmetros de consulta são inválidos.");
        problem.setTitle("Parâmetro inválido");
        return ResponseEntity.badRequest().body(problem);
    }
}
