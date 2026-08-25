package com.guilherme.controlefinanceiro.controller;

import com.guilherme.controlefinanceiro.dto.ImportacaoTransacaoDTO;
import com.guilherme.controlefinanceiro.service.ImportacaoService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/importacoes")
@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:5174" })
public class ImportacaoController {
    private final ImportacaoService service;

    public ImportacaoController(ImportacaoService service) {
        this.service = service;
    }

    @PostMapping(value = "/extrato", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<ImportacaoTransacaoDTO> analisar(@RequestParam("arquivo") MultipartFile arquivo) throws Exception {
        return service.analisar(arquivo);
    }
}
