package possg.com.a.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import possg.com.a.dto.EmployeeDto;

@Mapper
@Repository
public interface EmployeeDao {
	int addemployee(EmployeeDto dto);
	int updateEmployee(EmployeeDto dto);
	List<EmployeeDto> findemployee(EmployeeDto dto);
	List<EmployeeDto> findallemployee(int convSeq);
	int terminateEmployee(int employeeSeq);
}
