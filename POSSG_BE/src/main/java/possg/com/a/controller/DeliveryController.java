package possg.com.a.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import possg.com.a.dto.ConvenienceDto;
import possg.com.a.dto.CustomerDto;
import possg.com.a.dto.DeliveryDto;
import possg.com.a.dto.DeliveryJoinDto;
import possg.com.a.dto.DeliveryListDto;
import possg.com.a.dto.DeliveryParam;
import possg.com.a.dto.ProductDto;
import possg.com.a.service.ConvenienceService;
import possg.com.a.service.DeliveryService;
import possg.com.a.service.ProductService;
import possg.com.a.util.SecurityConfig;
import possg.com.a.util.TokenCreate;

@RestController
public class DeliveryController {

	@Autowired
	DeliveryService service;
	
	private TokenCreate tokenCreate;
	
	@Autowired
	public DeliveryController(TokenCreate tokenCreate) {
		this.tokenCreate = tokenCreate;
	}
	
	@Autowired
	ProductService productService;
	
	
	// 배달 장바구니에 추가
	@PostMapping("callAddDelivery")
	public String callAddDelivery(@RequestBody DeliveryDto dto, @RequestHeader("accessToken") String tokenHeader) {
		System.out.println("DeliveryController callAddDelivery " + new Date());
		
		if(tokenHeader == null) {
			return null;
		}
		
		int customerSeq = tokenParser(tokenHeader);
		System.out.println(customerSeq);
		
		CustomerDto userId = service.selectCustomer(customerSeq);
		System.out.println(userId);
		// 갯수에 따라 가격 맞춰서 넣기			
		ProductDto product = new ProductDto();
		
		product.setProductName(dto.getProductName());
		product.setConvSeq(dto.getConvSeq());

		DeliveryDto delivery = new DeliveryDto();
		delivery.setUserId(customerSeq);
		
		List<DeliveryDto> deli = service.selectDelivery(delivery);
		List<ProductDto> prodto = productService.findProductName(product);
		System.out.println(prodto);
		dto.setPrice(dto.getQuantity() * prodto.get(0).getPrice());
		dto.setUserId(customerSeq);
		dto.setLocation(userId.getLocation());
		dto.setBranchName(userId.getBranchName());
		dto.setDiscountRate(prodto.get(0).getDiscountRate());
		dto.setPromotionInfo(prodto.get(0).getPromotionInfo());
		
		// 장바구니에 이미 같은 이름의 상품이 있을경우
		boolean isProductNameFound = false;

		for (DeliveryDto deliveryDto : deli) {
		    if (deliveryDto.getProductName().equals(dto.getProductName())) {
		        isProductNameFound = true;
		        break;
		    }
		}

		if (isProductNameFound) {
		    int count = service.updateCountDelivery(dto);

		    if (count != 0) {
		        return "YES";
		    }
		        return "NO";		    
		}
		
		System.out.println(dto);
		// 성공 실패
		int count = service.callAddDelivery(dto);						
		if(count != 0) {
			return "YES";
		}
		return "NO";
	}
	
	// 주문전 배달 장바구니 목록 보여주기
	@GetMapping("selectDelivery")
	public List<DeliveryDto>selectDelivery(@RequestHeader("accessToken") String tokenHeader) {
		System.out.println("DeliveryController selectDelivery " + new Date());
		
		if(tokenHeader == null) {
			return null;
		}
		
		int customerSeq = tokenParser(tokenHeader);
		
		DeliveryDto newDeli = new DeliveryDto();
		
		newDeli.setUserId(customerSeq);
		
		List<DeliveryDto> deli = service.selectDelivery(newDeli);
		
		System.out.println(deli);
		
		return deli;
	}
	
