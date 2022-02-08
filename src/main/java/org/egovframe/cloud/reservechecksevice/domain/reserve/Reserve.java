package org.egovframe.cloud.reservechecksevice.domain.reserve;

import lombok.*;
import org.egovframe.cloud.reactive.domain.BaseEntity;
import org.egovframe.cloud.reservechecksevice.api.reserve.dto.ReserveUpdateRequestDto;
import org.egovframe.cloud.reservechecksevice.client.dto.UserResponseDto;
import org.egovframe.cloud.reservechecksevice.domain.location.Location;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * org.egovframe.cloud.reservechecksevice.domain.reserve.Reserve
 *
 * 예약 도메인 클래스
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
@Getter
@NoArgsConstructor
@ToString
@With
@Table("reserve")
public class Reserve extends BaseEntity {

    @Id
    @Column
    private String reserveId;     //예약 id

    @Column
    private Long reserveItemId; //예약 물품 id

    @Transient
    private ReserveItem reserveItem;

    @Setter
    @Column
    private Long locationId;    //지역 id

    @Setter
    @Column
    private String categoryId;  //예약유형 - 공통코드 reserve-category

    @Column
    private Integer reserveQty;    //예약 신청 인원/수량

    @Column
    private String reservePurposeContent;   //예약 목적

    @Column
    private String attachmentCode;  //첨부파일 코드

    @Column
    private LocalDateTime reserveStartDate; //예약 신청 시작일
    @Column
    private LocalDateTime reserveEndDate; //예약 신청 종료일

    @Column
    private String reserveStatusId;     //예약상태 - 공통코드(reserve-status)

    @Column
    private String userId;  //예약자

    @Transient
    private UserResponseDto user;

    @Column
    private String userContactNo;   //예약자 연락처

    @Column("user_email_addr")
    private String userEmail;   //예약자 이메일

    @Builder
    public Reserve(String reserveId, Long reserveItemId,
        ReserveItem reserveItem, Long locationId, String categoryId, Integer reserveQty,
        String reservePurposeContent, String attachmentCode, LocalDateTime reserveStartDate,
        LocalDateTime reserveEndDate, String reserveStatusId, String userId,
        UserResponseDto user, String userContactNo, String userEmail) {
        this.reserveId = reserveId;
        this.reserveItemId = reserveItemId;
        this.reserveItem = reserveItem;
        this.locationId = locationId;
        this.categoryId = categoryId;
        this.reserveQty = reserveQty;
        this.reservePurposeContent = reservePurposeContent;
        this.attachmentCode = attachmentCode;
        this.reserveStartDate = reserveStartDate;
        this.reserveEndDate = reserveEndDate;
        this.reserveStatusId = reserveStatusId;
        this.userId = userId;
        this.user = user;
        this.userContactNo = userContactNo;
        this.userEmail = userEmail;
    }

    public Reserve setReserveItem(ReserveItem reserveItem) {
        this.reserveItem = reserveItem;
        this.reserveItemId = reserveItem.getReserveItemId();
        return this;
    }

    public Reserve setUser(UserResponseDto user) {
        this.user = user;
        this.userId = user.getUserId();
        return this;
    }

    public Reserve updateStatus(String reserveStatusId) {
        this.reserveStatusId = reserveStatusId;
        return this;
    }

    public Reserve update(ReserveUpdateRequestDto updateRequestDto) {
        this.reserveQty = updateRequestDto.getReserveQty();
        this.reservePurposeContent = updateRequestDto.getReservePurposeContent();
        this.attachmentCode = updateRequestDto.getAttachmentCode();
        this.reserveStartDate = updateRequestDto.getReserveStartDate();
        this.reserveEndDate = updateRequestDto.getReserveEndDate();
        this.userId = updateRequestDto.getUserId();
        this.userEmail = updateRequestDto.getUserEmail();
        this.userContactNo = updateRequestDto.getUserContactNo();
        return this;
    }
}
