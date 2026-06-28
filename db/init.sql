-- Create database if not exists
CREATE DATABASE IF NOT EXISTS student_manager_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE student_manager_db;

-- Drop tables in correct order if they exist
DROP TABLE IF EXISTS leave_request;
DROP TABLE IF EXISTS notice;
DROP TABLE IF EXISTS schedule;
DROP TABLE IF EXISTS attendance;
DROP TABLE IF EXISTS score;
DROP TABLE IF EXISTS enrollment;
DROP TABLE IF EXISTS teaching_plan;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS class;
DROP TABLE IF EXISTS teacher;
DROP TABLE IF EXISTS major;
DROP TABLE IF EXISTS department;
DROP TABLE IF EXISTS user;

-- 1. user 用户表
CREATE TABLE user (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(64) NOT NULL,
    salt VARCHAR(32) NOT NULL,
    role ENUM('ADMIN', 'TEACHER', 'STUDENT') NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    status INT DEFAULT 1 COMMENT '1: 启用, 0: 禁用',
    failed_attempts INT DEFAULT 0,
    lock_until TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. department 院系表
CREATE TABLE department (
    id INT AUTO_INCREMENT PRIMARY KEY,
    dept_no VARCHAR(20) NOT NULL UNIQUE,
    dept_name VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;

-- 3. major 专业表
CREATE TABLE major (
    id INT AUTO_INCREMENT PRIMARY KEY,
    major_no VARCHAR(20) NOT NULL UNIQUE,
    major_name VARCHAR(50) NOT NULL,
    dept_id INT NOT NULL,
    duration_years INT NOT NULL DEFAULT 4,
    FOREIGN KEY (dept_id) REFERENCES department(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 4. teacher 教师表
CREATE TABLE teacher (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNIQUE NOT NULL,
    teacher_no VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    gender VARCHAR(10) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(50),
    title VARCHAR(50) COMMENT '职称: 教授, 副教授, 讲师, 助教',
    department_id INT NOT NULL,
    status INT DEFAULT 1 COMMENT '1: 在职, 0: 离职',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES department(id)
) ENGINE=InnoDB;

-- 5. class 班级表
CREATE TABLE class (
    id INT AUTO_INCREMENT PRIMARY KEY,
    class_no VARCHAR(20) NOT NULL UNIQUE,
    class_name VARCHAR(50) NOT NULL,
    major_id INT NOT NULL,
    enroll_year INT NOT NULL,
    head_teacher_id INT,
    FOREIGN KEY (major_id) REFERENCES major(id),
    FOREIGN KEY (head_teacher_id) REFERENCES teacher(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 6. student 学生表
CREATE TABLE student (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNIQUE NOT NULL,
    student_no VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    gender VARCHAR(10) NOT NULL CHECK (gender IN ('男', '女')),
    birth_date DATE NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(50),
    address VARCHAR(100),
    enroll_date DATE NOT NULL,
    class_id INT NOT NULL,
    photo_path VARCHAR(200),
    status INT DEFAULT 1 COMMENT '1: 在学, 0: 退学/毕业',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (class_id) REFERENCES class(id)
) ENGINE=InnoDB;

-- 7. course 课程表
CREATE TABLE course (
    id INT AUTO_INCREMENT PRIMARY KEY,
    course_no VARCHAR(20) NOT NULL UNIQUE,
    course_name VARCHAR(50) NOT NULL,
    credit DOUBLE NOT NULL,
    hours INT NOT NULL,
    type ENUM('必修', '选修', '公选') NOT NULL
) ENGINE=InnoDB;

-- 8. teaching_plan 教学计划表
CREATE TABLE teaching_plan (
    id INT AUTO_INCREMENT PRIMARY KEY,
    course_id INT NOT NULL,
    teacher_id INT NOT NULL,
    class_id INT COMMENT '必修课绑定班级，选修/公选课可为空（学生自由选课）',
    semester VARCHAR(20) NOT NULL COMMENT '学期，如：2025-2026-1',
    max_students INT DEFAULT 60 COMMENT '课程人数上限',
    current_students INT DEFAULT 0 COMMENT '已选人数',
    FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE,
    FOREIGN KEY (teacher_id) REFERENCES teacher(id) ON DELETE CASCADE,
    FOREIGN KEY (class_id) REFERENCES class(id) ON DELETE CASCADE,
    UNIQUE KEY unique_teaching (course_id, teacher_id, class_id, semester)
) ENGINE=InnoDB;

-- 9. enrollment 选课表
CREATE TABLE enrollment (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    teaching_plan_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE,
    FOREIGN KEY (teaching_plan_id) REFERENCES teaching_plan(id) ON DELETE CASCADE,
    UNIQUE KEY unique_student_plan (student_id, teaching_plan_id)
) ENGINE=InnoDB;

-- 10. score 成绩表
CREATE TABLE score (
    id INT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id INT NOT NULL,
    score DOUBLE DEFAULT NULL,
    grade_level VARCHAR(5) DEFAULT NULL COMMENT '等级制: A, B, C, D, F',
    exam_type ENUM('平时', '期中', '期末') NOT NULL DEFAULT '期末',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (enrollment_id) REFERENCES enrollment(id) ON DELETE CASCADE,
    UNIQUE KEY unique_enrollment_exam (enrollment_id, exam_type)
) ENGINE=InnoDB;

-- 10. attendance 考勤表
CREATE TABLE attendance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    teaching_plan_id INT NOT NULL,
    attend_date DATE NOT NULL,
    status ENUM('出勤', '迟到', '早退', '旷课', '请假') NOT NULL,
    remark VARCHAR(100),
    FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE,
    FOREIGN KEY (teaching_plan_id) REFERENCES teaching_plan(id) ON DELETE CASCADE,
    UNIQUE KEY unique_student_course_date (student_id, teaching_plan_id, attend_date)
) ENGINE=InnoDB;

-- 11. schedule 课表表
CREATE TABLE schedule (
    id INT AUTO_INCREMENT PRIMARY KEY,
    teaching_plan_id INT NOT NULL,
    day_of_week INT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    section_start INT NOT NULL COMMENT '第几节开始',
    section_end INT NOT NULL COMMENT '第几节结束',
    classroom VARCHAR(50) NOT NULL,
    campus VARCHAR(50) NOT NULL,
    FOREIGN KEY (teaching_plan_id) REFERENCES teaching_plan(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 12. notice 公告表
CREATE TABLE notice (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    publisher_id INT NOT NULL,
    target_role ENUM('ALL', 'TEACHER', 'STUDENT') NOT NULL DEFAULT 'ALL',
    is_top INT DEFAULT 0 COMMENT '1: 置顶, 0: 普通',
    status INT DEFAULT 1 COMMENT '1: 正常, 0: 撤回',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (publisher_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 13. leave_request 请假申请表
CREATE TABLE leave_request (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    teaching_plan_id INT DEFAULT NULL COMMENT '特定课程ID，全天请假时为NULL',
    start_date DATE NOT NULL,
    start_hour INT NOT NULL DEFAULT 0,
    end_date DATE NOT NULL,
    end_hour INT NOT NULL DEFAULT 23,
    reason VARCHAR(200) NOT NULL,
    status ENUM('待审批', '已批准', '已拒绝') NOT NULL DEFAULT '待审批',
    approver_id INT DEFAULT NULL,
    approve_time TIMESTAMP NULL DEFAULT NULL,
    remark VARCHAR(100) DEFAULT NULL,
    FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE,
    FOREIGN KEY (approver_id) REFERENCES user(id) ON DELETE SET NULL,
    FOREIGN KEY (teaching_plan_id) REFERENCES teaching_plan(id) ON DELETE SET NULL
) ENGINE=InnoDB;
