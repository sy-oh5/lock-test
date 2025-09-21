package org.example.locktest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LockTestApplicationTests {

    int port = 8080;

    RestClient restClient;

    Long bookId;
    Long authorId;

    @BeforeEach
    void setUp() {
        // 테스트용 RestClient를 랜덤 포트로 초기화
        restClient = RestClient.create("http://localhost:" + port);
        // 1) 테스트용 데이터 초기화
        Book saved = restClient.post().uri("/create").retrieve().body(Book.class);
        bookId = saved.getId();
        assertNotNull(bookId);

        Author author = restClient.post().uri("/create-author").retrieve().body(Author.class);
        authorId = author.getId();
        assertNotNull(authorId);
    }


    @DisplayName("동시요청시 에러를 뱉음")
    @Test
    void optimisticLock() {

        CompletableFuture<Book> f1 = CompletableFuture.supplyAsync(this::callOptimisticLock);
        CompletableFuture<Book> f2 = CompletableFuture.supplyAsync(this::callOptimisticLock);

        try {
            CompletableFuture.allOf(f1, f2).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new AssertionError("Concurrent request failed", e);
        }
    }


    @DisplayName("동시 2요청: 응답시간 4초 - Lock wait")
    @Test
    void pessimisticLock2() {
        long start = System.currentTimeMillis();

        CompletableFuture<Book> f1 = CompletableFuture.supplyAsync(this::callPessimisticLock);
        CompletableFuture<Book> f2 = CompletableFuture.supplyAsync(this::callPessimisticLock);

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
        callPessimisticLock();
        callPessimisticLock();
        long took = System.currentTimeMillis() - start;
        System.out.println("동시 2요청 총 응답시간 : " + took);
    }

    @DisplayName("비관적 락 데드락: AB vs BA 동시 호출 시 한쪽은 실패 발생")
    @Test
    void deadlock_pessimistic_ab_ba() throws Exception {
        // step 1) 서로 반대 순서로 잠금을 시도하는 두 요청을 동시에 실행
        CompletableFuture<Integer> a = CompletableFuture.supplyAsync(this::callDeadlockPessimisticAB);
        CompletableFuture<Integer> b = CompletableFuture.supplyAsync(this::callDeadlockPessimisticBA);

        // step 2) 두 요청 종료 대기 후 상태코드 수집
        Integer s1 = a.get();
        Integer s2 = b.get();

        System.out.println("s1=" + s1 + ", s2=" + s2);
    }

    @DisplayName("낙관적 락 상호 충돌: AB vs BA 동시 호출 시 최소 한쪽 실패 기대")
    @Test
    void deadlock_optimistic_ab_ba() throws Exception {
        // step 1) 낙관적 락은 DB 블로킹이 없으므로 둘 다 동시 진행됨
        CompletableFuture<Integer> a = CompletableFuture.supplyAsync(this::callDeadlockOptimisticAB);
        CompletableFuture<Integer> b = CompletableFuture.supplyAsync(this::callDeadlockOptimisticBA);

        // step 2) 종료 대기
        Integer s1 = a.get();
        Integer s2 = b.get();

        System.out.println("s1=" + s1 + ", s2=" + s2);
    }

    private Book callPessimisticLock(){
        return restClient.get().uri("/pessimistic-lock/" + bookId).retrieve().body(Book.class);
    }

    private Book callOptimisticLock(){
        return restClient.get().uri("/optimistic-lock/" + bookId).retrieve().body(Book.class);
    }

    // ===== 데드락/충돌 트리거용 REST 호출 헬퍼들 =====
    private int callDeadlockPessimisticAB() {
        try {
            restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/deadlock/pessimistic/ab")
                            .queryParam("bookId", bookId)
                            .queryParam("authorId", authorId)
                            .build())
                    .retrieve()
                    .body(String.class);
            return 200;
        } catch (RestClientResponseException e) {
            return e.getStatusCode().value();
        } catch (Exception e) {
            return 500;
        }
    }

    private int callDeadlockPessimisticBA() {
        try {
            restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/deadlock/pessimistic/ba")
                            .queryParam("bookId", bookId)
                            .queryParam("authorId", authorId)
                            .build())
                    .retrieve()
                    .body(String.class);
            return 200;
        } catch (RestClientResponseException e) {
            return e.getStatusCode().value();
        } catch (Exception e) {
            return 500;
        }
    }

    private int callDeadlockOptimisticAB() {
        try {
            restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/deadlock/optimistic/ab")
                            .queryParam("bookId", bookId)
                            .queryParam("authorId", authorId)
                            .build())
                    .retrieve()
                    .body(String.class);
            return 200;
        } catch (RestClientResponseException e) {
            return e.getStatusCode().value();
        } catch (Exception e) {
            return 500;
        }
    }

    private int callDeadlockOptimisticBA() {
        try {
            restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/deadlock/optimistic/ba")
                            .queryParam("bookId", bookId)
                            .queryParam("authorId", authorId)
                            .build())
                    .retrieve()
                    .body(String.class);
            return 200;
        } catch (RestClientResponseException e) {
            return e.getStatusCode().value();
        } catch (Exception e) {
            return 500;
        }
    }
}
