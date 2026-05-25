package com.bgupdata.lottery;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bgupdata.lottery.adapter.LotteryAdapter;
import com.bgupdata.lottery.model.DebugLevel;
import com.bgupdata.lottery.model.LotteryData;
import com.bgupdata.lottery.service.LotteryService;
import com.bgupdata.lottery.service.TaskManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TaskManager.TaskCallback {

    private TextView tvStatus, tvIssueCur, tvTimeCur, tvIssueNext, tvTimeNext, tvCountdown;
    private TextView tvCollectingTitle, tvCompletedTitle, tvDebug;
    private EditText etSubmitAddress, etProxy;
    private SwitchMaterial switchProxy;
    private MaterialButton btnStart, btnStop, btnClearDebug;
    private RecyclerView rvCollecting, rvCompleted;
    private ScrollView svDebug;

    private LotteryAdapter collectingAdapter;
    private LotteryAdapter completedAdapter;

    private TaskManager taskManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final SpannableStringBuilder debugBuilder = new SpannableStringBuilder();
    private int debugLineCount = 0;
    private static final int MAX_DEBUG_LINES = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initAdapters();
        initDefaultConfig();
        initListeners();

        taskManager = new TaskManager();
        taskManager.setCallback(this);
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tv_status);
        tvIssueCur = findViewById(R.id.tv_issue_cur);
        tvTimeCur = findViewById(R.id.tv_time_cur);
        tvIssueNext = findViewById(R.id.tv_issue_next);
        tvTimeNext = findViewById(R.id.tv_time_next);
        tvCountdown = findViewById(R.id.tv_countdown);
        tvCollectingTitle = findViewById(R.id.tv_collecting_title);
        tvCompletedTitle = findViewById(R.id.tv_completed_title);
        tvDebug = findViewById(R.id.tv_debug);
        etSubmitAddress = findViewById(R.id.et_submit_address);
        etProxy = findViewById(R.id.et_proxy);
        switchProxy = findViewById(R.id.switch_proxy);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        btnClearDebug = findViewById(R.id.btn_clear_debug);
        rvCollecting = findViewById(R.id.rv_collecting);
        rvCompleted = findViewById(R.id.rv_completed);
        svDebug = findViewById(R.id.sv_debug);
    }

    private void initAdapters() {
        collectingAdapter = new LotteryAdapter();
        completedAdapter = new LotteryAdapter();

        rvCollecting.setLayoutManager(new LinearLayoutManager(this));
        rvCollecting.setAdapter(collectingAdapter);

        rvCompleted.setLayoutManager(new LinearLayoutManager(this));
        rvCompleted.setAdapter(completedAdapter);
    }

    private void initDefaultConfig() {
        StringBuilder sb = new StringBuilder();
        sb.append("[wold]http://8.138.183.44/api/task/upload_twbgone?token=asdrv24n33323brf\n");
        sb.append("[wold]http://8.134.98.40/api/task/upload_twbgone?token=asdrv24n33323brf\n");
        sb.append("[w168]http://8.134.71.102/api/api/upload_result.do\n");
        sb.append("[boter]http://8.134.71.102:789/api/boter/uploadbg\n");
        etSubmitAddress.setText(sb.toString());
        etProxy.setText("127.0.0.1:7890");
    }

    private void initListeners() {
        btnStart.setOnClickListener(v -> startTask());
        btnStop.setOnClickListener(v -> stopTask());
        btnClearDebug.setOnClickListener(v -> clearDebug());
    }

    private void startTask() {
        String address = etSubmitAddress.getText().toString().trim();
        String proxy = etProxy.getText().toString().trim();
        boolean useProxy = switchProxy.isChecked();

        taskManager.setConfig(address, proxy, useProxy);
        taskManager.start();

        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        etSubmitAddress.setEnabled(false);

        startForegroundService();
    }

    private void stopTask() {
        taskManager.stop();
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        etSubmitAddress.setEnabled(true);

        stopService(new Intent(this, LotteryService.class));
    }

    private void startForegroundService() {
        Intent serviceIntent = new Intent(this, LotteryService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void clearDebug() {
        debugBuilder.clear();
        debugLineCount = 0;
        tvDebug.setText("");
    }

    // ==================== TaskCallback 实现 ====================

    @Override
    public void onCountdownUpdate(int seconds) {
        mainHandler.post(() -> {
            if (seconds >= 0) {
                int min = seconds / 60;
                int sec = seconds % 60;
                tvCountdown.setText(String.format(Locale.getDefault(), "倒计时: %02d:%02d", min, sec));
            } else {
                tvCountdown.setText("开奖中...");
            }
        });
    }

    @Override
    public void onIssueUpdate(String curIssueId, String curTime, String nextIssueId, String nextTime) {
        mainHandler.post(() -> {
            tvIssueCur.setText(curIssueId);
            tvTimeCur.setText(curTime);
            tvIssueNext.setText(nextIssueId);
            tvTimeNext.setText(nextTime);
        });
    }

    @Override
    public void onCollectingListUpdate(List<LotteryData> list) {
        mainHandler.post(() -> {
            collectingAdapter.setData(list);
            tvCollectingTitle.setText(String.format(Locale.getDefault(), "采集进行中 (%d)", list.size()));
        });
    }

    @Override
    public void onCompletedListUpdate(List<LotteryData> list) {
        mainHandler.post(() -> {
            completedAdapter.setData(list);
            tvCompletedTitle.setText(String.format(Locale.getDefault(), "采集完成 (%d)", list.size()));
        });
    }

    @Override
    public void onDebugMessage(String message, DebugLevel level) {
        mainHandler.post(() -> {
            if (debugLineCount >= MAX_DEBUG_LINES) {
                debugBuilder.clear();
                debugLineCount = 0;
            }

            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            String line = time + " " + message + "\n";
            SpannableString spanLine = new SpannableString(line);

            int color;
            switch (level) {
                case INFO:
                    color = getResources().getColor(R.color.debug_info);
                    break;
                case WARN:
                    color = getResources().getColor(R.color.debug_warn);
                    break;
                case ERROR:
                    color = getResources().getColor(R.color.debug_error);
                    break;
                default:
                    color = getResources().getColor(R.color.debug_normal);
                    break;
            }
            spanLine.setSpan(new ForegroundColorSpan(color), 0, line.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            debugBuilder.append(spanLine);
            debugLineCount++;

            tvDebug.setText(debugBuilder);
            svDebug.post(() -> svDebug.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }

    @Override
    public void onStatusChange(boolean running) {
        mainHandler.post(() -> {
            if (running) {
                tvStatus.setText("运行中");
                tvStatus.setTextColor(getResources().getColor(R.color.status_running));
            } else {
                tvStatus.setText("已停止");
                tvStatus.setTextColor(getResources().getColor(R.color.status_stopped));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (taskManager != null) {
            taskManager.stop();
        }
    }
}
