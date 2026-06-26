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
import java.util.Map;

public class AdminController {

    private final AdminService adminService = new AdminServiceImpl();
    private final AcademicService academicService = new AcademicServiceImpl();
    private final EducationService educationService = new EducationServiceImpl();
    private final UserService userService = new UserServiceImpl();

    public void showMainMenu(User loggedInUser) {
        while (true) {
            ConsoleUtil.clearScreen();
            List<String> items = Arrays.asList(
                    "[1] 学生管理",
                    "[2] 教师管理",
                    "[3] 班级管理",
                    "[4] 课程管理",
                    "[5] 教学计划管理",
                    "[6] 课表排课管理",
                    "[7] 公告通知管理",
                    "[8] 账户与安全管理",
                    "[9] 数据统计与报表",
                    "[0] 返回登录主页"
            );
            ConsoleUtil.printMenu("系统管理主页面 — 当前管理员: " + loggedInUser.getRealName(), items);
            int choice = ConsoleUtil.readChoice("请选择操作", 9);
            if (choice == 0) {
                break;
            }
            switch (choice) {
                case 1: manageStudents(); break;
                case 2: manageTeachers(); break;
                case 3: manageClasses(); break;
                case 4: manageCourses(); break;
                case 5: manageTeachingPlans(); break;
                case 6: manageSchedules(); break;
                case 7: manageNotices(loggedInUser.getId()); break;
                case 8: manageAccounts(); break;
                case 9: manageStatistics(); break;
            }
        }
    }

    // --- 1. STUDENT MANAGEMENT ---
    private void manageStudents() {
        while (true) {
            ConsoleUtil.clearScreen();
            List<String> items = Arrays.asList(
                    "[1] 查看学生花名册 (分页/查询)",
                    "[2] 添加新学生",
                    "[3] 修改学生信息",
                    "[4] 删除学生归档",
                    "[5] 学生转班操作",
                    "[6] CSV 批量导入学生",
                    "[7] 导出学生到 CSV",
                    "[0] 返回上级"
            );
            ConsoleUtil.printMenu("学生管理菜单", items);
            int choice = ConsoleUtil.readChoice("请选择操作", 7);
            if (choice == 0) break;
            switch (choice) {
                case 1: viewStudentsRoster(); break;
                case 2: addNewStudent(); break;
                case 3: updateStudentInfo(); break;
                case 4: deleteStudentArchive(); break;
                case 5: transferStudentClass(); break;
                case 6: importStudentsCSV(); break;
                case 7: exportStudentsCSV(); break;
            }
        }
    }

    private void viewStudentsRoster() {
        int page = 1;
        int pageSize = 10;
        String query = "";
        Integer classId = null;
        Integer majorId = null;

        while (true) {
            ConsoleUtil.clearScreen();
            int total = adminService.countStudents(query, classId, majorId);
            int totalPages = (int) Math.ceil((double) total / pageSize);
            if (totalPages == 0) totalPages = 1;

            List<Student> students = adminService.searchStudents(query, classId, majorId, page, pageSize);
            System.out.printf("---- 学生列表 (共 %d 条记录, 页码: %d/%d) ----\n", total, page, totalPages);

            List<String> headers = Arrays.asList("ID", "学号", "姓名", "性别", "出生日期", "手机号", "班级");
            List<List<String>> rows = new ArrayList<>();
            for (Student s : students) {
                rows.add(Arrays.asList(
                        s.getId().toString(),
                        s.getStudentNo(),
                        s.getName(),
                        s.getGender(),
                        s.getBirthDate().toString(),
                        s.getPhone() != null ? s.getPhone() : "",
                        s.getClassName() != null ? s.getClassName() : ""
                ));
            }
            ConsoleUtil.printTable(headers, rows);
            System.out.println("--------------------------------------------------");
            System.out.println("  [P] 上一页   [N] 下一页   [F] 筛选过滤   [C] 清除筛选   [Q] 返回上级");
            String opt = ConsoleUtil.readLine("请选择操作").toUpperCase();
            if (opt.equals("Q")) {
                break;
            } else if (opt.equals("P")) {
                if (page > 1) page--;
                else ConsoleUtil.printError("已经是第一页了");
            } else if (opt.equals("N")) {
                if (page < totalPages) page++;
                else ConsoleUtil.printError("已经是最后一页了");
            } else if (opt.equals("F")) {
                query = ConsoleUtil.readLine("输入学号/姓名关键字 (回车跳过)");
                System.out.println("[1] 不限班级  [2] 选择班级");
                if (ConsoleUtil.readChoice("选择班级筛选", 2) == 2) {
                    classId = selectClass();
                }
                page = 1;
            } else if (opt.equals("C")) {
                query = "";
                classId = null;
                majorId = null;
                page = 1;
                ConsoleUtil.printSuccess("筛选已重置");
            }
        }
    }

    private void addNewStudent() {
        System.out.println("---- 添加新学生 ----");
        String studentNo = ConsoleUtil.readLine("学号", false);
        if (adminService.getStudentByNo(studentNo) != null) {
            ConsoleUtil.printError("该学号已存在！");
            ConsoleUtil.pause();
            return;
        }
        String name = ConsoleUtil.readLine("姓名", false);
        String gender = ConsoleUtil.readLine("性别", false);
        Date birthDate = ConsoleUtil.readDate("出生日期");
        String phone = ConsoleUtil.readLine("手机号 (可选)");
        String email = ConsoleUtil.readLine("邮箱 (可选)");
        String address = ConsoleUtil.readLine("联系地址 (可选)");
        Integer classId = selectClass();
        if (classId == null) {
            ConsoleUtil.printError("必须绑定班级才能添加学生！");
            ConsoleUtil.pause();
            return;
        }

        Student s = new Student();
        s.setStudentNo(studentNo);
        s.setName(name);
        s.setGender(gender);
        s.setBirthDate(birthDate);
        s.setPhone(phone.isEmpty() ? null : phone);
        s.setEmail(email.isEmpty() ? null : email);
        s.setAddress(address.isEmpty() ? null : address);
        s.setEnrollDate(new Date(System.currentTimeMillis()));
        s.setClassId(classId);

        try {
            adminService.addStudent(s, studentNo, "student123");
            ConsoleUtil.printSuccess("学生添加成功！默认登录密码为: student123");
        } catch (Exception e) {
            ConsoleUtil.printError("添加学生失败: " + e.getMessage());
        }
        ConsoleUtil.pause();
    }

    private void updateStudentInfo() {
        System.out.println("---- 修改学生信息 ----");
        String studentNo = ConsoleUtil.readLine("请输入待修改学生的学号", false);
        Student s = adminService.getStudentByNo(studentNo);
        if (s == null) {
            ConsoleUtil.printError("学号不存在！");
            ConsoleUtil.pause();
            return;
        }

        System.out.println("正在修改学生: " + s.getName() + " (" + s.getStudentNo() + ")");
        String name = ConsoleUtil.readLine("姓名 (回车默认: " + s.getName() + ")");
        if (!name.isEmpty()) s.setName(name);

        String gender = ConsoleUtil.readLine("性别 (回车默认: " + s.getGender() + ")");
        if (!gender.isEmpty()) s.setGender(gender);

        Date bDate = ConsoleUtil.readDateOptional("出生日期", s.getBirthDate());
        s.setBirthDate(bDate);

        String phone = ConsoleUtil.readLine("手机 (回车默认: " + (s.getPhone() != null ? s.getPhone() : "") + ")");
        if (!phone.isEmpty()) s.setPhone(phone);

        String email = ConsoleUtil.readLine("邮箱 (回车默认: " + (s.getEmail() != null ? s.getEmail() : "") + ")");
        if (!email.isEmpty()) s.setEmail(email);

        String address = ConsoleUtil.readLine("地址 (回车默认: " + (s.getAddress() != null ? s.getAddress() : "") + ")");
        if (!address.isEmpty()) s.setAddress(address);

        try {
            adminService.updateStudent(s);
            ConsoleUtil.printSuccess("学生信息修改成功！");
        } catch (Exception e) {
            ConsoleUtil.printError("修改失败: " + e.getMessage());
        }
        ConsoleUtil.pause();
    }

