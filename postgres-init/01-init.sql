-- =============================================
-- Idempotent initialization script for microfinance system
-- Can be run multiple times safely
-- =============================================

-- Enable stop on error but continue through DO blocks
\set ON_ERROR_STOP off

-- =============================================
-- 1. Default admin user (password: admin123)
-- =============================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin') THEN
        INSERT INTO users (username, email, password, first_name, last_name, role, active, created_at, failed_login_attempts)
        VALUES (
            'admin',
            'admin@microfinance.com',
            '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW',
            'System',
            'Administrator',
            'SUPER_ADMIN',
            true,
            CURRENT_TIMESTAMP,
            0
        );
    END IF;
END $$;

-- =============================================
-- 2. Sample loan officer (password: officer123)
-- =============================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM users WHERE username = 'officer1') THEN
        INSERT INTO users (username, email, password, first_name, last_name, role, active, created_at, failed_login_attempts)
        VALUES (
            'officer1',
            'officer1@microfinance.com',
            '$2a$12$4R1J7Q8eN9S0A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q7R8S9T0U1V',
            'John',
            'Doe',
            'LOAN_OFFICER',
            true,
            CURRENT_TIMESTAMP,
            0
        );
    END IF;
END $$;

-- =============================================
-- 3. Sample credit approver (password: approver123)
-- =============================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM users WHERE username = 'approver1') THEN
        INSERT INTO users (username, email, password, first_name, last_name, role, active, created_at, failed_login_attempts)
        VALUES (
            'approver1',
            'approver1@microfinance.com',
            '$2a$12$W1X2Y3Z4A5B6C7D8E9F0G1H2I3J4K5L6M7N8O9P0Q1R2S3T4U5V6W7X',
            'Jane',
            'Smith',
            'CREDIT_APPROVER',
            true,
            CURRENT_TIMESTAMP,
            0
        );
    END IF;
END $$;

-- =============================================
-- 4. Role permissions for SUPER_ADMIN
-- =============================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM role_permissions WHERE role = 'SUPER_ADMIN' AND permission = 'USER_CREATE') THEN
        INSERT INTO role_permissions (role, permission, description) VALUES
        ('SUPER_ADMIN', 'USER_CREATE', 'Create users'),
        ('SUPER_ADMIN', 'USER_READ', 'View users'),
        ('SUPER_ADMIN', 'USER_UPDATE', 'Update users'),
        ('SUPER_ADMIN', 'USER_DELETE', 'Delete users'),
        ('SUPER_ADMIN', 'BORROWER_CREATE', 'Create borrowers'),
        ('SUPER_ADMIN', 'BORROWER_READ', 'View borrowers'),
        ('SUPER_ADMIN', 'BORROWER_UPDATE', 'Update borrowers'),
        ('SUPER_ADMIN', 'BORROWER_DELETE', 'Delete borrowers');
    END IF;
END $$;

-- =============================================
-- 5. Role permissions for LOAN_OFFICER
-- =============================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM role_permissions WHERE role = 'LOAN_OFFICER' AND permission = 'BORROWER_CREATE') THEN
        INSERT INTO role_permissions (role, permission, description) VALUES
        ('LOAN_OFFICER', 'BORROWER_CREATE', 'Create borrowers'),
        ('LOAN_OFFICER', 'BORROWER_READ', 'View borrowers'),
        ('LOAN_OFFICER', 'BORROWER_UPDATE', 'Update borrowers'),
        ('LOAN_OFFICER', 'APPLICATION_CREATE', 'Create loan applications');
    END IF;
END $$;

-- =============================================
-- 6. Role permissions for CREDIT_APPROVER
-- =============================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM role_permissions WHERE role = 'CREDIT_APPROVER' AND permission = 'APPLICATION_READ') THEN
        INSERT INTO role_permissions (role, permission, description) VALUES
        ('CREDIT_APPROVER', 'APPLICATION_READ', 'View loan applications'),
        ('CREDIT_APPROVER', 'APPLICATION_APPROVE', 'Approve/reject loan applications');
    END IF;
