DELIMITER $$

CREATE TRIGGER trg_saida_venda
AFTER INSERT ON venda_item
FOR EACH ROW
BEGIN
    DECLARE stockAntes DECIMAL(10,3);
    DECLARE stockDepois DECIMAL(10,3);

    SELECT stock_atual INTO stockAntes
    FROM produto
    WHERE id_produto = NEW.id_produto;

    SET stockDepois = stockAntes - NEW.quantidade;

    UPDATE produto
    SET stock_atual = stockDepois
    WHERE id_produto = NEW.id_produto;

    INSERT INTO stock_movimento
    (id_produto, tipo_movimento, quantidade, stock_antes, stock_depois, id_venda_item, observacao)
    VALUES
    (NEW.id_produto, 'SAIDA_VENDA', -NEW.quantidade, stockAntes, stockDepois, NEW.id_venda_item, 'Venda normal');
END $$

DELIMITER ;

DELIMITER $$

CREATE TRIGGER trg_entrada_devolucao
AFTER INSERT ON devolucao_item
FOR EACH ROW
BEGIN
    DECLARE stockAntes DECIMAL(10,3);
    DECLARE stockDepois DECIMAL(10,3);

    SELECT stock_atual INTO stockAntes
    FROM produto
    WHERE id_produto = NEW.id_produto;

    SET stockDepois = stockAntes + NEW.quantidade;

    UPDATE produto
    SET stock_atual = stockDepois
    WHERE id_produto = NEW.id_produto;

    INSERT INTO stock_movimento
    (id_produto, tipo_movimento, quantidade, stock_antes, stock_depois, id_devolucao_item, observacao)
    VALUES
    (NEW.id_produto, 'DEV_VENDA', NEW.quantidade, stockAntes, stockDepois, NEW.id_devolucao_item, 'Devolução de venda');
END $$

DELIMITER ;

DELIMITER $$

CREATE TRIGGER trg_saida_venda_delete
AFTER DELETE ON venda_item
FOR EACH ROW
BEGIN
    DECLARE stockAntes DECIMAL(10,3);
    DECLARE stockDepois DECIMAL(10,3);

    SELECT stock_atual INTO stockAntes
    FROM produto
    WHERE id_produto = OLD.id_produto;

    SET stockDepois = stockAntes + OLD.quantidade;

    UPDATE produto
    SET stock_atual = stockDepois
    WHERE id_produto = OLD.id_produto;

    INSERT INTO stock_movimento
    (id_produto, tipo_movimento, quantidade, stock_antes, stock_depois, observacao)
    VALUES
    (OLD.id_produto, 'AJUSTE', OLD.quantidade, stockAntes, stockDepois, 'Remoção de venda');
END $$

DELIMITER ;