    private void deleteStudentArchive() {
        System.out.println("---- 删除学生 ----");
        String studentNo = ConsoleUtil.readLine("请输入待删除学生的学号", false);
        Student s = adminService.getStudentByNo(studentNo);
        if (s == null) {
            ConsoleUtil.printError("学号不存在！");
            ConsoleUtil.pause();
            return;
        }

        if (ConsoleUtil.confirm("确认要删除并销毁学生 " + s.getName() + " 的档案及其账户？（此操作不可逆！）")) {
            try {
                adminService.deleteStudent(s.getId());
                ConsoleUtil.printSuccess("学生档案及对应账户删除成功！");
            } catch (Exception e) {
                ConsoleUtil.printError("删除失败: " + e.getMessage());
            }
        }
        ConsoleUtil.pause();
    }

    private void transferStudentClass() {
        System.out.println("---- 学生转班操作 ----");
        String studentNo = ConsoleUtil.readLine("学号", false);
        Student s = adminService.getStudentByNo(studentNo);
        if (s == null) {
            ConsoleUtil.printError("学生不存在！");
            ConsoleUtil.pause();
            return;
        }
        System.out.println("当前学生班级: " + s.getClassName());
        System.out.println("请选择目标班级:");
        Integer targetClassId = selectClass();
        if (targetClassId == null) return;

        try {
            adminService.changeStudentClass(s.getId(), targetClassId);
            ConsoleUtil.printSuccess("转班成功！");
        } catch (Exception e) {
            ConsoleUtil.printError("转班失败: " + e.getMessage());
        }
        ConsoleUtil.pause();
    }

    private void importStudentsCSV() {
        System.out.println("---- 从 CSV 文件导入学生 ----");
        String path = ConsoleUtil.readLine("请输入 CSV 文件绝对路径 (如: data/students.csv)", false);
        try {
            int count = adminService.importStudentsFromCSV(path);
            ConsoleUtil.printSuccess("成功导入 " + count + " 位学生！");
        } catch (Exception e) {
            ConsoleUtil.printError("导入失败: " + e.getMessage());
        }
        ConsoleUtil.pause();
    }

    private void exportStudentsCSV() {
        System.out.println("---- 导出学生到 CSV ----");
        String path = ConsoleUtil.readLine("请输入导出文件路径 (如: data/export_students.csv)", false);
        try {
            adminService.exportStudentsToCSV(path);
            ConsoleUtil.printSuccess("数据导出成功，文件已保存至: " + path);
        } catch (Exception e) {
            ConsoleUtil.printError("导出失败: " + e.getMessage());
        }
        ConsoleUtil.pause();
    }


    // --- 2. TEACHER MANAGEMENT ---
    private void manageTeachers() {
        while (true) {
            ConsoleUtil.clearScreen();
            List<String> items = Arrays.asList(
                    "[1] 查看教师列表",
                    "[2] 添加新教师",
                    "[3] 修改教师信息",
                    "[4] 删除教师",
                    "[0] 返回上级"
            );
            ConsoleUtil.printMenu("教师管理菜单", items);
            int choice = ConsoleUtil.readChoice("请选择操作", 4);
            if (choice == 0) break;
            switch (choice) {
                case 1: viewTeachersList(); break;
                case 2: addNewTeacher(); break;
                case 3: updateTeacherInfo(); break;
                case 4: deleteTeacherInfo(); break;
            }
        }
    }

    private void viewTeachersList() {
        List<Teacher> teachers = adminService.listAllTeachers();
        System.out.println("---- 教师花名册 ----");
        List<String> headers = Arrays.asList("ID", "工号", "姓名", "性别", "职称", "院系", "电话", "邮箱");
        List<List<String>> rows = new ArrayList<>();
        for (Teacher t : teachers) {
            rows.add(Arrays.asList(
                    t.getId().toString(),
                    t.getTeacherNo(),
                    t.getName(),
                    t.getGender(),
                    t.getTitle() != null ? t.getTitle() : "",
                    t.getDeptName() != null ? t.getDeptName() : "",
                    t.getPhone() != null ? t.getPhone() : "",
                    t.getEmail() != null ? t.getEmail() : ""
            ));
        }
        ConsoleUtil.printTable(headers, rows);
        ConsoleUtil.pause();
    }

    private void addNewTeacher() {
        System.out.println("---- 添加新教师 ----");
        String teacherNo = ConsoleUtil.readLine("工号", false);
        if (adminService.getTeacherByNo(teacherNo) != null) {
            ConsoleUtil.printError("该工号已存在！");
            ConsoleUtil.pause();
            return;
        }
        String name = ConsoleUtil.readLine("姓名", false);
        String gender = ConsoleUtil.readLine("性别", false);
        String title = ConsoleUtil.readLine("职称 (如: 教授/副教授/讲师)");
        String phone = ConsoleUtil.readLine("手机号 (可选)");
        String email = ConsoleUtil.readLine("邮箱 (可选)");

        Integer deptId = selectDepartment();
        if (deptId == null) {
            ConsoleUtil.printError("必须绑定院系才能添加教师！");
            ConsoleUtil.pause();
            return;
        }

        Teacher t = new Teacher();
        t.setTeacherNo(teacherNo);
        t.setName(name);
        t.setGender(gender);
        t.setTitle(title.isEmpty() ? null : title);
        t.setPhone(phone.isEmpty() ? null : phone);
        t.setEmail(email.isEmpty() ? null : email);
        t.setDepartmentId(deptId);
        t.setStatus(1);

        try {
            adminService.addTeacher(t, teacherNo, "teacher123");
            ConsoleUtil.printSuccess("教师添加成功！默认登录密码为: teacher123");
        } catch (Exception e) {
            ConsoleUtil.printError("添加失败: " + e.getMessage());
        }
        ConsoleUtil.pause();
    }

    private void updateTeacherInfo() {
        System.out.println("---- 修改教师信息 ----");
        String teacherNo = ConsoleUtil.readLine("请输入待修改教师工号", false);
        Teacher t = adminService.getTeacherByNo(teacherNo);
        if (t == null) {
            ConsoleUtil.printError("工号不存在！");
            ConsoleUtil.pause();
            return;
        }

        System.out.println("正在修改教师: " + t.getName() + " (" + t.getTeacherNo() + ")");
        String name = ConsoleUtil.readLine("姓名 (回车默认: " + t.getName() + ")");
        if (!name.isEmpty()) t.setName(name);

        String gender = ConsoleUtil.readLine("性别 (回车默认: " + t.getGender() + ")");
        if (!gender.isEmpty()) t.setGender(gender);

        String title = ConsoleUtil.readLine("职称 (回车默认: " + (t.getTitle() != null ? t.getTitle() : "") + ")");
        if (!title.isEmpty()) t.setTitle(title);

        String phone = ConsoleUtil.readLine("手机 (回车默认: " + (t.getPhone() != null ? t.getPhone() : "") + ")");
        if (!phone.isEmpty()) t.setPhone(phone);

        String email = ConsoleUtil.readLine("邮箱 (回车默认: " + (t.getEmail() != null ? t.getEmail() : "") + ")");
        if (!email.isEmpty()) t.setEmail(email);

        System.out.println("是否修改所属院系？");
        if (ConsoleUtil.confirm("修改院系？")) {
            Integer deptId = selectDepartment();
            if (deptId != null) t.setDepartmentId(deptId);
        }

        try {
            adminService.updateTeacher(t);
            ConsoleUtil.printSuccess("教师信息修改成功！");
        } catch (Exception e) {
            ConsoleUtil.printError("修改失败: " + e.getMessage());
        }
        ConsoleUtil.pause();
    }

