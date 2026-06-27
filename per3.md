# 同学三 (Person 3) 职责与开发指南
## — 课堂考勤与请假审批系统模块 —

本文件详细阐述同学三在项目中的代码所有权、核心实现逻辑，以及在系统运行时通过终端交互可看到的操作界面。

---

## 1. 负责的代码文件列表 (代码所有权)
为了避免与其他成员的代码发生冲突，同学三在开发期间**拥有以下文件的唯一修改权**（或特定公共方法编写权）：

### 📁 实体类层 (Entity)
* [`src/com/sms/entity/Attendance.java`](file:///d:/Dev/shixun/src/com/sms/entity/Attendance.java) (考勤记录实体)
* [`src/com/sms/entity/LeaveRequest.java`](file:///d:/Dev/shixun/src/com/sms/entity/LeaveRequest.java) (请假申请实体)

### 📁 数据访问层 (DAO)
* `src/com/sms/dao/AttendanceDAO.java` 与 `AttendanceDAOImpl.java`
* `src/com/sms/dao/LeaveRequestDAO.java` 与 `LeaveRequestDAOImpl.java`

### 📁 业务服务层 (Service 共享，同学三独占的方法)
同学三与同学七、八共享 [EducationServiceImpl.java](file:///d:/Dev/shixun/src/com/sms/service/impl/EducationServiceImpl.java)，同学三**全权负责以下考勤与请假相关的核心方法**：
* `takeAttendance(Integer planId, Date date, List<Attendance> list)` (保存考勤)
* `listAttendanceByStudent(Integer studentId)` (查询学生个人考勤历史)
* `listAttendanceByPlanAndDate(Integer planId, Date date)` (查询某课程某天的考勤)
* `applyLeave(LeaveRequest request)` (学生提交请假)
* `approveLeave(Integer requestId, String status, Integer approverId, String remark)` (教师审批请假与考勤同步)

### 📁 终端控制层 (Controller 独占方法)
* **教师端** ([TeacherController.java](file:///d:/Dev/shixun/src/com/sms/controller/TeacherController.java)) 中的方法：
  * `manageAttendance()` (考勤大菜单)
  * `takeClassAttendance()` (执行课堂点名录入)
  * `viewClassAttendance()` (查看指定日期的考勤明细)
  * `manageLeaves()` (审批学生请假申请)
* **学生端** ([StudentController.java](file:///d:/Dev/shixun/src/com/sms/controller/StudentController.java)) 中的方法：
  * `viewMyAttendance()` (查看我的考勤历史与统计)
  * `manageMyLeaves()` (请假控制菜单)
  * `applyForLeave()` (提交请假申请)
  * `viewLeaveHistory()` (查看我的请假记录历史)

---

## 2. 核心业务与实现逻辑

### 2.1 课堂考勤点名（全新高效批量录入模式）
同学三负责的课堂点名流程已进行了 UX 深度优化，摆脱了传统“一个一个输入”的低效交互：
* **首选今日日期**：点名开始时询问是否是今天，按回车或输入 `y` 即可跳过打字，加快录入。
* **预载请假信息**：系统自动去考勤表中拉取该教学计划当前已存在的考勤，如果在点名前已有学生请假获得审批，其考勤记录初始化便自动设为 `"请假"`。
* **批量序号键入**：教师通过表格序号，一次性输入：
  * 旷课的序号（如 `3, 5`）
  * 迟到的序号（如 `12`）
  * 早退的序号
* **异常备注输入**：系统仅对被修改为异常状态的对应学生，单独提示输入备注。
* **覆盖写入**：保存时，后台先执行 `DELETE` 擦除该日期和课程下的所有旧考勤，再重新 `INSERT` 提交新录入的名单。

### 2.2 智能请假与考勤联动机制 (核心亮点)
这是本模块的核心联动逻辑，写在 `EducationServiceImpl.java` 的 `approveLeave` 方法中：
* **工作流**：当教师审批通过某项请假（状态变更为 `"已批准"`）后：
  1. 系统自动计算请假的起始日期（`startDate`）到结束日期（`endDate`）。
  2. 遍历该请假学生已选报的所有教学计划（课程）。
  3. 后台使用 `Calendar` 循环累加日期，在**每天**的对应选课中插入一条 `status="请假"` 且 `remark="系统自动标记 - 请假已批准"` 的考勤记录，且自动去重。
* **效果**：实现点名与请假互通，杜绝信息不对称造成误记学生“旷课”的教学事故。

---

## 3. 操作运行时能看到什么 (终端界面展示)

### 3.1 教师端考勤点名流程
1. 教师进入 `[3] 课堂考勤点名` 并选择课程。
2. 确认点名日期：
   ```text
   今天是 2026-06-27，是否使用今天作为考勤点名日期？ (y/n) > y
   ```
3. 展现初始化列表（假设学生 3 已经自动请假）：
   ```text
   -------------------------------------------------------------------------
   序号 | 学号     | 姓名   | 班级         | 当前考勤状态 | 备注
   -------------------------------------------------------------------------
     1  | S230101  | 李明   | 计算机2301   | 出勤         |
     2  | S230102  | 张强   | 计算机2301   | 出勤         |
     3  | S230103  | 王芳   | 计算机2301   | 请假         | 系统自动标记...
   -------------------------------------------------------------------------
   ```
4. 提示批量修改：
   ```text
   请输入【旷课】学生的序号 > 2
   请输入学生 张强 的旷课备注 > 起床迟了没到教室
   请输入【迟到】学生的序号 > 
   ...
   ```
5. 打印修改后确认表，并输入 `y` 一键保存。

### 3.2 教师端请假审批
1. 进入 `[4] 学生请假审批`：
   ```text
   ---- 待审批的学生请假申请表 ----
   --------------------------------------------------------------
   ID | 班级       | 学号    | 姓名 | 开始日期   | 结束日期   | 原因
   --------------------------------------------------------------
   2  | 计算机2301 | S230103 | 王芳 | 2026-04-12 | 2026-04-14 | 感冒发烧
   --------------------------------------------------------------
   ```
2. 输入 `ID` 选择请选，再选择 `[1] 批准` 或 `[2] 拒绝`，输入审批意见即可完成。

### 3.3 学生端考勤汇总
1. 学生选择 `[4] 查看我的考勤统计`：可以看到自己历次被点名的日期、课程、状态和教师留言，并在最底端查看汇总：
   ```text
   【我的考勤率汇总】: 出勤率: 83.3% (总考勤点名:6次, 正常出勤:5, 迟到:0, 早退:0, 旷课:1, 请假:0)
   ```
2. 学生选择 `[5] 提交请假申请`：输入请假的起始、结束时间（格式 YYYY-MM-DD）和请假原因，确认后即可上报假条。

---
* 同学三可以根据此文档，清晰地认领自己需要负责的模块，编写对应的 DAO 和业务逻辑。
