package me.siowu.OplusKeyHook;

import android.app.Dialog;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textview.MaterialTextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import me.siowu.OplusKeyHook.utils.SPUtils;

public class MainActivity extends AppCompatActivity {

    private static final String TYPE_NONE = "none";
    private static final String TYPE_COMMON = "common";
    private static final String TYPE_OPEN_APP = "open_app";
    private static final String TYPE_XIAOBU_SHORTCUT = "xiaobu_shortcut";
    private static final String TYPE_CUSTOM_ACTIVITY = "custom_activity";
    private static final String TYPE_CUSTOM_URL_SCHEME = "custom_url_scheme";
    private static final String TYPE_CUSTOM_SHELL = "custom_shell";

    private static final String[] ACTION_TYPE_ICONS = { "❌", "⚡", "📱", "🎯", "🎭", "🔗", "💻" };

    // Gesture selector (issue 5: plain cards, no ButtonToggleGroup)
    private MaterialCardView cardGestureSingle, cardGestureDouble, cardGestureLong;
    private MaterialTextView textGestureSingle, textGestureDouble, textGestureLong;
    private int selectedGestureIndex = 0;

    // Action type carousel
    private RecyclerView recyclerActionType;
    private ActionTypeAdapter actionTypeAdapter;
    private int selectedTypeIndex = 0;

    // Common actions grid
    private GridLayout gridCommonActions;
    private int selectedCommonIndex = 0;
    private final List<MaterialCardView> commonActionCards = new ArrayList<>();

    // App / activity selectors
    private MaterialCardView cardSelectApp, cardSelectActivity;
    private MaterialCardView rowVibrate, rowScreenOff;
    private ImageView imageSelectApp;
    private MaterialTextView textSelectAppLabel, textSelectAppDetail;
    private View viewSelectActivityIcon;
    private MaterialTextView textSelectActivityLabel, textSelectActivityDetail;

    // Text inputs
    private EditText editUrlScheme, editxiaobuShortcuts, editShell;

    // Layout sections
    private LinearLayout layoutCommon, layoutCustomActivity, layoutActivitySelection;
    private LinearLayout layoutUrlScheme, layoutxiaobuShortcuts, layoutShell;

    // Switches + FAB
    private FloatingActionButton btnSave;
    private MaterialSwitch switchVibrate, switchScreenOff;

    // App / activity data
    private final List<AppOption> appOptions = new ArrayList<>();
    private final List<AppOption> filteredAppOptions = new ArrayList<>();
    private final List<ActivityOption> activityOptions = new ArrayList<>();
    private final List<ActivityOption> filteredActivityOptions = new ArrayList<>();

    private String selectedPackageName = "";
    private String selectedActivityName = "";
    private String loadedActivityPackageName = "";

    // Picker dialogs
    private Dialog appPickerDialog, activityPickerDialog;
    private AppListAdapter appListAdapter;
    private ActivityListAdapter activityListAdapter;
    private EditText editAppSearch, editActivitySearch;
    private ChipGroup chipGroupFilter;
    private ListView listViewApps, listViewActivities;
    private SwipeRefreshLayout swipeRefreshApps, swipeRefreshActivities;
    private TextView textAppPickerStatus, textActivityPickerStatus;

