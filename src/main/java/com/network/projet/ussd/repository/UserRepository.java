// package com.network.projet.ussd.repository;

// import org.springframework.data.r2dbc.repository.Query;
// import org.springframework.data.r2dbc.repository.R2dbcRepository;
// import org.springframework.stereotype.Repository;

// import com.network.projet.ussd.model.entities.User;

// import reactor.core.publisher.Mono;

// @Repository
// public interface UserRepository extends R2dbcRepository<User, Long> {
    
//     Mono<User> findByPhoneNumber(String phoneNumber);
    
//     Mono<User> findByEmail(String email);
    
//     @Query("SELECT * FROM users WHERE username = :username")
//     Mono<User> findByUsername(String username);
// }
