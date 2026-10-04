package me.siowu.OplusKeyHook.hooks;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class LauncherHook {
    public static final String LAUNCHER_PACKAGE = "com.android.launcher";
    public static final String ACTION_EXECUTE_SHELL = "me.siowu.OplusKeyHook.TRIGGER";
    private static final String EXECUTE_PERMISSION = "me.siowu.OplusKeyHook.permission.EXECUTE_SHELL";
    private static boolean registered;

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!LAUNCHER_PACKAGE.equals(lpparam.packageName)
                || !LAUNCHER_PACKAGE.equals(lpparam.processName)) {
            return;
        }
        // Application.attach runs for OEM subclasses before their onCreate.
        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (registered) return;
                try {
                    registerReceiver((Application) param.thisObject);
                    registered = true;
                } catch (Throwable error) {
                    XposedBridge.log(error);
                }
            }
        });
    }

    private void registerReceiver(Context context) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (!ACTION_EXECUTE_SHELL.equals(intent.getAction())) return;
                String command = intent.getStringExtra("cmd");
                if (command == null || command.trim().isEmpty()) return;
                new Thread(() -> executeShell(command), "OplusKeyHook-shell").start();
            }
        };
        IntentFilter filter = new IntentFilter(ACTION_EXECUTE_SHELL);
        // Both system_server and the module may send; other apps must hold the
        // module's signature permission. Never export a raw root executor.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, EXECUTE_PERMISSION, null, Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(receiver, filter, EXECUTE_PERMISSION, null);
        }
        XposedBridge.log("OplusKeyHook: launcher shell receiver registered");
    }

    private void executeShell(String command) {
        Process process = null;
        try {
            process = new ProcessBuilder("su", "-c", command).redirectErrorStream(true).start();
            // Drain stdout and stderr together so neither pipe can deadlock su.
            try (BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = output.readLine()) != null) XposedBridge.log("OplusKeyHook shell: " + line);
            }
            XposedBridge.log("OplusKeyHook shell exit=" + process.waitFor());
        } catch (Exception error) {
            XposedBridge.log(error);
        } finally {
            if (process != null) process.destroy();
        }
    }
}
