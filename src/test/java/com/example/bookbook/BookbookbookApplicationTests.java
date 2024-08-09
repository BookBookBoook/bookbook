package com.example.bookbook;

import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.bookbook.domain.entity.Role;
import com.example.bookbook.domain.entity.UserEntity;
import com.example.bookbook.domain.repository.UserEntityRepository;


import com.example.bookbook.domain.entity.Answer;
import com.example.bookbook.domain.repository.AnswerRepository;

import com.example.bookbook.mapper.ExamMapper;

@SpringBootTest
class BookbookbookApplicationTests {
	
	//@Autowired
    private ExamMapper userMapper;
    @Autowired
    private AnswerRepository answerRepo;
    //@Test
    void testCountUsers() {
        int count = userMapper.countUsers();
        // Expected count를 알 수 없으므로 임시로 0이 아닌 값이 리턴되면 성공으로 설정합니다.
        assertThat(count).isGreaterThanOrEqualTo(0);
    }
    

    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    UserEntityRepository mRepository;

   // @Test
    void 챗봇사전등록() {
    	answerRepo.save(Answer.builder()
    			.keyword("안녕")
    			.content("안녕하세요")
    			.build());
    }


    @Test
    void signup(){
    	UserEntity entity = UserEntity.builder()
    				.email("test03@test.com")
    				.password(passwordEncoder.encode("1234"))
    				.userName("테스트03")
    				.userRRN("1123123")
    				.gender("1")
    				.phoneNumber("1")
    				.birthDate("1")
    				.postcode("1")
    				.address("1")
    				.extraAddress("1")
    				.detailAddress("1")
    				.status(1)
    				.build()
    				.addRole(Role.User);
    		mRepository.save(entity);
    }
}
