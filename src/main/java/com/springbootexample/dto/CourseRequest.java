package com.springbootexample.dto;

public class CourseRequest {
private String course;
public int getPageNumber() {
	return pageNumber;
}

public void setPageNumber(int pageNumber) {
	this.pageNumber = pageNumber;
}

public int getPageSize() {
	return pageSize;
}

public void setPageSize(int pageSize) {
	this.pageSize = pageSize;
}

private int pageNumber;
private int pageSize;

public String getCourse() {
	return course;
}

public void setCourse(String course) {
	this.course = course;
}
}
