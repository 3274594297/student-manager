USE student_manager_db;

-- 1. user 用户表数据
-- 密码默认明文均为：admin -> admin123, teacher -> teacher123, student -> student123
INSERT INTO user (id, username, password_hash, salt, role, real_name, status) VALUES
(1, 'admin', 'd948d1acf15258835b23d7329add0e2b', 'salt123', 'ADMIN', '系统管理员', 1),
(2, 'teacher1', 'bc86e9fbd3c5ac12f5c3237b255034f8', 'salt456', 'TEACHER', '王建国', 1),
(3, 'teacher2', 'bc86e9fbd3c5ac12f5c3237b255034f8', 'salt456', 'TEACHER', '刘秀英', 1),
(4, 'teacher3', 'bc86e9fbd3c5ac12f5c3237b255034f8', 'salt456', 'TEACHER', '张爱华', 1),
(5, 'teacher4', 'bc86e9fbd3c5ac12f5c3237b255034f8', 'salt456', 'TEACHER', '赵文博', 1),
(6, 'student1', 'b36e6fd615a046a6ff9b5ccf75036500', 'salt789', 'STUDENT', '李明', 1),
(7, 'student2', 'b36e6fd615a046a6ff9b5ccf75036500', 'salt789', 'STUDENT', '张强', 1),
(8, 'student3', 'b36e6fd615a046a6ff9b5ccf75036500', 'salt789', 'STUDENT', '王芳', 1),
(9, 'student4', 'b36e6fd615a046a6ff9b5ccf75036500', 'salt789', 'STUDENT', '赵敏', 1),
(10, 'student5', 'b36e6fd615a046a6ff9b5ccf75036500', 'salt789', 'STUDENT', '陈龙', 1),
(11, 'student6', 'b36e6fd615a046a6ff9b5ccf75036500', 'salt789', 'STUDENT', '徐静', 1),
(12, 'student7', 'b36e6fd615a046a6ff9b5ccf75036500', 'salt789', 'STUDENT', '周杰', 1),
(13, 'student8', 'b36e6fd615a046a6ff9b5ccf75036500', 'salt789', 'STUDENT', '林静', 1),
(14, 'student9', 'b36e6fd615a046a6ff9b5ccf75036500', 'salt789', 'STUDENT', '吴磊', 1),
(15, 'student10', 'b36e6fd615a046a6ff9b5ccf75036500', 'salt789', 'STUDENT', '郑洁', 1),
(16, 'student11', 'b36e6fd615a046a6ff9b5ccf75036500', 'salt789', 'STUDENT', '孙越', 1),
(17, 'student12', 'b36e6fd615a046a6ff9b5ccf75036500', 'salt789', 'STUDENT', '马飞', 1),
(18, 'student13', 'b36e6fd615a046a6ff9b5ccf75036500', 'salt789', 'STUDENT', '朱莉', 1),
(19, 'student14', 'b36e6fd615a046a6ff9b5ccf75036500', 'salt789', 'STUDENT', '何伟', 1),
(20, 'student15', 'b36e6fd615a046a6ff9b5ccf75036500', 'salt789', 'STUDENT', '郭涛', 1),
(21, 'student16', 'b36e6fd615a046a6ff9b5ccf75036500', 'salt789', 'STUDENT', '梁静', 1);

-- 2. department 院系表数据
-- 优化后包含 2 个院系
INSERT INTO department (id, dept_no, dept_name) VALUES
(1, 'CS', '计算机科学与技术学院'),
(2, 'MATH', '数学科学学院');

-- 3. major 专业表数据
-- 优化后包含 2 个专业 (每个院系各 1 个专业)
INSERT INTO major (id, major_no, major_name, dept_id, duration_years) VALUES
(1, 'CS01', '计算机科学与技术', 1, 4),
(2, 'MA01', '应用数学', 2, 4);

-- 4. teacher 教师表数据
INSERT INTO teacher (id, user_id, teacher_no, name, gender, phone, email, title, department_id, status) VALUES
(1, 2, 'T2001', '王建国', '男', '13800010001', 'wangjg@school.edu.cn', '教授', 1, 1),
(2, 3, 'T2002', '刘秀英', '女', '13800010002', 'liuxy@school.edu.cn', '副教授', 1, 1),
(3, 4, 'T2003', '张爱华', '女', '13800010003', 'zhangah@school.edu.cn', '讲师', 2, 1),
(4, 5, 'T2004', '赵文博', '男', '13800010004', 'zhaowb@school.edu.cn', '教授', 2, 1);

-- 5. class 班级表数据
-- 优化后包含 4 个班级 (每个专业各 2 个班级)
INSERT INTO class (id, class_no, class_name, major_id, enroll_year, head_teacher_id) VALUES
(1, 'C2301', '计算机2301班', 1, 2023, 1),
(2, 'C2302', '计算机2302班', 1, 2023, 2),
(3, 'C2303', '数学2301班', 2, 2023, 3),
(4, 'C2304', '数学2302班', 2, 2023, 4);

