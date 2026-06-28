package com.sms.controller;

import com.sms.entity.*;
import com.sms.service.AcademicService;
import com.sms.service.AdminService;
import com.sms.service.EducationService;
import com.sms.service.UserService;
import com.sms.service.impl.AcademicServiceImpl;
import com.sms.service.impl.AdminServiceImpl;
import com.sms.service.impl.EducationServiceImpl;
import com.sms.service.impl.UserServiceImpl;
import com.sms.util.ConsoleUtil;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StudentController {

    private final AdminService adminService = new AdminServiceImpl();
    private final AcademicService academicService = new AcademicServiceImpl();
    private final EducationService educationService = new EducationServiceImpl();
    private final UserService userService = new UserServiceImpl();

    public void showMainMenu(User loggedInUser) {
        Student student = adminService.getStudentByUserId(loggedInUser.getId());
        if (student == null) {
            ConsoleUtil.printError("系统错误：未找到对应的学生档案！");
            ConsoleUtil.pause();
            return;
        }

        while (true) {
            ConsoleUtil.clearScreen();
            List<String> items = Arrays.asList(
                    "[1] 查看我的成绩单 (与 GPA 计算)",
                    "[2] 自主选课与退课 (选修/公选)",
                    "[3] 查看我的班级课表",
                    "[4] 查看我的考勤统计",
                    "[5] 提交请假申请与记录",
                    "[6] 查看系统公告栏",
                    "[7] 修改我的密码",
                    "[0] 返回登录主页"
            );
            ConsoleUtil.printMenu("学生服务终端 — 当前学生: " + student.getName() + " (" + student.getStudentNo() + ")", items);
            int choice = ConsoleUtil.readChoice("请选择操作", 7);
            if (choice == 0) break;
            switch (choice) {
                case 1: viewMyGrades(student, "2025-2026-1"); break;
                case 2: manageMyCourseSelection(student, "2025-2026-1"); break;
                case 3: viewMyTimetable(student, "2025-2026-1"); break;
                case 4: viewMyAttendance(student); break;
                case 5: manageMyLeaves(student); break;
                case 6: viewNotices(loggedInUser.getId()); break;
                case 7: changeMyPassword(loggedInUser); break;
            }
        }
    }

    // --- 1. VIEW GRADES & GPA ---
    private void viewMyGrades(Student s, String semester) {
        List<Score> scores = educationService.listScoresByStudent(s.getId(), semester);
        System.out.println("---- " + s.getName() + " 的个人成绩单 (" + semester + ") ----");
        
        List<String> headers = Arrays.asList("课程编号", "课程名称", "学分", "分数", "成绩等第", "考试类型");
        List<List<String>> rows = new ArrayList<>();
        for (Score sc : scores) {
            rows.add(Arrays.asList(
                    sc.getCourseNo(),
                    sc.getCourseName(),
                    sc.getCourseCredit() != null ? sc.getCourseCredit().toString() : "0",
                    sc.getScore() != null ? sc.getScore().toString() : "未录入",
                    sc.getGradeLevel() != null ? sc.getGradeLevel() : "暂无",
                    sc.getExamType()
            ));
        }
        ConsoleUtil.printTable(headers, rows);
        
        double gpa = educationService.calculateGPA(s.getId(), semester);
        System.out.printf("\n【学期总平均绩点 (GPA)】: %.2f / 4.0 (依据期末等第成绩计算)\n\n", gpa);
        ConsoleUtil.pause();
    }


    // --- 2. ELECTIVE SELECTION ---
    private void manageMyCourseSelection(Student s, String semester) {
        while (true) {
            ConsoleUtil.clearScreen();
            List<String> items = Arrays.asList(
                    "[1] 查看我已选的课程",
                    "[2] 查看可选的选修与公选课",
                    "[3] 选修新课程",
                    "[4] 退选已选课程",
                    "[0] 返回上级"
            );
            ConsoleUtil.printMenu("自主选课系统", items);
            int choice = ConsoleUtil.readChoice("请选择操作", 4);
            if (choice == 0) break;
            switch (choice) {
                case 1: viewMySelectedCourses(s, semester); break;
                case 2: viewAvailableElectives(s, semester); break;
                case 3: enrollElectiveCourse(s, semester); break;
                case 4: dropElectiveCourse(s, semester); break;
            }
        }
    }

    private void viewMySelectedCourses(Student s, String semester) {
        List<TeachingPlan> selected = academicService.listStudentSelectedPlans(s.getId(), semester);
        System.out.println("---- " + s.getName() + " 的已选课程列表 (" + semester + ") ----");
        if (selected.isEmpty()) {
            ConsoleUtil.printError("您本学期暂未选任何课程。");
            ConsoleUtil.pause();
            return;
        }
        List<String> headers = Arrays.asList("课程名称", "课程类型", "学分", "主讲教师", "备注");
        List<List<String>> rows = new ArrayList<>();
        for (TeachingPlan tp : selected) {
            rows.add(Arrays.asList(
                    tp.getCourseName(),
                    tp.getCourseType(),
                    tp.getCourseCredit().toString(),
                    tp.getTeacherName(),
                    tp.getClassId() == null ? "自主选修" : "班级必修/选修"
            ));
        }
        ConsoleUtil.printTable(headers, rows);
        System.out.printf("\n共已选 %d 门课程。\n", selected.size());
        ConsoleUtil.pause();
    }

    private void viewAvailableElectives(Student s, String semester) {
        List<TeachingPlan> electives = academicService.listAvailableElectivePlansForStudent(s.getId(), semester);
        System.out.println("---- 本学期可选课列表 ----");
        List<String> headers = Arrays.asList("ID", "课程名称", "课程类型", "学分", "主讲老师", "容量上限", "当前选课人数");
        List<List<String>> rows = new ArrayList<>();
        for (TeachingPlan tp : electives) {
            rows.add(Arrays.asList(
                    tp.getId().toString(),
                    tp.getCourseName(),
                    tp.getCourseType(),
                    tp.getCourseCredit().toString(),
                    tp.getTeacherName(),
                    tp.getMaxStudents().toString(),
                    tp.getCurrentStudents().toString()
            ));
        }
        ConsoleUtil.printTable(headers, rows);
        ConsoleUtil.pause();
    }

    private void enrollElectiveCourse(Student s, String semester) {
        System.out.println("---- 学生自主选课 ----");
        List<TeachingPlan> electives = academicService.listAvailableElectivePlansForStudent(s.getId(), semester);
        if (electives.isEmpty()) {
            ConsoleUtil.printError("本学期没有开设任何可选修的课程！");
            ConsoleUtil.pause();
            return;
        }

        List<String> items = new ArrayList<>();
        for (int i = 0; i < electives.size(); i++) {
            TeachingPlan tp = electives.get(i);
            items.add(String.format("[%d] %s (教师: %s) | 学分:%.1f | 人数:%d/%d",
                    i + 1, tp.getCourseName(), tp.getTeacherName(), tp.getCourseCredit(), tp.getCurrentStudents(), tp.getMaxStudents()));
        }
        items.add("[0] 返回上级");
        ConsoleUtil.printMenu("请选择要选修的课程", items);
        int choice = ConsoleUtil.readChoice("选择", electives.size());
        if (choice == 0) return;

        TeachingPlan target = electives.get(choice - 1);
        try {
            academicService.selectElective(s.getId(), target.getId());
            ConsoleUtil.printSuccess("选课成功！课程已加入您的课表及成绩单。");
        } catch (Exception e) {
            ConsoleUtil.printError("选课失败！" + e.getMessage());
        }
        ConsoleUtil.pause();
    }

    private void dropElectiveCourse(Student s, String semester) {
        System.out.println("---- 退选已选课程 ----");
        List<TeachingPlan> selected = academicService.listStudentSelectedPlans(s.getId(), semester);
        
        // Only allow dropping electives (class_id IS NULL)
        List<TeachingPlan> electives = new ArrayList<>();
        for (TeachingPlan tp : selected) {
            if (tp.getClassId() == null) electives.add(tp);
        }

        if (electives.isEmpty()) {
            ConsoleUtil.printError("您当前没有选择任何可退选的选修课程！(必修课无法退选)");
            ConsoleUtil.pause();
            return;
        }

        List<String> items = new ArrayList<>();
        for (int i = 0; i < electives.size(); i++) {
            items.add(String.format("[%d] %s (教师: %s)", i + 1, electives.get(i).getCourseName(), electives.get(i).getTeacherName()));
        }
        items.add("[0] 返回上级");
        ConsoleUtil.printMenu("请选择要退选的课程", items);
        int choice = ConsoleUtil.readChoice("选择", electives.size());
        if (choice == 0) return;

        TeachingPlan target = electives.get(choice - 1);
        if (ConsoleUtil.confirm("确认要退选课程 " + target.getCourseName() + " 吗？")) {
            try {
                academicService.deselectElective(s.getId(), target.getId());
                ConsoleUtil.printSuccess("退选成功。");
            } catch (Exception e) {
                ConsoleUtil.printError("退选失败: " + e.getMessage());
            }
        }
        ConsoleUtil.pause();
    }


    // --- 3. TIMETABLE ---
    private void viewMyTimetable(Student s, String semester) {
        List<Schedule> list = academicService.listSchedulesByStudent(s.getId(), semester);
        if (list.isEmpty()) {
            ConsoleUtil.printError("您的课表当前为空");
            ConsoleUtil.pause();
            return;
        }

        System.out.println("---- " + s.getName() + " 的个人课表一览 (" + semester + ") ----");
        
        // Group by Day of Week for neat displaying
        String[] days = {"", "星期一 (Monday)", "星期二 (Tuesday)", "星期三 (Wednesday)", "星期四 (Thursday)", "星期五 (Friday)", "星期六 (Saturday)", "星期日 (Sunday)"};
        
        for (int d = 1; d <= 7; d++) {
            List<Schedule> dayList = new ArrayList<>();
            for (Schedule sc : list) {
                if (sc.getDayOfWeek() == d) dayList.add(sc);
            }
            if (dayList.isEmpty()) continue;

            System.out.println("\n● " + days[d]);
            for (Schedule sc : dayList) {
                System.out.printf("  [第%d-%d节] %s | 老师: %s | 教室: %s (%s)\n",
                        sc.getSectionStart(), sc.getSectionEnd(), sc.getCourseName(), sc.getTeacherName(), sc.getClassroom(), sc.getCampus());
            }
        }
        System.out.println("\n--------------------------------------------------\n");
        ConsoleUtil.pause();
    }


    // --- 4. ATTENDANCE ---
    private void viewMyAttendance(Student s) {
        List<Attendance> list = educationService.listAttendanceByStudent(s.getId());
        System.out.println("---- 我的历史考勤清单 ----");
        if (list.isEmpty()) {
            ConsoleUtil.printError("当前没有任何您的课堂考勤记录");
            ConsoleUtil.pause();
            return;
        }

        List<String> headers = Arrays.asList("考勤日期", "上课课程", "学期", "出勤状态", "老师备注");
        List<List<String>> rows = new ArrayList<>();
        int present = 0, late = 0, early = 0, absent = 0, leave = 0;
        for (Attendance a : list) {
            rows.add(Arrays.asList(
                    a.getAttendDate().toString(),
                    a.getCourseName(),
                    a.getSemester(),
                    a.getStatus(),
                    a.getRemark() != null ? a.getRemark() : ""
            ));
            switch (a.getStatus()) {
                case "出勤": present++; break;
                case "迟到": late++; break;
                case "早退": early++; break;
                case "旷课": absent++; break;
                case "请假": leave++; break;
            }
        }
        ConsoleUtil.printTable(headers, rows);
        System.out.printf("\n【我的考勤率汇总】: 出勤率: %.1f%%  (总考勤点名:%d次, 正常出勤:%d, 迟到:%d, 早退:%d, 旷课:%d, 请假:%d)\n\n",
                (double) present / list.size() * 100.0, list.size(), present, late, early, absent, leave);
        ConsoleUtil.pause();
    }


    // --- 5. LEAVES ---
    private void manageMyLeaves(Student s) {
        while (true) {
            ConsoleUtil.clearScreen();
            List<String> items = Arrays.asList(
                    "[1] 提交新的请假申请",
                    "[2] 查看我的请假历史记录",
                    "[0] 返回上级"
            );
            ConsoleUtil.printMenu("请假审批服务", items);
            int choice = ConsoleUtil.readChoice("请选择操作", 2);
            if (choice == 0) break;
            switch (choice) {
                case 1: applyForLeave(s); break;
                case 2: viewLeaveHistory(s); break;
            }
        }
    }

    private void applyForLeave(Student s) {
        System.out.println("---- 申请请假 ----");
        java.sql.Date today = new java.sql.Date(System.currentTimeMillis());
        java.sql.Date start = ConsoleUtil.readDateOptional("请输入请假起始/指定日期", today);
        if (start == null) {
            ConsoleUtil.printError("操作已取消。");
            ConsoleUtil.pause();
            return;
        }

        int startHour = ConsoleUtil.readInt("请输入请假开始时间点 (0-23, 24小时制)", 0, 23);

        java.sql.Date end = ConsoleUtil.readDateOptional("请输入请假结束日期", start);
        if (end == null) {
            ConsoleUtil.printError("操作已取消。");
            ConsoleUtil.pause();
            return;
        }

        int endHour = ConsoleUtil.readInt("请输入请假结束时间点 (0-23, 24小时制)", 0, 23);

        if (start.after(end) || (start.equals(end) && startHour > endHour)) {
            ConsoleUtil.printError("错误：请假开始时间不能在结束时间之后！");
            ConsoleUtil.pause();
            return;
        }

        String reason = ConsoleUtil.readLine("请输入合理的请假事由", false);

        // Fetch student schedules to check which classes overlap
        List<Schedule> allSchedules = academicService.listSchedulesByStudent(s.getId(), "2025-2026-1");
        List<Schedule> affectedClasses = new ArrayList<>();

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(start);
        while (!cal.getTime().after(end)) {
            java.sql.Date checkDate = new java.sql.Date(cal.getTime().getTime());
            
            // Determine Day of Week (1=Monday, ..., 7=Sunday)
            java.util.Calendar tempCal = java.util.Calendar.getInstance();
            tempCal.setTime(checkDate);
            int calendarDay = tempCal.get(java.util.Calendar.DAY_OF_WEEK);
            int dayOfWeek = (calendarDay == java.util.Calendar.SUNDAY) ? 7 : (calendarDay - 1);

            for (Schedule sc : allSchedules) {
                if (sc.getDayOfWeek().equals(dayOfWeek)) {
                    int classStartHour = getSectionStartHour(sc.getSectionStart());
                    int classEndHour = getSectionEndHour(sc.getSectionEnd());
                    
                    boolean overlaps = false;
                    if (checkDate.toString().equals(start.toString()) && checkDate.toString().equals(end.toString())) {
                        overlaps = (classEndHour > startHour) && (classStartHour < endHour);
                    } else if (checkDate.toString().equals(start.toString())) {
                        overlaps = (classEndHour > startHour);
                    } else if (checkDate.toString().equals(end.toString())) {
                        overlaps = (classStartHour < endHour);
                    } else {
                        overlaps = true;
                    }

                    if (overlaps) {
                        // Check duplicates
                        boolean dup = false;
                        for (Schedule ac : affectedClasses) {
                            if (ac.getTeachingPlanId().equals(sc.getTeachingPlanId())) {
                                dup = true;
                                break;
                            }
                        }
                        if (!dup) affectedClasses.add(sc);
                    }
                }
            }
            cal.add(java.util.Calendar.DATE, 1);
        }

        System.out.println("\n----------------------------------");
        System.out.println("请假时间: " + start + " " + startHour + "时 至 " + end + " " + endHour + "时");
        if (affectedClasses.isEmpty()) {
            System.out.println("检测课程: 该时间段内您没有要上的课程记录。");
        } else {
            System.out.println("检测课程: 该时间段内您有以下课程需要请假：");
            for (Schedule sc : affectedClasses) {
                System.out.printf("  - %s (任课老师: %s | 时间: 周%d第%d-%d节)\n",
                        sc.getCourseName(), sc.getTeacherName(), sc.getDayOfWeek(), sc.getSectionStart(), sc.getSectionEnd());
            }
        }
        System.out.println("请假原因: " + reason);
        System.out.println("----------------------------------");

        if (ConsoleUtil.confirm("确认提交此请假申请？")) {
            LeaveRequest req = new LeaveRequest();
            req.setStudentId(s.getId());
            req.setTeachingPlanId(null); // Range-based leave with hour bounds
            req.setStartDate(start);
            req.setStartHour(startHour);
            req.setEndDate(end);
            req.setEndHour(endHour);
            req.setReason(reason);

            try {
                educationService.applyLeave(req);
                ConsoleUtil.printSuccess("请假申请已提交，请等待您的班主任老师审批！");
            } catch (Exception e) {
                ConsoleUtil.printError("申请失败: " + e.getMessage());
            }
        }
        ConsoleUtil.pause();
    }

    private void viewLeaveHistory(Student s) {
        List<LeaveRequest> list = educationService.listLeavesByStudent(s.getId());
        System.out.println("---- 历史请假申请记录 ----");
        if (list.isEmpty()) {
            ConsoleUtil.printError("您暂无请假申请历史");
            ConsoleUtil.pause();
            return;
        }

        List<String> headers = Arrays.asList("起始时间", "结束时间", "请假原因", "审批状态", "审批人", "审批意见");
        List<List<String>> rows = new ArrayList<>();
        for (LeaveRequest lr : list) {
            rows.add(Arrays.asList(
                    lr.getStartDate().toString() + " " + lr.getStartHour() + "时",
                    lr.getEndDate().toString() + " " + lr.getEndHour() + "时",
                    lr.getReason(),
                    lr.getStatus(),
                    lr.getApproverName() != null ? lr.getApproverName() : "无",
                    lr.getRemark() != null ? lr.getRemark() : "等待反馈"
            ));
        }
        ConsoleUtil.printTable(headers, rows);
        ConsoleUtil.pause();
    }


    // --- 6. NOTICES ---
    private void viewNotices(Integer userId) {
        while (true) {
            ConsoleUtil.clearScreen();
            List<Notice> notices = educationService.listNoticesForUser(userId);
            System.out.println("---- 公告通知栏 ----");
            if (notices.isEmpty()) {
                System.out.println("【暂无最新公告发布】");
                ConsoleUtil.pause();
                break;
            }

            List<String> items = new ArrayList<>();
            for (int i = 0; i < notices.size(); i++) {
                Notice n = notices.get(i);
                String tag = n.getIsTop() == 1 ? "【置顶】" : "";
                items.add(String.format("[%d] %s%s (发布人: %s)", i + 1, tag, n.getTitle(), n.getPublisherName() != null ? n.getPublisherName() : "管理员"));
            }
            ConsoleUtil.printMenu("点击阅读公告详情，或按 0 返回", items);
            int choice = ConsoleUtil.readChoice("选择阅读", notices.size());
            if (choice == 0) break;

            Notice selected = notices.get(choice - 1);
            ConsoleUtil.clearScreen();
            System.out.println("==================================================");
            System.out.println("标题: " + selected.getTitle());
            System.out.println("发布时间: " + selected.getCreatedAt());
            System.out.println("发布人: " + (selected.getPublisherName() != null ? selected.getPublisherName() : "系统"));
            System.out.println("--------------------------------------------------");
            System.out.println(selected.getContent());
            System.out.println("==================================================");
            ConsoleUtil.pause();
        }
    }

    private void changeMyPassword(User loggedInUser) {
        System.out.println("---- 修改我的密码 ----");
        String oldPwd = ConsoleUtil.readPassword("请输入原密码");
        String newPwd = ConsoleUtil.readLine("请输入新密码 (回车取消)", false);
        
        if (newPwd.trim().isEmpty()) {
            ConsoleUtil.printError("密码不能为空，操作已取消。");
            ConsoleUtil.pause();
            return;
        }
        
        try {
            userService.changePassword(loggedInUser.getId(), oldPwd, newPwd);
            ConsoleUtil.printSuccess("密码修改成功！下次登录请使用新密码。");
        } catch (Exception e) {
            ConsoleUtil.printError("修改失败: " + e.getMessage());
        }
        ConsoleUtil.pause();
    }

    private static int getSectionStartHour(int section) {
        switch (section) {
            case 1: return 8;
            case 2: return 9;
            case 3: return 10;
            case 4: return 11;
            case 5: return 14;
            case 6: return 15;
            case 7: return 16;
            case 8: return 17;
            case 9: return 19;
            case 10: return 20;
            case 11: return 21;
            case 12: return 22;
            default: return 8;
        }
    }

    private static int getSectionEndHour(int section) {
        switch (section) {
            case 1: return 9;
            case 2: return 10;
            case 3: return 11;
            case 4: return 12;
            case 5: return 15;
            case 6: return 16;
            case 7: return 17;
            case 8: return 18;
            case 9: return 20;
            case 10: return 21;
            case 11: return 22;
            case 12: return 23;
            default: return 23;
        }
    }
}
