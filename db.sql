-- CREATE DATABASE Shop;
-- USE Shop;

CREATE TABLE Users (
    UserID INT NOT NULL AUTO_INCREMENT,
    FirstName TEXT,
    LastName TEXT,
    Username VARCHAR(35),
    PRIMARY KEY (UserID)
) ;

ALTER TABLE `Shop`.`Users` 
MODIFY COLUMN UserId BIGINT NOT NULL AUTO_INcREMENT;



CREATE TABLE Goods (
    GoodsID INT NOT NULL AUTO_INCREMENT,
    Price INT,
    G_Count INT,
    G_Name VARCHAR(100),
    G_CPU VARCHAR(100),
    RAM INT,
    G_Memory INT,
    Weight DOUBLE,
    Msg_ID INT,
    PRIMARY KEY (GoodsID)
) ;

CREATE TABLE Cart (
    UserID BIGINT NOT NULL,
    GoodsID INT NOT NULL,
    PRIMARY KEY (UserID, GoodsID),
    CONSTRAINT Constr_Cart_User_fk
        FOREIGN KEY User_fk (UserID) REFERENCES Users (UserID)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT Constr_Cart_Goods_fk
        FOREIGN KEY Goods_fk (GoodsID) REFERENCES Goods (GoodsID)
        ON DELETE CASCADE ON UPDATE CASCADE
);

ALTER TABLE `Shop`.`Cart` 
ADD COLUMN Count INT DEFAULT 1;


SELECT * FROM `Shop`.`Goods` order by GoodsID desc limit 1;

SELECT * FROM `Shop`.`Cart` c
JOIN `Shop`.`Goods` g 
ON g.GoodsID = c.GoodsID 
WHERE c.UserID = '5221480491' AND g.GoodsID = 11;


DROP PROCEDURE Purchase;
DELIMITER $$ 
CREATE PROCEDURE Purchase(U_ID bigint , givenGoodsID int)
BEGIN 
	SET @G_count = (SELECT c.Count FROM `Shop`.`Cart` c
		WHERE c.UserID = U_ID AND c.GoodsID = givenGoodsID);

	IF @G_count > 0 THEN
		UPDATE `Shop`.`Cart` c SET c.Count = @G_count + 1 
        WHERE c.UserID = U_ID AND c.GoodsID = givenGoodsID ;
    ELSE 
		INSERT INTO `Shop`.`Cart` (`UserID`, `GoodsID`) VALUES (U_ID, givenGoodsID);
    END IF;
END $$ DELIMITER ;


CALL `Shop`.Purchase(5221480491, 15);

SELECT * FROM `Shop`.`Cart` ;

UPDATE `Shop`.`Goods` SET G_Count = 10
where GoodsID = 6;

UPDATE `Shop`.`Goods` SET G_Count = 10
WHERE GoodsID = 8;

SELECT * FROM `Shop`.`Users` WHERE UserID = 1;
UPDATE `Shop`.`Users` SET  `FirstName` = 43, `LastName` = 43, `Username` = 432 WHERE UserID = 1