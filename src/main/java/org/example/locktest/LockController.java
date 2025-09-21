package org.example.locktest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class LockController {
    private final LockService lockService;

    @GetMapping("/pessimistic-lock")
    public ResponseEntity<Boolean> pessimisticLock(Long id) {
        lockService.pessimisticLock(id);
        return ResponseEntity.ok(true);
    }
}
