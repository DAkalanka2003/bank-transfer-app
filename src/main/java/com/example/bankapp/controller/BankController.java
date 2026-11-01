package com.example.bankapp.controller;

import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.model.Customer;
import com.example.bankapp.service.BankService;
import com.example.bankapp.service.TransactionAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;
    private final TransactionAuditService auditService;

    private void loadDashboard(Model model, String keyword) {
        model.addAttribute("totalCustomers", bankService.countCustomersNative());
        model.addAttribute("totalBalance", bankService.getTotalBalance());
        model.addAttribute("successCount", bankService.countSuccessTransactions());
        model.addAttribute("failedCount", bankService.countFailedTransactions());
        model.addAttribute("allCustomers", bankService.getAllCustomers());
        model.addAttribute("customers", bankService.searchCustomers(keyword));
        model.addAttribute("transactions", bankService.getAllTransactions());
        if (!model.containsAttribute("transferRequest")) {
            model.addAttribute("transferRequest", new TransferRequest());
        }
        if (!model.containsAttribute("customer")) {
            model.addAttribute("customer", new Customer());
        }
        model.addAttribute("keyword", keyword);
    }

    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) String keyword, Model model) {
        loadDashboard(model, keyword);
        return "bank";
    }

    @PostMapping("/customers")
    public String addCustomer(@Valid @ModelAttribute("customer") Customer customer,
                              BindingResult result,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            loadDashboard(model, null);
            return "bank";
        }
        try {
            bankService.createCustomer(customer);
            redirectAttributes.addFlashAttribute("success", "Customer added successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping(value = "/customers", consumes = "application/json")
    public String addCustomerJson(@Valid @RequestBody Customer customer,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldErrors().stream()
                    .map(err -> err.getField() + ": " + err.getDefaultMessage())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Invalid customer details provided.");
            redirectAttributes.addFlashAttribute("error", errorMsg);
            return "redirect:/";
        }
        try {
            bankService.createCustomer(customer);
            redirectAttributes.addFlashAttribute("success", "Customer added successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/transfer")
    public String transferMoney(@Valid @ModelAttribute("transferRequest") TransferRequest request,
                                BindingResult result,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            loadDashboard(model, null);
            return "bank";
        }
        try {
            bankService.transfer(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    request.isSimulateFailure()
            );
            redirectAttributes.addFlashAttribute("success", "Transfer completed successfully.");
        } catch (Exception e) {
            auditService.recordFailure(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    e.getMessage()
            );
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping(value = "/transfer", consumes = "application/json")
    public String transferMoneyJson(@Valid @RequestBody TransferRequest request,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldErrors().stream()
                    .map(err -> err.getField() + ": " + err.getDefaultMessage())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Validation error in transfer request.");
            redirectAttributes.addFlashAttribute("error", errorMsg);
            return "redirect:/";
        }
        try {
            bankService.transfer(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    request.isSimulateFailure()
            );
            redirectAttributes.addFlashAttribute("success", "Transfer completed successfully.");
        } catch (Exception e) {
            auditService.recordFailure(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    e.getMessage()
            );
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/";
    }

    @GetMapping("/deposit")
    public String depositPage(Model model) {
        model.addAttribute("customers", bankService.getAllCustomers());
        return "deposit";
    }

    @PostMapping("/deposit")
    public String depositMoney(@RequestParam String accountNumber,
                               @RequestParam BigDecimal amount,
                               RedirectAttributes redirectAttributes) {
        try {
            bankService.deposit(accountNumber, amount);
            redirectAttributes.addFlashAttribute("success", "Deposit successful!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/deposit";
    }

    @GetMapping("/withdraw")
    public String withdrawPage(Model model) {
        model.addAttribute("customers", bankService.getAllCustomers());
        return "withdraw";
    }

    @PostMapping("/withdraw")
    public String withdrawMoney(@RequestParam String accountNumber,
                                @RequestParam BigDecimal amount,
                                RedirectAttributes redirectAttributes) {
        try {
            bankService.withdraw(accountNumber, amount);
            redirectAttributes.addFlashAttribute("success", "Withdrawal successful!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/withdraw";
    }

    @GetMapping("/large-transfers")
    public String largeTransfers(@RequestParam(defaultValue = "10000") BigDecimal min, Model model) {
        model.addAttribute("min", min);
        model.addAttribute("transfers", bankService.getLargeTransfers(min));
        return "large-transfers";
    }

    @GetMapping("/accounts/{accountNumber}/history")
    public String accountHistory(@PathVariable String accountNumber, Model model) {
        model.addAttribute("customer", bankService.getCustomer(accountNumber));
        model.addAttribute("transactions", bankService.getAccountHistory(accountNumber));
        model.addAttribute("totalSent", bankService.getTotalSent(accountNumber));
        return "history";
    }
}
