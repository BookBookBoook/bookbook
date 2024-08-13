package com.example.bookbook.service.impl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.client.RestTemplate;

import com.example.bookbook.domain.dto.BookDTO;
import com.example.bookbook.domain.dto.BookSlide;
import com.example.bookbook.domain.dto.NaverBookItem;
import com.example.bookbook.domain.dto.NaverBookResponse;
import com.example.bookbook.service.BookService;

@Service
public class BookServiceProcess implements BookService {

	@Value("${naver.openapi.clientId}")
	private String clientId;

	@Value("${naver.openapi.clientSecret}")
	private String clientSecret;

	private static final String NAVER_BOOK_API_URL = "https://openapi.naver.com/v1/search/book.json";

	// 검색 결과
	public void searchBooks(String query, Model model) {
		try {
			String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
			String url = NAVER_BOOK_API_URL + "?query=" + encodedQuery + "&display=20"; // 결과 수를 100개로 설정

			HttpHeaders headers = new HttpHeaders();
			headers.set("X-Naver-Client-Id", clientId);
			headers.set("X-Naver-Client-Secret", clientSecret);
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

			RestTemplate restTemplate = new RestTemplate();
			ResponseEntity<NaverBookResponse> responseEntity = restTemplate.exchange(url, HttpMethod.GET,
					new HttpEntity<>(headers), NaverBookResponse.class);

			NaverBookResponse response = responseEntity.getBody();
			if (response != null && response.getItems() != null && !response.getItems().isEmpty()) {
				List<BookDTO> books = response.getItems().stream().map(this::convertToBookDTO)
						.collect(Collectors.toList());
				model.addAttribute("books", books);
				model.addAttribute("query", query);
				logger.info("검색 결과: {} 건", books.size());
			} else {
				model.addAttribute("books", Collections.emptyList());
				model.addAttribute("error", "검색 결과가 없습니다.");
				logger.info("검색 결과 없음: {}", query);
			}
		} catch (Exception e) {
			logger.error("책 검색 중 오류 발생: {}", e.getMessage(), e);
			model.addAttribute("books", Collections.emptyList());
			model.addAttribute("error", "검색 중 오류가 발생했습니다: " + e.getMessage());
		}
	}

	private BookDTO convertToBookDTO(NaverBookItem item) {
		BookDTO book = new BookDTO();
		book.setTitle(item.getTitle() != null ? item.getTitle() : "");
		book.setAuthor(item.getAuthor() != null ? item.getAuthor() : "");
		book.setPublisher(item.getPublisher() != null ? item.getPublisher() : "");
		book.setPubdate(item.getPubdate() != null ? item.getPubdate() : "");
		book.setDescription(truncateDescription(item.getDescription()));
		book.setIsbn(item.getIsbn() != null ? item.getIsbn() : "");
		book.setImage(item.getImage() != null ? item.getImage() : "");
		book.setLink(item.getLink() != null ? item.getLink() : "");
		book.setDiscount(item.getDiscount() != null ? item.getDiscount() : "");
		return book;
	}

	// 도서 리스트 뿌리기
	@Override
	public void getBookList(Model model) {
		// 1. "만화"라는 쿼리로 책 정보를 가져오는 메소드 호출
		List<BookDTO> books = fetchBooksFromAPI("만화");

		// 2. 가져온 책 정보를 모델에 "books"라는 이름으로 추가
		model.addAttribute("books", books);
	}

