package com.training.repositories;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.product.ProductController;
import com.training.product.ProductResponse;
import com.training.product.ProductService;

@WebMvcTest(ProductController.class)
@ActiveProfiles("test")
public class ProductControllerTest {

	@Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;   // Jackson: object ↔ JSON

    // ProductController depends on ProductService — mock it
    @MockBean
    private ProductService productService;
    
    @Test
//    @WithMockUser          // any authenticated user can GET (see SecurityConfig)
    void getProducts_returnsListOf200() throws Exception {

        // ARRANGE: define what the mocked service returns
        ProductResponse laptop = ProductResponse.builder()
                .id(1L).name("Laptop Pro").price(new BigDecimal("1299.99"))
                .stockQuantity(10).categoryId(1L).categoryName("Electronics")
                .build();

        when(productService.findAll()).thenReturn(List.of(laptop));

        // ACT + ASSERT
        mockMvc.perform(get("/api/products"))          // HTTP GET /api/products
                .andExpect(status().isOk())     	       // response status = 200
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Laptop Pro"))
                .andExpect(jsonPath("$[0].price").value(1299.99));
    }
}
