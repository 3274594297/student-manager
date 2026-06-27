# 数据库表结构设计说明文档 (student_manager_db)

本文档对本管理系统的数据库设计进行详细说明，涵盖数据库中的所有表、字段含义、主外键约束以及表与表之间的关系。

---

## 1. 实体关系图 (ER Diagram)

以下是数据库中各表的主线关系图：

```mermaid
erDiagram
    user ||--o| student : "1:1 账户"
    user ||--o| teacher : "1:1 账户"
    department ||--o{ major : "1:N 拥有专业"
    department ||--o{ teacher : "1:N 归属院系"
    major ||--o{ class : "1:N 拥有班级"
    teacher ||--o| class : "1:1 担任班主任"
    class ||--o{ student : "1:N 拥有学生"
    course ||--o{ teaching_plan : "1:N 开设教学计划"
    teacher ||--o{ teaching_plan : "1:N 授课安排"
    class ||--o{ teaching_plan : "1:N 班级开课"
    student ||--o{ enrollment : "1:N 选课记录"
    teaching_plan ||--o{ enrollment : "1:N 被选课"
    enrollment ||--o{ score : "1:N 成绩记录"
    student ||--o{ attendance : "1:N 考勤记录"
    teaching_plan ||--o{ attendance : "1:N 考勤关联"
    teaching_plan ||--o{ schedule : "1:N 排课日程"
    user ||--o{ notice : "1:N 发布公告"
    student ||--o{ leave_request : "1:N 请假申请"
```

---

## 2. 数据库表详细说明

当前库中共有 **14** 张表，各表的详细结构与作用如下：

### 1. `user` (用户账户表)
* **作用**：存储所有登录系统的用户的账号、加密密码和权限角色。
* **字段说明**：
  | 字段名 | 类型 | 约束 | 备注 |
  | :--- | :--- | :--- | :--- |
  | `id` | `INT` | `PRIMARY KEY`, `AUTO_INCREMENT` | 用户唯一ID |
  | `username` | `VARCHAR(50)` | `NOT NULL`, `UNIQUE` | 登录账号 (如学号、工号) |
  | `password_hash`| `VARCHAR(64)` | `NOT NULL` | MD5盐值加密后的密码密文 |
  | `salt` | `VARCHAR(32)` | `NOT NULL` | 加密盐值 |
  | `role` | `ENUM` | `NOT NULL` | 角色：`'ADMIN'` (管理员), `'TEACHER'` (教师), `'STUDENT'` (学生) |
  | `real_name` | `VARCHAR(50)` | `NOT NULL` | 真实姓名 |
  | `status` | `INT` | 默认 `1` | 账号状态：`1` (启用), `0` (禁用) |
  | `failed_attempts`| `INT` | 默认 `0` | 登录失败尝试次数 (用于防暴力破解) |
  | `lock_until` | `TIMESTAMP` | 可空 | 账号锁定期限 |

---

### 2. `department` (院系表)
* **作用**：存储学校院系的基本数据。
* **字段说明**：
  | 字段名 | 类型 | 约束 | 备注 |
  | :--- | :--- | :--- | :--- |
  | `id` | `INT` | `PRIMARY KEY`, `AUTO_INCREMENT` | 院系唯一ID |
  | `dept_no` | `VARCHAR(20)` | `NOT NULL`, `UNIQUE` | 院系编号 (如 CS) |
  | `dept_name` | `VARCHAR(50)` | `NOT NULL`, `UNIQUE` | 院系名称 |

---

### 3. `major` (专业表)
* **作用**：存储院系下设的专业信息。
* **字段说明**：
  | 字段名 | 类型 | 约束 | 备注 |
  | :--- | :--- | :--- | :--- |
  | `id` | `INT` | `PRIMARY KEY`, `AUTO_INCREMENT` | 专业唯一ID |
  | `major_no` | `VARCHAR(20)` | `NOT NULL`, `UNIQUE` | 专业编号 |
  | `major_name` | `VARCHAR(50)` | `NOT NULL` | 专业名称 |
  | `dept_id` | `INT` | `FOREIGN KEY` $\rightarrow$ `department(id)` | 所属院系ID |
  | `duration_years`| `INT` | 默认 `4` | 学制年限 |

