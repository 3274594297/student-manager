package com.sms.dao.impl;

import com.sms.dao.ScheduleDAO;
import com.sms.entity.Schedule;
import com.sms.util.DBUtil;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ScheduleDAOImpl implements ScheduleDAO {

    private final DBUtil.RowMapper<Schedule> mapper = new DBUtil.RowMapper<Schedule>() {
        @Override
        public Schedule mapRow(ResultSet rs) throws SQLException {
            Schedule s = new Schedule(
                    rs.getInt("id"),
                    rs.getInt("teaching_plan_id"),
                    rs.getInt("day_of_week"),
                    rs.getInt("section_start"),
                    rs.getInt("section_end"),
                    rs.getString("classroom"),
                    rs.getString("campus")
            );
            try {
                s.setCourseName(rs.getString("course_name"));
                s.setTeacherName(rs.getString("teacher_name"));
                s.setClassName(rs.getString("class_name"));
                s.setSemester(rs.getString("semester"));
            } catch (SQLException e) {
                // ignore
            }
            return s;
        }
    };

    private static final String BASE_SELECT = "SELECT s.*, c.course_name, t.name as teacher_name, cl.class_name, tp.semester " +
            "FROM schedule s " +
            "JOIN teaching_plan tp ON s.teaching_plan_id = tp.id " +
            "JOIN course c ON tp.course_id = c.id " +
            "JOIN teacher t ON tp.teacher_id = t.id " +
            "LEFT JOIN class cl ON tp.class_id = cl.id ";

    @Override
    public Schedule findById(Integer id) {
        try {
            return DBUtil.executeQueryOne(BASE_SELECT + "WHERE s.id = ?", mapper, id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int insert(Schedule schedule) {
        try {
            int id = DBUtil.executeInsert("INSERT INTO schedule (teaching_plan_id, day_of_week, section_start, section_end, classroom, campus) VALUES (?, ?, ?, ?, ?, ?)",
                    schedule.getTeachingPlanId(), schedule.getDayOfWeek(), schedule.getSectionStart(), schedule.getSectionEnd(), schedule.getClassroom(), schedule.getCampus());
            if (id > 0) {
                schedule.setId(id);
                return 1;
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int update(Schedule schedule) {
        try {
            return DBUtil.executeUpdate("UPDATE schedule SET teaching_plan_id = ?, day_of_week = ?, section_start = ?, section_end = ?, classroom = ?, campus = ? WHERE id = ?",
                    schedule.getTeachingPlanId(), schedule.getDayOfWeek(), schedule.getSectionStart(), schedule.getSectionEnd(), schedule.getClassroom(), schedule.getCampus(), schedule.getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int deleteById(Integer id) {
        try {
            return DBUtil.executeUpdate("DELETE FROM schedule WHERE id = ?", id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Schedule> findByClassId(Integer classId, String semester) {
        try {
            // Bug #10 修复: 原子查询用 score 表判断"已选课"，但学期中途还没录分的学生查不出来。
            // 系统设计上，选课时即向 score 表插入一条空白记录（score=NULL），所以 score 表存在即代表已选课。
            // 此查询正确覆盖了：(1) 班级必修课 class_id=? (2) 该班学生自主选修课 via score
            String sql = BASE_SELECT + "WHERE (tp.class_id = ? OR tp.id IN (" +
                    "  SELECT DISTINCT e.teaching_plan_id FROM score sc " +
                    "  JOIN enrollment e ON sc.enrollment_id = e.id " +
                    "  JOIN student st ON e.student_id = st.id " +
                    "  WHERE st.class_id = ?" +
                    ")) AND tp.semester = ? ORDER BY s.day_of_week, s.section_start";
            return DBUtil.executeQuery(sql, mapper, classId, classId, semester);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public List<Schedule> findByStudentId(Integer studentId, String semester) {
        try {
            String sql = BASE_SELECT + "WHERE (tp.class_id = (SELECT class_id FROM student WHERE id = ?) OR tp.id IN (" +
                    "  SELECT e.teaching_plan_id FROM enrollment e WHERE e.student_id = ?" +
                    ")) AND tp.semester = ? ORDER BY s.day_of_week, s.section_start";
            return DBUtil.executeQuery(sql, mapper, studentId, studentId, semester);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public List<Schedule> findByTeacherId(Integer teacherId, String semester) {
        try {
            return DBUtil.executeQuery(BASE_SELECT + "WHERE tp.teacher_id = ? AND tp.semester = ? ORDER BY s.day_of_week, s.section_start", mapper, teacherId, semester);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Schedule> findByClassroom(String classroom, String semester) {
        try {
            return DBUtil.executeQuery(BASE_SELECT + "WHERE s.classroom = ? AND tp.semester = ? ORDER BY s.day_of_week, s.section_start", mapper, classroom, semester);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Schedule> findByPlanId(Integer planId) {
        try {
            return DBUtil.executeQuery(BASE_SELECT + "WHERE s.teaching_plan_id = ? ORDER BY s.day_of_week, s.section_start", mapper, planId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}