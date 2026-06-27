package com.sms.service.impl;

import com.sms.dao.ClazzDAO;
import com.sms.dao.DepartmentDAO;
import com.sms.dao.MajorDAO;
import com.sms.dao.StudentDAO;
import com.sms.dao.TeacherDAO;
import com.sms.dao.UserDAO;
import com.sms.dao.impl.ClazzDAOImpl;
import com.sms.dao.impl.DepartmentDAOImpl;
import com.sms.dao.impl.MajorDAOImpl;
import com.sms.dao.impl.StudentDAOImpl;
import com.sms.dao.impl.TeacherDAOImpl;
import com.sms.dao.impl.UserDAOImpl;
import com.sms.entity.Clazz;
import com.sms.entity.Department;
import com.sms.entity.Major;
import com.sms.entity.Student;
import com.sms.entity.Teacher;
import com.sms.entity.User;
import com.sms.entity.UserRole;
import com.sms.exception.BusinessException;
import com.sms.exception.EntityNotFoundException;
import com.sms.service.AdminService;
import com.sms.util.CSVUtil;
import com.sms.util.MD5Util;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.sms.dao.TeachingPlanDAO;
import com.sms.dao.ScoreDAO;
import com.sms.dao.impl.TeachingPlanDAOImpl;
import com.sms.dao.impl.ScoreDAOImpl;
import com.sms.entity.TeachingPlan;
import com.sms.entity.Score;

public class AdminServiceImpl implements AdminService {

    private final DepartmentDAO departmentDAO = new DepartmentDAOImpl();
    private final MajorDAO majorDAO = new MajorDAOImpl();
    private final ClazzDAO clazzDAO = new ClazzDAOImpl();
    private final TeacherDAO teacherDAO = new TeacherDAOImpl();
    private final StudentDAO studentDAO = new StudentDAOImpl();
    private final UserDAO userDAO = new UserDAOImpl();
    private final TeachingPlanDAO teachingPlanDAO = new TeachingPlanDAOImpl();
    private final ScoreDAO scoreDAO = new ScoreDAOImpl();

    // Department
    @Override
    public void addDepartment(Department dept) {
        if (departmentDAO.findByDeptNo(dept.getDeptNo()) != null) {
            throw new BusinessException("院系编号已存在: " + dept.getDeptNo());
        }
        departmentDAO.insert(dept);
    }

    @Override
    public void updateDepartment(Department dept) {
        Department exist = departmentDAO.findById(dept.getId());
        if (exist == null) throw new EntityNotFoundException("院系不存在");
        departmentDAO.update(dept);
    }

    @Override
    public void deleteDepartment(Integer id) {
        departmentDAO.deleteById(id);
    }

    @Override
    public Department getDepartmentById(Integer id) {
        return departmentDAO.findById(id);
    }

    @Override
    public List<Department> listAllDepartments() {
        return departmentDAO.findAll();
    }

    // Major
    @Override
    public void addMajor(Major major) {
        if (majorDAO.findByMajorNo(major.getMajorNo()) != null) {
            throw new BusinessException("专业编号已存在: " + major.getMajorNo());
        }
        if (departmentDAO.findById(major.getDeptId()) == null) {
            throw new EntityNotFoundException("所属院系不存在");
        }
        majorDAO.insert(major);
    }

    @Override
    public void updateMajor(Major major) {
        if (majorDAO.findById(major.getId()) == null) throw new EntityNotFoundException("专业不存在");
        if (departmentDAO.findById(major.getDeptId()) == null) throw new EntityNotFoundException("所属院系不存在");
        majorDAO.update(major);
    }

    @Override
    public void deleteMajor(Integer id) {
        majorDAO.deleteById(id);
    }

    @Override
    public Major getMajorById(Integer id) {
        return majorDAO.findById(id);
    }

    @Override
    public List<Major> listAllMajors() {
        return majorDAO.findAll();
    }

    @Override
    public List<Major> listMajorsByDept(Integer deptId) {
        return majorDAO.findByDeptId(deptId);
    }

