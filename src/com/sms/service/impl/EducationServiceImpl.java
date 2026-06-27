package com.sms.service.impl;

import com.sms.dao.AttendanceDAO;
import com.sms.dao.LeaveRequestDAO;
import com.sms.dao.NoticeDAO;
import com.sms.dao.ScoreDAO;
import com.sms.dao.StudentDAO;
import com.sms.dao.UserDAO;
import com.sms.dao.impl.AttendanceDAOImpl;
import com.sms.dao.impl.LeaveRequestDAOImpl;
import com.sms.dao.impl.NoticeDAOImpl;
import com.sms.dao.impl.ScoreDAOImpl;
import com.sms.dao.impl.StudentDAOImpl;
import com.sms.dao.impl.TeacherDAOImpl;
import com.sms.dao.impl.UserDAOImpl;
import com.sms.dao.ScoreDAO;
import com.sms.entity.Attendance;
import com.sms.entity.Clazz;
import com.sms.entity.LeaveRequest;
import com.sms.entity.Teacher;
import com.sms.entity.Notice;
import com.sms.entity.Score;
import com.sms.entity.Student;
import com.sms.entity.User;
import com.sms.exception.BusinessException;
import com.sms.exception.EntityNotFoundException;
import com.sms.service.EducationService;
import com.sms.util.CSVUtil;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EducationServiceImpl implements EducationService {

    private final ScoreDAO scoreDAO = new ScoreDAOImpl();
    private final AttendanceDAO attendanceDAO = new AttendanceDAOImpl();
    private final LeaveRequestDAO leaveRequestDAO = new LeaveRequestDAOImpl();
    private final NoticeDAO noticeDAO = new NoticeDAOImpl();
    private final StudentDAO studentDAO = new StudentDAOImpl();
    private final UserDAO userDAO = new UserDAOImpl();

    // Helper to calculate grade level
    private String getGradeLevelFromScore(Double score) {
        if (score == null) return null;
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }

    private double getGpaPoints(String gradeLevel) {
        if (gradeLevel == null) return 0.0;
        switch (gradeLevel) {
            case "A": return 4.0;
            case "B": return 3.0;
            case "C": return 2.0;
            case "D": return 1.0;
            default: return 0.0;
        }
    }

    // Score CRUD
    @Override
    public void recordScore(Score score) {
        // Auto-assign grade level based on score value
        score.setGradeLevel(getGradeLevelFromScore(score.getScore()));
        
        // Check if there is an existing score slot (e.g. for student and teaching plan exam type)
        Score exist = scoreDAO.findByStudentAndPlanAndExamType(score.getStudentId(), score.getTeachingPlanId(), score.getExamType());
        if (exist != null) {
            exist.setScore(score.getScore());
            exist.setGradeLevel(score.getGradeLevel());
            scoreDAO.update(exist);
            score.setId(exist.getId());
        } else {
            scoreDAO.insert(score);
        }
    }

    @Override
    public void updateScore(Score score) {
        Score exist = scoreDAO.findById(score.getId());
        if (exist == null) throw new EntityNotFoundException("成绩记录不存在");
        exist.setScore(score.getScore());
        exist.setGradeLevel(getGradeLevelFromScore(score.getScore()));
        exist.setExamType(score.getExamType());
        scoreDAO.update(exist);
    }

    @Override
    public void deleteScore(Integer id) {
        scoreDAO.deleteById(id);
    }

    @Override
    public Score getScoreById(Integer id) {
        return scoreDAO.findById(id);
    }

    @Override
    public List<Score> listScoresByPlan(Integer planId) {
        return scoreDAO.findByTeachingPlanId(planId);
    }

    @Override
    public List<Score> listScoresByStudent(Integer studentId, String semester) {
        return scoreDAO.findByStudentId(studentId, semester);
    }

    @Override
    public List<Score> listScoresByClassAndCourse(Integer classId, Integer courseId, String semester) {
        return scoreDAO.findByClassAndCourse(classId, courseId, semester);
    }

    // Statistics & Calculations
    @Override
    public double calculateGPA(Integer studentId, String semester) {
        List<Score> scores = scoreDAO.findByStudentId(studentId, semester);
        double totalCredits = 0.0;
        double weightedPoints = 0.0;
        for (Score s : scores) {
            if (s.getScore() != null && s.getExamType().equals("期末")) {
                double credit = s.getCourseCredit() != null ? s.getCourseCredit() : 0.0;
                weightedPoints += getGpaPoints(s.getGradeLevel()) * credit;
                totalCredits += credit;
            }
        }
        return totalCredits > 0 ? (weightedPoints / totalCredits) : 0.0;
    }

    @Override
    public Map<String, Object> calculatePlanScoreStats(Integer planId) {
        List<Score> scores = scoreDAO.findByTeachingPlanId(planId);
        Map<String, Object> stats = new HashMap<>();

        int totalCount = scores.size();
        int gradedCount = 0;
        double maxScore = -1.0;
        double minScore = 101.0;
        double sum = 0.0;
        int passCount = 0;
        int excelCount = 0;

        // Score Distribution Categories
        int catA = 0; // 90-100
        int catB = 0; // 80-89
        int catC = 0; // 70-79
        int catD = 0; // 60-69
        int catF = 0; // 0-59

        for (Score s : scores) {
            if (s.getScore() != null) {
                double val = s.getScore();
                gradedCount++;
                sum += val;
                if (val > maxScore) maxScore = val;
                if (val < minScore) minScore = val;

                if (val >= 60) passCount++;
                if (val >= 90) excelCount++;

                if (val >= 90) catA++;
                else if (val >= 80) catB++;
                else if (val >= 70) catC++;
                else if (val >= 60) catD++;
                else catF++;
            }
        }

        stats.put("total", totalCount);
        stats.put("graded", gradedCount);
        if (gradedCount > 0) {
            stats.put("avg", sum / gradedCount);
            stats.put("max", maxScore);
            stats.put("min", minScore);
            stats.put("passRate", (double) passCount / gradedCount * 100.0);
            stats.put("excelRate", (double) excelCount / gradedCount * 100.0);
            
            // Distributions
            stats.put("catA", catA);
            stats.put("catB", catB);
            stats.put("catC", catC);
            stats.put("catD", catD);
            stats.put("catF", catF);
            stats.put("pctA", (double) catA / gradedCount * 100.0);
            stats.put("pctB", (double) catB / gradedCount * 100.0);
            stats.put("pctC", (double) catC / gradedCount * 100.0);
            stats.put("pctD", (double) catD / gradedCount * 100.0);
            stats.put("pctF", (double) catF / gradedCount * 100.0);
        } else {
            stats.put("avg", 0.0);
            stats.put("max", 0.0);
            stats.put("min", 0.0);
            stats.put("passRate", 0.0);
            stats.put("excelRate", 0.0);
            stats.put("catA", 0); stats.put("catB", 0); stats.put("catC", 0); stats.put("catD", 0); stats.put("catF", 0);
            stats.put("pctA", 0.0); stats.put("pctB", 0.0); stats.put("pctC", 0.0); stats.put("pctD", 0.0); stats.put("pctF", 0.0);
        }
        return stats;
    }

    @Override
    public List<Score> getRankedScores(Integer planId) {
        List<Score> scores = scoreDAO.findByTeachingPlanId(planId);
        // Exclude unrecorded grades
        List<Score> recorded = new ArrayList<>();
        for (Score s : scores) {
            if (s.getScore() != null) recorded.add(s);
        }
        // Sort descending
        recorded.sort(new Comparator<Score>() {
            @Override
            public int compare(Score o1, Score o2) {
                return Double.compare(o2.getScore(), o1.getScore());
            }
        });
        return recorded;
    }

    @Override
    public void exportScoresToCSV(Integer planId, String filePath) throws Exception {
        List<Score> scores = scoreDAO.findByTeachingPlanId(planId);
        List<String> headers = Arrays.asList("学号", "姓名", "班级", "科目", "考试类型", "成绩", "等级");
        List<List<String>> rows = new ArrayList<>();
        for (Score s : scores) {
            rows.add(Arrays.asList(
                    s.getStudentNo(),
                    s.getStudentName(),
                    s.getClassName() != null ? s.getClassName() : "选修",
                    s.getCourseName(),
                    s.getExamType(),
                    s.getScore() != null ? s.getScore().toString() : "未录入",
                    s.getGradeLevel() != null ? s.getGradeLevel() : ""
            ));
        }
        CSVUtil.writeCSV(filePath, headers, rows);
    }

    // Attendance Management
    @Override
    public void takeAttendance(Integer planId, Date date, List<Attendance> attendanceList) {
        // Idempotent: delete existing records for the plan and date first, then write new
        List<Attendance> exist = attendanceDAO.findByPlanAndDate(planId, date);
        for (Attendance a : exist) {
            attendanceDAO.deleteById(a.getId());
        }

        for (Attendance a : attendanceList) {
            a.setTeachingPlanId(planId);
            a.setAttendDate(date);
            attendanceDAO.insert(a);
        }
    }

    @Override
    public List<Attendance> listAttendanceByStudent(Integer studentId) {
        return attendanceDAO.findByStudentId(studentId);
    }

    @Override
    public List<Attendance> listAttendanceByStudentAndPlan(Integer studentId, Integer planId) {
        return attendanceDAO.findByStudentAndCourse(studentId, planId);
    }

    @Override
    public List<Attendance> listAttendanceByPlanAndDate(Integer planId, Date date) {
        return attendanceDAO.findByPlanAndDate(planId, date);
    }

    @Override
    public Map<String, Object> calculateAttendanceStats(Integer studentId, Integer planId) {
        List<Attendance> list = attendanceDAO.findByStudentAndCourse(studentId, planId);
        Map<String, Object> stats = new HashMap<>();
        int total = list.size();
        int present = 0;
        int late = 0;
        int early = 0;
        int absent = 0;
        int leave = 0;

        for (Attendance a : list) {
            switch (a.getStatus()) {
                case "出勤": present++; break;
                case "迟到": late++; break;
                case "早退": early++; break;
                case "旷课": absent++; break;
                case "请假": leave++; break;
            }
        }
        stats.put("total", total);
        stats.put("present", present);
        stats.put("late", late);
        stats.put("early", early);
        stats.put("absent", absent);
        stats.put("leave", leave);
        stats.put("rate", total > 0 ? ((double) present / total * 100.0) : 100.0);
        return stats;
    }

    // Leave Requests
    @Override
    public void applyLeave(LeaveRequest request) {
        request.setStatus("待审批");
        leaveRequestDAO.insert(request);
    }

    @Override
    public void approveLeave(Integer requestId, String status, Integer approverUserId, String remark) {
        LeaveRequest req = leaveRequestDAO.findById(requestId);
        if (req == null) throw new EntityNotFoundException("请假申请不存在");
        
        req.setStatus(status);
        req.setApproverId(approverUserId);
        req.setApproveTime(new Timestamp(System.currentTimeMillis()));
        req.setRemark(remark);
        leaveRequestDAO.update(req);

        // Bug #8 修复：批准请假后自动为请假日期范围内已选课程插入"请假"状态考勤记录
        if ("已批准".equals(status)) {
            // 获取学生所有课程（不限学期，null表示全部）
            List<Score> allScores = scoreDAO.findByStudentId(req.getStudentId(), null);
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(req.getStartDate());
            while (!cal.getTime().after(req.getEndDate())) {
                Date leaveDate = new Date(cal.getTime().getTime());
                for (Score sc : allScores) {
                    if (sc.getTeachingPlanId() == null) continue;
                    // 检查当天是否已有考勤记录，避免重复插入
                    Attendance existing = attendanceDAO.findByStudentAndPlanAndDate(
                            req.getStudentId(), sc.getTeachingPlanId(), leaveDate);
                    if (existing == null) {
                        Attendance a = new Attendance();
                        a.setStudentId(req.getStudentId());
                        a.setTeachingPlanId(sc.getTeachingPlanId());
                        a.setAttendDate(leaveDate);
                        a.setStatus("请假");
                        a.setRemark("系统自动标记 - 请假已批准");
                        attendanceDAO.insert(a);
                    }
                }
                cal.add(java.util.Calendar.DATE, 1);
            }
        }
    }

    @Override
    public List<LeaveRequest> listLeavesByStudent(Integer studentId) {
        return leaveRequestDAO.findByStudentId(studentId);
    }

    @Override
    public List<LeaveRequest> listPendingLeavesByTeacher(Integer teacherUserId) {
        // Find teacher by user id
        User u = userDAO.findById(teacherUserId);
        if (u == null) return Collections.emptyList();
        
        // Find teacher entity
        // We'll write query in DAO
        // Since SQL links to teacher.id, let's look up teacher first:
        // We can create a quick mock or lookup:
        // TeacherDAO has findByUserId:
        Teacher t = new TeacherDAOImpl().findByUserId(teacherUserId);
        if (t == null) return Collections.emptyList();
        return leaveRequestDAO.findPendingByTeacherId(t.getId());
    }

    @Override
    public List<LeaveRequest> listAllLeaves() {
        return leaveRequestDAO.findAll();
    }

    // Notices
    @Override
    public void publishNotice(Notice notice) {
        notice.setStatus(1);
        noticeDAO.insert(notice);
    }

    @Override
    public void recallNotice(Integer noticeId) {
        Notice n = noticeDAO.findById(noticeId);
        if (n != null) {
            n.setStatus(0);
            noticeDAO.update(n);
        }
    }

    @Override
    public List<Notice> listNoticesForUser(Integer userId) {
        User u = userDAO.findById(userId);
        if (u == null) return Collections.emptyList();
        if (u.getRole() == com.sms.entity.UserRole.ADMIN) {
            return noticeDAO.findAll(); // Admins see all notices
        }
        return noticeDAO.findByRole(u.getRole().name());
    }

    @Override
    public List<Notice> listAllNotices() {
        return noticeDAO.findAll();
    }

    // === 新增：数据统计与报表 (模块10) ===

    @Override
    public List<Map<String, Object>> compareClassScores(Integer courseId, String semester) {
        // 获取该课程在各班级的教学计划
        List<com.sms.entity.TeachingPlan> allPlans = new com.sms.dao.impl.TeachingPlanDAOImpl().findAll();
        List<Map<String, Object>> results = new ArrayList<>();

        for (com.sms.entity.TeachingPlan tp : allPlans) {
            if (!tp.getCourseId().equals(courseId)) continue;
            if (tp.getClassId() == null) continue; // 跳过选修课（无班级绑定）
            if (semester != null && !semester.isEmpty() && !tp.getSemester().equals(semester)) continue;

            List<Score> scores = scoreDAO.findByTeachingPlanId(tp.getId());
            Map<String, Object> row = new HashMap<>();
            row.put("className", tp.getClassName());
            row.put("semester", tp.getSemester());
            row.put("courseName", tp.getCourseName());

            int graded = 0, passCount = 0, excelCount = 0;
            double sum = 0, max = -1, min = 101;
            for (Score s : scores) {
                if (s.getScore() != null) {
                    graded++;
                    sum += s.getScore();
                    if (s.getScore() > max) max = s.getScore();
                    if (s.getScore() < min) min = s.getScore();
                    if (s.getScore() >= 60) passCount++;
                    if (s.getScore() >= 90) excelCount++;
                }
            }
            row.put("total", scores.size());
            row.put("graded", graded);
            row.put("avg", graded > 0 ? sum / graded : 0.0);
            row.put("max", graded > 0 ? max : 0.0);
            row.put("min", graded > 0 ? min : 0.0);
            row.put("passRate", graded > 0 ? (double) passCount / graded * 100.0 : 0.0);
            row.put("excelRate", graded > 0 ? (double) excelCount / graded * 100.0 : 0.0);
            results.add(row);
        }
        return results;
    }

    @Override
    public List<Map<String, Object>> getScoreTrend(Integer classId, Integer courseId) {
        // 获取该班级该课程在所有学期的成绩统计趋势
        List<com.sms.entity.TeachingPlan> allPlans = new com.sms.dao.impl.TeachingPlanDAOImpl().findAll();
        List<Map<String, Object>> trends = new ArrayList<>();

        for (com.sms.entity.TeachingPlan tp : allPlans) {
            if (!tp.getCourseId().equals(courseId)) continue;
            if (tp.getClassId() == null || !tp.getClassId().equals(classId)) continue;

            List<Score> scores = scoreDAO.findByTeachingPlanId(tp.getId());
            Map<String, Object> row = new HashMap<>();
            row.put("semester", tp.getSemester());

            int graded = 0, passCount = 0;
            double sum = 0;
            for (Score s : scores) {
                if (s.getScore() != null) {
                    graded++;
                    sum += s.getScore();
                    if (s.getScore() >= 60) passCount++;
                }
            }
            row.put("graded", graded);
            row.put("avg", graded > 0 ? sum / graded : 0.0);
            row.put("passRate", graded > 0 ? (double) passCount / graded * 100.0 : 0.0);
            trends.add(row);
        }
        // 按学期排序
        trends.sort((a, b) -> ((String) a.get("semester")).compareTo((String) b.get("semester")));
        return trends;
    }

    @Override
    public List<Map<String, Object>> getClassAttendanceReport(String semester) {
        // 按班级统计考勤率
        List<Clazz> classes = new com.sms.dao.impl.ClazzDAOImpl().findAll();
        List<com.sms.entity.TeachingPlan> allPlans = new com.sms.dao.impl.TeachingPlanDAOImpl().findAll();
        List<Map<String, Object>> report = new ArrayList<>();

        for (Clazz c : classes) {
            int totalAttendance = 0, presentCount = 0, absentCount = 0;

            for (com.sms.entity.TeachingPlan tp : allPlans) {
                if (tp.getClassId() == null || !tp.getClassId().equals(c.getId())) continue;
                if (semester != null && !semester.isEmpty() && !tp.getSemester().equals(semester)) continue;

                // 获取该教学计划下所有学生的考勤
                List<Attendance> attList = attendanceDAO.findByStudentId(null); // 需要按 plan 查
                // 使用 findByPlanAndDate 的方式，遍历所有日期
                // 简化实现：直接查该 plan 的所有考勤
                attList = attendanceDAO.findByPlanAndDate(tp.getId(), null);
                if (attList.isEmpty()) {
                    // 尝试另一种方式
                    // 由于 DAO 限制，我们通过 student 遍历
                    List<Student> students = new com.sms.dao.impl.StudentDAOImpl().findByClassId(c.getId());
                    for (Student s : students) {
                        List<Attendance> sAtt = attendanceDAO.findByStudentAndCourse(s.getId(), tp.getId());
                        for (Attendance a : sAtt) {
                            totalAttendance++;
                            String status = a.getStatus();
                            if ("出勤".equals(status) || "迟到".equals(status) || "早退".equals(status)) {
                                presentCount++;
                            } else {
                                absentCount++;
                            }
                        }
                    }
                } else {
                    for (Attendance a : attList) {
                        totalAttendance++;
                        String status = a.getStatus();
                        if ("出勤".equals(status) || "迟到".equals(status) || "早退".equals(status)) {
                            presentCount++;
                        } else {
                            absentCount++;
                        }
                    }
                }
            }

            Map<String, Object> row = new HashMap<>();
            row.put("classId", c.getId());
            row.put("className", c.getClassName());
            row.put("total", totalAttendance);
            row.put("present", presentCount);
            row.put("absent", absentCount);
            row.put("rate", totalAttendance > 0 ? (double) presentCount / totalAttendance * 100.0 : 100.0);
            // 缺勤预警：出勤率低于 85%
            row.put("warning", totalAttendance > 0 && (double) presentCount / totalAttendance * 100.0 < 85.0);
            report.add(row);
        }
        return report;
    }

    @Override
    public void exportStatisticsToCSV(List<String> headers, List<List<String>> rows, String filePath) throws Exception {
        CSVUtil.writeCSV(filePath, headers, rows);
    }
}
