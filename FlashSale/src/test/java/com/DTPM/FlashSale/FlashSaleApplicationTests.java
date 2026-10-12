package com.DTPM.FlashSale;

import com.DTPM.FlashSale.infrastructure.RedisStockService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class FlashSaleApplicationTests {

	@MockitoBean
	private RedisStockService redisStockService;

	@MockitoBean
	private com.DTPM.FlashSale.infrastructure.RedisSeatLockService redisSeatLockService;

	@Test
	void contextLoads() {
	}

}


