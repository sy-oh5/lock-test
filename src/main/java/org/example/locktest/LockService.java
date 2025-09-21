package org.example.locktest;

import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class LockService {

    private final BookRepository bookRepository;

    // 비관적 락을 획득한 뒤 3초 동안 유지해 다른 트랜잭션이 대기/타임아웃 되도록 합니다.
    @Transactional
    public Book pessimisticLock(Long id) {
        Book book = bookRepository.findByIdForUpdateAsPessimistic(id).orElseThrow(); // SELECT ... FOR UPDATE
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread was interrupted while holding lock", e);
        }
        return book;
    }

    @Transactional
    public Book optimisticLock(Long id) {
        Book book = bookRepository.findByIdForUpdateAsOptimistic(id).orElseThrow(
                () -> new RuntimeException("Book not found")
        );
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread was interrupted while holding lock", e);
        }
        return book;
    }

    @Transactional
    public Book create() {
        return bookRepository.save(Book.create("title"));
    }
}