-- 6. student 学生表数据
-- 共 16 名学生，平摊到 4 个班级中，每班 4 名学生
INSERT INTO student (id, user_id, student_no, name, gender, birth_date, phone, email, address, enroll_date, class_id, status) VALUES
-- 计算机2301班 (ID = 1)
(1, 6, 'S230101', '李明', '男', '2005-05-12', '13900010001', 'liming@school.edu.cn', '北京市朝阳区', '2023-09-01', 1, 1),
(2, 7, 'S230102', '张强', '男', '2005-08-20', '13900010002', 'zhangqiang@school.edu.cn', '上海市浦东新区', '2023-09-01', 1, 1),
(3, 8, 'S230103', '王芳', '女', '2005-02-15', '13900010003', 'wangfang@school.edu.cn', '广州市天河区', '2023-09-01', 1, 1),
(4, 9, 'S230104', '赵敏', '女', '2005-11-30', '13900010004', 'zhaomin@school.edu.cn', '深圳市南山区', '2023-09-01', 1, 1),
-- 计算机2302班 (ID = 2)
(5, 10, 'S230105', '陈龙', '男', '2005-04-05', '13900010005', 'chenlong@school.edu.cn', '杭州市西湖区', '2023-09-01', 2, 1),
(6, 11, 'S230106', '徐静', '女', '2005-09-18', '13900010006', 'xujing@school.edu.cn', '成都市武侯区', '2023-09-01', 2, 1),
(7, 12, 'S230107', '周杰', '男', '2005-07-24', '13900010007', 'zhoujie@school.edu.cn', '南京市玄武区', '2023-09-01', 2, 1),
(8, 13, 'S230108', '林静', '女', '2005-12-10', '13900010008', 'linjing@school.edu.cn', '武汉市洪山区', '2023-09-01', 2, 1),
-- 数学2301班 (ID = 3)
(9, 14, 'S230201', '吴磊', '男', '2005-03-14', '13900020001', 'wulei@school.edu.cn', '西安市雁塔区', '2023-09-01', 3, 1),
(10, 15, 'S230202', '郑洁', '女', '2005-06-22', '13900020002', 'zhengjie@school.edu.cn', '济南市历下区', '2023-09-01', 3, 1),
(11, 16, 'S230203', '孙越', '男', '2005-10-05', '13900020003', 'sunyue@school.edu.cn', '郑州市金水区', '2023-09-01', 3, 1),
(12, 17, 'S230204', '马飞', '男', '2005-01-29', '13900020004', 'mafei@school.edu.cn', '合肥市蜀山区', '2023-09-01', 3, 1),
-- 数学2302班 (ID = 4)
(13, 18, 'S230205', '朱莉', '女', '2005-08-16', '13900020005', 'zhuli@school.edu.cn', '长沙市岳麓区', '2023-09-01', 4, 1),
(14, 19, 'S230206', '何伟', '男', '2005-09-03', '13900020006', 'hewei@school.edu.cn', '南昌市东湖区', '2023-09-01', 4, 1),
(15, 20, 'S230207', '郭涛', '男', '2005-04-11', '13900020007', 'guotao@school.edu.cn', '福州市鼓楼区', '2023-09-01', 4, 1),
(16, 21, 'S230208', '梁静', '女', '2005-11-25', '13900020008', 'liangjing@school.edu.cn', '厦门市思明区', '2023-09-01', 4, 1);

-- 7. course 课程表数据
INSERT INTO course (id, course_no, course_name, credit, hours, type) VALUES
(1, 'CO101', '高等数学', 5.0, 80, '必修'),
(2, 'CO102', 'Java程序设计', 4.0, 64, '必修'),
(3, 'CO103', '数据库系统原理', 3.5, 56, '必修'),
(4, 'CO104', '面向对象课程设计', 2.0, 32, '选修'),
(5, 'CO105', '音乐欣赏', 1.5, 24, '公选');

-- 8. teaching_plan 教学计划表数据 (定义"谁在哪个学期教哪个班的哪门课")
INSERT INTO teaching_plan (id, course_id, teacher_id, class_id, semester, max_students, current_students) VALUES
-- 必修课
(1, 1, 3, 1, '2025-2026-1', 60, 4), -- 高等数学，张爱华，计算机2301班
(2, 2, 1, 1, '2025-2026-1', 60, 4), -- Java程序设计，王建国，计算机2301班
(3, 3, 2, 2, '2025-2026-1', 60, 4), -- 数据库系统原理，刘秀英，计算机2302班
(4, 1, 4, 3, '2025-2026-1', 60, 4), -- 高等数学，赵文博，数学2301班
-- 选修课 (选修课class_id可为空)
(5, 4, 1, NULL, '2025-2026-1', 30, 4), -- 面向对象课程设计，王建国，选课模式
(6, 5, 2, NULL, '2025-2026-1', 50, 4); -- 音乐欣赏，刘秀英，选课模式

