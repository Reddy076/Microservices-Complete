CREATE DATABASE IF NOT EXISTS user_db;
CREATE DATABASE IF NOT EXISTS vault_db;
CREATE DATABASE IF NOT EXISTS security_db;
CREATE DATABASE IF NOT EXISTS generator_db;
CREATE DATABASE IF NOT EXISTS notification_db;
CREATE DATABASE IF NOT EXISTS ai_db;

-- ---------------------------------------------------------
-- ISOLATED SERVICE ACCOUNTS FOR MAXIMUM SECURITY
-- ---------------------------------------------------------

-- User Service
CREATE USER IF NOT EXISTS 'user_user'@'%' IDENTIFIED BY 'user_pass';
GRANT ALL PRIVILEGES ON user_db.* TO 'user_user'@'%';

-- Vault Service
CREATE USER IF NOT EXISTS 'vault_user'@'%' IDENTIFIED BY 'vault_pass';
GRANT ALL PRIVILEGES ON vault_db.* TO 'vault_user'@'%';

-- Security Service
CREATE USER IF NOT EXISTS 'security_user'@'%' IDENTIFIED BY 'security_pass';
GRANT ALL PRIVILEGES ON security_db.* TO 'security_user'@'%';

-- Generator Service
CREATE USER IF NOT EXISTS 'generator_user'@'%' IDENTIFIED BY 'generator_pass';
GRANT ALL PRIVILEGES ON generator_db.* TO 'generator_user'@'%';

-- Notification Service
CREATE USER IF NOT EXISTS 'notification_user'@'%' IDENTIFIED BY 'notification_pass';
GRANT ALL PRIVILEGES ON notification_db.* TO 'notification_user'@'%';

-- AI Service
CREATE USER IF NOT EXISTS 'ai_user'@'%' IDENTIFIED BY 'ai_pass';
GRANT ALL PRIVILEGES ON ai_db.* TO 'ai_user'@'%';

FLUSH PRIVILEGES;
