package possg.com.a.dao;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import possg.com.a.dto.ConvenienceDto;

@Mapper
@Repository
public interface ConvenienceDao {
	//아이디 중복체크
	int idcheck(String userId);	
	
	//회원가입
	int adduser(ConvenienceDto dto);
	
	//로그인
	ConvenienceDto login(ConvenienceDto dto);
	
	// 아이디 찾기	
	ConvenienceDto findUserByAddressAndPhoneNumber(String representativeName, String phoneNumber);
	
	// 패스워드 업데이트
	void changePassword(ConvenienceDto dto);
		
	// 비밀번호 찾기
	ConvenienceDto findUserByAddressAndUserId(String phoneNumber, String userId);
	
	// 마이페이지 내정보 변경
	int updateMypage(ConvenienceDto dto);
	
	//마이페이지
	ConvenienceDto mypage(String userId);
	
	//키 중복체크
	int keycheck(String convKey);
	
	int updateCodeStatus(ConvenienceDto dto);	

}