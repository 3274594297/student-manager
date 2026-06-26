package com.sms;

import com.sms.controller.AdminController;
import com.sms.controller.StudentController;
import com.sms.controller.TeacherController;
import com.sms.entity.User;
import com.sms.exception.AuthException;
import com.sms.exception.BusinessException;
import com.sms.service.UserService;
import com.sms.service.impl.UserServiceImpl;
import com.sms.util.ConsoleUtil;
import com.sms.util.SimpleConnectionPool;

import java.util.Arrays;
import java.util.List;

public class App {

    private static final UserService userService = new UserServiceImpl();
    private static final AdminController adminController = new AdminController();
    private static final TeacherController teacherController = new TeacherController();
    private static final StudentController studentController = new StudentController();

    public static void main(String[] args) {
        System.out.println("正在启动智慧学生管理系统...");
        
        // Trigger pool initialization
        try {
            SimpleConnectionPool.getInstance();
        } catch (Exception e) {
            System.err.println("数据库连接池启动失败，请检查数据库配置及 MySQL 服务状态！");
            e.printStackTrace();
            return;
        }

        while (true) {
            ConsoleUtil.clearScreen();
            ConsoleUtil.printHeader(" 智慧学生管理系统 v1.0 ");
            System.out.println("  [1] 用户账户登录");
            System.out.println("  [0] 关闭并退出系统");
            System.out.println("--------------------------------------------------");
            int choice = ConsoleUtil.readChoice("请选择", 1);
            if (choice == 0) {
                if (ConsoleUtil.confirm("确认退出系统？")) {
                    break;
                } else {
                    continue;
                }
            }

            // Login Loop
            runLoginProcess();
        }

        System.out.println("正在关闭数据库连接池...");
        SimpleConnectionPool.getInstance().shutdown();
        System.out.println("感谢使用，系统已退出！");
    }

    private static void runLoginProcess() {
        while (true) {
            ConsoleUtil.clearScreen();
            System.out.println("==================================================");
            System.out.println("                 用户登录界面                     ");
            System.out.println("==================================================");

            String username = ConsoleUtil.readLine("请输入用户名 (或输入 0 返回)", false);
            if (username.equals("0")) {
                return;
            }

            boolean firstTime = true;
            while (true) {
                if (!firstTime) {
                    ConsoleUtil.clearScreen();
                    System.out.println("==================================================");
                    System.out.println("                 用户登录界面                     ");
                    System.out.println("==================================================");
                    System.out.println("请输入用户名 (或输入 0 返回) > " + username);
                }
                firstTime = false;

                String password = ConsoleUtil.readPassword("请输入密码");
                if (password.equals("0")) {
                    return;
                }

                try {
                    System.out.println("验证中，请稍候...");
                    User loggedIn = userService.login(username, password);
                    ConsoleUtil.printSuccess("登录成功！欢迎回来，" + loggedIn.getRealName());
                    ConsoleUtil.pause();

                    // Route by role
                    switch (loggedIn.getRole()) {
                        case ADMIN:
                            adminController.showMainMenu(loggedIn);
                            break;
                        case TEACHER:
                            teacherController.showMainMenu(loggedIn);
                            break;
                        case STUDENT:
                            studentController.showMainMenu(loggedIn);
                            break;
                        default:
                            ConsoleUtil.printError("未知角色权限，登录取消。");
                            ConsoleUtil.pause();
                    }
                    return;
                } catch (AuthException e) {
                    ConsoleUtil.printError("登录失败: " + e.getMessage());
                    ConsoleUtil.pause();
                    if (e.getMessage().contains("锁定")) {
                        break;
                    }
                } catch (BusinessException e) {
                    ConsoleUtil.printError("业务逻辑异常: " + e.getMessage());
                    ConsoleUtil.pause();
                    break;
                } catch (Exception e) {
                    ConsoleUtil.printError("发生未知系统错误: " + e.getMessage());
                    e.printStackTrace();
                    ConsoleUtil.pause();
                    break;
                }
            }
        }
    }
}
