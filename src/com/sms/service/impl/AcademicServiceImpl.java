package com.sms.service.impl;

import com.sms.dao.CourseDAO;
import com.sms.dao.ScheduleDAO;
import com.sms.dao.ScoreDAO;
import com.sms.dao.StudentDAO;
import com.sms.dao.TeachingPlanDAO;
import com.sms.dao.impl.CourseDAOImpl;
import com.sms.dao.impl.ScheduleDAOImpl;
import com.sms.dao.impl.ScoreDAOImpl;
import com.sms.dao.impl.StudentDAOImpl;
import com.sms.dao.impl.TeachingPlanDAOImpl;
import com.sms.entity.Course;
import com.sms.entity.Schedule;
import com.sms.entity.Score;
import com.sms.entity.Student;
import com.sms.entity.TeachingPlan;
import com.sms.exception.BusinessException;
import com.sms.exception.EntityNotFoundException;
import com.sms.exception.ScheduleConflictException;
import com.sms.service.AcademicService;
import java.util.List;

public class AcademicServiceImpl implements AcademicService {

    private final CourseDAO courseDAO = new CourseDAOImpl();
    private final TeachingPlanDAO teachingPlanDAO = new TeachingPlanDAOImpl();
    private final ScheduleDAO scheduleDAO = new ScheduleDAOImpl();
    private final ScoreDAO scoreDAO = new ScoreDAOImpl();
    private final StudentDAO studentDAO = new StudentDAOImpl();

    // Course CRUD
    @Override
    public void addCourse(Course course) {
        if (courseDAO.findByCourseNo(course.getCourseNo()) != null) {
            throw new BusinessException("课程编号已存在: " + course.getCourseNo());
        }
        courseDAO.insert(course);
    }

    @Override
    public void updateCourse(Course course) {
        if (courseDAO.findById(course.getId()) == null) throw new EntityNotFoundException("课程不存在");
        courseDAO.update(course);
    }

    @Override
    public void deleteCourse(Integer id) {
        courseDAO.deleteById(id);
    }

    @Override
    public Course getCourseById(Integer id) {
        return courseDAO.findById(id);
    }

    @Override
    public Course getCourseByNo(String courseNo) {
        return courseDAO.findByCourseNo(courseNo);
    }

    @Override
    public List<Course> listAllCourses() {
        return courseDAO.findAll();
    }

    // TeachingPlan CRUD
    @Override
    public void addTeachingPlan(TeachingPlan plan) {
        // If mandatory (classId is not null), automatically enroll students in class by creating score slots
        teachingPlanDAO.insert(plan);

        if (plan.getClassId() != null) {
            // Retrieve all students in this class and assign them to this course
            List<Student> classRoster = studentDAO.findByClassId(plan.getClassId());
            for (Student s : classRoster) {
                Score sc = new Score();
                sc.setTeachingPlanId(plan.getId());
                sc.setStudentId(s.getId());
                sc.setExamType("期末");
                scoreDAO.insert(sc);
            }
            // Update teaching plan enrollment count
            plan.setCurrentStudents(classRoster.size());
            teachingPlanDAO.update(plan);
        }
    }

    @Override
    public void updateTeachingPlan(TeachingPlan plan) {
        if (teachingPlanDAO.findById(plan.getId()) == null) throw new EntityNotFoundException("教学计划不存在");
        teachingPlanDAO.update(plan);
    }

    @Override
    public void deleteTeachingPlan(Integer id) {
        teachingPlanDAO.deleteById(id);
    }

    @Override
    public TeachingPlan getTeachingPlanById(Integer id) {
        return teachingPlanDAO.findById(id);
    }

    @Override
    public List<TeachingPlan> listAllTeachingPlans() {
        return teachingPlanDAO.findAll();
    }

    @Override
    public List<TeachingPlan> listTeachingPlansByClass(Integer classId, String semester) {
        return teachingPlanDAO.findByClassId(classId, semester);
    }

    @Override
    public List<TeachingPlan> listTeachingPlansByTeacher(Integer teacherId, String semester) {
        return teachingPlanDAO.findByTeacherId(teacherId, semester);
    }

    @Override
    public List<TeachingPlan> listElectivePlans(String semester) {
        return teachingPlanDAO.findElectives(semester);
    }