    // Auto-save (issue 4)
    private final Handler autoSaveHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoSaveRunnable = () -> {
        hasPendingAutoSave = false;
        saveConfig();
    };
    private boolean hasPendingAutoSave = false;
    private boolean isSyncingConfig = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            SPUtils.init(this);
        } catch (SecurityException e) {
            runOnUiThread(() -> Toast.makeText(
                    MainActivity.this,
                    R.string.message_activate_module_first,
                    Toast.LENGTH_LONG
            ).show());
        }

        cardGestureSingle = findViewById(R.id.cardGestureSingle);
        cardGestureDouble = findViewById(R.id.cardGestureDouble);
        cardGestureLong = findViewById(R.id.cardGestureLong);
        textGestureSingle = findViewById(R.id.textGestureSingle);
        textGestureDouble = findViewById(R.id.textGestureDouble);
        textGestureLong = findViewById(R.id.textGestureLong);

        recyclerActionType = findViewById(R.id.recyclerActionType);
        gridCommonActions = findViewById(R.id.gridCommonActions);
        cardSelectApp = findViewById(R.id.cardSelectApp);
        cardSelectActivity = findViewById(R.id.cardSelectActivity);
        rowVibrate = findViewById(R.id.rowVibrate);
        rowScreenOff = findViewById(R.id.rowScreenOff);
        imageSelectApp = findViewById(R.id.imageSelectApp);
        textSelectAppLabel = findViewById(R.id.textSelectAppLabel);
        textSelectAppDetail = findViewById(R.id.textSelectAppDetail);
        viewSelectActivityIcon = findViewById(R.id.viewSelectActivityIcon);
        textSelectActivityLabel = findViewById(R.id.textSelectActivityLabel);
        textSelectActivityDetail = findViewById(R.id.textSelectActivityDetail);
        editUrlScheme = findViewById(R.id.editUrlScheme);
        editxiaobuShortcuts = findViewById(R.id.editxiaobuShortcuts);
        editShell = findViewById(R.id.editShell);
        layoutCommon = findViewById(R.id.layoutCommon);
        layoutCustomActivity = findViewById(R.id.layoutCustomActivity);
        layoutActivitySelection = findViewById(R.id.layoutActivitySelection);
        layoutUrlScheme = findViewById(R.id.layoutUrlScheme);
        layoutxiaobuShortcuts = findViewById(R.id.layoutxiaobuShortcuts);
        layoutShell = findViewById(R.id.layoutShell);
        switchVibrate = findViewById(R.id.switchVibrate);
        switchScreenOff = findViewById(R.id.switchScreenOff);
        btnSave = findViewById(R.id.btnSave);

        setupGestureSelector();
        setupActionTypeCarousel();
        setupCommonActionsGrid();
        setupSelectionButtons();

        // Auto-save on text input changes
        TextWatcher autoSaveWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { scheduleAutoSave(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        editUrlScheme.addTextChangedListener(autoSaveWatcher);
        editxiaobuShortcuts.addTextChangedListener(autoSaveWatcher);
        editShell.addTextChangedListener(autoSaveWatcher);

        // Auto-save on switch toggles
        switchVibrate.setOnCheckedChangeListener((btn, checked) -> scheduleAutoSave());
        switchScreenOff.setOnCheckedChangeListener((btn, checked) -> scheduleAutoSave());

        // FAB = save immediately
        btnSave.setOnClickListener(v -> {
            autoSaveHandler.removeCallbacks(autoSaveRunnable);
            hasPendingAutoSave = false;
            saveConfig();
        });

        loadInstalledApps(false, null);
        loadGestureConfig(0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (hasPendingAutoSave) {
            autoSaveHandler.removeCallbacks(autoSaveRunnable);
            hasPendingAutoSave = false;
            saveConfig();
        }
    }

    // ── Gesture selector (issue 5) ──────────────────────────────────────────

    private void setupGestureSelector() {
        String[] labels = getResources().getStringArray(R.array.gesture_options);
        textGestureSingle.setText(labels[0]);
        textGestureDouble.setText(labels[1]);
        textGestureLong.setText(labels[2]);

        View.OnClickListener click = v -> {
            int idx = (v == cardGestureDouble) ? 1 : (v == cardGestureLong) ? 2 : 0;
            loadGestureConfig(idx);
        };
        cardGestureSingle.setOnClickListener(click);
        cardGestureDouble.setOnClickListener(click);
        cardGestureLong.setOnClickListener(click);

        updateGestureSelector(0);
    }

    private void updateGestureSelector(int index) {
        selectedGestureIndex = index;
        int primary = resolveColor(com.google.android.material.R.attr.colorPrimaryContainer);
        int surface = resolveColor(com.google.android.material.R.attr.colorSurfaceContainerHigh);
        cardGestureSingle.setCardBackgroundColor(index == 0 ? primary : surface);
        cardGestureDouble.setCardBackgroundColor(index == 1 ? primary : surface);
        cardGestureLong.setCardBackgroundColor(index == 2 ? primary : surface);
    }

    // ── Action type carousel ────────────────────────────────────────────────

    private void setupActionTypeCarousel() {
        String[] typeLabels = getResources().getStringArray(R.array.type_options);
        actionTypeAdapter = new ActionTypeAdapter(typeLabels);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerActionType.setLayoutManager(layoutManager);
        recyclerActionType.setAdapter(actionTypeAdapter);

        LinearSnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(recyclerActionType);

        recyclerActionType.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    View snapView = snapHelper.findSnapView(layoutManager);
                    if (snapView != null) {
                        int pos = layoutManager.getPosition(snapView);
                        if (pos != selectedTypeIndex) {
                            selectedTypeIndex = pos;
                            actionTypeAdapter.notifyDataSetChanged();
                            updateLayout(selectedTypeIndex);
                            scheduleAutoSave();
                        }
                    }
                }
            }
        });

        recyclerActionType.getViewTreeObserver().addOnGlobalLayoutListener(
                new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        recyclerActionType.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        int halfWidth = recyclerActionType.getWidth() / 2;
                        int padding = Math.max(0, halfWidth - dpToPx(60));
                        recyclerActionType.setPaddingRelative(padding, 0, padding, 0);
                        recyclerActionType.scrollToPosition(selectedTypeIndex);
                    }
                });
    }

    // Issue 3: slower, smoother carousel scroll
    private void scrollCarouselSmooth(int pos) {
        LinearSmoothScroller scroller = new LinearSmoothScroller(this) {
            @Override
            protected float calculateSpeedPerPixel(android.util.DisplayMetrics dm) {
                return 80f / dm.densityDpi;
            }
            @Override
            protected int calculateTimeForScrolling(int dx) {
                return Math.min(500, super.calculateTimeForScrolling(dx));
            }
        };
        scroller.setTargetPosition(pos);
        RecyclerView.LayoutManager lm = recyclerActionType.getLayoutManager();
        if (lm != null) lm.startSmoothScroll(scroller);
    }

    private void scrollCarouselToPosition(int pos) {
        recyclerActionType.post(() -> recyclerActionType.scrollToPosition(pos));
    }

    // ── Common actions grid ─────────────────────────────────────────────────

    private void setupCommonActionsGrid() {
        String[] options = getResources().getStringArray(R.array.common_action_options);
        gridCommonActions.removeAllViews();
        commonActionCards.clear();

        for (int i = 0; i < options.length; i++) {
            final int index = i;
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_action_type, gridCommonActions, false);
            MaterialCardView card = (MaterialCardView) itemView;

            card.findViewById(R.id.textActionTypeIcon).setVisibility(View.GONE);
            ((MaterialTextView) card.findViewById(R.id.textActionTypeLabel)).setText(options[i]);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f)
            );
            params.width = 0;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
            card.setLayoutParams(params);

            card.setOnClickListener(v -> {
                selectedCommonIndex = index;
                updateCommonActionsGrid();
                scheduleAutoSave();
            });

            commonActionCards.add(card);
            gridCommonActions.addView(card);
        }
        updateCommonActionsGrid();
    }

    private void updateCommonActionsGrid() {
        int primary = resolveColor(com.google.android.material.R.attr.colorPrimaryContainer);
        int surface = resolveColor(com.google.android.material.R.attr.colorSurfaceContainerHigh);
        for (int i = 0; i < commonActionCards.size(); i++) {
            commonActionCards.get(i).setCardBackgroundColor(i == selectedCommonIndex ? primary : surface);
        }
    }

    // ── Selection buttons / behavior rows ───────────────────────────────────

    private void setupSelectionButtons() {
        cardSelectApp.setOnClickListener(v -> showAppPickerDialog());
        cardSelectActivity.setOnClickListener(v -> {
            if (TextUtils.isEmpty(selectedPackageName)) {
                Toast.makeText(this, R.string.prompt_select_app, Toast.LENGTH_SHORT).show();
                return;
            }
            showActivityPickerDialog();
        });
        rowVibrate.setOnClickListener(v -> switchVibrate.toggle());
        rowScreenOff.setOnClickListener(v -> switchScreenOff.toggle());
        updateSelectionViews();
    }

    // ── Config load / save ──────────────────────────────────────────────────

    private void loadGestureConfig(int gesture) {
        isSyncingConfig = true;
        try {
            String prefix = getPrefix(gesture);
            updateGestureSelector(gesture);

            selectedTypeIndex = getTypeIndex(SPUtils.getString(prefix + "type", TYPE_NONE));
            selectedCommonIndex = SPUtils.getInt(prefix + "common_index", 0);
            selectedPackageName = SPUtils.getString(prefix + "package", "");
            selectedActivityName = SPUtils.getString(prefix + "activity", "");
            editUrlScheme.setText(SPUtils.getString(prefix + "url", ""));
            editxiaobuShortcuts.setText(SPUtils.getString(prefix + "xiaobu_shortcuts", ""));
            editShell.setText(SPUtils.getString(prefix + "shell", ""));
            switchVibrate.setChecked(SPUtils.getBoolean(prefix + "vibrate", true));
            switchScreenOff.setChecked(SPUtils.getBoolean(prefix + "screen_off", true));

            loadedActivityPackageName = "";
            activityOptions.clear();
            filteredActivityOptions.clear();
            updateSelectionViews();
            updateCommonActionsGrid();
            actionTypeAdapter.notifyDataSetChanged();
            scrollCarouselToPosition(selectedTypeIndex);
            updateLayout(selectedTypeIndex);
        } finally {
            isSyncingConfig = false;
        }
    }

    // Issue 4: auto-save with 1 s debounce
    private void scheduleAutoSave() {
        if (isSyncingConfig) return;
        hasPendingAutoSave = true;
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        autoSaveHandler.postDelayed(autoSaveRunnable, 1000);
    }

    private void saveConfig() {
        String prefix = getPrefix(selectedGestureIndex);
        String type = getSelectedTypeValue();

        SPUtils.putString(prefix + "type", type);
        SPUtils.putInt(prefix + "common_index", selectedCommonIndex);
        SPUtils.putString(prefix + "package", selectedPackageName.trim());
        SPUtils.putString(prefix + "activity", selectedActivityName.trim());
        SPUtils.putString(prefix + "url", editUrlScheme.getText().toString().trim());
        SPUtils.putString(prefix + "xiaobu_shortcuts", editxiaobuShortcuts.getText().toString().trim());
        SPUtils.putString(prefix + "shell", editShell.getText().toString().trim());
        SPUtils.putBoolean(prefix + "vibrate", switchVibrate.isChecked());
        SPUtils.putBoolean(prefix + "screen_off", switchScreenOff.isChecked());

        Toast.makeText(this, R.string.message_saved, Toast.LENGTH_SHORT).show();

        if (TYPE_CUSTOM_SHELL.equals(type) && !isFinishing()) {
            if (applyRootPermission()) {
                Toast.makeText(this, R.string.message_root_granted, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.message_root_denied, Toast.LENGTH_SHORT).show();
            }
            showShellPermissionDialog();
        }
    }

    // ── App loading ─────────────────────────────────────────────────────────

    private void loadInstalledApps(boolean forceRefresh, Runnable onComplete) {
        if (!forceRefresh && !appOptions.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        updateAppPickerStatus(getString(R.string.loading_apps), true);
        new Thread(() -> {
            List<AppOption> options = new ArrayList<>();
            PackageManager pm = getPackageManager();
            Collator collator = Collator.getInstance(Locale.getDefault());

            try {
                List<ApplicationInfo> apps = getInstalledApplicationsCompat(pm);
                for (ApplicationInfo ai : apps) {
                    String label = pm.getApplicationLabel(ai).toString().trim();
                    if (label.isEmpty()) label = ai.packageName;
                    options.add(new AppOption(label, ai.packageName, ai.loadIcon(pm), isSystemApp(ai)));
                }
            } catch (Exception e) {
                Log.e("MainActivity", "loadInstalledApps", e);
            }

            options.sort((a, b) -> {
                int r = collator.compare(a.label, b.label);
                return r != 0 ? r : collator.compare(a.packageName, b.packageName);
            });

            runOnUiThread(() -> {
                appOptions.clear();
                appOptions.addAll(options);
                updateSelectionViews();
                applyAppPickerFilter();
                if (onComplete != null) onComplete.run();
            });
        }).start();
    }

    private void loadActivitiesForPackage(String packageName, boolean forceRefresh, Runnable onComplete) {
        if (TextUtils.isEmpty(packageName)) {
            loadedActivityPackageName = "";
            activityOptions.clear();
            filteredActivityOptions.clear();
            updateSelectionViews();
            applyActivityPickerFilter();
            if (onComplete != null) onComplete.run();
            return;
        }

        if (!forceRefresh && TextUtils.equals(loadedActivityPackageName, packageName)) {
            applyActivityPickerFilter();
            if (onComplete != null) onComplete.run();
            return;
        }

        updateActivityPickerStatus(getString(R.string.loading_activities), true);
        new Thread(() -> {
            List<ActivityOption> options = new ArrayList<>();
            PackageManager pm = getPackageManager();
            Collator collator = Collator.getInstance(Locale.getDefault());

            try {
                PackageInfo pi = getPackageInfoCompat(pm, packageName);
                String appLabel = pi.applicationInfo != null
                        ? pm.getApplicationLabel(pi.applicationInfo).toString().trim() : "";
                if (pi.activities != null) {
                    for (ActivityInfo ai : pi.activities) {
                        String className = normalizeClassName(ai.packageName, ai.name);
                        String detail = buildActivityDetail(ai.packageName, className);
                        String display = buildActivityLabel(ai.loadLabel(pm), appLabel, detail);
                        options.add(new ActivityOption(display, detail, className));
                    }
                }
            } catch (Exception e) {
                Log.e("MainActivity", "loadActivitiesForPackage", e);
            }

            options.sort((a, b) -> {
                int r = collator.compare(a.label, b.label);
                return r != 0 ? r : collator.compare(a.className, b.className);
            });

            runOnUiThread(() -> {
                if (!TextUtils.equals(selectedPackageName, packageName)) return;
                loadedActivityPackageName = packageName;
                activityOptions.clear();
                activityOptions.addAll(options);
                updateSelectionViews();
                applyActivityPickerFilter();
                if (onComplete != null) onComplete.run();
            });
        }).start();
    }

    private List<ApplicationInfo> getInstalledApplicationsCompat(PackageManager pm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0));
        }
        return pm.getInstalledApplications(0);
    }

    private PackageInfo getPackageInfoCompat(PackageManager pm, String pkg) throws PackageManager.NameNotFoundException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES));
        }
        return pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES);
    }

    // ── Picker dialogs ──────────────────────────────────────────────────────

    private void showAppPickerDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_app_picker, null, false);
        editAppSearch = dialogView.findViewById(R.id.editAppSearch);
        chipGroupFilter = dialogView.findViewById(R.id.chipGroupFilter);
        listViewApps = dialogView.findViewById(R.id.listViewApps);
        swipeRefreshApps = dialogView.findViewById(R.id.swipeRefreshApps);
        textAppPickerStatus = dialogView.findViewById(R.id.textAppPickerStatus);

        appListAdapter = new AppListAdapter();
        listViewApps.setAdapter(appListAdapter);
        listViewApps.setOnItemClickListener((parent, view, position, id) -> {
            AppOption option = filteredAppOptions.get(position);
            selectedPackageName = option.packageName;
            selectedActivityName = "";
            loadedActivityPackageName = "";
            activityOptions.clear();
            filteredActivityOptions.clear();
            updateSelectionViews();
            scheduleAutoSave();
            if (activityPickerDialog != null) activityPickerDialog.dismiss();
            if (appPickerDialog != null) appPickerDialog.dismiss();
            if (TYPE_CUSTOM_ACTIVITY.equals(getSelectedTypeValue())) {
                loadActivitiesForPackage(selectedPackageName, false, null);
            }
        });

        editAppSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyAppPickerFilter(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        AppOption selectedApp = findAppOption(selectedPackageName);
        if (selectedApp != null && selectedApp.systemApp) {
            ((Chip) dialogView.findViewById(R.id.chipSystemApps)).setChecked(true);
        } else {
            ((Chip) dialogView.findViewById(R.id.chipUserApps)).setChecked(true);
        }
        // Issue 1: ChipGroup change animates the list via applyAppPickerFilter's fade
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> applyAppPickerFilter());
        swipeRefreshApps.setOnRefreshListener(() -> loadInstalledApps(true, null));

        BottomSheetDialog bsd = new BottomSheetDialog(this);
        bsd.setContentView(dialogView);
        appPickerDialog = bsd;
        appPickerDialog.setOnDismissListener(d -> {
            appPickerDialog = null;
            appListAdapter = null;
            editAppSearch = null;
            chipGroupFilter = null;
            listViewApps = null;
            swipeRefreshApps = null;
            textAppPickerStatus = null;
            filteredAppOptions.clear();
        });
        appPickerDialog.show();
        loadInstalledApps(false, this::applyAppPickerFilter);
    }

    private void showActivityPickerDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_activity_picker, null, false);
        editActivitySearch = dialogView.findViewById(R.id.editActivitySearch);
        listViewActivities = dialogView.findViewById(R.id.listViewActivities);
        swipeRefreshActivities = dialogView.findViewById(R.id.swipeRefreshActivities);
        textActivityPickerStatus = dialogView.findViewById(R.id.textActivityPickerStatus);

        activityListAdapter = new ActivityListAdapter();
        listViewActivities.setAdapter(activityListAdapter);
        listViewActivities.setOnItemClickListener((parent, view, position, id) -> {
            selectedActivityName = filteredActivityOptions.get(position).className;
            updateSelectionViews();
            scheduleAutoSave();
            if (activityPickerDialog != null) activityPickerDialog.dismiss();
        });

        editActivitySearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyActivityPickerFilter(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        swipeRefreshActivities.setOnRefreshListener(() -> loadActivitiesForPackage(selectedPackageName, true, null));

        BottomSheetDialog bsd = new BottomSheetDialog(this);
        bsd.setContentView(dialogView);
        activityPickerDialog = bsd;
        activityPickerDialog.setOnDismissListener(d -> {
            activityPickerDialog = null;
            activityListAdapter = null;
            editActivitySearch = null;
            listViewActivities = null;
            swipeRefreshActivities = null;
            textActivityPickerStatus = null;
            filteredActivityOptions.clear();
        });
        activityPickerDialog.show();
        loadActivitiesForPackage(selectedPackageName, false, null);
    }

    // Issue 1: fade animation on list when chip filter changes
    private void applyAppPickerFilter() {
        if (appListAdapter == null) return;

        String query = editAppSearch == null ? "" : editAppSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        boolean showSystem = chipGroupFilter != null && chipGroupFilter.getCheckedChipId() == R.id.chipSystemApps;
        boolean launchableOnly = TYPE_OPEN_APP.equals(getSelectedTypeValue());

        filteredAppOptions.clear();
        for (AppOption opt : appOptions) {
            if (showSystem != opt.systemApp) continue;
            if (launchableOnly && !isLaunchableApp(opt)) continue;
            if (!query.isEmpty()) {
                if (!opt.label.toLowerCase(Locale.ROOT).contains(query)
                        && !opt.packageName.toLowerCase(Locale.ROOT).contains(query)) continue;
            }
            filteredAppOptions.add(opt);
        }

        String status = filteredAppOptions.isEmpty() ? getString(R.string.no_apps_found) : "";

        if (listViewApps != null) {
            listViewApps.animate().alpha(0f).setDuration(120).withEndAction(() -> {
                appListAdapter.notifyDataSetChanged();
                scrollAppPickerToSelected();
                updateAppPickerStatus(status, false);
                listViewApps.animate().alpha(1f).setDuration(180).start();
            }).start();
        } else {
            appListAdapter.notifyDataSetChanged();
            updateAppPickerStatus(status, false);
        }
    }

    private void applyActivityPickerFilter() {
        if (activityListAdapter == null) return;

        String query = editActivitySearch == null ? "" : editActivitySearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        filteredActivityOptions.clear();
        for (ActivityOption opt : activityOptions) {
            if (!query.isEmpty()) {
                if (!opt.label.toLowerCase(Locale.ROOT).contains(query)
                        && !opt.detail.toLowerCase(Locale.ROOT).contains(query)
                        && !opt.className.toLowerCase(Locale.ROOT).contains(query)) continue;
            }
            filteredActivityOptions.add(opt);
        }
        activityListAdapter.notifyDataSetChanged();
        scrollActivityPickerToSelected();
        updateActivityPickerStatus(filteredActivityOptions.isEmpty() ? getString(R.string.no_activities_found) : "", false);
    }

    // ── Selection views ─────────────────────────────────────────────────────

    private void updateSelectionViews() {
        AppOption selectedApp = findAppOption(selectedPackageName);
        if (selectedApp != null) {
            imageSelectApp.setImageDrawable(selectedApp.icon);
            imageSelectApp.setVisibility(View.VISIBLE);
            textSelectAppLabel.setText(selectedApp.label);
            textSelectAppLabel.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface));
            textSelectAppDetail.setText(selectedApp.packageName);
            textSelectAppDetail.setVisibility(View.VISIBLE);
        } else if (!TextUtils.isEmpty(selectedPackageName)) {
            imageSelectApp.setVisibility(View.GONE);
            textSelectAppLabel.setText(selectedPackageName);
            textSelectAppLabel.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
            textSelectAppDetail.setVisibility(View.GONE);
        } else {
            imageSelectApp.setVisibility(View.GONE);
            textSelectAppLabel.setText(R.string.prompt_select_app);
            textSelectAppLabel.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
            textSelectAppDetail.setVisibility(View.GONE);
        }

        ActivityOption selectedActivity = findActivityOption(selectedActivityName);
        if (selectedActivity != null) {
            viewSelectActivityIcon.setVisibility(View.VISIBLE);
            textSelectActivityLabel.setText(selectedActivity.label);
            textSelectActivityLabel.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface));
            boolean hasDetail = !TextUtils.isEmpty(selectedActivity.detail)
                    && !TextUtils.equals(selectedActivity.label, selectedActivity.detail);
            textSelectActivityDetail.setText(selectedActivity.detail);
            textSelectActivityDetail.setVisibility(hasDetail ? View.VISIBLE : View.GONE);
        } else if (!TextUtils.isEmpty(selectedActivityName)) {
            viewSelectActivityIcon.setVisibility(View.GONE);
            textSelectActivityLabel.setText(selectedActivityName);
            textSelectActivityLabel.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
            textSelectActivityDetail.setVisibility(View.GONE);
        } else {
            viewSelectActivityIcon.setVisibility(View.GONE);
            textSelectActivityLabel.setText(R.string.prompt_select_activity);
            textSelectActivityLabel.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
            textSelectActivityDetail.setVisibility(View.GONE);
        }

        cardSelectActivity.setAlpha(TextUtils.isEmpty(selectedPackageName) ? 0.5f : 1.0f);
    }

    // ── Utilities ───────────────────────────────────────────────────────────

    private AppOption findAppOption(String packageName) {
        if (TextUtils.isEmpty(packageName)) return null;
        for (AppOption o : appOptions) {
            if (TextUtils.equals(o.packageName, packageName)) return o;
        }
        return null;
    }

    private ActivityOption findActivityOption(String className) {
        if (TextUtils.isEmpty(className)) return null;
        for (ActivityOption o : activityOptions) {
            if (TextUtils.equals(o.className, className)) return o;
        }
        return null;
    }

    private boolean isSystemApp(ApplicationInfo ai) {
        return (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                || (ai.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
    }

    private boolean isLaunchableApp(AppOption option) {
        if (option.launchable != null) return option.launchable;
        option.launchable = getPackageManager().getLaunchIntentForPackage(option.packageName) != null;
        return option.launchable;
    }

    private void updateAppPickerStatus(String message, boolean loading) {
        if (textAppPickerStatus != null) {
            textAppPickerStatus.setVisibility(TextUtils.isEmpty(message) ? View.GONE : View.VISIBLE);
            textAppPickerStatus.setText(message);
        }
        if (swipeRefreshApps != null) swipeRefreshApps.setRefreshing(loading);
        if (appListAdapter != null) appListAdapter.setLoading(loading);
    }

    private void updateActivityPickerStatus(String message, boolean loading) {
        if (textActivityPickerStatus != null) {
            textActivityPickerStatus.setVisibility(TextUtils.isEmpty(message) ? View.GONE : View.VISIBLE);
            textActivityPickerStatus.setText(message);
        }
        if (swipeRefreshActivities != null) swipeRefreshActivities.setRefreshing(loading);
        if (activityListAdapter != null) activityListAdapter.setLoading(loading);
    }

    private String buildActivityLabel(CharSequence label, String appLabel, String fallback) {
        String s = label == null ? "" : label.toString().trim();
        if (!TextUtils.isEmpty(appLabel) && TextUtils.equals(s, appLabel)) s = "";
        if (s.isEmpty() || TextUtils.equals(s, fallback)) return fallback;
        return s;
    }

    private String buildActivityDetail(String packageName, String className) {
        if (TextUtils.isEmpty(className)) return "";
        String prefix = packageName + ".";
        return className.startsWith(prefix) ? "." + className.substring(prefix.length()) : className;
    }

    private void scrollAppPickerToSelected() {
        if (listViewApps == null || filteredAppOptions.isEmpty()) return;
        for (int i = 0; i < filteredAppOptions.size(); i++) {
            if (TextUtils.equals(filteredAppOptions.get(i).packageName, selectedPackageName)) {
                centerListItem(listViewApps, i);
                break;
            }
        }
    }

    private void scrollActivityPickerToSelected() {
        if (listViewActivities == null || filteredActivityOptions.isEmpty()) return;
        for (int i = 0; i < filteredActivityOptions.size(); i++) {
            if (TextUtils.equals(filteredActivityOptions.get(i).className, selectedActivityName)) {
                centerListItem(listViewActivities, i);
                break;
            }
        }
    }

    private void centerListItem(ListView lv, int position) {
        lv.post(() -> {
            int h = lv.getHeight();
            if (h <= 0) { lv.setSelection(position); return; }
            lv.setSelectionFromTop(position, Math.max(0, h / 2 - dpToPx(28)));
        });
    }

    private int resolveColor(@AttrRes int attr) {
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private String normalizeClassName(String pkg, String name) {
        if (TextUtils.isEmpty(name)) return "";
        return name.startsWith(".") ? pkg + name : name;
    }

    private String getPrefix(int gesture) {
        switch (gesture) {
            case 1: return "double_";
            case 2: return "long_";
            default: return "single_";
        }
    }

    private int getSelectedGestureIndex() {
        return selectedGestureIndex;
    }

    private int getTypeIndex(String type) {
        switch (type) {
            case TYPE_COMMON:       case "常用功能":              return 1;
            case TYPE_OPEN_APP:     case "打开应用":              return 2;
            case TYPE_XIAOBU_SHORTCUT: case "执行小布快捷指令":  return 3;
            case TYPE_CUSTOM_ACTIVITY: case "自定义Activity":     return 4;
            case TYPE_CUSTOM_URL_SCHEME: case "自定义UrlScheme":  return 5;
            case TYPE_CUSTOM_SHELL: case "自定义Shell命令":       return 6;
            default:                                               return 0;
        }
    }

    private String getSelectedTypeValue() {
        switch (selectedTypeIndex) {
            case 1: return TYPE_COMMON;
            case 2: return TYPE_OPEN_APP;
            case 3: return TYPE_XIAOBU_SHORTCUT;
            case 4: return TYPE_CUSTOM_ACTIVITY;
            case 5: return TYPE_CUSTOM_URL_SCHEME;
            case 6: return TYPE_CUSTOM_SHELL;
            default: return TYPE_NONE;
        }
    }

    private void updateLayout(int pos) {
        layoutCommon.setVisibility(View.GONE);
        layoutCustomActivity.setVisibility(View.GONE);
        layoutActivitySelection.setVisibility(View.GONE);
        layoutUrlScheme.setVisibility(View.GONE);
        layoutxiaobuShortcuts.setVisibility(View.GONE);
        layoutShell.setVisibility(View.GONE);
        switch (pos) {
            case 1: layoutCommon.setVisibility(View.VISIBLE); break;
            case 2: layoutCustomActivity.setVisibility(View.VISIBLE); break;
            case 3: layoutxiaobuShortcuts.setVisibility(View.VISIBLE); break;
            case 4: layoutCustomActivity.setVisibility(View.VISIBLE);
                    layoutActivitySelection.setVisibility(View.VISIBLE); break;
            case 5: layoutUrlScheme.setVisibility(View.VISIBLE); break;
            case 6: layoutShell.setVisibility(View.VISIBLE); break;
        }
    }

    public boolean applyRootPermission() {
        try {
            Process p = Runtime.getRuntime().exec("su -c echo root_ok");
            String result = new BufferedReader(new InputStreamReader(p.getInputStream())).readLine();
            p.destroy();
            return "root_ok".equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    private void showShellPermissionDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_title_notice)
                .setMessage(R.string.dialog_shell_permission_message)
                .setCancelable(false)
                .setNegativeButton(R.string.action_authorize, (d, w) -> gotoColorOSAutoStart())
                .setPositiveButton(R.string.action_confirm, null)
                .show();
    }

    private void gotoColorOSAutoStart() {
        try {
            Runtime.getRuntime().exec(new String[]{
                    "su", "-c",
                    "am start -n com.oplus.battery/com.oplus.startupapp.view.StartupAppListActivity"
            });
        } catch (Exception e) {
            Log.e("gotoColorOSAutoStart", e.getMessage());
        }
    }

    // ── ActionTypeAdapter ───────────────────────────────────────────────────

    private final class ActionTypeAdapter extends RecyclerView.Adapter<ActionTypeAdapter.ViewHolder> {
        private final String[] labels;

        ActionTypeAdapter(String[] labels) { this.labels = labels; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_action_type, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            boolean selected = position == selectedTypeIndex;
            holder.icon.setText(ACTION_TYPE_ICONS[position]);
            holder.label.setText(labels[position]);
            holder.card.setCardBackgroundColor(selected
                    ? resolveColor(com.google.android.material.R.attr.colorPrimaryContainer)
                    : resolveColor(com.google.android.material.R.attr.colorSurfaceContainerHigh));
            holder.card.setAlpha(selected ? 1.0f : 0.4f);
            holder.card.setScaleX(selected ? 1.0f : 0.88f);
            holder.card.setScaleY(selected ? 1.0f : 0.88f);
            // Issue 3: smooth scroll with custom speed
            holder.card.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                selectedTypeIndex = pos;
                notifyDataSetChanged();
                updateLayout(selectedTypeIndex);
                scrollCarouselSmooth(selectedTypeIndex);
                scheduleAutoSave();
            });
        }

        @Override
        public int getItemCount() { return labels.length; }

        final class ViewHolder extends RecyclerView.ViewHolder {
            final MaterialCardView card;
            final MaterialTextView icon, label;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                card = (MaterialCardView) itemView;
                icon = itemView.findViewById(R.id.textActionTypeIcon);
                label = itemView.findViewById(R.id.textActionTypeLabel);
            }
        }
    }

    // ── List adapters ───────────────────────────────────────────────────────

    private final class AppListAdapter extends BaseAdapter {
        private boolean loading;

        void setLoading(boolean v) { loading = v; notifyDataSetChanged(); }

        @Override public int getCount() { return loading ? 0 : filteredAppOptions.size(); }
        @Override public Object getItem(int i) { return filteredAppOptions.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder h;
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_app, parent, false);
                h = new ViewHolder(
                        convertView.findViewById(R.id.imageAppIcon),
                        convertView.findViewById(R.id.textAppLabel),
                        convertView.findViewById(R.id.textAppPackage));
                convertView.setTag(h);
            } else {
                h = (ViewHolder) convertView.getTag();
            }
            AppOption opt = filteredAppOptions.get(position);
            h.icon.setImageDrawable(opt.icon);
            h.icon.setContentDescription(getString(R.string.content_description_app_icon, opt.label));
            h.label.setText(opt.label);
            h.detail.setText(opt.packageName);
            return convertView;
        }
    }

    private final class ActivityListAdapter extends BaseAdapter {
        private boolean loading;

        void setLoading(boolean v) { loading = v; notifyDataSetChanged(); }

        @Override public int getCount() { return loading ? 0 : filteredActivityOptions.size(); }
        @Override public Object getItem(int i) { return filteredActivityOptions.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ActivityViewHolder h;
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_activity, parent, false);
                h = new ActivityViewHolder(
                        convertView.findViewById(R.id.textActivityLabel),
                        convertView.findViewById(R.id.textActivityClass));
                convertView.setTag(h);
            } else {
                h = (ActivityViewHolder) convertView.getTag();
            }
            ActivityOption opt = filteredActivityOptions.get(position);
            h.label.setText(opt.label);
            boolean hideDetail = TextUtils.equals(opt.label, opt.detail) || TextUtils.isEmpty(opt.detail);
            h.detail.setVisibility(hideDetail ? View.GONE : View.VISIBLE);
            if (!hideDetail) h.detail.setText(opt.detail);
            return convertView;
        }
    }

    private static final class ViewHolder {
        final ImageView icon; final TextView label, detail;
        ViewHolder(ImageView icon, TextView label, TextView detail) {
            this.icon = icon; this.label = label; this.detail = detail;
        }
    }

    private static final class ActivityViewHolder {
        final TextView label, detail;
        ActivityViewHolder(TextView label, TextView detail) { this.label = label; this.detail = detail; }
    }

    private static final class AppOption {
        final String label, packageName; final Drawable icon; final boolean systemApp; Boolean launchable;
        AppOption(String label, String packageName, Drawable icon, boolean systemApp) {
            this.label = label; this.packageName = packageName; this.icon = icon; this.systemApp = systemApp;
        }
    }

    private static final class ActivityOption {
        final String label, detail, className;
        ActivityOption(String label, String detail, String className) {
            this.label = label; this.detail = detail; this.className = className;
        }
    }
}
