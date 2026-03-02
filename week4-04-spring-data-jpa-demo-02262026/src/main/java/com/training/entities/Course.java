package com.training.entities;

import java.util.ArrayList;
import java.util.List;



import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity()           // Unique JPA entity name
@Data
@NoArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "course_name", nullable = false)
    private String courseName;

    @Column(name = "instructor")
    private String instructor;

    @Column(name = "duration_weeks")
    private Integer durationWeeks;
    
    @ManyToMany(mappedBy = "courses")
    private List<Student> students = new ArrayList();
    

}
