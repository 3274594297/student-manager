package com.sms.dao;

import com.sms.entity.Schedule;
import java.util.List;

public interface ScheduleDAO {
    Schedule findById(Integer id);
    int insert(Schedule schedule);
    int update(Schedule schedule);
    int deleteById(Integer id);
    List<Schedule> findByClassId(Integer classId, String semester);
    List<Schedule> findByStudentId(Integer studentId, String semester);
    List<Schedule> findByTeacherId(Integer teacherId, String semester);
    List<Schedule> findByClassroom(String classroom, String semester);
    List<Schedule> findByPlanId(Integer planId);
}
