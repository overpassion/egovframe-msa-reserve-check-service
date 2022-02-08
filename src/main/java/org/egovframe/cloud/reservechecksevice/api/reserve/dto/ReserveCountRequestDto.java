package org.egovframe.cloud.reservechecksevice.api.reserve.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * org.egovframe.cloud.reservechecksevice.api.reserve.dto.ReserveCountRequestDto
 * <p>
 * 얘약 물품 재고 조회 시 해당 물품에 예약건 조회 요청 dto class
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
@Getter
@NoArgsConstructor
public class ReserveCountRequestDto {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
