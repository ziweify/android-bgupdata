package com.bgupdata.lottery.service;

import com.bgupdata.lottery.api.BgBaseApi;
import com.bgupdata.lottery.api.LotteryCollector;
import com.bgupdata.lottery.model.DebugLevel;
import com.bgupdata.lottery.model.LotteryData;
import com.bgupdata.lottery.post.PostFactory;
import com.bgupdata.lottery.post.PostLotteryData;
import com.bgupdata.lottery.post.PostResult;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 任务管理器 - 控制采集和投递的核心逻辑
 */
public class TaskManager {

    public interface TaskCallback {
        void onCountdownUpdate(int seconds);
        void onIssueUpdate(String curIssueId, String curTime, String nextIssueId, String nextTime);
        void onCollectingListUpdate(List<LotteryData> list);
        void onCompletedListUpdate(List<LotteryData> list);
        void onFailedListUpdate(List<LotteryData> list);
        void onDebugMessage(String message, DebugLevel level);
        void onStatusChange(boolean running);
    }

    private TaskCallback callback;
    private ScheduledExecutorService countdownExecutor;
    private ExecutorService collectExecutor;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private String curIssueId = "";
    private String curOpenTime = "";
    private String nextIssueId = "";
    private String nextOpenTime = "";

    private final CopyOnWriteArrayList<LotteryData> collectingList = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<LotteryData> completedList = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<LotteryData> failedList = new CopyOnWriteArrayList<>();

    private String submitAddress = "";
    private String proxyIp = "";
    private boolean useProxy = false;

    private static final int MAX_POST_RETRY = 3;

    public TaskManager() {}

    public void setCallback(TaskCallback callback) {
        this.callback = callback;
    }

    public void setConfig(String submitAddress, String proxyIp, boolean useProxy) {
        this.submitAddress = submitAddress;
        this.proxyIp = proxyIp;
        this.useProxy = useProxy;
    }

    public boolean isRunning() { return isRunning.get(); }

    public List<LotteryData> getCollectingList() { return new ArrayList<>(collectingList); }
    public List<LotteryData> getCompletedList() { return new ArrayList<>(completedList); }
    public List<LotteryData> getFailedList() { return new ArrayList<>(failedList); }

    /**
     * 启动任务
     */
    public void start() {
        if (isRunning.get()) return;
        isRunning.set(true);

        if (callback != null) callback.onStatusChange(true);
        debug("启动任务", DebugLevel.INFO);

        refreshIssueData();

        countdownExecutor = Executors.newSingleThreadScheduledExecutor();
        countdownExecutor.scheduleAtFixedRate(this::tickCountdown, 0, 1, TimeUnit.SECONDS);

        collectExecutor = Executors.newSingleThreadExecutor();
        collectExecutor.submit(this::collectLoop);
    }

    /**
     * 停止任务
     */
    public void stop() {
        if (!isRunning.get()) return;
        isRunning.set(false);

        if (countdownExecutor != null) {
            countdownExecutor.shutdownNow();
            countdownExecutor = null;
        }
        if (collectExecutor != null) {
            collectExecutor.shutdownNow();
            collectExecutor = null;
        }

        if (callback != null) callback.onStatusChange(false);
        debug("任务已停止", DebugLevel.INFO);
    }

    /**
     * 刷新期号数据
     */
    private void refreshIssueData() {
        try {
            Date now = new Date();
            int nextId = BgBaseApi.getNextIssueId(now);
            int curId = nextId - 1;

            long curTimestamp = BgBaseApi.getOpenTimestamp(curId);
            long nextTimestamp = BgBaseApi.getOpenTimestamp(nextId);

            curIssueId = String.valueOf(curId);
            curOpenTime = BgBaseApi.formatDate(BgBaseApi.getDateFromTimestamp(curTimestamp));
            nextIssueId = String.valueOf(nextId);
            nextOpenTime = BgBaseApi.formatDate(BgBaseApi.getDateFromTimestamp(nextTimestamp));

            if (callback != null) {
                callback.onIssueUpdate(curIssueId, curOpenTime, nextIssueId, nextOpenTime);
            }
        } catch (Exception e) {
            debug("刷新期号失败: " + e.getMessage(), DebugLevel.ERROR);
        }
    }

    /**
     * 倒计时tick - 每秒执行
     */
    private void tickCountdown() {
        if (!isRunning.get()) return;

        try {
            if (nextIssueId.isEmpty() || nextOpenTime.isEmpty()) {
                refreshIssueData();
                return;
            }

            int nextId = Integer.parseInt(nextIssueId);
            long nextTimestamp = BgBaseApi.getOpenTimestamp(nextId);
            long nowSeconds = System.currentTimeMillis() / 1000;
            int countdown = (int) (nextTimestamp - nowSeconds);

            if (callback != null) {
                callback.onCountdownUpdate(countdown);
            }

            if (countdown < 0) {
                insertCollecting(nextId);
                refreshIssueData();
            }
        } catch (Exception e) {
            debug("倒计时异常: " + e.getMessage(), DebugLevel.ERROR);
        }
    }

    /**
     * 插入采集进行中列表
     */
    private void insertCollecting(int issueId) {
        for (LotteryData d : collectingList) {
            if (d.getIssueId() == issueId) return;
        }
        LotteryData newData = new LotteryData(issueId);
        collectingList.add(0, newData);
        debug("新增采集任务: " + issueId, DebugLevel.NORMAL);
        notifyCollectingUpdate();
    }