    // Clazz
    @Override
    public void addClazz(Clazz clazz) {
        if (clazzDAO.findByClassNo(clazz.getClassNo()) != null) {
            throw new BusinessException("班级编号已存在: " + clazz.getClassNo());
        }
        if (majorDAO.findById(clazz.getMajorId()) == null) {
            throw new EntityNotFoundException("所属专业不存在");
        }
        if (clazz.getHeadTeacherId() != null && teacherDAO.findById(clazz.getHeadTeacherId()) == null) {
            throw new EntityNotFoundException("担任班主任的教师不存在");
        }
        clazzDAO.insert(clazz);
    }

    @Override
    public void updateClazz(Clazz clazz) {
        if (clazzDAO.findById(clazz.getId()) == null) throw new EntityNotFoundException("班级不存在");
        if (majorDAO.findById(clazz.getMajorId()) == null) throw new EntityNotFoundException("所属专业不存在");
        if (clazz.getHeadTeacherId() != null && teacherDAO.findById(clazz.getHeadTeacherId()) == null) {
            throw new EntityNotFoundException("担任班主任的教师不存在");
        }
        clazzDAO.update(clazz);
    }

    @Override
    public void deleteClazz(Integer id) {
        clazzDAO.deleteById(id);
    }

    @Override
    public Clazz getClazzById(Integer id) {
        return clazzDAO.findById(id);
    }

    @Override
    public List<Clazz> listAllClazzes() {
        return clazzDAO.findAll();
    }

    @Override
    public List<Clazz> listClazzesByMajor(Integer majorId) {
        return clazzDAO.findByMajorId(majorId);
    }

    // Teacher
    @Override
    public Teacher addTeacher(Teacher teacher, String username, String password) {
        if (teacherDAO.findByTeacherNo(teacher.getTeacherNo()) != null) {
            throw new BusinessException("工号已存在: " + teacher.getTeacherNo());
        }
        if (userDAO.findByUsername(username) != null) {
            throw new BusinessException("用户名已存在: " + username);
        }
        if (departmentDAO.findById(teacher.getDepartmentId()) == null) {
            throw new EntityNotFoundException("院系不存在");
        }

        // Create User first
        String salt = MD5Util.generateSalt();
        User u = new User();
        u.setUsername(username);
        u.setSalt(salt);
        u.setPasswordHash(MD5Util.md5WithSalt(password, salt));
        u.setRole(UserRole.TEACHER);
        u.setRealName(teacher.getName());
        u.setStatus(1);
        userDAO.insert(u);

        // Create Teacher record
        teacher.setUserId(u.getId());
        teacher.setStatus(1);
        teacherDAO.insert(teacher);
        return teacher;
    }

    @Override
    public void updateTeacher(Teacher teacher) {
        Teacher exist = teacherDAO.findById(teacher.getId());
        if (exist == null) throw new EntityNotFoundException("教师不存在");
        if (departmentDAO.findById(teacher.getDepartmentId()) == null) {
            throw new EntityNotFoundException("院系不存在");
        }

        // Update real name in user table as well
        User u = userDAO.findById(exist.getUserId());
        if (u != null) {
            u.setRealName(teacher.getName());
            userDAO.update(u);
        }

        teacherDAO.update(teacher);
    }

    @Override
    public void deleteTeacher(Integer id) {
        Teacher exist = teacherDAO.findById(id);
        if (exist != null) {
            teacherDAO.deleteById(id);
            userDAO.deleteById(exist.getUserId()); // Cascade deletes user login account
        }
    }

    @Override
    public Teacher getTeacherById(Integer id) {
        return teacherDAO.findById(id);
    }

    @Override
    public Teacher getTeacherByUserId(Integer userId) {
        return teacherDAO.findByUserId(userId);
    }

    @Override
    public Teacher getTeacherByNo(String teacherNo) {
        return teacherDAO.findByTeacherNo(teacherNo);
    }