---

### 4. `teacher` (教师表)
* **作用**：记录教师的个人档案，并与其登录账户绑定。
* **字段说明**：
  | 字段名 | 类型 | 约束 | 备注 |
  | :--- | :--- | :--- | :--- |
  | `id` | `INT` | `PRIMARY KEY`, `AUTO_INCREMENT` | 教师唯一ID |
  | `user_id` | `INT` | `UNIQUE`, `FOREIGN KEY` $\rightarrow$ `user(id)` | 关联用户账户 |
  | `teacher_no` | `VARCHAR(20)` | `NOT NULL`, `UNIQUE` | 教师工号 |
  | `name` | `VARCHAR(50)` | `NOT NULL` | 姓名 |
  | `gender` | `VARCHAR(10)` | `NOT NULL` | 性别 |
  | `phone` | `VARCHAR(20)` | 可空 | 电话号码 |
  | `email` | `VARCHAR(50)` | 可空 | 电子邮箱 |
  | `title` | `VARCHAR(50)` | 可空 | 职称 (如: 教授、副教授、讲师) |
  | `department_id`| `INT` | `FOREIGN KEY` $\rightarrow$ `department(id)` | 所属院系 |
  | `status` | `INT` | 默认 `1` | 在职状态：`1` (在职), `0` (离职) |

---

### 5. `class` (班级表)
* **作用**：存储各专业的班级基本信息。
* **字段说明**：
  | 字段名 | 类型 | 约束 | 备注 |
  | :--- | :--- | :--- | :--- |
  | `id` | `INT` | `PRIMARY KEY`, `AUTO_INCREMENT` | 班级唯一ID |
  | `class_no` | `VARCHAR(20)` | `NOT NULL`, `UNIQUE` | 班级编号 |
  | `class_name` | `VARCHAR(50)` | `NOT NULL` | 班级名称 |
  | `major_id` | `INT` | `FOREIGN KEY` $\rightarrow$ `major(id)` | 所属专业 |
  | `enroll_year` | `INT` | `NOT NULL` | 入学年份 |
  | `head_teacher_id`| `INT`| `FOREIGN KEY` $\rightarrow$ `teacher(id)` | 担任班主任的教师ID |

---

### 6. `student` (学生表)
* **作用**：记录学生的个人档案，与其登录账户及班级绑定。
* **字段说明**：
  | 字段名 | 类型 | 约束 | 备注 |
  | :--- | :--- | :--- | :--- |
  | `id` | `INT` | `PRIMARY KEY`, `AUTO_INCREMENT` | 学生唯一ID |
  | `user_id` | `INT` | `UNIQUE`, `FOREIGN KEY` $\rightarrow$ `user(id)` | 关联用户账户 |
  | `student_no` | `VARCHAR(20)` | `NOT NULL`, `UNIQUE` | 学号 |
  | `name` | `VARCHAR(50)` | `NOT NULL` | 姓名 |
  | `gender` | `VARCHAR(10)` | `NOT NULL`, `CHECK (gender IN ('男', '女'))` | 性别 |
  | `birth_date` | `DATE` | `NOT NULL` | 出生日期 |
  | `phone` | `VARCHAR(20)` | 可空 | 联系电话 |
  | `email` | `VARCHAR(50)` | 可空 | 电子邮箱 |
  | `address` | `VARCHAR(100)`| 可空 | 家庭住址 |
  | `enroll_date` | `DATE` | `NOT NULL` | 入学日期 |
  | `class_id` | `INT` | `FOREIGN KEY` $\rightarrow$ `class(id)` | 绑定班级 |
  | `photo_path` | `VARCHAR(200)`| 可空 | 学生照片存储路径 |
  | `status` | `INT` | 默认 `1` | 在校状态：`1` (在学), `0` (退学/毕业) |

---

