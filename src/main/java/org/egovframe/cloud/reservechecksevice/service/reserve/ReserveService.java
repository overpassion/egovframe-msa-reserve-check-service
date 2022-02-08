package org.egovframe.cloud.reservechecksevice.service.reserve;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.egovframe.cloud.common.domain.Role;
import org.egovframe.cloud.common.exception.BusinessMessageException;
import org.egovframe.cloud.reactive.service.ReactiveAbstractService;
import org.egovframe.cloud.reservechecksevice.api.reserve.dto.ReserveCountRequestDto;
import org.egovframe.cloud.reservechecksevice.api.reserve.dto.ReserveCountResponseDto;
import org.egovframe.cloud.reservechecksevice.api.reserve.dto.ReserveListResponseDto;
import org.egovframe.cloud.reservechecksevice.api.reserve.dto.ReserveRequestDto;
import org.egovframe.cloud.reservechecksevice.api.reserve.dto.ReserveResponseDto;
import org.egovframe.cloud.reservechecksevice.api.reserve.dto.ReserveSaveRequestDto;
import org.egovframe.cloud.reservechecksevice.api.reserve.dto.ReserveUpdateRequestDto;
import org.egovframe.cloud.reservechecksevice.client.ReserveItemServiceClient;
import org.egovframe.cloud.reservechecksevice.domain.reserve.Reserve;
import org.egovframe.cloud.reservechecksevice.domain.reserve.ReserveRepository;
import org.egovframe.cloud.reservechecksevice.domain.reserve.ReserveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * org.egovframe.cloud.reservechecksevice.service.reserve.ReserveService
 *
 * 예약 service 클래스
 *
 * @author 표준프레임워크센터 shinmj
 * @version 1.0
 * @since 2021/09/15
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *     수정일        수정자           수정내용
 *  ----------    --------    ---------------------------
 *  2021/09/15    shinmj       최초 생성
 * </pre>
 */
