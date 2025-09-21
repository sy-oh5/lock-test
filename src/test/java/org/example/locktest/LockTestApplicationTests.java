package org.example.locktest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LockTestApplicationTests {

    @Autowired
    LockService lockService;

    @Autowired
    BookRepository bookRepository;

    Long bookId;

    @BeforeEach
    void setUp() {
        // 1) 테스트용 데이터 초기화
        bookRepository.deleteAll();
        Book saved = bookRepository.save(new Book("JPA", 10));
        bookId = saved.getId();
        assertNotNull(bookId);
    }

    @Test
    void pessimisticLock(){
        lockService.pessimisticLock(bookId);
    }
}
