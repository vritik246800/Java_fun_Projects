package com.mvvmcsv.util;

import com.mvvmcsv.model.Person;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * UTIL — lê e grava CSV com coluna ID como primeira coluna.
 * Formato: id,nome,idade,email
 */
public class CsvUtil {

    private static final String HEADER    = "id,nome,idade,email";
    private static final String DELIMITER = ",";

    public static void write(String filePath, List<Person> people) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
            bw.write(HEADER);
            bw.newLine();
            for (Person p : people) {
                bw.write(p.getId() + DELIMITER + p.getName() + DELIMITER + p.getAge() + DELIMITER + p.getEmail());
                bw.newLine();
            }
        }
    }

    public static List<Person> read(String filePath) throws IOException {
        List<Person> list = new ArrayList<>();
        File file = new File(filePath);
        if (!file.exists()) return list;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; }
                String[] parts = line.split(DELIMITER, -1);
                if (parts.length == 4) {
                    try {
                        int id  = Integer.parseInt(parts[0].trim());
                        int age = Integer.parseInt(parts[2].trim());
                        list.add(new Person(id, parts[1].trim(), age, parts[3].trim()));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return list;
    }
}