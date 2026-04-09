package com.springbootexample.service;

import java.util.List;

import org.springframework.data.domain.Page;
import com.springbootexample.entity.Student;

public interface StudentService {
	public Student saveStudent(Student student);
	
	public Student updateStudent(Student student);
	
	public void deleteStudent(long id);
	
	public Student findById(long id);
	
	public List<Student> findAll();

	public Page<Student> getStudents(int pageNumber, int pageSize);
	
	List<Student> getfindBycourse(String course);
	

	Page<Student> findByCourseWithPagination(String course, int pageNumber, int pageSize);
}
