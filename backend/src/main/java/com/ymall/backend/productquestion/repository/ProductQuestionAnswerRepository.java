package com.ymall.backend.productquestion.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ymall.backend.productquestion.entity.ProductQuestionAnswer;

public interface ProductQuestionAnswerRepository
    extends JpaRepository<ProductQuestionAnswer, Long> {
}