### 7. `course` (课程表)
* **作用**：存储学校开设的课程库。
* **字段说明**：
  | 字段名 | 类型 | 约束 | 备注 |
  | :--- | :--- | :--- | :--- |
  | `id` | `INT` | `PRIMARY KEY`, `AUTO_INCREMENT` | 课程唯一ID |
  | `course_no` | `VARCHAR(20)` | `NOT NULL`, `UNIQUE` | 课程编号 |
  | `course_name` | `VARCHAR(50)` | `NOT NULL` | 课程名称 |
  | `credit` | `DOUBLE` | `NOT NULL` | 学分 |
  | `hours` | `INT` | `NOT NULL` | 学时 |
  | `type` | `ENUM` | `NOT NULL` | 课程属性：`'必修'`, `'选修'`, `'公选'` |

---

### 8. `teaching_plan` (教学计划开课表)
* **作用**：定义“某老师在哪个学期教哪个班的哪门课”。
* **字段说明**：
  | 字段名 | 类型 | 约束 | 备注 |
  | :--- | :--- | :--- | :--- |
  | `id` | `INT` | `PRIMARY KEY`, `AUTO_INCREMENT` | 开课计划ID |
  | `course_id` | `INT` | `FOREIGN KEY` $\rightarrow$ `course(id)` | 关联对应课程 |
  | `teacher_id` | `INT` | `FOREIGN KEY` $\rightarrow$ `teacher(id)` | 关联授课教师 |
  | `class_id` | `INT` | 可空, `FOREIGN KEY` $\rightarrow$ `class(id)` | 班级必修课绑定此字段；自由选修/公选课此处为 `NULL` |
  | `semester` | `VARCHAR(20)` | `NOT NULL` | 授课学期 (如: `'2025-2026-1'`) |
  | `max_students` | `INT` | 默认 `60` | 选课学生人数上限 |
  | `current_students`| `INT` | 默认 `0` | 当前已选/关联的学生人数 |

> [!NOTE]
> 联合唯一索引：`(course_id, teacher_id, class_id, semester)` 保证同一学期下教师给班级教同一门课不会被重复创建。

---

### 9. `enrollment` (选课表)
* **作用**：存储学生与教学计划之间的多对多选课关联状态。
* **字段说明**：
  | 字段名 | 类型 | 约束 | 备注 |
  | :--- | :--- | :--- | :--- |
  | `id` | `INT` | `PRIMARY KEY`, `AUTO_INCREMENT` | 选课唯一ID |
  | `student_id` | `INT` | `FOREIGN KEY` $\rightarrow$ `student(id)` | 选课学生ID |
  | `teaching_plan_id`| `INT` | `FOREIGN KEY` $\rightarrow$ `teaching_plan(id)`| 关联开课教学计划ID |
  | `created_at` | `TIMESTAMP` | 默认当前时间 | 选课时间 |

> [!NOTE]
> 联合唯一索引：`(student_id, teaching_plan_id)` 保证学生针对同一门教学计划不能重复选课。

---

### 10. `score` (成绩明细表)
* **作用**：记录具体每次选课对应的期末、平时、期中等细分分数。
* **字段说明**：
  | 字段名 | 类型 | 约束 | 备注 |
  | :--- | :--- | :--- | :--- |
  | `id` | `INT` | `PRIMARY KEY`, `AUTO_INCREMENT` | 成绩明细唯一ID |
  | `enrollment_id` | `INT` | `FOREIGN KEY` $\rightarrow$ `enrollment(id)` | 关联的选课记录ID |
  | `score` | `DOUBLE` | 可空 (未录入时) | 分数值 (如: 95.0) |
  | `grade_level` | `VARCHAR(5)` | 可空 | 成绩等级制: A, B, C, D, F |
  | `exam_type` | `ENUM` | 默认 `'期末'` | 考试类型：`'平时'`, `'期中'`, `'期末'` |
  | `created_at` | `TIMESTAMP` | 默认当前时间 | 创建时间 |
  | `updated_at` | `TIMESTAMP` | 自动更新 | 更新时间 |

> [!NOTE]
> 联合唯一索引：`(enrollment_id, exam_type)` 保证某次选课对应的每种考试类型（如期末）仅能有一条成绩。

---

