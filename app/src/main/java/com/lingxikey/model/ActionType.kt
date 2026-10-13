package com.lingxikey.custom.model

/** Actions exposed to every gesture. KEEP enum names stable: they are persisted in SharedPreferences. */
enum class ActionType(val title: String, val description: String) {
    NONE("不执行操作", "禁用此手势"),
    SHOW_RINGER("显示响铃 / 振动 / 静音状态", "显示当前的响铃模式"),
    CYCLE_RINGER("切换响铃 / 振动 / 静音", "按响铃、振动、静音顺序循环"),
    TOGGLE_DND("开启或关闭免打扰", "在开启和关闭之间切换"),
    FLASHLIGHT("切换手电筒", "打开或关闭摄像头手电筒"),
    COOLING_FAN_TOGGLE("切换散热风扇", "开启或关闭机身内置散热风扇"),
    FAN_LIGHT_TOGGLE("灯效开关", "开启或关闭机身常亮灯效（与原厂灵犀键一致）"),
    RECORDING("开始 / 结束录音", "调用 ColorOS 系统录音入口"),
    SCREENSHOT("系统截屏", "调用系统截屏服务"),
    COMMON_FUNCTION("常用功能", "微信、支付宝和 ColorOS 快捷功能"),
    XIAOBU_SHORTCUT("执行小布快捷指令", "通过小布快捷指令 ID 执行"),
    XIAOBU_SCREEN_RECOGNIZE("小布识屏", "等效长按屏幕底部手势条，唤起屏幕识别"),
    GAME_ASSISTANT("打开游戏助手", "进入游戏空间（游戏助手的入口应用）"),
    LAUNCH_APP("打开应用", "启动自行选择的已安装应用"),
    CUSTOM_ACTIVITY("自定义 Activity", "启动指定应用的 Activity"),
    CUSTOM_URL("自定义 Url Scheme", "通过系统解析器打开 Url"),
    SHELL_COMMAND("执行 Shell 指令", "在系统框架进程中执行命令"),

    // App-scoped actions: only meaningful while the matching app is in the foreground.
    // 相机只保留"单击拍照/录制"：拍照模式下拍照，录像模式下开始或暂停录像。
    CAMERA_SHUTTER("相机：拍照 / 录制", "拍照模式下拍照，录像模式下开始或暂停录像"),
    MUSIC_PLAY_PAUSE("音乐：播放 / 暂停", "向当前媒体会话发送播放暂停"),
    MUSIC_NEXT("音乐：下一曲", "向当前媒体会话发送下一曲"),
    MUSIC_PREVIOUS("音乐：上一曲", "向当前媒体会话发送上一曲"),
    VIDEO_SEEK_FORWARD("视频：快进 10 秒", "向当前媒体会话快进"),
    VIDEO_SEEK_BACKWARD("视频：后退 10 秒", "向当前媒体会话后退")
}