    private void deleteTeacherInfo() {
        System.out.println("---- 删除教师 ----");
        String teacherNo = ConsoleUtil.readLine("请输入待删除教师工号", false);
        Teacher t = adminService.getTeacherByNo(teacherNo);
        if (t == null) {
            ConsoleUtil.printError("工号不存在！");
            ConsoleUtil.pause();
            return;
        }

        if (ConsoleUtil.confirm("确认要删除并销毁教师 " + t.getName() + " 档案及其账户？")) {
            try {
                adminService.deleteTeacher(t.getId());
                ConsoleUtil.printSuccess("教师删除成功！");
            } catch (Exception e) {
                ConsoleUtil.printError("删除失败: " + e.getMessage());
            }
        }
        ConsoleUtil.pause();
    }


    // --- 3. CLASS MANAGEMENT ---
    private void manageClasses() {
        while (true) {
            ConsoleUtil.clearScreen();
            List<String> items = Arrays.asList(
                    "[1] 查看班级列表",
                    "[2] 添加新班级",
                    "[3] 指定/更换班主任",
                    "[4] 查看班级花名册",
                    "[5] 删除班级",
                    "[0] 返回上级"
            );
            ConsoleUtil.printMenu("班级管理菜单", items);
            int choice = ConsoleUtil.readChoice("请选择操作", 5);
            if (choice == 0) break;
            switch (choice) {
                case 1: viewClassesList(); break;
                case 2: addNewClass(); break;
                case 3: assignClassHeadTeacher(); break;
                case 4: viewClassRoster(); break;
                case 5: deleteClassInfo(); break;
            }
        }
    }

    private void viewClassesList() {
        List<Clazz> classes = adminService.listAllClazzes();
        System.out.println("---- 班级一览表 ----");
        List<String> headers = Arrays.asList("ID", "班级编号", "班级名称", "所属专业", "入学年份", "班主任");
        List<List<String>> rows = new ArrayList<>();
        for (Clazz c : classes) {
            rows.add(Arrays.asList(
                    c.getId().toString(),
                    c.getClassNo(),
                    c.getClassName(),
                    c.getMajorName() != null ? c.getMajorName() : "",
                    c.getEnrollYear().toString() + "级",
                    c.getHeadTeacherName() != null ? c.getHeadTeacherName() : "未指定"
            ));
        }
        ConsoleUtil.printTable(headers, rows);
        ConsoleUtil.pause();
    }

    private void addNewClass() {
        System.out.println("---- 添加新班级 ----");
        String classNo = ConsoleUtil.readLine("班级编号", false);
        // 按班级编号检查是否已存在
        boolean exists = adminService.listAllClazzes().stream()
                .anyMatch(c -> c.getClassNo().equalsIgnoreCase(classNo));
        if (exists) {
            ConsoleUtil.printError("班级编号 " + classNo + " 已存在，请使用其他编号！");
            ConsoleUtil.pause();
            return;
        }
        String className = ConsoleUtil.readLine("班级名称", false);
        Integer majorId = selectMajor();
        if (majorId == null) {
            ConsoleUtil.printError("必须绑定专业！");
            ConsoleUtil.pause();
            return;
        }
        int enrollYear = ConsoleUtil.readInt("入学年份", 2000, 2090);
        System.out.println("是否指定班主任？");
        Integer teacherId = null;
        if (ConsoleUtil.confirm("指定班主任？")) {
            teacherId = selectTeacher();
        }

        Clazz c = new Clazz();
        c.setClassNo(classNo);
        c.setClassName(className);
        c.setMajorId(majorId);
        c.setEnrollYear(enrollYear);
        c.setHeadTeacherId(teacherId);

        try {
            adminService.addClazz(c);
            ConsoleUtil.printSuccess("班级创建成功！");
        } catch (Exception e) {
            ConsoleUtil.printError("创建失败: " + e.getMessage());
        }
        ConsoleUtil.pause();
    }

    private void assignClassHeadTeacher() {
        System.out.println("---- 指定/更换班主任 ----");
        Integer classId = selectClass();
        if (classId == null) return;

        Clazz c = adminService.getClazzById(classId);
        System.out.println("当前班主任: " + (c.getHeadTeacherName() != null ? c.getHeadTeacherName() : "暂无"));
        System.out.println("请选择新的班主任:");
        Integer teacherId = selectTeacher();
        if (teacherId == null) return;

        c.setHeadTeacherId(teacherId);
        try {
            adminService.updateClazz(c);
            ConsoleUtil.printSuccess("班主任指定/更换成功！");
        } catch (Exception e) {
            ConsoleUtil.printError("指定失败: " + e.getMessage());
        }
        ConsoleUtil.pause();
    }

    private void viewClassRoster() {
        System.out.println("---- 查看班级学生花名册 ----");
        Integer classId = selectClass();
        if (classId == null) return;

        Clazz c = adminService.getClazzById(classId);
        List<Student> students = adminService.listStudentsByClass(classId);
        System.out.printf("---- %s 花名册 (共 %d 人) ----\n", c.getClassName(), students.size());
        List<String> headers = Arrays.asList("序号", "学号", "姓名", "性别", "手机号", "邮箱");
        List<List<String>> rows = new ArrayList<>();
        for (int i = 0; i < students.size(); i++) {
            Student s = students.get(i);
            rows.add(Arrays.asList(
                    String.valueOf(i + 1),
                    s.getStudentNo(),
                    s.getName(),
                    s.getGender(),
                    s.getPhone() != null ? s.getPhone() : "",
                    s.getEmail() != null ? s.getEmail() : ""
            ));
        }
        ConsoleUtil.printTable(headers, rows);
        ConsoleUtil.pause();
    }

    private void deleteClassInfo() {
        System.out.println("---- 删除班级 ----");
        Integer classId = selectClass();
        if (classId == null) return;
        Clazz c = adminService.getClazzById(classId);
        if (ConsoleUtil.confirm("确认要删除班级 " + c.getClassName() + "？（关联的外键可能限制此操作）")) {
            try {
                adminService.deleteClazz(classId);
                ConsoleUtil.printSuccess("班级删除成功！");
            } catch (Exception e) {
                ConsoleUtil.printError("删除失败: " + e.getMessage());
            }
        }
        ConsoleUtil.pause();
    }


    // --- 4. COURSE MANAGEMENT ---
    private void manageCourses() {
        while (true) {
            ConsoleUtil.clearScreen();
            List<String> items = Arrays.asList(
                    "[1] 查看全部课程",
                    "[2] 添加新课程",
                    "[3] 修改课程信息",
                    "[4] 删除课程",
                    "[0] 返回上级"
            );
            ConsoleUtil.printMenu("课程管理菜单", items);
            int choice = ConsoleUtil.readChoice("请选择操作", 4);
            if (choice == 0) break;
            switch (choice) {
                case 1: viewCoursesList(); break;
                case 2: addNewCourse(); break;
                case 3: updateCourseInfo(); break;
                case 4: deleteCourseInfo(); break;
            }
        }
    }

    private void viewCoursesList() {
        List<Course> courses = academicService.listAllCourses();
        System.out.println("---- 课程大纲列表 ----");
        List<String> headers = Arrays.asList("ID", "课程编号", "课程名称", "学分", "学时", "课程类型");
        List<List<String>> rows = new ArrayList<>();
        for (Course c : courses) {
            rows.add(Arrays.asList(
                    c.getId().toString(),
                    c.getCourseNo(),
                    c.getCourseName(),
                    c.getCredit().toString(),
                    c.getHours().toString(),
                    c.getType()
            ));
        }
        ConsoleUtil.printTable(headers, rows);
        ConsoleUtil.pause();
    }

