package org.example.locktest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class OptimisticLockService {

    private final EntityManager em;

    @Transactional
    public void lock(Long id) throws InterruptedException {
        em.find(Book.class, id, LockModeType.OPTIMISTIC);
        Thread.sleep(3000);
    }
}
