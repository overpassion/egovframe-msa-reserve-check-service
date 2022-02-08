package org.egovframe.cloud.reservechecksevice.validator;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.egovframe.cloud.reservechecksevice.validator.annotation.ReserveSaveValid;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * org.egovframe.cloud.reservechecksevice.validator.ReserveSaveValidator
 *
 * 예약 신청 시 validation check를 하기 위한 custom validator
 *
 * @author 표준프레임워크센터 shinmj
 * @version 1.0
 * @since 2021/09/23
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *     수정일        수정자           수정내용
 *  ----------    --------    ---------------------------
 *  2021/09/23    shinmj       최초 생성
 * </pre>
 */
@Slf4j
@RequiredArgsConstructor
public class ReserveSaveValidator implements ConstraintValidator<ReserveSaveValid, Object> {

    private String message;


    @Override
    public void initialize(ReserveSaveValid constraintAnnotation) {
        message = constraintAnnotation.message();
    }

    /**
     * 예약 신청 시 비지니스 로직에 의한 validation check
     *
     * @param value
     * @param context
     * @return
     */
    @SneakyThrows
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        boolean fieldValid = true;

        String categoryId = String.valueOf(getFieldValue(value, "categoryId"));
        if ("education".equals(categoryId)) {
            //교육인 경우
            //신청인원
            fieldValid = checkReserveQty(value, context);

        }else if ("equipment".equals(categoryId)) {
            //장비인 경우
            //신청일자(기간), 신청수량
            fieldValid = checkReserveDate(value, context);
            fieldValid = checkReserveQty(value, context);

        }else if ("place".equals(categoryId)) {
            //공간인 경우
            //신청일자(기간)
            fieldValid = checkReserveDate(value, context);
        }

        return fieldValid;
    }

    /**
     * 예약 수량 체크
     *
     * @param value
     * @param context
     * @return
     */
    @SneakyThrows
    private boolean checkReserveQty(Object value, ConstraintValidatorContext context) {
        if (isNull(value, "reserveQty")) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("예약 수량은 필수입니다.")
                    .addPropertyNode("reserveQty")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

    /**
     * 예약 신청 기간 체크
     *
     * @param value
     * @param context
     * @return
     */
    @SneakyThrows
    private boolean checkReserveDate(Object value, ConstraintValidatorContext context) {
        // 예약 신청 기간 필수
        if (isNull(value, "reserveStartDate")) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("예약 신청 시작 기간은 필수입니다.")
                    .addPropertyNode("reserveStartDate")
                    .addConstraintViolation();
            return false;
        } else if (isNull(value, "reserveEndDate")) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("예약 신청 종료 기간은 필수입니다.")
                    .addPropertyNode("reserveEndDate")
                    .addConstraintViolation();
            return false;
        }else {
            // 예약 시작일, 종료일 체크
            LocalDateTime reserveStartDate = (LocalDateTime) getFieldValue(value, "reserveStartDate");
            LocalDateTime reserveEndDate = (LocalDateTime) getFieldValue(value, "reserveEndDate");
            if (reserveStartDate.isAfter(reserveEndDate)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("시작일이 종료일 보다 큽니다.")
                    .addPropertyNode("reserveStartDate")
                    .addConstraintViolation();
                return false;
            }
        }
        return true;
    }

    /**
     * 해당하는 field의 값 조회
     *
     * @param object
     * @param fieldName
     * @return
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private Object getFieldValue(Object object, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Class<?> clazz = object.getClass();
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(object);
    }

    /**
     * 해당하는 Field가 null인지 체크
     *
     * @param object
     * @param fieldName
     * @return
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private boolean isNull(Object object, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Class<?> clazz = object.getClass();
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(object) == null || !StringUtils.hasLength(String.valueOf(field.get(object)));
    }
}