    @Override
    public List<TeachingPlan> listAvailableElectivePlansForStudent(Integer studentId, String semester) {
        List<TeachingPlan> allElectives = teachingPlanDAO.findElectives(semester);
        List<TeachingPlan> selected = listStudentSelectedPlans(studentId, semester);
        
        java.util.Set<Integer> selectedPlanIds = new java.util.HashSet<>();
        for (TeachingPlan tp : selected) {
            selectedPlanIds.add(tp.getId());
        }
        
        List<TeachingPlan> available = new java.util.ArrayList<>();
        for (TeachingPlan tp : allElectives) {
            if (!selectedPlanIds.contains(tp.getId())) {
                available.add(tp);
            }
        }
        return available;
    }

    // Student Course Selection
    @Override
    public void selectElective(Integer studentId, Integer teachingPlanId) {
        Student student = studentDAO.findById(studentId);
        if (student == null) throw new EntityNotFoundException("学生不存在");

        TeachingPlan plan = teachingPlanDAO.findById(teachingPlanId);
        if (plan == null) throw new EntityNotFoundException("该课程计划不存在");

        if (plan.getClassId() != null) {
            throw new BusinessException("该课程为必修课，不属于自由选课范围");
        }

        // 1. Check if already selected
        Score exist = scoreDAO.findByStudentAndPlanAndExamType(studentId, teachingPlanId, "期末");
        if (exist != null) {
            throw new BusinessException("您已经选修了该门课程，请勿重复选择");
        }

        // 2. Check capacity
        if (plan.getCurrentStudents() >= plan.getMaxStudents()) {
            throw new BusinessException("选课人数已满 (" + plan.getCurrentStudents() + "/" + plan.getMaxStudents() + ")");
        }

        // 3. 时间冲突检测
        // Bug #5 修复：同时检查已选班级必修课和已选其他选修课的时间冲突
        List<Schedule> classSchedules = scheduleDAO.findByClassId(student.getClassId(), plan.getSemester());
        // 获取学生已选选修课的所有排课（class_id 为 null 的教学计划）
        List<TeachingPlan> selectedElectives = listStudentSelectedPlans(studentId, plan.getSemester());
        List<Schedule> allStudentSchedules = new java.util.ArrayList<>(classSchedules);
        for (TeachingPlan elective : selectedElectives) {
            if (elective.getClassId() == null && !elective.getId().equals(teachingPlanId)) {
                allStudentSchedules.addAll(scheduleDAO.findByPlanId(elective.getId()));
            }
        }
        List<Schedule> planSchedules = scheduleDAO.findByPlanId(teachingPlanId);

        for (Schedule ps : planSchedules) {
            for (Schedule ss : allStudentSchedules) {
                if (ps.getDayOfWeek().equals(ss.getDayOfWeek())) {
                    if (ps.getSectionStart() <= ss.getSectionEnd() && ps.getSectionEnd() >= ss.getSectionStart()) {
                        throw new ScheduleConflictException(String.format(
                                "选课时间冲突！该课程上课时间 [周%d 第%d-%d节] 与已有课表 [%s] 重合。",
                                ps.getDayOfWeek(), ps.getSectionStart(), ps.getSectionEnd(), ss.getCourseName()
                        ));
                    }
                }
            }
        }

        // 4. Enroll
        Score score = new Score();
        score.setTeachingPlanId(teachingPlanId);
        score.setStudentId(studentId);
        score.setExamType("期末");
        scoreDAO.insert(score);

        // Update count
        plan.setCurrentStudents(plan.getCurrentStudents() + 1);
        teachingPlanDAO.update(plan);
    }

    @Override
    public void deselectElective(Integer studentId, Integer teachingPlanId) {
        TeachingPlan plan = teachingPlanDAO.findById(teachingPlanId);
        if (plan == null) throw new EntityNotFoundException("课程计划不存在");

        Score score = scoreDAO.findByStudentAndPlanAndExamType(studentId, teachingPlanId, "期末");
        if (score == null) {
            throw new BusinessException("您未选修该课程，无法退选");
        }

        if (score.getScore() != null) {
            throw new BusinessException("该课程已被录入成绩，无法退选");
        }

        // Delete score slot
        scoreDAO.deleteById(score.getId());

        // Update count
        plan.setCurrentStudents(Math.max(0, plan.getCurrentStudents() - 1));
        teachingPlanDAO.update(plan);
    }