END $$;

-- =============================================
-- 7. System settings (single row, id=1)
-- =============================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM system_settings WHERE id = 1) THEN
        INSERT INTO system_settings (id, default_interest_calculation_method, default_interest_rate, default_penalty_rate,
                                   default_penalty_grace_period_days, company_name, default_currency, mfa_enabled,
                                   auto_backup_enabled, created_at, updated_at)
        VALUES (1, 'REDUCING_BALANCE', 12.5, 2.0, 7, 'Microfinance System', 'USD', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
    END IF;
END $$;

-- =============================================
-- 8. Default branches
-- =============================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM branches WHERE code = 'HO-001') THEN
        INSERT INTO branches (code, name, type, address, phone, active, created_at, updated_at)
        VALUES
        ('HO-001', 'Head Office', 'HEAD_OFFICE', '123 Main Street, City', '+1-555-0101', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('BR-001', 'Downtown Branch', 'BRANCH', '456 Downtown Ave, City', '+1-555-0102', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
    END IF;
END $$;

-- =============================================
-- 9. Default currencies
-- =============================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM currency_settings WHERE currency_code = 'USD') THEN
        INSERT INTO currency_settings (currency_code, currency_name, symbol, exchange_rate, default_value, active, created_at, updated_at)
        VALUES
        ('USD', 'US Dollar', '$', 1.0, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('EUR', 'Euro', '€', 0.85, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('GBP', 'British Pound', '£', 0.73, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
    END IF;
END $$;

-- =============================================
-- 10. Default number sequences
-- =============================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM number_sequences WHERE sequence_code = 'LOAN_APP') THEN
        INSERT INTO number_sequences (sequence_code, description, prefix, suffix, next_value, padding, reset_daily, reset_monthly, reset_yearly, active, created_at, updated_at)
        VALUES
        ('LOAN_APP', 'Loan Application Number', 'LA-', '', 1, 6, false, false, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('BORROWER', 'Borrower ID', 'BOR-', '', 1, 6, false, false, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('PAYMENT', 'Payment Receipt', 'PMT-', '', 1, 6, true, false, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
        ('EARLY_REPAYMENT', 'Early Repayment', 'ERP-', '', 1, 6, true, false, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
    END IF;
END $$;

-- =============================================
-- 11. Default holidays (check by name and date)
-- =============================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM holiday_calendar WHERE name = 'New Year''s Day' AND holiday_date = '2024-01-01') THEN
        INSERT INTO holiday_calendar (name, holiday_date, description, recurring, country_code, active)
        VALUES
        ('New Year''s Day', '2024-01-01', 'New Year''s Day Celebration', true, 'US', true),
        ('Christmas Day', '2024-12-25', 'Christmas Day Celebration', true, 'US', true);
    END IF;
END $$;



-- =====================================================
-- Financial Account Categories & Accounts (Idempotent)
-- =====================================================

-- Insert Account Categories (if not already present)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM fin_account_categories WHERE code = 'ASSET_C') THEN
        INSERT INTO fin_account_categories (code, name, description, account_type, normal_balance, sort_order) VALUES
        ('ASSET_C', 'Assets', 'All asset accounts', 'ASSET', 'DEBIT', 10),
        ('LIABILITY_C', 'Liabilities', 'All liability accounts', 'LIABILITY', 'CREDIT', 20),
        ('EQUITY_C', 'Equity', 'Equity accounts', 'EQUITY', 'CREDIT', 30),
        ('INCOME_C', 'Income', 'Income and revenue accounts', 'INCOME', 'CREDIT', 40),
        ('EXPENSE_C', 'Expenses', 'Expense accounts', 'EXPENSE', 'DEBIT', 50);
    END IF;
END $$;

-- Insert Asset Accounts (only if not already present)
DO $$
DECLARE
    asset_cat_id INTEGER;
BEGIN
    SELECT id INTO asset_cat_id FROM fin_account_categories WHERE code = 'ASSET_C' LIMIT 1;
    IF asset_cat_id IS NOT NULL THEN
        IF NOT EXISTS (SELECT 1 FROM fin_accounts WHERE code = '1010') THEN
            INSERT INTO fin_accounts (code, name, description, category_id, account_type, normal_balance, created_at)
            VALUES ('1010', 'Cash on Hand', 'Physical cash in office', asset_cat_id, 'ASSET', 'DEBIT', NOW());
        END IF;
        IF NOT EXISTS (SELECT 1 FROM fin_accounts WHERE code = '1020') THEN
            INSERT INTO fin_accounts (code, name, description, category_id, account_type, normal_balance, created_at)
            VALUES ('1020', 'Bank Account - Operating', 'Main operating bank account', asset_cat_id, 'ASSET', 'DEBIT', NOW());
        END IF;
              -- NEW: Bank Collections Account (1030)
        IF NOT EXISTS (SELECT 1 FROM fin_accounts WHERE code = '1030') THEN
            INSERT INTO fin_accounts (code, name, description, category_id, account_type, normal_balance, created_at)
            VALUES ('1030', 'Bank Account - Collections', 'Bank account for loan collections and repayments', asset_cat_id, 'ASSET', 'DEBIT', NOW());
        END IF;
        IF NOT EXISTS (SELECT 1 FROM fin_accounts WHERE code = '1110') THEN
            INSERT INTO fin_accounts (code, name, description, category_id, account_type, normal_balance, created_at)
            VALUES ('1110', 'Gross Loan Portfolio', 'Total principal amount of active loans', asset_cat_id, 'ASSET', 'DEBIT', NOW());
        END IF;

        -- NEW: Fees Receivable Account (1130)
        IF NOT EXISTS (SELECT 1 FROM fin_accounts WHERE code = '1130') THEN
            INSERT INTO fin_accounts (code, name, description, category_id, account_type, normal_balance, created_at)
            VALUES ('1130', 'Fees Receivable', 'Accrued fees not yet received', asset_cat_id, 'ASSET', 'DEBIT', NOW());
        END IF;

        IF NOT EXISTS (SELECT 1 FROM fin_accounts WHERE code = '1120') THEN
            INSERT INTO fin_accounts (code, name, description, category_id, account_type, normal_balance, created_at)
            VALUES ('1120', 'Interest Receivable', 'Accrued interest not yet received', asset_cat_id, 'ASSET', 'DEBIT', NOW());
        END IF;
        IF NOT EXISTS (SELECT 1 FROM fin_accounts WHERE code = '1310') THEN
            INSERT INTO fin_accounts (code, name, description, category_id, account_type, normal_balance, created_at)
            VALUES ('1310', 'Loan Loss Provision', 'Provision for expected credit losses', asset_cat_id, 'ASSET', 'CREDIT', NOW());
        END IF;
    END IF;
END $$;

-- Insert Liability Accounts
DO $$
DECLARE
    liab_cat_id INTEGER;
BEGIN
    SELECT id INTO liab_cat_id FROM fin_account_categories WHERE code = 'LIABILITY_C' LIMIT 1;
    IF liab_cat_id IS NOT NULL THEN
        IF NOT EXISTS (SELECT 1 FROM fin_accounts WHERE code = '2010') THEN
            INSERT INTO fin_accounts (code, name, description, category_id, account_type, normal_balance, created_at)
            VALUES ('2010', 'Accounts Payable', 'Amounts owed to suppliers', liab_cat_id, 'LIABILITY', 'CREDIT', NOW());
        END IF;
        IF NOT EXISTS (SELECT 1 FROM fin_accounts WHERE code = '2040') THEN
            INSERT INTO fin_accounts (code, name, description, category_id, account_type, normal_balance, created_at)
            VALUES ('2040', 'Tax Payable - Withholding', 'Withholding tax collected', liab_cat_id, 'LIABILITY', 'CREDIT', NOW());
        END IF;
        -- NEW: Interest Payable
        IF NOT EXISTS (SELECT 1 FROM fin_accounts WHERE code = '2050') THEN
            INSERT INTO fin_accounts (code, name, description, category_id, account_type, normal_balance, created_at)
            VALUES ('2050', 'Interest Payable', 'Interest owed to depositors or creditors', liab_cat_id, 'LIABILITY', 'CREDIT', NOW());
        END IF;

    END IF;
END $$;

-- Insert Equity Accounts
DO $$
DECLARE
    equity_cat_id INTEGER;
BEGIN
    SELECT id INTO equity_cat_id FROM fin_account_categories WHERE code = 'EQUITY_C' LIMIT 1;
    IF equity_cat_id IS NOT NULL THEN
        IF NOT EXISTS (SELECT 1 FROM fin_accounts WHERE code = '3010') THEN
            INSERT INTO fin_accounts (code, name, description, category_id, account_type, normal_balance, created_at)
            VALUES ('3010', 'Share Capital', 'Paid-in capital from shareholders', equity_cat_id, 'EQUITY', 'CREDIT', NOW());
        END IF;
        IF NOT EXISTS (SELECT 1 FROM fin_accounts WHERE code = '3110') THEN
            INSERT INTO fin_accounts (code, name, description, category_id, account_type, normal_balance, created_at)
            VALUES ('3110', 'Retained Earnings', 'Cumulative retained profits', equity_cat_id, 'EQUITY', 'CREDIT', NOW());
        END IF;
        -- NEW: Current Year Earnings
        IF NOT EXISTS (SELECT 1 FROM fin_accounts WHERE code = '3120') THEN
            INSERT INTO fin_accounts (code, name, description, category_id, account_type, normal_balance, created_at)
            VALUES ('3120', 'Current Year Earnings', 'Net profit/loss for current fiscal year', equity_cat_id, 'EQUITY', 'CREDIT', NOW());
        END IF;

    END IF;
END $$;

-- Insert Income Accounts
DO $$
DECLARE
    income_cat_id INTEGER;
BEGIN
    SELECT id INTO income_cat_id FROM fin_account_categories WHERE code = 'INCOME_C' LIMIT 1;
    IF income_cat_id IS NOT NULL THEN
        IF NOT EXISTS (SELECT 1 FROM fin_accounts WHERE code = '4010') THEN
            INSERT INTO fin_accounts (code, name, description, category_id, account_type, normal_balance, created_at)
            VALUES ('4010', 'Interest Income - Loans', 'Interest earned on loans', income_cat_id, 'INCOME', 'CREDIT', NOW());
        END IF;
        IF NOT EXISTS (SELECT 1 FROM fin_accounts WHERE code = '4030') THEN
            INSERT INTO fin_accounts (code, name, description, category_id, account_type, normal_balance, created_at)
            VALUES ('4030', 'Fee Income - Processing', 'Loan processing fees', income_cat_id, 'INCOME', 'CREDIT', NOW());
        END IF;
        -- NEW: Penalty Income (4060)
        IF NOT EXISTS (SELECT 1 FROM fin_accounts WHERE code = '4060') THEN
            INSERT INTO fin_accounts (code, name, description, category_id, account_type, normal_balance, created_at)
            VALUES ('4060', 'Penalty Income', 'Late payment penalties and fees', income_cat_id, 'INCOME', 'CREDIT', NOW());
        END IF;

        IF NOT EXISTS (SELECT 1 FROM fin_accounts WHERE code = '4110') THEN
            INSERT INTO fin_accounts (code, name, description, category_id, account_type, normal_balance, created_at)
            VALUES ('4110', 'Recovery Income', 'Recoveries from written-off loans', income_cat_id, 'INCOME', 'CREDIT', NOW());
        END IF;
        -- NEW: Other Income
        IF NOT EXISTS (SELECT 1 FROM fin_accounts WHERE code = '4120') THEN
            INSERT INTO fin_accounts (code, name, description, category_id, account_type, normal_balance, created_at)
            VALUES ('4120', 'Other Income', 'Miscellaneous income', income_cat_id, 'INCOME', 'CREDIT', NOW());
        END IF;

    END IF;
END $$;

-- Insert Expense Accounts
DO $$
DECLARE
    expense_cat_id INTEGER;
BEGIN
    SELECT id INTO expense_cat_id FROM fin_account_categories WHERE code = 'EXPENSE_C' LIMIT 1;
    IF expense_cat_id IS NOT NULL THEN
        IF NOT EXISTS (SELECT 1 FROM fin_accounts WHERE code = '5010') THEN
            INSERT INTO fin_accounts (code, name, description, category_id, account_type, normal_balance, created_at)
            VALUES ('5010', 'Salaries and Wages', 'Employee compensation', expense_cat_id, 'EXPENSE', 'DEBIT', NOW());
        END IF;
    -- NEW: Operating Expenses
    IF NOT EXISTS (SELECT 1 FROM fin_accounts WHERE code = '5020') THEN
        INSERT INTO fin_accounts (code, name, description, category_id, account_type, normal_balance, created_at)
        VALUES ('5020', 'Rent and Utilities', 'Office rent and utility expenses', expense_cat_id, 'EXPENSE', 'DEBIT', NOW());
    END IF;
        IF NOT EXISTS (SELECT 1 FROM fin_accounts WHERE code = '5210') THEN
            INSERT INTO fin_accounts (code, name, description, category_id, account_type, normal_balance, created_at)
            VALUES ('5210', 'Provision Expense', 'Provision for loan losses', expense_cat_id, 'EXPENSE', 'DEBIT', NOW());
        END IF;
        IF NOT EXISTS (SELECT 1 FROM fin_accounts WHERE code = '5220') THEN
            INSERT INTO fin_accounts (code, name, description, category_id, account_type, normal_balance, created_at)
            VALUES ('5220', 'Write-off Expense', 'Loans written off as uncollectible', expense_cat_id, 'EXPENSE', 'DEBIT', NOW());
        END IF;
         -- NEW: Interest Expense
         IF NOT EXISTS (SELECT 1 FROM fin_accounts WHERE code = '5040') THEN
             INSERT INTO fin_accounts (code, name, description, category_id, account_type, normal_balance, created_at)
             VALUES ('5040', 'Interest Expense', 'Interest paid on deposits or borrowings', expense_cat_id, 'EXPENSE', 'DEBIT', NOW());
         END IF;

    END IF;
END $$;


-- Verify all required accounts exist
DO $$
DECLARE
    missing_accounts TEXT[];
    required_accounts TEXT[] := ARRAY['1010', '1020', '1030', '1110', '1120', '1130', '1310',
                                        '4010', '4030', '4060', '4110', '5210', '5220'];
    acc_code TEXT;
BEGIN
    FOREACH acc_code IN ARRAY required_accounts
    LOOP
        IF NOT EXISTS (SELECT 1 FROM fin_accounts WHERE code = acc_code) THEN
            missing_accounts := array_append(missing_accounts, acc_code);
        END IF;
    END LOOP;

    IF array_length(missing_accounts, 1) > 0 THEN
        RAISE NOTICE 'WARNING: Missing accounts: %', missing_accounts;
    ELSE
        RAISE NOTICE 'SUCCESS: All required accounts exist';
    END IF;
END $$;



DO $$
BEGIN

UPDATE fin_account_categories
SET is_active = true,
    created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, NOW())
WHERE is_active IS NULL;


UPDATE fin_accounts
SET is_active = true,
    created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, NOW())
WHERE is_active IS NULL;


END $$;