    private void addNewCourse() {
        System.out.println("---- 添加新课程 ----");
        String courseNo = ConsoleUtil.readLine("课程编号", false);
        if (academicService.getCourseByNo(courseNo) != null) {
            ConsoleUtil.printError("课程编号已存在！");
            ConsoleUtil.pause();
            return;
        }
        String courseName = ConsoleUtil.readLine("课程名称", false);
        double credit = ConsoleUtil.readDouble("学分 (0.5 - 10.0)", 0.5, 10.0);
        int hours = ConsoleUtil.readInt("学时 (8 - 120)", 8, 120);
        System.out.println("请选择课程类型:\n  [1] 必修\n  [2] 选修\n  [3] 公选");
        int typeChoice = ConsoleUtil.readChoice("课程类型", 3);
        String type = "必修";
        if (typeChoice == 2) type = "选修";
        else if (typeChoice == 3) type = "公选";

        Course c = new Course();
        c.setCourseNo(courseNo);
        c.setCourseName(courseName);
        c.setCredit(credit);
        c.setHours(hours);
        c.setType(type);

        try {
            academicService.addCourse(c);
            ConsoleUtil.printSuccess("课程添加成功！");
        } catch (Exception e) {
            ConsoleUtil.printError("添加失败: " + e.getMessage());
        }
        ConsoleUtil.pause();
    }

    private void updateCourseInfo() {
        System.out.println("---- 修改课程信息 ----");
        String courseNo = ConsoleUtil.readLine("请输入待修改的课程编号", false);
        Course c = academicService.getCourseByNo(courseNo);
        if (c == null) {
            ConsoleUtil.printError("课程不存在！");
            ConsoleUtil.pause();
            return;
        }

        System.out.println("正在修改课程: " + c.getCourseName() + " (" + c.getCourseNo() + ")");
        String name = ConsoleUtil.readLine("课程名 (回车默认: " + c.getCourseName() + ")");
        if (!name.isEmpty()) c.setCourseName(name);

        // 修复：只用一次 readLine，并加 try-catch 防止非数字输入崩溃
        String creditStr = ConsoleUtil.readLine("学分 (回车默认: " + c.getCredit() + ")");
        if (!creditStr.isEmpty()) {
            try {
                double credit = Double.parseDouble(creditStr);
                if (credit < 0.5 || credit > 10.0) {
                    ConsoleUtil.printError("学分必须在 0.5 - 10.0 之间，已保留原值。");
                } else {
                    c.setCredit(credit);
                }
            } catch (NumberFormatException e) {
                ConsoleUtil.printError("学分格式不正确，已保留原值。");
            }
        }

        String hoursStr = ConsoleUtil.readLine("学时 (回车默认: " + c.getHours() + ")");
        if (!hoursStr.isEmpty()) {
            try {
                int hours = Integer.parseInt(hoursStr);
                if (hours < 1 || hours > 512) {
                    ConsoleUtil.printError("学时必须在 1 - 512 之间，已保留原值。");
                } else {
                    c.setHours(hours);
                }
            } catch (NumberFormatException e) {
                ConsoleUtil.printError("学时格式不正确，已保留原值。");
            }
        }

        System.out.println("修改课程类型？当前: " + c.getType());
        if (ConsoleUtil.confirm("修改类型？")) {
            System.out.println("  [1] 必修\n  [2] 选修\n  [3] 公选");
            int typeChoice = ConsoleUtil.readChoice("类型选择", 3);
            if (typeChoice == 1) c.setType("必修");
            else if (typeChoice == 2) c.setType("选修");
            else if (typeChoice == 3) c.setType("公选");
        }

        try {
            academicService.updateCourse(c);
            ConsoleUtil.printSuccess("课程更新成功！");
        } catch (Exception e) {
            ConsoleUtil.printError("更新失败: " + e.getMessage());
        }
        ConsoleUtil.pause();
    }

    private void deleteCourseInfo() {
        System.out.println("---- 删除课程 ----");
        String courseNo = ConsoleUtil.readLine("课程编号", false);
        Course c = academicService.getCourseByNo(courseNo);
        if (c == null) {
            ConsoleUtil.printError("课程不存在！");
            ConsoleUtil.pause();
            return;
        }

        if (ConsoleUtil.confirm("确认要删除课程 " + c.getCourseName() + " 吗？")) {
            try {
                academicService.deleteCourse(c.getId());
                ConsoleUtil.printSuccess("课程删除成功！");
            } catch (Exception e) {
                ConsoleUtil.printError("删除失败: " + e.getMessage());
            }
        }
        ConsoleUtil.pause();
    }


    // --- 5. TEACHING PLANS ---
    private void manageTeachingPlans() {
        while (true) {
            ConsoleUtil.clearScreen();
            List<String> items = Arrays.asList(
                    "[1] 查看教学计划 (授课安排)",
                    "[2] 开设课程授课 (新增计划)",
                    "[3] 删除授课安排",
                    "[0] 返回上级"
            );
            ConsoleUtil.printMenu("教学计划与排课安排菜单", items);
            int choice = ConsoleUtil.readChoice("请选择操作", 3);
            if (choice == 0) break;
            switch (choice) {
                case 1: viewTeachingPlansList(); break;
                case 2: addNewTeachingPlan(); break;
                case 3: deleteTeachingPlanInfo(); break;
            }
        }
    }

    private void viewTeachingPlansList() {
        String semester = ConsoleUtil.readLine("请输入需要查询的学期 (如: 2025-2026-1, 直接回车看全部)");
        List<TeachingPlan> plans;
        if (semester.isEmpty()) {
            plans = academicService.listAllTeachingPlans();
        } else {
            plans = academicService.listElectivePlans(semester); // Or semester based teaching plans
            // For simplicity, let's filter all list by semester
            plans = new ArrayList<>();
            for (TeachingPlan tp : academicService.listAllTeachingPlans()) {
                if (tp.getSemester().equals(semester)) plans.add(tp);
            }
        }

        System.out.println("---- 教学计划授课表 ----");
        List<String> headers = Arrays.asList("ID", "学期", "课程名称", "课程类型", "主讲教师", "授课班级", "最大人数", "已选人数");
        List<List<String>> rows = new ArrayList<>();
        for (TeachingPlan tp : plans) {
            rows.add(Arrays.asList(
                    tp.getId().toString(),
                    tp.getSemester(),
                    tp.getCourseName(),
                    tp.getCourseType(),
                    tp.getTeacherName(),
                    tp.getClassName() != null ? tp.getClassName() : "(自由选修)",
                    tp.getMaxStudents().toString(),
                    tp.getCurrentStudents().toString()
            ));
        }
        ConsoleUtil.printTable(headers, rows);
        ConsoleUtil.pause();
    }

    private void addNewTeachingPlan() {
        System.out.println("---- 新增授课安排 ----");
        String semester = ConsoleUtil.readLine("授课学期 (如: 2025-2026-1)", false);
        System.out.println("请选择课程:");
        Integer courseId = selectCourse();
        if (courseId == null) return;
        Course c = academicService.getCourseById(courseId);

        System.out.println("请选择主讲教师:");
        Integer teacherId = selectTeacher();
        if (teacherId == null) return;

        Integer classId = null;
        if ("必修".equals(c.getType())) {
            System.out.println("此课程为必修课，请选择授课班级:");
            classId = selectClass();
            if (classId == null) {
                ConsoleUtil.printError("必修课必须绑定班级！");
                ConsoleUtil.pause();
                return;
            }
        } else {
            System.out.println("此课程为选修/公选课，不绑定单一班级。");
        }

        int maxStudents = ConsoleUtil.readInt("班级最大容量/人数上限 (10-200)", 10, 200);

        TeachingPlan tp = new TeachingPlan();
        tp.setSemester(semester);
        tp.setCourseId(courseId);
        tp.setTeacherId(teacherId);
        tp.setClassId(classId);
        tp.setMaxStudents(maxStudents);
        tp.setCurrentStudents(0);

        try {
            academicService.addTeachingPlan(tp);
            ConsoleUtil.printSuccess("授课计划开设成功！");
        } catch (Exception e) {
            ConsoleUtil.printError("开设失败 (可能由于该学期已有相同课程冲突): " + e.getMessage());
        }
        ConsoleUtil.pause();
    }

