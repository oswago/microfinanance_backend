package com.microfinance.system.entity;

import com.fasterxml.jackson.databind.ser.Serializers;
import com.microfinance.base.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "holiday_calendar")
@Data
public class HolidayCalendar extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private LocalDate holidayDate;
    
    private String description;
    private boolean recurring;
    private String countryCode;
    private boolean active = true;
}