    @Override
    public List<Teacher> listAllTeachers() {
        return teacherDAO.findAll();
    }

    // Student
    @Override
    public Student addStudent(Student student, String username, String password) {
        if (studentDAO.findByStudentNo(student.getStudentNo()) != null) {
            throw new BusinessException("学号已存在: " + student.getStudentNo());
        }
        if (userDAO.findByUsername(username) != null) {
            throw new BusinessException("用户名已存在: " + username);
        }
        if (clazzDAO.findById(student.getClassId()) == null) {
            throw new EntityNotFoundException("所属班级不存在");
        }

        // Create User
        String salt = MD5Util.generateSalt();
        User u = new User();
        u.setUsername(username);
        u.setSalt(salt);
        u.setPasswordHash(MD5Util.md5WithSalt(password, salt));
        u.setRole(UserRole.STUDENT);
        u.setRealName(student.getName());
        u.setStatus(1);
        userDAO.insert(u);

        // Create Student
        student.setUserId(u.getId());
        student.setStatus(1);
        studentDAO.insert(student);

        // Automatically enroll the student in all existing teaching plans for their class
        List<TeachingPlan> plans = teachingPlanDAO.findAll();
        for (TeachingPlan plan : plans) {
            if (plan.getClassId() != null && plan.getClassId().equals(student.getClassId())) {
                Score exist = scoreDAO.findByStudentAndPlanAndExamType(student.getId(), plan.getId(), "期末");
                if (exist == null) {
                    Score sc = new Score();
                    sc.setTeachingPlanId(plan.getId());
                    sc.setStudentId(student.getId());
                    sc.setExamType("期末");
                    scoreDAO.insert(sc);

                    plan.setCurrentStudents(plan.getCurrentStudents() + 1);
                    teachingPlanDAO.update(plan);
                }
            }
        }

        return student;
    }

    @Override
    public void updateStudent(Student student) {
        Student exist = studentDAO.findById(student.getId());
        if (exist == null) throw new EntityNotFoundException("学生不存在");
        if (clazzDAO.findById(student.getClassId()) == null) {
            throw new EntityNotFoundException("班级不存在");
        }

        User u = userDAO.findById(exist.getUserId());
        if (u != null) {
            u.setRealName(student.getName());
            userDAO.update(u);
        }

        studentDAO.update(student);
    }

    @Override
    public void deleteStudent(Integer id) {
        Student exist = studentDAO.findById(id);
        if (exist != null) {
            studentDAO.deleteById(id);
            userDAO.deleteById(exist.getUserId());
        }
    }

    @Override
    public Student getStudentById(Integer id) {
        return studentDAO.findById(id);
    }

    @Override
    public Student getStudentByUserId(Integer userId) {
        return studentDAO.findByUserId(userId);
    }

    @Override
    public Student getStudentByNo(String studentNo) {
        return studentDAO.findByStudentNo(studentNo);
    }

    @Override
    public List<Student> listAllStudents() {
        return studentDAO.findAll();
    }

    @Override
    public List<Student> listStudentsByClass(Integer classId) {
        return studentDAO.findByClassId(classId);
    }

    @Override
    public List<Student> searchStudents(String queryStr, Integer classId, Integer majorId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return studentDAO.searchStudents(queryStr, classId, majorId, offset, pageSize);
    }

    @Override
    public int countStudents(String queryStr, Integer classId, Integer majorId) {
        return studentDAO.countStudents(queryStr, classId, majorId);
    }

