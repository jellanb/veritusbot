package com.example.veritusbot.repository;

import com.example.veritusbot.model.Causa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CausaRepository extends JpaRepository<Causa, UUID> {
}
