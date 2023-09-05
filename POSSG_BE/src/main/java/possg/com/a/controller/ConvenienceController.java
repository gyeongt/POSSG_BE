package possg.com.a.controller;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import possg.com.a.dto.ConvenienceDto;
import possg.com.a.dto.MessageDto;
import possg.com.a.dto.SmsRequestDto;
import possg.com.a.dto.SmsResponseDto;
import possg.com.a.service.ConvenienceService;
import possg.com.a.util.SecurityConfig;
import possg.com.a.util.VerificationCode;

@RestController
public class ConvenienceController {

	@Autowired
	ConvenienceService service;
	
	@Autowired
	SecurityConfig securityConfig;
	
	@Value("${naver-cloud-sms.accessKey}")
    private String accessKey;

	 @Value("${naver-cloud-sms.secretKey}")
	    private String secretKey;
	 
	 @Value("${naver-cloud-sms.serviceId}")
	    private String serviceId;
	 
	 @Value("${naver-cloud-sms.senderPhone}")
	    private String senderPhone;
	 
	 public long verificationCodeGenerationTime;	// 인증번호 보낼 시점 시간 저장
	
	
	@PostMapping("idcheck")
	public String idcheck(String userId) {
		System.out.println("ConvenienceController idcheck " + new Date());
		
		System.out.println(userId);
		int count = service.idcheck(userId);
		System.out.println(count);
		if (count == 0) {
			return "YES";
		}

		return "NO";
	}
	
	
	@PostMapping("login")
	public ResponseEntity<Map<String, String>> login(@RequestBody ConvenienceDto conv, HttpServletResponse response) {
		System.out.println("ConvenienceController login() " + new Date());
	    ConvenienceDto dto = service.login(conv);
	    

	    if (dto != null) {
	        // Access Token 생성
	        String accessToken = securityConfig.generateJwtToken(dto);

	        // Refresh Token 생성
	        String refreshToken = securityConfig.generateRefreshToken(dto);
	        System.out.println(refreshToken);
	        
	        Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
            accessTokenCookie.setHttpOnly(true);
            accessTokenCookie.setMaxAge(86400); // 토큰 유효 시간 (초 단위)
            response.addCookie(accessTokenCookie);

            Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setMaxAge(86400 * 7); // 토큰 유효 시간 (초 단위)
            response.addCookie(refreshTokenCookie);

	        Map<String, String> tokens = new HashMap<>();
	        tokens.put("accessToken", accessToken);
	        tokens.put("refreshToken", refreshToken);
	        
	        return ResponseEntity.ok(tokens);
	    }
	    System.out.println("login fail");
	    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
	}

	@PostMapping("refresh")
	public ResponseEntity<Map<String, String>> refreshAccessToken(@RequestParam("refreshToken")String refreshToken, HttpServletResponse response) {		
		System.out.println("ConvenienceController refresh() " + new Date());
	    try {
	        // Refresh Token을 파싱하여 유효성 검사
	    	
	    	JwtParser jwtParser = Jwts.parserBuilder()
	    		    .setSigningKey(securityConfig.securityKey) // 여기서 secretKey는 생성한 시크릿 키입니다.
	    		    .build();
	    	
	    	
	        Claims refreshClaims = jwtParser.parseClaimsJws(refreshToken).getBody();

	        String userId = refreshClaims.get("userId", String.class);

	        ConvenienceDto userDto = service.mypage(userId); // 사용자 정보 가져오기 등
	        
	        // 새로운 Access Token 발급
	        String newAccessToken = securityConfig.generateRefreshToken(userDto);
	        
	        Cookie accessTokenCookie = new Cookie("accessToken", newAccessToken);
            accessTokenCookie.setHttpOnly(true);
            accessTokenCookie.setMaxAge(86400); // 토큰 유효 시간 (초 단위)
            response.addCookie(accessTokenCookie);

	        Map<String, String> tokens = new HashMap<>();

	        tokens.put("accessToken", newAccessToken);

	        return ResponseEntity.ok(tokens);
	    } catch (Exception e) {
	    	
	    	System.out.println("refresh fail");
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
	    }
	}
	
	 @PostMapping("adduser") 
	 public String adduser(@RequestBody ConvenienceDto conv) {
	 System.out.println("ConvenienceController adduser() " + new Date());
	 
	 Date currentTime = new Date();
     SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
     String formattedTime = dateFormat.format(currentTime);
     
     conv.setRegistrationDate(formattedTime);
	 
	 String originPwd = conv.getPwd();
	 
	 String hashPwd = sha256(originPwd);
	 
	 conv.setPwd(hashPwd); 
	 
	 int count1 = service.updateCodeStatus(conv);
	 int count = service.adduser(conv);
	 if(count == 1 && count1 != 0){  		 
		 return "YES"; 
	 } 
	 return "NO"; 
	 }
	 