    @Override
    public void changeStudentClass(Integer studentId, Integer targetClassId) {
        Student student = studentDAO.findById(studentId);
        if (student == null) throw new EntityNotFoundException("学生不存在");
        if (clazzDAO.findById(targetClassId) == null) throw new EntityNotFoundException("目标班级不存在");
        
        Integer oldClassId = student.getClassId();
        
        student.setClassId(targetClassId);
        studentDAO.update(student);

        // 1. Withdraw from old class's ungraded mandatory courses
        if (oldClassId != null && !oldClassId.equals(targetClassId)) {
            List<TeachingPlan> oldPlans = teachingPlanDAO.findAll();
            for (TeachingPlan plan : oldPlans) {
                if (plan.getClassId() != null && plan.getClassId().equals(oldClassId)) {
                    Score sc = scoreDAO.findByStudentAndPlanAndExamType(studentId, plan.getId(), "期末");
                    if (sc != null && sc.getScore() == null) {
                        scoreDAO.deleteById(sc.getId());
                        plan.setCurrentStudents(Math.max(0, plan.getCurrentStudents() - 1));
                        teachingPlanDAO.update(plan);
                    }
                }
            }
        }

        // 2. Enroll in new class's mandatory courses
        List<TeachingPlan> targetPlans = teachingPlanDAO.findAll();
        for (TeachingPlan plan : targetPlans) {
            if (plan.getClassId() != null && plan.getClassId().equals(targetClassId)) {
                Score exist = scoreDAO.findByStudentAndPlanAndExamType(studentId, plan.getId(), "期末");
                if (exist == null) {
                    Score sc = new Score();
                    sc.setTeachingPlanId(plan.getId());
                    sc.setStudentId(studentId);
                    sc.setExamType("期末");
                    scoreDAO.insert(sc);

                    plan.setCurrentStudents(plan.getCurrentStudents() + 1);
                    teachingPlanDAO.update(plan);
                }
            }
        }
    }

    // CSV Imports & Exports
    @Override
    public int importStudentsFromCSV(String filePath) throws Exception {
        List<List<String>> csvData = CSVUtil.readCSV(filePath);
        if (csvData.isEmpty()) {
            return 0;
        }

        int importCount = 0;
        // Assume first row is header
        for (int i = 1; i < csvData.size(); i++) {
            List<String> row = csvData.get(i);
            if (row.size() < 8) continue; // Minimum required columns

            String studentNo = row.get(0).trim();
            String name = row.get(1).trim();
            String gender = row.get(2).trim();

            if (!"男".equals(gender) && !"女".equals(gender)) {
                continue;
            }
            String birthDateStr = row.get(3).trim();
            String phone = row.get(4).trim();
            String email = row.get(5).trim();
            String address = row.get(6).trim();
            String classNo = row.get(7).trim();

            if (studentDAO.findByStudentNo(studentNo) != null) {
                // Skip duplicates
                continue;
            }

            Clazz cl = clazzDAO.findByClassNo(classNo);
            if (cl == null) {
                // Skip if class doesn't exist
                continue;
            }

            Student s = new Student();
            s.setStudentNo(studentNo);
            s.setName(name);
            s.setGender(gender);
            s.setBirthDate(Date.valueOf(birthDateStr));
            s.setPhone(phone);
            s.setEmail(email);
            s.setAddress(address);
            s.setEnrollDate(new Date(System.currentTimeMillis()));
            s.setClassId(cl.getId());
            s.setStatus(1);

            // Default username = studentNo, password = student123
            addStudent(s, studentNo, "student123");
            importCount++;
        }
        return importCount;
    }

    @Override
    public void exportStudentsToCSV(String filePath) throws Exception {
        List<Student> students = studentDAO.findAll();
        List<String> headers = Arrays.asList("学号", "姓名", "性别", "出生日期", "手机号", "邮箱", "地址", "班级编号", "班级名称");
        List<List<String>> rows = new ArrayList<>();
        for (Student s : students) {
            rows.add(Arrays.asList(
                    s.getStudentNo(),
                    s.getName(),
                    s.getGender(),
                    s.getBirthDate().toString(),
                    s.getPhone() != null ? s.getPhone() : "",
                    s.getEmail() != null ? s.getEmail() : "",
                    s.getAddress() != null ? s.getAddress() : "",
                    s.getStudentNo().substring(0, 5), // fallback classNo representation if joint fields are missing
                    s.getClassName() != null ? s.getClassName() : ""
            ));
        }
        CSVUtil.writeCSV(filePath, headers, rows);
    }
}
