package org.example.locktest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LockTestApplicationTests {
    RestClient restClient = RestClient.create("http://localhost:8080");

    @Autowired
    BookRepository bookRepository;

    Long bookId;

    @BeforeEach
    void setUp() {
        // 1) 테스트용 데이터 초기화
        bookRepository.deleteAll();
        Book saved = bookRepository.save(Book.create("title"));
        bookId = saved.getId();
        assertNotNull(bookId);
    }


    @DisplayName("응답시간 3초")
    @Test
    void pessimisticLock() {
        long start = System.currentTimeMillis();
        callPessmisticLock();
        System.out.println("응답시간 : " + (System.currentTimeMillis() - start));
    }


    @DisplayName("응답시간 6초")
    @Test
    void pessimisticLock2() {
        long start = System.currentTimeMillis();
        callPessmisticLock();
        callPessmisticLock();
        System.out.println("응답시간 : " + (System.currentTimeMillis() - start));
    }

    private Book callPessmisticLock(){
        Book book = restClient.get().uri("/pessimistic-lock/" + bookId).retrieve().body(Book.class);
        System.out.println("book id = " + book.getId());
        return book;
    }
}
