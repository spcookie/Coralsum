-- Change user_points integer columns to decimal
ALTER TABLE user_points
    ALTER COLUMN permanent_points DECIMAL(18, 6) NOT NULL DEFAULT 0;
ALTER TABLE user_points
    ALTER COLUMN subscribe_points DECIMAL(18, 6) NOT NULL DEFAULT 0;

