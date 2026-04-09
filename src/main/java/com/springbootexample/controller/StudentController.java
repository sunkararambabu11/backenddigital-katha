package com.springbootexample.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.springbootexample.dto.CourseRequest;
import com.springbootexample.dto.PaginationResponse;
import com.springbootexample.entity.Student;
import com.springbootexample.service.StudentService;

@RestController
@RequestMapping("/api/students")
public class StudentController {
	@Autowired
	private StudentService studentService;
	
	@PostMapping
	public Student createStudent(@RequestBody Student student) {
		return studentService.saveStudent(student);
	}
	
	@GetMapping
	public PaginationResponse<Student> getAllStudents(
	        @RequestParam(defaultValue = "0") int pageNumber,
	        @RequestParam(defaultValue = "10") int pageSize) {

	    Page<Student> page = studentService.getStudents(pageNumber, pageSize);

	    return new PaginationResponse<>(
	            page.getContent(),
	            page.getNumber(),
	            page.getSize()
	    );
	}
	
	

	@GetMapping("/{id}")
	public Student getStudentById(@PathVariable long id) {
		return studentService.findById(id);
	}
	
	@PutMapping("/{id}")
	public Student updateStudent(@PathVariable long id,@RequestBody Student student) {
		student.setId(id);
		return studentService.updateStudent(student);
	}
	

	@DeleteMapping("/{id}")
	public void deleteStudent(@PathVariable Long id) {
	    studentService.deleteStudent(id);
	}
	
	@PostMapping("/course")
	public List<Student> getStudentBycourse(@RequestBody CourseRequest request ){
		return studentService.getfindBycourse(request.getCourse());
	}
	@PostMapping("/course-page")
	public Page<Student> getStudentBycoursepage(@RequestBody CourseRequest request){
		return  studentService.findByCourseWithPagination(request.getCourse(),request.getPageNumber(),
				request.getPageSize());
				
		
	}
	
}