	// 
	// 배달 주문하기 
	@PostMapping("insertDeliveryList")
	public String insertDeliveryList(@RequestBody DeliveryListDto dto, @RequestHeader("accessToken") String tokenHeader) {
		System.out.println("DeliveryController callAddDelivery " + new Date());
		
		if(tokenHeader == null) {
			return "NO";
		}
		
		int customerSeq = tokenParser(tokenHeader);
			
		DeliveryDto delivery = new DeliveryDto();
		
		delivery.setUserId(customerSeq);

		List<DeliveryDto> deli = service.selectDelivery(delivery);

		int price = 0;
		int quantity = 0;
		for(DeliveryDto forDto :deli) {
			price = price + forDto.getPrice();
			quantity = quantity + forDto.getQuantity();		
		}		
		 // 현재 날짜와 시간 얻기
        LocalDateTime currentDateTime = LocalDateTime.now();        
        // 날짜와 시간을 원하는 형식으로 포맷팅
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = currentDateTime.format(formatter);
        
        formattedDateTime = formattedDateTime.replaceAll("[^\\d]", "");
        
        dto.setUserId(delivery.getUserId());
        dto.setDelTotalPrice(price);
		dto.setDelTotalNumber(quantity);
		dto.setDelRef(formattedDateTime);
		
		if(dto.getDelTotalNumber() == 0 || dto.getDelTotalPrice() == 0) {
			System.out.println("물건이 안담겨 있음");
			return "NO";
		}
		
		int count = service.insertDeliveryList(dto);
		
		delivery.setRef(formattedDateTime);
		service.updateDelivery(delivery);

		if(count != 0) {
			return "YES";
		}
		return "NO";
	}
	// -----------------------------------여기서 부터는 점주 배달 기능입니다 ----------------------------------------------
	
	// 점주 배달 추가
		@PostMapping("convAddDelivery")
		public String convAddDelivery(@RequestBody ConvenienceDto dto, @RequestHeader("accessToken") String accessToken) {
			System.out.println("DeliveryController convAddDelivery " + new Date());
			
			//토큰 파싱
					accessToken = accessToken.replace("Bearer ", "");
					 System.out.println(dto);
					 JwtParser jwtParser = Jwts.parserBuilder()
				    		    .setSigningKey(tokenCreate.securityKey)
				    		    .build();
				    	
				        Claims refreshClaims = jwtParser.parseClaimsJws(accessToken).getBody();
					 
				        int convSeq = refreshClaims.get("convSeq", Integer.class);		 
					 	 
					 dto.setConvSeq(convSeq);

					 int count = service.convAddDelivery(dto);
			
			if(count !=  0) {
				return "YES";
			}		
			return "NO";
		}
	
		// convenience token에서 branch_name을 추출해서 delivery테이블에 branch_name 같고 order_status != 0 인 목록을 가져와서 ref같은거 끼리 묶어서 보여주기\

