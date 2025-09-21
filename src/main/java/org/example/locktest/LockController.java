package org.example.locktest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@RestController
public class LockController {
    private final LockService lockService;

    @GetMapping("/pessimistic-lock/{id}")
    public ResponseEntity<Book> pessimisticLock(@PathVariable Long id) {
        System.out.println("now : " + LocalDateTime.now());
        return ResponseEntity.ok(lockService.pessimisticLock(id));
    }

    @GetMapping("/optimistic-lock/{id}")
    public ResponseEntity<Book> optimisticLock(@PathVariable Long id) {
        System.out.println("now : " + LocalDateTime.now());
        return ResponseEntity.ok(lockService.optimisticLock(id));
    }

    // ===== 데드락 시나리오 트리거용 REST 엔드포인트 =====
    @GetMapping("/deadlock/pessimistic/ab")
    public ResponseEntity<Boolean> deadlockPessimisticAB(@RequestParam Long bookId, @RequestParam Long authorId) {
        return ResponseEntity.ok(lockService.pessimisticDeadlockAB(bookId, authorId));
    }

    @GetMapping("/deadlock/pessimistic/ba")
    public ResponseEntity<Boolean> deadlockPessimisticBA(@RequestParam Long bookId, @RequestParam Long authorId) {
        return ResponseEntity.ok(lockService.pessimisticDeadlockBA(bookId, authorId));
    }

    @GetMapping("/deadlock/optimistic/ab")
    public ResponseEntity<Boolean> deadlockOptimisticAB(@RequestParam Long bookId, @RequestParam Long authorId) {
        return ResponseEntity.ok(lockService.optimisticDeadlockAB(bookId, authorId));
    }

    @GetMapping("/deadlock/optimistic/ba")
    public ResponseEntity<Boolean> deadlockOptimisticBA(@RequestParam Long bookId, @RequestParam Long authorId) {
        return ResponseEntity.ok(lockService.optimisticDeadlockBA(bookId, authorId));
    }

    @PostMapping("/create")
    public ResponseEntity<Book> create() {
        return ResponseEntity.ok(lockService.create());
    }

    @PostMapping("/create-author")
    public ResponseEntity<Author> createAuthor() {
        return ResponseEntity.ok(lockService.createAuthor());
    }
}
