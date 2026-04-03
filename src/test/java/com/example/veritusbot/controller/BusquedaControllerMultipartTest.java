package com.example.veritusbot.controller;

import com.example.veritusbot.config.ResourceCleanupManager;
import com.example.veritusbot.dto.PersonaDTO;
import com.example.veritusbot.exception.InvalidClientFileException;
import com.example.veritusbot.service.AsyncProcessingService;
import com.example.veritusbot.service.ClientFileParserService;
import com.example.veritusbot.service.ProcessingStateManager;
import com.example.veritusbot.service.SearchRuntimeConfigService;
import com.example.veritusbot.service.auth.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BusquedaController.class)
@AutoConfigureMockMvc(addFilters = false)
class BusquedaControllerMultipartTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClientFileParserService clientFileParserService;

    @MockBean
    private AsyncProcessingService asyncProcessingService;

    @MockBean
    private ProcessingStateManager processingStateManager;

    @MockBean
    private SearchRuntimeConfigService searchRuntimeConfigService;

    @MockBean
    private ResourceCleanupManager resourceCleanupManager;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void searchPeopleShouldReturnAcceptedWhenMultipartFileIsValid() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "clientes.csv",
                "text/csv",
                "NOMBRES;APELIDO PATERNO;APELLIDO MATERNO;AÑOINI;AÑOFIN\nANA;PEREZ;DIAZ;2019;2025".getBytes()
        );

        when(asyncProcessingService.isBusy()).thenReturn(false);
        when(searchRuntimeConfigService.getThreadsPerPerson()).thenReturn(4);
        when(clientFileParserService.parseAndValidate(any())).thenReturn(
                List.of(new PersonaDTO("ANA", "PEREZ", "DIAZ", 2019, 2025))
        );

        mockMvc.perform(multipart("/api/buscar-personas")
                        .file(file)
                        .param("isAllRegionEnabled", "true")
                        .param("isSantiagoEnabled", "true"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.peopleCount").value(1))
                .andExpect(jsonPath("$.isAllRegionEnabled").value(true))
                .andExpect(jsonPath("$.isSantiagoEnabled").value(true));

        verify(asyncProcessingService).processSearchAsync(any(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyInt());
    }

    @Test
    void searchPeopleShouldReturnBadRequestWhenFileIsMissing() throws Exception {
        when(asyncProcessingService.isBusy()).thenReturn(false);
        doThrow(new InvalidClientFileException("El archivo es obligatorio y debe enviarse en el campo 'file'"))
                .when(clientFileParserService).parseAndValidate(any());

        mockMvc.perform(multipart("/api/buscar-personas")
                        .param("isAllRegionEnabled", "true")
                        .param("isSantiagoEnabled", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("INVALID_FILE"));
    }

    @Test
    void searchPeopleShouldReturnBadRequestWhenHeaderIsInvalid() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "clientes.csv",
                "text/csv",
                "contenido".getBytes()
        );

        when(asyncProcessingService.isBusy()).thenReturn(false);
        when(clientFileParserService.parseAndValidate(any()))
                .thenThrow(new InvalidClientFileException("Cabecera requerida no encontrada: AÑOINI"));

        mockMvc.perform(multipart("/api/buscar-personas")
                        .file(file)
                        .param("isAllRegionEnabled", "true")
                        .param("isSantiagoEnabled", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("INVALID_FILE"))
                .andExpect(jsonPath("$.detalle").value("Cabecera requerida no encontrada: AÑOINI"));
    }

    @Test
    void searchPeopleShouldReturnTooManyRequestsWhenSystemIsBusy() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "clientes.csv",
                "text/csv",
                "NOMBRES;APELIDO PATERNO;APELLIDO MATERNO;AÑOINI;AÑOFIN\nANA;PEREZ;DIAZ;2019;2025".getBytes()
        );

        when(asyncProcessingService.isBusy()).thenReturn(true);
        when(asyncProcessingService.getState()).thenReturn(new ProcessingStateManager.ProcessingState(
                true,
                "ANA PEREZ",
                null,
                null,
                1,
                0,
                5000
        ));

        mockMvc.perform(multipart("/api/buscar-personas").file(file))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value("BUSY"));
    }

    @Test
    void stopSearchShouldReturnAcceptedWhenProcessingIsActive() throws Exception {
        when(asyncProcessingService.isBusy()).thenReturn(true);
        when(asyncProcessingService.requestStop()).thenReturn(true);

        mockMvc.perform(post("/api/buscar-personas/detener"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("STOP_REQUESTED"));

        verify(asyncProcessingService).requestStop();
    }

    @Test
    void stopSearchShouldReturnConflictWhenNoProcessingIsActive() throws Exception {
        when(asyncProcessingService.isBusy()).thenReturn(false);

        mockMvc.perform(post("/api/buscar-personas/detener"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("NO_ACTIVE_SEARCH"));
    }
}

