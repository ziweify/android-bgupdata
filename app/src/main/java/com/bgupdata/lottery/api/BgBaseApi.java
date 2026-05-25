package com.bgupdata.lottery.api;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 台湾宾果基本API - 期号计算与时间戳转换
 * 每天203期，每5分钟一期，从07:05开始到23:55
 */
public class BgBaseApi {

    private static final int COUNT_REAL = 203;
    // 2026年基准数据
    private static final int FIRST_ISSUE_ID = 115000001;
    private static final long FIRST_TIMESTAMP = 1767222300L;

    /**
     * 通过当前时间计算下一期期号
     */
    public static int getNextIssueId(Date time) {
        Date firstDatetime = getDateFromTimestamp(FIRST_TIMESTAMP);
        long diffMs = time.getTime() - firstDatetime.getTime();
        int days = (int) (diffMs / (1000 * 60 * 60 * 24));

        int tempIssue = FIRST_ISSUE_ID + days * COUNT_REAL;
        int tempCount = 0;

        for (int i = 0; i < COUNT_REAL; i++) {
            long fTimestamp = getOpenTimestamp(tempIssue + i);
            Date fTime = getDateFromTimestamp(fTimestamp);
            if (time.after(fTime)) {
                tempCount++;
            } else {
                break;
            }
        }

        return tempIssue + tempCount;
    }

    /**
     * 通过期号计算该期的开奖时间戳(秒)
     */
    public static long getOpenTimestamp(int issueId) {
        int days = getDays(issueId);
        int number = getNumber(issueId);

        long baseMs = FIRST_TIMESTAMP * 1000L;
        long dayMs = (long) days * 24 * 60 * 60 * 1000L;
        long minuteMs = (long) (number - 1) * 5 * 60 * 1000L;

        return (baseMs + dayMs + minuteMs) / 1000L;
    }

    /**
     * 获取期号是当天的第几期 (1-203)
     */
    public static int getNumber(int issueId) {
        int value = issueId - FIRST_ISSUE_ID;
        if (value >= 0) {
            return value % COUNT_REAL + 1;
        } else {
            int result = value % COUNT_REAL + 1;
            return COUNT_REAL - Math.abs(result);
        }
    }

    /**
     * 获取期号相对于基准期号的天数
     */
    public static int getDays(int issueId) {
        int value = issueId - FIRST_ISSUE_ID;
        return value / COUNT_REAL;
    }

    /**
     * 时间戳(秒)转Date
     */
    public static Date getDateFromTimestamp(long timestamp) {
        return new Date(timestamp * 1000L);
    }

    /**
     * 格式化日期为字符串
     */
    public static String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Taipei"));
        return sdf.format(date);
    }

    /**
     * 格式化日期为短时间字符串
     */
    public static String formatTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Taipei"));
        return sdf.format(date);
    }

    /**
     * 获取当天日期字符串 yyyy-MM-dd
     */
    public static String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Taipei"));
        return sdf.format(new Date());
    }
}
