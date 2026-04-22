-- Flyway migration: create player_results table
-- Compatible with Microsoft SQL Server (Azure SQL)

CREATE TABLE player_results (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    nickname VARCHAR(255) NOT NULL,
    result VARCHAR(10),
    guess_count INT NULL,
    opponent VARCHAR(255) NULL,
    match_id VARCHAR(255) NULL,
    created_at DATETIME2 DEFAULT SYSUTCDATETIME()
);

CREATE INDEX idx_player_nickname ON player_results(nickname);
