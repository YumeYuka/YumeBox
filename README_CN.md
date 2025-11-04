**简体中文** |  [English](README.md)

## YuanShen

适用于 [Magisk](https://github.com/topjohnwu/Magisk) / [KernelSU](https://github.com/tiann/KernelSU) 的原神自动安装与更新模块
该模块可自动完成原神的安装与更新过程，快速启动，适合使用 Magisk 或 KernelSU 的用户

---

### 使用方法

1. 下载最新版本的模块压缩包：[YuanShen v0.0.1](https://github.com/YumeYuka/YuanShen/releases/tag/v0.0.1)
2. 通过以下任一方式安装模块：

    * 在 [Magisk Manager](https://github.com/topjohnwu/Magisk) 或 [KernelSU](https://github.com/tiann/KernelSU) 中手动安装模块
    * 或将压缩包放入相应的模块安装目录中；
3. 安装完成后，重启设备以使模块生效

---

### 生效检测

执行以下命令：

```bash
pidof Yuanshen
```

如果命令返回进程 ID，则表示模块已成功运行。

---

### WebUI 与动作启动

* 通过 Magisk 或 KernelSU 启动原神时，模块会自动运行；
* 若使用模块管理器（例如 [ShiroSU](https://github.com/shirosu-project)），可创建 WebUI 快捷方式启动原神