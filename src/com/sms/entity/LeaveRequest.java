package com.sms.entity;

import java.sql.Date;
import java.sql.Timestamp;

public class LeaveRequest {
    private Integer id;
    private Integer studentId;
    private Date startDate;
    private Integer startHour;
    private Date endDate;
    private Integer endHour;
    private String reason;
    private String status; // 待审批, 已批准, 已拒绝
    private Integer approverId;
    private Timestamp approveTime;
    private String remark;
    private Integer teachingPlanId;

    // Joint fields for display
    private String studentNo;
    private String studentName;
    private String className;
    private String approverName;
    private String courseName;

    public Integer getTeachingPlanId() { return teachingPlanId; }
    public void setTeachingPlanId(Integer teachingPlanId) { this.teachingPlanId = teachingPlanId; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public LeaveRequest() {}

    public LeaveRequest(Integer id, Integer studentId, Date startDate, Date endDate, String reason, String status) {
        this.id = id;
        this.studentId = studentId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.status = status;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getStudentId() { return studentId; }
    public void setStudentId(Integer studentId) { this.studentId = studentId; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Integer getStartHour() { return startHour; }
    public void setStartHour(Integer startHour) { this.startHour = startHour; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public Integer getEndHour() { return endHour; }
    public void setEndHour(Integer endHour) { this.endHour = endHour; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getApproverId() { return approverId; }
    public void setApproverId(Integer approverId) { this.approverId = approverId; }

    public Timestamp getApproveTime() { return approveTime; }
    public void setApproveTime(Timestamp approveTime) { this.approveTime = approveTime; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }

    @Override
    public String toString() {
        return "LeaveRequest{" +
                "id=" + id +
                ", studentName='" + studentName + '\'' +
                ", duration=" + startDate + " to " + endDate +
                ", status='" + status + '\'' +
                '}';
    }
}