	 @PostMapping("updateCodeStatus")
	 public String updateCodeStatus(ConvenienceDto conv) {
		 System.out.println("ConvenienceController updateCodeStatus() " + new Date());
		 
		 System.out.println(conv);
		 
		 int count = service.updateCodeStatus(conv);
		 
		 if(count != 0) {
			 return "YES";
		 }
		 return "NO";
	 }
	 
	 @PostMapping("findId")
		public ResponseEntity<?> findId(@RequestParam(value="representativeName", required=false) String representativeName,
		                                @RequestParam(value="phoneNumber", required=false) String phoneNumber) {
		 
		 System.out.println("ConvenienceController findId() " + new Date());
		    Map<String, String> response = new HashMap<>();
		    
		    // 폰번호랑 이메일 둘 다 안적으면 안됨
		    if (representativeName == null || phoneNumber == null) {
		        response.put("errorMessage", "이메일 주소와 전화번호 둘 다 제공해주세요.");
		        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		    }

		    // 폰번호랑 이메일 둘다 있어야 찾아짐
		    ConvenienceDto user = service.findUserByAddressAndPhoneNumber(representativeName, phoneNumber);
		    
		    if (user != null) {
		        response.put("user_id", user.getUserId());
		        return new ResponseEntity<>(response, HttpStatus.OK);
		    } else {
		        response.put("errorMessage", "해당 정보로 가입된 아이디가 없습니다.");
		        return new ResponseEntity<>(response, HttpStatus.OK);
		    }
		}
	 
	 @PostMapping("updateMypage")
	 public String updateMypage(@RequestBody ConvenienceDto conv) {
		 System.out.println("ConvenienceController updateMypage() " + new Date());
		 
		 System.out.println(conv);
		 
		 int count = service.updateMypage(conv);
		 
		 if(count != 0) {
			 return "YES";
		 }		 
		 return "NO";
	 }
	 
	 @GetMapping("mypage")
	 public ConvenienceDto mypage(@RequestHeader("accessToken") String tokenHeader) {
		 System.out.println("ConvenienceController mypage() " + new Date());
		 
		 String accessToken = tokenHeader.replace("Bearer ", "");
		 
		 System.out.println(accessToken);
		 
		 JwtParser jwtParser = Jwts.parserBuilder()
	    		    .setSigningKey(securityConfig.securityKey) // 여기서 secretKey는 생성한 시크릿 키입니다.
	    		    .build();
	    	
	    	
	        Claims refreshClaims = jwtParser.parseClaimsJws(accessToken).getBody();
		 
		 String userId = refreshClaims.get("userId", String.class);

		 ConvenienceDto user = service.mypage(userId);
		 
		 if(userId.equals(user.getUserId())) {
			 return user;
		 }
		 
		 return null;
	 }
	 
	 @PostMapping("keycheck")
	 public String keycheck(String convKey) {
		 System.out.println("ConvenienceController keycheck() " + new Date());	 
		 
		 System.out.println(convKey);
		 		 
		 int count = service.keycheck(convKey);
		 System.out.println(count);
		 if(count != 0) {
			 return "YES";
		 }	 
		 return "NO";
	 }

	 
	 @PostMapping("send")
	    public ResponseEntity<?> sendSms(@RequestBody MessageDto messageDto) throws Exception {
		 System.out.println("ConvenienceController sendSms() " + new Date());
		 String temp = messageDto.getTo();
		 String formattedNumber = temp.replaceAll("(\\d{3})(\\d{4})(\\d{4})", "$1-$2-$3");
		 		 
		 ConvenienceDto conv = service.mypage(messageDto.getContent());
		 String phoneNum = conv.getPhoneNumber();	
	    int veri = number();

		 if(messageDto.getContent().equals(conv.getUserId()) && phoneNum.equals(formattedNumber)) {
			 verificationCodeGenerationTime = System.currentTimeMillis();
	            SmsResponseDto response = sendSmsForSmsCert(messageDto, veri);	            
	            
	            return ResponseEntity.ok(response);   
		 }
		 return ResponseEntity.badRequest().body("SMS 전송 실패");
	    }
	 
	 
	 @PostMapping("regisend")
	    public ResponseEntity<?> regisend(@RequestBody MessageDto messageDto) throws Exception {
		 System.out.println("ConvenienceController sendSms() " + new Date());
		 
		 int veri = number();	   		
		 
			 verificationCodeGenerationTime = System.currentTimeMillis();
			 	System.out.println("send time" + verificationCodeGenerationTime);
	            SmsResponseDto response = sendSmsForSmsCert(messageDto, veri);  
	            return ResponseEntity.ok(response);   
	    }
	 

	 
	 @PostMapping("Authentication")
	 public String Authentication(@RequestParam int CodeNumber) {		 
		 System.out.println("ConvenienceController Authentication() " + new Date());
		 
		 
		 System.out.println(CodeNumber);
	
		 long currentTime = System.currentTimeMillis();
		 
		 if(currentTime - verificationCodeGenerationTime <= 300000) {			
			 
				 return "YES";
			 
		 }	
		 return "NO";
	 }
	 