	// fetchBooksFromAPI 메서드 수정 (BookDTO 리스트 반환)
	private List<BookDTO> fetchBooksFromAPI(String query) {
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Naver-Client-Id", clientId);
		headers.set("X-Naver-Client-Secret", clientSecret);

		String url = NAVER_BOOK_API_URL + "?query=" + query + "&display=20"; // 20개의 결과를 가져옵니다.

		ResponseEntity<NaverBookResponse> responseEntity = restTemplate.exchange(url, HttpMethod.GET,
				new HttpEntity<>(headers), NaverBookResponse.class);

		NaverBookResponse response = responseEntity.getBody();
		if (response != null && response.getItems() != null) {
			return response.getItems().stream().map(this::convertToBookDTO).collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	private List<BookDTO> parseBookResults(Map<String, Object> responseBody) {
		// API 응답에서 "items"라는 키로 책 정보를 리스트 형태로 추출
		List<Map<String, Object>> items = (List<Map<String, Object>>) responseBody.get("items");
		List<BookDTO> books = new ArrayList<>();

		// 추출한 책 정보들을 순회하며 BookDTO 객체로 변환
		for (Map<String, Object> item : items) {
			// 새로운 BookDTO 객체 생성
			BookDTO book = new BookDTO();

			// 각 필드에 API 응답에서 추출한 값을 설정
			book.setTitle((String) item.get("title")); // 책 제목 설정
			book.setAuthor((String) item.get("author")); // 저자 설정
			book.setPublisher((String) item.get("publisher")); // 출판사 설정
			book.setPubdate((String) item.get("pubdate")); // 출판일 설정

			// 설명 텍스트를 잘라서 설정 (설명이 너무 길 경우를 대비)
			String fullDescription = (String) item.get("description");
			book.setDescription(truncateDescription(fullDescription));

			// ISBN 설정 (추후 파싱 로직 수정 가능)
			book.setIsbn((String) item.get("isbn"));

			// 이미지 URL 설정
			book.setImage((String) item.get("image"));

			// 상세 정보 링크 설정
			book.setLink((String) item.get("link"));

			// 할인된 가격 설정 (판매가)
			book.setDiscount((String) item.get("discount"));

			// 카테고리 정보가 있는 경우 설정
			if (item.containsKey("category")) {
				book.setCategory((String) item.get("category"));
			}

			// 변환된 BookDTO 객체를 리스트에 추가
			books.add(book);
		}

		// 변환된 BookDTO 리스트 반환
		return books;
	}

	private static final Logger logger = LoggerFactory.getLogger(BookServiceProcess.class);

	// 책소개 250자 내외로 보여줘
	private String truncateDescription(String description) {
		logger.debug("Original description: {}", description);

		if (description == null) {
			return "";
		}

		// 300자 이하면 그대로 반환
		if (description.length() <= 250) {
			return description;
		}

		// 300자까지 자르기
		String truncated = description.substring(0, 250);

		// 단어 중간에서 잘리는 것을 방지하기 위해 마지막 공백 이후로 자르기
		int lastSpace = truncated.lastIndexOf(' ');
		if (lastSpace > 0) {
			truncated = truncated.substring(0, lastSpace);
		}

		// 생략 부호 추가
		String result = truncated + "...";

		logger.debug("Truncated description: {}", result);
		return result;
	}

	@Override
	public BookDTO getBookByIsbn(String isbn) {
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Naver-Client-Id", clientId);
		headers.set("X-Naver-Client-Secret", clientSecret);

		String url = NAVER_BOOK_API_URL + "?query=isbn:" + isbn;

		ResponseEntity<NaverBookResponse> responseEntity = restTemplate.exchange(url, HttpMethod.GET,
				new HttpEntity<>(headers), NaverBookResponse.class);

		NaverBookResponse response = responseEntity.getBody();
		if (response != null && !response.getItems().isEmpty()) {
			return convertToBookDTO(response.getItems().get(0));
		}
		return null;
	}

	private BookDTO convertToBookDTO1(NaverBookItem item) {
		BookDTO book = new BookDTO();
		book.setTitle(item.getTitle());
		book.setAuthor(item.getAuthor());
		book.setPublisher(item.getPublisher());
		book.setPubdate(item.getPubdate());
		book.setDescription(item.getDescription());
		book.setIsbn(item.getIsbn());
		book.setImage(item.getImage());
		book.setLink(item.getLink());
		book.setDiscount(item.getDiscount());
		// 기타 필요한 필드 설정
		return book;
	}

	@Override
	public void getDefaultBooks(Model model) {
		List<BookDTO> bestSellerBooks = fetchBooksFromAPI("베스트셀러");
		List<BookSlide> bestSellerSlides = createBookSlides(bestSellerBooks);
		model.addAttribute("bestSellerSlides", bestSellerSlides);

	}

	private List<BookSlide> createBookSlides(List<BookDTO> books) {
		List<BookSlide> slides = new ArrayList<>();
		for (int i = 0; i < books.size(); i += 4) {
			int end = Math.min(i + 4, books.size());
			slides.add(new BookSlide(books.subList(i, end)));
		}
		return slides;
	}

}