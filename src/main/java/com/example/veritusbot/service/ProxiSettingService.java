package com.example.veritusbot.service;

import com.example.veritusbot.dto.CreateProxiSettingRequestDTO;
import com.example.veritusbot.dto.ProxiSettingDTO;
import com.example.veritusbot.model.ProxiSetting;
import com.example.veritusbot.repository.ProxiSettingRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProxiSettingService {

    private final ProxiSettingRepository repository;

    public ProxiSettingService(ProxiSettingRepository repository) {
        this.repository = repository;
    }

    public List<ProxiSettingDTO> listarTodos() {
        return repository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public List<ProxiSetting> listarActivos() {
        return repository.findByActivoTrueOrderByOrdenAsc();
    }

    /**
     * Crea un nuevo proxy a partir del request del frontend.
     * Los campos opcionales se resuelven con valores por defecto.
     */
    public ProxiSettingDTO crear(CreateProxiSettingRequestDTO request) {
        if (request == null || !request.isValid()) {
            throw new IllegalArgumentException("El campo 'server' es obligatorio y no puede estar vacío");
        }

        boolean activo = request.getActivo() != null ? request.getActivo() : true;
        int orden = request.getOrden() != null ? request.getOrden() : 0;

        ProxiSetting entity = new ProxiSetting(
                request.getServer().trim(),
                normalizeCredential(request.getUsername()),
                normalizeCredential(request.getPassword()),
                activo,
                orden
        );

        return toDTO(repository.save(entity));
    }

    public ProxiSettingDTO actualizar(Long id, ProxiSettingDTO dto) {
        ProxiSetting existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ProxiSetting no encontrado con id: " + id));
        existing.setServer(dto.getServer());
        existing.setUsername(dto.getUsername());
        existing.setPassword(dto.getPassword());
        existing.setActivo(dto.isActivo());
        existing.setOrden(dto.getOrden());
        return toDTO(repository.save(existing));
    }

    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("ProxiSetting no encontrado con id: " + id);
        }
        repository.deleteById(id);
    }

    private ProxiSettingDTO toDTO(ProxiSetting entity) {
        return new ProxiSettingDTO(
                entity.getId(),
                entity.getServer(),
                entity.getUsername(),
                entity.getPassword(),
                entity.isActivo(),
                entity.getOrden());
    }

    private String normalizeCredential(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

