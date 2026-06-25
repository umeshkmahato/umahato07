package com.umahato.user.service;

import java.time.Duration;

import com.umahato.common.dto.UserProfileDto;
import com.umahato.user.dto.CreateUserRequest;
import com.umahato.user.dto.UpdateUserRequest;
import com.umahato.user.entity.UserProfileEntity;
import com.umahato.user.exception.UserAlreadyExistsException;
import com.umahato.user.exception.UserNotFoundException;
import com.umahato.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

@Service
public class UserService {

    private static final String LOCAL_CACHE_NAME = "usersLocal";
    private static final String USER_REDIS_KEY_PREFIX = "user:";

    private final UserRepository userRepository;
    private final CacheManager cacheManager;
    private final RedisTemplate<String, UserProfileDto> userProfileRedisTemplate;
    private final Duration redisTtl;

    public UserService(UserRepository userRepository,
                       CacheManager cacheManager,
                       RedisTemplate<String, UserProfileDto> userProfileRedisTemplate) {
        this.userRepository = userRepository;
        this.cacheManager = cacheManager;
        this.userProfileRedisTemplate = userProfileRedisTemplate;
        this.redisTtl = Duration.ofMinutes(10);
    }

    @Transactional(readOnly = true)
    public UserProfileDto getUserById(Long userId) {
        UserProfileDto localCacheHit = getFromLocalCache(userId);
        if (localCacheHit != null) {
            return localCacheHit;
        }

        String redisKey = redisKey(userId);
        UserProfileDto redisHit = userProfileRedisTemplate.opsForValue().get(redisKey);
        if (redisHit != null) {
            putInLocalCache(redisHit);
            return redisHit;
        }

        UserProfileEntity entity = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        UserProfileDto dto = toDto(entity);
        syncCaches(dto);
        return dto;
    }

    @Transactional
    public UserProfileDto createUser(CreateUserRequest request) {
        if (userRepository.existsById(request.id())) {
            throw new UserAlreadyExistsException(request.id());
        }

        UserProfileEntity entity = new UserProfileEntity(
                request.id(),
                request.email(),
                request.name(),
                request.preferences());
        UserProfileEntity saved = userRepository.save(entity);
        UserProfileDto dto = toDto(saved);
        syncCaches(dto);
        return dto;
    }

    @Transactional
    public UserProfileDto updateUser(Long userId, UpdateUserRequest request) {
        UserProfileEntity entity = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        entity.setEmail(request.email());
        entity.setName(request.name());
        entity.setPreferences(request.preferences());
        UserProfileEntity saved = userRepository.save(entity);
        UserProfileDto dto = toDto(saved);
        syncCaches(dto);
        return dto;
    }

    private UserProfileDto getFromLocalCache(Long userId) {
        Cache cache = cacheManager.getCache(LOCAL_CACHE_NAME);
        if (cache == null) {
            return null;
        }
        return cache.get(userId, UserProfileDto.class);
    }

    private void putInLocalCache(UserProfileDto userProfileDto) {
        Cache cache = cacheManager.getCache(LOCAL_CACHE_NAME);
        if (cache != null) {
            cache.put(userProfileDto.id(), userProfileDto);
        }
    }

    private void syncCaches(UserProfileDto userProfileDto) {
        putInLocalCache(userProfileDto);
        userProfileRedisTemplate.opsForValue().set(redisKey(userProfileDto.id()), userProfileDto, redisTtl);
    }

    private String redisKey(Long userId) {
        return USER_REDIS_KEY_PREFIX + userId;
    }

    private UserProfileDto toDto(UserProfileEntity entity) {
        return new UserProfileDto(
                entity.getId(),
                entity.getEmail(),
                entity.getName(),
                entity.getPreferences());
    }
}
