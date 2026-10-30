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

@Controller
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;
    private final TransactionAuditService auditService;

    private void loadDashboard(Model model, String keyword) {
        model.addAttribute("customers", bankService.searchCustomers(keyword));
        model.addAttribute("transactions", bankService.getLatestTransactions());
        model.addAttribute("totalBalance", bankService.getTotalBalance());
        model.addAttribute("successCount", bankService.getSuccessCount());
        model.addAttribute("failedCount", bankService.getFailedCount());
        if (!model.containsAttribute("transferRequest")) {
            model.addAttribute("transferRequest", new TransferRequest());
        }
        if (!model.containsAttribute("newCustomer")) {
            model.addAttribute("newCustomer", new Customer());
        }
        model.addAttribute("keyword", keyword);
    }

    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) String keyword, Model model) {
        loadDashboard(model, keyword);
        return "bank";
    }

    @PostMapping("/customers")
    public String addCustomer(@Valid @ModelAttribute("newCustomer") Customer customer,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            loadDashboard(model, null);
            return "bank";
        }
        try {
            bankService.createCustomer(customer);
            redirectAttributes.addFlashAttribute("successMessage", "Customer added successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
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
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            return "redirect:/";
        }
        try {
            bankService.createCustomer(customer);
            redirectAttributes.addFlashAttribute("successMessage", "Customer added successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/transfer")
    public String transferMoney(@Valid @ModelAttribute("transferRequest") TransferRequest request,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
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
            redirectAttributes.addFlashAttribute("successMessage", "Transfer completed successfully.");
        } catch (Exception e) {
            auditService.recordFailure(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    e.getMessage()
            );
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
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
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            return "redirect:/";
        }
        try {
            bankService.transfer(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    request.isSimulateFailure()
            );
            redirectAttributes.addFlashAttribute("successMessage", "Transfer completed successfully.");
        } catch (Exception e) {
            auditService.recordFailure(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    e.getMessage()
            );
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/";
    }

    @GetMapping("/accounts/{accountNumber}/history")
    public String accountHistory(@PathVariable String accountNumber, Model model) {
        model.addAttribute("customer", bankService.getCustomer(accountNumber));
        model.addAttribute("history", bankService.getAccountHistory(accountNumber));
        return "history";
    }
}