@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class ReserveService extends ReactiveAbstractService {


    private final ReserveRepository reserveRepository;
    private final ReserveItemServiceClient reserveItemServiceClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * entity -> dto 변환
     *
     * @param reserve
     * @return
     */
    private Mono<ReserveResponseDto> convertReserveResponseDto(Reserve reserve) {
        return Mono.just(ReserveResponseDto.builder()
                .entity(reserve)
                .build());
    }

    /**
     * entity -> 목록 dto 변환
     *
     * @param reserve
     * @return
     */
    private Mono<ReserveListResponseDto> convertReserveListResponseDto(Reserve reserve) {
        return Mono.just(ReserveListResponseDto.builder()
                .entity(reserve)
                .build());
    }

    /**
     * 현재 로그인 사용자가 관리자인지 체크
     *
     * @return
     */
    private Mono<Boolean> getIsAdmin() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getAuthorities)
                .map(grantedAuthorities -> {
                    List<SimpleGrantedAuthority> authorities =
                            new ArrayList<>((Collection<? extends SimpleGrantedAuthority>) grantedAuthorities);
                    SimpleGrantedAuthority adminRole = new SimpleGrantedAuthority(Role.ADMIN.getKey());
                    return authorities.contains(adminRole);
                });
    }

    /**
     * 현재 로그인 사용자 id
     *
     * @return
     */
    private Mono<String> getUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .map(String.class::cast);
    }

    /**
     * 목록 조회
     *
     * @param requestDto
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Mono<Page<ReserveListResponseDto>> search(ReserveRequestDto requestDto, Pageable pageable) {
        return reserveRepository.search(requestDto, pageable)
                .switchIfEmpty(Flux.empty())
                .flatMap(this::convertReserveListResponseDto)
                .collectList()
                .zipWith(reserveRepository.searchCount(requestDto, pageable))
                .flatMap(tuple -> Mono.just(new PageImpl<>(tuple.getT1(), pageable, tuple.getT2())));
    }

    /**
     * 한건 조회 dto return
     *
     * @param reserveId
     * @return
     */
    @Transactional(readOnly = true)
   public Mono<ReserveResponseDto> findReserveById(Long reserveId) {
        return reserveRepository.findReserveById(reserveId)
                .switchIfEmpty(monoResponseStatusEntityNotFoundException(reserveId))
                .flatMap(this::convertReserveResponseDto);
   }

    /**
     * 사용자용 예약 목록 조회 (로그인 사용자의 예약정보만 조회)
     *
     * @param userId
     * @param requestDto
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    public Mono<Page<ReserveListResponseDto>> searchForUser(String userId, ReserveRequestDto requestDto, Pageable pageable) {
        return reserveRepository.searchForUser(requestDto, pageable, userId)
                .switchIfEmpty(Flux.empty())
                .flatMap(this::convertReserveListResponseDto)
                .collectList()
                .zipWith(reserveRepository.searchCountForUser(requestDto, pageable, userId))
                .flatMap(tuple -> Mono.just(new PageImpl<>(tuple.getT1(), pageable, tuple.getT2())));
    }

    /**
     * 예약 정보 취소
     *
     * @param reserveId
     * @return
     */
    public Mono<Void> cancel(String reserveId) {
        return getIsAdmin().flatMap(isAdmin -> {
            if (isAdmin) {
                return reserveCancel(reserveId);
            } else {
                return findById(reserveId)
                    .zipWith(getUserId())
                    .flatMap(tuple -> {
                        if (tuple.getT1().getUserId().equals(tuple.getT2())) {
                            return Mono.just(tuple.getT1());
                        }else {
                            return Mono.error(new BusinessMessageException("해당 예약은 취소할 수 없습니다."));
                        }
                    })
                    .onErrorResume(throwable -> Mono.error(throwable))
                    .flatMap(reserve -> reserveCancel(reserveId));
            }
        });

    }

    /**
     * 예약 상태 취소로 변경
     *
     * @param reserveId
     * @return
     */
    private Mono<Void> reserveCancel(String reserveId) {
        System.out.println("reserveCancel : " + reserveId);
        return findById(reserveId)
                .map(reserve -> {
                    if (ReserveStatus.DONE.getKey().equals(reserve.getReserveStatusId())) {
                        throw new BusinessMessageException("해당 예약은 이미 실행되어 취소할 수 없습니다.");
                    }else {
                        return reserve.updateStatus(ReserveStatus.CANCEL.getKey());
                    }
                })
                .flatMap(reserveRepository::save)
                .then();
    }

    /**
     * 예약 정보 승인
     *
     * @param reserveId
     * @return
     */
    public Mono<Void> approve(String reserveId) {
        return getIsAdmin()
            .flatMap(isAdmin -> {
                if (isAdmin) {
                    return Mono.just(reserveId);
                }else {
                    return Mono.error(new BusinessMessageException("관리자만 승인할 수 있습니다."));
                }
            })
            .flatMap(this::findById)
            .flatMap(this::checkReserveItems)
            .onErrorResume(throwable -> Mono.error(throwable))
            .flatMap(reserve -> Mono.just(reserve.updateStatus(ReserveStatus.APPROVE.getKey())))
            .flatMap(reserveRepository::save).then();
    }

    /**
     * 예약 물품 재고 및 예약 일자 체크
     *
     * @param reserve
     * @return
     */
    private Mono<Reserve> checkReserveItems(Reserve reserve) {
        return reserveItemServiceClient.findById(reserve.getReserveItemId())
            .transform(CircuitBreakerOperator.of(circuitBreakerRegistry.circuitBreaker("reserve-item")))
            .onErrorResume(throwable -> Mono.empty())
            .flatMap(reserveItemResponseDto -> {
                // 교육, 장비인 경우 재고수량 체크
                if (!reserveItemResponseDto.getCategoryId().equals("space")) {
                    if (reserveItemResponseDto.getInventoryQty() <= 0) {
                        return Mono.error(new BusinessMessageException("예약가능한 재고/인원이 없습니다."));
                    }
                    if (reserveItemResponseDto.getInventoryQty() < reserve.getReserveQty()) {
                        return Mono.error(new BusinessMessageException("예약가능한 재고/인원이 부족합니다. (재고/인원:" + reserveItemResponseDto.getInventoryQty() + ")"));
                    }
                }
                // 장비, 공간인 경우 예약일자 체크
                if (!reserveItemResponseDto.getCategoryId().equals("education")) {
                    LocalDateTime startDate = reserveItemResponseDto.getReserveMeansId().equals("realtime") ?
                        reserveItemResponseDto.getRequestStartDate() : reserveItemResponseDto.getOperationStartDate();
                    LocalDateTime endDate = reserveItemResponseDto.getReserveMeansId().equals("realtime") ?
                        reserveItemResponseDto.getRequestEndDate() : reserveItemResponseDto.getOperationEndDate();

                    if (reserve.getReserveStartDate().isBefore(startDate)) {
                        return Mono.error(new BusinessMessageException("시작일이 운영/예약 시작일 이전입니다."));
                    }

                    if (reserve.getReserveEndDate().isAfter(endDate)) {
                        return Mono.error(new BusinessMessageException("종료일이 운영/예약 종료일 이후입니다."));
                    }
                }

                return Mono.just(reserve);
            });
    }

    /**
     * 예약 정보 수정
     *
     * @param reserveId
     * @return
     */
    public Mono<Reserve> update(String reserveId, ReserveUpdateRequestDto updateRequestDto) {
        return getIsAdmin().flatMap(isAdmin -> {
            if (isAdmin) {
                return updateReserve(reserveId, updateRequestDto);
            } else {
                return updateReserveForUser(reserveId, updateRequestDto);
            }
        });
    }

    /**
     * 사용자 예약 수정
     *
     * @param reserveId
     * @param updateRequestDto
     * @return
     */
    private Mono<Reserve> updateReserveForUser(String reserveId, ReserveUpdateRequestDto updateRequestDto) {
        return findById(reserveId)
                .zipWith(getUserId())
                .map(tuple -> {
                    if (!tuple.getT1().getUserId().equals(tuple.getT2())) {
                        throw new BusinessMessageException("해당 예약은 수정할 수 없습니다.");
                    }

                    if (!ReserveStatus.REQUEST.getKey().equals(tuple.getT1().getReserveStatusId())) {
                        throw new BusinessMessageException("예약 신청 상태인 경우에만 수정 가능합니다.");
                    }

                    return tuple.getT1().update(updateRequestDto);
                })
                .flatMap(this::checkReserveItems)
                .onErrorResume(throwable -> Mono.error(throwable))
                .flatMap(reserveRepository::save);
    }

    /**
     * 관리자 예약 수정
     *
     * @param reserveId
     * @param updateRequestDto
     * @return
     */
    private Mono<Reserve> updateReserve(String reserveId, ReserveUpdateRequestDto updateRequestDto) {
        return findById(reserveId)
                .map(reserve -> {
                    if (!ReserveStatus.REQUEST.getKey().equals(reserve.getReserveStatusId())) {
                        throw new BusinessMessageException("예약 신청 상태인 경우에만 수정 가능합니다.");
                    }
                    return reserve.update(updateRequestDto);
                })
                .flatMap(this::checkReserveItems)
                .onErrorResume(throwable -> Mono.error(throwable))
                .flatMap(reserveRepository::save);
    }

    /**
     * 한건 정보 조회 entity return
     *
     * @param reserveId
     * @return
     */
    private Mono<Reserve> findById(String reserveId) {
        return reserveRepository.findById(reserveId)
                .switchIfEmpty(monoResponseStatusEntityNotFoundException(reserveId));
    }

    /**
     * 관리자 예약 신청
     * 관리자의 경우 실시간이어도 이벤트 스트림 거치지 않고 바로 예약 처리
     *
     * @param saveRequestDto
     * @return
     */
    public Mono<ReserveResponseDto> create(ReserveSaveRequestDto saveRequestDto) {
        return Mono.just(saveRequestDto)
            .map(dto -> {
                String uuid = UUID.randomUUID().toString();
                dto.setReserveId(uuid);
                return dto.toEntity();
            })
            .flatMap(reserveRepository::insert)
            .flatMap(this::checkReserveItems)
            .flatMap(reserveRepository::loadRelations)
            .flatMap(this::convertReserveResponseDto);

    }


    /**
     * 예약 물품별 기간안에 있는 예약 목록 조회
     *
     * @param reserveItemId
     * @param requestDto
     * @return
     */
    @Transactional(readOnly = true)
    public Flux<ReserveCountResponseDto> findAllByReserveDate(Long reserveItemId, ReserveCountRequestDto requestDto) {
        return reserveRepository.findAllByReserveDate(reserveItemId, requestDto.getStartDate(), requestDto.getEndDate())
                .flatMap(reserve -> Mono.just(ReserveCountResponseDto.builder().entity(reserve).build()));
    }
}
