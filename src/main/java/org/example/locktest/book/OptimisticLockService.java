package org.example.locktest.book;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OptimisticLockService {

    private final EntityManager em;
    private final BookRepository bookRepository;

    public OptimisticLockService(EntityManager em, BookRepository bookRepository) {
        this.em = em;
        this.bookRepository = bookRepository;
    }

    // Acquire optimistic lock, hold for 3 seconds, then update title and flush
    @Transactional
    public void lockReadSleepAndUpdateTitle(Long id, String suffix) {
        Book book = em.find(Book.class, id, LockModeType.OPTIMISTIC);
        sleep3s();
        book.setTitle(book.getTitle() + suffix);
        bookRepository.saveAndFlush(book);
    }

    private void sleep3s() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