### 11. `attendance` (考勤表)
* **作用**：记录学生每节课程的课堂点名状态。
* **字段说明**：
  | 字段名 | 类型 | 约束 | 备注 |
  | :--- | :--- | :--- | :--- |
  | `id` | `INT` | `PRIMARY KEY`, `AUTO_INCREMENT` | 考勤唯一ID |
  | `student_id` | `INT` | `FOREIGN KEY` $\rightarrow$ `student(id)` | 学生ID |
  | `teaching_plan_id`| `INT` | `FOREIGN KEY` $\rightarrow$ `teaching_plan(id)`| 课程关联ID |
  | `attend_date` | `DATE` | `NOT NULL` | 考勤上课日期 |
  | `status` | `ENUM` | `NOT NULL` | 考勤结果：`'出勤'`, `'迟到'`, `'早退'`, `'旷课'`, `'请假'` |
  | `remark` | `VARCHAR(100)`| 可空 | 点名备注 (如迟到具体分钟) |

---

### 12. `schedule` (排课表)
* **作用**：记录每门开课计划在周几、第几节上课以及地点。
* **字段说明**：
  | 字段名 | 类型 | 约束 | 备注 |
  | :--- | :--- | :--- | :--- |
  | `id` | `INT` | `PRIMARY KEY`, `AUTO_INCREMENT` | 排课ID |
  | `teaching_plan_id`| `INT` | `FOREIGN KEY` $\rightarrow$ `teaching_plan(id)`| 开课计划关联 |
  | `day_of_week` | `INT` | 范围 1-7 | 星期几上课 |
  | `section_start` | `INT` | `NOT NULL` | 起始上课节次 (如: 第1节) |
  | `section_end` | `INT` | `NOT NULL` | 结束上课节次 (如: 第2节) |
  | `classroom` | `VARCHAR(50)` | `NOT NULL` | 教室名称 (如: 教三-301) |
  | `campus` | `VARCHAR(50)` | `NOT NULL` | 授课校区 (如: 主校区、东校区) |

---

### 13. `notice` (系统公告表)
* **作用**：存储管理员和老师发布的学校通知公告。
* **字段说明**：
  | 字段名 | 类型 | 约束 | 备注 |
  | :--- | :--- | :--- | :--- |
  | `id` | `INT` | `PRIMARY KEY`, `AUTO_INCREMENT` | 公告ID |
  | `title` | `VARCHAR(100)`| `NOT NULL` | 标题 |
  | `content` | `TEXT` | `NOT NULL` | 内容正文 |
  | `publisher_id` | `INT` | `FOREIGN KEY` $\rightarrow$ `user(id)` | 发布人用户ID |
  | `target_role` | `ENUM` | 默认 `'ALL'` | 接收对象：`'ALL'`, `'TEACHER'`, `'STUDENT'` |
  | `is_top` | `INT` | 默认 `0` | 是否置顶：`1` (置顶), `0` (普通) |
  | `status` | `INT` | 默认 `1` | 正常发布状态：`1` (展示中), `0` (已撤回) |
  | `created_at` | `TIMESTAMP` | 默认当前时间 | 发布时间 |

---

### 14. `leave_request` (学生请假表)
* **作用**：存储学生提交的请假申请及班主任审批记录。
* **字段说明**：
  | 字段名 | 类型 | 约束 | 备注 |
  | :--- | :--- | :--- | :--- |
  | `id` | `INT` | `PRIMARY KEY`, `AUTO_INCREMENT` | 请假ID |
  | `student_id` | `INT` | `FOREIGN KEY` $\rightarrow$ `student(id)` | 请假人ID |
  | `start_date` | `DATE` | `NOT NULL` | 请假开始日期 |
  | `end_date` | `DATE` | `NOT NULL` | 请假结束日期 |
  | `reason` | `VARCHAR(200)`| `NOT NULL` | 请假原因 |
  | `status` | `ENUM` | 默认 `'待审批'` | 审批状态：`'待审批'`, `'已批准'`, `'已拒绝'` |
  | `approver_id` | `INT` | 可空, `FOREIGN KEY` $\rightarrow$ `user(id)` | 审批人用户ID |
  | `approve_time` | `TIMESTAMP` | 可空 | 审批处理时间 |
  | `remark` | `VARCHAR(100)`| 可空 | 教师审批意见/备注 |