    private void deleteTeachingPlanInfo() {
        System.out.println("---- 删除授课安排 ----");
        List<TeachingPlan> list = academicService.listAllTeachingPlans();
        if (list.isEmpty()) {
            ConsoleUtil.printError("当前没有任何排课授课安排");
            ConsoleUtil.pause();
            return;
        }

        List<String> items = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            TeachingPlan tp = list.get(i);
            items.add(String.format("[%d] 学期:%s | %s | %s | %s", i + 1, tp.getSemester(), tp.getCourseName(), tp.getTeacherName(), tp.getClassName() != null ? tp.getClassName() : "选修"));
        }
        ConsoleUtil.printMenu("选择要删除的授课安排", items);
        int choice = ConsoleUtil.readChoice("选择", list.size());
        if (choice == 0) return;

        TeachingPlan selected = list.get(choice - 1);
        if (ConsoleUtil.confirm("确认要删除授课安排 [" + selected.getCourseName() + " - " + selected.getTeacherName() + "] 吗？这会清空所有该课程下的学生选课及成绩数据！")) {
            try {
                academicService.deleteTeachingPlan(selected.getId());
                ConsoleUtil.printSuccess("删除成功！");
            } catch (Exception e) {
                ConsoleUtil.printError("删除失败: " + e.getMessage());
            }
        }
        ConsoleUtil.pause();
    }


    // --- 6. WEEKLY SCHEDULES ---
    private void manageSchedules() {
        while (true) {
            ConsoleUtil.clearScreen();
            List<String> items = Arrays.asList(
                    "[1] 查看班级/教室/教师课表",
                    "[2] 安排每周排课 (新增时段)",
                    "[3] 删除排课时段",
                    "[0] 返回上级"
            );
            ConsoleUtil.printMenu("课表管理与冲突检测", items);
            int choice = ConsoleUtil.readChoice("请选择操作", 3);
            if (choice == 0) break;
            switch (choice) {
                case 1: viewTimetables(); break;
                case 2: arrangeCourseSchedule(); break;
                case 3: removeCourseSchedule(); break;
            }
        }
    }

    private void viewTimetables() {
        System.out.println("---- 课表查询 ----");
        String semester = ConsoleUtil.readLine("学期 (如: 2025-2026-1)", false);
        System.out.println("  [1] 按班级查询\n  [2] 按教师查询\n  [3] 按教室查询\n  [0] 返回");
        int type = ConsoleUtil.readChoice("选择查询类型", 3);
        if (type == 0) return;

        List<Schedule> list = null;
        String queryTitle = "";
        if (type == 1) {
            Integer classId = selectClass();
            if (classId != null) {
                list = academicService.listSchedulesByClass(classId, semester);
                queryTitle = adminService.getClazzById(classId).getClassName() + " 课表";
            }
        } else if (type == 2) {
            Integer teacherId = selectTeacher();
            if (teacherId != null) {
                list = academicService.listSchedulesByTeacher(teacherId, semester);
                queryTitle = adminService.getTeacherById(teacherId).getName() + " 老师课表";
            }
        } else if (type == 3) {
            String classroom = ConsoleUtil.readLine("请输入教室名称 (如: 教三-301)", false);
            list = academicService.listSchedulesByClassroom(classroom, semester);
            queryTitle = classroom + " 教室课表";
        }

        if (list == null || list.isEmpty()) {
            ConsoleUtil.printError("未找到课表排课记录");
            ConsoleUtil.pause();
            return;
        }

        System.out.println("---- " + queryTitle + " (" + semester + ") ----");
        List<String> headers = Arrays.asList("星期", "时段", "课程", "教师", "授课班级", "教室", "校区");
        List<List<String>> rows = new ArrayList<>();
        String[] days = {"", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
        for (Schedule s : list) {
            rows.add(Arrays.asList(
                    days[s.getDayOfWeek()],
                    String.format("第 %d-%d 节", s.getSectionStart(), s.getSectionEnd()),
                    s.getCourseName(),
                    s.getTeacherName(),
                    s.getClassName() != null ? s.getClassName() : "自由选课",
                    s.getClassroom(),
                    s.getCampus()
            ));
        }
        ConsoleUtil.printTable(headers, rows);
        ConsoleUtil.pause();
    }

    private void arrangeCourseSchedule() {
        System.out.println("---- 排课安排 ----");
        List<TeachingPlan> plans = academicService.listAllTeachingPlans();
        if (plans.isEmpty()) {
            ConsoleUtil.printError("请先添加教学计划授课安排！");
            ConsoleUtil.pause();
            return;
        }

        List<String> items = new ArrayList<>();
        for (int i = 0; i < plans.size(); i++) {
            TeachingPlan tp = plans.get(i);
            items.add(String.format("[%d] 学期:%s | %s | %s", i + 1, tp.getSemester(), tp.getCourseName(), tp.getTeacherName()));
        }
        ConsoleUtil.printMenu("请选择要排课的授课计划", items);
        int planChoice = ConsoleUtil.readChoice("选择", plans.size());
        if (planChoice == 0) return;
        TeachingPlan plan = plans.get(planChoice - 1);

        int dayOfWeek = ConsoleUtil.readInt("星期几上课 (1=周一, ..., 7=周日)", 1, 7);
        int start = ConsoleUtil.readInt("上课开始节次 (1-12)", 1, 12);
        int end = ConsoleUtil.readInt("上课结束节次 (" + start + "-12)", start, 12);
        String classroom = ConsoleUtil.readLine("教室 (如: 教三-301)", false);
        String campus = ConsoleUtil.readLine("校区 (如: 主校区/东校区)", false);

        Schedule s = new Schedule();
        s.setTeachingPlanId(plan.getId());
        s.setDayOfWeek(dayOfWeek);
        s.setSectionStart(start);
        s.setSectionEnd(end);
        s.setClassroom(classroom);
        s.setCampus(campus);

        try {
            academicService.addSchedule(s);
            ConsoleUtil.printSuccess("排课添加成功！已通过系统冲突检测。");
        } catch (Exception e) {
            ConsoleUtil.printError("排课失败！检测到时段冲突: " + e.getMessage());
        }
        ConsoleUtil.pause();
    }

    private void removeCourseSchedule() {
        System.out.println("---- 删除排课时段 ----");
        String semester = ConsoleUtil.readLine("学期", false);
        Integer classId = selectClass();
        if (classId == null) return;

        List<Schedule> list = academicService.listSchedulesByClass(classId, semester);
        if (list.isEmpty()) {
            ConsoleUtil.printError("该学期该班级暂无课表安排");
            ConsoleUtil.pause();
            return;
        }

        List<String> items = new ArrayList<>();
        String[] days = {"", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
        for (int i = 0; i < list.size(); i++) {
            Schedule s = list.get(i);
            items.add(String.format("[%d] %s 第%d-%d节 | %s | %s | %s", i + 1, days[s.getDayOfWeek()], s.getSectionStart(), s.getSectionEnd(), s.getCourseName(), s.getClassroom(), s.getCampus()));
        }
        ConsoleUtil.printMenu("请选择要删除的课表时段", items);
        int choice = ConsoleUtil.readChoice("选择", list.size());
        if (choice == 0) return;

        Schedule target = list.get(choice - 1);
        if (ConsoleUtil.confirm("确认要删除该节排课？")) {
            academicService.deleteSchedule(target.getId());
            ConsoleUtil.printSuccess("排课已成功删除。");
        }
        ConsoleUtil.pause();
    }


    // --- 7. NOTICE/ANNOUNCEMENTS ---
    private void manageNotices(Integer publisherUserId) {
        while (true) {
            ConsoleUtil.clearScreen();
            List<String> items = Arrays.asList(
                    "[1] 查看全部公告",
                    "[2] 发布新公告",
                    "[3] 撤回/取消公告",
                    "[0] 返回上级"
            );
            ConsoleUtil.printMenu("公告通知管理", items);
            int choice = ConsoleUtil.readChoice("请选择操作", 3);
            if (choice == 0) break;
            switch (choice) {
                case 1: viewAllNotices(); break;
                case 2: publishNewNotice(publisherUserId); break;
                case 3: recallNotice(); break;
            }
        }
    }

    private void viewAllNotices() {
        List<Notice> notices = educationService.listAllNotices();
        System.out.println("---- 系统历史公告 ----");
        List<String> headers = Arrays.asList("ID", "发布时间", "公告标题", "接收群体", "是否置顶", "状态");
        List<List<String>> rows = new ArrayList<>();
        for (Notice n : notices) {
            rows.add(Arrays.asList(
                    n.getId().toString(),
                    n.getCreatedAt().toString(),
                    n.getTitle(),
                    n.getTargetRole(),
                    n.getIsTop() == 1 ? "置顶" : "普通",
                    n.getStatus() == 1 ? "正常" : "已撤回"
            ));
        }
        ConsoleUtil.printTable(headers, rows);
        ConsoleUtil.pause();
    }

    private void publishNewNotice(Integer publisherUserId) {
        System.out.println("---- 发布新公告 ----");
        String title = ConsoleUtil.readLine("公告标题", false);
        String content = ConsoleUtil.readLine("公告内容 (支持多行, 输入 \\n 可作为换行)", false);
        content = content.replace("\\n", "\n");
        System.out.println("请选择目标群体:\n  [1] 全体可见 (ALL)\n  [2] 仅教师可见 (TEACHER)\n  [3] 仅学生可见 (STUDENT)");
        int targetChoice = ConsoleUtil.readChoice("选择接收群体", 3);
        String targetRole = "ALL";
        if (targetChoice == 2) targetRole = "TEACHER";
        else if (targetChoice == 3) targetRole = "STUDENT";

        boolean isTop = ConsoleUtil.confirm("是否置顶该公告？");

        Notice n = new Notice();
        n.setTitle(title);
        n.setContent(content);
        n.setPublisherId(publisherUserId);
        n.setTargetRole(targetRole);
        n.setIsTop(isTop ? 1 : 0);
        n.setStatus(1);

        try {
            educationService.publishNotice(n);
            ConsoleUtil.printSuccess("公告发布成功！");
        } catch (Exception e) {
            ConsoleUtil.printError("发布失败: " + e.getMessage());
        }
        ConsoleUtil.pause();
    }

    private void recallNotice() {
        System.out.println("---- 撤回公告 ----");
        List<Notice> notices = educationService.listAllNotices();
        List<String> items = new ArrayList<>();
        for (int i = 0; i < notices.size(); i++) {
            Notice n = notices.get(i);
            items.add(String.format("[%d] [%s] %s (%s)", i + 1, n.getStatus() == 1 ? "正常" : "已撤", n.getTitle(), n.getTargetRole()));
        }
        ConsoleUtil.printMenu("请选择要撤回的公告", items);
        int choice = ConsoleUtil.readChoice("选择", notices.size());
        if (choice == 0) return;

        Notice selected = notices.get(choice - 1);
        if (selected.getStatus() == 0) {
            ConsoleUtil.printError("该公告已经被撤回了！");
            ConsoleUtil.pause();
            return;
        }

        if (ConsoleUtil.confirm("确定撤回公告 [" + selected.getTitle() + "] 吗？这会让接收群体再也看不到此公告。")) {
            educationService.recallNotice(selected.getId());
            ConsoleUtil.printSuccess("公告已成功撤回。");
        }
        ConsoleUtil.pause();
    }


    // --- 8. ACCOUNTS & SAFETY ---
    private void manageAccounts() {
        while (true) {
            ConsoleUtil.clearScreen();
            List<String> items = Arrays.asList(
                    "[1] 查看全部系统账户",
                    "[2] 启用/禁用账户",
                    "[3] 重置账户密码",
                    "[4] 解除账户登录限制",
                    "[0] 返回上级"
            );
            ConsoleUtil.printMenu("账户安全与权限管理菜单", items);
            int choice = ConsoleUtil.readChoice("请选择操作", 4);
            if (choice == 0) break;
            switch (choice) {
                case 1: viewAllAccounts(); break;
                case 2: toggleAccountStatus(); break;
                case 3: resetAccountPassword(); break;
                case 4: unlockAccount(); break;
            }
        }
    }

    private void viewAllAccounts() {
        List<User> users = userService.listAllUsers();
        System.out.println("---- 账户安全管理中心 ----");
        List<String> headers = Arrays.asList("ID", "用户名", "真实姓名", "角色权限", "账户状态", "失败锁定截止日期");
        List<List<String>> rows = new ArrayList<>();
        for (User u : users) {
            rows.add(Arrays.asList(
                    u.getId().toString(),
                    u.getUsername(),
                    u.getRealName(),
                    u.getRole().name(),
                    u.getStatus() == 1 ? "启用" : "禁用",
                    u.getLockUntil() != null ? u.getLockUntil().toString() : "无"
            ));
        }
        ConsoleUtil.printTable(headers, rows);
        ConsoleUtil.pause();
    }

    private void toggleAccountStatus() {
        System.out.println("---- 启用/禁用账户 ----");
        List<User> users = userService.listAllUsers();
        List<String> items = new ArrayList<>();
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            items.add(String.format("[%d] 用户:%s | 姓名:%s | 角色:%s | 状态:%s", i + 1, u.getUsername(), u.getRealName(), u.getRole().name(), u.getStatus() == 1 ? "启用" : "禁用"));
        }
        ConsoleUtil.printMenu("请选择目标账户", items);
        int choice = ConsoleUtil.readChoice("选择", users.size());
        if (choice == 0) return;

        User selected = users.get(choice - 1);
        if (selected.getUsername().equals("admin")) {
            ConsoleUtil.printError("无法禁用超级管理员！");
            ConsoleUtil.pause();
            return;
        }

        int targetStatus = selected.getStatus() == 1 ? 0 : 1;
        String statusStr = targetStatus == 1 ? "启用" : "禁用";
        if (ConsoleUtil.confirm("确认要 " + statusStr + " 账户 " + selected.getUsername() + " 吗？")) {
            userService.toggleUserStatus(selected.getId(), targetStatus);
            ConsoleUtil.printSuccess("操作成功！该账户已" + statusStr);
        }
        ConsoleUtil.pause();
    }

    private void resetAccountPassword() {
        System.out.println("---- 重置账户密码 ----");
        List<User> users = userService.listAllUsers();
        List<String> items = new ArrayList<>();
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            items.add(String.format("[%d] 用户:%s | 姓名:%s | 角色:%s", i + 1, u.getUsername(), u.getRealName(), u.getRole().name()));
        }
        ConsoleUtil.printMenu("请选择目标账户", items);
        int choice = ConsoleUtil.readChoice("选择", users.size());
        if (choice == 0) return;

        User selected = users.get(choice - 1);
        String newPassword = ConsoleUtil.readLine("请输入新密码", false);
        if (ConsoleUtil.confirm("确认将账户 [" + selected.getUsername() + "] 密码重置？")) {
            userService.resetPassword(selected.getId(), newPassword);
            ConsoleUtil.printSuccess("密码重置成功！");
        }
        ConsoleUtil.pause();
    }

    private void unlockAccount() {
        System.out.println("---- 解除账户登录限制 ----");
        List<User> users = userService.listAllUsers();
        java.util.List<User> lockedUsers = new java.util.ArrayList<>();
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
        for (User u : users) {
            if ((u.getLockUntil() != null && u.getLockUntil().after(now)) || u.getFailedAttempts() > 0) {
                lockedUsers.add(u);
            }
        }

        if (lockedUsers.isEmpty()) {
            ConsoleUtil.printSuccess("当前没有被锁定或存在登录失败记录的账户。");
            ConsoleUtil.pause();
            return;
        }

        java.util.List<String> items = new java.util.ArrayList<>();
        for (int i = 0; i < lockedUsers.size(); i++) {
            User u = lockedUsers.get(i);
            String lockStatus = (u.getLockUntil() != null && u.getLockUntil().after(now)) ?
                    "已锁定 (至 " + u.getLockUntil() + ")" : "失败次数: " + u.getFailedAttempts();
            items.add(String.format("[%d] 账户:%s | 姓名:%s | 角色:%s | 状态: %s",
                    i + 1, u.getUsername(), u.getRealName(), u.getRole().name(), lockStatus));
        }

        ConsoleUtil.printMenu("请选择要解除限制的账户", items);
        int choice = ConsoleUtil.readChoice("选择", lockedUsers.size());
        if (choice == 0) return;

        User selected = lockedUsers.get(choice - 1);
        if (ConsoleUtil.confirm("确认要清除账户 [" + selected.getUsername() + "] 的登录限制吗？")) {
            try {
                userService.unlockUser(selected.getId());
                ConsoleUtil.printSuccess("解锁成功！账户限制已清除。");
            } catch (Exception e) {
                ConsoleUtil.printError("解锁失败: " + e.getMessage());
            }
        }
        ConsoleUtil.pause();
    }


    // --- 9. DATA STATISTICS & REPORTS ---
    private void manageStatistics() {
        while (true) {
            ConsoleUtil.clearScreen();
            List<String> items = Arrays.asList(
                    "[1] 成绩分布统计 (文本柱状图)",
                    "[2] 班级成绩对比分析",
                    "[3] 成绩趋势分析",
                    "[4] 考勤月度报表",
                    "[5] 统计结果导出 CSV",
                    "[0] 返回上级"
            );
            ConsoleUtil.printMenu("数据统计与报表中心", items);
            int choice = ConsoleUtil.readChoice("请选择操作", 5);
            if (choice == 0) break;
            switch (choice) {
                case 1: viewScoreDistribution(); break;
                case 2: viewClassComparison(); break;
                case 3: viewScoreTrend(); break;
                case 4: viewAttendanceReport(); break;
                case 5: exportStatisticsCSV(); break;
            }
        }
    }

    private void viewScoreDistribution() {
        System.out.println("---- 成绩分布统计 ----");
        System.out.println("请选择教学计划:");
        List<com.sms.entity.TeachingPlan> plans = academicService.listAllTeachingPlans();
        if (plans.isEmpty()) {
            ConsoleUtil.printError("暂无教学计划数据");
            ConsoleUtil.pause();
            return;
        }
        List<String> items = new ArrayList<>();
        for (int i = 0; i < plans.size(); i++) {
            com.sms.entity.TeachingPlan tp = plans.get(i);
            items.add(String.format("[%d] %s | %s | %s", i + 1, tp.getSemester(), tp.getCourseName(), tp.getTeacherName()));
        }
        ConsoleUtil.printMenu("选择教学计划", items);
        int choice = ConsoleUtil.readChoice("选择", plans.size());
        if (choice == 0) return;

        Integer planId = plans.get(choice - 1).getId();
        Map<String, Object> stats = educationService.calculatePlanScoreStats(planId);
        int graded = (int) stats.get("graded");
        if (graded == 0) {
            ConsoleUtil.printError("该课程暂无已录入的成绩");
            ConsoleUtil.pause();
            return;
        }

        List<String> labels = Arrays.asList("90-100 (A)", "80-89  (B)", "70-79  (C)", "60-69  (D)", "0-59   (F)");
        List<Integer> values = Arrays.asList(
                (int) stats.get("catA"), (int) stats.get("catB"),
                (int) stats.get("catC"), (int) stats.get("catD"), (int) stats.get("catF"));
        List<Double> pcts = Arrays.asList(
                (double) stats.get("pctA"), (double) stats.get("pctB"),
                (double) stats.get("pctC"), (double) stats.get("pctD"), (double) stats.get("pctF"));

        ConsoleUtil.printBarChart("成绩分布直方图 (共录入 " + graded + "/" + stats.get("total") + " 人)", labels, values, pcts);
        System.out.printf("平均分: %.1f  最高分: %.1f  最低分: %.1f  及格率: %.1f%%  优秀率: %.1f%%\n",
                (double) stats.get("avg"), (double) stats.get("max"), (double) stats.get("min"),
                (double) stats.get("passRate"), (double) stats.get("excelRate"));
        ConsoleUtil.pause();
    }

    private void viewClassComparison() {
        System.out.println("---- 班级成绩对比分析 ----");
        Integer courseId = selectCourse();
        if (courseId == null) return;
        String semester = ConsoleUtil.readLine("请输入学期 (如: 2025-2026-1, 回车查看全部)");

        List<Map<String, Object>> results = educationService.compareClassScores(courseId, semester);
        if (results.isEmpty()) {
            ConsoleUtil.printError("暂无该课程的成绩对比数据");
            ConsoleUtil.pause();
            return;
        }

        System.out.println("---- 班级对比: " + results.get(0).get("courseName") + " (" + (semester.isEmpty() ? "全部学期" : semester) + ") ----");
        List<String> headers = Arrays.asList("班级", "学期", "已录人数", "平均分", "最高分", "最低分", "及格率", "优秀率");
        List<List<String>> rows = new ArrayList<>();
        for (Map<String, Object> r : results) {
            rows.add(Arrays.asList(
                    (String) r.get("className"),
                    (String) r.get("semester"),
                    String.valueOf(r.get("graded")),
                    String.format("%.1f", (double) r.get("avg")),
                    String.format("%.1f", (double) r.get("max")),
                    String.format("%.1f", (double) r.get("min")),
                    String.format("%.1f%%", (double) r.get("passRate")),
                    String.format("%.1f%%", (double) r.get("excelRate"))
            ));
        }
        ConsoleUtil.printTable(headers, rows);
        ConsoleUtil.pause();
    }

    private void viewScoreTrend() {
        System.out.println("---- 成绩趋势分析 ----");
        Integer classId = selectClass();
        if (classId == null) return;
        Integer courseId = selectCourse();
        if (courseId == null) return;

        List<Map<String, Object>> trends = educationService.getScoreTrend(classId, courseId);
        if (trends.isEmpty()) {
            ConsoleUtil.printError("暂无该班级该课程的历史成绩数据");
            ConsoleUtil.pause();
            return;
        }

        System.out.println("---- 成绩趋势 ----");
        List<String> headers = Arrays.asList("学期", "已录人数", "平均分", "及格率");
        List<List<String>> rows = new ArrayList<>();
        for (Map<String, Object> t : trends) {
            rows.add(Arrays.asList(
                    (String) t.get("semester"),
                    String.valueOf(t.get("graded")),
                    String.format("%.1f", (double) t.get("avg")),
                    String.format("%.1f%%", (double) t.get("passRate"))
            ));
        }
        ConsoleUtil.printTable(headers, rows);

        // 绘制平均分趋势柱状图
        List<String> labels = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        List<Double> pcts = new ArrayList<>();
        for (Map<String, Object> t : trends) {
            labels.add((String) t.get("semester"));
            int avgInt = (int) Math.round((double) t.get("avg"));
            values.add(avgInt);
            pcts.add((double) t.get("avg"));
        }
        ConsoleUtil.printBarChart("平均分变化趋势", labels, values, pcts);
        ConsoleUtil.pause();
    }

    private void viewAttendanceReport() {
        System.out.println("---- 考勤月度报表 ----");
        String semester = ConsoleUtil.readLine("请输入学期 (如: 2025-2026-1)");

        List<Map<String, Object>> report = educationService.getClassAttendanceReport(semester);
        if (report.isEmpty()) {
            ConsoleUtil.printError("暂无考勤数据");
            ConsoleUtil.pause();
            return;
        }

        System.out.println("---- 考勤汇总报表 (" + semester + ") ----");
        List<String> headers = Arrays.asList("班级", "总考勤次数", "出勤次数", "缺勤次数", "出勤率", "预警");
        List<List<String>> rows = new ArrayList<>();
        for (Map<String, Object> r : report) {
            rows.add(Arrays.asList(
                    (String) r.get("className"),
                    String.valueOf(r.get("total")),
                    String.valueOf(r.get("present")),
                    String.valueOf(r.get("absent")),
                    String.format("%.1f%%", (double) r.get("rate")),
                    (boolean) r.get("warning") ? "⚠ 缺勤预警" : "正常"
            ));
        }
        ConsoleUtil.printTable(headers, rows);
        ConsoleUtil.pause();
    }

    private void exportStatisticsCSV() {
        System.out.println("---- 导出统计结果 ----");
        System.out.println("  [1] 导出成绩分布统计");
        System.out.println("  [2] 导出班级对比数据");
        System.out.println("  [3] 导出考勤报表");
        System.out.println("  [0] 返回");
        int choice = ConsoleUtil.readChoice("选择导出类型", 3);
        if (choice == 0) return;

        String path = ConsoleUtil.readLine("请输入导出文件路径 (如: data/stats.csv)");
        try {
            List<String> headers;
            List<List<String>> rows = new ArrayList<>();

            if (choice == 1) {
                Integer courseId = selectCourse();
                if (courseId == null) return;
                String semester = ConsoleUtil.readLine("学期 (回车全部)");
                List<Map<String, Object>> results = educationService.compareClassScores(courseId, semester);
                headers = Arrays.asList("班级", "学期", "平均分", "最高分", "最低分", "及格率", "优秀率");
                for (Map<String, Object> r : results) {
                    rows.add(Arrays.asList(
                            (String) r.get("className"), (String) r.get("semester"),
                            String.format("%.1f", (double) r.get("avg")),
                            String.format("%.1f", (double) r.get("max")),
                            String.format("%.1f", (double) r.get("min")),
                            String.format("%.1f%%", (double) r.get("passRate")),
                            String.format("%.1f%%", (double) r.get("excelRate"))
                    ));
                }
            } else if (choice == 2) {
                headers = Arrays.asList("班级", "学期", "平均分", "及格率");
                Integer classId = selectClass();
                if (classId == null) return;
                Integer courseId = selectCourse();
                if (courseId == null) return;
                List<Map<String, Object>> trends = educationService.getScoreTrend(classId, courseId);
                for (Map<String, Object> t : trends) {
                    rows.add(Arrays.asList(
                            (String) t.get("semester"), String.valueOf(t.get("graded")),
                            String.format("%.1f", (double) t.get("avg")),
                            String.format("%.1f%%", (double) t.get("passRate"))
                    ));
                }
            } else {
                String semester = ConsoleUtil.readLine("学期");
                List<Map<String, Object>> report = educationService.getClassAttendanceReport(semester);
                headers = Arrays.asList("班级", "总考勤", "出勤", "缺勤", "出勤率", "预警");
                for (Map<String, Object> r : report) {
                    rows.add(Arrays.asList(
                            (String) r.get("className"), String.valueOf(r.get("total")),
                            String.valueOf(r.get("present")), String.valueOf(r.get("absent")),
                            String.format("%.1f%%", (double) r.get("rate")),
                            (boolean) r.get("warning") ? "是" : "否"
                    ));
                }
            }

            educationService.exportStatisticsToCSV(headers, rows, path);
            ConsoleUtil.printSuccess("统计结果已导出至: " + path);
        } catch (Exception e) {
            ConsoleUtil.printError("导出失败: " + e.getMessage());
        }
        ConsoleUtil.pause();
    }

    // --- SELECT HELPER METHODS ---
    private Integer selectDepartment() {
        List<Department> list = adminService.listAllDepartments();
        if (list.isEmpty()) {
            ConsoleUtil.printError("暂无院系数据");
            return null;
        }
        List<String> items = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            items.add(String.format("[%d] %s (%s)", i + 1, list.get(i).getDeptName(), list.get(i).getDeptNo()));
        }
        ConsoleUtil.printMenu("选择院系", items);
        int choice = ConsoleUtil.readChoice("选择院系", list.size());
        if (choice == 0) return null;
        return list.get(choice - 1).getId();
    }

    private Integer selectMajor() {
        List<Major> list = adminService.listAllMajors();
        if (list.isEmpty()) {
            ConsoleUtil.printError("暂无专业数据");
            return null;
        }
        List<String> items = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            items.add(String.format("[%d] %s (%s)", i + 1, list.get(i).getMajorName(), list.get(i).getMajorNo()));
        }
        ConsoleUtil.printMenu("选择专业", items);
        int choice = ConsoleUtil.readChoice("选择专业", list.size());
        if (choice == 0) return null;
        return list.get(choice - 1).getId();
    }

    private Integer selectClass() {
        List<Clazz> list = adminService.listAllClazzes();
        if (list.isEmpty()) {
            ConsoleUtil.printError("暂无班级数据");
            return null;
        }
        List<String> items = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            items.add(String.format("[%d] %s (%s)", i + 1, list.get(i).getClassName(), list.get(i).getClassNo()));
        }
        ConsoleUtil.printMenu("选择班级", items);
        int choice = ConsoleUtil.readChoice("选择班级", list.size());
        if (choice == 0) return null;
        return list.get(choice - 1).getId();
    }

    private Integer selectTeacher() {
        List<Teacher> list = adminService.listAllTeachers();
        if (list.isEmpty()) {
            ConsoleUtil.printError("暂无教师数据");
            return null;
        }
        List<String> items = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            items.add(String.format("[%d] %s (%s) | %s", i + 1, list.get(i).getName(), list.get(i).getTeacherNo(), list.get(i).getTitle()));
        }
        ConsoleUtil.printMenu("选择教师", items);
        int choice = ConsoleUtil.readChoice("选择教师", list.size());
        if (choice == 0) return null;
        return list.get(choice - 1).getId();
    }

    private Integer selectCourse() {
        List<Course> list = academicService.listAllCourses();
        if (list.isEmpty()) {
            ConsoleUtil.printError("暂无课程数据");
            return null;
        }
        List<String> items = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            items.add(String.format("[%d] %s (%s) | 学分:%.1f | %s", i + 1, list.get(i).getCourseName(), list.get(i).getCourseNo(), list.get(i).getCredit(), list.get(i).getType()));
        }
        ConsoleUtil.printMenu("选择课程", items);
        int choice = ConsoleUtil.readChoice("选择课程", list.size());
        if (choice == 0) return null;
        return list.get(choice - 1).getId();
    }
}
