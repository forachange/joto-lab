package com.joto.lab.es.core.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * @author joey
 * @description
 * @date 2023/12/13 14:27
 */
public class DateUtil {

    private DateUtil(){}

    /**
     * 时间戳
     * @param localDate
     * @return
     */
    public static long toEpochMilli(LocalDate localDate) {
        return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 时间戳
     * @param localDateTime
     * @return
     */
    public static long toEpochMilli(LocalDateTime localDateTime) {
        return localDateTime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
    }

    public static boolean isSameYearMonth(final LocalDate startDatetime, final LocalDate endDatetime) {
        return startDatetime.getYear() == endDatetime.getYear() &&
                startDatetime.getMonthValue() == endDatetime.getMonthValue();
    }
}
