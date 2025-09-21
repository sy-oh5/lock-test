package org.example.locktest.book;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class PessimisticLockService {

    private final BookRepository bookRepository;


    @Transactional
    public void lock(Long id) throws InterruptedException {
        bookRepository.findByIdForUpdate(id).orElseThrow();
        Thread.sleep(3000);
    }
}