		@GetMapping("convenienceDeliveryList")
		public Map<String, Object> convenienceDeliveryList(DeliveryParam param, @RequestHeader("accessToken") String tokenHeader) {
		    System.out.println("DeliveryController convenienceDeliveryList " + new Date());

		    if (tokenHeader != null) {
		       
		        Claims claim = tokenCreate.getClaims(tokenHeader);

		        // 지점명 추출
		        String branchName = claim.get("branchName", String.class);
		        
		        param.setBranchName(branchName);
		        
		        System.out.println(param);
		        
		        // 모든 데이터 추출
		        List<DeliveryJoinDto> dto = service.convenienceDeliveryList(param);
		        Set<Integer> uniqueProductSeqs = new HashSet<>();
		        List<DeliveryJoinDto> uniqueDtos = new ArrayList<>();

		        for (DeliveryJoinDto dtos : dto) {
		            int productSeq = dtos.getProductSeq();

		            // uniqueProductSeqs에 ProductSeq가 중복되지 않는 경우만 uniqueDtos에 추가합니다.
		            if (!uniqueProductSeqs.contains(productSeq)) {
		                uniqueProductSeqs.add(productSeq);
		                uniqueDtos.add(dtos);
		            }
		        }       
		        System.out.println(uniqueDtos);
		        // 그룹화된 데이터를 담을 List
		        List<Map<String, Object>> groupedData = new ArrayList<>();
		        Map<String, Object> deliveryMap = new LinkedHashMap<>();

		        for (DeliveryJoinDto deliveryJoinDto : uniqueDtos) {
		            // 필드 값 설정
		            
		            deliveryMap.put("userId", deliveryJoinDto.getUserId());
		            deliveryMap.put("orderStatus", deliveryJoinDto.getOrderStatus());
		            deliveryMap.put("ref", deliveryJoinDto.getRef());
		            deliveryMap.put("location", deliveryJoinDto.getLocation());
		            deliveryMap.put("branchName", deliveryJoinDto.getBranchName());
		            deliveryMap.put("seq", deliveryJoinDto.getSeq());
		            deliveryMap.put("delDate", deliveryJoinDto.getDelDate());
		            deliveryMap.put("delStatus", deliveryJoinDto.getDelStatus());
		            deliveryMap.put("delTotalNumber", deliveryJoinDto.getDelTotalNumber());
		            deliveryMap.put("delTotalPrice", deliveryJoinDto.getDelTotalPrice());
		            deliveryMap.put("delRemark", deliveryJoinDto.getDelRemark());
		            deliveryMap.put("delRef", deliveryJoinDto.getDelRef());

					List<Map<String, Object>> deliveryDetails = new ArrayList<>();

					 Set<Integer> addedProductSeqs = new HashSet<>(); 

					    for (DeliveryJoinDto item : dto) {
					        if (item.getSeq() == deliveryJoinDto.getSeq()) {
					            int productSeq = item.getProductSeq();

					            if (!addedProductSeqs.contains(productSeq)) {
					                Map<String, Object> detail = new HashMap<>();
					                detail.put("product_name", item.getProductName());
					                detail.put("quantity", item.getQuantity());
					                detail.put("price", item.getPrice());
					                detail.put("product_seq", productSeq);
					                deliveryDetails.add(detail);
					                
					                addedProductSeqs.add(productSeq);
					            }
					        }
					    }

		            

		            deliveryMap.put("details", deliveryDetails);

		            // 그룹화된 데이터를 추가
		            groupedData.add(deliveryMap);
		        }
		        
		        System.out.println(groupedData);

		        // 중복된 데이터를 그룹화
		        List<Map<String, Object>> uniqueGroupedData = new ArrayList<>();
		        for (Map<String, Object> dataMap : groupedData) {
		            if (!uniqueGroupedData.contains(dataMap)) {
		                uniqueGroupedData.add(dataMap);
		            }
		            
		        }
		        
		        System.out.println(uniqueGroupedData);
		     // 편의점 보유 상품 총 개수
			    int count = service.getDeliveryCount(param);
				
			    
			    int AllPage = count / 10;
				if((count % 10) > 0) {
					AllPage = AllPage + 1;
				}	
		    			
								
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("DeliveryList", uniqueGroupedData);
				map.put("AllPage", AllPage);
				map.put("PageNumber", param.getPageNumber());
				map.put("Cnt", count);	//리액트 할때 추가해줬던거
				
				
		        // 결과 반환
		        if (!uniqueGroupedData.isEmpty()) {
		            return map;
		        } else {
		            return null;
		        }
		    }
		    return null;
		}
		
