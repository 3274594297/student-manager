-- Init script for studentsql_manage database (same schema as init.sql)
DROP TABLE IF EXISTS leave_request;
DROP TABLE IF EXISTS notice;
DROP TABLE IF EXISTS schedule;
DROP TABLE IF EXISTS attendance;
DROP TABLE IF EXISTS score;
DROP TABLE IF EXISTS teaching_plan;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS class;
DROP TABLE IF EXISTS teacher;
DROP TABLE IF EXISTS major;
DROP TABLE IF EXISTS department;
DROP TABLE IF EXISTS user;

-- user table
CREATE TABLE user (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(64) NOT NULL,
    salt VARCHAR(32) NOT NULL,
    role ENUM('ADMIN','TEACHER','STUDENT') NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    status INT DEFAULT 1 COMMENT '1: 启用, 0: 禁用',
    failed_attempts INT DEFAULT 0,
    lock_until TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- department table
CREATE TABLE department (
    id INT AUTO_INCREMENT PRIMARY KEY,
    dept_no VARCHAR(20) NOT NULL UNIQUE,
    dept_name VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;

-- major table
CREATE TABLE major (
    id INT AUTO_INCREMENT PRIMARY KEY,
    major_no VARCHAR(20) NOT NULL UNIQUE,
    major_name VARCHAR(50) NOT NULL,
    dept_id INT NOT NULL,
    duration_years INT NOT NULL DEFAULT 4,
    FOREIGN KEY (dept_id) REFERENCES department(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- teacher table
CREATE TABLE teacher (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNIQUE NOT NULL,
    teacher_no VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    gender VARCHAR(10) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(50),
    title VARCHAR(50) COMMENT '职称',
    department_id INT NOT NULL,
    status INT DEFAULT 1 COMMENT '1: 在职, 0: 离职',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES department(id)
) ENGINE=InnoDB;

-- class table
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

-- student table
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

-- Additional tables (course, teaching_plan, etc.) can be added as needed.
