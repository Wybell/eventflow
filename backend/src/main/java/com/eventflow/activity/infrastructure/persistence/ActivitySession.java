package com.eventflow.activity.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.eventflow.activity.domain.ActivitySessionStatus;
import java.time.LocalDateTime;

@TableName("ef_activity_session")
public class ActivitySession {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long activityId;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalQuota;
    private Integer availableQuota;
    private Integer reservedQuota;
    private Integer confirmedQuota;
    private ActivitySessionStatus status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getTotalQuota() {
        return totalQuota;
    }

    public void setTotalQuota(Integer totalQuota) {
        this.totalQuota = totalQuota;
    }

    public Integer getAvailableQuota() {
        return availableQuota;
    }

    public void setAvailableQuota(Integer availableQuota) {
        this.availableQuota = availableQuota;
    }

    public Integer getReservedQuota() {
        return reservedQuota;
    }

    public void setReservedQuota(Integer reservedQuota) {
        this.reservedQuota = reservedQuota;
    }

    public Integer getConfirmedQuota() {
        return confirmedQuota;
    }

    public void setConfirmedQuota(Integer confirmedQuota) {
        this.confirmedQuota = confirmedQuota;
    }

    public ActivitySessionStatus getStatus() {
        return status;
    }

    public void setStatus(ActivitySessionStatus status) {
        this.status = status;
    }
}
