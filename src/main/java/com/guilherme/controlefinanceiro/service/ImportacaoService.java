package com.guilherme.controlefinanceiro.service;

import com.guilherme.controlefinanceiro.dto.ImportacaoTransacaoDTO;
import com.guilherme.controlefinanceiro.model.Categoria;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ImportacaoService {
    private final CategorizacaoService categorizacao;

    public ImportacaoService(CategorizacaoService categorizacao) {
        this.categorizacao = categorizacao;
    }

    public List<ImportacaoTransacaoDTO> analisar(MultipartFile arquivo) throws Exception {
        String nome = arquivo.getOriginalFilename() == null ? ""
                : arquivo.getOriginalFilename().toLowerCase(Locale.ROOT);
        String texto = new String(arquivo.getBytes(), StandardCharsets.UTF_8);
        return nome.endsWith(".ofx") || texto.contains("<OFX>") ? analisarOfx(texto) : analisarCsv(texto);
    }

    private List<ImportacaoTransacaoDTO> analisarOfx(String texto) {
        List<ImportacaoTransacaoDTO> resultado = new ArrayList<>();
        for (String bloco : texto.split("(?i)<STMTTRN>")) {
            String valor = tag(bloco, "TRNAMT");
            String memo = tag(bloco, "MEMO");
            if (valor == null || memo == null)
                continue;
            resultado.add(criar(memo, Double.parseDouble(valor.replace(',', '.')), tag(bloco, "DTPOSTED")));
        }
        return resultado;
    }

    private List<ImportacaoTransacaoDTO> analisarCsv(String texto) throws Exception {
        List<ImportacaoTransacaoDTO> resultado = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new java.io.ByteArrayInputStream(texto.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8))) {
            String cabecalho = reader.readLine();
            if (cabecalho == null)
                return resultado;
            String separador = cabecalho.contains(";") ? ";" : ",";
            String linha;
            while ((linha = reader.readLine()) != null) {
                String[] colunas = linha.split(separador, -1);
                if (colunas.length < 3)
                    continue;
                resultado.add(criar(colunas[0].trim(),
                        Double.parseDouble(colunas[1].trim().replace(".", "").replace(',', '.')), colunas[2].trim()));
            }
        }
        return resultado;
    }

    private ImportacaoTransacaoDTO criar(String descricao, double valor, String dataTexto) {
        boolean entrada = valor >= 0;
        String data = dataTexto == null ? "" : dataTexto.replaceAll("^(\\d{4})(\\d{2})(\\d{2}).*", "$1-$2-$3");
        LocalDate dataFinal;
        try {
            dataFinal = LocalDate.parse(data, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception ignored) {
            dataFinal = LocalDate.now();
        }
        return new ImportacaoTransacaoDTO(descricao, Math.abs(valor), entrada ? "ENTRADA" : "SAIDA", dataFinal,
                entrada ? Categoria.SALARIO : categorizacao.categorizar(descricao));
    }

    private String tag(String texto, String nome) {
        var match = java.util.regex.Pattern
                .compile("<" + nome + ">(?:<[^>]+>)*([^<\\r\\n]+)", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(texto);
        return match.find() ? match.group(1).trim() : null;
    }
}
