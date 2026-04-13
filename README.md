<div align="center">

# OplusKeyHook

[中文 README](README.zh-CN.md)

**An Xposed module for ColorOS and OxygenOS devices with shortcut keys, letting you customize what the hardware key does.**

[![GitHub release](https://img.shields.io/github/v/release/Xposed-Modules-Repo/me.siowu.OplusKeyHook?style=flat-square)](https://github.com/Xposed-Modules-Repo/me.siowu.OplusKeyHook/releases)
[![GitHub stars](https://img.shields.io/github/stars/Xposed-Modules-Repo/me.siowu.OplusKeyHook?style=flat-square&color=yellow)](https://github.com/Xposed-Modules-Repo/me.siowu.OplusKeyHook/stargazers)
<a href="https://github.com/siowu/OplusKeyHook">
        <img src="https://img.shields.io/badge/Github-OplusKeyHook-yellow.svg" alt="socialify"/>
</a>
</div>

---

This module hooks the native system key listener to intercept shortcut-key events without adding extra power consumption.

## ✨Key Features

- Separate actions for single press, double press, and long press
- One-tap launch for WeChat/Alipay payment codes and scanner pages
- Support for Breeno shortcuts, Flash Notes, Breeno Memory, and other common actions via the [Breeno shortcut ID guide (Chinese)](https://github.com/siowu/OplusKeyHook/blob/main/docs/%E8%8E%B7%E5%8F%96%E5%B0%8F%E5%B8%83%E5%BF%AB%E6%8D%B7%E6%8C%87%E4%BB%A4ID%E6%95%99%E7%A8%8B.md)
- Support for launching a custom Activity via the [custom Activity guide (Chinese)](https://github.com/siowu/OplusKeyHook/blob/main/docs/%E8%87%AA%E5%AE%9A%E4%B9%89Activity%E6%95%99%E7%A8%8B.md)
- Support for invoking a custom URL scheme via the [custom URL scheme guide (Chinese)](https://github.com/siowu/OplusKeyHook/blob/main/docs/%E8%87%AA%E5%AE%9A%E4%B9%89UrlScheme%E6%95%99%E7%A8%8B.md)
- Support for executing shell commands
- Optional vibration feedback and support for running while the screen is off, then waking the screen for unlock

## 🚀Usage

1. Install an Xposed-compatible environment and activate this module.
2. Set the module scope to **System Framework**.
3. Reboot the phone, open the module, choose the action you want, and save it to apply immediately.
   *Note: only the first activation and module updates require a reboot. Later key-function changes take effect without rebooting.*

## 🎯Roadmap

Possible follow-up improvements:

- Separate settings for single press, double press, and long press (done in v1.1)
- Support custom shell commands (done in v1.3)

## 📝Changelog

- v1.4 Added GitHub Actions APK build/release automation and split out the Chinese README
- v1.3 Added support for custom shell commands
- v1.2 Added support for Breeno shortcuts, Flash Notes, and Breeno Memory
- v1.1 Added separate single/double/long press actions

## 📄Contributing

Issues and PRs are welcome. If you want support for more apps or new features, contributions are appreciated.

When reporting a problem, please include the system version, device model, module version, reproduction steps, and key log details.

## 🛡️Disclaimer

This module is provided for learning and technical research only. Do not use it for illegal purposes. The author is not responsible for any consequences caused by using this module.
