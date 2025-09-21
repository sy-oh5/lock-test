package org.example.locktest;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class LockService {

    private final BookRepository bookRepository;

    // 비관적 락을 획득한 뒤 3초 동안 유지해 다른 트랜잭션이 대기/타임아웃 되도록 합니다.
    @Transactional
    public Book pessimisticLock(Long id) {
        Book book = bookRepository.findByIdForUpdate(id).orElseThrow(); // SELECT ... FOR UPDATE
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread was interrupted while holding lock", e);
        }
        return book;
    }
}
