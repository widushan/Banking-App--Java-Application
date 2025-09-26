package com.example.bankapp.service;

import com.example.bankapp.repository.AccountRepository;
import com.example.bankapp.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.bankapp.model.Transaction;
import com.example.bankapp.model.Account;


@Service
public class AccountService implements UserDetailsService {

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    public Account findByUsername (String username){
        return accountRepository.findAccountByUsername(username).orElseThrow(() -> new RuntimeException("Account not found!"));
    }

    public Account registerAccount(String username, String password) {
        if (accountRepository.findAccountByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists.");
        }

        Account account = new Account();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(password));
        account.setBalance(BigDecimal.ZERO);

        return accountRepository.save(account);
    }


    public void deposit(Account account, BigDecimal amount) {
    account.setBalance(account.getBalance().add(amount));
    accountRepository.save(account);

    Transaction transaction = new Transaction(
        "Deposit",
        amount,
        LocalDateTime.now(),
        account
    );
    transactionRepository.save(transaction);
}

public void withdraw(Account account, BigDecimal amount) {
    if (account.getBalance().compareTo(amount) < 0) {
        throw new RuntimeException("Insufficient funds");
    }

    account.setBalance(account.getBalance().subtract(amount));
    accountRepository.save(account);

    Transaction transaction = new Transaction(
        "Withdrawal",
        amount,
        LocalDateTime.now(),
        account
    );
    transactionRepository.save(transaction);
}

    public List<Transaction> getTransactionHistory(Account account) {
        return transactionRepository.findByAccountId(account.getId());
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountRepository.findAccountByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Username or Password not found"));
        account.setAuthorities(authorities());
        return account;
    }

    private Collection<? extends GrantedAuthority> authorities() {
        return Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"));
    }


    public void transferAmount(Account fromAccount, String toUsername, BigDecimal amount) {
    if (fromAccount.getBalance().compareTo(amount) < 0) {
        throw new RuntimeException("Insufficient funds");
    }

    Account toAccount = accountRepository.findAccountByUsername(toUsername)
            .orElseThrow(() -> new RuntimeException("Recipient account not found"));

    // Deduct
    fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
    accountRepository.save(fromAccount);

    // Add
    toAccount.setBalance(toAccount.getBalance().add(amount));
    accountRepository.save(toAccount);

    // Create transaction records
    Transaction debitTransaction = new Transaction(
        "Transfer Out to " + toAccount.getUsername(),
        amount,
        LocalDateTime.now(),
        fromAccount
    );
    transactionRepository.save(debitTransaction);

    Transaction creditTransaction = new Transaction(
        "Transfer In from " + fromAccount.getUsername(),
        amount, 
        LocalDateTime.now(),
        toAccount
    );
    transactionRepository.save(creditTransaction);
}

    public Account findAccountByUsername(String username) {
        return accountRepository.findAccountByUsername(username)
            .orElseThrow(() -> new RuntimeException("Account not found!"));
    }
 


}

