package com.springbootexample.service;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.springbootexample.entity.Student;
import com.springbootexample.repository.StudentRepository;

@Service
public class StudentServiceImpl implements StudentService {
	@Autowired
	private StudentRepository studentRepository;

	@Override
	public Student saveStudent(Student student) {
		return studentRepository.save(student) ;
	}

	@Override
	public Student updateStudent(Student student) {
		return studentRepository.save(student);
	}

	@Override
	
	public void deleteStudent(long id) {
		studentRepository.deleteById(id);
	}
	
	@Override
	public Student findById(long id) {
		return studentRepository.findById(id).get();
	}

	@Override
	public List<Student> findAll() {
		return studentRepository.findAll();
	}
	
    @Override
    public Page<Student> getStudents(int pageNumber, int pageSize) {

        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        return studentRepository.findAll(pageable);
    }
    
    @Override
    public List<Student> getfindBycourse(String course){
    	return studentRepository.findByCourseIgnoreCase(course);
    }
    
    
    @Override
    public Page<Student> findByCourseWithPagination(String course, int pageNumber, int pageSize) {

        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        return studentRepository.findByCourseIgnoreCase(course, pageable);
    }
    
}
    


