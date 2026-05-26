package com.bgupdata.lottery.util;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;

import com.bgupdata.lottery.model.DebugLevel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 日志管理器 - 异步写入，按天分文件存储
 * 日志存放在外部存储: /sdcard/ShuiGuoCaiJi/logs/
 * 电脑USB连接手机后可直接查看
 */
public class LogManager {

    private static LogManager instance;
    private final File logDir;
    private final HandlerThread handlerThread;
    private final Handler asyncHandler;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private LogManager(Context context) {
        File externalDir = Environment.getExternalStorageDirectory();
        logDir = new File(externalDir, "ShuiGuoCaiJi/logs");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }

        handlerThread = new HandlerThread("LogWriter");
        handlerThread.start();
        asyncHandler = new Handler(handlerThread.getLooper());
    }

    public static synchronized LogManager getInstance(Context context) {
        if (instance == null) {
            instance = new LogManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * 获取日志目录路径（供外部显示）
     */
    public String getLogDirPath() {
        return logDir.getAbsolutePath();
    }

    /**
     * 异步写入日志
     */
    public void writeLog(String message, DebugLevel level) {
        asyncHandler.post(() -> {
            String fileName = "log_" + dateFormat.format(new Date()) + ".txt";
            File logFile = new File(logDir, fileName);
            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
                String time = timeFormat.format(new Date());
                String levelStr = levelToString(level);
                writer.println(time + " [" + levelStr + "] " + message);
            } catch (IOException e) {
                // 写入失败静默处理
            }
        });
    }

    /**
     * 读取当天日志内容
     */
    public String readTodayLogs() {
        String fileName = "log_" + dateFormat.format(new Date()) + ".txt";
        return readLogFile(fileName);
    }

    /**
     * 读取指定日志文件内容
     */
    public String readLogFile(String fileName) {
        File logFile = new File(logDir, fileName);
        if (!logFile.exists()) return "";

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            // 读取失败返回空
        }
        return sb.toString();
    }

    /**
     * 获取所有日志文件列表（按时间倒序）
     */
    public List<String> getLogFiles() {
        List<String> files = new ArrayList<>();
        File[] logFiles = logDir.listFiles((dir, name) -> name.startsWith("log_") && name.endsWith(".txt"));
        if (logFiles != null) {
            Arrays.sort(logFiles, (a, b) -> b.getName().compareTo(a.getName()));
            for (File f : logFiles) {
                files.add(f.getName());
            }
        }
        return files;
    }

    /**
     * 删除当天日志文件
     */
    public boolean deleteTodayLog() {
        String fileName = "log_" + dateFormat.format(new Date()) + ".txt";
        return deleteLogFile(fileName);
    }

    /**
     * 删除指定日志文件
     */
    public boolean deleteLogFile(String fileName) {
        File logFile = new File(logDir, fileName);
        if (logFile.exists()) {
            return logFile.delete();
        }
        return false;
    }

    /**
     * 删除全部日志文件
     */
    public void deleteAllLogs() {
        File[] logFiles = logDir.listFiles();
        if (logFiles != null) {
            for (File f : logFiles) {
                f.delete();
            }
        }
    }

    private String levelToString(DebugLevel level) {
        switch (level) {
            case INFO: return "INFO";
            case WARN: return "WARN";
            case ERROR: return "ERROR";
            default: return "NORMAL";
        }
    }

    public void shutdown() {
        handlerThread.quitSafely();
    }
}
