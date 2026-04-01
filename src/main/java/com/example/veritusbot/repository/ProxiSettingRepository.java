package com.example.veritusbot.repository;

import com.example.veritusbot.model.ProxiSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProxiSettingRepository extends JpaRepository<ProxiSetting, Long> {

    List<ProxiSetting> findByActivoTrueOrderByOrdenAsc();
}

