package com.mvvmcsv.viewmodel;

import com.mvvmcsv.model.Person;
import com.mvvmcsv.util.CsvUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * VIEWMODEL — lógica, estado e pesquisa em tempo real.
 * Não importa nada de Swing.
 */
public class PersonViewModel {

    private static final String CSV_PATH = "data/pessoas.csv";

    private final List<Person>           people        = new ArrayList<>();
    private       Consumer<List<Person>> onDataChanged;  // tabela principal
    private       Consumer<List<Person>> onSearchResult; // tabela de pesquisa
    private       Consumer<String>       onMessage;

    // ── Callbacks ──────────────────────────────────────────────────────────
    public void setOnDataChanged (Consumer<List<Person>> cb) { this.onDataChanged  = cb; }
    public void setOnSearchResult(Consumer<List<Person>> cb) { this.onSearchResult = cb; }
    public void setOnMessage     (Consumer<String>       cb) { this.onMessage      = cb; }

    // ── Acções ─────────────────────────────────────────────────────────────

    public void addPerson(String name, String ageStr, String email) {
        if (name.isBlank() || ageStr.isBlank() || email.isBlank()) {
            notify("Preenche todos os campos."); return;
        }
        int age;
        try { age = Integer.parseInt(ageStr.trim()); }
        catch (NumberFormatException e) { notify("Idade tem de ser um número inteiro."); return; }

        people.add(new Person(name.trim(), age, email.trim()));
        notify("Adicionado com ID #" + people.get(people.size()-1).getId());
        notifyData();
    }

    public void removePerson(int index) {
        if (index < 0 || index >= people.size()) { notify("Seleciona uma linha válida."); return; }
        String removed = people.get(index).getName();
        people.remove(index);
        notify("Removido: " + removed);
        notifyData();
    }

    public void saveToCSV() {
        try {
            CsvUtil.write(CSV_PATH, people);
            notify("Gravado em " + CSV_PATH + " (" + people.size() + " registos).");
        } catch (Exception e) {
            notify("Erro ao gravar: " + e.getMessage());
        }
    }

    public void loadFromCSV() {
        try {
            Person.resetCounter();
            List<Person> loaded = CsvUtil.read(CSV_PATH);
            people.clear();
            people.addAll(loaded);
            notify("Carregados " + loaded.size() + " registos de " + CSV_PATH);
            notifyData();
        } catch (Exception e) {
            notify("Erro ao ler: " + e.getMessage());
        }
    }

    /**
     * Pesquisa em tempo real por ID (número exacto) ou Nome (contém, case-insensitive).
     * Chamado pelo DocumentListener na View.
     */
    public void search(String query) {
        if (query == null || query.isBlank()) {
            notifySearch(Collections.emptyList());
            return;
        }
        String q = query.trim();
        List<Person> result;

        // Se for número, pesquisa por ID exacto; caso contrário, por nome
        try {
            int id = Integer.parseInt(q);
            result = people.stream()
                    .filter(p -> p.getId() == id)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            String lower = q.toLowerCase();
            result = people.stream()
                    .filter(p -> p.getName().toLowerCase().contains(lower))
                    .collect(Collectors.toList());
        }

        notifySearch(result);
    }

    public List<Person> getPeople() { return Collections.unmodifiableList(people); }

    // ── Privados ───────────────────────────────────────────────────────────
    private void notify    (String msg)          { if (onMessage      != null) onMessage.accept(msg); }
    private void notifyData()                    { if (onDataChanged  != null) onDataChanged.accept(getPeople()); }
    private void notifySearch(List<Person> list) { if (onSearchResult != null) onSearchResult.accept(list); }
}