    /**
     * 采集循环
     */
    private void collectLoop() {
        while (isRunning.get()) {
            try {
                if (!collectingList.isEmpty()) {
                    LotteryData first = collectingList.get(0);
                    int curId = 0;
                    if (!curIssueId.isEmpty()) {
                        curId = Integer.parseInt(curIssueId);
                    }

                    List<LotteryData> fetchedItems = new ArrayList<>();

                    // 优先使用最新一期API
                    if (first.getIssueId() == curId) {
                        try {
                            LotteryData latest = LotteryCollector.fetchLatest(useProxy ? proxyIp : null);
                            if (latest != null && latest.getIssueId() == curId) {
                                fetchedItems.add(latest);
                                debug("采集最新: " + latest.getIssueId() + " -> " + latest.getOpenData(), DebugLevel.NORMAL);
                            }
                        } catch (Exception e) {
                            debug("采集最新API失败: " + e.getMessage(), DebugLevel.WARN);
                        }
                    }

                    // 如果没有采到或者有多期需要采集，使用按日期API
                    if (fetchedItems.isEmpty() || collectingList.size() > 1) {
                        try {
                            String dateStr = BgBaseApi.getTodayDateString();
                            List<LotteryData> dateItems = LotteryCollector.fetchByDate(dateStr, 1, 10, useProxy ? proxyIp : null);
                            for (LotteryData item : dateItems) {
                                boolean exists = false;
                                for (LotteryData f : fetchedItems) {
                                    if (f.getIssueId() == item.getIssueId()) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) fetchedItems.add(item);
                            }
                        } catch (Exception e) {
                            debug("按日期采集失败: " + e.getMessage(), DebugLevel.WARN);
                        }
                    }

                    // 匹配并移动到完成列表
                    matchAndPost(fetchedItems);
                }

                Thread.sleep(isCollectingListEmpty() ? 2000 : 1500);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                debug("采集循环异常: " + e.getMessage(), DebugLevel.ERROR);
                try { Thread.sleep(2000); } catch (InterruptedException ie) { break; }
            }
        }
    }

    private boolean isCollectingListEmpty() {
        return collectingList.isEmpty();
    }

    /**
     * 匹配采集结果并投递，检测过期期号标记为失败
     */
    private void matchAndPost(List<LotteryData> fetchedItems) {
        // 找出采集到的最大期号，用于判断过期
        int maxFetchedIssue = 0;
        for (LotteryData f : fetchedItems) {
            if (f.getIssueId() > maxFetchedIssue) {
                maxFetchedIssue = f.getIssueId();
            }
        }

        List<LotteryData> toRemove = new ArrayList<>();

        for (LotteryData collecting : collectingList) {
            collecting.incrementAcCount();
            boolean matched = false;

            for (LotteryData fetched : fetchedItems) {
                if (fetched.getIssueId() == collecting.getIssueId()) {
                    fetched.setStatus(LotteryData.Status.SUCCESS);
                    completedList.add(0, fetched);
                    toRemove.add(collecting);
                    matched = true;

                    debug(String.format("采集成功: %d -> %s", fetched.getIssueId(), fetched.getOpenData()), DebugLevel.INFO);
                    postData(fetched);
                    break;
                }
            }

            // 未匹配到且采集到的最大期号已超过该期号10期以上，判定为失败
            if (!matched && maxFetchedIssue > 0 && (maxFetchedIssue - collecting.getIssueId()) >= 10) {
                collecting.setStatus(LotteryData.Status.FAILED);
                collecting.setAcTime(BgBaseApi.formatTime(new java.util.Date()));
                failedList.add(0, collecting);
                toRemove.add(collecting);

                debug(String.format("采集失败(过期): %d, 已超过%d期", collecting.getIssueId(), maxFetchedIssue - collecting.getIssueId()), DebugLevel.ERROR);
            }
        }

        for (LotteryData item : toRemove) {
            collectingList.remove(item);
        }

        if (!toRemove.isEmpty()) {
            notifyCollectingUpdate();
            notifyCompletedUpdate();
            notifyFailedUpdate();
        }
    }

    /**
     * 投递数据到所有配置的服务器
     */
    private void postData(LotteryData data) {
        List<PostLotteryData> posters = PostFactory.create(submitAddress);
        if (posters.isEmpty()) {
            debug("没有配置投递地址", DebugLevel.WARN);
            return;
        }

        debug(String.format("投递: %d - %s", data.getIssueId(), data.getOpenData()), DebugLevel.INFO);

        for (PostLotteryData poster : posters) {
            Executors.newSingleThreadExecutor().submit(() -> {
                int count = 0;
                while (count < MAX_POST_RETRY) {
                    count++;
                    try {
                        PostResult result = poster.post(data);
                        debug(String.format("[%d] %d -> %s :: %d :: %s",
                                count, data.getIssueId(), poster.getUrl(),
                                result.getStatusCode(), result.getHtml()), DebugLevel.INFO);

                        if (result.isSuccess()) break;
                    } catch (Exception e) {
                        debug(String.format("投递异常[%d]: %d -> %s :: %s",
                                count, data.getIssueId(), poster.getUrl(), e.getMessage()), DebugLevel.ERROR);
                    }
                }
                if (count >= MAX_POST_RETRY) {
                    debug(String.format("投递%d次失败: %d -> %s", MAX_POST_RETRY, data.getIssueId(), poster.getUrl()), DebugLevel.ERROR);
                }
            });
        }
    }

    private void notifyCollectingUpdate() {
        if (callback != null) {
            callback.onCollectingListUpdate(new ArrayList<>(collectingList));
        }
    }

    private void notifyCompletedUpdate() {
        if (callback != null) {
            callback.onCompletedListUpdate(new ArrayList<>(completedList));
        }
    }

    private void notifyFailedUpdate() {
        if (callback != null) {
            callback.onFailedListUpdate(new ArrayList<>(failedList));
        }
    }

    private void debug(String message, DebugLevel level) {
        if (callback != null) {
            callback.onDebugMessage(message, level);
        }
    }
}
