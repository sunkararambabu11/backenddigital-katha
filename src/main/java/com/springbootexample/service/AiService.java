package com.springbootexample.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.springbootexample.entity.Customer;
import com.springbootexample.entity.Transaction;
import com.springbootexample.repository.CustomerRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    @Value("${openai.api.key:YOUR_API_KEY_HERE}")
    private String apiKey;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private DashboardService dashboardService;

    private final RestTemplate restTemplate = new RestTemplate();

    // =========================================================================
    // 1. Multi-turn Interactive Session States
    // =========================================================================

    public static class CustomerCreationSession {
        public enum Step {
            AWAITING_NAME,
            AWAITING_PHONE,
            AWAITING_DESCRIPTION
        }

        private Step step;
        private String name = "";
        private String mobile = "";
        private String description = "";
        private Double openingBalance = 0.0;
        private long lastUpdatedTime = System.currentTimeMillis();

        public CustomerCreationSession(Step step) {
            this.step = step;
        }

        public Step getStep() { return step; }
        public void setStep(Step step) { this.step = step; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getMobile() { return mobile; }
        public void setMobile(String mobile) { this.mobile = mobile; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Double getOpeningBalance() { return openingBalance; }
        public void setOpeningBalance(Double openingBalance) { this.openingBalance = openingBalance; }

        public long getLastUpdatedTime() { return lastUpdatedTime; }
        public void setLastUpdatedTime(long lastUpdatedTime) { this.lastUpdatedTime = lastUpdatedTime; }
    }

    public static class TransactionCreationSession {
        public enum Step {
            AWAITING_CUSTOMER,
            AWAITING_TYPE,
            AWAITING_AMOUNT,
            AWAITING_DESCRIPTION
        }

        private Step step;
        private Long customerId;
        private String customerName = "";
        private String type = ""; // DEBIT or CREDIT
        private Double amount = 0.0;
        private String description = "";
        private long lastUpdatedTime = System.currentTimeMillis();

        public TransactionCreationSession(Step step) {
            this.step = step;
        }

        public Step getStep() { return step; }
        public void setStep(Step step) { this.step = step; }

        public Long getCustomerId() { return customerId; }
        public void setCustomerId(Long customerId) { this.customerId = customerId; }

        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public long getLastUpdatedTime() { return lastUpdatedTime; }
        public void setLastUpdatedTime(long lastUpdatedTime) { this.lastUpdatedTime = lastUpdatedTime; }
    }

    private final Map<String, CustomerCreationSession> activeCustomerSessions = new ConcurrentHashMap<>();
    private final Map<String, TransactionCreationSession> activeTxnSessions = new ConcurrentHashMap<>();

    // =========================================================================
    // 2. Main Entry Point
    // =========================================================================

    public Map<String, Object> process(String userId, String input) {
        if (input == null || input.trim().isEmpty()) {
            return Map.of("reply", "Please provide a valid message or command.");
        }

        String trimmedInput = input.trim();

        // 1. Check if user is in an active customer creation session
        CustomerCreationSession custSession = activeCustomerSessions.get(userId);
        if (custSession != null) {
            if (System.currentTimeMillis() - custSession.getLastUpdatedTime() > 600_000) {
                activeCustomerSessions.remove(userId);
            } else {
                if (isTransactionIntent(trimmedInput)) {
                    activeCustomerSessions.remove(userId);
                    return initiateTransactionCreation(userId, trimmedInput);
                }
                return handleInteractiveCustomerStep(userId, trimmedInput, custSession);
            }
        }

        // 2. Check if user is in an active transaction creation session
        TransactionCreationSession txnSession = activeTxnSessions.get(userId);
        if (txnSession != null) {
            if (System.currentTimeMillis() - txnSession.getLastUpdatedTime() > 600_000) {
                activeTxnSessions.remove(userId);
            } else {
                if (isCustomerCreationIntent(trimmedInput)) {
                    return initiateCustomerCreation(userId, trimmedInput);
                }
                // If user entered a fresh complete transaction command (e.g. "ramesh 1000 debit milk"),
                // execute it fresh instead of treating it as a single field input for the old prompt!
                if (isTransactionIntent(trimmedInput)) {
                    activeTxnSessions.remove(userId);
                    return initiateTransactionCreation(userId, trimmedInput);
                }
                return handleInteractiveTransactionStep(userId, trimmedInput, txnSession);
            }
        }

        // 3. User starting customer creation (e.g. "create customer")
        if (isCustomerCreationIntent(trimmedInput)) {
            return initiateCustomerCreation(userId, trimmedInput);
        }

        // 4. User starting transaction creation (e.g. "add transaction", "gave 500 to rahul")
        if (isTransactionIntent(trimmedInput)) {
            return initiateTransactionCreation(userId, trimmedInput);
        }

        // 5. OpenAI call if configured
        boolean hasValidApiKey = apiKey != null
                && !apiKey.isBlank()
                && !"YOUR_API_KEY_HERE".equalsIgnoreCase(apiKey.trim())
                && !apiKey.startsWith("YOUR_");

        if (hasValidApiKey) {
            try {
                String aiResponse = callOpenAI(trimmedInput);
                return handleAiResponse(userId, aiResponse, trimmedInput);
            } catch (HttpClientErrorException.Unauthorized e) {
                log.warn("OpenAI API Key is unauthorized (401). Falling back to local assistant.");
                return processLocally(userId, trimmedInput, "OpenAI API Key is invalid or unauthorized (401). Using built-in local assistant.");
            } catch (Exception e) {
                log.warn("OpenAI API call failed: {}. Falling back to local assistant.", e.getMessage());
                return processLocally(userId, trimmedInput, "OpenAI service unavailable (" + e.getMessage() + "). Using built-in local assistant.");
            }
        } else {
            return processLocally(userId, trimmedInput, null);
        }
    }

    // =========================================================================
    // 3. Helper: Customer Lookup
    // =========================================================================

    private Optional<Customer> findCustomerByUserAndQuery(Long userId, String query) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }
        String q = query.trim();
        List<Customer> customers = customerRepository.findByUserId(userId);
        if (customers.isEmpty()) {
            return Optional.empty();
        }

        // 1. Exact name match (case-insensitive)
        for (Customer c : customers) {
            if (c.getName() != null && c.getName().trim().equalsIgnoreCase(q)) {
                return Optional.of(c);
            }
        }

        // 2. Exact phone match
        String digits = q.replaceAll("[^0-9]", "");
        if (digits.length() == 10) {
            for (Customer c : customers) {
                if (c.getMobile() != null && c.getMobile().equals(digits)) {
                    return Optional.of(c);
                }
            }
        }

        // 3. Name contains query or query contains name
        for (Customer c : customers) {
            if (c.getName() != null) {
                String cName = c.getName().trim().toLowerCase();
                String qLower = q.toLowerCase();
                if (cName.contains(qLower) || qLower.contains(cName)) {
                    return Optional.of(c);
                }
            }
        }

        return Optional.empty();
    }

    // =========================================================================
    // 4. Intent Detectors
    // =========================================================================

    private boolean isCustomerCreationIntent(String input) {
        String lower = input.toLowerCase().trim();
        if (lower.contains("gave") || lower.contains("got") || lower.contains("transaction")
                || lower.contains("payment") || lower.contains("udhaar") || lower.contains("jama")) {
            return false;
        }

        // Comma-separated customer format: "ram,232323233,hyd" or "ram, 232323233"
        if (input.contains(",")) {
            String[] parts = input.split(",");
            if (parts.length >= 2) {
                String p0 = parts[0].replaceAll("(?i)\\b(create|add|new|customer)\\b", "").trim();
                String p1 = parts[1].trim().replaceAll("[^0-9]", "");
                if (p1.startsWith("91") && p1.length() == 12) {
                    p1 = p1.substring(2);
                }
                if (!p0.isEmpty() && p0.matches(".*[a-zA-Z].*") && p1.length() >= 7 && p1.length() <= 15) {
                    return true;
                }
            }
        }

        return lower.matches(".*\\b(create|add|new)\\s+(a\\s+)?(new\\s+)?customer\\b.*")
                || lower.matches(".*\\bcustomer\\s+(create|add|banaye|kare|karna)\\b.*")
                || lower.equals("create customer")
                || lower.equals("add customer")
                || lower.equals("new customer");
    }

    private boolean isTransactionIntent(String input) {
        String lower = input.toLowerCase().trim();
        if (lower.equals("add transaction") || lower.equals("new transaction")
                || lower.equals("create transaction") || lower.equals("record transaction")
                || lower.equals("transaction") || lower.startsWith("add transaction")
                || lower.startsWith("new transaction") || lower.startsWith("create transaction")
                || lower.startsWith("record transaction") || lower.contains("transaction")) {
            return true;
        }

        // Normalize letter/digit boundary: e.g. "ram1000" -> "ram 1000"
        String normalized = lower.replaceAll("(?<=[a-z])(?=\\d)|(?<=\\d)(?=[a-z])", " ");

        boolean hasAction = normalized.contains("gave") || normalized.contains("give") || normalized.contains("debit")
                || normalized.contains("udhaar") || normalized.contains("got") || normalized.contains("received")
                || normalized.contains("credit") || normalized.contains("jama") || normalized.contains("paid");

        return hasAction && !normalized.contains("customer");
    }

    // =========================================================================
    // 5. Customer Creation Wizard
    // =========================================================================

    private Map<String, Object> initiateCustomerCreation(String userId, String input) {
        // Direct comma-separated format: e.g. "ram,232323233,hyd" or "ram, 232323233, hyd, 500"
        if (input.contains(",")) {
            String[] parts = input.split(",");
            if (parts.length >= 2) {
                String rawName = parts[0].replaceAll("(?i)\\b(create|add|new|customer)\\b", "").trim();
                String rawPhone = parts[1].trim().replaceAll("[^0-9]", "");
                if (rawPhone.startsWith("91") && rawPhone.length() == 12) {
                    rawPhone = rawPhone.substring(2);
                }
                if (!rawName.isEmpty() && rawPhone.length() >= 7 && rawPhone.length() <= 15) {
                    String desc = "Created via Assistant";
                    Double openingBal = 0.0;
                    if (parts.length >= 3) {
                        String p2 = parts[2].trim();
                        if (p2.matches("^\\d+(\\.\\d+)?$")) {
                            try {
                                openingBal = Double.parseDouble(p2);
                                desc = "Opening balance: ₹" + openingBal;
                            } catch (Exception ignored) {}
                        } else {
                            desc = p2;
                        }
                    }
                    if (parts.length >= 4) {
                        String p3 = parts[3].trim();
                        if (p3.matches("^\\d+(\\.\\d+)?$")) {
                            try {
                                openingBal = Double.parseDouble(p3);
                            } catch (Exception ignored) {}
                        } else if ("Created via Assistant".equals(desc)) {
                            desc = p3;
                        } else {
                            desc = desc + ", " + p3;
                        }
                    }
                    return executeCreateCustomer(userId, rawName, rawPhone, desc, openingBal);
                }
            }
        }

        Matcher phoneMatcher = Pattern.compile("\\b([6-9]\\d{9}|\\d{7,15})\\b").matcher(input);
        String mobile = "";
        if (phoneMatcher.find()) {
            mobile = phoneMatcher.group(1);
        }

        Double openingBal = 0.0;
        String textWithoutPhone = !mobile.isEmpty() ? input.replace(mobile, " ") : input;
        Matcher numMatcher = Pattern.compile("\\b(\\d+(?:\\.\\d+)?)\\b").matcher(textWithoutPhone);
        if (numMatcher.find()) {
            try {
                openingBal = Double.parseDouble(numMatcher.group(1));
                textWithoutPhone = textWithoutPhone.replace(numMatcher.group(1), " ");
            } catch (Exception ignored) {}
        }

        String name = textWithoutPhone
                .replaceAll("(?i)\\b(create|add|new|a|customer|mobile|phone|opening|balance|rs|inr|rupees|description|note)\\b", "")
                .replaceAll("[^a-zA-Z\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (!name.isEmpty() && !mobile.isEmpty()) {
            if (openingBal > 0) {
                return executeCreateCustomer(userId, name, mobile, "Created via Assistant", openingBal);
            }
            CustomerCreationSession session = new CustomerCreationSession(CustomerCreationSession.Step.AWAITING_DESCRIPTION);
            session.setName(name);
            session.setMobile(mobile);
            session.setOpeningBalance(openingBal);
            activeCustomerSessions.put(userId, session);
            return Map.of("reply", String.format(
                "👤 Customer: **%s** (📱 %s)\n\nPlease enter a **Description / Note** for %s (or opening balance, or type **'skip'** to finish):",
                name, mobile, name
            ));
        }

        if (!name.isEmpty() && name.split(" ").length <= 4) {
            CustomerCreationSession session = new CustomerCreationSession(CustomerCreationSession.Step.AWAITING_PHONE);
            session.setName(name);
            activeCustomerSessions.put(userId, session);
            return Map.of("reply", String.format(
                "👤 Creating customer **%s**.\n\nPlease enter **%s's Phone Number** (7-15 digit mobile):\n_(Type 'cancel' anytime to abort)_",
                name, name
            ));
        }

        CustomerCreationSession session = new CustomerCreationSession(CustomerCreationSession.Step.AWAITING_NAME);
        activeCustomerSessions.put(userId, session);
        return Map.of("reply",
            "👤 **Create New Customer**\n\n" +
            "Please enter the **Customer Name**:\n" +
            "_(💡 Tip: You can quick-create in one shot: **Name, Phone, Description** e.g. `ram, 232323233, hyd`)_"
        );
    }

    private Map<String, Object> handleInteractiveCustomerStep(String userId, String input, CustomerCreationSession session) {
        String lower = input.toLowerCase().trim();

        if (lower.equals("cancel") || lower.equals("exit") || lower.equals("stop") || lower.equals("quit") || lower.equals("abort")) {
            activeCustomerSessions.remove(userId);
            return Map.of("reply", "❌ Customer creation cancelled. You can type **\"create customer\"** anytime to start again.");
        }

        if (lower.equals("create customer") || lower.equals("add customer") || lower.equals("new customer")) {
            activeCustomerSessions.remove(userId);
            return initiateCustomerCreation(userId, input);
        }

        if (input.contains(",")) {
            String[] parts = input.split(",");
            if (parts.length >= 2) {
                activeCustomerSessions.remove(userId);
                return initiateCustomerCreation(userId, input);
            }
        }

        if (session.getStep() == CustomerCreationSession.Step.AWAITING_NAME) {
            String cleanName = input.replaceAll("(?i)^(customer name is|name is|customer name:|name:)", "").trim();

            if (cleanName.isEmpty() || cleanName.matches("^\\d+$")) {
                return Map.of("reply", "⚠️ Please enter a valid customer name (letters and spaces), or type 'cancel' to exit:");
            }

            Matcher phoneMatcher = Pattern.compile("\\b([6-9]\\d{9}|\\d{7,15})\\b").matcher(cleanName);
            if (phoneMatcher.find()) {
                String mobile = phoneMatcher.group(1);
                String actualName = cleanName.replace(mobile, "").replaceAll("[^a-zA-Z\\s]", "").trim();
                session.setName(actualName.isEmpty() ? cleanName : actualName);
                session.setMobile(mobile);
                session.setStep(CustomerCreationSession.Step.AWAITING_DESCRIPTION);
                session.setLastUpdatedTime(System.currentTimeMillis());
                return Map.of("reply", String.format(
                    "👤 Customer: **%s** (📱 %s)\n\nPlease enter a **Description / Note** for %s (or opening balance, or type **'skip'**):",
                    session.getName(), mobile, session.getName()
                ));
            }

            session.setName(cleanName);
            session.setStep(CustomerCreationSession.Step.AWAITING_PHONE);
            session.setLastUpdatedTime(System.currentTimeMillis());
            return Map.of("reply", String.format(
                "Great! Customer Name: **%s** 👤\n\nNow, please enter the **Phone Number** (7-15 digit mobile):",
                session.getName()
            ));
        }

        if (session.getStep() == CustomerCreationSession.Step.AWAITING_PHONE) {
            String digits = input.replaceAll("[^0-9]", "");
            if (digits.startsWith("91") && digits.length() == 12) {
                digits = digits.substring(2);
            }

            if (digits.length() < 7 || digits.length() > 15) {
                return Map.of("reply", "⚠️ Please enter a valid mobile number (7-15 digits, e.g., 9876543210), or type 'cancel' to exit:");
            }

            Optional<Customer> existing = customerRepository.findByMobile(digits);
            if (existing.isPresent()) {
                return Map.of("reply", String.format(
                    "⚠️ A customer with mobile number **%s** already exists (**%s**).\n\nPlease enter a different phone number, or type 'cancel' to abort:",
                    digits, existing.get().getName()
                ));
            }

            session.setMobile(digits);
            session.setStep(CustomerCreationSession.Step.AWAITING_DESCRIPTION);
            session.setLastUpdatedTime(System.currentTimeMillis());
            return Map.of("reply", String.format(
                "Phone Number saved: **%s** 📱\n\nNow, please enter a **Description / Note** for **%s** (e.g., 'Regular customer', opening balance like '500', or type **'skip'**):",
                digits, session.getName()
            ));
        }

        if (session.getStep() == CustomerCreationSession.Step.AWAITING_DESCRIPTION) {
            String desc = "Created via Assistant";
            Double openingBal = 0.0;

            boolean isSkip = lower.equals("skip") || lower.equals("none") || lower.equals("no")
                    || lower.equals("-") || lower.equals("na") || lower.equals("n/a");

            if (!isSkip) {
                if (input.matches("^\\d+(\\.\\d+)?$")) {
                    openingBal = Double.parseDouble(input);
                    desc = "Opening balance: ₹" + openingBal;
                } else {
                    desc = input;
                    Matcher balMatcher = Pattern.compile("(?i)(?:bal|balance|rs|inr|opening)\\s*[:=]?\\s*(\\d+(?:\\.\\d+)?)").matcher(input);
                    if (balMatcher.find()) {
                        try {
                            openingBal = Double.parseDouble(balMatcher.group(1));
                        } catch (Exception ignored) {}
                    }
                }
            }

            activeCustomerSessions.remove(userId);
            return executeCreateCustomer(userId, session.getName(), session.getMobile(), desc, openingBal);
        }

        activeCustomerSessions.remove(userId);
        return Map.of("reply", "Session reset. Please type 'create customer' to start again.");
    }

    private Map<String, Object> executeCreateCustomer(String userId, String name, String mobile, String description, Double openingBalance) {
        try {
            Map<String, Object> req = new HashMap<>();
            req.put("name", name);
            req.put("mobile", mobile);
            req.put("openingBalance", openingBalance != null ? openingBalance : 0.0);
            req.put("description", description != null && !description.isBlank() ? description : "Created via Assistant");

            Customer created = customerService.createFromAi(req, userId);

            String reply = String.format(
                "✅ **Customer Created Successfully!**\n\n" +
                "• 👤 **Name:** %s\n" +
                "• 📱 **Phone:** %s\n" +
                "• 📝 **Description:** %s\n" +
                "• 💰 **Opening Balance:** ₹%.2f",
                name, mobile, req.get("description"), req.get("openingBalance")
            );

            Map<String, Object> result = new HashMap<>();
            result.put("reply", reply);
            result.put("action", "CUSTOMER_CREATED");
            if (created != null && created.getId() != null) {
                result.put("customerId", created.getId());
            }

            TransactionCreationSession pendingTxn = activeTxnSessions.get(userId);
            if (pendingTxn != null && pendingTxn.getAmount() != null && pendingTxn.getAmount() > 0 && !pendingTxn.getType().isEmpty()) {
                activeTxnSessions.remove(userId);
                Map<String, Object> txnResult = executeCreateTransaction(
                    userId,
                    created.getId(),
                    created.getName(),
                    pendingTxn.getType(),
                    pendingTxn.getAmount(),
                    pendingTxn.getDescription() != null && !pendingTxn.getDescription().isEmpty() ? pendingTxn.getDescription() : "Recorded via Assistant"
                );
                String txnReply = (String) txnResult.get("reply");
                result.put("reply", reply + "\n\n---\n\n" + txnReply);
                result.put("action", "TRANSACTION_CREATED");
                result.put("transactionId", txnResult.get("transactionId"));
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to create customer from AI: {}", e.getMessage(), e);
            return Map.of("reply", "❌ Failed to create customer: " + e.getMessage() + ". Please try again.");
        }
    }

    // =========================================================================
    // 6. Transaction Creation Wizard
    // =========================================================================

    private Map<String, Object> initiateTransactionCreation(String userId, String input) {
        Long uId = Long.parseLong(userId);

        // Normalize fused letters and numbers: e.g. "ram1000" -> "ram 1000", "1000debit" -> "1000 debit"
        String normalizedInput = input.replaceAll("(?<=[a-zA-Z])(?=\\d)|(?<=\\d)(?=[a-zA-Z])", " ");
        String lower = normalizedInput.toLowerCase();

        // 1. Extract 7-15 digit phone number if present (so 1000 won't be matched as phone)
        Matcher phoneMatcher = Pattern.compile("\\b([6-9]\\d{9}|\\d{7,15})\\b").matcher(normalizedInput);
        String phone = "";
        String textWithoutPhone = normalizedInput;
        if (phoneMatcher.find()) {
            phone = phoneMatcher.group(1);
            textWithoutPhone = textWithoutPhone.replace(phone, " ");
        }

        // 2. Extract amount (e.g. 1000, 1000.50, ₹1000)
        Matcher amtMatcher = Pattern.compile("(?i)(?:rs\\.?|inr|₹)?\\s*\\b(\\d+(?:\\.\\d+)?)\\b").matcher(textWithoutPhone);
        Double amount = null;
        String textWithoutAmt = textWithoutPhone;
        if (amtMatcher.find()) {
            try {
                amount = Double.parseDouble(amtMatcher.group(1));
                textWithoutAmt = textWithoutAmt.replace(amtMatcher.group(0), " ");
            } catch (Exception ignored) {}
        }

        // 3. Extract transaction type
        boolean isDebit = lower.matches(".*\\b(debit|debits|gave|give|udhaar|borrowed)\\b.*");
        boolean isCredit = lower.matches(".*\\b(credit|credits|got|received|jama|paid|payment)\\b.*");

        String type = "";
        if (isDebit && isCredit) {
            // E.g. "ram 1000 debit / credit for milk" -> pick the first one mentioned
            int idxDebit = lower.indexOf("debit");
            int idxCredit = lower.indexOf("credit");
            if (idxDebit != -1 && (idxCredit == -1 || idxDebit < idxCredit)) {
                type = "DEBIT";
            } else {
                type = "CREDIT";
            }
        } else if (isDebit) {
            type = "DEBIT";
        } else if (isCredit) {
            type = "CREDIT";
        }

        // 4. Extract Description ("for ...") and Customer
        String description = "";
        String targetName = "";

        // Check for "for <text>" (e.g. "for milk", "for grocery")
        Matcher forMatcher = Pattern.compile("(?i)\\bfor\\s+([a-zA-Z0-9\\s&/,-]+)").matcher(textWithoutAmt);
        if (forMatcher.find()) {
            String candidate = forMatcher.group(1).trim();
            // Check if candidate is actually a customer name in DB (e.g. "add transaction for Rahul")
            Optional<Customer> forCustomer = findCustomerByUserAndQuery(uId, candidate);
            if (forCustomer.isPresent()) {
                targetName = forCustomer.get().getName();
            } else {
                description = candidate;
            }
            textWithoutAmt = textWithoutAmt.replace(forMatcher.group(0), " ");
        }

        // Check for "to <name>" or "from <name>" (e.g. "to Rahul", "from Priya")
        if (targetName.isEmpty()) {
            Matcher toFromMatcher = Pattern.compile("(?i)\\b(?:to|from)\\s+([a-zA-Z0-9\\s]+)").matcher(textWithoutAmt);
            if (toFromMatcher.find()) {
                targetName = toFromMatcher.group(1).trim();
                textWithoutAmt = textWithoutAmt.replace(toFromMatcher.group(0), " ");
            }
        }

        // Clean remaining keywords (debit, credit, gave, got, slash /, etc.)
        String remainingText = textWithoutAmt
                .replaceAll("(?i)\\b(gave|give|got|received|payment|udhaar|jama|credit|credits|debit|debits|add|new|create|record|transaction|transactions|rs|inr|rupees|amount)\\b", " ")
                .replaceAll("[/\\\\&]", " ")
                .replaceAll("[^a-zA-Z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (targetName.isEmpty() && !remainingText.isEmpty()) {
            // Check if full remainingText matches a customer in DB
            Optional<Customer> fullCustomer = findCustomerByUserAndQuery(uId, remainingText);
            if (fullCustomer.isPresent()) {
                Customer matched = fullCustomer.get();
                targetName = matched.getName();
                if (description.isEmpty()) {
                    String remDesc = remainingText.replaceAll("(?i)\\b" + Pattern.quote(matched.getName()) + "\\b", "").trim();
                    if (!remDesc.isEmpty()) {
                        description = remDesc;
                    }
                }
            } else {
                // If remainingText has multiple words like "ram milk" (user didn't use 'for')
                String[] words = remainingText.split("\\s+");
                boolean foundCust = false;
                for (int i = 0; i < words.length; i++) {
                    Optional<Customer> wordCust = findCustomerByUserAndQuery(uId, words[i]);
                    if (wordCust.isPresent()) {
                        targetName = wordCust.get().getName();
                        StringBuilder descBuilder = new StringBuilder();
                        for (int j = 0; j < words.length; j++) {
                            if (j != i) {
                                if (descBuilder.length() > 0) descBuilder.append(" ");
                                descBuilder.append(words[j]);
                            }
                        }
                        if (description.isEmpty() && descBuilder.length() > 0) {
                            description = descBuilder.toString();
                        }
                        foundCust = true;
                        break;
                    }
                }
                if (!foundCust) {
                    if (!description.isEmpty()) {
                        targetName = remainingText;
                    } else if (words.length > 1) {
                        targetName = words[0];
                        StringBuilder descBuilder = new StringBuilder();
                        for (int j = 1; j < words.length; j++) {
                            if (descBuilder.length() > 0) descBuilder.append(" ");
                            descBuilder.append(words[j]);
                        }
                        description = descBuilder.toString();
                    } else {
                        targetName = remainingText;
                    }
                }
            }
        }

        if (!phone.isEmpty() && targetName.isEmpty()) {
            targetName = phone;
        }

        Optional<Customer> optCust = !targetName.isEmpty() ? findCustomerByUserAndQuery(uId, targetName) : Optional.empty();

        // Case A: Everything is present in one single message! (e.g. "ram1000 debit for milk")
        if (optCust.isPresent() && !type.isEmpty() && amount != null && amount > 0) {
            Customer c = optCust.get();
            String desc = !description.isEmpty() ? description : "Recorded via Assistant (" + (type.equals("DEBIT") ? "Given" : "Received") + ")";
            return executeCreateTransaction(userId, c.getId(), c.getName(), type, amount, desc);
        }

        // Case B: Target customer was specified but not found in DB
        if (optCust.isEmpty() && !targetName.isEmpty()) {
            List<Customer> all = customerRepository.findByUserId(uId);
            String customerList = all.stream()
                    .limit(5)
                    .map(Customer::getName)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("No customers registered yet");

            TransactionCreationSession session = new TransactionCreationSession(TransactionCreationSession.Step.AWAITING_CUSTOMER);
            if (!type.isEmpty()) session.setType(type);
            if (amount != null && amount > 0) session.setAmount(amount);
            if (!description.isEmpty()) session.setDescription(description);
            activeTxnSessions.put(userId, session);

            return Map.of("reply", String.format(
                "⚠️ Customer \"**%s**\" was not found in your records.\n\n" +
                "💡 **Quick create %s:** Type `%s, [phone], [address]` (e.g. `%s, 9876543210, hyd`)\n\n" +
                "📋 **Or choose from your existing customers:** %s\n" +
                "_(Type customer name or 'cancel' to abort)_",
                targetName, targetName, targetName, targetName, customerList
            ));
        }

        TransactionCreationSession session = new TransactionCreationSession(TransactionCreationSession.Step.AWAITING_CUSTOMER);

        if (optCust.isPresent()) {
            Customer c = optCust.get();
            session.setCustomerId(c.getId());
            session.setCustomerName(c.getName());
            session.setType(type);
            session.setAmount(amount != null ? amount : 0.0);
            if (!description.isEmpty()) {
                session.setDescription(description);
            }

            if (type.isEmpty()) {
                session.setStep(TransactionCreationSession.Step.AWAITING_TYPE);
                activeTxnSessions.put(userId, session);
                return Map.of("reply", String.format(
                    "Selected customer: **%s** 👤 (Current Balance: ₹%.2f)\n\n" +
                    "Was this money given or received?\n" +
                    "• Reply **1** or **Gave** (Debit / Udhaar)\n" +
                    "• Reply **2** or **Got** (Credit / Payment)\n\n" +
                    "_(Type 'cancel' anytime to abort)_",
                    c.getName(), c.getCurrentBalance() != null ? c.getCurrentBalance() : 0.0
                ));
            } else if (amount == null || amount <= 0) {
                session.setStep(TransactionCreationSession.Step.AWAITING_AMOUNT);
                activeTxnSessions.put(userId, session);
                return Map.of("reply", String.format(
                    "Recording **%s** for **%s** 💸\n\n" +
                    "Please enter the **Amount** in ₹ (e.g. 500):\n" +
                    "_(Type 'cancel' anytime to abort)_",
                    type.equals("DEBIT") ? "Debit (Gave)" : "Credit (Got)", c.getName()
                ));
            } else if (!description.isEmpty()) {
                // Customer, Type, Amount, Description all known -> execute directly!
                return executeCreateTransaction(userId, c.getId(), c.getName(), type, amount, description);
            } else {
                session.setStep(TransactionCreationSession.Step.AWAITING_DESCRIPTION);
                activeTxnSessions.put(userId, session);
                return Map.of("reply", String.format(
                    "Customer: **%s** | %s of **₹%.2f** 💸\n\n" +
                    "Please enter a **Description / Note** (or type **'skip'** to finish):",
                    c.getName(), type.equals("DEBIT") ? "Debit" : "Credit", amount
                ));
            }
        }

        if (!type.isEmpty()) {
            session.setType(type);
        }
        if (amount != null && amount > 0) {
            session.setAmount(amount);
        }
        if (!description.isEmpty()) {
            session.setDescription(description);
        }
        session.setStep(TransactionCreationSession.Step.AWAITING_CUSTOMER);
        activeTxnSessions.put(userId, session);

        StringBuilder sb = new StringBuilder("💸 **Record New Transaction**\n\n");
        if (amount != null && amount > 0 && !type.isEmpty()) {
            sb.append(String.format("Recording **%s of ₹%.2f**", type.equals("DEBIT") ? "Debit (Gave)" : "Credit (Got)", amount));
            if (!description.isEmpty()) {
                sb.append(String.format(" (for _%s_)", description));
            }
            sb.append(".\n\n");
        } else if (amount != null && amount > 0) {
            sb.append(String.format("Recording amount **₹%.2f**.\n\n", amount));
        }
        sb.append("Who is this transaction for?\n");
        sb.append("Please enter the **Customer Name** (or Phone):\n");
        sb.append("_(Type 'cancel' anytime to abort)_");

        return Map.of("reply", sb.toString());
    }

    private Map<String, Object> handleInteractiveTransactionStep(String userId, String input, TransactionCreationSession session) {
        String lower = input.toLowerCase().trim();
        Long uId = Long.parseLong(userId);

        if (lower.equals("cancel") || lower.equals("exit") || lower.equals("stop") || lower.equals("quit") || lower.equals("abort")) {
            activeTxnSessions.remove(userId);
            return Map.of("reply", "❌ Transaction cancelled. You can type **\"add transaction\"** anytime to start again.");
        }

        if (isTransactionIntent(input)) {
            activeTxnSessions.remove(userId);
            return initiateTransactionCreation(userId, input);
        }

        // STEP 1: AWAITING CUSTOMER
        if (session.getStep() == TransactionCreationSession.Step.AWAITING_CUSTOMER) {
            String cleanQuery = input.replaceAll("(?i)^(customer name is|customer is|to|for|from)\\s*", "").trim();
            Optional<Customer> optCust = findCustomerByUserAndQuery(uId, cleanQuery);

            if (optCust.isEmpty()) {
                List<Customer> all = customerRepository.findByUserId(uId);
                String customerList = all.stream()
                        .limit(5)
                        .map(Customer::getName)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("No registered customers");

                return Map.of("reply", String.format(
                    "⚠️ Customer \"**%s**\" was not found in your records.\n\n" +
                    "💡 **Quick create %s:** Type `%s, [phone], [address]` (e.g. `%s, 9876543210, hyd`)\n\n" +
                    "📋 **Or choose from your existing customers:** %s\n" +
                    "_(Type customer name or 'cancel' to exit)_",
                    cleanQuery, cleanQuery, cleanQuery, cleanQuery, customerList
                ));
            }

            Customer c = optCust.get();
            session.setCustomerId(c.getId());
            session.setCustomerName(c.getName());
            session.setLastUpdatedTime(System.currentTimeMillis());

            // Check if Type is missing
            if (session.getType().isEmpty()) {
                session.setStep(TransactionCreationSession.Step.AWAITING_TYPE);
                return Map.of("reply", String.format(
                    "Selected Customer: **%s** 👤 (Current Balance: ₹%.2f)\n\n" +
                    "Was this money given or received?\n" +
                    "• Reply **1** or **Gave** (Debit / Udhaar)\n" +
                    "• Reply **2** or **Got** (Credit / Payment)\n\n" +
                    "_(Type 'cancel' to abort)_",
                    c.getName(), c.getCurrentBalance() != null ? c.getCurrentBalance() : 0.0
                ));
            } else if (session.getAmount() == null || session.getAmount() <= 0) {
                // Check if Amount is missing
                session.setStep(TransactionCreationSession.Step.AWAITING_AMOUNT);
                return Map.of("reply", String.format(
                    "Customer: **%s** 👤 | %s\n\n" +
                    "Please enter the **Amount** in ₹ (e.g. 500):",
                    c.getName(), session.getType().equals("DEBIT") ? "Debit (Gave)" : "Credit (Got)"
                ));
            } else if (session.getDescription() != null && !session.getDescription().isEmpty()) {
                // ALL 4 fields are ready! (e.g. "add debits 1000 for milk" -> then user provided customer name)
                activeTxnSessions.remove(userId);
                return executeCreateTransaction(userId, c.getId(), c.getName(), session.getType(), session.getAmount(), session.getDescription());
            } else {
                session.setStep(TransactionCreationSession.Step.AWAITING_DESCRIPTION);
                return Map.of("reply", String.format(
                    "Customer: **%s** | %s of **₹%.2f** 💸\n\n" +
                    "Please enter a **Description / Note** (or type **'skip'** to finish):",
                    c.getName(), session.getType().equals("DEBIT") ? "Debit" : "Credit", session.getAmount()
                ));
            }
        }

        // STEP 2: AWAITING TYPE (DEBIT vs CREDIT)
        if (session.getStep() == TransactionCreationSession.Step.AWAITING_TYPE) {
            String type = "";
            if (lower.equals("1") || lower.contains("gave") || lower.contains("give") || lower.contains("debit") || lower.contains("udhaar")) {
                type = "DEBIT";
            } else if (lower.equals("2") || lower.contains("got") || lower.contains("received") || lower.contains("credit") || lower.contains("jama") || lower.contains("paid") || lower.contains("payment")) {
                type = "CREDIT";
            }

            if (type.isEmpty()) {
                return Map.of("reply", """
                    ⚠️ Please select:
                    • Reply **1** or **Gave** (Debit / Udhaar)
                    • Reply **2** or **Got** (Credit / Payment)
                    _(Type 'cancel' to exit)_
                    """);
            }

            session.setType(type);
            session.setLastUpdatedTime(System.currentTimeMillis());

            if (session.getAmount() == null || session.getAmount() <= 0) {
                session.setStep(TransactionCreationSession.Step.AWAITING_AMOUNT);
                return Map.of("reply", String.format(
                    "Type: **%s** 💳\n\nNow, please enter the **Amount** in ₹ (e.g. 500):",
                    type.equals("DEBIT") ? "Debit (Udhaar / You Gave)" : "Credit (Payment / You Received)"
                ));
            } else if (session.getDescription() != null && !session.getDescription().isEmpty()) {
                activeTxnSessions.remove(userId);
                return executeCreateTransaction(userId, session.getCustomerId(), session.getCustomerName(), session.getType(), session.getAmount(), session.getDescription());
            } else {
                session.setStep(TransactionCreationSession.Step.AWAITING_DESCRIPTION);
                return Map.of("reply", String.format(
                    "Customer: **%s** | %s of **₹%.2f** 💸\n\nNow, please enter a **Description / Note** (or type **'skip'** to finish):",
                    session.getCustomerName(), type.equals("DEBIT") ? "Debit" : "Credit", session.getAmount()
                ));
            }
        }

        // STEP 3: AWAITING AMOUNT
        if (session.getStep() == TransactionCreationSession.Step.AWAITING_AMOUNT) {
            Matcher amtMatcher = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(input);
            Double amount = null;
            if (amtMatcher.find()) {
                try {
                    amount = Double.parseDouble(amtMatcher.group(1));
                } catch (Exception ignored) {}
            }

            if (amount == null || amount <= 0) {
                return Map.of("reply", "⚠️ Please enter a valid numeric amount (e.g., 500 or 1200.50), or type 'cancel' to exit:");
            }

            session.setAmount(amount);
            session.setLastUpdatedTime(System.currentTimeMillis());

            if (session.getDescription() != null && !session.getDescription().isEmpty()) {
                activeTxnSessions.remove(userId);
                return executeCreateTransaction(userId, session.getCustomerId(), session.getCustomerName(), session.getType(), session.getAmount(), session.getDescription());
            }

            session.setStep(TransactionCreationSession.Step.AWAITING_DESCRIPTION);
            return Map.of("reply", String.format(
                "Amount: **₹%.2f** 💰\n\nNow, please enter a **Description / Note** for this transaction (e.g., 'Groceries bill', or type **'skip'**):",
                amount
            ));
        }

        // STEP 4: AWAITING DESCRIPTION
        if (session.getStep() == TransactionCreationSession.Step.AWAITING_DESCRIPTION) {
            boolean isSkip = lower.equals("skip") || lower.equals("none") || lower.equals("no")
                    || lower.equals("-") || lower.equals("na") || lower.equals("n/a");

            String desc = isSkip
                    ? "Recorded via Assistant (" + (session.getType().equals("DEBIT") ? "Given" : "Received") + ")"
                    : input;

            activeTxnSessions.remove(userId);
            return executeCreateTransaction(userId, session.getCustomerId(), session.getCustomerName(), session.getType(), session.getAmount(), desc);
        }

        activeTxnSessions.remove(userId);
        return Map.of("reply", "Session reset. Please type 'add transaction' to start again.");
    }

    private Map<String, Object> executeCreateTransaction(String userId, Long customerId, String customerName, String type, Double amount, String description) {
        try {
            Long uId = Long.parseLong(userId);

            Long resolvedCustomerId = customerId;
            String resolvedCustomerName = customerName != null ? customerName : "Unknown";

            if (resolvedCustomerId == null && customerName != null) {
                final String lookupName = customerName;
                Customer c = findCustomerByUserAndQuery(uId, lookupName)
                        .orElseThrow(() -> new RuntimeException("Customer '" + lookupName + "' not found"));
                resolvedCustomerId = c.getId();
                resolvedCustomerName = c.getName();
            }

            Transaction t = new Transaction();
            t.setCustomerId(resolvedCustomerId);
            t.setAmount(amount);
            t.setType(type);
            t.setDescription(description != null && !description.isBlank() ? description : "Recorded via Assistant");

            Transaction saved = transactionService.addTransaction(t, uId);

            boolean isDebit = "DEBIT".equalsIgnoreCase(type);
            String typeLabel = isDebit ? "Debit (Udhaar / You Gave) 🔴" : "Credit (Payment / You Received) 🟢";

            String reply = String.format(
                "✅ **Transaction Recorded Successfully!**\n\n" +
                "• 👤 **Customer:** %s\n" +
                "• 💳 **Type:** %s\n" +
                "• 💰 **Amount:** ₹%.2f\n" +
                "• 📝 **Description:** %s\n" +
                "• 📊 **Updated Balance:** ₹%.2f",
                customerName,
                typeLabel,
                amount,
                t.getDescription(),
                saved.getBalanceAfter() != null ? saved.getBalanceAfter() : 0.0
            );

            Map<String, Object> result = new HashMap<>();
            result.put("reply", reply);
            result.put("action", "TRANSACTION_CREATED");
            result.put("customerId", customerId);
            if (saved.getId() != null) {
                result.put("transactionId", saved.getId());
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to record transaction: {}", e.getMessage(), e);
            return Map.of("reply", "❌ Could not record transaction: " + e.getMessage() + ". Please try again.");
        }
    }

    // =========================================================================
    // 7. OpenAI Response Handling
    // =========================================================================

    @SuppressWarnings("unchecked")
    private Map<String, Object> handleAiResponse(String userId, String aiResponse, String originalInput) {
        String cleanedJson = aiResponse.replaceAll("(?s)```(?:json)?\\s*", "").replaceAll("```", "").trim();

        Map<String, Object> json;
        try {
            json = new com.fasterxml.jackson.databind.ObjectMapper().readValue(cleanedJson, Map.class);
        } catch (Exception e) {
            log.warn("Failed to parse OpenAI JSON response: {}", aiResponse);
            return processLocally(userId, originalInput, null);
        }

        String action = json.get("action") != null ? json.get("action").toString().trim() : "";

        // ================= CREATE CUSTOMER =================
        if ("CREATE_CUSTOMER".equalsIgnoreCase(action)) {
            String name = json.get("name") != null ? json.get("name").toString().trim() : "";
            String mobile = json.get("mobile") != null ? json.get("mobile").toString().trim() : "";
            Double openingBalance = 0.0;
            if (json.get("openingBalance") != null) {
                try {
                    openingBalance = Double.parseDouble(json.get("openingBalance").toString());
                } catch (Exception ignored) {}
            }
            String description = json.get("description") != null ? json.get("description").toString().trim() : "";

            if (name.isEmpty() || mobile.isEmpty()) {
                return initiateCustomerCreation(userId, originalInput);
            }

            return executeCreateCustomer(userId, name, mobile, description, openingBalance);
        }

        // ================= ADD TRANSACTION =================
        if ("ADD_TRANSACTION".equalsIgnoreCase(action)) {
            String name = json.get("name") != null ? json.get("name").toString().trim() : "";
            Double amount = null;
            if (json.get("amount") != null) {
                try {
                    amount = Double.parseDouble(json.get("amount").toString());
                } catch (Exception ignored) {}
            }
            String type = json.get("type") != null ? json.get("type").toString().trim() : "DEBIT";
            String description = json.get("description") != null ? json.get("description").toString().trim() : "";

            if (name.isEmpty() || amount == null || amount <= 0) {
                return initiateTransactionCreation(userId, originalInput);
            }

            return executeCreateTransaction(userId, null, name, type, amount, description);
        }

        // ================= DASHBOARD =================
        if ("GET_DASHBOARD".equalsIgnoreCase(action)) {
            try {
                Map<String, Object> data = dashboardService.getSummary(Long.parseLong(userId));
                return Map.of(
                    "reply",
                    "📊 Business Summary:\n• Total Customers: " + data.get("totalCustomers") +
                    "\n• Total Outstanding: ₹" + data.get("totalOutstanding") +
                    "\n• Total Debit: ₹" + data.get("totalDebit") +
                    "\n• Total Credit: ₹" + data.get("totalCredit")
                );
            } catch (Exception e) {
                return Map.of("reply", "Failed to load dashboard: " + e.getMessage() + " ❌");
            }
        }

        return processLocally(userId, originalInput, null);
    }

    // =========================================================================
    // 8. Built-in Local Command Interpreter
    // =========================================================================

    private Map<String, Object> processLocally(String userId, String input, String note) {
        String lower = input.toLowerCase().trim();

        // 1. Greetings & Help
        if (lower.matches("^(hi|hello|hey|help|namaste|hlo|helo|start|menu).*") || lower.equals("help") || lower.equals("?")) {
            String help = """
                👋 Hello! I am your Digital Katha Assistant.

                You can use these commands:
                • 👤 Create Customer: Type "Create customer" (Interactive setup: Name ➔ Phone ➔ Description) or "Create customer Rahul 9876543210 500"
                • 💸 Record Transaction: Type "Add transaction" (Interactive setup: Customer ➔ Gave/Got ➔ Amount ➔ Note) or "Gave 500 to Rahul" / "Got 200 from Rahul"
                • 📊 View Summary: "Show dashboard" or "Outstanding"
                """;
            if (note != null) {
                help += "\nℹ️ Note: " + note;
            }
            return Map.of("reply", help.trim());
        }

        // 2. Dashboard / Summary
        if (lower.contains("dashboard") || lower.contains("summary") || lower.contains("overview")
                || lower.contains("outstanding") || lower.contains("total balance") || lower.contains("hisab")
                || lower.contains("report")) {
            try {
                Map<String, Object> data = dashboardService.getSummary(Long.parseLong(userId));
                return Map.of(
                    "reply",
                    "📊 Business Summary:\n• Total Customers: " + data.get("totalCustomers") +
                    "\n• Total Outstanding: ₹" + data.get("totalOutstanding") +
                    "\n• Total Debit: ₹" + data.get("totalDebit") +
                    "\n• Total Credit: ₹" + data.get("totalCredit")
                );
            } catch (Exception e) {
                return Map.of("reply", "Failed to retrieve dashboard: " + e.getMessage() + " ❌");
            }
        }

        // 3. Create Customer
        if (isCustomerCreationIntent(input)) {
            return initiateCustomerCreation(userId, input);
        }

        // 4. Add Transaction
        if (isTransactionIntent(input)) {
            return initiateTransactionCreation(userId, input);
        }

        // 5. Unrecognized command fallback
        StringBuilder reply = new StringBuilder();
        if (note != null) {
            reply.append("⚠️ ").append(note).append("\n\n");
        } else {
            reply.append("I couldn't understand that command.\n\n");
        }
        reply.append("""
            Available commands:
            • "Create customer" (Interactive setup: Name ➔ Phone ➔ Description)
            • "Add transaction" (Interactive setup: Customer ➔ Gave/Got ➔ Amount ➔ Note)
            • "Gave [Amount] to [Customer]" (e.g. Gave 500 to Rahul)
            • "Got [Amount] from [Customer]" (e.g. Got 200 from Rahul)
            • "Show dashboard summary"

            💡 Tip: To enable advanced AI reasoning, set a valid `openai.api.key` in `application.properties` or set the `OPENAI_API_KEY` environment variable.
            """);

        return Map.of("reply", reply.toString().trim());
    }

    // =========================================================================
    // 9. OpenAI Network Call
    // =========================================================================

    private String callOpenAI(String input) {
        String prompt = """
        You are an AI assistant for a digital ledger app (Digital Katha).
        Convert the user input into a JSON command.

        Actions:
        - CREATE_CUSTOMER: when creating or adding a new customer.
        - ADD_TRANSACTION: when recording money given (type: "DEBIT") or money received (type: "CREDIT").
        - GET_DASHBOARD: when requesting dashboard, balance, or business summary.

        Return ONLY valid JSON (no markdown formatting):
        {
         "action": "CREATE_CUSTOMER" | "ADD_TRANSACTION" | "GET_DASHBOARD",
         "name": "",
         "mobile": "",
         "amount": 0,
         "type": "DEBIT" | "CREDIT",
         "openingBalance": 0,
         "description": ""
        }

        User Input: "%s"
        """.formatted(input);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.trim());

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-4o-mini");
        body.put("temperature", 0);

        body.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        Map response = restTemplate.postForObject(
                "https://api.openai.com/v1/chat/completions",
                request,
                Map.class
        );

        if (response == null || !response.containsKey("choices")) {
            throw new RuntimeException("Empty response from OpenAI");
        }

        Map choice = (Map) ((List) response.get("choices")).get(0);
        Map message = (Map) choice.get("message");

        return message.get("content").toString().trim();
    }
}