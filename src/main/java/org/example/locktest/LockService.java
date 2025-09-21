package org.example.locktest;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class LockService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    // 비관적 락을 획득한 뒤 2초 동안 유지해 다른 트랜잭션이 대기/타임아웃 되도록 합니다.
    @Transactional
    public Book pessimisticLock(Long id) {
        Book book = bookRepository.findByIdForUpdateAsPessimistic(id).orElseThrow(); // SELECT ... FOR UPDATE
        sleep(2000);
        return book;
    }

    // 낙관적 락(버전 강제 증가)으로 읽고 2초 대기 — 충돌 시 커밋/플러시에서 예외 발생
    @Transactional
    public Book optimisticLock(Long id) {
        Book book = bookRepository.findByIdForUpdateAsOptimistic(id).orElseThrow();
        sleep(2000);
        return book;
    }

    // 데드락 실험용 데이터 생성
    @Transactional
    public Book create() {
        return bookRepository.save(Book.create("title"));
    }

    // =========================
    // 데드락 시나리오 (비관적 락)
    // 두 트랜잭션이 서로 잠금 순서를 반대로 잡으면 교착 상태를 유발할 수 있습니다.
    // T1: Book → Author,  T2: Author → Book
    // =========================

    /**
     * 비관적 락 데드락: Book 먼저 잡고(SELECT FOR UPDATE) 잠시 대기 후 Author 잠금 시도
     */
    @Transactional
    public String pessimisticDeadlockAB(Long bookId, Long authorId) {
        // step1) Book 행을 비관적 락으로 선점
        bookRepository.findByIdForUpdateAsPessimistic(bookId).orElseThrow();
        // step2) 잠시 대기하여 반대편 트랜잭션이 Author를 먼저 잡을 시간을 줌
        sleep(500);
        // step3) 이제 Author 락을 시도 — 반대편이 이미 Book을 기다리고 있으면 교착 가능
        authorRepository.findByIdForUpdateAsPessimistic(authorId).orElseThrow();
        // step4) 실험 가시성을 위해 약간 더 유지
        sleep(1500);
        return "OK";
    }

    /**
     * 비관적 락 데드락: Author 먼저 → Book
     */
    @Transactional
    public String pessimisticDeadlockBA(Long bookId, Long authorId) {
        // step1) Author 행을 비관적 락으로 선점
        authorRepository.findByIdForUpdateAsPessimistic(authorId).orElseThrow();
        // step2) 잠시 대기하여 반대편이 Book을 먼저 잡게 함
        sleep(500);
        // step3) 이제 Book 락을 시도 — 반대편과 순서가 반대라면 교착 가능
        bookRepository.findByIdForUpdateAsPessimistic(bookId).orElseThrow();
        // step4) 약간 유지
        sleep(1500);
        return "OK";
    }

    // =========================
    // 데드락(유사) 시나리오 (낙관적 락)
    // 낙관적 락은 DB 레벨 잠금을 걸지 않아 '블로킹형 교착'은 없지만,
    // 서로 버전을 올리며 플러시/커밋 시 양쪽이 충돌 예외를 맞는 상황(상호 충돌)을 재현합니다.
    // =========================

    /**
     * 낙관적 락 충돌 시나리오: Book → Author 순서로 OPTIMISTIC_FORCE_INCREMENT 후 대기
     */
    @Transactional
    public String optimisticDeadlockAB(Long bookId, Long authorId) {
        // step1) Book을 낙관적(버전 강제 증가) 모드로 로드
        bookRepository.findByIdForUpdateAsOptimistic(bookId).orElseThrow();
        // step2) 반대편 트랜잭션이 Author를 먼저 로드하도록 짧게 대기
        sleep(500);
        // step3) Author도 낙관적 모드로 로드
        authorRepository.findByIdForUpdateAsOptimistic(authorId).orElseThrow();
        // step4) 커밋 시점에 두 트랜잭션 모두 버전 증가를 시도 → 한쪽 이상에서 버전 충돌 예외
        sleep(1500);
        return "OK";
    }

    /**
     * 낙관적 락 충돌 시나리오: Author → Book 순서
     */
    @Transactional
    public String optimisticDeadlockBA(Long bookId, Long authorId) {
        // step1) Author를 낙관적(버전 강제 증가) 모드로 로드
        authorRepository.findByIdForUpdateAsOptimistic(authorId).orElseThrow();
        // step2) 반대편이 Book을 먼저 로드하도록 대기
        sleep(500);
        // step3) Book도 낙관적 모드로 로드
        bookRepository.findByIdForUpdateAsOptimistic(bookId).orElseThrow();
        // step4) 커밋 시 양쪽에서 버전 증가 충돌 가능
        sleep(1500);
        return "OK";
    }

    // 공통 슬립 유틸 (인터럽트 플래그 복원)
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread interrupted", e);
        }
    }
}
