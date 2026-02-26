package com.ecommerce.service;

import com.ecommerce.model.dto.ProductDTO;
import com.ecommerce.model.entity.Category;
import com.ecommerce.model.entity.Product;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    private Product validProduct;
    private Category validCategory;

    @BeforeEach
    void setUp() {
        validCategory = Category.builder()
                .id(2L)
                .name("Electronics")
                .build();

        validProduct = Product.builder()
                .id(1L)
                .name("Laptop")
                .price(BigDecimal.valueOf(999.99))
                .stock(50)
                .category(validCategory)
                .isActive(true)
                .build();
    }

    @Test
    void testGetAllProducts_Success() {
        Page<Product> page = new PageImpl<>(List.of(validProduct));
        when(productRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<ProductDTO> result = productService.getAllProducts(Pageable.unpaged());

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Laptop");
    }

    @Test
    void testGetProductById_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(validProduct));

        Product result = productService.getProductById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void testGetProductById_NotFound_ThrowsException() {
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Product not found");
    }

    @Test
    void testAddProduct_Success() {
        ProductDTO dto = new ProductDTO();
        dto.setName("Laptop");
        dto.setCategoryId(2L);
        dto.setPrice(BigDecimal.valueOf(999.99));
        dto.setStock(50);

        when(categoryRepository.findById(2L)).thenReturn(Optional.of(validCategory));
        when(productRepository.save(any(Product.class))).thenReturn(validProduct);

        Product result = productService.addProduct(dto);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Laptop");
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void testDeleteProduct_SoftDelete_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(validProduct));

        productService.deleteProduct(1L);

        assertThat(validProduct.getIsActive()).isFalse();
        verify(productRepository, times(1)).save(validProduct);
    }
}
