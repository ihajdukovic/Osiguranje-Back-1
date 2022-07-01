package raf.osiguranje.accounttransaction.services;

import io.jsonwebtoken.Jwts;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import raf.osiguranje.accounttransaction.model.Account;
import raf.osiguranje.accounttransaction.model.Balance;
import raf.osiguranje.accounttransaction.model.BalanceId;
import raf.osiguranje.accounttransaction.model.dto.*;
import raf.osiguranje.accounttransaction.repositories.AccountRepository;
import raf.osiguranje.accounttransaction.repositories.BalanceRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor
@Service
public class BalanceService {

    private AccountRepository accountRepository;
    private BalanceRepository balanceRepository;

    private RestTemplate rest;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${api.securities}")
    private String securitiesApiUrl;

    @Value("${api.usercrud}")
    private String usercrudApiUrl;

    @Value("${api.buyingmarket}")
    private String buyingApiUrl;

    @Autowired
    public BalanceService(AccountRepository accountRepository, BalanceRepository balanceRepository, RestTemplate rest) {
        this.accountRepository = accountRepository;
        this.balanceRepository = balanceRepository;
        this.rest = rest;
    }

    public boolean createBalance(Long accountNumber,Long securityId,SecurityType securityType,int amount,String jwt) throws Exception{
        Account account = accountRepository.findAccountByAccountNumber(accountNumber);
        if(accountNumber==null){
            throw new Exception("Couldn't find account");
        }
        System.out.println(accountNumber + " {} " + account);
        String email = extractUsername(jwt);
        UserDto user = getUserByUsernameFromUserService(email);

        /*
        Proveravam da li postoji securiti u nase sistemu
         */
        try {
            if(securityType.equals(SecurityType.CURRENCY)){
                CurrencyDTO currencyDTO = getCurrencyById(securityId,jwt);
            }else {
                SecurityDTO securityDto = getSecurityByTypeAndId(securityType, securityId, jwt);
            }
        } catch (Exception e) {
            throw e;
        }
        if (balanceRepository.findById(new BalanceId(accountNumber, securityId, securityType)).isPresent()){
            throw new Exception("Balance already exist");
        }

        Balance balance;
        try {
            balance = new Balance(account, securityId, securityType, amount);
            System.out.println(balance);
            balanceRepository.save(balance);
        }catch (Exception e){
            throw e;
        }

        return true;
    }

    public boolean deleteBalance(Long accountNumber,Long securityId, SecurityType securityType,String jwt) throws Exception{

        Optional<Balance> balance = balanceRepository.findById(new BalanceId(accountNumber,securityId,securityType));
        if(balance.isEmpty())
            throw new Exception("Couldn't find balance");

        balanceRepository.delete(balance.get());
        return true;
    }

    public List<Balance> getAllBalances(){
        return balanceRepository.findAll();
    }

    @Transactional
    public List<Balance> getBalancesByAccount(Long accountId){
        Account account = accountRepository.findAccountByAccountNumber(accountId);
        if(account==null){
            return new ArrayList<>();
        }
        return balanceRepository.findBalanceByAccount(account);
    }

    @Transactional
    public List<Balance> getBalancesBySecurity(Long security){
        return balanceRepository.findBalanceBySecurityId(security);
    }

    public List<Balance> getBalancesByAccountAndSecurity(Long accountId, Long security){
        Account account = accountRepository.findAccountByAccountNumber(accountId);
        if(account==null){
            return Collections.emptyList();
        }
        return balanceRepository.findBalanceByAccountIdAndSecurityId(accountId,security);
    }

    @Transactional
    public Optional<Balance> getBalancesByFullId(Long accountId, Long security,SecurityType securityType){
        Account account = accountRepository.findAccountByAccountNumber(accountId);
        if(account==null){
            return Optional.empty();
        }
        return balanceRepository.findById(new BalanceId(accountId,security,securityType));
    }

    @Transactional
    public boolean updateAmount(BalanceUpdateDto input,String jwt) throws Exception{

        Optional<Balance> balanceOptional = getBalancesByFullId(input.getAccountId(), input.getSecurityId(), input.getSecurityType());
        if(balanceOptional.isEmpty()){
            throw new Exception("Couldn't find balance" + input);
        }

        Balance balance = balanceOptional.get();

        int newAmount = balance.getAmount() + input.getAmount();

        if(input.getAmount()+ balance.getAvailable() < 0){
            throw new Exception("Overflow amount: "+newAmount+". Not enough available");
        }
        balance.setAmount(newAmount);
        balanceRepository.save(balance);

        return true;
    }

    @Transactional
    public boolean updateReserve(BalanceUpdateDto input,String jwt) throws Exception{
        Optional<Balance> balanceOptional = getBalancesByFullId(input.getAccountId(), input.getSecurityId(), input.getSecurityType());
        if(balanceOptional.isEmpty()){
            throw new Exception("Couldn't find balance" + input);
        }

        Balance balance = balanceOptional.get();

        int newReserve = balance.getReserved() + input.getAmount();

        if(newReserve < 0  || balance.getAmount() < newReserve ){
            throw new Exception("Overflow amount: "+ newReserve);
        }
        balance.setReserved(newReserve);
        balanceRepository.save(balance);

        return true;
    }

    public String extractUsername(String jws) {
        jws = jws.replace("Bearer ", "");
        return Jwts.parser()
                .setSigningKey(jwtSecret.getBytes())
                .parseClaimsJws(jws)
                .getBody()
                .getSubject();
    }

    public UserDto getUserByUsernameFromUserService(String username) throws Exception {
        String urlString = usercrudApiUrl + "/api/users/search/email";
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(urlString);
        String urlTemplate = uriComponentsBuilder.queryParam("email", username).encode().toUriString();
        ResponseEntity<UserDto> response = null;
        try {
            response = rest.exchange(urlTemplate, HttpMethod.GET, null, UserDto.class);
        } catch(RestClientException e) {
            throw new Exception("Something went wrong while trying to retrieve user info");
        }
        UserDto user = null;
        if(response.getBody() != null) {
            user = response.getBody();
        }
        if(user == null) {
            throw new IllegalArgumentException("Something went wrong trying to find user");
        }
        return user;
    }

    protected CurrencyDTO getCurrencyById(Long id,String jwtToken) throws Exception{
        String urlString = securitiesApiUrl + "/api/data/currency/" + id;
        ResponseEntity<CurrencyDTO> response;
        try {
            response = rest.exchange(urlString, HttpMethod.GET, null, CurrencyDTO.class);
        } catch(RestClientException e) {
            throw new Exception("Something went wrong while trying to retrieve security info");
        }

        CurrencyDTO currencyDTO = null;
        if(response.getBody() != null){
            currencyDTO = response.getBody();
        }

        if (currencyDTO == null) {
            throw new IllegalArgumentException("Something went wrong trying to find security");
        }

        return currencyDTO;
    }

    protected SecurityDTO getSecurityByTypeAndId(SecurityType securityType, Long securityId,String jwtToken) throws Exception {
        String urlString = securitiesApiUrl + "/api/data/" + securityType.toString().toLowerCase() + "/" + securityId;
        ResponseEntity<SecurityDTO> response;
        try {
            response = rest.exchange(urlString, HttpMethod.GET, null, SecurityDTO.class);
        } catch(RestClientException e) {
            throw new Exception("Something went wrong while trying to retrieve security info");
        }
        SecurityDTO security = null;
        if(response.getBody() != null) {
            security = response.getBody();
        }
        if (security == null) {
            throw new IllegalArgumentException("Something went wrong trying to find security");
        }
        return security;
    }
}
