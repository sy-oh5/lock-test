package org.example.locktest.book;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PessimisticLockService {

    private final BookRepository bookRepository;

    public PessimisticLockService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    // Acquire PESSIMISTIC_WRITE lock and hold it for 3 seconds
    @Transactional
    public void lockReadAndSleep(Long id) {
        bookRepository.findByIdForUpdate(id).orElseThrow();
        sleep3s();
    }

    // Try to acquire PESSIMISTIC_WRITE lock and update title
    @Transactional
    public void tryAcquireAndUpdateTitle(Long id) {
        Book book = bookRepository.findByIdForUpdate(id).orElseThrow();
        book.setTitle(book.getTitle() + "!");
        bookRepository.flush();
    }

    private void sleep3s() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
