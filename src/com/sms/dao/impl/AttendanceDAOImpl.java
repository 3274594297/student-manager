package com.sms.dao.impl;

import com.sms.dao.AttendanceDAO;
import com.sms.entity.Attendance;
import com.sms.util.DBUtil;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class AttendanceDAOImpl implements AttendanceDAO {

    private final DBUtil.RowMapper<Attendance> mapper = new DBUtil.RowMapper<Attendance>() {
        @Override
        public Attendance mapRow(ResultSet rs) throws SQLException {
            Attendance a = new Attendance(
                    rs.getInt("id"),
                    rs.getInt("student_id"),
                    rs.getInt("teaching_plan_id"),
                    rs.getDate("attend_date"),
                    rs.getString("status"),
                    rs.getString("remark")
            );
            try {
                a.setStudentNo(rs.getString("student_no"));
                a.setStudentName(rs.getString("student_name"));
                a.setCourseName(rs.getString("course_name"));
                a.setClassName(rs.getString("class_name"));
                a.setSemester(rs.getString("semester"));
            } catch (SQLException e) {
                // ignore
            }
            return a;
        }
    };

    private static final String BASE_SELECT = "SELECT a.*, st.student_no, st.name as student_name, cl.class_name, " +
            "c.course_name, tp.semester " +
            "FROM attendance a " +
            "JOIN student st ON a.student_id = st.id " +
            "JOIN class cl ON st.class_id = cl.id " +
            "JOIN teaching_plan tp ON a.teaching_plan_id = tp.id " +
            "JOIN course c ON tp.course_id = c.id ";

    @Override
    public Attendance findById(Integer id) {
        try {
            return DBUtil.executeQueryOne(BASE_SELECT + "WHERE a.id = ?", mapper, id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int insert(Attendance attendance) {
        try {
            int id = DBUtil.executeInsert("INSERT INTO attendance (student_id, teaching_plan_id, attend_date, status, remark) VALUES (?, ?, ?, ?, ?)",
                    attendance.getStudentId(), attendance.getTeachingPlanId(), attendance.getAttendDate(), attendance.getStatus(), attendance.getRemark());
            if (id > 0) {
                attendance.setId(id);
                return 1;
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int update(Attendance attendance) {
        try {
            return DBUtil.executeUpdate("UPDATE attendance SET student_id = ?, teaching_plan_id = ?, attend_date = ?, status = ?, remark = ? WHERE id = ?",
                    attendance.getStudentId(), attendance.getTeachingPlanId(), attendance.getAttendDate(), attendance.getStatus(), attendance.getRemark(), attendance.getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int deleteById(Integer id) {
        try {
            return DBUtil.executeUpdate("DELETE FROM attendance WHERE id = ?", id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Attendance> findByStudentId(Integer studentId) {
        try {
            return DBUtil.executeQuery(BASE_SELECT + "WHERE a.student_id = ? ORDER BY a.attend_date DESC", mapper, studentId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Attendance> findByStudentAndCourse(Integer studentId, Integer planId) {
        try {
            return DBUtil.executeQuery(BASE_SELECT + "WHERE a.student_id = ? AND a.teaching_plan_id = ? ORDER BY a.attend_date DESC", mapper, studentId, planId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Attendance> findByPlanAndDate(Integer planId, Date date) {
        try {
            if (date == null) {
                return DBUtil.executeQuery(BASE_SELECT + "WHERE a.teaching_plan_id = ? ORDER BY st.student_no", mapper, planId);
            } else {
                return DBUtil.executeQuery(BASE_SELECT + "WHERE a.teaching_plan_id = ? AND a.attend_date = ? ORDER BY st.student_no", mapper, planId, date);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Attendance findByStudentAndPlanAndDate(Integer studentId, Integer planId, Date date) {
        try {
            return DBUtil.executeQueryOne(BASE_SELECT + "WHERE a.student_id = ? AND a.teaching_plan_id = ? AND a.attend_date = ?", mapper, studentId, planId, date);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
