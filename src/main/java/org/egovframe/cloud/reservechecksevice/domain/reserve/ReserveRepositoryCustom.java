package org.egovframe.cloud.reservechecksevice.domain.reserve;

import org.egovframe.cloud.common.dto.RequestDto;
import org.egovframe.cloud.reservechecksevice.api.reserve.dto.ReserveRequestDto;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * org.egovframe.cloud.reservechecksevice.domain.reserve.ReserveRepositoryCustom
 *
 * 예약 도메인 custom Repository interface
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
public interface ReserveRepositoryCustom {
    Flux<Reserve> search(ReserveRequestDto requestDto, Pageable pageable);
    Mono<Long> searchCount(ReserveRequestDto requestDto, Pageable pageable);
    Mono<Reserve> findReserveById(Long reserveId);

    Flux<Reserve> searchForUser(ReserveRequestDto requestDto, Pageable pageable, String userId);
    Mono<Long> searchCountForUser(ReserveRequestDto requestDto, Pageable pageable, String userId);

    Mono<Reserve> loadRelations(Reserve reserve);

    Flux<Reserve> findAllByReserveDate(Long reserveItemId, LocalDateTime startDate, LocalDateTime endDate);

    Mono<Reserve> insert(Reserve reserve);

}
