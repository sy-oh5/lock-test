package org.example.locktest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LockTestApplicationTests {
    @LocalServerPort
    int port;

    RestClient restClient;

    Long bookId;

    @BeforeEach
    void setUp() {
        // 테스트용 RestClient를 랜덤 포트로 초기화
        restClient = RestClient.create("http://localhost:" + port);
        // 1) 테스트용 데이터 초기화
        Book saved = restClient.post().uri("/create").retrieve().body(Book.class);
        bookId = saved.getId();
        assertNotNull(bookId);
    }


    @DisplayName("응답시간 2초")
    @Test
    void pessimisticLock() {
        long start = System.currentTimeMillis();
        callPessmisticLock();
        long took = System.currentTimeMillis() - start;
        System.out.println("응답시간 : " + took);
    }


    @DisplayName("동시 2요청: 응답시간 4초")
    @Test
    void pessimisticLock2() throws Exception {
        // step 1) 두 개의 동시 요청을 만들어 각 3초 슬립을 포함한 비관적 락 엔드포인트를 호출
        long start = System.currentTimeMillis();

        CompletableFuture<Book> f1 = CompletableFuture.supplyAsync(this::callPessmisticLock);
        CompletableFuture<Book> f2 = CompletableFuture.supplyAsync(this::callPessmisticLock);

        // step 2) 두 요청이 모두 완료될 때까지 대기
        try {
            CompletableFuture.allOf(f1, f2).get();
        } catch (ExecutionException e) {
            throw new AssertionError("Concurrent request failed", e);
        }

        long took = System.currentTimeMillis() - start;
        System.out.println("동시 2요청 총 응답시간 : " + took);
    }

    @DisplayName("동시 2요청: 응답시간 4초 - fake")
    @Test
    void pessimisticLock3() throws Exception {
        long start = System.currentTimeMillis();
        callPessmisticLock();
        callPessmisticLock();
        long took = System.currentTimeMillis() - start;
        System.out.println("동시 2요청 총 응답시간 : " + took);
    }

    private Book callPessmisticLock(){
        Book book = restClient.get().uri("/pessimistic-lock/" + bookId).retrieve().body(Book.class);
        System.out.println("book id = " + book.getId());
        return book;
    }
}
