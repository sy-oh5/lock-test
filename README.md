# lock-test

동시성(비관적 락/낙관적 락)과 데드락 시나리오를 실험한 프로젝트

- 언어/런타임: Java 21
- 프레임워크: Spring Boot 3.5.x, Spring Data JPA, Spring MVC
- DB: H2 (in-memory)
- 빌드: Gradle

## 주요 목표
- 비관적 락(PESSIMISTIC_WRITE)과 낙관적 락(OPTIMISTIC / OPTIMISTIC_FORCE_INCREMENT) 동작을 REST 호출과 테스트를 통해 이해
- 동시 요청에서의 대기/타임아웃, 버전 충돌 예외, 교착상태(데드락) 관찰

---

## 테스트

- optimisticLock: 낙관적 락 동시 요청 시 충돌
- pessimisticLock2: 같은 엔드포인트를 동시에 호출해 총 소요 시간(대기) 관찰
- pessimisticLock3: 순차 호출로 I/O 대기와 락 대기 비교
- deadlock_pessimistic_ab_ba: AB vs BA 반대 순서 잠금으로 데드락/타임아웃 관찰
- deadlock_optimistic_ab_ba: 낙관적 락 FORCE_INCREMENT로 상호 충돌 관찰
---

## MySQL vs PostgreSQL

| 구분 | MySQL | PostgreSQL                                      |
|---|---|-------------------------------------------------|
| **Lock 방식** | 비관적 락: `Select ... For Update` 데이터를 잠금 | 낙관적 락: 데이터를 스냅샷 떠서 버전으로 관리                 |
| **가정** | 충돌이 자주 발생한다는 가정 | 충돌이 자주 발생하지 않는다는 가정                             |
| **충돌 시** | 잠금이 풀릴 때까지 대기 | 에러를 뱉음                                          |
| **장점** | - 잠금이 종료될 때까지 대기하고 수행 (단, 타임아웃 발생할 수 있음) | - DB 오버헤드가 적다 <br>- 갱신 손실이 발생되지 않음 (데이터 무결성 보장) |
| **단점** | - DB 오버헤드가 크다 <br>- 갱신 손실이 발생될 수 있음 | - 동시성 에러 시 재시도 요청해야 함 (별도 로직 작성 필요)             |
| **Deadlock** | 잠금 의존성 그래프에서 감지하여 발생 | 커밋 시점에 에러 발생                                    |

본 프로젝트는 H2 데이터베이스를 사용했으므로, 위에 기술된 MySQL, PostgreSQL과의 락(Lock) 방식 및 에러 처리 방식에 차이가 있을 수 있습니다.
