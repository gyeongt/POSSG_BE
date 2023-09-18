package possg.com.a.dao;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import possg.com.a.dto.PaymentDto;

@Mapper
@Repository
public interface PaymentDao {
	
	int addkakaopayment(PaymentDto dto);
	int cancelkakaopayment(int seq);
}
