package com.algoforge.backend.user.service;

import com.algoforge.backend.common.exception.BusinessException;
import com.algoforge.backend.common.exception.ErrorCode;
import com.algoforge.backend.user.domain.User;
import com.algoforge.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }
}
