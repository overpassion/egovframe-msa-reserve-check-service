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
 * ?????? service ?????????
 *
 * @author ??????????????????????????? shinmj
 * @version 1.0
 * @since 2021/09/15
 *
 * <pre>
 * << ????????????(Modification Information) >>
 *
 *     ?????????        ?????????           ????????????
 *  ----------    --------    ---------------------------
 *  2021/09/15    shinmj       ?????? ??????
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
     * entity -> dto ??????
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
     * entity -> ?????? dto ??????
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
     * ?????? ????????? ???????????? ??????????????? ??????
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
     * ?????? ????????? ????????? id
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
     * ?????? ??????
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
     * ?????? ?????? dto return
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
     * ???????????? ?????? ?????? ?????? (????????? ???????????? ??????????????? ??????)
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
     * ?????? ?????? ??????
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
                            return Mono.error(new BusinessMessageException("?????? ????????? ????????? ??? ????????????."));
                        }
                    })
                    .onErrorResume(throwable -> Mono.error(throwable))
                    .flatMap(reserve -> reserveCancel(reserveId));
            }
        });

    }

    /**
     * ?????? ?????? ????????? ??????
     *
     * @param reserveId
     * @return
     */
    private Mono<Void> reserveCancel(String reserveId) {
        System.out.println("reserveCancel : " + reserveId);
        return findById(reserveId)
                .map(reserve -> {
                    if (ReserveStatus.DONE.getKey().equals(reserve.getReserveStatusId())) {
                        throw new BusinessMessageException("?????? ????????? ?????? ???????????? ????????? ??? ????????????.");
                    }else {
                        return reserve.updateStatus(ReserveStatus.CANCEL.getKey());
                    }
                })
                .flatMap(reserveRepository::save)
                .then();
    }

    /**
     * ?????? ?????? ??????
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
                    return Mono.error(new BusinessMessageException("???????????? ????????? ??? ????????????."));
                }
            })
            .flatMap(this::findById)
            .flatMap(this::checkReserveItems)
            .onErrorResume(throwable -> Mono.error(throwable))
            .flatMap(reserve -> Mono.just(reserve.updateStatus(ReserveStatus.APPROVE.getKey())))
            .flatMap(reserveRepository::save).then();
    }

    /**
     * ?????? ?????? ?????? ??? ?????? ?????? ??????
     *
     * @param reserve
     * @return
     */
    private Mono<Reserve> checkReserveItems(Reserve reserve) {
        return reserveItemServiceClient.findById(reserve.getReserveItemId())
            .transform(CircuitBreakerOperator.of(circuitBreakerRegistry.circuitBreaker("reserve-item")))
            .onErrorResume(throwable -> Mono.empty())
            .flatMap(reserveItemResponseDto -> {
                // ??????, ????????? ?????? ???????????? ??????
                if (!reserveItemResponseDto.getCategoryId().equals("space")) {
                    if (reserveItemResponseDto.getInventoryQty() <= 0) {
                        return Mono.error(new BusinessMessageException("??????????????? ??????/????????? ????????????."));
                    }
                    if (reserveItemResponseDto.getInventoryQty() < reserve.getReserveQty()) {
                        return Mono.error(new BusinessMessageException("??????????????? ??????/????????? ???????????????. (??????/??????:" + reserveItemResponseDto.getInventoryQty() + ")"));
                    }
                }
                // ??????, ????????? ?????? ???????????? ??????
                if (!reserveItemResponseDto.getCategoryId().equals("education")) {
                    LocalDateTime startDate = reserveItemResponseDto.getReserveMeansId().equals("realtime") ?
                        reserveItemResponseDto.getRequestStartDate() : reserveItemResponseDto.getOperationStartDate();
                    LocalDateTime endDate = reserveItemResponseDto.getReserveMeansId().equals("realtime") ?
                        reserveItemResponseDto.getRequestEndDate() : reserveItemResponseDto.getOperationEndDate();

                    if (reserve.getReserveStartDate().isBefore(startDate)) {
                        return Mono.error(new BusinessMessageException("???????????? ??????/?????? ????????? ???????????????."));
                    }

                    if (reserve.getReserveEndDate().isAfter(endDate)) {
                        return Mono.error(new BusinessMessageException("???????????? ??????/?????? ????????? ???????????????."));
                    }
                }

                return Mono.just(reserve);
            });
    }

    /**
     * ?????? ?????? ??????
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
     * ????????? ?????? ??????
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
                        throw new BusinessMessageException("?????? ????????? ????????? ??? ????????????.");
                    }

                    if (!ReserveStatus.REQUEST.getKey().equals(tuple.getT1().getReserveStatusId())) {
                        throw new BusinessMessageException("?????? ?????? ????????? ???????????? ?????? ???????????????.");
                    }

                    return tuple.getT1().update(updateRequestDto);
                })
                .flatMap(this::checkReserveItems)
                .onErrorResume(throwable -> Mono.error(throwable))
                .flatMap(reserveRepository::save);
    }

    /**
     * ????????? ?????? ??????
     *
     * @param reserveId
     * @param updateRequestDto
     * @return
     */
    private Mono<Reserve> updateReserve(String reserveId, ReserveUpdateRequestDto updateRequestDto) {
        return findById(reserveId)
                .map(reserve -> {
                    if (!ReserveStatus.REQUEST.getKey().equals(reserve.getReserveStatusId())) {
                        throw new BusinessMessageException("?????? ?????? ????????? ???????????? ?????? ???????????????.");
                    }
                    return reserve.update(updateRequestDto);
                })
                .flatMap(this::checkReserveItems)
                .onErrorResume(throwable -> Mono.error(throwable))
                .flatMap(reserveRepository::save);
    }

    /**
     * ?????? ?????? ?????? entity return
     *
     * @param reserveId
     * @return
     */
    private Mono<Reserve> findById(String reserveId) {
        return reserveRepository.findById(reserveId)
                .switchIfEmpty(monoResponseStatusEntityNotFoundException(reserveId));
    }

    /**
     * ????????? ?????? ??????
     * ???????????? ?????? ?????????????????? ????????? ????????? ????????? ?????? ?????? ?????? ??????
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
     * ?????? ????????? ???????????? ?????? ?????? ?????? ??????
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
