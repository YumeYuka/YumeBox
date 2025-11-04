**English** |  [简体中文](README_CN.md)

## YuanShen

An automatic installation and update module for *Genshin Impact*, compatible with [Magisk](https://github.com/topjohnwu/Magisk) and [KernelSU](https://github.com/tiann/KernelSU).
This module automates the installation and update process of *Genshin Impact*, enabling quick startup and providing a convenient solution for users of Magisk or KernelSU.

---

### Usage

1. Download the latest version of the module package: [YuanShen v0.0.1](https://github.com/YumeYuka/YuanShen/releases/tag/v0.0.1)
2. Install the module using one of the following methods:

    * Manually install it through [Magisk Manager](https://github.com/topjohnwu/Magisk) or [KernelSU](https://github.com/tiann/KernelSU);
    * Or place the package into the appropriate module installation directory.
3. After installation is complete, reboot your device to apply the module.

---

### Activation Check

Run the following command:

```bash
pidof Yuanshen
```

If the command returns a process ID, it indicates that the module is running successfully.

---

### WebUI and Action Launch

* When *Genshin Impact* is launched via Magisk or KernelSU, the module will automatically run.
* If you are using a module manager (such as [ShiroSU](https://github.com/OOM-WG/ShiroSU)), you can create a WebUI shortcut to launch *Genshin Impact* directly.
