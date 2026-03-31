#!/bin/bash
# ============================================================
# ADC Individual Project — Full Test Script
# Tests all 10 REST operations in sequence
# Usage: chmod +x test_all.sh && ./test_all.sh
# ============================================================

BASE_URL="http://localhost:8080/rest"
# For local testing use:
# BASE_URL="http://localhost:8080/rest"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Helper function
call() {
    NAME=$1
    URL=$2
    DATA=$3
    echo -e "${CYAN}=============================="
    echo "TEST: $NAME"
    echo -e "==============================${NC}"
    echo "POST $URL"
    echo "Payload: $DATA"
    RESPONSE=$(curl -s -X POST "$URL" \
        -H "Content-Type: application/json" \
        -d "$DATA")
    echo -e "${GREEN}Response:${NC}"
    echo "$RESPONSE"
    echo ""
}

# ============================================================
# SETUP: extract tokens after login using grep+sed
# (replace manually if your shell doesn't support this)
# ============================================================

echo -e "${CYAN}=============================="
echo "STEP 1 — Create test accounts"
echo -e "==============================${NC}"

# Op1 — Create a USER account
call "Op1 - Create USER account" \
    "$BASE_URL/createaccount" \
    '{"input":{"username":"tomas@adc.pt","password":"pass123","confirmation":"pass123","phone":"912345678","address":"Lisbon","role":"USER"}}'

# Op1 — Create a BOFFICER account
call "Op1 - Create BOFFICER account" \
    "$BASE_URL/createaccount" \
    '{"input":{"username":"tomasBofficer@adc.pt","password":"pass123","confirmation":"pass123","phone":"912345679","address":"Porto","role":"BOFFICER"}}'

# Op1 — Create an ADMIN account
call "Op1 - Create ADMIN account" \
    "$BASE_URL/createaccount" \
    '{"input":{"username":"tomasAdmin@adc.pt","password":"pass123","confirmation":"pass123","phone":"912345670","address":"Faro","role":"ADMIN"}}'

# Op1 — Error: duplicate username
call "Op1 - ERROR: USER_ALREADY_EXISTS" \
    "$BASE_URL/createaccount" \
    '{"input":{"username":"tomas@adc.pt","password":"pass123","confirmation":"pass123","phone":"912345678","address":"Lisbon","role":"USER"}}'

# Op1 — Error: invalid input (password mismatch)
call "Op1 - ERROR: INVALID_INPUT (password mismatch)" \
    "$BASE_URL/createaccount" \
    '{"input":{"username":"x@adc.pt","password":"abc","confirmation":"xyz","phone":"912345678","address":"Lisbon","role":"USER"}}'

# ============================================================
echo -e "${CYAN}=============================="
echo "STEP 2 — Login and get tokens"
echo "NOTE: copy tokenId values from responses below"
echo "      and replace TOKEN_USER / TOKEN_BOFFICER / TOKEN_ADMIN"
echo -e "==============================${NC}"

# Op2 — Login as USER
call "Op2 - Login as USER" \
    "$BASE_URL/login" \
    '{"input":{"username":"tomas@adc.pt","password":"pass123"}}'

# Op2 — Login as BOFFICER
call "Op2 - Login as BOFFICER" \
    "$BASE_URL/login" \
    '{"input":{"username":"tomasBofficer@adc.pt","password":"pass123"}}'

# Op2 — Login as ADMIN
call "Op2 - Login as ADMIN" \
    "$BASE_URL/login" \
    '{"input":{"username":"tomasAdmin@adc.pt","password":"pass123"}}'

# Op2 — Error: wrong password
call "Op2 - ERROR: INVALID_CREDENTIALS" \
    "$BASE_URL/login" \
    '{"input":{"username":"tomas@adc.pt","password":"wrongpass"}}'

# Op2 — Error: user not found
call "Op2 - ERROR: USER_NOT_FOUND" \
    "$BASE_URL/login" \
    '{"input":{"username":"ghost@adc.pt","password":"pass123"}}'

# ============================================================
# IMPORTANT: replace these with real tokenIds from the login responses above!
TOKEN_USER="7a5f639d-36ea-4498-8b86-f915b4d57766"
TOKEN_BOFFICER="3789a2da-42b9-467c-91e2-3b89981733b3"
TOKEN_ADMIN="a5ca7765-6652-4060-9766-d3ed9e5eb6d1"
# ============================================================

echo -e "${CYAN}=============================="
echo "STEP 3 — Authenticated operations"
echo -e "==============================${NC}"

# Op3 — ShowUsers (ADMIN)
call "Op3 - ShowUsers as ADMIN" \
    "$BASE_URL/showusers" \
    "{\"input\":{},\"token\":{\"tokenId\":\"$TOKEN_ADMIN\"}}"

# Op3 — ShowUsers (BOFFICER)
call "Op3 - ShowUsers as BOFFICER" \
    "$BASE_URL/showusers" \
    "{\"input\":{},\"token\":{\"tokenId\":\"$TOKEN_BOFFICER\"}}"

# Op3 — Error: USER not allowed
call "Op3 - ERROR: UNAUTHORIZED (USER)" \
    "$BASE_URL/showusers" \
    "{\"input\":{},\"token\":{\"tokenId\":\"$TOKEN_USER\"}}"