    @Override
    public List<TeachingPlan> listStudentSelectedPlans(Integer studentId, String semester) {
        // Find plans through student's scores
        List<Score> scores = scoreDAO.findByStudentId(studentId, semester);
        List<TeachingPlan> plans = new java.util.ArrayList<>();
        java.util.Set<Integer> seenIds = new java.util.HashSet<>();
        for (Score s : scores) {
            if (s.getTeachingPlanId() != null && !seenIds.contains(s.getTeachingPlanId())) {
                seenIds.add(s.getTeachingPlanId());
                TeachingPlan p = teachingPlanDAO.findById(s.getTeachingPlanId());
                if (p != null) plans.add(p);
            }
        }
        return plans;
    }

    // Schedule Slot CRUD
    @Override
    public void addSchedule(Schedule schedule) {
        TeachingPlan plan = teachingPlanDAO.findById(schedule.getTeachingPlanId());
        if (plan == null) throw new EntityNotFoundException("关联的教学计划不存在");

        // 1. Conflict detection
        checkScheduleConflict(schedule, plan.getSemester());

        // 2. Save
        scheduleDAO.insert(schedule);
    }

    @Override
    public void deleteSchedule(Integer id) {
        scheduleDAO.deleteById(id);
    }

    @Override
    public List<Schedule> listSchedulesByClass(Integer classId, String semester) {
        return scheduleDAO.findByClassId(classId, semester);
    }

    @Override
    public List<Schedule> listSchedulesByTeacher(Integer teacherId, String semester) {
        return scheduleDAO.findByTeacherId(teacherId, semester);
    }

    @Override
    public List<Schedule> listSchedulesByClassroom(String classroom, String semester) {
        return scheduleDAO.findByClassroom(classroom, semester);
    }

    @Override
    public List<Schedule> listSchedulesByPlan(Integer planId) {
        return scheduleDAO.findByPlanId(planId);
    }

    @Override
    public void checkScheduleConflict(Schedule s, String semester) {
        TeachingPlan plan = teachingPlanDAO.findById(s.getTeachingPlanId());
        if (plan == null) return;

        // Check Classroom conflict in the same semester
        List<Schedule> crSchedules = scheduleDAO.findByClassroom(s.getClassroom(), semester);
        for (Schedule ex : crSchedules) {
            if (ex.getDayOfWeek().equals(s.getDayOfWeek())) {
                if (s.getSectionStart() <= ex.getSectionEnd() && s.getSectionEnd() >= ex.getSectionStart()) {
                    throw new ScheduleConflictException(String.format("教室冲突: %s 的周%d 第%d-%d节 已被课程 [%s] 占用",
                            s.getClassroom(), s.getDayOfWeek(), s.getSectionStart(), s.getSectionEnd(), ex.getCourseName()));
                }
            }
        }

        // Check Teacher conflict in the same semester
        List<Schedule> tSchedules = scheduleDAO.findByTeacherId(plan.getTeacherId(), semester);
        for (Schedule ex : tSchedules) {
            if (ex.getDayOfWeek().equals(s.getDayOfWeek())) {
                if (s.getSectionStart() <= ex.getSectionEnd() && s.getSectionEnd() >= ex.getSectionStart()) {
                    throw new ScheduleConflictException(String.format("教师冲突: 该时段内教师已有课程 [%s] (%s)",
                            ex.getCourseName(), ex.getClassName() != null ? ex.getClassName() : "选修"));
                }
            }
        }

        // Check Class conflict (only if this teaching plan is assigned to a class)
        if (plan.getClassId() != null) {
            List<Schedule> clSchedules = scheduleDAO.findByClassId(plan.getClassId(), semester);
            for (Schedule ex : clSchedules) {
                if (ex.getDayOfWeek().equals(s.getDayOfWeek())) {
                    if (s.getSectionStart() <= ex.getSectionEnd() && s.getSectionEnd() >= ex.getSectionStart()) {
                        throw new ScheduleConflictException(String.format("班级时间冲突: 班级该时段已有课程 [%s]",
                                ex.getCourseName()));
                    }
                }
            }
        }
    }
}
