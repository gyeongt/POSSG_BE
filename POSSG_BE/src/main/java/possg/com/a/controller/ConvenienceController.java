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
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
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
import possg.com.a.dto.SmsDto;
import possg.com.a.dto.SmsRequestDto;
import possg.com.a.dto.SmsResponseDto;
import possg.com.a.dto.TokenDto;
import possg.com.a.service.ConvenienceService;
import possg.com.a.util.SecurityConfig;
import possg.com.a.util.VerificationCode;

@RestController
@CrossOrigin(origins = "http://localhost:3000") //CROS 설정
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
	public ResponseEntity<?> login(@RequestBody ConvenienceDto conv) {
		System.out.println("ConvenienceController login() " + new Date());
	    ConvenienceDto dto = service.login(conv);    

	    if (dto != null) {
	        // Access Token 생성
	        String accessToken = securityConfig.generateJwtToken(dto);

	        // Refresh Token 생성
	        String refreshToken = securityConfig.generateRefreshToken(dto);
	        System.out.println(refreshToken);
	        
	        TokenDto token = new TokenDto();
	      
	        token.setUserId(conv.getUserId());
	        token.setRefresh(refreshToken);
	        
	        int refresh = service.insertToken(token);
     
	        // HTTP 요청 헤더 설정
	        HttpHeaders headers = new HttpHeaders();
	        headers.add("accessToken", accessToken);
	        
	        if(refresh == 0) {
	        	ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("NO");
	        }	

	        return ResponseEntity.ok().headers(headers).body("YES");   
	    }
	    System.out.println("login fail");
	    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("NO");
	}

	@PostMapping("refresh")
	public ResponseEntity<Map<String, String>> refreshAccessToken(@RequestHeader("accessToken")String accessToken) {		
		System.out.println("ConvenienceController refresh() " + new Date());
	   
	    	
	    	if(accessToken != null) {
	        // Refresh Token을 파싱하여 유효성 검사	    	
	    	JwtParser jwtParser = Jwts.parserBuilder()
	    		    .setSigningKey(securityConfig.securityKey) // 여기서 secretKey는 생성한 시크릿 키입니다.
	    		    .build();
	    	System.out.println(jwtParser);
	    	// userId 파싱
	        Claims refreshClaims = jwtParser.parseClaimsJws(accessToken).getBody();	        	        
	        String userId = refreshClaims.get("userId", String.class);
	        
	        // 유효기간 파싱
	        Date expirationDate = refreshClaims.getExpiration();        
	        Date date = new Date();
	        
	        // 유저가 아님 
	        if(userId == null) {
	        	System.out.println("넌 유저가 아니다");
	        	return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
	        }	        	        	        	        

	        ConvenienceDto userDto = service.mypage(userId); // 사용자 정보 가져오기 등
	        List<TokenDto> userToken = service.selectToken(userId);
	        
	        System.out.println(userToken);
	        
	        // 로그인 아이디랑 일치하는 토큰이 없는거
	        if(userToken == null) {
	        	System.out.println("유효한 refresh토큰이 없습니다.");
	        	return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
	        }
	        	        
	        // 새로운 Access Token 발급
	        String newAccessToken = securityConfig.generateJwtToken(userDto);
	       
	        Map<String, String> tokens = new HashMap<>();

	        tokens.put("accessToken", newAccessToken);
	        
	     // 토큰 기간이 만료됨
	        if(expirationDate.before(date)) {
	        	System.out.println("토큰이 만료됨");	        	
	        	// 그래도 유저인게 확인되면 토큰발행
	        	if(userId != null) {
	        		return ResponseEntity.ok(tokens);
	        	}	        		
	        }

	        return ResponseEntity.ok(tokens);
	    } 
	    	
	    	System.out.println("refresh fail");
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
	    
	}
	
	 @PostMapping("addUser") 
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
	 
	 @GetMapping("myPage")
	    public ConvenienceDto mypage(@RequestHeader("accessToken") String tokenHeader) {
	        // "Bearer " 문자열을 제거하여 실제 토큰을 추출
	        String accessToken = tokenHeader.replace("Bearer ", "");

	        // JWT 토큰 검증
	        	JwtParser jwtParser = Jwts.parserBuilder()
		    		    .setSigningKey(securityConfig.securityKey)
		    		    .build();

	            Claims claims = jwtParser.parseClaimsJws(accessToken).getBody();

	            // 사용자 ID 추출
	            String userId = claims.get("userId", String.class);
	            
	            System.out.println(userId);

	            // 사용자 정보를 서비스에서 가져오기
	            ConvenienceDto user = service.mypage(userId);
	            System.out.println(user);
	            
	            if(user == null) {
		        	   return null;
		           }
	            // 사용자 정보 반환
	            return user;	          
	    }
	 
	 /*
	 @GetMapping("myPage")
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
	 */
	 @PostMapping("keyCheck")
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
		
		 System.out.println(temp);
		 		 
		 ConvenienceDto conv = service.mypage(messageDto.getContent());
		 String phoneNum = conv.getPhoneNumber();	
	    int veri = number();

		 if(messageDto.getContent().equals(conv.getUserId()) && phoneNum.equals(temp)) {
			 verificationCodeGenerationTime = System.currentTimeMillis();
	            SmsResponseDto response = sendSmsForSmsCert(messageDto, veri);
	            
	            
	            service.insertSms(veri);
	            
	            return ResponseEntity.ok(response);   
		 }
		 return ResponseEntity.badRequest().body("SMS 전송 실패");
	    }
	 
	 
	 @PostMapping("regiSend")
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
		 
		 // 코드 넘버 확인하고 db랑 비교 후 맞으면 yes 틀리면 no
		 System.out.println(CodeNumber);
		 
		 int smsNum = service.selectSms(CodeNumber);
		 
		 System.out.println(smsNum);
		 
		 if(smsNum == 0) {
			 System.out.println("db에 일치하는 인증번호가 없습니다.");
			 return "NO";
		 }	 
	
		 long currentTime = System.currentTimeMillis();
		 System.out.println(currentTime);
		 
		 System.out.println(verificationCodeGenerationTime);
		  
		 if(currentTime - verificationCodeGenerationTime <= 300000 && smsNum == 1) {			
			 				 
			 service.deleteSms(CodeNumber);		 
			 
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
