# 智慧学生管理系统 — 业务服务接口文档 (Service API Documentation)

本文档详细记录了系统逻辑层（Service 层）的所有核心服务接口定义，包括方法名称、参数说明、返回值及业务作用，供开发团队成员协作及调用参考。

系统业务层采用分层设计，共划分为 4 个核心服务接口：
1. **[UserService](#1-userservice-用户安全与账户服务)**：负责登录验证、密码加盐加密及账户安全锁定。
2. **[AdminService](#2-adminservice-基础教务资源与人员档案服务)**：负责院系、专业、班级、教师、学生的基础档案 CRUD 及 CSV 导入导出。
3. **[AcademicService](#3-academicservice-教学计划排课与选课事务服务)**：负责制定教学计划、排课冲突校验、课表生成及学生自主选课逻辑。
4. **[EducationService](#4-educationservice-教学成效考勤请假与公告决策大屏服务)**：负责成绩录入、GPA 换算、考勤批录、请假自动联动、系统公告发布及决策分析报表。

---

## 1. `UserService` (用户安全与账户服务)
* **包路径**：`com.sms.service.UserService`
* **作用**：维护系统安全底座，处理登录流控及账号控制。

| 方法签名 | 参数说明 | 返回值 | 业务作用与说明 |
| :--- | :--- | :--- | :--- |
| `login(String username, String password)` | `username`: 用户名<br>`password`: 明文密码 | `User` (登录成功实体) | 校验账号状态和加盐密码；**连续 5 次失败触发账号锁定**，抛出 `AuthException` 异常。 |
| `changePassword(Integer userId, String oldPassword, String newPassword)` | `userId`: 账户ID<br>`oldPassword`: 旧密码<br>`newPassword`: 新密码 | `void` | 用户自主修改密码，内部需验证旧密码。 |
| `resetPassword(Integer targetUserId, String newPassword)` | `targetUserId`: 目标用户ID<br>`newPassword`: 重置新密码 | `void` | 管理员强制重置账户密码，**自动生成新 Salt 重新加密**。 |
| `toggleUserStatus(Integer targetUserId, Integer status)` | `targetUserId`: 目标ID<br>`status`: 1=激活, 0=禁用 | `void` | 锁定或解锁用户账户。被锁定的账号必须通过此操作恢复。 |
| `listAllUsers()` | 无 | `List<User>` | 获取全校所有账号的详细状态列表（用于安全大厅展示）。 |
| `registerUser(String username, String password, String realName, String roleName)` | 基础注册字段 | `User` | 注册/创建新的登录用户实体。 |

---

## 2. `AdminService` (基础教务资源与人员档案服务)
* **包路径**：`com.sms.service.AdminService`
* **作用**：教务行政基础组织档案库的管理。

### 2.1 院系、专业与行政班级
| 方法签名 | 主要参数 | 返回值 | 业务作用说明 |
| :--- | :--- | :--- | :--- |
| `addDepartment(Department dept)` | `dept`: 院系实体 | `void` | 新增教学学院/系（如：计算机学院）。 |
| `listAllDepartments()` | 无 | `List<Department>` | 获得全校所有院系。 |
| `addMajor(Major major)` | `major`: 专业实体 | `void` | 新建专业，需绑定关联院系。 |
| `listMajorsByDept(Integer deptId)` | `deptId`: 院系ID | `List<Major>` | 根据院系帅选名下专业。 |
| `addClazz(Clazz clazz)` | `clazz`: 班级实体 | `void` | 新建行政班级，需关联专业与入学年份。 |
| `listClazzesByMajor(Integer majorId)` | `majorId`: 专业ID | `List<Clazz>` | 获取某专业下的所有行政班级。 |
| `changeStudentClass(Integer studentId, TargetClassId)` | `studentId`: 学生ID<br>`targetClassId`: 新班级ID | `void` | **学生转班核心接口**。将学生迁移到新行政班级。 |

### 2.2 教师与学生档案（含 CSV 批量支持）
| 方法签名 | 主要参数 | 返回值 | 业务作用说明 |
| :--- | :--- | :--- | :--- |
| `addStudent(Student s, String user, String pass)` | `s`: 学生实体<br>`user`/`pass`: 初始登录账号/密码 | `Student` | 注册学生学籍，**并在后台自动同步生成 User 账户**。 |
| `searchStudents(String queryStr, Integer classId, Integer majorId, int page, int pageSize)` | `queryStr`: 模糊搜索词<br>`classId`: 过滤班级<br>`page`: 页码 | `List<Student>` | **物理分页条件检索接口**，每页固定10/20条数据。 |
| `countStudents(String queryStr, Integer classId, Integer majorId)` | 相同条件参数 | `int` | 配合分页，统计符合当前筛选条件的学生总数。 |
| `importStudentsFromCSV(String filePath)` | `filePath`: CSV路径 | `int` (导入成功数) | **批量导入学生**，自动跳过冲突学号，自动加密生成默认账户。 |
| `exportStudentsToCSV(String filePath)` | `filePath`: 保存路径 | `void` | 一键将全校/当前筛选的学生名册导出为标准 CSV。 |

---

## 3. `AcademicService` (教学计划排课与选课事务服务)
* **包路径**：`com.sms.service.AcademicService`
* **作用**：控制学期排课计划与选退课事务。

### 3.1 教学计划与课程字典
| 方法签名 | 主要参数 | 返回值 | 业务作用说明 |
| :--- | :--- | :--- | :--- |
| `addCourse(Course course)` | `course`: 课程定义 | `void` | 在课程库中添加一门课（如线性代数、5.0学分）。 |
| `addTeachingPlan(TeachingPlan plan)` | `plan`: 教学计划 | `void` | 绑定 **学期 + 课程 + 教师 + 行政班级**（为排课做准备）。 |
| `listAvailableElectivePlansForStudent(Integer sId, String sem)`| `sId`: 学生ID<br>`sem`: 当前学期 | `List<TeachingPlan>` | **智能过滤选课列表**，过滤掉学生已经选好的计划，返回可选修的清单。 |

### 3.2 智能排课与冲突校验
| 方法签名 | 主要参数 | 返回值 | 业务作用说明 |
| :--- | :--- | :--- | :--- |
| `addSchedule(Schedule schedule)` | `schedule`: 排课信息 | `void` | 向教学计划添加上课时间地点（星期几、第几节、教室、校区）。 |
| `checkScheduleConflict(Schedule s, String sem)` | `s`: 准备排课的信息<br>`sem`: 学期 | `void` | **核心校验算法**：检测并拦截同一学期内**教室时间冲突、教师授课冲突、班级听课冲突**，如有重叠抛出 `BusinessException`。 |
| `listSchedulesByClass(Integer classId, String sem)` | 关联过滤参数 | `List<Schedule>` | 生成行政班级的课表一览。 |
| `listSchedulesByTeacher(Integer teacherId, String sem)` | 关联过滤参数 | `List<Schedule>` | 生成任课教师的个人授课课表一览。 |

---

## 4. `EducationService` (教学成效、考勤、请假与分析大屏)
* **包路径**：`com.sms.service.EducationService`
* **作用**：成绩、日常考勤、假条管理以及全局报表统计。

### 4.1 成绩与绩点换算
| 方法签名 | 主要参数 | 返回值 | 业务作用说明 |
| :--- | :--- | :--- | :--- |
| `recordScore(Score score)` | `score`: 分数实体 | `void` | 录入或更新学生分数，**后台自动把百分制换算为 A/B/C/D/F 五级等第**。 |
| `calculateGPA(Integer studentId, String semester)` | `studentId`: 学生ID<br>`semester`: 学期 | `double` | 根据学生在某学期所有“期末”等第，依公式 $\frac{\sum(学分 \times 绩点)}{\sum 学分}$ 换算学期总 **GPA**。 |
| `calculatePlanScoreStats(Integer planId)` | `planId`: 计划ID | `Map<String, Object>` | 班级成绩大盘统计：平均分、最高、最低、及格率、优秀率及各分数段人数分布。 |

### 4.2 智能考勤与请假审批联动
| 方法签名 | 主要参数 | 返回值 | 业务作用说明 |
| :--- | :--- | :--- | :--- |
| `takeAttendance(Integer planId, Date date, List<Attendance> list)` | `planId`: 教学计划<br>`date`: 点名日期<br>`list`: 考勤列表 | `void` | **保存点名结果**。写入前自动清空原有记录，防冲突，支持覆盖更新。 |
| `listAttendanceByPlanAndDate(Integer planId, Date date)` | `date`: 可为空 | `List<Attendance>` | **查询考勤**。当 `date` 为 `null` 时直接返回该计划所有的历史考勤。 |
| `approveLeave(Integer reqId, String status, Integer appUserId, String rmk)` | `reqId`: 假条ID<br>`status`: 已批准/已拒绝 | `void` | **审批请假联动接口**：如果假条状态更新为“已批准”，系统会自动为该学生在请假时间段内**所有选修科目自动生成“请假”状态的考勤记录**。 |

### 4.3 管理员决策分析报表（模块 10）
| 方法签名 | 主要参数 | 返回值 | 业务作用说明 |
| :--- | :--- | :--- | :--- |
| `compareClassScores(Integer courseId, String sem)` | `courseId`: 课程ID | `List<Map>` | **班级对比报表**：对比各班在同一门课的平均分及格率。 |
| `getScoreTrend(Integer classId, Integer courseId)` | 关联条件 | `List<Map>` | **趋势分析报表**：统计某班在同一门课各学期分数的走势。 |
| `getClassAttendanceReport(String semester)` | `semester`: 学期 | `List<Map>` | **学风监控报表**：统计各行政班考勤，出勤率低于 **`85%`** 的班级自动标注 `warning=true` 触发缺勤预警。 |
| `exportStatisticsToCSV(List<String> hd, List<List<String>> rows, String path)` | 报表数据 | `void` | 将以上各类宏观分析报表数据一键导出至本地 CSV。 |
