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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherController {

    private final AdminService adminService = new AdminServiceImpl();
    private final AcademicService academicService = new AcademicServiceImpl();
    private final EducationService educationService = new EducationServiceImpl();
    private final UserService userService = new UserServiceImpl();

    public void showMainMenu(User loggedInUser) {
        Teacher teacher = adminService.getTeacherByUserId(loggedInUser.getId());
        if (teacher == null) {
            ConsoleUtil.printError("系统错误：未找到对应的教师档案！");
            ConsoleUtil.pause();
            return;
        }

        while (true) {
            ConsoleUtil.clearScreen();
            List<String> items = Arrays.asList(
                    "[1] 查看我的授课日程 (课表)",
                    "[2] 学生成绩录入与管理",
                    "[3] 课堂考勤点名",
                    "[4] 学生请假审批",
                    "[5] 发布公告通知",
                    "[6] 修改我的密码",
                    "[0] 返回登录主页"
            );
            ConsoleUtil.printMenu("教师服务终端 — 当前教师: " + teacher.getName() + " (" + teacher.getTitle() + ")", items);
            int choice = ConsoleUtil.readChoice("请选择操作", 6);
            if (choice == 0) break;
            switch (choice) {
                case 1: viewMySchedule(teacher, "2025-2026-1"); break;
                case 2: manageScores(teacher, "2025-2026-1"); break;
                case 3: manageAttendance(teacher, "2025-2026-1"); break;
                case 4: manageLeaves(loggedInUser); break;
                case 5: manageNotices(loggedInUser); break;
                case 6: changeMyPassword(loggedInUser); break;
            }
        }
    }

    // --- 1. SCHEDULE ---
    private void viewMySchedule(Teacher t, String semester) {
        List<Schedule> list = academicService.listSchedulesByTeacher(t.getId(), semester);
        if (list.isEmpty()) {
            ConsoleUtil.printError("您在当前学期没有排课记录");
            ConsoleUtil.pause();
            return;
        }

        System.out.println("---- " + t.getName() + " 老师个人课表 (" + semester + ") ----");
        List<String> headers = Arrays.asList("星期", "时间段", "授课科目", "授课班级", "教学楼教室", "校区");
        List<List<String>> rows = new ArrayList<>();
        String[] days = {"", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
        for (Schedule s : list) {
            rows.add(Arrays.asList(
                    days[s.getDayOfWeek()],
                    String.format("第 %d-%d 节", s.getSectionStart(), s.getSectionEnd()),
                    s.getCourseName(),
                    s.getClassName() != null ? s.getClassName() : "自由选修课",
                    s.getClassroom(),
                    s.getCampus()
            ));
        }
        ConsoleUtil.printTable(headers, rows);
        ConsoleUtil.pause();
    }


    // --- 2. SCORES ---
    private void manageScores(Teacher t, String semester) {
        while (true) {
            ConsoleUtil.clearScreen();
            List<String> items = Arrays.asList(
                    "[1] 录入/修改学生成绩 (逐个录入)",
                    "[2] 批量录入成绩 (表格模式)",
                    "[3] 查看成绩单与综合统计",
                    "[4] 导出成绩单到 CSV",
                    "[0] 返回上级"
            );
            ConsoleUtil.printMenu("学生成绩控制中心", items);
            int choice = ConsoleUtil.readChoice("请选择操作", 4);
            if (choice == 0) break;

            Integer planId = selectMyTeachingPlan(t, semester);
            if (planId == null) continue;

            switch (choice) {
                case 1: recordScoreSingle(planId); break;
                case 2: recordScoreBatch(planId); break;
                case 3: viewScoreStats(planId); break;
                case 4: exportScoreCSV(planId); break;
            }
        }
    }

    private void recordScoreSingle(Integer planId) {
        System.out.println("---- 成绩单独录入 ----");
        List<Score> scores = educationService.listScoresByPlan(planId);
        if (scores.isEmpty()) {
            ConsoleUtil.printError("当前课程没有已选课的学生");
            ConsoleUtil.pause();
            return;
        }

        List<String> items = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            Score s = scores.get(i);
            items.add(String.format("[%d] %s (%s) | 当前成绩: %s", i + 1, s.getStudentName(), s.getStudentNo(), s.getScore() != null ? s.getScore() : "未录入"));
        }
        ConsoleUtil.printMenu("请选择要录入的学生", items);
        int choice = ConsoleUtil.readChoice("选择", scores.size());
        if (choice == 0) return;

        Score target = scores.get(choice - 1);
        double scoreVal = ConsoleUtil.readDouble("请输入学生 " + target.getStudentName() + " 的分数 (0 - 100)", 0.0, 100.0);
        target.setScore(scoreVal);
        target.setExamType("期末");

        try {
            educationService.recordScore(target);
            ConsoleUtil.printSuccess("成绩已成功录入/更新");
        } catch (Exception e) {
            ConsoleUtil.printError("录入失败: " + e.getMessage());
        }
        ConsoleUtil.pause();
    }

    private void recordScoreBatch(Integer planId) {
        System.out.println("---- 成绩批量录入 (表格模式) ----");
        List<Score> scores = educationService.listScoresByPlan(planId);
        if (scores.isEmpty()) {
            ConsoleUtil.printError("当前课程没有已选课的学生");
            ConsoleUtil.pause();
            return;
        }

        System.out.println("进入批量输入。输入分数范围 0-100，或输入 -1 取消该学生录入。\n");
        List<Score> toSave = new ArrayList<>();

        for (int i = 0; i < scores.size(); i++) {
            Score s = scores.get(i);
            System.out.printf("[%d/%d] 请输入学号: %s 姓名: %s 的成绩 > ", i + 1, scores.size(), s.getStudentNo(), s.getStudentName());
            double scoreVal = ConsoleUtil.readDouble("", -1.0, 100.0);
            if (scoreVal == -1.0) {
                System.out.println("已跳过该学生录入");
                continue;
            }
            Score copy = new Score();
            copy.setId(s.getId());
            copy.setTeachingPlanId(s.getTeachingPlanId());
            copy.setStudentId(s.getStudentId());
            copy.setScore(scoreVal);
            copy.setExamType("期末");
            toSave.add(copy);
        }

        if (toSave.isEmpty()) {
            ConsoleUtil.printError("未输入任何有效分数，已退出。");
            ConsoleUtil.pause();
            return;
        }

        System.out.printf("\n录入完成！已录入 %d 人。\n", toSave.size());
        if (ConsoleUtil.confirm("确认保存这批学生成绩吗？这会覆盖原有成绩。")) {
            try {
                for (Score s : toSave) {
                    educationService.recordScore(s);
                }
                ConsoleUtil.printSuccess("批量成绩已成功保存到数据库！");
            } catch (Exception e) {
                ConsoleUtil.printError("保存失败: " + e.getMessage());
            }
        }
        ConsoleUtil.pause();
    }

    private void viewScoreStats(Integer planId) {
        List<Score> list = educationService.listScoresByPlan(planId);
        if (list.isEmpty()) {
            ConsoleUtil.printError("当前无学生选课数据");
            ConsoleUtil.pause();
            return;
        }

        System.out.println("\n---- 学生成绩汇总表 ----");
        List<String> headers = Arrays.asList("学号", "姓名", "班级", "成绩", "等级成绩");
        List<List<String>> rows = new ArrayList<>();
        for (Score s : list) {
            rows.add(Arrays.asList(
                    s.getStudentNo(),
                    s.getStudentName(),
                    s.getClassName() != null ? s.getClassName() : "自由选修",
                    s.getScore() != null ? s.getScore().toString() : "未录入",
                    s.getGradeLevel() != null ? s.getGradeLevel() : "无"
            ));
        }
        ConsoleUtil.printTable(headers, rows);
        System.out.println();

        // Statistical Distribution
        Map<String, Object> stats = educationService.calculatePlanScoreStats(planId);
        int total = (int) stats.get("total");
        int graded = (int) stats.get("graded");
        double avg = (double) stats.get("avg");
        double max = (double) stats.get("max");
        double min = (double) stats.get("min");
        double passRate = (double) stats.get("passRate");
        double excelRate = (double) stats.get("excelRate");

        if (graded == 0) {
            System.out.println("该科目当前无任何已录入的成绩数据，无法展示统计图。");
            ConsoleUtil.pause();
            return;
        }

        List<String> labels = Arrays.asList("90-100 (A)", "80-89  (B)", "70-79  (C)", "60-69  (D)", "0-59   (F)");
        List<Integer> values = Arrays.asList(
                (int) stats.get("catA"),
                (int) stats.get("catB"),
                (int) stats.get("catC"),
                (int) stats.get("catD"),
                (int) stats.get("catF")
        );
        List<Double> pcts = Arrays.asList(
                (double) stats.get("pctA"),
                (double) stats.get("pctB"),
                (double) stats.get("pctC"),
                (double) stats.get("pctD"),
                (double) stats.get("pctF")
        );

        ConsoleUtil.printBarChart("成绩正态分布直方图 (共录入 " + graded + "/" + total + " 人)", labels, values, pcts);
        System.out.printf("平均分: %.1f  最高分: %.1f  最低分: %.1f  及格率: %.1f%%  优秀率: %.1f%%\n", avg, max, min, passRate, excelRate);

        // Print ranking
        System.out.println("\n---- 班级前 3 名优秀奖 ----");
        List<Score> ranked = educationService.getRankedScores(planId);
        for (int i = 0; i < Math.min(3, ranked.size()); i++) {
            Score rs = ranked.get(i);
            System.out.printf("  第 %d 名: %s (成绩: %.1f, 等级: %s)\n", i + 1, rs.getStudentName(), rs.getScore(), rs.getGradeLevel());
        }
        System.out.println("---------------------------\n");
        ConsoleUtil.pause();
    }

    private void exportScoreCSV(Integer planId) {
        System.out.println("---- 导出成绩单到 CSV ----");
        String path = ConsoleUtil.readLine("请输入保存路径 (如: data/course_scores.csv)", false);
        try {
            educationService.exportScoresToCSV(planId, path);
            ConsoleUtil.printSuccess("成绩已成功导出至: " + path);
        } catch (Exception e) {
            ConsoleUtil.printError("导出失败: " + e.getMessage());
        }
        ConsoleUtil.pause();
    }


    // --- 3. ATTENDANCE ---
    private void manageAttendance(Teacher t, String semester) {
        while (true) {
            ConsoleUtil.clearScreen();
            List<String> items = Arrays.asList(
                    "[1] 课堂考勤录入 (点名模式)",
                    "[2] 查看指定日期考勤记录",
                    "[0] 返回上级"
            );
            ConsoleUtil.printMenu("学生出勤管理系统", items);
            int choice = ConsoleUtil.readChoice("请选择操作", 2);
            if (choice == 0) break;

            Integer planId = selectMyTeachingPlan(t, semester);
            if (planId == null) continue;

            switch (choice) {
                case 1: takeClassAttendance(planId); break;
                case 2: viewClassAttendance(planId); break;
            }
        }
    }

    private void takeClassAttendance(Integer planId) {
        System.out.println("---- 课堂考勤点名 ----");
        Date date;
        Date today = new Date(System.currentTimeMillis());
        if (ConsoleUtil.confirm("今天是 " + today.toString() + "，是否使用今天作为考勤点名日期？")) {
            date = today;
        } else {
            date = ConsoleUtil.readDate("请输入实际的考勤点名日期");
        }
        
        // Retrieve all student score records which double as class selection listings
        List<Score> students = educationService.listScoresByPlan(planId);
        if (students.isEmpty()) {
            ConsoleUtil.printError("当前课程无选课学生");
            ConsoleUtil.pause();
            return;
        }

        // Fetch existing attendance records for this plan and date to pre-populate statuses
        List<Attendance> exist = educationService.listAttendanceByPlanAndDate(planId, date);
        Map<Integer, Attendance> existMap = new HashMap<>();
        for (Attendance a : exist) {
            existMap.put(a.getStudentId(), a);
        }

        List<Attendance> attendList = new ArrayList<>();
        for (Score s : students) {
            Attendance att = new Attendance();
            att.setStudentId(s.getStudentId());
            att.setStudentNo(s.getStudentNo());
            att.setStudentName(s.getStudentName());
            att.setClassName(s.getClassName());
            
            // Check if there is an existing record (e.g., approved leave)
            Attendance ex = existMap.get(s.getStudentId());
            if (ex != null) {
                att.setStatus(ex.getStatus());
                att.setRemark(ex.getRemark());
            } else {
                att.setStatus("出勤");
                att.setRemark(null);
            }
            attendList.add(att);
        }

        // Print initial status list
        System.out.println("\n---- 本班学生考勤状态初始化一览 ----");
        List<String> headers = Arrays.asList("序号", "学号", "姓名", "班级", "当前考勤状态", "备注");
        List<List<String>> rows = new ArrayList<>();
        for (int i = 0; i < attendList.size(); i++) {
            Attendance att = attendList.get(i);
            rows.add(Arrays.asList(
                String.valueOf(i + 1),
                att.getStudentNo(),
                att.getStudentName(),
                att.getClassName() != null ? att.getClassName() : "自由选修",
                att.getStatus(),
                att.getRemark() != null ? att.getRemark() : ""
            ));
        }
        ConsoleUtil.printTable(headers, rows);

        System.out.println("\n批量修改模式：请输入相应状态的学生序号，多个序号可用空格或逗号分隔（如: 1,3,5），直接回车表示无");

        // 1. Absent (旷课)
        String absentInput = ConsoleUtil.readLine("请输入【旷课】学生的序号");
        List<Integer> absentIdxs = parseIndices(absentInput, attendList.size());
        for (int idx : absentIdxs) {
            Attendance att = attendList.get(idx - 1);
            att.setStatus("旷课");
            String remark = ConsoleUtil.readLine("请输入学生 " + att.getStudentName() + " 的旷课备注 (回车跳过)", true);
            if (!remark.isEmpty()) {
                att.setRemark(remark);
            }
        }

        // 2. Late (迟到)
        String lateInput = ConsoleUtil.readLine("请输入【迟到】学生的序号");
        List<Integer> lateIdxs = parseIndices(lateInput, attendList.size());
        for (int idx : lateIdxs) {
            Attendance att = attendList.get(idx - 1);
            att.setStatus("迟到");
            String remark = ConsoleUtil.readLine("请输入学生 " + att.getStudentName() + " 的迟到备注 (回车跳过)", true);
            if (!remark.isEmpty()) {
                att.setRemark(remark);
            }
        }

        // 3. Early (早退)
        String earlyInput = ConsoleUtil.readLine("请输入【早退】学生的序号");
        List<Integer> earlyIdxs = parseIndices(earlyInput, attendList.size());
        for (int idx : earlyIdxs) {
            Attendance att = attendList.get(idx - 1);
            att.setStatus("早退");
            String remark = ConsoleUtil.readLine("请输入学生 " + att.getStudentName() + " 的早退备注 (回车跳过)", true);
            if (!remark.isEmpty()) {
                att.setRemark(remark);
            }
        }

        // 4. Leave (请假 - 手动添加非审批假条的情况)
        String leaveInput = ConsoleUtil.readLine("请输入其他【请假】学生的序号");
        List<Integer> leaveIdxs = parseIndices(leaveInput, attendList.size());
        for (int idx : leaveIdxs) {
            Attendance att = attendList.get(idx - 1);
            att.setStatus("请假");
            String remark = ConsoleUtil.readLine("请输入学生 " + att.getStudentName() + " 的请假备注 (回车跳过)", true);
            if (!remark.isEmpty()) {
                att.setRemark(remark);
            }
        }

        // Print confirmation list
        System.out.println("\n---- 修改后考勤状态确认一览 ----");
        rows.clear();
        for (int i = 0; i < attendList.size(); i++) {
            Attendance att = attendList.get(i);
            rows.add(Arrays.asList(
                String.valueOf(i + 1),
                att.getStudentNo(),
                att.getStudentName(),
                att.getClassName() != null ? att.getClassName() : "自由选修",
                att.getStatus(),
                att.getRemark() != null ? att.getRemark() : ""
            ));
        }
        ConsoleUtil.printTable(headers, rows);

        if (ConsoleUtil.confirm("\n点名录入完毕。确认保存该考勤记录？")) {
            try {
                educationService.takeAttendance(planId, date, attendList);
                ConsoleUtil.printSuccess("考勤记录已成功写入！");
            } catch (Exception e) {
                ConsoleUtil.printError("写入失败: " + e.getMessage());
            }
        }
        ConsoleUtil.pause();
    }

    private List<Integer> parseIndices(String input, int maxIndex) {
        List<Integer> list = new ArrayList<>();
        if (input == null || input.trim().isEmpty()) return list;
        String[] parts = input.split("[,\\s]+");
        for (String part : parts) {
            try {
                int idx = Integer.parseInt(part.trim());
                if (idx >= 1 && idx <= maxIndex) {
                    list.add(idx);
                } else {
                    System.out.println("[警告] 忽略超出有效范围的序号: " + part);
                }
            } catch (NumberFormatException e) {
                System.out.println("[警告] 忽略无效的数字输入: " + part);
            }
        }
        return list;
    }

    private void viewClassAttendance(Integer planId) {
        System.out.println("---- 查看指定日期考勤 ----");
        
        // Fetch all attendance records for this plan to find distinct dates
        List<Attendance> allAttendance = educationService.listAttendanceByPlanAndDate(planId, null);
        java.util.Set<Date> dateSet = new java.util.TreeSet<>(java.util.Collections.reverseOrder());
        for (Attendance a : allAttendance) {
            if (a.getAttendDate() != null) {
                dateSet.add(a.getAttendDate());
            }
        }
        
        if (dateSet.isEmpty()) {
            ConsoleUtil.printError("该课程暂无任何历史考勤记录！");
            ConsoleUtil.pause();
            return;
        }

        List<Date> dateList = new ArrayList<>(dateSet);
        List<String> items = new ArrayList<>();
        for (int i = 0; i < dateList.size(); i++) {
            items.add(String.format("[%d] %s", i + 1, dateList.get(i).toString()));
        }
        ConsoleUtil.printMenu("请选择要查看的考勤日期 (已按最近时间排序)", items);
        int choice = ConsoleUtil.readChoice("选择日期", dateList.size());
        if (choice == 0) return;
        
        Date date = dateList.get(choice - 1);
        List<Attendance> attendances = educationService.listAttendanceByPlanAndDate(planId, date);
        if (attendances.isEmpty()) {
            ConsoleUtil.printError("该日期下没有找到点名考勤记录");
            ConsoleUtil.pause();
            return;
        }

        System.out.println("---- 考勤统计表 (" + date + ") ----");
        List<String> headers = Arrays.asList("学号", "姓名", "班级", "出勤状态", "备注");
        List<List<String>> rows = new ArrayList<>();
        int present = 0, late = 0, early = 0, absent = 0, leave = 0;
        for (Attendance a : attendances) {
            rows.add(Arrays.asList(
                    a.getStudentNo(),
                    a.getStudentName(),
                    a.getClassName() != null ? a.getClassName() : "选修",
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
        System.out.printf("统计摘要: 出勤:%d | 迟到:%d | 早退:%d | 旷课:%d | 请假:%d | 实到率:%.1f%%\n",
                present, late, early, absent, leave, (double) (present + late + early) / attendances.size() * 100.0);
        ConsoleUtil.pause();
    }


    // --- 4. LEAVES APPROVAL ---
    private void manageLeaves(User loggedInUser) {
        while (true) {
            ConsoleUtil.clearScreen();
            List<LeaveRequest> list = educationService.listPendingLeavesByTeacher(loggedInUser.getId());
            if (list.isEmpty()) {
                System.out.println("---- 暂无需要您审批的请假申请 ----");
                ConsoleUtil.pause();
                break;
            }

            System.out.println("---- 待审批的学生请假申请表 ----");
            List<String> headers = Arrays.asList("ID", "班级", "学号", "姓名", "开始日期", "结束日期", "请假原因");
            List<List<String>> rows = new ArrayList<>();
            for (LeaveRequest lr : list) {
                rows.add(Arrays.asList(
                        lr.getId().toString(),
                        lr.getClassName(),
                        lr.getStudentNo(),
                        lr.getStudentName(),
                        lr.getStartDate().toString(),
                        lr.getEndDate().toString(),
                        lr.getReason()
                ));
            }
            ConsoleUtil.printTable(headers, rows);
            System.out.println("----------------------------------");
            System.out.println(" 请输入对应的 ID 开始审批，输入 0 返回");
            int reqId = ConsoleUtil.readInt("审批 ID", 0, 9999);
            if (reqId == 0) break;

            // Search request in the list
            LeaveRequest selected = null;
            for (LeaveRequest lr : list) {
                if (lr.getId().equals(reqId)) {
                    selected = lr;
                    break;
                }
            }

            if (selected == null) {
                ConsoleUtil.printError("无效的审批 ID，请重新选择");
                ConsoleUtil.pause();
                continue;
            }

            System.out.println("您正在审批: " + selected.getStudentName() + " 的请假申请 (" + selected.getStartDate() + " 至 " + selected.getEndDate() + ")");
            System.out.println("请给出审批意见:\n  [1] 批准请假\n  [2] 拒绝请假\n  [0] 暂不处理");
            int decision = ConsoleUtil.readChoice("审批选择", 2);
            if (decision == 0) continue;

            String status = decision == 1 ? "已批准" : "已拒绝";
            String remark = ConsoleUtil.readLine("审批备注 (如: 同意，注意销假/不予批准原因)");

            try {
                educationService.approveLeave(selected.getId(), status, loggedInUser.getId(), remark);
                ConsoleUtil.printSuccess("请假审批处理成功！结果已写入。");
            } catch (Exception e) {
                ConsoleUtil.printError("处理失败: " + e.getMessage());
            }
            ConsoleUtil.pause();
        }
    }


    // --- 5. NOTICES ---
    private void manageNotices(User loggedInUser) {
        System.out.println("---- 发布公告 (老师专属公告) ----");
        String title = ConsoleUtil.readLine("公告标题", false);
        String content = ConsoleUtil.readLine("公告正文 (换行可用 \\n)", false);
        content = content.replace("\\n", "\n");
        boolean isTop = ConsoleUtil.confirm("置顶显示？");

        Notice n = new Notice();
        n.setTitle(title);
        n.setContent(content);
        n.setPublisherId(loggedInUser.getId());
        n.setTargetRole("STUDENT"); // Teachers can notify students
        n.setIsTop(isTop ? 1 : 0);
        n.setStatus(1);

        try {
            educationService.publishNotice(n);
            ConsoleUtil.printSuccess("公告已成功发布到学生公告板！");
        } catch (Exception e) {
            ConsoleUtil.printError("发布失败: " + e.getMessage());
        }
        ConsoleUtil.pause();
    }


    // --- HELPER SELECTOR ---
    private Integer selectMyTeachingPlan(Teacher t, String semester) {
        List<TeachingPlan> plans = academicService.listTeachingPlansByTeacher(t.getId(), semester);
        if (plans.isEmpty()) {
            ConsoleUtil.printError("您在本学期没有任何授课教学安排！");
            ConsoleUtil.pause();
            return null;
        }

        List<String> items = new ArrayList<>();
        for (int i = 0; i < plans.size(); i++) {
            TeachingPlan tp = plans.get(i);
            items.add(String.format("[%d] %s (%s)", i + 1, tp.getCourseName(), tp.getClassName() != null ? tp.getClassName() : "选修班"));
        }
        ConsoleUtil.printMenu("请选择授课科目与班级", items);
        int choice = ConsoleUtil.readChoice("选择", plans.size());
        if (choice == 0) return null;
        return plans.get(choice - 1).getId();
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
}
