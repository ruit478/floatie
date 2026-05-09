package com.future.floatie.repository;


import com.future.floatie.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PetRepository extends JpaRepository<Pet, UUID> {
    Optional<Pet> findByUser_Username(String username);
}