	 // 인증 성공하면 비밀번호 찾기 창 열어주기 그리고 비밀번호 입력하면 비밀번호 변경시켜주고 통과	 
	 @PostMapping("changePassword")
	 public String changePassword(@RequestBody ConvenienceDto userDto) {
		 System.out.println("ConvenienceController changePassword() " + new Date());
		   	   
		   if(userDto != null) {
			   
			   ConvenienceDto id = new ConvenienceDto();
			   id.setUserId(userDto.getUserId());
			   
			   // 비밀번호 변경
			   String hashedPassword = sha256(userDto.getNewPwd());
			   id.setPwd(hashedPassword);
			   
			   service.changePassword(id);
			   return "YES";
		   }	  		   
		   return "NO";
		}

	
	 // 비밀번호 해시화 (SHA-256 사용) 
	public static String sha256(String pw) { try {
		MessageDigest md = MessageDigest.getInstance("SHA-256"); byte[] hash =
		md.digest(pw.getBytes("UTF-8")); StringBuffer hexString = new StringBuffer();
	 
	 	for (int i = 0; i < hash.length; i++) { String hex = Integer.toHexString(0xff
	 		& hash[i]); if (hex.length() == 1) { hexString.append('0'); }
	 		hexString.append(hex); } return hexString.toString(); } catch (Exception e) {
		 return ""; 
	 	}
	
	 	}
	
	//문자 보내기
	public SmsResponseDto sendSmsForSmsCert(MessageDto dto, int number) throws Exception {
	    String time = Long.toString(System.currentTimeMillis());
	    System.out.println("test1~~~~~~~~~~~~~~~~~~~~");
	    
	    int veri = number;
	    
		String message = "POSSG 본인확인 인증번호 입니다 ["+ veri + "]인증번호를 입력해 주세요";
	
	    List<MessageDto> smsMessageList = new ArrayList<>();
	    MessageDto smsMessage = new MessageDto(dto.getTo(), message);
	    smsMessageList.add(smsMessage);
	    
	    SmsRequestDto smsRequest = new SmsRequestDto();
        smsRequest.setMessages(smsMessageList);

        // json 형태로 변환
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonBody = objectMapper.writeValueAsString(smsRequest);
	    
	    System.out.println("test2~~~~~~~~~~~~~~~~~~~~");
	    
	    HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
	    headers.set("x-ncp-apigw-timestamp", time);
	    headers.set("x-ncp-iam-access-key", accessKey);
	    headers.set("x-ncp-apigw-signature-v2", getSignature(time));
	    System.out.println("test3~~~~~");

	    RestTemplate restTemplate = new RestTemplate();
	    HttpEntity<String> body = new HttpEntity<>(jsonBody, headers);

	    SmsResponseDto smsResponse = restTemplate.postForObject(
	            "https://sens.apigw.ntruss.com/sms/v2/services/" + serviceId + "/messages",
	            body,
	            SmsResponseDto.class
	    );

	    System.out.println(smsResponse + "controller");
	    return smsResponse;
	}
	
	//인증번호 생성
		public static int number() {
				 
				 Random random = new Random();
			        int min = 100000;
			        int max = 999999;
			        int verificationCode = random.nextInt(max - min + 1) + min;	        
			        
			       int verificationCodes = verificationCode;
			        
			     return verificationCodes; 
			 }
 

 // 문자 알고리즘 암호화
 private String getSignature(String time) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        String space = " ";
        String newLine = "\n";
        String method = "POST";
        String url = "/sms/v2/services/" + serviceId + "/messages";

        String message = new StringBuilder()
                .append(method)
                .append(space)
                .append(url)
                .append(newLine)
                .append(time)
                .append(newLine)
                .append(accessKey)
                .toString();

        SecretKeySpec signingKey = new SecretKeySpec(secretKey.getBytes("UTF-8"), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(signingKey);

        byte[] rawHmac = mac.doFinal(message.getBytes("UTF-8"));
        String encodeBase64String = Base64.getEncoder().encodeToString(rawHmac);

        return encodeBase64String;
    } 
	

}