# Op5 — ModifyAccount (USER modifies own)
call "Op5 - ModifyAccount (USER modifies own)" \
    "$BASE_URL/modaccount" \
    "{\"input\":{\"username\":\"tomas@adc.pt\",\"attributes\":{\"phone\":\"999999999\",\"address\":\"New Lisbon\"}},\"token\":{\"tokenId\":\"$TOKEN_USER\"}}"

# Op5 — Error: USER tries to modify another account
call "Op5 - ERROR: FORBIDDEN (USER modifies other)" \
    "$BASE_URL/modaccount" \
    "{\"input\":{\"username\":\"tomasBofficer@adc.pt\",\"attributes\":{\"phone\":\"111111111\"}},\"token\":{\"tokenId\":\"$TOKEN_USER\"}}"

# Op6 — ShowAuthSessions (ADMIN only)
call "Op6 - ShowAuthSessions as ADMIN" \
    "$BASE_URL/showauthsessions" \
    "{\"input\":{},\"token\":{\"tokenId\":\"$TOKEN_ADMIN\"}}"

# Op6 — Error: BOFFICER not allowed
call "Op6 - ERROR: UNAUTHORIZED (BOFFICER)" \
    "$BASE_URL/showauthsessions" \
    "{\"input\":{},\"token\":{\"tokenId\":\"$TOKEN_BOFFICER\"}}"

# Op7 — ShowUserRole (ADMIN)
call "Op7 - ShowUserRole as ADMIN" \
    "$BASE_URL/showuserrole" \
    "{\"input\":{\"username\":\"tomas@adc.pt\"},\"token\":{\"tokenId\":\"$TOKEN_ADMIN\"}}"

# Op7 — BOFFICER checking an ADMIN role → FORBIDDEN
call "Op7 - ERROR: FORBIDDEN (BOFFICER checks ADMIN role)" \
    "$BASE_URL/showuserrole" \
    "{\"input\":{\"username\":\"tomasAdmin@adc.pt\"},\"token\":{\"tokenId\":\"$TOKEN_BOFFICER\"}}"

# Op8 — ChangeUserRole (ADMIN changes USER to BOFFICER)
call "Op8 - ChangeUserRole USER -> BOFFICER" \
    "$BASE_URL/changeuserrole" \
    "{\"input\":{\"username\":\"tomas@adc.pt\",\"newRole\":\"BOFFICER\"},\"token\":{\"tokenId\":\"$TOKEN_ADMIN\"}}"

# Op8 — Error: ADMIN changes own role → FORBIDDEN
call "Op8 - ERROR: FORBIDDEN (ADMIN changes own role)" \
    "$BASE_URL/changeuserrole" \
    "{\"input\":{\"username\":\"tomasAdmin@adc.pt\",\"newRole\":\"USER\"},\"token\":{\"tokenId\":\"$TOKEN_ADMIN\"}}"

# Op9 — ChangeUserPassword (USER changes own password)
call "Op9 - ChangeUserPassword (own account)" \
    "$BASE_URL/changeuserpwd" \
    "{\"input\":{\"username\":\"tomas@adc.pt\",\"oldPassword\":\"pass123\",\"newPassword\":\"newpass456\"},\"token\":{\"tokenId\":\"$TOKEN_USER\"}}"

# Op9 — Error: wrong old password
call "Op9 - ERROR: INVALID_CREDENTIALS (wrong old password)" \
    "$BASE_URL/changeuserpwd" \
    "{\"input\":{\"username\":\"tomas@adc.pt\",\"oldPassword\":\"wrongpass\",\"newPassword\":\"newpass456\"},\"token\":{\"tokenId\":\"$TOKEN_USER\"}}"

# Op9 — Error: trying to change someone else's password
call "Op9 - ERROR: FORBIDDEN (change other's password)" \
    "$BASE_URL/changeuserpwd" \
    "{\"input\":{\"username\":\"tomasBofficer@adc.pt\",\"oldPassword\":\"pass123\",\"newPassword\":\"newpass\"},\"token\":{\"tokenId\":\"$TOKEN_USER\"}}"

# Op4 — DeleteAccount (ADMIN deletes USER)
call "Op4 - DeleteAccount (ADMIN deletes USER)" \
    "$BASE_URL/deleteaccount" \
    "{\"input\":{\"username\":\"tomas@adc.pt\"},\"token\":{\"tokenId\":\"$TOKEN_ADMIN\"}}"

# Op4 — Error: ADMIN deletes own account → FORBIDDEN
call "Op4 - ERROR: FORBIDDEN (ADMIN deletes self)" \
    "$BASE_URL/deleteaccount" \
    "{\"input\":{\"username\":\"tomasAdmin@adc.pt\"},\"token\":{\"tokenId\":\"$TOKEN_ADMIN\"}}"

# Op10 — Logout
call "Op10 - Logout (USER)" \
    "$BASE_URL/logout" \
    "{\"input\":{\"username\":\"tomasBofficer@adc.pt\"},\"token\":{\"tokenId\":\"$TOKEN_BOFFICER\"}}"

# Op10 — ADMIN logs out anyone
call "Op10 - Logout (ADMIN logs out BOFFICER)" \
    "$BASE_URL/logout" \
    "{\"input\":{\"username\":\"tomasBofficer@adc.pt\"},\"token\":{\"tokenId\":\"$TOKEN_ADMIN\"}}"

echo -e "${GREEN}=============================="
echo "All tests completed!"
echo -e "==============================${NC}"
