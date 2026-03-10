package com.example.cognito_lab.controller;

import com.example.cognito_lab.dto.PartnerSubmission;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/partners")
public class PartnerController {

    private static final List<String> ALLOWED_TYPES = List.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public PartnerController(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @GetMapping("/whoami")
    public ResponseEntity<?> whoAmI(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "user", authentication.getName(),
                "status", "authenticated"
        ));
    }

    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> submit(
            @RequestPart("data") String dataJson,
            @RequestPart("files") List<MultipartFile> files,
            Authentication authentication) {

        validateFiles(files);

        PartnerSubmission submission = parseSubmission(dataJson);
        validateSubmission(submission);

        submission.setPartnerEmail(authentication.getName());

        return ResponseEntity.ok(Map.of(
                "user", authentication.getName(),
                "record", Map.of(
                        "nombre", submission.getNombre(),
                        "empresa", submission.getEmpresa(),
                        "mensaje", submission.getMensaje(),
                        "partnerEmail", submission.getPartnerEmail()
                ),
                "filesCount", files.size(),
                "status", "ok"
        ));
    }

    private PartnerSubmission parseSubmission(String dataJson) {
        try {
            return objectMapper.readValue(dataJson, PartnerSubmission.class);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El campo 'data' no contiene un JSON válido"
            );
        }
    }

    private void validateSubmission(PartnerSubmission submission) {
        Set<ConstraintViolation<PartnerSubmission>> violations = validator.validate(submission);

        if (!violations.isEmpty()) {
            String message = violations.iterator().next().getMessage();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private void validateFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe adjuntarse al menos un archivo"
            );
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Uno de los archivos está vacío"
                );
            }

            String contentType = file.getContentType();

            if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Tipo de archivo no permitido: " + file.getOriginalFilename()
                );
            }

            if (file.getSize() > MAX_BYTES) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Archivo demasiado grande: " + file.getOriginalFilename()
                );
            }
        }
    }
}