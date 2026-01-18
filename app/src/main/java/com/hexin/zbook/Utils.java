package com.hexin.zbook;

import android.text.format.DateUtils;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    /**
     * 从一个可能不规范的日期字符串中健壮地提取年份。
     * - "2014" -> 2014
     * - "14" -> 2014 (处理两位数年份)
     * - "2024-05-10" -> 2024
     * - "May 2023" -> 2023
     * - "invalid-string" -> null
     * - null or "" -> null
     * @param dateStr 原始日期字符串
     * @return 提取到的四位数年份，如果无法提取则返回 null
     */
    public static Integer extractYear(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        // 使用正则表达式查找一个4位数字或一个2位数字
        // \b 确保是单词边界，避免从 "12345" 中匹配 "1234"
        Pattern pattern = Pattern.compile("\\b(\\d{4}|\\d{2})\\b");
        Matcher matcher = pattern.matcher(dateStr);

        if (matcher.find()) {
            String yearStr = matcher.group(1);
            try {
                int year = Integer.parseInt(yearStr);
                // 处理两位数年份 ("14" -> 2014)
                if (year < 100) {
                    int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                    // 假设 "00-29" 是 20xx, "30-99" 是 19xx. 这是一个常见的处理方式。
                    // 或者更简单，我们假设所有两位数都是2000年以后。
                    if (year + 2000 <= currentYear + 1) { // 加1以容纳明年
                        return year + 2000; // e.g., 24 -> 2024
                    } else {
                        return year + 1900; // e.g., 99 -> 1999
                    }
                }
                // 处理四位数年份
                if (year > 1000 && year < 3000) { // 一个合理的年份范围
                    return year;
                }
            } catch (NumberFormatException e) {
                // 正则表达式保证了这是数字，所以这里理论上不会发生
                return null;
            }
        }
        return null; // 没有找到匹配的年份
    }


    public static String formatRelativeTime(long timestamp) {
        return DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS).toString();
    }
}
