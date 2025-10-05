package swp391.fa25.lms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import swp391.fa25.lms.model.Account;
import swp391.fa25.lms.repository.AccountRepo;


@Service
public class AccountService {
    @Autowired
    private AccountRepo accountRepo;

    public Account getAccountByEmail(String email){
        return accountRepo.findByEmail(email);
    }

    public Account getAccountById(long id){
        return accountRepo.findById(id).orElse(null);
    }
}