		@PostMapping("statusUpdate")
		public String statusUpdate(@RequestBody DeliveryJoinDto dto, @RequestHeader("accessToken") String accessToken) {
			 System.out.println("DeliveryController statusUpdate " + new Date());			
						 
	        int statusUpdate = service.statusUpdate(dto);
	        
	      if(statusUpdate != 0) {
	    	  return "YES";
	      }
	        return "NO";	        	        
		}

		
		// 배달목록 누르면 detail 페이지 상세보기
		@GetMapping("allDeliveryList")
		public List<Map<String, Object>> allDelivery(@RequestBody String ref, @RequestHeader("accessToken") String accessToken) {
			System.out.println("DeliveryController allDeliveryList " + new Date());
			
			DeliveryDto dto = new DeliveryDto();
			
			Claims claim = tokenCreate.getClaims(accessToken);
			
			String branchName = claim.get("branchName", String.class); 
			
			dto.setBranchName(branchName);
			dto.setRef(ref);
			System.out.println(dto);
			List<DeliveryDto> delivery = service.allDeliveryList(dto);			
			
			// 그룹화된 데이터를 담을 List
	        List<Map<String, Object>> groupedData = new ArrayList<>();
	        Map<String, Object> deliveryMap = new LinkedHashMap<>();
	        for (DeliveryDto deliveryDto : delivery) {
	            // 필드 값 설정
	            deliveryMap.put("orderSeq", deliveryDto.getOrderSeq());
	            deliveryMap.put("userId", deliveryDto.getUserId());
	            deliveryMap.put("orderStatus", deliveryDto.getOrderStatus());
	            deliveryMap.put("ref", deliveryDto.getRef());
	            deliveryMap.put("location", deliveryDto.getLocation());
	            deliveryMap.put("branchName", deliveryDto.getBranchName());

	            List<Map<String, Object>> deliveryDetails = new ArrayList<>();

	            // 중복된 seq 값을 그룹으로 묶기
	            for (DeliveryDto item : delivery) {
	                if (item.getRef().equals(deliveryDto.getRef()) ) {
	                    Map<String, Object> detail = new HashMap<>();
	                    detail.put("product_name", item.getProductName());
	                    detail.put("quantity", item.getQuantity());
	                    detail.put("price", item.getPrice());
	                    detail.put("product_seq", item.getProductSeq());
	                    deliveryDetails.add(detail);
	                }
	            }

	            deliveryMap.put("details", deliveryDetails);

	            // 그룹화된 데이터를 추가
	            groupedData.add(deliveryMap);
	        }
	        
	        // 중복된 데이터를 그룹화
	        List<Map<String, Object>> uniqueGroupedData = new ArrayList<>();
	        for (Map<String, Object> dataMap : groupedData) {
	            if (!uniqueGroupedData.contains(dataMap)) {
	                uniqueGroupedData.add(dataMap);
	            }
	        }
						
			return uniqueGroupedData;	
		}
		
		// 장바구니 삭제
		@PostMapping("deleteDelivery")
		public String deleteDelivery(@RequestBody DeliveryDto dto, @RequestHeader("accessToken") String accessToken) {
			System.out.println("DeliveryController deleteDelivery " + new Date());
			
			int count = service.deleteDelivery(dto);
			
			if(count != 0) {
				return "YES";
			}
			return "NO";
		}
		
		// 배달점포인지 체크
		@GetMapping("deliveryCheck")
		public String deliveryCheck(@RequestHeader("accessToken") String accessToken) {
			System.out.println("DeliveryController deleteDelivery " + new Date());
			
			String userId = tokenCreate.getuserIdFromToken(accessToken);			
			System.out.println(userId);
			
			ConvenienceDto dto = service.getDeliveryStatus(userId);
			
			if(dto.getConvLocation() == null) {
				return "NO";
			}
			return "YES";
		}
		
		//---------------------------------------------------함수------------------------------------------------------
	
	// customerSeq 추출하는 로직
	public int tokenParser(String tokenHeader) {
		
		// "Bearer " 문자열을 제거하여 실제 토큰을 추출
	    String accessToken = tokenHeader.replace("Bearer ", "");

	    // JWT 토큰 검증
	    	JwtParser jwtParser = Jwts.parserBuilder()
	    		    .setSigningKey(tokenCreate.securityKey)
	    		    .build();

	        Claims claims = jwtParser.parseClaimsJws(accessToken).getBody();

	        // 사용자 ID 추출
	        int customerSeq = claims.get("customerSeq", Integer.class);
	        
	        return customerSeq;
	}	
	
	
}
