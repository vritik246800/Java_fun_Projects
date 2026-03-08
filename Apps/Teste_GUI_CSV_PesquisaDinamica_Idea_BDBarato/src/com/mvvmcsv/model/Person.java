package com.mvvmcsv.model;

/**
 * MODEL — dados puros.
 * ID gerado automaticamente (autoincremento global).
 */
public class Person {

    private static int nextId = 1;

    private int    id;
    private String name;
    private int    age;
    private String email;

    /** Usado ao adicionar nova pessoa — gera ID automático. */
    public Person(String name, int age, String email) {
        this.id    = nextId++;
        this.name  = name;
        this.age   = age;
        this.email = email;
    }

    /** Usado ao carregar do CSV — preserva ID existente. */
    public Person(int id, String name, int age, String email) {
        this.id    = id;
        this.name  = name;
        this.age   = age;
        this.email = email;
        if (id >= nextId) nextId = id + 1;
    }

    /** Repõe o contador antes de carregar CSV. */
    public static void resetCounter() { nextId = 1; }

    public int    getId()                { return id; }
    public String getName()              { return name; }
    public void   setName(String name)   { this.name = name; }
    public int    getAge()               { return age; }
    public void   setAge(int age)        { this.age = age; }
    public String getEmail()             { return email; }
    public void   setEmail(String email) { this.email = email; }

    @Override
    public String toString() {
        return id + " | " + name + " | " + age + " | " + email;
    }
}