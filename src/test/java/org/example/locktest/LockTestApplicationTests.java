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


    @DisplayName("동시 2요청: 응답시간 4초 - Lock wait")
    @Test
    void pessimisticLock2() {
        long start = System.currentTimeMillis();

        CompletableFuture<Book> f1 = CompletableFuture.supplyAsync(this::callPessmisticLock);
        CompletableFuture<Book> f2 = CompletableFuture.supplyAsync(this::callPessmisticLock);

        try {
            CompletableFuture.allOf(f1, f2).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new AssertionError("Concurrent request failed", e);
        }

        long took = System.currentTimeMillis() - start;
        System.out.println("동시 2요청 총 응답시간 : " + took);
    }

    @DisplayName("동시 2요청: 응답시간 4초 - I/O wait")
    @Test
    void pessimisticLock3() {
        /*
        * 언뜻 보기엔 위 방식과 동일한 응답시간을 가져 같은 테스트처럼 보일 수 있으나
        * 첫번째 call 후 3초 기다리고 2번째 call이라 응답시간이 4초가 걸린 fake test case였다.
        * */
        long start = System.currentTimeMillis();
        callPessmisticLock();
        callPessmisticLock();
        long took = System.currentTimeMillis() - start;
        System.out.println("동시 2요청 총 응답시간 : " + took);
    }

    private Book callPessmisticLock(){
        return restClient.get().uri("/pessimistic-lock/" + bookId).retrieve().body(Book.class);
    }
}
