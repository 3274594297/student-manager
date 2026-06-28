package com.sms.service;

import com.sms.entity.Course;
import com.sms.entity.Schedule;
import com.sms.entity.TeachingPlan;
import java.util.List;

public interface AcademicService {
    // Course
    void addCourse(Course course);
    void updateCourse(Course course);
    void deleteCourse(Integer id);
    Course getCourseById(Integer id);
    Course getCourseByNo(String courseNo);
    List<Course> listAllCourses();

    // TeachingPlan
    void addTeachingPlan(TeachingPlan plan);
    void updateTeachingPlan(TeachingPlan plan);
    void deleteTeachingPlan(Integer id);
    TeachingPlan getTeachingPlanById(Integer id);
    List<TeachingPlan> listAllTeachingPlans();
    List<TeachingPlan> listTeachingPlansByClass(Integer classId, String semester);
    List<TeachingPlan> listTeachingPlansByTeacher(Integer teacherId, String semester);
    List<TeachingPlan> listElectivePlans(String semester);
    List<TeachingPlan> listAvailableElectivePlansForStudent(Integer studentId, String semester);

    // Student Course Selection
    void selectElective(Integer studentId, Integer teachingPlanId);
    void deselectElective(Integer studentId, Integer teachingPlanId);
    List<TeachingPlan> listStudentSelectedPlans(Integer studentId, String semester);

    // Schedule (Weekly Timetable)
    void addSchedule(Schedule schedule);
    void deleteSchedule(Integer id);
    List<Schedule> listSchedulesByClass(Integer classId, String semester);
    List<Schedule> listSchedulesByStudent(Integer studentId, String semester);
    List<Schedule> listSchedulesByTeacher(Integer teacherId, String semester);
    List<Schedule> listSchedulesByClassroom(String classroom, String semester);
    List<Schedule> listSchedulesByPlan(Integer planId);

    // Conflict Check
    void checkScheduleConflict(Schedule schedule, String semester);
}
