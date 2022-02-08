package org.egovframe.cloud.reservechecksevice.api.reserve.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.egovframe.cloud.reservechecksevice.client.dto.ReserveItemRelationResponseDto;
import org.egovframe.cloud.reservechecksevice.domain.reserve.Reserve;

import java.time.LocalDateTime;

/**
 * org.egovframe.cloud.reservechecksevice.api.reserve.dto.ReserveCountResponseDto
 * <p>
 * 얘약 물품 재고 조회 시 해당 물품에 예약건 조회 응답 dto class
 *
 * @author 표준프레임워크센터 shinmj
 * @version 1.0
 * @since 2021/09/27
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *     수정일        수정자           수정내용
 *  ----------    --------    ---------------------------
 *  2021/09/27    shinmj      최초 생성
 * </pre>
 */
@NoArgsConstructor
@Getter
public class ReserveCountResponseDto {
    private String reserveId;
    private Long reserveItemId;
    private Integer reserveQty;
    private LocalDateTime reserveStartDate;
    private LocalDateTime reserveEndDate;

    @Builder
    public ReserveCountResponseDto(Reserve entity) {
        this.reserveId = entity.getReserveId();
        this.reserveItemId = entity.getReserveItemId();
        this.reserveQty = entity.getReserveQty();
        this.reserveStartDate = entity.getReserveStartDate();
        this.reserveEndDate = entity.getReserveEndDate();
    }
}
