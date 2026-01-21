package com.karaoke_management.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_sessions")
public class RoomSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ================= ROOM ================= */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    /* ================= TIME ================= */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    /* ================= STATUS ================= */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomSessionStatus status;

    /* ================= USER ================= */
    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "checked_out_by")
    private String checkedOutBy;

    /* ================= CONSTRUCTOR ================= */
    public RoomSession() {}

    /* ================= GETTER / SETTER ================= */
    public Long getId() { return id; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public RoomSessionStatus getStatus() { return status; }
    public void setStatus(RoomSessionStatus status) { this.status = status; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getCheckedOutBy() { return checkedOutBy; }
    public void setCheckedOutBy(String checkedOutBy) { this.checkedOutBy = checkedOutBy; }
}