-- 9. enrollment 选课表数据
INSERT INTO enrollment (id, student_id, teaching_plan_id) VALUES
-- 计划 1 (计算机2301班 全班必修数学)
(1, 1, 1), (2, 2, 1), (3, 3, 1), (4, 4, 1),
-- 计划 2 (计算机2301班 全班必修Java)
(5, 1, 2), (6, 2, 2), (7, 3, 2), (8, 4, 2),
-- 计划 3 (计算机2302班 全班必修数据库)
(9, 5, 3), (10, 6, 3), (11, 7, 3), (12, 8, 3),
-- 计划 4 (数学2301班 全班必修数学)
(13, 9, 4), (14, 10, 4), (15, 11, 4), (16, 12, 4),
-- 计划 5 (选修课 面向对象课程设计 选课学生: 1, 2, 5, 9)
(17, 1, 5), (18, 2, 5), (19, 5, 5), (20, 9, 5),
-- 计划 6 (选修课 音乐欣赏 选课学生: 3, 6, 10, 13)
(21, 3, 6), (22, 6, 6), (23, 10, 6), (24, 13, 6);

-- 10. score 成绩表数据
INSERT INTO score (enrollment_id, score, grade_level, exam_type) VALUES
-- 计划 1 期末成绩
(1, 85.0, 'B', '期末'),
(2, 92.0, 'A', '期末'),
(3, 58.0, 'F', '期末'),
(4, 74.0, 'C', '期末'),
-- 计划 2 期末成绩
(5, 95.0, 'A', '期末'),
(6, 88.0, 'B', '期末'),
(7, 76.0, 'C', '期末'),
(8, 82.0, 'B', '期末'),
-- 计划 3 期末成绩
(9, 82.0, 'B', '期末'),
(10, 68.0, 'D', '期末'),
(11, 91.0, 'A', '期末'),
(12, 85.0, 'B', '期末'),
-- 计划 4 期末成绩
(13, 78.0, 'C', '期末'),
(14, 85.0, 'B', '期末'),
(15, 90.0, 'A', '期末'),
(16, 62.0, 'D', '期末'),
-- 计划 5 (面向对象课程设计) 成绩
(17, 90.0, 'A', '期末'),
(18, 85.0, 'B', '期末'),
(19, 73.0, 'C', '期末'),
(20, NULL, NULL, '期末'), -- 未录入
-- 计划 6 (音乐欣赏) 成绩
(21, 88.0, 'B', '期末'),
(22, 91.0, 'A', '期末'),
(23, NULL, NULL, '期末'),
(24, NULL, NULL, '期末');

-- 11. schedule 排课日程表数据
INSERT INTO schedule (id, teaching_plan_id, day_of_week, section_start, section_end, classroom, campus) VALUES
(1, 1, 1, 1, 2, '教三-301', '主校区'), -- 周一 1-2节
(2, 2, 2, 3, 4, '机房-502', '主校区'), -- 周二 3-4节
(3, 3, 3, 5, 6, '教二-204', '主校区'), -- 周三 5-6节
(4, 4, 1, 3, 4, '教三-302', '主校区'), -- 周一 3-4节
(5, 5, 4, 1, 2, '机房-501', '主校区'), -- 周四 1-2节
(6, 6, 5, 7, 8, '音乐厅', '东校区');   -- 周五 7-8节

-- 12. attendance 考勤表初始数据
INSERT INTO attendance (id, student_id, teaching_plan_id, attend_date, status, remark) VALUES
(1, 1, 1, '2026-06-25', '出勤', ''),
(2, 2, 1, '2026-06-25', '出勤', ''),
(3, 3, 1, '2026-06-25', '迟到', '迟到5分钟'),
(4, 4, 1, '2026-06-25', '出勤', '');

-- 13. notice 系统公告数据
INSERT INTO notice (id, title, content, publisher_id, target_role, is_top, status) VALUES
(1, '关于期末考试安排的通知', '各位同学，期末考试定于6月30日举行，请做好复习准备。', 1, 'ALL', 1, 1),
(2, '关于选修课退选截止的通知', '本学期选修课退选通道将于周五17:00关闭。', 1, 'ALL', 0, 1),
(3, '关于提交教学总结的通知', '各位教师请在下周一前提交教学大纲和工作总结。', 1, 'TEACHER', 0, 1);

-- 14. leave_request 学生请假数据
INSERT INTO leave_request (id, student_id, start_date, end_date, reason, status, approver_id, remark) VALUES
(1, 3, '2026-06-26', '2026-06-28', '感冒发烧需要打点滴', '待审批', NULL, NULL);
