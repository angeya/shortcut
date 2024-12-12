package org.example;


import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * 通过中间文件修改剪切板实现复制粘贴
 * @Author: angeya
 * @Date: 2024/6/11 09:59
 * @Description:
 */
public class CvByMiddleFileApp implements NativeKeyListener {

    /**
     * 被按下的按键集合
     */
    private static final Set<String> PRESSED_KEY_CODE_SET = new HashSet<>();

    /**
     * 触发复制的按键集合
     */
    private static final Set<String> COPY_PRESSED_KEY_CODE_SET = new HashSet<>();

    /**
     * cv文件
     */
    private static String middleFilePath;

    /**
     * 复制文本到剪切板脚本路径
     */
    private static final String SCRIPT_EXE_PATH = "fileToClipboard.exe";

    /**
     * 进程构建器
     */
    private static final ProcessBuilder SCRIPT_EXE_BUILDER;

    /**
     * JDK标准日志记录器
     */
    private static final java.util.logging.Logger JDK_LOG = java.util.logging.Logger.getLogger(CvByMiddleFileApp.class.getName());
    
    private static final Logger LOG = LoggerFactory.getLogger(CvByMiddleFileApp.class);

    /**
     * 创建 Robot 对象
     */
    private static Robot robot;

    static {
        // 添加快捷键
        COPY_PRESSED_KEY_CODE_SET.add("ALT");
        COPY_PRESSED_KEY_CODE_SET.add("Z");

        try {
            robot = new Robot();
        } catch (Exception e) {
            LOG.error("create windows robot error", e);
            // 异常不退出方便查看日志
        }
        initFile();
        SCRIPT_EXE_BUILDER = new ProcessBuilder(SCRIPT_EXE_PATH, middleFilePath);
    }

    /**
     * 初始化中间文件
     */
    private static void initFile() {
        String userHomePath = System.getProperty("user.home");
        Path filePath = Paths.get(userHomePath, ".cv");
        if (!Files.exists(filePath)) {
            LOG.info("cv file does not exists, create now...");
            try {
                Files.createFile(filePath);
            } catch (IOException e) {
                LOG.error("create cv file error", e);
            }
        } else {
            LOG.info("cv file exists!");
        }
        middleFilePath = filePath.toFile().getAbsolutePath();

    }

    /**
     * 按鍵按下
     */
    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        String keyCode = NativeKeyEvent.getKeyText(e.getKeyCode());
        PRESSED_KEY_CODE_SET.add(keyCode.toUpperCase());
    }

    /**
     * 按键释放
     */
    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        String keyCode = NativeKeyEvent.getKeyText(e.getKeyCode());
        if (this.matchShortcut(COPY_PRESSED_KEY_CODE_SET)) {
            LOG.info("match shortcut: {}",  COPY_PRESSED_KEY_CODE_SET);
            copy();
        }
        LOG.info("release key: {}", keyCode);
        // 释放的时候直接清空按键集合，避免有些按键释放没有被监听到，导致快捷键失效
        PRESSED_KEY_CODE_SET.clear();
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
        // 按键完成下压和释放后触发
    }

    /**
     * 匹配快捷键
     *
     * @param shortcutKeyCodeSet 快捷键集合
     * @return 是否匹配
     */
    private boolean matchShortcut(Set<String> shortcutKeyCodeSet) {
        // 按键元素个数需要相等，多了也不行
        return PRESSED_KEY_CODE_SET.size() == shortcutKeyCodeSet.size() && PRESSED_KEY_CODE_SET.containsAll(shortcutKeyCodeSet);
    }

    /**
     * 复制
     */
    private void copy() {
        try {
            copyAndSaveToFile();
            readFileAndSetToClipboard();
        } catch (Exception e) {
            LOG.error("copy error", e);
        }
    }

    /**
     * 复制并保存到文件
     * @throws Exception
     */
    private void copyAndSaveToFile() throws Exception {
        // 复制
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_C);
        robot.keyRelease(KeyEvent.VK_C);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        Thread.sleep(200);

        // 获取剪切板内容
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        String pastedText = (String) contents.getTransferData(DataFlavor.stringFlavor);

        // 保存到文件 UTF8编码 （如果文件不存在则创建，截断现有内容）
        Files.write(Paths.get(middleFilePath), pastedText.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * 读取文件内容并设置到剪切板
     */
    private void readFileAndSetToClipboard() throws Exception {
        // 当前功能java实现不了，需要通过python脚本来实现（后期可以尝试全部使用python脚本）
        // ClipboardOwner CLIPBOARD_OWNER = (clipboard, contents) -> LOG.info("剪切板数据丢失");
        // Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        // String text = new String(Files.readAllBytes(Paths.get("D:\\Program Files\\Notepad++\\cv.txt")), "UTF-8");
        // LOG.info(text);
        // StringSelection stringSelection = new StringSelection(text);
        // clipboard.setContents(stringSelection, CLIPBOARD_OWNER);

        // 调用python脚本
        Process process = SCRIPT_EXE_BUILDER.start();
        // 获取进程的输入流（标准输出）
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            LOG.info(line);
        }

        // 等待进程结束并获取退出码
        int exitCode = process.waitFor();
        String execResult = exitCode == 0 ? "success !" : "failed !";
        process.destroy();
        LOG.info("exec script exit code is: {}, {}", exitCode, execResult);
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            LOG.info("use custom shortcut key: [{}]", String.join(" + ", args));
            // 清空快捷键并替换为自定义的(快捷键用大写)
            COPY_PRESSED_KEY_CODE_SET.clear();
            for (String key : args) {
                COPY_PRESSED_KEY_CODE_SET.add(key.toUpperCase());
            }
        }

        try {
            GlobalScreen.registerNativeHook();
        } catch (Exception e) {
            LOG.error("Failed to register native hook", e);
            System.exit(1);
        }

        // 调整框架默认的jdk日志，否则会一直在刷屏
        JDK_LOG.setLevel(Level.INFO);

        CvByMiddleFileApp demo = new CvByMiddleFileApp();
        GlobalScreen.addNativeKeyListener(demo);

        LOG.info("start listening...");
    }
}


/**
 * python 脚本如下（需要 pip install pywin32）：
 */
/*
import win32clipboard
import sys

def set_clipboard_content():

    text_file_path = sys.argv[1]
    print('text file path is: ' + text_file_path)

    # 打开剪贴板
    win32clipboard.OpenClipboard()
    try:
        content = read_file(text_file_path)
        # 清空剪贴板（没有这一步不行）
        win32clipboard.EmptyClipboard()
        # 设置剪贴板内容
        win32clipboard.SetClipboardText(content)
        print("Clipboard content set to:", content)
    finally:
        # 关闭剪贴板
        win32clipboard.CloseClipboard()

# 读取文件内容
def read_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as file:
        content = file.read()
    return content


if __name__ == "__main__":
    set_clipboard_content()



 */
