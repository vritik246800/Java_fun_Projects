CREATE DATABASE teste_java;

USE teste_java;

CREATE TABLE usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100),
    email VARCHAR(100)
);

INSERT INTO usuarios (nome, email) VALUES
('João Silva', 'joao@gmail.com'),
('Maria Santos', 'maria@gmail.com');
