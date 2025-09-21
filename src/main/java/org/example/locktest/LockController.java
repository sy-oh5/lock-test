package org.example.locktest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class LockController {
    private final LockService lockService;

    @GetMapping("/pessimistic-lock/{id}")
    public ResponseEntity<Book> pessimisticLock(@PathVariable Long id) {
        return ResponseEntity.ok(lockService.pessimisticLock(id));
    }
}
