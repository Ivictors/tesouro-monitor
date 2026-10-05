package br.com.tesouromonitor.service;

import br.com.tesouromonitor.dto.TesouroResponseDTO;
import br.com.tesouromonitor.dto.TituloDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TesouroCsvParser {

    private static final char DELIMITER = ';';
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final List<String> REQUIRED_COLUMNS = List.of(
            "Tipo Titulo", "Data Vencimento", "Data Base", "Taxa Compra Manha",
            "Taxa Venda Manha", "PU Compra Manha", "PU Venda Manha", "PU Base Manha");

    public TesouroResponseDTO parse(String csv) {
        if (csv == null || csv.isBlank()) {
            throw new IllegalArgumentException("O CSV do Tesouro Direto está vazio.");
        }

        String[] lines = csv.split("\\R");
        List<String> headers = parseLine(stripBom(lines[0]));
        Map<String, Integer> columns = indexColumns(headers);
        List<TituloDTO> latestTitles = new ArrayList<>();
        LocalDate latestBaseDate = null;

        for (int lineIndex = 1; lineIndex < lines.length; lineIndex++) {
            if (lines[lineIndex].isBlank()) {
                continue;
            }
            try {
                TituloDTO title = toTitle(parseLine(lines[lineIndex]), columns);
                if (latestBaseDate == null || title.dataBase().isAfter(latestBaseDate)) {
                    latestBaseDate = title.dataBase();
                    latestTitles.clear();
                    latestTitles.add(title);
                } else if (title.dataBase().equals(latestBaseDate)) {
                    latestTitles.add(title);
                }
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException(
                        "Registro CSV inválido na linha " + (lineIndex + 1) + ".", exception);
            }
        }

        if (latestBaseDate == null) {
            throw new IllegalArgumentException("O CSV não contém cotações.");
        }

        return new TesouroResponseDTO(latestTitles);
    }

    private Map<String, Integer> indexColumns(List<String> headers) {
        Map<String, Integer> columns = new HashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            columns.put(headers.get(index).trim(), index);
        }
        for (String requiredColumn : REQUIRED_COLUMNS) {
            if (!columns.containsKey(requiredColumn)) {
                throw new IllegalArgumentException("Coluna obrigatória ausente no CSV: " + requiredColumn);
            }
        }
        return columns;
    }

    private TituloDTO toTitle(List<String> values, Map<String, Integer> columns) {
        return new TituloDTO(
                value(values, columns, "Tipo Titulo"),
                parseDate(value(values, columns, "Data Vencimento")),
                parseDate(value(values, columns, "Data Base")),
                parseDecimal(value(values, columns, "Taxa Compra Manha")),
                parseDecimal(value(values, columns, "Taxa Venda Manha")),
                parseDecimal(value(values, columns, "PU Compra Manha")),
                parseDecimal(value(values, columns, "PU Venda Manha")),
                parseDecimal(value(values, columns, "PU Base Manha")));
    }

    private String value(List<String> values, Map<String, Integer> columns, String column) {
        int index = columns.get(column);
        if (index >= values.size() || values.get(index).isBlank()) {
            throw new IllegalArgumentException("Valor ausente na coluna " + column);
        }
        return values.get(index).trim();
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value, DATE_FORMAT);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Data inválida: " + value, exception);
        }
    }

    private BigDecimal parseDecimal(String value) {
        String normalized = value.replace(".", "").replace(',', '.');
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Valor decimal inválido: " + value, exception);
        }
    }

    private List<String> parseLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;

        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    value.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == DELIMITER && !quoted) {
                values.add(value.toString());
                value.setLength(0);
            } else {
                value.append(current);
            }
        }

        if (quoted) {
            throw new IllegalArgumentException("Campo CSV com aspas não fechadas.");
        }
        values.add(value.toString());
        return values;
    }

    private String stripBom(String value) {
        return value.startsWith("\uFEFF") ? value.substring(1) : value;
    }
}
