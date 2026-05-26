package com.bgupdata.lottery;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bgupdata.lottery.adapter.AddressAdapter;
import com.bgupdata.lottery.adapter.LotteryAdapter;
import com.bgupdata.lottery.model.AddressItem;
import com.bgupdata.lottery.model.DebugLevel;
import com.bgupdata.lottery.model.LotteryData;
import com.bgupdata.lottery.model.PostSiteType;
import com.bgupdata.lottery.service.LotteryService;
import com.bgupdata.lottery.service.TaskManager;
import com.bgupdata.lottery.util.LogManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TaskManager.TaskCallback {

    private TextView tvStatus, tvIssueCur, tvTimeCur, tvIssueNext, tvTimeNext, tvCountdown;
    private TextView tvCollectingTitle, tvCompletedTitle, tvFailedTitle, tvDebug;
    private EditText etProxy;
    private SwitchMaterial switchProxy;
    private MaterialButton btnStart, btnStop, btnClearDebug, btnAddAddress;
    private RecyclerView rvAddress, rvCollecting, rvCompleted, rvFailed;
    private ScrollView svDebug;

    private AddressAdapter addressAdapter;
    private LotteryAdapter collectingAdapter;
    private LotteryAdapter completedAdapter;
    private LotteryAdapter failedAdapter;

    private TaskManager taskManager;
    private LogManager logManager;
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
        initDefaultAddresses();
        initListeners();

        logManager = LogManager.getInstance(this);

        taskManager = new TaskManager();
        taskManager.setLogManager(logManager);
        taskManager.setCallback(this);

        loadTodayLogs();
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
        tvFailedTitle = findViewById(R.id.tv_failed_title);
        tvDebug = findViewById(R.id.tv_debug);
        etProxy = findViewById(R.id.et_proxy);
        switchProxy = findViewById(R.id.switch_proxy);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        btnClearDebug = findViewById(R.id.btn_clear_debug);
        btnAddAddress = findViewById(R.id.btn_add_address);
        rvAddress = findViewById(R.id.rv_address);
        rvCollecting = findViewById(R.id.rv_collecting);
        rvCompleted = findViewById(R.id.rv_completed);
        rvFailed = findViewById(R.id.rv_failed);
        svDebug = findViewById(R.id.sv_debug);
    }

    private void initAdapters() {
        addressAdapter = new AddressAdapter();
        addressAdapter.setListener(new AddressAdapter.OnItemActionListener() {
            @Override
            public void onEdit(int position, AddressItem item) {
                showAddressDialog(position, item);
            }

            @Override
            public void onDelete(int position, AddressItem item) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("确认删除")
                        .setMessage("删除: " + item.getUrl() + " ?")
                        .setPositiveButton("删除", (d, w) -> addressAdapter.removeItem(position))
                        .setNegativeButton("取消", null)
                        .show();
            }

            @Override
            public void onToggle(int position, AddressItem item, boolean enabled) {
                // 状态已在adapter内更新
            }
        });

        rvAddress.setLayoutManager(new LinearLayoutManager(this));
        rvAddress.setAdapter(addressAdapter);

        collectingAdapter = new LotteryAdapter();
        rvCollecting.setLayoutManager(new LinearLayoutManager(this));
        rvCollecting.setAdapter(collectingAdapter);

        completedAdapter = new LotteryAdapter();
        rvCompleted.setLayoutManager(new LinearLayoutManager(this));
        rvCompleted.setAdapter(completedAdapter);

        failedAdapter = new LotteryAdapter();
        rvFailed.setLayoutManager(new LinearLayoutManager(this));
        rvFailed.setAdapter(failedAdapter);
    }

    private void initDefaultAddresses() {
        List<AddressItem> defaults = new ArrayList<>();
        defaults.add(new AddressItem("http://8.138.183.44/api/task/upload_twbgone?token=asdrv24n33323brf", PostSiteType.WOLD, true));
        defaults.add(new AddressItem("http://8.134.98.40/api/task/upload_twbgone?token=asdrv24n33323brf", PostSiteType.WOLD, true));
        defaults.add(new AddressItem("http://8.134.71.102/api/api/upload_result.do", PostSiteType.W168, true));
        defaults.add(new AddressItem("http://8.134.71.102:789/api/boter/uploadbg", PostSiteType.BOTER, true));
        addressAdapter.setData(defaults);

        etProxy.setText("127.0.0.1:7890");
    }

    private void initListeners() {
        btnStart.setOnClickListener(v -> startTask());
        btnStop.setOnClickListener(v -> stopTask());
        btnClearDebug.setOnClickListener(v -> clearDebug());
        btnAddAddress.setOnClickListener(v -> showAddressDialog(-1, null));

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_main);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_about) {
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            }
            return false;
        });
    }

    /**
     * 显示添加/编辑地址对话框
     * @param position -1表示新增，>=0表示编辑
     */
    private void showAddressDialog(int position, AddressItem existingItem) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_address, null);
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        Spinner spinnerType = dialogView.findViewById(R.id.spinner_type);
        EditText etUrl = dialogView.findViewById(R.id.et_url);

        // 设置类型下拉菜单
        String[] typeNames = new String[PostSiteType.values().length];
        for (int i = 0; i < PostSiteType.values().length; i++) {
            typeNames[i] = PostSiteType.values()[i].getValue();
        }
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, typeNames);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        boolean isEdit = position >= 0 && existingItem != null;
        if (isEdit) {
            tvTitle.setText("编辑投递地址");
            etUrl.setText(existingItem.getUrl());
            for (int i = 0; i < PostSiteType.values().length; i++) {
                if (PostSiteType.values()[i] == existingItem.getSiteType()) {
                    spinnerType.setSelection(i);
                    break;
                }
            }
        } else {
            tvTitle.setText("添加投递地址");
        }

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(isEdit ? "保存" : "添加", (dialog, which) -> {
                    String url = etUrl.getText().toString().trim();
                    if (url.isEmpty()) {
                        Toast.makeText(this, "请输入网址", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int selectedIndex = spinnerType.getSelectedItemPosition();
                    PostSiteType type = PostSiteType.values()[selectedIndex];

                    if (isEdit) {
                        AddressItem updated = new AddressItem(url, type, existingItem.isEnabled());
                        addressAdapter.updateItem(position, updated);
                    } else {
                        AddressItem newItem = new AddressItem(url, type, true);
                        addressAdapter.addItem(newItem);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 从地址列表构建配置字符串(仅启用的项)
     */
    private String buildAddressConfig() {
        StringBuilder sb = new StringBuilder();
        for (AddressItem item : addressAdapter.getData()) {
            if (item.isEnabled()) {
                sb.append(item.toConfigString()).append(",");
            }
        }
        return sb.toString();
    }

    private void startTask() {
        String address = buildAddressConfig();
        String proxy = etProxy.getText().toString().trim();
        boolean useProxy = switchProxy.isChecked();

        if (address.isEmpty()) {
            Toast.makeText(this, "请至少添加一个启用的投递地址", Toast.LENGTH_SHORT).show();
            return;
        }

        taskManager.setConfig(address, proxy, useProxy);
        taskManager.start();

        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        btnAddAddress.setEnabled(false);

        startForegroundService();
    }

    private void stopTask() {
        taskManager.stop();
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        btnAddAddress.setEnabled(true);

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
        PopupMenu popup = new PopupMenu(this, btnClearDebug);
        popup.getMenu().add(0, 1, 0, "清除屏幕显示");
        popup.getMenu().add(0, 2, 1, "删除当天日志");
        popup.getMenu().add(0, 3, 2, "删除全部日志");
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    debugBuilder.clear();
                    debugLineCount = 0;
                    tvDebug.setText("");
                    return true;
                case 2:
                    logManager.deleteTodayLog();
                    debugBuilder.clear();
                    debugLineCount = 0;
                    tvDebug.setText("");
                    Toast.makeText(this, "当天日志已删除", Toast.LENGTH_SHORT).show();
                    return true;
                case 3:
                    new AlertDialog.Builder(this)
                            .setTitle("确认")
                            .setMessage("确定要删除全部日志文件吗？")
                            .setPositiveButton("删除", (d, w) -> {
                                logManager.deleteAllLogs();
                                debugBuilder.clear();
                                debugLineCount = 0;
                                tvDebug.setText("");
                                Toast.makeText(this, "全部日志已删除", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("取消", null)
                            .show();
                    return true;
            }
            return false;
        });
        popup.show();
    }

    private void loadTodayLogs() {
        new Thread(() -> {
            String logs = logManager.readTodayLogs();
            if (!logs.isEmpty()) {
                mainHandler.post(() -> {
                    String[] lines = logs.split("\n");
                    for (String line : lines) {
                        if (line.isEmpty()) continue;
                        SpannableString spanLine = new SpannableString(line + "\n");
                        int color = getResources().getColor(R.color.debug_normal);
                        if (line.contains("[INFO]")) {
                            color = getResources().getColor(R.color.debug_info);
                        } else if (line.contains("[WARN]")) {
                            color = getResources().getColor(R.color.debug_warn);
                        } else if (line.contains("[ERROR]")) {
                            color = getResources().getColor(R.color.debug_error);
                        }
                        spanLine.setSpan(new ForegroundColorSpan(color), 0, spanLine.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        debugBuilder.append(spanLine);
                        debugLineCount++;
                    }
                    tvDebug.setText(debugBuilder);
                    svDebug.post(() -> svDebug.fullScroll(ScrollView.FOCUS_DOWN));
                });
            }
        }).start();
    }

    // ==================== TaskCallback ====================

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
    public void onFailedListUpdate(List<LotteryData> list) {
        mainHandler.post(() -> {
            failedAdapter.setData(list);
            tvFailedTitle.setText(String.format(Locale.getDefault(), "采集失败 (%d)", list.size()));
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
